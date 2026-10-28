package com.chronogenesis.temple;

import java.util.Locale;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.player.Power;
import com.chronogenesis.player.PlayerEssence;
import com.chronogenesis.player.Powers;
import com.chronogenesis.registry.ModAttachments;
import com.chronogenesis.time.TimeZone;
import com.chronogenesis.time.TimeZones;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * A temple seal: one of five locks on a Temporal Core, each opened by a different gift of the glands. Breaking any
 * three stills the core ({@link TemporalCoreBlockEntity}). Seals cannot be broken like blocks; each has its own way.
 */
@EventBusSubscriber(modid = ChronoGenesis.MODID)
public class SealBlock extends Block {
    public static final BooleanProperty BROKEN = BooleanProperty.create("broken");
    /** Faster than a sprint, in blocks per tick: only Quickening (or a charge) reaches it. */
    public static final double SWIFT_SPEED = 0.32;
    /** Shrunk at least this much to turn the Narrow Seal. */
    public static final double NARROW_SCALE = 0.75;
    /** Time at the Stilled Seal must run at most this fast for it to be touched. */
    public static final double STILL_RATE = 0.6;
    /** Whether the local player sees veiled things (set by the client: Glowing Sight is on). */
    public static java.util.function.BooleanSupplier veiledSight = () -> false;

    public enum Kind {
        /** Density: punched with Iron Fists, or shaken by a Ground Slam. */
        HEAVY,
        /** Size: at the end of a crawlspace, and turned only by a shrunken hand. */
        NARROW,
        /** Speed: run into it faster than a sprint. */
        SWIFT,
        /** Awareness: hidden in the wall; only Glowing Sight shows (and finds) it. */
        VEILED,
        /** Time: it spins too fast to touch, unless time around it is slowed or stopped. */
        STILLED;

        public String key() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private final Kind kind;

    public SealBlock(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(BROKEN, false));
    }

    public Kind kind() {
        return kind;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BROKEN);
    }

    /** Breaks a seal (if not broken) and tells its core. */
    public static boolean breakSeal(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof SealBlock) || state.getValue(BROKEN)) {
            return false;
        }
        level.setBlockAndUpdate(pos, state.setValue(BROKEN, true));
        level.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.2f, 0.5f);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 40, 0.4, 0.4, 0.4, 0.2);
        for (TemporalCoreBlockEntity core : TemporalCoreBlockEntity.LOADED) {
            if (core.getLevel() == level && core.barrier().inflate(2).contains(pos.getCenter())) {
                core.sealBroken(level);
                break;
            }
        }
        return true;
    }

    private static void refuse(Player player, String why) {
        player.displayClientMessage(Component.translatable("message.chronogenesis.seal." + why).withStyle(ChatFormatting.GRAY), true);
    }

    /** Whether this player can break this seal by using it (and if not, why not). */
    public String refusal(Player player, Level level, BlockPos pos) {
        PlayerEssence essence = player.getData(ModAttachments.PLAYER_ESSENCE);
        return switch (kind) {
            case HEAVY -> "heavy";
            case SWIFT -> "swift";
            case NARROW -> player.getScale() <= NARROW_SCALE ? null : "narrow";
            case VEILED -> essence.isOn(Power.GLOWING_SIGHT) ? null : "veiled";
            case STILLED -> {
                TimeZone zone = TimeZones.at(level, pos);
                yield zone != null && zone.rate <= STILL_RATE ? null : "stilled";
            }
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(BROKEN)) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server) {
            String why = refusal(player, level, pos);
            if (why == null) {
                breakSeal(server, pos);
            } else {
                refuse(player, why);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /** The Heavy Seal gives way to a fist of iron. */
    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (kind == Kind.HEAVY && level instanceof ServerLevel server && !state.getValue(BROKEN)) {
            if (Powers.ironFists(player, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState())) {
                breakSeal(server, pos);
            } else {
                refuse(player, "heavy");
            }
        }
    }

    /** A Ground Slam landing shakes loose the Heavy Seals around it. */
    public static void slam(ServerLevel level, BlockPos centre, int radius) {
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-radius, -2, -radius), centre.offset(radius, 2, radius))) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof SealBlock seal && seal.kind == Kind.HEAVY) {
                breakSeal(level, pos.immutable());
            }
        }
    }

    /** The Swift Seal breaks when someone runs into it faster than a sprint. */
    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Vec3 motion = player.position().subtract(player.xo, player.yo, player.zo);
        if (motion.horizontalDistance() < SWIFT_SPEED) {
            return;
        }
        Vec3 ahead = player.position().add(motion.normalize().scale(0.8));
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(ahead).offset(-1, 0, -1), BlockPos.containing(ahead).offset(1, 1, 1))) {
            BlockState state = player.level().getBlockState(pos);
            if (state.getBlock() instanceof SealBlock seal && seal.kind == Kind.SWIFT && !state.getValue(BROKEN)
                    && pos.getCenter().distanceToSqr(player.position().add(0, 0.5, 0)) < 2.5) {
                breakSeal(player.serverLevel(), pos.immutable());
            }
        }
    }

    /** The Veiled Seal glimmers, for eyes that can see it; the Stilled Seal spins. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(BROKEN)) {
            return;
        }
        if (kind == Kind.VEILED) {
            if (veiledSight.getAsBoolean()) {
                level.addParticle(ParticleTypes.GLOW, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(),
                        pos.getZ() + random.nextDouble(), 0, 0.01, 0);
            }
        } else if (kind == Kind.STILLED) {
            double angle = (level.getGameTime() % 20) / 20.0 * Math.PI * 2;
            level.addParticle(ParticleTypes.ENCHANT, pos.getX() + 0.5 + Math.cos(angle) * 0.7, pos.getY() + 0.5,
                    pos.getZ() + 0.5 + Math.sin(angle) * 0.7, 0, 0, 0);
        }
    }
}

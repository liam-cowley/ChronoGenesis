package com.chronogenesis.timetravel;

import com.chronogenesis.essence.Aspect;
import com.chronogenesis.item.EternalHourglassItem;
import com.chronogenesis.player.Powers;
import com.mojang.serialization.MapCodec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

/**
 * An Unmade Nail, on its pillar in the arena Before Time: a nail's head with nothing yet to hold. Used with
 * {@value #COST} drams of Time in the blood, it is driven: time takes hold around it. With all four driven, time has
 * begun in the arena, and the Unbegotten's wounds stay.
 */
public class UnmadeNailBlock extends Block {
    public static final MapCodec<UnmadeNailBlock> CODEC = simpleCodec(UnmadeNailBlock::new);
    public static final BooleanProperty DRIVEN = BooleanProperty.create("driven");
    /** Drams of Time from the player's pool to drive one. */
    public static final int COST = 24;

    public UnmadeNailBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(DRIVEN, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DRIVEN);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(DRIVEN)) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) {
            drive(server, pos, serverPlayer);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /** Drives the nail with the player's Time, if they have enough. Returns whether it was driven. */
    public static boolean drive(ServerLevel level, BlockPos pos, ServerPlayer player) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof UnmadeNailBlock) || state.getValue(DRIVEN)) {
            return false;
        }
        if (!EternalHourglassItem.carried(player)) {
            if (Powers.get(player).pool(Aspect.TIME) < COST) {
                player.displayClientMessage(Component.translatable("message.chronogenesis.nail.no_time", COST)
                        .withStyle(ChatFormatting.GRAY), true);
                return false;
            }
            Powers.spend(player, Aspect.TIME, COST);
        }
        level.setBlockAndUpdate(pos, state.setValue(DRIVEN, true));
        level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1.5f, 0.4f);
        level.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 1.5f, 0.5f);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 60, 0.4, 0.8, 0.4, 0.2);
        // How many of the arena's nails are driven: the arena's middle is the one these nails ring.
        int driven = 0;
        for (BlockPos centre : centresOf(pos)) {
            driven = Math.max(driven, ElderArena.driven(level, centre));
        }
        for (ServerPlayer near : level.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(64))) {
            near.displayClientMessage(Component.translatable(driven >= 4 ? "message.chronogenesis.nail.time_begins"
                    : "message.chronogenesis.nail.driven", driven).withStyle(ChatFormatting.DARK_AQUA), driven < 4);
        }
        return true;
    }

    /** The arena middles this nail could belong to (it stands north, east, south or west of its middle). */
    private static java.util.List<BlockPos> centresOf(BlockPos nail) {
        BlockPos base = nail.below(ElderArena.PILLAR + 1);
        int r = ElderArena.NAIL_RING;
        return java.util.List.of(base.offset(0, 0, r), base.offset(-r, 0, 0), base.offset(0, 0, -r), base.offset(r, 0, 0));
    }
}

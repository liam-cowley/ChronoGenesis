package com.chronogenesis.timetravel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.entity.Unbegotten;
import com.chronogenesis.entity.UnbegottenLimb;
import com.chronogenesis.registry.ModBlocks;
import com.chronogenesis.registry.ModEntities;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Before Time (core-systems.md section 5, the last era): nothing, and an island in it. Whoever arrives finds (or makes,
 * if there is none near) an arena of unmade stone:
 * <ul>
 *   <li>a disc with a dais in its middle, and on the dais the <b>Cradle Stone</b>, with channels of crying obsidian
 *       running from it to four causeways;</li>
 *   <li>at the end of each causeway an outcrop with a pillar, an <b>Unmade Nail</b> on top.</li>
 * </ul>
 * It is empty: nothing waits there until the player plants a Last Offshoot in the Cradle and wakes it. Their own
 * Chronocortex drinks the time of the nails, grows into the Unbegotten and the fight begins ({@link #beginFight}): its
 * body rises at the north rim, four tentacles from the void round the disc, two eyes beside the south outcrop. While
 * it lasts nothing can be built on the arena (anything that was is torn out and dropped), its tether will not pull a
 * traveller home, and the shift does not end. Only one fight at a time in an arena, recorded with it ({@link Arenas}),
 * so arriving again never makes another. When the body dies the arena is empty again.
 */
@EventBusSubscriber(modid = ChronoGenesis.MODID)
public final class ElderArena {
    public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION, ChronoGenesis.id("before_time"));
    /** The island's floor. */
    public static final int FLOOR_Y = 96;
    /** The middle disc. */
    public static final int RADIUS = 13;
    /** The outcrops (with their nails) stand this far out from the middle, each this wide. */
    public static final int NAIL_RING = 24;
    public static final int OUTCROP = 5;
    /** The nails stand on pillars this tall. */
    public static final int PILLAR = 3;
    /** Everything within this many blocks of the middle (and from 12 below to 20 above the floor) is the arena. */
    public static final int BOUNDS = 34;
    /** An arena within this many blocks of an arrival is the one they arrive at. */
    private static final int FIND_RADIUS = 160;

    public enum State {
        /** Waiting for a Last Offshoot. */
        EMPTY,
        /** A woken offshoot is drinking the nails' time. */
        WAKING,
        /** The Unbegotten is alive. */
        FIGHT
    }

    private ElderArena() {}

    // ---------------------------------------------------------------- where things are

    /** The four directions of the causeways and outcrops: north, east, south, west. */
    public static final Direction[] SIDES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    /** Where the four nails stand around an arena's middle (north, east, south, west). */
    public static List<BlockPos> nails(BlockPos centre) {
        List<BlockPos> out = new ArrayList<>();
        for (Direction side : SIDES) {
            out.add(centre.relative(side, NAIL_RING).above(PILLAR + 1));
        }
        return out;
    }

    /** The Cradle Stone: where the Last Offshoot is planted. */
    public static BlockPos cradle(BlockPos centre) {
        return centre.above();
    }

    /** Where the body rises: past the north outcrop's rim. */
    public static Vec3 bodySpot(BlockPos centre) {
        return new Vec3(centre.getX() + 0.5, centre.getY() + 1, centre.getZ() + 0.5 - (NAIL_RING + OUTCROP + 4));
    }

    /**
     * Where each limb rises: four tentacles from the void just off the disc, between the causeways (north-east,
     * south-east, south-west, north-west); then two eyes on stalks beside the south outcrop.
     */
    public static Vec3 limbSpot(BlockPos centre, int slot) {
        Vec3 middle = Vec3.atBottomCenterOf(centre);
        if (slot < UnbegottenLimb.TENTACLES) {
            double angle = Math.toRadians(45 + 90 * slot);
            double r = RADIUS + 2.5;
            return middle.add(Math.sin(angle) * r, -6, -Math.cos(angle) * r);
        }
        double side = slot == UnbegottenLimb.TENTACLES ? -1 : 1;
        return middle.add(side * (OUTCROP + 6), -4, NAIL_RING + 2);
    }

    public static int driven(Level level, BlockPos centre) {
        int count = 0;
        for (BlockPos nail : nails(centre)) {
            BlockState state = level.getBlockState(nail);
            if (state.is(ModBlocks.UNMADE_NAIL.get()) && state.getValue(UnmadeNailBlock.DRIVEN)) {
                count++;
            }
        }
        return count;
    }

    /** Whether a position is inside an arena's bounds. */
    public static boolean within(BlockPos centre, BlockPos pos) {
        int dx = pos.getX() - centre.getX();
        int dz = pos.getZ() - centre.getZ();
        int dy = pos.getY() - centre.getY();
        return dx * dx + dz * dz <= BOUNDS * BOUNDS && dy >= -12 && dy <= 20;
    }

    // ---------------------------------------------------------------- arriving

    /** A traveller has just arrived: onto the nearest arena (made if need be), at the south end of the disc. */
    public static void arrive(ServerPlayer player, ServerLevel level) {
        Arenas arenas = arenas(level);
        Arena arena = arenas.near(player.blockPosition(), FIND_RADIUS);
        if (arena == null) {
            BlockPos centre = new BlockPos(player.getBlockX(), FLOOR_Y, player.getBlockZ());
            build(level, centre);
            arena = arenas.add(centre);
        } else if (arena.state == State.EMPTY && !level.getBlockState(cradle(arena.centre)).is(ModBlocks.CRADLE_STONE.get())) {
            // An arena from before the Cradle: made again, the new way.
            clear(level, arena.centre);
            build(level, arena.centre);
        }
        BlockPos centre = arena.centre;
        double x = centre.getX() + 0.5;
        double z = centre.getZ() + RADIUS - 2.5;
        if (player instanceof FakePlayer) {
            player.moveTo(x, centre.getY() + 1, z, 180, 0);
        } else {
            player.teleportTo(level, x, centre.getY() + 1, z, 180, 0);
        }
        level.playSound(null, centre, SoundEvents.WARDEN_EMERGE, SoundSource.HOSTILE, 2.0f, 0.4f);
        player.displayClientMessage(Component.translatable(arena.state == State.FIGHT ? "message.chronogenesis.before_time.waiting"
                : "message.chronogenesis.before_time.arrived").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC), false);
    }

    // ---------------------------------------------------------------- the island

    /**
     * The island, block by block (always the same for the same middle): a disc of blackstone ringed in polished
     * bricks, veined with crying obsidian and lit from below, crumbling at its rim and tapering underneath; a dais with
     * the Cradle Stone; channels from it to four causeways; four outcrops, each with a pillar of obelisk stone and an
     * Unmade Nail.
     */
    public static void layout(BlockPos centre, BiConsumer<BlockPos, BlockState> put) {
        RandomSource random = RandomSource.create(centre.asLong() * 31 + 1031);
        BlockState blackstone = Blocks.BLACKSTONE.defaultBlockState();
        BlockState bricks = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        BlockState polished = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
        BlockState crying = Blocks.CRYING_OBSIDIAN.defaultBlockState();
        disc(centre, RADIUS, random, put, (dx, dz, d) -> d < 4 ? bricks
                : (int) d == RADIUS - 1 ? polished
                : (dx == 0 || dz == 0) && d > 3 ? crying
                : (dx * 7 + dz * 13) % 29 == 0 ? Blocks.SEA_LANTERN.defaultBlockState()
                : random.nextFloat() < 0.05f ? crying : blackstone);
        for (Direction side : SIDES) {
            // The causeway: three wide, bricks at its edges, a channel of crying obsidian down its middle.
            for (int t = RADIUS - 1; t <= NAIL_RING - OUTCROP + 1; t++) {
                for (int w = -1; w <= 1; w++) {
                    BlockPos at = centre.relative(side, t).relative(side.getClockWise(), w);
                    put.accept(at, w == 0 ? crying : bricks);
                    put.accept(at.below(), blackstone);
                    if (w == 0 || t % 3 == 0) {
                        put.accept(at.below(2), blackstone);
                    }
                }
            }
            BlockPos outcrop = centre.relative(side, NAIL_RING);
            disc(outcrop, OUTCROP, random, put, (dx, dz, d) -> d < 1.5 ? polished : d > OUTCROP - 1 ? bricks
                    : random.nextFloat() < 0.08f ? crying : blackstone);
            for (int y = 1; y <= PILLAR; y++) {
                put.accept(outcrop.above(y), (y == 2 ? ModBlocks.OBELISK_INSCRIPTION : ModBlocks.OBELISK_STONE).get().defaultBlockState());
            }
            put.accept(outcrop.above(PILLAR + 1), ModBlocks.UNMADE_NAIL.get().defaultBlockState());
        }
        // The dais: a step of slabs round a 3x3 top, the Cradle Stone in its middle.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                boolean rim = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                put.accept(centre.offset(dx, 1, dz), rim
                        ? Blocks.POLISHED_BLACKSTONE_BRICK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM)
                        : dx == 0 && dz == 0 ? ModBlocks.CRADLE_STONE.get().defaultBlockState() : crying);
            }
        }
    }

    private interface Surface {
        BlockState at(int dx, int dz, double d);
    }

    private static void disc(BlockPos middle, int radius, RandomSource random, BiConsumer<BlockPos, BlockState> put, Surface surface) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                boolean crumbled = d > radius - 1.5 && random.nextFloat() < 0.3f;
                int depth = (int) Math.max(1, (radius - d) / 2.0) + random.nextInt(2);
                if (d > radius + 0.5 || crumbled) {
                    continue;
                }
                put.accept(middle.offset(dx, 0, dz), surface.at(dx, dz, d));
                // Underneath it tapers away into nothing.
                for (int y = 1; y <= depth; y++) {
                    put.accept(middle.offset(dx, -y, dz), Blocks.BLACKSTONE.defaultBlockState());
                }
            }
        }
    }

    public static void build(ServerLevel level, BlockPos centre) {
        layout(centre, (pos, state) -> level.setBlock(pos, state, 2));
    }

    /** Empties an arena's bounds (an old arena, made before the Cradle). */
    private static void clear(ServerLevel level, BlockPos centre) {
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-BOUNDS, -12, -BOUNDS), centre.offset(BOUNDS, 20, BOUNDS))) {
            if (!level.getBlockState(pos).isAir()) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            }
        }
    }

    /**
     * The fight begins: everything built on the arena is torn out and dropped, anything broken out of it grows back,
     * and the nails are undriven again (time has not begun).
     */
    public static void mend(ServerLevel level, BlockPos centre) {
        Map<BlockPos, BlockState> island = new HashMap<>();
        layout(centre, (pos, state) -> island.put(pos.immutable(), state));
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-BOUNDS, -12, -BOUNDS), centre.offset(BOUNDS, 20, BOUNDS))) {
            BlockState now = level.getBlockState(pos);
            BlockState was = island.get(pos);
            if (was == null) {
                if (!now.isAir() && !now.is(ModBlocks.UNBEGOTTEN_FLESH.get())) {
                    level.destroyBlock(pos.immutable(), true);
                }
            } else if (now != was) {
                if (!now.isAir() && !now.is(was.getBlock())) {
                    level.destroyBlock(pos.immutable(), true);
                }
                level.setBlock(pos.immutable(), was, 2);
            }
        }
    }

    // ---------------------------------------------------------------- the fight

    /**
     * The woken Chronocortex has drunk the nails and burst out of its jar: the Unbegotten rises, body and limbs, named
     * as the cortex was.
     */
    public static void beginFight(ServerLevel level, BlockPos centre, @Nullable String name) {
        Arena arena = arenas(level).at(centre);
        if (arena == null || arena.state == State.FIGHT) {
            return;
        }
        mend(level, centre);
        Unbegotten body = ModEntities.UNBEGOTTEN.get().create(level);
        if (body == null) {
            return;
        }
        Vec3 at = bodySpot(centre);
        body.moveTo(at.x, at.y, at.z, 0, 0);
        body.setHome(centre);
        body.setCortexName(name);
        body.finalizeSpawn(level, level.getCurrentDifficultyAt(centre), MobSpawnType.EVENT, null);
        level.addFreshEntity(body);
        for (int slot = 0; slot < UnbegottenLimb.SLOTS; slot++) {
            body.growLimb(level, slot);
        }
        arena.state = State.FIGHT;
        arena.boss = body.getUUID();
        arenas(level).setDirty();
        level.playSound(null, centre, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 3.0f, 0.5f);
        for (ServerPlayer player : players(level, centre)) {
            player.displayClientMessage(Component.translatable("message.chronogenesis.unbegotten.rises", body.getDisplayName())
                    .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD), false);
        }
    }

    /** The body is dead: the arena is empty again (and ready for another offshoot), the flesh it laid gone. */
    public static void endFight(ServerLevel level, BlockPos centre) {
        Arena arena = arenas(level).at(centre);
        if (arena != null) {
            arena.state = State.EMPTY;
            arena.boss = null;
            arena.defeats++;
            arenas(level).setDirty();
        }
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-BOUNDS, -2, -BOUNDS), centre.offset(BOUNDS, 6, BOUNDS))) {
            if (level.getBlockState(pos).is(ModBlocks.UNBEGOTTEN_FLESH.get())) {
                level.setBlock(pos.immutable(), Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    public static List<ServerPlayer> players(ServerLevel level, BlockPos centre) {
        return level.getEntitiesOfClass(ServerPlayer.class, new AABB(centre).inflate(BOUNDS + 16, 48, BOUNDS + 16),
                player -> player.isAlive() && !player.isSpectator());
    }

    /** The arena a position is in (in Before Time), or null. */
    @Nullable
    public static Arena arenaAt(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server) || server.dimension() != DIMENSION) {
            return null;
        }
        Arena arena = arenas(server).near(pos, BOUNDS + 8);
        return arena != null && within(arena.centre, pos) ? arena : null;
    }

    /** Whether the Unbegotten holds this player here: they are in an arena while it fights. No tether pulls them home. */
    public static boolean binds(Player player) {
        Arena arena = arenaAt(player.level(), player.blockPosition());
        return arena != null && arena.state == State.FIGHT;
    }

    /** Whether nothing may be built (or broken, or poured) at a position: an arena while the Cradle wakes or it fights. */
    private static boolean sealed(Level level, BlockPos pos, @Nullable Player player) {
        if (player != null && player.isCreative()) {
            return false;
        }
        Arena arena = arenaAt(level, pos);
        if (arena == null || arena.state == State.EMPTY) {
            return false;
        }
        if (player != null) {
            player.displayClientMessage(Component.translatable("message.chronogenesis.before_time.sealed").withStyle(ChatFormatting.DARK_PURPLE), true);
        }
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.02);
        }
        return true;
    }

    @SubscribeEvent
    static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof Level level && sealed(level, event.getPos(), event.getEntity() instanceof Player p ? p : null)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof Level level && sealed(level, event.getPos(), event.getPlayer())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getItemStack().getItem() instanceof BucketItem && sealed(event.getLevel(),
                event.getPos().relative(event.getFace() == null ? Direction.UP : event.getFace()), event.getEntity())) {
            event.setCanceled(true);
        }
    }

    /** A fight whose body is gone (killed by a command, lost with a broken save) is over. */
    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != DIMENSION || level.getGameTime() % 100 != 0) {
            return;
        }
        Arenas arenas = arenas(level);
        for (Arena arena : arenas.arenas) {
            if (arena.state == State.FIGHT && level.isPositionEntityTicking(arena.centre)) {
                boolean alive = arena.boss != null && level.getEntity(arena.boss) instanceof Unbegotten body && body.isAlive();
                arena.missing = alive ? 0 : arena.missing + 1;
                if (arena.missing >= 3) {
                    arena.state = State.EMPTY;
                    arena.boss = null;
                    arena.missing = 0;
                    arenas.setDirty();
                }
            }
        }
    }

    // ---------------------------------------------------------------- where the arenas are

    public static Arenas arenas(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(Arenas::new, Arenas::load), "chronogenesis_elder_arenas");
    }

    /** One arena: its middle, whether it is waking or fighting (and the body that fights), how often it has been won. */
    public static class Arena {
        public final BlockPos centre;
        public State state = State.EMPTY;
        @Nullable
        public UUID boss;
        public int defeats;
        int missing;

        Arena(BlockPos centre) {
            this.centre = centre;
        }
    }

    /** The arenas made so far in a level. */
    public static class Arenas extends SavedData {
        private final List<Arena> arenas = new ArrayList<>();

        static Arenas load(CompoundTag tag, HolderLookup.Provider registries) {
            Arenas arenas = new Arenas();
            for (Tag entry : tag.getList("arenas", Tag.TAG_INT_ARRAY)) {
                // Saved before arenas had a state: a bare position.
                int[] xyz = ((IntArrayTag) entry).getAsIntArray();
                if (xyz.length == 3) {
                    arenas.arenas.add(new Arena(new BlockPos(xyz[0], xyz[1], xyz[2])));
                }
            }
            for (Tag entry : tag.getList("fights", Tag.TAG_COMPOUND)) {
                CompoundTag saved = (CompoundTag) entry;
                Arena arena = new Arena(NbtUtils.readBlockPos(saved, "centre").orElse(BlockPos.ZERO));
                try {
                    arena.state = State.valueOf(saved.getString("state"));
                } catch (IllegalArgumentException e) {
                    arena.state = State.EMPTY;
                }
                // A waking offshoot does not survive a restart: it begins again when used.
                if (arena.state == State.WAKING) {
                    arena.state = State.EMPTY;
                }
                arena.boss = saved.hasUUID("boss") ? saved.getUUID("boss") : null;
                arena.defeats = saved.getInt("defeats");
                arenas.arenas.add(arena);
            }
            return arenas;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            ListTag list = new ListTag();
            for (Arena arena : arenas) {
                CompoundTag saved = new CompoundTag();
                saved.put("centre", NbtUtils.writeBlockPos(arena.centre));
                saved.putString("state", arena.state.name());
                if (arena.boss != null) {
                    saved.putUUID("boss", arena.boss);
                }
                saved.putInt("defeats", arena.defeats);
                list.add(saved);
            }
            tag.put("fights", list);
            return tag;
        }

        @Nullable
        public Arena near(BlockPos pos, int radius) {
            Arena best = null;
            double bestDistance = (double) radius * radius;
            for (Arena arena : arenas) {
                double d = arena.centre.distToCenterSqr(pos.getX(), arena.centre.getY(), pos.getZ());
                if (d <= bestDistance) {
                    best = arena;
                    bestDistance = d;
                }
            }
            return best;
        }

        @Nullable
        public Arena at(BlockPos centre) {
            for (Arena arena : arenas) {
                if (arena.centre.equals(centre)) {
                    return arena;
                }
            }
            return null;
        }

        public Arena add(BlockPos centre) {
            Arena arena = new Arena(centre.immutable());
            arenas.add(arena);
            setDirty();
            return arena;
        }
    }
}

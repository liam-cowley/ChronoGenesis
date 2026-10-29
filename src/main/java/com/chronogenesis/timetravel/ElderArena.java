package com.chronogenesis.timetravel;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.entity.Unbegotten;
import com.chronogenesis.registry.ModBlocks;
import com.chronogenesis.registry.ModEntities;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;

/**
 * Before Time (core-systems.md section 5, the last era): nothing, and in it the Unbegotten. Whoever arrives finds (or
 * makes, if there is none near) an island of unmade stone with four <b>Unmade Nails</b> on pillars round it, and the
 * Unbegotten over its middle. Wounds do not stay on it while time has not begun: drive all four nails (with Time from
 * the blood) and time begins here, and it can die. Or hold it in slowed or stopped time, where it cannot rewind.
 * The first nails ever driven are these: the obelisks of every later age.
 */
public final class ElderArena {
    public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION, ChronoGenesis.id("before_time"));
    /** The island's floor. */
    public static final int FLOOR_Y = 96;
    public static final int RADIUS = 22;
    /** The nails stand this far out from the middle, on pillars this tall. */
    public static final int NAIL_RING = 15;
    public static final int PILLAR = 6;
    /** An arena within this many blocks of an arrival is the one they arrive at. */
    private static final int FIND_RADIUS = 160;

    private ElderArena() {}

    /** Where the four nails stand around an arena's middle. */
    public static List<BlockPos> nails(BlockPos centre) {
        List<BlockPos> out = new ArrayList<>();
        for (int[] d : new int[][] {{0, -1}, {1, 0}, {0, 1}, {-1, 0}}) {
            out.add(centre.offset(d[0] * NAIL_RING, PILLAR + 1, d[1] * NAIL_RING));
        }
        return out;
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

    /** A traveller has just arrived: onto the nearest arena (made if need be), with the Unbegotten waiting over it. */
    public static void arrive(ServerPlayer player, ServerLevel level) {
        Arenas arenas = arenas(level);
        BlockPos centre = arenas.near(player.blockPosition(), FIND_RADIUS);
        if (centre == null) {
            centre = new BlockPos(player.getBlockX(), FLOOR_Y, player.getBlockZ());
            build(level, centre, level.getRandom());
            arenas.add(centre);
        }
        if (driven(level, centre) < 4 && level.getEntitiesOfClass(Unbegotten.class, new AABB(centre).inflate(64)).isEmpty()) {
            Unbegotten god = ModEntities.UNBEGOTTEN.get().create(level);
            if (god != null) {
                god.moveTo(centre.getX() + 0.5, centre.getY() + 10, centre.getZ() + 0.5, 0, 0);
                god.setHome(centre);
                god.finalizeSpawn(level, level.getCurrentDifficultyAt(centre), MobSpawnType.EVENT, null);
                level.addFreshEntity(god);
            }
        }
        double x = centre.getX() + 0.5;
        double z = centre.getZ() + RADIUS - 3.5;
        if (player instanceof FakePlayer) {
            player.moveTo(x, centre.getY() + 1, z, 180, 0);
        } else {
            player.teleportTo(level, x, centre.getY() + 1, z, 180, 0);
        }
        level.playSound(null, centre, SoundEvents.WARDEN_EMERGE, SoundSource.HOSTILE, 2.0f, 0.4f);
        player.displayClientMessage(Component.translatable("message.chronogenesis.before_time.arrived")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC), false);
    }

    /**
     * The island: a disc of blackstone ringed in polished bricks, veined with crying obsidian and lit from below with
     * sea lanterns, crumbling at its rim and tapering underneath; a dais in the middle; four pillars of obelisk stone,
     * each with an Unmade Nail on top.
     */
    public static void build(ServerLevel level, BlockPos centre, RandomSource random) {
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > RADIUS + 0.5 || (d > RADIUS - 2 && random.nextFloat() < 0.35f)) {
                    continue;
                }
                BlockState top = d < 4 ? Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                        : ((int) d) % 6 == 0 ? Blocks.POLISHED_BLACKSTONE.defaultBlockState()
                        : random.nextFloat() < 0.06f ? Blocks.CRYING_OBSIDIAN.defaultBlockState()
                        : (dx * 7 + dz * 13) % 29 == 0 ? Blocks.SEA_LANTERN.defaultBlockState()
                        : Blocks.BLACKSTONE.defaultBlockState();
                level.setBlock(centre.offset(dx, 0, dz), top, 2);
                // Underneath it tapers away into nothing.
                int depth = (int) Math.max(1, (RADIUS - d) / 2.5) + random.nextInt(2);
                for (int y = 1; y <= depth; y++) {
                    level.setBlock(centre.offset(dx, -y, dz), Blocks.BLACKSTONE.defaultBlockState(), 2);
                }
            }
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                level.setBlock(centre.offset(dx, 1, dz), Math.abs(dx) == 2 || Math.abs(dz) == 2
                        ? Blocks.POLISHED_BLACKSTONE_BRICK_SLAB.defaultBlockState() : Blocks.CRYING_OBSIDIAN.defaultBlockState(), 2);
            }
        }
        for (BlockPos nail : nails(centre)) {
            for (int y = 1; y <= PILLAR; y++) {
                level.setBlock(nail.below(PILLAR + 1 - y), (y % 3 == 0 ? ModBlocks.OBELISK_INSCRIPTION : ModBlocks.OBELISK_STONE)
                        .get().defaultBlockState(), 2);
            }
            level.setBlock(nail, ModBlocks.UNMADE_NAIL.get().defaultBlockState(), 3);
        }
    }

    // ---------------------------------------------------------------- where the arenas are

    public static Arenas arenas(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(Arenas::new, Arenas::load), "chronogenesis_elder_arenas");
    }

    /** The middles of the arenas made so far in a level. */
    public static class Arenas extends SavedData {
        private final List<BlockPos> centres = new ArrayList<>();

        static Arenas load(CompoundTag tag, HolderLookup.Provider registries) {
            Arenas arenas = new Arenas();
            ListTag list = tag.getList("arenas", Tag.TAG_INT_ARRAY);
            for (Tag entry : list) {
                int[] xyz = ((IntArrayTag) entry).getAsIntArray();
                if (xyz.length == 3) {
                    arenas.centres.add(new BlockPos(xyz[0], xyz[1], xyz[2]));
                }
            }
            return arenas;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            ListTag list = new ListTag();
            for (BlockPos pos : centres) {
                list.add(NbtUtils.writeBlockPos(pos));
            }
            tag.put("arenas", list);
            return tag;
        }

        @Nullable
        BlockPos near(BlockPos pos, int radius) {
            BlockPos best = null;
            double bestDistance = (double) radius * radius;
            for (BlockPos centre : centres) {
                double d = centre.distToCenterSqr(pos.getX(), centre.getY(), pos.getZ());
                if (d <= bestDistance) {
                    best = centre;
                    bestDistance = d;
                }
            }
            return best;
        }

        void add(BlockPos centre) {
            centres.add(centre);
            setDirty();
        }
    }
}

package com.chronogenesis.time;

import com.chronogenesis.Config;
import com.chronogenesis.registry.ModAttachments;
import com.chronogenesis.registry.ModBlocks;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Temporal Stress: how strained time is in a chunk (core-systems.md section 4).
 * Extracting Time raises it; it fades on its own. Above the threshold, Time Tears open.
 * Stored as a chunk attachment with the game time of the last update, and decayed lazily.
 */
public record TemporalStress(int stress, long lastUpdate) {
    public static final Codec<TemporalStress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("stress").forGetter(TemporalStress::stress),
            Codec.LONG.fieldOf("last_update").forGetter(TemporalStress::lastUpdate)
    ).apply(instance, TemporalStress::new));

    public static final TemporalStress NONE = new TemporalStress(0, 0L);

    /** Stress added per dram of Time pulled out of a creature. */
    public static final int PER_DRAM_EXTRACTED = 4;

    /** Current stress after decay since the last update. */
    public int current(long gameTime) {
        int decayTicks = Math.max(1, Config.STRESS_DECAY_TICKS.getAsInt());
        long decayed = (gameTime - lastUpdate) / decayTicks;
        return (int) Math.max(0, stress - decayed);
    }

    public static int get(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        return chunk.getData(ModAttachments.TEMPORAL_STRESS).current(level.getGameTime());
    }

    /** Adds stress to the chunk at {@code pos}, opening Time Tears if it is pushed too far. */
    public static void add(ServerLevel level, BlockPos pos, int amount) {
        LevelChunk chunk = level.getChunkAt(pos);
        long now = level.getGameTime();
        int value = chunk.getData(ModAttachments.TEMPORAL_STRESS).current(now) + amount;
        int threshold = Config.TEAR_STRESS_THRESHOLD.getAsInt();

        if (value >= threshold && tryOpenTear(level, pos, level.getRandom())) {
            value -= threshold / 2; // a tear relieves some of the pressure
        } else if (value >= threshold * 3 / 4) {
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.AMBIENT, 1.0f, 0.4f);
        }
        chunk.setData(ModAttachments.TEMPORAL_STRESS, new TemporalStress(value, now));
        chunk.setUnsaved(true);
    }

    /** Opens a Time Tear in an empty spot near {@code pos}. */
    private static boolean tryOpenTear(ServerLevel level, BlockPos pos, RandomSource random) {
        for (int attempt = 0; attempt < 12; attempt++) {
            BlockPos candidate = pos.offset(random.nextInt(7) - 3, random.nextInt(3), random.nextInt(7) - 3);
            if (level.isEmptyBlock(candidate)) {
                level.setBlockAndUpdate(candidate, ModBlocks.TIME_TEAR.get().defaultBlockState());
                level.playSound(null, candidate, SoundEvents.PORTAL_TRIGGER, SoundSource.BLOCKS, 0.6f, 1.6f);
                return true;
            }
        }
        return false;
    }
}

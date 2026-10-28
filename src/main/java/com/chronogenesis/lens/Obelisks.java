package com.chronogenesis.lens;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

/**
 * Obelisks (core-systems.md section 7): ancient stones almost untouched by time, nails holding it in place.
 * Through the Temporal Lens the past view bends around one like light around a black hole: inside its event
 * horizon it goes dark, and at the edge fragments of a much older inscription can be read.
 * Used on both sides: the server for the past view and clues, the client for the distortion.
 */
public final class Obelisks {
    /** The event horizon, in blocks from the obelisk's heart. Nothing of the past survives inside it. */
    public static final double HORIZON = 4.5;
    /** Standing this far out (from the horizon) the older inscription can be read. */
    public static final double EDGE_WIDTH = 3.0;
    /** The capstone sits this far above the obelisk's heart (the middle of the shaft). */
    public static final int HEART_BELOW_CAPSTONE = 5;

    private Obelisks() {}

    /** The obelisk's heart: the middle of its shaft, where the horizon is centred. */
    public static Vec3 heart(BlockPos capstone) {
        return Vec3.atCenterOf(capstone.below(HEART_BELOW_CAPSTONE));
    }

    /** Capstones of obelisks within {@code radius} blocks of {@code pos}, from loaded chunks' block entities. */
    public static List<BlockPos> near(Level level, Vec3 pos, double radius) {
        List<BlockPos> found = new ArrayList<>();
        int r = (int) Math.ceil(radius / 16.0);
        int cx = ((int) Math.floor(pos.x)) >> 4;
        int cz = ((int) Math.floor(pos.z)) >> 4;
        for (int x = cx - r; x <= cx + r; x++) {
            for (int z = cz - r; z <= cz + r; z++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(x, z);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (blockEntity instanceof ObeliskBlockEntity && heart(blockEntity.getBlockPos()).distanceTo(pos) <= radius) {
                        found.add(blockEntity.getBlockPos());
                    }
                }
            }
        }
        return found;
    }

    /** Whether a point lies inside some obelisk's event horizon. */
    public static boolean swallowed(List<BlockPos> capstones, Vec3 point) {
        for (BlockPos capstone : capstones) {
            if (heart(capstone).distanceTo(point) < HORIZON) {
                return true;
            }
        }
        return false;
    }
}

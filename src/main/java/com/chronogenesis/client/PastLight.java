package com.chronogenesis.client;

import java.util.ArrayDeque;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Light inside one of the past's areas, worked out for the past's blocks rather than the present's: rubble the
 * Lens has removed casts no shadow, a roof that only stood in the past does, and the past's lanterns glow.
 * The same rules as the game's light engine, simplified: sky light falls straight down undimmed through
 * clear blocks and otherwise loses at least one level per block; block light spreads from emitting blocks.
 * At the area's edges, light flows in from the real world around it, so the two meet without a seam.
 */
public final class PastLight {
    private final BoundingBox box;
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;
    private final byte[] sky;
    private final byte[] block;

    private PastLight(BoundingBox box) {
        this.box = box;
        this.sizeX = box.getXSpan();
        this.sizeY = box.getYSpan();
        this.sizeZ = box.getZSpan();
        this.sky = new byte[sizeX * sizeY * sizeZ];
        this.block = new byte[sizeX * sizeY * sizeZ];
    }

    /** Light just outside an area: where the past's light comes in from. In game, the real level's light. */
    @FunctionalInterface
    public interface Outside {
        int get(LightLayer layer, BlockPos pos);
    }

    /** Light for {@code box} as the past world has it; light from outside the box comes from {@code outside}. */
    public static PastLight compute(BlockGetter past, Outside outside, BoundingBox box) {
        PastLight light = new PastLight(box);
        light.propagate(past, outside, LightLayer.SKY);
        light.propagate(past, outside, LightLayer.BLOCK);
        return light;
    }

    public boolean contains(BlockPos pos) {
        return box.isInside(pos);
    }

    /** Light of the given layer at a position inside the box. */
    public int get(LightLayer layer, BlockPos pos) {
        int i = index(pos.getX() - box.minX(), pos.getY() - box.minY(), pos.getZ() - box.minZ());
        return (layer == LightLayer.SKY ? sky : block)[i];
    }

    private int index(int x, int y, int z) {
        return (y * sizeZ + z) * sizeX + x;
    }

    private void propagate(BlockGetter past, Outside outsideLight, LightLayer layer) {
        byte[] values = layer == LightLayer.SKY ? sky : block;
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        BlockPos.MutableBlockPos outside = new BlockPos.MutableBlockPos();

        for (BlockPos cell : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            BlockState state = past.getBlockState(cell);
            int opacity = state.getLightBlock(past, cell);
            int best = layer == LightLayer.BLOCK ? state.getLightEmission(past, cell) : 0;
            // Light flowing in across the area's boundary, from the real world outside it.
            for (Direction direction : Direction.values()) {
                outside.setWithOffset(cell, direction);
                if (box.isInside(outside)) {
                    continue;
                }
                int from = outsideLight.get(layer, outside);
                best = Math.max(best, arriving(layer, from, direction.getOpposite(), opacity));
            }
            if (best > 0) {
                values[index(cell.getX() - box.minX(), cell.getY() - box.minY(), cell.getZ() - box.minZ())] = (byte) best;
                queue.add(cell.immutable());
            }
        }

        BlockPos.MutableBlockPos next = new BlockPos.MutableBlockPos();
        while (!queue.isEmpty()) {
            BlockPos cell = queue.poll();
            int value = values[index(cell.getX() - box.minX(), cell.getY() - box.minY(), cell.getZ() - box.minZ())];
            for (Direction direction : Direction.values()) {
                next.setWithOffset(cell, direction);
                if (!box.isInside(next)) {
                    continue;
                }
                int opacity = past.getBlockState(next).getLightBlock(past, next);
                int lit = arriving(layer, value, direction, opacity);
                int i = index(next.getX() - box.minX(), next.getY() - box.minY(), next.getZ() - box.minZ());
                if (lit > values[i]) {
                    values[i] = (byte) lit;
                    queue.add(next.immutable());
                }
            }
        }
    }

    /**
     * The light that reaches a block from a neighbour with light {@code from}, travelling in {@code travel}.
     * Full sky light keeps going straight down through clear blocks; everything else loses at least one level,
     * more through blocks that dim light, and nothing gets into a block that stops it entirely.
     */
    private static int arriving(LightLayer layer, int from, Direction travel, int opacity) {
        if (opacity >= 15) {
            return 0;
        }
        if (layer == LightLayer.SKY && travel == Direction.DOWN && from == 15 && opacity == 0) {
            return 15;
        }
        return Math.max(0, from - Math.max(1, opacity));
    }
}

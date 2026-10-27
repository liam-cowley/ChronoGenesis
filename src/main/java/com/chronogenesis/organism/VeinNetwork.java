package com.chronogenesis.organism;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.essence.Aspect;
import com.chronogenesis.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A breadth-first walk through the open veins starting in front of a graft.
 * Finds the grafts plugged into the network (their front faces a vein), the nearest bare conduit
 * the veins could grow into next, and shortest paths (used by the Racetrack).
 */
public final class VeinNetwork {
    public static final int MAX_BLOCKS = 512;

    private final Map<BlockPos, BlockPos> parent = new HashMap<>();
    private final List<BlockPos> grafts = new ArrayList<>();
    @Nullable
    private BlockPos growthTarget;

    private VeinNetwork() {}

    /** Walks from {@code start} (the block in front of a graft). */
    public static VeinNetwork walk(Level level, BlockPos start, BlockPos origin) {
        VeinNetwork network = new VeinNetwork();
        BlockState startState = level.getBlockState(start);
        if (!startState.is(ModBlocks.BRASS_CONDUIT.get())) {
            return network;
        }
        if (!startState.getValue(BrassConduitBlock.VEIN)) {
            network.growthTarget = start;
            return network;
        }
        if (startState.getValue(BrassConduitBlock.CLOGGED)) {
            return network;
        }
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        network.parent.put(start, start);
        while (!queue.isEmpty() && network.parent.size() < MAX_BLOCKS) {
            BlockPos pos = queue.poll();
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (network.parent.containsKey(next) || next.equals(origin) || !level.isLoaded(next)) {
                    continue;
                }
                BlockState state = level.getBlockState(next);
                if (BrassConduitBlock.isOpenVein(state)) {
                    network.parent.put(next, pos);
                    queue.add(next);
                } else if (state.is(ModBlocks.BRASS_CONDUIT.get()) && !state.getValue(BrassConduitBlock.VEIN)
                        && network.growthTarget == null) {
                    network.growthTarget = next;
                } else if (state.getBlock() instanceof GraftBlock
                        && next.relative(state.getValue(GraftBlock.FACING)).equals(pos)
                        && !network.grafts.contains(next)) {
                    network.grafts.add(next);
                }
            }
        }
        return network;
    }

    /** Grafts (other than the origin) whose front faces a vein in this network, nearest first. */
    public List<BlockPos> grafts() {
        return grafts;
    }

    /** The nearest bare conduit touching the network, which the veins can grow into next. */
    @Nullable
    public BlockPos growthTarget() {
        return growthTarget;
    }

    /** Vein positions from the start to the vein in front of {@code graft}, inclusive. Empty if unreachable. */
    public List<BlockPos> pathTo(Level level, BlockPos graft) {
        BlockState state = level.getBlockState(graft);
        if (!(state.getBlock() instanceof GraftBlock)) {
            return List.of();
        }
        BlockPos end = graft.relative(state.getValue(GraftBlock.FACING));
        if (!parent.containsKey(end)) {
            return List.of();
        }
        List<BlockPos> path = new ArrayList<>();
        BlockPos pos = end;
        while (true) {
            path.add(0, pos);
            BlockPos previous = parent.get(pos);
            if (previous.equals(pos)) {
                break;
            }
            pos = previous;
        }
        return path;
    }

    /** A vein is "heavy" when it runs alongside Density flesh: pulses crawl through it at half pace. */
    public static boolean isHeavy(Level level, BlockPos vein) {
        for (Direction direction : Direction.values()) {
            BlockState state = level.getBlockState(vein.relative(direction));
            if (state.is(ModBlocks.CHRONOFLESH.get()) && state.getValue(FleshBlock.ASPECT) == Aspect.DENSITY) {
                return true;
            }
        }
        return false;
    }
}

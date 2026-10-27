package com.chronogenesis.organism;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.essence.Aspect;
import com.chronogenesis.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Walks the connected flesh of an organism (breadth-first, bounded).
 * Organisms keep no list of their blocks: the living tissue <em>is</em> the data, so breaking
 * flesh or a frame block is always reflected on the next scan.
 */
public final class OrganismScan {
    private OrganismScan() {}

    /** The result of scanning from a core: its flesh and the frame blocks it can grow into next. */
    public record Body(List<BlockPos> flesh, List<BlockPos> frontier) {}

    public static boolean isTissue(BlockState state, Aspect aspect) {
        return (state.is(ModBlocks.CHRONOFLESH.get()) || state.is(ModBlocks.ORGANISM_CORE.get()))
                && state.getValue(FleshBlock.ASPECT) == aspect;
    }

    /** Scans the organism around its core. Frontier is sorted nearest-first; flesh farthest-last. */
    public static Body scan(Level level, BlockPos core, Aspect aspect, int limit) {
        List<BlockPos> flesh = new ArrayList<>();
        Set<BlockPos> frontier = new HashSet<>();
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(core);
        visited.add(core);

        while (!queue.isEmpty() && flesh.size() < limit) {
            BlockPos pos = queue.poll();
            if (!pos.equals(core)) {
                flesh.add(pos);
            }
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (!visited.add(next) || !level.isLoaded(next)) {
                    continue;
                }
                BlockState state = level.getBlockState(next);
                if (state.is(ModBlocks.CHRONOFLESH.get()) && state.getValue(FleshBlock.ASPECT) == aspect) {
                    queue.add(next);
                } else if (state.is(ModBlocks.COPPER_ARMATURE.get())) {
                    frontier.add(next);
                }
            }
        }
        List<BlockPos> sortedFrontier = new ArrayList<>(frontier);
        sortedFrontier.sort(Comparator.comparingDouble(p -> p.distSqr(core)));
        return new Body(flesh, sortedFrontier);
    }

    /** Finds the core of the organism that the tissue at {@code start} belongs to. */
    @Nullable
    public static OrganismCoreBlockEntity findCore(Level level, BlockPos start, int limit) {
        BlockState startState = level.getBlockState(start);
        if (!startState.hasProperty(FleshBlock.ASPECT)) {
            return null;
        }
        if (level.getBlockEntity(start) instanceof OrganismCoreBlockEntity core) {
            return core;
        }
        Aspect aspect = startState.getValue(FleshBlock.ASPECT);
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        visited.add(start);
        while (!queue.isEmpty() && visited.size() < limit * 6) {
            BlockPos pos = queue.poll();
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (!visited.add(next) || !level.isLoaded(next)) {
                    continue;
                }
                BlockState state = level.getBlockState(next);
                if (!isTissue(state, aspect)) {
                    continue;
                }
                if (level.getBlockEntity(next) instanceof OrganismCoreBlockEntity core) {
                    return core;
                }
                queue.add(next);
            }
        }
        return null;
    }
}

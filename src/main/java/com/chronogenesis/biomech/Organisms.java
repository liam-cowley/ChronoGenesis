package com.chronogenesis.biomech;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.essence.Aspect;
import com.chronogenesis.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/** Walking an organism's body: the core plus every connected flesh block of its aspect. */
public final class Organisms {
    /** Hard limit on how far a search walks, so a huge organism cannot stall the server. */
    public static final int MAX_BODY = 1024;

    public static boolean isBody(BlockState state) {
        return state.is(ModBlocks.ORGANISM_CORE.get()) || state.is(ModBlocks.ORGANISM_FLESH.get());
    }

    /**
     * @param flesh    flesh blocks in breadth-first order from the core (the last ones are the farthest)
     * @param frontier empty Copper Armature next to the body: where it can grow next
     */
    public record Body(List<BlockPos> flesh, List<BlockPos> frontier) {}

    public static Body scan(BlockGetter level, BlockPos core, Aspect aspect) {
        List<BlockPos> flesh = new ArrayList<>();
        List<BlockPos> frontier = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(core);
        seen.add(core);
        while (!queue.isEmpty() && seen.size() < MAX_BODY) {
            BlockPos pos = queue.poll();
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (!seen.add(next)) {
                    continue;
                }
                BlockState state = level.getBlockState(next);
                if (state.is(ModBlocks.ORGANISM_FLESH.get()) && state.getValue(OrganismFleshBlock.ASPECT) == aspect) {
                    flesh.add(next);
                    queue.add(next);
                } else if (state.is(ModBlocks.COPPER_ARMATURE.get())) {
                    frontier.add(next);
                }
            }
        }
        return new Body(flesh, frontier);
    }

    /** The core of the organism that the body block at {@code start} belongs to, if any. */
    @Nullable
    public static OrganismCoreBlockEntity findCore(BlockGetter level, BlockPos start) {
        if (!isBody(level.getBlockState(start))) {
            return null;
        }
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty() && seen.size() < MAX_BODY) {
            BlockPos pos = queue.poll();
            if (level.getBlockEntity(pos) instanceof OrganismCoreBlockEntity core) {
                return core;
            }
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (seen.add(next) && isBody(level.getBlockState(next))) {
                    queue.add(next);
                }
            }
        }
        return null;
    }

    private Organisms() {}
}

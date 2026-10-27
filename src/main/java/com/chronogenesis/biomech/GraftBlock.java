package com.chronogenesis.biomech;

import java.util.EnumMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A flesh part grafted onto an organism's surface (core-systems.md §2, "Grafts"). {@link #FACING} points
 * away from the organism; the block behind it must be part of the organism's body, or the graft drops off.
 */
public abstract class GraftBlock extends DirectionalBlock {
    private final Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);

    /** The shape is given as if the graft points up, sitting on the floor, and rotated for other facings. */
    protected GraftBlock(Properties properties, double x1, double y1, double z1, double x2, double y2, double z2) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
        for (Direction facing : Direction.values()) {
            shapes.put(facing, rotatedBox(facing, x1, y1, z1, x2, y2, z2));
        }
    }

    private static VoxelShape rotatedBox(Direction facing, double x1, double y1, double z1, double x2, double y2, double z2) {
        return switch (facing) {
            case UP -> Block.box(x1, y1, z1, x2, y2, z2);
            case DOWN -> Block.box(x1, 16 - y2, z1, x2, 16 - y1, z2);
            case NORTH -> Block.box(x1, z1, 16 - y2, x2, z2, 16 - y1);
            case SOUTH -> Block.box(x1, z1, y1, x2, z2, y2);
            case WEST -> Block.box(16 - y2, z1, x1, 16 - y1, z2, x2);
            case EAST -> Block.box(y1, z1, x1, y2, z2, x2);
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getClickedFace());
        return canSurvive(state, context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return Organisms.isBody(level.getBlockState(supportPos(state, pos)));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        if (direction == state.getValue(FACING).getOpposite() && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.get(state.getValue(FACING));
    }

    protected static BlockPos supportPos(BlockState state, BlockPos pos) {
        return pos.relative(state.getValue(FACING).getOpposite());
    }

    /** The core of the organism this graft is attached to. */
    @Nullable
    protected static OrganismCoreBlockEntity core(BlockGetter level, BlockState state, BlockPos pos) {
        OrganismCoreBlockEntity core = Organisms.findCore(level, supportPos(state, pos));
        return core == null || core.isNecrotic() ? null : core;
    }
}

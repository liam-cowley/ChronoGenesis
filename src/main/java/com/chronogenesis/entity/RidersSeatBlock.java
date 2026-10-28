package com.chronogenesis.entity;

import com.chronogenesis.registry.ModEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A Rider's Seat: brass and padded leather. Sit in it (use it); built into a Titanosaur's cargo, it steers the beast:
 * forward and back to walk, left and right to turn. Sneak to get up.
 */
public class RidersSeatBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<RidersSeatBlock> CODEC = simpleCodec(RidersSeatBlock::new);
    private static final VoxelShape SHAPE = box(1, 0, 1, 15, 8, 15);

    public RidersSeatBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!level.getEntitiesOfClass(RiderSeat.class, new AABB(pos)).isEmpty() || player.isPassenger()) {
            return InteractionResult.PASS;
        }
        RiderSeat seat = new RiderSeat(ModEntities.RIDER_SEAT.get(), level);
        seat.moveTo(pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, state.getValue(FACING).toYRot(), 0);
        level.addFreshEntity(seat);
        player.startRiding(seat);
        return InteractionResult.CONSUME;
    }
}

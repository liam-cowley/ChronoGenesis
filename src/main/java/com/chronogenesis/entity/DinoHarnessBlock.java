package com.chronogenesis.entity;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A Dino Harness, buckled on a Titanosaur's back: the origin of its cargo ({@link DinoCargo}), standing in the beast's
 * hold. Blocks are built against it, and against each other, on the beast. Breaking the harness (by any means) drops
 * the whole cargo at once.
 */
public class DinoHarnessBlock extends BaseEntityBlock {
    public static final MapCodec<DinoHarnessBlock> CODEC = simpleCodec(DinoHarnessBlock::new);

    public DinoHarnessBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DinoHarnessBlockEntity(pos, state);
    }

    /** Broken, blown up, or pushed: nothing holds the cargo on, and it falls. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof DinoHarnessBlockEntity harness) {
            Titanosaur beast = harness.owner();
            if (beast != null && pos.equals(beast.cargo().harness())) {
                beast.cargo().collapse();
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}

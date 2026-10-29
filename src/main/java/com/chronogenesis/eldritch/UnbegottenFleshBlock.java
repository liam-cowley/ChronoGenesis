package com.chronogenesis.eldritch;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The flesh of an Unbegotten tentacle lying across its arena ({@link com.chronogenesis.entity.UnbegottenLimb}): a wall
 * that cannot be broken, only hacked at (which wounds the tentacle). It lifts away when the tentacle does, and on its
 * own after a while (should its tentacle be gone).
 */
public class UnbegottenFleshBlock extends Block {
    /** It withers away on its own after this long. */
    private static final int LASTS = 400;

    public UnbegottenFleshBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        if (!old.is(this)) {
            level.scheduleTick(pos, this, LASTS);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }
}

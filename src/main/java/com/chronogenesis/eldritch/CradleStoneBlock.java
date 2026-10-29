package com.chronogenesis.eldritch;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Cradle Stone, in the middle of the arena Before Time: the one place a Last Offshoot takes root. Runes of eldritch
 * green ring its top, and motes rise from it, slowly.
 */
public class CradleStoneBlock extends Block {
    public CradleStoneBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 1.05,
                    pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, 0.03, 0);
        }
    }
}

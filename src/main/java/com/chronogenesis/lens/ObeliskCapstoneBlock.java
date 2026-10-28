package com.chronogenesis.lens;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The capstone of an obelisk: the head of the nail. Its block entity marks where an obelisk stands, so the
 * Temporal Lens (on both sides) can find obelisks without searching block by block. Time bends around it.
 */
public class ObeliskCapstoneBlock extends BaseEntityBlock {
    public static final MapCodec<ObeliskCapstoneBlock> CODEC = simpleCodec(ObeliskCapstoneBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(box(1, 0, 1, 15, 6, 15), box(4, 6, 4, 12, 12, 12), box(6.5, 12, 6.5, 9.5, 16, 9.5));

    public ObeliskCapstoneBlock(Properties properties) {
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

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ObeliskBlockEntity(pos, state);
    }

    /** Light falling the wrong way: particles drift in towards the tip instead of away. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < 2; i++) {
            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 3;
            double y = pos.getY() + 0.5 + (random.nextDouble() - 0.5) * 3;
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 3;
            level.addParticle(ParticleTypes.REVERSE_PORTAL, x, y, z,
                    (pos.getX() + 0.5 - x) * 0.05, (pos.getY() + 0.9 - y) * 0.05, (pos.getZ() + 0.5 - z) * 0.05);
        }
    }
}

package com.chronogenesis.temple;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The heart of a Jurassic temple: an amethyst engine in a gold cage, turning time to stone around it. While it is
 * {@link #ACTIVE}, the temple is held still (see {@link TempleBarrier}). Stilled (three seals broken) it can be
 * taken: it drops as a Stilled Core.
 */
public class TemporalCoreBlock extends BaseEntityBlock {
    public static final MapCodec<TemporalCoreBlock> CODEC = simpleCodec(TemporalCoreBlock::new);
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    private static final VoxelShape SHAPE = box(2, 0, 2, 14, 16, 14);

    public TemporalCoreBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, true));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
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
        return new TemporalCoreBlockEntity(pos, state);
    }

    /** It can only be broken once stilled. */
    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return state.getValue(ACTIVE) ? 0.0f : super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TemporalCoreBlockEntity core) {
            player.displayClientMessage(Component.translatable(core.active() ? "message.chronogenesis.temple.core_active"
                    : "message.chronogenesis.temple.core_still", core.sealsBroken(), TemporalCoreBlockEntity.SEALS_NEEDED)
                    .withStyle(ChatFormatting.GOLD), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(ACTIVE)) {
            for (int i = 0; i < 2; i++) {
                level.addParticle(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5 + random.nextGaussian() * 0.4,
                        pos.getY() + 0.5 + random.nextGaussian() * 0.4, pos.getZ() + 0.5 + random.nextGaussian() * 0.4, 0, 0, 0);
            }
        }
    }
}

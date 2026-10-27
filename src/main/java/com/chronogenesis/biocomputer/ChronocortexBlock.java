package com.chronogenesis.biocomputer;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.network.OpenChronocortexPayload;
import com.chronogenesis.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;

/** The woken brain in its jar on the old Dissection Table. Use it to see its view: organisms and research. */
public class ChronocortexBlock extends BaseEntityBlock {
    public static final MapCodec<ChronocortexBlock> CODEC = simpleCodec(ChronocortexBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(
            box(0, 7, 0, 16, 9, 16),
            box(1, 0, 1, 3, 7, 3), box(13, 0, 1, 15, 7, 3), box(1, 0, 13, 3, 7, 15), box(13, 0, 13, 15, 7, 15),
            box(3, 9, 3, 13, 16, 13));

    public ChronocortexBlock(Properties properties) {
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
        return new ChronocortexBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.CHRONOCORTEX.get(), ChronocortexBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof ChronocortexBlockEntity cortex) {
            PacketDistributor.sendToPlayer(serverPlayer, OpenChronocortexPayload.of(cortex));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /** Thought crackles over it now and then. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.3 + random.nextDouble() * 0.4,
                    pos.getY() + 1.05, pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0, 0.02, 0);
        }
    }
}

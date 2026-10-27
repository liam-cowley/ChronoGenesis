package com.chronogenesis.time;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.biocomputer.Clues;
import com.chronogenesis.essence.Aspect;
import com.chronogenesis.essence.EssenceContents;
import com.chronogenesis.essence.EssenceItems;
import com.chronogenesis.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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

/**
 * A Time Tear: a rift opened by too much Temporal Stress. It ages nearby blocks and swallows
 * dropped items. Seal it with 4 drams of Density ("heavy things keep time still").
 */
public class TimeTearBlock extends BaseEntityBlock {
    public static final MapCodec<TimeTearBlock> CODEC = simpleCodec(TimeTearBlock::new);
    public static final int SEAL_COST = 4;
    private static final VoxelShape SHAPE = box(3, 3, 3, 13, 13, 13);

    public TimeTearBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    /** However a tear opens (stress, or a Time organism bursting), whoever sees it takes note. */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel serverLevel && !oldState.is(state.getBlock())) {
            Clues.witness(serverLevel, pos, Clues.TORN);
        }
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        EssenceContents contents = EssenceItems.get(stack);
        if (contents == null || contents.aspect() != Aspect.DENSITY) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (contents.amount() < SEAL_COST) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.chronogenesis.tear.not_enough",
                        SEAL_COST).withStyle(ChatFormatting.GRAY), true);
            }
            return ItemInteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            EssenceItems.drainHeld(player, hand, Aspect.DENSITY, SEAL_COST);
            level.removeBlock(pos, false);
            level.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.BLOCKS, 1.0f, 0.8f);
            player.displayClientMessage(Component.translatable("message.chronogenesis.tear.sealed")
                    .withStyle(ChatFormatting.GRAY), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < 3; i++) {
            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 1.5;
            double y = pos.getY() + 0.5 + (random.nextDouble() - 0.5) * 1.5;
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 1.5;
            // Particles drift inwards, as if pulled into the rift.
            level.addParticle(ParticleTypes.REVERSE_PORTAL, x, y, z,
                    (pos.getX() + 0.5 - x) * 0.1, (pos.getY() + 0.5 - y) * 0.1, (pos.getZ() + 0.5 - z) * 0.1);
        }
        if (random.nextInt(40) == 0) {
            level.playLocalSound(pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.5f, 0.3f, false);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TimeTearBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.TIME_TEAR.get(), TimeTearBlockEntity::serverTick);
    }
}

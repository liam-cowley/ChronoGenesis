package com.chronogenesis.entity;

import javax.annotation.Nullable;

import com.chronogenesis.essence.Aspect;
import com.chronogenesis.essence.EssenceContents;
import com.chronogenesis.essence.EssenceItems;
import com.chronogenesis.item.EternalHourglassItem;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A dinosaur's egg, budded by the Chronocortex from its own flesh once it has been fed that creature's fossil. Fed
 * Time (from any container holding it), it quickens: {@link #HATCH_AT} drams and it cracks open, and the hatchling is
 * out ({@link Dinos}). Its shell cracks as it fills ({@code cracks} 0-3).
 */
public class DinoEggBlock extends BaseEntityBlock {
    public static final MapCodec<DinoEggBlock> CODEC = simpleCodec(DinoEggBlock::new);
    public static final IntegerProperty CRACKS = IntegerProperty.create("cracks", 0, 3);
    /** Drams of Time to hatch. */
    public static final int HATCH_AT = 16;
    private static final VoxelShape SHAPE = box(4, 0, 4, 12, 11, 12);

    public DinoEggBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CRACKS, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CRACKS);
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
        return new DinoEggBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        EssenceContents held = EssenceItems.get(stack);
        boolean hourglass = EternalHourglassItem.carried(player);
        if ((held == null || held.aspect() != Aspect.TIME || held.isEmpty()) && !hourglass) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!(level instanceof ServerLevel server) || !(level.getBlockEntity(pos) instanceof DinoEggBlockEntity egg)) {
            return ItemInteractionResult.SUCCESS;
        }
        int want = HATCH_AT - egg.fed();
        EssenceContents taken = hourglass ? new EssenceContents(Aspect.TIME, want) : EssenceItems.drainHeld(player, hand, Aspect.TIME, want);
        if (taken == null || taken.isEmpty()) {
            return ItemInteractionResult.CONSUME;
        }
        egg.feed(taken.amount());
        server.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.2, 0.3, 0.2, 0.02);
        if (egg.fed() >= HATCH_AT) {
            egg.hatch(server, player);
        } else {
            int cracks = Math.min(3, egg.fed() * 4 / HATCH_AT);
            if (cracks != state.getValue(CRACKS)) {
                level.setBlockAndUpdate(pos, state.setValue(CRACKS, cracks));
                level.playSound(null, pos, SoundEvents.TURTLE_EGG_CRACK, SoundSource.BLOCKS, 0.8f, 0.7f);
            }
        }
        return ItemInteractionResult.CONSUME;
    }
}

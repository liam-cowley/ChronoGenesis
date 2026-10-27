package com.chronogenesis.organism;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.essence.EssenceContents;
import com.chronogenesis.essence.EssenceItems;
import com.mojang.serialization.MapCodec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
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
 * Essence Jug: a placeable 64-dram store. Use a filled container on it to pour in, an empty
 * container to fill up from it. A Tap facing the jug fills it automatically. {@link #FILL} shows how full
 * it is through the glass; the liquid is tinted by the aspect inside.
 */
public class EssenceJugBlock extends BaseEntityBlock {
    public static final MapCodec<EssenceJugBlock> CODEC = simpleCodec(EssenceJugBlock::new);
    public static final IntegerProperty FILL = IntegerProperty.create("fill", 0, 4);
    private static final VoxelShape SHAPE = box(4, 0, 4, 12, 12, 12);

    public EssenceJugBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FILL, 0));
    }

    /** 0 when empty, otherwise 1-4. */
    public static int fillLevel(@Nullable EssenceContents contents) {
        if (contents == null || contents.amount() <= 0) {
            return 0;
        }
        return 1 + Math.min(3, (contents.amount() - 1) * 4 / EssenceJugBlockEntity.CAPACITY);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FILL);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FILL, fillLevel(EssenceItems.get(context.getItemInHand())));
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

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (!EssenceItems.isContainer(stack) || !(level.getBlockEntity(pos) instanceof EssenceJugBlockEntity jug)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        EssenceContents held = EssenceItems.get(stack);
        if (held != null) {
            // Pour in.
            int space = jug.space(held.aspect());
            EssenceContents taken = space > 0 ? EssenceItems.drainHeld(player, hand, held.aspect(), space) : null;
            if (taken == null) {
                return ItemInteractionResult.FAIL;
            }
            jug.insert(taken.aspect(), taken.amount());
            level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.8f, 1.0f);
        } else {
            // Fill the empty container from the jug.
            EssenceContents inJug = jug.contents();
            if (inJug == null) {
                return ItemInteractionResult.FAIL;
            }
            int space = EssenceItems.space(stack, inJug.aspect());
            int moved = jug.extract(space);
            EssenceItems.fillHeld(player, hand, inJug.aspect(), moved);
            level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.8f, 1.0f);
        }
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof EssenceJugBlockEntity jug) {
            EssenceContents contents = jug.contents();
            Component message = contents == null
                    ? Component.translatable("tooltip.chronogenesis.empty")
                    : Component.translatable("tooltip.chronogenesis.contents", contents.amount(),
                            EssenceJugBlockEntity.CAPACITY, contents.aspect().displayName());
            player.displayClientMessage(message.copy().withStyle(ChatFormatting.GRAY), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof EssenceJugBlockEntity jug ? jug.comparatorSignal() : 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EssenceJugBlockEntity(pos, state);
    }
}

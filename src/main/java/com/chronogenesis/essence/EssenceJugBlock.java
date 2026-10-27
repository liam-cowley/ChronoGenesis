package com.chronogenesis.essence;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.item.EssenceItems;
import com.chronogenesis.item.EssenceHolder;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Essence Jug: a placeable glass jug for 64 drams of a stable aspect. Use a filled container on it to pour
 * in, an empty one to draw out. {@link #FILL} shows how full it is; the liquid is tinted by the aspect.
 */
public class EssenceJugBlock extends BaseEntityBlock {
    public static final MapCodec<EssenceJugBlock> CODEC = simpleCodec(EssenceJugBlock::new);
    public static final IntegerProperty FILL = IntegerProperty.create("fill", 0, 4);
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(4, 0, 4, 12, 10, 12),
            Block.box(6, 10, 6, 10, 15, 10));

    public EssenceJugBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FILL, 0));
    }

    /** 0 when empty, otherwise 1–4. */
    public static int fillLevel(int drams) {
        return drams <= 0 ? 0 : 1 + Math.min(3, (drams - 1) * 4 / EssenceJugBlockEntity.CAPACITY);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FILL);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FILL, fillLevel(EssenceItems.drams(context.getItemInHand())));
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
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EssenceJugBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof EssenceHolder) || !(level.getBlockEntity(pos) instanceof EssenceJugBlockEntity jug)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        return EssenceItems.exchangeWithStorage(player, stack, jug, true, true)
                ? ItemInteractionResult.SUCCESS : ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof EssenceJugBlockEntity jug) {
            EssenceContents contents = jug.contents();
            player.displayClientMessage(contents == null
                    ? Component.translatable("tooltip.chronogenesis.empty")
                    : Component.translatable("tooltip.chronogenesis.contents", contents.aspect().displayName(),
                            contents.drams(), jug.capacity()), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof EssenceJugBlockEntity jug
                ? (int) Math.ceil(15.0 * jug.stored() / jug.capacity()) : 0;
    }
}

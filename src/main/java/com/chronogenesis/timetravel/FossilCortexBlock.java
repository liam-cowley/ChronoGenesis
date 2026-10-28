package com.chronogenesis.timetravel;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.item.NerveSpoolItem;
import com.mojang.serialization.MapCodec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A Fossil Cortex: an offshoot planted in a past era, grown through all the ages since, and petrified (see
 * {@link Offshoots}). It still dreams the era it grew in. Bind a {@link NerveSpoolItem} to it and lay the nerve to
 * your Chronocortex, at any distance, and it gives those memories up ({@code awake}: linked, its veins lit).
 */
public class FossilCortexBlock extends BaseEntityBlock {
    public static final MapCodec<FossilCortexBlock> CODEC = simpleCodec(FossilCortexBlock::new);
    public static final BooleanProperty AWAKE = BooleanProperty.create("awake");
    private static final VoxelShape SHAPE = Shapes.or(box(0, 0, 0, 16, 6, 16), box(2, 6, 2, 14, 15, 14));

    public FossilCortexBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AWAKE, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AWAKE);
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
        return new FossilCortexBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof NerveSpoolItem) {
            return NerveSpoolItem.bind(stack, level, pos, player);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** It tells you which age it dreams of. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FossilCortexBlockEntity fossil && fossil.era() != null) {
            Era era = Eras.get(level.registryAccess(), fossil.era());
            Component name = era == null ? Component.literal(fossil.era().toString()) : era.title();
            player.displayClientMessage(Component.translatable(state.getValue(AWAKE)
                    ? "message.chronogenesis.fossil.linked" : "message.chronogenesis.fossil.dreaming", name)
                    .withStyle(ChatFormatting.DARK_AQUA), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(AWAKE) && random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 1.0,
                    pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, 0.02, 0);
        } else if (random.nextInt(12) == 0) {
            level.addParticle(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                    random.nextGaussian() * 0.3, 0.3, random.nextGaussian() * 0.3);
        }
    }
}

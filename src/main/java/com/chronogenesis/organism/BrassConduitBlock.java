package com.chronogenesis.organism;

import java.util.EnumMap;
import java.util.Map;

import com.chronogenesis.registry.ModBlocks;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Brass Conduit: a pipe-like frame for veins (core-systems.md section 2). It connects to other
 * conduits and to grafts whose front faces it. A Tap pushing into a conduit grows it into a living
 * {@link #VEIN}, one block at a time; essence only travels through veins.
 * A Racetrack pulse that stalls leaves the vein {@link #CLOGGED}: cut the clot out with shears.
 */
public class BrassConduitBlock extends Block {
    public static final BooleanProperty VEIN = BooleanProperty.create("vein");
    public static final BooleanProperty CLOGGED = BooleanProperty.create("clogged");
    public static final Map<Direction, BooleanProperty> CONNECTIONS = new EnumMap<>(Map.of(
            Direction.NORTH, BlockStateProperties.NORTH, Direction.SOUTH, BlockStateProperties.SOUTH,
            Direction.EAST, BlockStateProperties.EAST, Direction.WEST, BlockStateProperties.WEST,
            Direction.UP, BlockStateProperties.UP, Direction.DOWN, BlockStateProperties.DOWN));

    private static final VoxelShape CORE = box(5, 5, 5, 11, 11, 11);
    private static final Map<Direction, VoxelShape> ARMS = new EnumMap<>(Map.of(
            Direction.NORTH, box(5, 5, 0, 11, 11, 5), Direction.SOUTH, box(5, 5, 11, 11, 11, 16),
            Direction.WEST, box(0, 5, 5, 5, 11, 11), Direction.EAST, box(11, 5, 5, 16, 11, 11),
            Direction.DOWN, box(5, 0, 5, 11, 5, 11), Direction.UP, box(5, 11, 5, 11, 16, 11)));

    public BrassConduitBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any().setValue(VEIN, false).setValue(CLOGGED, false);
        for (BooleanProperty property : CONNECTIONS.values()) {
            state = state.setValue(property, false);
        }
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VEIN, CLOGGED);
        CONNECTIONS.values().forEach(builder::add);
    }

    /** A vein that essence can flow through right now. */
    public static boolean isOpenVein(BlockState state) {
        return state.getBlock() instanceof BrassConduitBlock && state.getValue(VEIN) && !state.getValue(CLOGGED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE;
        for (Map.Entry<Direction, BooleanProperty> entry : CONNECTIONS.entrySet()) {
            if (state.getValue(entry.getValue())) {
                shape = Shapes.or(shape, ARMS.get(entry.getKey()));
            }
        }
        return shape;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withConnections(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
            BlockPos pos, BlockPos neighborPos) {
        return state.setValue(CONNECTIONS.get(direction), connectsTo(neighborState, direction));
    }

    public static BlockState withConnections(BlockState state, BlockGetter level, BlockPos pos) {
        for (Map.Entry<Direction, BooleanProperty> entry : CONNECTIONS.entrySet()) {
            BlockState neighbor = level.getBlockState(pos.relative(entry.getKey()));
            state = state.setValue(entry.getValue(), connectsTo(neighbor, entry.getKey()));
        }
        return state;
    }

    /** Connects to other conduits, and to grafts whose front faces this conduit. */
    private static boolean connectsTo(BlockState neighbor, Direction direction) {
        if (neighbor.is(ModBlocks.BRASS_CONDUIT.get())) {
            return true;
        }
        return neighbor.getBlock() instanceof GraftBlock && neighbor.getValue(GraftBlock.FACING) == direction.getOpposite();
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (stack.is(Items.SHEARS) && state.getValue(CLOGGED)) {
            if (!level.isClientSide()) {
                level.setBlockAndUpdate(pos, state.setValue(CLOGGED, false));
                level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 1.0f, 0.6f);
                popResource(level, pos, new ItemStack(Items.ROTTEN_FLESH));
                stack.hurtAndBreak(1, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(hand));
                player.displayClientMessage(Component.translatable("message.chronogenesis.vein.unclogged")
                        .withStyle(ChatFormatting.GRAY), true);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}

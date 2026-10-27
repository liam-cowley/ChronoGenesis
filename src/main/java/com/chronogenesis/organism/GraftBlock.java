package com.chronogenesis.organism;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.registry.ModBlockEntities;
import com.chronogenesis.registry.ModBlocks;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A graft: a flesh part attached to the surface of an organism, deciding what it does.
 * {@code FACING} points away from the flesh it grows on. If that flesh is gone, the graft drops.
 */
public class GraftBlock extends BaseEntityBlock {
    public static final MapCodec<GraftBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            GraftType.CODEC.fieldOf("graft").forGetter(GraftBlock::type),
            propertiesCodec()
    ).apply(instance, GraftBlock::new));

    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    /** The graft is visibly at work right now (a Leech Tendril swelling as it swallows). Purely visual. */
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    private static final Map<Direction, VoxelShape> SHAPES = Map.of(
            Direction.NORTH, box(3, 3, 6, 13, 13, 16),
            Direction.SOUTH, box(3, 3, 0, 13, 13, 10),
            Direction.EAST, box(0, 3, 3, 10, 13, 13),
            Direction.WEST, box(6, 3, 3, 16, 13, 13),
            Direction.UP, box(3, 0, 3, 13, 10, 13),
            Direction.DOWN, box(3, 6, 3, 13, 16, 13));

    private final GraftType type;

    public GraftBlock(GraftType type, Properties properties) {
        super(properties);
        this.type = type;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP).setValue(ACTIVE, false));
    }

    public GraftType type() {
        return type;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    /** The tissue block this graft grows out of. */
    public static BlockPos supportPos(BlockState state, BlockPos pos) {
        return pos.relative(state.getValue(FACING).getOpposite());
    }

    private static boolean isTissue(BlockState state) {
        return state.is(ModBlocks.CHRONOFLESH.get()) || state.is(ModBlocks.ORGANISM_CORE.get());
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getClickedFace());
        return canSurvive(state, context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return isTissue(level.getBlockState(supportPos(state, pos)));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
            BlockPos pos, BlockPos neighborPos) {
        if (direction == state.getValue(FACING).getOpposite() && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof GraftBlockEntity graft) {
            return graft.useItemOn(stack, player, hand);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof GraftBlockEntity graft) {
            return graft.useWithoutItem(player);
        }
        return InteractionResult.PASS;
    }

    // The Eye graft: redstone while it sees a creature, comparator output for how full the organism is.

    @Override
    protected boolean isSignalSource(BlockState state) {
        return type == GraftType.EYE;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return type == GraftType.EYE && level.getBlockEntity(pos) instanceof GraftBlockEntity eye && eye.seesCreature() ? 15 : 0;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return type == GraftType.EYE;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof GraftBlockEntity eye ? eye.reportedFill() : 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GraftBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.GRAFT.get(), GraftBlockEntity::serverTick);
    }
}

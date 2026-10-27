package com.chronogenesis.organism;

import com.chronogenesis.essence.Aspect;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Chronoflesh: living tissue grown over a Copper Armature frame by an organism.
 * Tinted by the aspect it was grown on. Feeding any flesh block feeds the whole organism.
 * <p>
 * {@link #GROWTH} is how far the flesh has swallowed its frame:
 * 0 tendrils on the bare frame, 1 a creeping inner mass, 2 a swollen mass straining the frame,
 * 3 mature. New flesh starts at 0 and the core matures it; starving organisms wither it back down.
 */
public class FleshBlock extends Block {
    public static final EnumProperty<Aspect> ASPECT = EnumProperty.create("aspect", Aspect.class);
    public static final IntegerProperty GROWTH = IntegerProperty.create("growth", 0, 3);
    public static final int MATURE = 3;

    public FleshBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ASPECT, Aspect.DENSITY).setValue(GROWTH, MATURE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ASPECT, GROWTH);
    }

    /** Only mature flesh hides the faces of its neighbours; younger growth is see-through frame. */
    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(GROWTH) == MATURE ? Shapes.block() : Shapes.empty();
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(GROWTH) < 2;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        return OrganismInteractions.useItemOnTissue(stack, level, pos, player, hand);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return OrganismInteractions.inspect(level, pos, player);
    }
}

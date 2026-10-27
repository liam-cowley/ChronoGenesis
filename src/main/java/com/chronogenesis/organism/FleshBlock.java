package com.chronogenesis.organism;

import com.chronogenesis.essence.Aspect;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Chronoflesh: living tissue grown over a Copper Armature frame by an organism.
 * Tinted by the aspect it was grown on. Feeding any flesh block feeds the whole organism.
 */
public class FleshBlock extends Block {
    public static final EnumProperty<Aspect> ASPECT = EnumProperty.create("aspect", Aspect.class);

    public FleshBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ASPECT, Aspect.DENSITY));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ASPECT);
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

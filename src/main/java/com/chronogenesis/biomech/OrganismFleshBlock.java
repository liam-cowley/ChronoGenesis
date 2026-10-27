package com.chronogenesis.biomech;

import com.chronogenesis.essence.Aspect;
import com.mojang.serialization.MapCodec;

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

/** Living flesh grown over a Copper Armature. Cutting it off yields Living Flesh and the bare frame. */
public class OrganismFleshBlock extends Block {
    public static final MapCodec<OrganismFleshBlock> CODEC = simpleCodec(OrganismFleshBlock::new);
    public static final EnumProperty<Aspect> ASPECT = OrganismCoreBlock.ASPECT;

    public OrganismFleshBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ASPECT, Aspect.DENSITY));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ASPECT);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        return OrganismCoreBlock.feed(stack, level, pos, player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return OrganismCoreBlock.showStatus(level, pos, player);
    }
}

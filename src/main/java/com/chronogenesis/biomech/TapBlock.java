package com.chronogenesis.biomech;

import com.chronogenesis.item.EssenceHolder;
import com.chronogenesis.item.EssenceItems;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Tap graft (output): opens the organism's store. Use an empty or matching container on it to fill it.
 * A Maw facing the Tap pulls from it too.
 */
public class TapBlock extends GraftBlock {
    public static final MapCodec<TapBlock> CODEC = simpleCodec(TapBlock::new);

    public TapBlock(Properties properties) {
        super(properties, 4, 0, 4, 12, 9, 12);
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    /** The core feeding this Tap, if the organism is alive. */
    public static OrganismCoreBlockEntity coreOf(Level level, BlockState state, BlockPos pos) {
        return core(level, state, pos);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof EssenceHolder)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        OrganismCoreBlockEntity core = core(level, state, pos);
        if (core == null || !EssenceItems.exchangeWithStorage(player, stack, core, false, true)) {
            player.displayClientMessage(Component.translatable("message.chronogenesis.tap.dry"), true);
            return ItemInteractionResult.CONSUME;
        }
        return ItemInteractionResult.SUCCESS;
    }
}

package com.chronogenesis.lens;

import java.util.List;

import com.chronogenesis.biocomputer.Clues;
import com.chronogenesis.registry.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Rarely, one blueprint in an Abandoned Laboratory survived. Read it (use it) to copy it into the journal as a
 * clue. The paper is kept.
 */
public class IntactBlueprintItem extends Item {
    public IntactBlueprintItem(Properties properties) {
        super(properties);
    }

    public static Blueprint design(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.BLUEPRINT.get(), Blueprint.LOCKET);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            Clues.grant(serverPlayer, design(stack).clue());
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.chronogenesis.blueprint." + design(stack).getSerializedName())
                .withStyle(ChatFormatting.GRAY));
    }
}

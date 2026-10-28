package com.chronogenesis.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

/**
 * A block that is only ever made in play (by a ritual, say), offered as an item in the creative tab for testing.
 * No recipe makes it. Its tooltip says what it is and that it is creative only.
 */
public class CreativeBlockItem extends BlockItem {
    private final String hint;

    public CreativeBlockItem(Block block, Properties properties, String hint) {
        super(block, properties);
        this.hint = hint;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(hint).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.chronogenesis.eternal_hourglass.creative").withStyle(ChatFormatting.DARK_GRAY));
    }
}

package com.chronogenesis.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * The Confluence Needle: two hollow needles twisted into one, a brass Y-joint where they meet. Fitted into an
 * Enhancing Gland (use it on the gland; sneak-use with an empty hand to take it back), it draws on two organisms
 * at once: the gland's own, and one of another essence wired to it by veins. The essences mix in the player, who
 * learns the power the two make together.
 */
public class ConfluenceNeedleItem extends Item {
    public ConfluenceNeedleItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.chronogenesis.confluence_needle").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.chronogenesis.confluence_needle.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}

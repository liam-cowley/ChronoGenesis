package com.chronogenesis.item;

import java.util.List;

import com.chronogenesis.essence.EssenceContents;
import com.chronogenesis.essence.EssenceItems;
import com.chronogenesis.organism.EssenceJugBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

/** The jug as an item: shows what it holds. Contents travel in the essence data component. */
public class EssenceJugItem extends BlockItem {
    public EssenceJugItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        EssenceContents contents = EssenceItems.get(stack);
        if (contents == null) {
            tooltip.add(Component.translatable("tooltip.chronogenesis.empty").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.chronogenesis.contents", contents.amount(),
                    EssenceJugBlockEntity.CAPACITY, contents.aspect().displayName())
                    .withStyle(style -> style.withColor(contents.aspect().color())));
        }
    }
}

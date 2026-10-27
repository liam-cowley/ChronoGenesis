package com.chronogenesis.item;

import java.util.List;

import com.chronogenesis.essence.Needle;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** A swappable syringe needle. Right-click it onto a Chrono Syringe in the inventory to fit it. */
public class NeedleItem extends Item {
    private final Needle needle;

    public NeedleItem(Properties properties, Needle needle) {
        super(properties);
        this.needle = needle;
    }

    public Needle needle() {
        return needle;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.chronogenesis.needle.draws", needle.aspect().displayName())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.chronogenesis.needle.fit").withStyle(ChatFormatting.DARK_GRAY));
    }
}

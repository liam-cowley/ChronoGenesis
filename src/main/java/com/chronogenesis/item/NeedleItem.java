package com.chronogenesis.item;

import java.util.List;

import com.chronogenesis.essence.Aspect;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * A syringe needle. Each draws one aspect (core-systems.md section 1):
 * iron = Density, gold = Speed, bone = Size, amethyst = Time.
 * Fit it by holding it in the off hand and sneak-using the syringe.
 */
public class NeedleItem extends Item {
    private final Aspect aspect;

    public NeedleItem(Properties properties, Aspect aspect) {
        super(properties);
        this.aspect = aspect;
    }

    public Aspect aspect() {
        return aspect;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.chronogenesis.needle_draws", aspect.displayName())
                .withStyle(style -> style.withColor(aspect.color())));
        tooltip.add(Component.translatable("tooltip.chronogenesis.needle_hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}

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
 * iron = Density, gold = Speed, bone = Size, amethyst = Time; the needles of the ages (sabre fang, raptor claw,
 * mammoth ivory, zircon) draw the same aspects two drams a jab.
 * Fit it by holding it in the off hand and sneak-using the syringe.
 */
public class NeedleItem extends Item {
    private final Aspect aspect;
    private final int draw;

    public NeedleItem(Properties properties, Aspect aspect, int draw) {
        super(properties);
        this.aspect = aspect;
        this.draw = draw;
    }

    public Aspect aspect() {
        return aspect;
    }

    /** Drams drawn with each jab. */
    public int draw() {
        return draw;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.chronogenesis.needle_draws", aspect.displayName())
                .withStyle(style -> style.withColor(aspect.color())));
        if (draw > 1) {
            tooltip.add(Component.translatable("tooltip.chronogenesis.needle_draw", draw).withStyle(ChatFormatting.GOLD));
        }
        tooltip.add(Component.translatable("tooltip.chronogenesis.needle_hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}

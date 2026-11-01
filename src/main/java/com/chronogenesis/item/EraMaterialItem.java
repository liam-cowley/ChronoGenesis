package com.chronogenesis.item;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * A material from one age of the world, brought back by temporal shift (or, for the present's own components, made
 * at the bench). Its tooltip names the age it comes from in that age's colour, then a line of what it is
 * ({@code tooltip.chronogenesis.<item>}). Every era has its own: the Precursors' five kingdoms, the Ice Age's beasts
 * and frost, the Jurassic's claws, scales and amber, the Hadean's first minerals, and the ichor of what waits before
 * time.
 */
public class EraMaterialItem extends Item {
    /** The ages, each with its colour; {@code null} for the present's own components. */
    public enum Age {
        PRECURSOR_ERA(0xE6B84A), ICE_AGE(0x9FD8F0), JURASSIC_AGE(0x86C45A), HADEAN_EON(0xE8693A), BEFORE_TIME(0xB07AD8);

        private final int color;

        Age(int color) {
            this.color = color;
        }

        public Component displayName() {
            return Component.translatable("era.chronogenesis." + name().toLowerCase(java.util.Locale.ROOT));
        }
    }

    @Nullable
    private final Age age;

    public EraMaterialItem(Properties properties, @Nullable Age age) {
        super(properties);
        this.age = age;
    }

    @Nullable
    public Age age() {
        return age;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (age != null) {
            tooltip.add(Component.translatable("tooltip.chronogenesis.from_age", age.displayName())
                    .withStyle(style -> style.withColor(age.color)));
        }
        tooltip.add(Component.translatable("tooltip.chronogenesis." + BuiltInRegistries.ITEM.getKey(this).getPath())
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}

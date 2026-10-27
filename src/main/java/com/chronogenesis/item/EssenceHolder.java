package com.chronogenesis.item;

import com.chronogenesis.essence.Aspect;

import net.minecraft.world.item.ItemStack;

/**
 * An item that carries essence in its {@code ModDataComponents.ESSENCE} component. {@link EssenceItems} does
 * the actual moving, so every container (syringe, vial, ampoule, jug, culture jar) pours the same way.
 */
public interface EssenceHolder {
    int capacity(ItemStack stack);

    boolean accepts(ItemStack stack, Aspect aspect);

    /** Whether essence can be poured back out of this item. */
    default boolean canGive(ItemStack stack) {
        return true;
    }
}

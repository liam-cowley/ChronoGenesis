package com.chronogenesis.item;

import java.util.List;
import java.util.function.Predicate;

import com.chronogenesis.essence.Aspect;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * A glass container for essence: Essence Vial, Stasis Ampoule, and the base of the syringe and culture jar.
 * Right-click one container onto another in the inventory to pour.
 */
public class EssenceContainerItem extends Item implements EssenceHolder {
    private final int capacity;
    private final Predicate<Aspect> accepts;

    public EssenceContainerItem(Properties properties, int capacity, Predicate<Aspect> accepts) {
        super(properties);
        this.capacity = capacity;
        this.accepts = accepts;
    }

    /** Syringes, vials and jugs only hold stable aspects. */
    public static boolean stableOnly(Aspect aspect) {
        return aspect.stability() == Aspect.Stability.STABLE;
    }

    @Override
    public int capacity(ItemStack stack) {
        return capacity;
    }

    @Override
    public boolean accepts(ItemStack stack, Aspect aspect) {
        return accepts.test(aspect);
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess access) {
        return EssenceItems.handleStackedOnMe(stack, other, action, player, access);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        EssenceItems.appendContentsTooltip(stack, capacity(stack), tooltip);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return EssenceItems.drams(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return EssenceItems.barWidth(stack, capacity(stack));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return EssenceItems.barColor(stack);
    }
}

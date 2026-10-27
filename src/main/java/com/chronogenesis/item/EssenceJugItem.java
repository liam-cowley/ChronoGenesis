package com.chronogenesis.item;

import java.util.List;

import com.chronogenesis.essence.Aspect;
import com.chronogenesis.essence.EssenceJugBlockEntity;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

/** The Essence Jug as an item: keeps its contents when broken, and pours like any other container. */
public class EssenceJugItem extends BlockItem implements EssenceHolder {
    public EssenceJugItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public int capacity(ItemStack stack) {
        return EssenceJugBlockEntity.CAPACITY;
    }

    @Override
    public boolean accepts(ItemStack stack, Aspect aspect) {
        return EssenceContainerItem.stableOnly(aspect);
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

package com.chronogenesis.item;

import java.util.List;
import java.util.function.Predicate;

import com.chronogenesis.essence.Aspect;
import com.chronogenesis.essence.EssenceContents;
import com.chronogenesis.essence.EssenceItems;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** An item that holds drams of one aspect: syringe, vial, Stasis Ampoule. */
public class EssenceContainerItem extends Item {
    private final int capacity;
    private final Predicate<Aspect> accepts;

    public EssenceContainerItem(Properties properties, int capacity, Predicate<Aspect> accepts) {
        super(properties);
        this.capacity = capacity;
        this.accepts = accepts;
    }

    public int capacity() {
        return capacity;
    }

    public boolean accepts(Aspect aspect) {
        return accepts.test(aspect);
    }

    /**
     * Right-click a container onto another in an inventory to pour between them: a filled one on the
     * cursor pours in, an empty one on the cursor draws out. Otherwise vanilla behaviour is kept.
     */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack carried, Slot slot, ClickAction action,
            Player player, SlotAccess access) {
        if (action != ClickAction.SECONDARY || !EssenceItems.isContainer(carried)) {
            return false;
        }
        boolean moved = EssenceItems.get(carried) != null
                ? EssenceItems.pour(player, carried, stack)
                : EssenceItems.pour(player, stack, carried);
        if (moved) {
            player.playSound(SoundEvents.BOTTLE_FILL, 0.6f, 1.4f);
        }
        return moved;
    }

    /** Using a filled container in a recipe gives the empty container back. */
    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return EssenceItems.get(stack) != null;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        if (EssenceItems.get(stack) == null) {
            return ItemStack.EMPTY;
        }
        ItemStack empty = stack.copyWithCount(1);
        EssenceItems.set(empty, null);
        return empty;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return EssenceItems.get(stack) != null;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        EssenceContents contents = EssenceItems.get(stack);
        return contents == null ? 0 : Mth.clamp(Math.round(13.0f * contents.amount() / capacity), 0, 13);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        EssenceContents contents = EssenceItems.get(stack);
        return contents == null ? 0xFFFFFF : contents.aspect().color();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        EssenceContents contents = EssenceItems.get(stack);
        if (contents == null) {
            tooltip.add(Component.translatable("tooltip.chronogenesis.empty").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.chronogenesis.contents",
                    contents.amount(), capacity, contents.aspect().displayName())
                    .withStyle(style -> style.withColor(contents.aspect().color())));
        }
    }
}

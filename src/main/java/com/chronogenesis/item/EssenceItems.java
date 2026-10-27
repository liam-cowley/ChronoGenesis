package com.chronogenesis.item;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.essence.Aspect;
import com.chronogenesis.essence.EssenceContents;
import com.chronogenesis.essence.EssenceStorage;
import com.chronogenesis.registry.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.item.ItemStack;

/**
 * Moving essence between item stacks and storage blocks. Stacks of several empty containers are
 * split: one container is filled and handed back to the player, the rest stay where they were.
 */
public final class EssenceItems {
    @Nullable
    public static EssenceContents contents(ItemStack stack) {
        return stack.get(ModDataComponents.ESSENCE);
    }

    public static int drams(ItemStack stack) {
        EssenceContents contents = contents(stack);
        return contents == null ? 0 : contents.drams();
    }

    public static void setContents(ItemStack stack, @Nullable Aspect aspect, int drams) {
        if (aspect == null || drams <= 0) {
            stack.remove(ModDataComponents.ESSENCE);
        } else {
            stack.set(ModDataComponents.ESSENCE, new EssenceContents(aspect, drams));
        }
    }

    /** How many drams of {@code aspect} the holder stack could still take. */
    public static int space(ItemStack stack, Aspect aspect) {
        if (!(stack.getItem() instanceof EssenceHolder holder) || !holder.accepts(stack, aspect)) {
            return 0;
        }
        EssenceContents contents = contents(stack);
        if (contents != null && contents.aspect() != aspect) {
            return 0;
        }
        return holder.capacity(stack) - drams(stack);
    }

    /**
     * Adds essence to one container of the stack, splitting it off if the stack holds several.
     *
     * @return drams added
     */
    public static int fill(Player player, ItemStack stack, Aspect aspect, int drams) {
        int amount = Math.min(drams, space(stack, aspect));
        if (amount <= 0) {
            return 0;
        }
        ItemStack target = single(stack);
        setContents(target, aspect, drams(target) + amount);
        giveBackIfSplit(player, stack, target);
        return amount;
    }

    /**
     * Removes essence from one container of the stack.
     *
     * @return drams removed
     */
    public static int empty(Player player, ItemStack stack, int drams) {
        EssenceContents contents = contents(stack);
        if (contents == null || !(stack.getItem() instanceof EssenceHolder holder) || !holder.canGive(stack)) {
            return 0;
        }
        int amount = Math.min(drams, contents.drams());
        ItemStack target = single(stack);
        setContents(target, contents.aspect(), contents.drams() - amount);
        giveBackIfSplit(player, stack, target);
        return amount;
    }

    /** The stack itself if it is a single item, otherwise one item split off it. */
    private static ItemStack single(ItemStack stack) {
        return stack.getCount() == 1 ? stack : stack.split(1);
    }

    /** Hands a split-off container to the player, only once it has been modified (or it would merge straight back). */
    private static void giveBackIfSplit(Player player, ItemStack original, ItemStack target) {
        if (target != original) {
            player.getInventory().placeItemBackInInventory(target);
        }
    }

    /** Right-clicking one container onto another in an inventory pours between them. */
    public static boolean handleStackedOnMe(ItemStack me, ItemStack carried, ClickAction action, Player player, SlotAccess carriedAccess) {
        if (action != ClickAction.SECONDARY || !(carried.getItem() instanceof EssenceHolder)) {
            return false;
        }
        boolean moved = contents(carried) != null
                ? pour(player, carried, me)
                : pour(player, me, carried);
        if (moved) {
            player.playSound(SoundEvents.BOTTLE_FILL, 0.6f, 1.4f);
        }
        return moved;
    }

    private static boolean pour(Player player, ItemStack from, ItemStack to) {
        EssenceContents source = contents(from);
        if (source == null || !(from.getItem() instanceof EssenceHolder holder) || !holder.canGive(from)) {
            return false;
        }
        int amount = Math.min(source.drams(), space(to, source.aspect()));
        if (amount <= 0) {
            return false;
        }
        fill(player, to, source.aspect(), amount);
        empty(player, from, amount);
        return true;
    }

    /**
     * Pours from a held container into a storage block, or fills the container from the block.
     *
     * @return whether anything moved
     */
    public static boolean exchangeWithStorage(Player player, ItemStack held, EssenceStorage storage, boolean allowPour, boolean allowDraw) {
        EssenceContents inHand = contents(held);
        if (allowPour && inHand != null && held.getItem() instanceof EssenceHolder holder && holder.canGive(held)) {
            int amount = storage.insert(inHand.aspect(), inHand.drams(), true);
            if (amount > 0) {
                empty(player, held, amount);
                storage.insert(inHand.aspect(), amount, false);
                player.level().playSound(null, player.blockPosition(), SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 0.8f, 0.8f);
                return true;
            }
        }
        EssenceContents stored = storage.contents();
        if (allowDraw && stored != null) {
            int amount = Math.min(space(held, stored.aspect()), storage.extract(Integer.MAX_VALUE, true));
            if (amount > 0) {
                storage.extract(amount, false);
                fill(player, held, stored.aspect(), amount);
                player.level().playSound(null, player.blockPosition(), SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 0.8f, 0.8f);
                return true;
            }
        }
        return false;
    }

    public static void appendContentsTooltip(ItemStack stack, int capacity, List<Component> tooltip) {
        EssenceContents contents = contents(stack);
        if (contents == null) {
            tooltip.add(Component.translatable("tooltip.chronogenesis.empty").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.chronogenesis.contents",
                    contents.aspect().displayName(), contents.drams(), capacity).withStyle(ChatFormatting.GRAY));
        }
    }

    public static int barWidth(ItemStack stack, int capacity) {
        return Mth.clamp(Math.round(13f * drams(stack) / capacity), 0, 13);
    }

    public static int barColor(ItemStack stack) {
        EssenceContents contents = contents(stack);
        return contents == null ? 0xFFFFFF : contents.aspect().color();
    }

    private EssenceItems() {}
}

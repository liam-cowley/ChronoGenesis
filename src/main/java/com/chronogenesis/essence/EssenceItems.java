package com.chronogenesis.essence;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.item.EssenceContainerItem;
import com.chronogenesis.registry.ModDataComponents;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Helpers for moving essence in and out of container items (syringe, vial, ampoule).
 * Filled containers are always handled one at a time: a stack of empty vials is split so a
 * single filled vial is produced.
 */
public final class EssenceItems {
    private EssenceItems() {}

    @Nullable
    public static EssenceContents get(ItemStack stack) {
        EssenceContents contents = stack.get(ModDataComponents.ESSENCE.get());
        return contents == null || contents.isEmpty() ? null : contents;
    }

    public static boolean isContainer(ItemStack stack) {
        return stack.getItem() instanceof EssenceContainerItem;
    }

    /** How many drams of {@code aspect} the (single) container could still take. */
    public static int space(ItemStack stack, Aspect aspect) {
        if (!(stack.getItem() instanceof EssenceContainerItem container) || !container.accepts(aspect)) {
            return 0;
        }
        EssenceContents contents = get(stack);
        if (contents == null) {
            return container.capacity();
        }
        return contents.aspect() == aspect ? container.capacity() - contents.amount() : 0;
    }

    public static void set(ItemStack stack, @Nullable EssenceContents contents) {
        if (contents == null || contents.isEmpty()) {
            stack.remove(ModDataComponents.ESSENCE.get());
        } else {
            stack.set(ModDataComponents.ESSENCE.get(), contents);
        }
    }

    /**
     * Pours up to {@code amount} drams into the container held in {@code hand}.
     * Returns how much went in.
     */
    public static int fillHeld(Player player, InteractionHand hand, Aspect aspect, int amount) {
        ItemStack held = player.getItemInHand(hand);
        int space = space(held, aspect);
        int moved = Math.min(space, amount);
        if (moved <= 0) {
            return 0;
        }
        EssenceContents current = get(held);
        EssenceContents result = new EssenceContents(aspect, (current == null ? 0 : current.amount()) + moved);
        if (held.getCount() == 1) {
            set(held, result);
        } else {
            ItemStack single = held.split(1);
            set(single, result);
            if (!player.getInventory().add(single)) {
                player.drop(single, false);
            }
        }
        return moved;
    }

    /**
     * Takes up to {@code amount} drams out of the container held in {@code hand}.
     * Returns what was taken, or null if nothing (or the wrong aspect) was there.
     */
    @Nullable
    public static EssenceContents drainHeld(Player player, InteractionHand hand, @Nullable Aspect required, int amount) {
        ItemStack held = player.getItemInHand(hand);
        EssenceContents current = get(held);
        if (current == null || amount <= 0 || (required != null && current.aspect() != required)) {
            return null;
        }
        int moved = Math.min(amount, current.amount());
        EssenceContents remaining = current.withAmount(current.amount() - moved);
        if (held.getCount() == 1) {
            set(held, remaining);
        } else {
            ItemStack single = held.split(1);
            set(single, remaining);
            if (!player.getInventory().add(single)) {
                player.drop(single, false);
            }
        }
        return current.withAmount(moved);
    }

    /**
     * Finds an inventory slot whose storage container (vial, ampoule; never a syringe) can take
     * some of {@code aspect}, or -1.
     */
    public static int findSlotWithSpace(Player player, Aspect aspect) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (isStorage(stack) && stack.getCount() == 1 && space(stack, aspect) > 0 && get(stack) != null) {
                return i; // prefer topping up a partly filled container
            }
        }
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (isStorage(stack) && space(stack, aspect) > 0) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isStorage(ItemStack stack) {
        return isContainer(stack) && !(stack.getItem() instanceof com.chronogenesis.item.ChronoSyringeItem);
    }

    /** Pours into the given inventory slot (splitting stacks as needed). Returns drams moved. */
    public static int fillSlot(Player player, int slot, Aspect aspect, int amount) {
        var inventory = player.getInventory();
        ItemStack stack = inventory.getItem(slot);
        int moved = Math.min(space(stack, aspect), amount);
        if (moved <= 0) {
            return 0;
        }
        EssenceContents current = get(stack);
        EssenceContents result = new EssenceContents(aspect, (current == null ? 0 : current.amount()) + moved);
        if (stack.getCount() == 1) {
            set(stack, result);
        } else {
            ItemStack single = stack.split(1);
            set(single, result);
            if (!inventory.add(single)) {
                player.drop(single, false);
            }
        }
        return moved;
    }

    /**
     * Pours as much as fits from one container into another (right-clicking one onto the other in an
     * inventory). Stacks are split so only a single container on each side changes. Returns whether
     * anything moved.
     */
    public static boolean pour(Player player, ItemStack source, ItemStack target) {
        EssenceContents contents = get(source);
        if (contents == null) {
            return false;
        }
        int moved = Math.min(contents.amount(), space(target, contents.aspect()));
        if (moved <= 0) {
            return false;
        }
        EssenceContents current = get(target);
        setSingle(player, target, new EssenceContents(contents.aspect(), (current == null ? 0 : current.amount()) + moved));
        setSingle(player, source, contents.withAmount(contents.amount() - moved));
        return true;
    }

    /** Sets the contents of one container of the stack, splitting it off into the inventory if needed. */
    private static void setSingle(Player player, ItemStack stack, EssenceContents result) {
        if (stack.getCount() == 1) {
            set(stack, result);
            return;
        }
        ItemStack single = stack.split(1);
        set(single, result);
        if (!player.getInventory().add(single)) {
            player.drop(single, false);
        }
    }
}

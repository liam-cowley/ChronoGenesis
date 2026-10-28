package com.chronogenesis.item;

import java.util.List;

import com.chronogenesis.essence.Aspect;
import com.chronogenesis.journal.JournalUnlock;
import com.chronogenesis.essence.EssenceContents;
import com.chronogenesis.essence.EssenceItems;
import com.chronogenesis.lens.TemporalLensItem;
import com.chronogenesis.player.PlayerEssence;
import com.chronogenesis.player.Power;
import com.chronogenesis.player.Powers;
import com.chronogenesis.registry.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Creative-only testing aid (no recipe; found only in the creative tab). While it is anywhere in a player's
 * inventory, every power is at its highest level with a full pool, Temporal Exposure stays at zero,
 * instruments stay charged (every Temporal Lens carried is kept full of Time and costs nothing to look through),
 * and research at a Chronocortex draws its essence from the hourglass instead of wired organisms.
 */
public class EternalHourglassItem extends Item {
    private static final int INTERVAL = 10;

    public EternalHourglassItem(Properties properties) {
        super(properties);
    }

    /** Whether the player carries an Eternal Hourglass. */
    public static boolean carried(Player player) {
        return player.getInventory().contains(stack -> stack.is(ModItems.ETERNAL_HOURGLASS.get()));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (entity instanceof ServerPlayer player && player.tickCount % INTERVAL == 0) {
            saturate(player);
        }
    }

    /** Fills every pool at the highest power level, teaches every composite power, clears Exposure, and charges every Lens carried. */
    public static void saturate(ServerPlayer player) {
        PlayerEssence before = Powers.get(player);
        PlayerEssence essence = before;
        for (Aspect aspect : Aspect.values()) {
            if (essence.level(aspect) < Powers.MAX_LEVEL) {
                essence = essence.withLevel(aspect, Powers.MAX_LEVEL);
            }
            essence = essence.withPool(aspect, essence.max(aspect));
        }
        for (Power power : Power.values()) {
            if (power.isComposite()) {
                essence = essence.withComposite(power);
            }
        }
        essence = essence.withExposure(0);
        if (!essence.equals(before)) {
            Powers.set(player, essence);
            Powers.applyModifiers(player);
        }
        for (ItemStack carried : player.getInventory().items) {
            charge(carried);
        }
        player.getInventory().offhand.forEach(EternalHourglassItem::charge);
    }

    private static void charge(ItemStack stack) {
        if (stack.getItem() instanceof TemporalLensItem) {
            EssenceContents own = EssenceItems.get(stack);
            if (own == null || own.amount() < TemporalLensItem.CAPACITY) {
                EssenceItems.set(stack, new EssenceContents(Aspect.TIME, TemporalLensItem.CAPACITY));
            }
        }
    }

    /** In creative mode, using it unlocks the whole journal. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            if (serverPlayer.isCreative()) {
                JournalUnlock.unlockAll(serverPlayer);
            } else {
                player.displayClientMessage(Component.translatable("message.chronogenesis.journal.creative_only")
                        .withStyle(ChatFormatting.GRAY), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.chronogenesis.eternal_hourglass").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("tooltip.chronogenesis.eternal_hourglass.journal").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.chronogenesis.eternal_hourglass.creative").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}

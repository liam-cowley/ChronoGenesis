package com.chronogenesis.item;

import java.util.List;

import com.chronogenesis.timetravel.TemporalShift;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * The Chrono Tether: a brass pocket watch on a cord of living vein, its other end still in the present. A traveller
 * needs one to be shifted at all (without it there is no way back), and pulling on it (using it) in another era
 * reels them home early.
 */
public class ChronoTetherItem extends Item {
    public ChronoTetherItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            if (TemporalShift.get(serverPlayer).active()) {
                TemporalShift.returnHome(serverPlayer, "message.chronogenesis.shift.reeled_back");
            } else {
                player.displayClientMessage(Component.translatable("message.chronogenesis.shift.slack")
                        .withStyle(ChatFormatting.GRAY), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.chronogenesis.chrono_tether").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.chronogenesis.chrono_tether.use").withStyle(ChatFormatting.DARK_GRAY));
    }
}

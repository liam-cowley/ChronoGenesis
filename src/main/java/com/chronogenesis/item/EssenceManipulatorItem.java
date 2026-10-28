package com.chronogenesis.item;

import java.util.List;

import com.chronogenesis.player.Powers;

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
 * The Essence Manipulator: a brass-knuckled leather glove with living tendrils inside, wired into the wearer's
 * veins. It steers the powers.
 * <ul>
 *   <li>Right-click: use the selected active power (Rewind, Slow Time...).</li>
 *   <li>Shift + R (carried anywhere): the power wheel. Point at an active power, or scroll through them, and let go
 *       of Shift to choose it. Click a passive power to switch it on or off; right-click any power to change its
 *       strength.</li>
 *   <li>Shift + scroll (in hand): the selected power's strength.</li>
 * </ul>
 * A hand in the glove still counts as a bare fist for Iron Fists.
 */
public class EssenceManipulatorItem extends Item {
    private static final int COOLDOWN = 8;

    public EssenceManipulatorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            Powers.use(serverPlayer);
            player.getCooldowns().addCooldown(this, COOLDOWN);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.chronogenesis.essence_manipulator").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.chronogenesis.essence_manipulator.use").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.chronogenesis.essence_manipulator.wheel").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.chronogenesis.essence_manipulator.strength").withStyle(ChatFormatting.DARK_GRAY));
    }
}

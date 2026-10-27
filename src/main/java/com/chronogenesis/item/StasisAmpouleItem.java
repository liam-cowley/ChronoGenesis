package com.chronogenesis.item;

import java.util.List;

import com.chronogenesis.essence.Aspect;
import com.chronogenesis.essence.EssenceContents;
import com.chronogenesis.essence.EssenceItems;
import com.chronogenesis.player.Powers;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * A vial lined with phantom membrane: the only early container that can hold Time.
 * Use it to inject the Time into yourself (unlocks the Time power, raises Temporal Exposure).
 */
public class StasisAmpouleItem extends EssenceContainerItem {
    public static final int CAPACITY = 4;

    public StasisAmpouleItem(Properties properties) {
        super(properties, CAPACITY, aspect -> aspect == Aspect.TIME);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        EssenceContents contents = EssenceItems.get(stack);
        if (contents == null) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide()) {
            int injected = Powers.injectTime(player, contents.amount());
            if (injected > 0) {
                EssenceItems.drainHeld(player, hand, Aspect.TIME, injected);
                level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 0.5f);
            } else {
                player.displayClientMessage(Component.translatable("message.chronogenesis.gland.full")
                        .withStyle(ChatFormatting.GRAY), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.chronogenesis.ampoule").withStyle(ChatFormatting.DARK_GRAY));
    }
}

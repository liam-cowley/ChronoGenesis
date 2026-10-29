package com.chronogenesis.item;

import java.util.List;

import com.chronogenesis.player.PlayerGraft;
import com.chronogenesis.player.PlayerGrafts;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * A piece of the Unbegotten, still alive. Held to the body (use it, and hold) it takes: it is grown in for good
 * ({@link PlayerGrafts}). Only a Dissection Table and Time cut it out again.
 */
public class PlayerGraftItem extends Item {
    private final PlayerGraft graft;

    public PlayerGraftItem(PlayerGraft graft, Properties properties) {
        super(properties);
        this.graft = graft;
    }

    public PlayerGraft graft() {
        return graft;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (PlayerGrafts.has(player, graft)) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.chronogenesis.player_graft.already").withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player && PlayerGrafts.graft(player, graft)) {
            stack.consume(1, player);
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 60;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.chronogenesis.player_graft." + graft.getSerializedName()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.chronogenesis.player_graft").withStyle(ChatFormatting.DARK_PURPLE));
    }
}

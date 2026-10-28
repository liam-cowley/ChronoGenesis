package com.chronogenesis.temple;

import java.util.List;

import com.chronogenesis.essence.Aspect;
import com.chronogenesis.player.PlayerEssence;
import com.chronogenesis.player.Powers;
import com.chronogenesis.player.TemporalExposure;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * A Stilled Core, taken from a temple once its seals were broken: all the time it held still, packed tight.
 * Crushed in the hand it floods the holder's Time pool (filling it, at their level), at a cost in Exposure.
 */
public class StilledCoreItem extends Item {
    private static final int EXPOSURE = 30;

    public StilledCoreItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            PlayerEssence essence = Powers.get(serverPlayer);
            if (essence.level(Aspect.TIME) == 0) {
                player.displayClientMessage(Component.translatable("message.chronogenesis.stilled_core.no_time").withStyle(ChatFormatting.GRAY), true);
                return InteractionResultHolder.fail(stack);
            }
            Powers.set(serverPlayer, essence.withPool(Aspect.TIME, essence.max(Aspect.TIME)));
            TemporalExposure.expose(serverPlayer, EXPOSURE);
            stack.consume(1, player);
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 1.0f, 0.5f);
            player.displayClientMessage(Component.translatable("message.chronogenesis.stilled_core.used").withStyle(ChatFormatting.DARK_AQUA), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.chronogenesis.stilled_core").withStyle(ChatFormatting.GRAY));
    }
}

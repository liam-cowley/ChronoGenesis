package com.chronogenesis.item;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.Config;
import com.chronogenesis.essence.Aspect;
import com.chronogenesis.essence.EssenceContents;
import com.chronogenesis.essence.EssenceDrain;
import com.chronogenesis.essence.Needle;
import com.chronogenesis.registry.ModDataComponents;
import com.chronogenesis.registry.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * The Chrono Syringe: copper and glass, holds 4 drams of one stable aspect. Which aspect it draws depends
 * on the fitted {@link Needle}. The amethyst needle draws Time, which glass cannot hold, so it goes
 * straight into a Stasis Ampoule from the player's inventory.
 */
public class ChronoSyringeItem extends EssenceContainerItem {
    public static final int CAPACITY = 4;

    public ChronoSyringeItem(Properties properties) {
        super(properties, CAPACITY, EssenceContainerItem::stableOnly);
    }

    @Nullable
    public static Needle needle(ItemStack stack) {
        return stack.get(ModDataComponents.NEEDLE);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof Player) {
            return InteractionResult.PASS;
        }
        if (player.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Needle needle = needle(stack);
        if (needle == null) {
            return fail(player, "no_needle");
        }
        Aspect aspect = needle.aspect();
        if (aspect == Aspect.TIME) {
            return drawTime(player, target);
        }
        EssenceContents contents = EssenceItems.contents(stack);
        if (contents != null && contents.aspect() != aspect) {
            return fail(player, "mixed");
        }
        if (EssenceItems.space(stack, aspect) <= 0) {
            return fail(player, "full");
        }
        int taken = EssenceDrain.drain(target, aspect, 1);
        if (taken <= 0) {
            return fail(player, "nothing_left");
        }
        EssenceItems.fill(player, stack, aspect, taken);
        afterDraw(player, target);
        return InteractionResult.SUCCESS;
    }

    private InteractionResult drawTime(Player player, LivingEntity target) {
        ItemStack ampoule = findAmpoule(player);
        if (ampoule == null) {
            return fail(player, "needs_ampoule");
        }
        int taken = EssenceDrain.drain(target, Aspect.TIME, 1);
        if (taken <= 0) {
            return fail(player, "no_time");
        }
        EssenceItems.fill(player, ampoule, Aspect.TIME, taken);
        afterDraw(player, target);
        return InteractionResult.SUCCESS;
    }

    @Nullable
    private static ItemStack findAmpoule(Player player) {
        for (ItemStack candidate : player.getInventory().items) {
            if (candidate.is(ModItems.STASIS_AMPOULE.get()) && EssenceItems.space(candidate, Aspect.TIME) > 0) {
                return candidate;
            }
        }
        ItemStack offhand = player.getOffhandItem();
        return offhand.is(ModItems.STASIS_AMPOULE.get()) && EssenceItems.space(offhand, Aspect.TIME) > 0 ? offhand : null;
    }

    private void afterDraw(Player player, LivingEntity target) {
        player.getCooldowns().addCooldown(this, Config.SYRINGE_COOLDOWN_TICKS.get());
        player.level().playSound(null, target.blockPosition(), SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 0.7f, 1.6f);
    }

    private static InteractionResult fail(Player player, String reason) {
        player.displayClientMessage(Component.translatable("message.chronogenesis.syringe." + reason), true);
        return InteractionResult.CONSUME;
    }

    /** Sneak-use squirts the contents out. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        EssenceContents contents = EssenceItems.contents(stack);
        if (!player.isShiftKeyDown() || contents == null) {
            return InteractionResultHolder.pass(stack);
        }
        if (level instanceof ServerLevel serverLevel) {
            var look = player.getLookAngle();
            serverLevel.sendParticles(EssenceDrain.dust(contents.aspect()),
                    player.getX() + look.x, player.getEyeY() + look.y - 0.2, player.getZ() + look.z,
                    12, 0.15, 0.15, 0.15, 0);
            level.playSound(null, player.blockPosition(), SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 0.7f, 1.5f);
        }
        EssenceItems.setContents(stack, null, 0);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** Right-click a needle onto the syringe to fit it (the old one comes back); right-click with an empty cursor to pull it. */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack carried, Slot slot, ClickAction action, Player player, SlotAccess access) {
        if (action == ClickAction.SECONDARY && stack.getCount() == 1) {
            Needle fitted = needle(stack);
            if (carried.getItem() instanceof NeedleItem needleItem) {
                ItemStack old = fitted == null ? ItemStack.EMPTY : new ItemStack(ModItems.needle(fitted).get());
                stack.set(ModDataComponents.NEEDLE, needleItem.needle());
                carried.shrink(1);
                if (carried.isEmpty()) {
                    access.set(old);
                } else if (!old.isEmpty()) {
                    player.getInventory().placeItemBackInInventory(old);
                }
                player.playSound(SoundEvents.CHAIN_PLACE, 0.6f, 1.6f);
                return true;
            }
            if (carried.isEmpty() && fitted != null) {
                stack.remove(ModDataComponents.NEEDLE);
                access.set(new ItemStack(ModItems.needle(fitted).get()));
                player.playSound(SoundEvents.CHAIN_BREAK, 0.6f, 1.6f);
                return true;
            }
        }
        return super.overrideOtherStackedOnMe(stack, carried, slot, action, player, access);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Needle needle = needle(stack);
        tooltip.add(needle == null
                ? Component.translatable("tooltip.chronogenesis.syringe.no_needle").withStyle(ChatFormatting.RED)
                : Component.translatable("tooltip.chronogenesis.syringe.needle",
                        Component.translatable(ModItems.needle(needle).get().getDescriptionId())).withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}

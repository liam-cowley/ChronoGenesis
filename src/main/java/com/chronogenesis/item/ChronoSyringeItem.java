package com.chronogenesis.item;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.essence.Aspect;
import com.chronogenesis.essence.EssenceContents;
import com.chronogenesis.essence.EssenceItems;
import com.chronogenesis.essence.MobEssence;
import com.chronogenesis.registry.ModDataComponents;
import com.chronogenesis.registry.ModItems;
import com.chronogenesis.time.TemporalStress;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * The Chrono Syringe: copper and glass with a swappable needle.
 * <ul>
 *   <li>Use on a mob: draw 1 dram of the needle's aspect into the syringe (max 4).</li>
 *   <li>Amethyst needle: Time goes straight into a Stasis Ampoule in your inventory. Without one,
 *       the syringe's glass ages to sand.</li>
 *   <li>Use in the air: empty the syringe into a vial in your inventory.</li>
 *   <li>Sneak-use with a needle in the off hand: swap needles.</li>
 * </ul>
 */
public class ChronoSyringeItem extends EssenceContainerItem {
    public static final int CAPACITY = 4;
    private static final int COOLDOWN_TICKS = 10;

    public ChronoSyringeItem(Properties properties) {
        super(properties, CAPACITY, Aspect::fitsInGlass);
    }

    @Nullable
    public static Aspect needle(ItemStack syringe) {
        return syringe.get(ModDataComponents.ASPECT.get());
    }

    /**
     * Draws from a creature. Called from {@link com.chronogenesis.essence.SyringeEvents} before the
     * creature's own interaction (so villagers' trade screens don't swallow the click).
     */
    public InteractionResult drawFrom(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        Aspect aspect = needle(stack);
        if (aspect == null) {
            message(player, "message.chronogenesis.syringe.no_needle");
            return InteractionResult.FAIL;
        }
        if (!MobEssence.canBeDrained(target)) {
            return InteractionResult.PASS;
        }
        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (MobEssence.remaining(target, aspect) <= 0) {
            message(player, "message.chronogenesis.syringe.nothing_left");
            return InteractionResult.FAIL;
        }

        if (aspect == Aspect.TIME) {
            return drawTime(stack, player, target, hand);
        }

        if (EssenceItems.space(stack, aspect) <= 0) {
            message(player, "message.chronogenesis.syringe.full");
            return InteractionResult.FAIL;
        }
        int taken = MobEssence.drain(target, aspect, 1);
        if (taken > 0) {
            EssenceContents current = EssenceItems.get(stack);
            EssenceItems.set(stack, new EssenceContents(aspect, (current == null ? 0 : current.amount()) + taken));
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        }
        return InteractionResult.SUCCESS;
    }

    /** Time cannot touch ordinary glass: it must go straight into a Stasis Ampoule. */
    private InteractionResult drawTime(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        int slot = EssenceItems.findSlotWithSpace(player, Aspect.TIME);
        if (slot < 0) {
            // The glass ages to sand in an instant. The needle survives.
            ServerLevel level = (ServerLevel) player.level();
            level.playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8f, 0.6f);
            message(player, "message.chronogenesis.syringe.glass_aged");
            player.setItemInHand(hand, new ItemStack(Items.SAND));
            give(player, new ItemStack(ModItems.AMETHYST_NEEDLE.get()));
            return InteractionResult.CONSUME;
        }
        int taken = MobEssence.drain(target, Aspect.TIME, 1);
        if (taken > 0) {
            EssenceItems.fillSlot(player, slot, Aspect.TIME, taken);
            TemporalStress.add((ServerLevel) player.level(), target.blockPosition(), taken * TemporalStress.PER_DRAM_EXTRACTED);
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS * 2);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);

        if (player.isSecondaryUseActive() && other.getItem() instanceof NeedleItem newNeedle) {
            if (!level.isClientSide()) {
                swapNeedle(player, stack, other, newNeedle);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        EssenceContents contents = EssenceItems.get(stack);
        if (contents == null) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide()) {
            int slot = EssenceItems.findSlotWithSpace(player, contents.aspect());
            if (slot < 0) {
                message(player, "message.chronogenesis.syringe.no_vial");
                return InteractionResultHolder.fail(stack);
            }
            int moved = EssenceItems.fillSlot(player, slot, contents.aspect(), contents.amount());
            EssenceItems.set(stack, contents.withAmount(contents.amount() - moved));
            level.playSound(null, player.blockPosition(), SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 0.6f, 1.2f);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private void swapNeedle(Player player, ItemStack syringe, ItemStack needleStack, NeedleItem newNeedle) {
        Aspect old = needle(syringe);
        if (old == newNeedle.aspect()) {
            return;
        }
        EssenceContents contents = EssenceItems.get(syringe);
        if (contents != null) {
            message(player, "message.chronogenesis.syringe.empty_first");
            return;
        }
        needleStack.shrink(1);
        syringe.set(ModDataComponents.ASPECT.get(), newNeedle.aspect());
        if (old != null) {
            give(player, new ItemStack(needleFor(old)));
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 0.6f, 1.6f);
    }

    public static Item needleFor(Aspect aspect) {
        return switch (aspect) {
            case DENSITY -> ModItems.IRON_NEEDLE.get();
            case SPEED -> ModItems.GOLD_NEEDLE.get();
            case SIZE -> ModItems.BONE_NEEDLE.get();
            case TIME -> ModItems.AMETHYST_NEEDLE.get();
            case AWARENESS -> Items.AIR; // no needle can hold Awareness
        };
    }

    private static void give(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private static void message(Player player, String key) {
        if (player.level().isClientSide()) {
            return;
        }
        player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.GRAY), true);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Aspect aspect = needle(stack);
        if (aspect == null) {
            tooltip.add(Component.translatable("tooltip.chronogenesis.no_needle").withStyle(ChatFormatting.DARK_RED));
        } else {
            tooltip.add(Component.translatable("tooltip.chronogenesis.needle", aspect.displayName())
                    .withStyle(style -> style.withColor(aspect.color())));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }
}

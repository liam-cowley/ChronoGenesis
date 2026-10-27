package com.chronogenesis.item;

import java.util.List;

import com.chronogenesis.essence.Aspect;
import com.chronogenesis.essence.EssenceContents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * A vial lined with phantom membrane: the only glass that holds Time. It is not a good seal, so a full
 * ampoule slowly leaks if it is left in the inventory.
 */
public class StasisAmpouleItem extends EssenceContainerItem {
    public static final int CAPACITY = 3;
    /** Average ticks between leaking one dram (two in-game minutes). */
    private static final int LEAK_CHANCE = 2400;

    public StasisAmpouleItem(Properties properties) {
        super(properties, CAPACITY, aspect -> aspect == Aspect.TIME);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        EssenceContents contents = EssenceItems.contents(stack);
        if (level.isClientSide || contents == null || level.random.nextInt(LEAK_CHANCE) != 0) {
            return;
        }
        EssenceItems.setContents(stack, contents.aspect(), contents.drams() - 1);
        level.playSound(null, entity.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.3f, 1.8f);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (EssenceItems.contents(stack) != null) {
            tooltip.add(Component.translatable("tooltip.chronogenesis.stasis_ampoule.leaks").withStyle(ChatFormatting.DARK_AQUA));
        }
    }
}

package com.chronogenesis.item;

import java.util.List;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.registry.ModArmorMaterials;
import com.chronogenesis.registry.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * A piece of the Chrono-Insulated Suit. Worn, it is drawn as its own 3D model ({@code client/SuitModel}): smoked-glass
 * goggles on a leather cap, a lab coat with a Time canister on its back, belted trousers, rubber galoshes.
 * <p>
 * Infused with Time at an Enhancing Gland it becomes <b>Time-warded</b> ({@link ModDataComponents#TEMPORAL_WARD}):
 * tendrils grow out of its seams, and a traveller warded head to foot shifts between eras properly (see
 * {@code TemporalShift}).
 */
public class ChronoSuitItem extends ArmorItem {
    private static final ResourceLocation TEXTURE = ChronoGenesis.id("textures/models/armor/chrono_suit.png");

    public ChronoSuitItem(Type type, Properties properties) {
        super(ModArmorMaterials.CHRONO_INSULATED, type, properties);
    }

    public static boolean warded(ItemStack stack) {
        return stack.has(ModDataComponents.TEMPORAL_WARD.get());
    }

    public static void ward(ItemStack stack) {
        stack.set(ModDataComponents.TEMPORAL_WARD.get(), Unit.INSTANCE);
    }

    /** One texture for every piece: the 3D models lay their own UVs out on it. */
    @Override
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer,
            boolean innerModel) {
        return TEXTURE;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (warded(stack)) {
            tooltip.add(Component.translatable("tooltip.chronogenesis.suit.warded").withStyle(ChatFormatting.DARK_PURPLE));
        } else {
            tooltip.add(Component.translatable("tooltip.chronogenesis.suit.unwarded").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}

package com.chronogenesis.registry;

import java.util.EnumMap;
import java.util.List;

import com.chronogenesis.ChronoGenesis;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, ChronoGenesis.MODID);

    /**
     * The Chrono-Insulated Suit: leather lined with phantom membrane, brass fittings. About as
     * protective as leather; what it really keeps out is Time (see TemporalExposure).
     * Worn textures: textures/models/armor/chrono_insulated_layer_1.png and _layer_2.png.
     */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> CHRONO_INSULATED = ARMOR_MATERIALS.register("chrono_insulated",
            () -> new ArmorMaterial(
                    new EnumMap<>(java.util.Map.of(
                            ArmorItem.Type.HELMET, 1,
                            ArmorItem.Type.CHESTPLATE, 3,
                            ArmorItem.Type.LEGGINGS, 2,
                            ArmorItem.Type.BOOTS, 1,
                            ArmorItem.Type.BODY, 3)),
                    15,
                    SoundEvents.ARMOR_EQUIP_LEATHER,
                    () -> Ingredient.of(net.minecraft.world.item.Items.PHANTOM_MEMBRANE),
                    List.of(new ArmorMaterial.Layer(ChronoGenesis.id("chrono_insulated"))),
                    0.0f,
                    0.0f));

    private ModArmorMaterials() {}
}

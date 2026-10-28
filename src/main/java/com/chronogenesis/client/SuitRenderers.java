package com.chronogenesis.client;

import java.util.EnumMap;
import java.util.Map;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.item.ChronoSuitItem;
import com.chronogenesis.registry.ModItems;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * The Chrono-Insulated Suit is worn as its 3D models ({@link SuitModel}), tendrils and all once warded; its icons
 * switch to their warded versions (item property {@code chronogenesis:warded}).
 */
@EventBusSubscriber(modid = ChronoGenesis.MODID, value = Dist.CLIENT)
public final class SuitRenderers {
    private SuitRenderers() {}

    @SubscribeEvent
    static void clientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private final Map<EquipmentSlot, SuitModel> models = new EnumMap<>(EquipmentSlot.class);

            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot,
                    HumanoidModel<?> original) {
                return models.computeIfAbsent(slot, SuitModel::new);
            }

            @Override
            public void setupModelAnimations(LivingEntity entity, ItemStack stack, EquipmentSlot slot, Model model,
                    float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw,
                    float headPitch) {
                if (model instanceof SuitModel suit) {
                    // Each wearer writhes on its own beat.
                    suit.animate(ageInTicks + (entity.getId() % 64) * 7.0f, ChronoSuitItem.warded(stack));
                }
            }
        }, ModItems.CHRONO_GOGGLES.get(), ModItems.CHRONO_LAB_COAT.get(), ModItems.CHRONO_TROUSERS.get(),
                ModItems.CHRONO_GALOSHES.get());
    }

    @SubscribeEvent
    static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            for (Item item : new Item[] {ModItems.CHRONO_GOGGLES.get(), ModItems.CHRONO_LAB_COAT.get(),
                    ModItems.CHRONO_TROUSERS.get(), ModItems.CHRONO_GALOSHES.get()}) {
                ItemProperties.register(item, ChronoGenesis.id("warded"),
                        (stack, level, entity, seed) -> ChronoSuitItem.warded(stack) ? 1.0f : 0.0f);
            }
        });
    }
}

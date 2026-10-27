package com.chronogenesis.client;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.essence.EssenceContents;
import com.chronogenesis.essence.EssenceJugBlockEntity;
import com.chronogenesis.essence.Needle;
import com.chronogenesis.item.ChronoSyringeItem;
import com.chronogenesis.item.EssenceItems;
import com.chronogenesis.registry.ModBlocks;
import com.chronogenesis.registry.ModItems;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/** Liquid tints for essence containers, and the syringe's needle model switch. */
@EventBusSubscriber(modid = ChronoGenesis.MODID, value = Dist.CLIENT)
public final class ModClientEvents {
    /** Tint of the liquid layer when a container is empty: just cloudy glass. */
    private static final int EMPTY_GLASS = 0xD8E4EA;

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // 0 = no needle, then one step per needle in declaration order (matches the model overrides).
        event.enqueueWork(() -> ItemProperties.register(ModItems.CHRONO_SYRINGE.get(), ChronoGenesis.id("needle"),
                (stack, level, entity, seed) -> {
                    Needle needle = ChronoSyringeItem.needle(stack);
                    return needle == null ? 0 : (needle.ordinal() + 1) / (float) Needle.values().length;
                }));
    }

    @SubscribeEvent
    static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> tintIndex == 0 ? FastColor.ARGB32.opaque(liquidColor(stack)) : -1,
                ModItems.CHRONO_SYRINGE.get(), ModItems.ESSENCE_VIAL.get(), ModItems.STASIS_AMPOULE.get(),
                ModItems.CULTURE_JAR.get(), ModItems.ESSENCE_JUG.get());
    }

    @SubscribeEvent
    static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> {
            if (tintIndex == 0 && level != null && pos != null
                    && level.getBlockEntity(pos) instanceof EssenceJugBlockEntity jug && jug.contents() != null) {
                return FastColor.ARGB32.opaque(jug.contents().aspect().color());
            }
            return FastColor.ARGB32.opaque(EMPTY_GLASS);
        }, ModBlocks.ESSENCE_JUG.get());
    }

    private static int liquidColor(ItemStack stack) {
        EssenceContents contents = EssenceItems.contents(stack);
        return contents == null ? EMPTY_GLASS : contents.aspect().color();
    }

    private ModClientEvents() {}
}

package com.chronogenesis.registry;

import java.util.List;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.essence.Aspect;
import com.chronogenesis.essence.EssenceContents;
import com.chronogenesis.item.ChronoSyringeItem;
import com.chronogenesis.item.CultureItem;
import com.chronogenesis.item.EssenceContainerItem;
import com.chronogenesis.lore.PrecursorLore;
import com.chronogenesis.lore.PrecursorTabletItem;
import com.chronogenesis.organism.EssenceJugBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The ChronoGenesis tab lists every item in registration order, expanded into useful variants:
 * an empty and a full container for each essence it can hold, a syringe for each needle, and a
 * culture of each aspect. Brass also appears in vanilla's Ingredients and Building Blocks tabs.
 */
@EventBusSubscriber(modid = ChronoGenesis.MODID)
public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ChronoGenesis.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = CREATIVE_MODE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.chronogenesis"))
                    .icon(() -> new ItemStack(ModItems.CHRONO_SYRINGE.get()))
                    .displayItems((parameters, output) -> ModItems.ITEMS.getEntries()
                            .forEach(item -> output.acceptAll(variants(item.get()))))
                    .build());

    /** What a single item expands to in the tab. */
    private static List<ItemStack> variants(Item item) {
        List<ItemStack> stacks = new java.util.ArrayList<>();
        stacks.add(new ItemStack(item));
        if (item instanceof ChronoSyringeItem) {
            for (Aspect aspect : Aspect.values()) {
                if (aspect != Aspect.AWARENESS) {
                    ItemStack syringe = new ItemStack(item);
                    syringe.set(ModDataComponents.ASPECT.get(), aspect);
                    stacks.add(syringe);
                }
            }
        } else if (item instanceof EssenceContainerItem container) {
            for (Aspect aspect : Aspect.values()) {
                if (container.accepts(aspect)) {
                    stacks.add(filled(item, aspect, container.capacity()));
                }
            }
        } else if (item == ModItems.ESSENCE_JUG.get()) {
            for (Aspect aspect : Aspect.values()) {
                if (aspect.fitsInGlass()) {
                    stacks.add(filled(item, aspect, EssenceJugBlockEntity.CAPACITY));
                }
            }
        } else if (item instanceof PrecursorTabletItem) {
            stacks.clear();
            PrecursorLore.FRAGMENTS.forEach(fragment -> stacks.add(PrecursorTabletItem.of(fragment)));
        } else if (item instanceof CultureItem) {
            stacks.clear();
            for (Aspect aspect : Aspect.values()) {
                stacks.add(CultureItem.of(aspect));
            }
        }
        return stacks;
    }

    private static ItemStack filled(Item item, Aspect aspect, int amount) {
        ItemStack stack = new ItemStack(item);
        stack.set(ModDataComponents.ESSENCE.get(), new EssenceContents(aspect, amount));
        return stack;
    }

    @SubscribeEvent
    static void addToVanillaTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.BRASS_BLEND.get());
            event.accept(ModItems.BRASS_INGOT.get());
            event.accept(ModItems.BRASS_NUGGET.get());
        } else if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(ModItems.BRASS_BLOCK.get());
        }
    }

    private ModCreativeTabs() {}
}

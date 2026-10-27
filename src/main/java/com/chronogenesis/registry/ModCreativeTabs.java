package com.chronogenesis.registry;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.essence.Aspect;
import com.chronogenesis.item.CultureItem;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ChronoGenesis.MODID);

    /** Every ChronoGenesis item in registration order; cultures are listed once per aspect. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = CREATIVE_MODE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.chronogenesis"))
                    .icon(() -> new ItemStack(ModItems.CHRONO_SYRINGE.get()))
                    .displayItems((parameters, output) -> ModItems.ITEMS.getEntries().forEach(item -> {
                        if (item.get() == ModItems.CULTURE.get()) {
                            for (Aspect aspect : Aspect.values()) {
                                if (aspect != Aspect.TIME) {
                                    output.accept(CultureItem.of(aspect));
                                }
                            }
                        } else {
                            output.accept(item.get());
                        }
                    }))
                    .build());

    private ModCreativeTabs() {}
}

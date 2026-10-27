package com.chronogenesis;

import org.slf4j.Logger;

import com.chronogenesis.registry.ModAttachments;
import com.chronogenesis.registry.ModBlockEntities;
import com.chronogenesis.registry.ModBlocks;
import com.chronogenesis.registry.ModCreativeTabs;
import com.chronogenesis.registry.ModDataComponents;
import com.chronogenesis.registry.ModItems;
import com.mojang.logging.LogUtils;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

/**
 * ChronoGenesis entry point.
 * <p>
 * Every registry lives in {@code com.chronogenesis.registry} and is wired up here.
 * Keep this class thin: no gameplay logic, only registration.
 */
@Mod(ChronoGenesis.MODID)
public class ChronoGenesis {
    public static final String MODID = "chronogenesis";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ChronoGenesis(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    /** Shorthand for {@code chronogenesis:<path>}. */
    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}

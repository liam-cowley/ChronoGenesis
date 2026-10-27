package com.chronogenesis;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Client-only entry point. Never loaded on a dedicated server, so client classes
 * (renderers, screens, Minecraft.getInstance()) are safe to reference here and in
 * {@code com.chronogenesis.client}.
 */
@Mod(value = ChronoGenesis.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = ChronoGenesis.MODID, value = Dist.CLIENT)
public class ChronoGenesisClient {
    public ChronoGenesisClient(ModContainer container) {
        // Config screen: Mods menu > ChronoGenesis > Config
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        ChronoGenesis.LOGGER.info("ChronoGenesis client setup complete");
    }
}

package com.chronogenesis.registry;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.essence.EssenceProfile;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

/** Custom datapack registries, so content can be tweaked by packs. */
@EventBusSubscriber(modid = ChronoGenesis.MODID)
public final class ModRegistries {
    /** {@code data/<ns>/chronogenesis/essence_profile/<entity>.json}, keyed by entity type id. Server-side only. */
    public static final ResourceKey<Registry<EssenceProfile>> ESSENCE_PROFILE =
            ResourceKey.createRegistryKey(ChronoGenesis.id("essence_profile"));

    @SubscribeEvent
    static void registerDatapackRegistries(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(ESSENCE_PROFILE, EssenceProfile.CODEC);
    }

    private ModRegistries() {}
}

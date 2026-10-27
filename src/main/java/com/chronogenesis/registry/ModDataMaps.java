package com.chronogenesis.registry;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.essence.EssenceProfile;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/** Data maps: per-registry-entry data loaded from datapacks. */
@EventBusSubscriber(modid = ChronoGenesis.MODID)
public final class ModDataMaps {
    /** {@code data/<ns>/data_maps/entity_type/essence_profile.json}: drams of each aspect per mob type. */
    public static final DataMapType<EntityType<?>, EssenceProfile> ESSENCE_PROFILE = DataMapType
            .builder(ChronoGenesis.id("essence_profile"), Registries.ENTITY_TYPE, EssenceProfile.CODEC)
            .build();

    @SubscribeEvent
    static void register(RegisterDataMapTypesEvent event) {
        event.register(ESSENCE_PROFILE);
    }

    private ModDataMaps() {}
}

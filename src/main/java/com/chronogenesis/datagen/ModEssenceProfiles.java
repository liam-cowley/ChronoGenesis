package com.chronogenesis.datagen;

import java.util.EnumMap;
import java.util.Map;

import com.chronogenesis.essence.Aspect;
import com.chronogenesis.essence.EssenceProfile;
import com.chronogenesis.registry.ModRegistries;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;

/**
 * Hand-tuned essence profiles for the creatures the early game revolves around. Everything else uses the
 * defaults derived from attributes in {@link EssenceProfile}.
 */
final class ModEssenceProfiles {
    static void bootstrap(BootstrapContext<EssenceProfile> context) {
        // Farm animals: the first experiments. Cows are heavy, chickens quick and small.
        register(context, EntityType.COW, 8, 5, 7, 4, 0);
        register(context, EntityType.PIG, 7, 5, 5, 3, 0);
        register(context, EntityType.SHEEP, 6, 5, 6, 3, 0);
        register(context, EntityType.CHICKEN, 2, 6, 1, 2, 0);
        register(context, EntityType.VILLAGER, 6, 4, 5, 10, 0);
        // Time bearers: still only a trickle.
        register(context, EntityType.PHANTOM, 3, 10, 4, 6, 4);
        register(context, EntityType.ENDERMAN, 8, 10, 9, 8, 3);
        register(context, EntityType.ZOMBIE, 9, 6, 5, 8, 2);
    }

    private static void register(BootstrapContext<EssenceProfile> context, EntityType<?> type,
                                 int density, int speed, int size, int awareness, int time) {
        Map<Aspect, Integer> drams = new EnumMap<>(Aspect.class);
        drams.put(Aspect.DENSITY, density);
        drams.put(Aspect.SPEED, speed);
        drams.put(Aspect.SIZE, size);
        drams.put(Aspect.AWARENESS, awareness);
        drams.put(Aspect.TIME, time);
        context.register(ResourceKey.create(ModRegistries.ESSENCE_PROFILE, BuiltInRegistries.ENTITY_TYPE.getKey(type)),
                new EssenceProfile(drams));
    }

    private ModEssenceProfiles() {}
}

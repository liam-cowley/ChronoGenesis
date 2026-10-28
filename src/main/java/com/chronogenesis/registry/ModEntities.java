package com.chronogenesis.registry;

import java.util.function.Supplier;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.entity.Precursor;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Entity types. */
@EventBusSubscriber(modid = ChronoGenesis.MODID)
public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, ChronoGenesis.MODID);

    /** The people of the first cities (the Precursor era). A little taller than a villager. */
    public static final Supplier<EntityType<Precursor>> PRECURSOR = ENTITY_TYPES.register("precursor",
            () -> EntityType.Builder.of(Precursor::new, MobCategory.CREATURE)
                    .sized(0.6f, 2.2f)
                    .eyeHeight(1.95f)
                    .clientTrackingRange(10)
                    .build(ChronoGenesis.id("precursor").toString()));

    private ModEntities() {}

    @SubscribeEvent
    static void attributes(EntityAttributeCreationEvent event) {
        event.put(PRECURSOR.get(), Precursor.createAttributes().build());
    }
}

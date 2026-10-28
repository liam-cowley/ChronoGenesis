package com.chronogenesis.registry;

import java.util.function.Supplier;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.entity.Precursor;
import com.chronogenesis.entity.Raptor;
import com.chronogenesis.entity.RiderSeat;
import com.chronogenesis.entity.Titanosaur;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
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

    /** A pack hunter of the Jurassic Age, and the temples' guard. */
    public static final Supplier<EntityType<Raptor>> RAPTOR = ENTITY_TYPES.register("raptor",
            () -> EntityType.Builder.of(Raptor::new, MobCategory.MONSTER)
                    .sized(0.8f, 1.5f)
                    .eyeHeight(1.3f)
                    .clientTrackingRange(10)
                    .build(ChronoGenesis.id("raptor").toString()));

    /** A sauropod the size of a house: harness it, build on it, ride it. */
    public static final Supplier<EntityType<Titanosaur>> TITANOSAUR = ENTITY_TYPES.register("titanosaur",
            () -> EntityType.Builder.of(Titanosaur::new, MobCategory.CREATURE)
                    .sized(3.4f, 4.6f)
                    .eyeHeight(4.2f)
                    .clientTrackingRange(16)
                    .build(ChronoGenesis.id("titanosaur").toString()));

    /** What a player sits on in a Rider's Seat. */
    public static final Supplier<EntityType<RiderSeat>> RIDER_SEAT = ENTITY_TYPES.register("rider_seat",
            () -> EntityType.Builder.<RiderSeat>of(RiderSeat::new, MobCategory.MISC)
                    .sized(0.4f, 0.4f)
                    .noSummon()
                    .clientTrackingRange(10)
                    .build(ChronoGenesis.id("rider_seat").toString()));

    private ModEntities() {}

    @SubscribeEvent
    static void spawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(RAPTOR.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkAnyLightMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(TITANOSAUR.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mob::checkMobSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    @SubscribeEvent
    static void attributes(EntityAttributeCreationEvent event) {
        event.put(PRECURSOR.get(), Precursor.createAttributes().build());
        event.put(RAPTOR.get(), Raptor.createAttributes().build());
        event.put(TITANOSAUR.get(), Titanosaur.createAttributes().build());
    }
}

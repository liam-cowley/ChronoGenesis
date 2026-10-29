package com.chronogenesis.registry;

import java.util.function.Supplier;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.entity.Mammoth;
import com.chronogenesis.entity.Precursor;
import com.chronogenesis.entity.PrecursorGolem;
import com.chronogenesis.entity.Raptor;
import com.chronogenesis.entity.RiderSeat;
import com.chronogenesis.entity.Smilodon;
import com.chronogenesis.entity.Titanosaur;
import com.chronogenesis.entity.Unbegotten;
import com.chronogenesis.entity.WoollyRhino;

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

    /** The Elder God, before the beginning of time: a Chronocortex grown into one in its arena. Never spawned naturally. */
    public static final Supplier<EntityType<Unbegotten>> UNBEGOTTEN = ENTITY_TYPES.register("unbegotten",
            () -> EntityType.Builder.of(Unbegotten::new, MobCategory.MONSTER)
                    .sized(7.0f, 9.0f)
                    .eyeHeight(6.0f)
                    .fireImmune()
                    .clientTrackingRange(16)
                    .build(ChronoGenesis.id("unbegotten").toString()));

    /** A tentacle or an eye of the Unbegotten, rising at the edge of its arena. Grown by its body, never spawned. */
    public static final Supplier<EntityType<com.chronogenesis.entity.UnbegottenLimb>> UNBEGOTTEN_LIMB = ENTITY_TYPES.register("unbegotten_limb",
            () -> EntityType.Builder.of(com.chronogenesis.entity.UnbegottenLimb::new, MobCategory.MONSTER)
                    .sized(2.0f, 10.0f)
                    .eyeHeight(9.0f)
                    .fireImmune()
                    .clientTrackingRange(16)
                    .build(ChronoGenesis.id("unbegotten_limb").toString()));

    /** The Hearth's iron golems, kept bright: guards of the Precursors' plains cities. */
    public static final Supplier<EntityType<PrecursorGolem>> PRECURSOR_GOLEM = ENTITY_TYPES.register("precursor_golem",
            () -> EntityType.Builder.of(PrecursorGolem::new, MobCategory.MISC)
                    .sized(1.4f, 2.7f)
                    .clientTrackingRange(10)
                    .build(ChronoGenesis.id("precursor_golem").toString()));

    /** The Ice Age's great herd beast. */
    public static final Supplier<EntityType<Mammoth>> MAMMOTH = ENTITY_TYPES.register("mammoth",
            () -> EntityType.Builder.of(Mammoth::new, MobCategory.CREATURE)
                    .sized(3.0f, 3.8f)
                    .eyeHeight(3.2f)
                    .clientTrackingRange(12)
                    .build(ChronoGenesis.id("mammoth").toString()));

    /** The Ice Age's sabre-toothed cat. */
    public static final Supplier<EntityType<Smilodon>> SMILODON = ENTITY_TYPES.register("smilodon",
            () -> EntityType.Builder.of(Smilodon::new, MobCategory.CREATURE)
                    .sized(1.1f, 1.3f)
                    .eyeHeight(1.1f)
                    .clientTrackingRange(10)
                    .build(ChronoGenesis.id("smilodon").toString()));

    /** The Ice Age's woolly rhinoceros. */
    public static final Supplier<EntityType<WoollyRhino>> WOOLLY_RHINO = ENTITY_TYPES.register("woolly_rhino",
            () -> EntityType.Builder.of(WoollyRhino::new, MobCategory.CREATURE)
                    .sized(1.8f, 1.8f)
                    .eyeHeight(1.3f)
                    .clientTrackingRange(10)
                    .build(ChronoGenesis.id("woolly_rhino").toString()));

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
        for (var type : java.util.List.of(MAMMOTH.get(), SMILODON.get(), WOOLLY_RHINO.get())) {
            event.register(type, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (t, level, reason, pos, random) -> level.getBlockState(pos.below()).isSolid(), RegisterSpawnPlacementsEvent.Operation.REPLACE);
        }
        event.register(TITANOSAUR.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mob::checkMobSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    @SubscribeEvent
    static void attributes(EntityAttributeCreationEvent event) {
        event.put(PRECURSOR.get(), Precursor.createAttributes().build());
        event.put(RAPTOR.get(), Raptor.createAttributes().build());
        event.put(TITANOSAUR.get(), Titanosaur.createAttributes().build());
        event.put(PRECURSOR_GOLEM.get(), net.minecraft.world.entity.animal.IronGolem.createAttributes().build());
        event.put(MAMMOTH.get(), Mammoth.createAttributes().build());
        event.put(SMILODON.get(), Smilodon.createAttributes().build());
        event.put(WOOLLY_RHINO.get(), WoollyRhino.createAttributes().build());
        event.put(UNBEGOTTEN.get(), Unbegotten.createAttributes().build());
        event.put(UNBEGOTTEN_LIMB.get(), com.chronogenesis.entity.UnbegottenLimb.createAttributes().build());
    }
}

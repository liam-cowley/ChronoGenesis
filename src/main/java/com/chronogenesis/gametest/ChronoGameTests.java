package com.chronogenesis.gametest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.biocomputer.ChronocortexBlockEntity;
import com.chronogenesis.biocomputer.Clues;
import com.chronogenesis.biocomputer.DissectionTableBlock;
import com.chronogenesis.biocomputer.DissectionTableBlockEntity;
import com.chronogenesis.biocomputer.PlayerResearch;
import com.chronogenesis.biocomputer.Research;
import com.chronogenesis.essence.Aspect;
import com.chronogenesis.essence.DrainEffects;
import com.chronogenesis.essence.EssenceContents;
import com.chronogenesis.essence.EssenceItems;
import com.chronogenesis.essence.MobEssence;
import com.chronogenesis.item.EternalHourglassItem;
import com.chronogenesis.item.UmbilicalLocketItem;
import com.chronogenesis.lens.Blueprint;
import com.chronogenesis.lens.FadedBlueprintBlock;
import com.chronogenesis.lens.LensDepth;
import com.chronogenesis.lens.LensSync;
import com.chronogenesis.lens.Obelisks;
import com.chronogenesis.lens.PastViews;
import com.chronogenesis.lens.TemporalLensItem;
import com.chronogenesis.network.PastSectionsPayload;
import com.chronogenesis.organism.BrassConduitBlock;
import com.chronogenesis.organism.CorePhase;
import com.chronogenesis.organism.FleshBlock;
import com.chronogenesis.organism.GraftBlock;
import com.chronogenesis.organism.GraftBlockEntity;
import com.chronogenesis.organism.OrganismCoreBlock;
import com.chronogenesis.organism.OrganismCoreBlockEntity;
import com.chronogenesis.player.PlayerEssence;
import com.chronogenesis.player.Powers;
import com.chronogenesis.registry.ModBlocks;
import com.chronogenesis.registry.ModDataComponents;
import com.chronogenesis.registry.ModItems;
import com.chronogenesis.timetravel.PastDimension;
import com.chronogenesis.timetravel.PastStamping;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Automated in-world tests. Run with {@code gradlew runGameTestServer} (CI runs them on feature branches).
 * The {@code floor7} template is a 7x5x7 space with a stone floor; see tools/make_test_structures.py.
 * In test coordinates ({@code helper.absolutePos}) that floor is at <b>y=1</b>, so open space starts at y=2.
 * (Many older tests place blocks at y=1, replacing floor blocks; that is harmless for them, but matters for
 * anything about light, line of sight or what stands on the floor.)
 */
@GameTestHolder(ChronoGenesis.MODID)
@PrefixGameTestTemplate(false)
public final class ChronoGameTests {
    private ChronoGameTests() {}

    /** Draining Density makes a mob float off the ground. */
    @GameTest(template = "floor7", timeoutTicks = 200)
    public static void drainedDensityFloats(GameTestHelper helper) {
        Cow cow = helper.spawn(EntityType.COW, 3, 1, 3);
        double startY = cow.getY();
        int taken = MobEssence.drain(cow, Aspect.DENSITY, MobEssence.max(cow, Aspect.DENSITY) / 2);
        helper.assertTrue(taken > 0, "nothing was drained");
        helper.succeedWhen(() -> helper.assertTrue(cow.getY() > startY + 0.9,
                "cow should float: y=" + cow.getY() + " start=" + startY));
    }

    /** Draining all Size shrinks a mob to a quarter of its size. */
    @GameTest(template = "floor7")
    public static void drainedSizeShrinks(GameTestHelper helper) {
        Cow cow = helper.spawn(EntityType.COW, 3, 1, 3);
        MobEssence.drain(cow, Aspect.SIZE, MobEssence.max(cow, Aspect.SIZE));
        helper.assertTrue(cow.getScale() < 0.5f, "cow should shrink, scale=" + cow.getScale());
        helper.succeed();
    }

    /** Draining a calf's Time (its future) makes it grow up at once. */
    @GameTest(template = "floor7")
    public static void drainedTimeAgesCalf(GameTestHelper helper) {
        Cow calf = helper.spawn(EntityType.COW, 3, 1, 3);
        calf.setAge(-24000);
        helper.assertTrue(calf.isBaby(), "should start as a calf");
        MobEssence.drain(calf, Aspect.TIME, MobEssence.max(calf, Aspect.TIME));
        helper.assertFalse(calf.isBaby(), "calf should have grown up");
        helper.succeed();
    }

    /** A fed culture on a frame grows young flesh into the frame and wakes up. */
    @GameTest(template = "floor7", timeoutTicks = 300)
    public static void organismGrowsIntoFrame(GameTestHelper helper) {
        BlockPos corePos = new BlockPos(1, 1, 3);
        helper.setBlock(corePos, ModBlocks.ORGANISM_CORE.get().defaultBlockState()
                .setValue(FleshBlock.ASPECT, Aspect.DENSITY).setValue(OrganismCoreBlock.PHASE, CorePhase.SEED));
        for (int x = 2; x <= 4; x++) {
            helper.setBlock(new BlockPos(x, 1, 3), ModBlocks.BRASS_ARMATURE.get());
        }
        OrganismCoreBlockEntity core = helper.getBlockEntity(corePos);
        helper.assertTrue(core.feed(16) == 16, "core should accept 16 drams");

        helper.succeedWhen(() -> {
            helper.assertBlockPresent(ModBlocks.CHRONOFLESH.get(), new BlockPos(2, 1, 3));
            helper.assertBlockPresent(ModBlocks.CHRONOFLESH.get(), new BlockPos(3, 1, 3));
            helper.assertBlockProperty(corePos, OrganismCoreBlock.PHASE, CorePhase.AWAKE);
        });
    }

    /** A Tap facing a Brass Conduit grows living veins along it, one block per beat. */
    @GameTest(template = "floor7", timeoutTicks = 300)
    public static void tapGrowsVeins(GameTestHelper helper) {
        BlockPos corePos = new BlockPos(1, 1, 3);
        helper.setBlock(corePos, core(Aspect.DENSITY));
        helper.setBlock(corePos.above(), graft(ModBlocks.TAP.get()));
        for (int x = 1; x <= 3; x++) {
            helper.setBlock(new BlockPos(x, 3, 3), ModBlocks.BRASS_CONDUIT.get());
        }
        OrganismCoreBlockEntity core = helper.getBlockEntity(corePos);
        core.feed(12);
        helper.succeedWhen(() -> {
            for (int x = 1; x <= 3; x++) {
                helper.assertBlockProperty(new BlockPos(x, 3, 3), BrassConduitBlock.VEIN, true);
            }
        });
    }

    /** A Time organism with no Stasis Bladders, filled up, grows unstable and bursts into a Time Tear. */
    @GameTest(template = "floor7", timeoutTicks = 700)
    public static void unstableTimeOrganismBursts(GameTestHelper helper) {
        BlockPos corePos = new BlockPos(3, 1, 3);
        helper.setBlock(corePos, core(Aspect.TIME));
        OrganismCoreBlockEntity core = helper.getBlockEntity(corePos);
        core.feed(16);
        helper.succeedWhen(() -> helper.assertBlockPresent(ModBlocks.TIME_TEAR.get(), corePos));
    }

    /** Racetrack: a Speed pulse runs down the vein from the Starting Gate and Time condenses at the Finish. */
    @GameTest(template = "floor7", timeoutTicks = 400)
    public static void racetrackMakesTime(GameTestHelper helper) {
        BlockPos speedCore = new BlockPos(1, 1, 1);
        BlockPos timeCore = new BlockPos(5, 1, 1);
        helper.setBlock(speedCore, core(Aspect.SPEED));
        helper.setBlock(timeCore, core(Aspect.TIME));
        helper.setBlock(speedCore.above(), graft(ModBlocks.STARTING_GATE.get()));
        helper.setBlock(timeCore.above(), graft(ModBlocks.FINISH.get()));
        for (int x = 1; x <= 5; x++) {
            helper.setBlock(new BlockPos(x, 3, 1), ModBlocks.BRASS_CONDUIT.get().defaultBlockState()
                    .setValue(BrassConduitBlock.VEIN, true));
        }
        OrganismCoreBlockEntity speed = helper.getBlockEntity(speedCore);
        speed.feed(16);
        OrganismCoreBlockEntity time = helper.getBlockEntity(timeCore);
        helper.succeedWhen(() -> helper.assertTrue(time.stored() > 0, "no Time arrived at the finish"));
    }

    /** A Heart grafted on an organism is counted on its next scan and doubles its Taps' rate. */
    @GameTest(template = "floor7", timeoutTicks = 200)
    public static void heartSpeedsUpTap(GameTestHelper helper) {
        BlockPos corePos = new BlockPos(3, 1, 3);
        helper.setBlock(corePos, core(Aspect.DENSITY));
        helper.setBlock(corePos.east(), ModBlocks.HEART.get().defaultBlockState()
                .setValue(GraftBlock.FACING, Direction.EAST));
        OrganismCoreBlockEntity core = helper.getBlockEntity(corePos);
        helper.succeedWhen(() -> {
            helper.assertTrue(core.hearts() == 1, "heart not counted: " + core.hearts());
            helper.assertTrue(GraftBlockEntity.tapRate(core) == 8, "tap rate should double: " + GraftBlockEntity.tapRate(core));
        });
    }

    /** An Eye gives redstone while a creature is near, and a comparator reads how full the organism is. */
    @GameTest(template = "floor7", timeoutTicks = 200)
    public static void eyeWatches(GameTestHelper helper) {
        BlockPos corePos = new BlockPos(1, 1, 1);
        BlockPos eyePos = corePos.above();
        helper.setBlock(corePos, core(Aspect.DENSITY));
        helper.setBlock(eyePos, graft(ModBlocks.EYE.get()));
        OrganismCoreBlockEntity core = helper.getBlockEntity(corePos);
        core.feed(16);
        // A still cow, so it cannot wander out of the eye's sight during the test.
        helper.spawn(EntityType.COW, 3, 1, 3).setNoAi(true);
        helper.succeedWhen(() -> {
            GraftBlockEntity eye = helper.getBlockEntity(eyePos);
            helper.assertTrue(eye.seesCreature(), "eye should see the cow");
            helper.assertTrue(helper.getLevel().hasNeighborSignal(helper.absolutePos(eyePos).above()),
                    "the block above the eye should be powered");
            helper.assertTrue(eye.reportedFill() > 0, "comparator should read a full organism");
        });
    }

    /**
     * Twin Clocks: a Clock buried in Density flesh, linked by a vein to a Clock on a Time organism, misses
     * beats; the free Clock turns them into Time.
     */
    @GameTest(template = "floor7", timeoutTicks = 1000)
    public static void twinClocksMakeTime(GameTestHelper helper) {
        BlockPos densityCore = new BlockPos(1, 1, 1);
        BlockPos timeCore = new BlockPos(5, 1, 1);
        BlockPos buried = densityCore.above();
        helper.setBlock(densityCore, core(Aspect.DENSITY));
        helper.setBlock(timeCore, core(Aspect.TIME));
        helper.setBlock(timeCore.east(), ModBlocks.STASIS_BLADDER.get().defaultBlockState()
                .setValue(GraftBlock.FACING, Direction.EAST));
        // Heavy flesh packed around the buried clock, leaving its vein clear.
        for (BlockPos pos : BlockPos.betweenClosed(0, 1, 0, 2, 4, 3)) {
            boolean keep = pos.equals(densityCore) || pos.equals(buried) || (pos.getY() == 3 && pos.getZ() == 1 && pos.getX() >= 1);
            if (!keep) {
                helper.setBlock(pos, ModBlocks.CHRONOFLESH.get().defaultBlockState()
                        .setValue(FleshBlock.ASPECT, Aspect.DENSITY).setValue(FleshBlock.GROWTH, FleshBlock.MATURE));
            }
        }
        helper.setBlock(buried, graft(ModBlocks.CLOCK.get()));
        helper.setBlock(timeCore.above(), graft(ModBlocks.CLOCK.get()));
        for (int x = 1; x <= 5; x++) {
            helper.setBlock(new BlockPos(x, 3, 1), ModBlocks.BRASS_CONDUIT.get().defaultBlockState()
                    .setValue(BrassConduitBlock.VEIN, true));
        }
        helper.<OrganismCoreBlockEntity>getBlockEntity(densityCore).feed(64);
        OrganismCoreBlockEntity time = helper.getBlockEntity(timeCore);
        helper.succeedWhen(() -> helper.assertTrue(time.stored() > 0, "no Time from the twin clocks"));
    }

    /** A Leech Tendril drains a nearby creature into its organism, then swells as the stream of essence arrives. */
    @GameTest(template = "floor7", timeoutTicks = 200)
    public static void leechTendrilDrainsAndGulps(GameTestHelper helper) {
        BlockPos corePos = new BlockPos(1, 1, 1);
        BlockPos tendrilPos = corePos.above();
        helper.setBlock(corePos, core(Aspect.DENSITY));
        helper.setBlock(tendrilPos, graft(ModBlocks.LEECH_TENDRIL.get()));
        Cow cow = helper.spawn(EntityType.COW, 3, 1, 3);
        cow.setNoAi(true);
        OrganismCoreBlockEntity core = helper.getBlockEntity(corePos);
        boolean[] gulped = {false};
        helper.onEachTick(() -> {
            if (helper.getBlockState(tendrilPos).getValue(GraftBlock.ACTIVE)) {
                gulped[0] = true;
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(core.stored() > 0, "nothing drained into the organism");
            helper.assertTrue(MobEssence.remaining(cow, Aspect.DENSITY) < MobEssence.max(cow, Aspect.DENSITY), "cow not drained");
            helper.assertTrue(gulped[0], "the tendril never gulped");
            helper.assertFalse(helper.getBlockState(tendrilPos).getValue(GraftBlock.ACTIVE), "the tendril should settle back");
        });
    }

    // ---------------------------------------------------------------- the Chronocortex (build step 7)

    /** Lightning on a rod over a ready Dissection Table wakes the brain: the table becomes the Chronocortex. */
    @GameTest(template = "floor7")
    public static void lightningWakesChronocortex(GameTestHelper helper) {
        BlockPos tablePos = readyTable(helper, true);
        strike(helper, tablePos.above());
        helper.succeedWhenBlockPresent(ModBlocks.CHRONOCORTEX.get(), tablePos);
    }

    /** A brain that has not soaked up enough Awareness and Time cooks instead. */
    @GameTest(template = "floor7")
    public static void unreadyBrainBurns(GameTestHelper helper) {
        BlockPos tablePos = readyTable(helper, false);
        strike(helper, tablePos.above());
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(ModBlocks.DISSECTION_TABLE.get(), tablePos);
            helper.assertBlockProperty(tablePos, DissectionTableBlock.BRAIN, false);
        });
    }

    /** An organism with a Ganglion near the Chronocortex is wired in, and its essence shows up. */
    @GameTest(template = "floor7", timeoutTicks = 200)
    public static void ganglionWiresOrganism(GameTestHelper helper) {
        BlockPos cortexPos = new BlockPos(1, 1, 1);
        BlockPos corePos = new BlockPos(4, 1, 4);
        helper.setBlock(cortexPos, ModBlocks.CHRONOCORTEX.get());
        helper.setBlock(corePos, core(Aspect.DENSITY));
        helper.setBlock(corePos.above(), graft(ModBlocks.GANGLION.get()));
        OrganismCoreBlockEntity core = helper.getBlockEntity(corePos);
        core.feed(10);
        ChronocortexBlockEntity cortex = helper.getBlockEntity(cortexPos);
        // Neighbouring test arenas are within the cortex's reach too, so look for this organism specifically.
        helper.succeedWhen(() -> {
            helper.assertTrue(cortex.wiredCores().contains(core), "organism not wired");
            helper.assertTrue(cortex.availableEssence().getOrDefault(Aspect.DENSITY, 0) >= 10,
                    "essence not visible: " + cortex.availableEssence());
        });
    }

    /** Researching Deeper Infusion draws essence from wired organisms, takes the materials and spends one clue set. */
    @GameTest(template = "floor7", timeoutTicks = 200)
    public static void researchPaysItsCosts(GameTestHelper helper) {
        BlockPos cortexPos = new BlockPos(3, 1, 3);
        BlockPos awarenessCore = new BlockPos(1, 1, 1);
        BlockPos timeCore = new BlockPos(5, 1, 1);
        helper.setBlock(cortexPos, ModBlocks.CHRONOCORTEX.get());
        helper.setBlock(awarenessCore, core(Aspect.AWARENESS));
        helper.setBlock(awarenessCore.above(), graft(ModBlocks.GANGLION.get()));
        helper.setBlock(timeCore, core(Aspect.TIME));
        helper.setBlock(timeCore.above(), graft(ModBlocks.GANGLION.get()));
        OrganismCoreBlockEntity awareness = helper.getBlockEntity(awarenessCore);
        awareness.feed(16);
        helper.<OrganismCoreBlockEntity>getBlockEntity(timeCore).feed(8);

        ServerPlayer player = fakePlayer(helper);
        player.getInventory().add(new ItemStack(Items.AMETHYST_SHARD, 8));
        player.getInventory().add(new ItemStack(ModItems.PRESERVED_HEART.get()));
        Research.set(player, Research.get(player).withKnown(Clues.FLOATING).withKnown(Clues.VACANT));
        ChronocortexBlockEntity cortex = helper.getBlockEntity(cortexPos);
        OrganismCoreBlockEntity time = helper.getBlockEntity(timeCore);

        helper.succeedWhen(() -> {
            helper.assertTrue(cortex.wiredCores().contains(awareness) && cortex.wiredCores().contains(time),
                    "both organisms should be wired");
            helper.assertTrue(Research.perform(player, cortex, Research.LEVEL_2_POWERS), "research should succeed");
            helper.assertTrue(Research.has(player, Research.LEVEL_2_POWERS), "research not recorded");
            helper.assertTrue(awareness.stored() == 0, "Awareness not drawn: " + awareness.stored());
            helper.assertFalse(player.getInventory().contains(new ItemStack(Items.AMETHYST_SHARD)), "materials not taken");
            helper.assertFalse(Research.get(player).hasClue(Clues.FLOATING), "clue set not spent");
        });
    }

    /** With an Eternal Hourglass carried, a lone Chronocortex (no organisms wired) researches on the hourglass's essence. */
    @GameTest(template = "floor7")
    public static void hourglassSuppliesResearchEssence(GameTestHelper helper) {
        BlockPos cortexPos = new BlockPos(3, 1, 3);
        helper.setBlock(cortexPos, ModBlocks.CHRONOCORTEX.get());
        ChronocortexBlockEntity cortex = helper.getBlockEntity(cortexPos);
        helper.assertTrue(ModItems.CHRONOCORTEX.get().getBlock() == ModBlocks.CHRONOCORTEX.get(), "the creative item should place the cortex");

        ServerPlayer player = fakePlayer(helper);
        player.getInventory().add(new ItemStack(ModItems.ETERNAL_HOURGLASS.get()));
        player.getInventory().add(new ItemStack(Items.AMETHYST_SHARD, 8));
        player.getInventory().add(new ItemStack(ModItems.PRESERVED_HEART.get()));
        Research.set(player, Research.get(player).withKnown(Clues.FLOATING).withKnown(Clues.VACANT));

        helper.assertTrue(cortex.wiredCores().isEmpty(), "no organisms should be wired");
        helper.assertTrue(Research.perform(player, cortex, Research.LEVEL_2_POWERS), "research should succeed on the hourglass");
        helper.assertTrue(Research.has(player, Research.LEVEL_2_POWERS), "research not recorded");
        helper.assertFalse(player.getInventory().contains(new ItemStack(Items.AMETHYST_SHARD)), "materials not taken");
        helper.succeed();
    }

    /** A worn Umbilical Locket tied to an organism draws its essence into the wearer's pool, from a distance. */
    @GameTest(template = "floor7")
    public static void locketRefillsFromAfar(GameTestHelper helper) {
        BlockPos corePos = new BlockPos(1, 1, 1);
        helper.setBlock(corePos, core(Aspect.DENSITY));
        OrganismCoreBlockEntity core = helper.getBlockEntity(corePos);
        core.feed(10);
        ServerPlayer player = fakePlayer(helper);
        player.moveTo(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(5, 1, 5))));
        Powers.set(player, PlayerEssence.EMPTY.withLevel(Aspect.DENSITY, 1));
        ItemStack locket = new ItemStack(ModItems.UMBILICAL_LOCKET.get());
        locket.set(ModDataComponents.LOCKET_LINK.get(), GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(corePos)));

        int drawn = UmbilicalLocketItem.refill(player, locket);
        helper.assertTrue(drawn == 2, "locket should draw 2 drams, drew " + drawn);
        helper.assertTrue(Powers.get(player).pool(Aspect.DENSITY) == 2, "pool not filled");
        helper.assertTrue(core.stored() == 8, "organism not drawn from: " + core.stored());
        helper.succeed();
    }

    /** A Temporal Field on a Time organism gives a drained creature its essence back, for the organism's Time. */
    @GameTest(template = "floor7", timeoutTicks = 300)
    public static void temporalFieldRegenerates(GameTestHelper helper) {
        BlockPos corePos = new BlockPos(3, 1, 3);
        helper.setBlock(corePos, core(Aspect.TIME));
        helper.setBlock(corePos.above(), graft(ModBlocks.TEMPORAL_FIELD.get()));
        // Bladders keep the Time organism from bursting while the test runs.
        for (Direction side : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            helper.setBlock(corePos.relative(side), ModBlocks.STASIS_BLADDER.get().defaultBlockState().setValue(GraftBlock.FACING, side));
        }
        OrganismCoreBlockEntity core = helper.getBlockEntity(corePos);
        core.feed(8);
        Cow cow = helper.spawn(EntityType.COW, 1, 1, 1);
        cow.setNoAi(true);
        MobEssence.drain(cow, Aspect.SPEED, 3);
        int drained = MobEssence.max(cow, Aspect.SPEED) - MobEssence.remaining(cow, Aspect.SPEED);
        helper.succeedWhen(() -> {
            int now = MobEssence.max(cow, Aspect.SPEED) - MobEssence.remaining(cow, Aspect.SPEED);
            helper.assertTrue(now < drained, "speed not given back: still " + now + " drained");
            helper.assertTrue(core.stored() < 8, "the field should pay in Time");
        });
    }

    /** Clues come at the right moment: floating at half Density gone (not at the first dram), Vacant at none left. */
    @GameTest(template = "floor7")
    public static void drainCluesAtTheRightMoment(GameTestHelper helper) {
        Cow cow = helper.spawn(EntityType.COW, 3, 1, 3);
        cow.setNoAi(true);
        MobEssence.drain(cow, Aspect.DENSITY, 1);
        helper.assertTrue(DrainEffects.clueFor(cow, Aspect.DENSITY) == null, "one dram should not make it float yet");
        MobEssence.drain(cow, Aspect.DENSITY, MobEssence.max(cow, Aspect.DENSITY) / 2);
        helper.assertTrue(Clues.FLOATING.equals(DrainEffects.clueFor(cow, Aspect.DENSITY)), "half drained should float");
        MobEssence.drain(cow, Aspect.AWARENESS, 1);
        helper.assertTrue(DrainEffects.clueFor(cow, Aspect.AWARENESS) == null, "a little Awareness gone is not Vacant");
        MobEssence.drain(cow, Aspect.AWARENESS, MobEssence.max(cow, Aspect.AWARENESS));
        helper.assertTrue(Clues.VACANT.equals(DrainEffects.clueFor(cow, Aspect.AWARENESS)), "no Awareness left is Vacant");
        helper.succeed();
    }

    // ---------------------------------------------------------------- the Temporal Lens and the past (build step 8)




    /**
     * The past dimension's files are valid: its type is registered (with a sky) and the dimension itself decodes
     * with the game's own codec. (The game-test server builds only the flat preset's dimensions, so the past
     * cannot be loaded here; real servers and worlds load it from the datapack.)
     */
    @GameTest(template = "floor7")
    public static void pastDimensionIsValid(GameTestHelper helper) throws Exception {
        var server = helper.getLevel().getServer();
        var types = server.registryAccess().registryOrThrow(Registries.DIMENSION_TYPE);
        var type = types.get(PastDimension.KEY.location());
        helper.assertTrue(type != null && type.hasSkyLight(), "the past's dimension type is missing or has no sky");
        var resource = server.getResourceManager().getResourceOrThrow(ChronoGenesis.id("dimension/past.json"));
        try (var reader = resource.openAsReader()) {
            var json = JsonParser.parseReader(reader);
            var stem = LevelStem.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, server.registryAccess()), json);
            helper.assertTrue(stem.result().isPresent(), "the past dimension does not decode: " + stem.error().map(Object::toString).orElse(""));
        }
        helper.succeed();
    }

    /**
     * A ruin's past is stamped into the past dimension the way a jigsaw piece is placed: the lab stands whole,
     * the brain on its table, where the rotation puts it; and a past section survives the trip to the client.
     */
    @GameTest(template = "floor7", timeoutTicks = 400)
    public static void pastIsStampedAndSent(GameTestHelper helper) {
        // Stamping and encoding work on any level; the test world stands in for the past dimension here.
        ServerLevel past = helper.getLevel();
        StructureTemplate template = past.getStructureManager().get(ChronoGenesis.id("abandoned_laboratory_past")).orElseThrow();
        BlockState brainTable = ModBlocks.DISSECTION_TABLE.get().defaultBlockState().setValue(DissectionTableBlock.BRAIN, true);
        // Far from anything else, high above the past's ground.
        BlockPos plain = new BlockPos(helper.absolutePos(BlockPos.ZERO).getX() + 100_000, 200, helper.absolutePos(BlockPos.ZERO).getZ());
        BlockPos turned = plain.offset(64, 0, 0);
        for (BlockPos origin : List.of(plain, turned)) {
            for (int dx = -16; dx <= 16; dx += 16) {
                for (int dz = -16; dz <= 16; dz += 16) {
                    past.getChunk((origin.getX() + dx) >> 4, (origin.getZ() + dz) >> 4);
                }
            }
        }
        PastStamping.place(past, template, plain, Rotation.NONE);
        PastStamping.place(past, template, turned, Rotation.CLOCKWISE_90);
        helper.assertTrue(past.getBlockState(plain.offset(5, 1, 6)).equals(brainTable), "table not at (5,1,6)");
        // Clockwise: (x, y, z) -> (-z, y, x).
        helper.assertTrue(past.getBlockState(turned.offset(-6, 1, 5)).equals(brainTable), "rotated table misplaced");

        // The section holding the table, encoded for the client and read back, has the same blocks and light.
        BlockPos table = plain.offset(5, 1, 6);
        LevelChunk chunk = past.getChunk(table.getX() >> 4, table.getZ() >> 4);
        int sectionY = SectionPos.blockToSectionCoord(table.getY());
        long key = SectionPos.asLong(table.getX() >> 4, sectionY, table.getZ() >> 4);
        PastSectionsPayload.Section section = LensSync.encode(past, chunk, sectionY, key);
        LevelChunkSection decoded = section.decode(past.registryAccess().registryOrThrow(Registries.BIOME));
        helper.assertTrue(decoded.getBlockState(table.getX() & 15, table.getY() & 15, table.getZ() & 15).equals(brainTable),
                "the table did not survive encoding");
        helper.assertTrue(section.skyLight().get(table.getX() & 15, table.getY() & 15, table.getZ() & 15)
                == past.getBrightness(LightLayer.SKY, table), "sky light did not survive encoding");
        helper.succeed();
    }

    /** Reading a blueprint scribbles: the sound event exists and plays its pen-on-paper sounds. */
    @GameTest(template = "floor7")
    public static void scribbleSoundRegistered(GameTestHelper helper) {
        helper.assertTrue(BuiltInRegistries.SOUND_EVENT.containsKey(ChronoGenesis.id("lens.scribble")), "scribble sound missing");
        helper.succeed();
    }

    /**
     * Creative players can unlock the whole journal: the command exists, and there are ChronoGenesis
     * advancements (which gate the journal's entries) for it to grant. FakePlayers ignore advancements, so the
     * granting itself is left to a real player.
     */
    @GameTest(template = "floor7")
    public static void journalUnlockAvailable(GameTestHelper helper) {
        var root = helper.getLevel().getServer().getCommands().getDispatcher().getRoot();
        var unlock = root.getChild(ChronoGenesis.MODID) == null ? null
                : root.getChild(ChronoGenesis.MODID).getChild("journal") == null ? null
                : root.getChild(ChronoGenesis.MODID).getChild("journal").getChild("unlock");
        helper.assertTrue(unlock != null, "/chronogenesis journal unlock is not registered");
        long advancements = helper.getLevel().getServer().getAdvancements().getAllAdvancements().stream()
                .filter(advancement -> advancement.id().getNamespace().equals(ChronoGenesis.MODID)).count();
        helper.assertTrue(advancements >= 7, "expected the journal's advancements, found " + advancements);
        helper.succeed();
    }

    /** The ruin generates with its readable blueprints and a loot chest, and is registered as a structure. */
    @GameTest(template = "floor7")
    public static void laboratoryIsRegistered(GameTestHelper helper) {
        var level = helper.getLevel();
        helper.assertTrue(level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                .containsKey(ChronoGenesis.id("abandoned_laboratory")), "structure not registered");
        helper.assertTrue(level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET)
                .containsKey(ChronoGenesis.id("abandoned_laboratory")), "structure set not registered");
        StructureTemplate ruin = level.getStructureManager().get(ChronoGenesis.id("abandoned_laboratory")).orElseThrow();
        helper.assertTrue(ruin.getSize().equals(new Vec3i(11, 7, 9)), "unexpected size " + ruin.getSize());
        var blocks = ruin.palettes.get(0).blocks();
        helper.assertTrue(blocks.stream().filter(info -> info.state().is(ModBlocks.FADED_BLUEPRINT.get())).count() == 2,
                "the ruin should keep its two blueprints");
        helper.assertTrue(blocks.stream().anyMatch(info -> info.state().is(Blocks.CHEST) && info.nbt() != null
                && info.nbt().getString("LootTable").equals("chronogenesis:chests/abandoned_laboratory")), "chest loot missing");
        helper.succeed();
    }

    /** Holding the Lens on a faded blueprint at Decades copies it into the journal as a clue. */
    @GameTest(template = "floor7")
    public static void lensReadsBlueprint(GameTestHelper helper) {
        BlockPos sheet = new BlockPos(3, 1, 3);
        helper.setBlock(sheet, ModBlocks.FADED_BLUEPRINT.get().defaultBlockState().setValue(FadedBlueprintBlock.DESIGN, Blueprint.FIELD));
        ServerPlayer player = fakePlayer(helper);
        // Standing over the sheet, looking straight down at the paper (it lies flat, a sliver above its block's floor).
        player.moveTo(Vec3.atBottomCenterOf(helper.absolutePos(sheet)).add(0, 1, 0));
        player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atBottomCenterOf(helper.absolutePos(sheet)).add(0, 0.01, 0));
        boolean read = false;
        for (int i = 0; i < TemporalLensItem.SCAN_TICKS && !read; i++) {
            read = TemporalLensItem.scanTick(player, LensDepth.DECADES);
        }
        helper.assertTrue(read, "blueprint not read");
        helper.assertTrue(Research.get(player).hasClue(Blueprint.FIELD.clue()), "blueprint clue not granted");
        helper.assertFalse(TemporalLensItem.scanTick(player, LensDepth.ECHO), "Echo should not read blueprints");
        helper.succeed();
    }

    /** The Lens burns its own Time first, then the wearer's pool. */
    @GameTest(template = "floor7")
    public static void lensPaysFromItselfFirst(GameTestHelper helper) {
        ServerPlayer player = fakePlayer(helper);
        Powers.set(player, PlayerEssence.EMPTY.withLevel(Aspect.TIME, 1).withPool(Aspect.TIME, 5));
        ItemStack lens = new ItemStack(ModItems.TEMPORAL_LENS.get());
        EssenceItems.set(lens, new EssenceContents(Aspect.TIME, 1));
        helper.assertTrue(TemporalLensItem.pay(player, lens, 2), "should be able to pay 2");
        helper.assertTrue(EssenceItems.get(lens) == null, "lens store should be spent first");
        helper.assertTrue(Powers.get(player).pool(Aspect.TIME) == 4, "the rest from the pool");
        helper.assertFalse(TemporalLensItem.pay(player, lens, 5), "cannot pay more than there is");
        helper.succeed();
    }

    /** An obelisk is found by its capstone, and its event horizon swallows what lies close to its heart. */
    @GameTest(template = "floor7")
    public static void obeliskHorizonSwallows(GameTestHelper helper) {
        BlockPos capstone = new BlockPos(3, 4, 3);
        helper.setBlock(capstone, ModBlocks.OBELISK_CAPSTONE.get());
        Vec3 heart = Obelisks.heart(helper.absolutePos(capstone));
        List<BlockPos> found = Obelisks.near(helper.getLevel(), heart, 16);
        helper.assertTrue(found.contains(helper.absolutePos(capstone)), "obelisk not found by its capstone");
        helper.assertTrue(Obelisks.swallowed(found, heart.add(0, 1, 2)), "inside the horizon should be swallowed");
        helper.assertFalse(Obelisks.swallowed(found, heart.add(0, 0, Obelisks.HORIZON + 1)), "outside should survive");
        helper.succeed();
    }

    /** Through the Lens an obelisk is noted from afar; its older inscription can only be read from the edge. */
    @GameTest(template = "floor7")
    public static void obeliskInscriptionReadFromTheEdge(GameTestHelper helper) {
        BlockPos capstone = new BlockPos(0, 4, 0);
        helper.setBlock(capstone, ModBlocks.OBELISK_CAPSTONE.get());
        Vec3 heart = Obelisks.heart(helper.absolutePos(capstone));
        ServerPlayer player = fakePlayer(helper);
        Research.set(player, PlayerResearch.EMPTY);
        // About seven blocks from the heart: just outside the horizon.
        player.moveTo(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(5, 1, 4))));
        player.lookAt(EntityAnchorArgument.Anchor.EYES, heart);
        double distance = heart.distanceTo(player.getEyePosition());
        helper.assertTrue(distance > Obelisks.HORIZON && distance < Obelisks.HORIZON + Obelisks.EDGE_WIDTH, "not at the edge: " + distance);
        boolean read = false;
        for (int i = 0; i < TemporalLensItem.INSCRIPTION_TICKS && !read; i++) {
            read = TemporalLensItem.obeliskTick(player);
        }
        helper.assertTrue(read, "inscription not read at the edge");
        helper.assertTrue(Research.get(player).hasClue(Clues.NAIL_IN_TIME), "obelisk not noted");
        helper.assertTrue(Research.get(player).hasClue(Clues.OLDER_INSCRIPTION), "inscription clue missing");

        // Looking away, nothing more is read.
        player.lookAt(EntityAnchorArgument.Anchor.EYES, heart.add(0, 0, 30));
        for (int i = 0; i < TemporalLensItem.INSCRIPTION_TICKS; i++) {
            helper.assertFalse(TemporalLensItem.obeliskTick(player), "read while looking away");
        }
        helper.succeed();
    }

    /** The creative Eternal Hourglass fills every pool at the top level and keeps the Lens charged and free. */
    @GameTest(template = "floor7")
    public static void eternalHourglassSaturates(GameTestHelper helper) {
        ServerPlayer player = fakePlayer(helper);
        player.getInventory().clearContent();
        Powers.set(player, PlayerEssence.EMPTY);
        ItemStack lens = new ItemStack(ModItems.TEMPORAL_LENS.get());
        // setItem keeps this very stack in the slot (add() would move a copy in and empty ours).
        player.getInventory().setItem(0, lens);
        helper.assertTrue(TemporalLensItem.timeAvailable(player, lens) == 0, "an empty lens without the hourglass has no Time");
        player.getInventory().add(new ItemStack(ModItems.ETERNAL_HOURGLASS.get()));

        EternalHourglassItem.saturate(player);
        PlayerEssence essence = Powers.get(player);
        for (Aspect aspect : Aspect.values()) {
            helper.assertTrue(essence.level(aspect) == Powers.MAX_LEVEL, aspect + " not at max level");
            helper.assertTrue(essence.pool(aspect) == essence.max(aspect) && essence.max(aspect) > 0, aspect + " pool not full");
        }
        helper.assertTrue(EssenceItems.get(lens) != null && EssenceItems.get(lens).amount() == TemporalLensItem.CAPACITY, "lens not charged");
        helper.assertTrue(TemporalLensItem.pay(player, lens, 2), "looking should be free");
        helper.assertTrue(EssenceItems.get(lens).amount() == TemporalLensItem.CAPACITY, "the lens should not drain");
        player.getInventory().clearContent();
        helper.succeed();
    }

    /**
     * A stand-in player for server-side logic. Not a mock "joined" player: Curios tries to sync its slots to
     * those and fails without a client. A FakePlayer never joins the level and swallows packets.
     */
    private static ServerPlayer fakePlayer(GameTestHelper helper) {
        // A fresh one per test (the factory caches by profile), so no inventory or pools leak between tests.
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "chronogenesis_test"));
    }

    /** A Dissection Table with a brain and a lightning rod on it, soaked full (or not) of Awareness and Time. */
    private static BlockPos readyTable(GameTestHelper helper, boolean soaked) {
        BlockPos tablePos = new BlockPos(3, 1, 3);
        helper.setBlock(tablePos, ModBlocks.DISSECTION_TABLE.get().defaultBlockState().setValue(DissectionTableBlock.BRAIN, true));
        helper.setBlock(tablePos.above(), Blocks.LIGHTNING_ROD);
        if (soaked) {
            DissectionTableBlockEntity table = helper.getBlockEntity(tablePos);
            table.insert(Aspect.AWARENESS, DissectionTableBlockEntity.NEEDED);
            table.insert(Aspect.TIME, DissectionTableBlockEntity.NEEDED);
        }
        return tablePos;
    }

    /** A harmless (visual-only) lightning bolt on the given block. */
    private static void strike(GameTestHelper helper, BlockPos pos) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(helper.getLevel());
        bolt.moveTo(Vec3.atBottomCenterOf(helper.absolutePos(pos)));
        bolt.setVisualOnly(true);
        helper.getLevel().addFreshEntity(bolt);
    }

    private static BlockState core(Aspect aspect) {
        return ModBlocks.ORGANISM_CORE.get().defaultBlockState()
                .setValue(FleshBlock.ASPECT, aspect).setValue(OrganismCoreBlock.PHASE, CorePhase.SEED);
    }

    private static BlockState graft(net.minecraft.world.level.block.Block block) {
        return block.defaultBlockState().setValue(GraftBlock.FACING, Direction.UP);
    }
}

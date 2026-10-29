package com.chronogenesis.gametest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.biocomputer.ChronocortexBlockEntity;
import com.chronogenesis.biocomputer.Clues;
import com.chronogenesis.biocomputer.CortexLobeBlock;
import com.chronogenesis.biocomputer.DissectionTableBlock;
import com.chronogenesis.biocomputer.DissectionTableBlockEntity;
import com.chronogenesis.biocomputer.Lessons;
import com.chronogenesis.biocomputer.PlayerResearch;
import com.chronogenesis.biocomputer.Research;
import com.chronogenesis.entity.Precursor;
import com.chronogenesis.entity.Titanosaur;
import com.chronogenesis.entity.Unbegotten;
import com.chronogenesis.essence.Aspect;
import com.chronogenesis.essence.DrainEffects;
import com.chronogenesis.essence.EssenceContents;
import com.chronogenesis.essence.EssenceItems;
import com.chronogenesis.essence.MobEssence;
import com.chronogenesis.item.ChronoSuitItem;
import com.chronogenesis.item.CultureItem;
import com.chronogenesis.item.EternalHourglassItem;
import com.chronogenesis.item.NerveSpoolItem;
import com.chronogenesis.item.UmbilicalLocketItem;
import com.chronogenesis.lens.Blueprint;
import com.chronogenesis.lens.FadedBlueprintBlock;
import com.chronogenesis.lens.LensDepth;
import com.chronogenesis.lens.LensSync;
import com.chronogenesis.lens.Obelisks;
import com.chronogenesis.lens.PastViews;
import com.chronogenesis.lens.TemporalLensItem;
import com.chronogenesis.lore.PrecursorLore;
import com.chronogenesis.lore.PrecursorTabletItem;
import com.chronogenesis.network.PastSectionsPayload;
import com.chronogenesis.organism.BrassConduitBlock;
import com.chronogenesis.organism.CorePhase;
import com.chronogenesis.organism.EssenceJugBlockEntity;
import com.chronogenesis.organism.FleshBlock;
import com.chronogenesis.organism.GlandBlock;
import com.chronogenesis.organism.GraftBlock;
import com.chronogenesis.organism.GraftBlockEntity;
import com.chronogenesis.organism.OrganismCoreBlock;
import com.chronogenesis.organism.OrganismCoreBlockEntity;
import com.chronogenesis.player.PlayerEssence;
import com.chronogenesis.player.Power;
import com.chronogenesis.player.Powers;
import com.chronogenesis.player.Rewind;
import com.chronogenesis.player.TemporalExposure;
import com.chronogenesis.registry.ModAttachments;
import com.chronogenesis.registry.ModBlocks;
import com.chronogenesis.registry.ModDataComponents;
import com.chronogenesis.registry.ModEntities;
import com.chronogenesis.registry.ModItems;
import com.chronogenesis.temple.SealBlock;
import com.chronogenesis.temple.TempleBarrier;
import com.chronogenesis.temple.TemporalCoreBlock;
import com.chronogenesis.time.TimeZones;
import com.chronogenesis.timetravel.ElderArena;
import com.chronogenesis.timetravel.Era;
import com.chronogenesis.timetravel.Eras;
import com.chronogenesis.timetravel.FossilCortexBlock;
import com.chronogenesis.timetravel.FossilCortexBlockEntity;
import com.chronogenesis.timetravel.PastDimension;
import com.chronogenesis.timetravel.PastStamping;
import com.chronogenesis.timetravel.Remnants;
import com.chronogenesis.timetravel.TemporalShift;
import com.chronogenesis.timetravel.UnmadeNailBlock;
import com.chronogenesis.world.PlacedTemplateStructure;
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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.BlockHitResult;
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
            helper.assertTrue(Research.perform(player, cortex, Research.LEVEL_2_POWERS), "research should succeed: essence "
                    + cortex.availableEssence() + ", stage " + cortex.stage() + ", clues " + Research.get(player).known());
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

    /** Rewind takes the player back to where they stood 5 seconds ago, for 4 drams of Time. */
    @GameTest(template = "floor7")
    public static void rewindReturnsToTheTrail(GameTestHelper helper) {
        ServerPlayer player = fakePlayer(helper);
        Vec3 start = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
        Vec3 later = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(5, 1, 5)));
        Powers.set(player, PlayerEssence.EMPTY.withLevel(Aspect.TIME, 1).withPool(Aspect.TIME, 10));
        player.moveTo(start.x, start.y, start.z);
        for (int i = 0; i < 30; i++) {
            Rewind.record(player);
        }
        player.moveTo(later.x, later.y, later.z);
        for (int i = 0; i < 10; i++) {
            Rewind.record(player);
        }
        Powers.use(player);
        helper.assertTrue(player.position().distanceTo(start) < 0.01, "not rewound: " + player.position());
        helper.assertTrue(Powers.get(player).pool(Aspect.TIME) == 6, "rewind should cost 4 drams");
        helper.succeed();
    }

    /** Time Freeze stops the chunk: a dropped item hangs in the air. Released, the chunk catches back up. */
    @GameTest(template = "floor7", timeoutTicks = 200)
    public static void timeFreezeStopsTheChunk(GameTestHelper helper) {
        ServerPlayer player = fakePlayer(helper);
        Vec3 here = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(3, 1, 3)));
        player.moveTo(here.x, here.y, here.z);
        Powers.set(player, PlayerEssence.EMPTY.withLevel(Aspect.TIME, 3).withPool(Aspect.TIME, 150)
                .withSelected(Power.TIME_FREEZE));
        Powers.use(player);
        ServerLevel level = helper.getLevel();
        helper.assertTrue(TimeZones.at(level, BlockPos.containing(here)) != null, "no zone made");
        ItemEntity item = new ItemEntity(level, here.x, here.y + 4, here.z, new ItemStack(Items.APPLE));
        item.setNoGravity(false);
        helper.runAfterDelay(30, () -> level.addFreshEntity(item));
        double[] heldAt = new double[1];
        helper.runAfterDelay(35, () -> heldAt[0] = item.getY());
        helper.runAfterDelay(70, () -> {
            helper.assertTrue(TimeZones.stoppedAt(level, BlockPos.containing(here)), "the chunk should be stopped");
            helper.assertTrue(Math.abs(item.getY() - heldAt[0]) < 1.0e-6, "the item fell in stopped time: "
                    + heldAt[0] + " -> " + item.getY());
            Powers.use(player); // let go
            helper.assertTrue(TimeZones.at(level, BlockPos.containing(here)).releasing, "not let go");
            TimeZones.clear(level);
            item.discard();
            helper.succeed();
        });
    }

    /**
     * A culture of its own essence, research, and drams from an organism big enough to spare them make a gland
     * swell; the next infusion raises the power level to the gland's.
     */
    @GameTest(template = "floor7", timeoutTicks = 100)
    public static void glandGrowsALevel(GameTestHelper helper) {
        BlockPos corePos = new BlockPos(3, 1, 3);
        helper.setBlock(corePos, core(Aspect.DENSITY));
        for (Direction side : Direction.Plane.HORIZONTAL) {
            helper.setBlock(corePos.relative(side), ModBlocks.CHRONOFLESH.get());
            helper.setBlock(corePos.relative(side).relative(side.getClockWise()), ModBlocks.CHRONOFLESH.get());
        }
        helper.setBlock(corePos.above(), graft(ModBlocks.ENHANCING_GLAND.get()));
        ServerPlayer player = fakePlayer(helper);
        Research.set(player, Research.get(player).withCompleted(Research.LEVEL_2_POWERS));
        // Once the core has measured its body (8 flesh: room for 80 drams), fill it and feed the gland a culture.
        helper.runAfterDelay(10, () -> {
            OrganismCoreBlockEntity core = helper.getBlockEntity(corePos);
            core.feed(100);
            helper.assertTrue(core.stored() >= 64, "the organism cannot hold enough: " + core.stored());
            player.setItemInHand(InteractionHand.MAIN_HAND, CultureItem.of(Aspect.DENSITY));
            GraftBlockEntity gland = helper.getBlockEntity(corePos.above());
            gland.useItemOn(player.getMainHandItem(), player, InteractionHand.MAIN_HAND);
            helper.assertBlockProperty(corePos.above(), GlandBlock.LEVEL, 2);
            core.feed(100);
            gland.useWithoutItem(player);
            helper.assertTrue(Powers.get(player).level(Aspect.DENSITY) == 2, "infusion should reach level 2");
            helper.succeed();
        });
    }

    /**
     * A suit piece pressed into a swollen Time gland comes out Time-warded, for the gland's drams; a gland too small
     * (or not of Time) refuses it. Warded head to foot, a shift holds the era's full minutes; unwarded, half.
     */
    @GameTest(template = "floor7", timeoutTicks = 100)
    public static void timeGlandWardsTheSuit(GameTestHelper helper) {
        BlockPos corePos = new BlockPos(3, 1, 3);
        helper.setBlock(corePos, core(Aspect.TIME));
        BlockState timeFlesh = ModBlocks.CHRONOFLESH.get().defaultBlockState().setValue(FleshBlock.ASPECT, Aspect.TIME);
        for (Direction side : Direction.Plane.HORIZONTAL) {
            helper.setBlock(corePos.relative(side), timeFlesh);
            helper.setBlock(corePos.relative(side).relative(side.getClockWise()), timeFlesh);
        }
        helper.setBlock(corePos.above(), graft(ModBlocks.ENHANCING_GLAND.get()));
        ServerPlayer player = fakePlayer(helper);
        helper.runAfterDelay(10, () -> {
            OrganismCoreBlockEntity core = helper.getBlockEntity(corePos);
            core.feed(100);
            GraftBlockEntity gland = helper.getBlockEntity(corePos.above());
            ItemStack coat = new ItemStack(ModItems.CHRONO_LAB_COAT.get());
            gland.useItemOn(coat, player, InteractionHand.MAIN_HAND);
            helper.assertFalse(ChronoSuitItem.warded(coat), "a level 1 gland is too small to ward");
            helper.setBlock(corePos.above(), graft(ModBlocks.ENHANCING_GLAND.get()).setValue(GlandBlock.LEVEL, 2));
            GraftBlockEntity swollen = helper.getBlockEntity(corePos.above());
            int before = core.stored();
            swollen.useItemOn(coat, player, InteractionHand.MAIN_HAND);
            helper.assertTrue(ChronoSuitItem.warded(coat), "the coat should be warded (" + core.stored() + " drams)");
            helper.assertTrue(core.stored() == before - GraftBlockEntity.WARD_COST, "warding should cost the gland's drams");

            player.setItemSlot(EquipmentSlot.CHEST, coat);
            helper.assertTrue(TemporalExposure.wardedPieces(player) == 1, "one warded piece worn");
            Era era = new Era(Component.literal("Test"), Component.empty(), Items.CLOCK, Level.OVERWORLD, 1, Optional.empty(),
                    Optional.empty(), List.of(), 10, 4, Optional.empty(), 0, false);
            helper.assertTrue(TemporalShift.duration(era, 0) == 4 * 1200 / 2, "unwarded: half the minutes");
            helper.assertTrue(TemporalShift.duration(era, 4) == 4 * 1200, "warded head to foot: all of them");
            helper.succeed();
        });
    }

    /**
     * A gland with a Confluence Needle, on an organism wired by veins (Tap, vein, Maw) to one of another essence,
     * mixes both into the player, who learns their composite power.
     */
    @GameTest(template = "floor7")
    public static void confluenceTeachesAComposite(GameTestHelper helper) {
        BlockPos densityCore = new BlockPos(1, 1, 1);
        BlockPos speedCore = new BlockPos(5, 1, 1);
        helper.setBlock(densityCore, core(Aspect.DENSITY));
        helper.setBlock(speedCore, core(Aspect.SPEED));
        helper.setBlock(new BlockPos(2, 1, 1), ModBlocks.TAP.get().defaultBlockState().setValue(GraftBlock.FACING, Direction.EAST));
        helper.setBlock(new BlockPos(3, 1, 1), ModBlocks.BRASS_CONDUIT.get().defaultBlockState().setValue(BrassConduitBlock.VEIN, true));
        helper.setBlock(new BlockPos(4, 1, 1), ModBlocks.MAW.get().defaultBlockState().setValue(GraftBlock.FACING, Direction.WEST));
        helper.setBlock(densityCore.above(), graft(ModBlocks.ENHANCING_GLAND.get()).setValue(GlandBlock.NEEDLE, true));
        helper.<OrganismCoreBlockEntity>getBlockEntity(densityCore).feed(20);
        helper.<OrganismCoreBlockEntity>getBlockEntity(speedCore).feed(20);
        ServerPlayer player = fakePlayer(helper);
        Powers.set(player, PlayerEssence.EMPTY.withLevel(Aspect.DENSITY, 1).withLevel(Aspect.SPEED, 1));
        helper.<GraftBlockEntity>getBlockEntity(densityCore.above()).useWithoutItem(player);
        PlayerEssence essence = Powers.get(player);
        helper.assertTrue(essence.composites().contains(Power.IMPETUS), "Impetus not learned");
        helper.assertTrue(essence.knows(Power.IMPETUS), "Impetus not usable");
        helper.assertTrue(essence.pool(Aspect.SPEED) > 0, "no Speed came through the veins");
        helper.succeed();
    }

    /** The Chronocortex grows lobes into armature on wired Awareness, and learns a lesson it has room for. */
    @GameTest(template = "floor7", timeoutTicks = 400)
    public static void cortexGrowsAndLearns(GameTestHelper helper) {
        BlockPos cortexPos = new BlockPos(3, 1, 3);
        BlockPos corePos = new BlockPos(1, 1, 1);
        helper.setBlock(cortexPos, ModBlocks.CHRONOCORTEX.get());
        helper.setBlock(cortexPos.east(), ModBlocks.BRASS_ARMATURE.get());
        helper.setBlock(cortexPos.east().east(), ModBlocks.BRASS_ARMATURE.get());
        helper.setBlock(corePos, core(Aspect.AWARENESS));
        helper.setBlock(corePos.above(), graft(ModBlocks.GANGLION.get()));
        helper.<OrganismCoreBlockEntity>getBlockEntity(corePos).feed(16);
        ChronocortexBlockEntity cortex = helper.getBlockEntity(cortexPos);
        ServerPlayer player = fakePlayer(helper);
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(ModBlocks.CORTEX_LOBE.get(), cortexPos.east());
            helper.assertBlockPresent(ModBlocks.CORTEX_LOBE.get(), cortexPos.east().east());
            helper.assertTrue(cortex.lobes() == 2, "lobes counted: " + cortex.lobes());
            ItemStack clock = new ItemStack(Items.CLOCK);
            cortex.teach(clock, player);
            helper.assertTrue(cortex.knows(Lessons.TIME), "the clock was not learned");
            helper.assertTrue(clock.isEmpty(), "the clock was not used up");
            helper.assertFalse(cortex.knows(Lessons.WORDS), "words need more room");
        });
    }

    /** The Eternal Hourglass pours: an organism filled with its own essence, an empty jug with the chosen one, and the
     *  Chronocortex's armature grown into firm lobes at once. */
    @GameTest(template = "floor7")
    public static void hourglassPoursIntoMachines(GameTestHelper helper) {
        ServerPlayer player = fakePlayer(helper);
        ItemStack hourglass = new ItemStack(ModItems.ETERNAL_HOURGLASS.get());
        hourglass.set(ModDataComponents.ASPECT.get(), Aspect.SPEED);
        player.setItemInHand(InteractionHand.MAIN_HAND, hourglass);

        BlockPos corePos = new BlockPos(1, 1, 1);
        helper.setBlock(corePos, core(Aspect.SIZE));
        pourAt(helper, player, corePos);
        OrganismCoreBlockEntity core = helper.getBlockEntity(corePos);
        helper.assertTrue(core.stored() == core.capacity(), "organism not filled: " + core.stored() + "/" + core.capacity());

        BlockPos jugPos = new BlockPos(5, 1, 1);
        helper.setBlock(jugPos, ModBlocks.ESSENCE_JUG.get());
        pourAt(helper, player, jugPos);
        EssenceJugBlockEntity jug = helper.getBlockEntity(jugPos);
        helper.assertTrue(jug.contents() != null && jug.contents().aspect() == Aspect.SPEED
                && jug.contents().amount() == EssenceJugBlockEntity.CAPACITY, "jug not filled with Speed");

        BlockPos cortexPos = new BlockPos(3, 1, 4);
        helper.setBlock(cortexPos, ModBlocks.CHRONOCORTEX.get());
        helper.setBlock(cortexPos.east(), ModBlocks.BRASS_ARMATURE.get());
        helper.setBlock(cortexPos.above(), ModBlocks.BRASS_ARMATURE.get());
        pourAt(helper, player, cortexPos);
        helper.assertBlockProperty(cortexPos.east(), CortexLobeBlock.PART, CortexLobeBlock.Part.MATURE);
        helper.assertBlockProperty(cortexPos.above(), CortexLobeBlock.PART, CortexLobeBlock.Part.MATURE);
        helper.assertTrue(helper.<ChronocortexBlockEntity>getBlockEntity(cortexPos).lobes() == 2, "lobes not counted");
        helper.succeed();
    }

    private static void pourAt(GameTestHelper helper, ServerPlayer player, BlockPos relative) {
        BlockPos pos = helper.absolutePos(relative);
        ItemStack held = player.getMainHandItem();
        held.getItem().onItemUseFirst(held, new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)));
    }

    /** The Ice Age decodes with the game's own codec (the test server cannot load datapack dimensions). */
    @GameTest(template = "floor7")
    public static void iceAgeDimensionIsValid(GameTestHelper helper) throws Exception {
        var server = helper.getLevel().getServer();
        var resource = server.getResourceManager().getResourceOrThrow(ChronoGenesis.id("dimension/ice_age.json"));
        try (var reader = resource.openAsReader()) {
            var stem = LevelStem.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, server.registryAccess()), JsonParser.parseReader(reader));
            helper.assertTrue(stem.result().isPresent(), "the Ice Age does not decode: " + stem.error().map(Object::toString).orElse(""));
        }
        helper.succeed();
    }

    /** A shift into the Ice Age needs a stage 1 cortex, the lesson of the cold, the memory of the Precursors' age,
     *  a tether and the Time. */
    @GameTest(template = "floor7")
    public static void shiftNeedsItsCosts(GameTestHelper helper) {
        ServerPlayer player = fakePlayer(helper);
        Era era = Eras.get(helper.getLevel().registryAccess(), ChronoGenesis.id("ice_age"));
        helper.assertTrue(era != null, "the Ice Age era is missing");
        Map<Aspect, Integer> time = Map.of(Aspect.TIME, era.cost());
        TemporalShift.Check bare = TemporalShift.check(player, era, 0, Set.of(), Set.of(), Map.of());
        helper.assertFalse(bare.stage() || bare.lesson() || bare.memory() || bare.tether() || bare.time(), "nothing should be met yet");
        player.getInventory().add(new ItemStack(ModItems.CHRONO_TETHER.get()));
        TemporalShift.Check ready = TemporalShift.check(player, era, 1, Set.of(Lessons.COLD), Set.of(ChronoGenesis.id("precursor_era")), time);
        helper.assertTrue(ready.ok(), "everything is met: " + ready);
        helper.succeed();
    }

    /** Pulling the tether in another era brings the traveller back where they left. */
    @GameTest(template = "floor7")
    public static void tetherReelsHome(GameTestHelper helper) {
        ServerPlayer player = fakePlayer(helper);
        Vec3 home = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
        player.moveTo(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(5, 1, 5))));
        player.setData(ModAttachments.TIME_SHIFT, new TemporalShift.Shift(helper.getLevel().dimension(), home,
                ChronoGenesis.id("ice_age"), 1200));
        ItemStack tether = new ItemStack(ModItems.CHRONO_TETHER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, tether);
        tether.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.position().distanceTo(home) < 0.01, "not home: " + player.position());
        helper.assertFalse(TemporalShift.get(player).active(), "still shifted");
        helper.succeed();
    }

    /** The Precursor era decodes, its biomes are registered, and its cities' templates are there. */
    @GameTest(template = "floor7")
    public static void precursorEraIsValid(GameTestHelper helper) throws Exception {
        var server = helper.getLevel().getServer();
        var resource = server.getResourceManager().getResourceOrThrow(ChronoGenesis.id("dimension/precursor_era.json"));
        try (var reader = resource.openAsReader()) {
            var stem = LevelStem.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, server.registryAccess()), JsonParser.parseReader(reader));
            helper.assertTrue(stem.result().isPresent(), "the Precursor era does not decode: " + stem.error().map(Object::toString).orElse(""));
        }
        var biomes = server.registryAccess().registryOrThrow(Registries.BIOME);
        for (String biome : List.of("precursor_meadow", "precursor_jungle", "precursor_desert", "precursor_coast")) {
            helper.assertTrue(biomes.containsKey(ChronoGenesis.id(biome)), biome + " is missing");
        }
        // Every kingdom's town and capital, and the ruin of each: the same size, so the ruin lies under its city.
        for (String kingdom : List.of("sun", "tide", "deep", "verdant")) {
            for (String size : List.of("town", "capital")) {
                String name = "precursor_city_" + kingdom + "_" + size;
                StructureTemplate city = helper.getLevel().getStructureManager().get(ChronoGenesis.id(name)).orElseThrow();
                StructureTemplate ruin = helper.getLevel().getStructureManager().get(ChronoGenesis.id(name + "_ruin")).orElseThrow();
                helper.assertTrue(city.getSize().getX() == (size.equals("town") ? 65 : 97), name + " has the wrong size");
                helper.assertTrue(ruin.getSize().equals(city.getSize()), name + "'s ruin does not match it");
            }
        }
        helper.succeed();
    }

    /** A Precursor stays (it is part of its city), keeps near home, and speaks the same sounds for the same meaning. */
    @GameTest(template = "floor7")
    public static void precursorsLiveInTheirCity(GameTestHelper helper) {
        Precursor precursor = helper.spawn(ModEntities.PRECURSOR.get(), new BlockPos(3, 1, 3));
        helper.assertFalse(precursor.removeWhenFarAway(1000), "Precursors should not despawn");
        helper.assertTrue(Precursor.tongue(4).equals(Precursor.tongue(4)) && !Precursor.tongue(4).equals(Precursor.tongue(5)),
                "their words should be fixed and different");
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(precursor.hasRestriction(), "it should keep to its home");
            helper.succeed();
        });
    }

    /**
     * An offshoot planted in an era waits in the present as a Fossil Cortex; a Nerve Spool from it to the Chronocortex
     * gives the cortex the era's memory (and opens the next era), and its people's tongue to whoever laid it.
     */
    @GameTest(template = "floor7")
    public static void offshootBecomesAMemory(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ResourceLocation era = ChronoGenesis.id("precursor_era");
        BlockPos fossilPos = new BlockPos(1, 1, 1);
        Remnants.record(level.getServer(), era, helper.absolutePos(fossilPos),
                Remnants.crumbled(ModBlocks.CORTEX_OFFSHOOT.get().defaultBlockState()));
        helper.assertTrue(Remnants.materialize(level) >= 1, "nothing grew");
        helper.assertBlockPresent(ModBlocks.FOSSIL_CORTEX.get(), fossilPos);
        FossilCortexBlockEntity fossil = helper.getBlockEntity(fossilPos);
        helper.assertTrue(era.equals(fossil.era()), "the fossil should remember the Precursors");

        BlockPos cortexPos = new BlockPos(5, 1, 5);
        helper.setBlock(cortexPos, ModBlocks.CHRONOCORTEX.get());
        ChronocortexBlockEntity cortex = helper.getBlockEntity(cortexPos);
        ServerPlayer player = fakePlayer(helper);
        ItemStack spool = new ItemStack(ModItems.NERVE_SPOOL.get());
        NerveSpoolItem.bind(spool, level, helper.absolutePos(fossilPos), player);
        cortex.link(spool, player);
        helper.assertTrue(cortex.memories().contains(era), "no memory");
        helper.assertTrue(cortex.awareness() >= ChronocortexBlockEntity.MEMORY_AWARENESS, "the memory should make it aware");
        helper.assertBlockProperty(fossilPos, FossilCortexBlock.AWAKE, true);
        helper.assertTrue(Research.has(player, Precursor.TONGUE), "the linker should understand the Precursors");
        helper.assertTrue(spool.isEmpty(), "the spool should be laid");
        Era iceAge = Eras.get(level.registryAccess(), ChronoGenesis.id("ice_age"));
        helper.assertTrue(TemporalShift.check(player, iceAge, 1, Set.of(Lessons.COLD), cortex.memories(), Map.of()).memory(),
                "the Ice Age should now be within memory");
        helper.succeed();
    }

    /** Lasting blocks crumble into their remnant forms, keeping their shape; gold stays gold; wood does not last. */
    @GameTest(template = "floor7")
    public static void lastingBlocksCrumble(GameTestHelper helper) {
        BlockState stairs = Blocks.STONE_BRICK_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, Direction.EAST).setValue(StairBlock.HALF, Half.TOP);
        BlockState crumbled = Remnants.crumbled(stairs);
        helper.assertTrue(crumbled != null && crumbled.is(Blocks.MOSSY_STONE_BRICK_STAIRS)
                && crumbled.getValue(StairBlock.FACING) == Direction.EAST && crumbled.getValue(StairBlock.HALF) == Half.TOP,
                "stairs should crumble into mossy stairs, the same way round: " + crumbled);
        helper.assertTrue(Remnants.crumbled(Blocks.GOLD_BLOCK.defaultBlockState()).is(Blocks.GOLD_BLOCK), "gold should not tarnish");
        helper.assertTrue(Remnants.crumbled(Blocks.OAK_PLANKS.defaultBlockState()) == null, "wood should not last");
        helper.assertTrue(Remnants.crumbled(ModBlocks.CORTEX_OFFSHOOT.get().defaultBlockState()).is(ModBlocks.FOSSIL_CORTEX.get()),
                "an offshoot should last as a Fossil Cortex");
        helper.succeed();
    }

    /** A remnant appears in the present where it was placed in its era, and is taken back if it never lasted. */
    @GameTest(template = "floor7")
    public static void remnantsAppearAndVanish(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ResourceLocation era = ChronoGenesis.id("precursor_era");
        BlockPos pos = new BlockPos(2, 1, 2);
        Remnants.record(level.getServer(), era, helper.absolutePos(pos), Remnants.crumbled(Blocks.STONE_BRICKS.defaultBlockState()));
        Remnants.materialize(level);
        helper.assertBlockPresent(Blocks.CRACKED_STONE_BRICKS, pos);
        Remnants.forget(level.getServer(), era, helper.absolutePos(pos));
        helper.assertBlockPresent(Blocks.AIR, pos);
        helper.succeed();
    }

    /** Once the cranium closes over the jar only lobes can be reached: a Nerve Spool used on a lobe still links. */
    @GameTest(template = "floor7")
    public static void spoolLinksThroughALobe(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ResourceLocation era = ChronoGenesis.id("precursor_era");
        BlockPos fossilPos = new BlockPos(1, 1, 1);
        Remnants.record(level.getServer(), era, helper.absolutePos(fossilPos),
                Remnants.crumbled(ModBlocks.CORTEX_OFFSHOOT.get().defaultBlockState()));
        Remnants.materialize(level);
        BlockPos cortexPos = new BlockPos(5, 1, 5);
        BlockPos lobePos = cortexPos.east();
        helper.setBlock(cortexPos, ModBlocks.CHRONOCORTEX.get());
        helper.setBlock(lobePos, ModBlocks.CORTEX_LOBE.get().defaultBlockState().setValue(CortexLobeBlock.PART, CortexLobeBlock.Part.CEREBRUM));
        ServerPlayer player = fakePlayer(helper);
        ItemStack spool = new ItemStack(ModItems.NERVE_SPOOL.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, spool);
        BlockPos fossil = helper.absolutePos(fossilPos);
        level.getBlockState(fossil).useItemOn(spool, level, player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(fossil), Direction.UP, fossil, false));
        BlockPos lobe = helper.absolutePos(lobePos);
        level.getBlockState(lobe).useItemOn(spool, level, player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(lobe), Direction.UP, lobe, false));
        helper.assertTrue(helper.<ChronocortexBlockEntity>getBlockEntity(cortexPos).memories().contains(era),
                "the spool used on a lobe did not link");
        helper.succeed();
    }

    /** The Jurassic Age decodes, its biomes are registered, and its temple's template is there. */
    @GameTest(template = "floor7")
    public static void jurassicAgeIsValid(GameTestHelper helper) throws Exception {
        var server = helper.getLevel().getServer();
        var resource = server.getResourceManager().getResourceOrThrow(ChronoGenesis.id("dimension/jurassic_age.json"));
        try (var reader = resource.openAsReader()) {
            var stem = LevelStem.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, server.registryAccess()), JsonParser.parseReader(reader));
            helper.assertTrue(stem.result().isPresent(), "the Jurassic Age does not decode: " + stem.error().map(Object::toString).orElse(""));
        }
        var biomes = server.registryAccess().registryOrThrow(Registries.BIOME);
        for (String biome : List.of("jurassic_fernlands", "jurassic_jungle", "jurassic_swamp", "jurassic_coast")) {
            helper.assertTrue(biomes.containsKey(ChronoGenesis.id(biome)), biome + " is missing");
        }
        StructureTemplate temple = helper.getLevel().getStructureManager().get(ChronoGenesis.id("jurassic_temple")).orElseThrow();
        helper.assertTrue(temple.getSize().getX() == 35 && temple.getSize().getZ() == 35, "the temple has the wrong size");
        // Built round an obelisk at its centre (where the present's obelisk stands), the core tied to it by sinew.
        StructurePlaceSettings asIs = new StructurePlaceSettings();
        helper.assertTrue(temple.filterBlocks(BlockPos.ZERO, asIs, ModBlocks.OBELISK_CAPSTONE.get()).stream()
                .anyMatch(info -> info.pos().equals(new BlockPos(17, 27, 17))), "no obelisk at the centre");
        helper.assertTrue(temple.filterBlocks(BlockPos.ZERO, asIs, ModBlocks.TEMPORAL_CORE.get()).stream()
                .anyMatch(info -> info.pos().equals(new BlockPos(17, 3, 15))), "no core beside the obelisk");
        helper.assertTrue(temple.filterBlocks(BlockPos.ZERO, asIs, ModBlocks.CORE_SINEW.get()).stream()
                .anyMatch(info -> info.pos().equals(new BlockPos(17, 3, 16))), "the core is not tied to the obelisk");
        helper.succeed();
    }

    /** The Jurassic Age has two ways in: the Ice Age's memory, or the Precursors' and the lesson of Deep Time. */
    @GameTest(template = "floor7")
    public static void jurassicHasTwoRoutes(GameTestHelper helper) {
        ServerPlayer player = fakePlayer(helper);
        player.getInventory().add(new ItemStack(ModItems.CHRONO_TETHER.get()));
        Era era = Eras.get(helper.getLevel().registryAccess(), ChronoGenesis.id("jurassic_age"));
        Map<Aspect, Integer> time = Map.of(Aspect.TIME, era.cost());
        ResourceLocation precursors = ChronoGenesis.id("precursor_era");
        ResourceLocation deepTime = ChronoGenesis.id("deep_time");
        helper.assertTrue(TemporalShift.check(player, era, 2, Set.of(), Set.of(ChronoGenesis.id("ice_age")), time).ok(),
                "the Ice Age's memory should open it");
        helper.assertTrue(TemporalShift.check(player, era, 2, Set.of(deepTime), Set.of(precursors), time).ok(),
                "the Precursors' memory and Deep Time should open it");
        helper.assertFalse(TemporalShift.check(player, era, 2, Set.of(), Set.of(precursors), time).ok(),
                "the Precursors' memory alone should not");
        helper.succeed();
    }

    /** A running Temporal Core holds its temple still; three broken seals (any three) bring the barrier down. */
    @GameTest(template = "floor7")
    public static void templeBarrierFallsAfterThreeSeals(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(3, 1, 3), ModBlocks.TEMPORAL_CORE.get());
        // A new core joins the barrier list as its block entity loads, on the next tick.
        helper.runAfterDelay(2, () -> {
            BlockPos inside = helper.absolutePos(new BlockPos(5, 2, 5));
            helper.assertTrue(TempleBarrier.holds(level, inside), "the barrier should hold the temple");
            List<BlockPos> seals = List.of(new BlockPos(1, 1, 1), new BlockPos(5, 1, 1), new BlockPos(1, 1, 5));
            helper.setBlock(seals.get(0), ModBlocks.HEAVY_SEAL.get());
            helper.setBlock(seals.get(1), ModBlocks.SWIFT_SEAL.get());
            helper.setBlock(seals.get(2), ModBlocks.STILLED_SEAL.get());
            SealBlock.breakSeal(level, helper.absolutePos(seals.get(0)));
            SealBlock.breakSeal(level, helper.absolutePos(seals.get(1)));
            helper.assertTrue(TempleBarrier.holds(level, inside), "two seals should not be enough");
            SealBlock.breakSeal(level, helper.absolutePos(seals.get(2)));
            helper.assertFalse(TempleBarrier.holds(level, inside), "three seals should bring the barrier down");
            helper.assertBlockProperty(new BlockPos(3, 1, 3), TemporalCoreBlock.ACTIVE, false);
            helper.succeed();
        });
    }

    /** Each seal answers its own gift: shrunk for the Narrow Seal, Glowing Sight for the Veiled, slowed time for the Stilled. */
    @GameTest(template = "floor7")
    public static void sealsAnswerTheirGifts(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = fakePlayer(helper);
        BlockPos pos = helper.absolutePos(new BlockPos(3, 1, 3));
        SealBlock narrow = ModBlocks.NARROW_SEAL.get();
        SealBlock veiled = ModBlocks.VEILED_SEAL.get();
        SealBlock stilled = ModBlocks.STILLED_SEAL.get();
        helper.assertTrue(narrow.refusal(player, level, pos) != null, "a full-sized hand should not turn it");
        helper.assertTrue(veiled.refusal(player, level, pos) != null, "unseeing hands should not find it");
        helper.assertTrue(stilled.refusal(player, level, pos) != null, "it should spin too fast in normal time");
        Powers.set(player, PlayerEssence.EMPTY.withLevel(Aspect.SIZE, 1).withLevel(Aspect.AWARENESS, 1)
                .withPool(Aspect.SIZE, 10).withPool(Aspect.AWARENESS, 10)
                .withPassive(Power.SHRINK, true).withPassive(Power.GLOWING_SIGHT, true));
        Powers.applyModifiers(player);
        helper.assertTrue(narrow.refusal(player, level, pos) == null, "shrunk, it should turn: scale " + player.getScale());
        helper.assertTrue(veiled.refusal(player, level, pos) == null, "Glowing Sight should find it");
        helper.succeed();
    }

    /**
     * A harnessed Titanosaur carries its cargo: real blocks (a chest keeps its contents) moved and turned with it.
     * Breaking the harness drops the whole cargo.
     */
    @GameTest(template = "floor7", timeoutTicks = 200)
    public static void titanosaurCarriesItsCargo(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Titanosaur beast = helper.spawn(ModEntities.TITANOSAUR.get(), new BlockPos(3, 1, 3));
        beast.setNoAi(true);
        beast.setYRot(0);
        ServerPlayer player = fakePlayer(helper);
        helper.runAfterDelay(10, () -> {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.DINO_HARNESS.get()));
            beast.interact(player, InteractionHand.MAIN_HAND);
            BlockPos harness = beast.cargo().harness();
            helper.assertTrue(harness != null && level.getBlockState(harness).is(ModBlocks.BUCKLED_HARNESS.get()), "not harnessed");
            BlockPos chestPos = harness.east();
            level.setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
            ((ChestBlockEntity) level.getBlockEntity(chestPos)).setItem(0, new ItemStack(Items.DIAMOND, 3));
            helper.assertTrue(beast.cargo().attach(chestPos), "the chest should join the cargo");

            // A step east: the cargo follows, the chest with its diamonds.
            beast.setPos(beast.getX() + 1, beast.getY(), beast.getZ());
            helper.assertTrue(beast.cargo().follow(), "the cargo should follow");
            BlockPos moved = harness.east();
            helper.assertTrue(moved.equals(beast.cargo().harness()), "the harness did not move: " + beast.cargo().harness());
            helper.assertTrue(level.getBlockState(moved.east()).is(Blocks.CHEST), "the chest did not move");
            helper.assertTrue(((ChestBlockEntity) level.getBlockEntity(moved.east())).getItem(0).getCount() == 3,
                    "the chest lost its contents");
            helper.assertTrue(level.getBlockState(harness).isAir(), "the old harness spot should be empty");

            // A quarter turn clockwise: east becomes south.
            beast.setYRot(90);
            helper.assertTrue(beast.cargo().follow(), "the cargo should turn");
            helper.assertTrue(level.getBlockState(moved.south()).is(Blocks.CHEST), "the chest did not turn with it");

            // The harness breaks: everything falls.
            level.destroyBlock(moved, true);
            helper.assertFalse(beast.cargo().harnessed(), "still harnessed");
            helper.assertTrue(level.getBlockState(moved.south()).isAir(), "the cargo should have fallen");
            beast.discard();
            helper.succeed();
        });
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
    /** Sneak-using the Lens goes Echo, Decades, Centuries, then each era its holder has stood in (oldest last), and round. */
    @GameTest(template = "floor7")
    public static void lensLooksIntoVisitedEras(GameTestHelper helper) {
        ServerPlayer player = fakePlayer(helper);
        ItemStack lens = new ItemStack(ModItems.TEMPORAL_LENS.get());
        lens.set(ModDataComponents.LENS_DEPTH.get(), LensDepth.CENTURIES);
        TemporalLensItem.cycle(player, lens);
        helper.assertTrue(TemporalLensItem.era(lens) == null && TemporalLensItem.depth(lens) == LensDepth.ECHO,
                "with no era visited it should go round to Echo");
        TemporalShift.remember(player, ChronoGenesis.id("jurassic_age"));
        TemporalShift.remember(player, ChronoGenesis.id("precursor_era"));
        lens.set(ModDataComponents.LENS_DEPTH.get(), LensDepth.CENTURIES);
        TemporalLensItem.cycle(player, lens);
        helper.assertTrue(ChronoGenesis.id("precursor_era").equals(TemporalLensItem.era(lens)), "the nearest era should come first");
        TemporalLensItem.cycle(player, lens);
        helper.assertTrue(ChronoGenesis.id("jurassic_age").equals(TemporalLensItem.era(lens)), "then the older one");
        helper.assertTrue(TemporalLensItem.cost(lens) == TemporalLensItem.ERA_COST, "looking into an era costs more");
        TemporalLensItem.cycle(player, lens);
        helper.assertTrue(TemporalLensItem.era(lens) == null && TemporalLensItem.depth(lens) == LensDepth.ECHO, "then round again");
        helper.succeed();
    }

    /**
     * A Precursor Tablet means nothing until their tongue is understood; read then, its fragment is recorded, and the
     * last key fragment brings the whole story together.
     */
    @GameTest(template = "floor7")
    public static void tabletsTellWhyTheyFell(GameTestHelper helper) {
        ServerPlayer player = fakePlayer(helper);
        ItemStack tablet = PrecursorTabletItem.of("nails_1");
        player.setItemInHand(InteractionHand.MAIN_HAND, tablet);
        tablet.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertFalse(PrecursorLore.known(player, "nails_1"), "unreadable without their tongue");
        Research.set(player, Research.get(player).withCompleted(Precursor.TONGUE));
        tablet.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(PrecursorLore.known(player, "nails_1"), "read, it should be recorded");
        for (String key : PrecursorLore.KEY) {
            helper.assertFalse(PrecursorLore.known(player, PrecursorLore.SLIP), "not everything is known yet");
            ItemStack fragment = PrecursorTabletItem.of(key);
            player.setItemInHand(InteractionHand.MAIN_HAND, fragment);
            fragment.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        }
        helper.assertTrue(PrecursorLore.known(player, PrecursorLore.SLIP), "every kingdom's fall and the voice: it should fall into place");
        helper.succeed();
    }

    /** A Precursor remembers its kingdom (its city's template sets it). */
    @GameTest(template = "floor7")
    public static void precursorsKnowTheirKingdom(GameTestHelper helper) {
        Precursor precursor = helper.spawn(ModEntities.PRECURSOR.get(), new BlockPos(3, 1, 3));
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        precursor.saveWithoutId(tag);
        tag.putString("Kingdom", "tide");
        precursor.load(tag);
        helper.assertTrue(precursor.kingdom().equals("tide"), "it should know it is of the Tide Kingdom");
        helper.succeed();
    }

    /** The Hadean and Before Time decode; the Hadean's air is toxic, and only warded goggles filter it. */
    @GameTest(template = "floor7")
    public static void lastErasAreValid(GameTestHelper helper) throws Exception {
        var server = helper.getLevel().getServer();
        for (String name : List.of("hadean_eon", "before_time")) {
            var resource = server.getResourceManager().getResourceOrThrow(ChronoGenesis.id("dimension/" + name + ".json"));
            try (var reader = resource.openAsReader()) {
                var stem = LevelStem.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, server.registryAccess()), JsonParser.parseReader(reader));
                helper.assertTrue(stem.result().isPresent(), name + " does not decode: " + stem.error().map(Object::toString).orElse(""));
            }
        }
        Era hadean = Eras.get(helper.getLevel().registryAccess(), ChronoGenesis.id("hadean_eon"));
        Era before = Eras.get(helper.getLevel().registryAccess(), ChronoGenesis.id("before_time"));
        helper.assertTrue(hadean != null && hadean.toxicAir(), "the Hadean's air should be toxic");
        helper.assertTrue(before != null && before.memory().orElseThrow().equals(ChronoGenesis.id("hadean_eon")), "Before Time follows the Hadean");
        ServerPlayer player = fakePlayer(helper);
        helper.assertFalse(TemporalShift.breathes(player), "bare-faced, the air burns");
        ItemStack goggles = new ItemStack(ModItems.CHRONO_GOGGLES.get());
        player.setItemSlot(EquipmentSlot.HEAD, goggles);
        helper.assertFalse(TemporalShift.breathes(player), "plain goggles do not filter it");
        ChronoSuitItem.ward(goggles);
        helper.assertTrue(TemporalShift.breathes(player), "warded goggles do");
        // The structures that share placements decode as the custom type.
        var structures = server.registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (String name : List.of("obelisk", "jurassic_temple", "precursor_city", "precursor_ruin")) {
            helper.assertTrue(structures.get(ChronoGenesis.id(name)) instanceof PlacedTemplateStructure, name + " should be a placed template");
        }
        helper.succeed();
    }

    /**
     * Before Time: an arena with four Unmade Nails. The Unbegotten's wounds unhappen until all four are driven; then
     * time has begun and it is mortal.
     */
    @GameTest(template = "floor7", timeoutTicks = 200)
    public static void drivingTheNailsBeginsTime(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos centre = helper.absolutePos(new BlockPos(3, 120, 3));
        ElderArena.build(level, centre, level.getRandom());
        Unbegotten god = ModEntities.UNBEGOTTEN.get().create(level);
        god.moveTo(centre.getX() + 0.5, centre.getY() + 10, centre.getZ() + 0.5, 0, 0);
        god.setHome(centre);
        helper.assertFalse(god.mortal(), "before time begins it should not die");
        ServerPlayer player = fakePlayer(helper);
        List<BlockPos> nails = ElderArena.nails(centre);
        helper.assertFalse(UnmadeNailBlock.drive(level, nails.get(0), player), "a body without Time cannot drive a nail");
        player.getInventory().add(new ItemStack(ModItems.ETERNAL_HOURGLASS.get()));
        for (BlockPos nail : nails) {
            helper.assertTrue(UnmadeNailBlock.drive(level, nail, player), "the nail should go in");
        }
        helper.assertTrue(ElderArena.driven(level, centre) == 4, "all four nails should be driven");
        helper.assertTrue(god.mortal(), "with time begun its wounds should stay");
        god.discard();
        helper.succeed();
    }

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

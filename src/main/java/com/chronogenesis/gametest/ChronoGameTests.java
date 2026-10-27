package com.chronogenesis.gametest;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.essence.Aspect;
import com.chronogenesis.essence.MobEssence;
import com.chronogenesis.organism.BrassConduitBlock;
import com.chronogenesis.organism.CorePhase;
import com.chronogenesis.organism.FleshBlock;
import com.chronogenesis.organism.GraftBlock;
import com.chronogenesis.organism.GraftBlockEntity;
import com.chronogenesis.organism.OrganismCoreBlock;
import com.chronogenesis.organism.OrganismCoreBlockEntity;
import com.chronogenesis.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Automated in-world tests. Run with {@code gradlew runGameTestServer} (CI runs them on feature branches).
 * The {@code floor7} template is a 7x5x7 space with a stone floor; see tools/make_test_structures.py.
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
                .setValue(GraftBlock.FACING, net.minecraft.core.Direction.EAST));
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
                .setValue(GraftBlock.FACING, net.minecraft.core.Direction.EAST));
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

    private static net.minecraft.world.level.block.state.BlockState core(Aspect aspect) {
        return ModBlocks.ORGANISM_CORE.get().defaultBlockState()
                .setValue(FleshBlock.ASPECT, aspect).setValue(OrganismCoreBlock.PHASE, CorePhase.SEED);
    }

    private static net.minecraft.world.level.block.state.BlockState graft(net.minecraft.world.level.block.Block block) {
        return block.defaultBlockState().setValue(GraftBlock.FACING, net.minecraft.core.Direction.UP);
    }
}

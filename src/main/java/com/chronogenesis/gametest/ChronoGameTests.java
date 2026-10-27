package com.chronogenesis.gametest;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.essence.Aspect;
import com.chronogenesis.essence.MobEssence;
import com.chronogenesis.organism.CorePhase;
import com.chronogenesis.organism.FleshBlock;
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
}

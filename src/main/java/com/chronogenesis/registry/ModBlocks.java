package com.chronogenesis.registry;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.biocomputer.ChronocortexBlock;
import com.chronogenesis.biocomputer.DissectionTableBlock;
import com.chronogenesis.lens.FadedBlueprintBlock;
import com.chronogenesis.organism.EssenceJugBlock;
import com.chronogenesis.organism.FleshBlock;
import com.chronogenesis.organism.GraftBlock;
import com.chronogenesis.organism.GraftType;
import com.chronogenesis.organism.OrganismCoreBlock;
import com.chronogenesis.time.TimeTearBlock;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ChronoGenesis.MODID);

    /** Storage block for brass: copper alloyed with gold, the mad scientist's metal. */
    public static final DeferredBlock<Block> BRASS_BLOCK = BLOCKS.registerSimpleBlock("brass_block",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GOLD)
                    .strength(3.0f, 6.0f)
                    .sound(SoundType.COPPER)
                    .requiresCorrectToolForDrops());

    /** The frame organisms grow into, twelve brass beams. Its shape is the player's design decision. */
    public static final DeferredBlock<Block> BRASS_ARMATURE = BLOCKS.registerSimpleBlock("brass_armature",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GOLD)
                    .strength(2.0f, 6.0f)
                    .sound(SoundType.COPPER)
                    .noOcclusion()
                    .requiresCorrectToolForDrops());

    /** Living tissue grown over the frame. Tinted by the aspect of its organism. */
    public static final DeferredBlock<FleshBlock> CHRONOFLESH = BLOCKS.registerBlock("chronoflesh", FleshBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(1.0f, 2.0f)
                    .sound(SoundType.SLIME_BLOCK)
                    .pushReaction(PushReaction.BLOCK));

    /** Where a culture took hold: stores the organism's essence and drives growth and hunger. */
    public static final DeferredBlock<OrganismCoreBlock> ORGANISM_CORE = BLOCKS.registerBlock("organism_core", OrganismCoreBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(1.5f, 3.0f)
                    .sound(SoundType.SLIME_BLOCK)
                    .pushReaction(PushReaction.BLOCK));

    /** Pipe frame for veins: a Tap grows veins along it, carrying essence to a Maw. */
    public static final DeferredBlock<com.chronogenesis.organism.BrassConduitBlock> BRASS_CONDUIT = BLOCKS.registerBlock("brass_conduit",
            com.chronogenesis.organism.BrassConduitBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GOLD)
                    .strength(1.5f, 6.0f)
                    .sound(SoundType.COPPER)
                    .noOcclusion()
                    .requiresCorrectToolForDrops());

    public static final DeferredBlock<GraftBlock> TAP = graft("tap", GraftType.TAP);
    public static final DeferredBlock<GraftBlock> MAW = graft("maw", GraftType.MAW);
    public static final DeferredBlock<GraftBlock> STASIS_BLADDER = graft("stasis_bladder", GraftType.STASIS_BLADDER);
    public static final DeferredBlock<GraftBlock> STARTING_GATE = graft("starting_gate", GraftType.STARTING_GATE);
    public static final DeferredBlock<GraftBlock> FINISH = graft("finish", GraftType.FINISH);
    public static final DeferredBlock<GraftBlock> LEECH_TENDRIL = graft("leech_tendril", GraftType.LEECH_TENDRIL);
    public static final DeferredBlock<GraftBlock> ENHANCING_GLAND = graft("enhancing_gland", GraftType.ENHANCING_GLAND);
    public static final DeferredBlock<GraftBlock> HEART = graft("heart", GraftType.HEART);
    public static final DeferredBlock<GraftBlock> EYE = graft("eye", GraftType.EYE);
    public static final DeferredBlock<GraftBlock> CLOCK = graft("clock", GraftType.CLOCK);
    public static final DeferredBlock<GraftBlock> GANGLION = graft("ganglion", GraftType.GANGLION);
    public static final DeferredBlock<GraftBlock> TEMPORAL_FIELD = graft("temporal_field", GraftType.TEMPORAL_FIELD);

    /** The reanimation altar: a brain in a jar, Awareness and Time, lightning rods, a storm. */
    public static final DeferredBlock<DissectionTableBlock> DISSECTION_TABLE = BLOCKS.registerBlock(
            "dissection_table", DissectionTableBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.5f, 6.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .requiresCorrectToolForDrops());

    /** The woken brain: the Chronocortex. Breaking it kills it; only the table is left. */
    public static final DeferredBlock<ChronocortexBlock> CHRONOCORTEX = BLOCKS.registerBlock(
            "chronocortex", ChronocortexBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PINK)
                    .strength(5.0f, 1200.0f)
                    .sound(SoundType.GLASS)
                    .noOcclusion()
                    .lightLevel(state -> 7)
                    .requiresCorrectToolForDrops());

    public static final DeferredBlock<EssenceJugBlock> ESSENCE_JUG = BLOCKS.registerBlock("essence_jug", EssenceJugBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.NONE)
                    .strength(0.3f)
                    .sound(SoundType.GLASS)
                    .noOcclusion());

    /** Left on the tables of Abandoned Laboratories. Readable only through the Temporal Lens; crumbles if taken. */
    public static final DeferredBlock<FadedBlueprintBlock> FADED_BLUEPRINT = BLOCKS.registerBlock("faded_blueprint",
            FadedBlueprintBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_WHITE)
                    .strength(0.1f)
                    .sound(SoundType.WOOL)
                    .noCollission()
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY));

    /** A rift opened by too much Temporal Stress. Cannot be mined: seal it with Density. */
    public static final DeferredBlock<TimeTearBlock> TIME_TEAR = BLOCKS.registerBlock("time_tear", TimeTearBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(-1.0f, 3600000.0f)
                    .noCollission()
                    .noOcclusion()
                    .noLootTable()
                    .lightLevel(state -> 9)
                    .pushReaction(PushReaction.BLOCK));

    private static DeferredBlock<GraftBlock> graft(String name, GraftType type) {
        return BLOCKS.registerBlock(name, properties -> new GraftBlock(type, properties),
                BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_PINK)
                        .strength(0.8f)
                        .sound(SoundType.HONEY_BLOCK)
                        .noOcclusion()
                        .pushReaction(PushReaction.DESTROY));
    }

    private ModBlocks() {}
}

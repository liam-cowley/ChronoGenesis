package com.chronogenesis.registry;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.biocomputer.ChronocortexBlock;
import com.chronogenesis.biocomputer.CortexLobeBlock;
import com.chronogenesis.biocomputer.DissectionTableBlock;
import com.chronogenesis.lens.FadedBlueprintBlock;
import com.chronogenesis.lens.ObeliskCapstoneBlock;
import com.chronogenesis.organism.EssenceJugBlock;
import com.chronogenesis.organism.FleshBlock;
import com.chronogenesis.organism.GlandBlock;
import com.chronogenesis.organism.GraftBlock;
import com.chronogenesis.organism.GraftType;
import com.chronogenesis.organism.OrganismCoreBlock;
import com.chronogenesis.time.TimeTearBlock;
import com.chronogenesis.timetravel.CortexOffshootBlock;
import com.chronogenesis.timetravel.FossilCortexBlock;

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
    /** Grows in level when fed cultures; takes a Confluence Needle to mix two essences. */
    public static final DeferredBlock<GlandBlock> ENHANCING_GLAND = BLOCKS.registerBlock("enhancing_gland", GlandBlock::new,
            graftProperties());
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

    /** Brain matter the Chronocortex grows over Brass Armature around its jar: room in its mind. */
    public static final DeferredBlock<CortexLobeBlock> CORTEX_LOBE = BLOCKS.registerBlock("cortex_lobe", CortexLobeBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PINK)
                    .strength(1.5f, 6.0f)
                    .sound(SoundType.HONEY_BLOCK)
                    .lightLevel(state -> state.getValue(CortexLobeBlock.PART).formed() ? 5 : 0)
                    .pushReaction(PushReaction.BLOCK));

    /** A bud of the Chronocortex in a jar, to be planted in a past era. */
    public static final DeferredBlock<CortexOffshootBlock> CORTEX_OFFSHOOT = BLOCKS.registerBlock("cortex_offshoot",
            CortexOffshootBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PINK)
                    .strength(0.5f)
                    .sound(SoundType.GLASS)
                    .noOcclusion()
                    .lightLevel(state -> 3)
                    .pushReaction(PushReaction.DESTROY));

    /** An offshoot grown through the ages and petrified: the old Chronocortex waiting in the present. */
    public static final DeferredBlock<FossilCortexBlock> FOSSIL_CORTEX = BLOCKS.registerBlock("fossil_cortex",
            FossilCortexBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(50.0f, 1200.0f)
                    .sound(SoundType.DEEPSLATE)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(FossilCortexBlock.AWAKE) ? 7 : 2)
                    .pushReaction(PushReaction.BLOCK)
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

    // Obelisks: nails holding time in place. Untouched by weather or tools.
    private static BlockBehaviour.Properties obelisk() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(-1.0f, 3600000.0f)
                .noLootTable()
                .sound(SoundType.DEEPSLATE_TILES)
                .pushReaction(PushReaction.BLOCK);
    }

    public static final DeferredBlock<Block> OBELISK_STONE = BLOCKS.registerSimpleBlock("obelisk_stone", obelisk());
    public static final DeferredBlock<Block> OBELISK_INSCRIPTION = BLOCKS.registerSimpleBlock("obelisk_inscription",
            obelisk().lightLevel(state -> 4));
    public static final DeferredBlock<ObeliskCapstoneBlock> OBELISK_CAPSTONE = BLOCKS.registerBlock("obelisk_capstone",
            ObeliskCapstoneBlock::new, obelisk().noOcclusion().lightLevel(state -> 6));

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
        return BLOCKS.registerBlock(name, properties -> new GraftBlock(type, properties), graftProperties());
    }

    private static BlockBehaviour.Properties graftProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PINK)
                .strength(0.8f)
                .sound(SoundType.HONEY_BLOCK)
                .noOcclusion()
                .pushReaction(PushReaction.DESTROY);
    }

    private ModBlocks() {}
}

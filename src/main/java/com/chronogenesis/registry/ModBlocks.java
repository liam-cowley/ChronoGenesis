package com.chronogenesis.registry;

import com.chronogenesis.ChronoGenesis;
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

    /** The frame organisms grow into. Its shape is the player's design decision. */
    public static final DeferredBlock<Block> COPPER_ARMATURE = BLOCKS.registerSimpleBlock("copper_armature",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
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

    public static final DeferredBlock<GraftBlock> TAP = graft("tap", GraftType.TAP);
    public static final DeferredBlock<GraftBlock> LEECH_TENDRIL = graft("leech_tendril", GraftType.LEECH_TENDRIL);
    public static final DeferredBlock<GraftBlock> ENHANCING_GLAND = graft("enhancing_gland", GraftType.ENHANCING_GLAND);

    public static final DeferredBlock<EssenceJugBlock> ESSENCE_JUG = BLOCKS.registerBlock("essence_jug", EssenceJugBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.NONE)
                    .strength(0.3f)
                    .sound(SoundType.GLASS)
                    .noOcclusion());

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

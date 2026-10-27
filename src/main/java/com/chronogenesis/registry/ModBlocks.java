package com.chronogenesis.registry;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.biomech.LeechTendrilBlock;
import com.chronogenesis.biomech.MawBlock;
import com.chronogenesis.biomech.OrganismCoreBlock;
import com.chronogenesis.biomech.OrganismFleshBlock;
import com.chronogenesis.biomech.TapBlock;
import com.chronogenesis.essence.EssenceJugBlock;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ChronoGenesis.MODID);

    /** Living tissue grown around brass scaffolding: the base material of every biomechanical contraption. */
    public static final DeferredBlock<Block> CHRONOFLESH = BLOCKS.registerSimpleBlock("chronoflesh",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(1.5f, 4.0f)
                    .sound(SoundType.SLIME_BLOCK));

    /** The frame an organism grows into. Its shape is the organism's shape. */
    public static final DeferredBlock<Block> COPPER_ARMATURE = BLOCKS.registerSimpleBlock("copper_armature",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(2.0f, 6.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.COPPER_GRATE)
                    .noOcclusion()
                    .isViewBlocking((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false));

    private static BlockBehaviour.Properties flesh() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PINK)
                .strength(1.0f, 2.0f)
                .sound(SoundType.HONEY_BLOCK)
                .pushReaction(PushReaction.BLOCK);
    }

    public static final DeferredBlock<OrganismCoreBlock> ORGANISM_CORE = BLOCKS.registerBlock("organism_core",
            OrganismCoreBlock::new, flesh().strength(2.0f, 4.0f));

    public static final DeferredBlock<OrganismFleshBlock> ORGANISM_FLESH = BLOCKS.registerBlock("organism_flesh",
            OrganismFleshBlock::new, flesh());

    public static final DeferredBlock<TapBlock> TAP = BLOCKS.registerBlock("tap",
            TapBlock::new, flesh().noOcclusion().strength(0.5f));

    public static final DeferredBlock<MawBlock> MAW = BLOCKS.registerBlock("maw",
            MawBlock::new, flesh().noOcclusion().strength(0.5f));

    public static final DeferredBlock<LeechTendrilBlock> LEECH_TENDRIL = BLOCKS.registerBlock("leech_tendril",
            LeechTendrilBlock::new, flesh().noOcclusion().noCollission().strength(0.5f));

    public static final DeferredBlock<EssenceJugBlock> ESSENCE_JUG = BLOCKS.registerBlock("essence_jug",
            EssenceJugBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.NONE)
                    .strength(0.3f)
                    .sound(SoundType.GLASS)
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY));

    private ModBlocks() {}
}

package com.chronogenesis.registry;

import com.chronogenesis.ChronoGenesis;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
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

    private ModBlocks() {}
}

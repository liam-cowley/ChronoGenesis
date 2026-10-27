package com.chronogenesis.datagen;

import java.util.Set;

import com.chronogenesis.registry.ModBlocks;
import com.chronogenesis.registry.ModDataComponents;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

public class ModBlockLootProvider extends BlockLootSubProvider {
    public ModBlockLootProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        dropSelf(ModBlocks.BRASS_BLOCK.get());
        dropSelf(ModBlocks.BRASS_ARMATURE.get());
        // Flesh and cores drop nothing: living tissue dies when cut out (a living core returns its culture in code).
        add(ModBlocks.CHRONOFLESH.get(), noDrop());
        add(ModBlocks.ORGANISM_CORE.get(), noDrop());
        dropSelf(ModBlocks.BRASS_CONDUIT.get());
        dropSelf(ModBlocks.TAP.get());
        dropSelf(ModBlocks.MAW.get());
        dropSelf(ModBlocks.STASIS_BLADDER.get());
        dropSelf(ModBlocks.STARTING_GATE.get());
        dropSelf(ModBlocks.FINISH.get());
        dropSelf(ModBlocks.HEART.get());
        dropSelf(ModBlocks.EYE.get());
        dropSelf(ModBlocks.CLOCK.get());
        dropSelf(ModBlocks.LEECH_TENDRIL.get());
        dropSelf(ModBlocks.ENHANCING_GLAND.get());
        // The jug keeps its essence when broken.
        add(ModBlocks.ESSENCE_JUG.get(), LootTable.lootTable().withPool(applyExplosionCondition(ModBlocks.ESSENCE_JUG.get(),
                LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                        .add(LootItem.lootTableItem(ModBlocks.ESSENCE_JUG.get())
                                .apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)
                                        .include(ModDataComponents.ESSENCE.get()))))));
    }

    /** Every ChronoGenesis block must have a loot table (or {@code noLootTable()}), or datagen fails. */
    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().map(holder -> (Block) holder.value()).toList();
    }
}

package com.chronogenesis.datagen;

import java.util.Set;

import com.chronogenesis.biocomputer.DissectionTableBlock;
import com.chronogenesis.registry.ModBlocks;
import com.chronogenesis.registry.ModDataComponents;
import com.chronogenesis.registry.ModItems;

import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
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
        dropSelf(ModBlocks.GANGLION.get());
        dropSelf(ModBlocks.TEMPORAL_FIELD.get());
        // Faded blueprints crumble if taken: they can only be read where they lie, through the Lens.
        add(ModBlocks.FADED_BLUEPRINT.get(), noDrop());
        // The table gives its brain back; the Chronocortex dies when broken, leaving only the table.
        add(ModBlocks.DISSECTION_TABLE.get(), block -> createSingleItemTable(block)
                .withPool(LootPool.lootPool()
                        .add(LootItem.lootTableItem(ModItems.PRESERVED_BRAIN.get()))
                        .when(LootItemBlockStatePropertyCondition
                                .hasBlockStateProperties(block)
                                .setProperties(StatePropertiesPredicate.Builder.properties()
                                        .hasProperty(DissectionTableBlock.BRAIN, true)))));
        dropOther(ModBlocks.CHRONOCORTEX.get(), ModItems.DISSECTION_TABLE.get());
        dropSelf(ModBlocks.CORTEX_OFFSHOOT.get());
        // Broken off its Cradle, the last bud survives; linked, its link is gone.
        dropSelf(ModBlocks.LAST_OFFSHOOT.get());
        dropSelf(ModBlocks.RIDERS_SEAT.get());
        // The harness comes off whole; the core, stilled, is all the time it held.
        dropOther(ModBlocks.BUCKLED_HARNESS.get(), ModItems.DINO_HARNESS.get());
        dropOther(ModBlocks.TEMPORAL_CORE.get(), ModItems.STILLED_CORE.get());
        // A fossil cut out of the ages crumbles: its memories go with it.
        add(ModBlocks.FOSSIL_CORTEX.get(), noDrop());
        // A lobe cut away dies; its brass frame is left.
        dropOther(ModBlocks.CORTEX_LOBE.get(), ModItems.BRASS_ARMATURE.get());
        dropSelf(ModBlocks.LEECH_TENDRIL.get());
        dropSelf(ModBlocks.ENHANCING_GLAND.get());
        dropSelf(ModBlocks.AEON_ICE.get());
        // An egg keeps what is in it (and loses the Time it was fed).
        add(ModBlocks.DINO_EGG.get(), LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(ModBlocks.DINO_EGG.get())
                        .apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)
                                .include(ModDataComponents.DINO.get())))));
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

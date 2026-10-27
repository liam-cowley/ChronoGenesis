package com.chronogenesis.datagen;

import java.util.Set;

import com.chronogenesis.biomech.OrganismCoreBlock;
import com.chronogenesis.registry.ModBlocks;
import com.chronogenesis.registry.ModDataComponents;
import com.chronogenesis.registry.ModItems;

import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

public class ModBlockLootProvider extends BlockLootSubProvider {
    public ModBlockLootProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        dropSelf(ModBlocks.CHRONOFLESH.get());
        dropSelf(ModBlocks.COPPER_ARMATURE.get());
        dropSelf(ModBlocks.TAP.get());
        dropSelf(ModBlocks.MAW.get());
        dropSelf(ModBlocks.LEECH_TENDRIL.get());

        // Cutting flesh off an organism leaves Living Flesh and the bare frame.
        add(ModBlocks.ORGANISM_FLESH.get(), LootTable.lootTable()
                .withPool(singleItem(ModItems.LIVING_FLESH.get(), 1))
                .withPool(singleItem(ModItems.COPPER_ARMATURE.get(), 1)));

        // A dead core only gives rotten flesh back.
        LootItemBlockStatePropertyCondition.Builder alive = LootItemBlockStatePropertyCondition
                .hasBlockStateProperties(ModBlocks.ORGANISM_CORE.get())
                .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(OrganismCoreBlock.NECROTIC, false));
        add(ModBlocks.ORGANISM_CORE.get(), LootTable.lootTable()
                .withPool(singleItem(ModItems.LIVING_FLESH.get(), 2).when(alive))
                .withPool(singleItem(Items.ROTTEN_FLESH, 2).when(alive.invert()))
                .withPool(singleItem(ModItems.COPPER_ARMATURE.get(), 1)));

        // The jug keeps what is inside it.
        add(ModBlocks.ESSENCE_JUG.get(), LootTable.lootTable().withPool(applyExplosionCondition(ModBlocks.ESSENCE_JUG.get(),
                LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                        .add(LootItem.lootTableItem(ModBlocks.ESSENCE_JUG.get())
                                .apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)
                                        .include(ModDataComponents.ESSENCE.get()))))));
    }

    private static LootPool.Builder singleItem(ItemLike item, int count) {
        return LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(item).apply(SetItemCountFunction.setCount(ConstantValue.exactly(count))));
    }

    /** Every ChronoGenesis block must have a loot table, or datagen fails. That is intentional. */
    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().map(holder -> (Block) holder.value()).toList();
    }
}

package com.chronogenesis.registry;

import com.chronogenesis.ChronoGenesis;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ChronoGenesis.MODID);

    /** Empty glass vessel used to draw essence from living beings. */
    public static final DeferredItem<Item> ESSENCE_VIAL = ITEMS.registerSimpleItem("essence_vial",
            new Item.Properties().stacksTo(16));

    /** Crystallised temporal essence left behind when a creature's time runs out. */
    public static final DeferredItem<Item> TEMPORAL_RESIDUE = ITEMS.registerSimpleItem("temporal_residue",
            new Item.Properties().rarity(Rarity.UNCOMMON));

    public static final DeferredItem<BlockItem> CHRONOFLESH = ITEMS.registerSimpleBlockItem("chronoflesh", ModBlocks.CHRONOFLESH);

    private ModItems() {}
}

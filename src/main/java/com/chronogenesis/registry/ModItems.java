package com.chronogenesis.registry;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.essence.Aspect;
import com.chronogenesis.item.ChronoSyringeItem;
import com.chronogenesis.item.CultureItem;
import com.chronogenesis.item.EssenceContainerItem;
import com.chronogenesis.item.EssenceJugItem;
import com.chronogenesis.item.NeedleItem;
import com.chronogenesis.item.StasisAmpouleItem;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ChronoGenesis.MODID);

    // Instruments
    public static final DeferredItem<ChronoSyringeItem> CHRONO_SYRINGE = ITEMS.registerItem("chrono_syringe",
            ChronoSyringeItem::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<NeedleItem> IRON_NEEDLE = needle("iron_needle", Aspect.DENSITY);
    public static final DeferredItem<NeedleItem> GOLD_NEEDLE = needle("gold_needle", Aspect.SPEED);
    public static final DeferredItem<NeedleItem> BONE_NEEDLE = needle("bone_needle", Aspect.SIZE);
    public static final DeferredItem<NeedleItem> AMETHYST_NEEDLE = needle("amethyst_needle", Aspect.TIME);

    // Containers
    /** 8 drams of one glass-safe aspect. */
    public static final DeferredItem<EssenceContainerItem> ESSENCE_VIAL = ITEMS.registerItem("essence_vial",
            properties -> new EssenceContainerItem(properties, 8, Aspect::fitsInGlass), new Item.Properties().stacksTo(16));
    /** Phantom-membrane-lined vial: the only early container for Time. */
    public static final DeferredItem<StasisAmpouleItem> STASIS_AMPOULE = ITEMS.registerItem("stasis_ampoule",
            StasisAmpouleItem::new, new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON));

    // Biology
    /** A living culture of one aspect: seeds an organism on a Copper Armature. */
    public static final DeferredItem<CultureItem> CULTURE = ITEMS.registerItem("culture",
            CultureItem::new, new Item.Properties().stacksTo(16));
    /** Taken from zombie villagers and witches. The seed of Awareness cultures. */
    public static final DeferredItem<Item> PRESERVED_BRAIN = ITEMS.registerSimpleItem("preserved_brain",
            new Item.Properties().stacksTo(16));
    /** Crystallised time left behind when a creature desiccates. */
    public static final DeferredItem<Item> TEMPORAL_RESIDUE = ITEMS.registerSimpleItem("temporal_residue",
            new Item.Properties().rarity(Rarity.UNCOMMON));

    // Block items
    public static final DeferredItem<BlockItem> COPPER_ARMATURE = ITEMS.registerSimpleBlockItem("copper_armature", ModBlocks.COPPER_ARMATURE);
    public static final DeferredItem<BlockItem> CHRONOFLESH = ITEMS.registerSimpleBlockItem("chronoflesh", ModBlocks.CHRONOFLESH);
    public static final DeferredItem<BlockItem> TAP = ITEMS.registerSimpleBlockItem("tap", ModBlocks.TAP);
    public static final DeferredItem<BlockItem> LEECH_TENDRIL = ITEMS.registerSimpleBlockItem("leech_tendril", ModBlocks.LEECH_TENDRIL);
    public static final DeferredItem<BlockItem> ENHANCING_GLAND = ITEMS.registerSimpleBlockItem("enhancing_gland", ModBlocks.ENHANCING_GLAND);
    public static final DeferredItem<EssenceJugItem> ESSENCE_JUG = ITEMS.registerItem("essence_jug",
            properties -> new EssenceJugItem(ModBlocks.ESSENCE_JUG.get(), properties), new Item.Properties().stacksTo(1));

    private static DeferredItem<NeedleItem> needle(String name, Aspect aspect) {
        return ITEMS.registerItem(name, properties -> new NeedleItem(properties, aspect), new Item.Properties().stacksTo(16));
    }

    private ModItems() {}
}

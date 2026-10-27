package com.chronogenesis.registry;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.essence.Needle;
import com.chronogenesis.item.ChronoSyringeItem;
import com.chronogenesis.item.CultureJarItem;
import com.chronogenesis.item.EssenceContainerItem;
import com.chronogenesis.item.EssenceJugItem;
import com.chronogenesis.item.NeedleItem;
import com.chronogenesis.item.StasisAmpouleItem;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registration order is creative tab order. */
public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ChronoGenesis.MODID);

    // --- Extraction ---
    public static final DeferredItem<ChronoSyringeItem> CHRONO_SYRINGE = ITEMS.registerItem("chrono_syringe",
            ChronoSyringeItem::new, new Item.Properties().stacksTo(1));

    public static final DeferredItem<NeedleItem> IRON_NEEDLE = ITEMS.registerItem("iron_needle",
            props -> new NeedleItem(props, Needle.IRON));
    public static final DeferredItem<NeedleItem> GOLD_NEEDLE = ITEMS.registerItem("gold_needle",
            props -> new NeedleItem(props, Needle.GOLD));
    public static final DeferredItem<NeedleItem> BONE_NEEDLE = ITEMS.registerItem("bone_needle",
            props -> new NeedleItem(props, Needle.BONE));
    public static final DeferredItem<NeedleItem> AMETHYST_NEEDLE = ITEMS.registerItem("amethyst_needle",
            props -> new NeedleItem(props, Needle.AMETHYST), new Item.Properties().rarity(Rarity.UNCOMMON));

    // --- Containers ---
    /** Glass vial for 8 drams of a stable aspect. */
    public static final DeferredItem<EssenceContainerItem> ESSENCE_VIAL = ITEMS.registerItem("essence_vial",
            props -> new EssenceContainerItem(props, 8, EssenceContainerItem::stableOnly), new Item.Properties().stacksTo(16));

    public static final DeferredItem<StasisAmpouleItem> STASIS_AMPOULE = ITEMS.registerItem("stasis_ampoule",
            StasisAmpouleItem::new, new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON));

    public static final DeferredItem<EssenceJugItem> ESSENCE_JUG = ITEMS.registerItem("essence_jug",
            props -> new EssenceJugItem(ModBlocks.ESSENCE_JUG.get(), props), new Item.Properties().stacksTo(1));

    // --- Grown machines ---
    public static final DeferredItem<CultureJarItem> CULTURE_JAR = ITEMS.registerItem("culture_jar",
            CultureJarItem::new, new Item.Properties().stacksTo(16));

    public static final DeferredItem<BlockItem> COPPER_ARMATURE = ITEMS.registerSimpleBlockItem("copper_armature", ModBlocks.COPPER_ARMATURE);

    /** Cut from a grown organism. Grafts are made from it. */
    public static final DeferredItem<Item> LIVING_FLESH = ITEMS.registerSimpleItem("living_flesh");

    public static final DeferredItem<BlockItem> TAP = ITEMS.registerSimpleBlockItem("tap", ModBlocks.TAP);
    public static final DeferredItem<BlockItem> MAW = ITEMS.registerSimpleBlockItem("maw", ModBlocks.MAW);
    public static final DeferredItem<BlockItem> LEECH_TENDRIL = ITEMS.registerSimpleBlockItem("leech_tendril", ModBlocks.LEECH_TENDRIL);

    public static final DeferredItem<BlockItem> CHRONOFLESH = ITEMS.registerSimpleBlockItem("chronoflesh", ModBlocks.CHRONOFLESH);

    /** Crystallised temporal essence left behind when a creature's time runs out. */
    public static final DeferredItem<Item> TEMPORAL_RESIDUE = ITEMS.registerSimpleItem("temporal_residue",
            new Item.Properties().rarity(Rarity.UNCOMMON));

    public static DeferredItem<NeedleItem> needle(Needle needle) {
        return switch (needle) {
            case IRON -> IRON_NEEDLE;
            case GOLD -> GOLD_NEEDLE;
            case BONE -> BONE_NEEDLE;
            case AMETHYST -> AMETHYST_NEEDLE;
        };
    }

    private ModItems() {}
}

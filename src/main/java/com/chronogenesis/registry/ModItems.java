package com.chronogenesis.registry;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.essence.Aspect;
import com.chronogenesis.item.ChronoSyringeItem;
import com.chronogenesis.item.ConfluenceNeedleItem;
import com.chronogenesis.item.CreativeBlockItem;
import com.chronogenesis.item.EssenceManipulatorItem;
import com.chronogenesis.item.CultureItem;
import com.chronogenesis.item.EssenceContainerItem;
import com.chronogenesis.item.EssenceJugItem;
import com.chronogenesis.item.EternalHourglassItem;
import com.chronogenesis.item.NeedleItem;
import com.chronogenesis.item.StasisAmpouleItem;
import com.chronogenesis.item.UmbilicalLocketItem;
import com.chronogenesis.lens.IntactBlueprintItem;
import com.chronogenesis.lens.TemporalLensItem;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ChronoGenesis.MODID);

    // Materials
    /** Copper and gold, crushed together. Smelt it into brass. */
    public static final DeferredItem<Item> BRASS_BLEND = ITEMS.registerSimpleItem("brass_blend", new Item.Properties());
    public static final DeferredItem<Item> BRASS_INGOT = ITEMS.registerSimpleItem("brass_ingot", new Item.Properties());
    public static final DeferredItem<Item> BRASS_NUGGET = ITEMS.registerSimpleItem("brass_nugget", new Item.Properties());
    public static final DeferredItem<BlockItem> BRASS_BLOCK = ITEMS.registerSimpleBlockItem("brass_block", ModBlocks.BRASS_BLOCK);

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
    /** A living culture of one aspect: seeds an organism on a Brass Armature. */
    public static final DeferredItem<CultureItem> CULTURE = ITEMS.registerItem("culture",
            CultureItem::new, new Item.Properties().stacksTo(16));
    /** Taken from zombie villagers and witches. The seed of Awareness cultures. */
    public static final DeferredItem<Item> PRESERVED_BRAIN = ITEMS.registerSimpleItem("preserved_brain",
            new Item.Properties().stacksTo(16));
    /** Taken from cows, pigs and sheep. Grafted on, it pumps. */
    public static final DeferredItem<Item> PRESERVED_HEART = ITEMS.registerSimpleItem("preserved_heart",
            new Item.Properties().stacksTo(16));
    /** Crystallised time left behind when a creature desiccates. */
    public static final DeferredItem<Item> TEMPORAL_RESIDUE = ITEMS.registerSimpleItem("temporal_residue",
            new Item.Properties().rarity(Rarity.UNCOMMON));

    /** Worn in the necklace slot, tied to an organism: refills your pool from a distance. Grown by research. */
    public static final DeferredItem<UmbilicalLocketItem> UMBILICAL_LOCKET = ITEMS.registerItem("umbilical_locket",
            UmbilicalLocketItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.RARE));

    /** Look into the past. Burns Time. Grown by research. */
    public static final DeferredItem<TemporalLensItem> TEMPORAL_LENS = ITEMS.registerItem("temporal_lens",
            TemporalLensItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    /** The rare blueprint that survived in an Abandoned Laboratory. Read it for a clue. */
    public static final DeferredItem<IntactBlueprintItem> INTACT_BLUEPRINT = ITEMS.registerItem("intact_blueprint",
            IntactBlueprintItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

    /** Creative only (no recipe): keeps essences full and instruments charged while carried. For testing. */
    /** Steers the powers: right-click to use, Shift + R for the wheel, Shift + scroll for strength. */
    public static final DeferredItem<EssenceManipulatorItem> ESSENCE_MANIPULATOR = ITEMS.registerItem("essence_manipulator",
            EssenceManipulatorItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

    /** Fitted into an Enhancing Gland: mixes two essences wired together by veins. Grown by research. */
    public static final DeferredItem<ConfluenceNeedleItem> CONFLUENCE_NEEDLE = ITEMS.registerItem("confluence_needle",
            ConfluenceNeedleItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

    /** Creative only (no recipe; in play it is woken by the ritual): an already awake Chronocortex. */
    public static final DeferredItem<CreativeBlockItem> CHRONOCORTEX = ITEMS.registerItem("chronocortex",
            properties -> new CreativeBlockItem(ModBlocks.CHRONOCORTEX.get(), properties, "tooltip.chronogenesis.chronocortex"),
            new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));

    public static final DeferredItem<EternalHourglassItem> ETERNAL_HOURGLASS = ITEMS.registerItem("eternal_hourglass",
            EternalHourglassItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));

    // Block items
    public static final DeferredItem<BlockItem> BRASS_ARMATURE = ITEMS.registerSimpleBlockItem("brass_armature", ModBlocks.BRASS_ARMATURE);
    public static final DeferredItem<BlockItem> CHRONOFLESH = ITEMS.registerSimpleBlockItem("chronoflesh", ModBlocks.CHRONOFLESH);
    public static final DeferredItem<BlockItem> BRASS_CONDUIT = ITEMS.registerSimpleBlockItem("brass_conduit", ModBlocks.BRASS_CONDUIT);
    public static final DeferredItem<BlockItem> TAP = ITEMS.registerSimpleBlockItem("tap", ModBlocks.TAP);
    public static final DeferredItem<BlockItem> MAW = ITEMS.registerSimpleBlockItem("maw", ModBlocks.MAW);
    public static final DeferredItem<BlockItem> LEECH_TENDRIL = ITEMS.registerSimpleBlockItem("leech_tendril", ModBlocks.LEECH_TENDRIL);
    public static final DeferredItem<BlockItem> ENHANCING_GLAND = ITEMS.registerSimpleBlockItem("enhancing_gland", ModBlocks.ENHANCING_GLAND);
    public static final DeferredItem<BlockItem> STASIS_BLADDER = ITEMS.registerSimpleBlockItem("stasis_bladder", ModBlocks.STASIS_BLADDER);
    public static final DeferredItem<BlockItem> STARTING_GATE = ITEMS.registerSimpleBlockItem("starting_gate", ModBlocks.STARTING_GATE);
    public static final DeferredItem<BlockItem> FINISH = ITEMS.registerSimpleBlockItem("finish", ModBlocks.FINISH);
    public static final DeferredItem<BlockItem> HEART = ITEMS.registerSimpleBlockItem("heart", ModBlocks.HEART);
    public static final DeferredItem<BlockItem> EYE = ITEMS.registerSimpleBlockItem("eye", ModBlocks.EYE);
    public static final DeferredItem<BlockItem> CLOCK = ITEMS.registerSimpleBlockItem("clock", ModBlocks.CLOCK);
    public static final DeferredItem<BlockItem> GANGLION = ITEMS.registerSimpleBlockItem("ganglion", ModBlocks.GANGLION);
    /** Grown by research, not crafted. */
    public static final DeferredItem<BlockItem> TEMPORAL_FIELD = ITEMS.registerSimpleBlockItem("temporal_field",
            ModBlocks.TEMPORAL_FIELD, new Item.Properties().rarity(Rarity.RARE));
    public static final DeferredItem<BlockItem> DISSECTION_TABLE = ITEMS.registerSimpleBlockItem("dissection_table",
            ModBlocks.DISSECTION_TABLE);
    public static final DeferredItem<EssenceJugItem> ESSENCE_JUG = ITEMS.registerItem("essence_jug",
            properties -> new EssenceJugItem(ModBlocks.ESSENCE_JUG.get(), properties), new Item.Properties().stacksTo(1));

    // The Chrono-Insulated Suit: each piece worn blocks a quarter of Temporal Exposure.
    public static final DeferredItem<net.minecraft.world.item.ArmorItem> CHRONO_GOGGLES = suit("chrono_goggles",
            net.minecraft.world.item.ArmorItem.Type.HELMET);
    public static final DeferredItem<net.minecraft.world.item.ArmorItem> CHRONO_LAB_COAT = suit("chrono_lab_coat",
            net.minecraft.world.item.ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<net.minecraft.world.item.ArmorItem> CHRONO_TROUSERS = suit("chrono_trousers",
            net.minecraft.world.item.ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<net.minecraft.world.item.ArmorItem> CHRONO_GALOSHES = suit("chrono_galoshes",
            net.minecraft.world.item.ArmorItem.Type.BOOTS);

    private static DeferredItem<net.minecraft.world.item.ArmorItem> suit(String name, net.minecraft.world.item.ArmorItem.Type type) {
        return ITEMS.registerItem(name, properties -> new net.minecraft.world.item.ArmorItem(ModArmorMaterials.CHRONO_INSULATED, type,
                properties.durability(type.getDurability(12))));
    }

    private static DeferredItem<NeedleItem> needle(String name, Aspect aspect) {
        return ITEMS.registerItem(name, properties -> new NeedleItem(properties, aspect), new Item.Properties().stacksTo(16));
    }

    private ModItems() {}
}

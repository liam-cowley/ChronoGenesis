package com.chronogenesis.registry;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.essence.Aspect;
import com.chronogenesis.item.ChronoSuitItem;
import com.chronogenesis.item.ChronoSyringeItem;
import com.chronogenesis.item.ChronoTetherItem;
import com.chronogenesis.item.ConfluenceNeedleItem;
import com.chronogenesis.item.CreativeBlockItem;
import com.chronogenesis.item.CultureItem;
import com.chronogenesis.item.EssenceContainerItem;
import com.chronogenesis.item.EssenceJugItem;
import com.chronogenesis.item.EssenceManipulatorItem;
import com.chronogenesis.item.EternalHourglassItem;
import com.chronogenesis.item.NeedleItem;
import com.chronogenesis.item.NerveSpoolItem;
import com.chronogenesis.item.StasisAmpouleItem;
import com.chronogenesis.item.UmbilicalLocketItem;
import com.chronogenesis.lens.IntactBlueprintItem;
import com.chronogenesis.lens.TemporalLensItem;
import com.chronogenesis.lore.PrecursorTabletItem;
import com.chronogenesis.temple.StilledCoreItem;
import com.chronogenesis.timetravel.OffshootItem;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
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

    /** Needed to be shifted into another era; pull it there to come home early. Grown by research. */
    public static final DeferredItem<ChronoTetherItem> CHRONO_TETHER = ITEMS.registerItem("chrono_tether",
            ChronoTetherItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.RARE));

    /** A fragment of why the Precursors fell. Found, never made. */
    public static final DeferredItem<PrecursorTabletItem> PRECURSOR_TABLET = ITEMS.registerItem("precursor_tablet",
            PrecursorTabletItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

    /** A bud of the Chronocortex, planted in a past era to grow through the ages. Grown by research. */
    public static final DeferredItem<OffshootItem> CORTEX_OFFSHOOT = ITEMS.registerItem("cortex_offshoot",
            properties -> new OffshootItem(ModBlocks.CORTEX_OFFSHOOT.get(), properties),
            new Item.Properties().stacksTo(1).rarity(Rarity.RARE));

    /** Lays a nerve from a Fossil Cortex to the Chronocortex, over any distance. */
    public static final DeferredItem<NerveSpoolItem> NERVE_SPOOL = ITEMS.registerItem("nerve_spool", NerveSpoolItem::new,
            new Item.Properties().stacksTo(1));

    public static final DeferredItem<DeferredSpawnEggItem> PRECURSOR_SPAWN_EGG = ITEMS.registerItem("precursor_spawn_egg",
            properties -> new DeferredSpawnEggItem(ModEntities.PRECURSOR, 0x2B3F7A, 0xD8B45A, properties), new Item.Properties());

    public static final DeferredItem<BlockItem> FOSSIL_CORTEX = ITEMS.registerSimpleBlockItem("fossil_cortex", ModBlocks.FOSSIL_CORTEX);

    /** Buckled onto a Titanosaur (use it on one): its back becomes ground to build on. Grown by research. */
    public static final DeferredItem<Item> DINO_HARNESS = ITEMS.registerItem("dino_harness",
            properties -> new Item(properties) {
                @Override
                public void appendHoverText(net.minecraft.world.item.ItemStack stack, TooltipContext context,
                        java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
                    tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.chronogenesis.dino_harness")
                            .withStyle(net.minecraft.ChatFormatting.GRAY));
                }
            }, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

    public static final DeferredItem<BlockItem> RIDERS_SEAT = ITEMS.registerSimpleBlockItem("riders_seat", ModBlocks.RIDERS_SEAT);

    /** A temple's Temporal Core, stilled: all the time it held, packed tight. */
    public static final DeferredItem<StilledCoreItem> STILLED_CORE = ITEMS.registerItem("stilled_core", StilledCoreItem::new,
            new Item.Properties().stacksTo(16).rarity(Rarity.RARE));

    public static final DeferredItem<BlockItem> TEMPORAL_CORE = ITEMS.registerSimpleBlockItem("temporal_core", ModBlocks.TEMPORAL_CORE);
    public static final DeferredItem<BlockItem> UNMADE_NAIL = ITEMS.registerSimpleBlockItem("unmade_nail", ModBlocks.UNMADE_NAIL);
    /** What the Unbegotten leaves: a heart that beat before time began. */
    public static final DeferredItem<Item> HEART_OF_THE_UNBEGOTTEN = ITEMS.registerSimpleItem("heart_of_the_unbegotten",
            new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    /** The Unbegotten's heart, made a pump by the Chronocortex: grafted onto a level 3 gland, it wakes the Eldritch powers. */
    public static final DeferredItem<Item> UNBEGOTTEN_PUMP = ITEMS.registerItem("unbegotten_pump",
            properties -> new Item(properties) {
                @Override
                public void appendHoverText(net.minecraft.world.item.ItemStack stack, TooltipContext context,
                        java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
                    tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.chronogenesis.unbegotten_pump")
                            .withStyle(net.minecraft.ChatFormatting.GRAY));
                }
            }, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    /** The Chronocortex's last bud: planted in the Cradle Stone Before Time. Grown by research. */
    public static final DeferredItem<com.chronogenesis.eldritch.LastOffshootBlock.Item> LAST_OFFSHOOT = ITEMS.registerItem("last_offshoot",
            properties -> new com.chronogenesis.eldritch.LastOffshootBlock.Item(ModBlocks.LAST_OFFSHOOT.get(), properties),
            new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    public static final DeferredItem<BlockItem> CRADLE_STONE = ITEMS.registerSimpleBlockItem("cradle_stone", ModBlocks.CRADLE_STONE);
    public static final DeferredItem<BlockItem> UNBEGOTTEN_FLESH = ITEMS.registerSimpleBlockItem("unbegotten_flesh", ModBlocks.UNBEGOTTEN_FLESH);
    /** Grafts of the Unbegotten, for a body: left when it dies. */
    public static final DeferredItem<com.chronogenesis.item.PlayerGraftItem> UNBLINKING_EYE = playerGraft("unblinking_eye",
            com.chronogenesis.player.PlayerGraft.UNBLINKING_EYE);
    public static final DeferredItem<com.chronogenesis.item.PlayerGraftItem> DORSAL_TENDRILS = playerGraft("dorsal_tendrils",
            com.chronogenesis.player.PlayerGraft.DORSAL_TENDRILS);
    public static final DeferredItem<com.chronogenesis.item.PlayerGraftItem> GRASPING_TENTACLE = playerGraft("grasping_tentacle",
            com.chronogenesis.player.PlayerGraft.GRASPING_TENTACLE);
    public static final DeferredItem<com.chronogenesis.item.PlayerGraftItem> UNBEGOTTEN_HIDE = playerGraft("unbegotten_hide",
            com.chronogenesis.player.PlayerGraft.UNBEGOTTEN_HIDE);
    public static final DeferredItem<DeferredSpawnEggItem> UNBEGOTTEN_SPAWN_EGG = ITEMS.registerItem("unbegotten_spawn_egg",
            properties -> new DeferredSpawnEggItem(ModEntities.UNBEGOTTEN, 0x1A0A24, 0xA6FF80, properties));
    public static final DeferredItem<BlockItem> CORE_SINEW = ITEMS.registerSimpleBlockItem("core_sinew", ModBlocks.CORE_SINEW);
    public static final DeferredItem<BlockItem> HEAVY_SEAL = ITEMS.registerSimpleBlockItem("heavy_seal", ModBlocks.HEAVY_SEAL);
    public static final DeferredItem<BlockItem> NARROW_SEAL = ITEMS.registerSimpleBlockItem("narrow_seal", ModBlocks.NARROW_SEAL);
    public static final DeferredItem<BlockItem> SWIFT_SEAL = ITEMS.registerSimpleBlockItem("swift_seal", ModBlocks.SWIFT_SEAL);
    public static final DeferredItem<BlockItem> VEILED_SEAL = ITEMS.registerSimpleBlockItem("veiled_seal", ModBlocks.VEILED_SEAL);
    public static final DeferredItem<BlockItem> STILLED_SEAL = ITEMS.registerSimpleBlockItem("stilled_seal", ModBlocks.STILLED_SEAL);

    public static final DeferredItem<BlockItem> AEON_ICE = ITEMS.registerSimpleBlockItem("aeon_ice", ModBlocks.AEON_ICE);
    /** What is left in a beast of the Jurassic once all its Time is drawn out: fed to the Chronocortex, it can bud its eggs. */
    public static final DeferredItem<com.chronogenesis.item.FossilItem> FOSSIL = ITEMS.registerItem("fossil",
            com.chronogenesis.item.FossilItem::new, new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<com.chronogenesis.entity.DinoEggItem> DINO_EGG = ITEMS.registerItem("dino_egg",
            properties -> new com.chronogenesis.entity.DinoEggItem(ModBlocks.DINO_EGG.get(), properties),
            new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<DeferredSpawnEggItem> MAMMOTH_SPAWN_EGG = ITEMS.registerItem("mammoth_spawn_egg",
            properties -> new DeferredSpawnEggItem(ModEntities.MAMMOTH, 0x5A3A22, 0xE8DCC0, properties), new Item.Properties());
    public static final DeferredItem<DeferredSpawnEggItem> SMILODON_SPAWN_EGG = ITEMS.registerItem("smilodon_spawn_egg",
            properties -> new DeferredSpawnEggItem(ModEntities.SMILODON, 0xD8C8A8, 0x8A6A44, properties), new Item.Properties());
    public static final DeferredItem<DeferredSpawnEggItem> WOOLLY_RHINO_SPAWN_EGG = ITEMS.registerItem("woolly_rhino_spawn_egg",
            properties -> new DeferredSpawnEggItem(ModEntities.WOOLLY_RHINO, 0x6A4A2E, 0x3A2A1A, properties), new Item.Properties());
    public static final DeferredItem<DeferredSpawnEggItem> PRECURSOR_GOLEM_SPAWN_EGG = ITEMS.registerItem("precursor_golem_spawn_egg",
            properties -> new DeferredSpawnEggItem(ModEntities.PRECURSOR_GOLEM, 0xD8D8D8, 0x6A8AA0, properties), new Item.Properties());

    public static final DeferredItem<DeferredSpawnEggItem> RAPTOR_SPAWN_EGG = ITEMS.registerItem("raptor_spawn_egg",
            properties -> new DeferredSpawnEggItem(ModEntities.RAPTOR, 0x8A6A3A, 0x3A2A18, properties), new Item.Properties());
    public static final DeferredItem<DeferredSpawnEggItem> TITANOSAUR_SPAWN_EGG = ITEMS.registerItem("titanosaur_spawn_egg",
            properties -> new DeferredSpawnEggItem(ModEntities.TITANOSAUR, 0x6E7A58, 0xB8B088, properties), new Item.Properties());

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
    public static final DeferredItem<ChronoSuitItem> CHRONO_GOGGLES = suit("chrono_goggles", ArmorItem.Type.HELMET);
    public static final DeferredItem<ChronoSuitItem> CHRONO_LAB_COAT = suit("chrono_lab_coat", ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<ChronoSuitItem> CHRONO_TROUSERS = suit("chrono_trousers", ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<ChronoSuitItem> CHRONO_GALOSHES = suit("chrono_galoshes", ArmorItem.Type.BOOTS);

    private static DeferredItem<ChronoSuitItem> suit(String name, ArmorItem.Type type) {
        return ITEMS.registerItem(name, properties -> new ChronoSuitItem(type, properties.durability(type.getDurability(12))));
    }

    private static DeferredItem<NeedleItem> needle(String name, Aspect aspect) {
        return ITEMS.registerItem(name, properties -> new NeedleItem(properties, aspect), new Item.Properties().stacksTo(16));
    }

    private static DeferredItem<com.chronogenesis.item.PlayerGraftItem> playerGraft(String name, com.chronogenesis.player.PlayerGraft graft) {
        return ITEMS.registerItem(name, properties -> new com.chronogenesis.item.PlayerGraftItem(graft, properties),
                new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    private ModItems() {}
}

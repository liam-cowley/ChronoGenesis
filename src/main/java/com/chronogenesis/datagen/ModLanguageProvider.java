package com.chronogenesis.datagen;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.essence.Aspect;
import com.chronogenesis.registry.ModBlocks;
import com.chronogenesis.registry.ModItems;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

/** en_us translations. Every new item, block, message and config entry gets a line here. */
public class ModLanguageProvider extends LanguageProvider {
    public ModLanguageProvider(PackOutput output) {
        super(output, ChronoGenesis.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup.chronogenesis", "ChronoGenesis");

        // Aspects
        add(Aspect.DENSITY.translationKey(), "Density");
        add(Aspect.SPEED.translationKey(), "Speed");
        add(Aspect.SIZE.translationKey(), "Size");
        add(Aspect.AWARENESS.translationKey(), "Awareness");
        add(Aspect.TIME.translationKey(), "Time");

        // Materials
        addItem(ModItems.BRASS_BLEND, "Brass Blend");
        addItem(ModItems.BRASS_INGOT, "Brass Ingot");
        addItem(ModItems.BRASS_NUGGET, "Brass Nugget");
        addBlock(ModBlocks.BRASS_BLOCK, "Block of Brass");

        // Items
        addItem(ModItems.CHRONO_SYRINGE, "Chrono Syringe");
        addItem(ModItems.IRON_NEEDLE, "Iron Needle");
        addItem(ModItems.GOLD_NEEDLE, "Gold Needle");
        addItem(ModItems.BONE_NEEDLE, "Bone Needle");
        addItem(ModItems.AMETHYST_NEEDLE, "Amethyst Needle");
        addItem(ModItems.ESSENCE_VIAL, "Essence Vial");
        addItem(ModItems.STASIS_AMPOULE, "Stasis Ampoule");
        addItem(ModItems.CULTURE, "Culture");
        add("item.chronogenesis.culture.of", "%s Culture");
        addItem(ModItems.PRESERVED_BRAIN, "Preserved Brain");
        addItem(ModItems.TEMPORAL_RESIDUE, "Temporal Residue");

        // Blocks
        addBlock(ModBlocks.BRASS_ARMATURE, "Brass Armature");
        addBlock(ModBlocks.CHRONOFLESH, "Chronoflesh");
        addBlock(ModBlocks.ORGANISM_CORE, "Organism Core");
        addBlock(ModBlocks.TAP, "Tap Graft");
        addBlock(ModBlocks.LEECH_TENDRIL, "Leech Tendril Graft");
        addBlock(ModBlocks.ENHANCING_GLAND, "Enhancing Gland Graft");
        addBlock(ModBlocks.ESSENCE_JUG, "Essence Jug");
        addBlock(ModBlocks.TIME_TEAR, "Time Tear");

        // Tooltips
        add("tooltip.chronogenesis.empty", "Empty");
        add("tooltip.chronogenesis.contents", "%s / %s drams of %s");
        add("tooltip.chronogenesis.needle", "Needle: %s");
        add("tooltip.chronogenesis.no_needle", "No needle fitted");
        add("tooltip.chronogenesis.needle_draws", "Draws %s");
        add("tooltip.chronogenesis.needle_hint", "Hold in off hand, sneak-use a syringe to fit");
        add("tooltip.chronogenesis.culture", "Use on a Brass Armature, then feed it");
        add("tooltip.chronogenesis.ampoule", "Use to inject the Time into yourself");

        // Syringe
        add("message.chronogenesis.syringe.no_needle", "The syringe has no needle.");
        add("message.chronogenesis.syringe.nothing_left", "There is nothing left to draw.");
        add("message.chronogenesis.syringe.full", "The syringe is full. Empty it into a vial.");
        add("message.chronogenesis.syringe.no_vial", "You need a vial with room for it.");
        add("message.chronogenesis.syringe.empty_first", "Empty the syringe before changing needles.");
        add("message.chronogenesis.syringe.glass_aged",
                "The glass clouds, ages and crumbles to sand. Time needs a Stasis Ampoule.");

        // Organisms
        add("message.chronogenesis.culture.took_hold", "The culture takes hold. It is hungry.");
        add("message.chronogenesis.organism.dead", "There is nothing alive here to feed.");
        add("message.chronogenesis.organism.necrotic", "The organism is necrotic. It will not grow again.");
        add("message.chronogenesis.organism.recoils", "It recoils from the %s.");
        add("message.chronogenesis.organism.sated", "It cannot hold any more.");
        add("message.chronogenesis.organism.status", "%s organism: %s blocks, %s / %s drams, %s");
        add("message.chronogenesis.organism.hunger.starving", "starving");
        add("message.chronogenesis.organism.hunger.hungry", "hungry");
        add("message.chronogenesis.organism.hunger.sated", "sated");
        add("message.chronogenesis.tap.volatile", "%s evaporates the moment it meets glass.");

        // Glands and powers
        add("message.chronogenesis.gland.full", "Your body can hold no more.");
        add("message.chronogenesis.gland.empty", "The organism has nothing to give.");
        add("message.chronogenesis.gland.first.density",
                "The gland pierces your wrist. Your fists feel heavy as iron.");
        add("message.chronogenesis.gland.first.speed",
                "The gland pierces your wrist. Your heartbeat quickens and will not slow.");
        add("message.chronogenesis.gland.first.size",
                "The gland pierces your wrist. Your bones creak, unsure how big to be.");
        add("message.chronogenesis.gland.first.awareness",
                "The gland pierces your wrist. You can hear things breathing in the dark.");
        add("message.chronogenesis.gland.first.time",
                "Time floods your veins. Somewhere, a clock skips a beat.");
        add("message.chronogenesis.power.unknown", "You have not taken that essence into yourself.");
        add("message.chronogenesis.power.empty", "Your %s is spent. Refill at an Enhancing Gland.");

        // Time
        add("message.chronogenesis.tear.not_enough", "Sealing a tear takes %s drams of Density.");
        add("message.chronogenesis.tear.sealed", "The weight of it presses the rift shut.");

        // Keys
        add("key.categories.chronogenesis", "ChronoGenesis");
        add("key.chronogenesis.cycle_power", "Select next essence power");
        add("key.chronogenesis.use_power", "Use / toggle selected power");

        // Config screen
        add("chronogenesis.configuration.title", "ChronoGenesis Configs");
        add("chronogenesis.configuration.section.chronogenesis.common.toml", "ChronoGenesis Configs");
        add("chronogenesis.configuration.section.chronogenesis.common.toml.title", "ChronoGenesis Configs");
        add("chronogenesis.configuration.organisms", "Organisms");
        add("chronogenesis.configuration.growthCost", "Growth cost (drams per block)");
        add("chronogenesis.configuration.upkeepInterval", "Upkeep interval (ticks)");
        add("chronogenesis.configuration.maxOrganismSize", "Max organism size (blocks)");
        add("chronogenesis.configuration.time", "Time");
        add("chronogenesis.configuration.tearStressThreshold", "Time Tear stress threshold");
        add("chronogenesis.configuration.stressDecayTicks", "Stress decay (ticks per point)");
        add("chronogenesis.configuration.powers", "Powers");
        add("chronogenesis.configuration.poolPerLevel", "Pool size per power level");
    }
}

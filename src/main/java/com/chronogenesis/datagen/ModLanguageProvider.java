package com.chronogenesis.datagen;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.registry.ModBlocks;
import com.chronogenesis.registry.ModItems;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

/** en_us translations. Every new item, block, tab and config entry gets a line here. */
public class ModLanguageProvider extends LanguageProvider {
    public ModLanguageProvider(PackOutput output) {
        super(output, ChronoGenesis.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup.chronogenesis", "ChronoGenesis");

        // Aspects
        add("aspect.chronogenesis.density", "Density");
        add("aspect.chronogenesis.speed", "Speed");
        add("aspect.chronogenesis.size", "Size");
        add("aspect.chronogenesis.awareness", "Awareness");
        add("aspect.chronogenesis.time", "Time");

        // Items
        addItem(ModItems.CHRONO_SYRINGE, "Chrono Syringe");
        addItem(ModItems.IRON_NEEDLE, "Iron Needle");
        addItem(ModItems.GOLD_NEEDLE, "Gold Needle");
        addItem(ModItems.BONE_NEEDLE, "Bone Needle");
        addItem(ModItems.AMETHYST_NEEDLE, "Amethyst Needle");
        addItem(ModItems.ESSENCE_VIAL, "Essence Vial");
        addItem(ModItems.STASIS_AMPOULE, "Stasis Ampoule");
        addItem(ModItems.CULTURE_JAR, "Culture Jar");
        add("item.chronogenesis.culture", "%s Culture");
        addItem(ModItems.LIVING_FLESH, "Living Flesh");
        addItem(ModItems.TEMPORAL_RESIDUE, "Temporal Residue");

        // Blocks
        addBlock(ModBlocks.CHRONOFLESH, "Chronoflesh");
        addBlock(ModBlocks.COPPER_ARMATURE, "Copper Armature");
        addBlock(ModBlocks.ORGANISM_CORE, "Organism Core");
        addBlock(ModBlocks.ORGANISM_FLESH, "Organism Flesh");
        addBlock(ModBlocks.TAP, "Tap");
        addBlock(ModBlocks.MAW, "Maw");
        addBlock(ModBlocks.LEECH_TENDRIL, "Leech Tendril");
        addBlock(ModBlocks.ESSENCE_JUG, "Essence Jug");

        // Tooltips
        add("tooltip.chronogenesis.empty", "Empty");
        add("tooltip.chronogenesis.contents", "%s: %s / %s drams");
        add("tooltip.chronogenesis.needle.draws", "Draws %s");
        add("tooltip.chronogenesis.needle.fit", "Right-click onto a Chrono Syringe to fit it");
        add("tooltip.chronogenesis.syringe.needle", "Needle: %s");
        add("tooltip.chronogenesis.syringe.no_needle", "No needle fitted");
        add("tooltip.chronogenesis.stasis_ampoule.leaks", "The membrane weeps. It will not hold forever.");
        add("tooltip.chronogenesis.culture.unripe", "Pour 4 drams of one essence into it to start a culture");
        add("tooltip.chronogenesis.culture.ripe", "Set it into a Copper Armature");

        // Messages
        add("message.chronogenesis.syringe.no_needle", "The syringe has no needle.");
        add("message.chronogenesis.syringe.mixed", "The syringe already holds another essence.");
        add("message.chronogenesis.syringe.full", "The syringe is full.");
        add("message.chronogenesis.syringe.nothing_left", "There is nothing left to draw.");
        add("message.chronogenesis.syringe.needs_ampoule", "Time would age the glass to sand. You need an empty Stasis Ampoule.");
        add("message.chronogenesis.syringe.no_time", "This creature has no time to spare.");
        add("message.chronogenesis.culture.unripe", "The culture is too weak to take.");
        add("message.chronogenesis.culture.planted", "The culture takes to the frame. It is hungry.");
        add("message.chronogenesis.organism.status", "%s Organism: %s / %s drams stored, %s flesh");
        add("message.chronogenesis.organism.necrotic", "It is dead. Necrotic flesh does not grow.");
        add("message.chronogenesis.organism.refuses", "It refuses this.");
        add("message.chronogenesis.tap.dry", "The tap runs dry.");

        // Advancements
        add("advancements.chronogenesis.root.title", "ChronoGenesis");
        add("advancements.chronogenesis.root.description", "Everything that lives carries one of these.");
        add("advancements.chronogenesis.syringe.title", "The First Experiment");
        add("advancements.chronogenesis.syringe.description", "Make a Chrono Syringe. Every creature has something to give.");
        add("advancements.chronogenesis.culture.title", "Something That Wants It");
        add("advancements.chronogenesis.culture.description", "Make a Culture Jar.");
        add("advancements.chronogenesis.living_flesh.title", "It's Growing");
        add("advancements.chronogenesis.living_flesh.description", "Cut Living Flesh from an organism you grew.");
        add("advancements.chronogenesis.temporal_residue.title", "Out of Time");
        add("advancements.chronogenesis.temporal_residue.description", "Drain a creature of all its Time.");

        // Journal (Patchouli)
        add("chronogenesis.journal.name", "Experimenter's Journal");
        add("chronogenesis.journal.landing", "Notes on the attributes of living things, and on what can be done with them once they are taken.");

        // Config screen
        add("chronogenesis.configuration.title", "ChronoGenesis Configs");
        add("chronogenesis.configuration.section.chronogenesis.common.toml", "ChronoGenesis Configs");
        add("chronogenesis.configuration.section.chronogenesis.common.toml.title", "ChronoGenesis Configs");
        add("chronogenesis.configuration.essence", "Essence");
        add("chronogenesis.configuration.syringeCooldownTicks", "Syringe Cooldown (ticks)");
        add("chronogenesis.configuration.organisms", "Organisms");
        add("chronogenesis.configuration.growthCost", "Growth Cost (drams per flesh block)");
        add("chronogenesis.configuration.baseCapacity", "Core Capacity (drams)");
        add("chronogenesis.configuration.capacityPerFlesh", "Capacity per Flesh Block (drams)");
        add("chronogenesis.configuration.upkeepIntervalTicks", "Upkeep Interval (ticks)");
        add("chronogenesis.configuration.leechRange", "Leech Tendril Range (blocks)");
    }
}

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
        addItem(ModItems.PRESERVED_HEART, "Preserved Heart");
        addItem(ModItems.TEMPORAL_RESIDUE, "Temporal Residue");

        // Blocks
        addBlock(ModBlocks.BRASS_ARMATURE, "Brass Armature");
        addBlock(ModBlocks.CHRONOFLESH, "Chronoflesh");
        addBlock(ModBlocks.ORGANISM_CORE, "Organism Core");
        addBlock(ModBlocks.BRASS_CONDUIT, "Brass Conduit");
        addBlock(ModBlocks.TAP, "Tap Graft");
        addBlock(ModBlocks.MAW, "Maw Graft");
        addBlock(ModBlocks.STASIS_BLADDER, "Stasis Bladder Graft");
        addBlock(ModBlocks.STARTING_GATE, "Starting Gate Graft");
        addBlock(ModBlocks.FINISH, "Finish Graft");
        addBlock(ModBlocks.HEART, "Heart Graft");
        addBlock(ModBlocks.EYE, "Eye Graft");
        addBlock(ModBlocks.CLOCK, "Clock Graft");
        addBlock(ModBlocks.GANGLION, "Ganglion Graft");
        addBlock(ModBlocks.DISSECTION_TABLE, "Dissection Table");
        addBlock(ModBlocks.CHRONOCORTEX, "Chronocortex");
        addItem(ModItems.CHRONO_GOGGLES, "Chrono-Insulated Goggles");
        addItem(ModItems.CHRONO_LAB_COAT, "Chrono-Insulated Lab Coat");
        addItem(ModItems.CHRONO_TROUSERS, "Chrono-Insulated Trousers");
        addItem(ModItems.CHRONO_GALOSHES, "Chrono-Insulated Galoshes");
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

        // Veins, Racetrack, Time organisms
        add("message.chronogenesis.vein.unclogged", "You cut the rotting clot out of the vein.");
        // The Chronocortex: the ritual
        add("message.chronogenesis.table.brain_set", "The brain settles in its jar. Now it must soak up Awareness and Time.");
        add("message.chronogenesis.table.no_brain", "There is no brain on the table.");
        add("message.chronogenesis.table.full", "The brain cannot take any more of that.");
        add("message.chronogenesis.table.status", "Awareness %s/%s, Time %s/%s, lightning rods: %s");
        add("message.chronogenesis.table.burnt", "The brain twitches, then smokes. It did not have enough of a mind to hold the current.");
        add("message.chronogenesis.gland.deeper", "The gland fills you deeper than before. %s answers more strongly.");
        add("message.chronogenesis.clue", "A clue for the journal: %s");

        // Clues
        add("clue.chronogenesis.floating_creature", "A creature that forgot its weight");
        add("clue.chronogenesis.vacant_mind", "A mind left vacant");
        add("clue.chronogenesis.dust_to_dust", "Dust that was once alive");
        add("clue.chronogenesis.torn_time", "A tear in time");
        add("clue.chronogenesis.the_long_road", "Time left on a long road");
        add("clue.chronogenesis.heavy_heart", "The beats a heavy heart misses");
        add("clue.chronogenesis.its_alive", "A brain woken by lightning");

        // Research
        add("research.chronogenesis.level_2_powers", "Deeper Infusion");
        add("research.chronogenesis.level_2_powers.desc", "The Enhancing Gland can fill you deeper: every power you hold grows to its second level. Density: diamond-hard fists. Awareness: you see through sneaking. Time: a wider, longer Stutter.");

        // The Chronocortex speaks
        add("whisper.chronogenesis.prefix", "<The Chronocortex> ");
        add("whisper.chronogenesis.wake", "...light. So much light. Who are you? Are you my maker? Show me everything.");
        add("whisper.chronogenesis.unstable", "The time at %s %s %s is slipping. It will tear. Do something.");
        add("whisper.chronogenesis.research_done", "%s. Yes. I understand it now. Do you?");
        add("whisper.chronogenesis.idle.0", "I can hear them all, the little hearts. Feed them. They are hungry.");
        add("whisper.chronogenesis.idle.1", "What was here before the house? Before the hill? I want to see.");
        add("whisper.chronogenesis.idle.2", "Your clock is ticking. I can hear it from here.");
        add("whisper.chronogenesis.idle.3", "Bring me something old. Older than you.");
        add("whisper.chronogenesis.idle.4", "I dreamt of ice. Miles of it. Is that a memory?");
        add("whisper.chronogenesis.idle.5", "The cows are floating again. I like the cows.");
        add("whisper.chronogenesis.idle.6", "There were others like me. I can almost remember their names.");
        add("whisper.chronogenesis.idle.7", "Teach me. I will teach you back.");

        // The Chronocortex screen
        add("screen.chronogenesis.chronocortex.title", "Chronocortex");
        add("screen.chronogenesis.chronocortex.organisms", "Organisms");
        add("screen.chronogenesis.chronocortex.research", "Research");
        add("screen.chronogenesis.chronocortex.begin", "Research");
        add("screen.chronogenesis.chronocortex.stage", "Stage %s");
        add("screen.chronogenesis.chronocortex.no_organisms", "No organisms are wired to me. Graft a Ganglion onto one within 32 blocks and I will feel it.");
        add("screen.chronogenesis.chronocortex.organism", "%s Organism (%s) at %s %s %s");
        add("screen.chronogenesis.chronocortex.instability", "Unstable %s/%s");
        add("screen.chronogenesis.chronocortex.no_research", "I cannot see anything to learn yet.");
        add("screen.chronogenesis.chronocortex.pick", "Choose something to learn.");
        add("screen.chronogenesis.chronocortex.done", "Understood.");
        add("screen.chronogenesis.chronocortex.needs_first", "Something else must be understood first.");
        add("screen.chronogenesis.chronocortex.essence_cost", "%s from wired organisms: %s/%s");
        add("screen.chronogenesis.chronocortex.material_cost", "%s: %s/%s");
        add("screen.chronogenesis.chronocortex.clues", "Clues (any one line):");

        // Umbilical Locket
        addItem(ModItems.UMBILICAL_LOCKET, "Umbilical Locket");
        add("message.chronogenesis.locket.linked", "The cord takes. You are tied to the %s organism.");
        add("tooltip.chronogenesis.locket.unlinked", "Use it on an organism's Tap to tie it there");
        add("tooltip.chronogenesis.locket.linked", "Tied to the organism at %s %s %s (%s)");
        add("tooltip.chronogenesis.locket.range", "Wear it in the necklace slot. Refills you within %s blocks");
        add("research.chronogenesis.umbilical_locket", "Umbilical Locket");
        add("research.chronogenesis.umbilical_locket.desc", "A cord from a living store to your own body. Worn at the throat, tied to an organism through its Tap, it refills your pool of that essence while you stay near the lab.");
        add("screen.chronogenesis.chronocortex.regrow", "Grow another");

        // Temporal Field
        addBlock(ModBlocks.TEMPORAL_FIELD, "Temporal Field Graft");
        add("research.chronogenesis.temporal_field", "Temporal Field");
        add("research.chronogenesis.temporal_field.desc", "A graft for a Time organism that gently rewinds the creatures around it, so what was drained from them slowly grows back. The end of breeding animals only to drain them.");

        // The Temporal Lens and the past
        addItem(ModItems.TEMPORAL_LENS, "Temporal Lens");
        addItem(ModItems.INTACT_BLUEPRINT, "Intact Blueprint");
        addBlock(ModBlocks.FADED_BLUEPRINT, "Faded Blueprint");
        add("lens.chronogenesis.depth.echo", "Echo (hours)");
        add("lens.chronogenesis.depth.decades", "Decades");
        add("lens.chronogenesis.depth.centuries", "Centuries");
        add("message.chronogenesis.lens.depth", "The Lens now looks back: %s");
        add("message.chronogenesis.lens.no_time", "The Lens needs Time to see through. Pour a Stasis Ampoule onto it.");
        add("message.chronogenesis.lens.scanned", "You trace the faded lines into your journal.");
        add("message.chronogenesis.blueprint.faded", "The lines have faded past reading. Perhaps they were clearer once.");
        add("tooltip.chronogenesis.lens.depth", "Looks back: %s");
        add("tooltip.chronogenesis.lens.hint", "Hold use to look; sneak-use to look further back");
        add("tooltip.chronogenesis.blueprint.locket", "A cord, a locket, a heart: something worn at the throat");
        add("tooltip.chronogenesis.blueprint.field", "A dome on a Time organism, and animals around it, whole again");
        add("clue.chronogenesis.blueprint_locket", "A scribbled blueprint: the locket on a cord");
        add("clue.chronogenesis.blueprint_field", "A scribbled blueprint: the rewinding dome");
        add("clue.chronogenesis.lab_in_its_prime", "A laboratory as it was, lights burning");
        add("research.chronogenesis.temporal_lens", "Temporal Lens");
        add("research.chronogenesis.temporal_lens.desc", "A spyglass that looks backwards. Burns Time to show the past over the present: ruins whole again, and old blueprints legible where they lie.");

        // Obelisks
        addBlock(ModBlocks.OBELISK_STONE, "Obelisk Stone");
        addBlock(ModBlocks.OBELISK_INSCRIPTION, "Obelisk Inscription");
        addBlock(ModBlocks.OBELISK_CAPSTONE, "Obelisk Capstone");
        add("clue.chronogenesis.nail_in_time", "A stone that time bends around");
        add("clue.chronogenesis.older_inscription", "Fragments of an inscription older than time");

        // Creative testing aid
        addItem(ModItems.ETERNAL_HOURGLASS, "Eternal Hourglass");
        add("tooltip.chronogenesis.eternal_hourglass", "While carried: every power at its highest level, every pool full, no Temporal Exposure, every Temporal Lens charged and free to look through");
        add("tooltip.chronogenesis.eternal_hourglass.creative", "Creative only");

        add("message.chronogenesis.gate.pace", "Pace %s: %s ticks per block of track. Slower pulses make more Time, but stall past 600 ticks.");
        add("message.chronogenesis.organism.stability", " | stability %s/%s, instability %s/%s");

        // Time
        add("message.chronogenesis.tear.not_enough", "Sealing a tear takes %s drams of Density.");
        add("message.chronogenesis.tear.sealed", "The weight of it presses the rift shut.");

        // Keys
        add("key.categories.chronogenesis", "ChronoGenesis");
        add("key.chronogenesis.cycle_power", "Select power (tap: next, hold: radial menu)");
        add("screen.chronogenesis.radial.title", "Choose a power");
        add("screen.chronogenesis.radial.hint", "Point at a power and release");
        add("screen.chronogenesis.radial.none", "No powers yet. Fill yourself at an Enhancing Gland.");
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

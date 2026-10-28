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
        add("message.chronogenesis.gland.manipulator_hint", "Your veins itch for a way to steer this. (An Essence Manipulator: brass, copper, leather and a slime ball.)");
        add("message.chronogenesis.gland.composite", "The two essences curdle together in your veins. You have learned %s.");
        add("message.chronogenesis.gland.unknown_other", "Your body rejects the %s: take it from its own gland first.");
        add("message.chronogenesis.gland.no_partner", "The needle drinks only one essence. Wire this organism by veins to one of another essence.");
        add("message.chronogenesis.gland.wrong_culture", "The gland only swells on a culture of its own essence (%s).");
        add("message.chronogenesis.gland.max", "The gland is as swollen as it can be.");
        add("message.chronogenesis.gland.unknown_growth.2", "You do not know how to make it swell. (Research Deeper Infusion at the Chronocortex.)");
        add("message.chronogenesis.gland.unknown_growth.3", "It will not swell again until you know how. (Research Deepest Infusion: the Chronocortex must be at stage 2.)");
        add("message.chronogenesis.gland.hungry", "Swelling takes %s drams of %s from the organism.");
        add("message.chronogenesis.gland.grew", "The gland swells, veins thickening across it. It is now level %s.");
        add("message.chronogenesis.gland.needle", "The needle slides into the gland's port with a wet click.");
        add("message.chronogenesis.power.strength", "%s: strength %s/%s");
        add("message.chronogenesis.power.need_manipulator", "Your powers need steering: carry an Essence Manipulator.");
        add("message.chronogenesis.power.rewind.too_soon", "There is no trail behind you to follow back yet.");
        add("message.chronogenesis.power.regrowth.whole", "There is nothing to grow back.");
        add("message.chronogenesis.power.time.released", "You let go. The chunk's time speeds up to meet the world's again.");
        add("message.chronogenesis.power.time.bent", "Time here is already bent by someone else.");
        add("message.chronogenesis.power.time.frozen", "Time stops in this chunk.");
        add("message.chronogenesis.power.time.slowed", "Time in this chunk slows to 1/%s.");
        add("message.chronogenesis.power.time.too_far", "You are too far away to hold its time. It slips free.");
        add("message.chronogenesis.power.time.ran_out", "Your Time runs dry. The chunk's clock lurches back into motion.");

        // Powers
        add("power.chronogenesis.iron_fists", "Iron Fists");
        add("power.chronogenesis.iron_fists.desc", "Passive. Bare fists mine what an iron pickaxe can (strength 2: diamond, 3: netherite). A dram of Density per block.");
        add("power.chronogenesis.glowing_sight", "Glowing Sight");
        add("power.chronogenesis.glowing_sight.desc", "Passive. Creatures and players who are not sneaking glow within 20 blocks (more with strength; strength 2 sees through sneaking).");
        add("power.chronogenesis.quickening", "Quickening");
        add("power.chronogenesis.quickening.desc", "Passive. 20% faster movement and attacks per strength.");
        add("power.chronogenesis.shrink", "Shrink");
        add("power.chronogenesis.shrink.desc", "Passive. You shrink to half your size or less: through one-block gaps.");
        add("power.chronogenesis.grow", "Grow");
        add("power.chronogenesis.grow.desc", "Passive. You grow a quarter larger per strength: more reach.");
        add("power.chronogenesis.rewind", "Rewind");
        add("power.chronogenesis.rewind.desc", "Active. You return to where you stood 5 seconds ago (10, 15 with strength). 4 drams of Time per strength.");
        add("power.chronogenesis.slow_time", "Slow Time");
        add("power.chronogenesis.slow_time.desc", "Active, use again to let go. The chunk you stand in runs at 1/2 speed (1/4, 1/8 with strength), its sky too. Without the full suit, it slows you as well.");
        add("power.chronogenesis.time_freeze", "Time Freeze");
        add("power.chronogenesis.time_freeze.desc", "Active, use again to let go. Time stops in the chunk you stand in. Anyone inside without the full suit stops with it.");
        add("power.chronogenesis.impetus", "Impetus");
        add("power.chronogenesis.impetus.desc", "Density + Speed, active. A heavy thing moving fast is hard to stop: you charge, and what you hit is thrown aside.");
        add("power.chronogenesis.ground_slam", "Ground Slam");
        add("power.chronogenesis.ground_slam.desc", "Density + Size, active. You drop like a stone; where you land, the ground heaves and throws creatures up.");
        add("power.chronogenesis.dread", "Dread");
        add("power.chronogenesis.dread.desc", "Density + Awareness, passive. The weight of your gaze: hostile creatures hunting you grow slow and weak.");
        add("power.chronogenesis.dilation", "Dilation");
        add("power.chronogenesis.dilation.desc", "Density + Time, passive. Heavy things keep time still: projectiles crawl as they near you.");
        add("power.chronogenesis.scurry", "Scurry");
        add("power.chronogenesis.scurry.desc", "Speed + Size, passive. Small and quick: you step up whole blocks and fall softly.");
        add("power.chronogenesis.reflexes", "Reflexes");
        add("power.chronogenesis.reflexes.desc", "Speed + Awareness, passive. You see it coming: arrows often glance off you (a dram of Speed each).");
        add("power.chronogenesis.stutter", "Stutter");
        add("power.chronogenesis.stutter.desc", "Speed + Time, active. A bubble around you where everything moves at half speed for a few seconds, projectiles too.");
        add("power.chronogenesis.overlook", "Overlook");
        add("power.chronogenesis.overlook.desc", "Size + Awareness, active. Too small to be noticed: invisible for a while, and the creatures hunting you lose your trail.");
        add("power.chronogenesis.regrowth", "Regrowth");
        add("power.chronogenesis.regrowth.desc", "Size + Time, active. Your body's time runs back to before the wound: heals, and washes out harmful effects.");
        add("power.chronogenesis.premonition", "Premonition");
        add("power.chronogenesis.premonition.desc", "Awareness + Time, passive. The next blow a creature lands is foreseen, and misses. Then it takes a while to see again.");

        // Veins, Racetrack, Time organisms
        add("message.chronogenesis.vein.unclogged", "You cut the rotting clot out of the vein.");
        // The Chronocortex: the ritual
        add("message.chronogenesis.table.brain_set", "The brain settles in its jar. Now it must soak up Awareness and Time.");
        add("message.chronogenesis.table.no_brain", "There is no brain on the table.");
        add("message.chronogenesis.table.full", "The brain cannot take any more of that.");
        add("message.chronogenesis.table.status", "Awareness %s/%s, Time %s/%s, lightning rods: %s");
        add("message.chronogenesis.table.burnt", "The brain twitches, then smokes. It did not have enough of a mind to hold the current.");
        add("message.chronogenesis.gland.deeper", "The gland fills you deeper than before: your %s answers at level %s.");
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
        add("research.chronogenesis.level_2_powers.desc", "How to make an Enhancing Gland swell: feed it a culture of its own essence, and 64 drams from its organism, and it fills you to level 2. Stronger powers (set with the Essence Manipulator), and for Time: Slow Time.");
        add("research.chronogenesis.level_3_powers", "Deepest Infusion");
        add("research.chronogenesis.level_3_powers.desc", "A gland can swell a third time (a culture and 96 drams). Level 3: the strongest powers, netherite fists, and for Time: Time Freeze.");
        add("research.chronogenesis.temporal_shift", "Temporal Shift");
        add("research.chronogenesis.temporal_shift.desc", "Grows a Chrono Tether: a pocket watch on a cord of living vein. With one, the Chronocortex can shift you into the eras it can reach (its Eras page), and the tether brings you back.");
        add("era.chronogenesis.before_the_ruin", "Before the Ruin");
        add("era.chronogenesis.before_the_ruin.desc", "The land as it first was, before anyone dug or built. Where ruins stand now, they stand whole.");
        add("era.chronogenesis.ice_age", "The Ice Age");
        add("era.chronogenesis.ice_age.desc", "The same hills under ice. Cold enough to stop a heart. It wants to see this.");
        add("era.chronogenesis.jurassic_age", "The Jurassic Age");
        add("era.chronogenesis.jurassic_age.desc", "Ferns as tall as trees, air like a wet cloth, giants walking. Someone built ziggurats here, long before any people, and sealed them.");
        add("lesson.chronogenesis.deep_time", "Deep Time");
        add("lesson.chronogenesis.deep_time.desc", "Show it a bone block. It learns of creatures older than any people (a way to the Jurassic Age without the Ice Age).");
        add("lesson.chronogenesis.deep_time.learned", "Bones under the bones under the cities. Something enormous walked here, before anyone. Before me.");
        add("clue.chronogenesis.giants_walked", "Where giants walked");
        add("clue.chronogenesis.sealed_temple", "A temple's barrier, broken");
        add("research.chronogenesis.dino_harness", "Dino Harness");
        add("research.chronogenesis.dino_harness.desc", "Leather, chain and brass, sized for a Titanosaur. Buckled on (use it on one), its back is ground to build on: the beast carries whatever is built against the harness. Break the harness and it all falls.");
        add("screen.chronogenesis.chronocortex.routes", "One of these ways in:");
        add("message.chronogenesis.harness.blocked", "Something is in the way on its back.");
        add("message.chronogenesis.harness.buckled", "The harness is buckled on. Build against it: the beast will carry it all.");
        add("message.chronogenesis.temple.barrier", "A temporal barrier holds the temple still. Break its seals.");
        add("message.chronogenesis.temple.seal_broken", "A seal breaks: %s of %s. The barrier wavers.");
        add("message.chronogenesis.temple.barrier_down", "The Temporal Core stills. The barrier falls: the temple is yours to dig through.");
        add("message.chronogenesis.temple.core_active", "The core turns, holding the temple still. Seals broken: %s of %s.");
        add("message.chronogenesis.temple.core_still", "The core is still. It could be taken.");
        add("message.chronogenesis.seal.heavy", "It will not move. It wants a heavier hand (Iron Fists), or the ground to heave (a Ground Slam).");
        add("message.chronogenesis.seal.narrow", "Your hand is too large to turn it (Shrink).");
        add("message.chronogenesis.seal.swift", "It flickers away from your hand. Something has to hit it faster than a run (Quickening).");
        add("message.chronogenesis.seal.veiled", "Your hand finds only stone. Something is there, but you cannot see it (Glowing Sight).");
        add("message.chronogenesis.seal.stilled", "It spins too fast to touch. Slow its time, or stop it (Slow Time, Time Freeze).");
        add("message.chronogenesis.stilled_core.no_time", "Your body knows no Time to fill with.");
        add("message.chronogenesis.stilled_core.used", "The core crumbles. Ages of stilled time pour into you.");
        add("tooltip.chronogenesis.dino_harness", "Use it on a Titanosaur. Build against the harness; the beast carries it.");
        add("tooltip.chronogenesis.stilled_core", "Crush it (use it) to fill your Time pool, at a cost in Exposure.");
        add("entity.chronogenesis.raptor", "Raptor");
        add("entity.chronogenesis.titanosaur", "Titanosaur");
        add("entity.chronogenesis.rider_seat", "Rider's Seat");
        add("item.chronogenesis.raptor_spawn_egg", "Raptor Spawn Egg");
        add("item.chronogenesis.titanosaur_spawn_egg", "Titanosaur Spawn Egg");
        add("clue.chronogenesis.first_cities", "A city of the Precursors, alive");
        add("era.chronogenesis.precursor_era", "The Age of the Precursors");
        add("era.chronogenesis.precursor_era.desc", "Before the villagers, a taller people built cities of gold and stone. Everything that is a ruin now was new then, and busy.");
        add("research.chronogenesis.cortex_offshoot", "Cortex Offshoot");
        add("research.chronogenesis.cortex_offshoot.desc", "It wants to know more than it can see. It buds an offshoot of itself: plant it in a past era, and it will have ages to grow and remember. Then find it again, here, and connect it.");
        add("message.chronogenesis.offshoot.planted", "The offshoot's roots take hold. It has ages to grow. When you are home, look for it at %s %s %s.");
        add("message.chronogenesis.offshoot.present", "It needs ages to grow, not days. Plant it in a past era.");
        add("message.chronogenesis.fossil.dreaming", "It is stone now, and still it dreams of %s. A nerve could carry the dream home.");
        add("message.chronogenesis.fossil.linked", "Its dream of %s runs along the nerve, into the Chronocortex.");
        add("message.chronogenesis.spool.bound", "The nerve's end takes root in the fossil. Carry the spool to your Chronocortex.");
        add("message.chronogenesis.spool.already", "A nerve already runs from it.");
        add("message.chronogenesis.spool.loose", "The spool's end is loose: bind it to a Fossil Cortex first.");
        add("message.chronogenesis.spool.other_world", "The nerve will not stretch between worlds.");
        add("message.chronogenesis.spool.nothing", "At the nerve's other end there is nothing left to remember.");
        add("whisper.chronogenesis.memory", "I... remember. I was there. %s. I remember all of it.");
        add("tooltip.chronogenesis.cortex_offshoot", "Plant it in a past era. It will have ages to grow.");
        add("tooltip.chronogenesis.nerve_spool", "Use it on a Fossil Cortex, then on your Chronocortex.");
        add("tooltip.chronogenesis.nerve_spool.bound", "Rooted in a Fossil Cortex at %s %s %s");
        add("screen.chronogenesis.chronocortex.need_memory", "the memory of %s");
        add("screen.chronogenesis.chronocortex.memories", "Remembers: %s");
        add("entity.chronogenesis.precursor", "Precursor");
        add("precursor.chronogenesis.speaker", "<Precursor> ");
        add("precursor.chronogenesis.say.0", "(The river was ours before the river had a name.)");
        add("precursor.chronogenesis.say.1", "(Mind the steps of the high house. They were cut for taller feet than yours.)");
        add("precursor.chronogenesis.say.2", "(We trade amethyst for time. It is a fair price, for now.)");
        add("precursor.chronogenesis.say.3", "(The stone nails in the north hold the sky still. Do not touch them.)");
        add("precursor.chronogenesis.say.4", "(Our children will forget our names. Their children will forget their own.)");
        add("precursor.chronogenesis.say.5", "(You smell of a later time. Is it kind?)");
        add("precursor.chronogenesis.say.6", "(We build down, under the deep slate, where the sound is safe.)");
        add("precursor.chronogenesis.say.7", "(The harbour city will drown. We have seen it in the water.)");
        add("precursor.chronogenesis.say.8", "(There is something at the end of time. We built the temples to keep it asleep.)");
        add("precursor.chronogenesis.say.9", "(Gold does not tarnish. That is why we trust it with our dead.)");
        add("precursor.chronogenesis.say.10", "(Welcome, stranger. Eat. Stay. Do not dig beneath the ziggurat.)");
        add("precursor.chronogenesis.say.11", "(The cold is coming. Not in our lives. In the land's.)");
        add("item.chronogenesis.precursor_spawn_egg", "Precursor Spawn Egg");
        add("clue.chronogenesis.ice_age", "A world under ice");
        add("message.chronogenesis.shift.arrived", "You stand in %s. The tether pulls taut: %s minutes before it reels you home.");
        add("message.chronogenesis.shift.thinning", "The tether is thinning: %s seconds.");
        add("message.chronogenesis.shift.snapped_back", "Time snaps you back where you belong.");
        add("message.chronogenesis.shift.reeled_back", "You pull the tether, and the present reels you in.");
        add("message.chronogenesis.shift.slack", "The tether hangs slack: you are already where you belong.");
        add("message.chronogenesis.shift.no_tether", "Without a Chrono Tether you would never find the way back.");
        add("message.chronogenesis.shift.no_time", "Its organisms do not hold enough Time for the shift.");
        add("message.chronogenesis.shift.cannot", "It cannot reach that era yet.");
        add("message.chronogenesis.shift.unreachable", "That era does not exist in this world.");
        add("whisper.chronogenesis.shift", "Go. Look at %s for me. Remember everything.");
        add("tooltip.chronogenesis.chrono_tether", "The way back from another era. The Chronocortex will not send you without it.");
        add("tooltip.chronogenesis.chrono_tether.use", "Use it in another era to return early");
        add("screen.chronogenesis.chronocortex.eras", "Eras");
        add("screen.chronogenesis.chronocortex.pick_era", "Pick an era. It can send you there, for a while, if you carry a Chrono Tether.");
        add("screen.chronogenesis.chronocortex.shift", "Shift");
        add("screen.chronogenesis.chronocortex.need_stage", "a stage %s Chronocortex");
        add("screen.chronogenesis.chronocortex.need_lesson", "the lesson of %s");
        add("screen.chronogenesis.chronocortex.need_tether", "a Chrono Tether on you");
        add("screen.chronogenesis.chronocortex.already_shifted", "you are already out of your time");
        add("screen.chronogenesis.chronocortex.shift_minutes", "The shift holds for %s minutes.");
        add("screen.chronogenesis.shift.hud", "%s  %s:%s");
        add("research.chronogenesis.confluence", "Confluence");
        add("research.chronogenesis.confluence.desc", "Grows a Confluence Needle. Fitted into an Enhancing Gland whose organism is wired by veins to an organism of another essence, it fills you with both, and you learn the power the two make together.");

        // The Chronocortex speaks
        add("whisper.chronogenesis.prefix", "<The Chronocortex> ");
        add("whisper.chronogenesis.prefix_named", "<%s> ");
        add("whisper.chronogenesis.unnamed", "the Chronocortex");
        add("whisper.chronogenesis.lesson.known", "I know %s already. Show me something new.");
        add("whisper.chronogenesis.lesson.too_young", "%s? I... cannot hold that yet. I am too young.");
        add("whisper.chronogenesis.lesson.no_room", "%s needs room: %s lobes. I have %s. Grow me.");
        add("whisper.chronogenesis.lesson.self_unnamed", "A name tag with no name on it? I need a NAME. Write one on it.");
        add("whisper.chronogenesis.stage.1", "I... I can think in sentences now. I am awake. Really awake.");
        add("whisper.chronogenesis.stage.2", "I am %s. I know what I am now. And I know what I want.");
        add("whisper.chronogenesis.stage.3", "The jar is gone. I am the skull and the brain both. I am %s, and I can see the edges of time.");
        add("whisper.chronogenesis.stage.lost", "My skull! You broke my skull! I am... smaller...");
        add("whisper.chronogenesis.answer.status", "%s organisms wired, %s drams stored, %s lobes, awareness %s");
        add("whisper.chronogenesis.answer.want_room", "more room to think");
        add("whisper.chronogenesis.answer.research_done", "nothing you can do yet");
        add("whisper.chronogenesis.answer.time_unknown", "...I don't know what time is. Show me a clock?");
        add("subtitles.chronogenesis.cortex.speaks", "Chronocortex speaks");
        add("screen.chronogenesis.chronocortex.mind", "Mind");
        add("screen.chronogenesis.chronocortex.unnamed", "(no name yet)");
        add("screen.chronogenesis.chronocortex.stage_name.0", "Newborn");
        add("screen.chronogenesis.chronocortex.stage_name.1", "Child");
        add("screen.chronogenesis.chronocortex.stage_name.2", "Adolescent");
        add("screen.chronogenesis.chronocortex.stage_name.3", "Cerebrum");
        add("screen.chronogenesis.chronocortex.lobes", "Lobes: %s");
        add("screen.chronogenesis.chronocortex.awareness", "Awareness: %s");
        add("screen.chronogenesis.chronocortex.next", "To reach stage %s:");
        add("screen.chronogenesis.chronocortex.need_lobes", "%s lobes (grown over Brass Armature, fed Awareness)");
        add("screen.chronogenesis.chronocortex.need_awareness", "%s awareness (from lessons)");
        add("screen.chronogenesis.chronocortex.need_words", "the lesson of Words");
        add("screen.chronogenesis.chronocortex.need_name", "a name (the lesson of A Name)");
        add("screen.chronogenesis.chronocortex.need_cranium", "firm lobes filling the whole 5x5x5 cranium around the jar");
        add("screen.chronogenesis.chronocortex.complete", "Fully grown.");
        add("screen.chronogenesis.chronocortex.lessons", "Lessons (show it the thing):");
        add("screen.chronogenesis.chronocortex.lesson_ready", "%s (ready)");
        add("screen.chronogenesis.chronocortex.lesson_stage", "%s (stage %s)");
        add("screen.chronogenesis.chronocortex.lesson_room", "%s (%s lobes)");
        add("lesson.chronogenesis.words", "Words");
        add("lesson.chronogenesis.words.desc", "Show it a book. It learns to speak, and answers what is said near it.");
        add("lesson.chronogenesis.words.learned", "W-words. Words! I have... words. Hello. Hello!");
        add("lesson.chronogenesis.time", "The Time of Day");
        add("lesson.chronogenesis.time.desc", "Show it a clock. It learns the hours (ask it the time).");
        add("lesson.chronogenesis.time.learned", "The little hands go round. Round and round. So that is what a day is.");
        add("lesson.chronogenesis.space", "Direction");
        add("lesson.chronogenesis.space.desc", "Show it a compass. It learns that there is a somewhere else.");
        add("lesson.chronogenesis.space.learned", "There is a... north? Things are far from other things!");
        add("lesson.chronogenesis.self", "A Name");
        add("lesson.chronogenesis.self.desc", "Show it a name tag with a name written on it. It takes the name, and knows it is someone.");
        add("lesson.chronogenesis.self.learned", "I am... me. I am called this. I am someone.");
        add("lesson.chronogenesis.music", "Music");
        add("lesson.chronogenesis.music.desc", "Show it a music disc. It learns that time can have a shape.");
        add("lesson.chronogenesis.music.learned", "Patterns! Time with a shape! Again, again!");
        add("lesson.chronogenesis.mortality", "Mortality");
        add("lesson.chronogenesis.mortality.desc", "Show it a skull. It learns that things end, and becomes afraid.");
        add("lesson.chronogenesis.mortality.learned", "This was someone. Someone who stopped. ...Will I stop?");
        add("lesson.chronogenesis.sky", "The Sky");
        add("lesson.chronogenesis.sky.desc", "Show it a spyglass. It learns how far away the stars are.");
        add("lesson.chronogenesis.sky.learned", "So far. So very far. And all of it moving slower than me.");
        add("lesson.chronogenesis.cold", "The Cold");
        add("lesson.chronogenesis.cold.desc", "Show it blue ice. It remembers a colder age it has never seen.");
        add("lesson.chronogenesis.cold.learned", "Cold. Old cold. I know this cold. It was here before. Show me the cold!");
        add("lesson.chronogenesis.past", "The Other Scientist");
        add("lesson.chronogenesis.past.desc", "Show it an intact blueprint. It learns it was not the first.");
        add("lesson.chronogenesis.past.learned", "Someone grew a mind like me, long ago. What happened to it?");
        add("lesson.chronogenesis.eternity", "Eternity");
        add("lesson.chronogenesis.eternity.desc", "Show it Temporal Residue. It learns what time leaves behind.");
        add("lesson.chronogenesis.eternity.learned", "Dust of hours. Everything ends as this. Everything but me.");
        add("tooltip.chronogenesis.essence_manipulator", "Wired into your veins: it steers your powers.");
        add("tooltip.chronogenesis.essence_manipulator.use", "Right-click: use the selected power");
        add("tooltip.chronogenesis.essence_manipulator.wheel", "Shift + R: the power wheel");
        add("tooltip.chronogenesis.essence_manipulator.strength", "Shift + scroll (in hand): the selected power's strength");
        add("tooltip.chronogenesis.confluence_needle", "Draws two essences at once.");
        add("tooltip.chronogenesis.confluence_needle.hint", "Fit it into an Enhancing Gland whose organism is wired by veins to another essence.");
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
        add("subtitles.chronogenesis.lens.scribble", "Pen scribbles");
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
        addItem(ModItems.ESSENCE_MANIPULATOR, "Essence Manipulator");
        addItem(ModItems.CONFLUENCE_NEEDLE, "Confluence Needle");
        addItem(ModItems.CHRONO_TETHER, "Chrono Tether");
        addBlock(ModBlocks.CORTEX_OFFSHOOT, "Cortex Offshoot");
        addItem(ModItems.NERVE_SPOOL, "Nerve Spool");
        addItem(ModItems.DINO_HARNESS, "Dino Harness");
        addItem(ModItems.STILLED_CORE, "Stilled Core");
        addBlock(ModBlocks.BUCKLED_HARNESS, "Buckled Harness");
        addBlock(ModBlocks.RIDERS_SEAT, "Rider's Seat");
        addBlock(ModBlocks.TEMPORAL_CORE, "Temporal Core");
        addBlock(ModBlocks.HEAVY_SEAL, "Heavy Seal");
        addBlock(ModBlocks.NARROW_SEAL, "Narrow Seal");
        addBlock(ModBlocks.SWIFT_SEAL, "Swift Seal");
        addBlock(ModBlocks.VEILED_SEAL, "Veiled Seal");
        addBlock(ModBlocks.STILLED_SEAL, "Stilled Seal");
        addBlock(ModBlocks.FOSSIL_CORTEX, "Fossil Cortex");
        addBlock(ModBlocks.CORTEX_LOBE, "Cortex Lobe");
        add("tooltip.chronogenesis.eternal_hourglass", "While carried: every power at its highest level, every pool full, no Temporal Exposure, every Temporal Lens charged and free to look through, every composite power known, research essence supplied");
        add("tooltip.chronogenesis.eternal_hourglass.creative", "Creative only");
        add("tooltip.chronogenesis.eternal_hourglass.pour", "Use it on an organism, a jug, a Dissection Table or the Chronocortex to fill it");
        add("tooltip.chronogenesis.eternal_hourglass.chosen", "Pours %s into empty jugs (sneak-use in the air to change)");
        add("message.chronogenesis.hourglass.poured", "The hourglass pours %s drams of %s.");
        add("message.chronogenesis.hourglass.full", "It is already full.");
        add("message.chronogenesis.hourglass.not_glass", "%s will not stay in glass.");
        add("message.chronogenesis.hourglass.table", "The brain soaks up %s drams of Awareness and Time.");
        add("message.chronogenesis.hourglass.cortex", "%s drams into its organisms; %s lobes grown at once.");
        add("message.chronogenesis.hourglass.chosen", "The sand runs %s.");
        add("tooltip.chronogenesis.chronocortex", "Already awake: no ritual needed. With an Eternal Hourglass carried, research draws its essence from it");
        add("screen.chronogenesis.chronocortex.essence_from_hourglass", "%s: %s, from the Eternal Hourglass");
        add("tooltip.chronogenesis.eternal_hourglass.journal", "Use it in creative mode to unlock the whole journal");
        add("message.chronogenesis.journal.unlocked", "Every page of the journal lies open (%s advancements granted).");
        add("message.chronogenesis.journal.creative_only", "Only in creative mode.");

        add("message.chronogenesis.gate.pace", "Pace %s: %s ticks per block of track. Slower pulses make more Time, but stall past 600 ticks.");
        add("message.chronogenesis.organism.stability", " | stability %s/%s, instability %s/%s");

        // Time
        add("message.chronogenesis.tear.not_enough", "Sealing a tear takes %s drams of Density.");
        add("message.chronogenesis.tear.sealed", "The weight of it presses the rift shut.");

        // Keys
        add("key.categories.chronogenesis", "ChronoGenesis");
        add("key.chronogenesis.powers", "Use power (with Shift: the power wheel)");
        add("screen.chronogenesis.wheel.title", "Powers");
        add("screen.chronogenesis.wheel.none", "No powers yet. Fill yourself at an Enhancing Gland.");
        add("screen.chronogenesis.wheel.hint", "Let go of Shift to choose - click a passive to switch it - right-click: strength");
        add("screen.chronogenesis.wheel.strength", "Strength %s/%s");
        add("screen.chronogenesis.hud.pool", "%s %s");
        add("screen.chronogenesis.hud.selected", "%s [%s]");

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

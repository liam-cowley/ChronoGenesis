# Early Game: "The First Experiment"

> Status: **design draft**. A walkthrough of the first ~4–5 hours. The systems themselves are
> defined in `core-systems.md`; this doc covers order, pacing and how the player feels.

## Mood pillars
- **You are a mad scientist,** the way Witchery made you a witch. Your tools are improvised from
  vanilla things: copper, glass, clocks, needles, amethyst, leather, lightning rods.
- **Grown, not built.** Machines are flesh grown over copper frames, fed on what they hunger for.
  They are never made of gears and plates.
- **Gloomy, but Minecraft.** Verdigris green, bruised purple, candlelight, cloudy glass, ticking,
  heartbeats, distant thunder. Gore stays at the vanilla rotten-flesh level.
- **The world reacts visibly.** Floating cows, shrunken zombies, frozen-minded villagers that
  just stand there. Every experiment leaves a weird mark you can see.
- **Discovery over instruction.** The journal fills in as you experiment and as you look into the
  past.

## The loop

```
 EXTRACT ──► GROW ──► FARM ──► EMPOWER ──► SYNTHESISE TIME ──► AWAKEN ──► RESEARCH & EXPLORE
 (syringe,   (feed a   (leech    (gland:    (Laws: racetrack,    (Chrono-    (Lens, ruins, clues,
  needles)    culture   tendrils, powers as  heavy heart)         cortex)     blueprints) ──┐
              into a    taps,     mana)                                                     │
              frame)    jugs)                                                               │
   ▲                                                                                        │
   └──────────── new needles, grafts, power levels, fields, locket ◄────────────────────────┘
```

## Hour 0 — The Spark
- The first time you pick up a **Clock**, an advancement says: *"Everything that lives carries
  one of these."* That leads to the **Journal** (Book + Clock + Ink Sac).
- Craft the **Chrono Syringe** (copper + glass) and the first needles: **iron** (Density),
  **gold** (Speed) and **bone** (Size). Craft **Essence Vials**.
- **First experiment:** syringe a cow with the iron needle. It lifts off the ground. Drain it again
  and it keeps rising. You learn on the spot that a **leash** holds it down, and the journal writes
  its first scrawled page. Shrinking a zombie with the bone needle is the second "oh no" moment.
- **Limits you find quickly:**
  - Syringes are tiny (4 drams).
  - Drained mobs **don't regenerate**.
  - **Awareness can't be taken** (*"it evaporated before I could stopper the vial"*).

  The journal concludes: *"I need something that can hold more. Something that wants it."*

## Hours 0.5–1.5 — The First Organism
- Make a **Density Culture**: vials of Density + rotten flesh + bone meal, set in a jar.
- Build a small **Copper Armature** frame in your basement, put the culture in it, and **feed**
  it. It hungers. It grows into the frame block by block, then starts storing the surplus.
- Graft a **Tap** onto it, and fill your first **Jug**.
- Graft a **Leech Tendril**. Now the organism drains **leashed cows** in a pen automatically.
  Your first farm is a field of floating, leashed cows.
- **The breeding treadmill:** drained cows stay drained, so you breed calves, raise them and drain
  the adults. This is deliberate early friction that Temporal Fields will fix later.
- Branching starts here: the player picks which aspect to grow next (Speed, Size, or Density
  first), and each one opens different powers and Laws.

## Hours 1.5–2.5 — Becoming the Experiment
- Graft the first **Enhancing Gland** and fill yourself up. **Level 1 powers:**
  - Density: **Iron Fists** (mine iron-tier blocks bare-handed, slowly)
  - Speed: +20% speed
  - Size: shrink or grow one step
- **Awareness Organism:** its culture needs a **brain** (from a zombie villager or a witch).
  Leech Tendrils drain Awareness straight from mobs. Fully drained mobs go **Vacant**, standing
  and breathing but doing nothing, which makes every other farm calmer. The power: **glowing
  sight** of mobs and non-sneaking players within 20 blocks.
- You can only refill **at the gland**. Every trip away from the lab is limited by your pools.
  Wanting to fix that is the main pull toward research.

## Hours 2.5–3.5 — Time Is Expensive
- The **amethyst needle** and **Stasis Ampoules** (phantom membrane) give tiny draws of Time from
  phantoms, endermen, undead and babies. Over-harvesting one area causes the first **Time Tear**:
  copper oxidises, stone crumbles, a dropped stack vanishes. A lesson in fear.
- You grow a **Time Organism** with Stasis Bladders, and learn to **wrap it in Density flesh** so
  it doesn't burst (*"Heavy things keep time still."*).
- **Law of the Road: the Racetrack.** This is the first real puzzle: a coiled vein track where
  slow Speed pulses on a long road condense into Time. You balance track length, vein upkeep,
  pulse strength and stall clogs, and fill the cellar with living intestine.
- **Temporal Exposure** starts to show: ticking, micro-rewinds. The **Chrono-Insulated Suit**
  (goggles, lab coat, gauntlets, galoshes) becomes necessary. The player now looks the part.

## Hours 3.5–4.5 — "It's Alive!"
- Reanimation ritual:
  1. Build a **Dissection Table** altar with a brain in a jar.
  2. Feed in Awareness and Time from your organisms.
  3. Put lightning rods on the frame and wait for a storm, or use a Channeling trident.
- Lightning strikes and the **Chronocortex** wakes up (stage 0). It whispers. Wire your organisms
  to it with **Ganglion** grafts, and it starts to show you their levels and hunger.
- **Research opens up.** The first topics, each with alternative clue paths:
  - **Umbilical Locket**: refill your pools away from the lab
  - **Temporal Lens**: look into the past
  - **Temporal Field**: animals regenerate, which ends the breeding treadmill
  - **Level 2 powers**

## Hours 4.5+ — Into the Past (bridge to mid-game)
- The **Temporal Lens** turns exploring into the research engine:
  - Ruins become **castles**, empty villages become **marketplaces**, dead labs light up.
  - **Faded blueprints** in abandoned labs can be read and scanned through the Lens.
  - Every observation is a **Clue** in the journal.
- Your first **Obelisk** bends the Lens view like a black hole, and the inscription at its event
  horizon is unlike anything else.
- The Chronocortex reads it and says: *"Show me the cold."* Stage 1 and the **Ice Age** are next.

## Early-game branch summary

| Choice | Options | Consequence |
|---|---|---|
| First aspect to grow | Density / Speed / Size | Which powers and Laws come first |
| Time source | Direct extraction / Racetrack / Heavy Heart | Risk (Tears) vs. puzzle-building vs. mass-hoarding |
| Sustainability | Breeding treadmill / rush Temporal Fields | Effort now vs. research cost |
| Research order | Locket / Lens / Field / powers, through different clue sets | Exploration-led or lab-led playstyle |

## Suggested build order (implementation)
1. Essence-pool attachment + essence profiles (data) + drain effects:
   - floating (Density)
   - scale (Size)
   - slowness (Speed)
   - AI throttling (Awareness)
   - ageing (Time)
2. Chrono Syringe with needle component, Vials, Jugs, the Journal (Patchouli).
3. Organism growth system: culture → armature → hunger and growth → storage. Then Tap, Maw and
   Leech Tendril grafts.
4. Enhancing Gland + player pools + Level 1 powers + radial keybind.
5. Time: Stasis Ampoule, chunk Temporal Stress, Time Tears, Stasis Bladder, Density stabilising.
6. Racetrack (Starting Gate, Finish, pulses), Temporal Exposure + Suit.
7. Chronocortex + Ganglion + research (data-driven) + reanimation ritual.
8. Umbilical Locket (Curios), Temporal Field, Temporal Lens + first structures.

## Open questions
1. **Brains for Awareness cultures and the Chronocortex:** zombie villagers and witches only, or
   villagers too (darker)?
2. Should **Temporal Exposure** be able to kill (from old age), or only weaken?
3. **Time Loop and mobs:** should loops respawn *wild* mobs that were there at snapshot time
   (more duplication), or never touch entities, as currently designed?
4. Should Level 1 powers stay cheap and fun, or cost enough that the player plans every trip?

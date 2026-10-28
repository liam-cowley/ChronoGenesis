# Early Game: "The First Experiment"

> Status: **design draft**. A walkthrough of the first ~4–5 hours. The systems themselves are
> defined in `core-systems.md`; this doc covers order, pacing and how the player feels.

## Mood pillars
- **You are a mad scientist,** the way Witchery made you a witch. Your tools are improvised from
  vanilla things: copper, glass, clocks, needles, amethyst, leather, lightning rods.
- **Grown, not built.** Machines are flesh grown over brass frames, fed on what they hunger for.
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
- Build a small **Brass Armature** frame in your basement, put the culture in it, and **feed**
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
  (goggles, lab coat, trousers, galoshes) becomes necessary. The player now looks the part.

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

## Build order (implementation status)
1. ✅ Essence-pool attachment + essence profiles (data map) + drain effects (float, shrink, slow,
   AI throttling, ageing/desiccation).
2. ✅ Chrono Syringe with needle component, Vials, Jugs (fill level visible), pouring between
   containers in the inventory, the Journal (Patchouli).
3. ✅ Organism growth (culture → armature → hunger/growth → storage → starvation/necrosis);
   Tap, Leech Tendril grafts. ✅ Maw graft, Brass Conduit and veins (grown by Taps).
4. ✅ Enhancing Gland + player pools + Level 1 powers (keybinds, HUD, radial menu: hold the power key).
5. ✅ Stasis Ampoule, chunk Temporal Stress, Time Tears (ageing, swallowing, sealing).
   ✅ Stasis Bladder, Time organisms (instability, bursting), Density stabilising.
6. ✅ Racetrack (Starting Gate, Finish, pace, stalls, heavy veins). ✅ Temporal Exposure. ✅ Chrono-Insulated Suit.
   ✅ Heart graft (Taps pump faster), Eye graft (redstone + comparator), Twin Clocks (Law of the Heavy Heart).
7. ✅ Chronocortex (stage 0) + Ganglion + data-driven research + reanimation ritual. Clues come from
   witnessed experiments until the Lens exists; the first topic is Level 2 powers (Deeper Infusion).
   ✅ Growth: lobes on armature (Awareness), lessons (data-driven), stages 1-3, the Cerebrum (5x5x5 multiblock),
   speech in chat (data-driven dialogue). Still to do: flow control from the Chronocortex (redirecting, Tap limits),
   time travel from stage 1.
8. ✅ Umbilical Locket (Curios necklace; grown by research, re-growable). ✅ Temporal Field (graft on a Time
   organism; one creature per 5 s gets a dram back, for a dram of Time). ✅ Temporal Lens (Echo/Decades/
   Centuries; ghosts of past templates; reads faded blueprints into clues). ✅ Abandoned Laboratory (worldgen,
   ruin + past template, faded blueprints, loot with rare intact blueprints). ✅ Obelisks (worldgen, unbreakable;
   through the Lens a black-hole distortion shader; past view swallowed by the horizon; two clues).
   Still to do: the other structures (castle, marketplace, observatory, drowned specimen house, intact lab),
   shrines, Echo visuals (recent events), player-placed blocks fading in the Lens, "Ages" (stage 1).

9. ✅ Powers rework: passive and active powers, the Essence Manipulator (wheel, strength), gland levels
   (cultures, Deeper/Deepest Infusion), the Confluence Needle and ten composite powers. ✅ Time 1-3: Rewind, Slow
   Time, Time Freeze (per-chunk tick rate, local sky, catching up, the redshift/black-column look).
   ✅ Every essence's organisms look different (flesh shapes and textures, core eyes).
10. ✅ Temporal shifts (bridge to mid-game): eras as data, the Eras page, the Chrono Tether, the Ice Age (a frozen
    copy of the overworld's land).
11. ✅ The Age of the Precursors (first era): its own biomes, Precursor cities and Precursors. ✅ Cortex Offshoots,
    Fossil Cortexes, Nerve Spools: memories of eras, each leading to the next. Next: the Jurassic age, the world's
    creation, the time before time (the Elder God); things to bring back from the eras; the Lens's "Ages" setting.

12. ✅ The Jurassic Age (third era, two routes in): Raptors, Titanosaurs, building on and riding harnessed
    Titanosaurs, sealed temples with five ability seals (any three). Research gains alternative clue sets from the
    Jurassic (Dino Harness; Deepest Infusion through a broken temple). Next: the world's creation, the time before
    time (the Elder God).
13. ✅ The suit as 3D models (cap and goggles, lab coat with a Time canister, trousers, galoshes). Time warding: a
    suit piece pressed into a swollen (level 2+) Time gland grows tendrils; warded head to foot, shifts hold the
    era's full minutes without nausea or exposure (half the minutes unwarded, and the era tugs at every unwarded
    piece).

## Open questions
1. **Brains for Awareness cultures and the Chronocortex:** zombie villagers and witches only, or
   villagers too (darker)?
2. Should **Temporal Exposure** be able to kill (from old age), or only weaken?
3. **Time Loop and mobs:** should loops respawn *wild* mobs that were there at snapshot time
   (more duplication), or never touch entities, as currently designed?
4. Should Level 1 powers stay cheap and fun, or cost enough that the player plans every trip?

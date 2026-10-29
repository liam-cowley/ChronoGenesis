# Core Systems

> Status: **design draft**. This doc is the source of truth for how essence, grown machines,
> powers, time manipulation, research and exploration work. `early-game.md` walks through the
> first hours using these systems. Numbers are placeholders for playtesting.

## 1. Essences: the attributes of a creature

Essences are not magic juice. They are a creature's **physical, almost philosophical attributes**,
pulled out of it. Draining one takes that attribute away from the mob, and you can see it happen.

| Essence | What it is | Colour | Mob drained of it… | Stability |
|---|---|---|---|---|
| **Density** | Weight, heaviness, how firmly it belongs to the ground | Leaden blue-grey | Starts **floating** a set height above the floor. Drain more and it **rises higher and higher**. Only a **leash** keeps it down. Fully drained, it drifts off into the sky | Stable: fits in a syringe and vials |
| **Speed** | How fast it can move and act | Sickly yellow-green | Slows down, and in the end is **rooted** in place | Stable |
| **Size** | How much space it takes up | Rust ochre | **Shrinks** (uses the vanilla `scale` attribute) | Stable |
| **Awareness** | Consciousness and perception | Pale violet | Its **AI updates less and less often**: it reacts late, then stops. Fully drained, it is **Vacant**: it breathes but does not tick | **Volatile**: evaporates out of glass in seconds, so it can only be taken with grown machines |
| **Time** | The creature's own time: its age and its future | Teal with amber sparks | **Ages**: babies grow up instantly, adults wither. Fully drained, it **desiccates** into dust and Temporal Residue | **Unstable**: ordinary glass ages to sand on contact. Needs special containers and dangerous machines |

Using aspects together is part of the fun. Drain a cow's **Awareness** first and it stops
reacting, which makes draining everything else easy.

### Where essence comes from
- Every mob has an **essence profile**: how much of each aspect it holds. Profiles are
  **data-driven** (`data/<ns>/chronogenesis/essence_profile/<entity>.json`). Mobs without a file
  get defaults derived from their attributes:
  - Density from max health and armor
  - Speed from the movement-speed attribute
  - Size from hitbox volume
  - Awareness from the follow-range attribute
- **Time is special.** Early on, only a few mobs give any:
  - **Phantoms**: born from sleeplessness, the most "temporal" vanilla mob
  - **Endermen**: they slip through space
  - **Undead**: they live on borrowed time
  - **Baby animals**: they carry their future, so taking it makes them adults

  Even these give only trickles. A tag `#chronogenesis:time_bearers` controls the list.
- **Drained essence does not come back by itself.** A cow drained of Density stays a balloon.
  Before **Temporal Fields** (section 6), sustainable farming means **breeding**: calves are born
  full, you raise them, drain the adults, and repeat.

### Units and containers
- The unit is the **dram** (an old apothecary measure, which suits the mad-scientist feel).
- **Chrono Syringe**: holds 4 drams of one stable aspect. It takes **swappable needles**, and each
  needle draws one aspect:

  | Needle | Aspect | Why (in-world logic) |
  |---|---|---|
  | Iron needle | Density | Iron is heavy |
  | Gold needle | Speed | Gold tools are the fastest in Minecraft |
  | Bone needle | Size | Bone is the frame that gives a body its size |
  | Amethyst needle | Time (in tiny amounts, straight into a Stasis Ampoule) | Amethyst resonates; it is the mod's time crystal |
  | *(none early)* | Awareness | Volatile; only grown Leech Tendrils can take it |

- **Essence Vial** (8 drams), **Essence Jug** (64 drams, can be placed as a block, and shows how
  full it is). Neither can hold Awareness or Time.
- **Stasis Ampoule**: a vial lined with phantom membrane. It holds a few drams of Time, and leaks
  if left in the inventory for too long.

## 2. Grown machines (Organisms)

**Machines are grown, not built.** Every machine is an **Organism**: living tissue that grows over
a frame you build, fed on exactly the aspect you want it to deal with.

### Growing one
1. Make a **Culture** of an aspect (for example, a *Density Culture*) and place it inside a
   **frame** of Brass Armature blocks. The shape of the frame is up to you.
2. **Feed** it that aspect. The culture now **hungers** for more of it.
3. Essence is **converted into growth**. Each new block of flesh costs drams, and the organism
   grows outward into the armature, one block at a time.
4. When the frame is **full**, extra essence is **stored** instead, up to a capacity that scales
   with the organism's size.
5. The organism keeps **hungering**. If you stop feeding it, it first burns stored essence, then
   eats its own flesh (shrinking back), and finally turns **Necrotic** and has to be regrown.

A bigger frame means a bigger organism, with more storage and faster flow. That makes the shape of
the frame a real design decision.

### Shapes (as built)
Each essence's organisms must be grown into **their own shape** to work (`organism/OrganismShape`, data:
`data/<ns>/chronogenesis/organism_shape/<name>.json`). A seedling (under 5 blocks, core included) works whatever its
shape; past that, an organism out of shape is **malformed** (`formed` blockstate false): it lives, eats and grows, but
nothing can draw on it (Taps, Glands, the Chronocortex, lockets get nothing; the cortex does not count its essence).
Its status line says what its shape needs.

| Essence | Shape | Rule (default) |
|---|---|---|
| Density | compact | at least 0.6 of its bounding box filled, no side more than twice another |
| Speed | elongated | its longest side at least 3 times the next |
| Size | broad | both widths at least twice its height |
| Awareness | branching | at least 3 tips (blocks with one neighbour): dendrites |
| Time | spire | at least twice as tall as it is wide: grown like a Nail |

### Looks (as built)
Machines move. Their living parts are drawn animated by block entity renderers from shared parts
(`client/OrganParts`, sheet `textures/entity/organ.png`): flesh, eyes, iron straps, tendons, amethyst shards, sacs,
brain folds, teeth, brass, tentacle segments, tinted by essence.
- **Organism cores** (`client/OrganismCoreRenderer`; the block model is only the frame): a mass that beats, lumps
  swelling out of step, an eye that turns to whoever is near and blinks; Density strapped in iron, Speed drawn taut
  on thrumming tendons to the frame's corners, Size with sacs swelling and sagging, Awareness folded like a brain
  with eyes on stalks, Time circled by amethyst shards with a brass hand going round. A seed is a small bud; hungry
  it shrinks and its heart races; necrotic it is grey, slumped and stitched shut; malformed it twitches, flushed.
- **Grafts** (`client/GraftRenderer`): the Tap's plunger works, the Maw's teeth open and close, the Leech Tendril
  writhes (gulping while it drinks), glands throb and bladders swell, the Heart beats, the Eye follows you, the
  Clock's hands go round, the Ganglion's nerves twitch, the Temporal Field's rings of shards turn.
- **The Chronocortex** (`client/CortexBrainRenderer`): see section 5.

### Grafts
You **graft** flesh parts onto an organism's surface, and they decide what it does. Grafts are
made from harvested mob parts and flesh. They are data-driven, so new ones can be added through
datapacks.

| Graft | Made from (sketch) | Effect |
|---|---|---|
| **Tap** (output) | Flesh + glass + copper | Opens up the stored essence: fill vials or jugs from it by hand, or let a neighbouring organism pull from it |
| **Maw** (input) | Flesh + bone | Takes essence from vials and jugs, or from a neighbouring organism's Tap |
| **Leech Tendril** | Flesh + spider eye + matching needle | Drains its aspect from mobs within range. **This is how farms work** |
| **Vein** | Grown along Brass Conduit frames | Carries essence between organisms. The living answer to pipes |
| **Heart** | Animal heart specimen (cows, pigs, sheep) | Pumps: each Heart adds the base rate to the organism's Taps, up to three |
| **Eye** | Spider or ender eye | Senses fill level and nearby creatures within 4 blocks (comparator and redstone output) |
| **Enhancing Gland** | Flesh + aspect culture + amethyst | Infuses the **player** with the organism's essence (section 3) |
| **Ganglion** | Brain specimen | Connects the organism to the Chronocortex for monitoring and control (section 5) |
| **Stasis Bladder** | Phantom membrane + flesh | Required to keep Time essence (section 4) |

**Example farm.** A Density Organism grows inside a pen of leashed cows. Leech Tendrils pull them
off the ground one by one, and the drams flow through a Vein to a big storage organism with a Tap.
The result is a field of floating, leashed cows, which is exactly the right image for the mod.

## 3. Player powers

### Filling up
The **Enhancing Gland** graft fills the player with that organism's aspect. This is the player's
**mana**: one pool per aspect (a player data attachment). How big each pool is depends on the
player's power level for that aspect.

**There is no regeneration away from a gland** until the player researches the **Umbilical
Locket**. It is a **bauble** (Curios slot) that you link to an organism with a Tap, and it
refills your pools from that organism remotely while you are within range. Range grows with
research. It is a literal umbilical cord to your lab.

### Level 1 powers
Each power is toggled or activated through a keybind (a radial menu to pick one) and drains its
pool while in use.

| Aspect | Level 1 power | Scaling at higher levels |
|---|---|---|
| **Density** | **Iron Fists**: bare hands mine anything an iron pickaxe can (with normal drops), but at stone-pickaxe speed. Drains per block broken | Diamond- then netherite-tier fists. Faster. Knockback immunity |
| **Awareness** | Mobs, and players who are not sneaking, **glow** within 20 blocks | Bigger radius, see through sneaking, see what mobs are targeting |
| **Speed** | +20% movement and attack speed | Bigger bonus, faster mining, step-up |
| **Size** | Shrink (fit through 1-block gaps) or grow (more reach and health) by a small step | Bigger steps |
| **Time** | **Stutter**: a 3-block bubble where everything moves at half speed for 3 s (projectiles too) | Area slow, area haste, time-stopped chunk, Time Loop (section 6) |

### As built: powers, the Manipulator, gland levels, composites
- **Featherweight** (passive, Density level 2): the body made light. Blows land a third harder per strength (fall
  damage excepted), but gravity is roughly halved, jumps go several blocks high, landings hurt half as much, and a
  second jump in mid-air is possible (once until you land; the client leaps and tells the server, a dram of Density).
- **Passive and active.** Every power is one entry of `player/Power`. Passive powers (Iron Fists, Glowing Sight,
  Quickening, Shrink, Grow, and the passive composites) are switched on and burn their pools slowly; several can
  run at once. One active power is selected and used: Rewind (Time 1), Slow Time (Time 2), Time Freeze (Time 3),
  and the active composites.
- **Essence Manipulator** (a glove; tendrils move in its lining, animated texture): right-click uses the selected
  active power; **Shift + R** opens the wheel while Shift is held (active powers on the outer ring: point or scroll,
  let go of Shift to choose; passive powers on the inner ring: click to switch; right-click any power: strength);
  **Shift + scroll** with the glove in hand sets the selected power's strength. R alone uses the selected power.
  A hand in the glove is still a fist for Iron Fists.
- **Strength** goes from 1 to the lowest power level of the power's essences (at most 3) and scales effect and cost.
- **Gland levels.** The Enhancing Gland has a `gland_level` (1-3). Fed a culture of its own essence it swells a
  level, paying 32 x level drams from its organism, once Deeper Infusion (level 2) or Deepest Infusion (level 3,
  Chronocortex stage 2) is researched. Infusing raises the player's power level for the essence to the gland's.
- **Composite powers.** A **Confluence Needle** (research: Confluence, Chronocortex stage 1) fitted into a gland
  (`needle` blockstate) makes it draw on a second organism of another essence wired to its organism by veins (a
  Tap or Maw of one, veins, a Tap or Maw of the other). If the player already holds both essences they are filled
  with both and learn the pair's composite power:

  | Pair | Power | Kind | Effect |
  |---|---|---|---|
  | Density + Speed | Impetus | active | A charge; what you hit is hurt and thrown aside (p = m v) |
  | Density + Size | Ground Slam | active | Drop like a stone; a shockwave where you land (m = rho V) |
  | Density + Awareness | Dread | passive | Hostile creatures hunting you are slowed and weakened |
  | Density + Time | Dilation | passive | Projectiles near you crawl ("heavy things keep time still") |
  | Speed + Size | Scurry | passive | Step up whole blocks, safe from longer falls |
  | Speed + Awareness | Reflexes | passive | Projectiles often glance off (a dram of Speed each) |
  | Speed + Time | Stutter | active | A bubble of half-speed time, projectiles too (was the old Time level 1 power) |
  | Size + Awareness | Overlook | active | Invisible; creatures hunting you lose your trail |
  | Size + Time | Regrowth | active | Heals and clears harmful effects: your body's time runs back |
  | Awareness + Time | Premonition | passive | The next blow from a creature is foreseen and misses; then a cooldown |

- **Eldritch powers** (power level 4, only from a gland with the Unbegotten's pump, `player/EldritchPowers`): one per
  essence. **Collapse** (Density, active: a point of crushing weight where you look; creatures fall in, then are
  crushed), **Slip** (Speed, active: step between moments to where you look), **Writhe** (Size, active: tentacles burst
  from the ground round you, striking and flinging), **Thousand Eyes** (Awareness, passive: everything living within
  48 blocks glows through walls; nothing blinds you), **Aeon** (Time, active: a cone of ages; creatures wither, slow
  and weaken, the undead turn to dust, plants grow).
- **Slow Time and Time Freeze rest a minute** once let go (or torn away): `TimeZones.COOLDOWN`.

### Temporal Exposure and protective gear
Time essence inside a body is unnatural. Holding Time essence, using Time powers, or working near
Time organisms builds **Temporal Exposure** (a player attachment):
- **Low:** a ticking sound, and the screen edge flickers on each tick.
- **Medium:** random **micro-rewinds** (you snap back 2 seconds), and you get hungry faster.
- **High:** you **age**: max health drops for a while, your hands tremble (slower attacks), and
  your hair greys (cosmetic).

The **Chrono-Insulated Suit** keeps the player "normal". It has four pieces: smoked-glass
**Goggles**, a membrane-lined **Lab Coat**, **Trousers** and **Galoshes** (gauntlets became trousers: Minecraft has no hand slot), made from leather,
copper and phantom membrane. Each piece cuts exposure. Wearing the full suit is required for
higher Time powers and for working safely with Time machines. The Goggles also protect your eyes
while using the Temporal Lens.

**As built:** each piece is worn as its own 3D model (`client/SuitModel`): a leather aviator cap with brass-rimmed
smoked goggles, a temple canister and a copper aerial; a lab coat with tails, brass pauldrons and cuffs, a Time
canister on the back, a chest gauge and a vial in the pocket; belted trousers with copper knee pads and a pouch;
rubber galoshes with brass toe caps. **Time warding:** a piece pressed into an Enhancing Gland of Time, swollen to
level 2 or more, is infused for 48 drams (`temporal_ward` component). Warded pieces grow writhing tendrils from
their seams (like the Essence Manipulator) and the coat's canister glows. Warding is what makes time travel work
properly: a traveller warded head to foot shifts cleanly and stays the era's full minutes; anyone else is wrenched
through (exposure, nausea), the shift holds half the minutes (an eighth more per warded piece), and the era pulls
an exposure from every unwarded piece every 5 seconds.

## 4. Time essence: scarce, dangerous, central

### Direct extraction (small and risky)
Use an amethyst needle on time-bearing mobs, straight into a Stasis Ampoule. You get tiny amounts.
Every time you extract Time, **Temporal Stress** in that chunk goes up (a chunk attachment that
slowly fades). Too much stress causes **Time Tears**:
- Rifts that rewind or age nearby blocks (copper oxidises, stone crumbles to gravel, wood rots).
- They swallow dropped items.
- They sometimes spit out a **displaced creature from another era**. This is foreshadowing for
  time travel.
- You can seal a tear with dense flesh (Density) or study it. Late in the game, harvesting tears
  becomes a Time source in its own right.

### Time organisms (unstable storage)
Time can only be stored in an organism with **Stasis Bladder** grafts. Time organisms are
**unstable**. If they are damaged, starved, or close to capacity, they **burst**: a local Time
Tear plus an ageing shockwave.

**Stabilising one is the first physics puzzle.** Heavy mass slows time (gravitational time
dilation), so a Time organism **wrapped in Density flesh** is much more stable. In the notebook:
*"Heavy things keep time still."*

### Synthesis: combining aspects with the "Laws"
The reliable way to get Time early is to **combine other aspects using real physical laws**. The
notebook writes these as **Laws**: plain, almost proverb-like sentences about combining things,
never equations to solve. The real formula sits beside each one in small print, for players who
like that.

#### Law of the Road: Time from Speed and Distance
> *"A traveller's time is the length of its road, divided by its haste. A long road walked slowly
> leaves the most time behind."*
> (small print: t = d / v)

**The Racetrack** is the core early puzzle:
1. Grow a **Vein** track between a **Starting Gate** graft and a **Finish** graft.
2. The gate sends out **pulses** of Speed essence. Each pulse runs along the track. At the finish,
   the time it spent on the way condenses into Time essence.
3. **Time per pulse ∝ track length ÷ pulse speed.** The player wants a **long** road and a
   **slow** traveller.
4. There are two catches, and they are what make it a puzzle:
   - **Veins feed on what passes through them**, so every block of track costs a bit of Speed.
     Long tracks are expensive.
   - **A pulse that is too slow stalls** and rots inside the vein. That is a clog you have to cut
     out with a scalpel.
5. Ways to tune it:
   - **Coil** the track in 3D to fit a lot of distance into a small lab. The frame shape matters
     again.
   - Graft **Dense segments** (Density-fed vein blocks) to slow pulses down in chosen places
     ("the road gets heavy").
   - Set the gate's pulse strength.
   - Space pulses out so they don't catch up with each other.

A satisfying Racetrack is a living coiled intestine of veins filling a room. It looks properly
mad.

#### Law of the Heavy Heart: Time from Density (mid early game)
> *"A heart buried in heavy flesh beats slower than one standing free. Count the beats it misses:
> they are yours."*
> (small print: gravitational time dilation)

**The Twin Clocks**: grow two **Clock** grafts (made from a vanilla clock + flesh). Bury one deep
inside a massive Density organism and keep the other outside, linked by a vein. Every tick, the
**difference** between them turns into Time. More mass around the buried clock means more Time.
The puzzle is packing mass around the buried clock as densely as possible.
As built: the free clock sits on a **Time organism**, which catches the Time. Every 2 seconds it adds
up how much more Density flesh lies within 2 blocks of the buried clock than of itself; 600 of these
missed beats make a dram.

#### Other Laws (other uses, and later conversions)
| Law (notebook wording) | Real formula | Makes |
|---|---|---|
| *"Weight is heaviness spread over size."* | m = ρ · V | **Weight** (Density + Size): gravity effects, Heavy Heart upgrades |
| *"A heavy thing moving fast is hard to stop."* | p = m · v | **Impetus** (Weight + Speed): charge and knockback powers, breaking through blocks |
| *"A swing is slow when the rope is long and the pull is weak."* | T = 2π √(L/g) | **The Pendulum**: a later Time generator. A long hanging tendril where *low* Density means slower swings |

Laws are **discovered**. Each one starts as a page with blanks, and the research that unlocks it
fills in the wording.

## 5. The Chronocortex (the bio computer)

The notebook's technical name for it is the **Temporal Control Unit (T.C.U.)**. It is grown from a
brain specimen, Awareness and Time, and **woken by lightning** in a reanimation ritual
(*"It's alive!"*). It grows through stages, and each stage reaches further back in time, ending in
its madness.

Its roles:
1. **Essence interface.** Every organism wired to it with a **Ganglion** graft appears in its
   view, showing levels, hunger and stability. From there you can redirect flow, set Tap limits,
   and get warnings before a Time organism bursts.
2. **Research.** Research topics are data-driven, and each costs:
   - essence
   - materials
   - **Clues** from the journal (past observations, scanned blueprints, obelisk readings)

   Most topics accept **several alternative clue sets**, so the research tree branches: you can
   unlock the Lens or the Suit through different discoveries in different orders.
   **Its look (as built, `client/CortexBrainRenderer`):** not a jar and a cube of lobes but a living brain. Folds of
   brain rise out of the jar, more every stage; its grown lobes are not drawn as blocks but as swelling masses of
   folds where they stand (the Cerebrum's eyes open in them, watching). Tendrils reach out of it, more for every
   stage and every thing it knows, longer as it grows. Its knowledge (`knowledge()`: lessons, eras remembered twice
   over, creatures learned; synced to clients) makes it stranger: its flesh goes from grey-pink to bruise violet to
   an eldritch green, eyes open in a ring round it, its folds stop keeping to a brain's shape, a crown of shards
   turns over it (10+), and at the last a maw opens in it (16+).
3. **Temporal shifts.** Time travel, from stage 1 onward.
   **As built:** eras are data (`data/<ns>/chronogenesis/era/<name>.json`: dimension, stage, lesson, Time cost,
   minutes, clue). Its **Eras** page sends a player within 8 blocks (`timetravel/TemporalShift`): it needs the era's
   stage, lesson and remembered era, the Time from wired organisms, and a **Chrono Tether** (research: Temporal Shift). They arrive at
   the same x/z on the ground of the era's dimension (same seed, so the land lines up) and are snapped back where
   they left when the minutes run out; using the tether returns them early; dying ends it. Warded head to foot (the
   suit infused with Time), the era holds them for as long as they like (`TemporalShift.HELD`; the HUD says so):
   only the tether, or taking a piece off, starts it letting go.
   **Eras, in the order they are discovered:** *The Age of the Precursors* (`chronogenesis:precursor_era`), *The Ice Age*
   (`chronogenesis:ice_age`), *The Jurassic Age*, *The Hadean Eon* (the world's making) and *Before Time*, where the
   Elder God waits. Every era stood in is remembered (`visited_eras` attachment) and becomes a Temporal Lens setting,
   except Before Time (era `lens: false`: there is nothing there to see).
   - *The Age of the Precursors:* the overworld's terrain and seed with four golden-age biomes of its own (meadow,
     jungle, desert, coast: warm sky, teal water, no monsters). The Precursors never knew the obelisks for the Nails
     that hold time, nor that time could be travelled: they knew that near the black stones nothing went missing, and
     that elsewhere things were being **unmade** (never there at all). Every kingdom kept its own watch against it.
     **Their cities stand exactly where the present has their remains** (`world/RemainsStructure`,
     `chronogenesis:remains`): each city's structure set copies a vanilla structure's placement, and asks the
     overworld's own generator whether that structure really stands at the chunk (biome, terrain, the monument's
     sea), and where. Each kingdom is built its own way by `tools/precursor_cities.py` (about 160 blocks across),
     its people dressed its own way (`textures/entity/precursor_<kingdom>.png`) and talking of their own watch:
     - the **Sun Court** (desert temples): the City of No Shadows. A walled square of flat white roofs on a grid, a
       maze of alleys in the common quarter, courtyard villas with pools, the Processional Way of gold-tipped pylons,
       a great market round a sundial, the Well of Noon, a necropolis of mastabas with sealed chambers, and the
       *Court of Noon*: a step pyramid with a pillared throne hall, a treasury, and under it the tomb they sealed
       their king in when the shadow took his face (fire-powder under the floor: the present's desert temple trap).
       Dress: white linen, a gold collar, a striped headcloth.
     - the **Tide Kingdom** (ocean ruins: towns; ocean monuments: the capital): rings of stilted decks on the open
       sea (piers carried to the sea floor), bridges, lighthouses, quartz houses with stepped prismarine roofs, the
       *Watch Pools* where their **Guardians** came up to be fed, and the *Tidehall*: a rotunda over the *Eye Pool*,
       with a glass stair down to the *Sanctum of the Open Eye* under the sea. Dress: sea-green scales, a shell sash.
     - the **Deep Kingdom** (ancient cities): the ancient city itself, **restored** as it was (the same jigsaw, the same
       seed, so the same streets; `world/RestoredPiece` mends cracked stone, clears the sculk's overgrowth and the
       dead, lights candles, stops shriekers calling), its folk and its blind **Wardens** back in its streets.
       Dress: a charcoal hood, a soul-blue band over the eyes.
     - the **Hearth** (villages): timber-framed cottages along crooked lanes, front gardens and cellars, smithies,
       windmills, fields, a low field-stone wall, and the *Anvil Hall*: a great hall on its green with the hearth that
       never went out, the high seat, the golems' forge pit, the armoury and vault below. Its **iron golems** are kept
       bright (`entity/PrecursorGolem`: no vines, no rust, no cracks). Dress: russet wool, a leather apron, a red cap.
     - the **Verdant Kingdom** (jungle temples): houses up in great trees round spiral stairs, rope bridges between
       them, stilt huts, terraced gardens, and the *Garden of Engines*: a glass dome on copper ribs full of living
       machines grown to remember for them, the *Remembering Engine* at its heart, a gallery round it. Dress: woven
       green, copper armbands, a crown of flowers.
     Every city has an archive (its kingdom's mural, shelves, chests of tablets), a statue of its king, stalls,
     houses with chests (`precursor_house_<k>`) and a vault in its great building (`precursor_vault_<k>`).
   - **Guards** (`entity/PrecursorGuards`): the Wardens, Guardians and golems (tag `chronogenesis.guard`, or a
     Precursor Golem) are friendly to whoever keeps the peace: they will not hunt, sense, roar at or darken a player
     (`mixin/WardenMixin`; target changes cancelled). Hurt a guard or a Precursor and the guards hold a **grudge**
     (`precursor_grudge`, two minutes): every guard within 48 blocks turns on you.
   - **Shrines** (`precursor_shrine`): the obelisks stand in the Precursor era too, on exactly the same spots (the
     obelisks' placement), each in a small temple of the kingdom of its land (`precursor_shrine_<k>`: a sun court
     open to the sky, a round deck of quartz columns on the sea, a timber lych-house, a ring of grown arches).
   - **The present's remains** carry their kingdoms' tablets: desert and jungle temples, ocean ruins (their suspicious
     sand and gravel too), ancient cities, and village chests (the Hearth's).
   - **What they watched for** (`lore/PrecursorLore`): twenty fragments on **Precursor Tablets**, readable once their
     tongue is understood (linking the Precursor era's memory); reading one records it (`read_lore`) and opens its
     journal page. The Nails (`nails_*`), each kingdom's watch and how it failed (`sun_*`, `tide_*`, `deep_*`,
     `verdant_*`, `hearth_*`: the Hearth's children became the villagers, and kept the golems), and two older
     tablets in the temples (`temple_1`, the Jurassic; `temple_2`, frozen in the Ice Age). The last tablet of every
     kingdom and the Nails' third make it fall into place: *The Unmaking* (`the_slip`).
   - *The Ice Age:* the overworld's noise and seed with four frozen biomes of its own (`ice_age_spikes`, `_slopes`,
     `_peaks`, `_grove`). Needs the lesson of the cold and the memory of the Precursors. The obelisks stand whole
     there, in **what is left of the Jurassic temples** (`ice_temple_ruin`, the obelisks' placement): broken tiers,
     rooms half full of snow and ice, a raptor's bones, and where the core hung, the moment it died frozen round it:
     **Aeon Ice**, time stopped still. **Why go:** Aeon Ice never melts, and every block of it touching a Time
     organism steadies it by 2 (a Stasis Bladder is 3); the oldest eras cost more Time than an unsteadied organism can
     hold. And its creatures: the **Mammoth** (herds; the richest Density and Size there is; hurt one and the herd
     turns on you), the **Woolly Rhinoceros** (charges, throwing what it hits) and the **Smilodon** (a sabre-toothed
     cat that stalks anyone walking upright). It is also one way into the Jurassic.
   - *The Jurassic Age* (`chronogenesis:jurassic_age`, stage 2): four hot, wet biomes (fernlands, jungle, swamp,
     coast). Two routes in (eras can list alternative `routes`): the Ice Age's memory, or the Precursors' memory plus
     the lesson **Deep Time** (a bone block). **Raptors** (hostile, leaping, in packs) and **Titanosaurs** (huge,
     peaceful unless hurt).
     - **Building on a Titanosaur** (`entity/DinoCargo`), blocks fixed to its body (the way Valkyrien Skies or Create
       Aeronautics carry a ship): a **Dino Harness** (research, needs a Jurassic clue) used on a grown one gives it a
       **hold** (`entity/CargoHold`: a plot of its own level near the world border, high in the sky, force-loaded while
       the beast is) with the harness in the middle. The cargo's real blocks stand there (block entities tick, chests
       keep their contents, machines run); the beast mirrors them on its back, centred over it and turning with it at
       any angle, never squared to the grid (synced as a palette, drawn by its renderer). The client finds the block
       under the cursor in the beast's frame (`client/CargoControls`: outline, cracks while breaking) and the server
       applies the click to the real block in the hold (`CargoActionPayload`, the player turned into the cargo's frame
       so facings come out right): build against it, break it, open it, use it, as normal. Reach to a hold block is
       measured on the beast (`mixin/PlayerMixin`), and anything dropped in a hold falls from the beast. Up to 384
       blocks, 12 out from the harness, 6 down and 16 up. Solid through part entities (`entity/CargoPart`, boxes of a
       few blocks: you can stand on it and are carried along); it cannot pass through terrain (the beast stops). A
       **Rider's Seat** in it steers. If the beast dies or the harness breaks, every block drops. Older saves' cargo
       (anchored or lifted) is moved into a hold on loading.
     - **Fossils and eggs**: draw the last of a dinosaur's Time out of it in the Jurassic (raptors hold 12 drams,
       titanosaurs 40) and it leaves a **Fossil** of its kind (`dino` component). Shown to the Chronocortex, it learns
       the creature and buds a **Dinosaur Egg** from its own matter (16 drams of Size from its organisms); a hen's egg
       shown to it buds another of a creature it knows (each in turn). Placed and fed 16 drams of Time (any container
       holding it), the egg cracks and hatches a young one that remembers who hatched it (raptors never hunt their
       hatcher). It grows up in a day (`entity/Dinos`), faster for its food (leaves for plant-eaters, meat for
       meat-eaters: 1800 ticks) or Time (600 ticks a dram). A calf cannot be harnessed.
     - **Sealed temples** (`jurassic_temple`, a 35x35 five-tier ziggurat) are built **round the obelisks**: the temple
       shares the obelisks' placement (`placed_template`, centred, unrotated), so wherever an obelisk stands in the
       present a temple stands round it in the Jurassic, the obelisk's shaft rising through its core chamber and out of
       its summit. The **Temporal Core** hangs beside the shaft, tied to it by **Core Sinews**; it holds a
       barrier (18 blocks around it, `temple/TempleBarrier`): no placing, breaking, pouring or explosions inside. Five
       **seals**, each needing a different power: Heavy (Iron Fists, or a Ground Slam), Narrow (at the end of a
       one-high crawlspace; only turned while Shrunk), Swift (run into it faster than a sprint: Quickening), Veiled
       (looks like the wall; Glowing Sight shows it, and is needed to open it), Stilled (only touchable in slowed or
       stopped time). **Any three** still the core: the barrier falls, the clue *A temple's barrier, broken* is noted,
       and the core can be taken as a **Stilled Core** (fills the Time pool). Raptors guard it; a chest holds loot.
   - *The Hadean Eon* (`chronogenesis:hadean_eon`, stage 3, needs the Jurassic's memory): the overworld's land before
     it cooled (`noise_settings/hadean`: blackstone, lava seas, basalt and magma, deltas, ash). Nothing lives, but the
     obelisks already stand. Its air is **toxic** (era `toxic_air`): poison and nausea unless Time-warded goggles filter it.
   - *Before Time* (`chronogenesis:before_time`, stage 3, needs the Hadean's memory): a void under an eternal night. An
     arriving traveller is put on the nearest **arena** (`timetravel/ElderArena`, made where they arrive if there is
     none within 160 blocks): a disc of unmade stone with a dais in its middle and the **Cradle Stone** on it (green
     runes, channels of crying obsidian running out from it), four causeways to four outcrops, and on each a pillar
     topped by an **Unmade Nail**. It is **empty**: nothing waits there. Each arena records its state (empty, waking,
     fighting) and its body, so arriving never makes a second boss.
     - **The Last Offshoot.** After linking the Hadean's memory the Chronocortex asks to be taken there; research
       grows a **Last Offshoot** (stage 3), which takes root only in the Cradle Stone. A **Nerve Spool** drawn through
       a **level 3 Enhancing Gland** (32 drams) comes out **threaded** (`spool_threaded`): used on the Chronocortex its
       loose end roots in the cortex (`spool_cortex`, its name); laid into the planted offshoot, the offshoot *is* the
       cortex, there, with its name. Used, it wakes (`eldritch/LastOffshootBlockEntity`, 26 s): it feels everything,
       drinks the time out of the four nails in streams of motes, and grows (`client/LastOffshootRenderer`: swelling
       folds, pink to violet to green, tendrils, a ring of eyes, a maw), saying it will not bow, until it bursts out of
       its jar.
     - **The Unbegotten** (`entity/Unbegotten`, the body, 900 health, named "<cortex name>, Unbegotten") rises past the
       north rim, a raid boss of separate parts round the arena's edge: four **tentacles** from the void round the disc
       and two **eyes** on stalks by the south outcrop (`entity/UnbegottenLimb`, 160 and 110 health, each killed on its
       own; a second boss bar shows them). Every living tentacle shields the body from 15% of every wound. Area
       attacks are marked on the floor first (`entity/UnbegottenBlast`: violet rings that go green, then burst).
       Phases: (1) *Unborn*: its wounds unhappen (every 5 s it is as whole as 5 s before) unless held in slowed or
       stopped time, but it **tears any hold on time lasting over 5 s** near it; Ripples of Unmaking (the outer ring and
       the outcrops, then the middle) sweep the arena; tentacles **slam across a causeway** to keep a player from the
       nail they are nearest, lying there 15 s as a wall of **Unbegotten Flesh** (unbreakable; hacking at it wounds the
       tentacle, which takes double harm while it lies there), or slam where a player stands; they **sweep** anyone
       close to their root towards the edge; eyes charge **gazes** (break line of sight behind a pillar). Driving the
       four nails again (24 drams of Time each) begins time: (2) its wounds stay; it sheds vexes; dead limbs regrow
       (sooner than in phase 1). (3) Below 35% its **maw** opens: it **inhales** (dragging everyone towards it), **stars
       fall** (marked discs, one under every player), eyes **stare** (a disc under someone); limbs no longer regrow.
       While it fights nothing can be placed, broken or poured on the arena (whatever was built there is torn out and
       dropped when it rises, and the arena mends), the Chrono Tether goes slack and the shift does not count down.
       Slain, it leaves the **Heart of the Unbegotten**, two of four **grafts for the body**, the clue *Its heart, still
       beating*, and the arena is empty again (another offshoot can be woken there).
     - **The heart** given to the Chronocortex (research *The Unbegotten Pump*, needs that clue) is made a **pump**:
       grafted onto a level 3 Enhancing Gland (`pump` blockstate, a heart strapped to its side, beating) it fills the
       player to **level 4**, where the **Eldritch power** of the gland's essence wakes (section 3).
     - **Grafts for the body** (`player/PlayerGrafts`, attachment `player_grafts`, kept through death, drawn on the
       player by `client/PlayerGraftLayer` and synced to everyone who sees them): the **Unblinking Eye** (a third eye in
       the forehead: night vision, never blinded), **Dorsal Tendrils** (from the spine: climb walls, sneak to cling),
       the **Grasping Tentacle** (round the right arm: +2 reach), **Unbegotten Hide** (plates over chest and shoulders:
       +5 armour, +2 toughness). Held to the body (use, 3 s) a graft takes, painfully. Only an empty **Dissection Table**
       (sneak-use it, empty-handed) cuts one out: 16 drams of Time from the player's own pool run the body back to
       before the last graft, which comes away whole.
     The first nails ever driven are these: the obelisks of every later age.
   **Remnants:** blocks that outlast the ages (the `chronogenesis:remnant` block data map: stone, deepslate,
   sandstone, prismarine, bricks, terracotta, concrete, gold, copper, amethyst, obsidian...) placed in an era remain:
   they are recorded (`timetravel/Remnants`) and, as soon as the spot is loaded, stand at the same place in the
   present and in the Temporal Lens's past, crumbled (stone bricks cracked, cobblestone and stone-brick stairs mossy,
   cut sandstone worn plain, prismarine bricks back to prismarine, copper oxidized; gold and amethyst unchanged),
   keeping their shape (facing, half). So the Lens shows them too. Breaking one again in its era before leaving
   undoes it. Wood, glass, wool and the like do not last.
   **Offshoots and memories:** research grows a **Cortex Offshoot**, which can only be planted in an era. It is a
   remnant: having ages to grow, it waits at the same spot in the present (and in the Lens's past) as a **Fossil
   Cortex**. A **Nerve Spool** bound to the fossil and used on the Chronocortex (any
   distance) gives it the era as a **memory**: +3 awareness, the next era (eras can require a memory), dialogue that
   needs it, and the linking player understands that era's people.

It talks. At first it whispers in chat and in the journal, curious and childlike. As it grows it
becomes demanding, and by the final stage it is insane.

**As built (growth, lessons, stages, speech):**
- *Lobes:* it grows `cortex_lobe` blocks into Brass Armature touching it (within 4 blocks of the jar), one every
  5 s for 4 drams of Awareness from its wired organisms. Lobes are its capacity. Using a lobe is using the cortex.
- *Lessons* (`data/<ns>/chronogenesis/lesson/<name>.json`: item, stage, capacity, awareness, what it says): show it
  the item to teach it. Words (book) lets it speak; A Name (a named name tag) names it; The Time of Day (clock) lets
  it tell the time. Others: direction, music, mortality, the sky, the cold, the other scientist, eternity.
- *Stages:* 1 (Child): 8 lobes, 4 awareness, words. 2 (Adolescent): 32 lobes, 9 awareness, a name. 3 (the
  Cerebrum): 13 awareness and firm lobes filling the whole 5 x 5 x 5 cranium around the jar, a multiblock: the lobes
  become Cerebrum with an eye in the middle of each face; break it and it drops to stage 2. Its wiring range grows
  (32, 48, 64, 96) and its jar model changes with each stage. Research topics are gated by stage.
- *Speech:* with words, it answers chat said to it (its name, "cortex"/"brain", a question, or anything said within
  6 blocks) to everyone in hearing range (16-48 blocks by stage), after a moment. Replies are data
  (`data/<ns>/chronogenesis/dialogue/<name>.json`: keywords, lines per stage, and answers it works out: its state,
  what it wants to learn, the next research, the time, its name, its stage). It mutters unprompted, remembers what
  players ask to be called, and before it has words only twitches.

**As built (stage 0):**
- *Ritual:* a Preserved Brain on a **Dissection Table** soaks up 16 drams each of Awareness (only from a
  Tap facing the table, since glass cannot hold it) and Time (Tap or Stasis Ampoule). Lightning striking
  within 3 blocks of a table with a lightning rod on its frame wakes it; an unready brain burns.
- *Wiring:* Ganglion grafts within 32 blocks of the Chronocortex wire their organism in. Its screen
  shows each wired organism's store, hunger and instability, and it warns aloud before a Time
  organism bursts. Redirecting flow and Tap limits are not built yet.
- *Research:* topics are a synced datapack registry, `data/<ns>/chronogenesis/research/<topic>.json`
  (title, description, icon, stage, requires, essence, materials, alternative clue sets). Essence is
  drawn from wired organisms. Completed topics are checked in code by id.
- *Clues:* until the Lens exists, clues are witnessed (within 16 blocks): a creature floating from
  lost Density, a Vacant mind, a desiccation, a Time Tear opening, a Racetrack pulse arriving, Twin
  Clocks making Time, and the Chronocortex waking. Clues are spent by research and never re-granted.

## 6. Temporal manipulation (higher Time powers)

Each step is unlocked by research and needs the full suit.

| Power | Effect |
|---|---|
| **Slow Area** | A region where entities, block entities and random ticks run slower |
| **Haste Area** | The same region, faster: crops, furnaces, and baby mobs growing up |
| **Stopped Chunk** | Time stands still in one chunk: nothing ticks, water and lava freeze mid-flow |
| **Time Loop** | Puts a whole chunk on a daily loop (see below) |

**Local daytime.** Every affected chunk keeps its **own clock**. Standing in a slowed chunk, you
watch its sun crawl. A stopped chunk can stay at midnight forever. Implementation note: a
per-chunk time offset and rate are stored in a chunk attachment. The client renders the sky and
sky light from the local clock of the chunk the player stands in, and mob spawning checks use the
local light.

**As built (Slow Time, Time Freeze):** `time/TimeZones`. A zone is one chunk with its own rate (Slow Time: 1/2,
1/4 or 1/8 by strength; Freeze: 0), held by its caster, who pays Time to keep it (more for a freeze) and must stay
within 96 blocks. Inside, entities (except players), block entities, random ticks and scheduled ticks run at the
chunk's rate (`mixin/BoundTickingBlockEntityMixin`, `ServerLevelMixin`, `LevelTicksMixin`; entities through
`EntityTickEvent.Pre`); players without the full suit are slowed through their attributes, and stopped ones cannot
act. The chunk keeps a lag behind the world's clock; whoever stands in it sees its sky (`mixin/ClientLevelMixin`
overrides `dayTime()`). Let go, the zone accelerates past the world's rate and eases back, landing exactly on the
world's time (and moon phase); extra ticks are run while it catches up. Zones are synced to clients every tick.
**How it looks** (`shaders/program/time_zone.fsh`): light is slowed too. From outside, a slowed chunk is redshifted
and dimmed; a stopped chunk lets no light out or through: a black, chunk-shaped column with a thin glow where light
grazes it. An unshielded viewer inside sees the outside blueshifted and bright. Chrono Goggles let the stalled light
through: a stopped chunk looks like a sepia photograph.

### Time Loop
At the chunk's local dawn, it **resets to a snapshot** taken when the loop was made. Ores grow
back, trees regrow and natural resources reappear. This is intended resource duplication, and the
cost is Time essence upkeep and Temporal Stress.

**Why chests (and other things) don't reset: the Rule of Observation.**
> *"Time forgets what no mind has touched. What a mind has held, it remembers."*

Anything that a **conscious being has handled or placed** is **anchored** in the present by the
Awareness left on it, and the loop cannot rewind it:
- **Items** have been handled, so all **container contents** are anchored. Chests, furnaces and
  barrels keep their current contents. Nothing in an inventory is ever restored.
- **Blocks placed by a player** are anchored, so they are left out of the snapshot. Without this,
  you could place diamond blocks, loop the chunk, and mine them again every day.
- **Living creatures** have their own Awareness, so they keep their own time. Mobs are not
  duplicated.
- What loops is only the **untouched land**: natural terrain, ores, wild plants and water.

This shuts down duplication exploits, and it also explains the Awareness essence's role in the
world. Implementation: a per-chunk bitset of player-placed positions. Block entities and entities
are always left out.

### Temporal Fields
The **Temporal Field** is a Time organism with a **Field Graft**. It gently rewinds living things
in its radius, so drained farm animals **slowly regenerate** their essence. This is what turns
breed-and-drain farms into sustainable ones. It costs a steady trickle of Time and adds a little
Temporal Stress.

## 7. The Temporal Lens and exploration

### The Lens
The **Temporal Lens** is a brass and glass spyglass with an amethyst lens. It runs on Time essence
stored in it, or taken from the player's Time pool. Its **power setting** sets how far back you
look:

| Setting | You see |
|---|---|
| Echo (hours) | Ghosts of recent events: which blocks were broken, where mobs walked |
| Decades | Abandoned villages as **bustling marketplaces** with ghost villagers. Ruined labs with their lights on |
| Centuries | The **full castle** instead of the ruin. Intact bridges and roads |
| Ages *(stage 1+)* | The land itself changes: ice sheets, other flora. This is the Ice Age teaser |

How it looks: a hazy, desaturated, slightly warped view.
- **Player-placed blocks fade out.**
- **Mod structures swap to their intact "past" templates.** Every ruin structure has a matching
  past template.
- **Terrain gets an era overlay** (snow, flora swaps).

The view is generated from the world seed plus these changes, so it is a plausible past rather
than a pure replay.

**As built:** the Lens is grown by research and burns Time (its own store, then the player's pool;
1 dram per 2 s, 2 at Centuries). The past shows only inside a **round lens** in the middle of the
screen (like Thaumcraft's), framed in brass; around it the present goes on, dimmed.

The past is a **real dimension**, `chronogenesis:past` (`data/chronogenesis/dimension/past.json`): the
overworld's generator and seed, so its land is the land as it first generated, before anyone dug or
built ("player-placed blocks fade out" for free). Where an overworld ruin has a **past view**
(`data/<ns>/chronogenesis/past_view/<name>.json`: structure, past template, depth, clue), its past
template is stamped into the past dimension once, at the same spot and rotation (`timetravel/PastStamping`).
While the Lens is in use, the server keeps the past chunks around the viewer loaded but not ticking
(generating them if new) and sends their sections once each, nearest first, with the past's own light
(`lens/LensSync`). The client builds them into chunk-section meshes a few per frame (`client/PastMesh`,
through `client/PastWorld`), and inside the lens draws only the past: everything the present drew is
wiped (the sky is kept) and the past is drawn in its place, then composited by one post shader
(`shaders/post/temporal_lens.json`, `client/LensView`). What the lens hides is still there: a floor
seen through it may be a hole now, and what the past lacks may still block the way. The past is
tangible: later eras of time travel can let players walk in it. Echo shows nothing yet; Centuries shows
the same past as Decades for now. Holding the view on a Faded Blueprint for 2 s at Decades or deeper
reads it into a `blueprint_<name>` clue, with a pen-scribble sound (`chronogenesis:lens.scribble`).
Echo shows nothing yet. Holding the view on a Faded Blueprint for 2 s at Decades or deeper reads it
into a `blueprint_<name>` clue.

**Eras through the Lens:** after Centuries, sneak-use goes on to every era its holder has stood in (newest first,
the `lens_era` component), then round to Echo. Set to an era, the Lens shows that era's own dimension at the same
spot instead of the past dimension (3 drams per 2 s): `LensSync` streams whichever level is being looked into, and
the client drops its copy when the source changes.

### Clues and the journal
Things you notice through the Lens become **Clues**, which are logged in the journal:
- the merchant's stall that sold something important
- the inscription on the castle's clocktower
- the layout of a lab's machine

Clues are spent in Chronocortex research.

### Structures (every one has a purpose)
| Structure | Purpose |
|---|---|
| **Abandoned Laboratory** | The other scientist who came before you. Mostly decayed. **Faded blueprints** on the tables are illegible now, but **readable through the Lens**: scanning one adds a *scribbled blueprint* to the journal. Rarely, one intact blueprint is left |
| **Intact Laboratory** (rare, hidden) | Working blueprints, rare specimens, a dead Chronocortex |
| **Ruined Castle** | Through the Lens: the full keep. The clocktower and the lord's archive hold clues about Laws |
| **Abandoned Village / Marketplace** | Through the Lens: trade stalls whose wares hint at recipes and needle materials |
| **Collapsed Observatory** | Astronomers measured time. Clues for Time synthesis and the Pendulum |
| **Drowned Specimen House** | Underwater lab. Aquatic specimens and grafts |

### Shrines and Obelisks
These are ancient stones that are **almost untouched by time**: no weathering, no moss. Looking at
one through the Lens shows a distortion **like a black hole**:
- Within an **event horizon** radius, the past view bends, smears and goes dark.
- At the edge, fragments of a much older inscription can be read: high-value Clues about the
  nature of time, and about something waiting at its end.

**As built:** obelisks generate across the overworld (a dark spire on a plinth, bands of glowing
inscription, a capstone) and cannot be broken. The capstone's block entity marks them. Through the
Lens, the lens shader (`shaders/post/temporal_lens.json`), centred on the obelisk's projected heart,
lenses and swirls the image (across the whole screen, overwhelmingly inside the lens), smears it along the orbit, splits its colours,
tears scanlines, jumps blocks of the image, and burns a photon ring around a black horizon with static
inside; strength fades from 16 to 64 blocks. Past-view ghosts inside the horizon (4.5 blocks) are
dropped. Seeing one gives *A stone that time bends around*; holding the Lens on it from the edge
(4.5-7.5 blocks) for 3 s gives *Fragments of an inscription older than time*.

Lore: obelisks are the **Nails** that hold time in place. The Precursors built shrines round them (near them
nothing went missing) without knowing what they were; the Jurassic temples are built round them, and the Ice Age keeps
those temples' remains; they already stand in the Hadean; and Before Time the player drives the first of them, to make
the Elder God mortal. Through every era they stand on exactly the same spots.

## 8. Implementation notes for the current code
- Replace the single `temporal_essence` int attachment with an **essence-pool attachment**: a map
  from aspect to (current, max). Max comes from the essence profile; current starts full.
- Change the `essenceRegenTicks` default to **0** (no natural regeneration). Temporal Fields add
  regeneration.
- New attachments:
  - player: aspect pools, power levels, Temporal Exposure
  - chunk: Temporal Stress, local time offset and rate, loop snapshot, player-placed bitset
- New data-driven registries: essence profiles, grafts, research topics, Laws, structure past
  templates.
- Mods to enable: **Curios** (Umbilical Locket), **GeckoLib** (organisms, grafts),
  **Patchouli** (journal).

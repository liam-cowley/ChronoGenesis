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
| **Heart** | Animal heart specimen | Pumps: faster flow through Taps and Veins |
| **Eye** | Spider or ender eye | Senses fill level and nearby mobs (comparator and redstone output) |
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
3. **Temporal shifts.** Time travel, from stage 1 onward.

It talks. At first it whispers in chat and in the journal, curious and childlike. As it grows it
becomes demanding, and by the final stage it is insane.

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

Lore hook for later: obelisks are **nails holding time in place**, left by whoever (or whatever)
came before. They become anchors and targets for time travel, and are a thread that leads to the
Elder God.

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

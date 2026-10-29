"""
Writes the ChronoGenesis worldgen structure templates (NBT) to src/main/resources/data/chronogenesis/structure/.

Every ruin has a matching "past" template for the Temporal Lens (core-systems.md section 7). Both are built from
one layout here: the past is drawn as it was, then decayed (with a fixed seed) into the ruin, so the two always
line up block for block.

  abandoned_laboratory       the ruin that generates in the world
  abandoned_laboratory_past  the same lab in its prime, as the Lens shows it at Decades
  obelisk                    an obelisk: untouched by time, so it has no past template of its own
  precursor_city_<name>      a city of the Precursors (the Precursor era), over the present's remains: sun,
                             tide_town, tide_capital, hearth, verdant. See precursor_cities.py
  precursor_shrine_<k>       an obelisk in the shrine a kingdom built round it (the Precursor era)
  jurassic_temple            a sealed ziggurat of the Jurassic Age: a Temporal Core, five seals, raptors
  ice_temple_ruin            what is left of it in the Ice Age: broken, frozen, the obelisk still whole

Usage: python3 tools/make_structures.py
"""
import gzip
import os
import random
import struct

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "src/main/resources/data/chronogenesis/structure")
DATA_VERSION = 3955  # Minecraft 1.21.1

TAG_END, TAG_BYTE, TAG_INT, TAG_DOUBLE, TAG_STRING, TAG_LIST, TAG_COMPOUND = 0, 1, 3, 6, 8, 9, 10


# ---------------------------------------------------------------- NBT

def name(n):
    b = n.encode("utf-8")
    return struct.pack(">H", len(b)) + b


def payload(tag, value):
    if tag == TAG_BYTE:
        return struct.pack(">b", value)
    if tag == TAG_INT:
        return struct.pack(">i", value)
    if tag == TAG_DOUBLE:
        return struct.pack(">d", value)
    if tag == TAG_STRING:
        return name(value)
    if tag == TAG_LIST:
        elem_tag, items = value
        out = struct.pack(">bi", elem_tag if items else TAG_END, len(items))
        return out + b"".join(payload(elem_tag, i) for i in items)
    if tag == TAG_COMPOUND:
        out = b""
        for key, (t, v) in value.items():
            out += struct.pack(">b", t) + name(key) + payload(t, v)
        return out + struct.pack(">b", TAG_END)
    raise ValueError(tag)


def ints(*values):
    return (TAG_LIST, (TAG_INT, list(values)))


def parse_state(state):
    """'minecraft:oak_slab[type=top]' -> ('minecraft:oak_slab', {'type': 'top'})."""
    if "[" not in state:
        return state, {}
    block, props = state[:-1].split("[", 1)
    return block, dict(p.split("=", 1) for p in props.split(","))


def structure(size, blocks, block_nbt, entities=()):
    """blocks: {(x, y, z): state string}; block_nbt: {(x, y, z): {key: string}} for block entities;
    entities: [((x, y, z) as floats, (x, y, z) block, entity id[, {tag: string or [strings]}])], kept persistent."""
    palette = []
    index = {}
    entries = []
    for pos in sorted(blocks, key=lambda p: (p[1], p[2], p[0])):
        state = blocks[pos]
        if state not in index:
            index[state] = len(palette)
            palette.append(state)
        entry = {"pos": ints(*pos), "state": (TAG_INT, index[state])}
        if pos in block_nbt:
            entry["nbt"] = (TAG_COMPOUND, {k: (TAG_STRING, v) for k, v in block_nbt[pos].items()})
        entries.append(entry)
    palette_tags = []
    for state in palette:
        block, props = parse_state(state)
        tag = {"Name": (TAG_STRING, block)}
        if props:
            tag["Properties"] = (TAG_COMPOUND, {k: (TAG_STRING, v) for k, v in props.items()})
        palette_tags.append(tag)
    root = {
        "DataVersion": (TAG_INT, DATA_VERSION),
        "size": ints(*size),
        "palette": (TAG_LIST, (TAG_COMPOUND, palette_tags)),
        "blocks": (TAG_LIST, (TAG_COMPOUND, entries)),
        "entities": (TAG_LIST, (TAG_COMPOUND, [{
            "pos": (TAG_LIST, (TAG_DOUBLE, list(pos))),
            "blockPos": ints(*block),
            "nbt": (TAG_COMPOUND, {"id": (TAG_STRING, e[2]), "PersistenceRequired": (TAG_BYTE, 1),
                                   **{k: ((TAG_LIST, (TAG_STRING, v)) if isinstance(v, list) else (TAG_STRING, v))
                                      for k, v in (e[3] if len(e) > 3 else {}).items()}}),
        } for e in entities for pos, block in [(e[0], e[1])]])),
    }
    return struct.pack(">b", TAG_COMPOUND) + name("") + payload(TAG_COMPOUND, root)


def write(file_name, data):
    os.makedirs(OUT, exist_ok=True)
    with gzip.open(os.path.join(OUT, file_name), "wb") as f:
        f.write(data)
    print("wrote", file_name)


# ---------------------------------------------------------------- the Abandoned Laboratory

W, H, D = 11, 7, 9  # x, y, z. Floor at y=0, roof at y=5, the lightning rod pokes up to y=6.
AIR = "minecraft:air"
WALL = "minecraft:stone_bricks"
CG = "chronogenesis:"


def laboratory_past():
    """The lab as it was: whole, lit, an organism growing in its frame, a brain waiting on the table."""
    b = {}
    for x in range(W):
        for z in range(D):
            b[(x, 0, z)] = "minecraft:polished_andesite"
            b[(x, 5, z)] = "minecraft:dark_oak_planks"
            for y in range(1, 5):
                edge = x in (0, W - 1) or z in (0, D - 1)
                corner = x in (0, W - 1) and z in (0, D - 1)
                b[(x, y, z)] = "minecraft:stripped_dark_oak_log[axis=y]" if corner else WALL if edge else AIR
            b[(x, 6, z)] = AIR
    # Windows.
    for x in (2, 3, 7, 8):
        for y in (2, 3):
            b[(x, y, 0)] = "minecraft:glass"
    for x in (4, 5, 6):
        b[(x, 3, D - 1)] = "minecraft:glass"
    for z in (5, 6):
        for y in (2, 3):
            b[(W - 1, y, z)] = "minecraft:glass"
    # The door.
    b[(5, 1, 0)] = "minecraft:dark_oak_door[facing=north,half=lower,hinge=left,open=false]"
    b[(5, 2, 0)] = "minecraft:dark_oak_door[facing=north,half=upper,hinge=left,open=false]"

    # A Density organism grown into its brass frame, a Tap on top.
    for x in (1, 2):
        for y in (1, 2):
            for z in (2, 3, 4):
                b[(x, y, z)] = CG + "chronoflesh[aspect=density,growth=3]"
    b[(1, 1, 3)] = CG + "organism_core[aspect=density,phase=awake]"
    b[(1, 3, 3)] = CG + "tap[facing=up,active=false]"

    # The Dissection Table with a brain in its jar, a lightning rod on the frame and one through the roof.
    b[(5, 1, 6)] = CG + "dissection_table[brain=true]"
    b[(5, 2, 6)] = "minecraft:lightning_rod"
    b[(5, 6, 6)] = "minecraft:lightning_rod"

    # Work tables with blueprints, candles burning.
    for z in (2, 5):
        for x in (8, 9):
            b[(x, 1, z)] = "minecraft:spruce_slab[type=top]"
    b[(8, 2, 2)] = CG + "faded_blueprint[design=locket,facing=west]"
    b[(8, 2, 5)] = CG + "faded_blueprint[design=field,facing=west]"
    b[(9, 2, 2)] = "minecraft:candle[candles=3,lit=true]"
    b[(9, 2, 5)] = "minecraft:candle[candles=2,lit=true]"

    # Shelves, a brewing stand, a cauldron, the chest, lanterns.
    for x in (1, 2, 3):
        for y in (1, 2):
            b[(x, y, D - 2)] = "minecraft:bookshelf"
    b[(4, 1, D - 2)] = "minecraft:brewing_stand"
    b[(7, 1, D - 2)] = "minecraft:water_cauldron[level=2]"
    b[(9, 1, D - 2)] = "minecraft:chest[facing=west]"
    b[(3, 4, 4)] = "minecraft:lantern[hanging=true]"
    b[(7, 4, 4)] = "minecraft:lantern[hanging=true]"
    return b


def laboratory_ruin(past):
    """Decades of neglect: the roof fallen in, walls cracked and holed, glass gone, the organism dead."""
    rng = random.Random(1983)
    b = dict(past)
    collapse = (3, 5)  # x, z of the hole in the roof
    for (x, y, z), state in past.items():
        if y == 5:
            if (x - collapse[0]) ** 2 + (z - collapse[1]) ** 2 < 7 + rng.random() * 5:
                b[(x, y, z)] = AIR
                if 0 < x < W - 1 and 0 < z < D - 1 and past[(x, 1, z)] == AIR and rng.random() < 0.6:
                    b[(x, 1, z)] = rng.choice(["minecraft:cobblestone", "minecraft:gravel", "minecraft:mossy_cobblestone"])
            elif rng.random() < 0.12:
                b[(x, y, z)] = AIR
        elif state == WALL:
            r = rng.random()
            hole_chance = 0.25 if y == 4 else 0.06
            b[(x, y, z)] = (AIR if r < hole_chance else "minecraft:cracked_stone_bricks" if r < 0.45
                            else "minecraft:mossy_stone_bricks" if r < 0.65 else WALL)
        elif state == "minecraft:glass":
            b[(x, y, z)] = AIR
        elif state.startswith("minecraft:dark_oak_door"):
            b[(x, y, z)] = AIR
        elif state.startswith(CG + "chronoflesh"):
            b[(x, y, z)] = CG + "brass_armature" if rng.random() < 0.7 else AIR
        elif state.startswith(CG + "organism_core"):
            b[(x, y, z)] = CG + "organism_core[aspect=density,phase=necrotic]"
        elif state.startswith(CG + "tap"):
            b[(x, y, z)] = AIR
        elif state.startswith(CG + "dissection_table"):
            b[(x, y, z)] = CG + "dissection_table[brain=false]"
        elif state.startswith("minecraft:candle"):
            b[(x, y, z)] = state.replace("lit=true", "lit=false")
        elif state == "minecraft:bookshelf" and rng.random() < 0.5:
            b[(x, y, z)] = AIR
        elif state.startswith("minecraft:lantern") or state.startswith("minecraft:water_cauldron"):
            b[(x, y, z)] = AIR if state.startswith("minecraft:lantern") else "minecraft:cauldron"
        elif state == "minecraft:polished_andesite" and rng.random() < 0.15:
            b[(x, y, z)] = "minecraft:andesite"
    # A little life moving back in.
    for _ in range(8):
        x, y, z = rng.randrange(1, W - 1), rng.choice([3, 4]), rng.randrange(1, D - 1)
        if b[(x, y, z)] == AIR:
            b[(x, y, z)] = "minecraft:cobweb"
    for x in range(1, W - 1):
        for z in range(1, D - 1):
            if b[(x, 1, z)] == AIR and rng.random() < 0.12:
                b[(x, 1, z)] = "minecraft:moss_carpet"
    return b


# ---------------------------------------------------------------- Obelisks

def obelisk():
    """A dark spire on a three-by-three plinth, bands of inscription, the capstone (the nail's head) on top.
    The capstone sits HEART_BELOW_CAPSTONE (5) blocks above the middle of the shaft, matching Obelisks.java."""
    b = {}
    for x in range(3):
        for z in range(3):
            b[(x, 0, z)] = CG + "obelisk_stone"
            b[(x, 1, z)] = CG + "obelisk_stone" if (x, z) == (1, 1) else "minecraft:polished_blackstone_slab[type=bottom]"
            for y in range(2, 12):
                b[(x, y, z)] = AIR
    for y in range(1, 11):
        b[(1, y, 1)] = CG + ("obelisk_inscription" if y in (3, 6, 9) else "obelisk_stone")
    b[(1, 11, 1)] = CG + "obelisk_capstone"
    return b


# ---------------------------------------------------------------- Precursor cities (the Age of the Precursors)
# See precursor_cities.py: the kingdoms' cities (over the present's remains) and the shrines round the obelisks.

import precursor_cities  # noqa: E402  (next to this script)


# ---------------------------------------------------------------- the Jurassic temple
# A great ziggurat whose Temporal Core holds the inside still: nothing in it can be placed or broken until three of
# its five seals are broken, each by a different gift of the glands (see temple/SealBlock):
#   heavy (east room, Iron Fists or a Ground Slam), narrow (west, at the end of a one-block crawlspace: Shrink),
#   swift (north, at the end of a long hall: run into it with Quickening), veiled (one of the core chamber's walls,
#   seen with Glowing Sight), stilled (the upper room, up the ladder: slow or stop its time).
# It is built around an obelisk (the same one that stands in the present: the temple is centred on the obelisks'
# placement), whose shaft rises through the core chamber and out of the summit. The core hangs beside the shaft,
# tied to it by sinews. Local coordinates: the shaft at (17, y, 17), the core at (17, 3, 15); its barrier reaches 18
# around the core, from 5 below to 24 above.

TEMPLE = 35
TEMPLE_H = 28
T_CORE = (17, 3, 15)
T_WALL = "minecraft:polished_blackstone_bricks"
T_TRIM = "minecraft:gilded_blackstone"
T_FLOOR = "minecraft:polished_deepslate"
T_PILLAR = "minecraft:chiseled_polished_blackstone"
T_LAMP = "minecraft:soul_lantern[hanging=true,waterlogged=false]"
T_RAPTORS = [(17, 1, 28), (28, 1, 17), (8, 1, 5), (20, 10, 14)]


def jurassic_temple():
    b = {}

    def box(x0, y0, z0, x1, y1, z1, state):
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1):
                    b[(x, y, z)] = state

    # Five tiers, each three blocks in from the last, gold-trimmed at their rims; a banded shrine on top.
    for t in range(5):
        lo, hi = 3 * t, TEMPLE - 1 - 3 * t
        y0 = 5 * t
        box(lo, y0, lo, hi, y0 + 4, hi, T_WALL)
        for x in range(lo, hi + 1):
            for z in range(lo, hi + 1):
                if x in (lo, hi) or z in (lo, hi):
                    b[(x, y0 + 4, z)] = T_TRIM
    box(16, 25, 16, 18, 25, 18, CG + "obelisk_inscription")
    box(1, 0, 1, TEMPLE - 2, 0, TEMPLE - 2, T_FLOOR)

    # The entrance hall, from the south face to the core chamber.
    box(16, 1, 23, 18, 3, TEMPLE - 1, AIR)
    for z in (26, 30):
        b[(16, 3, z)] = T_LAMP
        b[(18, 3, z)] = T_LAMP
    # The core chamber: tall, pillared, the core in the middle, its treasure behind it.
    box(11, 1, 11, 23, 8, 23, AIR)
    for x, z in [(12, 12), (12, 22), (22, 12), (22, 22)]:
        box(x, 1, z, x, 8, z, T_PILLAR)
    b[(17, 1, 12)] = "minecraft:chest[facing=south,type=single,waterlogged=false]"
    for x, z in [(14, 14), (20, 14), (14, 20), (20, 20)]:
        b[(x, 8, z)] = T_LAMP
    # The veiled seal: one block of the west wall, like all the others.
    b[(10, 3, 20)] = CG + "veiled_seal[broken=false]"

    # East: the heavy seal on a gold plinth.
    box(24, 1, 16, 25, 3, 18, AIR)
    box(26, 1, 14, 31, 4, 20, AIR)
    b[(30, 1, 17)] = T_TRIM
    b[(30, 2, 17)] = CG + "heavy_seal[broken=false]"
    b[(28, 4, 17)] = T_LAMP

    # West: a crawlspace one block high, and the narrow seal in the low cell at its end.
    box(4, 1, 17, 10, 1, 17, AIR)
    box(2, 1, 15, 4, 1, 19, AIR)
    b[(2, 1, 17)] = CG + "narrow_seal[broken=false]"

    # North: a long hall to run down, the swift seal at its far end.
    box(13, 1, 7, 13, 3, 10, AIR)
    box(3, 1, 4, 31, 3, 6, AIR)
    b[(32, 1, 5)] = CG + "swift_seal[broken=false]"
    b[(32, 2, 5)] = CG + "swift_seal[broken=false]"
    for x in (6, 14, 22, 30):
        b[(x, 3, 4)] = T_LAMP

    # Up: a ladder on the chamber's north wall, a hatchway, and the upper room with the stilled seal.
    for y in range(1, 10):
        b[(17, y, 11)] = "minecraft:ladder[facing=south,waterlogged=false]"
    box(12, 10, 11, 22, 13, 17, AIR)
    for y in range(9, 11):
        b[(17, y, 11)] = "minecraft:ladder[facing=south,waterlogged=false]"
    b[(17, 10, 15)] = T_TRIM
    b[(17, 11, 15)] = CG + "stilled_seal[broken=false]"
    b[(17, 13, 14)] = T_LAMP

    # The obelisk: the temple's spine, from the floor out through the summit to its capstone.
    for y in range(0, TEMPLE_H - 1):
        b[(17, y, 17)] = CG + ("obelisk_inscription" if y % 4 == 2 else "obelisk_stone")
    b[(17, TEMPLE_H - 1, 17)] = CG + "obelisk_capstone"
    # The core hangs beside it, tied on by sinews: one to the shaft, a strand down to the floor and one up and over,
    # and more knotted round the shaft above and below.
    cx, cy, cz = T_CORE
    b[(cx, cy, cz)] = CG + "temporal_core[active=true]"
    sinew = CG + "core_sinew[axis={}]"
    b[(cx, cy, cz + 1)] = sinew.format("z")
    for y in (1, 2, 4, 5, 6):
        b[(cx, y, cz)] = sinew.format("y")
    b[(cx, 6, cz + 1)] = sinew.format("z")
    for x, y, z, axis in [(16, 2, 17, "x"), (18, 4, 17, "x"), (17, 3, 18, "z"), (16, 7, 17, "x"), (18, 8, 17, "x"),
                          (17, 7, 18, "z"), (15, 2, 17, "x"), (19, 4, 17, "x"), (17, 8, 16, "z")]:
        b[(x, y, z)] = sinew.format(axis)
    # On the entrance hall's walls, what its builders left: the nail, the sinews, the eye beneath.
    mural = ["..k..", "mmkmm", ".mkm.", "..k..", ".ppp.", ".pep."]
    colours = {"k": CG + "obelisk_stone", "m": "minecraft:magenta_terracotta", "p": "minecraft:purple_concrete",
               "e": "minecraft:lime_concrete"}
    for row, line in enumerate(mural):
        for col, ch in enumerate(line):
            if ch in colours:
                b[(15, 1 + 5 - row, 26 + col)] = colours[ch]
    return b


def ice_temple_ruin():
    """The Jurassic temple as the Ice Age has it: its tiers broken, its rooms half full of ice and snow, its seals
    and lamps long gone. The obelisk stands whole in the middle (exactly the obelisk of the present: it is the same
    one), and where the core hung, its time froze when it died: Aeon Ice round the frozen sinews."""
    r = random.Random(7303)
    temple = jurassic_temple()
    out = {}
    for (x, y, z), state in temple.items():
        if x == 17 and z == 17:
            continue
        if state == AIR:
            out[(x, y, z)] = "minecraft:snow_block" if y <= 2 and r.random() < 0.35 else ("minecraft:packed_ice" if y == 1 and r.random() < 0.1 else AIR)
            continue
        if "seal" in state or "lantern" in state or "ladder" in state:
            out[(x, y, z)] = AIR if "seal" not in state else T_WALL
            continue
        if "temporal_core" in state:
            continue
        if r.random() < (y - 6) / 26.0:
            continue
        if state == T_WALL and r.random() < 0.3:
            state = r.choice(["minecraft:cracked_polished_blackstone_bricks", "minecraft:packed_ice", "minecraft:blue_ice"])
        out[(x, y, z)] = state
    # Snow on whatever is left open to the sky.
    tops = {}
    for (x, y, z), state in out.items():
        if state != AIR:
            tops[(x, z)] = max(tops.get((x, z), -1), y)
    for (x, z), y in tops.items():
        if y + 1 < TEMPLE_H and r.random() < 0.8:
            out[(x, y + 1, z)] = "minecraft:snow[layers=" + str(r.randint(1, 3)) + "]"
    # Where the core hung: Aeon Ice, the core's last moment, round the frozen sinews.
    cx, cy, cz = T_CORE
    for dx in (-1, 0, 1):
        for dy in (-1, 0, 1):
            for dz in (-1, 0, 1):
                if abs(dx) + abs(dy) + abs(dz) <= 2 and (cx + dx, cz + dz) != (17, 17):
                    out[(cx + dx, cy + dy, cz + dz)] = CG + "aeon_ice"
    for x, y, z in [(14, 1, 13), (20, 2, 21), (12, 1, 19), (22, 1, 15)]:
        out[(x, y, z)] = CG + "aeon_ice"
    # A raptor that froze where it stood guard: its bones in the ice.
    for x, y, z in [(17, 1, 27), (17, 2, 27), (17, 1, 28), (17, 1, 29), (17, 2, 26)]:
        out[(x, y, z)] = "minecraft:bone_block[axis=z]"
    out[(17, 1, 12)] = "minecraft:chest[facing=south,type=single,waterlogged=false]"
    for (x, y, z), state in list(out.items()):
        if (x, z) in ((16, 17), (18, 17), (17, 16), (17, 18)) or (16 <= x <= 18 and 16 <= z <= 18):
            out.pop((x, y, z), None)
    precursor_cities.obelisk_into(out, 17, 0, 17)
    return out


def raptors_in_temple():
    return [((x + 0.5, float(y), z + 0.5), (x, y, z), "chronogenesis:raptor") for x, y, z in T_RAPTORS]


if __name__ == "__main__":
    past = laboratory_past()
    ruin = laboratory_ruin(past)
    chest = {(9, 1, D - 2): {"id": "minecraft:chest", "LootTable": "chronogenesis:chests/abandoned_laboratory"}}
    write("abandoned_laboratory.nbt", structure((W, H, D), ruin, chest))
    write("abandoned_laboratory_past.nbt", structure((W, H, D), past, {}))
    write("obelisk.nbt", structure((3, 12, 3), obelisk(), {}))
    for i, city_name in enumerate(precursor_cities.CITIES):
        built = precursor_cities.city(city_name, 4100 + 10 * i)
        write("precursor_city_" + city_name + ".nbt", structure((built.n, built.H, built.n), built.b, built.nbt, built.entities))
    for i, kingdom in enumerate(["sun", "tide", "hearth", "verdant"]):
        blocks, chests = precursor_cities.shrine(kingdom, 6100 + i)
        write("precursor_shrine_" + kingdom + ".nbt", structure((precursor_cities.S, precursor_cities.SH, precursor_cities.S),
                                                                blocks, chests))
    temple_chest = {(17, 1, 12): {"id": "minecraft:chest", "LootTable": "chronogenesis:chests/jurassic_temple"}}
    write("jurassic_temple.nbt", structure((TEMPLE, TEMPLE_H, TEMPLE), jurassic_temple(), temple_chest, raptors_in_temple()))
    ice_chest = {(17, 1, 12): {"id": "minecraft:chest", "LootTable": "chronogenesis:chests/ice_temple_ruin"}}
    write("ice_temple_ruin.nbt", structure((TEMPLE, TEMPLE_H, TEMPLE), ice_temple_ruin(), ice_chest))

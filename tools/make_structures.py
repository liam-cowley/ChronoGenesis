"""
Writes the ChronoGenesis worldgen structure templates (NBT) to src/main/resources/data/chronogenesis/structure/.

Every ruin has a matching "past" template for the Temporal Lens (core-systems.md section 7). Both are built from
one layout here: the past is drawn as it was, then decayed (with a fixed seed) into the ruin, so the two always
line up block for block.

  abandoned_laboratory       the ruin that generates in the world
  abandoned_laboratory_past  the same lab in its prime, as the Lens shows it at Decades
  obelisk                    an obelisk: untouched by time, so it has no past template of its own

Usage: python3 tools/make_structures.py
"""
import gzip
import os
import random
import struct

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "src/main/resources/data/chronogenesis/structure")
DATA_VERSION = 3955  # Minecraft 1.21.1

TAG_END, TAG_INT, TAG_STRING, TAG_LIST, TAG_COMPOUND = 0, 3, 8, 9, 10


# ---------------------------------------------------------------- NBT

def name(n):
    b = n.encode("utf-8")
    return struct.pack(">H", len(b)) + b


def payload(tag, value):
    if tag == TAG_INT:
        return struct.pack(">i", value)
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


def structure(size, blocks, block_nbt):
    """blocks: {(x, y, z): state string}; block_nbt: {(x, y, z): {key: string}} for block entities."""
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
        "entities": (TAG_LIST, (TAG_COMPOUND, [])),
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


if __name__ == "__main__":
    past = laboratory_past()
    ruin = laboratory_ruin(past)
    chest = {(9, 1, D - 2): {"id": "minecraft:chest", "LootTable": "chronogenesis:chests/abandoned_laboratory"}}
    write("abandoned_laboratory.nbt", structure((W, H, D), ruin, chest))
    write("abandoned_laboratory_past.nbt", structure((W, H, D), past, {}))
    write("obelisk.nbt", structure((3, 12, 3), obelisk(), {}))

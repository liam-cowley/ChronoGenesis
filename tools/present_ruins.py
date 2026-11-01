"""
The present's ruins (core-systems.md section 7, "Structures: every one has a purpose"), imported by
make_structures.py. Each is drawn as it was (its "past" template, what the Temporal Lens shows) and then decayed
with a fixed seed into what stands now, so the two line up block for block.

  ruined_castle            curtain walls, four towers and a gatehouse round a keep: the great hall, the lord's
                           archive (his ledger, readable through the Lens), his chamber, and the clocktower whose
                           inscription is a clue. Centuries: the keep whole, banners up, the clock going.
  abandoned_marketplace    a paved square round a fountain, six stalls and the merchant's house. Decades: the
                           awnings bright and the counters full: glass, amethyst, copper, silk, a clockmaker's dials.
  collapsed_observatory    a round tower on the heights: a spiral ramp, a pendulum that fell and cracked the floor, a
                           fallen telescope, an orrery, a star chart. Centuries: the dome whole, the pendulum swinging.
  drowned_specimen_house   a laboratory on the sea floor, flooded: shattered tanks of dead coral. Decades: dry under
                           its glass roof, lit, the specimens alive in their tanks.
  intact_laboratory        rare and hidden: an old dry well with a rotten lid, a lightning rod in the grass beside it,
                           and a ladder down to a sealed bunker. A dead Chronocortex, its organisms necrotic, the
                           scientist's skull on the pillow, one soul lantern still burning. Decades: all of it alive.

Each builder returns (size, past, ruin, chests): blocks as {(x, y, z): state}, chests as {(x, y, z): nbt} (the ruin's).
"""
import math
import random

AIR = "minecraft:air"
WATER = "minecraft:water"
CG = "chronogenesis:"


class Plan:
    """A template being drawn: blocks by position, kept inside its size."""

    def __init__(self, w, h, d):
        self.w, self.h, self.d = w, h, d
        self.b = {}

    def inside(self, x, y, z):
        return 0 <= x < self.w and 0 <= y < self.h and 0 <= z < self.d

    def set(self, x, y, z, state):
        if self.inside(x, y, z):
            self.b[(x, y, z)] = state

    def get(self, x, y, z):
        return self.b.get((x, y, z))

    def box(self, x0, y0, z0, x1, y1, z1, state):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.set(x, y, z, state)

    def walls(self, x0, y0, z0, x1, y1, z1, state):
        """The four walls of a box (no floor or ceiling)."""
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if x in (x0, x1) or z in (z0, z1):
                    for y in range(y0, y1 + 1):
                        self.set(x, y, z, state)


def door(plan, x, y, z, wood, facing, hinge="left"):
    plan.set(x, y, z, f"minecraft:{wood}_door[facing={facing},half=lower,hinge={hinge},open=false]")
    plan.set(x, y + 1, z, f"minecraft:{wood}_door[facing={facing},half=upper,hinge={hinge},open=false]")


def bed(plan, x, y, z, colour, facing):
    """A bed with its head at (x, y, z), its foot one block away from where it faces."""
    dx, dz = {"north": (0, 1), "south": (0, -1), "east": (-1, 0), "west": (1, 0)}[facing]
    plan.set(x, y, z, f"minecraft:{colour}_bed[facing={facing},part=head]")
    plan.set(x + dx, y, z + dz, f"minecraft:{colour}_bed[facing={facing},part=foot]")


def noise_heights(n, rng, low, high, smooth=3):
    """n wall heights round a ring or along a wall, jagged but not random: a smoothed random walk."""
    raw = [rng.uniform(low, high) for _ in range(n)]
    out = []
    for i in range(n):
        window = [raw[(i + k) % n] for k in range(-smooth, smooth + 1)]
        out.append(int(round(sum(window) / len(window) + rng.uniform(-1.2, 1.2))))
    return out


STONE_AGE = {
    "minecraft:stone_bricks": [("minecraft:cracked_stone_bricks", 0.25), ("minecraft:mossy_stone_bricks", 0.25)],
    "minecraft:cobblestone": [("minecraft:mossy_cobblestone", 0.4)],
    "minecraft:polished_andesite": [("minecraft:andesite", 0.3), ("minecraft:cobblestone", 0.1)],
    "minecraft:stone_brick_stairs": [("minecraft:mossy_stone_brick_stairs", 0.3)],
    "minecraft:stone_brick_slab[type=bottom]": [("minecraft:mossy_stone_brick_slab[type=bottom]", 0.3)],
    "minecraft:deepslate_tiles": [("minecraft:cracked_deepslate_tiles", 0.25)],
    "minecraft:deepslate_bricks": [("minecraft:cracked_deepslate_bricks", 0.25)],
    "minecraft:copper_block": [("minecraft:oxidized_copper", 0.6), ("minecraft:weathered_copper", 1.0)],
    "minecraft:cut_copper": [("minecraft:oxidized_cut_copper", 0.6), ("minecraft:weathered_cut_copper", 1.0)],
    "minecraft:copper_grate": [("minecraft:oxidized_copper_grate", 0.7), ("minecraft:weathered_copper_grate", 1.0)],
    "minecraft:prismarine_bricks": [("minecraft:prismarine", 0.45), ("minecraft:dark_prismarine", 0.1)],
}


def weather(state, rng):
    """A block aged: stone cracked and mossy, copper gone green."""
    base = state.split("[")[0]
    for key in (state, base):
        if key in STONE_AGE:
            r = rng.random()
            acc = 0.0
            for aged, p in STONE_AGE[key]:
                acc = p if p >= acc else acc
                if r < p:
                    if "[" in state and "[" not in aged:
                        return aged + state[state.index("["):]
                    return aged
            return state
    return state


def is_wood(state):
    return any(w in state for w in ("_planks", "_slab", "_stairs", "_fence", "_door", "_trapdoor", "_bed", "bookshelf", "lectern",
                                    "barrel", "_wall_banner", "_banner", "_log", "hay_block", "_carpet", "_wool", "crafting_table"))


def vines_on(plan, rng, chance, solid_test, y_max):
    """Vines hanging off the outside of walls, attached to the face they hang on."""
    faces = [("north", 0, -1), ("south", 0, 1), ("west", -1, 0), ("east", 1, 0)]
    for (x, y, z), state in list(plan.b.items()):
        if not solid_test(state) or y < 2 or y > y_max:
            continue
        for face, dx, dz in faces:
            nx, nz = x - dx, z - dz
            if plan.inside(nx, y, nz) and plan.get(nx, y, nz) in (None, AIR) and rng.random() < chance:
                # The vine is on the side of (x, y, z) facing (nx, nz): it attaches towards the wall.
                plan.set(nx, y, nz, f"minecraft:vine[{face}=true]")
                for k in range(1, rng.randint(1, 4)):
                    if plan.get(nx, y - k, nz) in (None, AIR) and y - k > 0:
                        plan.set(nx, y - k, nz, f"minecraft:vine[{face}=true]")


def solid_stone(state):
    return state is not None and any(s in state for s in ("stone_brick", "cobblestone", "andesite", "deepslate", "prismarine")) \
        and "stairs" not in state and "slab" not in state and "wall" not in state


# ---------------------------------------------------------------- the Ruined Castle

def ruined_castle():
    W, H, D = 33, 30, 33
    p = Plan(W, H, D)
    SB = "minecraft:stone_bricks"
    # The ground of the bailey: paved paths over trodden earth.
    for x in range(W):
        for z in range(D):
            p.set(x, 0, z, "minecraft:cobblestone" if (x + z) % 5 and 9 <= x <= 23 else "minecraft:dirt_path"
                  if abs(x - 16) <= 1 or abs(z - 8) <= 1 else "minecraft:grass_block")

    # Curtain walls, two thick, a walk along the top behind the battlements.
    for x in range(1, 32):
        for z in range(1, 32):
            if x in (1, 2, 30, 31) or z in (1, 2, 30, 31):
                p.box(x, 0, z, x, 8, z, SB)
                if (x in (1, 31) or z in (1, 31)) and (x + z) % 2 == 0:
                    p.set(x, 9, z, SB)
    # Stairs up to the walk, inside the east wall.
    for k in range(8):
        p.set(29, k + 1, 20 - k, "minecraft:stone_brick_stairs[facing=north,half=bottom,shape=straight]")
        for y in range(1, k + 1):
            p.set(29, y, 20 - k, SB)

    # Four round towers at the corners, with floors, ladders and battlements.
    for cx, cz in ((4, 4), (28, 4), (4, 28), (28, 28)):
        for x in range(cx - 4, cx + 5):
            for z in range(cz - 4, cz + 5):
                d = math.hypot(x - cx, z - cz)
                if d <= 3.9:
                    wall = d > 2.7
                    for y in range(0, 14):
                        p.set(x, y, z, SB if wall or y in (0, 4, 8) else AIR)
                    if wall and (x + z) % 2 == 0:
                        p.set(x, 14, z, SB)
        for y in range(1, 14):
            p.set(cx, y, cz, "minecraft:ladder[facing=north]" if y < 13 else AIR)
        for y in range(1, 13):
            p.set(cx, y, cz + 1, SB)
        # Doors out onto the wall walk, where the walls meet the tower.
        for x in range(cx - 4, cx + 5):
            for z in range(cz - 4, cz + 5):
                if 2.7 < math.hypot(x - cx, z - cz) <= 3.9 and (x in (1, 2, 30, 31) or z in (1, 2, 30, 31)):
                    p.set(x, 9, z, AIR)
                    p.set(x, 10, z, AIR)
        p.set(cx + 1, 7, cz - 1, "minecraft:lantern[hanging=true]")

    # The gatehouse: two squat towers over the gate, a portcullis raised in its slot.
    for gx in (12, 18):
        p.box(gx, 0, 0, gx + 2, 11, 4, SB)
        p.box(gx + 1, 1, 1, gx + 1, 10, 3, AIR)
        for x in range(gx, gx + 3):
            for z in range(0, 5):
                if (x + z) % 2 == 0:
                    p.set(x, 12, z, SB)
    p.box(15, 0, 0, 17, 8, 4, SB)
    p.box(15, 1, 0, 17, 5, 4, AIR)
    for x in range(15, 18):
        p.set(x, 0, 0, "minecraft:dirt_path")
        p.set(x, 5, 2, "minecraft:iron_bars")
        p.set(x, 6, 2, "minecraft:iron_bars")
    p.set(15, 5, 4, "minecraft:lantern[hanging=true]")
    p.set(17, 5, 4, "minecraft:lantern[hanging=true]")
    for x in (14, 18):
        p.set(x, 4, 5, "minecraft:red_wall_banner[facing=south]")

    # The keep: three floors under a battlemented roof.
    K0, K1, KZ0, KZ1 = 9, 23, 13, 27
    p.box(K0, 0, KZ0, K1, 19, KZ1, SB)
    p.box(K0 + 1, 1, KZ0 + 1, K1 - 1, 18, KZ1 - 1, AIR)
    for x in range(K0 + 1, K1):
        for z in range(KZ0 + 1, KZ1):
            p.set(x, 0, z, "minecraft:polished_andesite" if (x + z) % 2 else "minecraft:stone_bricks")
            p.set(x, 7, z, "minecraft:dark_oak_planks")
            p.set(x, 13, z, "minecraft:spruce_planks")
    for x in range(K0, K1 + 1):
        for z in range(KZ0, KZ1 + 1):
            if (x in (K0, K1) or z in (KZ0, KZ1)) and (x + z) % 2 == 0:
                p.set(x, 20, z, SB)
    # Windows: slits of glass all round, on every floor.
    for y in (3, 9, 15):
        for k in range(K0 + 2, K1 - 1, 3):
            for yy in (y, y + 1):
                p.set(k, yy, KZ1, "minecraft:glass")
                p.set(K0, yy, k + (KZ0 - K0), "minecraft:glass")
                p.set(K1, yy, k + (KZ0 - K0), "minecraft:glass")
    # Its door, and a ladder up through the floors in the north-west corner.
    door(p, 15, 1, KZ0, "dark_oak", "north")
    door(p, 17, 1, KZ0, "dark_oak", "north", "right")
    p.set(16, 1, KZ0, AIR)
    p.set(16, 2, KZ0, AIR)
    p.set(16, 3, KZ0, "minecraft:stone_brick_stairs[facing=south,half=top,shape=straight]")
    for y in range(1, 19):
        p.set(K0 + 1, y, KZ0 + 1, "minecraft:ladder[facing=east]")

    # The great hall: a long table, benches, the lord's seat, a hearth, chandeliers, banners.
    for z in range(17, 25):
        for x in (15, 16, 17):
            p.set(x, 1, z, "minecraft:dark_oak_slab[type=top]")
        p.set(14, 1, z, "minecraft:spruce_stairs[facing=west,half=bottom,shape=straight]")
        p.set(18, 1, z, "minecraft:spruce_stairs[facing=east,half=bottom,shape=straight]")
    p.set(16, 1, 25, "minecraft:polished_blackstone_stairs[facing=south,half=bottom,shape=straight]")
    p.set(16, 2, 26, "minecraft:red_wall_banner[facing=north]")
    p.set(15, 1, 25, "minecraft:candle[candles=3,lit=true]")
    for x in (12, 20):
        p.set(x, 6, 20, "minecraft:chain[axis=y]")
        p.set(x, 5, 20, "minecraft:lantern[hanging=true]")
    p.set(16, 6, 21, "minecraft:chain[axis=y]")
    p.set(16, 5, 21, "minecraft:lantern[hanging=true]")
    # The hearth in the west wall, its chimney up through it.
    p.set(K0, 1, 20, "minecraft:campfire[lit=true,facing=east,signal_fire=false,waterlogged=false]")
    p.set(K0 + 1, 3, 20, "minecraft:stone_brick_stairs[facing=west,half=top,shape=straight]")
    for z in (19, 21):
        p.set(K0 + 1, 1, z, "minecraft:stone_brick_wall")
        p.set(K0 + 1, 2, z, "minecraft:stone_brick_wall")
    for z in (16, 24):
        p.set(K1 - 1, 3, z, "minecraft:red_wall_banner[facing=west]")
        p.set(K0 + 1, 3, z, "minecraft:blue_wall_banner[facing=east]")
    # A chest of stores under the stairs' corner.
    p.set(K1 - 1, 1, KZ0 + 1, "minecraft:chest[facing=west,type=single,waterlogged=false]")
    p.set(K1 - 2, 1, KZ0 + 1, "minecraft:barrel[facing=up,open=false]")
    p.set(K1 - 1, 1, KZ0 + 2, "minecraft:barrel[facing=up,open=false]")

    # The archive: shelves along every wall, lecterns, a reading desk and the lord's ledger.
    for x in range(K0 + 1, K1):
        for z in range(KZ0 + 1, KZ1):
            edge = x in (K0 + 1, K1 - 1) or z in (KZ0 + 1, KZ1 - 1)
            if edge and not (x == K0 + 1 and z <= KZ0 + 2) and not (x == 21 and z == KZ0 + 1):
                for y in (8, 9, 10):
                    p.set(x, y, z, "minecraft:bookshelf" if (x + z + y) % 7 else "minecraft:chiseled_bookshelf[facing=north]")
    for z in (17, 20, 23):
        for x in (13, 19):
            p.set(x, 8, z, "minecraft:bookshelf")
            p.set(x, 9, z, "minecraft:bookshelf")
    for x in (15, 16, 17):
        p.set(x, 8, 22, "minecraft:spruce_slab[type=top]")
    p.set(16, 9, 22, CG + "faded_blueprint[design=ledger,facing=south]")
    p.set(15, 9, 22, "minecraft:candle[candles=2,lit=true]")
    p.set(17, 9, 22, "minecraft:candle[candles=1,lit=true]")
    p.set(16, 8, 23, "minecraft:spruce_stairs[facing=south,half=bottom,shape=straight]")
    p.set(16, 8, 17, "minecraft:lectern[facing=north,has_book=false,powered=false]")
    p.set(K1 - 2, 8, KZ1 - 2, "minecraft:chest[facing=west,type=single,waterlogged=false]")
    p.set(16, 12, 20, "minecraft:lantern[hanging=true]")

    # The lord's chamber: his bed, his chest, a carpet; a door into the clocktower.
    for x in range(12, 21):
        for z in range(16, 25):
            p.set(x, 14, z, "minecraft:red_carpet" if 13 <= x <= 19 and 18 <= z <= 23 else AIR)
    bed(p, 16, 14, 25, "red", "south")
    p.set(14, 14, 25, "minecraft:candle[candles=1,lit=true]")
    p.set(18, 14, 25, "minecraft:barrel[facing=up,open=false]")
    p.set(20, 18, 20, "minecraft:lantern[hanging=true]")
    p.set(21, 14, KZ0, AIR)
    p.set(21, 15, KZ0, AIR)

    # The clocktower, out of the keep's front corner: a door at its foot, a ladder to the clock room, the
    # inscription in a band of carved stone, a face on every side, a bell, and the works.
    T0, T1, TZ0, TZ1 = 19, 23, 9, 13
    p.box(T0, 0, TZ0, T1, 26, TZ1, SB)
    p.box(T0 + 1, 1, TZ0 + 1, T1 - 1, 25, TZ1 - 1, AIR)
    for x in range(T0 + 1, T1):
        for z in range(TZ0 + 1, TZ1):
            p.set(x, 0, z, "minecraft:polished_andesite")
    door(p, T0, 1, 11, "spruce", "west")
    for y in range(1, 25):
        p.set(22, y, 11, "minecraft:ladder[facing=west]")
    for x in range(T0, T1 + 1):
        for z in range(TZ0, TZ1 + 1):
            if x in (T0, T1) or z in (TZ0, TZ1):
                p.set(x, 18, z, "minecraft:chiseled_stone_bricks")
    # The works: gears turning, weights on chains.
    for y in (15, 16):
        p.set(20, y, 10, "minecraft:grindstone[face=wall,facing=east]")
    p.set(22, 17, 12, "minecraft:grindstone[face=wall,facing=west]")
    for y in range(6, 18):
        p.set(20, y, 12, "minecraft:chain[axis=y]")
    p.set(20, 5, 12, "minecraft:heavy_core[waterlogged=false]")
    # The faces: a white dial ringed in gold at the hours, black hands at a quarter past ten.
    for face in range(4):
        for i in range(5):
            for j in range(5):
                ring = i in (0, 4) or j in (0, 4)
                hour = (i, j) in ((2, 0), (2, 4), (0, 2), (4, 2))
                state = "minecraft:gold_block" if hour else "minecraft:smooth_quartz" if ring else "minecraft:white_concrete"
                hand = (i == 2 and j in (1, 2)) or (j == 2 and i == 3) or (i, j) == (1, 1)
                if hand:
                    state = "minecraft:black_concrete"
                y = 24 - j
                if face == 0:
                    p.set(T0 + i, y, TZ0, state)
                elif face == 1:
                    p.set(T1, y, TZ0 + i, state)
                elif face == 2:
                    p.set(T1 - i, y, TZ1, state)
                else:
                    p.set(T0, y, TZ1 - i, state)
    p.box(T0 + 1, 22, TZ0 + 1, T1 - 1, 22, TZ0 + 1, "minecraft:stripped_dark_oak_log[axis=x]")
    p.set(21, 21, 10, "minecraft:bell[attachment=ceiling,facing=north,powered=false]")
    # Its roof: a stepped spire and a lightning rod.
    for k, y in enumerate(range(26, 29)):
        p.box(T0 + k, y, TZ0 + k, T1 - k, y, TZ1 - k, "minecraft:deepslate_tiles")
    p.set(21, 29, 11, "minecraft:lightning_rod[facing=up,powered=false,waterlogged=false]")

    # The bailey: a well, the stables, a smithy by the gate, a garden.
    p.walls(6, 1, 17, 8, 1, 19, "minecraft:cobblestone")
    p.set(7, 1, 18, WATER)
    p.set(7, 0, 18, WATER)
    for x, z in ((6, 17), (8, 17), (6, 19), (8, 19)):
        p.set(x, 2, z, "minecraft:spruce_fence")
        p.set(x, 3, z, "minecraft:spruce_fence")
    p.box(6, 4, 17, 8, 4, 19, "minecraft:spruce_slab[type=bottom]")
    for z in range(9, 26, 4):
        p.set(3, 1, z, "minecraft:hay_block[axis=y]")
        p.set(4, 1, z, "minecraft:hay_block[axis=y]")
        p.set(3, 2, z, "minecraft:hay_block[axis=y]")
        p.set(5, 1, z + 1, "minecraft:cauldron")
        for zz in (z - 1, z + 2):
            p.set(5, 1, zz, "minecraft:spruce_fence")
    p.box(3, 4, 8, 5, 4, 26, "minecraft:spruce_slab[type=bottom]")
    p.set(10, 1, 5, "minecraft:anvil[facing=north]")
    p.set(11, 1, 5, "minecraft:blast_furnace[facing=south,lit=false]")
    p.set(9, 1, 5, "minecraft:smithing_table")
    p.set(10, 1, 6, "minecraft:grindstone[face=floor,facing=north]")
    for x in range(24, 29):
        for z in range(8, 12):
            p.set(x, 1, z, "minecraft:rose_bush[half=lower]" if (x + z) % 3 == 0 else "minecraft:short_grass" if (x + z) % 3 == 1 else "minecraft:poppy")
            if (x + z) % 3 == 0:
                p.set(x, 2, z, "minecraft:rose_bush[half=upper]")
    for x, z in ((13, 7), (19, 7), (13, 11)):
        p.set(x, 1, z, "minecraft:spruce_fence")
        p.set(x, 2, z, "minecraft:spruce_fence")
        p.set(x, 3, z, "minecraft:lantern[hanging=false]")
    past = dict(p.b)
    return (W, H, D), past, castle_ruin(past, W, H, D), {
        (22, 1, 14): {"id": "minecraft:chest", "LootTable": "chronogenesis:chests/ruined_castle"},
        (21, 8, 25): {"id": "minecraft:chest", "LootTable": "chronogenesis:chests/ruined_castle_archive"}}


def castle_ruin(past, W, H, D):
    """Centuries of it: walls fallen to jagged stumps (one breached), a tower collapsed, the keep's roof and top floor
    gone, everything wooden rotted away but a few shelves, the clocktower still standing with its bell, one face fallen."""
    rng = random.Random(1347)
    p = Plan(W, H, D)
    p.b = dict(past)
    SB = "minecraft:stone_bricks"
    # Wall heights round the curtain, breached in the west.
    perimeter = [(x, 1) for x in range(1, 32)] + [(31, z) for z in range(2, 32)] + [(x, 31) for x in range(30, 0, -1)] \
        + [(1, z) for z in range(30, 1, -1)]
    heights = noise_heights(len(perimeter), rng, 3, 10)
    top = {}
    for (x, z), h in zip(perimeter, heights):
        if x == 1 and 12 <= z <= 17:
            h = rng.randint(0, 1)
        for dx in (0, 1, -1):
            for dz in (0, 1, -1):
                key = (x + dx, z + dz)
                top[key] = min(top.get(key, 99), h)
    for (x, y, z), state in list(p.b.items()):
        if (x in (1, 2, 30, 31) or z in (1, 2, 30, 31)) and (x, z) in top and y > top[(x, z)] and not (11 <= x <= 21 and z <= 5):
            if math.hypot(x - 4, z - 4) > 3.9 and math.hypot(x - 28, z - 4) > 3.9 and math.hypot(x - 4, z - 28) > 3.9 \
                    and math.hypot(x - 28, z - 28) > 3.9:
                p.b[(x, y, z)] = AIR
    # Towers: the south-west one collapsed, the others broken at different heights.
    for (cx, cz), h in (((4, 4), 11), ((28, 4), 13), ((4, 28), 3), ((28, 28), 8)):
        ring_heights = noise_heights(24, rng, h - 3, h + 1, 2)
        for (x, y, z), state in list(p.b.items()):
            d = math.hypot(x - cx, z - cz)
            if d <= 3.9 and y > 0:
                a = int((math.atan2(z - cz, x - cx) + math.pi) / (2 * math.pi) * 24) % 24
                if y > ring_heights[a]:
                    p.b[(x, y, z)] = AIR
    # The gatehouse loses its tops.
    for (x, y, z), state in list(p.b.items()):
        if z <= 5 and 11 <= x <= 21 and y > 6 + (x * 7 + z * 3) % 4:
            p.b[(x, y, z)] = AIR
    # The keep: jagged from the third floor up, its roof gone, the top floor fallen through in the middle.
    keep_heights = noise_heights(56, rng, 11, 19, 2)
    for (x, y, z), state in list(p.b.items()):
        if 9 <= x <= 23 and 13 <= z <= 27 and not (19 <= x <= 23 and z == 13):
            edge = x in (9, 23) or z in (13, 27)
            if edge:
                a = (x - 9) if z == 13 else 14 + (z - 13) if x == 23 else 28 + (23 - x) if z == 27 else 42 + (27 - z)
                if y > keep_heights[a % 56]:
                    p.b[(x, y, z)] = AIR
            elif y >= 19:
                p.b[(x, y, z)] = AIR
            elif y == 13 and math.hypot(x - 15, z - 20) < 5.5 + rng.random():
                p.b[(x, y, z)] = AIR
            elif y == 7 and math.hypot(x - 13, z - 24) < 2.2 + rng.random():
                p.b[(x, y, z)] = AIR
    # The clocktower stands, but its spire and rod are gone, the front face has fallen out and the others lost hands.
    for (x, y, z), state in list(p.b.items()):
        if 19 <= x <= 23 and 9 <= z <= 13:
            if y >= 26 + (x + z) % 2:
                p.b[(x, y, z)] = AIR
            elif z == 9 and y >= 20 and x % 4 != 3:
                p.b[(x, y, z)] = AIR
            elif state == "minecraft:black_concrete" and rng.random() < 0.5:
                p.b[(x, y, z)] = "minecraft:smooth_quartz" if rng.random() < 0.5 else AIR
            elif state == "minecraft:white_concrete" and rng.random() < 0.3:
                p.b[(x, y, z)] = "minecraft:light_gray_concrete"
    p.b[(20, 5, 12)] = AIR
    p.b[(20, 1, 12)] = "minecraft:heavy_core[waterlogged=false]"
    for y in range(6, 12):
        p.b[(20, y, 12)] = AIR
    # The fallen face's dial lies smashed at the tower's foot.
    for x in range(18, 25):
        for z in range(4, 9):
            if rng.random() < 0.35 and p.get(x, 1, z) in (AIR, None):
                p.b[(x, 1, z)] = rng.choice(["minecraft:smooth_quartz", "minecraft:white_concrete", "minecraft:gold_block",
                                             "minecraft:cobblestone", "minecraft:cracked_stone_bricks"]) if rng.random() < 0.9 else "minecraft:gold_block"
    # Rot: wood mostly gone, banners and lanterns gone, candles out, fires dead; stone aged.
    for (x, y, z), state in list(p.b.items()):
        if state in (AIR, WATER):
            continue
        if "lantern" in state or "banner" in state or "campfire" in state:
            p.b[(x, y, z)] = AIR
        elif "candle" in state:
            p.b[(x, y, z)] = state.replace("lit=true", "lit=false")
        elif "bookshelf" in state:
            p.b[(x, y, z)] = state if rng.random() < 0.35 else AIR
        elif "_bed" in state or "carpet" in state:
            p.b[(x, y, z)] = AIR if rng.random() < 0.7 else ("minecraft:brown_carpet" if "carpet" in state else AIR)
        elif state.startswith("minecraft:dark_oak_planks") or state.startswith("minecraft:spruce_planks"):
            p.b[(x, y, z)] = state if rng.random() < (0.8 if y == 7 else 0.5) else AIR
        elif is_wood(state) and "chest" not in state and "barrel" not in state and "faded_blueprint" not in state:
            if not (state.startswith("minecraft:spruce_slab") and y == 8 and z == 22):
                p.b[(x, y, z)] = AIR if rng.random() < 0.7 else state
        elif "rose_bush" in state or "poppy" in state:
            p.b[(x, y, z)] = AIR
        elif "iron_bars" in state:
            p.b[(x, y, z)] = AIR if rng.random() < 0.6 else state
        elif "glass" in state:
            p.b[(x, y, z)] = AIR
        elif state == WATER:
            pass
        else:
            p.b[(x, y, z)] = weather(state, rng)
    # Keep the ladders only where their wall still stands; doors gone.
    for (x, y, z), state in list(p.b.items()):
        if "_door" in state:
            p.b[(x, y, z)] = AIR
    # Floors that fell lie as rubble below; rubble heaps at the foot of every broken wall.
    for x in range(W):
        for z in range(D):
            ground = p.get(x, 1, z)
            if ground not in (AIR, None):
                continue
            near_wall = x in (3, 29) or z in (3, 29) or (11 <= x <= 21 and 15 <= z <= 25 and rng.random() < 0.3)
            if near_wall and rng.random() < 0.35:
                p.b[(x, 1, z)] = rng.choice(["minecraft:cobblestone", "minecraft:mossy_cobblestone", "minecraft:gravel",
                                             "minecraft:cracked_stone_bricks", "minecraft:stone_brick_slab[type=bottom]"])
            elif not (9 <= x <= 23 and 13 <= z <= 27) and rng.random() < 0.25 and p.get(x, 0, z) == "minecraft:grass_block":
                p.b[(x, 1, z)] = rng.choice(["minecraft:short_grass", "minecraft:short_grass", "minecraft:fern", "minecraft:oak_leaves[persistent=true]"])
    # The bailey grassed over.
    for x in range(W):
        for z in range(D):
            g = p.get(x, 0, z)
            if g in ("minecraft:cobblestone", "minecraft:dirt_path") and rng.random() < 0.5:
                p.b[(x, 0, z)] = rng.choice(["minecraft:grass_block", "minecraft:coarse_dirt", "minecraft:moss_block"])
    # The breach in the west wall spilled outwards.
    for z in range(11, 19):
        for x in (0, 3, 4):
            if rng.random() < 0.6 and p.get(x, 1, z) in (AIR, None):
                p.b[(x, 1, z)] = rng.choice(["minecraft:cobblestone", "minecraft:mossy_stone_bricks", "minecraft:gravel"])
    vines_on(p, rng, 0.03, solid_stone, 18)
    for _ in range(30):
        x, y, z = rng.randrange(10, 23), rng.choice([5, 6, 11, 12]), rng.randrange(14, 27)
        if p.get(x, y, z) == AIR:
            p.b[(x, y, z)] = "minecraft:cobweb"
    return p.b


# ---------------------------------------------------------------- the Abandoned Marketplace

STALLS = [
    # (x0, z0, front z, awning colours, counter goods, back goods)
    (3, 3, 5, ("cyan", "white"), ["minecraft:glass", "minecraft:tinted_glass", "minecraft:light_blue_stained_glass"],
     ["minecraft:glass", "minecraft:barrel[facing=up,open=false]", "minecraft:glass"]),
    (11, 3, 5, ("purple", "white"), ["minecraft:amethyst_cluster[facing=up,waterlogged=false]", "minecraft:large_amethyst_bud[facing=up,waterlogged=false]",
                                    "minecraft:amethyst_cluster[facing=up,waterlogged=false]"],
     ["minecraft:amethyst_block", "minecraft:barrel[facing=up,open=false]", "minecraft:amethyst_block"]),
    (19, 3, 5, ("orange", "brown"), ["minecraft:lightning_rod[facing=up,powered=false,waterlogged=false]", "minecraft:copper_block",
                                     "minecraft:chain[axis=x]"],
     ["minecraft:cut_copper", "minecraft:barrel[facing=up,open=false]", "minecraft:copper_block"]),
    (3, 19, 19, ("green", "lime"), ["minecraft:brewing_stand[has_bottle_0=true,has_bottle_1=false,has_bottle_2=true]", "minecraft:potted_fern",
                                   "minecraft:candle[candles=4,lit=true]"],
     ["minecraft:water_cauldron[level=3]", "minecraft:barrel[facing=up,open=false]", "minecraft:potted_red_mushroom"]),
    (11, 19, 19, ("yellow", "black"), ["minecraft:daylight_detector[inverted=false,power=0]", "minecraft:lantern[hanging=false]",
                                       "minecraft:daylight_detector[inverted=false,power=0]"],
     ["minecraft:redstone_lamp[lit=false]", "minecraft:barrel[facing=up,open=false]", "minecraft:target[power=0]"]),
    (19, 19, 19, ("gray", "red"), ["minecraft:cobweb", "minecraft:skeleton_skull[rotation=0]", "minecraft:bone_block[axis=y]"],
     ["minecraft:slime_block", "minecraft:barrel[facing=up,open=false]", "minecraft:honey_block"]),
]


def abandoned_marketplace():
    W, H, D = 25, 11, 25
    p = Plan(W, H, D)
    # The square: cobbles, a ring of andesite round the fountain, paths out to the stalls.
    for x in range(W):
        for z in range(D):
            d = math.hypot(x - 12, z - 12)
            p.set(x, 0, z, "minecraft:polished_andesite" if d < 5.5 else "minecraft:stone_bricks" if d < 6.6
                  else "minecraft:cobblestone" if (x * 3 + z) % 7 else "minecraft:gravel")
    # The fountain: a basin, water, a column with a lantern.
    p.walls(10, 1, 10, 14, 1, 14, "minecraft:stone_bricks")
    p.box(11, 1, 11, 13, 1, 13, WATER)
    p.box(11, 0, 11, 13, 0, 13, "minecraft:stone_bricks")
    p.box(12, 1, 12, 12, 3, 12, "minecraft:stone_brick_wall")
    p.set(12, 4, 12, "minecraft:lantern[hanging=false]")
    # Lamp posts at the corners of the square.
    for x, z in ((7, 7), (17, 7), (7, 17), (17, 17)):
        p.box(x, 1, z, x, 2, z, "minecraft:spruce_fence")
        p.set(x, 3, z, "minecraft:lantern[hanging=false]")

    # The stalls, each with its trade.
    for x0, z0, fz, (c1, c2), goods, back in STALLS:
        bz = z0 + 2 if fz == z0 else z0
        for x in (x0 - 1, x0 + 3):
            for z in (z0, z0 + 2):
                p.box(x, 1, z, x, 3, z, "minecraft:spruce_fence")
        for i in range(-1, 4):
            for k in range(3):
                p.set(x0 + i, 4, z0 + k, f"minecraft:{c1 if i % 2 == 0 else c2}_wool")
        for i in range(3):
            p.set(x0 + i, 1, fz, "minecraft:barrel[facing=up,open=false]" if i != 1 else "minecraft:spruce_planks")
            p.set(x0 + i, 2, fz, goods[i])
            if back[i] != "minecraft:barrel[facing=up,open=false]" or i == 1:
                p.set(x0 + i, 1, bz, back[i])
        # A hanging sign of sorts: a lantern under the awning's middle.
        p.set(x0 + 1, 3, z0 + 1, "minecraft:lantern[hanging=true]")

    # The merchant's house on the west side: stone footing, timber walls, a gable roof.
    X0, X1, Z0, Z1 = 0, 6, 9, 15
    p.box(X0, 0, Z0, X1, 0, Z1, "minecraft:cobblestone")
    p.walls(X0, 1, Z0, X1, 4, Z1, "minecraft:oak_planks")
    for x, z in ((X0, Z0), (X1, Z0), (X0, Z1), (X1, Z1)):
        p.box(x, 1, z, x, 4, z, "minecraft:stripped_oak_log[axis=y]")
    for k in range(4):
        for x in range(X0 - 0, X1 + 1):
            p.set(x, 5 + k, Z0 + k, "minecraft:spruce_stairs[facing=south,half=bottom,shape=straight]")
            p.set(x, 5 + k, Z1 - k, "minecraft:spruce_stairs[facing=north,half=bottom,shape=straight]")
        for z in range(Z0 + k + 1, Z1 - k):
            p.set(X0, 5 + k, z, "minecraft:oak_planks")
            p.set(X1, 5 + k, z, "minecraft:oak_planks")
    for x in range(X0, X1 + 1):
        p.set(x, 8, 12, "minecraft:spruce_slab[type=bottom]")
    door(p, X1, 1, 12, "spruce", "east")
    for z in (10, 14):
        p.set(X1, 2, z, "minecraft:glass")
        p.set(X0, 2, z, "minecraft:glass")
    # Inside: the counter, the ledger's lectern, a bed, the strongbox.
    for z in range(10, 15):
        p.set(4, 1, z, "minecraft:spruce_slab[type=top]" if z != 12 else AIR)
    p.set(4, 2, 11, "minecraft:candle[candles=2,lit=true]")
    p.set(2, 1, 10, "minecraft:lectern[facing=east,has_book=false,powered=false]")
    bed(p, 1, 1, 14, "brown", "east")
    p.set(1, 1, 10, "minecraft:chest[facing=east,type=single,waterlogged=false]")
    p.set(2, 1, 14, "minecraft:barrel[facing=up,open=false]")
    p.set(3, 4, 12, "minecraft:lantern[hanging=true]")

    # East of the square: the market bell on its post, and a cart of produce.
    p.set(21, 1, 12, "minecraft:spruce_fence")
    p.set(21, 2, 12, "minecraft:bell[attachment=floor,facing=west,powered=false]")
    for z in (9, 10):
        p.set(22, 1, z, "minecraft:spruce_fence")
    p.box(21, 2, 9, 23, 2, 10, "minecraft:spruce_slab[type=bottom]")
    p.set(21, 3, 9, "minecraft:melon")
    p.set(22, 3, 10, "minecraft:pumpkin")
    p.set(23, 3, 9, "minecraft:hay_block[axis=x]")
    for z in (14, 15):
        p.set(22, 1, z, "minecraft:hay_block[axis=z]")
    p.set(23, 1, 15, "minecraft:composter[level=4]")
    past = dict(p.b)
    return (W, H, D), past, marketplace_ruin(past, W, H, D), {
        (1, 1, 10): {"id": "minecraft:chest", "LootTable": "chronogenesis:chests/abandoned_marketplace"}}


def marketplace_ruin(past, W, H, D):
    """Decades empty: the awnings rotted to rags, the counters stripped (the amethyst and the skulls left), the
    fountain dry, the square grassing over, the house's roof fallen in. The bell still hangs."""
    rng = random.Random(1911)
    p = Plan(W, H, D)
    p.b = dict(past)
    for (x, y, z), state in list(p.b.items()):
        if state.endswith("_wool"):
            r = rng.random()
            p.b[(x, y, z)] = AIR if r < 0.55 else "minecraft:brown_wool" if r < 0.75 else "minecraft:light_gray_wool" if r < 0.9 \
                else "minecraft:cobweb"
        elif "lantern" in state:
            p.b[(x, y, z)] = AIR
        elif "candle" in state:
            p.b[(x, y, z)] = state.replace("lit=true", "lit=false")
        elif state == WATER:
            p.b[(x, y, z)] = "minecraft:moss_carpet" if rng.random() < 0.4 else "minecraft:gravel" if y == 1 and rng.random() < 0.3 else AIR
        elif y == 2 and 3 <= z <= 21 and 3 <= x <= 21 and z in (5, 19):
            # What was for sale: most of it taken or broken.
            keep = "amethyst" in state or "skull" in state or "bone" in state or "brewing" in state or "cobweb" in state
            if not keep and rng.random() < 0.8:
                p.b[(x, y, z)] = AIR
            elif "potted" in state:
                p.b[(x, y, z)] = "minecraft:flower_pot"
        elif state in ("minecraft:melon", "minecraft:pumpkin"):
            p.b[(x, y, z)] = AIR if rng.random() < 0.5 else "minecraft:carved_pumpkin[facing=west]"
        elif "spruce_fence" in state and rng.random() < 0.18:
            p.b[(x, y, z)] = AIR
        elif "_door" in state or "glass" in state and "tinted" not in state and y >= 2 and x <= 6:
            p.b[(x, y, z)] = AIR
        elif state.startswith("minecraft:spruce_stairs") and x <= 6:
            p.b[(x, y, z)] = AIR if rng.random() < 0.45 + 0.1 * (y - 5) else state
        elif state == "minecraft:oak_planks" and y >= 3 and rng.random() < 0.3:
            p.b[(x, y, z)] = AIR
        elif "_bed" in state:
            p.b[(x, y, z)] = state.replace("brown_bed", "gray_bed")
        elif state == "minecraft:water_cauldron[level=3]":
            p.b[(x, y, z)] = "minecraft:cauldron"
        elif y == 0 and state in ("minecraft:cobblestone", "minecraft:gravel", "minecraft:polished_andesite") and rng.random() < 0.35:
            p.b[(x, y, z)] = rng.choice(["minecraft:grass_block", "minecraft:grass_block", "minecraft:coarse_dirt", "minecraft:moss_block",
                                         "minecraft:dirt_path"])
        else:
            p.b[(x, y, z)] = weather(state, rng)
    # The roof's fallen timbers inside; the slab ridge mostly gone.
    for x in range(1, 6):
        for z in range(10, 15):
            if p.get(x, 1, z) in (AIR, None) and rng.random() < 0.2:
                p.b[(x, 1, z)] = rng.choice(["minecraft:spruce_slab[type=bottom]", "minecraft:oak_planks", "minecraft:cobweb"])
    # Grass and flowers through the cracks.
    for x in range(W):
        for z in range(D):
            if p.get(x, 0, z) in ("minecraft:grass_block", "minecraft:moss_block") and p.get(x, 1, z) in (AIR, None):
                r = rng.random()
                if r < 0.4:
                    p.b[(x, 1, z)] = "minecraft:short_grass"
                elif r < 0.47:
                    p.b[(x, 1, z)] = rng.choice(["minecraft:dandelion", "minecraft:oxeye_daisy", "minecraft:cornflower"])
    for _ in range(14):
        x, z = rng.randrange(1, 6), rng.randrange(10, 15)
        y = rng.choice([3, 4])
        if p.get(x, y, z) in (AIR, None):
            p.b[(x, y, z)] = "minecraft:cobweb"
    return p.b


# ---------------------------------------------------------------- the Collapsed Observatory

def collapsed_observatory():
    W, H, D = 17, 26, 17
    C = 8
    p = Plan(W, H, D)

    def ring(x, z):
        return math.hypot(x - C, z - C)

    for x in range(W):
        for z in range(D):
            d = ring(x, z)
            if d <= 7.2:
                p.set(x, 0, z, "minecraft:stone_bricks" if d > 5.6 else "minecraft:polished_andesite")
                for y in range(1, 18):
                    if d > 5.6:
                        p.set(x, y, z, "minecraft:polished_deepslate" if y in (6, 12, 17) else "minecraft:stone_bricks")
                    else:
                        p.set(x, y, z, AIR)
                if d <= 5.6:
                    p.set(x, 6, z, "minecraft:spruce_planks")
                    p.set(x, 12, z, "minecraft:spruce_planks")
    # A compass rose under the pendulum: hours in candles round it.
    for x in range(C - 2, C + 3):
        for z in range(C - 2, C + 3):
            p.set(x, 0, z, "minecraft:chiseled_stone_bricks" if (x, z) == (C, C) else "minecraft:polished_diorite"
                  if x == C or z == C else "minecraft:polished_andesite")
    for k in range(8):
        a = k * math.pi / 4
        p.set(C + round(3.4 * math.cos(a)), 1, C + round(3.4 * math.sin(a)), "minecraft:candle[candles=1,lit=true]")
    # The shaft the pendulum hangs through, railed on each floor.
    for y in (6, 12):
        for x in range(C - 1, C + 2):
            for z in range(C - 1, C + 2):
                p.set(x, y, z, AIR)
        for x in range(C - 2, C + 3):
            for z in range(C - 2, C + 3):
                if max(abs(x - C), abs(z - C)) == 2:
                    p.set(x, y + 1, z, "minecraft:spruce_fence")
    # Windows: slits all round on every floor.
    for k in range(8):
        a = k * math.pi / 4 + math.pi / 8
        for r in (6.0, 6.8):
            x, z = C + round(r * math.cos(a)), C + round(r * math.sin(a))
            for y in (3, 4, 9, 10, 14, 15):
                p.set(x, y, z, "minecraft:glass")
    # The door, south.
    p.set(C, 1, 14, AIR)
    p.set(C, 2, 14, AIR)
    door(p, C, 1, 15, "spruce", "south")
    # A spiral ramp of slabs up the inside of the wall, half a block a step.
    ramp = []
    for k in range(24):
        a = k * math.pi / 9 - math.pi / 2
        x, z = C + round(4.6 * math.cos(a)), C + round(4.6 * math.sin(a))
        y = 1 + k // 2
        ramp.append((x, y, z))
        p.set(x, y, z, "minecraft:spruce_slab[type=bottom]" if k % 2 == 0 else "minecraft:spruce_slab[type=top]")
        for head in (1, 2, 3):
            if y + head in (6, 12):
                p.set(x, y + head, z, AIR)

    # The pendulum: a brass bob on a chain from the dome's apex, just clear of the rose.
    for y in range(3, 24):
        p.set(C, y, C, "minecraft:chain[axis=y]")
    p.set(C, 2, C, CG + "brass_block")
    # Ground floor: benches and the instrument chests.
    p.set(2, 1, 8, "minecraft:barrel[facing=up,open=false]")
    p.set(3, 1, 11, "minecraft:spruce_stairs[facing=east,half=bottom,shape=straight]")
    p.set(13, 1, 7, "minecraft:chest[facing=west,type=single,waterlogged=false]")
    p.set(3, 5, 8, "minecraft:lantern[hanging=true]")
    p.set(13, 5, 8, "minecraft:lantern[hanging=true]")

    # The study: shelves, the orrery, the astronomers' desk with its star chart.
    for x in range(W):
        for z in range(D):
            d = ring(x, z)
            if 4.9 < d <= 5.6 and z < C and (x, 7, z) not in ramp and (x, 8, z) not in ramp:
                p.set(x, 7, z, "minecraft:bookshelf")
                p.set(x, 8, z, "minecraft:bookshelf")
    for x in (11, 12):
        p.set(x, 7, 11, "minecraft:spruce_slab[type=top]")
    p.set(11, 8, 11, CG + "faded_blueprint[design=pendulum,facing=north]")
    p.set(12, 8, 11, "minecraft:candle[candles=3,lit=true]")
    p.set(11, 7, 12, "minecraft:spruce_stairs[facing=south,half=bottom,shape=straight]")
    p.set(5, 7, 10, "minecraft:spruce_fence")
    p.set(5, 8, 10, "minecraft:spruce_fence")
    p.set(5, 9, 10, "minecraft:gold_block")
    p.set(6, 9, 10, "minecraft:chain[axis=x]")
    p.set(7, 9, 10, "minecraft:lapis_block")
    p.set(5, 9, 9, "minecraft:chain[axis=z]")
    p.set(5, 9, 8, "minecraft:redstone_block")
    p.set(12, 11, 5, "minecraft:lantern[hanging=true]")

    # The dome: glass on copper ribs, a slot open to the south for the telescope.
    for x in range(W):
        for z in range(D):
            for y in range(18, 25):
                d3 = math.sqrt((x - C) ** 2 + (y - 17.5) ** 2 + (z - C) ** 2)
                if 6.2 <= d3 <= 7.3 or (y == 18 and ring(x, z) <= 7.2 and ring(x, z) > 5.6):
                    angle = math.atan2(z - C, x - C)
                    rib = abs(((angle + math.pi) / (math.pi / 4)) % 1 - 0.5) > 0.38
                    slot = abs(x - C) <= 2 and z > C + 1 and y < 24
                    p.set(x, y, z, AIR if slot and y > 18 else "minecraft:cut_copper" if rib or y == 18 else "minecraft:glass")
    p.set(C, 24, C, "minecraft:copper_block")
    p.set(C, 25, C, "minecraft:lightning_rod[facing=up,powered=false,waterlogged=false]")
    # The telescope: a copper tube on its mount, eyepiece low, aimed out through the slot.
    p.set(10, 13, 9, "minecraft:polished_blackstone_wall")
    p.set(10, 14, 9, "minecraft:lightning_rod[facing=north,powered=false,waterlogged=false]")
    for (y, z, s) in ((14, 10, "minecraft:cut_copper"), (15, 11, "minecraft:cut_copper"), (16, 12, "minecraft:copper_block"),
                      (17, 13, "minecraft:copper_block"), (18, 14, "minecraft:tinted_glass")):
        p.set(10, y, z, s)
    p.set(9, 13, 8, "minecraft:spruce_stairs[facing=west,half=bottom,shape=straight]")
    # The ramp is laid last, with headroom over every step.
    for k, (x, y, z) in enumerate(ramp):
        p.set(x, y, z, "minecraft:spruce_slab[type=bottom]" if k % 2 == 0 else "minecraft:spruce_slab[type=top]")
        for head in (1, 2):
            if (x, y + head, z) not in ramp:
                p.set(x, y + head, z, AIR)
    past = dict(p.b)
    return (W, H, D), past, observatory_ruin(past, W, H, D, ramp), {
        (13, 1, 7): {"id": "minecraft:chest", "LootTable": "chronogenesis:chests/collapsed_observatory"}}


def observatory_ruin(past, W, H, D, ramp):
    """Centuries on: the dome caved in (a few ribs left, green), the wall broken jagged and low on the west, the top
    floor fallen with the telescope through to the study, the pendulum's chain snapped and its bob sunk into the
    cracked rose, gaps in the ramp. The star chart still lies on its desk."""
    rng = random.Random(1609)
    C = 8
    p = Plan(W, H, D)
    p.b = dict(past)
    heights = noise_heights(32, rng, 11, 18, 2)
    for (x, y, z), state in list(p.b.items()):
        d = math.hypot(x - C, z - C)
        a = int((math.atan2(z - C, x - C) + math.pi) / (2 * math.pi) * 32) % 32
        h = heights[a] - (6 if x < 4 else 0)
        if y >= 18:
            if state == "minecraft:cut_copper" and y <= 20 and rng.random() < 0.5:
                p.b[(x, y, z)] = "minecraft:oxidized_cut_copper"
            else:
                p.b[(x, y, z)] = AIR
        elif d > 5.6 and y > h:
            p.b[(x, y, z)] = AIR
        elif y == 12 and d <= 5.6 and (d < 4.2 or rng.random() < 0.4):
            p.b[(x, y, z)] = AIR
        elif y == 6 and d <= 5.6 and rng.random() < 0.18 and (x, y, z) not in ramp:
            p.b[(x, y, z)] = AIR
        elif (x, y, z) in ramp and rng.random() < 0.2 and y > 2:
            p.b[(x, y, z)] = AIR
    # The telescope fell through to the study and lies across it.
    for y in range(13, 19):
        for z in range(8, 15):
            if p.get(10, y, z) not in (None, AIR):
                p.b[(10, y, z)] = AIR
    for i, (x, z) in enumerate([(7, 12), (8, 11), (9, 11), (10, 10), (11, 9)]):
        p.b[(x, 7, z)] = "minecraft:oxidized_cut_copper" if i < 4 else "minecraft:oxidized_copper"
    p.b[(6, 7, 12)] = "minecraft:lightning_rod[facing=west,powered=false,waterlogged=false]"
    # The pendulum: the chain snapped, the bob sunk into the rose, the floor cracked round it.
    for y in range(2, 24):
        if p.get(C, y, C) is not None:
            p.b[(C, y, C)] = AIR
    p.b[(C, 1, C)] = CG + "brass_block"
    for x in range(C - 2, C + 3):
        for z in range(C - 2, C + 3):
            if (x, z) != (C, C) and rng.random() < 0.6:
                p.b[(x, 0, z)] = rng.choice(["minecraft:cracked_stone_bricks", "minecraft:gravel", "minecraft:cobblestone"])
    for x, z in ((C + 1, C), (C, C - 1), (C - 2, C + 1)):
        p.b[(x, 1, z)] = "minecraft:chain[axis=x]" if x != C else "minecraft:chain[axis=z]"
    # Everything else aged; candles out, a few kicked aside; most shelves gone; rubble on each floor.
    for (x, y, z), state in list(p.b.items()):
        if state in (AIR, WATER) or "faded_blueprint" in state:
            continue
        if "lantern" in state or "glass" in state and "tinted" not in state or "_door" in state:
            p.b[(x, y, z)] = AIR
        elif "candle" in state:
            p.b[(x, y, z)] = AIR if rng.random() < 0.4 else state.replace("lit=true", "lit=false")
        elif "bookshelf" in state and rng.random() < 0.6:
            p.b[(x, y, z)] = AIR
        elif "spruce_fence" in state and rng.random() < 0.5:
            p.b[(x, y, z)] = AIR
        elif state in ("minecraft:lapis_block", "minecraft:redstone_block") or "chain[axis=z]" in state and y == 9:
            p.b[(x, y, z)] = AIR
        elif state == "minecraft:chain[axis=x]" and y == 9:
            p.b[(x, y, z)] = AIR
        else:
            p.b[(x, y, z)] = weather(state, rng)
    for x in range(W):
        for z in range(D):
            d = math.hypot(x - C, z - C)
            for y in (1, 7):
                if d < 5.2 and p.get(x, y, z) == AIR and p.get(x, y - 1, z) not in (AIR, None) and rng.random() < 0.14:
                    p.b[(x, y, z)] = rng.choice(["minecraft:cobblestone", "minecraft:mossy_cobblestone", "minecraft:stone_brick_slab[type=bottom]",
                                                 "minecraft:gravel"])
    p.b[(6, 7, 11)] = "minecraft:lapis_block"
    vines_on(p, rng, 0.06, solid_stone, 16)
    return p.b


# ---------------------------------------------------------------- the Drowned Specimen House

def drowned_specimen_house():
    W, H, D = 15, 9, 13
    p = Plan(W, H, D)
    PB = "minecraft:prismarine_bricks"
    p.box(0, 0, 0, W - 1, 0, D - 1, "minecraft:dark_prismarine")
    for x in range(1, W - 1):
        for z in range(1, D - 1):
            if (x + z) % 2 == 0:
                p.set(x, 0, z, PB)
    p.walls(0, 1, 0, W - 1, 5, D - 1, PB)
    p.box(1, 1, 1, W - 2, 7, D - 2, AIR)
    for x, z in ((0, 0), (W - 1, 0), (0, D - 1), (W - 1, D - 1), (7, 0), (7, D - 1)):
        p.box(x, 1, z, x, 6, z, "minecraft:cut_copper")
    # The vaulted glass roof on copper ribs.
    for x in range(W):
        for z in range(D):
            edge = x in (0, W - 1) or z in (0, D - 1)
            y = 6 if edge or x in (1, W - 2) or z in (1, D - 2) else 7
            p.set(x, y, z, "minecraft:cut_copper" if x % 7 == 0 or edge and y == 6 and (x + z) % 4 == 0 else "minecraft:glass")
            if y == 7:
                p.set(x, 6, z, AIR)
    for x in range(W):
        for z in range(D):
            if x in (0, W - 1) or z in (0, D - 1):
                p.set(x, 6, z, "minecraft:cut_copper")
    # Portholes along the long walls.
    for x in (3, 5, 9, 11):
        p.set(x, 3, 0, "minecraft:glass")
        p.set(x, 3, D - 1, "minecraft:glass")
    # The airlock door (west) and the sea lanterns set in the floor's corners.
    p.set(0, 1, 6, "minecraft:iron_door[facing=west,half=lower,hinge=left,open=false,powered=false]")
    p.set(0, 2, 6, "minecraft:iron_door[facing=west,half=upper,hinge=left,open=false,powered=false]")
    p.set(1, 1, 5, "minecraft:stone_button[face=wall,facing=east,powered=false]")
    for x, z in ((1, 1), (W - 2, 1), (1, D - 2), (W - 2, D - 2)):
        p.set(x, 0, z, "minecraft:sea_lantern")

    # The specimen tanks along the north wall: glass fronts and dividers, water, something kept alive in each.
    for x in range(2, 13):
        for y in (1, 2, 3, 4):
            p.set(x, y, 1, PB)
        for y in (1, 2, 3):
            p.set(x, y, 3, "minecraft:glass")
            if x % 2 == 0:
                p.set(x, y, 2, "minecraft:glass")
        p.set(x, 4, 2, "minecraft:copper_grate")
        p.set(x, 4, 3, "minecraft:copper_grate")
    specimens = {
        3: ["minecraft:brain_coral_block", "minecraft:brain_coral[waterlogged=true]", WATER],
        5: ["minecraft:sea_pickle[pickles=4,waterlogged=true]", "minecraft:kelp_plant", "minecraft:kelp[age=20]"],
        7: [CG + "chronoflesh[aspect=awareness,growth=2]", WATER, WATER],
        9: ["minecraft:tube_coral_block", "minecraft:horn_coral[waterlogged=true]", WATER],
        11: ["minecraft:wet_sponge", "minecraft:sea_pickle[pickles=2,waterlogged=true]", WATER],
    }
    for x, column in specimens.items():
        for k, state in enumerate(column):
            p.set(x, 1 + k, 2, state)

    # The work floor: two Dissection Tables, jugs of essence on the counter, a brewing stand, a brass scaffold.
    p.set(5, 1, 6, CG + "dissection_table[brain=false]")
    p.set(9, 1, 6, CG + "dissection_table[brain=false]")
    p.set(5, 2, 6, "minecraft:lightning_rod[facing=up,powered=false,waterlogged=false]")
    for x in range(10, 14):
        p.set(x, 1, 8, "minecraft:prismarine_brick_slab[type=top,waterlogged=false]")
    p.set(11, 2, 8, CG + "essence_jug[fill=0]")
    p.set(12, 2, 8, CG + "essence_jug[fill=0]")
    p.set(13, 2, 8, "minecraft:brewing_stand[has_bottle_0=true,has_bottle_1=true,has_bottle_2=false]")
    p.box(12, 1, 4, 13, 3, 5, CG + "brass_armature")
    # The south wall: the desk with its notes on gills, shelves, the bunk and the chest.
    for x in (3, 4, 5):
        p.set(x, 1, D - 2, "minecraft:prismarine_brick_slab[type=top,waterlogged=false]")
    p.set(4, 2, D - 2, CG + "faded_blueprint[design=gills,facing=north]")
    p.set(3, 2, D - 2, "minecraft:candle[candles=2,lit=true,waterlogged=false]")
    p.set(4, 1, D - 3, "minecraft:spruce_stairs[facing=south,half=bottom,shape=straight,waterlogged=false]")
    for x in (7, 8, 9):
        p.set(x, 1, D - 2, "minecraft:bookshelf")
        p.set(x, 2, D - 2, "minecraft:bookshelf")
    bed(p, 12, 1, D - 2, "cyan", "south")
    p.set(13, 1, D - 3, "minecraft:chest[facing=west,type=single,waterlogged=false]")
    for x, z in ((4, 6), (10, 6), (7, 9)):
        p.set(x, 5, z, "minecraft:lantern[hanging=true,waterlogged=false]")
        p.set(x, 6, z, "minecraft:chain[axis=y,waterlogged=false]")
    past = dict(p.b)
    return (W, H, D), past, drowned_ruin(past, W, H, D), {
        (13, 1, D - 3): {"id": "minecraft:chest", "LootTable": "chronogenesis:chests/drowned_specimen_house"}}


def drowned_ruin(past, W, H, D):
    """Flooded: no air anywhere in it (that would leave a bubble in the sea), the roof and portholes mostly broken,
    the coral dead, kelp and seagrass grown in, sand drifted against the walls. The sea pickles still glow."""
    rng = random.Random(1912)
    p = Plan(W, H, D)
    p.b = dict(past)
    dead = {"minecraft:brain_coral_block": "minecraft:dead_brain_coral_block", "minecraft:tube_coral_block": "minecraft:dead_tube_coral_block",
            "minecraft:brain_coral[waterlogged=true]": "minecraft:dead_brain_coral[waterlogged=true]",
            "minecraft:horn_coral[waterlogged=true]": "minecraft:dead_horn_coral[waterlogged=true]"}
    for (x, y, z), state in list(p.b.items()):
        if state in dead:
            p.b[(x, y, z)] = dead[state]
        elif state == "minecraft:glass":
            broken = 0.7 if y >= 6 else 0.5
            p.b[(x, y, z)] = AIR if rng.random() < broken else state
        elif "lantern[hanging=true" in state or "candle" in state or "_door" in state or "button" in state:
            p.b[(x, y, z)] = AIR
        elif "chronoflesh" in state:
            p.b[(x, y, z)] = CG + "brass_armature"
        elif "essence_jug" in state and rng.random() < 0.5:
            p.b[(x, y, z)] = AIR
        elif state == "minecraft:bookshelf" and rng.random() < 0.6:
            p.b[(x, y, z)] = AIR
        elif "_bed" in state:
            p.b[(x, y, z)] = AIR
        else:
            p.b[(x, y, z)] = weather(state, rng)
    # Kelp up through the broken roof, seagrass on the floor, sand drifted in.
    for x in range(1, W - 1):
        for z in range(1, D - 1):
            if p.get(x, 1, z) == AIR:
                r = rng.random()
                if r < 0.07:
                    top = rng.randint(3, 7)
                    for y in range(1, top):
                        if p.get(x, y, z) == AIR:
                            p.b[(x, y, z)] = "minecraft:kelp_plant"
                    if p.get(x, top, z) == AIR:
                        p.b[(x, top, z)] = "minecraft:kelp[age=25]"
                elif r < 0.4:
                    p.b[(x, 1, z)] = "minecraft:seagrass"
                elif r < 0.52 and (x in (1, W - 2) or z in (1, D - 2)):
                    p.b[(x, 1, z)] = "minecraft:sand"
    # Flood it: every air inside is sea, and whatever can hold water holds it.
    for key, state in list(p.b.items()):
        if state == AIR:
            p.b[key] = WATER
        elif "waterlogged=false" in state:
            p.b[key] = state.replace("waterlogged=false", "waterlogged=true")
    return p.b


# ---------------------------------------------------------------- the Intact Laboratory

LAB_SURFACE = 14


def intact_laboratory():
    """A sealed bunker 14 blocks down (its template's y 14 is the surface), reached down a shaft hidden in a well."""
    W, H, D = 13, 19, 11
    S = LAB_SURFACE
    p = Plan(W, H, D)
    DT = "minecraft:deepslate_tiles"
    # The bunker's shell: floor, walls, ceiling; everything inside it air.
    p.box(0, 0, 0, W - 1, 6, D - 1, DT)
    p.box(1, 1, 1, W - 2, 5, D - 2, AIR)
    for x in range(1, W - 1):
        for z in range(1, D - 1):
            p.set(x, 0, z, "minecraft:polished_deepslate" if (x + z) % 2 else "minecraft:deepslate_bricks")
    for x in (0, 4, 8, 12):
        p.box(x, 1, 0, x, 5, 0, "minecraft:polished_deepslate")
        p.box(x, 1, D - 1, x, 5, D - 1, "minecraft:polished_deepslate")
    # The shaft: a ladder from the corner of the bunker up to the well, lined with stone.
    for y in range(6, S + 1):
        for x, z in ((0, 1), (2, 1), (1, 0), (1, 2)):
            p.set(x, y, z, "minecraft:stone_bricks" if y < S else "minecraft:mossy_cobblestone")
        p.set(1, y, 1, "minecraft:ladder[facing=east,waterlogged=false]")
    for y in range(1, 6):
        p.set(1, y, 1, "minecraft:ladder[facing=east,waterlogged=false]")
    # The well round its mouth, a little roof on posts.
    for x in range(0, 3):
        for z in range(0, 3):
            if (x, z) != (1, 1):
                p.set(x, S, z, "minecraft:mossy_cobblestone")
                p.set(x, S + 1, z, "minecraft:cobblestone_wall" if (x + z) % 2 else "minecraft:mossy_cobblestone")
    p.set(1, S + 1, 1, "minecraft:ladder[facing=east,waterlogged=false]")
    p.set(0, S + 1, 1, "minecraft:mossy_cobblestone")
    for x, z in ((0, 0), (2, 0), (0, 2), (2, 2)):
        p.set(x, S + 2, z, "minecraft:spruce_fence")
        p.set(x, S + 3, z, "minecraft:spruce_fence")
    p.box(0, S + 4, 0, 2, S + 4, 2, "minecraft:spruce_slab[type=bottom]")

    # The lightning conductor: a copper line from the rod by the well down through the rock to the brain's jar.
    for y in range(3, S):
        p.set(6, y, 5, "minecraft:cut_copper")
    p.set(6, S, 5, "minecraft:lightning_rod[facing=up,powered=false,waterlogged=false]")
    p.set(6, 2, 5, "minecraft:lightning_rod[facing=down,powered=false,waterlogged=false]")
    # The Chronocortex: a brain in its jar on the old table, caged in brass armature, the conductor over it.
    p.set(6, 1, 5, CG + "dissection_table[brain=true]")
    for x in (5, 7):
        for z in (4, 6):
            p.box(x, 1, z, x, 3, z, CG + "brass_armature")
    for x, z in ((5, 5), (7, 5), (6, 4), (6, 6)):
        p.set(x, 3, z, CG + "brass_armature")
    # An Awareness organism grown in its frame, east, and the scaffold for a second.
    for x in (9, 10):
        for y in (1, 2):
            for z in (1, 2):
                p.set(x, y, z, CG + "chronoflesh[aspect=awareness,growth=3]")
    p.set(9, 1, 1, CG + "organism_core[aspect=awareness,phase=awake]")
    p.box(9, 1, 4, 10, 2, 5, CG + "brass_armature")
    # The west wall: jugs on shelves, the brewing stand, the cauldron.
    for z in range(3, 9):
        p.set(1, 2, z, "minecraft:spruce_slab[type=top]")
        p.set(1, 3, z, CG + "essence_jug[fill=0]" if z % 2 else "minecraft:candle[candles=2,lit=true]")
    p.set(1, 1, 3, "minecraft:brewing_stand[has_bottle_0=true,has_bottle_1=false,has_bottle_2=true]")
    p.set(1, 1, 8, "minecraft:water_cauldron[level=2]")
    # The desk, south: blueprints still on it, the chest, a lectern.
    for x in (4, 5, 6, 7):
        p.set(x, 1, D - 2, "minecraft:spruce_slab[type=top]")
    p.set(4, 2, D - 2, CG + "faded_blueprint[design=locket,facing=north]")
    p.set(6, 2, D - 2, CG + "faded_blueprint[design=field,facing=north]")
    p.set(5, 2, D - 2, "minecraft:candle[candles=3,lit=true]")
    p.set(7, 2, D - 2, "minecraft:candle[candles=1,lit=true]")
    p.set(5, 1, D - 3, "minecraft:spruce_stairs[facing=south,half=bottom,shape=straight]")
    p.set(3, 1, D - 2, "minecraft:chest[facing=north,type=single,waterlogged=false]")
    p.set(8, 1, D - 2, "minecraft:lectern[facing=north,has_book=false,powered=false]")
    # Where the scientist slept, in the east alcove.
    bed(p, 11, 1, 8, "white", "east")
    p.set(11, 1, 6, "minecraft:barrel[facing=up,open=false]")
    # Lamps.
    for x, z in ((3, 5), (9, 7), (6, 2)):
        p.set(x, 5, z, "minecraft:lantern[hanging=true,waterlogged=false]")
    p.set(11, 5, 3, "minecraft:soul_lantern[hanging=true,waterlogged=false]")
    past = dict(p.b)
    return (W, H, D), past, intact_ruin(past, W, H, D), {
        (3, 1, D - 2): {"id": "minecraft:chest", "LootTable": "chronogenesis:chests/intact_laboratory"},
        (11, 1, 6): {"id": "minecraft:barrel", "LootTable": "chronogenesis:chests/intact_laboratory_stores"}}


def intact_ruin(past, W, H, D):
    """Sealed, so barely touched: dust and cobwebs, the organism dead in its frame, the lamps out but for one soul
    lantern still burning, the scientist's skull on the pillow. Above, the well's roof has fallen and a rotten lid
    covers the shaft; only the lightning rod in the grass beside it gives it away."""
    rng = random.Random(1888)
    S = LAB_SURFACE
    p = Plan(W, H, D)
    p.b = dict(past)
    for (x, y, z), state in list(p.b.items()):
        if y > S + 1 and ("fence" in state or "slab" in state):
            p.b[(x, y, z)] = AIR if rng.random() < 0.75 else state
        elif "minecraft:lantern" in state:
            p.b[(x, y, z)] = AIR
        elif "candle" in state:
            p.b[(x, y, z)] = state.replace("lit=true", "lit=false")
        elif "organism_core" in state:
            p.b[(x, y, z)] = CG + "organism_core[aspect=awareness,phase=necrotic]"
        elif "chronoflesh" in state:
            p.b[(x, y, z)] = state if rng.random() < 0.35 else CG + "brass_armature"
        elif state == "minecraft:water_cauldron[level=2]":
            p.b[(x, y, z)] = "minecraft:cauldron"
        elif state == "minecraft:cut_copper":
            p.b[(x, y, z)] = "minecraft:weathered_cut_copper"
        elif y < 7:
            p.b[(x, y, z)] = weather(state, rng)
    p.b[(1, S + 1, 1)] = "minecraft:spruce_trapdoor[facing=north,half=bottom,open=false,powered=false,waterlogged=false]"
    p.b[(11, 2, 8)] = "minecraft:skeleton_skull[rotation=12]"
    for _ in range(26):
        x, y, z = rng.randrange(1, W - 1), rng.randrange(2, 6), rng.randrange(1, D - 1)
        if p.get(x, y, z) == AIR:
            p.b[(x, y, z)] = "minecraft:cobweb"
    for x in range(2, W - 1):
        for z in range(2, D - 1):
            if p.get(x, 1, z) == AIR and rng.random() < 0.06:
                p.b[(x, 1, z)] = "minecraft:cobweb"
    return p.b


LOOSE = ("carpet", "candle", "lectern", "barrel", "bookshelf", "_bed", "slab[type=top]", "fence", "faded_blueprint", "short_grass",
         "fern", "dandelion", "daisy", "cornflower", "poppy", "rose_bush", "flower_pot", "potted_", "gravel", "minecraft:sand",
         "brewing_stand", "cauldron", "skull", "anvil", "grindstone[face=floor", "lantern[hanging=false", "amethyst_cluster",
         "amethyst_bud", "lightning_rod[facing=up", "daylight_detector", "chest", "heavy_core", "essence_jug", "dissection_table",
         "melon", "pumpkin", "hay_block", "bell[attachment=floor", "seagrass", "cobweb", "moss_carpet", "brain_coral[", "horn_coral[",
         "sea_pickle", "smithing_table", "blast_furnace", "composter", "target", "redstone_lamp")


def settle(blocks, keep=()):
    """Loose things left over nothing (a floor fell, a table rotted) fall away with it, until nothing hangs in the air."""
    changed = True
    while changed:
        changed = False
        for (x, y, z), state in list(blocks.items()):
            if y <= 1 or (x, y, z) in keep or state in (AIR, WATER) or not any(k in state for k in LOOSE):
                continue
            if blocks.get((x, y - 1, z)) in (AIR, WATER, None):
                blocks[(x, y, z)] = AIR
                changed = True


HANGING = ("air", "water", "vine", "chain", "lantern[hanging=true", "bell[attachment=ceiling", "ladder", "cobweb", "kelp", "seagrass",
           "glass")


def drop_floating(blocks, keep=()):
    """Whatever has nothing left holding it up to the ground (a lintel over a fallen wall, an awning whose posts
    rotted) has fallen: it goes. Hanging things, glass and anything in {@code keep} are left as they are."""
    solid = {pos for pos, state in blocks.items() if not any(k in state for k in HANGING)}
    held = {pos for pos in solid if pos[1] == 0}
    frontier = list(held)
    while frontier:
        x, y, z = frontier.pop()
        for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            n = (x + dx, y + dy, z + dz)
            if n in solid and n not in held:
                held.add(n)
                frontier.append(n)
    for pos in solid - held:
        if pos not in keep:
            blocks[pos] = AIR


def build(name):
    """(size, past, ruin, chests) for a ruin, its loose blocks settled and its chests where they were."""
    size, past, ruin, chests = BUILDERS[name]()
    keep = set(chests)
    if name == "collapsed_observatory":
        keep |= {pos for pos, state in past.items() if "spruce_slab" in state}
    drop_floating(ruin, keep)
    settle(ruin, keep)
    for pos in chests:
        below = (pos[0], pos[1] - 1, pos[2])
        if ruin.get(below) in (AIR, WATER, None):
            ruin[below] = past[below]
        state = past[pos]
        ruin[pos] = state.replace("waterlogged=false", "waterlogged=true") if name == "drowned_specimen_house" else state
    if name == "drowned_specimen_house":
        for pos, state in list(ruin.items()):
            if state == AIR:
                ruin[pos] = WATER
    return size, past, ruin, chests


BUILDERS = {
    "ruined_castle": ruined_castle,
    "abandoned_marketplace": abandoned_marketplace,
    "collapsed_observatory": collapsed_observatory,
    "drowned_specimen_house": drowned_specimen_house,
    "intact_laboratory": intact_laboratory,
}

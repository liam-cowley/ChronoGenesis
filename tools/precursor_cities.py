"""
The cities of the Precursors, one generator per kingdom, and the shrines they built round the obelisks (see
make_structures.py).

The Precursors never knew the obelisks for the Nails that hold time, nor that time could be travelled. They knew that
something was coming for them: things went missing from the world as if they had never been (a street no one could
remember building, a name no one could say), and every kingdom kept its own watch against it. Their cities stand in
the Age of the Precursors exactly where the present has their remains (worldgen/structure/precursor_*.json,
world/RemainsStructure):

  sun      the Sun Court, where the desert temples stand: the City of No Shadows. A walled square of flat white roofs
           on a grid round the Court of Noon, a great step pyramid with a throne hall and, below, the tomb they sealed
           their king in; a necropolis of mastabas; a market; the Well of Noon. They believed it came out of shadow.
  tide     the Tide Kingdom, where the ocean ruins (towns) and monuments (the capital) are: rings of stilted decks on
           the open sea round the Tidehall, a quartz rotunda over the Eye Pool; lighthouses; the Watch Pools where
           their Guardians lived, eyes that never close.
  hearth   the Hearth, where the villages are: timber-framed cottages along crooked lanes round the Anvil Hall, their
           smithies, fields and windmills; iron golems kept bright (rust was where the unmaking began, they said).
  verdant  the Verdant Kingdom, where the jungle temples stand: houses up in great trees and on stilts, rope bridges,
           terraced gardens round the Garden of Engines, a dome of glass on copper ribs where they grew machines to
           remember for them.
  (deep    the Deep Kingdom is the ancient cities, restored as they were: not generated here.)

Every city has an archive (its kingdom's mural, chests of tablets), a statue of its king, market stalls, cellars,
its townsfolk (who know their kingdom, NBT "Kingdom") and its guards (scoreboard tag chronogenesis.guard).
A template's ground is at y = G (the sea's surface for the Tide Kingdom); its bottom layer is carried down to the
ground (foundations).
"""
import math
import random

AIR = "minecraft:air"
CG = "chronogenesis:"
G = 4             # the ground's y in every land city template (cellars below)
TIDE_G = 8        # a Tide city's deck: piers below it, down into the sea
AIR_TOP = 7       # streets are cleared up to G + AIR_TOP
GUARD = {"Tags": ["chronogenesis.guard"]}
LEAVES = "minecraft:jungle_leaves[distance=7,persistent=true,waterlogged=false]"


def stairs(block, facing, half="bottom", shape="straight"):
    return f"{block}[facing={facing},half={half},shape={shape},waterlogged=false]"


def slab(block, kind="bottom"):
    return f"{block}[type={kind},waterlogged=false]"


def wall_block(block):
    return f"{block}[east=none,north=none,south=none,up=true,waterlogged=false,west=none]"


def lantern(block="minecraft:lantern", hanging=False):
    return f"{block}[hanging={'true' if hanging else 'false'},waterlogged=false]"


def vine(face):
    return "minecraft:vine[" + ",".join(f"{s}={'true' if s == face else 'false'}" for s in ("east", "north", "south", "up", "west")) + "]"


CHEST = "minecraft:chest[facing={},type=single,waterlogged=false]"

PAL = {
    "sun": {
        "pave": "minecraft:smooth_sandstone", "street": "minecraft:cut_red_sandstone", "wall": "minecraft:cut_sandstone",
        "found": "minecraft:sandstone", "trim": "minecraft:gold_block", "pillar": "minecraft:chiseled_sandstone",
        "accent": "minecraft:blue_terracotta", "lamp": lantern(),
        "canopy": ["minecraft:orange_wool", "minecraft:yellow_wool", "minecraft:white_wool", "minecraft:red_wool"],
    },
    "tide": {
        "pave": "minecraft:calcite", "street": "minecraft:polished_diorite", "wall": "minecraft:smooth_quartz",
        "found": "minecraft:prismarine_bricks", "trim": "minecraft:prismarine_bricks", "pillar": "minecraft:quartz_pillar[axis=y]",
        "accent": "minecraft:light_blue_terracotta", "lamp": "minecraft:sea_lantern",
        "canopy": ["minecraft:cyan_wool", "minecraft:light_blue_wool", "minecraft:white_wool", "minecraft:blue_wool"],
    },
    "hearth": {
        "pave": "minecraft:packed_mud", "street": "minecraft:cobblestone", "wall": "minecraft:stone_bricks",
        "found": "minecraft:cobblestone", "trim": "minecraft:iron_block", "pillar": "minecraft:stripped_spruce_log[axis=y]",
        "accent": "minecraft:polished_andesite", "lamp": lantern(),
        "canopy": ["minecraft:red_wool", "minecraft:brown_wool", "minecraft:white_wool", "minecraft:orange_wool"],
    },
    "verdant": {
        "pave": "minecraft:moss_block", "street": "minecraft:mossy_cobblestone", "wall": "minecraft:mossy_stone_bricks",
        "found": "minecraft:stone_bricks", "trim": "minecraft:waxed_cut_copper", "pillar": "minecraft:stripped_jungle_log[axis=y]",
        "accent": "minecraft:chiseled_stone_bricks", "lamp": "minecraft:shroomlight",
        "canopy": ["minecraft:lime_wool", "minecraft:green_wool", "minecraft:yellow_wool", "minecraft:brown_wool"],
    },
}

# Murals: each kingdom's watch against the thing that came. In every one, somewhere, the eye.
MURAL_COLOURS = {
    "k": "minecraft:black_concrete", "w": "minecraft:white_terracotta", "y": "minecraft:yellow_terracotta",
    "o": "minecraft:orange_terracotta", "g": "minecraft:gold_block", "s": "minecraft:sand", "b": "minecraft:blue_terracotta",
    "c": "minecraft:cyan_terracotta", "l": "minecraft:light_blue_concrete", "i": "minecraft:iron_block",
    "p": "minecraft:purple_concrete", "e": "minecraft:lime_concrete", "d": "minecraft:gray_terracotta",
    "m": "minecraft:magenta_terracotta", "n": "minecraft:green_terracotta", "r": "minecraft:red_terracotta",
    "u": "minecraft:brown_terracotta", "a": "minecraft:light_gray_concrete", ".": None,
}
MURALS = {
    # The sun over the city; its houses without shadows; at the edge, a shadow with an eye in it.
    "sun": ["...yyy.......",
            "..yyyyy......",
            "...yyy.......",
            ".............",
            ".www.www..kk.",
            ".www.www.kek.",
            "sssssssssskks"],
    # The rings of the city on the sea; a guardian's open eye; beneath, the dark with an eye in it.
    "tide": ["...ccccc.....",
             "..cwwwwwc.ll.",
             "..cwccwwc.lel",
             "...ccccc..ll.",
             "lllllllllllll",
             "bbbbbkkkbbbbb",
             "bbbbkkekkbbbb"],
    # A golem of iron between the houses and a figure with no face: an eye where its face should be.
    "hearth": [".......kkk...",
               "..ii...kek...",
               ".iiii...k....",
               "..ii...kkk...",
               "..ii..k.k.k..",
               ".i..i...k....",
               "nnnnnnnnnnnnn"],
    # The engines remembering: a brass frame grown with flesh, holding a house; above it, the eye.
    "verdant": [".....kkk.....",
                "....kmemk....",
                ".....kkk.....",
                "..mmmmmmmmm..",
                "..m.wwwww.m..",
                "..m.wrrrw.m..",
                "nnnnnnnnnnnnn"],
}


class City:
    G = G
    H = 48

    def __init__(self, kingdom, size, seed):
        self.k = kingdom
        self.n = size
        self.c = size // 2
        self.R = self.c - 1
        self.p = PAL[kingdom]
        self.r = random.Random(seed)
        self.b = {}
        self.nbt = {}
        self.entities = []
        self.taken = set()        # columns already built on
        self.road = set()         # columns that are street
        self.floors = []          # (x, y, z): floor spots for townsfolk inside

    # ---- basics
    def inside(self, x, z):
        return 0 <= x < self.n and 0 <= z < self.n and self.shape(x, z)

    def shape(self, x, z):
        return True

    def set(self, x, y, z, state):
        if 0 <= x < self.n and 0 <= z < self.n and 0 <= y < self.H and state is not None:
            self.b[(x, y, z)] = state

    def get(self, x, y, z):
        return self.b.get((x, y, z), AIR)

    def fill(self, x0, y0, z0, x1, y1, z1, state):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.set(x, y, z, state)

    def dist(self, x, z):
        return math.hypot(x - self.c, z - self.c)

    def free(self, x0, z0, x1, z1, margin=1):
        """Inside the city, clear of streets, and at least {margin} from anything built."""
        for x in range(x0 - margin, x1 + margin + 1):
            for z in range(z0 - margin, z1 + margin + 1):
                if (x, z) in self.taken:
                    return False
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if not self.inside(x, z) or (x, z) in self.road:
                    return False
        return True

    def claim(self, x0, z0, x1, z1):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for z in range(min(z0, z1), max(z0, z1) + 1):
                self.taken.add((x, z))

    def chest(self, x, y, z, facing, table):
        self.set(x, y, z, CHEST.format(facing))
        self.nbt[(x, y, z)] = {"id": "minecraft:chest", "LootTable": "chronogenesis:chests/" + table}

    # ---- the ground: foundations, paving, streets, air above
    def lay_ground(self):
        g = self.G
        for x in range(self.n):
            for z in range(self.n):
                if not self.inside(x, z):
                    continue
                for y in range(0, g):
                    self.set(x, y, z, self.p["found"])
                self.set(x, g, z, self.p["street"] if (x, z) in self.road else self.p["pave"])
                for y in range(g + 1, g + AIR_TOP + 1):
                    self.set(x, y, z, AIR)

    def path(self, points, width):
        """A street through a list of points (straight segments)."""
        for (x0, z0), (x1, z1) in zip(points, points[1:]):
            steps = int(max(abs(x1 - x0), abs(z1 - z0))) + 1
            for i in range(steps + 1):
                t = i / max(1, steps)
                x, z = round(x0 + (x1 - x0) * t), round(z0 + (z1 - z0) * t)
                for dx in range(-(width // 2), width - width // 2):
                    for dz in range(-(width // 2), width - width // 2):
                        if self.inside(x + dx, z + dz):
                            self.road.add((x + dx, z + dz))

    def ring(self, cx, cz, radius, width):
        steps = int(2 * math.pi * radius * 1.5) + 8
        self.path([(cx + math.cos(2 * math.pi * i / steps) * radius, cz + math.sin(2 * math.pi * i / steps) * radius)
                   for i in range(steps + 1)], width)

    def door_side(self, x0, z0, x1, z1):
        """The side of a rectangle nearest a street: (door x, door z, facing out)."""
        sides = [((x0 + x1) // 2, z0, "north", (0, -1)), ((x0 + x1) // 2, z1, "south", (0, 1)),
                 (x0, (z0 + z1) // 2, "west", (-1, 0)), (x1, (z0 + z1) // 2, "east", (1, 0))]

        def reach(side):
            sx, sz, _, (ox, oz) = side
            return next((k for k in range(1, 8) if (sx + ox * k, sz + oz * k) in self.road), 99)
        sides.sort(key=reach)
        return sides[0][0], sides[0][1], sides[0][2]

    # ---- furniture and things
    def furnish(self, x0, z0, x1, z1, y, things, chance=0.7, avoid=()):
        for (fx, fz) in [(x0 + 1, z0 + 1), (x1 - 1, z0 + 1), (x0 + 1, z1 - 1), (x1 - 1, z1 - 1)]:
            if self.r.random() < chance and (fx, fz) not in avoid and self.get(fx, y, fz) == AIR:
                self.set(fx, y, fz, self.r.choice(things))

    def cellar(self, x0, z0, x1, z1, gy):
        """A cellar under a house floor: a trapdoor by the west wall, a ladder down, barrels and the household's chest."""
        if x1 - x0 < 4 or z1 - z0 < 4 or gy < 4:
            return
        self.fill(x0 + 1, gy - 3, z0 + 1, x1 - 1, gy - 1, z1 - 1, AIR)
        hx, hz = x0 + 1, z0 + 2
        self.set(hx, gy, hz, "minecraft:spruce_trapdoor[facing=east,half=top,open=false,powered=false,waterlogged=false]")
        for y in range(gy - 3, gy):
            self.set(hx, y, hz, "minecraft:ladder[facing=east,waterlogged=false]")
        self.set(x1 - 1, gy - 3, z1 - 1, "minecraft:barrel[facing=up,open=false]")
        self.set(x0 + 1, gy - 3, z1 - 1, "minecraft:barrel[facing=up,open=false]")
        self.chest(x1 - 1, gy - 3, z0 + 1, "west", "precursor_house_" + self.k)
        self.set((x0 + x1) // 2, gy - 1, (z0 + z1) // 2, "minecraft:sea_lantern" if self.k == "tide" else lantern(hanging=True))

    def stall(self, x, z):
        if not self.free(x - 1, z - 1, x + 1, z + 1, 0):
            return
        gy = self.G
        canopy = self.r.choice(self.p["canopy"])
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                self.set(x + dx, gy + 3, z + dz, canopy)
        for dx, dz in [(-1, -1), (-1, 1), (1, -1), (1, 1)]:
            self.fill(x + dx, gy + 1, z + dz, x + dx, gy + 2, z + dz, "minecraft:oak_fence")
        goods = {"sun": ["minecraft:hay_block[axis=y]", "minecraft:melon", "minecraft:decorated_pot[facing=north,waterlogged=false]",
                         "minecraft:barrel[facing=up,open=true]"],
                 "tide": ["minecraft:barrel[facing=up,open=true]", "minecraft:dried_kelp_block", "minecraft:sponge",
                          "minecraft:decorated_pot[facing=north,waterlogged=false]"],
                 "hearth": ["minecraft:hay_block[axis=y]", "minecraft:pumpkin", "minecraft:anvil[facing=north]",
                            "minecraft:barrel[facing=up,open=true]", "minecraft:smithing_table"],
                 "verdant": ["minecraft:melon", "minecraft:moss_block", "minecraft:barrel[facing=up,open=true]",
                             "minecraft:flowering_azalea"]}[self.k]
        self.set(x, gy + 1, z, self.r.choice(goods))
        self.claim(x - 1, z - 1, x + 1, z + 1)

    def lamp(self, x, z, height=2):
        gy = self.G
        self.fill(x, gy + 1, z, x, gy + height, z, "minecraft:spruce_fence" if self.k == "hearth" else self.p["pillar"])
        self.set(x, gy + height + 1, z, self.p["lamp"])

    def mural(self, x0, y0, z):
        """The kingdom's mural on a wall running along x at z, bottom-left at (x0, y0)."""
        rows = MURALS[self.k]
        for row, line in enumerate(rows):
            for col, ch in enumerate(line):
                state = MURAL_COLOURS[ch]
                if state:
                    self.set(x0 + col, y0 + len(rows) - 1 - row, z, state)

    def archive(self, x0, z0):
        """The Hall of Records: the mural on its back wall, lecterns, shelves, chests of tablets. Built at (x0, z0),
        or the nearest free spot."""
        w, d = 15, 11
        for ring in range(0, self.n):
            spots = [(x0 + dx, z0 + dz) for dx in range(-ring, ring + 1) for dz in range(-ring, ring + 1)
                     if max(abs(dx), abs(dz)) == ring]
            spots = [(x, z) for x, z in spots if self.free(x, z, x + w - 1, z + d - 1, 0)]
            if spots:
                x0, z0 = spots[0]
                break
        else:
            return False
        gy = self.G
        x1, z1 = x0 + w - 1, z0 + d - 1
        wall = self.p["wall"]
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                edge = x in (x0, x1) or z in (z0, z1)
                corner = x in (x0, x1) and z in (z0, z1)
                for y in range(gy + 1, gy + 9):
                    self.set(x, y, z, (self.p["pillar"] if corner else wall) if edge else AIR)
                self.set(x, gy, z, self.p["accent"] if (x + z) % 2 else self.p["pave"])
                self.set(x, gy + 9, z, self.p["trim"] if edge else wall)
        dx = (x0 + x1) // 2
        self.fill(dx - 1, gy + 1, z0, dx + 1, gy + 3, z0, AIR)
        for x in range(x0 + 2, x1 - 1, 3):
            self.set(x, gy + 6, z0, "minecraft:glass_pane")
            self.set(x, gy + 6, z1, "minecraft:glass_pane")
        self.mural(x0 + 1, gy + 1, z1 - 1)
        for x in (x0 + 2, x1 - 2):
            self.chest(x, gy + 1, z0 + 2, "south", "precursor_archive_" + self.k)
        for x in (x0 + 4, x0 + 7, x0 + 10):
            self.set(x, gy + 1, z0 + 5, "minecraft:lectern[facing=south,has_book=false,powered=false]")
        for z in range(z0 + 2, z1 - 1):
            self.fill(x0 + 1, gy + 1, z, x0 + 1, gy + 2, z, "minecraft:bookshelf")
            self.set(x1 - 1, gy + 1, z, "minecraft:bookshelf")
        for x, z in [(x0 + 4, z0 + 8), (x1 - 4, z0 + 8)]:
            self.set(x, gy + 1, z, "minecraft:candle[candles=4,lit=true,waterlogged=false]")
        self.set(dx, gy + 8, (z0 + z1) // 2, lantern(hanging=True) if "lantern" in self.p["lamp"] else self.p["lamp"])
        self.floors.append((dx, gy + 1, z0 + 7))
        self.claim(x0, z0, x1, z1)
        return True

    def statue(self, x, z):
        """A king of this kingdom on a plinth, a staff of black stone in hand."""
        gy = self.G
        p = self.p
        self.fill(x - 1, gy + 1, z - 1, x + 1, gy + 1, z + 1, p["accent"])
        self.fill(x, gy + 2, z, x, gy + 4, z, p["wall"])
        self.set(x - 1, gy + 4, z, p["wall"])
        self.set(x + 1, gy + 4, z, p["wall"])
        self.set(x, gy + 5, z, "minecraft:iron_block" if self.k == "hearth" else "minecraft:gold_block")
        self.set(x, gy + 6, z, {"sun": "minecraft:gold_block", "tide": "minecraft:prismarine_bricks",
                                "hearth": "minecraft:iron_block", "verdant": "minecraft:moss_block"}[self.k])
        self.fill(x + 1, gy + 2, z - 1, x + 1, gy + 6, z - 1, wall_block("minecraft:polished_blackstone_wall"))
        self.claim(x - 1, z - 1, x + 1, z + 1)

    def palm(self, x, z, h=None):
        gy = self.G
        h = h or self.r.randint(5, 8)
        self.fill(x, gy + 1, z, x, gy + h, z, "minecraft:stripped_jungle_log[axis=y]")
        for dx, dz in [(1, 0), (-1, 0), (0, 1), (0, -1), (2, 0), (-2, 0), (0, 2), (0, -2), (1, 1), (-1, -1), (1, -1), (-1, 1)]:
            self.set(x + dx, gy + h + (0 if abs(dx) + abs(dz) < 2 else -1), z + dz, LEAVES)
        self.set(x, gy + h + 1, z, LEAVES)
        self.claim(x, z, x, z)

    def big_tree(self, x, z, h, radius):
        """A broad tree: a trunk two wide, a crown of leaves, vines."""
        gy = self.G
        for dx in (0, 1):
            for dz in (0, 1):
                self.fill(x + dx, gy + 1, z + dz, x + dx, gy + h, z + dz, "minecraft:jungle_log[axis=y]")
        for y in range(gy + h - 3, gy + h + 4):
            rr = radius - abs(y - (gy + h)) * 0.8
            for dx in range(-radius, radius + 2):
                for dz in range(-radius, radius + 2):
                    if math.hypot(dx - 0.5, dz - 0.5) <= rr and self.get(x + dx, y, z + dz) == AIR and self.r.random() < 0.92:
                        self.set(x + dx, y, z + dz, LEAVES)
        for dx, dz, f in [(-1, 0, "east"), (2, 0, "west"), (0, -1, "south"), (0, 2, "north")]:
            for dy in range(h - 6, h - 1):
                if self.r.random() < 0.35:
                    self.set(x + dx, gy + dy, z + dz, vine(f))
        self.claim(x - 1, z - 1, x + 2, z + 2)

    def fill_lots(self, attempts, sizes, house, gap=2):
        """Houses along the streets, each a step back ({gap}) from a street."""
        roads = sorted(self.road)
        for _ in range(attempts):
            w, d = self.r.randint(*sizes), self.r.randint(*sizes)
            rx, rz = self.r.choice(roads)
            side = self.r.randrange(4)
            if side == 0:
                x0, z0 = rx - self.r.randint(0, w - 1), rz + gap
            elif side == 1:
                x0, z0 = rx - self.r.randint(0, w - 1), rz - d - gap + 1
            elif side == 2:
                x0, z0 = rx + gap, rz - self.r.randint(0, d - 1)
            else:
                x0, z0 = rx - w - gap + 1, rz - self.r.randint(0, d - 1)
            if self.free(x0, z0, x0 + w - 1, z0 + d - 1, margin=1):
                house(x0, z0, w, d)

    def townsfolk(self, count):
        spots = [(x, z) for (x, z) in self.road if (x, z) not in self.taken]
        self.r.shuffle(spots)
        inside = list(self.floors)
        self.r.shuffle(inside)
        chosen = [(x, self.G + 1, z) for x, z in spots[:count - count // 3]] + inside[:count // 3]
        for x, y, z in chosen:
            while self.get(x, y, z) != AIR and y < self.H - 3:
                y += 1
            self.entities.append(((x + 0.5, float(y), z + 0.5), (x, y, z), "chronogenesis:precursor", {"Kingdom": self.k}))

    def guards(self, entity, spots, tags=True):
        for x, y, z in spots:
            self.entities.append(((x + 0.5, float(y), z + 0.5), (x, y, z), entity, GUARD if tags else {}))


# ---------------------------------------------------------------- the Sun Court: the City of No Shadows

class SunCity(City):
    H = 52

    def build(self):
        n, c, r = self.n, self.c, self.r
        p = self.p
        # Streets: the Processional Way (north-south, wide), the cross avenue, a grid of lanes every 16; the common
        # quarter (south-west) a tighter maze of alleys every 9.
        for x in range(3, n - 3):
            for z in range(3, n - 3):
                common = x < c - 4 and z > c + 3
                if abs(x - c) <= 3 or abs(z - c) <= 2:
                    self.road.add((x, z))
                elif common and ((x - c) % 9 == 0 or (z - c) % 9 == 0):
                    self.road.add((x, z))
                elif not common and ((x - c) % 16 in (0, 1) or (z - c) % 16 in (0, 1)):
                    self.road.add((x, z))
        self.lay_ground()
        # The wall: sandstone, gold-capped, a gate in each side; towers at the corners and by the gates.
        for x in range(n):
            for z in range(n):
                if min(x, z, n - 1 - x, n - 1 - z) <= 1:
                    gate = abs(x - c) <= 3 or abs(z - c) <= 2
                    for y in range(G + 1, G + 9):
                        if not (gate and y <= G + 6):
                            self.set(x, y, z, p["wall"] if y < G + 8 else p["trim"])
                    self.taken.add((x, z))
        for tx, tz in [(3, 3), (n - 4, 3), (3, n - 4), (n - 4, n - 4), (c - 7, 3), (c + 7, 3), (c - 7, n - 4), (c + 7, n - 4)]:
            self.tower(tx, tz)
        # The Court of Noon, the throne hall in it, the tomb below.
        self.pyramid(c, c - 10, 22)
        # The Processional Way: pylons tipped with gold, all the way to the south gate.
        for z in range(c + 16, n - 7, 7):
            for x in (c - 5, c + 5):
                self.fill(x, G + 1, z, x, G + 5, z, p["pillar"])
                self.set(x, G + 6, z, "minecraft:gold_block")
                self.taken.add((x, z))
        self.well(c + 34, c + 30)
        self.market(c - 36, c - 36)
        # The necropolis (south-east): mastabas, each with a sealed burial chamber.
        for mx in range(c + 14, n - 10, 14):
            for mz in range(c + 44, n - 9, 12):
                self.mastaba(mx, mz)
        self.archive(c + 24, c - 50)
        for x in (c - 9, c, c + 9):
            self.statue(x, c + 18)
        # Gardens of palms and wheat on the east, round water channels.
        for _ in range(40):
            x0, z0 = r.randint(c + 12, n - 14), r.randint(8, c - 12)
            if self.free(x0, z0, x0 + 6, z0 + 6):
                for x in range(x0, x0 + 7):
                    for z in range(z0, z0 + 7):
                        self.set(x, G, z, "minecraft:water" if x - x0 == 3 else "minecraft:farmland[moisture=7]")
                        if x - x0 != 3:
                            self.set(x, G + 1, z, "minecraft:wheat[age=7]")
                self.claim(x0, z0, x0 + 6, z0 + 6)
                if self.free(x0 - 1, z0 - 1, x0 - 1, z0 - 1, 0):
                    self.palm(x0 - 1, z0 - 1)
        self.fill_lots(1800, (9, 13), self.villa)
        self.fill_lots(2400, (5, 8), self.house)
        for x in range(8, n - 8, 9):
            for z in (c - 3, c + 3):
                if (x, z) not in self.taken and abs(x - c) > 4:
                    self.lamp(x, z)
                    self.taken.add((x, z))
        self.townsfolk(48)
        return self

    def tower(self, tx, tz):
        p = self.p
        for x in range(tx - 2, tx + 3):
            for z in range(tz - 2, tz + 3):
                edge = max(abs(x - tx), abs(z - tz)) == 2
                for y in range(G + 1, G + 14):
                    self.set(x, y, z, (p["accent"] if y % 4 == 0 else p["wall"]) if edge else AIR)
                self.set(x, G + 14, z, p["pave"])
                if edge:
                    self.set(x, G + 15, z, slab("minecraft:smooth_sandstone_slab"))
        for y in range(G + 2, G + 15):
            self.set(tx, y, tz - 1, "minecraft:ladder[facing=south,waterlogged=false]")
        self.set(tx, G + 14, tz - 1, AIR)
        self.fill(tx, G + 1, tz + 2, tx, G + 2, tz + 2, AIR)
        self.claim(tx - 2, tz - 2, tx + 2, tz + 2)

    def house(self, x0, z0, w, d):
        """A common house: a white box, a flat roof with a parapet and an awning on fence posts (they slept up there,
        in the last of the light); small high windows; no overhang anywhere to throw a shadow."""
        p = self.p
        x1, z1 = x0 + w - 1, z0 + d - 1
        gy = G
        floors = self.r.choice((1, 1, 2))
        top = gy + 4 * floors
        material = self.r.choice(["minecraft:white_terracotta", "minecraft:smooth_sandstone", "minecraft:cut_sandstone",
                                  "minecraft:white_concrete"])
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                edge = x in (x0, x1) or z in (z0, z1)
                for y in range(gy + 1, top + 1):
                    if edge:
                        window = (y - gy) % 4 == 3 and (x + z) % 3 == 0 and not (x in (x0, x1) and z in (z0, z1))
                        self.set(x, y, z, "minecraft:white_stained_glass_pane" if window else material)
                    else:
                        self.set(x, y, z, slab("minecraft:smooth_sandstone_slab") if (y - gy) % 4 == 0 else AIR)
                self.set(x, top + 1, z, p["pave"])
                if edge:
                    self.set(x, top + 2, z, slab("minecraft:smooth_sandstone_slab"))
        dx, dz, facing = self.door_side(x0, z0, x1, z1)
        self.fill(dx, gy + 1, dz, dx, gy + 2, dz, AIR)
        canopy = self.r.choice(p["canopy"])
        for x in range(x0 + 1, min(x0 + 4, x1)):
            for z in range(z0 + 1, min(z0 + 4, z1)):
                self.set(x, top + 4, z, canopy)
        for x, z in [(x0 + 1, z0 + 1), (min(x0 + 3, x1 - 1), min(z0 + 3, z1 - 1))]:
            self.fill(x, top + 2, z, x, top + 3, z, "minecraft:oak_fence")
        self.set(x1 - 1, top + 2, z1 - 1, "minecraft:white_bed[facing=north,occupied=false,part=foot]")
        # A ladder up through a hatch to the roof.
        self.fill(x1 - 1, gy + 1, z0 + 1, x1 - 1, top, z0 + 1, "minecraft:ladder[facing=west,waterlogged=false]")
        self.set(x1 - 1, top + 1, z0 + 1, "minecraft:birch_trapdoor[facing=west,half=top,open=false,powered=false,waterlogged=false]")
        things = ["minecraft:decorated_pot[facing=north,waterlogged=false]", "minecraft:barrel[facing=up,open=false]",
                  "minecraft:loom[facing=north]", "minecraft:candle[candles=2,lit=true,waterlogged=false]",
                  "minecraft:flower_pot", "minecraft:cauldron"]
        self.furnish(x0, z0, x1, z1, gy + 1, things, avoid=[(dx, dz)])
        self.set((x0 + x1) // 2, top, (z0 + z1) // 2, lantern(hanging=True))
        self.floors.append(((x0 + x1) // 2, gy + 1, (z0 + z1) // 2))
        if self.r.random() < 0.3:
            self.cellar(x0, z0, x1, z1, gy)
        self.claim(x0, z0, x1, z1)

    def villa(self, x0, z0, w, d):
        """A noble's villa: rooms round an open courtyard with a pool and a palm (nothing roofs it: no shade)."""
        if w < 9 or d < 9:
            return self.house(x0, z0, w, d)
        p = self.p
        x1, z1 = x0 + w - 1, z0 + d - 1
        gy = G
        top = gy + 5
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                court = x0 + 3 < x < x1 - 3 and z0 + 3 < z < z1 - 3
                court_wall = (x in (x0 + 3, x1 - 3) and z0 + 3 <= z <= z1 - 3) or (z in (z0 + 3, z1 - 3) and x0 + 3 <= x <= x1 - 3)
                edge = x in (x0, x1) or z in (z0, z1)
                for y in range(gy + 1, top + 1):
                    if court:
                        self.set(x, y, z, AIR)
                    elif court_wall:
                        column = (x + z) % 2 == 0
                        self.set(x, y, z, p["pillar"] if column else (AIR if y <= gy + 3 else p["wall"]))
                    elif edge:
                        window = y == gy + 3 and (x + z) % 4 == 1 and not (x in (x0, x1) and z in (z0, z1))
                        self.set(x, y, z, "minecraft:white_stained_glass_pane" if window else p["wall"])
                    else:
                        self.set(x, y, z, AIR)
                if court:
                    pool = x0 + 5 <= x <= x1 - 5 and z0 + 5 <= z <= z1 - 5
                    self.set(x, gy, z, "minecraft:water" if pool else p["accent"])
                else:
                    self.set(x, top + 1, z, p["pave"])
                    if edge:
                        self.set(x, top + 2, z, "minecraft:gold_block" if (x + z) % 6 == 0 else slab("minecraft:smooth_sandstone_slab"))
        self.palm(x0 + 4, z0 + 4, 6)
        dx, dz, facing = self.door_side(x0, z0, x1, z1)
        self.fill(dx, gy + 1, dz, dx, gy + 3, dz, AIR)
        self.set(x0 + 1, gy + 1, z0 + 1, "minecraft:yellow_bed[facing=north,occupied=false,part=foot]")
        self.set(x1 - 1, gy + 1, z1 - 1, "minecraft:yellow_bed[facing=north,occupied=false,part=foot]")
        self.chest(x1 - 1, gy + 1, z0 + 1, "south", "precursor_house_sun")
        self.set(x0 + 1, gy + 1, z1 - 1, "minecraft:decorated_pot[facing=north,waterlogged=false]")
        self.set(x0 + 2, gy + 1, z1 - 1, "minecraft:loom[facing=north]")
        for x, z in [(x0 + 1, z0 + 2), (x1 - 1, z1 - 2)]:
            self.set(x, top, z, lantern(hanging=True))
        self.floors.append((x0 + 1, gy + 1, (z0 + z1) // 2))
        self.claim(x0, z0, x1, z1)

    def pyramid(self, cx, cz, half):
        """The Court of Noon: steps of two, gold-rimmed, a stair up its south face to the sun disc. Inside, the throne
        hall (pillars, the gold throne, the mural, the treasury); under it, down a hidden stair, the tomb they sealed
        their king in when the shadow took his face, over a bed of TNT (to keep him in)."""
        p = self.p
        y = G + 1
        h = half
        while h >= 2:
            for x in range(cx - h, cx + h + 1):
                for z in range(cz - h, cz + h + 1):
                    rim = max(abs(x - cx), abs(z - cz)) == h
                    self.set(x, y, z, p["trim"] if rim and h % 4 == 2 else p["wall"])
                    self.set(x, y + 1, z, p["accent"] if rim and h % 6 == 0 else p["wall"])
            for dy in range(0, 2):
                for off in range(-2, 3):
                    self.set(cx + off, y + dy, cz + h, stairs("minecraft:sandstone_stairs", "north"))
            y += 2
            h -= 2
        for dx, dz in [(-1, -1), (-1, 1), (1, -1), (1, 1)]:
            self.fill(cx + dx, y, cz + dz, cx + dx, y + 3, cz + dz, p["pillar"])
        self.fill(cx - 2, y + 4, cz - 2, cx + 2, y + 4, cz + 2, "minecraft:gold_block")
        self.set(cx, y + 5, cz, "minecraft:glowstone")
        # The throne hall, through a gate in the south face.
        self.fill(cx - 12, G + 1, cz - 10, cx + 12, G + 10, cz + 12, AIR)
        self.fill(cx - 2, G + 1, cz + 12, cx + 2, G + 5, cz + half, AIR)
        for z in range(cz + 13, cz + half + 1, 3):
            self.set(cx - 2, G + 5, z, lantern(hanging=True))
            self.set(cx + 2, G + 5, z, lantern(hanging=True))
        for x in range(cx - 12, cx + 13):
            for z in range(cz - 10, cz + 13):
                self.set(x, G, z, "minecraft:gold_block" if x == cx and z > cz - 6 else
                         (p["accent"] if (x + z) % 2 else "minecraft:smooth_quartz"))
        for x in (cx - 8, cx - 4, cx + 4, cx + 8):
            for z in (cz - 6, cz, cz + 6):
                self.fill(x, G + 1, z, x, G + 10, z, p["pillar"])
        for x in range(cx - 11, cx + 12, 3):
            self.set(x, G + 10, cz + 11, "minecraft:glowstone")
            self.set(x, G + 10, cz - 9, "minecraft:glowstone")
            self.set(x, G + 10, cz + 1, "minecraft:glowstone")
        # The throne on a dais; yellow hangings behind it; the mural above.
        self.fill(cx - 3, G + 1, cz - 10, cx + 3, G + 1, cz - 7, p["trim"])
        self.set(cx, G + 2, cz - 8, stairs("minecraft:smooth_quartz_stairs", "north"))
        self.set(cx - 1, G + 2, cz - 8, "minecraft:gold_block")
        self.set(cx + 1, G + 2, cz - 8, "minecraft:gold_block")
        self.fill(cx, G + 3, cz - 9, cx, G + 5, cz - 9, "minecraft:gold_block")
        for x in (cx - 5, cx + 5):
            self.fill(x, G + 2, cz - 10, x, G + 8, cz - 10, "minecraft:yellow_wool")
        self.mural(cx - 6, G + 3, cz - 11)
        # The treasury, behind the throne, through a narrow door on the left.
        self.fill(cx - 12, G + 1, cz - 16, cx - 5, G + 4, cz - 12, AIR)
        self.fill(cx - 9, G + 1, cz - 11, cx - 9, G + 2, cz - 11, AIR)
        self.chest(cx - 11, G + 1, cz - 15, "east", "precursor_vault_sun")
        self.chest(cx - 6, G + 1, cz - 15, "west", "precursor_archive_sun")
        self.set(cx - 8, G + 4, cz - 14, lantern(hanging=True))
        # The tomb: down a hidden stair under a slab by the east pillars; the sealed room; the trap.
        tz = cz - 2
        for i in range(5):
            self.set(cx + 6, G - i, tz + 4 + i, stairs("minecraft:sandstone_stairs", "north"))
            self.fill(cx + 6, G - i + 1, tz + 4 + i, cx + 6, G - i + 3, tz + 4 + i, AIR)
        self.set(cx + 6, G, tz + 4, slab("minecraft:smooth_sandstone_slab"))
        self.fill(cx + 2, G - 7, tz - 3, cx + 10, G - 4, tz + 8, AIR)
        self.fill(cx + 6, G - 3, tz + 8, cx + 6, G - 1, tz + 8, AIR)
        for x in range(cx + 2, cx + 11):
            for z in range(tz - 3, tz + 9):
                self.set(x, G - 8, z, "minecraft:orange_terracotta" if (x + z) % 2 else p["accent"])
        self.fill(cx + 6, G - 7, tz - 1, cx + 6, G - 7, tz + 1, "minecraft:gold_block")
        self.set(cx + 6, G - 6, tz, "minecraft:skeleton_skull[powered=false,rotation=8]")
        self.set(cx + 6, G - 7, tz + 3, "minecraft:stone_pressure_plate[powered=false]")
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                self.set(cx + 6 + dx, G - 9, tz + 3 + dz, "minecraft:tnt[unstable=false]")
        self.set(cx + 6, G - 8, tz + 3, "minecraft:tnt[unstable=false]")
        self.chest(cx + 3, G - 7, tz - 2, "east", "precursor_tomb_sun")
        self.chest(cx + 9, G - 7, tz - 2, "west", "precursor_tomb_sun")
        self.set(cx + 6, G - 4, tz + 6, lantern(hanging=True))
        self.floors.append((cx, G + 1, cz))
        self.floors.append((cx - 6, G + 1, cz + 8))
        self.claim(cx - half, cz - half, cx + half, cz + half)

    def well(self, cx, cz):
        """The Well of Noon: a stepped cistern open to the sky, so even the water has no shade."""
        p = self.p
        rad = 6
        for x in range(cx - rad, cx + rad + 1):
            for z in range(cz - rad, cz + rad + 1):
                d = max(abs(x - cx), abs(z - cz))
                step = min(rad - d, 3)
                for y in range(G - step + 1, G + 1):
                    self.set(x, y, z, AIR)
                self.set(x, G - step, z, p["wall"])
                if rad - d >= 3:
                    self.set(x, G - 2, z, "minecraft:water")
                    self.set(x, G - 1, z, "minecraft:water")
                if d == rad:
                    self.set(x, G + 1, z, slab("minecraft:smooth_sandstone_slab"))
        for dx, dz in [(-rad, -rad), (rad, -rad), (-rad, rad), (rad, rad)]:
            self.fill(cx + dx, G + 1, cz + dz, cx + dx, G + 4, cz + dz, p["pillar"])
            self.set(cx + dx, G + 5, cz + dz, "minecraft:gold_block")
        self.claim(cx - rad, cz - rad, cx + rad, cz + rad)

    def market(self, cx, cz):
        """The great market: a square of stalls round a gold-tipped sundial."""
        p = self.p
        for x in range(cx - 12, cx + 13):
            for z in range(cz - 12, cz + 13):
                if self.inside(x, z) and (x, z) not in self.road:
                    self.set(x, G, z, p["street"] if (x + z) % 3 else p["accent"])
        self.fill(cx, G + 1, cz, cx, G + 4, cz, p["pillar"])
        self.set(cx, G + 5, cz, "minecraft:gold_block")
        self.claim(cx - 1, cz - 1, cx + 1, cz + 1)
        for x in range(cx - 10, cx + 11, 4):
            for z in range(cz - 10, cz + 11, 5):
                if abs(x - cx) > 2 or abs(z - cz) > 2:
                    self.stall(x, z)
        self.claim(cx - 12, cz - 12, cx + 12, cz + 12)

    def mastaba(self, mx, mz):
        """A tomb of the Court's dead: a low sloped block, a false door, an offering chapel; under it, stopped with
        sand, the burial chamber."""
        p = self.p
        if not self.free(mx - 5, mz - 4, mx + 5, mz + 4, 1):
            return
        for y in range(0, 4):
            k = y // 2
            self.fill(mx - 5 + k, G + 1 + y, mz - 4 + k, mx + 5 - k, G + 1 + y, mz + 4 - k,
                      p["wall"] if y % 2 == 0 else "minecraft:sandstone")
        self.fill(mx - 4, G + 5, mz - 3, mx + 4, G + 5, mz + 3, slab("minecraft:smooth_sandstone_slab"))
        self.set(mx, G + 3, mz + 3, "minecraft:chiseled_red_sandstone")
        self.fill(mx - 2, G + 1, mz - 1, mx + 2, G + 2, mz + 2, AIR)
        self.fill(mx, G + 1, mz + 3, mx, G + 2, mz + 4, AIR)
        self.set(mx - 2, G + 1, mz - 1, "minecraft:candle[candles=3,lit=true,waterlogged=false]")
        self.fill(mx - 2, G - 3, mz - 3, mx + 2, G - 1, mz + 1, AIR)
        self.set(mx, G, mz, "minecraft:sand")
        self.set(mx, G - 1, mz, "minecraft:sand")
        self.chest(mx - 2, G - 3, mz - 3, "south", "precursor_tomb_sun")
        self.set(mx + 2, G - 3, mz - 3, "minecraft:decorated_pot[facing=south,waterlogged=false]")
        self.claim(mx - 5, mz - 4, mx + 5, mz + 4)


# ---------------------------------------------------------------- the Tide Kingdom: rings on the sea

class TideCity(City):
    G = TIDE_G
    H = TIDE_G + 32

    def __init__(self, kingdom, size, seed, capital):
        super().__init__(kingdom, size, seed)
        self.capital = capital
        self.deck = set()
        self.decked = False

    def shape(self, x, z):
        return self.dist(x, z) <= self.R

    def inside(self, x, z):
        # Once the decks are laid, only they are the city: the sea between is not built on.
        return (x, z) in self.deck if self.decked else super().inside(x, z)

    def lay_deck(self, cols):
        """Decks on piers: a pier every few blocks reaches down into the sea (foundations carry it to the floor)."""
        g = self.G
        p = self.p
        for (x, z) in cols:
            if not self.inside(x, z):
                continue
            self.deck.add((x, z))
            self.set(x, g, z, p["street"] if (x, z) in self.road else p["pave"])
            self.set(x, g - 1, z, p["found"])
            for y in range(g + 1, g + 6):
                self.set(x, y, z, AIR)
            if x % 4 == 0 and z % 4 == 0:
                for y in range(0, g - 1):
                    self.set(x, y, z, p["found"])

    def build(self):
        n, c, r, R = self.n, self.c, self.r, self.R
        rings = [R * 0.36, R * 0.62, R * 0.88] if self.capital else [R * 0.42, R * 0.84]
        hub = R * 0.2
        cols = set()
        for x in range(n):
            for z in range(n):
                d = self.dist(x, z)
                if d <= hub or any(ring - 8 <= d <= ring + 8 for ring in rings):
                    cols.add((x, z))
        spokes = 8 if self.capital else 6
        bridges = set()
        for i in range(spokes):
            a = 2 * math.pi * i / spokes + 0.2
            for t in range(int(R)):
                x, z = c + math.cos(a) * t, c + math.sin(a) * t
                for dx in (-1, 0, 1):
                    for dz in (-1, 0, 1):
                        bridges.add((round(x + dx), round(z + dz)))
        for ring in rings:
            self.ring(c, c, ring, 2)
        self.road |= {b for b in bridges if self.inside(*b)}
        self.lay_deck(cols | bridges)
        for (x, z) in bridges - cols:
            if not self.inside(x, z):
                continue
            for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                if (x + dx, z + dz) not in self.deck and self.inside(x + dx, z + dz):
                    self.set(x, self.G + 1, z, wall_block("minecraft:prismarine_wall"))
                    break
        self.decked = True
        self.tidehall(c, c, 10 if self.capital else 8)
        lights = 4 if self.capital else 2
        for i in range(lights):
            a = 2 * math.pi * i / lights + 0.75
            self.lighthouse(int(c + math.cos(a) * rings[-1]), int(c + math.sin(a) * rings[-1]))
        pools = []
        count = 6 if self.capital else 3
        for i in range(count):
            a = 2 * math.pi * i / count + 0.4
            ring = rings[1] if self.capital else rings[0]
            pool = self.watch_pool(int(c + math.cos(a) * ring), int(c + math.sin(a) * ring))
            if pool:
                pools.append(pool)
        a = r.random() * 2 * math.pi
        self.archive(int(c + math.cos(a) * rings[0]) - 7, int(c + math.sin(a) * rings[0]) - 5)
        self.statue(c + int(hub) - 2, c - int(hub) + 2)
        for i in range(12 if self.capital else 7):
            b = r.random() * 2 * math.pi
            ring = r.choice(rings)
            self.stall(int(c + math.cos(b) * (ring + 3)), int(c + math.sin(b) * (ring + 3)))
        self.fill_lots(3600 if self.capital else 2200, (5, 7), self.house, gap=1)
        for i in range(0, 360, 12):
            for ring in rings:
                x, z = int(c + math.cos(math.radians(i)) * (ring + 4)), int(c + math.sin(math.radians(i)) * (ring + 4))
                if (x, z) in self.deck and (x, z) not in self.taken and (x, z) not in self.road:
                    self.lamp(x, z)
                    self.taken.add((x, z))
        # The Guardians: in the Watch Pools and the open water between the rings.
        spots = [(px, self.G - 3, pz) for px, pz in pools]
        for i in range(12 if self.capital else 6):
            b = r.random() * 2 * math.pi
            d = (rings[0] + rings[1]) / 2
            x, z = int(c + math.cos(b) * d), int(c + math.sin(b) * d)
            if (x, z) not in self.deck:
                spots.append((x, self.G - 4, z))
        self.guards("minecraft:guardian", spots)
        self.townsfolk(58 if self.capital else 36)
        return self

    def house(self, x0, z0, w, d):
        """A house on the decks: quartz and calcite, open corners, a stepped prismarine roof."""
        p = self.p
        x1, z1 = x0 + w - 1, z0 + d - 1
        if not all((x, z) in self.deck for x in (x0, x1) for z in (z0, z1)):
            return
        gy = self.G
        floors = self.r.choice((1, 2))
        top = gy + 4 * floors
        material = self.r.choice(["minecraft:calcite", "minecraft:smooth_quartz", "minecraft:white_terracotta", "minecraft:diorite"])
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                edge = x in (x0, x1) or z in (z0, z1)
                corner = x in (x0, x1) and z in (z0, z1)
                for y in range(gy + 1, top + 1):
                    if corner:
                        self.set(x, y, z, p["pillar"])
                    elif edge:
                        window = (y - gy) % 4 == 2 and (x + z) % 2 == 0
                        self.set(x, y, z, "minecraft:light_blue_stained_glass_pane" if window else material)
                    else:
                        self.set(x, y, z, slab("minecraft:prismarine_brick_slab") if (y - gy) % 4 == 0 else AIR)
        k = 0
        while x0 + k <= x1 - k and z0 + k <= z1 - k:
            last = x0 + k + 1 > x1 - k - 1 or z0 + k + 1 > z1 - k - 1
            for x in range(x0 + k, x1 - k + 1):
                for z in range(z0 + k, z1 - k + 1):
                    self.set(x, top + 1 + k, z, "minecraft:dark_prismarine" if last else "minecraft:prismarine")
            k += 1
        dx, dz, facing = self.door_side(x0, z0, x1, z1)
        self.fill(dx, gy + 1, dz, dx, gy + 2, dz, AIR)
        things = ["minecraft:barrel[facing=up,open=false]", "minecraft:cyan_bed[facing=north,occupied=false,part=foot]",
                  "minecraft:candle[candles=3,lit=true,waterlogged=false]", "minecraft:decorated_pot[facing=north,waterlogged=false]",
                  "minecraft:sea_pickle[pickles=3,waterlogged=false]", "minecraft:lectern[facing=north,has_book=false,powered=false]"]
        self.furnish(x0, z0, x1, z1, gy + 1, things, avoid=[(dx, dz)])
        if self.r.random() < 0.35:
            self.chest(x1 - 1, gy + 1, z1 - 1, "north", "precursor_house_tide")
        self.set((x0 + x1) // 2, top, (z0 + z1) // 2, "minecraft:sea_lantern")
        self.floors.append(((x0 + x1) // 2, gy + 1, (z0 + z1) // 2))
        self.claim(x0, z0, x1, z1)

    def lighthouse(self, lx, lz):
        p = self.p
        g = self.G
        for y in range(g + 1, g + 24):
            for x in range(lx - 3, lx + 4):
                for z in range(lz - 3, lz + 4):
                    d = math.hypot(x - lx, z - lz)
                    if d <= 3.2:
                        self.set(x, y, z, (p["accent"] if (y // 3) % 2 else p["wall"]) if d > 2.2 else AIR)
        for y in range(g + 1, g + 24):
            self.set(lx, y, lz - 1, p["wall"])
            self.set(lx, y, lz, "minecraft:ladder[facing=south,waterlogged=false]")
        self.fill(lx, g + 1, lz + 3, lx, g + 2, lz + 3, AIR)
        for x in range(lx - 3, lx + 4):
            for z in range(lz - 3, lz + 4):
                if math.hypot(x - lx, z - lz) <= 3.2 and (x, z) != (lx, lz):
                    self.set(x, g + 24, z, p["pave"])
        for dx, dz in [(-2, -2), (2, -2), (-2, 2), (2, 2)]:
            self.fill(lx + dx, g + 25, lz + dz, lx + dx, g + 27, lz + dz, p["pillar"])
        self.set(lx + 1, g + 25, lz + 1, "minecraft:sea_lantern")
        self.set(lx - 1, g + 25, lz - 1, "minecraft:sea_lantern")
        self.fill(lx - 1, g + 26, lz - 1, lx + 1, g + 26, lz + 1, "minecraft:light_blue_stained_glass")
        self.set(lx, g + 26, lz, "minecraft:sea_lantern")
        self.fill(lx - 2, g + 28, lz - 2, lx + 2, g + 28, lz + 2, "minecraft:dark_prismarine")
        self.claim(lx - 3, lz - 3, lx + 3, lz + 3)

    def watch_pool(self, px, pz):
        """A Watch Pool: an opening in the deck down to the sea, railed, lit; the Guardians came up here to be fed."""
        if not all((x, z) in self.deck and (x, z) not in self.taken for x in (px - 3, px + 3) for z in (pz - 3, pz + 3)):
            return None
        for x in range(px - 3, px + 4):
            for z in range(pz - 3, pz + 4):
                if max(abs(x - px), abs(z - pz)) == 3:
                    self.set(x, self.G + 1, z, wall_block("minecraft:prismarine_wall"))
                else:
                    for y in range(0, self.G + 1):
                        self.b.pop((x, y, z), None)
        self.set(px - 3, self.G + 2, pz - 3, "minecraft:sea_lantern")
        self.set(px + 3, self.G + 2, pz + 3, "minecraft:sea_lantern")
        self.claim(px - 3, pz - 3, px + 3, pz + 3)
        return px, pz

    def tidehall(self, cx, cz, radius):
        """The Tidehall: a rotunda of quartz under a prismarine dome; in its floor the Eye Pool, open to the sea, where
        the eldest Guardian kept watch. A stair beside it goes down to the Sanctum of the Open Eye, a glass room under
        the sea, and the kingdom's vault."""
        p = self.p
        g = self.G
        for x in range(cx - radius - 2, cx + radius + 3):
            for z in range(cz - radius - 2, cz + radius + 3):
                d = math.hypot(x - cx, z - cz)
                if d <= radius + 2.5:
                    self.set(x, g, z, p["trim"] if d > radius + 0.5 else ("minecraft:dark_prismarine" if int(d) % 3 == 0 else p["pave"]))
                if d <= radius + 0.5:
                    for y in range(g + 1, g + 8):
                        wall = d > radius - 0.6
                        door = abs(x - cx) <= 1 and z > cz
                        self.set(x, y, z, (p["pillar"] if (x + z) % 3 == 0 else "minecraft:light_blue_stained_glass")
                                 if wall and not door else AIR)
        for x in range(cx - radius, cx + radius + 1):
            for z in range(cz - radius, cz + radius + 1):
                for y in range(0, radius + 1):
                    d = math.sqrt((x - cx) ** 2 + (z - cz) ** 2 + y * y)
                    if d <= radius + 0.5:
                        on = d > radius - 0.6
                        rib = x == cx or z == cz
                        self.set(x, g + 8 + y, z, ("minecraft:dark_prismarine" if rib else "minecraft:prismarine") if on else AIR)
        self.set(cx, g + 7 + radius, cz, "minecraft:sea_lantern")
        for x in range(cx - 2, cx + 3):
            for z in range(cz - 2, cz + 3):
                for y in range(0, g + 1):
                    self.b.pop((x, y, z), None)
        for x, z in [(cx - 3, cz), (cx + 3, cz), (cx, cz - 3), (cx, cz + 3)]:
            self.set(x, g + 1, z, "minecraft:sea_lantern")
        self.mural(cx - 6, g + 1, cz - radius + 1)
        # The Sanctum: down a glass stair west of the pool into a glass room under the sea.
        sx, sz = cx - 5, cz
        for i in range(10):
            x = sx - i
            self.set(x, g - i, sz, stairs("minecraft:prismarine_brick_stairs", "east"))
            self.fill(x, g - i + 1, sz, x, g - i + 3, sz, AIR)
            for dz in (-1, 1):
                self.fill(x, g - i, sz + dz, x, g - i + 3, sz + dz, "minecraft:glass")
            self.set(x, g - i - 1, sz, "minecraft:prismarine_bricks")
            self.set(x, g - i + 4, sz, "minecraft:glass")
        ry = g - 10
        rx0, rx1, rz0, rz1 = sx - 17, sx - 9, sz - 4, sz + 4
        for x in range(rx0, rx1 + 1):
            for z in range(rz0, rz1 + 1):
                for y in range(ry - 1, ry + 5):
                    edge = x in (rx0, rx1) or z in (rz0, rz1) or y in (ry - 1, ry + 4)
                    glass = edge and ry - 1 < y < ry + 4 or y == ry + 4 and not (x in (rx0, rx1) or z in (rz0, rz1))
                    self.set(x, y, z, ("minecraft:glass" if glass else "minecraft:prismarine_bricks") if edge else AIR)
        self.fill(rx1, ry, sz, rx1, ry + 2, sz, AIR)
        self.chest(rx0 + 1, ry, sz, "east", "precursor_vault_tide")
        self.chest(rx0 + 1, ry, rz0 + 1, "south", "precursor_archive_tide")
        self.set(rx0 + 3, ry, rz1 - 1, "minecraft:conduit[waterlogged=false]")
        self.set((rx0 + rx1) // 2, ry + 3, sz, "minecraft:sea_lantern")
        self.floors.append((cx + 4, g + 1, cz + 2))
        self.claim(cx - radius - 2, cz - radius - 2, cx + radius + 2, cz + radius + 2)


# ---------------------------------------------------------------- the Hearth: lanes, smithies, the Anvil Hall

class HearthCity(City):
    H = 44

    def shape(self, x, z):
        a = math.atan2(z - self.c, x - self.c)
        return self.dist(x, z) <= self.R - 2 + 2 * math.sin(a * 3 + 1.1)

    def build(self):
        n, c, r, R = self.n, self.c, self.r, self.R
        # Crooked lanes out from the green to the low wall; three rings of lanes.
        for i in range(9):
            a = 2 * math.pi * i / 9 + r.random() * 0.3
            pts = []
            for t in range(0, 13):
                d = 16 + (R - 18) * t / 12
                bend = math.sin(t * 0.6 + i * 1.7) * 0.18
                pts.append((c + math.cos(a + bend) * d, c + math.sin(a + bend) * d))
            self.path(pts, 3)
        for ring in (25, R * 0.5, R * 0.72):
            self.ring(c, c, ring, 3)
        self.lay_ground()
        # The low wall of field stone round the town.
        for x in range(n):
            for z in range(n):
                if self.inside(x, z) and not all(self.inside(x + dx, z + dz) for dx, dz in ((2, 0), (-2, 0), (0, 2), (0, -2))):
                    if (x, z) not in self.road:
                        self.fill(x, G + 1, z, x, G + 2, z, "minecraft:mossy_cobblestone" if (x * 7 + z) % 5 == 0 else "minecraft:cobblestone")
                        self.set(x, G + 3, z, wall_block("minecraft:cobblestone_wall"))
                    self.taken.add((x, z))
        self.anvil_hall(c, c)
        for i in range(5):
            a = 2 * math.pi * i / 5 + 0.5
            self.windmill(int(c + math.cos(a) * R * 0.84), int(c + math.sin(a) * R * 0.84))
        # Fields in the outer belt.
        for _ in range(80):
            a = r.random() * 2 * math.pi
            d = r.uniform(R * 0.76, R * 0.9)
            x0, z0 = int(c + math.cos(a) * d) - 4, int(c + math.sin(a) * d) - 4
            if self.free(x0 - 1, z0 - 1, x0 + 9, z0 + 9, 0):
                crop = r.choice(["minecraft:wheat[age=7]", "minecraft:carrots[age=7]", "minecraft:potatoes[age=7]",
                                 "minecraft:beetroots[age=3]"])
                for x in range(x0, x0 + 9):
                    for z in range(z0, z0 + 9):
                        if x - x0 == 4:
                            self.set(x, G, z, "minecraft:water")
                        else:
                            self.set(x, G, z, "minecraft:farmland[moisture=7]")
                            self.set(x, G + 1, z, crop)
                for x in range(x0 - 1, x0 + 10):
                    for z in (z0 - 1, z0 + 9):
                        self.set(x, G + 1, z, "minecraft:oak_fence[east=true,north=false,south=false,waterlogged=false,west=true]")
                self.claim(x0 - 1, z0 - 1, x0 + 9, z0 + 9)
        for i in range(8):
            a = 2 * math.pi * i / 8 + 0.2
            self.smithy(int(c + math.cos(a) * R * 0.36), int(c + math.sin(a) * R * 0.36))
        self.archive(c + 24, c - 42)
        self.statue(c, c + 23)
        for i in range(14):
            b = r.random() * 2 * math.pi
            self.stall(int(c + math.cos(b) * 28), int(c + math.sin(b) * 28))
        self.fill_lots(3000, (6, 10), self.cottage)
        for _ in range(80):
            x, z = r.randint(6, n - 7), r.randint(6, n - 7)
            if self.inside(x, z) and self.free(x - 2, z - 2, x + 2, z + 2, 1):
                self.oak(x, z)
        for i in range(0, 360, 10):
            for ring in (R * 0.5, R * 0.72):
                x, z = int(c + math.cos(math.radians(i)) * (ring + 3)), int(c + math.sin(math.radians(i)) * (ring + 3))
                if self.inside(x, z) and (x, z) not in self.taken and (x, z) not in self.road:
                    self.lamp(x, z, 3)
                    self.taken.add((x, z))
        # Their golems, kept bright, on the lanes and at the hall's door.
        spots = []
        for i in range(10):
            a = 2 * math.pi * i / 10 + 0.3
            x, z = int(c + math.cos(a) * R * 0.6), int(c + math.sin(a) * R * 0.6)
            spots.append((x, G + 1, z))
        spots += [(c - 6, G + 1, c + 21), (c + 6, G + 1, c + 21)]
        self.guards("chronogenesis:precursor_golem", spots, tags=False)
        self.townsfolk(54)
        return self

    def oak(self, x, z):
        gy = G
        h = self.r.randint(4, 6)
        leaves = "minecraft:oak_leaves[distance=7,persistent=true,waterlogged=false]"
        self.fill(x, gy + 1, z, x, gy + h, z, "minecraft:oak_log[axis=y]")
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                for dy in (-1, 0, 1):
                    if abs(dx) + abs(dz) + max(0, dy) <= 3 and self.get(x + dx, gy + h + dy, z + dz) == AIR:
                        self.set(x + dx, gy + h + dy, z + dz, leaves)
        self.set(x, gy + h + 2, z, leaves)
        self.claim(x - 2, z - 2, x + 2, z + 2)

    def gable(self, x0, z0, x1, z1, top, along_x, material="minecraft:dark_oak"):
        """A steep gabled roof: stairs climbing from both eaves to a ridge (along x, or along z), gable ends filled."""
        stair = material + "_stairs"
        if along_x:
            lo, hi = z0 - 1, z1 + 1
        else:
            lo, hi = x0 - 1, x1 + 1
        k = 0
        while lo + k <= hi - k:
            y = top + 1 + k
            if along_x:
                for x in range(x0 - 1, x1 + 2):
                    if lo + k == hi - k:
                        self.set(x, y, lo + k, slab(material + "_slab"))
                    else:
                        self.set(x, y, lo + k, stairs(stair, "south"))
                        self.set(x, y, hi - k, stairs(stair, "north"))
                for z in range(lo + k + 1, hi - k):
                    for x in (x0, x1):
                        self.set(x, y, z, "minecraft:white_terracotta" if (z + y) % 3 else material + "_planks")
            else:
                for z in range(z0 - 1, z1 + 2):
                    if lo + k == hi - k:
                        self.set(lo + k, y, z, slab(material + "_slab"))
                    else:
                        self.set(lo + k, y, z, stairs(stair, "east"))
                        self.set(hi - k, y, z, stairs(stair, "west"))
                for x in range(lo + k + 1, hi - k):
                    for z in (z0, z1):
                        self.set(x, y, z, "minecraft:white_terracotta" if (x + y) % 3 else material + "_planks")
            k += 1

    def cottage(self, x0, z0, w, d):
        """A timber-framed cottage: a stone sill, spruce frame posts and beams, white infill, windows, a steep dark
        roof, a brick chimney with its fire lit; beds, a stove, a front garden; a cellar under some."""
        x1, z1 = x0 + w - 1, z0 + d - 1
        gy = G
        floors = self.r.choice((1, 2, 2))
        top = gy + 4 * floors
        infill = self.r.choice(["minecraft:white_terracotta", "minecraft:calcite", "minecraft:mushroom_stem", "minecraft:white_concrete"])
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                edge = x in (x0, x1) or z in (z0, z1)
                corner = x in (x0, x1) and z in (z0, z1)
                post = corner or (z in (z0, z1) and (x - x0) % 4 == 0) or (x in (x0, x1) and (z - z0) % 4 == 0)
                self.set(x, gy, z, "minecraft:spruce_planks")
                for y in range(gy + 1, top + 1):
                    if edge and post:
                        self.set(x, y, z, "minecraft:stripped_spruce_log[axis=y]")
                    elif edge and (y - gy) % 4 == 0:
                        self.set(x, y, z, "minecraft:stripped_spruce_log[axis=" + ("x" if z in (z0, z1) else "z") + "]")
                    elif edge:
                        window = (y - gy) % 4 == 2 and (x + z) % 3 == 1
                        self.set(x, y, z, "minecraft:cobblestone" if y == gy + 1 else ("minecraft:glass_pane" if window else infill))
                    else:
                        self.set(x, y, z, "minecraft:spruce_planks" if (y - gy) % 4 == 0 and y < top else AIR)
        if floors == 2:
            for i in range(4):
                self.set(x0 + 1 + i, gy + 1 + i, z1 - 1, stairs("minecraft:spruce_stairs", "east"))
                self.set(x0 + 1 + i, gy + 4, z1 - 1, AIR)
        self.gable(x0, z0, x1, z1, top, w >= d)
        cx, cz = x1 - 1, z0 + 1
        h = top + 2 + min(w, d) // 2
        self.fill(cx, gy + 2, z0, cx, h, z0, "minecraft:bricks")
        self.set(cx, h + 1, z0, "minecraft:campfire[facing=north,lit=true,signal_fire=false,waterlogged=false]")
        self.set(cx, gy + 1, cz, "minecraft:furnace[facing=south,lit=true]")
        dx, dz, facing = self.door_side(x0, z0, x1, z1)
        self.fill(dx, gy + 1, dz, dx, gy + 2, dz, AIR)
        things = ["minecraft:red_bed[facing=north,occupied=false,part=foot]", "minecraft:barrel[facing=up,open=false]",
                  "minecraft:crafting_table", "minecraft:composter[level=4]", "minecraft:loom[facing=north]",
                  "minecraft:cauldron", "minecraft:smoker[facing=north,lit=false]"]
        self.furnish(x0, z0, x1, z1, gy + 1, things, avoid=[(dx, dz), (cx, cz)])
        if floors == 2:
            self.furnish(x0, z0, x1, z1, gy + 5, ["minecraft:red_bed[facing=north,occupied=false,part=foot]",
                                                   "minecraft:barrel[facing=up,open=false]", "minecraft:white_carpet"])
        self.set((x0 + x1) // 2, gy + 3, (z0 + z1) // 2, lantern(hanging=True))
        self.floors.append(((x0 + x1) // 2, gy + 1, (z0 + z1) // 2))
        if self.r.random() < 0.4:
            self.cellar(x0, z0, x1, z1, gy)
        ox, oz = {"north": (0, -1), "south": (0, 1), "west": (-1, 0), "east": (1, 0)}[facing]
        for i in (-2, -1, 1, 2):
            fx, fz = (dx + i, dz + oz) if ox == 0 else (dx + ox, dz + i)
            if (fx, fz) not in self.road and (fx, fz) not in self.taken and self.inside(fx, fz):
                self.set(fx, G + 1, fz, self.r.choice(["minecraft:poppy", "minecraft:cornflower", "minecraft:dandelion",
                                                        "minecraft:oxeye_daisy", "minecraft:sweet_berry_bush[age=2]"]))
        self.claim(x0, z0, x1, z1)

    def smithy(self, sx, sz):
        """An open smithy under a roof on posts: the forge and its blast furnace, an anvil, a quench trough, and a
        golem's frame of iron half made (the pumpkin never set on it)."""
        if not self.free(sx - 4, sz - 4, sx + 4, sz + 4, 1):
            return
        for x in range(sx - 4, sx + 5):
            for z in range(sz - 4, sz + 5):
                self.set(x, G, z, "minecraft:stone_bricks" if (x + z) % 2 else "minecraft:polished_andesite")
        for x, z in [(sx - 4, sz - 4), (sx + 4, sz - 4), (sx - 4, sz + 4), (sx + 4, sz + 4)]:
            self.fill(x, G + 1, z, x, G + 4, z, "minecraft:stripped_spruce_log[axis=y]")
        self.gable(sx - 4, sz - 4, sx + 4, sz + 4, G + 4, True, "minecraft:spruce")
        self.fill(sx - 3, G + 1, sz - 3, sx - 1, G + 2, sz - 3, "minecraft:stone_bricks")
        self.set(sx - 2, G + 1, sz - 2, "minecraft:blast_furnace[facing=south,lit=true]")
        self.set(sx - 2, G + 3, sz - 3, "minecraft:campfire[facing=north,lit=true,signal_fire=false,waterlogged=false]")
        self.set(sx + 1, G + 1, sz, "minecraft:anvil[facing=north]")
        self.set(sx + 2, G + 1, sz - 2, "minecraft:smithing_table")
        self.fill(sx + 3, G + 1, sz + 1, sx + 3, G + 1, sz + 3, "minecraft:water_cauldron[level=3]")
        self.fill(sx - 1, G + 1, sz + 2, sx - 1, G + 2, sz + 2, "minecraft:iron_block")
        self.set(sx - 2, G + 2, sz + 2, "minecraft:iron_block")
        self.set(sx, G + 2, sz + 2, "minecraft:iron_block")
        self.chest(sx + 3, G + 1, sz - 3, "west", "precursor_house_hearth")
        self.set(sx, G + 4, sz, lantern(hanging=True))
        self.floors.append((sx, G + 1, sz - 1))
        self.claim(sx - 4, sz - 4, sx + 4, sz + 4)

    def windmill(self, mx, mz):
        """A windmill: a stone base, a planked tower, sails of wool on the south face; sacks of grain inside."""
        if not self.free(mx - 3, mz - 3, mx + 3, mz + 3, 0):
            return
        for y in range(G + 1, G + 13):
            for x in range(mx - 2, mx + 3):
                for z in range(mz - 2, mz + 3):
                    edge = max(abs(x - mx), abs(z - mz)) == 2
                    self.set(x, y, z, ("minecraft:stone_bricks" if y < G + 5 else "minecraft:spruce_planks") if edge else AIR)
        self.gable(mx - 2, mz - 2, mx + 2, mz + 2, G + 12, True)
        self.fill(mx, G + 1, mz - 2, mx, G + 2, mz - 2, AIR)
        hy = G + 10
        self.set(mx, hy, mz + 3, "minecraft:spruce_log[axis=z]")
        for i in range(1, 7):
            for ax, ay, sx, sy in [(i, 0, 0, 1), (-i, 0, 0, -1), (0, i, -1, 0), (0, -i, 1, 0)]:
                self.set(mx + ax, hy + ay, mz + 3, "minecraft:spruce_fence")
                if i > 1:
                    self.set(mx + ax + sx, hy + ay + sy, mz + 3, "minecraft:white_wool")
        self.chest(mx + 1, G + 1, mz + 1, "west", "precursor_house_hearth")
        self.fill(mx - 1, G + 1, mz + 1, mx - 1, G + 2, mz + 1, "minecraft:hay_block[axis=y]")
        self.claim(mx - 3, mz - 3, mx + 3, mz + 4)

    def anvil_hall(self, cx, cz):
        """The Anvil Hall on its green: stone below, timber above, a long steep roof. Inside: the hearth that was
        never let go out, the long tables, the high seat, and the forge pit where their golems were made, an iron
        golem's likeness over it. Under the hall, the armoury and the vault."""
        hw, hd = 11, 17
        x0, x1, z0, z1 = cx - hw, cx + hw, cz - hd, cz + hd
        for x in range(cx - 16, cx + 17):
            for z in range(cz - 22, cz + 23):
                if (x, z) not in self.road and self.inside(x, z):
                    self.set(x, G, z, "minecraft:grass_block[snowy=false]")
        top = G + 9
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                edge = x in (x0, x1) or z in (z0, z1)
                self.set(x, G, z, "minecraft:spruce_planks" if (x + z) % 2 else "minecraft:dark_oak_planks")
                for y in range(G + 1, top + 1):
                    if edge:
                        post = (z - z0) % 4 == 0 and x in (x0, x1) or (x - x0) % 4 == 0 and z in (z0, z1)
                        if y <= G + 4:
                            self.set(x, y, z, "minecraft:chiseled_stone_bricks" if post else "minecraft:stone_bricks")
                        else:
                            window = (y - G) in (6, 7) and not post
                            self.set(x, y, z, "minecraft:stripped_dark_oak_log[axis=y]" if post else
                                     ("minecraft:glass_pane" if window else "minecraft:white_terracotta"))
                    else:
                        self.set(x, y, z, AIR)
        self.gable(x0, z0, x1, z1, top, False)
        for z in range(z0 + 2, z1 - 1, 4):
            for x in range(x0 + 1, x1):
                self.set(x, top, z, "minecraft:stripped_dark_oak_log[axis=x]")
            for x in (cx - 5, cx + 5):
                self.set(x, top - 1, z, "minecraft:chain[axis=y,waterlogged=false]")
                self.set(x, top - 2, z, lantern(hanging=True))
        # The great door (south), iron pillars beside it; the high seat (north).
        self.fill(cx - 2, G + 1, z1, cx + 2, G + 4, z1, AIR)
        for x in (cx - 3, cx + 3):
            self.fill(x, G + 1, z1 + 1, x, G + 5, z1 + 1, "minecraft:iron_block")
        self.fill(cx - 2, G + 1, z0 + 1, cx + 2, G + 1, z0 + 3, "minecraft:polished_andesite")
        self.set(cx, G + 2, z0 + 2, stairs("minecraft:dark_oak_stairs", "south"))
        self.fill(cx, G + 3, z0 + 1, cx, G + 4, z0 + 1, "minecraft:iron_block")
        for x in (cx - 2, cx + 2):
            self.fill(x, G + 2, z0 + 1, x, G + 7, z0 + 1, "minecraft:red_wool")
        self.mural(cx - 6, G + 5, z0 + 1)
        for x in (cx - 5, cx + 5):
            for z in range(z0 + 6, z1 - 10):
                self.set(x, G + 1, z, "minecraft:spruce_planks" if z % 3 else "minecraft:barrel[facing=up,open=false]")
                self.set(x - 1, G + 1, z, stairs("minecraft:spruce_stairs", "east"))
                self.set(x + 1, G + 1, z, stairs("minecraft:spruce_stairs", "west"))
        self.fill(cx - 1, G + 1, cz - 3, cx + 1, G + 1, cz - 1, "minecraft:stone_bricks")
        self.set(cx, G + 1, cz - 2, "minecraft:campfire[facing=north,lit=true,signal_fire=false,waterlogged=false]")
        # The forge pit at the south end, the golem's likeness over it.
        fz = z1 - 6
        self.fill(cx - 3, G - 2, fz - 2, cx + 3, G, fz + 2, AIR)
        self.fill(cx - 3, G - 3, fz - 2, cx + 3, G - 3, fz + 2, "minecraft:magma_block")
        self.set(cx, G - 2, fz, "minecraft:lava")
        for x in (cx - 4, cx + 4):
            self.fill(x, G + 1, fz - 2, x, G + 1, fz + 2, "minecraft:iron_bars[east=false,north=true,south=true,waterlogged=false,west=false]")
        self.fill(cx - 1, G - 1, fz, cx - 1, G + 2, fz, "minecraft:iron_block")
        self.fill(cx + 1, G - 1, fz, cx + 1, G + 2, fz, "minecraft:iron_block")
        self.fill(cx - 2, G + 3, fz, cx + 2, G + 3, fz, "minecraft:iron_block")
        self.fill(cx - 2, G + 1, fz, cx - 2, G + 2, fz, "minecraft:iron_block")
        self.fill(cx + 2, G + 1, fz, cx + 2, G + 2, fz, "minecraft:iron_block")
        self.fill(cx, G + 4, fz, cx, G + 4, fz, "minecraft:iron_block")
        self.set(cx, G + 5, fz, "minecraft:carved_pumpkin[facing=south]")
        # Under the hall: the armoury and the vault, down a stair in the north-east corner.
        for i in range(4):
            self.set(x1 - 2, G - i, z0 + 3 + i, stairs("minecraft:stone_brick_stairs", "south"))
            self.fill(x1 - 2, G - i + 1, z0 + 3 + i, x1 - 2, G - i + 3, z0 + 3 + i, AIR)
        self.fill(x0 + 2, G - 4, z0 + 7, x1 - 2, G - 1, z0 + 15, AIR)
        for x in range(x0 + 3, x1 - 2, 2):
            self.set(x, G - 4, z0 + 7, "minecraft:smithing_table" if x % 4 == 1 else "minecraft:grindstone[face=floor,facing=south]")
        self.chest(x0 + 3, G - 4, z0 + 14, "east", "precursor_vault_hearth")
        self.chest(x0 + 3, G - 4, z0 + 12, "east", "precursor_archive_hearth")
        self.set(cx, G - 1, z0 + 11, lantern(hanging=True))
        self.floors.append((cx, G + 1, cz + 4))
        self.floors.append((cx - 3, G + 1, z0 + 5))
        self.claim(cx - 16, cz - 22, cx + 16, cz + 22)


# ---------------------------------------------------------------- the Verdant Kingdom: up in the trees

class VerdantCity(City):
    H = 54

    def shape(self, x, z):
        a = math.atan2(z - self.c, x - self.c)
        return self.dist(x, z) <= self.R - 4 + 4 * math.sin(a * 5 + self.phase)

    def build(self):
        n, c, r, R = self.n, self.c, self.r, self.R
        self.phase = r.random() * 6
        for i in range(7):
            a = 2 * math.pi * i / 7 + r.random() * 0.4
            pts = []
            for t in range(0, 14):
                d = 24 + (R - 26) * t / 13
                bend = math.sin(t * 0.7 + i) * 0.35
                pts.append((c + math.cos(a + bend) * d, c + math.sin(a + bend) * d))
            self.path(pts, 2)
        self.ring(c, c, R * 0.5, 2)
        self.ring(c, c, 23, 3)
        self.lay_ground()
        for (x, z) in self.road:
            if r.random() < 0.5:
                self.set(x, G, z, "minecraft:dirt_path")
        self.garden(c, c, 17)
        # Great trees with platforms round them, and rope bridges between neighbours.
        trees = []
        for _ in range(500):
            a = r.random() * 2 * math.pi
            d = r.uniform(30, R - 9)
            x, z = int(c + math.cos(a) * d), int(c + math.sin(a) * d)
            if self.free(x - 6, z - 6, x + 7, z + 7, 1) and all(math.hypot(x - tx, z - tz) > 18 for tx, tz, _ in trees):
                level = r.choice((10, 13, 16))
                self.tree_house(x, z, level)
                trees.append((x, z, level))
        for i, (ax, az, al) in enumerate(trees):
            near = sorted(trees[:i] + trees[i + 1:], key=lambda t: math.hypot(t[0] - ax, t[1] - az))[:2]
            for b in near:
                if math.hypot(b[0] - ax, b[1] - az) < 32:
                    self.bridge((ax, az, al), b)
        # Terraced gardens, stepping up to a pool.
        for _ in range(50):
            x0, z0 = r.randint(6, n - 15), r.randint(6, n - 15)
            if self.free(x0, z0, x0 + 8, z0 + 8, 1):
                for x in range(x0, x0 + 9):
                    for z in range(z0, z0 + 9):
                        step = min(x - x0, z - z0, x0 + 8 - x, z0 + 8 - z)
                        y = G + min(step, 3)
                        self.fill(x, G, z, x, y - 1, z, "minecraft:mossy_stone_bricks")
                        self.set(x, y, z, "minecraft:water" if step >= 3 else "minecraft:farmland[moisture=7]")
                        if step < 3:
                            self.set(x, y + 1, z, r.choice(["minecraft:wheat[age=7]", "minecraft:beetroots[age=3]",
                                                            "minecraft:carrots[age=7]", "minecraft:potatoes[age=7]"]))
                self.claim(x0, z0, x0 + 8, z0 + 8)
        self.archive(c - 36, c + 30)
        self.statue(c, c + 22)
        for i in range(12):
            b = r.random() * 2 * math.pi
            self.stall(int(c + math.cos(b) * R * 0.5), int(c + math.sin(b) * R * 0.5))
        self.fill_lots(2200, (5, 8), self.stilt_house)
        for _ in range(240):
            x, z = r.randint(3, n - 4), r.randint(3, n - 4)
            if self.inside(x, z) and self.free(x, z, x, z, 1):
                if r.random() < 0.4:
                    self.palm(x, z, r.randint(6, 10))
                else:
                    self.set(x, G + 1, z, r.choice(["minecraft:fern", "minecraft:azalea", "minecraft:flowering_azalea",
                                                    "minecraft:moss_carpet", "minecraft:short_grass"]))
                    self.taken.add((x, z))
        self.townsfolk(50)
        return self

    def stilt_house(self, x0, z0, w, d):
        """A hut on jungle-log stilts, a ladder up to its door, woven walls, a thatch-and-moss roof."""
        x1, z1 = x0 + w - 1, z0 + d - 1
        gy = G + 3
        top = gy + 4
        wall = self.r.choice(["minecraft:bamboo_block[axis=y]", "minecraft:jungle_planks", "minecraft:mud_bricks",
                              "minecraft:stripped_bamboo_block[axis=y]"])
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                for y in range(G + 1, gy):
                    self.set(x, y, z, AIR)
        for x, z in [(x0, z0), (x1, z0), (x0, z1), (x1, z1)]:
            self.fill(x, G + 1, z, x, gy, z, "minecraft:jungle_log[axis=y]")
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                corner = x in (x0, x1) and z in (z0, z1)
                edge = x in (x0, x1) or z in (z0, z1)
                self.set(x, gy, z, "minecraft:jungle_planks")
                for y in range(gy + 1, top + 1):
                    if corner:
                        self.set(x, y, z, "minecraft:jungle_log[axis=y]")
                    elif edge:
                        self.set(x, y, z, "minecraft:glass_pane" if y == gy + 2 and (x + z) % 3 == 0 else wall)
                    else:
                        self.set(x, y, z, AIR)
        k = -1
        while x0 + k <= x1 - k and z0 + k <= z1 - k:
            for x in range(x0 + k, x1 - k + 1):
                for z in range(z0 + k, z1 - k + 1):
                    self.set(x, top + 2 + k, z, "minecraft:hay_block[axis=y]" if k < 1 else "minecraft:moss_block")
            k += 1
        dx, dz, facing = self.door_side(x0, z0, x1, z1)
        self.fill(dx, gy + 1, dz, dx, gy + 2, dz, AIR)
        ox, oz = {"north": (0, -1), "south": (0, 1), "west": (-1, 0), "east": (1, 0)}[facing]
        for y in range(G + 1, gy + 1):
            self.set(dx + ox, y, dz + oz, f"minecraft:ladder[facing={facing},waterlogged=false]")
        things = ["minecraft:lime_bed[facing=north,occupied=false,part=foot]", "minecraft:flower_pot",
                  "minecraft:composter[level=5]", "minecraft:barrel[facing=up,open=false]",
                  "minecraft:candle[candles=2,lit=true,waterlogged=false]", "minecraft:potted_fern"]
        self.furnish(x0, z0, x1, z1, gy + 1, things, avoid=[(dx, dz)])
        if self.r.random() < 0.35:
            self.chest(x1 - 1, gy + 1, z1 - 1, "north", "precursor_house_verdant")
        self.set((x0 + x1) // 2, top, (z0 + z1) // 2, "minecraft:shroomlight")
        self.floors.append(((x0 + x1) // 2, gy + 1, (z0 + z1) // 2))
        self.claim(x0 - 1, z0 - 1, x1 + 1, z1 + 1)

    def tree_house(self, x, z, level):
        """A great tree, a ring platform round its trunk at {level}, two huts on it, a spiral stair up."""
        self.big_tree(x, z, level + 12, 6)
        py = G + level
        for dx in range(-5, 7):
            for dz in range(-5, 7):
                d = math.hypot(dx - 0.5, dz - 0.5)
                if 1.5 < d <= 6.0:
                    self.set(x + dx, py, z + dz, "minecraft:jungle_planks" if d < 5.2 else "minecraft:stripped_jungle_log[axis=y]")
                    if d >= 5.2:
                        self.set(x + dx, py + 1, z + dz, "minecraft:jungle_fence")
                    for y in range(py + 1, py + 4):
                        if d < 5.2 and self.get(x + dx, y, z + dz) == LEAVES:
                            self.set(x + dx, y, z + dz, AIR)
        for i in range(level):
            a = i * 0.55
            sx, sz = round(x + 0.5 + math.cos(a) * 2.6), round(z + 0.5 + math.sin(a) * 2.6)
            self.set(sx, G + 1 + i, sz, slab("minecraft:jungle_slab", "top"))
            self.fill(sx, G + 2 + i, sz, sx, G + 4 + i, sz, AIR)
        for hx, hz in [(x - 4, z - 1), (x + 4, z)]:
            for dx in range(-1, 2):
                for dz in range(-1, 3):
                    edge = dx in (-1, 1) or dz in (-1, 2)
                    for y in range(py + 1, py + 4):
                        self.set(hx + dx, y, hz + dz, "minecraft:bamboo_block[axis=y]" if edge else AIR)
                    self.set(hx + dx, py + 4, hz + dz, "minecraft:moss_block")
            self.fill(hx, py + 1, hz - 1, hx, py + 2, hz - 1, AIR)
            self.set(hx, py + 1, hz + 1, self.r.choice(["minecraft:lime_bed[facing=north,occupied=false,part=foot]",
                                                        "minecraft:barrel[facing=up,open=false]"]))
            self.set(hx, py + 3, hz, "minecraft:shroomlight")
        self.chest(x + 4, py + 1, z, "south", "precursor_house_verdant")
        self.floors.append((x + 3, py + 1, z - 3))

    def bridge(self, a, b):
        """A rope bridge from platform to platform: bamboo slats, sagging in the middle."""
        (ax, az, al), (bx, bz, bl) = a, b
        ay, by = G + al, G + bl
        steps = int(math.hypot(bx - ax, bz - az) * 1.5)
        for i in range(steps + 1):
            t = i / steps
            if 0.17 < t < 0.83:
                x = round(ax + 0.5 + (bx - ax) * t)
                z = round(az + 0.5 + (bz - az) * t)
                y = round(ay + (by - ay) * t - math.sin(t * math.pi) * 2)
                here = self.get(x, y, z)
                if here == AIR or here == LEAVES:
                    self.set(x, y, z, slab("minecraft:bamboo_slab"))
                    self.set(x, y + 1, z, AIR)
                    self.set(x, y + 2, z, AIR)

    def garden(self, cx, cz, radius):
        """The Garden of Engines: a dome of glass on copper ribs; their living machines inside, flesh grown over brass
        frames to remember for them (whatever went missing, the engines would remember it had been there). At its
        heart the Remembering Engine, its tendrils climbing the ribs; a gallery round the inside to look up at it."""
        for x in range(cx - radius - 3, cx + radius + 4):
            for z in range(cz - radius - 3, cz + radius + 4):
                d = math.hypot(x - cx, z - cz)
                if d <= radius + 3:
                    self.set(x, G, z, "minecraft:moss_block" if d <= radius else "minecraft:mossy_stone_bricks")
        for x in range(cx - radius, cx + radius + 1):
            for z in range(cz - radius, cz + radius + 1):
                for y in range(0, radius + 1):
                    d = math.sqrt((x - cx) ** 2 + (z - cz) ** 2 + (y * 1.2) ** 2)
                    if d <= radius + 0.5:
                        on = d > radius - 0.7
                        a = math.degrees(math.atan2(z - cz, x - cx)) % 30
                        rib = a < 4 or y % 5 == 0
                        self.set(x, G + 1 + y, z, ("minecraft:waxed_oxidized_cut_copper" if rib else
                                                   "minecraft:green_stained_glass") if on else AIR)
        for dz in range(0, 4):
            self.fill(cx - 1, G + 1, cz + radius - dz, cx + 1, G + 3, cz + radius - dz, AIR)
        aspects = ["size", "speed", "awareness", "density", "time"]
        for i in range(22):
            a = self.r.random() * 2 * math.pi
            d = self.r.uniform(6, radius - 4)
            x, z = int(cx + math.cos(a) * d), int(cz + math.sin(a) * d)
            h = self.r.randint(2, 6)
            aspect = self.r.choice(aspects)
            self.fill(x, G + 1, z, x, G + h, z, CG + "brass_armature")
            for dx in (-1, 0, 1):
                for dz in (-1, 0, 1):
                    for dy in range(1, h + 1):
                        if (dx or dz) and self.r.random() < 0.55 and self.get(x + dx, G + dy, z + dz) == AIR:
                            self.set(x + dx, G + dy, z + dz, CG + "chronoflesh[aspect=" + aspect + ",growth=3]")
        for y in range(G + 1, G + radius):
            rr = 3.2 - (y - G) * 0.12
            for x in range(cx - 4, cx + 5):
                for z in range(cz - 4, cz + 5):
                    d = math.hypot(x - cx, z - cz)
                    if d <= rr:
                        self.set(x, y, z, CG + "brass_armature" if d < 1 else CG + "chronoflesh[aspect=awareness,growth=3]")
        for i in range(6):
            a = 2 * math.pi * i / 6
            for t in range(0, radius + 2):
                x = round(cx + math.cos(a + t * 0.08) * min(t, radius - 2))
                z = round(cz + math.sin(a + t * 0.08) * min(t, radius - 2))
                y = G + 2 + int(t * 0.9)
                if self.get(x, y, z) == AIR:
                    self.set(x, y, z, CG + "chronoflesh[aspect=awareness,growth=2]")
        self.set(cx, G + radius + 1, cz, "minecraft:shroomlight")
        gy = G + 7
        for x in range(cx - radius, cx + radius + 1):
            for z in range(cz - radius, cz + radius + 1):
                d = math.hypot(x - cx, z - cz)
                if radius - 4.5 < d <= radius - 2:
                    self.set(x, gy, z, "minecraft:waxed_cut_copper_slab[type=bottom,waterlogged=false]")
                elif radius - 5.5 < d <= radius - 4.5:
                    self.set(x, gy + 1, z, "minecraft:jungle_fence")
        for i in range(7):
            self.set(cx + radius - 4 - i, G + 1 + i, cz, stairs("minecraft:waxed_cut_copper_stairs", "west"))
            self.fill(cx + radius - 4 - i, G + 2 + i, cz, cx + radius - 4 - i, G + 4 + i, cz, AIR)
        self.chest(cx - radius + 4, gy + 1, cz, "east", "precursor_vault_verdant")
        self.chest(cx, G + 1, cz - radius + 4, "south", "precursor_archive_verdant")
        self.floors.append((cx + 6, G + 1, cz + 4))
        self.floors.append((cx - 3, gy + 1, cz - radius + 3))
        self.claim(cx - radius - 3, cz - radius - 3, cx + radius + 3, cz + radius + 3)


CITIES = {
    "sun": lambda seed: SunCity("sun", 161, seed),
    "tide_town": lambda seed: TideCity("tide", 129, seed, False),
    "tide_capital": lambda seed: TideCity("tide", 177, seed, True),
    "hearth": lambda seed: HearthCity("hearth", 161, seed),
    "verdant": lambda seed: VerdantCity("verdant", 161, seed),
}


def city(name, seed):
    return CITIES[name](seed).build()


# ---------------------------------------------------------------- the shrines round the obelisks

S = 23            # a shrine's width
SG = 3            # a shrine's ground (the obelisk's plinth) in its template
SH = SG + 20


def obelisk_into(b, ox, oy, oz):
    """The obelisk exactly as make_structures.obelisk() draws it, its plinth's middle at (ox, oy, oz)."""
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            b[(ox + dx, oy, oz + dz)] = CG + "obelisk_stone"
            b[(ox + dx, oy + 1, oz + dz)] = CG + "obelisk_stone" if (dx, dz) == (0, 0) \
                else "minecraft:polished_blackstone_slab[type=bottom,waterlogged=false]"
            for y in range(oy + 2, oy + 12):
                b[(ox + dx, y, oz + dz)] = AIR
    for y in range(1, 11):
        b[(ox, oy + y, oz)] = CG + ("obelisk_inscription" if y in (3, 6, 9) else "obelisk_stone")
    b[(ox, oy + 11, oz)] = CG + "obelisk_capstone"


def shrine(kingdom, seed):
    """The shrine a kingdom built round an obelisk on its land: they did not know what the black stones were, only
    that near them nothing went missing. A small temple in their own style, the obelisk in its open middle."""
    r = random.Random(seed)
    p = PAL[kingdom]
    b = {}
    nbt = {}
    m = S // 2

    def put(x, y, z, state):
        if 0 <= x < S and 0 <= z < S and 0 <= y < SH:
            b[(x, y, z)] = state

    def box(x0, y0, z0, x1, y1, z1, state):
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1):
                    put(x, y, z, state)

    if kingdom == "tide":
        # A round deck on piers, a ring of quartz columns under a ring of slabs; a low prismarine rail; sea lanterns.
        for x in range(S):
            for z in range(S):
                d = math.hypot(x - m, z - m)
                if d <= m:
                    put(x, SG, z, p["pave"] if d < m - 1.5 else p["trim"])
                    if (x % 4 == 0 and z % 4 == 0) or d > m - 1:
                        box(x, 0, z, x, SG - 1, z, p["found"])
                    box(x, SG + 1, z, x, SG + 10, z, AIR)
                    if d > m - 1:
                        put(x, SG + 1, z, wall_block("minecraft:prismarine_wall"))
                    if m - 5.5 <= d <= m - 2.5:
                        put(x, SG + 8, z, slab("minecraft:prismarine_brick_slab"))
        for i in range(8):
            a = 2 * math.pi * i / 8
            x, z = round(m + math.cos(a) * (m - 4)), round(m + math.sin(a) * (m - 4))
            box(x, SG + 1, z, x, SG + 7, z, p["pillar"])
            put(x, SG + 8, z, "minecraft:sea_lantern")
        put(m, SG + 1, m - 3, "minecraft:conduit[waterlogged=false]")
    elif kingdom == "sun":
        # A walled court of white stone open to the sky (no shadow may fall in it), gold-capped; pylons at the gate.
        box(0, 0, 0, S - 1, SG - 1, S - 1, p["found"])
        box(0, SG, 0, S - 1, SG, S - 1, p["pave"])
        box(0, SG + 1, 0, S - 1, SG + 10, S - 1, AIR)
        for x in range(2, S - 2):
            for z in range(2, S - 2):
                if (x + z) % 2 and max(abs(x - m), abs(z - m)) > 2:
                    put(x, SG, z, p["accent"])
        for x in range(1, S - 1):
            for z in range(1, S - 1):
                if (x in (1, S - 2) or z in (1, S - 2)) and abs(x - m) > 1:
                    box(x, SG + 1, z, x, SG + 4, z, p["wall"])
                    put(x, SG + 5, z, p["trim"])
        for x in (m - 2, m + 2):
            box(x, SG + 1, S - 2, x, SG + 7, S - 2, p["pillar"])
            put(x, SG + 8, S - 2, "minecraft:gold_block")
        for x, z in [(4, 4), (S - 5, 4), (4, S - 5), (S - 5, S - 5)]:
            put(x, SG + 1, z, "minecraft:decorated_pot[facing=north,waterlogged=false]")
            put(x, SG + 2, z, lantern())
    elif kingdom == "hearth":
        # A timber lych-house: spruce posts, a steep dark roof over the obelisk (its tip through the ridge), benches,
        # an iron ring round the plinth, flowers along the yard.
        box(0, 0, 0, S - 1, SG - 1, S - 1, p["found"])
        box(0, SG, 0, S - 1, SG, S - 1, "minecraft:grass_block[snowy=false]")
        box(0, SG + 1, 0, S - 1, SG + 14, S - 1, AIR)
        lo, hi = 5, S - 6
        box(lo, SG, lo, hi, SG, hi, "minecraft:cobblestone")
        for x, z in [(lo, lo), (hi, lo), (lo, hi), (hi, hi), (m, lo), (lo, m), (hi, m)]:
            box(x, SG + 1, z, x, SG + 5, z, "minecraft:stripped_spruce_log[axis=y]")
        for k in range(0, (hi - lo) // 2 + 2):
            for z in range(lo - 1, hi + 2):
                if lo - 1 + k < m:
                    put(lo - 1 + k, SG + 6 + k, z, stairs("minecraft:dark_oak_stairs", "east"))
                    put(hi + 1 - k, SG + 6 + k, z, stairs("minecraft:dark_oak_stairs", "west"))
        for x, z in [(lo + 1, lo + 1), (hi - 1, lo + 1), (lo + 1, hi - 1), (hi - 1, hi - 1)]:
            put(x, SG + 1, z, stairs("minecraft:spruce_stairs", "north"))
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                if max(abs(dx), abs(dz)) == 2 and (dx, dz) != (0, 2):
                    put(m + dx, SG + 1, m + dz, "minecraft:iron_bars[east=true,north=true,south=true,waterlogged=false,west=true]")
        put(m - 3, SG + 5, m, lantern(hanging=True))
        put(m + 3, SG + 5, m, lantern(hanging=True))
        for x in range(1, S - 1, 3):
            put(x, SG + 1, 1, "minecraft:poppy")
            put(x, SG + 1, S - 2, "minecraft:cornflower")
    else:
        # Verdant: a ring of jungle-log arches grown over with leaves, an engine grown on a brass frame at each
        # quarter, shroomlights.
        box(0, 0, 0, S - 1, SG - 1, S - 1, p["found"])
        box(0, SG, 0, S - 1, SG, S - 1, "minecraft:moss_block")
        box(0, SG + 1, 0, S - 1, SG + 10, S - 1, AIR)
        for i in range(10):
            a = 2 * math.pi * i / 10
            x, z = round(m + math.cos(a) * 8), round(m + math.sin(a) * 8)
            box(x, SG + 1, z, x, SG + 6, z, "minecraft:jungle_log[axis=y]")
            put(x, SG + 7, z, LEAVES)
            nx, nz = round(m + math.cos(a + math.pi / 10) * 8), round(m + math.sin(a + math.pi / 10) * 8)
            put(nx, SG + 6, nz, "minecraft:jungle_log[axis=y]")
            put(nx, SG + 7, nz, LEAVES)
        for x, z in [(m - 6, m - 6), (m + 6, m - 6), (m - 6, m + 6), (m + 6, m + 6)]:
            box(x, SG + 1, z, x, SG + 3, z, CG + "brass_armature")
            for dx, dz in [(1, 0), (0, 1), (-1, 0), (0, -1)]:
                if r.random() < 0.7:
                    put(x + dx, SG + 1 + r.randint(0, 2), z + dz, CG + "chronoflesh[aspect=awareness,growth=3]")
            put(x, SG + 4, z, "minecraft:shroomlight")
        for x in range(2, S - 2):
            for z in range(2, S - 2):
                if r.random() < 0.06 and math.hypot(x - m, z - m) > 3 and (x, SG + 1, z) not in b:
                    put(x, SG + 1, z, r.choice(["minecraft:fern", "minecraft:azalea", "minecraft:moss_carpet"]))
    # Every kingdom: an offering before the obelisk, a candle, a chest of what they left, and the obelisk itself.
    put(m, SG + 1, m + 3, {"sun": "minecraft:gold_block", "tide": "minecraft:sea_lantern", "hearth": "minecraft:iron_block",
                           "verdant": "minecraft:flowering_azalea"}[kingdom])
    put(m - 1, SG + 1, m + 3, "minecraft:candle[candles=3,lit=true,waterlogged=false]")
    put(m + 2, SG + 1, m + 3, CHEST.format("north"))
    nbt[(m + 2, SG + 1, m + 3)] = {"id": "minecraft:chest", "LootTable": "chronogenesis:chests/precursor_shrine_" + kingdom}
    obelisk_into(b, m, SG, m)
    return b, nbt

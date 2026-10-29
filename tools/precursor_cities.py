"""
The cities of the Precursors, one generator per kingdom, and the ruins they leave (see make_structures.py).

Four kingdoms, each built (and fallen) its own way. Every city is a template of the same layout: its ground at
y = G, foundations, cellars, tombs and the Deep Kingdom's pit below.
  sun      the Sun Court: a walled grid of flat-roofed houses round the Undying Court, a step pyramid with a sealed
           tomb (and a trap that keeps its king in). Fell to the desert its king drank the years of.
  tide     the Tide Kingdom: rings of canals round the Tidehall, a domed temple holding a wave frozen mid-fall, a
           lighthouse. Drowned the day the frozen sea let go.
  deep     the Deep Kingdom: terraces stepping down a great pit to the Gate at its bottom, ringed in sculk. Went
           down to the nail's point to listen, and heard.
  verdant  the Verdant Kingdom: winding paths through trees to the Garden of Engines, a glass dome of living
           machines. Eaten by what it grew.
Each city has an archive (a mural of its kingdom's fall, chests of tablets), a Nail Tap (a replica obelisk they drew
time from), market stalls and its townsfolk, who know which kingdom they belong to.

The ruin of a city is the same template decayed: upper storeys crumbled, perishables gone, interiors filled with
earth (left to the terrain) except preserved chambers (the archive, tombs), and suspicious gravel or sand with
tablets for whoever brushes it out. It is placed buried in the present, lined up with the city in the past.
"""
import math
import random

AIR = "minecraft:air"
CG = "chronogenesis:"
G = 18            # the ground's y in every city template
AIR_TOP = 22      # cleared up to G + AIR_TOP
CITY_H = G + 27   # template height
RUIN_TOP = 10     # a ruin's top: ground + this

PAL = {
    "sun": {
        "pave": "minecraft:smooth_sandstone", "street": "minecraft:cut_red_sandstone", "wall": "minecraft:cut_sandstone",
        "trim": "minecraft:gold_block", "pillar": "minecraft:chiseled_sandstone", "accent": "minecraft:blue_terracotta",
        "house": ["minecraft:white_terracotta", "minecraft:smooth_sandstone", "minecraft:cut_sandstone", "minecraft:orange_terracotta"],
        "roof": "minecraft:smooth_sandstone", "roof_top": "minecraft:cut_red_sandstone", "slab": "minecraft:smooth_sandstone_slab",
        "stair": "minecraft:sandstone_stairs", "lamp": "minecraft:lantern[hanging=false,waterlogged=false]",
        "canopy": ["minecraft:orange_wool", "minecraft:yellow_wool", "minecraft:white_wool", "minecraft:red_wool"],
        "silt": "minecraft:sand", "suspicious": "minecraft:suspicious_sand",
    },
    "tide": {
        "pave": "minecraft:calcite", "street": "minecraft:polished_diorite", "wall": "minecraft:smooth_quartz",
        "trim": "minecraft:prismarine_bricks", "pillar": "minecraft:quartz_pillar[axis=y]", "accent": "minecraft:light_blue_terracotta",
        "house": ["minecraft:calcite", "minecraft:smooth_quartz", "minecraft:white_terracotta", "minecraft:diorite"],
        "roof": "minecraft:prismarine", "roof_top": "minecraft:dark_prismarine", "slab": "minecraft:prismarine_brick_slab",
        "stair": "minecraft:quartz_stairs", "lamp": "minecraft:sea_lantern",
        "canopy": ["minecraft:cyan_wool", "minecraft:light_blue_wool", "minecraft:white_wool", "minecraft:blue_wool"],
        "silt": "minecraft:gravel", "suspicious": "minecraft:suspicious_gravel",
    },
    "deep": {
        "pave": "minecraft:polished_deepslate", "street": "minecraft:deepslate_tiles", "wall": "minecraft:deepslate_bricks",
        "trim": "minecraft:gold_block", "pillar": "minecraft:chiseled_deepslate", "accent": "minecraft:polished_blackstone_bricks",
        "house": ["minecraft:deepslate_bricks", "minecraft:polished_blackstone_bricks", "minecraft:polished_deepslate", "minecraft:deepslate_tiles"],
        "roof": "minecraft:blackstone", "roof_top": "minecraft:gilded_blackstone", "slab": "minecraft:deepslate_tile_slab",
        "stair": "minecraft:deepslate_brick_stairs", "lamp": "minecraft:soul_lantern[hanging=false,waterlogged=false]",
        "canopy": ["minecraft:purple_wool", "minecraft:cyan_wool", "minecraft:gray_wool", "minecraft:black_wool"],
        "silt": "minecraft:gravel", "suspicious": "minecraft:suspicious_gravel",
    },
    "verdant": {
        "pave": "minecraft:stone_bricks", "street": "minecraft:mossy_cobblestone", "wall": "minecraft:mossy_stone_bricks",
        "trim": "minecraft:waxed_cut_copper", "pillar": "minecraft:stripped_jungle_log[axis=y]", "accent": "minecraft:chiseled_stone_bricks",
        "house": ["minecraft:stone_bricks", "minecraft:mossy_stone_bricks", "minecraft:jungle_planks", "minecraft:packed_mud"],
        "roof": "minecraft:moss_block", "roof_top": "minecraft:jungle_leaves[distance=7,persistent=true,waterlogged=false]",
        "slab": "minecraft:mossy_stone_brick_slab", "stair": "minecraft:mossy_stone_brick_stairs",
        "lamp": "minecraft:shroomlight",
        "canopy": ["minecraft:lime_wool", "minecraft:green_wool", "minecraft:yellow_wool", "minecraft:brown_wool"],
        "silt": "minecraft:gravel", "suspicious": "minecraft:suspicious_gravel",
    },
}

# Murals: each kingdom's fall, in its own colours. The eye of the One Outside watches from a corner of each.
MURAL_COLOURS = {
    "k": "minecraft:black_concrete", "w": "minecraft:white_terracotta", "y": "minecraft:yellow_terracotta",
    "o": "minecraft:orange_terracotta", "g": "minecraft:gold_block", "s": "minecraft:sand", "b": "minecraft:blue_terracotta",
    "c": "minecraft:cyan_terracotta", "l": "minecraft:light_blue_concrete", "i": "minecraft:packed_ice",
    "p": "minecraft:purple_concrete", "e": "minecraft:lime_concrete", "d": "minecraft:gray_terracotta",
    "m": "minecraft:magenta_terracotta", "n": "minecraft:green_terracotta", "r": "minecraft:red_terracotta",
    "u": "minecraft:brown_terracotta", ".": None,
}
MURALS = {
    # The king drinks from the nail; the fields beneath him run to sand.
    "sun": ["ppp..yyy.",
            "pep..yoy.",
            "ppp..yyy.",
            "..g..k...",
            ".ggg.k...",
            "..g..k.n.",
            "sssssssnn"],
    # A hand holds the sea still; behind it the nail; the water stands up like a wall.
    "tide": ["ppp...lll",
             "pep..llll",
             "ppp.liil.",
             "..k.lill.",
             "..k.www..",
             "..k..ww..",
             "ccccccccc"],
    # The nail goes down and down; at its point, an eye opens.
    "deep": ["....k....",
             "..ddkdd..",
             "...dkd...",
             "....k....",
             "....k....",
             "...ppp...",
             "...pep..."],
    # A growth on a brass frame swallows a house; the nail stands behind.
    "verdant": ["ppp...k..",
                "pep...k..",
                "ppp.mmkm.",
                "...mmmmmm",
                "..mmwwmm.",
                "..mwrrwm.",
                "nnnnnnnnn"],
}

PERISHABLE = ("wool", "glass", "lantern", "candle", "planks", "log", "leaves", "fence", "hay_block", "melon", "pumpkin",
              "wheat", "farmland", "flower", "barrel", "lectern", "composter", "torch", "water", "vine", "loom", "carpet",
              "shroomlight", "moss_carpet", "azalea", "mud", "sea_lantern", "ladder", "chain", "amethyst_cluster",
              "lightning_rod", "sculk_sensor", "sculk_shrieker", "fire", "campfire")
CRACKED = {
    "minecraft:stone_bricks": "minecraft:cracked_stone_bricks", "minecraft:deepslate_bricks": "minecraft:cracked_deepslate_bricks",
    "minecraft:deepslate_tiles": "minecraft:cracked_deepslate_tiles", "minecraft:polished_blackstone_bricks":
        "minecraft:cracked_polished_blackstone_bricks", "minecraft:cut_sandstone": "minecraft:sandstone",
    "minecraft:smooth_sandstone": "minecraft:sandstone", "minecraft:smooth_quartz": "minecraft:diorite",
    "minecraft:calcite": "minecraft:diorite", "minecraft:mossy_stone_bricks": "minecraft:cracked_stone_bricks",
}


class City:
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
        self.preserved = set()    # air kept in the ruin: archives, tombs
        self.taken = set()        # columns already built on
        self.road = set()         # columns that are street
        self.floors = []          # ((x, z), y): floor spots for townsfolk and silt finds
        self.archive_spots = []
        self.door = (0, -1)
        self.g = {}               # ground y per column (default G)

    # ---- basics
    def ground(self, x, z):
        return self.g.get((x, z), G)

    def inside(self, x, z):
        return 0 <= x < self.n and 0 <= z < self.n and self.shape(x, z)

    def shape(self, x, z):
        return True

    def set(self, x, y, z, state):
        if 0 <= x < self.n and 0 <= z < self.n and 0 <= y < CITY_H and state is not None:
            self.b[(x, y, z)] = state

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
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                self.taken.add((x, z))

    # ---- the ground: paving, streets, air above, a footing below
    def lay_ground(self):
        for x in range(self.n):
            for z in range(self.n):
                if not self.inside(x, z):
                    continue
                gy = self.ground(x, z)
                self.set(x, gy, z, self.p["street"] if (x, z) in self.road else self.p["pave"])
                self.set(x, gy - 1, z, self.p["wall"])
                for y in range(gy + 1, G + AIR_TOP + 1):
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

    # ---- buildings
    def house(self, x0, z0, w, d, floors=1, material=None, roof=None, preserve=False):
        """A house on the rectangle (x0, z0)-(x0 + w - 1, z0 + d - 1): walls, windows, a door towards the nearest
        street, furnishings, and the kingdom's roof."""
        p = self.p
        x1, z1 = x0 + w - 1, z0 + d - 1
        gy = max(self.ground(x, z) for x in range(x0, x1 + 1) for z in range(z0, z1 + 1))
        material = material or self.r.choice(p["house"])
        top = gy + 4 * floors
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                edge = x in (x0, x1) or z in (z0, z1)
                corner = x in (x0, x1) and z in (z0, z1)
                self.set(x, gy, z, p["pave"])
                for y in range(gy + 1, top + 1):
                    if corner:
                        self.set(x, y, z, p["pillar"])
                    elif edge:
                        window = (y - gy) % 4 == 2 and (x + z) % 2 == 0 and not (x in (x0, x1) and z in (z0 + 1, z1 - 1)) \
                            and not (z in (z0, z1) and x in (x0 + 1, x1 - 1))
                        self.set(x, y, z, "minecraft:glass_pane" if window else material)
                    else:
                        self.set(x, y, z, p["slab"] + "[type=bottom,waterlogged=false]" if (y - gy) % 4 == 0 else AIR)
                        if preserve:
                            self.preserved.add((x, y, z))
        # The door: on the side nearest a street.
        sides = [((x0 + x1) // 2, z0, 0, -1), ((x0 + x1) // 2, z1, 0, 1), (x0, (z0 + z1) // 2, -1, 0), (x1, (z0 + z1) // 2, 1, 0)]

        def reach(side):
            sx, sz, ox, oz = side
            return next((k for k in range(1, 6) if (sx + ox * k, sz + oz * k) in self.road), 99)
        sides.sort(key=reach)
        dx, dz = sides[0][0], sides[0][1]
        self.door = (sides[0][2], sides[0][3])
        self.set(dx, gy + 1, dz, AIR)
        self.set(dx, gy + 2, dz, AIR)
        # Furnishings against the walls.
        things = ["minecraft:barrel[facing=up,open=false]", "minecraft:lectern[facing=north,has_book=false,powered=false]",
                  "minecraft:candle[candles=3,lit=true,waterlogged=false]", "minecraft:flower_pot", "minecraft:loom[facing=north]",
                  "minecraft:composter[level=3]", "minecraft:cauldron", "minecraft:decorated_pot[facing=north,waterlogged=false]"]
        for (fx, fz) in [(x0 + 1, z0 + 1), (x1 - 1, z0 + 1), (x0 + 1, z1 - 1), (x1 - 1, z1 - 1)]:
            if self.r.random() < 0.7 and (fx, fz) != (dx, dz):
                self.set(fx, gy + 1, fz, self.r.choice(things))
        self.set((x0 + x1) // 2, top - 1 if floors == 1 else gy + 3, (z0 + z1) // 2,
                 self.p["lamp"].replace("hanging=false", "hanging=true") if "lantern" in self.p["lamp"] else self.p["lamp"])
        self.floors.append((((x0 + x1) // 2, (z0 + z1) // 2), gy + 1))
        (roof or self.roof)(x0, z0, x1, z1, top)
        self.claim(x0, z0, x1, z1)
        return gy

    def roof(self, x0, z0, x1, z1, top):
        """Flat roof with a parapet (the Sun Court's)."""
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                self.set(x, top + 1, z, self.p["roof"])
                if x in (x0, x1) or z in (z0, z1):
                    self.set(x, top + 2, z, self.p["slab"] + "[type=bottom,waterlogged=false]")

    def stepped_roof(self, x0, z0, x1, z1, top, steep=1):
        k = 0
        while x0 + k <= x1 - k and z0 + k <= z1 - k:
            last = x0 + k + 1 > x1 - k - 1 or z0 + k + 1 > z1 - k - 1
            for x in range(x0 + k, x1 - k + 1):
                for z in range(z0 + k, z1 - k + 1):
                    for s in range(steep):
                        self.set(x, top + 1 + k * steep + s, z, self.p["roof_top"] if last else self.p["roof"])
            k += 1

    def dome(self, cx, cz, radius, base, shell, inner=AIR, frame=None):
        for x in range(cx - radius, cx + radius + 1):
            for z in range(cz - radius, cz + radius + 1):
                for y in range(0, radius + 1):
                    d = math.sqrt((x - cx) ** 2 + (z - cz) ** 2 + y * y)
                    if d <= radius + 0.5:
                        on = d > radius - 0.6
                        rib = frame and (x == cx or z == cz or y == 0)
                        self.set(x, base + y, z, (frame if rib else shell) if on else inner)

    def stall(self, x, z):
        if not self.free(x - 1, z - 1, x + 1, z + 1, 0):
            return
        gy = self.ground(x, z)
        canopy = self.r.choice(self.p["canopy"])
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                self.set(x + dx, gy + 3, z + dz, canopy)
        for dx, dz in [(-1, -1), (-1, 1), (1, -1), (1, 1)]:
            self.fill(x + dx, gy + 1, z + dz, x + dx, gy + 2, z + dz, "minecraft:oak_fence")
        self.set(x, gy + 1, z, self.r.choice(["minecraft:barrel[facing=up,open=true]", "minecraft:melon", "minecraft:pumpkin",
                                              "minecraft:hay_block[axis=y]", "minecraft:composter[level=7]"]))
        self.claim(x - 1, z - 1, x + 1, z + 1)

    def lamp(self, x, z):
        gy = self.ground(x, z)
        self.fill(x, gy + 1, z, x, gy + 2, z, self.p["pillar"])
        self.set(x, gy + 3, z, self.p["lamp"])

    def nail_tap(self, x, z):
        """A replica of an obelisk with the siphons they drew time from it with: copper rods, amethyst, gold."""
        gy = self.ground(x, z)
        self.fill(x - 2, gy, z - 2, x + 2, gy, z + 2, self.p["trim"])
        self.fill(x, gy + 1, z, x, gy + 9, z, "minecraft:polished_blackstone")
        self.set(x, gy + 10, z, "minecraft:gilded_blackstone")
        for dx, dz, facing in [(-1, 0, "west"), (1, 0, "east"), (0, -1, "north"), (0, 1, "south")]:
            self.set(x + dx, gy + 4, z + dz, "minecraft:lightning_rod[facing=" + facing + ",powered=false,waterlogged=false]")
            self.set(x + 2 * dx, gy + 1, z + 2 * dz, "minecraft:amethyst_cluster[facing=up,waterlogged=false]")
            self.set(x + dx, gy + 7, z + dz, "minecraft:chain[axis=y,waterlogged=false]")
        self.claim(x - 2, z - 2, x + 2, z + 2)

    def mural(self, x0, y0, z, facing_south=True):
        """The kingdom's mural on a wall running along x at z, bottom-left at (x0, y0)."""
        for row, line in enumerate(MURALS[self.k]):
            for col, ch in enumerate(line):
                state = MURAL_COLOURS[ch]
                if state:
                    self.set(x0 + col, y0 + 6 - row, z, state)

    def archive(self, x0, z0):
        """The Hall of Records: the mural on its back wall, lecterns, chests of tablets. Kept in its ruin.
        Built at (x0, z0), or the nearest free, level spot."""
        w, d = 13, 9
        for ring in range(0, self.n):
            spots = [(x0 + dx, z0 + dz) for dx in range(-ring, ring + 1) for dz in range(-ring, ring + 1)
                     if max(abs(dx), abs(dz)) == ring]
            spots = [(x, z) for x, z in spots if self.free(x, z, x + w - 1, z + d - 1, 0)
                     and len({self.ground(a, b) for a in range(x, x + w) for b in range(z, z + d)}) == 1]
            if spots:
                x0, z0 = spots[0]
                break
        else:
            return False
        gy = self.house(x0, z0, w, d, floors=2, material=self.p["wall"], preserve=True)
        if self.door == (0, 1):
            # The mural goes on the wall without the door.
            self.set((x0 + x0 + w - 1) // 2, gy + 1, z0 + d - 1, self.p["wall"])
            self.set((x0 + x0 + w - 1) // 2, gy + 2, z0 + d - 1, self.p["wall"])
            self.set((x0 + x0 + w - 1) // 2, gy + 1, z0, AIR)
            self.set((x0 + x0 + w - 1) // 2, gy + 2, z0, AIR)
        self.fill(x0 + 1, gy + 4, z0 + 1, x0 + w - 2, gy + 4, z0 + d - 2, AIR)   # one tall hall
        for x in range(x0 + 1, x0 + w - 1):
            for z in range(z0 + 1, z0 + d - 1):
                for y in range(gy + 1, gy + 8):
                    self.preserved.add((x, y, z))
        self.mural(x0 + 2, gy + 1, z0 + d - 2)
        for i, x in enumerate((x0 + 2, x0 + w - 3)):
            self.set(x, gy + 1, z0 + 2, "minecraft:chest[facing=south,type=single,waterlogged=false]")
            self.nbt[(x, gy + 1, z0 + 2)] = {"id": "minecraft:chest", "LootTable": "chronogenesis:chests/precursor_archive_" + self.k}
            self.archive_spots.append((x, gy + 1, z0 + 2))
        for x in (x0 + 4, x0 + 6, x0 + 8):
            self.set(x, gy + 1, z0 + 3, "minecraft:lectern[facing=south,has_book=false,powered=false]")
        self.set(x0 + 6, gy + 1, z0 + d - 3, "minecraft:candle[candles=4,lit=true,waterlogged=false]")
        return True

    def statue(self, x, z):
        """A king of this kingdom, in gold and stone, a staff of black stone in hand."""
        gy = self.ground(x, z)
        self.fill(x - 1, gy + 1, z - 1, x + 1, gy + 1, z + 1, self.p["accent"])
        self.fill(x, gy + 2, z, x, gy + 3, z, self.p["wall"])          # legs, body
        self.set(x, gy + 4, z, self.p["wall"])
        self.set(x - 1, gy + 4, z, self.p["wall"])                     # arms
        self.set(x + 1, gy + 4, z, self.p["wall"])
        self.set(x, gy + 5, z, "minecraft:gold_block")                 # head
        self.set(x, gy + 6, z, "minecraft:gold_block" if self.k != "deep" else "minecraft:gilded_blackstone")
        self.fill(x + 1, gy + 2, z - 1, x + 1, gy + 6, z - 1, "minecraft:polished_blackstone_wall[east=none,north=none,south=none,up=true,waterlogged=false,west=none]")
        self.claim(x - 1, z - 1, x + 1, z + 1)

    def tree(self, x, z, height=None):
        gy = self.ground(x, z)
        h = height or self.r.randint(5, 8)
        log = "minecraft:jungle_log[axis=y]" if self.k == "verdant" else "minecraft:stripped_jungle_log[axis=y]"
        leaves = "minecraft:jungle_leaves[distance=7,persistent=true,waterlogged=false]"
        self.fill(x, gy + 1, z, x, gy + h, z, log)
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                for dy in (0, 1):
                    if abs(dx) + abs(dz) <= 3 - dy:
                        if self.b.get((x + dx, gy + h + dy, z + dz), AIR) == AIR:
                            self.set(x + dx, gy + h + dy, z + dz, leaves)
        self.set(x, gy + h + 2, z, leaves)
        if self.k == "verdant":
            for dx, dz, f in [(-1, 0, "east"), (1, 0, "west"), (0, -1, "south"), (0, 1, "north")]:
                for dy in range(2, 4):
                    if self.r.random() < 0.4:
                        self.set(x + dx * 2, gy + h - dy, z + dz * 2, "minecraft:vine[" + ",".join(
                            f"{s}={'true' if s == f else 'false'}" for s in ("east", "north", "south", "up", "west")) + "]")
        self.claim(x, z, x, z)

    def fill_lots(self, attempts, sizes=(5, 9), floors=(1, 2)):
        """Houses along the streets: each lot is put a step back from a street, on level ground."""
        roads = sorted(self.road)
        for _ in range(attempts):
            w, d = self.r.randint(*sizes), self.r.randint(*sizes)
            rx, rz = self.r.choice(roads)
            side = self.r.randrange(4)
            if side == 0:
                x0, z0 = rx - self.r.randint(0, w - 1), rz + 1
            elif side == 1:
                x0, z0 = rx - self.r.randint(0, w - 1), rz - d
            elif side == 2:
                x0, z0 = rx + 1, rz - self.r.randint(0, d - 1)
            else:
                x0, z0 = rx - w, rz - self.r.randint(0, d - 1)
            if self.free(x0, z0, x0 + w - 1, z0 + d - 1, margin=0):
                if len({self.ground(x, z) for x in range(x0, x0 + w) for z in range(z0, z0 + d)}) == 1:
                    self.house(x0, z0, w, d, floors=self.r.choice(floors), preserve=self.r.random() < 0.2)

    def townsfolk(self, count):
        spots = [(x, z) for (x, z) in self.road if (x, z) not in self.taken]
        self.r.shuffle(spots)
        spots = spots[:count - count // 4] + [s for s, _ in self.floors[:count // 4]]
        for x, z in spots:
            y = self.ground(x, z) + 1
            while self.b.get((x, y, z), AIR) != AIR and y < CITY_H - 2:
                y += 1
            self.entities.append(((x + 0.5, float(y), z + 0.5), (x, y, z), "chronogenesis:precursor", {"Kingdom": self.k}))

    def finish(self, folk):
        self.townsfolk(folk)
        return self


# ---------------------------------------------------------------- the Sun Court

class SunCity(City):
    def build(self):
        n, c, r = self.n, self.c, self.r
        cell = 12
        for x in range(2, n - 2):
            for z in range(2, n - 2):
                if abs(x - c) <= 1 or abs(z - c) <= 1 or (x - c) % cell == 0 or (z - c) % cell == 0:
                    self.road.add((x, z))
        self.lay_ground()
        # Walls, gates, corner towers.
        for x in range(n):
            for z in range(n):
                if min(x, z, n - 1 - x, n - 1 - z) <= 1:
                    gate = abs(x - c) <= 1 or abs(z - c) <= 1
                    top = 6
                    for y in range(G + 1, G + top + 1):
                        if not (gate and y <= G + 4):
                            self.set(x, y, z, self.p["wall"] if y < G + top else self.p["trim"])
                    self.taken.add((x, z))
        for tx, tz in [(2, 2), (n - 3, 2), (2, n - 3), (n - 3, n - 3)]:
            for x in range(tx - 2, tx + 3):
                for z in range(tz - 2, tz + 3):
                    for y in range(G + 1, G + 11):
                        edge = max(abs(x - tx), abs(z - tz)) == 2
                        self.set(x, y, z, (self.p["accent"] if y % 3 == 0 else self.p["wall"]) if edge else AIR)
                    self.set(x, G + 11, z, self.p["slab"] + "[type=bottom,waterlogged=false]")
            self.claim(tx - 2, tz - 2, tx + 2, tz + 2)
        self.pyramid(c, c, 10 if n < 80 else 15)
        self.archive(c + 6, c - 18 if n > 70 else c - 15)
        self.nail_tap(c - 12, c + 12)
        self.statue(c + 12, c + 12)
        for i in range(8 if n < 80 else 14):
            self.stall(r.randint(4, n - 5), r.choice([c - 3, c + 3]))
        # Gardens: wheat on farmland, a palm at each corner of the plot.
        for _ in range(4 if n < 80 else 8):
            x0, z0 = r.randint(4, n - 10), r.randint(4, n - 10)
            if self.free(x0, z0, x0 + 5, z0 + 5):
                for x in range(x0, x0 + 6):
                    for z in range(z0, z0 + 6):
                        self.set(x, G, z, "minecraft:farmland[moisture=7]")
                        self.set(x, G + 1, z, "minecraft:wheat[age=7]")
                for x, z in [(x0 - 1, z0 - 1), (x0 + 6, z0 + 6)]:
                    self.tree(x, z, 5)
                self.claim(x0, z0, x0 + 5, z0 + 5)
        self.fill_lots(900 if n > 70 else 450)
        for x in range(6, n - 6, 8):
            for z in (c - 2, c + 2):
                if (x, z) not in self.taken and self.inside(x, z):
                    self.lamp(x, z)
        return self.finish(22 if n < 80 else 36)

    def pyramid(self, cx, cz, half):
        """The Undying Court: steps of two blocks, gold-rimmed; inside, at the ground, the sealed tomb of the king who
        would not die, over a ring of TNT (the trap is to keep him in)."""
        y = G + 1
        h = half
        while h >= 2:
            for x in range(cx - h, cx + h + 1):
                for z in range(cz - h, cz + h + 1):
                    rim = max(abs(x - cx), abs(z - cz)) == h
                    self.set(x, y, z, self.p["trim"] if rim and (h % 2 == 0) else self.p["wall"])
                    self.set(x, y + 1, z, self.p["accent"] if rim and h % 3 == 0 else self.p["wall"])
            for dz in range(0, 2):
                for off in (-1, 0, 1):
                    self.set(cx + off, y + dz, cz + h, self.p["stair"] + "[facing=north,half=bottom,shape=straight,waterlogged=false]")
            y += 2
            h -= 2
        # The shrine on top: four pillars, the sun disc.
        for dx, dz in [(-1, -1), (-1, 1), (1, -1), (1, 1)]:
            self.fill(cx + dx, y, cz + dz, cx + dx, y + 2, cz + dz, self.p["pillar"])
        self.fill(cx - 1, y + 3, cz - 1, cx + 1, y + 3, cz + 1, "minecraft:gold_block")
        # The tomb: sealed, no door.
        self.fill(cx - 3, G + 1, cz - 3, cx + 3, G + 4, cz + 3, AIR)
        for x in range(cx - 3, cx + 4):
            for z in range(cz - 3, cz + 4):
                for yy in range(G + 1, G + 5):
                    self.preserved.add((x, yy, z))
                self.set(x, G, z, self.p["accent"] if (x + z) % 2 else "minecraft:orange_terracotta")
        self.fill(cx, G + 1, cz - 1, cx, G + 1, cz + 1, "minecraft:gold_block")      # the sarcophagus
        self.set(cx, G + 2, cz, "minecraft:skeleton_skull[powered=false,rotation=8]")
        self.set(cx, G + 1, cz + 2, "minecraft:stone_pressure_plate[powered=false]")
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                self.set(cx + dx, G - 1, cz + 2 + dz, "minecraft:tnt[unstable=false]")
        self.set(cx, G, cz + 2, "minecraft:tnt[unstable=false]")
        self.set(cx - 2, G + 1, cz - 2, "minecraft:chest[facing=east,type=single,waterlogged=false]")
        self.nbt[(cx - 2, G + 1, cz - 2)] = {"id": "minecraft:chest", "LootTable": "chronogenesis:chests/precursor_tomb_sun"}
        self.archive_spots.append((cx - 2, G + 1, cz - 2))
        self.claim(cx - half, cz - half, cx + half, cz + half)


# ---------------------------------------------------------------- the Tide Kingdom

class TideCity(City):
    def shape(self, x, z):
        return self.dist(x, z) <= self.R

    def build(self):
        n, c, r, R = self.n, self.c, self.r, self.R
        rings = [R * 0.42, R * 0.74] if n > 70 else [R * 0.55]
        canal = set()
        for x in range(n):
            for z in range(n):
                d = self.dist(x, z)
                if any(ring <= d < ring + 3 for ring in rings):
                    canal.add((x, z))
        spokes = 4 if n < 80 else 8
        for i in range(spokes):
            a = 2 * math.pi * i / spokes
            self.path([(c, c), (c + math.cos(a) * (R - 1), c + math.sin(a) * (R - 1))], 3)
        for ring in rings:
            for i in range(180):
                a = 2 * math.pi * i / 180
                self.path([(c + math.cos(a) * (ring - 2), c + math.sin(a) * (ring - 2))] * 2, 2)
        self.lay_ground()
        bridges = set(canal) & set(self.road)
        for (x, z) in canal:
            if (x, z) in bridges:
                self.set(x, G - 1, z, "minecraft:water[level=0]")
                self.set(x, G, z, self.p["trim"])
            else:
                self.set(x, G - 2, z, self.p["trim"])
                self.set(x, G - 1, z, "minecraft:water[level=0]")
                self.set(x, G, z, "minecraft:water[level=0]")
            self.taken.add((x, z))
        for (x, z) in canal:
            for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                if (x + dx, z + dz) not in canal and self.inside(x + dx, z + dz):
                    self.set(x + dx, G - 1, z + dz, self.p["wall"])
        self.tidehall(c, c, 6 if n < 80 else 8)
        # The lighthouse, on the city's edge.
        lx, lz = c + int(R * 0.86), c
        for y in range(G + 1, G + 19):
            for x in range(lx - 2, lx + 3):
                for z in range(lz - 2, lz + 3):
                    if math.hypot(x - lx, z - lz) <= 2.3:
                        edge = math.hypot(x - lx, z - lz) > 1.2
                        self.set(x, y, z, (self.p["accent"] if y % 4 == 0 else self.p["wall"]) if edge else AIR)
        self.set(lx, G + 19, lz, self.p["lamp"])
        self.fill(lx - 1, G + 20, lz - 1, lx + 1, G + 20, lz + 1, self.p["roof_top"])
        self.claim(lx - 2, lz - 2, lx + 2, lz + 2)
        ax = c - int(R * 0.6)
        self.archive(ax - 6, c + 3)
        self.nail_tap(c + int(R * 0.58), c + int(R * 0.58))
        self.statue(c - int(R * 0.58), c - int(R * 0.58))
        for i in range(6 if n < 80 else 12):
            a = r.random() * 2 * math.pi
            d = r.uniform(R * 0.5, R * 0.68)
            self.stall(int(c + math.cos(a) * d), int(c + math.sin(a) * d))
        self.fill_lots(900 if n > 70 else 450, sizes=(4, 8))
        return self.finish(22 if n < 80 else 36)

    def roof(self, x0, z0, x1, z1, top):
        self.stepped_roof(x0, z0, x1, z1, top)

    def tidehall(self, cx, cz, radius):
        """The Tidehall: a ring of quartz pillars under a prismarine dome; inside, the sea's wave held still in
        mid-fall, in ice that never melted while the kingdom stood."""
        for x in range(cx - radius, cx + radius + 1):
            for z in range(cz - radius, cz + radius + 1):
                d = math.hypot(x - cx, z - cz)
                if d <= radius + 0.5:
                    self.set(x, G, z, self.p["trim"] if d > radius - 1 else self.p["pave"])
                    for y in range(G + 1, G + 6):
                        wall = radius - 0.6 < d <= radius + 0.5
                        door = abs(x - cx) <= 1 and z > cz
                        self.set(x, y, z, (self.p["pillar"] if (x + z) % 3 == 0 else "minecraft:glass")
                                 if wall and not door else AIR)
                        if not wall:
                            self.preserved.add((x, y, z))
        self.dome(cx, cz, radius, G + 6, self.p["roof"], frame=self.p["roof_top"])
        # The frozen wave: a curl of ice rising from the basin and hanging over it.
        self.fill(cx - 2, G, cz - 2, cx + 2, G, cz + 2, "minecraft:water[level=0]")
        for i, (dx, dy) in enumerate([(-2, 1), (-2, 2), (-1, 3), (0, 4), (1, 4), (2, 3), (2, 2)]):
            for dz in (-1, 0, 1):
                self.set(cx + dx, G + dy, cz + dz, "minecraft:blue_ice" if i % 2 else "minecraft:packed_ice")
        self.mural(cx - 4, G + 1 - 1, cz - radius + 1)
        self.set(cx - 3, G + 1, cz + 3, "minecraft:chest[facing=north,type=single,waterlogged=false]")
        self.nbt[(cx - 3, G + 1, cz + 3)] = {"id": "minecraft:chest", "LootTable": "chronogenesis:chests/precursor_archive_tide"}
        self.archive_spots.append((cx - 3, G + 1, cz + 3))
        self.claim(cx - radius, cz - radius, cx + radius, cz + radius)


# ---------------------------------------------------------------- the Deep Kingdom

class DeepCity(City):
    def shape(self, x, z):
        return self.dist(x, z) <= self.R

    def build(self):
        n, c, r, R = self.n, self.c, self.r, self.R
        P = R * 0.55
        tiers = 4
        for x in range(n):
            for z in range(n):
                d = self.dist(x, z)
                if d < P:
                    step = min(tiers - 1, int((P - d) / (P / tiers)))
                    self.g[(x, z)] = G - 3 * (step + 1)
        bottom = G - 3 * tiers
        # A ramp down the north side (a block down per block), streets round the rim and out to the edge.
        start = c - (G - bottom) - 2
        for z in range(start, c + 1):
            for x in range(c - 1, c + 2):
                self.g[(x, z)] = max(bottom, G - max(0, z - start))
        self.path([(c, start - 2), (c, c - 2)], 3)
        for i in range(6):
            a = 2 * math.pi * i / 6 + 0.3
            self.path([(c + math.cos(a) * (P + 2), c + math.sin(a) * (P + 2)), (c + math.cos(a) * (R - 1), c + math.sin(a) * (R - 1))], 3)
        for i in range(240):
            a = 2 * math.pi * i / 240
            self.path([(c + math.cos(a) * (P + 2), c + math.sin(a) * (P + 2))] * 2, 3)
        self.lay_ground()
        # Retaining walls on each terrace's edge, gold-rimmed; footings under the terraces.
        for x in range(n):
            for z in range(n):
                gy = self.ground(x, z)
                if gy < G:
                    self.fill(x, gy - 2, z, x, gy - 1, z, self.p["wall"])
                    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                        if self.ground(x + dx, z + dz) > gy and self.inside(x + dx, z + dz):
                            top = self.ground(x + dx, z + dz)
                            if (x, z) in self.road:
                                break
                            self.fill(x, gy + 1, z, x, top, z, self.p["wall"])
                            self.set(x, top + 1, z, "minecraft:deepslate_brick_wall[east=none,north=none,south=none,up=true,waterlogged=false,west=none]"
                                     if (x + z) % 4 else self.p["lamp"])
                            break
        # The ramp's steps: a stair wherever it drops a block.
        for z in range(start, c + 1):
            gy = self.ground(c, z)
            if self.ground(c, z - 1) > gy:
                for x in range(c - 1, c + 2):
                    self.set(x, gy + 1, z, self.p["stair"] + "[facing=north,half=bottom,shape=straight,waterlogged=false]")
        self.gate(c, c)
        self.archive(c - 6, c + int(P) + 6)
        self.nail_tap(c + int(P) + 6, c - int(P) - 4)
        self.statue(c - int(P) - 5, c - 4)
        for i in range(6 if n < 80 else 12):
            a = r.random() * 2 * math.pi
            d = P + 3
            self.stall(int(c + math.cos(a) * d), int(c + math.sin(a) * d))
        self.fill_lots(900 if n > 70 else 450, sizes=(4, 8))
        return self.finish(22 if n < 80 else 36)

    def roof(self, x0, z0, x1, z1, top):
        self.stepped_roof(x0, z0, x1, z1, top, steep=2)

    def gate(self, cx, cz):
        """The Gate at the bottom of the pit: a frame of reinforced deepslate the size of a house, sculk spreading
        from it, sensors listening. What they built to go down past the point of the nail."""
        gy = self.ground(cx, cz)
        for x in range(cx - 5, cx + 6):
            for y in range(gy + 1, gy + 10):
                edge = abs(x - cx) == 5 or y == gy + 9
                if edge:
                    self.set(x, y, cz, "minecraft:reinforced_deepslate")
                elif abs(x - cx) == 4 or y == gy + 8:
                    self.set(x, y, cz, "minecraft:crying_obsidian")
        for x in range(cx - 7, cx + 8):
            for z in range(cz - 7, cz + 8):
                d = math.hypot(x - cx, z - cz)
                if d < 7 and self.r.random() < 1.1 - d / 7:
                    self.set(x, self.ground(x, z), z, "minecraft:sculk")
        for x, z in [(cx - 3, cz + 3), (cx + 3, cz + 3), (cx, cz - 3)]:
            self.set(x, self.ground(x, z) + 1, z, "minecraft:sculk_sensor[power=0,sculk_sensor_phase=inactive,waterlogged=false]")
        self.set(cx, gy + 1, cz + 4, "minecraft:sculk_shrieker[can_summon=false,shrieking=false,waterlogged=false]")
        # A crypt under the gate: their dead, gold, and what they wrote last.
        self.fill(cx - 3, gy - 5, cz - 3, cx + 3, gy - 1, cz + 3, self.p["wall"])
        self.fill(cx - 2, gy - 4, cz - 2, cx + 2, gy - 2, cz + 2, AIR)
        for x in range(cx - 2, cx + 3):
            for z in range(cz - 2, cz + 3):
                for y in range(gy - 4, gy - 1):
                    self.preserved.add((x, y, z))
        self.set(cx, gy - 4, cz, "minecraft:chest[facing=north,type=single,waterlogged=false]")
        self.nbt[(cx, gy - 4, cz)] = {"id": "minecraft:chest", "LootTable": "chronogenesis:chests/precursor_tomb_deep"}
        self.archive_spots.append((cx, gy - 4, cz))
        self.set(cx - 2, gy - 4, cz - 2, "minecraft:soul_lantern[hanging=false,waterlogged=false]")
        for z in range(cz - 2, cz + 3, 2):
            self.set(cx + 2, gy - 4, z, "minecraft:skeleton_skull[powered=false,rotation=12]")
        self.claim(cx - 7, cz - 7, cx + 7, cz + 7)


# ---------------------------------------------------------------- the Verdant Kingdom

class VerdantCity(City):
    def shape(self, x, z):
        a = math.atan2(z - self.c, x - self.c)
        return self.dist(x, z) <= self.R - 3 + 3 * math.sin(a * 5 + self.k_phase)

    def build(self):
        n, c, r, R = self.n, self.c, self.r, self.R
        self.k_phase = r.random() * 6
        for i in range(6):
            a = 2 * math.pi * i / 6 + r.random() * 0.4
            pts = []
            for t in range(0, 11):
                d = 4 + (R - 6) * t / 10
                bend = math.sin(t * 0.7 + i) * 0.35
                pts.append((c + math.cos(a + bend) * d, c + math.sin(a + bend) * d))
            self.path(pts, 2)
        for i in range(200):
            a = 2 * math.pi * i / 200
            self.path([(c + math.cos(a) * (R * 0.5), c + math.sin(a) * (R * 0.5))] * 2, 2)
        self.lay_ground()
        for (x, z) in self.road:
            if r.random() < 0.4:
                self.set(x, G, z, "minecraft:dirt_path")
        self.garden(c, c, 8 if n < 80 else 11)
        a = r.random() * 2 * math.pi
        self.archive(int(c + math.cos(a) * R * 0.62) - 6, int(c + math.sin(a) * R * 0.62) - 4)
        self.nail_tap(int(c - math.cos(a) * R * 0.6), int(c - math.sin(a) * R * 0.6))
        self.statue(int(c + math.sin(a) * R * 0.6), int(c - math.cos(a) * R * 0.6))
        for i in range(5 if n < 80 else 10):
            b = r.random() * 2 * math.pi
            self.stall(int(c + math.cos(b) * R * 0.45), int(c + math.sin(b) * R * 0.45))
        self.fill_lots(700 if n > 70 else 350, sizes=(5, 8))
        for _ in range(60 if n < 80 else 140):
            x, z = r.randint(3, n - 4), r.randint(3, n - 4)
            if self.inside(x, z) and self.free(x, z, x, z, 1):
                self.tree(x, z)
        return self.finish(20 if n < 80 else 34)

    def roof(self, x0, z0, x1, z1, top):
        # Overhanging green roofs: a moss slab over planks, leaves at the eaves.
        for x in range(x0 - 1, x1 + 2):
            for z in range(z0 - 1, z1 + 2):
                eave = x in (x0 - 1, x1 + 1) or z in (z0 - 1, z1 + 1)
                self.set(x, top + 1, z, self.p["roof_top"] if eave else "minecraft:jungle_planks")
                if not eave:
                    self.set(x, top + 2, z, self.p["roof"])
        self.stepped_roof(x0 + 1, z0 + 1, x1 - 1, z1 - 1, top + 2)

    def garden(self, cx, cz, radius):
        """The Garden of Engines: a dome of glass on copper ribs; inside, their living machines, flesh grown over
        brass frames and fed on time. They grew. They wanted. Then they were hungry."""
        self.dome(cx, cz, radius, G + 1, "minecraft:green_stained_glass", frame="minecraft:waxed_oxidized_cut_copper")
        for x in range(cx - radius, cx + radius + 1):
            for z in range(cz - radius, cz + radius + 1):
                if math.hypot(x - cx, z - cz) <= radius:
                    self.set(x, G, z, "minecraft:moss_block")
        self.fill(cx - 1, G + 1, cz + radius, cx + 1, G + 3, cz + radius, AIR)
        aspects = ["size", "speed", "awareness", "density"]
        for i in range(10 if radius < 10 else 16):
            a = self.r.random() * 2 * math.pi
            d = self.r.uniform(2, radius - 3)
            x, z = int(cx + math.cos(a) * d), int(cz + math.sin(a) * d)
            h = self.r.randint(2, 5)
            aspect = self.r.choice(aspects)
            self.fill(x, G + 1, z, x, G + h, z, CG + "brass_armature")
            for dx in (-1, 0, 1):
                for dz in (-1, 0, 1):
                    for dy in range(1, h + 1):
                        if (dx or dz) and self.r.random() < 0.55:
                            self.set(x + dx, G + dy, z + dz, CG + "chronoflesh[aspect=" + aspect + ",growth=3]")
        self.set(cx, G + radius - 1, cz, "minecraft:shroomlight")
        self.claim(cx - radius, cz - radius, cx + radius, cz + radius)


KINGDOMS = {"sun": SunCity, "tide": TideCity, "deep": DeepCity, "verdant": VerdantCity}
SIZES = {"town": 65, "capital": 97}


def city(kingdom, size, seed):
    return KINGDOMS[kingdom](kingdom, SIZES[size], seed).build()


def ruin(c, seed):
    """The city as it lies in the present, under the earth: see the module's notes."""
    r = random.Random(seed)
    out = {}
    nbt = {}
    p = c.p
    for (x, y, z), state in c.b.items():
        gy = c.ground(x, z)
        if state == AIR:
            if (x, y, z) in c.preserved:
                out[(x, y, z)] = AIR
            continue
        if any(word in state for word in PERISHABLE) and "chest" not in state:
            continue
        if "chronoflesh" in state:
            if r.random() < 0.7:
                out[(x, y, z)] = "minecraft:bone_block[axis=y]"
            continue
        if "packed_ice" in state:
            continue
        h = y - gy
        if h > 0 and r.random() > 0.95 - 0.1 * h:
            continue
        base = state.split("[")[0]
        if base in CRACKED and r.random() < 0.45:
            state = CRACKED[base]
        elif h <= 0 and r.random() < 0.08:
            state = p["silt"]
        out[(x, y, z)] = state
        if (x, y, z) in c.nbt:
            nbt[(x, y, z)] = c.nbt[(x, y, z)]
    # Finds for the brush: in the archive and tombs, and in houses' floors.
    spots = [((x, z), y) for (x, z), y in c.floors]
    r.shuffle(spots)
    finds = [(x, y, z) for (x, z), y in spots[:14]]
    for (x, y, z) in c.archive_spots:
        finds.append((x + 1, y, z))
    for (x, y, z) in finds:
        if out.get((x, y - 1, z)) not in (None, AIR):
            out[(x, y, z)] = p["suspicious"] + "[dusted=0]"
            nbt[(x, y, z)] = {"id": "minecraft:brushable_block", "LootTable": "chronogenesis:archaeology/precursor_" + c.k}
    return out, nbt

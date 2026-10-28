"""
ChronoGenesis texture generator (16x16 pixel art).

Style: Frankenstein (stitches, staples, electrodes, jars), Call of Cthulhu (eyes, maws, tentacles,
eldritch glows) and steampunk (brass, copper, rivets, valves, gauges).

Usage:  python3 tools/textures.py src/main/resources/assets/chronogenesis/textures [preview.png]

Layer conventions (must match the models in datagen):
  * Containers (syringe, vial, ampoule): <name> = vessel, <name>_fill = liquid mask (tinted by aspect),
    <name>_front = untinted details drawn over the liquid.
  * Culture: culture = growth (tinted by aspect), culture_dish = untinted dish drawn on top.
  * Flesh and organism core: <name>_<aspect> = flesh (tinted by aspect), <name>_<aspect>_overlay = untinted details.
"""
import math
import os
import random
import sys

from PIL import Image

OUT = sys.argv[1]
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else None
WRITTEN = []

# ---------------------------------------------------------------- palette

CLEAR = (0, 0, 0, 0)
OUTLINE = (34, 24, 22, 255)

BRASS_HI = (252, 226, 146, 255)
BRASS = (214, 168, 80, 255)
BRASS_DK = (152, 108, 46, 255)
BRASS_DDK = (92, 62, 30, 255)

COPPER_HI = (238, 164, 112, 255)
COPPER = (192, 108, 66, 255)
COPPER_DK = (128, 66, 40, 255)

VERD_HI = (132, 206, 176, 255)
VERD = (80, 152, 128, 255)
VERD_DK = (46, 98, 84, 255)

STEEL_HI = (226, 230, 238, 255)
STEEL = (164, 170, 182, 255)
STEEL_DK = (98, 102, 116, 255)
STEEL_DDK = (58, 60, 70, 255)

GLASS_EDGE = (186, 214, 224, 235)
GLASS_EDGE_DK = (120, 150, 165, 235)
GLASS_IN = (205, 232, 242, 70)
GLINT = (255, 255, 255, 255)

CORK = (164, 116, 70, 255)
CORK_DK = (110, 74, 42, 255)

FLESH_HI = (240, 178, 176, 255)
FLESH = (204, 124, 130, 255)
FLESH_DK = (152, 80, 94, 255)
FLESH_DDK = (98, 44, 60, 255)

BONE_HI = (248, 242, 222, 255)
BONE = (224, 212, 180, 255)
BONE_DK = (168, 152, 118, 255)

AME_HI = (236, 200, 255, 255)
AME = (174, 112, 224, 255)
AME_DK = (108, 58, 160, 255)

TEAL_HI = (176, 255, 240, 255)
TEAL = (64, 210, 196, 255)
TEAL_DK = (24, 118, 118, 255)

AMBER_HI = (255, 214, 120, 255)
AMBER = (232, 150, 40, 255)
AMBER_DK = (150, 84, 20, 255)

ELDRITCH = (166, 255, 128, 255)
ELDRITCH_DK = (70, 150, 60, 255)

THREAD = (40, 26, 30, 255)
THREAD_HI = (90, 66, 70, 255)


def new():
    return Image.new("RGBA", (16, 16), CLEAR)


def save(img, path):
    full = os.path.join(OUT, path + ".png")
    os.makedirs(os.path.dirname(full), exist_ok=True)
    img.save(full)
    WRITTEN.append((path, img))


def grid(rows, palette, img=None):
    """Draws a 16x16 character grid. '.' is transparent / untouched."""
    img = img or new()
    assert len(rows) == 16, f"grid has {len(rows)} rows"
    for y, row in enumerate(rows):
        assert len(row) == 16, f"row {y} has {len(row)} chars: {row!r}"
        for x, ch in enumerate(row):
            if ch != ".":
                img.putpixel((x, y), palette[ch])
    return img


def shade(c, amount):
    return tuple(max(0, min(255, v + amount)) for v in c[:3]) + (c[3],)


def gray(v, a=255):
    v = max(0, min(255, int(v)))
    return (v, v, v, a)


def rng(seed):
    return random.Random(seed)


# ---------------------------------------------------------------- diagonal items
# Items drawn along the anti-diagonal (bottom-left to top-right), parametrised by
# s = position along the item (0 at bottom-left, 15 at top-right) and
# v = perpendicular offset (negative = upper-left/lit side, positive = lower-right/shadow side).

def diagonal(shape):
    img = new()
    for y in range(16):
        for x in range(16):
            v = x + y - 15
            s = (x - y + 15) / 2.0
            c = shape(s, v)
            if c:
                img.putpixel((x, y), c)
    return img


def syringe():
    def body(s, v):
        av = abs(v)
        if 0.5 <= s <= 2.0 and av <= 2:  # plunger ring handle
            if 1.0 <= s <= 1.5 and av <= 0:
                return None
            return BRASS_HI if v < 0 else BRASS_DK if v > 0 else BRASS
        if 2.0 < s <= 3.5 and av <= 0:  # plunger rod
            return STEEL_HI if s < 3 else STEEL
        if 3.5 < s <= 4.5 and av <= 2:  # rear cap
            return BRASS_HI if v < -1 else BRASS_DDK if v > 1 else BRASS
        if 4.5 < s <= 10.5 and av <= 2:  # glass barrel
            if v == -2:
                return GLASS_EDGE
            if v == 2:
                return GLASS_EDGE_DK
            return GLASS_IN
        if 10.5 < s <= 11.5 and av <= 2:  # front cap
            return BRASS_HI if v < -1 else BRASS_DDK if v > 1 else BRASS
        if 11.5 < s <= 12.5 and av <= 1:  # needle hub
            return BRASS_DK
        if 12.5 < s <= 15 and av <= 0:  # needle
            return STEEL_HI if s > 14 else STEEL
        return None

    def fill(s, v):
        if 4.5 < s <= 10.5 and -1 <= v <= 1:
            return gray({-1: 250, 0: 225, 1: 190}[v])
        return None

    def front(s, v):
        if 4.5 < s <= 10.5:
            if v == -1 and 5.0 <= s <= 6.5:
                return GLINT
            if v == 1 and s in (6.5, 8.0, 9.5):  # brass graduation ticks
                return BRASS
        return None

    save(diagonal(body), "item/chrono_syringe")
    save(diagonal(fill), "item/chrono_syringe_fill")
    save(diagonal(front), "item/chrono_syringe_front")


def needle(name, hub, shaft, tip, wide=False):
    def shape(s, v):
        av = abs(v)
        if 1.0 <= s <= 4.0 and av <= 2:
            return hub(s, v)
        if 4.0 < s <= 13.5:
            if v in (0, 1):  # two pixels wide, shadowed underside
                c = shaft(s, v)
                return shade(c, -45) if v == 1 else c
            if wide and v == -1 and s <= 11.0:
                return shaft(s, v)
        if 13.5 < s <= 15.0 and av == 0:
            return tip
        return None

    save(diagonal(shape), "item/" + name)


def needles():
    # Iron: gunmetal collar with a rivet.
    needle("iron_needle",
           lambda s, v: STEEL_HI if (s, v) == (2.5, -1) else STEEL_DDK if v > 0 else STEEL_DK,
           lambda s, v: STEEL_HI if s < 8 else STEEL,
           STEEL_HI)
    # Gold: a toothed brass collar (a tiny gear).
    needle("gold_needle",
           lambda s, v: BRASS_HI if (int(s * 2) % 2 == 0 and v != 0) else BRASS_DK if v > 0 else BRASS,
           lambda s, v: AMBER_HI if s < 9 else (250, 200, 90, 255),
           BRASS_HI)
    # Bone: wrapped in dark sinew.
    needle("bone_needle",
           lambda s, v: (110, 48, 40, 255) if int(s * 2) % 2 == 0 else (150, 74, 62, 255),
           lambda s, v: BONE_HI if s < 8 else BONE if s < 12 else BONE_DK,
           BONE_HI)
    # Amethyst: a faceted crystal shard in a brass collar, glowing at the tip.
    needle("amethyst_needle",
           lambda s, v: BRASS_HI if v < 0 else BRASS_DK if v > 0 else BRASS,
           lambda s, v: AME_HI if v < 0 else AME_DK if v > 0 else AME,
           (255, 230, 255, 255), wide=True)


# ---------------------------------------------------------------- vessels

def vial():
    p = {"a": CORK_DK, "A": CORK, "k": BRASS_DDK, "B": BRASS, "Y": BRASS_HI, "b": BRASS_DK,
         "g": GLASS_EDGE, "G": GLASS_EDGE_DK, "I": GLASS_IN}
    save(grid([
        "................",
        "......aAAa......",
        "......aAAa......",
        ".....kYBBbk.....",
        "......gIIG......",
        "......gIIG......",
        ".....gIIIIG.....",
        "...ggIIIIIIGG...",
        "..gIIIIIIIIIIG..",
        ".gIIIIIIIIIIIIG.",
        ".gIIIIIIIIIIIIG.",
        ".gIIIIIIIIIIIIG.",
        ".gIIIIIIIIIIIIG.",
        "..gIIIIIIIIIIG..",
        "...gGIIIIIIGG...",
        ".....GGGGGG.....",
    ], p), "item/essence_vial")

    fill = new()
    inside = [(2, 13), (1, 14), (1, 14), (1, 14), (1, 14), (2, 13), (4, 11)]
    for i, (x0, x1) in enumerate(inside):
        y = 8 + i
        for x in range(x0 + 1, x1):
            v = 250 - i * 9 - (22 if x >= x1 - 2 else 0) + (10 if x <= x0 + 2 else 0)
            fill.putpixel((x, y), gray(v))
    save(fill, "item/essence_vial_fill")

    save(grid([
        "................",
        "................",
        "................",
        "................",
        "................",
        "......W.........",
        "................",
        "...kbBBBBBBBbk..",
        "................",
        "...W............",
        "..WW............",
        "..W.............",
        "................",
        "................",
        "....kbBBBBBbk...",
        "................",
    ], {"W": GLINT, "k": BRASS_DDK, "b": BRASS_DK, "B": BRASS}), "item/essence_vial_front")


def ampoule():
    p = {"k": BRASS_DDK, "B": BRASS, "Y": BRASS_HI, "b": BRASS_DK, "m": (96, 82, 118, 255),
         "M": (132, 116, 156, 255), "I": (60, 58, 84, 150), "s": STEEL}
    save(grid([
        ".......ss.......",
        "......kBBk......",
        ".....kYBBbk.....",
        ".....kBBBbk.....",
        ".....MIIIIm.....",
        ".....MIIIIm.....",
        ".....MIIIIm.....",
        ".....MIIIIm.....",
        ".....MIIIIm.....",
        ".....MIIIIm.....",
        ".....MIIIIm.....",
        ".....MIIIIm.....",
        ".....kYBBbk.....",
        ".....kBBBbk.....",
        "......kBBk......",
        ".......kk.......",
    ], p), "item/stasis_ampoule")

    fill = new()
    for y in range(5, 12):
        for x in range(6, 10):
            fill.putpixel((x, y), gray(240 - (y - 5) * 6 - (30 if x == 9 else 0) + (10 if x == 6 else 0)))
    save(fill, "item/stasis_ampoule_fill")

    # An eldritch ward scratched into the glass, glowing faintly.
    save(grid([
        "................",
        "................",
        "................",
        "................",
        "......W.........",
        "......W..e......",
        "........eEe.....",
        ".........e......",
        "........e.e.....",
        ".........E......",
        "........e.e.....",
        "................",
        "................",
        "................",
        "................",
        "................",
    ], {"W": GLINT, "e": ELDRITCH_DK, "E": ELDRITCH}), "item/stasis_ampoule_front")


# ---------------------------------------------------------------- biology items

def culture():
    # The growth: greyscale, tinted by the culture's aspect. Tentacles reach out of the dish.
    p = {"w": gray(150), "W": gray(212), "H": gray(250), "d": gray(110)}
    save(grid([
        "................",
        "................",
        "...........w....",
        "....w......w....",
        "....w.....w.....",
        ".....w....w.....",
        ".....w...w...w..",
        "......wwWw..w...",
        "....wwWWHWww....",
        "...wWWHHWWWWw...",
        "...wWWWWWdWWw...",
        "....wWdWWWWw....",
        ".....wwwwww.....",
        "................",
        "................",
        "................",
    ], p), "item/culture")

    # The dish: a brass-rimmed petri dish. Back rim is glass, front rim brass.
    dish = new()
    cx, cy, rx, ry = 7.5, 10.5, 7.0, 3.6
    for y in range(16):
        for x in range(16):
            d = ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2
            if 0.62 <= d <= 1.08:
                if y < cy:
                    dish.putpixel((x, y), GLASS_EDGE)
                else:
                    dish.putpixel((x, y), BRASS_DK if y >= 13 else BRASS)
            elif d < 0.62 and y >= 12:
                dish.putpixel((x, y), (205, 232, 242, 50))
    for x in range(3, 13):
        dish.putpixel((x, 14), BRASS_DDK)
    for p_ in [(2, 12), (13, 12)]:
        dish.putpixel(p_, BRASS_HI)
    save(dish, "item/culture_dish")


def brain():
    # A brain in a jar of preserving fluid, two electrodes through the brass lid.
    p = {"Y": AMBER_HI, "s": STEEL, "S": STEEL_DK, "k": BRASS_DDK, "B": BRASS, "b": BRASS_DK, "H": BRASS_HI,
         "g": GLASS_EDGE, "G": GLASS_EDGE_DK, "F": (130, 182, 120, 190), "f": (100, 150, 96, 200),
         "o": (200, 240, 190, 220), "p": (166, 74, 92, 255), "P": (226, 142, 152, 255), "h": (252, 196, 200, 255),
         "r": (186, 98, 114, 255), "T": (140, 60, 76, 255), "W": GLINT}
    save(grid([
        "....Y.....Y.....",
        "....s.....s.....",
        "..kHBBBBBBBBbk..",
        "..kbBBBBBBBbbk..",
        "...gFFsFFFsFG...",
        "...gFoSFFFSFG...",
        "...gFFpPPPpFG...",
        "...gWpPhPrPpG...",
        "...gWPrPPPrPG...",
        "...gpPPrPPPPp...",
        "...gFpPPrPpFG...",
        "...gFFppTppoG...",
        "...gFFFFTFFFG...",
        "...gffFoffffG...",
        "...gffffffffG...",
        "...kbBBBBBBbk...",
    ], p), "item/preserved_brain")


def residue():
    # Crystallised time: teal crystals around an amber clockwork core.
    p = {"t": TEAL_DK, "T": TEAL, "L": TEAL_HI, "A": AMBER, "a": AMBER_DK, "O": AMBER_HI, "k": (40, 46, 56, 255),
         "K": (70, 78, 90, 255)}
    save(grid([
        "........t.......",
        ".......tLt......",
        ".......tLT......",
        "......tLLTt.....",
        "......tLTTt.....",
        "..t...tAaAt.....",
        ".tLt..aAOAa..t..",
        ".tLt..AOaOA.tLt.",
        ".tLTt.aAOAa.tLt.",
        ".tLTt.tAaAt.tLTt",
        "tLLTt.tLTTt.tLTt",
        "tLTTttLLTTTttTTt",
        ".tTTTtLTTTTtTTt.",
        "..KKKKKKKKKKKKK.",
        "..kkkkkkkkkkkkk.",
        "................",
    ], p), "item/temporal_residue")


# ---------------------------------------------------------------- blocks

def flesh_base(seed, base=205):
    """Greyscale-ish wrinkled flesh. Tinted in-game by the organism's aspect."""
    r = rng(seed)
    img = new()
    for y in range(16):
        for x in range(16):
            n = r.randint(-10, 10)
            fold = 10 * math.sin((x * 0.9 + y * 0.35) + math.sin(y * 0.7) * 1.4)
            v = base + n + fold
            img.putpixel((x, y), (int(min(255, v + 8)), int(v), int(v - 4), 255))
    # creases
    for _ in range(3):
        x, y = r.randint(0, 15), r.randint(0, 15)
        for _ in range(7):
            c = img.getpixel((x, y))
            img.putpixel((x, y), shade(c, -48))
            x = (x + r.choice([1, 1, 0])) % 16
            y = (y + r.choice([-1, 0, 1])) % 16
    # pores
    for _ in range(6):
        x, y = r.randint(0, 15), r.randint(0, 15)
        img.putpixel((x, y), shade(img.getpixel((x, y)), -70))
    return img


def organism_core():
    # The seed's flesh, veins converging on the centre (each essence's awake core has its own; see organism_textures).
    img = flesh_base(23, base=200)
    r = rng(5)
    for angle in [0.3, 1.4, 2.5, 3.7, 4.8, 5.8]:
        for dist in range(6, 9):
            x = int(round(7.5 + math.cos(angle) * dist))
            y = int(round(7.5 + math.sin(angle) * dist))
            if 0 <= x < 16 and 0 <= y < 16:
                img.putpixel((x, y), shade(img.getpixel((x, y)), -55))
    save(img, "block/organism_core")


def necrotic():
    r = rng(99)
    img = new()
    for y in range(16):
        for x in range(16):
            n = r.randint(-14, 14)
            img.putpixel((x, y), (74 + n, 64 + n, 56 + n, 255))
    for _ in range(14):
        x, y = r.randint(0, 15), r.randint(0, 15)
        img.putpixel((x, y), (36, 30, 28, 255))
    for _ in range(8):
        x, y = r.randint(0, 15), r.randint(0, 15)
        img.putpixel((x, y), (112, 126, 70, 255))
    # The eye, stitched shut. A rusted staple.
    rust = (140, 70, 40, 255)
    grid([
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "....t..t..t.....",
        "...dDDDDDDDDd...",
        "....t..t..t.....",
        "................",
        "................",
        "................",
        ".........rRr....",
        "................",
        "................",
        "................",
    ], {"t": THREAD, "d": (40, 30, 28, 255), "D": (60, 46, 42, 255), "r": (90, 44, 26, 255), "R": rust}, img)
    save(img, "block/organism_core_necrotic")


def graft_flesh(seed, base, dark):
    r = rng(seed)
    img = new()
    for y in range(16):
        for x in range(16):
            n = r.randint(-12, 12)
            fold = 12 * math.sin(x * 0.8 + math.sin(y * 0.6) * 1.6)
            img.putpixel((x, y), shade(base, int(n + fold)))
    for _ in range(5):
        x, y = r.randint(0, 15), r.randint(0, 15)
        for _ in range(5):
            img.putpixel((x, y), dark)
            x = (x + r.choice([1, 0])) % 16
            y = (y + r.choice([-1, 1])) % 16
    return img


def tap():
    img = graft_flesh(41, FLESH, FLESH_DK)
    # A brass valve wheel bolted into the flesh, with a spout.
    grid([
        "................",
        "................",
        "................",
        "......kbbk......",
        "....kbB..Bbk....",
        "....b.kBBk.b....",
        "...kB.B..B.Bk...",
        "...bB.BxxB.Bb...",
        "...bB.BxxB.Bb...",
        "...kB.B..B.Bk...",
        "....b.kBBk.b....",
        "....kbB..Bbk....",
        "......kHHk......",
        ".......ss.......",
        ".......ss.......",
        ".......SS.......",
    ], {"k": BRASS_DDK, "b": BRASS_DK, "B": BRASS, "H": BRASS_HI, "x": OUTLINE, "s": COPPER, "S": COPPER_DK}, img)
    save(img, "block/tap")


def leech_tendril():
    img = graft_flesh(52, (150, 62, 72, 255), (90, 30, 40, 255))
    # A lamprey maw: rings of hooked teeth around a black throat.
    grid([
        "................",
        "................",
        "......dddd......",
        "....ddwddwdd....",
        "...dwdrrrrdwd...",
        "...ddrwrrwrdd...",
        "..dwrwrxxrwrwd..",
        "..ddrrxXXxrrdd..",
        "..ddrrxXXxrrdd..",
        "..dwrwrxxrwrwd..",
        "...ddrwrrwrdd...",
        "...dwdrrrrdwd...",
        "....ddwddwdd....",
        "......dddd......",
        "................",
        "................",
    ], {"d": (96, 30, 42, 255), "r": (184, 70, 84, 255), "w": BONE_HI, "x": (40, 10, 16, 255),
        "X": (10, 4, 6, 255)}, img)
    save(img, "block/leech_tendril")


def enhancing_gland():
    img = graft_flesh(63, (222, 140, 168, 255), (170, 90, 120, 255))
    # Blue veins feeding a swollen gland, pierced by a brass injection port.
    vein = (90, 96, 170, 255)
    for (x0, y0, dx, dy) in [(0, 3, 1, 1), (15, 2, -1, 1), (0, 12, 1, -1), (15, 13, -1, -1)]:
        x, y = x0, y0
        for _ in range(5):
            img.putpixel((x, y), vein)
            x += dx
            y += dy if _ % 2 == 0 else 0
    grid([
        "................",
        "................",
        "................",
        ".....gGGGGg.....",
        "....gGhGGGGg....",
        "...gGhGkbbkGg...",
        "...gGGkBHBkGg...",
        "...gGGbHsBbGg...",
        "...gGGbBSBbGg...",
        "...gGGkbBbkGg...",
        "...gGGGkkkGGg...",
        "....gGGGGGGg....",
        ".....gGGGGg.....",
        "................",
        "................",
        "................",
    ], {"g": (176, 80, 120, 255), "G": (238, 150, 188, 255), "h": (255, 210, 228, 255), "k": BRASS_DDK,
        "b": BRASS_DK, "B": BRASS, "H": BRASS_HI, "s": STEEL_HI, "S": STEEL}, img)
    save(img, "block/enhancing_gland")


def essence_jug():
    # A demijohn of dark green glass in a riveted brass cage, corked.
    p = {"g": (36, 70, 52, 255), "G": (54, 100, 74, 255), "L": (96, 150, 112, 255), "W": (180, 220, 196, 255),
         "k": BRASS_DDK, "b": BRASS_DK, "B": BRASS, "H": BRASS_HI, "a": CORK_DK, "A": CORK}
    save(grid([
        "GGGGGGGGGGGGGGGG",
        "GLGGGGGGGGGGGGgG",
        "GGGGGGGGGGGGGGGg",
        "gggggaAAAAagggg.",
        "GGGGGaAAAAaGGGG.",
        "kbbbbbkkkkbbbbbk",
        "GWLGGGGGGGGGGGGg",
        "kBHBBBBBBBBBBBbk",
        "GWLGGGbGGGGbGGGg",
        "GWLGGGbGGGGbGGgg",
        "GLGGGGbGGGGbGGgg",
        "GLGGGGbGGGGbGGgg",
        "GGGGGGbGGGGbGggg",
        "GGGGGGbGGGGbGggg",
        "kBHBBBBBBBBBBBbk",
        "kkkkkkkkkkkkkkkk",
    ], p), "block/essence_jug")


def essence_jug_fill():
    # Liquid seen through the glass band between the jug's brass hoops (rows 8-13 of the side), tinted by
    # aspect. The cage bars (columns 6 and 11) stay clear so they read in front of the liquid. The model
    # shows rows from the bottom up to the jug's fill level.
    p = {"l": (200, 200, 200, 255), "L": (236, 236, 236, 255), "W": (255, 255, 255, 255)}
    save(grid([
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "LLWLLL.LLLL.LLLL",
        "lLllll.llll.llll",
        "llllll.llll.llll",
        "llllll.llll.llll",
        "llllll.llll.llll",
        "llllll.llll.llll",
        "................",
        "................",
    ], p), "block/essence_jug_fill")


def essence_jug_top():
    # Top of the jug: green glass shoulder in a brass rim, the cork in the middle (the neck samples the centre).
    p = {"G": (54, 100, 74, 255), "g": (36, 70, 52, 255), "L": (96, 150, 112, 255), "k": BRASS_DDK,
         "b": BRASS_DK, "B": BRASS, "H": BRASS_HI, "a": CORK_DK, "A": CORK, "c": (196, 150, 98, 255)}
    save(grid([
        "kbbbbbbbbbbbbbbk",
        "bHBBBBBBBBBBBBBb",
        "bBGGGGGGGGGGGGBb",
        "bBGLLGGGGGGGGgBb",
        "bBGLGGGGGGGGGgBb",
        "bBGGGkbbbbkGGgBb",
        "bBGGGbaAAabGGgBb",
        "bBGGGbAcAAbGGgBb",
        "bBGGGbAAcAbGGgBb",
        "bBGGGbaAAabGGgBb",
        "bBGGGkbbbbkGGgBb",
        "bBGGGGGGGGGGGgBb",
        "bBGGGGGGGGGGgggb",
        "bBgggggggggggggb",
        "bbBBBBBBBBBBBBbb",
        "kbbbbbbbbbbbbbbk",
    ], p), "block/essence_jug_top")


def time_tear():
    # A wound in time: a black eye of void, teal spiral arms, amber shards of clockwork.
    img = new()
    r = rng(7)
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            d = math.hypot(dx, dy)
            if d > 7.8:
                continue
            ang = math.atan2(dy, dx)
            arm = math.sin(3 * ang + d * 0.9)
            alpha = int(255 * max(0.0, 1.0 - (d / 8.0) ** 1.6))
            if d < 2.2:
                c = (8, 10, 18, 255)
            elif arm > 0.55:
                c = TEAL_HI[:3] + (alpha,)
            elif arm > 0.0:
                c = TEAL[:3] + (alpha,)
            else:
                c = (20, 40, 60, int(alpha * 0.85))
            img.putpixel((x, y), c)
    # An eldritch pupil in the void, and shards of clock hands.
    img.putpixel((7, 7), ELDRITCH)
    img.putpixel((8, 8), ELDRITCH_DK)
    for (x, y) in [(3, 4), (4, 5), (12, 10), (11, 11), (10, 3)]:
        img.putpixel((x, y), AMBER_HI)
    for _ in range(4):
        x, y = r.randint(2, 13), r.randint(2, 13)
        if math.hypot(x - 7.5, y - 7.5) > 3:
            img.putpixel((x, y), (255, 255, 255, 230))
    save(img, "block/time_tear")


# ---------------------------------------------------------------- brass (material)

YB_HI = (252, 232, 150, 255)
YB_LT = (236, 202, 108, 255)
YB = (214, 172, 76, 255)
YB_DK = (170, 128, 50, 255)
YB_DDK = (118, 84, 32, 255)


def brass_block():
    # In the manner of vanilla metal blocks: mostly one calm tone with soft lighter streaks, a few
    # bright flecks, and no hard border so it tiles cleanly.
    r = rng(71)
    img = new()
    for y in range(16):
        for x in range(16):
            n = (math.sin((x + y * 0.5) * 0.785) + 0.6 * math.sin((y - x * 0.4) * 0.785 * 2)
                 + r.uniform(-0.2, 0.2))
            c = YB_LT if n > 1.05 else YB_DK if n < -1.15 else YB
            img.putpixel((x, y), c)
    for (x, y) in [(2, 3), (9, 1), (13, 9), (5, 12), (11, 14)]:
        img.putpixel((x, y), YB_HI)
    save(img, "block/brass_block")


def brass_ingot():
    p = {"k": YB_DDK, "H": YB_HI, "Y": YB_LT, "M": YB, "d": YB_DK, "W": (255, 250, 220, 255)}
    save(grid([
        "................",
        "................",
        "................",
        "................",
        ".......kkkkkkk..",
        ".....kkHHHHHHk..",
        "...kkHHWHHHHdk..",
        "..kHHHHHHHHddk..",
        "..kYYYYYYYYddk..",
        "..kYWMMMMMMddk..",
        "..kMMMMMMMMdk...",
        "..kddddddddk....",
        "...kkkkkkkk.....",
        "................",
        "................",
        "................",
    ], p), "item/brass_ingot")


def brass_nugget():
    p = {"k": YB_DDK, "H": YB_HI, "Y": YB_LT, "M": YB, "d": YB_DK}
    save(grid([
        "................",
        "................",
        "................",
        "................",
        "................",
        "......kkk.......",
        ".....kHHYk......",
        "....kHYYMdk.....",
        "....kYMMMdk.kk..",
        ".....kddddk.kHk.",
        "......kkkk..kdk.",
        ".............k..",
        "................",
        "................",
        "................",
        "................",
    ], p), "item/brass_nugget")


def brass_blend():
    # A heap of crushed copper and gold, not yet one metal: orange and yellow grains.
    r = rng(73)
    img = new()
    heap = [(6, 9), (4, 11), (3, 12), (2, 13), (2, 13), (3, 12)]
    for i, (x0, x1) in enumerate(heap):
        y = 7 + i
        for x in range(x0, x1 + 1):
            c = r.choice([COPPER, COPPER_HI, COPPER_DK, YB, YB_LT, YB_HI, YB_DK])
            if x in (x0, x1) or i == len(heap) - 1:
                c = shade(c, -40)
            img.putpixel((x, y), c)
    for (x, y) in [(7, 6), (8, 6), (5, 8), (10, 8)]:
        img.putpixel((x, y), r.choice([COPPER_HI, YB_HI]))
    save(img, "item/brass_blend")


# ---------------------------------------------------------------- 3D model parts

def brass_fitting():
    r = rng(19)
    img = new()
    for y in range(16):
        for x in range(16):
            c = BRASS_HI if y % 8 == 0 else BRASS if y % 8 < 6 else BRASS_DK
            img.putpixel((x, y), shade(c, r.randint(-6, 6)))
    for (x, y) in [(2, 2), (13, 2), (2, 13), (13, 13), (7, 7)]:
        img.putpixel((x, y), BRASS_HI)
        img.putpixel((x + 1, y + 1), BRASS_DDK)
    save(img, "block/brass_fitting")


def steel():
    img = new()
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), STEEL_HI if x % 4 == 0 else STEEL if x % 4 < 3 else STEEL_DK)
    save(img, "block/steel")


def graft_flesh_texture():
    # Plain pink flesh for the mounds grafts grow out of.
    save(graft_flesh(47, FLESH, FLESH_DK), "block/graft_flesh")


def gland_sac():
    # The gland's swollen sac: tight pink skin with blue veins (no port; the 3D model has a real one).
    img = graft_flesh(64, (226, 146, 172, 255), (186, 104, 136, 255))
    r = rng(65)
    vein = (96, 100, 176, 255)
    for _ in range(4):
        x, y = r.randint(0, 15), r.randint(0, 15)
        for _ in range(7):
            img.putpixel((x, y), vein)
            x = (x + r.choice([1, 1, 0])) % 16
            y = (y + r.choice([-1, 0, 1])) % 16
    for (x, y) in [(4, 4), (11, 6), (6, 11)]:
        img.putpixel((x, y), (255, 216, 232, 255))
    save(img, "block/gland_sac")


def leech_skin():
    # Dark, ringed lamprey skin with pale ridges between segments.
    r = rng(29)
    img = new()
    for y in range(16):
        for x in range(16):
            base = (140, 56, 68, 255) if y % 4 else (204, 120, 128, 255)
            if y % 4 == 3:
                base = (88, 30, 42, 255)
            img.putpixel((x, y), shade(base, r.randint(-10, 10)))
    for _ in range(6):
        img.putpixel((r.randint(0, 15), r.randint(0, 15)), (230, 170, 170, 255))
    save(img, "block/leech_skin")


def flesh_pustule():
    # Swollen, glistening sacs (untinted): sickly yellow with a wet highlight.
    r = rng(31)
    img = new()
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), shade((214, 196, 110, 255), r.randint(-14, 14)))
    for (x, y) in [(3, 3), (10, 5), (5, 11), (12, 12)]:
        img.putpixel((x, y), (255, 250, 220, 255))
        img.putpixel((x + 1, y + 1), (160, 130, 60, 255))
    save(img, "block/flesh_pustule")


# ---------------------------------------------------------------- veins, new grafts

def vein():
    # Living pipe: dark red tissue in rings, threaded with blue veins.
    img = graft_flesh(81, (150, 46, 58, 255), (96, 24, 36, 255))
    for y in range(0, 16, 4):
        for x in range(16):
            img.putpixel((x, y), shade(img.getpixel((x, y)), -30))
    r = rng(82)
    for _ in range(3):
        x, y = r.randint(0, 15), r.randint(0, 15)
        for _ in range(8):
            img.putpixel((x, y), (80, 84, 160, 255))
            x = (x + 1) % 16
            y = (y + r.choice([-1, 0, 1])) % 16
    save(img, "block/vein")


def vein_clogged():
    # A pulse that died in the vein: grey-brown rot weeping sickly pus.
    r = rng(83)
    img = new()
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), shade((82, 64, 52, 255), r.randint(-16, 16)))
    for _ in range(12):
        img.putpixel((r.randint(0, 15), r.randint(0, 15)), r.choice([(170, 176, 80, 255), (120, 130, 60, 255)]))
    for _ in range(8):
        img.putpixel((r.randint(0, 15), r.randint(0, 15)), (34, 26, 24, 255))
    save(img, "block/vein_clogged")


def maw():
    # A puckered, toothed mouth (the top of the Maw's brass ring samples the middle 8x8).
    img = graft_flesh(84, FLESH, FLESH_DK)
    grid([
        "................",
        "................",
        "................",
        "................",
        "....dddddddd....",
        "....dLwLLwLd....",
        "....dwrrrrwd....",
        "....dLrxxrLd....",
        "....dLrxxrLd....",
        "....dwrrrrwd....",
        "....dLwLLwLd....",
        "....dddddddd....",
        "................",
        "................",
        "................",
        "................",
    ], {"d": FLESH_DDK, "L": (190, 96, 108, 255), "w": BONE_HI, "r": (120, 36, 50, 255), "x": (24, 8, 12, 255)}, img)
    save(img, "block/maw")


def stasis_bladder():
    # Dark, stretched membrane (phantom-like), purple veins, a wet sheen.
    r = rng(85)
    img = new()
    for y in range(16):
        for x in range(16):
            v = 10 * math.sin(x * 0.6 + y * 0.3)
            img.putpixel((x, y), shade((84, 72, 112, 255), int(v + r.randint(-8, 8))))
    for _ in range(3):
        x, y = r.randint(0, 15), 0
        for _ in range(16):
            img.putpixel((x, y), (150, 90, 190, 255))
            x = (x + r.choice([-1, 0, 1])) % 16
            y += 1
            if y > 15:
                break
    for (x, y) in [(3, 3), (4, 3), (3, 4), (11, 9), (12, 9)]:
        img.putpixel((x, y), (200, 190, 230, 255))
    save(img, "block/stasis_bladder")


def dial(name, face, hand, marks):
    # A brass-rimmed gauge face filling the middle of the texture.
    img = new()
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), BRASS_DK)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 7.6:
                img.putpixel((x, y), BRASS if d > 6.2 else face)
            if 6.2 <= d < 7.0 and (x + y) % 3 == 0:
                img.putpixel((x, y), BRASS_HI)
    for (x, y) in marks:
        img.putpixel((x, y), OUTLINE)
    for (x, y) in hand:
        img.putpixel((x, y), (150, 30, 30, 255))
    img.putpixel((7, 7), BRASS_DDK)
    img.putpixel((8, 8), BRASS_DDK)
    save(img, "block/" + name)


def gauges():
    marks = [(7, 2), (8, 2), (13, 7), (13, 8), (2, 7), (2, 8), (3, 3), (12, 3), (3, 12), (12, 12)]
    dial("starting_gate", (236, 228, 204, 255), [(8, 6), (9, 5), (10, 4)], marks)
    dial("finish", (232, 170, 70, 255), [(7, 6), (7, 5), (7, 4), (9, 8), (10, 8)], marks)


# ---------------------------------------------------------------- Heart, Eye and Clock grafts

def heart_specimen():
    # An animal heart in a jar of preserving fluid, a brass tube still stitched into the aorta.
    p = {"s": STEEL, "k": BRASS_DDK, "B": BRASS, "b": BRASS_DK, "H": BRASS_HI,
         "g": GLASS_EDGE, "G": GLASS_EDGE_DK, "F": (176, 150, 110, 190), "f": (150, 124, 90, 200),
         "o": (230, 214, 170, 220), "r": (150, 30, 44, 255), "R": (196, 54, 64, 255), "h": (236, 120, 120, 255),
         "v": (70, 60, 130, 255), "t": THREAD, "W": GLINT}
    save(grid([
        "................",
        "......BH........",
        "..kHBBBbBBBBbk..",
        "..kbBBBbBBBbbk..",
        "...gFFFbFvFFG...",
        "...gFFFbvvFFG...",
        "...gFRRRrRRFG...",
        "...gWRhRRRrRG...",
        "...gRhRRtRRrG...",
        "...gRRRRtRRrG...",
        "...gFRRRtRrFG...",
        "...gFFRRRrFoG...",
        "...gFFFRrFFFG...",
        "...gffFoffffG...",
        "...gffffffffG...",
        "...kbBBBBBBbk...",
    ], p), "item/preserved_heart")


def heart_muscle():
    # Heart graft: dark red muscle in diagonal bands, blue and red vessels, a stitched seam.
    r = rng(91)
    img = new()
    for y in range(16):
        for x in range(16):
            v = 14 * math.sin((x + y) * 0.7)
            img.putpixel((x, y), shade((150, 36, 50, 255), int(v + r.randint(-10, 10))))
    for start, colour in [((2, 0), (70, 64, 140, 255)), ((11, 0), (210, 70, 70, 255))]:
        x, y = start
        while y < 16:
            img.putpixel((x % 16, y), colour)
            x += r.choice([-1, 0, 0, 1])
            y += 1
    for y in range(3, 13, 2):
        img.putpixel((7, y), THREAD)
        img.putpixel((8, y + 1), THREAD_HI)
    save(img, "block/heart")


def eye_graft():
    # Eye graft: a lidded eyeball with an eldritch green iris, watching what passes.
    p = {"l": (150, 80, 90, 255), "L": (190, 110, 120, 255), "w": (236, 228, 212, 255), "W": GLINT,
         "s": (200, 186, 170, 255), "r": (200, 70, 70, 255), "i": ELDRITCH_DK, "I": ELDRITCH, "x": (12, 8, 12, 255),
         "t": THREAD}
    save(grid([
        "llllllllllllllll",
        "lLLLLLLLLLLLLLLl",
        "lLLtLLtLLtLLtLLl",
        "lLsssssssssssLLl",
        "lLswwwwwwwwwsLLl",
        "lswwwwiiiiwwrwsl",
        "lswwwiIIIIiwwwsl",
        "lswwiIIxxIIiwwsl",
        "lswrwiIxxIIiwwsl",
        "lswwwiIIIIiwWwsl",
        "lswwwwiiiiwwwwsl",
        "lLswwwwrwwwwwsLl",
        "lLssswwwwwwsssLl",
        "lLLLsssssssLLLLl",
        "lLLLLLLLLLLLLLLl",
        "llllllllllllllll",
    ], p), "block/eye")


def clock_graft():
    # Clock graft: a vanilla clock face set in brass, the hands stopped at nearly midnight.
    marks = [(7, 2), (8, 2), (13, 7), (13, 8), (2, 7), (2, 8), (7, 13), (8, 13),
             (4, 4), (11, 4), (4, 11), (11, 11)]
    dial("clock", (232, 222, 190, 255), [(7, 6), (7, 5), (7, 4), (7, 3), (8, 6), (9, 5)], marks)


# ---------------------------------------------------------------- the Chronocortex (build step 7)

def ganglion():
    # Ganglion graft: a knot of grey-pink nerve tissue, white nerve fibres running out of it.
    r = rng(101)
    img = new()
    for y in range(16):
        for x in range(16):
            v = 12 * math.sin(x * 0.9) * math.cos(y * 0.7)
            img.putpixel((x, y), shade((176, 140, 150, 255), int(v + r.randint(-8, 8))))
    for start in [(0, 4), (0, 11), (15, 7)]:
        x, y = start
        dx = 1 if x == 0 else -1
        for _ in range(16):
            if 0 <= x < 16 and 0 <= y < 16:
                img.putpixel((x, y), (236, 228, 220, 255))
            x += dx
            y += r.choice([-1, 0, 0, 1])
    for (x, y) in [(6, 6), (7, 6), (6, 7), (9, 9), (10, 8)]:
        img.putpixel((x, y), (120, 70, 96, 255))
    save(img, "block/ganglion")


def dissection_table():
    # Top: a zinc slab, stained, with a drain groove round the edge. Side: dark cast iron, rivets.
    r = rng(103)
    top = new()
    for y in range(16):
        for x in range(16):
            edge = x in (0, 15) or y in (0, 15)
            groove = x in (1, 14) or y in (1, 14)
            if edge:
                c = STEEL_DK
            elif groove:
                c = STEEL_DDK
            else:
                c = shade(STEEL, r.randint(-10, 6))
            top.putpixel((x, y), c)
    for (x, y) in [(5, 6), (6, 6), (6, 7), (7, 7), (10, 10), (11, 10), (4, 11)]:
        top.putpixel((x, y), (110, 52, 58, 255))  # old stains
    save(top, "block/dissection_table_top")

    side = new()
    for y in range(16):
        for x in range(16):
            side.putpixel((x, y), shade((62, 60, 66, 255), r.randint(-6, 6)))
    for x in (2, 13):
        for y in (3, 12):
            side.putpixel((x, y), STEEL_HI)
    save(side, "block/dissection_table_side")


def brain_tissue():
    # Folded brain matter for the jars: pink coils with darker sulci.
    img = new()
    for y in range(16):
        for x in range(16):
            v = math.sin(x * 1.3 + math.sin(y * 0.9) * 2.2) + math.sin(y * 1.1 + x * 0.3)
            c = (226, 142, 152, 255) if v > 0.6 else (186, 98, 114, 255) if v > -0.6 else (140, 60, 76, 255)
            img.putpixel((x, y), c)
    save(img, "block/brain_tissue")


def cortex_jar():
    # The jar's glass: clear, with a bright rim, a glint, and the preserving fluid seen through it.
    glass = new()
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                glass.putpixel((x, y), GLASS_EDGE)
            else:
                glass.putpixel((x, y), GLASS_IN)
    for y in range(2, 9):
        glass.putpixel((2, y), GLINT)
    save(glass, "block/cortex_glass")

    fluid = new()
    r = rng(107)
    for y in range(16):
        for x in range(16):
            fluid.putpixel((x, y), (120, 190, 120, 110 + r.randint(-10, 10)))
    for (x, y) in [(4, 12), (9, 6), (12, 10), (6, 3)]:
        fluid.putpixel((x, y), (210, 250, 200, 170))  # bubbles
    save(fluid, "block/cortex_fluid")


def essence_mote():
    # Particle: a soft round glow, white so the particle can tint it with the essence's colour.
    img = Image.new("RGBA", (8, 8), CLEAR)
    for y in range(8):
        for x in range(8):
            d = math.hypot(x - 3.5, y - 3.5)
            if d < 3.8:
                a = int(255 * max(0.0, 1.0 - d / 3.8) ** 1.4)
                img.putpixel((x, y), (255, 255, 255, a))
    img.putpixel((3, 3), GLINT)
    save(img, "particle/essence_mote")


def umbilical_locket():
    # A brass oval locket, a stitched seam down its face, hanging from a twisted fleshy cord.
    p = {"c": FLESH, "C": FLESH_HI, "d": FLESH_DK, "k": BRASS_DDK, "b": BRASS_DK, "B": BRASS, "H": BRASS_HI,
         "t": THREAD, "r": (150, 40, 60, 255), "W": GLINT}
    save(grid([
        ".....cC..Cc.....",
        "....c.d..d.c....",
        "...C..c..c..C...",
        "...c...dd...c...",
        "....d..cc..d....",
        ".....c.Cd.c.....",
        "......dccd......",
        "......kHBk......",
        ".....kHBBBk.....",
        "....kHBtBBbk....",
        "....kBBtrBbk....",
        "....kBBtBBbk....",
        "....kBBtBbbk....",
        ".....kBtBbk.....",
        "......kbbk......",
        "................",
    ], p), "item/umbilical_locket")


def temporal_field():
    # Temporal Field dome: deep teal, a spiral of lighter time winding in, amber sparks.
    img = new()
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            angle = math.atan2(dy, dx)
            radius = math.hypot(dx, dy)
            arm = math.sin(angle * 2 + radius * 0.9)
            img.putpixel((x, y), TEAL_HI if arm > 0.75 else TEAL if arm > -0.2 else TEAL_DK)
    for (x, y) in [(3, 4), (11, 3), (12, 11), (4, 12), (8, 8)]:
        img.putpixel((x, y), AMBER_HI)
    save(img, "block/temporal_field")


def faded_blueprint():
    # Yellowed paper, the drawing washed out to faint grey-blue lines and stains.
    r = rng(131)
    img = new()
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), shade((214, 200, 160, 255), r.randint(-10, 6)))
    for y in (4, 8, 11):
        for x in range(3, 13):
            if r.random() < 0.55:
                img.putpixel((x, y), (168, 172, 176, 255))
    for x in (5, 10):
        for y in range(3, 13):
            if r.random() < 0.5:
                img.putpixel((x, y), (168, 172, 176, 255))
    for (x, y) in [(12, 2), (13, 3), (2, 13), (3, 13)]:
        img.putpixel((x, y), (170, 140, 90, 255))  # water stains
    save(img, "block/faded_blueprint")


def intact_blueprint():
    # A rolled blueprint: deep blue paper, crisp white lines, tied with string.
    p = {"b": (40, 70, 140, 255), "B": (60, 96, 170, 255), "w": (230, 236, 250, 255), "s": (200, 180, 140, 255),
         "k": (24, 40, 90, 255)}
    save(grid([
        "................",
        "..........kk....",
        ".........kBBk...",
        "........kBwbBk..",
        ".......kBbwbBk..",
        "......kBwwbBk...",
        ".....kBbbwBk....",
        "....ksssssk.....",
        "...kBbwbBk......",
        "..kBwwbBk.......",
        ".kBbbwBk........",
        ".kBwbBk.........",
        "..kBBk..........",
        "...kk...........",
        "................",
        "................",
    ], p), "item/intact_blueprint")


def temporal_lens():
    # A brass spyglass with an amethyst lens at its far end and verdigris on the joints.
    p = {"k": BRASS_DDK, "b": BRASS_DK, "B": BRASS, "H": BRASS_HI, "a": AME_DK, "A": AME, "L": AME_HI, "v": VERD}
    save(grid([
        "................",
        "...........kkk..",
        "..........kALAk.",
        ".........kAaLAk.",
        "........kHaaAk..",
        ".......kHBkkk...",
        "......kHBbk.....",
        ".....kvBbk......",
        "....kHBbk.......",
        "...kHBbk........",
        "..kvBbk.........",
        ".kHBbk..........",
        ".kBbk...........",
        "..kk............",
        "................",
        "................",
    ], p), "item/temporal_lens")


def obelisk():
    # Obelisk stone: near-black and perfectly crisp, no weathering or moss, a faint violet sheen.
    r = rng(151)
    stone = new()
    for y in range(16):
        for x in range(16):
            v = 6 if (x + y) % 7 == 0 else 0
            stone.putpixel((x, y), shade((26, 22, 34, 255), v + r.randint(-3, 3)))
    for x in range(16):
        stone.putpixel((x, 0), (40, 34, 52, 255))
    save(stone, "block/obelisk_stone")
    # Inscription band: glyphs that glow a cold pale teal, in no alphabet anyone knows.
    glyphs = stone.copy()
    marks = [(2, 4), (2, 5), (2, 6), (3, 6), (4, 6), (6, 3), (7, 3), (7, 4), (7, 5), (8, 5), (6, 7), (7, 7),
             (11, 4), (11, 5), (12, 4), (13, 4), (13, 5), (13, 6), (12, 8), (11, 9), (12, 10), (13, 9),
             (3, 10), (4, 10), (4, 11), (4, 12), (5, 12), (8, 10), (8, 11), (9, 12), (8, 13)]
    for (x, y) in marks:
        glyphs.putpixel((x, y), (170, 255, 236, 255))
    for (x, y) in marks[::3]:
        glyphs.putpixel((x, min(15, y + 1)), (80, 160, 150, 255))
    save(glyphs, "block/obelisk_inscription")


def eternal_hourglass():
    # Creative-only hourglass: a brass frame, glass bulbs, teal time running upwards.
    p = {"k": BRASS_DDK, "b": BRASS_DK, "B": BRASS, "H": BRASS_HI, "g": GLASS_EDGE, "G": GLASS_EDGE_DK,
         "t": TEAL, "T": TEAL_HI, "d": TEAL_DK, "a": AMBER_HI}
    save(grid([
        "................",
        "...kHBBBBBBbk...",
        "...kbbbbbbbbk...",
        "....kg.TT.gk....",
        "....kgtTTtgk....",
        "....kgtttdgk....",
        ".....kgddgk.....",
        "......kgak......",
        "......kgTk......",
        ".....kg.a.gk....",
        "....kg..T..gk...",
        "....kg.....gk...",
        "....kg..a..gk...",
        "...kbbbbbbbbk...",
        "...kHBBBBBBbk...",
        "................",
    ], p), "item/eternal_hourglass")


# ---------------------------------------------------------------- the Chrono-Insulated Suit

COAT = (196, 204, 186, 255)
COAT_DK = (150, 158, 140, 255)
LEATHER = (96, 64, 44, 255)
LEATHER_DK = (64, 42, 30, 255)
RUBBER = (44, 44, 50, 255)
RUBBER_HI = (90, 92, 104, 255)
LENS = (60, 110, 90, 255)
LENS_HI = (150, 220, 180, 255)


def suit_icons():
    save(grid([
        "................",
        "................",
        "................",
        "................",
        "................",
        "..kBBk....kBBk..",
        ".kBLLBk..kBLLBk.",
        "aBLlLLBaaBLlLLBa",
        "aBLLLLBaaBLLLLBa",
        ".kBLLBk..kBLLBk.",
        "..kBBk....kBBk..",
        "................",
        "................",
        "................",
        "................",
        "................",
    ], {"k": BRASS_DDK, "B": BRASS, "L": LENS, "l": LENS_HI, "a": LEATHER}), "item/chrono_goggles")
    save(grid([
        "................",
        "....cc....cc....",
        "...cCCddddCCc...",
        "..cCCCdkkdCCCc..",
        ".cCCCCCdkCCCCCc.",
        ".cCCcCCCBCCcCCc.",
        ".cCCcCCCCCCcCCc.",
        ".cCCcCCCBCCcCCc.",
        ".cBBcCCCCCCcBBc.",
        "....cCCCBCCc....",
        "....cCCgCCCc....",
        "....cCCCBCCc....",
        "....cCCCCCCc....",
        "....cccccccc....",
        "................",
        "................",
    ], {"c": COAT_DK, "C": COAT, "d": (110, 116, 104, 255), "k": OUTLINE, "B": BRASS, "g": ELDRITCH_DK}),
        "item/chrono_lab_coat")
    save(grid([
        "................",
        "...kBBBHBBBBk...",
        "...aLLLLLLLLa...",
        "...aLLLLLLLLa...",
        "...aLLLaaLLLa...",
        "...aLLLa.aLLa...",
        "...aBBLa.aBBa...",
        "...aBBLa.aBBa...",
        "...aLLLa.aLLa...",
        "...aLLLa.aLLa...",
        "...aLLLa.aLLa...",
        "...aLLLa.aLLa...",
        "...aaaaa.aaaa...",
        "................",
        "................",
        "................",
    ], {"k": BRASS_DDK, "B": BRASS, "H": BRASS_HI, "a": LEATHER_DK, "L": LEATHER}), "item/chrono_trousers")
    save(grid([
        "................",
        "................",
        "................",
        "................",
        "...rrrr..rrrr...",
        "...rRrr..rRrr...",
        "...rRrr..rRrr...",
        "...BBBB..BBBB...",
        "...rRrr..rRrr...",
        "...rRrr..rRrr...",
        "..rRrrr.rRrrr...",
        ".rRrrrr.rRrrrr..",
        ".rrrrrr.rrrrrr..",
        ".kkkkkk.kkkkkk..",
        "................",
        "................",
    ], {"r": RUBBER, "R": RUBBER_HI, "B": BRASS, "k": (20, 20, 24, 255)}), "item/chrono_galoshes")


def fill_rect(img, x0, y0, x1, y1, color_fn):
    for y in range(y0, y1):
        for x in range(x0, x1):
            img.putpixel((x, y), color_fn(x, y))


def suit_armor_layers():
    """Worn textures (64x32, vanilla humanoid armour layout)."""
    r = rng(91)
    layer1 = Image.new("RGBA", (64, 32), CLEAR)

    # Goggles: a leather strap round the head at eye level, two brass-rimmed lenses on the front.
    fill_rect(layer1, 0, 11, 32, 13, lambda x, y: LEATHER if y == 11 else LEATHER_DK)
    for cx in (10, 13):
        for (dx, dy) in [(-1, 0), (0, 0), (-1, 1), (0, 1)]:
            layer1.putpixel((cx + dx, 11 + dy), LENS)
        layer1.putpixel((cx - 1, 11), LENS_HI)
        for (dx, dy) in [(-2, 0), (1, 0), (-2, 1), (1, 1), (-1, -1), (0, -1), (-1, 2), (0, 2)]:
            layer1.putpixel((cx + dx, 11 + dy), BRASS)

    # Lab coat: body and both arms, stained, brass buttons, darker collar and cuffs.
    coat = lambda x, y: shade(COAT, r.randint(-10, 8))
    fill_rect(layer1, 16, 16, 40, 32, coat)   # body (all faces)
    fill_rect(layer1, 40, 16, 56, 32, coat)   # arm (all faces)
    fill_rect(layer1, 20, 20, 28, 21, lambda x, y: COAT_DK)    # collar line
    for y in (22, 25, 28):
        layer1.putpixel((24, y), BRASS)
    for x in range(20, 28):
        layer1.putpixel((x, 31), COAT_DK)
    fill_rect(layer1, 40, 30, 56, 32, lambda x, y: BRASS_DK if y == 30 else LEATHER_DK)   # cuffs
    for (x, y) in [(22, 26), (26, 23), (33, 27), (45, 25)]:
        layer1.putpixel((x, y), ELDRITCH_DK)  # stains from a careless experiment

    # Galoshes: black rubber over the feet, a brass strap.
    fill_rect(layer1, 0, 25, 16, 32, lambda x, y: RUBBER_HI if (x % 4 == 1 and y < 30) else RUBBER)
    fill_rect(layer1, 0, 26, 16, 27, lambda x, y: BRASS)
    fill_rect(layer1, 4, 16, 12, 20, lambda x, y: CLEAR)

    layer2 = Image.new("RGBA", (64, 32), CLEAR)
    # Trousers: oiled leather, a brass-buckled belt, brass knee plates.
    leather = lambda x, y: shade(LEATHER, r.randint(-8, 8))
    fill_rect(layer2, 0, 16, 16, 32, leather)       # leg
    fill_rect(layer2, 16, 20, 40, 32, leather)      # body (waist part is what shows)
    fill_rect(layer2, 16, 20, 40, 22, lambda x, y: LEATHER_DK)
    layer2.putpixel((23, 20), BRASS)
    layer2.putpixel((24, 20), BRASS_HI)
    fill_rect(layer2, 4, 24, 8, 26, lambda x, y: BRASS if y == 24 else BRASS_DK)

    for img, name in [(layer1, "chrono_insulated_layer_1"), (layer2, "chrono_insulated_layer_2")]:
        full = os.path.join(OUT, "models/armor", name + ".png")
        os.makedirs(os.path.dirname(full), exist_ok=True)
        img.save(full)


# ---------------------------------------------------------------- organisms by essence (texture overhaul)
# Each essence's flesh has its own texture, not only its own tint: the flesh layer (tinted) carries the pattern,
# the overlay (untinted) the details. Cores have their own eye per essence, and a hungry version of it.

def tissue(seed, value):
    """A greyscale-ish flesh texture from a value function v(x, y) (tinted in game)."""
    r = rng(seed)
    img = new()
    for y in range(16):
        for x in range(16):
            v = value(x, y) + r.randint(-7, 7)
            img.putpixel((x, y), (int(max(0, min(255, v + 8))), int(max(0, min(255, v))), int(max(0, min(255, v - 4))), 255))
    return img


def density_tissue(seed):
    # Compressed strata, as if the flesh had settled under its own weight.
    return tissue(seed, lambda x, y: 186 + 16 * math.sin(y * 1.55 + math.sin(x * 0.4) * 0.6) - (6 if y % 4 == 0 else 0))


def speed_tissue(seed):
    # Striated muscle: fine diagonal fibres, a pale tendon every few.
    def v(x, y):
        d = (x + y) % 5
        return 214 if d == 0 else 196 + 10 * math.sin((x + y) * 1.25)
    return tissue(seed, v)


def size_tissue(seed):
    # Swollen cells: round bubbles with dark walls and a nucleus.
    r = rng(seed)
    cells = [(r.uniform(0, 16), r.uniform(0, 16), r.uniform(2.6, 4.2)) for _ in range(7)]

    def v(x, y):
        best = 99.0
        for cx, cy, rad in cells:
            for ox in (-16, 0, 16):
                for oy in (-16, 0, 16):
                    d = math.hypot(x + 0.5 - cx - ox, y + 0.5 - cy - oy) / rad
                    best = min(best, d)
        if best < 0.25:
            return 150  # nucleus
        if best < 0.85:
            return 222 - 14 * best
        return 160
    return tissue(seed, v)


def awareness_tissue(seed):
    # Brain folds: winding gyri, dark sulci between them.
    def v(x, y):
        f = math.sin(x * 1.2 + math.sin(y * 0.8) * 2.4) + math.sin(y * 1.05 + x * 0.35)
        return 224 if f > 0.55 else 196 if f > -0.55 else 156
    return tissue(seed, v)


def time_tissue(seed):
    # Growth rings, like a tree's: time written into the flesh, off-centre.
    return tissue(seed, lambda x, y: 196 + 18 * math.sin(math.hypot(x - 3.5, y - 12.5) * 1.6))


TISSUES = {"density": density_tissue, "speed": speed_tissue, "size": size_tissue,
           "awareness": awareness_tissue, "time": time_tissue}

IRON_HI = (200, 204, 212, 255)
IRON = (128, 132, 142, 255)
IRON_DK = (70, 72, 82, 255)


def flesh_overlays():
    """The untinted details on mature flesh, one per essence."""
    # Density: rows of iron rivets, a scar sewn with wire.
    img = new()
    for x in (2, 6, 10, 14):
        for y in (1, 14):
            img.putpixel((x, y), IRON_HI)
            img.putpixel((x, y + 1 if y == 1 else y - 1), IRON_DK)
    for i, x in enumerate(range(3, 13)):
        img.putpixel((x, 8), (96, 52, 60, 255))
        if i % 2:
            img.putpixel((x, 7), STEEL)
            img.putpixel((x, 9), STEEL_DK)
    save(img, "block/chronoflesh_density_overlay")

    # Speed: pale tendons glinting along the fibres, copper staples across them.
    img = new()
    for x, y in [(1, 14), (2, 13), (3, 12), (4, 11), (9, 6), (10, 5), (11, 4), (12, 3), (13, 2)]:
        img.putpixel((x, y), (246, 236, 220, 200))
    for (x, y) in [(6, 9), (7, 8)]:
        img.putpixel((x, y), COPPER_HI)
        img.putpixel((x - 1, y - 1), COPPER_DK)
        img.putpixel((x + 1, y + 1), COPPER)
    save(img, "block/chronoflesh_speed_overlay")

    # Size: wet highlights on the swollen cells.
    img = new()
    r = rng(71)
    for _ in range(7):
        x, y = r.randint(1, 13), r.randint(1, 13)
        img.putpixel((x, y), (255, 255, 255, 170))
        img.putpixel((x + 1, y), (255, 255, 255, 90))
    save(img, "block/chronoflesh_size_overlay")

    # Awareness: tiny eyes opening in the folds.
    img = new()
    for (x, y) in [(3, 4), (10, 2), (7, 10), (12, 12)]:
        img.putpixel((x - 1, y), (226, 214, 196, 255))
        img.putpixel((x, y), ELDRITCH_DK)
        img.putpixel((x + 1, y), (226, 214, 196, 255))
        img.putpixel((x, y - 1), (120, 70, 80, 255))
    save(img, "block/chronoflesh_awareness_overlay")

    # Time: amber sparks caught in the rings, teal crystal flecks.
    img = new()
    for (x, y) in [(5, 11), (9, 8), (12, 4), (2, 6)]:
        img.putpixel((x, y), AMBER_HI)
    for (x, y) in [(7, 13), (13, 9), (4, 2), (10, 14)]:
        img.putpixel((x, y), TEAL_HI)
        img.putpixel((x + 1 if x < 15 else x - 1, y), TEAL_DK)
    save(img, "block/chronoflesh_time_overlay")


def organism_textures():
    seeds = {"density": 31, "speed": 32, "size": 33, "awareness": 34, "time": 35}
    for name, make in TISSUES.items():
        save(make(seeds[name]), "block/chronoflesh_" + name)
        core = make(seeds[name] + 100)
        # Veins converging on the eye.
        for angle in [0.3, 1.4, 2.5, 3.7, 4.8, 5.8]:
            for dist in range(6, 9):
                x = int(round(7.5 + math.cos(angle) * dist))
                y = int(round(7.5 + math.sin(angle) * dist))
                if 0 <= x < 16 and 0 <= y < 16:
                    core.putpixel((x, y), shade(core.getpixel((x, y)), -55))
        save(core, "block/organism_core_" + name)
    flesh_overlays()

    W = (230, 220, 200, 255)
    WS = (190, 176, 156, 255)
    X = (14, 10, 14, 255)
    eyes = {
        # Density: a heavy-lidded grey eye behind a riveted iron porthole.
        "density": (grid([
            "................",
            "................",
            "..kkkkkkkkkkkk..",
            "..kIIrIIIIrIIk..",
            "..kIllllllllIk..",
            "..kIlLLLLLLlIk..",
            "..rIsWWWWWWsIr..",
            "..kIWWgGGgWWIk..",
            "..kIWWGxxGWWIk..",
            "..kIsWgGGgWsIk..",
            "..rIssWWWWssIr..",
            "..kIIsssssIIIk..",
            "..kIIrIIIIrIIk..",
            "..kkkkkkkkkkkk..",
            "................",
            "................",
        ], {"k": IRON_DK, "I": IRON, "r": IRON_HI, "l": (120, 66, 74, 255), "L": (156, 90, 98, 255),
            "s": WS, "W": W, "g": STEEL_DK, "G": STEEL, "x": X})),
        # Speed: a darting eye with a goat's slit, motion-streaked, in a copper dial.
        "speed": (grid([
            "................",
            "................",
            "......kccck.....",
            "....kcCHHCck....",
            "...kCsWWWWsCk...",
            "..kCsWWWWWWWsCk.",
            "..cWWyYYYYyWWc..",
            ".-cWyYxxxxxYyWc-",
            "..cWWyYYYYyWWc..",
            "..kCsWWWWWWWsCk.",
            "...kCsWWWWsCk...",
            "....kcCCCCck....",
            "......kcck......",
            "................",
            "................",
            "................",
        ], {"k": COPPER_DK, "c": COPPER, "C": COPPER, "H": COPPER_HI, "s": WS, "W": W,
            "y": (130, 150, 30, 255), "Y": (196, 214, 70, 255), "x": X, "-": (246, 236, 200, 160)})),
        # Size: one huge, dilated eye under a magnifying glass.
        "size": (grid([
            ".....eeeeee.....",
            "...eegggggee....",
            "..egWWWWWWWge...",
            ".egWWsoooosWge..",
            ".egWsoOOOOosWge.",
            "egWsoOxxxxOosWge",
            "egWoOxxxxxxOoWge",
            "egWoOxxxxxxOoWge",
            "egWoOxxxxxxOoWge",
            "egWsoOxxxxOosWge",
            ".egWsoOOOOosWge.",
            ".egWWsoooosWWge.",
            "..egWWWWWWWWge..",
            "...eegggggee....",
            ".....eeeeee.....",
            "................",
        ], {"e": BRASS_DK, "g": GLASS_EDGE, "W": W, "s": WS, "o": AMBER_DK, "O": AMBER, "x": X})),
        # Awareness: three eyes, the middle one ringed in brass.
        "awareness": (grid([
            "................",
            "..sWWs....sWWs..",
            ".sWiIWs..sWIiWs.",
            ".sWIxWs..sWxIWs.",
            "..sWWs....sWWs..",
            "......kbbk......",
            ".....kBsWBk.....",
            "....kBWWWWBk....",
            "....bWiIIiWb....",
            "....BWIxxIWB....",
            "....bWiIIiWb....",
            "....kBWWWWBk....",
            ".....kBssBk.....",
            "......kbbk......",
            "................",
            "................",
        ], {"k": BRASS_DDK, "b": BRASS_DK, "B": BRASS, "s": WS, "W": W, "i": ELDRITCH_DK, "I": ELDRITCH, "x": X})),
        # Time: an amber iris marked like a clock face, the pupil two hands, in a brass bezel.
        "time": (grid([
            "................",
            "................",
            "......kbbk......",
            "....kbBHBBbk....",
            "...kBsWWWWsBk...",
            "...bWaAtAAaWb...",
            "..kBaAAxAAAaBk..",
            "..bWtAAxAAAtWb..",
            "..bWaAAxxxAaWb..",
            "..kBaAAAAAAaBk..",
            "...bWaAtAAaWb...",
            "...kBsWWWWsBk...",
            "....kbBBBBbk....",
            "......kbbk......",
            "................",
            "................",
        ], {"k": BRASS_DDK, "b": BRASS_DK, "B": BRASS, "H": BRASS_HI, "s": WS, "W": W,
            "a": AMBER_DK, "A": AMBER, "t": TEAL_HI, "x": X})),
    }
    for name, img in eyes.items():
        save(img, "block/organism_core_" + name + "_overlay")
        save(hungry_eye(img), "block/organism_core_" + name + "_hungry_overlay")


def hungry_eye(awake):
    """The same eye, half shut under a heavy lid, bloodshot, its metal tarnished."""
    img = awake.copy()
    rows = [y for y in range(16) if any(img.getpixel((x, y))[3] > 0 for x in range(16))]
    top, bottom = min(rows), max(rows)
    lid_to = top + (bottom - top) * 5 // 9
    r = rng(17)
    for y in range(16):
        for x in range(16):
            c = img.getpixel((x, y))
            if c[3] == 0:
                continue
            metal = c[0] > 120 and c[0] > c[2] + 40 and c[1] > c[2]
            pale = c[0] > 170 and c[1] > 160 and c[2] > 140
            if metal:
                img.putpixel((x, y), shade(c, -50))
            elif y <= lid_to:
                img.putpixel((x, y), (132, 72, 82, 255) if y < lid_to else (86, 40, 50, 255))
            elif pale and r.random() < 0.25:
                img.putpixel((x, y), (200, 60, 60, 255))
    return img


# ---------------------------------------------------------------- the Chronocortex's lobes and Cerebrum

def lobe_texture(seed, deep):
    r = rng(seed)
    img = new()
    for y in range(16):
        for x in range(16):
            f = math.sin(x * (0.95 if deep else 1.25) + math.sin(y * 0.75) * 2.6) + math.sin(y * 1.0 + x * 0.4)
            if f > 0.5:
                c = (214, 168, 176, 255) if not deep else (198, 140, 156, 255)
            elif f > -0.5:
                c = (178, 128, 142, 255) if not deep else (156, 98, 120, 255)
            else:
                c = (118, 72, 92, 255) if not deep else (84, 44, 66, 255)
            img.putpixel((x, y), shade(c, r.randint(-6, 6)))
    return img


def cortex_lobes():
    save(lobe_texture(201, False), "block/cortex_lobe")
    cerebrum = lobe_texture(202, True)
    # Brass staples along a suture, and electrodes sparking in the folds.
    for i, x in enumerate(range(1, 15)):
        y = 7 + (1 if i % 4 < 2 else 0)
        cerebrum.putpixel((x, y), (70, 30, 46, 255))
        if i % 3 == 0:
            cerebrum.putpixel((x, y - 1), BRASS_HI)
            cerebrum.putpixel((x, y + 1), BRASS_DK)
    for (x, y) in [(3, 3), (12, 12), (11, 2)]:
        cerebrum.putpixel((x, y), STEEL_HI)
        cerebrum.putpixel((x + 1, y), TEAL_HI)
    save(cerebrum, "block/cerebrum")

    eye = lobe_texture(203, True)
    grid([
        "................",
        "................",
        "....kkkkkkkk....",
        "...kllllllllk...",
        "..klsWWWWWWslk..",
        ".klsWWiiiiWWslk.",
        ".ksWWiIIIIiWWsk.",
        ".kWWiIIxxIIiWWk.",
        ".kWWiIxxxxIiWWk.",
        ".ksWWiIIIIirWsk.",
        ".klsWWiiiiWWslk.",
        "..klsWWrWWWslk..",
        "...kllllllllk...",
        "....kkkkkkkk....",
        "................",
        "................",
    ], {"k": (60, 24, 40, 255), "l": (150, 84, 104, 255), "s": (200, 186, 170, 255), "W": (236, 228, 212, 255),
        "i": ELDRITCH_DK, "I": ELDRITCH, "x": (10, 8, 12, 255), "r": (200, 70, 70, 255)}, eye)
    save(eye, "block/cerebrum_eye")


# ---------------------------------------------------------------- the Essence Manipulator (animated)

GLOVE = [
    "................",
    "....kk.kk.kk....",
    "...kLLkLLkLLk...",
    "...kLLkLLkLLk...",
    "...kBBkBBkBBkk..",
    "...kLLkLLkLLkLk.",
    "..kkLLdLLdLLkLLk",
    "..kLLLLLLLLLLLk.",
    "..kLdLBBBBLLdk..",
    "..kLLBHBBBBLLk..",
    "..kLLLBBBBLLLk..",
    "...kLLLdLLLLk...",
    "...kCCCCCCCCk...",
    "...kCrCCCCrCk...",
    "...kCCCCCCCCk...",
    "...kkkkkkkkkk...",
]
LEATHER_PAL = {"k": (34, 22, 20, 255), "L": (104, 64, 44, 255), "B": BRASS, "H": BRASS_HI, "C": COPPER, "r": COPPER_HI,
               "d": (30, 12, 28, 255)}
# Tendrils curling out of the slits: (slit, path outward). Each frame shows a different length of each.
TENDRILS = [
    ((6, 6), [(6, 5), (5, 4)]),
    ((9, 6), [(9, 5), (10, 4)]),
    ((4, 8), [(3, 9), (2, 9), (1, 8)]),
    ((12, 8), [(13, 9), (14, 9), (15, 8)]),
    ((7, 11), [(7, 12)]),
]
TENDRIL = (138, 64, 128, 255)
TENDRIL_TIP = (176, 232, 128, 255)
FRAMES = 8


def essence_manipulator():
    strip = Image.new("RGBA", (16, 16 * FRAMES), CLEAR)
    for f in range(FRAMES):
        img = grid(GLOVE, LEATHER_PAL)
        # Light in the slits: something moving inside.
        for i, ((sx, sy), path) in enumerate(TENDRILS):
            glow = 0.5 + 0.5 * math.sin(2 * math.pi * f / FRAMES + i * 1.7)
            img.putpixel((sx, sy), (int(30 + 110 * glow), int(12 + 60 * glow), int(28 + 90 * glow), 255))
            length = int(round((len(path) + 0.4) * (0.5 + 0.5 * math.sin(2 * math.pi * f / FRAMES + i * 2.3))))
            for j, (x, y) in enumerate(path[:length]):
                tip = j == length - 1
                img.putpixel((x, y), TENDRIL_TIP if tip else TENDRIL)
        strip.paste(img, (0, 16 * f))
    full = os.path.join(OUT, "item/essence_manipulator.png")
    os.makedirs(os.path.dirname(full), exist_ok=True)
    strip.save(full)
    with open(full + ".mcmeta", "w") as meta:
        meta.write('{\n  "animation": {\n    "frametime": 3,\n    "interpolate": false\n  }\n}\n')
    WRITTEN.append(("item/essence_manipulator", strip.crop((0, 0, 16, 16))))


def confluence_needle():
    # Two hollow needles twisted into one: a brass Y-joint, two steel shafts splaying to two points.
    save(grid([
        "............s..s",
        "...........sS.sS",
        "..........sS.sS.",
        ".........sS.sS..",
        "........sS.sS...",
        ".......sSsSS....",
        "......sSSS......",
        ".....sSS........",
        "....kBBk........",
        "...kBHBk........",
        "..kBBBk.........",
        ".kBBBk..........",
        ".kGGk...........",
        "kGGk............",
        "kkk.............",
        "................",
    ], {"s": STEEL_HI, "S": STEEL, "k": BRASS_DDK, "B": BRASS, "H": BRASS_HI, "G": (150, 80, 110, 255)}),
        "item/confluence_needle")


# ---------------------------------------------------------------- power icons (wheel and HUD)

GLYPH = (244, 236, 214, 255)
GLYPH_EDGE = (24, 16, 18, 255)


def icon(name, draw_fn):
    from PIL import ImageDraw
    img = new()
    d = ImageDraw.Draw(img)
    draw_fn(d)
    # A dark outline around the glyph so it reads on any colour.
    out = img.copy()
    for y in range(16):
        for x in range(16):
            if img.getpixel((x, y))[3] == 0 and any(
                    0 <= x + dx < 16 and 0 <= y + dy < 16 and img.getpixel((x + dx, y + dy))[3] > 0
                    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                out.putpixel((x, y), GLYPH_EDGE)
    save(out, "gui/power/" + name)


def power_icons():
    G = GLYPH

    def fist(d):
        d.rectangle([4, 5, 11, 11], fill=G)
        for x in (5, 7, 9):
            d.point((x, 5), fill=(150, 140, 120, 255))
        d.rectangle([5, 12, 10, 13], fill=G)

    def eye(d, pupil=True):
        d.ellipse([2, 5, 13, 10], outline=G)
        if pupil:
            d.rectangle([7, 6, 8, 9], fill=G)

    def glowing(d):
        eye(d)
        for p in [(1, 3), (14, 3), (7, 2), (1, 12), (14, 12)]:
            d.point(p, fill=G)

    def quick(d):
        d.line([(3, 3), (7, 7), (3, 11)], fill=G)
        d.line([(8, 3), (12, 7), (8, 11)], fill=G)

    def shrink(d):
        d.rectangle([6, 6, 9, 9], outline=G)
        for a, b in [((1, 1), (4, 4)), ((14, 1), (11, 4)), ((1, 14), (4, 11)), ((14, 14), (11, 11))]:
            d.line([a, b], fill=G)

    def grow(d):
        d.rectangle([5, 5, 10, 10], outline=G)
        for a, b in [((4, 4), (1, 1)), ((11, 4), (14, 1)), ((4, 11), (1, 14)), ((11, 11), (14, 14))]:
            d.line([a, b], fill=G)
        for p in [(1, 2), (2, 1), (14, 2), (13, 1), (1, 13), (2, 14), (14, 13), (13, 14)]:
            d.point(p, fill=G)

    def rewind(d):
        d.polygon([(7, 3), (7, 11), (2, 7)], fill=G)
        d.polygon([(13, 3), (13, 11), (8, 7)], fill=G)

    def hourglass(d):
        d.line([(3, 2), (12, 2)], fill=G)
        d.line([(3, 13), (12, 13)], fill=G)
        d.polygon([(4, 3), (11, 3), (8, 7), (7, 7)], fill=G)
        d.polygon([(7, 8), (8, 8), (11, 12), (4, 12)], outline=G)

    def freeze(d):
        d.line([(7, 1), (7, 14)], fill=G)
        d.line([(1, 7), (14, 7)], fill=G)
        d.line([(3, 3), (12, 12)], fill=G)
        d.line([(12, 3), (3, 12)], fill=G)
        d.rectangle([6, 6, 8, 8], fill=(0, 0, 0, 0))
        d.point((7, 7), fill=G)

    def impetus(d):
        d.polygon([(9, 3), (14, 7), (9, 11)], fill=G)
        d.rectangle([5, 6, 9, 8], fill=G)
        d.line([(1, 4), (4, 4)], fill=G)
        d.line([(1, 10), (4, 10)], fill=G)

    def slam(d):
        d.rectangle([6, 1, 9, 6], fill=G)
        d.polygon([(3, 6), (12, 6), (7, 10)], fill=G)
        d.line([(1, 13), (14, 13)], fill=G)
        d.point((2, 11), fill=G)
        d.point((13, 11), fill=G)

    def dread(d):
        eye(d)
        d.rectangle([2, 4, 13, 6], fill=G)
        d.point((5, 12), fill=G)
        d.point((10, 13), fill=G)

    def dilation(d):
        d.point((7, 7), fill=G)
        d.arc([4, 4, 11, 11], 200, 340, fill=G)
        d.arc([1, 1, 14, 14], 200, 340, fill=G)
        d.arc([4, 4, 11, 11], 20, 160, fill=G)
        d.arc([1, 1, 14, 14], 20, 160, fill=G)

    def scurry(d):
        d.line([(1, 13), (5, 13), (5, 9), (9, 9), (9, 5), (13, 5), (13, 1)], fill=G)
        d.rectangle([2, 10, 3, 11], fill=G)
        d.rectangle([6, 6, 7, 7], fill=G)

    def reflexes(d):
        d.line([(1, 12), (7, 6)], fill=G)
        d.line([(7, 6), (13, 12)], fill=G)
        d.line([(11, 12), (13, 12), (13, 10)], fill=G)
        d.arc([4, 1, 11, 8], 180, 360, fill=G)

    def stutter(d):
        for i in range(12):
            a = 2 * math.pi * i / 12
            if i % 2 == 0:
                d.point((7.5 + 6 * math.cos(a), 7.5 + 6 * math.sin(a)), fill=G)
        d.ellipse([5, 5, 10, 10], outline=G)

    def overlook(d):
        eye(d)
        d.line([(2, 13), (13, 2)], fill=G)

    def regrowth(d):
        d.polygon([(7, 13), (2, 7), (2, 4), (4, 2), (7, 4), (10, 2), (12, 4), (12, 7)], fill=G)
        d.line([(7, 6), (7, 10)], fill=(0, 0, 0, 0))
        d.line([(5, 8), (9, 8)], fill=(0, 0, 0, 0))

    def premonition(d):
        eye(d, pupil=False)
        d.line([(7, 7), (7, 5)], fill=G)
        d.line([(7, 7), (9, 8)], fill=G)
        d.point((7, 1), fill=G)
        d.point((7, 14), fill=G)

    for name, fn in [("iron_fists", fist), ("glowing_sight", glowing), ("quickening", quick), ("shrink", shrink),
                     ("grow", grow), ("rewind", rewind), ("slow_time", hourglass), ("time_freeze", freeze),
                     ("impetus", impetus), ("ground_slam", slam), ("dread", dread), ("dilation", dilation),
                     ("scurry", scurry), ("reflexes", reflexes), ("stutter", stutter), ("overlook", overlook),
                     ("regrowth", regrowth), ("premonition", premonition)]:
        icon(name, fn)


# ---------------------------------------------------------------- run

syringe()
needles()
vial()
ampoule()
culture()
brain()
residue()
organism_core()
necrotic()
tap()
leech_tendril()
enhancing_gland()
essence_jug()
essence_jug_fill()
essence_jug_top()
time_tear()
brass_fitting()
steel()
brass_block()
brass_ingot()
brass_nugget()
brass_blend()
graft_flesh_texture()
gland_sac()
leech_skin()
flesh_pustule()

vein()
vein_clogged()
maw()
stasis_bladder()
gauges()
heart_specimen()
heart_muscle()
eye_graft()
clock_graft()
ganglion()
dissection_table()
brain_tissue()
cortex_jar()
essence_mote()
umbilical_locket()
temporal_field()
faded_blueprint()
intact_blueprint()
temporal_lens()
obelisk()
eternal_hourglass()
suit_icons()
suit_armor_layers()
organism_textures()
cortex_lobes()
essence_manipulator()
confluence_needle()
power_icons()

if PREVIEW:
    # Preview sheet: each texture at 4x, on a dark background, with tinted examples for tinted layers.
    cols = 8
    cell = 72
    rows = (len(WRITTEN) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * cell, rows * cell), (36, 34, 40, 255))
    for i, (name, img) in enumerate(WRITTEN):
        big = img.resize((64, 64), Image.NEAREST)
        sheet.paste(big, ((i % cols) * cell + 4, (i // cols) * cell + 4), big)
    sheet.save(PREVIEW)

print(f"wrote {len(WRITTEN)} textures")

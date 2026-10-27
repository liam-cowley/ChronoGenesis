"""
ChronoGenesis texture generator (16x16 pixel art).

Style: Frankenstein (stitches, staples, electrodes, jars), Call of Cthulhu (eyes, maws, tentacles,
eldritch glows) and steampunk (brass, copper, rivets, valves, gauges).

Usage:  python3 tools/textures.py src/main/resources/assets/chronogenesis/textures [preview.png]

Layer conventions (must match the models in datagen):
  * Containers (syringe, vial, ampoule): <name> = vessel, <name>_fill = liquid mask (tinted by aspect),
    <name>_front = untinted details drawn over the liquid.
  * Culture: culture = growth (tinted by aspect), culture_dish = untinted dish drawn on top.
  * Flesh and organism core: <name> = flesh (tinted by aspect), <name>_overlay = untinted details.
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


def chronoflesh():
    save(flesh_base(11), "block/chronoflesh")
    # A sutured scar (Frankenstein) and a pair of brass surgical staples.
    img = new()
    scar = [(12, 3), (11, 4), (10, 5), (9, 6), (8, 7), (7, 8), (6, 9), (5, 10), (4, 11), (3, 12)]
    for i, (x, y) in enumerate(scar):
        img.putpixel((x, y), (112, 46, 58, 255) if i % 2 else (140, 66, 76, 255))
        if i % 2 == 1:  # stitches cross the scar
            img.putpixel((x - 1, y - 1), THREAD)
            img.putpixel((x + 1, y + 1), THREAD_HI)
    grid([
        "................",
        "................",
        "..kBHk..........",
        "..b..b..........",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "..........b..b..",
        "..........kHBk..",
        "................",
    ], {"k": BRASS_DDK, "b": BRASS_DK, "B": BRASS, "H": BRASS_HI}, img)
    save(img, "block/chronoflesh_overlay")


def organism_core():
    # Flesh with veins converging on the centre.
    img = flesh_base(23, base=200)
    r = rng(5)
    for angle in [0.3, 1.4, 2.5, 3.7, 4.8, 5.8]:
        for dist in range(6, 9):
            x = int(round(7.5 + math.cos(angle) * dist))
            y = int(round(7.5 + math.sin(angle) * dist))
            if 0 <= x < 16 and 0 <= y < 16:
                img.putpixel((x, y), shade(img.getpixel((x, y)), -55))
    save(img, "block/organism_core")

    # The eye: sickly sclera, a slit pupil, held in a riveted brass monocle.
    p = {"k": BRASS_DDK, "B": BRASS, "H": BRASS_HI, "b": BRASS_DK, "r": (200, 70, 60, 255),
         "S": (220, 208, 130, 255), "s": (176, 160, 90, 255), "i": (160, 200, 60, 255), "I": (110, 150, 40, 255),
         "x": (16, 10, 14, 255), "W": GLINT, "R": BRASS_HI}
    save(grid([
        "................",
        "................",
        "......kbbk......",
        "....kbBHBBbk....",
        "...kBsSSSSsBk...",
        "...bSrSiiSSSb...",
        "..kHSiIxxIiSBk..",
        "..bBSiWxxIiSBb..",
        "..bBSiIxxIirBb..",
        "..kBSSiIIiSSBk..",
        "...bsSSrSSSsb...",
        "...kBsSSSSsBk...",
        "....kbBBBBbk....",
        "......kbbk......",
        "................",
        "................",
    ], p), "block/organism_core_overlay")


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


def organism_core_hungry_overlay():
    # The eye when starving: lid half shut, bloodshot, the brass monocle tarnished.
    p = {"k": BRASS_DDK, "B": BRASS_DK, "b": (110, 80, 40, 255), "r": (210, 50, 50, 255),
         "S": (196, 180, 110, 255), "s": (150, 130, 80, 255), "i": (140, 160, 60, 255), "x": (16, 10, 14, 255),
         "l": (120, 60, 70, 255), "L": (150, 80, 90, 255), "t": THREAD}
    save(grid([
        "................",
        "................",
        "......kbbk......",
        "....kbBBBBbk....",
        "...kBLLLLLLBk...",
        "...bLLLLLLLLb...",
        "..kBlllllllLBk..",
        "..bBSrSixxiSBb..",
        "..bBSiixxirSBb..",
        "..kBsSrSiSSsBk..",
        "...bsSSrSSSsb...",
        "...kBsSSSSsBk...",
        "....kbBBBBbk....",
        "......kbbk......",
        "................",
        "................",
    ], p), "block/organism_core_hungry_overlay")


# ---------------------------------------------------------------- run

syringe()
needles()
vial()
ampoule()
culture()
brain()
residue()
chronoflesh()
organism_core()
necrotic()
tap()
leech_tendril()
enhancing_gland()
essence_jug()
essence_jug_top()
time_tear()
brass_fitting()
steel()
graft_flesh_texture()
gland_sac()
leech_skin()
flesh_pustule()
organism_core_hungry_overlay()

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

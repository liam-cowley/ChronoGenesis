"""
Generates the ChronoGenesis placeholder textures (16x16 pixel art).

Colours are sampled from the vanilla textures (copper, glass, rotten flesh, nuggets...) inside the
Minecraft resources jar that ModDevGradle extracts, so the mod sits next to vanilla without clashing.
Everything else is drawn procedurally with a fixed seed, so re-running gives identical output.

Usage (from the repo root, after any gradle build so the jar exists):
    python tools/textures/generate_textures.py            # write textures that don't exist yet
    python tools/textures/generate_textures.py --force    # overwrite all generated textures

Requires Pillow (pip install pillow). Hand-made textures are never overwritten without --force.
"""
import argparse
import glob
import io
import math
import os
import random
import zipfile

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "chronogenesis", "textures")

# Essence colours, kept in sync with com.chronogenesis.essence.Aspect.
ASPECTS = {
    "density": (0x6E, 0x7F, 0x94),
    "speed": (0xB5, 0xC8, 0x3A),
    "size": (0xB7, 0x74, 0x2E),
    "awareness": (0xB9, 0xA3, 0xE3),
    "time": (0x2F, 0xA5, 0x9A),
}

CLEAR = (0, 0, 0, 0)


# ---------------------------------------------------------------- vanilla access

class Vanilla:
    def __init__(self):
        candidates = glob.glob(os.path.join(ROOT, "build", "moddev", "artifacts", "*client-extra*.jar"))
        if not candidates:
            raise SystemExit("Vanilla resources jar not found; run any gradle task (e.g. gradlew compileJava) first.")
        self.jar = zipfile.ZipFile(candidates[0])

    def image(self, path):
        data = self.jar.read("assets/minecraft/textures/" + path + ".png")
        return Image.open(io.BytesIO(data)).convert("RGBA")

    def palette(self, path):
        """Opaque colours of a vanilla texture, darkest first."""
        image = self.image(path)
        colours = {p[:3] for p in image.get_flattened_data() if p[3] > 200}
        return sorted(colours, key=luminance)


def luminance(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def pick(palette, t):
    """Colour at fraction t (0 = darkest, 1 = lightest) of a palette."""
    t = min(max(t, 0.0), 1.0)
    return palette[min(len(palette) - 1, int(t * len(palette)))]


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def shade(c, f):
    return tuple(max(0, min(255, int(round(v * f)))) for v in c[:3])


def rgba(c, a=255):
    return (c[0], c[1], c[2], a)


def new():
    return Image.new("RGBA", (16, 16), CLEAR)


# ---------------------------------------------------------------- noise

def value_noise(seed, scale=4.0):
    """Smooth tileable-ish value noise on a 16x16 grid, returns dict (x, y) -> 0..1."""
    rng = random.Random(seed)
    cells = int(16 / scale) + 2
    grid = [[rng.random() for _ in range(cells)] for _ in range(cells)]

    def smooth(t):
        return t * t * (3 - 2 * t)

    out = {}
    for y in range(16):
        for x in range(16):
            gx, gy = x / scale, y / scale
            x0, y0 = int(gx), int(gy)
            fx, fy = smooth(gx - x0), smooth(gy - y0)
            a = grid[y0][x0] * (1 - fx) + grid[y0][x0 + 1] * fx
            b = grid[y0 + 1][x0] * (1 - fx) + grid[y0 + 1][x0 + 1] * fx
            out[(x, y)] = a * (1 - fy) + b * fy
    return out


def random_walk(rng, length, start=None):
    x, y = start or (rng.randrange(16), rng.randrange(16))
    dx, dy = rng.choice([(1, 0), (0, 1), (-1, 0), (0, -1)])
    points = []
    for _ in range(length):
        points.append((x % 16, y % 16))
        if rng.random() < 0.35:
            dx, dy = rng.choice([(1, 0), (0, 1), (-1, 0), (0, -1), (dx, dy)])
        x, y = x + dx, y + dy
    return points


# ---------------------------------------------------------------- flesh

FLESH_LIGHT = (0xC9, 0x7F, 0x86)
FLESH_DARK = (0x6A, 0x2E, 0x3E)


def flesh(seed, tint=None, tint_amount=0.18, light=FLESH_LIGHT, dark=FLESH_DARK):
    """Lumpy living tissue: two octaves of noise between a dark fold and a light bulge colour."""
    image = new()
    big = value_noise(seed, 5.0)
    small = value_noise(seed + 1, 2.0)
    rng = random.Random(seed + 2)
    for y in range(16):
        for x in range(16):
            v = 0.65 * big[(x, y)] + 0.35 * small[(x, y)]
            v = int(v * 5) / 4.0  # posterise to a pixel-art ramp
            c = mix(dark, light, v)
            if tint:
                c = mix(c, tint, tint_amount)
            if rng.random() < 0.06:
                c = shade(c, 1.12)  # glistening specks
            image.putpixel((x, y), rgba(c))
    return image


def add_veins(image, seed, colour, count=3, length=14):
    rng = random.Random(seed)
    for _ in range(count):
        for i, (x, y) in enumerate(random_walk(rng, length)):
            c = colour if i % 5 else shade(colour, 1.25)
            image.putpixel((x, y), rgba(c))
    return image


def organism_flesh(aspect, colour):
    seed = 100 + list(ASPECTS).index(aspect) * 17
    image = flesh(seed, colour)
    return add_veins(image, seed + 5, shade(colour, 0.85))


def organism_core(aspect, colour):
    seed = 300 + list(ASPECTS).index(aspect) * 17
    image = flesh(seed, colour, 0.25)
    add_veins(image, seed + 5, shade(colour, 0.85), count=4, length=10)
    # A swollen nucleus that glows with its aspect, ringed by darker tissue.
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 2.2:
                image.putpixel((x, y), rgba(mix(colour, (255, 255, 255), 0.45)))
            elif d < 3.6:
                image.putpixel((x, y), rgba(colour))
            elif d < 4.6:
                image.putpixel((x, y), rgba(shade(mix(FLESH_DARK, colour, 0.3), 0.8)))
    image.putpixel((6, 6), rgba((255, 255, 255)))
    return image


def necrotic():
    image = flesh(777, None, light=(0x8A, 0x86, 0x6A), dark=(0x3A, 0x36, 0x2C))
    rng = random.Random(778)
    for _ in range(9):
        x, y = rng.randrange(16), rng.randrange(16)
        image.putpixel((x, y), rgba((0x2A, 0x24, 0x1E)))
    return add_veins(image, 779, (0x55, 0x5A, 0x3A), count=2, length=10)


# ---------------------------------------------------------------- blocks

def copper_armature(v):
    copper = v.palette("block/cut_copper")
    image = new()
    for y in range(16):
        for x in range(16):
            edge = x in (0, 15) or y in (0, 15)
            inner_edge = x in (1, 14) or y in (1, 14)
            brace = abs(x - y) == 0 or abs(x + y - 15) == 0
            if edge:
                # light from the top-left like vanilla
                t = 0.85 if (x == 0 or y == 0) else 0.3
                image.putpixel((x, y), rgba(pick(copper, t)))
            elif inner_edge and not (1 < x < 14 and 1 < y < 14):
                image.putpixel((x, y), rgba(pick(copper, 0.5)))
            elif brace and 1 < x < 14:
                image.putpixel((x, y), rgba(pick(copper, 0.65 if x < 8 else 0.4)))
    for (x, y) in [(1, 1), (14, 1), (1, 14), (14, 14), (7, 7), (8, 8), (7, 8), (8, 7)]:
        image.putpixel((x, y), rgba(pick(copper, 0.1)))  # rivets
    return image


def copper_plain(v, seed):
    copper = v.palette("block/cut_copper")
    noise = value_noise(seed, 3.0)
    image = new()
    for y in range(16):
        for x in range(16):
            t = 0.3 + 0.55 * noise[(x, y)]
            if y % 5 == 0:
                t -= 0.25  # banding like a riveted pipe
            image.putpixel((x, y), rgba(pick(copper, t)))
    return image


def maw_top():
    image = flesh(510, None, light=(0xB8, 0x6A, 0x72))
    teeth = (0xE6, 0xDD, 0xC2)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 2.6:
                image.putpixel((x, y), rgba((0x1E, 0x08, 0x10)))
            elif d < 3.6:
                angle = math.atan2(y - 7.5, x - 7.5)
                is_tooth = int((angle + math.pi) / (2 * math.pi) * 10) % 2 == 0
                image.putpixel((x, y), rgba(teeth if is_tooth else (0x4A, 0x14, 0x22)))
            elif d < 4.6:
                image.putpixel((x, y), rgba((0x8E, 0x3C, 0x4A)))
    return image


def maw_side():
    image = flesh(520, None, light=(0xB8, 0x6A, 0x72))
    for x in range(16):
        if x % 3 == 0:
            for y in range(16):
                px = image.getpixel((x, y))
                image.putpixel((x, y), rgba(shade(px, 0.82)))
    return image


def leech_tendril():
    image = flesh(530, (0x7A, 0x4E, 0x8A), 0.35)
    for y in range(0, 16, 3):
        for x in range(16):
            px = image.getpixel((x, y))
            image.putpixel((x, y), rgba(shade(px, 0.7)))
    return image


def leech_tendril_tip():
    image = flesh(540, (0x5A, 0x2A, 0x6A), 0.45)
    for y in range(16):
        for x in range(16):
            if math.hypot(x - 7.5, y - 7.5) < 2.0:
                image.putpixel((x, y), rgba((0x2A, 0x0C, 0x24)))
    return image


def jug_glass(v):
    glass = v.palette("block/glass")
    image = new()
    rng = random.Random(600)
    for y in range(16):
        for x in range(16):
            c = pick(glass, 0.8)
            a = 70
            if x == y + 2 or x == y + 3:  # highlight streak
                c, a = (255, 255, 255), 150
            elif rng.random() < 0.08:
                a = 110  # cloudy flecks
            image.putpixel((x, y), rgba(c, a))
    return image


def cork(v):
    wood = v.palette("block/stripped_spruce_log")
    noise = value_noise(610, 2.0)
    image = new()
    for y in range(16):
        for x in range(16):
            image.putpixel((x, y), rgba(pick(wood, 0.25 + 0.6 * noise[(x, y)])))
    return image


def jug_liquid():
    image = new()
    noise = value_noise(620, 4.0)
    for y in range(16):
        for x in range(16):
            g = int(215 + 40 * noise[(x, y)])
            image.putpixel((x, y), (g, g, g, 215))
    return image


# ---------------------------------------------------------------- items

def stroke(image, points, colour):
    for x, y in points:
        if 0 <= x < 16 and 0 <= y < 16:
            image.putpixel((x, y), rgba(colour) if len(colour) == 3 else colour)


def diagonal(t0, t1):
    """Pixels on the bottom-left to top-right diagonal, from step t0 to t1 (x = t, y = 15 - t)."""
    return [(t, 15 - t) for t in range(t0, t1 + 1)]


def syringe_parts():
    """
    Classifies every pixel of the syringe, laid along the bottom-left to top-right diagonal.
    t runs along the syringe (0 at the thumb rest), s across it, both in pixels.
    """
    parts = {}
    for y in range(16):
        for x in range(16):
            dx, dy = x - 1, 14 - y
            t = (dx + dy) / math.sqrt(2)
            s = (dx - dy) / math.sqrt(2)
            a = abs(s)
            part = None
            if t < -0.1 or t > 18.6:
                part = None
            elif t <= 0.8 and a <= 1.5:
                part = "thumb"
            elif t <= 3.6 and a <= 0.4:
                part = "rod"
            elif t <= 4.6 and a <= 2.2:
                part = "flange"
            elif t <= 12.8 and a <= 1.5:
                if a > 0.8 or t <= 5.4 or t > 12.1:
                    part = "wall_light" if s < 0 else "wall"
                elif t <= 6.2:
                    part = "stopper"
                else:
                    part = "liquid"
            elif t <= 14.2 and a <= 0.8:
                part = "hub"
            elif t <= 18.6 and a <= 0.4:
                part = "needle_base" if t <= 15.6 else "needle_tip"
            if part:
                parts[(x, y)] = part
    return parts


def syringe_body(v):
    copper = v.palette("block/cut_copper")
    glass = v.palette("block/glass")
    colours = {
        "thumb": rgba(pick(copper, 0.45)),
        "rod": rgba(pick(copper, 0.8)),
        "flange": rgba(pick(copper, 0.55)),
        "wall": rgba(pick(glass, 0.7), 235),
        "wall_light": rgba((255, 255, 255), 225),
        "stopper": rgba((0x3A, 0x32, 0x36)),
        "hub": rgba(pick(copper, 0.65)),
    }
    image = new()
    for pos, part in syringe_parts().items():
        if part in colours:
            image.putpixel(pos, colours[part])
    return image


def syringe_liquid():
    image = new()
    for pos, part in syringe_parts().items():
        if part == "liquid":
            image.putpixel(pos, (235, 235, 235, 210))
    return image


def syringe_needle(base, tip):
    image = new()
    for pos, part in syringe_parts().items():
        if part == "needle_base":
            image.putpixel(pos, rgba(base))
        elif part == "needle_tip":
            image.putpixel(pos, rgba(tip))
    return image


def needle_item(shaft, tip=None, eye=(0x30, 0x30, 0x30)):
    """A loose needle lying on the diagonal with an eye at the bottom-left."""
    image = new()
    light, dark = shaft
    for t in range(3, 14):
        image.putpixel((t, 15 - t), rgba(light))
        image.putpixel((t + 1, 15 - t), rgba(dark))
    image.putpixel((4, 11), rgba(eye))
    if tip:
        for p in [(12, 3), (13, 2), (13, 3), (14, 1), (12, 2)]:
            image.putpixel(p, rgba(tip if p != (14, 1) else shade(tip, 1.2)))
    return image


def bottle_shape(rows):
    """rows: list of (y, x_start, x_end) giving the outline of a container, interior is the inside."""
    return {y: (x0, x1) for y, x0, x1 in rows}


def container(shape, liquid_from, glass_edge, highlight=True, extra=None):
    """Draws the glass outline of a container; returns (glass, liquid) images."""
    glass = new()
    liquid = new()
    ys = sorted(shape)
    for y in ys:
        x0, x1 = shape[y]
        for x in range(x0, x1 + 1):
            is_edge = x in (x0, x1) or y in (ys[0], ys[-1]) \
                or (y - 1 in shape and not (shape[y - 1][0] <= x <= shape[y - 1][1])) \
                or (y + 1 in shape and not (shape[y + 1][0] <= x <= shape[y + 1][1]))
            if is_edge:
                glass.putpixel((x, y), glass_edge)
            elif y >= liquid_from:
                liquid.putpixel((x, y), (235, 235, 235, 205))
    if highlight:
        x0, _ = shape[ys[len(ys) // 2]]
        glass.putpixel((x0 + 1, ys[len(ys) // 2] - 1), (255, 255, 255, 230))
        glass.putpixel((x0 + 1, ys[len(ys) // 2]), (255, 255, 255, 200))
    if extra:
        extra(glass)
    return glass, liquid


def vial(v):
    glass = v.palette("block/glass")
    edge = rgba(pick(glass, 0.85), 235)
    shape = bottle_shape([(3, 6, 9), (4, 6, 9), (5, 5, 10)] + [(y, 4, 11) for y in range(6, 14)] + [(14, 5, 10)])
    cork_colours = v.palette("block/stripped_spruce_log")

    def add_cork(image):
        for x in range(6, 10):
            image.putpixel((x, 1), rgba(pick(cork_colours, 0.7)))
            image.putpixel((x, 2), rgba(pick(cork_colours, 0.4)))

    return container(shape, 8, edge, extra=add_cork)


def ampoule(v):
    membrane = v.palette("item/phantom_membrane")
    glass = v.palette("block/glass")
    edge = rgba(mix(pick(glass, 0.8), pick(membrane, 0.4), 0.5), 240)
    shape = bottle_shape([(1, 7, 8), (2, 7, 8), (3, 6, 9), (4, 6, 9)] + [(y, 5, 10) for y in range(5, 14)] + [(14, 6, 9)])

    def lining(image):
        # Membrane veins along the walls.
        for y in (6, 9, 12):
            image.putpixel((5, y), rgba(pick(membrane, 0.2)))
            image.putpixel((10, y + 1), rgba(pick(membrane, 0.2)))
        image.putpixel((7, 1), rgba(pick(membrane, 0.6)))
        image.putpixel((8, 1), rgba(pick(membrane, 0.6)))

    return container(shape, 6, edge, extra=lining)


def culture_jar(v):
    glass = v.palette("block/glass")
    edge = rgba(pick(glass, 0.85), 235)
    rotten = v.palette("item/rotten_flesh")
    cork_colours = v.palette("block/stripped_spruce_log")
    shape = bottle_shape([(4, 5, 10)] + [(y, 3, 12) for y in range(5, 15)])

    def contents(image):
        for x in range(5, 11):
            image.putpixel((x, 2), rgba(pick(cork_colours, 0.7)))
            image.putpixel((x, 3), rgba(pick(cork_colours, 0.35)))
        # A lump of rotten flesh and bone meal sitting at the bottom.
        rng = random.Random(700)
        for x in range(4, 12):
            for y in range(12, 14):
                if y == 12 and x in (4, 11):
                    continue
                image.putpixel((x, y), rgba(pick(rotten, rng.random() * 0.8)))
        image.putpixel((6, 12), rgba((0xEE, 0xEC, 0xE4)))
        image.putpixel((9, 13), rgba((0xEE, 0xEC, 0xE4)))

    return container(shape, 7, edge, extra=contents)


def living_flesh(v):
    """Vanilla rotten flesh's silhouette, recoloured to something that is very much alive."""
    rotten = v.image("item/rotten_flesh")
    image = new()
    for y in range(16):
        for x in range(16):
            r, g, b, a = rotten.getpixel((x, y))
            if a == 0:
                continue
            l = luminance((r, g, b)) / 255.0
            c = mix(FLESH_DARK, (0xE0, 0x8C, 0x94), min(1.0, l * 1.5))
            image.putpixel((x, y), rgba(c, a))
    # A copper staple holding it together.
    copper = v.palette("block/cut_copper")
    for p in [(6, 7), (7, 7), (8, 7)]:
        image.putpixel(p, rgba(pick(copper, 0.7)))
    return image


# ---------------------------------------------------------------- main

def build(v):
    textures = {}
    iron = v.palette("item/iron_nugget")
    gold = v.palette("item/gold_nugget")
    bone = v.palette("item/bone")
    amethyst = v.palette("item/amethyst_shard")

    # Blocks
    textures["block/copper_armature"] = copper_armature(v)
    for aspect, colour in ASPECTS.items():
        textures["block/organism_flesh_" + aspect] = organism_flesh(aspect, colour)
        textures["block/organism_core_" + aspect] = organism_core(aspect, colour)
    textures["block/organism_core_necrotic"] = necrotic()
    textures["block/graft_base"] = flesh(500)
    textures["block/tap"] = copper_plain(v, 505)
    textures["block/maw_top"] = maw_top()
    textures["block/maw_side"] = maw_side()
    textures["block/leech_tendril"] = leech_tendril()
    textures["block/leech_tendril_tip"] = leech_tendril_tip()
    textures["block/essence_jug"] = jug_glass(v)
    textures["block/essence_jug_cork"] = cork(v)
    textures["block/essence_jug_liquid"] = jug_liquid()

    # Items
    textures["item/chrono_syringe"] = syringe_body(v)
    textures["item/chrono_syringe_liquid"] = syringe_liquid()
    textures["item/chrono_syringe_needle_iron"] = syringe_needle(pick(iron, 0.55), pick(iron, 1.0))
    textures["item/chrono_syringe_needle_gold"] = syringe_needle(pick(gold, 0.45), pick(gold, 1.0))
    textures["item/chrono_syringe_needle_bone"] = syringe_needle(pick(bone, 0.45), pick(bone, 1.0))
    textures["item/chrono_syringe_needle_amethyst"] = syringe_needle(pick(iron, 0.6), pick(amethyst, 0.8))
    textures["item/iron_needle"] = needle_item((pick(iron, 0.9), pick(iron, 0.4)))
    textures["item/gold_needle"] = needle_item((pick(gold, 0.85), pick(gold, 0.35)))
    textures["item/bone_needle"] = needle_item((pick(bone, 0.9), pick(bone, 0.4)), eye=pick(bone, 0.1))
    textures["item/amethyst_needle"] = needle_item((pick(iron, 0.9), pick(iron, 0.4)), tip=pick(amethyst, 0.6))
    textures["item/essence_vial"], textures["item/essence_vial_liquid"] = vial(v)
    textures["item/stasis_ampoule"], textures["item/stasis_ampoule_liquid"] = ampoule(v)
    textures["item/culture_jar"], textures["item/culture_jar_liquid"] = culture_jar(v)
    textures["item/living_flesh"] = living_flesh(v)
    return textures


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--force", action="store_true", help="overwrite existing textures")
    parser.add_argument("--preview", help="also write an 8x upscaled contact sheet to this path")
    args = parser.parse_args()

    textures = build(Vanilla())
    written = 0
    for name, image in textures.items():
        path = os.path.join(OUT, name + ".png")
        if os.path.exists(path) and not args.force:
            continue
        os.makedirs(os.path.dirname(path), exist_ok=True)
        image.save(path)
        written += 1
    print(f"{written} textures written, {len(textures) - written} kept")

    if args.preview:
        names = sorted(textures)
        columns = 8
        scale = 8
        sheet = Image.new("RGBA", (columns * 17 * scale, ((len(names) + columns - 1) // columns) * 17 * scale), (60, 60, 70, 255))
        for i, name in enumerate(names):
            tile = textures[name].resize((16 * scale, 16 * scale), Image.NEAREST)
            sheet.alpha_composite(tile, ((i % columns) * 17 * scale, (i // columns) * 17 * scale))
        sheet.save(args.preview)
        print("preview:", args.preview, "order:", ", ".join(names))


if __name__ == "__main__":
    main()

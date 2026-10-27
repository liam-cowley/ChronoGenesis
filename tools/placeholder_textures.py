"""Generates ChronoGenesis placeholder pixel art (16x16). Usage: python3 tools/placeholder_textures.py src/main/resources/assets/chronogenesis/textures"""
import os
import random
import sys
from PIL import Image

OUT = sys.argv[1]
random.seed(1337)
T = (0, 0, 0, 0)


def new():
    return Image.new("RGBA", (16, 16), T)


def save(img, path):
    full = os.path.join(OUT, path + ".png")
    os.makedirs(os.path.dirname(full), exist_ok=True)
    img.save(full)


def jitter(c, n=12):
    r = random.randint(-n, n)
    return tuple(max(0, min(255, v + r)) for v in c[:3]) + (c[3] if len(c) > 3 else 255,)


def line(img, pts, c):
    for p in pts:
        img.putpixel(p, c)


# ---------------------------------------------------------------- items

COPPER = (190, 110, 70, 255)
COPPER_D = (130, 70, 45, 255)
VERDI = (90, 160, 140, 255)
GLASS = (210, 230, 235, 255)
GLASS_D = (150, 175, 185, 255)
GLASS_IN = (220, 240, 250, 70)


def syringe():
    img, fill = new(), new()
    # diagonal barrel from (3,12) to (11,4)
    for i in range(7):
        x, y = 4 + i, 11 - i
        img.putpixel((x, y), GLASS_D)
        img.putpixel((x + 1, y + 1), GLASS_D)
        img.putpixel((x + 1, y), GLASS_IN)
        fill.putpixel((x + 1, y), (255, 255, 255, 255))
    # copper bands and plunger
    for p in [(3, 12), (4, 12), (3, 11), (11, 4), (12, 4), (12, 3)]:
        img.putpixel(p, COPPER)
    line(img, [(2, 13), (1, 14), (2, 14), (1, 13)], COPPER_D)
    # needle
    line(img, [(13, 2), (14, 1)], (200, 200, 210, 255))
    save(img, "item/chrono_syringe")
    save(fill, "item/chrono_syringe_fill")


def needle(name, color, dark):
    img = new()
    for i in range(10):
        x, y = 3 + i, 12 - i
        img.putpixel((x, y), color)
        if i < 3:
            img.putpixel((x + 1, y), dark)
    img.putpixel((13, 2), (230, 230, 235, 255))
    img.putpixel((14, 1), (240, 240, 245, 255))
    save(img, "item/" + name)


def vial(name, cork, body_w, lining=None):
    img, fill = new(), new()
    line(img, [(7, 1), (8, 1), (7, 2), (8, 2)], cork)
    line(img, [(6, 3), (9, 3), (6, 4), (9, 4)], GLASS)
    for y in range(5, 15):
        w = body_w if 6 <= y <= 13 else body_w - 1
        for x in range(8 - w, 8 + w):
            edge = x in (8 - w, 8 + w - 1) or y in (5, 14)
            if edge:
                img.putpixel((x, y), lining if lining and y % 3 == 0 else GLASS)
            else:
                img.putpixel((x, y), GLASS_IN)
                if y >= 8:
                    fill.putpixel((x, y), (255, 255, 255, 255))
    img.putpixel((8 - body_w + 1, 6), (255, 255, 255, 255))
    save(img, "item/" + name)
    save(fill, "item/" + name + "_fill")


def culture():
    # Petri dish with a pale blob; greyscale so the aspect tint colours it.
    img = new()
    for y in range(4, 13):
        for x in range(2, 14):
            dx, dy = (x - 7.5) / 6, (y - 8) / 4.5
            d = dx * dx + dy * dy
            if d <= 1.0:
                img.putpixel((x, y), (235, 235, 235, 255) if d > 0.7 else jitter((205, 205, 205, 255), 25))
    for x in range(3, 13):
        img.putpixel((x, 12), (170, 170, 170, 255))
    save(img, "item/culture")


def brain():
    img = new()
    base = (205, 140, 150, 255)
    for y in range(4, 13):
        for x in range(2, 14):
            dx, dy = (x - 7.5) / 6, (y - 8.5) / 4.5
            if dx * dx + dy * dy <= 1:
                img.putpixel((x, y), jitter(base, 15))
    for x, y in [(4, 6), (5, 7), (6, 6), (8, 7), (9, 6), (10, 8), (5, 10), (7, 9), (9, 10), (11, 10), (7.5, 5)]:
        img.putpixel((int(x), int(y)), (150, 85, 100, 255))
    for y in range(5, 12):
        img.putpixel((8, y), (140, 80, 95, 255))
    save(img, "item/preserved_brain")


def residue():
    img = new()
    pts = {}
    for y in range(2, 14):
        half = max(1, (y - 1) // 3) if y < 8 else max(1, (14 - y) // 2)
        for x in range(8 - half, 8 + half + 1):
            pts[(x, y)] = 1
    for (x, y) in pts:
        edge = (x - 1, y) not in pts or (x + 1, y) not in pts or (x, y - 1) not in pts or (x, y + 1) not in pts
        img.putpixel((x, y), (30, 110, 120, 255) if edge else ((140, 255, 235, 255) if x < 8 else (70, 200, 200, 255)))
    save(img, "item/temporal_residue")


# ---------------------------------------------------------------- blocks

def armature():
    img = new()
    for i in range(16):
        for p in [(i, 0), (i, 15), (0, i), (15, i)]:
            img.putpixel(p, jitter(COPPER, 10))
        img.putpixel((i, i), jitter(COPPER_D, 8))
        img.putpixel((15 - i, i), jitter(COPPER_D, 8))
    for p in [(0, 0), (15, 0), (0, 15), (15, 15), (7, 7), (8, 8), (7, 8), (8, 7)]:
        img.putpixel(p, VERDI)
    for p in [(3, 0), (11, 15), (0, 9), (15, 5)]:
        img.putpixel(p, VERDI)
    save(img, "block/copper_armature")


def flesh(name, nucleus=False):
    # Light, slightly pink-grey flesh: the block tint pushes it towards the aspect colour.
    img = new()
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), jitter((215, 200, 200, 255), 14))
    # veins
    for start in [(0, 4), (0, 11)]:
        x, y = start
        while x < 16:
            img.putpixel((x, y), (150, 125, 130, 255))
            x += 1
            y = max(0, min(15, y + random.choice([-1, 0, 0, 1])))
    for x in range(3, 15, 5):
        for y in range(16):
            if random.random() < 0.25:
                img.putpixel((x, y), (165, 140, 145, 255))
    # copper showing through
    for _ in range(5):
        img.putpixel((random.randint(0, 15), random.randint(0, 15)), (200, 150, 95, 255))
    if nucleus:
        for y in range(5, 11):
            for x in range(5, 11):
                d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
                if d < 3.2:
                    img.putpixel((x, y), (120, 90, 100, 255) if d > 2.2 else (60, 40, 50, 255))
        img.putpixel((7, 7), (240, 230, 230, 255))
    save(img, "block/" + name)


def necrotic():
    img = new()
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), jitter((70, 58, 52, 255), 14))
    for _ in range(18):
        img.putpixel((random.randint(0, 15), random.randint(0, 15)), (40, 32, 30, 255))
    for _ in range(6):
        img.putpixel((random.randint(0, 15), random.randint(0, 15)), (110, 120, 70, 255))
    save(img, "block/organism_core_necrotic")


def graft(name, base, accent, pattern):
    img = new()
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), jitter(base, 12))
    pattern(img, accent)
    save(img, "block/" + name)


def tap_pattern(img, accent):
    for y in range(5, 11):
        for x in range(5, 11):
            img.putpixel((x, y), accent)
    for p in [(7, 7), (8, 7), (7, 8), (8, 8)]:
        img.putpixel(p, (40, 20, 25, 255))
    for i in range(16):
        img.putpixel((i, 0), COPPER)
        img.putpixel((i, 15), COPPER)


def leech_pattern(img, accent):
    for r in range(3):
        for x in range(16):
            y = int(4 + r * 4 + 1.5 * ((x // 2) % 2))
            img.putpixel((x, min(15, y)), accent)
    for p in [(3, 3), (12, 6), (6, 12), (10, 13)]:
        img.putpixel(p, (250, 235, 220, 255))


def gland_pattern(img, accent):
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d < 6:
                img.putpixel((x, y), jitter(accent, 10))
            if 4.5 < d < 5.3:
                img.putpixel((x, y), (120, 50, 80, 255))
    for p in [(6, 6), (9, 7), (7, 9)]:
        img.putpixel(p, (255, 220, 235, 255))


def jug():
    img = new()
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), (175, 200, 205, 180))
    for x in range(16):
        img.putpixel((x, 0), (120, 80, 55, 255))
        img.putpixel((x, 1), COPPER)
        img.putpixel((x, 15), COPPER_D)
    for y in range(2, 15):
        img.putpixel((0, y), GLASS_D)
        img.putpixel((15, y), GLASS_D)
    for y in range(3, 9):
        img.putpixel((3, y), (245, 250, 255, 230))
    save(img, "block/essence_jug")


def tear():
    img = new()
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d < 7.5:
                a = int(255 * (1 - d / 8))
                c = (20, 30, 40, a) if d < 2.5 else jitter((60, 210, 200, a), 25)
                img.putpixel((x, y), c)
    for _ in range(10):
        x, y = random.randint(3, 12), random.randint(3, 12)
        img.putpixel((x, y), (255, 200, 120, 255))
    save(img, "block/time_tear")


syringe()
needle("iron_needle", (200, 200, 205, 255), (130, 130, 140, 255))
needle("gold_needle", (250, 215, 80, 255), (190, 140, 40, 255))
needle("bone_needle", (235, 228, 205, 255), (180, 170, 145, 255))
needle("amethyst_needle", (190, 140, 230, 255), (120, 80, 170, 255))
vial("essence_vial", (125, 85, 55, 255), 4)
vial("stasis_ampoule", (60, 55, 80, 255), 3, lining=(110, 120, 160, 255))
culture()
brain()
residue()
armature()
flesh("chronoflesh")
flesh("organism_core", nucleus=True)
necrotic()
graft("tap", (200, 150, 155, 255), (150, 90, 100, 255), tap_pattern)
graft("leech_tendril", (175, 110, 120, 255), (110, 50, 65, 255), leech_pattern)
graft("enhancing_gland", (210, 140, 165, 255), (230, 120, 160, 255), gland_pattern)
jug()
tear()
print("ok")

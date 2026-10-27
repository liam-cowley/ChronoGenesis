"""
Isometric preview renderer for ChronoGenesis block models (no game needed).

Reads the generated block model JSON (elements, textures, tint indices, element rotations) and
draws them from above-front-right with simple face shading and a depth buffer.

Usage:
  python3 tools/render_models.py <output.png> [scene]
Scenes: "organisms" (default) renders every flesh growth stage, core phase and graft, plus a small
organism mid-growth. "vessels" renders the Essence Jug at every fill level.
"""
import json
import math
import os
import sys

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MODELS = os.path.join(ROOT, "src/generated/resources/assets/chronogenesis/models/block")
TEXTURES = os.path.join(ROOT, "src/main/resources/assets/chronogenesis/textures")

ASPECT_COLORS = {"density": 0x6E7F96, "speed": 0xB8C93A, "size": 0xB4762E, "awareness": 0xB9A3E3, "time": 0x3FD1C2}
FLESH_BASE = 0xA0546E
FACE_LIGHT = {"up": 1.0, "south": 0.82, "east": 0.64}
_tex_cache = {}


def flesh_tint(aspect):
    a = ASPECT_COLORS[aspect]
    return tuple((((FLESH_BASE >> s) & 255) + ((a >> s) & 255)) // 2 for s in (16, 8, 0))


def texture(ref):
    ns, path = ref.split(":") if ":" in ref else ("minecraft", ref)
    key = (ns, path)
    if key not in _tex_cache:
        if ns == "minecraft":
            # Vanilla textures are not in this repo: stand in a plain copper-coloured tile for previews.
            img = Image.new("RGBA", (16, 16), (192, 108, 66, 255))
            for i in range(16):
                img.putpixel((i, 0), (226, 150, 104, 255))
                img.putpixel((0, i), (226, 150, 104, 255))
                img.putpixel((i, 15), (140, 74, 46, 255))
            _tex_cache[key] = img
        else:
            _tex_cache[key] = Image.open(os.path.join(TEXTURES, path + ".png")).convert("RGBA")
    return _tex_cache[key]


def load_model(name):
    with open(os.path.join(MODELS, name + ".json")) as f:
        model = json.load(f)
    textures = model.get("textures", {})

    def resolve(ref):
        while ref.startswith("#"):
            ref = textures[ref[1:]]
        return ref

    return model.get("elements", []), resolve


def rotate(point, rotation):
    if not rotation:
        return point
    ox, oy, oz = rotation["origin"]
    a = math.radians(rotation["angle"])
    x, y, z = point[0] - ox, point[1] - oy, point[2] - oz
    c, s = math.cos(a), math.sin(a)
    axis = rotation["axis"]
    if axis == "x":
        y, z = y * c - z * s, y * s + z * c
    elif axis == "y":
        x, z = x * c + z * s, -x * s + z * c
    else:
        x, y = x * c - y * s, x * s + y * c
    return (x + ox, y + oy, z + oz)


class Canvas:
    def __init__(self, width, height, scale, origin):
        self.img = Image.new("RGBA", (width, height), (46, 42, 52, 255))
        self.depth = [[-1e9] * width for _ in range(height)]
        self.scale = scale
        self.ox, self.oy = origin
        self.w, self.h = width, height

    def project(self, p):
        x, y, z = p
        sx = self.ox + (x - z) * 0.866 * self.scale
        sy = self.oy + ((x + z) * 0.5 - y) * self.scale
        return sx, sy, x + y + z

    def splat(self, p, color, size):
        sx, sy, d = self.project(p)
        r = max(1, int(size))
        for py in range(int(sy) - r // 2, int(sy) + r - r // 2):
            for px in range(int(sx) - r // 2, int(sx) + r - r // 2):
                if 0 <= px < self.w and 0 <= py < self.h and d >= self.depth[py][px] - 1e-6:
                    self.depth[py][px] = d
                    self.img.putpixel((px, py), color)


def draw_model(canvas, name, offset=(0, 0, 0), tint=None):
    elements, resolve = load_model(name)
    step = 0.25
    for el in elements:
        (x1, y1, z1), (x2, y2, z2) = el["from"], el["to"]
        rot = el.get("rotation")
        for face_name, face in el.get("faces", {}).items():
            if face_name not in FACE_LIGHT:
                continue
            tex = texture(resolve(face["texture"]))
            light = FACE_LIGHT[face_name]
            tinted = face.get("tintindex", -1) >= 0 and tint is not None
            if face_name == "up":
                us, vs = (x1, x2), (z1, z2)
                point = lambda u, v: (u, y2, v)
                uv = lambda u, v: (u, v)
            elif face_name == "south":
                us, vs = (x1, x2), (y1, y2)
                point = lambda u, v: (u, v, z2)
                uv = lambda u, v: (u, 16 - v)
            else:  # east
                us, vs = (z1, z2), (y1, y2)
                point = lambda u, v: (x2, v, u)
                uv = lambda u, v: (16 - u, 16 - v)
            u = us[0] + step / 2
            while u < us[1]:
                v = vs[0] + step / 2
                while v < vs[1]:
                    tu, tv = uv(u, v)
                    px = tex.getpixel((int(tu) % 16, int(tv) % 16))
                    if px[3] >= 128:
                        r, g, b = px[:3]
                        if tinted:
                            r, g, b = r * tint[0] // 255, g * tint[1] // 255, b * tint[2] // 255
                        color = (int(r * light), int(g * light), int(b * light), 255)
                        p = rotate(point(u, v), rot)
                        p = (p[0] + offset[0], p[1] + offset[1], p[2] + offset[2])
                        canvas.splat(p, color, canvas.scale * step * 1.6)
                    v += step
                u += step


def organisms_scene(out):
    tint = flesh_tint("density")
    cells = [
        ("chronoflesh_growth_0", tint, "Flesh 0: tendrils"), ("chronoflesh_growth_1", tint, "1: creeping"),
        ("chronoflesh_growth_2", tint, "2: swelling"), ("chronoflesh_growth_3", tint, "3: mature"),
        ("organism_core_seed", tint, "Core: seed"), ("organism_core_awake", tint, "awake"),
        ("organism_core_hungry", tint, "hungry"), ("organism_core_necrotic", None, "necrotic"),
        ("tap", None, "Tap"), ("leech_tendril", None, "Leech Tendril"), ("enhancing_gland", None, "Enhancing Gland"),
        ("heart", None, "Heart"), ("eye", None, "Eye"), ("clock", None, "Clock"), ("ganglion", None, "Ganglion"),
        ("dissection_table", None, "Dissection Table"), ("dissection_table_brain", None, "with brain"),
        ("chronocortex", None, "Chronocortex"),
    ]
    cols, cell = 4, 200
    rows = (len(cells) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * cell, rows * cell + 330), (46, 42, 52, 255))
    for i, (name, t, _label) in enumerate(cells):
        c = Canvas(cell, cell, 6.0, (cell / 2, cell / 2 + 22))
        draw_model(c, name, tint=t)
        sheet.paste(c.img, ((i % cols) * cell, (i // cols) * cell))

    # A small organism mid-growth: frame, flesh at every stage, the core awake, grafts on top.
    scene = Canvas(cols * cell, 330, 5.5, (cols * cell / 2, 190))
    layout = [
        ("chronoflesh_growth_0", (-32, 0, 0)), ("chronoflesh_growth_1", (-16, 0, 0)),
        ("organism_core_awake", (0, 0, 0)), ("chronoflesh_growth_2", (16, 0, 0)), ("chronoflesh_growth_3", (32, 0, 0)),
        ("chronoflesh_growth_3", (0, 0, -16)), ("chronoflesh_growth_3", (16, 0, -16)), ("chronoflesh_growth_2", (-16, 0, -16)),
        ("chronoflesh_growth_3", (0, 16, -16)), ("tap", (0, 32, -16)), ("enhancing_gland", (32, 16, 0)),
        ("leech_tendril", (16, 16, 0)),
    ]
    for name, off in sorted(layout, key=lambda e: e[1][0] + e[1][1] + e[1][2]):
        draw_model(scene, name, offset=off, tint=tint if "flesh" in name or "core" in name else None)
    sheet.paste(scene.img, (0, rows * cell))
    sheet.save(out)


def vessels_scene(out):
    # Jug fill levels 0-4, liquid tinted with a different aspect each.
    cells = [("essence_jug", None)] + [
        ("essence_jug_%d" % fill, aspect) for fill, aspect in zip(range(1, 5), ["density", "speed", "size", "density"])]
    cell = 200
    sheet = Image.new("RGBA", (len(cells) * cell, cell), (46, 42, 52, 255))
    for i, (name, aspect) in enumerate(cells):
        c = Canvas(cell, cell, 9.0, (cell / 2, cell / 2 - 30))
        color = ASPECT_COLORS[aspect] if aspect else None
        tint = None if color is None else ((color >> 16) & 255, (color >> 8) & 255, color & 255)
        draw_model(c, name, tint=tint)
        sheet.paste(c.img, (i * cell, 0))
    sheet.save(out)


SCENES = {"organisms": organisms_scene, "vessels": vessels_scene}

if __name__ == "__main__":
    SCENES[sys.argv[2] if len(sys.argv) > 2 else "organisms"](sys.argv[1])
    print("rendered", sys.argv[1])

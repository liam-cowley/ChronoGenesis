"""
The Chronocortex's models, one per stage, and its Time Capsule's: Bedrock geometry (.geo.json, the format Blockbench
opens as a "Bedrock Entity" model, box UV) in assets/chronogenesis/geo/, each with its texture painted for its cubes in
assets/chronogenesis/textures/entity/. client/GeoModel loads them; client/CortexBody poses them.

  cortex_stage_0   the brain afloat in its jar (drawn at half scale: authored at 2 units a pixel)
  cortex_stage_1   swollen to fill the jar, cracking it, tentacles budding over the rim (half scale)
  cortex_stage_2   the lump of brain grown over its 3 x 3 x 2, the jar the middle of the bottom layer
  cortex_stage_3   the writhing mass over its 5 x 5 x 3, machinery shining through
  cortex_capsule   a Time Capsule built into it (drawn wherever one stands in its last shell, facing out)

Model space: pixels, y up, the origin the middle of the bottom of the jar's block, +z the front. The stage 2 and 3
models leave four slots for the corridor to the jar (two blocks high, from beside the jar out along each axis) and
fill each with a door_<side> bone, hidden while a corridor is open on that side.

Bone names say what a bone is, for the painter here and the poser in the game:
  lump*, crown*, fold*, knob*, brow*, door*, brain*, hemi*, flesh*   brain matter (painted pale: tinted in game)
  tentacle*, cord*            tentacle skin; each child of a chain sways
  eye_*                       an eyeball: turned to look at whoever it sees, within its rest direction
  iris_*, pupil_*             on the eye's front (+z); the iris glows from stage 2
  lid_*_up, lid_*_down        lids, closing for a blink
  brass*, band*, rib*, rivet*, electrode*, collar*, cap*    brass
  iron*, plate*, housing*     riveted iron;  rod_*: a piston rod, pumping;  gauge_*: a gauge (dial on its front)
  needle_*                    a gauge's needle, twitching;  copper*, pipe*: copper
  glow*, vent*                burning light (drawn full bright);  glass*: capsule glass (its front a picture)
  nerve*, sinew*, strand*     sinew;  inside_<side>*: only drawn while that side's corridor is open
  ring_*                      a capsule's ring, turning;  core*: the moment in a capsule, turning, glowing

Usage: python3 tools/cortex_models.py
"""
import json
import math
import os
import random

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
GEO = os.path.join(ROOT, "src/main/resources/assets/chronogenesis/geo")
TEX = os.path.join(ROOT, "src/main/resources/assets/chronogenesis/textures/entity")


class Model:
    def __init__(self, name):
        self.name = name
        self.bones = []  # (name, parent, pivot, rot)
        self.cubes = {}  # bone -> [(min, size, material)]

    def bone(self, name, pivot=(0, 0, 0), rot=(0, 0, 0), parent=None):
        assert all(b[0] != name for b in self.bones), name
        self.bones.append((name, parent, tuple(pivot), tuple(rot)))
        self.cubes[name] = []
        return name

    def cube(self, bone, mn, size, material=None):
        size = tuple(max(1, int(round(s))) for s in size)
        self.cubes[bone].append((tuple(mn), size, material or material_of(bone)))

    # ---------------------------------------------------------------- writing

    def write(self, texture_width=512):
        layout = pack([(b, i, c) for b, _, _, _ in self.bones for i, c in enumerate(self.cubes[b])], texture_width)
        height = 64
        while height < layout["height"]:
            height *= 2
        bones = []
        for name, parent, pivot, rot in self.bones:
            entry = {"name": name, "pivot": [-pivot[0], pivot[1], pivot[2]]}
            if parent:
                entry["parent"] = parent
            if any(rot):
                entry["rotation"] = [-rot[0], -rot[1], rot[2]]
            cubes = []
            for i, (mn, size, _) in enumerate(self.cubes[name]):
                u, v = layout["at"][(name, i)]
                cubes.append({"origin": [-(mn[0] + size[0]), mn[1], mn[2]], "size": list(size), "uv": [u, v]})
            if cubes:
                entry["cubes"] = cubes
            bones.append(entry)
        geo = {"format_version": "1.12.0", "minecraft:geometry": [{
            "description": {"identifier": "geometry.chronogenesis." + self.name, "texture_width": texture_width,
                            "texture_height": height, "visible_bounds_width": 6, "visible_bounds_height": 4,
                            "visible_bounds_offset": [0, 1.5, 0]},
            "bones": bones}]}
        os.makedirs(GEO, exist_ok=True)
        with open(os.path.join(GEO, self.name + ".geo.json"), "w", encoding="utf-8", newline="\n") as f:
            json.dump(geo, f, indent=1)
            f.write("\n")
        img = Image.new("RGBA", (texture_width, height), (0, 0, 0, 0))
        for (name, i), (u, v) in layout["at"].items():
            mn, size, material = self.cubes[name][i]
            paint_cube(img, u, v, size, material, hash((self.name, name, i)) & 0xFFFF)
        os.makedirs(TEX, exist_ok=True)
        img.save(os.path.join(TEX, self.name + ".png"))
        print("wrote", self.name, len(self.bones), "bones,", sum(len(c) for c in self.cubes.values()), "cubes,",
              f"{texture_width}x{height}")


def material_of(bone):
    for prefix, material in (("vial", "vial"), ("iris", "iris"), ("pupil", "pupil"), ("eye", "sclera"), ("lid", "lid"), ("tentacle", "tentacle"),
                             ("cord", "tentacle"), ("needle", "needle"), ("gauge", "gauge"), ("rod", "rod"),
                             ("glass", "glass"), ("glow", "glow"), ("vent", "glow"), ("core", "glow"),
                             ("copper", "copper"), ("pipe", "copper"), ("iron", "iron"), ("plate", "iron"),
                             ("housing", "iron"), ("brass", "brass"), ("band", "brass"), ("rib", "brass"),
                             ("rivet", "brass"), ("electrode", "brass"), ("collar", "brass"), ("cap", "brass"),
                             ("ring", "brass"), ("nerve", "sinew"), ("sinew", "sinew"), ("strand", "sinew"),
                             ("inside", "sinew")):
        if bone.startswith(prefix) or ("_" + prefix) in bone:
            return material
    return "brain"


def pack(items, width):
    """Shelf-packs each cube's box-UV unwrap ((2d + 2w) x (d + h)), tallest first."""
    boxes = []
    for bone, i, (mn, (w, h, d), _) in items:
        boxes.append((2 * (w + d), d + h, (bone, i)))
    boxes.sort(key=lambda b: -b[1])
    x = y = shelf = 0
    at = {}
    for bw, bh, key in boxes:
        assert bw <= width, (key, bw)
        if x + bw > width:
            y += shelf
            x = shelf = 0
        at[key] = (x, y)
        x += bw
        shelf = max(shelf, bh)
    return {"at": at, "height": y + shelf}


# ---------------------------------------------------------------- painting

def paint_cube(img, u, v, size, material, seed):
    """Paints one cube's box-UV unwrap: top and bottom on the first row, the four sides below (the last, +z, its front)."""
    w, h, d = size
    faces = [("top", u + d, v, w, d), ("bottom", u + d + w, v, w, d), ("east", u, v + d, d, h), ("north", u + d, v + d, w, h),
             ("west", u + d + w, v + d, d, h), ("south", u + 2 * d + w, v + d, w, h)]
    r = random.Random(seed)
    for face, x0, y0, fw, fh in faces:
        for y in range(fh):
            for x in range(fw):
                c = pixel(material, face, x, y, fw, fh, r, seed)
                if c is not None:
                    img.putpixel((x0 + x, y0 + y), c)


def clamp(c):
    return tuple(max(0, min(255, int(k))) for k in c[:3]) + ((c[3] if len(c) > 3 else 255),)


def pixel(material, face, x, y, w, h, r, seed):
    j = r.randint(-5, 5)
    shade = 1.08 if face == "top" else 0.82 if face == "bottom" else 1.0
    if material == "brain":
        # Serpentine gyri, dark sulci between them (pale: tinted by what it knows).
        s = seed % 97
        w1 = (x + s) * 0.62 + 2.4 * math.sin((y + s) * 0.33 + 0.6) + 1.6 * math.sin((x + y) * 0.19)
        crown = abs(math.sin(w1))
        if crown < 0.22 or (abs(math.sin((y + s) * 0.45 + 2.2 * math.sin((x + s) * 0.24))) < 0.08):
            return clamp((118 * shade + j, 72 * shade + j, 84 * shade + j))
        v = (172 + 76 * crown ** 0.6) * shade
        return clamp((v + j, v * 0.88 + j, v * 0.9 + j))
    if material == "tentacle":
        ring = 0.82 if y % 4 == 0 else 1.0
        if face == "north" and w >= 2 and (y % 4 == 2) and (x % 3 == 1):
            return clamp((246, 228, 224))
        v = 214 * ring * shade
        return clamp((v + j, v * 0.85 + j, v * 0.88 + j))
    if material == "lid":
        v = (200 + 18 * math.sin(y * 1.7 + x * 0.3)) * shade
        if y == h - 1 or y == 0:
            v *= 0.72
        return clamp((v + j, v * 0.82 + j, v * 0.84 + j))
    if material == "sclera":
        v = 236 * shade
        if r.random() < 0.06:
            return clamp((196, 52, 60))
        return clamp((v + j, v * 0.95 + j, v * 0.9 + j))
    if material == "iris":
        if face == "south":
            dx, dy = (x + 0.5) / w - 0.5, (y + 0.5) / h - 0.5
            dd = math.hypot(dx, dy) * 2
            a = math.atan2(dy, dx)
            stria = 0.78 + 0.22 * math.sin(a * 11)
            if dd > 0.92:
                return clamp((70, 60, 54))
            v = (255 if dd < 0.4 else 236 - 60 * (dd - 0.4)) * stria
            return clamp((v, v * 0.97, v * 0.85))
        return clamp((90, 76, 70))
    if material == "pupil":
        return clamp((14, 8, 18))
    if material == "brass":
        c = (252, 226, 146) if (x + y) % 5 == 0 else (214, 168, 80) if (x + y) % 5 < 4 else (152, 108, 46)
        return clamp(tuple(k * shade for k in c))
    if material == "iron":
        edge = x in (0, w - 1) or y in (0, h - 1)
        if (x in (1, w - 2) and y in (1, h - 2)) and w > 3 and h > 3:
            return clamp((176, 178, 188))
        v = (96 - (14 if edge else 0) + r.randint(-6, 6)) * shade
        return clamp((v, v + 2, v + 10))
    if material == "rod":
        v = (200 - 30 * (x % 2)) * shade
        return clamp((v, v * 0.96, v * 0.86))
    if material == "copper":
        if (x * 3 + y * 5) % 11 == 0:
            return clamp((88, 168, 136))
        return clamp(((196 - y * 2) * shade, (112 - y) * shade, 66 * shade))
    if material == "gauge":
        if face == "south":
            dx, dy = x - (w - 1) / 2, y - (h - 1) / 2
            dd = math.hypot(dx, dy) / max(1, (w - 1) / 2)
            return clamp((176, 132, 56) if dd > 0.85 else (236, 226, 196) if dd > 0.12 else (40, 30, 30))
        return pixel("iron", face, x, y, w, h, r, seed)
    if material == "needle":
        return clamp((30, 24, 28))
    if material == "glow":
        dd = math.hypot((x + 0.5) / w - 0.5, (y + 0.5) / h - 0.5) * 2
        return clamp((150 + 100 * (1 - dd), 255, 200 + 50 * (1 - dd)))
    if material == "glass":
        if face == "south":
            dx, dy = (x + 0.5) / w - 0.5, ((y + 0.5) / h - 0.5) * 0.7
            dd = math.hypot(dx, dy) * 2
            if x == 1 and 1 <= y <= h - 3:
                return clamp((248, 255, 255))
            return clamp((255, 196, 90) if dd < 0.45 else (190, 226, 236) if dd < 0.95 else (140, 176, 190))
        return clamp((160, 196, 206))
    if material == "vial":
        # Glass: its edges and a glint, clear between (whatever burns inside shows).
        if x in (0, w - 1) or y in (0, h - 1):
            return clamp((176, 214, 226))
        if x == 1 and 2 <= y <= h - 3:
            return clamp((240, 252, 255))
        return None
    if material == "sinew":
        v = (206 + 26 * math.sin(x * 2.1 + y * 0.3)) * shade
        return clamp((v + j, v * 0.86 + j, v * 0.85 + j))
    return clamp((255, 0, 255))


# ---------------------------------------------------------------- parts

def facing(n):
    """Rotation (degrees) turning +z to the unit direction n: about y, then x (applied z, y, x)."""
    x, y, z = n
    length = math.sqrt(x * x + y * y + z * z)
    x, y, z = x / length, y / length, z / length
    return (math.degrees(-math.asin(max(-1, min(1, y)))), math.degrees(math.atan2(x, z)), 0)


def pointing(n):
    """Rotation (degrees) turning +y to the unit direction n."""
    x, y, z = n
    length = math.sqrt(x * x + y * y + z * z)
    x, y, z = x / length, y / length, z / length
    return (math.degrees(math.acos(max(-1, min(1, y)))), math.degrees(math.atan2(x, z)), 0)


def eye(m, name, centre, normal, s, parent=None):
    """An eyeball of side s at centre, resting towards normal: the ball, its iris and pupil on the front, lids, a rim."""
    cx, cy, cz = centre
    rot = facing(normal)
    ball = m.bone("eye_" + name, centre, rot, parent)
    m.cube(ball, (cx - s / 2, cy - s / 2, cz - s / 2), (s, s, s))
    i = max(1, s - 2 if s >= 4 else s - 1)
    iris = m.bone("iris_" + name, centre, parent=ball)
    m.cube(iris, (cx - i / 2, cy - i / 2, cz + s / 2), (i, i, 1 if s >= 6 else 1))
    pw = max(1, round(s / 5))
    ph = max(1, i - 2 if i >= 4 else i)
    pupil = m.bone("pupil_" + name, centre, parent=iris)
    m.cube(pupil, (cx - pw / 2, cy - ph / 2, cz + s / 2 + 0.6), (pw, ph, 1))
    lw = s + 2
    lt = max(1, round(s * 0.3))
    up = m.bone("lid_" + name + "_up", centre, rot, parent)
    m.cube(up, (cx - lw / 2, cy + s / 2 - lt / 2, cz - s / 2), (lw, lt, s + 1))
    down = m.bone("lid_" + name + "_down", centre, rot, parent)
    m.cube(down, (cx - lw / 2, cy - s / 2 - lt / 2, cz - s / 2), (lw, lt, s + 1))
    rim = m.bone("flesh_" + name + "_rim", centre, rot, parent)
    m.cube(rim, (cx - s / 2 - 1, cy - s / 2, cz - s / 2), (1, s, s))
    m.cube(rim, (cx + s / 2, cy - s / 2, cz - s / 2), (1, s, s))


def tentacle(m, name, root, direction, segments, thick, length, taper=0.65, eye_at_tip=None, parent=None):
    """A tentacle of box segments from root along direction, tapering; each segment a bone that sways."""
    rx, ry, rz = root
    bone = m.bone("tentacle_" + name, root, pointing(direction), parent)
    for k in range(segments):
        t = max(1, round(thick * (1 - taper * k / segments)))
        if k > 0:
            bone = m.bone(f"tentacle_{name}_{k}", (rx, ry + k * length, rz), parent=bone)
        m.cube(bone, (rx - t / 2, ry + k * length, rz - t / 2), (t, length + 1, t))
        if k % 3 == 1 and t >= 2:
            m.cube(bone, (rx - t / 2 - 0.5, ry + k * length + length / 2 - 0.5, rz - t / 2 - 0.5), (t + 1, 1, t + 1))
    if eye_at_tip:
        tip = (rx, ry + segments * length + eye_at_tip / 2, rz)
        eye(m, "stalk_" + name, tip, (0, 1, 0), eye_at_tip, parent=bone)


def lump(m, name, mn, size, parent=None):
    """A lump of brain, its pivot at the middle of its foot (so it heaves up and out)."""
    pivot = (mn[0] + size[0] / 2, mn[1], mn[2] + size[2] / 2)
    b = m.bone(name, pivot, parent=parent)
    m.cube(b, mn, size)
    return b


def mirror_quadrants(fn):
    for sx in (-1, 1):
        for sz in (-1, 1):
            fn(sx, sz)


def box_in_quadrant(sx, sz, x0, x1, y0, y1, z0, z1):
    """A box between x0..x1 and z0..z1 out from the middle, in quadrant (sx, sz): (min, size)."""
    xs = sorted((sx * x0, sx * x1))
    zs = sorted((sz * z0, sz * z1))
    return (xs[0], y0, zs[0]), (xs[1] - xs[0], y1 - y0, zs[1] - zs[0])


SIDES = {"south": (0, 1), "north": (0, -1), "east": (1, 0), "west": (-1, 0)}


def door(m, side, reach, height, bulge):
    """The plug of flesh in the corridor slot on one side: from beside the jar out past the body, two blocks high."""
    dx, dz = SIDES[side]
    b = m.bone("door_" + side, (dx * reach / 2, 0, dz * reach / 2))
    near, far = 8, reach + bulge
    if dx:
        xs = sorted((dx * near, dx * far))
        m.cube(b, (xs[0], 0, -8), (xs[1] - xs[0], height, 16))
        m.cube(b, (dx * far - (0 if dx > 0 else 2), 6, -6), (2, height - 12, 12))
        seam = m.bone("door_" + side + "_fold", (dx * (far + 1), 0, 0), parent=b)
        m.cube(seam, (dx * far - (0 if dx > 0 else 1) + dx * 1.5, 3, -0.5), (1, height - 6, 1))
    else:
        zs = sorted((dz * near, dz * far))
        m.cube(b, (-8, 0, zs[0]), (16, height, zs[1] - zs[0]))
        m.cube(b, (-6, 6, dz * far - (0 if dz > 0 else 2)), (12, height - 12, 2))
        seam = m.bone("door_" + side + "_fold", (0, 0, dz * (far + 1)), parent=b)
        m.cube(seam, (-0.5, 3, dz * far - (0 if dz > 0 else 1) + dz * 1.5), (1, height - 6, 1))
    # Strands hanging from the corridor's roof, seen only while it is open.
    inside = m.bone("inside_" + side, (dx * reach / 2, height, dz * reach / 2))
    for k in range(3):
        along = 11 + k * (reach - 14) / 2
        off = (-3, 2, -1)[k]
        hang = (5, 8, 6)[k]
        if dx:
            m.cube(inside, (dx * along - 0.5, height - hang, off - 0.5), (1, hang, 1))
        else:
            m.cube(inside, (off - 0.5, height - hang, dz * along - 0.5), (1, hang, 1))


# ---------------------------------------------------------------- stage 0 and 1: the brain in its jar (2 units a pixel)

def jar_brain(swollen):
    m = Model("cortex_stage_1" if swollen else "cortex_stage_0")
    cy = 26 if swollen else 25
    brain = m.bone("brain", (0, cy, 0))
    if swollen:
        hw, hh, hd, y0 = 8, 12, 18, 20
    else:
        hw, hh, hd, y0 = 6, 6, 12, 22
    m.cube(brain, (-hw - 1, y0, -hd / 2), (hw, hh, hd), "brain")
    m.cube(brain, (1, y0, -hd / 2), (hw, hh, hd), "brain")
    m.cube(brain, (-1, y0 + 1, -hd / 2 + 1), (2, hh - 2, hd - 2), "brain")
    # Folds over each hemisphere, offset from each other.
    for side in (-1, 1):
        x0 = -hw - 1 if side < 0 else 1
        for k in range(4 if swollen else 3):
            z = -hd / 2 + 1 + k * (hd - 2) / (4 if swollen else 3)
            fold = m.bone(f"fold_{'l' if side < 0 else 'r'}{k}", (x0 + hw / 2, y0 + hh, z), parent=brain)
            m.cube(fold, (x0 + 1 + (k % 2), y0 + hh, z), (hw - 2 - (k % 2), 1 if not swollen else 2, 2 if not swollen else 3))
        m.cube(brain, (x0 + (0 if side < 0 else hw) - (1 if side < 0 else 0), y0 + 1, -hd / 2 + 2), (1, hh - 2, hd - 5), "brain")
    m.cube(brain, (-hw * 0.6, y0 - 3, -hd / 2), (round(hw * 1.2), 3, round(hd * 0.35)), "brain")
    if not swollen:
        cord = m.bone("cord_0", (0, y0 - 2, -hd / 2 + 1), (40, 0, 0), brain)
        for k in range(3):
            if k:
                cord = m.bone(f"cord_{k}", (0, y0 - 2 - k * 3, -hd / 2 + 1), parent=cord)
            m.cube(cord, (-1, y0 - 2 - (k + 1) * 3, -hd / 2), (2, 3, 2))
    eye(m, "brow", (0, y0 + hh * 0.6, hd / 2 + 1), (0, 0, 1), 6 if swollen else 4, parent=brain)
    if swollen:
        eye(m, "side", (-hw - 2.5, y0 + hh * 0.45, 3), (-1, 0, 0), 4, parent=brain)
        for k, (x, z) in enumerate(((6, 6), (-6, 6), (6, -6), (-6, -6))):
            tentacle(m, f"bud{k}", (x, 31, z), (x * 0.08, 1, z * 0.08), 4, 4, 3, parent=brain)
    lid = 33 if swollen else 31
    for k, (x, z) in enumerate(((-5, 0), (5, 0)) if not swollen else ((-5, 0), (5, 0), (0, -5), (0, 5))):
        e = m.bone(f"electrode_{k}", (x, y0 + hh, z))
        m.cube(e, (x - 0.5, y0 + hh, z - 0.5), (1, lid - y0 - hh, 1))
    m.write(128)


# ---------------------------------------------------------------- the grown bodies

def keep_out(reach):
    """What the body must not grow into: the jar's block, and the four corridor slots (two blocks high)."""
    zones = [(-8, 0, -8, 8, 16, 8)]
    for dx, dz in SIDES.values():
        if dx:
            zones.append((min(8 * dx, (reach + 8) * dx), 0, -8, max(8 * dx, (reach + 8) * dx), 32, 8))
        else:
            zones.append((-8, 0, min(8 * dz, (reach + 8) * dz), 8, 32, max(8 * dz, (reach + 8) * dz)))
    return zones


def clip(box, zones):
    """Pulls a box (x0, y0, z0, x1, y1, z1) back out of each zone it pokes into, along its shallowest way out."""
    box = list(box)
    for z in zones:
        if box[0] >= z[3] or box[3] <= z[0] or box[1] >= z[4] or box[4] <= z[1] or box[2] >= z[5] or box[5] <= z[2]:
            continue
        best = None
        for a in range(3):
            for low in (True, False):
                depth = box[a + 3] - z[a] if low else z[a + 3] - box[a]
                if best is None or depth < best[0]:
                    best = (depth, a, low)
        _, a, low = best
        if low:
            box[a + 3] = z[a]
        else:
            box[a] = z[a + 3]
    return box


def sorted_box(b):
    return (min(b[0], b[3]), min(b[1], b[4]), min(b[2], b[5]), max(b[0], b[3]), max(b[1], b[4]), max(b[2], b[5]))


def put_lump(m, name, box, parent=None):
    """A lump for a box (x0, y0, z0, x1, y1, z1), if anything is left of it; returns the box drawn, or None."""
    mn = (round(box[0]), round(box[1]), round(box[2]))
    size = (round(box[3]) - mn[0], round(box[4]) - mn[1], round(box[5]) - mn[2])
    if min(size) < 1:
        return None
    lump(m, name, mn, size, parent)
    return (mn[0], mn[1], mn[2], mn[0] + size[0], mn[1] + size[1], mn[2] + size[2])


def heap(m, name, region, r, pieces, zones, swell, parent=None):
    """A heap of lumps filling a region (x0, y0, z0, x1, y1, z1): a core just inside it and lumps of every size round
    that, swelling out of it by up to swell; none in the zones. Returns the lumps' boxes."""
    x0, y0, z0, x1, y1, z1 = region
    boxes = [(x0 + 1, y0, z0 + 1, x1 - 1, y1 - 1, z1 - 1)]
    for _ in range(pieces):
        w = r.uniform(0.35, 0.8) * (x1 - x0)
        d = r.uniform(0.35, 0.8) * (z1 - z0)
        h = r.uniform(0.3, 0.75) * (y1 - y0)
        cx = r.uniform(x0 + w / 2 - swell, x1 - w / 2 + swell)
        cz = r.uniform(z0 + d / 2 - swell, z1 - d / 2 + swell)
        cy = r.uniform(y0 + h / 2, y1 - h / 2 + swell * 0.6)
        boxes.append((cx - w / 2, max(0, cy - h / 2), cz - d / 2, cx + w / 2, cy + h / 2, cz + d / 2))
    out = []
    for i, b in enumerate(boxes):
        drawn = put_lump(m, f"{name}_{i}" if i else name, clip(b, zones), parent if i == 0 else (parent or name))
        if drawn:
            out.append(drawn)
    return out


def entry(b, origin, direction):
    """How far along a ray from origin a box is first entered (None if it is missed or behind)."""
    t0, t1 = -1e9, 1e9
    for a in range(3):
        o, d = origin[a], direction[a]
        if abs(d) < 1e-9:
            if not (b[a] <= o <= b[a + 3]):
                return None
            continue
        ta, tb = (b[a] - o) / d, (b[a + 3] - o) / d
        t0, t1 = max(t0, min(ta, tb)), min(t1, max(ta, tb))
    return t0 if t1 >= t0 and t0 >= 0 else None


def on_skin(boxes, at, normal, out=0.5):
    """The spot on the body's skin met coming in from far out along normal through at, pushed out proud by out."""
    origin = tuple(at[a] + normal[a] * 90 for a in range(3))
    inward = tuple(-n for n in normal)
    best = None
    for b in boxes:
        hit = entry(b, origin, inward)
        if hit is not None and (best is None or hit < best):
            best = hit
    if best is None:
        return None
    return tuple(origin[a] + inward[a] * best + normal[a] * out for a in range(3))


def grown_body(m, r, half, height, swell, quad_pieces, crown_pieces, crown_rise):
    """The body over a 3 x 3 x 2 (half 24, height 32) or a 5 x 5 x 3 (40, 48): a heap in each quadrant round the jar,
    the middle over the jar, a domed crown of lobes over everything, swellings round its edge, folds over it."""
    zones = keep_out(half)
    boxes = []
    for sx in (-1, 1):
        for sz in (-1, 1):
            q = f"{'e' if sx > 0 else 'w'}{'s' if sz > 0 else 'n'}"
            x0, x1 = sorted((sx * 8, sx * half))
            z0, z1 = sorted((sz * 8, sz * half))
            boxes += heap(m, "lump_" + q, (x0, 0, z0, x1, 32, z1), r, quad_pieces, zones, swell)
    boxes += heap(m, "lump_middle", (-8, 16, -8, 8, 32, 8), r, 0, zones, 0)
    crown = heap(m, "crown", (-half, 30, -half, half, height, half), r, 0, zones, 0)
    for i in range(crown_pieces):
        # Lobes over the crown, rising higher towards its middle.
        a = r.uniform(-half + 6, half - 6)
        b = r.uniform(-half + 6, half - 6)
        edge = max(abs(a), abs(b)) / half
        w = r.uniform(0.25, 0.5) * half * 2
        d = r.uniform(0.25, 0.5) * half * 2
        top = height + (1 - edge) * crown_rise + r.uniform(-2, 3)
        bottom = height - r.uniform(4, 8)
        drawn = put_lump(m, f"crown_{i + 1}", clip((a - w / 2, bottom, b - d / 2, a + w / 2, top, b + d / 2), zones), "crown")
        if drawn:
            crown.append(drawn)
    # Swellings round the crown's edge (over the corridors' mouths too: they are two blocks high, the crown above them).
    for side, (dx, dz) in SIDES.items():
        for k in range(2):
            along = r.uniform(-half * 0.6, half * 0.6)
            w, h, d = r.uniform(10, 18), r.uniform(6, 10), r.uniform(3, 6)
            y = r.uniform(32, height - 4)
            if dx:
                box = (half * dx, y, along - w / 2, half * dx + d * dx, y + h, along + w / 2)
            else:
                box = (along - w / 2, y, half * dz, along + w / 2, y + h, half * dz + d * dz)
            drawn = put_lump(m, f"crown_bulge_{side}_{k}", clip(sorted_box(box), zones), "crown")
            if drawn:
                crown.append(drawn)
    boxes += crown
    # Gyri: folds winding over the crown.
    for k in range(12 if half > 30 else 6):
        a = r.uniform(-half + 4, half - 4)
        length = r.uniform(half * 0.4, half * 1.1)
        start = r.uniform(-half + 2, half - 2 - length)
        if k % 2:
            p = on_skin(crown, (start + length / 2, 0, a), (0, 1, 0), 0)
            if p:
                lump(m, f"fold_crown_{k}", (round(start), round(p[1] - 1), round(a)), (round(length), 2, 2), "crown")
        else:
            p = on_skin(crown, (a, 0, start + length / 2), (0, 1, 0), 0)
            if p:
                lump(m, f"fold_crown_{k}", (round(a), round(p[1] - 1), round(start)), (2, 2, round(length)), "crown")
    return boxes, zones


def side_spot(normal, along, y):
    """A point on the vertical line through a side's middle, along it and up y (for on_skin)."""
    return (along if normal[0] == 0 else 0, y, along if normal[2] == 0 else 0)


SIDE_NORMALS = ((1, 0, 0), (-1, 0, 0), (0, 0, 1), (0, 0, -1))


# ---------------------------------------------------------------- stage 2: the lump over its 3 x 3 x 2

def stage_2():
    m = Model("cortex_stage_2")
    r = random.Random(2002)
    boxes, zones = grown_body(m, r, 24, 32, 3, 5, 7, 6)
    for side in SIDES:
        door(m, side, 24, 32, 2)
    # Eyes: the great one under a brow over the front corridor, smaller ones on its quadrants and crown.
    lump(m, "brow", (-9, 32, 19), (18, 9, 8))
    eye(m, "great", (0, 37, 28), (0, 0, 1), 8)
    for k, ((along, y), normal) in enumerate((((16, 16), (0, 0, 1)), ((-17, 22), (0, 0, 1)), ((-15, 18), (1, 0, 0)), ((14, 10), (-1, 0, 0)),
                                              ((17, 20), (0, 0, -1)), ((-12, 12), (0, 0, -1)))):
        p = on_skin(boxes, side_spot(normal, along, y), normal, 1.5)
        if p:
            eye(m, f"small{k}", p, normal, 4)
    p = on_skin(boxes, (10, 0, -6), (0, 1, 0), 1.5)
    if p:
        eye(m, "small_top", p, (0, 1, 0), 5)
    # Tentacles out of its sides and crown.
    k = 0
    for normal in SIDE_NORMALS:
        for j in range(2):
            along = (-16 if j == 0 else 15) + r.uniform(-2, 2)
            p = on_skin(boxes, side_spot(normal, along, r.uniform(8, 28)), normal, -1)
            if p:
                tentacle(m, str(k), p, (normal[0] + r.uniform(-0.3, 0.3), 0.5 + r.uniform(0, 0.5), normal[2] + r.uniform(-0.3, 0.3)),
                         6 + r.randint(0, 2), 4, 3)
                k += 1
    for j in range(3):
        p = on_skin(boxes, (r.uniform(-16, 16), 0, r.uniform(-16, 16)), (0, 1, 0), -1)
        if p:
            tentacle(m, str(k), p, (r.uniform(-0.4, 0.4), 1, r.uniform(-0.4, 0.4)), 7, 4, 3)
            k += 1
    # What shows through: a brass band round its crown, a gauge, a pipe up its side, nerves from the jar.
    for side, (mn, size) in {"n": ((-26, 31, -27), (52, 2, 1)), "s": ((-26, 31, 26), (52, 2, 1)),
                             "e": ((26, 31, -26), (1, 2, 52)), "w": ((-27, 31, -26), (1, 2, 52))}.items():
        b = m.bone("band_" + side, (0, 31, 0))
        m.cube(b, mn, size)
    p = on_skin(boxes, (0, 18, 16), (1, 0, 0), 0)
    if p:
        gauge = m.bone("gauge_0", p, facing((1, 0, 0)))
        m.cube(gauge, (p[0] - 3, p[1] - 3, p[2] - 1), (6, 6, 2))
        needle = m.bone("needle_0", (p[0], p[1], p[2] + 1), parent=gauge)
        m.cube(needle, (p[0] - 0.5, p[1], p[2] + 1), (1, 3, 1))
    p = on_skin(boxes, (0, 16, -18), (-1, 0, 0), 0)
    if p:
        pipe = m.bone("pipe_0", p)
        m.cube(pipe, (p[0] - 1, 2, p[2] - 1), (2, 30, 2))
        m.cube(pipe, (p[0] - 1, 31, p[2] - 1), (8, 2, 2))
    for k, x in enumerate((-2.2, 0, 2.2)):
        nerve = m.bone(f"nerve_{k}", (x, 14, 0))
        m.cube(nerve, (x - 0.7, 14, -0.7), (1, 3, 1))
    m.write(512)


# ---------------------------------------------------------------- stage 3: the writhing mass over its 5 x 5 x 3

def stage_3():
    m = Model("cortex_stage_3")
    r = random.Random(3003)
    boxes, zones = grown_body(m, r, 40, 48, 5, 9, 16, 9)
    for side in SIDES:
        door(m, side, 40, 32, 4)
    # Knobs all over its quadrants' sides.
    for k in range(28):
        normal = SIDE_NORMALS[k % 4]
        p = on_skin(boxes, side_spot(normal, r.choice((-1, 1)) * r.uniform(12, 36), r.uniform(3, 28)), normal, 0)
        if not p:
            continue
        w, h, d = r.randint(4, 9), r.randint(4, 9), r.randint(2, 4)
        if normal[0]:
            box = (p[0] - 0.5 * normal[0], p[1] - h / 2, p[2] - w / 2, p[0] + (d - 0.5) * normal[0], p[1] + h / 2, p[2] + w / 2)
        else:
            box = (p[0] - w / 2, p[1] - h / 2, p[2] - 0.5 * normal[2], p[0] + w / 2, p[1] + h / 2, p[2] + (d - 0.5) * normal[2])
        put_lump(m, f"knob_{k}", clip(sorted_box(box), zones))
    # Glass vials of something burning, sunk in its four corners.
    for sx in (-1, 1):
        for sz in (-1, 1):
            q = f"{'e' if sx > 0 else 'w'}{'s' if sz > 0 else 'n'}"
            for name, (a0, a1, y0, y1) in (("vial", (33, 39, 4, 26)), ("glow", (34, 38, 6, 24)), ("cap", (32, 40, 26, 28)),
                                           ("cap_foot", (32, 40, 2, 4))):
                mn, size = box_in_quadrant(sx, sz, a0, a1, y0, y1, a0, a1)
                lump(m, f"{name}_{q}" if name != "cap_foot" else f"cap_{q}_foot", mn, size)
    # Eyes: the great one under its brow over the front corridor, a dozen over it.
    lump(m, "brow", (-12, 32, 37), (24, 15, 9))
    eye(m, "great", (0, 40, 49), (0, 0, 1), 14)
    k = 0
    for normal in SIDE_NORMALS:
        for j in range(3):
            along = r.choice((-1, 1)) * r.uniform(14, 32)
            y = r.uniform(8, 40)
            p = on_skin(boxes, side_spot(normal, along, y), normal, 2)
            if p and not (normal == (0, 0, 1) and abs(along) < 18 and y > 28):
                eye(m, f"small{k}", p, normal, 4 + k % 4)
                k += 1
    for j in range(4):
        p = on_skin(boxes, (r.uniform(-28, 28), 0, r.uniform(-28, 28)), (0, 1, 0), 2)
        if p:
            eye(m, f"small{k}", p, (0, 1, 0), 5 + j % 3)
            k += 1
    # Tentacles: thirty-odd, out of its sides and crown, some ending in eyes.
    k = 0
    for normal in SIDE_NORMALS:
        for j in range(5):
            along = r.choice((-1, 1)) * r.uniform(12, 36)
            p = on_skin(boxes, side_spot(normal, along, r.uniform(4, 40)), normal, -1)
            if p:
                d = (normal[0] + r.uniform(-0.4, 0.4), 0.2 + r.uniform(0, 0.8), normal[2] + r.uniform(-0.4, 0.4))
                tentacle(m, str(k), p, d, 8 + r.randint(0, 5), 6 if j < 3 else 5, 4, eye_at_tip=5 if k % 5 == 2 else None)
                k += 1
    for j in range(12):
        p = on_skin(boxes, (r.uniform(-30, 30), 0, r.uniform(-30, 30)), (0, 1, 0), -1)
        if p:
            tentacle(m, str(k), p, (r.uniform(-0.6, 0.6), 1, r.uniform(-0.6, 0.6)), 9 + r.randint(0, 5), 5, 4,
                     eye_at_tip=5 if j % 3 == 0 else None)
            k += 1
    # Machinery: brass ribs over its crown and down its sides, pistons, gauges, vents, iron plates, copper pipes, nerves.
    for k, x in enumerate((-20, 0, 20)):
        rib = m.bone(f"rib_{k}", (x, 48, 0))
        prev = None
        for z in range(-36, 37, 4):
            p = on_skin(boxes, (x, 0, z), (0, 1, 0), 0)
            if p:
                m.cube(rib, (x - 1, p[1] - 1, z - 2), (2, 2, 4))
                if prev is not None and abs(prev - p[1]) > 1.5:
                    lo, hi = sorted((prev, p[1]))
                    m.cube(rib, (x - 1, lo - 1, z - 3), (2, round(hi - lo) + 1, 2))
                prev = p[1]
        if x != 0:
            for s in (-1, 1):
                p = on_skin(boxes, (x, 20, 0), (0, 0, s), 0)
                if p:
                    m.cube(rib, (x - 1, 4, p[2] - (0 if s > 0 else 2)), (2, 30, 2))
                    for y in range(8, 32, 6):
                        m.cube(rib, (x - 1.5, y, p[2] + (1.5 if s > 0 else -2.5)), (3, 1, 1))
    for k, (normal, along, y) in enumerate((((1, 0, 0), 24, 12), ((-1, 0, 0), -20, 20), ((0, 0, 1), 28, 18), ((0, 0, -1), -26, 10))):
        p = on_skin(boxes, side_spot(normal, along, y), normal, -2)
        if not p:
            continue
        housing = m.bone(f"housing_{k}", p, facing(normal))
        m.cube(housing, (p[0] - 3, p[1] - 3, p[2] - 3), (6, 6, 6))
        rod = m.bone(f"rod_{k}", p, parent=housing)
        m.cube(rod, (p[0] - 1, p[1] - 1, p[2] + 3), (2, 2, 5))
        m.cube(rod, (p[0] - 2, p[1] - 2, p[2] + 8), (4, 4, 1), "iron")
    for k, (normal, along, y) in enumerate((((1, 0, 0), -14, 26), ((-1, 0, 0), 14, 8), ((0, 0, -1), 12, 24))):
        p = on_skin(boxes, side_spot(normal, along, y), normal, 0)
        if not p:
            continue
        gauge = m.bone(f"gauge_{k}", p, facing(normal))
        m.cube(gauge, (p[0] - 3.5, p[1] - 3.5, p[2] - 1), (7, 7, 2))
        needle = m.bone(f"needle_{k}", (p[0], p[1], p[2] + 1), parent=gauge)
        m.cube(needle, (p[0] - 0.5, p[1], p[2] + 1), (1, 3, 1))
    for k in range(6):
        normal = SIDE_NORMALS[k % 4]
        p = on_skin(boxes, side_spot(normal, r.choice((-1, 1)) * r.uniform(14, 34), r.uniform(6, 28)), normal, 0)
        if not p:
            continue
        vent = m.bone(f"iron_vent_{k}", p, facing(normal))
        m.cube(vent, (p[0] - 3, p[1] - 1.5, p[2] - 1), (6, 3, 2))
        glow = m.bone(f"vent_{k}", p, parent=vent)
        m.cube(glow, (p[0] - 2, p[1] - 0.5, p[2] + 1), (4, 1, 1))
    for k in range(3):
        x, z = r.uniform(-26, 26), r.uniform(-26, 26)
        p = on_skin(boxes, (x, 0, z), (0, 1, 0), 0)
        if not p:
            continue
        plate = m.bone(f"plate_{k}", p)
        m.cube(plate, (x - 5, p[1] - 0.5, z - 5), (10, 1, 10))
        for c in range(4):
            m.cube(plate, (x - 4 + (c % 2) * 7, p[1] + 0.5, z - 4 + (c // 2) * 7), (1, 1, 1), "brass")
    for k, (x, z) in enumerate(((-30, 30), (30, -30), (30, 30))):
        p = on_skin(boxes, (x, 0, z), (0, 1, 0), 0)
        q = on_skin(boxes, (0, 0, z), (0, 1, 0), 0)
        if not p or not q:
            continue
        pipe = m.bone(f"pipe_{k}", p)
        top = max(p[1], q[1]) + 2
        m.cube(pipe, (x - 1, p[1] - 1, z - 1), (2, round(top - p[1]) + 2, 2))
        m.cube(pipe, (min(x, 0) - 1, top, z - 1), (round(abs(x)) + 2, 2, 2))
    for k, x in enumerate((-2.2, 0, 2.2)):
        nerve = m.bone(f"nerve_{k}", (x, 14, 0))
        m.cube(nerve, (x - 0.7, 14, -0.7), (1, 3, 1))
    m.write(1024)


# ---------------------------------------------------------------- the Time Capsule built into it

def capsule():
    m = Model("cortex_capsule")
    body = m.bone("cap", (0, 8, 0))
    m.cube(body, (-7, 0, -7), (14, 3, 14))
    m.cube(body, (-7, 13, -7), (14, 3, 14))
    for k in range(4):
        x, z = (-6, -6) if k == 0 else (5, -6) if k == 1 else (-6, 5) if k == 2 else (5, 5)
        m.cube(body, (x, 3, z), (1, 10, 1))
    for k, (rot, n) in enumerate(((0, "s"), (90, "e"), (180, "n"), (270, "w"))):
        g = m.bone("glass_" + n, (0, 8, 0), (0, rot, 0))
        m.cube(g, (-5, 3, 4.5), (10, 10, 1))
    core = m.bone("core", (0, 8, 0))
    m.cube(core, (-2, 5, -2), (4, 6, 4))
    for k in range(2):
        ring = m.bone(f"ring_{k}", (0, 7 + k * 3, 0))
        y = 7 + k * 3
        m.cube(ring, (-6, y, -6), (12, 1, 1))
        m.cube(ring, (-6, y, 5), (12, 1, 1))
        m.cube(ring, (-6, y, -5), (1, 1, 10))
        m.cube(ring, (5, y, -5), (1, 1, 10))
    collar = m.bone("collar", (0, 8, 8))
    m.cube(collar, (-9, -1, 7), (18, 2, 3))
    m.cube(collar, (-9, 15, 7), (18, 2, 3))
    m.cube(collar, (-9, 1, 7), (2, 14, 3))
    m.cube(collar, (7, 1, 7), (2, 14, 3))
    for k, (x, y) in enumerate(((-8, 0), (8, 0), (-8, 16), (8, 16))):
        m.cube(collar, (x - 0.5, y - 0.5, 10), (1, 1, 1))
    m.write(128)


if __name__ == "__main__":
    jar_brain(False)
    jar_brain(True)
    stage_2()
    stage_3()
    capsule()

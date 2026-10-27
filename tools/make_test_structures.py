"""
Writes the GameTest arena structures (NBT) to src/main/resources/data/chronogenesis/structure/.

  floor7: a 7x5x7 space with a stone floor at y=0. Tests place blocks and mobs from y=1 up.

Usage: python3 tools/make_test_structures.py
"""
import gzip
import os
import struct

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "src/main/resources/data/chronogenesis/structure")
DATA_VERSION = 3955  # Minecraft 1.21.1

TAG_END, TAG_INT, TAG_STRING, TAG_LIST, TAG_COMPOUND = 0, 3, 8, 9, 10


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


def structure(size, blocks, palette):
    root = {
        "DataVersion": (TAG_INT, DATA_VERSION),
        "size": ints(*size),
        "palette": (TAG_LIST, (TAG_COMPOUND, [{"Name": (TAG_STRING, p)} for p in palette])),
        "blocks": (TAG_LIST, (TAG_COMPOUND, [
            {"pos": ints(*pos), "state": (TAG_INT, state)} for pos, state in blocks])),
        "entities": (TAG_LIST, (TAG_COMPOUND, [])),
    }
    return struct.pack(">b", TAG_COMPOUND) + name("") + payload(TAG_COMPOUND, root)


def write(file_name, data):
    os.makedirs(OUT, exist_ok=True)
    with gzip.open(os.path.join(OUT, file_name), "wb") as f:
        f.write(data)


floor = [((x, 0, z), 0) for x in range(7) for z in range(7)]
write("floor7.nbt", structure((7, 5, 7), floor, ["minecraft:stone"]))
print("wrote floor7.nbt")

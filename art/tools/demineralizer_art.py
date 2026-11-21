import json
import os
import sys

from PIL import Image

from controller_texture import palette
from plant_style import BLOCK, BRASS, GLASS, ramp, source

NAME = "demineralizer"
M = "criticalmass:block/"
ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "criticalmass")
ITEM = os.path.join(ASSETS, "textures", "item")
STATES = ("none", "fresh", "used", "spent")
FACING = {"north": 0, "east": 90, "south": 180, "west": 270}
WINDOW = (4, 2, 11, 11)
BED = (5, 4, 10, 10)
PORT = ((6, 4), (7, 4), (8, 4), (9, 4), (5, 5), (10, 5), (4, 6), (11, 6), (4, 7), (11, 7), (4, 8), (11, 8), (4, 9), (11, 9), (5, 10), (10, 10), (6, 11), (7, 11), (8, 11), (9, 11))


def put(img, x, y, color):
    img.putpixel((x, y), tuple(color[:3]) + (255,))


def beads(img, x0, y0, x1, y1, tones, spent_rows=0, dark=None):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            pick = dark if dark is not None and y - y0 < spent_rows else tones
            put(img, x, y, pick[1] if (x + 2 * y) % 3 == 0 else pick[2])
            if (x * 3 + y * 5) % 7 == 0:
                put(img, x, y, pick[3])


def front(state):
    img = source("steel_block")
    amber = palette("amber")
    brown = palette("wood")
    x0, y0, x1, y1 = WINDOW
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            edge = x in (x0, x1) or y in (y0, y1)
            put(img, x, y, (BRASS[3] if x == x0 or y == y0 else BRASS[1]) if edge else GLASS[0])
    put(img, x0, y0, BRASS[4])
    put(img, x1, y1, BRASS[0])
    put(img, x0 + 1, y0 + 1, GLASS[1])
    put(img, x0 + 2, y0 + 1, GLASS[1])
    bx0, by0, bx1, by1 = BED
    if state == "fresh":
        beads(img, bx0, by0, bx1, by1, amber)
    elif state == "used":
        beads(img, bx0, by0, bx1, by1, amber, spent_rows=3, dark=brown)
    elif state == "spent":
        beads(img, bx0, by0, bx1, by1, brown)
    for x in range(6, 10):
        put(img, x, 13, BRASS[2])
    put(img, 6, 13, BRASS[3])
    put(img, 9, 13, BRASS[1])
    return img


def side():
    img = source("steel_block")
    dark = ramp(source("reactor_vessel_casing"))
    for x, y in PORT:
        put(img, x, y, dark[2])
    for y in range(5, 11):
        for x in range(5, 11):
            if (x, y) not in PORT and not (x in (5, 10) and y in (5, 10)):
                put(img, x, y, dark[0] if 6 <= x <= 9 and 6 <= y <= 9 else dark[1])
    return img


def top():
    img = source("steel_block")
    dark = ramp(source("reactor_vessel_casing"))
    for y in range(3, 13):
        for x in range(3, 13):
            edge_light = x == 3 or y == 3
            edge_dark = x == 12 or y == 12
            put(img, x, y, dark[5] if edge_light and not edge_dark else dark[0] if edge_dark and not edge_light else dark[3])
    for x in range(6, 10):
        put(img, x, 7, BRASS[3])
        put(img, x, 8, BRASS[1])
    put(img, 6, 7, BRASS[4])
    put(img, 9, 8, BRASS[0])
    return img


def cartridge(spent):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    steel = ramp(source("steel_block"))
    body = palette("wood") if spent else palette("amber")
    for y in range(2, 14):
        for x in range(5, 11):
            cap = y in (2, 3, 12, 13)
            if cap:
                tone = steel[5] if x == 5 else steel[2] if x == 10 else steel[4]
                if y in (3, 13):
                    tone = steel[3] if 5 < x < 10 else tone
            else:
                tone = body[3] if x == 6 else body[1] if x == 10 else body[2]
                if x == 5:
                    tone = body[2]
                if (x + y) % 3 == 0 and 6 < x < 10:
                    tone = body[1]
            put(img, x, y, tone)
    for x in range(5, 11):
        put(img, x, 7, steel[3] if x < 10 else steel[1])
    if spent:
        hazard = palette("hazard")
        for x in range(5, 11):
            put(img, x, 9, hazard[2] if x % 2 else (20, 22, 26))
    for x in range(6, 10):
        put(img, x, 1, steel[4])
        put(img, x, 14, steel[1])
    return img


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=1)
        f.write("\n")


def models():
    block = os.path.join(ASSETS, "models", "block")
    for state in STATES:
        write(os.path.join(block, "%s_%s.json" % (NAME, state)), {"parent": "minecraft:block/orientable_with_bottom", "textures": {
            "top": M + NAME + "_top", "bottom": "criticalmass:block/steel_block", "side": M + NAME + "_side", "front": "%s%s_front_%s" % (M, NAME, state)}})
    write(os.path.join(ASSETS, "models", "item", NAME + ".json"), {"parent": M + NAME + "_none"})
    for item in ("resin_cartridge", "used_resin_cartridge"):
        write(os.path.join(ASSETS, "models", "item", item + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": "criticalmass:item/" + item}})
    variants = {}
    for facing, turn in FACING.items():
        for state in STATES:
            variants["facing=%s,resin=%s" % (facing, state)] = dict({"model": "%s%s_%s" % (M, NAME, state)}, **({"y": turn} if turn else {}))
    write(os.path.join(ASSETS, "blockstates", NAME + ".json"), {"variants": variants})


def main():
    models()
    targets = {os.path.join(BLOCK, NAME + "_side.png"): side, os.path.join(BLOCK, NAME + "_top.png"): top,
               os.path.join(ITEM, "resin_cartridge.png"): lambda: cartridge(False), os.path.join(ITEM, "used_resin_cartridge.png"): lambda: cartridge(True)}
    for state in STATES:
        targets[os.path.join(BLOCK, "%s_front_%s.png" % (NAME, state))] = lambda state=state: front(state)
    for path, paint in targets.items():
        if os.path.exists(path) and "fresh" not in sys.argv:
            print("left alone; pass fresh to repaint", os.path.basename(path))
            continue
        paint().save(path)
        print("painted", os.path.basename(path))
    if len(sys.argv) > 1 and sys.argv[-1].endswith(".png"):
        tiles = [front(s) for s in STATES] + [side(), top(), cartridge(False), cartridge(True)]
        sheet = Image.new("RGBA", (len(tiles) * 136 + 8, 144), (88, 110, 74, 255))
        for i, tile in enumerate(tiles):
            sheet.alpha_composite(tile.resize((128, 128), Image.NEAREST), (8 + i * 136, 8))
        sheet.save(sys.argv[-1])


if __name__ == "__main__":
    main()

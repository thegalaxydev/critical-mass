import os
import shutil
import sys
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ORIGINALS = os.path.join(ROOT, "art", "originals")
TEXTURES = os.path.join(ROOT, "src", "main", "resources", "assets", "criticalmass", "textures", "block")
SINGLE = "shielding_concrete.png"
SHEET = "shielding_concrete_connected.png"
BANDS = [4, 4, 4, 3, 3, 3, 3, 2, 2, 2, 3, 3, 3, 4, 3, 3]
DRIFT = [0, 0, 0, 1, 1, 1, 2, 2, 1, 1, 0, 0, 15, 15, 15, 0]
FLECKS = {(6, 9): 1, (7, 8): 1, (12, 4): 2, (3, 12): 2, (10, 12): 4, (4, 5): 4}
NAMES = ("up", "down", "left", "right", "top_left", "top_right", "bottom_left", "bottom_right")


def ramp(single):
    return sorted(set(single.getdata()), key=lambda c: c[0] + c[1] + c[2])


def field(tones):
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            tone = BANDS[(x + y + DRIFT[(x - y) % 16]) % 16]
            img.putpixel((x, y), tones[FLECKS.get((x, y), tone)])
    return img


def contexts():
    for bits in range(256):
        c = {n: bool(bits >> i & 1) for i, n in enumerate(NAMES)}
        c["top_left"] = c["top_left"] and c["up"] and c["left"]
        c["top_right"] = c["top_right"] and c["up"] and c["right"]
        c["bottom_left"] = c["bottom_left"] and c["down"] and c["left"]
        c["bottom_right"] = c["bottom_right"] and c["down"] and c["right"]
        yield c


def index(c):
    x = (1 if c["up"] else 0) + (2 if c["down"] else 0)
    y = (1 if c["left"] else 0) + (2 if c["right"] else 0)
    borders = sum(1 for n in ("up", "down", "left", "right") if not c[n])
    if borders == 0:
        x += (1 if c["top_right"] else 0) + (2 if c["top_left"] else 0)
        y += (2 if c["bottom_right"] else 0) + (1 if c["bottom_left"] else 0)
    if borders == 1:
        if not c["right"] and (c["top_left"] or c["bottom_left"]):
            y = 4
            x = -1 + (1 if c["bottom_left"] else 0) + (2 if c["top_left"] else 0)
        if not c["left"] and (c["top_right"] or c["bottom_right"]):
            y = 5
            x = -1 + (1 if c["bottom_right"] else 0) + (2 if c["top_right"] else 0)
        if not c["down"] and (c["top_left"] or c["top_right"]):
            y = 6
            x = -1 + (1 if c["top_left"] else 0) + (2 if c["top_right"] else 0)
        if not c["up"] and (c["bottom_left"] or c["bottom_right"]):
            y = 7
            x = -1 + (1 if c["bottom_left"] else 0) + (2 if c["bottom_right"] else 0)
    if borders == 2 and (c["top_left"] or c["bottom_left"] or c["top_right"] or c["bottom_right"]):
        x += 3
    return x, y


def tile(c, single, slab):
    tones = ramp(single)
    light, dark, mixed, hole, lip = single.getpixel((8, 0)), single.getpixel((8, 15)), single.getpixel((15, 0)), tones[0], tones[4]
    img = slab.copy()
    put = img.putpixel
    put((1, 1), hole)
    put((2, 1), lip)
    put((1, 2), lip)
    if not (c["right"] and c["down"] and c["bottom_right"]):
        put((14, 14), hole)
        put((13, 14), lip)
        put((14, 13), lip)
    up, down, left, right = c["up"], c["down"], c["left"], c["right"]
    for i in range(16):
        if not up:
            put((i, 0), light)
        if not left:
            put((0, i), light)
        if not down:
            put((i, 15), dark)
        if not right:
            put((15, i), dark)
    if not up and not left:
        put((0, 0), single.getpixel((0, 0)))
    elif up and left and not c["top_left"]:
        put((0, 0), light)
    if up != right:
        put((15, 0), dark if up else light)
    elif not up or not c["top_right"]:
        put((15, 0), mixed)
    if down != left:
        put((0, 15), light if down else dark)
    elif not down or not c["bottom_left"]:
        put((0, 15), mixed)
    if down and right and not c["bottom_right"]:
        put((15, 15), dark)
    return img


def sheet(single):
    slab = field(ramp(single))
    cells = {}
    for c in contexts():
        at = index(c)
        made = tile(c, single, slab)
        if at in cells and list(cells[at].getdata()) != list(made.getdata()):
            raise SystemExit("two different tiles want cell %s" % (at,))
        cells[at] = made
    cells[(0, 0)] = single
    img = Image.new("RGBA", (128, 128))
    for (x, y), made in cells.items():
        img.paste(made, (x * 16, y * 16))
    return img, len(cells)


def main(fresh):
    single = Image.open(os.path.join(ORIGINALS, SINGLE)).convert("RGBA")
    kept = os.path.join(ORIGINALS, SHEET)
    if fresh or not os.path.exists(kept):
        img, count = sheet(single)
        img.save(kept)
        print("drew", SHEET, "with", count, "tiles")
    shutil.copyfile(kept, os.path.join(TEXTURES, SHEET))
    print("installed", SHEET)


if __name__ == "__main__":
    main("fresh" in sys.argv[1:])

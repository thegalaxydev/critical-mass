import os
import shutil
import sys
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ORIGINALS = os.path.join(ROOT, "art", "originals")
TEXTURES = os.path.join(ROOT, "src", "main", "resources", "assets", "criticalmass", "textures", "block")
SINGLE = "shielding_concrete.png"
SHEET = "shielding_concrete_connected.png"
NAMES = ("up", "down", "left", "right", "top_left", "top_right", "bottom_left", "bottom_right")


def between(tones, a, b, part):
    i, j = tones.index(a), tones.index(b)
    return tones[int(round(i + (j - i) * part / 3.0))]


def slab(single):
    tones = sorted(set(single.getdata()), key=lambda c: c[0] + c[1] + c[2])
    img = single.copy()
    for x in range(1, 15):
        low, high = single.getpixel((x, 14)), single.getpixel((x, 1))
        img.putpixel((x, 15), between(tones, low, high, 1))
        img.putpixel((x, 0), between(tones, low, high, 2))
    for y in range(16):
        low, high = img.getpixel((14, y)), img.getpixel((1, y))
        img.putpixel((15, y), between(tones, low, high, 1))
        img.putpixel((0, y), between(tones, low, high, 2))
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


def corner(img, single, at, across, along, inner, missing):
    x, y = at
    side = (x, 1 if y == 0 else 14)
    level = (1 if x == 0 else 14, y)
    if not across and not along:
        img.putpixel(at, single.getpixel(at))
    elif not across:
        img.putpixel(at, single.getpixel(level))
    elif not along:
        img.putpixel(at, single.getpixel(side))
    elif missing:
        img.putpixel(at, single.getpixel(inner))


def tile(c, single, base):
    img = base.copy()
    up, down, left, right = c["up"], c["down"], c["left"], c["right"]
    for i in range(1, 15):
        if not up:
            img.putpixel((i, 0), single.getpixel((i, 0)))
        if not down:
            img.putpixel((i, 15), single.getpixel((i, 15)))
        if not left:
            img.putpixel((0, i), single.getpixel((0, i)))
        if not right:
            img.putpixel((15, i), single.getpixel((15, i)))
    corner(img, single, (0, 0), up, left, (0, 1), not c["top_left"])
    corner(img, single, (15, 0), up, right, (15, 0), not c["top_right"])
    corner(img, single, (0, 15), down, left, (0, 15), not c["bottom_left"])
    corner(img, single, (15, 15), down, right, (15, 15), not c["bottom_right"])
    return img


def sheet(single):
    base = slab(single)
    cells = {}
    for c in contexts():
        at = index(c)
        made = tile(c, single, base)
        if at in cells and list(cells[at].getdata()) != list(made.getdata()):
            raise SystemExit("two different tiles want cell %s" % (at,))
        cells[at] = made
    if list(cells[(0, 0)].getdata()) != list(single.getdata()):
        raise SystemExit("the lone tile does not match the single texture")
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

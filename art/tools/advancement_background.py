import os
import sys

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
TEXTURES = os.path.join(ROOT, "src", "main", "resources", "assets", "criticalmass", "textures")
SHEET = os.path.join(TEXTURES, "block", "reactor_vessel_casing_connected.png")
OUT = os.path.join(TEXTURES, "gui", "advancement_background.png")
TILE = 16


def joined():
    sheet = Image.open(SHEET).convert("RGBA")
    found = []
    for ty in range(sheet.height // TILE):
        for tx in range(sheet.width // TILE):
            tile = sheet.crop((tx * TILE, ty * TILE, tx * TILE + TILE, ty * TILE + TILE))
            pixels = [tile.getpixel((x, y)) for y in range(TILE) for x in range(TILE)]
            if all(p[3] == 255 for p in pixels) and not any(p[0] + p[1] + p[2] > 330 for p in pixels):
                found.append(tile)
    assert len(found) == 1, "the sheet should have one tile joined on every side, with no rim or rivet: found %d" % len(found)
    return found[0]


def main():
    if "fresh" not in sys.argv:
        print("left alone; pass fresh to cut the advancement background again from the connected casing sheet")
        return
    joined().save(OUT)
    print("cut", os.path.relpath(OUT, ROOT))


if __name__ == "__main__":
    main()

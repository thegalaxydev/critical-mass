import json
import os
import sys

from PIL import Image

from plant_style import BLOCK

COOLING = (0.0, 0.4, 0.8, 0.4)
FRAME_TIME = 14
BRIGHT = 200
COLDEST = (190, 40, 24)


def veins():
    cold = Image.open(os.path.join(BLOCK, "corium.png")).convert("RGBA")
    hot = Image.open(os.path.join(BLOCK, "corium_hot.png")).convert("RGBA")
    assert cold.size == hot.size and hot.width == hot.height, "corium and corium_hot should be the same square size"
    lit = {}
    for y in range(hot.height):
        for x in range(hot.width):
            c = hot.getpixel((x, y))
            if c != cold.getpixel((x, y)) and max(c[:3]) >= BRIGHT:
                lit[x, y] = c[:3]
    return hot.width, lit


def cooler(colour, tones):
    at = tones.index(colour)
    return tones[at + 1] if at + 1 < len(tones) else COLDEST


def glow():
    size, lit = veins()
    tones = sorted(set(lit.values()), key=lambda c: -(c[0] + c[1] + c[2]))
    sheet = Image.new("RGBA", (size, size * len(COOLING)))
    for i, share in enumerate(COOLING):
        for (x, y), c in lit.items():
            to = cooler(c, tones)
            sheet.putpixel((x, y + size * i), tuple(round(c[k] + (to[k] - c[k]) * share) for k in range(3)) + (255,))
    return sheet, len(lit)


def main():
    if "fresh" not in sys.argv:
        print("left alone; pass fresh to make the corium glow again from the author's corium and corium_hot sheets")
        return
    sheet, count = glow()
    sheet.save(os.path.join(BLOCK, "corium_glow.png"))
    with open(os.path.join(BLOCK, "corium_glow.png.mcmeta"), "w", encoding="utf8", newline="\n") as f:
        json.dump({"animation": {"frametime": FRAME_TIME, "interpolate": True}}, f, indent=1)
        f.write("\n")
    print("made the corium glow from", count, "lit pixels")


if __name__ == "__main__":
    main()

import json
import os
import sys

from PIL import Image

from plant_style import BLOCK, CRUST, EMBER

CRACKS = (
    "......#.........",
    "......#.........",
    ".....#..........",
    ".....#..........",
    "....##..........",
    "+++.#.....++++++",
    "...+#....+......",
    "....#...+.......",
    "....#++++.......",
    ".....#..........",
    ".....#..........",
    "......#.........",
    "......#....-....",
    ".......-----....",
    "......#.........",
    "......#.........",
)
PULSE = (1.0, 0.86, 0.72, 0.86)
FRAME_TIME = 14
HOT = {"#": 2, "+": 1, "-": 0}
LIT = {"#": 4, "+": 2}


def crack(x, y):
    return CRACKS[y % 16][x % 16]


def split(x, y):
    return crack(x, y) != "."


def crust(hot):
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            kind = crack(x, y)
            if kind != ".":
                c = EMBER[HOT[kind]] if hot else CRUST[0]
            elif split(x - 1, y) or split(x, y - 1):
                c = CRUST[3]
            elif split(x + 1, y) or split(x, y + 1):
                c = CRUST[1]
            else:
                c = CRUST[2]
            img.putpixel((x, y), c + (255,))
    return img


def glow():
    sheet = Image.new("RGBA", (16, 16 * len(PULSE)))
    for i, level in enumerate(PULSE):
        for y in range(16):
            for x in range(16):
                kind = crack(x, y)
                if kind in LIT:
                    sheet.putpixel((x, y + 16 * i), tuple(round(v * level) for v in EMBER[LIT[kind]]) + (255,))
    return sheet


def main():
    cold = os.path.join(BLOCK, "corium.png")
    if "fresh" not in sys.argv and os.path.exists(os.path.join(BLOCK, "corium_glow.png")):
        print("left alone; pass fresh to repaint the corium textures")
        return
    crust(False).save(cold)
    crust(True).save(os.path.join(BLOCK, "corium_hot.png"))
    glow().save(os.path.join(BLOCK, "corium_glow.png"))
    with open(os.path.join(BLOCK, "corium_glow.png.mcmeta"), "w", encoding="utf8", newline="\n") as f:
        json.dump({"animation": {"frametime": FRAME_TIME, "interpolate": True}}, f, indent=1)
        f.write("\n")
    print("painted the corium textures")


if __name__ == "__main__":
    main()

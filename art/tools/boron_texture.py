import colorsys
import os
import shutil
import sys

from PIL import Image

from plant_style import BLOCK

TEXTURES = os.path.dirname(BLOCK)
ITEM = os.path.join(TEXTURES, "item")
FLUID = os.path.join(TEXTURES, "fluid")
BORAX = {"d": (92, 104, 112), "s": (150, 166, 172), "m": (198, 212, 214), "l": (228, 238, 238), "h": (250, 253, 252)}
BORAX_PIXELS = (
    "................",
    "................",
    "................",
    "......dd........",
    ".....dhld.......",
    ".....dlmsd.dd...",
    "....dhlmsddhld..",
    "...ddlmmsdlmmsd.",
    "..dhldmsdhlmmsd.",
    ".dhlmsddlmmmssd.",
    ".dlmmmsdlmmsssd.",
    ".dmmmssdmmssssd.",
    ".dsssssdssssssd.",
    "..dddddddddddd..",
    "................",
    "................",
)
ACID_HUE = 0.44
ACID_SATURATION = 0.32
ACID_LIFT = 0.42


def draw(rows, tones):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, key in enumerate(row):
            if key != ".":
                img.putpixel((x, y), tones[key] + (255,))
    return img


def acid(name):
    src = Image.open(os.path.join(FLUID, "primary_coolant_" + name + ".png")).convert("RGBA")
    out = src.copy()
    for x in range(src.width):
        for y in range(src.height):
            r, g, b, a = src.getpixel((x, y))
            if a == 0:
                continue
            _, s, v = colorsys.rgb_to_hsv(r / 255.0, g / 255.0, b / 255.0)
            v = v + (1 - v) * ACID_LIFT
            r2, g2, b2 = colorsys.hsv_to_rgb(ACID_HUE, min(1.0, s * ACID_SATURATION), v)
            out.putpixel((x, y), (int(r2 * 255), int(g2 * 255), int(b2 * 255), min(a, 190)))
    out.save(os.path.join(FLUID, "boric_acid_" + name + ".png"))
    shutil.copyfile(os.path.join(FLUID, "primary_coolant_" + name + ".png.mcmeta"), os.path.join(FLUID, "boric_acid_" + name + ".png.mcmeta"))


def main():
    if "fresh" not in sys.argv:
        print("left alone; pass fresh to repaint the Borax item and the Boric Acid fluid")
        return
    art = {"borax": lambda: draw(BORAX_PIXELS, BORAX).save(os.path.join(ITEM, "borax.png")),
           "boric_acid": lambda: (acid("still"), acid("flow"))}
    wanted = [name for name in sys.argv[1:] if name in art]
    for name, paint in art.items():
        if not wanted or name in wanted:
            paint()
            print("painted", name)


if __name__ == "__main__":
    main()

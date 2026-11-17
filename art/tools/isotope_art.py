import os
import sys

from PIL import Image

from controller_texture import palette
from plant_style import BLOCK, ramp

ITEM = os.path.join(os.path.dirname(BLOCK), "item")
ASH = {"d": (48, 55, 44), "m": (74, 84, 64), "l": (104, 117, 86), "g": (150, 232, 92)}
ASH_PIXELS = (
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    ".......dd.......",
    ".....ddlmd......",
    "....dlmmmmdd....",
    "...dlmmgmmmmd...",
    "..dlmmmmmlmmmd..",
    ".dlmgmmmmmmmgmd.",
    ".dmmmmmlmmgmmmd.",
    ".dddddddddddddd.",
    "................",
    "................",
)
METAL = {"d": (62, 60, 46), "s": (112, 108, 82), "m": (166, 162, 126), "l": (208, 205, 168), "h": (240, 238, 212)}
METAL_PIXELS = (
    "................",
    "................",
    "................",
    "................",
    ".....dddddd.....",
    "....dllllhhd....",
    "...dlmmmmmlhd...",
    "...dmmmmmmmld...",
    "...dmmmmmmmmd...",
    "...dsmmmmmmsd...",
    "...dssmmmmssd...",
    "....dssssssd....",
    ".....dddddd.....",
    "................",
    "................",
    "................",
)
SHEARED = {"d": (46, 50, 56), "s": (104, 110, 118), "m": (150, 156, 164), "l": (204, 208, 214), "f": (64, 82, 58), "g": (132, 206, 96)}
SHEARED_PIXELS = (
    "................",
    "................",
    "...dd...........",
    "..dlmd....dd....",
    "..dmmsd..dlmd...",
    "...dsfd..dmmsd..",
    "....dd....dsgd..",
    ".....dd....dd...",
    "....dlmd........",
    "....dmmsd..dd...",
    ".....dsgd.dlmd..",
    "......dd..dmmsd.",
    "...........dsfd.",
    "............dd..",
    "................",
    "................",
)
CELL_PIXELS = (
    "................",
    "......cccc......",
    ".....cCCCCc.....",
    ".....dlaaad.....",
    ".....dlaaad.....",
    ".....dgGGgd.....",
    ".....dGHHGd.....",
    ".....dGHHGd.....",
    ".....dgGGgd.....",
    ".....dlaaad.....",
    ".....dlaaad.....",
    ".....dlaaad.....",
    ".....cCCCCc.....",
    "......cccc......",
    "................",
    "................",
)


def draw(pixels, tones):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(pixels):
        assert len(row) == 16, row
        for x, key in enumerate(row):
            if key != ".":
                img.putpixel((x, y), tones[key] + (255,))
    return img


def cell_tones():
    alloy = ramp(Image.open(os.path.join(ITEM, "reactor_alloy_ingot.png")).convert("RGBA"))
    copper, green = palette("copper"), palette("green")
    return {"c": copper[2], "C": copper[4], "d": alloy[1], "a": alloy[3], "l": alloy[5], "g": green[2], "G": green[3], "H": green[4]}


def marker():
    copper, green = palette("copper"), palette("green")
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(3):
        for x in range(3):
            img.putpixel((x, y), (green[4] if (x, y) == (1, 1) else green[3]) + (255,))
    for x in range(3):
        img.putpixel((x, 3), copper[4] + (255,))
    return img


def main():
    if "fresh" not in sys.argv:
        print("left alone; pass fresh to repaint the fallout ash, strontium, sheared fuel and cell art")
        return
    art = {"fallout_ash": lambda: draw(ASH_PIXELS, ASH).save(os.path.join(ITEM, "fallout_ash.png")),
           "strontium_90": lambda: draw(METAL_PIXELS, METAL).save(os.path.join(ITEM, "strontium_90.png")),
           "sheared_fuel": lambda: draw(SHEARED_PIXELS, SHEARED).save(os.path.join(ITEM, "sheared_fuel.png")),
           "radioisotope_cell": lambda: draw(CELL_PIXELS, cell_tones()).save(os.path.join(ITEM, "radioisotope_cell.png")),
           "station_battery_cell": lambda: marker().save(os.path.join(BLOCK, "station_battery_cell.png"))}
    wanted = [name for name in sys.argv[1:] if name in art]
    for name, paint in art.items():
        if not wanted or name in wanted:
            paint()
            print("painted", name)


if __name__ == "__main__":
    main()

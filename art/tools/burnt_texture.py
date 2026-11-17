import os
import sys

from PIL import Image

from controller_texture import palette
from plant_style import BLOCK, source

HOLE = (16, 14, 14, 255)
NOTCHES = ((3, 6), (12, 5), (12, 9), (5, 11), (9, 11), (7, 3))
STREAKS = ((5, 12, 3), (10, 12, 2), (8, 0, 2))


def shade(pixel, factor):
    return (int(pixel[0] * factor), int(pixel[1] * factor), int(pixel[2] * factor), 255)


def side():
    steel = source("steel_block")
    rust = palette("copper")
    out = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            out.putpixel((x, y), shade(steel.getpixel((x, y)), 0.26 + 0.16 * y / 15))
    for y in range(3, 12):
        for x in range(3, 13):
            edge = x in (3, 12) or y in (3, 11)
            out.putpixel((x, y), shade(steel.getpixel((x, y)), 0.16) if edge else HOLE)
    for x, y in NOTCHES:
        out.putpixel((x, y), HOLE)
    for x, y in ((4, 4), (11, 4), (4, 10), (11, 10)):
        out.putpixel((x, y), tuple(int(v * 0.7) for v in rust[1]) + (255,))
    for x, y, length in STREAKS:
        for i in range(length):
            out.putpixel((x, y + i), shade(steel.getpixel((x, y + i)), 0.14))
    return out


def top():
    steel = source("steel_block")
    rust = palette("copper")
    out = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            out.putpixel((x, y), shade(steel.getpixel((x, y)), 0.3))
    for y in range(5, 11):
        for x in range(5, 11):
            ring = x in (5, 10) or y in (5, 10)
            out.putpixel((x, y), tuple(int(v * 0.6) for v in rust[1]) + (255,) if ring else HOLE)
    return out


def bottom():
    steel = source("steel_block")
    out = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            out.putpixel((x, y), shade(steel.getpixel((x, y)), 0.3))
    return out


def main():
    if "fresh" not in sys.argv:
        print("left alone; pass fresh to repaint the burnt-out equipment")
        return
    side().save(os.path.join(BLOCK, "burnt_equipment.png"))
    top().save(os.path.join(BLOCK, "burnt_equipment_top.png"))
    bottom().save(os.path.join(BLOCK, "burnt_equipment_bottom.png"))
    print("painted the burnt-out equipment")


if __name__ == "__main__":
    main()

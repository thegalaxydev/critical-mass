import os
import sys

from controller_texture import palette
from plant_style import BLOCK, GLASS, Sheet, nine, ramp, source

ISLANDS = {
    "base_side": (0, 0, 16, 12),
    "base_top": (16, 0, 16, 16),
    "base_bottom": (0, 12, 16, 16),
    "swivel_side": (16, 16, 8, 2),
    "swivel_top": (16, 18, 8, 8),
    "body_side": (24, 16, 6, 4),
    "body_top": (24, 20, 6, 6),
    "barrel_side": (16, 26, 8, 3),
    "barrel_end": (24, 26, 3, 3),
    "nozzle_side": (27, 26, 2, 2),
    "nozzle_end": (29, 26, 2, 2),
}
HOOPS = (1, 6)


def collar(sheet, island, x0, y0, x1, y1, tones, hole):
    sheet.outline(island, x0, y0, x1, y1, tones[4])
    for i in range(x0, x1 + 1):
        sheet.put(island, i, y0, tones[5])
        sheet.put(island, i, y1, tones[3])
    for i in range(y0, y1 + 1):
        sheet.put(island, x0, i, tones[5])
        sheet.put(island, x1, i, tones[3])
    sheet.fill(island, x0 + 1, y0 + 1, x1 - 1, y1 - 1, hole)


def base(sheet, steel, tone, brass, copper):
    sheet.paste("base_top", steel)
    collar(sheet, "base_top", 3, 3, 12, 12, brass, tone[1])
    sheet.paste("base_bottom", steel)
    collar(sheet, "base_bottom", 5, 5, 10, 10, brass, GLASS[0])
    sheet.paste("base_side", nine(steel, 16, 12))
    collar(sheet, "base_side", 5, 1, 10, 6, copper, GLASS[0])
    for x in range(6, 10):
        sheet.put("base_side", x, 2, GLASS[1])


def head(sheet, brass, copper):
    for x in range(8):
        end = x in (0, 7)
        sheet.put("swivel_side", x, 0, brass[4] if end else brass[5])
        sheet.put("swivel_side", x, 1, brass[2] if end else brass[3])
    sheet.fill("swivel_top", 0, 0, 7, 7, brass[4])
    for i in range(8):
        sheet.put("swivel_top", i, 0, brass[5])
        sheet.put("swivel_top", 0, i, brass[5])
    for i in range(8):
        sheet.put("swivel_top", i, 7, brass[3])
        sheet.put("swivel_top", 7, i, brass[3])
    rows = (copper[5], copper[4], copper[4], copper[2])
    for y, c in enumerate(rows):
        for x in range(6):
            sheet.put("body_side", x, y, copper[3] if x in (0, 5) and y in (1, 2) else c)
    sheet.fill("body_top", 0, 0, 5, 5, copper[4])
    for i in range(6):
        sheet.put("body_top", i, 0, copper[5])
        sheet.put("body_top", 0, i, copper[5])
    for i in range(6):
        sheet.put("body_top", i, 5, copper[2])
        sheet.put("body_top", 5, i, copper[2])
    sheet.fill("body_top", 2, 2, 3, 3, brass[4])
    sheet.put("body_top", 2, 2, brass[5])
    sheet.put("body_top", 3, 3, brass[3])
    for x in range(8):
        hoop = x in HOOPS
        sheet.put("barrel_side", x, 0, brass[5] if hoop else copper[5])
        sheet.put("barrel_side", x, 1, brass[4] if hoop else copper[4])
        sheet.put("barrel_side", x, 2, brass[3] if hoop else copper[2])
    sheet.fill("barrel_end", 0, 0, 2, 2, copper[3])
    sheet.put("barrel_end", 0, 0, copper[5])
    sheet.put("barrel_end", 1, 1, copper[1])
    for x, y, c in ((0, 0, brass[5]), (1, 0, brass[4]), (0, 1, brass[4]), (1, 1, brass[3])):
        sheet.put("nozzle_side", x, y, c)
    for x, y, c in ((0, 0, brass[4]), (1, 0, brass[3]), (0, 1, brass[3]), (1, 1, GLASS[0])):
        sheet.put("nozzle_end", x, y, c)


def cannon():
    steel = source("steel_block")
    sheet = Sheet(32, ISLANDS)
    base(sheet, steel, ramp(steel), palette("brass"), palette("copper"))
    head(sheet, palette("brass"), palette("copper"))
    return sheet


def main():
    if "fresh" not in sys.argv:
        print("left alone; pass fresh to repaint the water cannon sheet")
        return
    cannon().save(os.path.join(BLOCK, "water_cannon.png"))
    print("painted the water cannon sheet")


if __name__ == "__main__":
    main()

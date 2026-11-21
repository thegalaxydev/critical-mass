import os
import sys

from controller_texture import palette
from create_style import BRASS, CASING, COPPER, FUEL_YELLOW
from plant_style import BLOCK, Sheet

NAME = "area_radiation_monitor"
SIZE = 32
ISLANDS = {
    "front": (0, 0, 10, 11),
    "side": (10, 0, 4, 11),
    "top": (14, 0, 10, 4),
    "bottom": (14, 4, 10, 4),
    "dial": (0, 11, 8, 5),
    "rim_long": (8, 11, 8, 1),
    "rim_short": (8, 12, 1, 5),
    "lamp": (14, 8, 2, 2),
    "lamp_lit": (16, 8, 2, 2),
    "tube": (0, 16, 8, 2),
    "tube_end": (8, 17, 2, 2),
    "needle": (10, 12, 1, 3),
}
FACE = ((142, 166, 124), (124, 150, 110), (166, 186, 146))
INK = (20, 22, 26)
TICK = (84, 108, 82)


def shell(sheet, island):
    _, _, w, h = ISLANDS[island]
    sheet.fill(island, 0, 0, w - 1, h - 1, CASING[3])
    for x in range(w):
        sheet.put(island, x, 0, CASING[5])
        sheet.put(island, x, h - 1, CASING[1])
    for y in range(h):
        sheet.put(island, 0, y, CASING[5])
        sheet.put(island, w - 1, y, CASING[1])
    sheet.put(island, 0, 0, CASING[6])
    sheet.put(island, w - 1, h - 1, CASING[0])
    sheet.put(island, w - 1, 0, CASING[3])
    sheet.put(island, 0, h - 1, CASING[3])


def front(sheet):
    shell(sheet, "front")
    sheet.fill("front", 1, 1, 8, 9, FUEL_YELLOW[1])
    for x in range(1, 9):
        sheet.put("front", x, 6, FUEL_YELLOW[2])
        sheet.put("front", x, 9, FUEL_YELLOW[0])
    for y in (7, 8):
        for x in range(2, 8):
            sheet.put("front", x, y, CASING[0] if x % 2 == 0 else CASING[2])


def dial(sheet):
    red = palette("red")
    sheet.outline("dial", 0, 0, 7, 4, BRASS[6])
    for x in range(8):
        sheet.put("dial", x, 0, BRASS[9])
        sheet.put("dial", x, 4, BRASS[3])
    for y in range(5):
        sheet.put("dial", 0, y, BRASS[9])
        sheet.put("dial", 7, y, BRASS[3])
    sheet.put("dial", 0, 0, BRASS[11])
    sheet.put("dial", 7, 4, BRASS[1])
    sheet.fill("dial", 1, 1, 6, 3, FACE[0])
    for x in range(1, 7):
        sheet.put("dial", x, 1, FACE[2] if x % 2 == 0 else TICK)
        sheet.put("dial", x, 3, FACE[1])
    sheet.put("dial", 5, 1, red[2])
    sheet.put("dial", 6, 1, red[3])
    for x in range(8):
        sheet.put("rim_long", x, 0, BRASS[8] if x < 7 else BRASS[3])
    for y in range(5):
        sheet.put("rim_short", 0, y, BRASS[4] if y < 4 else BRASS[1])


def lamps(sheet):
    red = palette("red")
    for x, y, c in ((0, 0, red[1]), (1, 0, red[0]), (0, 1, red[0]), (1, 1, red[0])):
        sheet.put("lamp", x, y, c)
    for x, y, c in ((0, 0, red[4]), (1, 0, red[3]), (0, 1, red[3]), (1, 1, red[2])):
        sheet.put("lamp_lit", x, y, c)


def tube(sheet):
    for x in range(8):
        cap = x in (0, 7)
        band = x in (2, 5)
        sheet.put("tube", x, 0, BRASS[9] if cap else COPPER[9] if band else COPPER[7])
        sheet.put("tube", x, 1, BRASS[4] if cap else COPPER[4] if band else COPPER[2])
    for x, y, c in ((0, 0, BRASS[9]), (1, 0, BRASS[6]), (0, 1, BRASS[6]), (1, 1, BRASS[3])):
        sheet.put("tube_end", x, y, c)


def needle(sheet):
    red = palette("red")
    sheet.put("needle", 0, 0, red[2])
    sheet.put("needle", 0, 1, INK)
    sheet.put("needle", 0, 2, INK)


def monitor():
    sheet = Sheet(SIZE, ISLANDS)
    front(sheet)
    for island in ("side", "top", "bottom"):
        shell(sheet, island)
    dial(sheet)
    lamps(sheet)
    tube(sheet)
    needle(sheet)
    return sheet


def main():
    out = os.path.join(BLOCK, NAME + ".png")
    if "fresh" not in sys.argv and os.path.exists(out):
        print("left alone; pass fresh to repaint the area monitor sheet")
        return
    monitor().save(out)
    print("painted the area monitor sheet")


if __name__ == "__main__":
    main()

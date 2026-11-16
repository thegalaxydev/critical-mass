import os
import sys

from controller_texture import palette, plate
from plant_style import BLOCK, BRASS, GLASS, Sheet, source

TRACK = (8, 9)
ISLANDS = {
    "panel": (0, 0, 12, 12),
    "wall_top": (12, 0, 16, 2),
    "wall_bottom": (12, 2, 16, 2),
    "wall_side": (28, 4, 2, 12),
    "bezel": (0, 12, 6, 6),
    "edge_top": (6, 12, 6, 1),
    "edge_bottom": (6, 13, 6, 1),
    "edge_inner": (6, 14, 6, 1),
    "edge_left": (6, 15, 1, 4),
    "edge_right": (7, 15, 1, 4),
    "edge_inner_v": (8, 15, 1, 4),
    "lens": (12, 4, 4, 4),
    "lens_lit": (16, 4, 4, 4),
    "tab": (20, 4, 2, 2),
}


def track(sheet):
    for y in TRACK:
        for x in range(12):
            sheet.put("panel", x, y, GLASS[x // 2 % 2])


def bezel(sheet):
    sheet.outline("bezel", 0, 0, 5, 5, BRASS[2])
    for i in range(6):
        sheet.put("bezel", i, 0, BRASS[3])
        sheet.put("bezel", 0, i, BRASS[3])
    for i in range(1, 6):
        sheet.put("bezel", i, 5, BRASS[1])
        sheet.put("bezel", 5, i, BRASS[1])
    sheet.put("bezel", 0, 0, BRASS[4])
    sheet.put("bezel", 5, 5, BRASS[0])
    sheet.fill("bezel", 1, 1, 4, 4, GLASS[0])
    for x in range(6):
        sheet.put("edge_top", x, 0, BRASS[3])
        sheet.put("edge_bottom", x, 0, BRASS[0])
        sheet.put("edge_inner", x, 0, BRASS[0])
    for y in range(4):
        sheet.put("edge_left", 0, y, BRASS[2])
        sheet.put("edge_right", 0, y, BRASS[0])
        sheet.put("edge_inner_v", 0, y, BRASS[0])


def lens(sheet):
    red = palette("red")
    sheet.fill("lens", 0, 0, 3, 3, red[0])
    sheet.put("lens", 1, 1, red[1])
    sheet.fill("lens_lit", 0, 0, 3, 3, red[3])
    sheet.put("lens_lit", 1, 1, red[4])
    for i in range(4):
        sheet.put("lens_lit", i, 3, red[2])
        sheet.put("lens_lit", 3, i, red[2])


def tab(sheet):
    for x, y, c in ((0, 0, BRASS[4]), (1, 0, BRASS[3]), (0, 1, BRASS[3]), (1, 1, BRASS[1])):
        sheet.put("tab", x, y, c)


def interface():
    sheet = Sheet(32, ISLANDS)
    plate(sheet, source("reactor_vessel_casing"))
    track(sheet)
    bezel(sheet)
    lens(sheet)
    tab(sheet)
    return sheet


def main():
    if "fresh" not in sys.argv:
        print("left alone; pass fresh to repaint the signal interface sheet")
        return
    interface().save(os.path.join(BLOCK, "reactor_signal_interface.png"))
    print("painted the signal interface sheet")


if __name__ == "__main__":
    main()

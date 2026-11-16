import os
import sys

from controller_texture import palette
from plant_style import BLOCK, GLASS, Sheet, ramp, source

LEVELS = ("cold", "warm", "hot")
ISLANDS = {
    "plate_top": (0, 0, 16, 16),
    "plate_side": (16, 0, 16, 3),
    "header_face": (0, 16, 8, 8),
    "header_side": (8, 16, 2, 8),
    "header_top": (10, 16, 8, 2),
}
for i, level in enumerate(LEVELS):
    ISLANDS["coil_side_" + level] = (16, 3 + 2 * i, 12, 2)
    ISLANDS["coil_end_" + level] = (16, 9 + 2 * i, 10, 2)
    ISLANDS["coil_link_" + level] = (28, 3 + 2 * i, 2, 2)
    ISLANDS["coil_flat_" + level] = (30, 3 + 2 * i, 2, 2)
GROOVES = (4, 7, 10)
HOT = ((255, 176, 96), (222, 104, 44))


def plate(sheet, c):
    sheet.fill("plate_top", 0, 0, 15, 15, c[4])
    sheet.outline("plate_top", 0, 0, 15, 15, c[5])
    for i in range(16):
        sheet.put("plate_top", i, 15, c[3])
        sheet.put("plate_top", 15, i, c[3])
    for y in GROOVES:
        for x in range(3, 13):
            sheet.put("plate_top", x, y, c[3])
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
        sheet.put("plate_top", x, y, c[2])
    for x in range(16):
        end = x in (0, 15)
        sheet.put("plate_side", x, 0, c[4] if end else c[5])
        sheet.put("plate_side", x, 1, c[3] if end else c[4])
        sheet.put("plate_side", x, 2, c[1] if end else c[2])


def coil(sheet, level, top, bottom):
    for x in range(12):
        sheet.put("coil_side_" + level, x, 0, top)
        sheet.put("coil_side_" + level, x, 1, bottom)
    for x in range(10):
        sheet.put("coil_end_" + level, x, 0, top if x % 3 != 2 else bottom)
        sheet.put("coil_end_" + level, x, 1, bottom)
    for island in ("coil_link_" + level, "coil_flat_" + level):
        sheet.fill(island, 0, 0, 1, 1, bottom)
    sheet.put("coil_link_" + level, 0, 0, top)


def header(sheet, c, tone):
    sheet.fill("header_face", 0, 0, 7, 7, tone[3])
    sheet.outline("header_face", 0, 0, 7, 7, tone[4])
    sheet.outline("header_face", 1, 1, 6, 6, c[4])
    for i in range(1, 7):
        sheet.put("header_face", i, 6, c[2])
        sheet.put("header_face", 6, i, c[2])
    sheet.fill("header_face", 2, 2, 5, 5, GLASS[0])
    for y in range(8):
        sheet.put("header_side", 0, y, tone[4])
        sheet.put("header_side", 1, y, tone[2])
    for x in range(8):
        sheet.put("header_top", x, 0, tone[5])
        sheet.put("header_top", x, 1, tone[3])


def boiler_plate():
    c = palette("copper")
    sheet = Sheet(32, ISLANDS)
    plate(sheet, c)
    coil(sheet, "cold", c[3], c[1])
    coil(sheet, "warm", c[5], c[3])
    coil(sheet, "hot", HOT[0], HOT[1])
    header(sheet, c, ramp(source("steel_block")))
    return sheet


def main():
    if "fresh" not in sys.argv:
        print("left alone; pass fresh to repaint the boiler heating plate sheet")
        return
    boiler_plate().save(os.path.join(BLOCK, "boiler_heating_plate.png"))
    print("painted the boiler heating plate sheet")


if __name__ == "__main__":
    main()

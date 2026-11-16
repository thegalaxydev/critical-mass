import os
import sys

from plant_style import BLOCK, BRASS, GLASS, Sheet, ramp, source

ISLANDS = {
    "wall": (0, 0, 14, 12),
    "inner": (14, 0, 14, 12),
    "floor": (0, 12, 12, 12),
    "cell_edge": (12, 12, 5, 2),
    "rod": (12, 14, 3, 9),
    "cap_side": (15, 14, 3, 2),
    "cap_top": (15, 16, 3, 3),
}
BANDS = (2, 5)


def tub(sheet, casing, tone):
    edge, shadow, fill = casing.getpixel((3, 2)), casing.getpixel((4, 2)), casing.getpixel((4, 4))
    sheet.fill("wall", 0, 0, 13, 11, fill)
    for x in range(14):
        sheet.put("wall", x, 0, edge)
        sheet.put("wall", x, 11, shadow)
    sheet.fill("inner", 0, 0, 13, 11, shadow)
    for x in range(14):
        sheet.put("inner", x, 0, edge)
    sheet.fill("floor", 0, 0, 11, 11, GLASS[0])
    for x in range(5):
        sheet.put("cell_edge", x, 0, tone[2])
        sheet.put("cell_edge", x, 1, tone[1])


def assembly(sheet, tone):
    across = (tone[5], tone[4], tone[3])
    for x, c in enumerate(across):
        for y in range(9):
            sheet.put("rod", x, y, tone[2] if y in BANDS else c)
    for x, c in enumerate((BRASS[3], BRASS[2], BRASS[1])):
        sheet.put("cap_side", x, 0, c)
        sheet.put("cap_side", x, 1, BRASS[1] if x < 2 else BRASS[0])
    sheet.fill("cap_top", 0, 0, 2, 2, BRASS[2])
    sheet.put("cap_top", 0, 0, BRASS[4])
    sheet.put("cap_top", 1, 1, GLASS[0])
    sheet.put("cap_top", 2, 2, BRASS[1])


def rack():
    tone = ramp(source("steel_block"))
    sheet = Sheet(32, ISLANDS)
    tub(sheet, source("reactor_vessel_casing"), tone)
    assembly(sheet, tone)
    return sheet


def main():
    if "fresh" not in sys.argv:
        print("left alone; pass fresh to repaint the spent fuel rack sheet")
        return
    rack().save(os.path.join(BLOCK, "spent_fuel_rack.png"))
    print("painted the spent fuel rack sheet")


if __name__ == "__main__":
    main()

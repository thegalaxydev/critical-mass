import os
import sys

from controller_texture import palette
from plant_style import BLOCK, BRASS, GLASS, Sheet, nine, ramp, source

INSULATOR = {"base_top": (0, 0, 6, 6), "base_side": (6, 0, 6, 1), "shed_top": (6, 2, 6, 6), "shed_side": (6, 8, 6, 1), "body": (0, 6, 4, 7), "cap": (12, 0, 2, 2), "cap_side": (12, 2, 2, 2)}
WIRE = {"all": (0, 0, 16, 16)}
ITEM = os.path.join(os.path.dirname(BLOCK), "item")
STRAND = [(24, 28, 36), (40, 47, 58), (66, 77, 92)]
GENERATOR = {"barrel": (0, 0, 12, 14), "flange_side": (12, 0, 14, 1), "end": (0, 14, 14, 14), "bore": (14, 14, 4, 1), "stud": (18, 14, 2, 2)}
BREAKER = {"door": (0, 0, 14, 14), "wall": (14, 0, 14, 1), "handle": (14, 2, 2, 2), "red": (16, 2, 2, 2), "red_lit": (18, 2, 2, 2), "green": (20, 2, 2, 2),
           "green_lit": (22, 2, 2, 2), "terminal": (0, 14, 16, 16)}
GRID = {"tank": (0, 0, 14, 11), "tank_top": (0, 11, 14, 14), "bushing": (14, 0, 2, 5), "bushing_top": (16, 0, 2, 2)}
WINDING = (5, 6, 7, 8)


def across(width, light, mid, shade, dark):
    return [light if i == 0 else dark if i == width - 1 else shade if i == width - 2 else mid for i in range(width)]


def navy(casing):
    return casing.getpixel((7, 8)), casing.getpixel((4, 4)), casing.getpixel((4, 2)), casing.getpixel((2, 3))


def insulator(steel, tone):
    ceramic, copper = palette("ceramic"), palette("copper")
    sheet = Sheet(16, INSULATOR)
    sheet.paste("base_top", nine(steel, 6, 6, 1))
    for x in range(6):
        sheet.put("base_side", x, 0, tone[2] if x in (0, 5) else tone[4])
        sheet.put("shed_side", x, 0, ceramic[1] if x == 5 else ceramic[5] if x == 0 else ceramic[4])
    sheet.fill("shed_top", 0, 0, 5, 5, ceramic[4])
    sheet.outline("shed_top", 0, 0, 5, 5, ceramic[3])
    sheet.fill("shed_top", 1, 1, 4, 4, ceramic[2])
    for y in range(7):
        row = across(4, ceramic[4], ceramic[3], ceramic[2], ceramic[1])
        for x in range(4):
            sheet.put("body", x, y, row[x])
    for name in ("cap", "cap_side"):
        for x, y, c in ((0, 0, copper[5]), (1, 0, copper[4]), (0, 1, copper[3]), (1, 1, copper[2])):
            sheet.put(name, x, y, c)
    return sheet


def wire():
    copper = palette("copper")
    sheet = Sheet(16, WIRE)
    for y in range(16):
        for x in range(16):
            r = ((x - 6.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if 2.6 <= r < 6.4:
                band = STRAND[0] if r < 3.3 or r >= 5.7 else STRAND[2] if 4.1 <= r < 4.9 and x + y < 14 else STRAND[1]
                sheet.put("all", x, y, band)
    for x, y, c in ((11, 11, STRAND[1]), (12, 12, STRAND[1]), (12, 11, STRAND[0]), (11, 12, STRAND[0]), (13, 13, copper[4]), (14, 14, copper[5]), (14, 13, copper[2]), (13, 14, copper[2])):
        sheet.put("all", x, y, c)
    return sheet


def generator(casing, steel, tone):
    copper = palette("copper")
    sheet = Sheet(32, GENERATOR)
    for y in range(14):
        row = across(12, *((copper[5], copper[4], copper[3], copper[2]) if y in WINDING else navy(casing)))
        for x in range(12):
            sheet.put("barrel", x, y, row[x])
    for x in range(14):
        sheet.put("flange_side", x, 0, tone[3] if x in (0, 13) else tone[5])
    sheet.paste("end", nine(steel, 14, 14))
    sheet.outline("end", 4, 4, 9, 9, tone[2])
    sheet.fill("end", 5, 5, 8, 8, GLASS[0])
    for x in range(4):
        sheet.put("bore", x, 0, tone[1])
    for x, y, c in ((0, 0, copper[5]), (1, 0, copper[4]), (0, 1, copper[3]), (1, 1, copper[2])):
        sheet.put("stud", x, y, c)
    return sheet


def breaker(casing, steel, tone):
    light, mid, shade, dark = navy(casing)
    red, green, copper = palette("red"), palette("green"), palette("copper")
    sheet = Sheet(32, BREAKER)
    sheet.fill("door", 0, 0, 13, 13, mid)
    for i in range(14):
        sheet.put("door", i, 0, dark)
        sheet.put("door", 0, i, shade)
    sheet.fill("door", 6, 3, 7, 10, GLASS[0])
    for x in range(3, 11):
        sheet.put("door", x, 12, shade)
    for x in range(14):
        sheet.put("wall", x, 0, dark)
    for x, y, c in ((0, 0, BRASS[4]), (1, 0, BRASS[3]), (0, 1, BRASS[2]), (1, 1, BRASS[1])):
        sheet.put("handle", x, y, c)
    for name, tones in (("red", red), ("green", green)):
        for x, y, c in ((0, 0, tones[1]), (1, 0, tones[0]), (0, 1, tones[0]), (1, 1, tones[0])):
            sheet.put(name, x, y, c)
        for x, y, c in ((0, 0, tones[4]), (1, 0, tones[3]), (0, 1, tones[3]), (1, 1, tones[2])):
            sheet.put(name + "_lit", x, y, c)
    sheet.paste("terminal", steel)
    for x, ring in ((2, copper), (10, BRASS)):
        sheet.outline("terminal", x, 6, x + 3, 9, ring[-3])
        for i in range(4):
            sheet.put("terminal", x + i, 9, ring[1])
            sheet.put("terminal", x + 3, 6 + i, ring[1])
        sheet.fill("terminal", x + 1, 7, x + 2, 8, GLASS[0])
    return sheet


def grid(steel, tone):
    ceramic, copper = palette("ceramic"), palette("copper")
    sheet = Sheet(32, GRID)
    for x in range(14):
        rib = tone[3] if x % 2 else tone[2]
        for y in range(11):
            sheet.put("tank", x, y, tone[4] if y == 0 else tone[1] if y == 10 else tone[5] if x in (0, 13) else rib)
    sheet.paste("tank_top", nine(steel, 14, 14))
    for y in range(5):
        sheet.put("bushing", 0, y, ceramic[4] if y % 2 == 0 else ceramic[2])
        sheet.put("bushing", 1, y, ceramic[3] if y % 2 == 0 else ceramic[1])
    sheet.fill("bushing_top", 0, 0, 1, 1, copper[4])
    sheet.put("bushing_top", 1, 1, copper[2])
    return sheet


def sheets():
    casing = source("reactor_vessel_casing")
    steel = source("steel_block")
    tone = ramp(steel)
    return {"insulator": insulator(steel, tone), "generator": generator(casing, steel, tone), "circuit_breaker": breaker(casing, steel, tone), "grid_connection": grid(steel, tone)}


def main():
    if "fresh" not in sys.argv:
        print("left alone; pass fresh to repaint the electrical sheets")
        return
    for name, sheet in sheets().items():
        sheet.save(os.path.join(BLOCK, name + ".png"))
        print("painted", name)
    wire().save(os.path.join(ITEM, "power_wire.png"))
    print("painted power_wire")


if __name__ == "__main__":
    main()

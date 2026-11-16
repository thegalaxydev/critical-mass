import os
import sys

from controller_texture import palette
from plant_style import BLOCK, BRASS, GLASS, Sheet, nine, ramp, source

INSULATOR = {"base_top": (0, 0, 6, 6), "base_side": (6, 0, 6, 1), "shed_top": (6, 2, 6, 6), "shed_side": (6, 8, 6, 1), "body": (0, 6, 4, 7), "cap": (12, 0, 2, 2), "cap_side": (12, 2, 2, 2)}
WIRE = {"all": (0, 0, 16, 16)}
ITEM = os.path.join(os.path.dirname(BLOCK), "item")
STRAND = [(84, 44, 30), (136, 76, 48), (190, 118, 76)]
GENERATOR = {"barrel": (0, 0, 12, 14), "flange_side": (12, 0, 14, 1), "end": (0, 14, 14, 14), "bore": (14, 14, 4, 1), "stud": (18, 14, 2, 2)}
BREAKER = {"door": (0, 0, 14, 14), "wall": (14, 0, 14, 1), "handle": (14, 2, 2, 2), "red": (16, 2, 2, 2), "red_lit": (18, 2, 2, 2), "green": (20, 2, 2, 2),
           "green_lit": (22, 2, 2, 2), "terminal": (0, 14, 16, 16)}
GRID = {"tank": (0, 0, 14, 11), "tank_top": (0, 11, 14, 14), "bushing": (14, 0, 2, 5), "bushing_top": (16, 0, 2, 2)}
MOTOR = {"side": (0, 0, 11, 12), "top": (12, 0, 12, 11), "front": (0, 12, 10, 10), "back": (10, 12, 10, 10), "rim": (20, 12, 12, 12), "foot": (0, 22, 10, 2),
         "box_side": (0, 24, 6, 2), "box_top": (6, 24, 6, 6)}
BUS = {"bar": (0, 0, 4, 16), "bar_end": (4, 0, 4, 4), "post": (4, 4, 5, 4), "plate": (9, 4, 6, 6), "plate_side": (4, 10, 6, 1)}
TRANSFORMER = {"tank_end": (0, 0, 8, 11), "tank_top": (8, 0, 14, 8), "fins": (0, 11, 12, 8), "fin_end": (12, 11, 3, 8), "lid": (0, 19, 16, 10), "lid_side": (16, 19, 16, 1),
               "bushing": (22, 0, 2, 4), "bushing_top": (24, 0, 2, 2), "tank": (26, 0, 4, 3)}
CABLE_TONES = [(10, 12, 16), (22, 25, 31), (44, 49, 58)]
LINE_TONES = [(92, 99, 108), (142, 150, 160), (196, 202, 210)]
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


def wire(strand=None, tip=None):
    copper = tip or palette("copper")
    strand = strand or STRAND
    sheet = Sheet(16, WIRE)
    for y in range(16):
        for x in range(16):
            r = ((x - 6.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if 2.6 <= r < 6.4:
                band = strand[0] if r < 3.3 or r >= 5.7 else strand[2] if 4.1 <= r < 4.9 and x + y < 14 else strand[1]
                sheet.put("all", x, y, band)
    for x, y, c in ((11, 11, strand[1]), (12, 12, strand[1]), (12, 11, strand[0]), (11, 12, strand[0]), (13, 13, copper[4]), (14, 14, copper[5]), (14, 13, copper[2]), (13, 14, copper[2])):
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


def motor(casing, steel, tone):
    light, mid, shade, dark = navy(casing)
    copper = palette("copper")
    sheet = Sheet(32, MOTOR)
    for y in range(12):
        for x in range(11):
            sheet.put("side", x, y, light if y == 0 else dark if y == 11 else shade if x % 2 else mid)
    sheet.fill("top", 0, 0, 11, 10, mid)
    for y in range(11):
        sheet.put("top", 0, y, light)
        sheet.put("top", 11, y, shade)
    sheet.fill("rim", 0, 0, 11, 11, shade)
    sheet.outline("rim", 0, 0, 11, 11, dark)
    for name in ("front", "back"):
        sheet.paste(name, nine(steel, 10, 10))
    sheet.outline("front", 2, 2, 7, 7, tone[2])
    sheet.fill("front", 3, 3, 6, 6, GLASS[0])
    for y in (2, 4, 6):
        for x in range(2, 8):
            sheet.put("back", x, y, GLASS[0])
            sheet.put("back", x, y + 1, tone[2])
    for x in range(10):
        sheet.put("foot", x, 0, tone[4])
        sheet.put("foot", x, 1, tone[2])
    for x in range(6):
        sheet.put("box_side", x, 0, tone[5] if x not in (0, 5) else tone[3])
        sheet.put("box_side", x, 1, tone[3] if x not in (0, 5) else tone[2])
    sheet.paste("box_top", nine(steel, 6, 6, 1))
    sheet.outline("box_top", 1, 1, 4, 4, copper[4])
    sheet.put("box_top", 4, 4, copper[2])
    sheet.fill("box_top", 2, 2, 3, 3, GLASS[0])
    return sheet


def bus(steel, tone):
    ceramic, copper = palette("ceramic"), palette("copper")
    sheet = Sheet(16, BUS)
    row = across(4, copper[5], copper[4], copper[3], copper[2])
    for y in range(16):
        for x in range(4):
            sheet.put("bar", x, y, row[x])
    sheet.fill("bar_end", 0, 0, 3, 3, copper[3])
    sheet.outline("bar_end", 0, 0, 3, 3, copper[2])
    for x in range(5):
        for y in range(4):
            sheet.put("post", x, y, (ceramic[4], ceramic[3], ceramic[3], ceramic[1])[y] if x % 2 == 0 else (ceramic[3], ceramic[2], ceramic[2], ceramic[1])[y])
    sheet.paste("plate", nine(steel, 6, 6, 1))
    for x in range(6):
        sheet.put("plate_side", x, 0, tone[2] if x in (0, 5) else tone[4])
    return sheet


def transformer(steel, tone):
    ceramic, copper = palette("ceramic"), palette("copper")
    sheet = Sheet(32, TRANSFORMER)
    for y in range(11):
        for x in range(8):
            sheet.put("tank_end", x, y, tone[4] if y == 0 else tone[1] if y == 10 else tone[5] if x == 0 else tone[2] if x == 7 else tone[3])
    sheet.paste("tank_top", nine(steel, 14, 8))
    for y in range(8):
        for x in range(12):
            sheet.put("fins", x, y, tone[1] if y == 7 else tone[4] if x % 2 == 0 else tone[1])
        for x in range(3):
            sheet.put("fin_end", x, y, tone[1] if y == 7 else (tone[4], tone[3], tone[2])[x])
    sheet.paste("lid", nine(steel, 16, 10))
    for x in range(16):
        sheet.put("lid_side", x, 0, tone[2] if x in (0, 15) else tone[5])
    for y in range(4):
        sheet.put("bushing", 0, y, ceramic[4] if y % 2 == 0 else ceramic[2])
        sheet.put("bushing", 1, y, ceramic[3] if y % 2 == 0 else ceramic[1])
    sheet.fill("bushing_top", 0, 0, 1, 1, copper[4])
    sheet.put("bushing_top", 1, 1, copper[2])
    for y in range(3):
        for x in range(4):
            sheet.put("tank", x, y, tone[5] if y == 0 else tone[2] if y == 2 else tone[4] if x < 3 else tone[2])
    return sheet


def sheets():
    casing = source("reactor_vessel_casing")
    steel = source("steel_block")
    tone = ramp(steel)
    return {"insulator": insulator(steel, tone), "generator": generator(casing, steel, tone), "circuit_breaker": breaker(casing, steel, tone), "grid_connection": grid(steel, tone), "electric_motor": motor(casing, steel, tone), "bus_bar": bus(steel, tone), "transformer": transformer(steel, tone)}


def main():
    if "fresh" not in sys.argv:
        print("left alone; pass fresh to repaint the electrical sheets")
        return
    for name, sheet in sheets().items():
        sheet.save(os.path.join(BLOCK, name + ".png"))
        print("painted", name)
    wire().save(os.path.join(ITEM, "power_wire.png"))
    wire(CABLE_TONES).save(os.path.join(ITEM, "power_cable.png"))
    wire(LINE_TONES, LINE_TONES + LINE_TONES).save(os.path.join(ITEM, "transmission_line.png"))
    print("painted the three wires")


if __name__ == "__main__":
    main()

import os
import sys

from plant_style import BLOCK, BRASS, GLASS, Sheet, ramp, source

OUT = os.path.join(BLOCK, "fuel_channel_cap.png")
ISLANDS = {
    "ring_top": (0, 0, 16, 16),
    "well_wall": (0, 16, 16, 3),
    "plug": (16, 0, 10, 10),
    "well": (16, 10, 10, 10),
}
BOLTS = ((2, 2), (7, 2), (2, 7), (7, 7))
RODS = (1, 4, 7)


def paint():
    steel = source("steel_block")
    casing = source("reactor_vessel_casing")
    alloy = ramp(source("reactor_alloy_block"))
    tone = ramp(steel)
    dark = ramp(casing)
    shadow, fill = casing.getpixel((4, 2)), casing.getpixel((4, 4))
    sheet = Sheet(32, ISLANDS)
    sheet.paste("ring_top", steel)
    for x in range(16):
        sheet.put("well_wall", x, 0, fill)
        sheet.put("well_wall", x, 1, shadow)
        sheet.put("well_wall", x, 2, GLASS[0])
    sheet.fill("plug", 0, 0, 9, 9, dark[3])
    for i in range(10):
        sheet.put("plug", i, 0, dark[5])
        sheet.put("plug", 0, i, dark[5])
        sheet.put("plug", i, 9, dark[0])
        sheet.put("plug", 9, i, dark[0])
    sheet.put("plug", 0, 9, dark[3])
    sheet.put("plug", 9, 0, dark[3])
    sheet.outline("plug", 3, 3, 6, 6, dark[1])
    sheet.fill("plug", 4, 4, 5, 5, BRASS[2])
    sheet.put("plug", 4, 4, BRASS[4])
    sheet.put("plug", 5, 5, BRASS[0])
    for x, y in BOLTS:
        sheet.put("plug", x, y, tone[6])
        sheet.put("plug", x + 1, y + 1, dark[0])
    sheet.fill("well", 0, 0, 9, 9, GLASS[0])
    sheet.outline("well", 0, 0, 9, 9, alloy[1])
    for x in RODS:
        for y in RODS:
            sheet.fill("well", x, y, x + 1, y + 1, alloy[4])
            sheet.put("well", x, y, alloy[6])
            sheet.put("well", x + 1, y + 1, alloy[2])
    return sheet


def main():
    if "fresh" not in sys.argv:
        print("left alone; pass fresh to repaint", OUT)
        return
    paint().save(OUT)
    print("painted", OUT)


if __name__ == "__main__":
    main()

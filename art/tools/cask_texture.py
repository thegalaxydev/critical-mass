import os
import sys

from controller_texture import palette
from plant_style import BLOCK, GLASS, Sheet, nine, ramp, source

ISLANDS = {
    "barrel": (0, 0, 12, 10),
    "lid_top": (0, 10, 12, 12),
    "body_top": (12, 0, 12, 12),
    "lid_side": (12, 12, 12, 2),
    "stud_side": (12, 14, 1, 4),
    "stud_top": (14, 14, 1, 1),
}
BAND = (4, 5)


def across(width, light, mid, shade, dark):
    return [light if i == 0 else dark if i == width - 1 else shade if i == width - 2 else mid for i in range(width)]


def body(sheet, casing):
    navy = (casing.getpixel((7, 8)), casing.getpixel((4, 4)), casing.getpixel((4, 2)), casing.getpixel((2, 3)))
    hazard = palette("hazard")
    band = (hazard[3], hazard[2], hazard[1], hazard[0])
    for y in range(10):
        row = across(12, *(band if y in BAND else navy))
        for x in range(12):
            sheet.put("barrel", x, y, row[x])
    sheet.fill("body_top", 0, 0, 11, 11, GLASS[0])
    sheet.outline("body_top", 0, 0, 11, 11, navy[2])


def lid(sheet, steel, tone):
    sheet.paste("lid_top", nine(steel, 12, 12))
    sheet.outline("lid_top", 4, 4, 7, 7, tone[2])
    sheet.fill("lid_top", 5, 5, 6, 6, tone[1])
    for x in range(12):
        sheet.put("lid_side", x, 0, tone[5] if 0 < x < 11 else tone[3])
        sheet.put("lid_side", x, 1, tone[2] if 0 < x < 11 else tone[1])
    for y in range(4):
        sheet.put("stud_side", 0, y, tone[2] if y % 2 else tone[4])
    sheet.put("stud_top", 0, 0, tone[6])


def cask():
    steel = source("steel_block")
    sheet = Sheet(32, ISLANDS)
    body(sheet, source("reactor_vessel_casing"))
    lid(sheet, steel, ramp(steel))
    return sheet


def main():
    if "fresh" not in sys.argv:
        print("left alone; pass fresh to repaint the shielded cask sheet")
        return
    cask().save(os.path.join(BLOCK, "shielded_cask.png"))
    print("painted the shielded cask sheet")


if __name__ == "__main__":
    main()

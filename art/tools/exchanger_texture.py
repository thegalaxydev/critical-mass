import os
import sys

import create_style as cs
from plant_style import BLOCK, Sheet, nine, ramp, source

ISLANDS = {
    "barrel_side": (0, 0, 12, 16),
    "flange_end": (12, 0, 14, 14),
    "flange_side": (12, 14, 14, 1),
    "barrel_top": (0, 16, 12, 16),
    "barrel_flank": (12, 16, 16, 12),
}
HOOPS = (4, 11)
UNDER_FLANGE = (1, 14)
LOW, SPAN = 0.1, 0.8


def lit(tones, value):
    return cs.tone(tones, LOW + SPAN * value)


def along(i):
    return 0.12 if i in HOOPS else -0.28 if i - 1 in HOOPS else -0.2 if i == UNDER_FLANGE[0] else -0.1 if i == UNDER_FLANGE[1] else 0.0


def barrel(sheet, tones):
    band = cs.tube(12)
    for x in range(12):
        for y in range(16):
            sheet.put("barrel_side", x, y, lit(tones, band[x] + along(y)))
            sheet.put("barrel_top", x, y, lit(tones, band[x] + along(y)))
    for y in range(12):
        for x in range(16):
            sheet.put("barrel_flank", x, y, lit(tones, band[y] + along(x)))


def flange(sheet, end, tones):
    sheet.paste("flange_end", end)
    for x, v in enumerate(cs.tube(14)):
        sheet.put("flange_side", x, 0, lit(tones, v - 0.1))


def copper_plate():
    plate = nine(source("steel_block"), 14, 14)
    for (x, y), v in cs.plate(14, 14).items():
        plate.putpixel((x, y), cs.tone(cs.COPPER, v * 0.9) + (255,))
    return plate


def heat_exchanger():
    steel = source("steel_block")
    sheet = Sheet(32, ISLANDS)
    barrel(sheet, cs.COPPER)
    flange(sheet, nine(steel, 14, 14), ramp(steel, 11))
    return sheet


def condenser():
    sheet = Sheet(32, ISLANDS)
    barrel(sheet, ramp(source("steel_block"), 11))
    flange(sheet, copper_plate(), cs.COPPER)
    return sheet


def main():
    if "fresh" not in sys.argv:
        print("left alone; pass fresh to repaint the exchanger sheets")
        return
    heat_exchanger().save(os.path.join(BLOCK, "heat_exchanger.png"))
    condenser().save(os.path.join(BLOCK, "condenser.png"))
    print("painted the exchanger sheets")


if __name__ == "__main__":
    main()

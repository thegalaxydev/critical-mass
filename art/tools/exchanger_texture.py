import json
import os
import sys

from PIL import Image

from plant_style import BLOCK, ROOT, Sheet, nine, ramp, source

ISLANDS = {
    "barrel_side": (0, 0, 12, 16),
    "flange_end": (12, 0, 14, 14),
    "flange_side": (12, 14, 14, 1),
    "barrel_top": (0, 16, 12, 16),
    "barrel_flank": (12, 16, 16, 12),
}


def copper():
    ramps = json.load(open(os.path.join(ROOT, "docs", "art", "palette.json")))
    ramps = ramps.get("ramps", ramps)["copper"]
    return [tuple(int(c[i:i + 2], 16) for i in (1, 3, 5)) for c in ramps]


def across(width, light, mid, shade, dark):
    return [light if i == 0 else dark if i == width - 1 else shade if i == width - 2 else mid for i in range(width)]


def barrel(sheet, tones):
    hoops = (4, 11)
    for x, c in enumerate(across(12, *tones)):
        for y in range(16):
            ring = tones[2] if y in hoops and 0 < x < 10 else c
            sheet.put("barrel_side", x, y, ring)
            sheet.put("barrel_top", x, y, ring)
    for y, c in enumerate(across(12, *tones)):
        for x in range(16):
            sheet.put("barrel_flank", x, y, tones[2] if x in hoops and 0 < y < 10 else c)


def flange(sheet, plate, edge):
    sheet.paste("flange_end", plate)
    for x in range(14):
        sheet.put("flange_side", x, 0, edge[1] if x in (0, 13) else edge[0])


def flat(color_light, color_mid, color_dark):
    plate = Image.new("RGBA", (14, 14), color_mid + (255,))
    for i in range(14):
        plate.putpixel((i, 0), color_light + (255,))
        plate.putpixel((0, i), color_light + (255,))
    for i in range(14):
        plate.putpixel((i, 13), color_dark + (255,))
        plate.putpixel((13, i), color_dark + (255,))
    return plate


def heat_exchanger():
    c = copper()
    steel = source("steel_block")
    tone = ramp(steel)
    sheet = Sheet(32, ISLANDS)
    barrel(sheet, (c[5], c[4], c[3], c[2]))
    flange(sheet, nine(steel, 14, 14), (tone[4], tone[1]))
    return sheet


def condenser():
    c = copper()
    tone = ramp(source("steel_block"))
    sheet = Sheet(32, ISLANDS)
    barrel(sheet, (tone[6], tone[5], tone[4], tone[2]))
    flange(sheet, flat(c[5], c[4], c[2]), (c[4], c[2]))
    return sheet


def main():
    first = os.path.join(BLOCK, "heat_exchanger.png")
    if "fresh" not in sys.argv and Image.open(first).getpixel((31, 15))[3] == 0:
        print("left alone; pass fresh to repaint the exchanger sheets")
        return
    heat_exchanger().save(first)
    condenser().save(os.path.join(BLOCK, "condenser.png"))
    print("painted the exchanger sheets")


if __name__ == "__main__":
    main()

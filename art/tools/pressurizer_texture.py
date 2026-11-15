import json
import os
import sys

from PIL import Image

from plant_style import BLOCK, BRASS, ROOT, Sheet, nine, ramp, source

OUT = os.path.join(BLOCK, "pressurizer.png")
ISLANDS = {
    "barrel_lower": (0, 0, 12, 15),
    "barrel_upper": (12, 0, 12, 9),
    "flange_side": (12, 9, 14, 1),
    "nozzle_side": (26, 0, 2, 1),
    "nozzle_top": (26, 2, 2, 2),
    "shoulder_side": (0, 16, 10, 3),
    "cap_side": (10, 16, 6, 2),
    "cap_top": (10, 18, 6, 6),
    "shoulder_top": (0, 19, 10, 10),
    "flange_end": (18, 16, 14, 14),
}


def copper():
    ramps = json.load(open(os.path.join(ROOT, "docs", "art", "palette.json")))
    ramps = ramps.get("ramps", ramps)["copper"]
    return [tuple(int(c[i:i + 2], 16) for i in (1, 3, 5)) for c in ramps]


def across(width, light, mid, shade, dark):
    return [light if i == 0 else dark if i == width - 1 else shade if i == width - 2 else mid for i in range(width)]


def paint():
    steel = source("steel_block")
    casing = source("reactor_vessel_casing")
    tone = ramp(steel)
    c = copper()
    navy = (casing.getpixel((7, 8)), casing.getpixel((4, 4)), casing.getpixel((4, 2)), casing.getpixel((2, 3)))
    heat = (c[5], c[4], c[3], c[2])
    sheet = Sheet(32, ISLANDS)
    for island, height, heater in (("barrel_lower", 15, True), ("barrel_upper", 9, False)):
        for x in range(12):
            for y in range(height):
                band = heater and y >= height - 3
                sheet.put(island, x, y, across(12, *(heat if band else navy))[x])
    for island, width in (("shoulder_side", 10), ("cap_side", 6)):
        _, _, w, h = ISLANDS[island]
        for x in range(w):
            for y in range(h):
                sheet.put(island, x, y, across(width, *(navy if island == "shoulder_side" else (tone[6], tone[5], tone[4], tone[2])))[x])
    sheet.fill("shoulder_top", 0, 0, 9, 9, navy[1])
    for i in range(10):
        sheet.put("shoulder_top", i, 0, navy[0])
        sheet.put("shoulder_top", 0, i, navy[0])
        sheet.put("shoulder_top", i, 9, navy[3])
        sheet.put("shoulder_top", 9, i, navy[3])
    sheet.paste("cap_top", nine(steel, 6, 6, 1))
    sheet.paste("flange_end", nine(steel, 14, 14))
    for x in range(14):
        sheet.put("flange_side", x, 0, tone[1] if x in (0, 13) else tone[4])
    sheet.put("nozzle_side", 0, 0, BRASS[3])
    sheet.put("nozzle_side", 1, 0, BRASS[1])
    sheet.fill("nozzle_top", 0, 0, 1, 1, BRASS[2])
    sheet.put("nozzle_top", 0, 0, BRASS[4])
    return sheet


def main():
    if "fresh" not in sys.argv and Image.open(OUT).getpixel((31, 0))[3] == 0:
        print("left alone; pass fresh to repaint", OUT)
        return
    paint().save(OUT)
    print("painted", OUT)


if __name__ == "__main__":
    main()

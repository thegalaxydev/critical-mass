import os
import sys

from PIL import Image

from plant_style import BLOCK, BRASS, Sheet, ramp, source

ISLANDS = {
    "plate_face": (0, 0, 16, 16),
    "plate_side": (16, 0, 16, 2),
    "flange_face": (16, 2, 10, 10),
    "post": (26, 2, 2, 12),
    "drum_flat": (28, 2, 4, 8),
    "flange_side": (16, 12, 10, 1),
    "neck": (16, 13, 4, 1),
}
HOOPS = (2, 5)


def plate(sheet, steel, tone):
    sheet.paste("plate_face", steel)
    sheet.outline("plate_face", 5, 5, 10, 10, BRASS[2])
    for i in range(5, 11):
        sheet.put("plate_face", i, 5, BRASS[3])
        sheet.put("plate_face", 5, i, BRASS[3])
    for i in range(6, 11):
        sheet.put("plate_face", i, 10, BRASS[1])
        sheet.put("plate_face", 10, i, BRASS[1])
    sheet.fill("plate_face", 6, 6, 9, 9, tone[1])
    for i in range(6, 10):
        sheet.put("plate_face", i, 6, tone[0])
        sheet.put("plate_face", 6, i, tone[0])
    for x in range(16):
        end = x in (0, 15)
        sheet.put("plate_side", x, 0, tone[3] if end else tone[5])
        sheet.put("plate_side", x, 1, tone[1] if end else tone[2])


def post(sheet, tone):
    for y in range(12):
        sheet.put("post", 0, y, tone[5])
        sheet.put("post", 1, y, tone[3])


def rotor(sheet, tone):
    across = (BRASS[4], BRASS[3], BRASS[3], BRASS[3])
    for x, c in enumerate(across):
        for y in range(8):
            sheet.put("drum_flat", x, y, BRASS[2] if y in HOOPS else c)
    sheet.fill("flange_face", 0, 0, 9, 9, BRASS[3])
    for i in range(10):
        sheet.put("flange_face", i, 0, BRASS[4])
        sheet.put("flange_face", 0, i, BRASS[4])
    for i in range(10):
        sheet.put("flange_face", i, 9, BRASS[2])
        sheet.put("flange_face", 9, i, BRASS[2])
    for x in range(10):
        sheet.put("flange_side", x, 0, BRASS[1] if x in (0, 9) else BRASS[2])
    for x, c in enumerate((tone[3], tone[2], tone[2], tone[1])):
        sheet.put("neck", x, 0, c)


def centrifuge():
    steel = source("steel_block")
    tone = ramp(steel)
    sheet = Sheet(32, ISLANDS)
    plate(sheet, steel, tone)
    post(sheet, tone)
    rotor(sheet, tone)
    return sheet


def main():
    if "fresh" not in sys.argv:
        print("left alone; pass fresh to repaint the centrifuge sheet")
        return
    centrifuge().save(os.path.join(BLOCK, "centrifuge.png"))
    print("painted the centrifuge sheet")


if __name__ == "__main__":
    main()

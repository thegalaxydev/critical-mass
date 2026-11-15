import os
import sys

from PIL import Image

from plant_style import BLOCK, BRASS, GLASS, RED, Sheet, nine, ramp, source

OUT = os.path.join(BLOCK, "control_rod_drive.png")
ROD = os.path.join(BLOCK, "control_rod_shaft.png")
ISLANDS = {
    "base_top": (0, 0, 16, 16),
    "deck_top": (16, 0, 14, 14),
    "deck_side": (16, 14, 14, 1),
    "case_front": (0, 16, 12, 10),
    "case_side": (12, 16, 12, 10),
    "sleeve_side": (24, 16, 4, 5),
    "sleeve_top": (28, 16, 4, 4),
    "base_side": (0, 26, 16, 2),
}


def paint():
    steel = source("steel_block")
    casing = source("reactor_vessel_casing")
    tone = ramp(steel)
    sheet = Sheet(32, ISLANDS)
    sheet.paste("base_top", steel)
    sheet.paste("deck_top", nine(steel, 14, 14))
    sheet.paste("base_side", steel.crop((0, 0, 16, 2)))
    for x in range(14):
        sheet.put("deck_side", x, 0, tone[1] if x in (0, 13) else tone[4] if x % 3 else tone[5])
    sheet.paste("case_front", nine(casing, 12, 10))
    sheet.fill("case_front", 4, 2, 7, 5, GLASS[0])
    sheet.outline("case_front", 3, 1, 8, 6, BRASS[2])
    sheet.put("case_front", 3, 1, BRASS[4])
    sheet.put("case_front", 4, 1, BRASS[3])
    sheet.put("case_front", 3, 2, BRASS[3])
    sheet.put("case_front", 8, 6, BRASS[0])
    sheet.put("case_front", 7, 6, BRASS[1])
    sheet.put("case_front", 8, 5, BRASS[1])
    sheet.paste("case_side", nine(casing, 12, 10))
    sheet.fill("case_side", 4, 3, 7, 6, GLASS[0])
    sheet.outline("case_side", 3, 2, 8, 7, BRASS[2])
    sheet.put("case_side", 3, 2, BRASS[4])
    sheet.put("case_side", 8, 7, BRASS[0])
    sheet.put("case_side", 4, 3, GLASS[2])
    sheet.put("case_side", 5, 3, GLASS[1])
    sheet.put("case_side", 4, 4, GLASS[1])
    sheet.put("case_side", 6, 5, RED[1])
    sheet.put("case_side", 7, 5, RED[2])
    for y in range(5):
        for x, c in enumerate((BRASS[1], BRASS[3], BRASS[2], BRASS[0])):
            sheet.put("sleeve_side", x, y, BRASS[4] if y == 0 and x == 1 else BRASS[0] if y == 4 else c)
    sheet.fill("sleeve_top", 0, 0, 3, 3, BRASS[2])
    sheet.fill("sleeve_top", 1, 1, 2, 2, GLASS[0])
    sheet.put("sleeve_top", 0, 0, BRASS[4])
    sheet.put("sleeve_top", 3, 3, BRASS[0])
    return sheet


def rod():
    steel = ramp(source("steel_block"))
    img = Image.open(ROD).convert("RGBA")
    for y in range(16):
        turn = y % 4
        img.putpixel((0, y), (steel[5] if turn < 2 else steel[3]) + (255,))
        img.putpixel((1, y), (steel[3] if turn in (0, 3) else steel[1]) + (255,))
    return img


def main():
    if "fresh" not in sys.argv and os.path.exists(OUT) and Image.open(OUT).size == (32, 32):
        print("left alone; pass fresh to repaint", OUT)
        return
    paint().save(OUT)
    rod().save(ROD)
    print("painted", OUT, "and", ROD)


if __name__ == "__main__":
    main()

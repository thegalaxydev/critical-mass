import os
import sys

from PIL import Image

from plant_style import BLOCK, BRASS, GLASS, Sheet, nine, panel, ramp, source

OUT = os.path.join(BLOCK, "control_rod_drive.png")
ROD = os.path.join(BLOCK, "control_rod_shaft.png")
ISLANDS = {
    "base_top": (0, 0, 16, 16),
    "cap_top": (16, 0, 12, 12),
    "cap_side": (16, 12, 12, 1),
    "sleeve_side": (28, 0, 4, 2),
    "sleeve_top": (28, 2, 4, 4),
    "base_side": (0, 16, 16, 2),
    "case_upper": (0, 18, 10, 4),
    "case_lower": (0, 22, 10, 4),
    "case_end": (10, 18, 10, 10),
}


def paint():
    steel = source("steel_block")
    casing = source("reactor_vessel_casing")
    tone = ramp(steel)
    sheet = Sheet(32, ISLANDS)
    sheet.paste("base_top", steel)
    sheet.paste("cap_top", nine(steel, 12, 12))
    sheet.paste("base_side", steel.crop((0, 0, 16, 2)))
    for x in range(12):
        sheet.put("cap_side", x, 0, tone[1] if x in (0, 11) else tone[4] if x % 3 else tone[5])
    sheet.paste("case_end", panel(casing, 10, 10))
    sheet.paste("case_upper", panel(casing, 10, 4))
    sheet.paste("case_lower", panel(casing, 10, 4))
    for y in range(2):
        for x, c in enumerate((BRASS[1], BRASS[3], BRASS[2], BRASS[0])):
            sheet.put("sleeve_side", x, y, BRASS[4] if y == 0 and x == 1 else c)
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

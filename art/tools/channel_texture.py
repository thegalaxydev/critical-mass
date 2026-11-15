import json
import os
import sys

from PIL import Image

from plant_style import BLOCK, ROOT, Sheet, source

ITEM = os.path.join(os.path.dirname(BLOCK), "item")
ISLANDS = {
    "plate_top": (0, 0, 16, 16),
    "plate_side": (0, 16, 16, 1),
    "rod_side": (16, 0, 2, 14),
    "guide_side": (18, 0, 2, 14),
    "tube_side": (20, 0, 3, 14),
    "absorber_side": (24, 0, 4, 16),
    "absorber_end": (28, 0, 4, 4),
}
RODS = (2, 7, 12)
GLOW = {1: ((196, 222, 240), (112, 150, 190)), 2: ((232, 246, 255), (150, 204, 240)), 3: ((255, 226, 160), (244, 150, 70))}


def tones(pick):
    img = Image.open(os.path.join(ITEM, "control_rod.png")).convert("RGBA")
    found = set()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = img.getpixel((x, y))
            if a and pick(r, g, b):
                found.add((r, g, b))
    out = sorted(found, key=sum)
    assert len(out) >= 3, "the Control Rod icon no longer has the tones this needs"
    return out


def copper():
    ramp = json.load(open(os.path.join(ROOT, "docs", "art", "palette.json")))
    ramp = ramp.get("ramps", ramp)["copper"]
    return [tuple(int(c[i:i + 2], 16) for i in (1, 3, 5)) for c in ramp]


def plate(sheet, light, mid, dark):
    sheet.fill("plate_top", 0, 0, 15, 15, mid)
    for i in range(16):
        sheet.put("plate_top", i, 0, light)
        sheet.put("plate_top", 0, i, light)
    for i in range(16):
        sheet.put("plate_top", i, 15, dark)
        sheet.put("plate_top", 15, i, dark)
    for x in range(16):
        sheet.put("plate_side", x, 0, dark if x in (0, 15) else mid)


def column(sheet, island, colors):
    _, _, w, h = ISLANDS[island]
    for y in range(h):
        for x in range(w):
            sheet.put(island, x, y, colors[x])


def fuel(rod):
    silver = tones(lambda r, g, b: abs(r - b) < 45 and r + g + b > 250)
    gold = tones(lambda r, g, b: r - b > 120)
    sheet = Sheet(32, ISLANDS)
    plate(sheet, gold[-1], gold[len(gold) // 2], gold[0])
    for x in RODS:
        for y in RODS:
            sheet.fill("plate_top", x, y, x + 1, y + 1, silver[-2])
    column(sheet, "rod_side", rod or (silver[-2], silver[1]))
    return sheet


def dark_plate(sheet):
    casing = source("reactor_vessel_casing")
    plate(sheet, casing.getpixel((15, 1)), casing.getpixel((4, 4)), casing.getpixel((4, 2)))
    return casing


def control():
    gold = tones(lambda r, g, b: r - b > 120)
    sheet = Sheet(32, ISLANDS)
    casing = dark_plate(sheet)
    sheet.fill("plate_top", 6, 6, 9, 9, gold[len(gold) // 2])
    column(sheet, "guide_side", (casing.getpixel((0, 1)), casing.getpixel((15, 1))))
    column(sheet, "absorber_side", (gold[-1], gold[-2], gold[len(gold) // 2], gold[0]))
    sheet.fill("absorber_end", 0, 0, 3, 3, gold[0])
    return sheet


def coolant():
    ramp = copper()
    sheet = Sheet(32, ISLANDS)
    dark_plate(sheet)
    for x in (2, 11):
        for y in (2, 11):
            sheet.fill("plate_top", x, y, x + 2, y + 2, ramp[3])
    column(sheet, "tube_side", (ramp[5], ramp[4], ramp[2]))
    return sheet


def ruined():
    sheet = Sheet(32, ISLANDS)
    plate(sheet, (72, 58, 46), (52, 42, 36), (30, 26, 24))
    for x in RODS:
        for y in RODS:
            sheet.fill("plate_top", x, y, x + 1, y + 1, (34, 30, 30))
    column(sheet, "rod_side", ((58, 50, 46), (34, 30, 30)))
    sheet.put("rod_side", 0, 9, (196, 96, 40))
    sheet.put("rod_side", 0, 10, (126, 52, 24))
    return sheet


def main():
    first = os.path.join(BLOCK, "fuel_channel.png")
    if "fresh" not in sys.argv and Image.open(first).getpixel((31, 31))[3] == 0:
        print("left alone; pass fresh to repaint the channel sheets")
        return
    fuel(None).save(first)
    for level, rod in GLOW.items():
        fuel(rod).save(os.path.join(BLOCK, "fuel_channel_glow_%d.png" % level))
    control().save(os.path.join(BLOCK, "control_channel.png"))
    coolant().save(os.path.join(BLOCK, "coolant_channel.png"))
    ruined().save(os.path.join(BLOCK, "ruined_channel.png"))
    print("painted the channel sheets")


if __name__ == "__main__":
    main()

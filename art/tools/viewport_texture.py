import os
import sys

from plant_style import BLOCK, Sheet, source

NAME = "reactor_viewport"
FRAME = 2
STAGES = 3
ISLANDS = {
    "face": (0, 0, 16, 16),
    "pane": (16, 0, 12, 12),
    "reveal_upright": (28, 0, 2, 16),
    "reveal_level": (16, 12, 12, 2),
}


def glass_tones():
    glass = source("shielded_glass")
    return {"light": glass.getpixel((4, 4)), "dark": glass.getpixel((11, 5)), "deep": glass.getpixel((11, 10)), "clear": glass.getpixel((6, 6)), "glint": glass.getpixel((9, 8))}


def viewport():
    casing = source("reactor_vessel_casing")
    sheet = Sheet(32, ISLANDS)
    for y in range(16):
        for x in range(16):
            if min(x, y, 15 - x, 15 - y) < FRAME:
                sheet.put("face", x, y, casing.getpixel((x, y)))
    tone = glass_tones()
    ox, oy, w, h = ISLANDS["pane"]
    for y in range(h):
        for x in range(w):
            sheet.img.putpixel((ox + x, oy + y), tone["clear"])
    for i in range(w):
        sheet.put("pane", i, 0, tone["light"])
        sheet.put("pane", 0, i, tone["light"])
    for i in range(1, w):
        sheet.put("pane", i, h - 1, tone["deep"] if i > 4 else tone["dark"])
        sheet.put("pane", w - 1, i, tone["deep"] if i > 6 else tone["dark"])
    sheet.put("pane", 6, 6, tone["glint"])
    sheet.put("pane", 5, 7, tone["light"])
    rim, wall = casing.getpixel((3, 2)), casing.getpixel((4, 2))
    for y in range(16):
        sheet.put("reveal_upright", 0, y, rim)
        sheet.put("reveal_upright", 1, y, wall)
    for x in range(12):
        sheet.put("reveal_level", x, 0, rim)
        sheet.put("reveal_level", x, 1, wall)
    return sheet


def cracked(stage):
    sheet = viewport()
    crack = source("casing_crack_%d" % stage)
    for y in range(16):
        for x in range(16):
            r, g, b, a = crack.getpixel((x, y))
            if a < 200:
                continue
            if min(x, y, 15 - x, 15 - y) < FRAME:
                sheet.put("face", x, y, (r, g, b))
            else:
                sheet.put("pane", x - FRAME, y - FRAME, (r, g, b))
    return sheet


def main():
    path = os.path.join(BLOCK, NAME + ".png")
    if "fresh" not in sys.argv and os.path.exists(path):
        print("left alone; pass fresh to repaint the reactor viewport sheets")
        return
    viewport().save(path)
    for stage in range(1, STAGES + 1):
        cracked(stage).save(os.path.join(BLOCK, "%s_crack_%d.png" % (NAME, stage)))
    print("painted the reactor viewport sheets")


if __name__ == "__main__":
    main()

import json
import os
import sys

from plant_style import BLOCK, BRASS, GLASS, ROOT, Sheet, source

STATES = ("off", "standby", "running", "tripped")
LAMPS = ("green", "amber", "red")
LIT = {"off": None, "standby": "amber", "running": "green", "tripped": "red"}
TRACE = {"standby": (None, 2, 2, 2, 2, 2, 2, None), "running": (3, 3, 2, 1, 2, 3, 3, 2), "tripped": (None, 2, 2, 2, 2, 2, 2, None)}
ISLANDS = {
    "panel": (0, 0, 12, 12),
    "wall_top": (12, 0, 16, 2),
    "wall_bottom": (12, 2, 16, 2),
    "wall_side": (28, 4, 2, 12),
    "bezel": (0, 12, 10, 7),
    "edge_top": (0, 19, 10, 1),
    "edge_bottom": (0, 20, 10, 1),
    "edge_inner": (0, 21, 10, 1),
    "edge_left": (10, 12, 1, 5),
    "edge_right": (11, 12, 1, 5),
    "edge_inner_v": (10, 17, 1, 5),
    "glass_off": (12, 4, 8, 5),
    "glass_standby": (20, 4, 8, 5),
    "glass_running": (12, 9, 8, 5),
    "glass_tripped": (20, 9, 8, 5),
}
for i, lamp in enumerate(LAMPS):
    ISLANDS["lamp_" + lamp] = (14 + 3 * i, 15, 2, 2)
    ISLANDS["lamp_" + lamp + "_lit"] = (14 + 3 * i, 18, 2, 2)


def palette(name):
    tones = json.load(open(os.path.join(ROOT, "docs", "art", "palette.json"), encoding="utf-8"))[name]
    return [tuple(int(t[i:i + 2], 16) for i in (1, 3, 5)) for t in tones]


def plate(sheet, casing):
    edge, shadow, fill = casing.getpixel((3, 2)), casing.getpixel((4, 2)), casing.getpixel((4, 4))
    sheet.fill("panel", 0, 0, 11, 11, fill)
    for i in range(12):
        sheet.put("panel", i, 0, edge)
        sheet.put("panel", 0, i, shadow)
    for x in range(16):
        for y in range(2):
            sheet.put("wall_top", x, y, edge)
            sheet.put("wall_bottom", x, y, casing.getpixel((8, 8)))
    for y in range(12):
        sheet.put("wall_side", 0, y, shadow)
        sheet.put("wall_side", 1, y, shadow)


def bezel(sheet):
    sheet.outline("bezel", 0, 0, 9, 6, BRASS[2])
    for x in range(10):
        sheet.put("bezel", x, 0, BRASS[3])
    for y in range(7):
        sheet.put("bezel", 0, y, BRASS[3])
    for x in range(1, 10):
        sheet.put("bezel", x, 6, BRASS[1])
    for y in range(1, 7):
        sheet.put("bezel", 9, y, BRASS[1])
    sheet.put("bezel", 0, 0, BRASS[4])
    sheet.put("bezel", 9, 6, BRASS[0])
    for x in range(10):
        sheet.put("edge_top", x, 0, BRASS[3])
        sheet.put("edge_bottom", x, 0, BRASS[0])
        sheet.put("edge_inner", x, 0, BRASS[0])
    for y in range(5):
        sheet.put("edge_left", 0, y, BRASS[2])
        sheet.put("edge_right", 0, y, BRASS[0])
        sheet.put("edge_inner_v", 0, y, BRASS[0])


def displays(sheet):
    tones = {lamp: palette(lamp) for lamp in LAMPS}
    for state in STATES:
        island = "glass_" + state
        sheet.fill(island, 0, 0, 7, 4, GLASS[0])
        if LIT[state] is None:
            sheet.put(island, 1, 0, GLASS[1])
            sheet.put(island, 0, 1, GLASS[1])
            continue
        for x, y in enumerate(TRACE[state]):
            if y is not None:
                sheet.put(island, x, y, tones[LIT[state]][3])
    for lamp in LAMPS:
        t = tones[lamp]
        for x, y, c in ((0, 0, t[1]), (1, 0, t[0]), (0, 1, t[0]), (1, 1, t[0])):
            sheet.put("lamp_" + lamp, x, y, c)
        for x, y, c in ((0, 0, t[4]), (1, 0, t[3]), (0, 1, t[3]), (1, 1, t[2])):
            sheet.put("lamp_" + lamp + "_lit", x, y, c)


def controller():
    sheet = Sheet(32, ISLANDS)
    plate(sheet, source("reactor_vessel_casing"))
    bezel(sheet)
    displays(sheet)
    return sheet


def main():
    if "fresh" not in sys.argv:
        print("left alone; pass fresh to repaint the controller sheet")
        return
    controller().save(os.path.join(BLOCK, "reactor_controller.png"))
    print("painted the controller sheet")


if __name__ == "__main__":
    main()

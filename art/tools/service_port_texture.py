import os
import sys

from PIL import Image

from plant_style import BLOCK, BRASS, GLASS, Sheet, ramp, source

OUT = os.path.join(BLOCK, "reactor_service_port.png")
ISLANDS = {
    "ring_top": (0, 0, 16, 16),
    "wall_long": (0, 16, 16, 3),
    "wall_short": (16, 0, 8, 3),
    "floor": (16, 4, 8, 8),
    "bar_top": (24, 4, 2, 8),
    "bar_side": (16, 13, 8, 3),
    "boss_top": (27, 4, 4, 4),
    "boss_side": (27, 9, 4, 3),
    "door_top": (0, 20, 3, 8),
    "door_edge": (4, 20, 8, 1),
}


def paint():
    steel = source("steel_block")
    casing = source("reactor_vessel_casing")
    tone = ramp(steel)
    shadow, fill = casing.getpixel((4, 2)), casing.getpixel((4, 4))
    sheet = Sheet(32, ISLANDS)
    sheet.paste("ring_top", steel)
    for island, width in (("wall_long", 16), ("wall_short", 8), ("bar_side", 8)):
        for x in range(width):
            sheet.put(island, x, 0, fill)
            sheet.put(island, x, 1, shadow)
            sheet.put(island, x, 2, shadow)
    sheet.fill("floor", 0, 0, 7, 7, GLASS[0])
    for y in range(8):
        sheet.put("bar_top", 0, y, tone[1] if y in (0, 7) else tone[5])
        sheet.put("bar_top", 1, y, tone[1] if y in (0, 7) else tone[4])
    sheet.fill("boss_top", 0, 0, 3, 3, BRASS[2])
    sheet.fill("boss_top", 1, 1, 2, 2, GLASS[0])
    sheet.put("boss_top", 0, 0, BRASS[4])
    sheet.put("boss_top", 3, 3, BRASS[0])
    sheet.fill("boss_side", 0, 0, 3, 2, BRASS[1])
    for y in range(8):
        for x, c in enumerate((casing.getpixel((0, 1)), fill, shadow)):
            sheet.put("door_top", x, y, c)
    for x in range(8):
        sheet.put("door_edge", x, 0, shadow)
    return sheet


def main():
    if "fresh" not in sys.argv and Image.open(OUT).getpixel((31, 31))[3] == 0:
        print("left alone; pass fresh to repaint", OUT)
        return
    paint().save(OUT)
    print("painted", OUT)


if __name__ == "__main__":
    main()

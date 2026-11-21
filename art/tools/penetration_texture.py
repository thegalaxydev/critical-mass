import os
import sys

from plant_style import BLOCK, ramp, source

OUT = os.path.join(BLOCK, "encased_reinforced_pipe_end.png")
FLANGE = (3, 12)
BORE = (5, 10)


def paint():
    img = source("shielding_concrete")
    steel = ramp(source("reinforced_pipe"))
    lo, hi = FLANGE
    for y in range(lo, hi + 1):
        for x in range(lo, hi + 1):
            light = x == lo or y == lo
            dark = x == hi or y == hi
            tone = steel[5] if light and not dark else steel[1] if dark and not light else steel[3]
            img.putpixel((x, y), tuple(tone[:3]) + (255,))
    lo, hi = BORE
    for y in range(lo, hi + 1):
        for x in range(lo, hi + 1):
            rim = x in (lo, hi) or y in (lo, hi)
            img.putpixel((x, y), tuple((steel[0] if rim else (12, 14, 17))[:3]) + (255,))
    for x, y in ((3, 3), (12, 3), (3, 12), (12, 12)):
        img.putpixel((x, y), tuple(steel[6][:3]) + (255,))
    return img


def main():
    if os.path.exists(OUT) and "fresh" not in sys.argv:
        print("left alone; pass fresh to repaint", OUT)
        return
    paint().save(OUT)
    print("painted", OUT)


if __name__ == "__main__":
    main()

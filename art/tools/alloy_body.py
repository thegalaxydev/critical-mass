import os
import sys

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
PARTS = os.path.join(ROOT, "src", "main", "resources", "assets", "criticalmass", "textures", "block", "dashboard_part")
SHEETS = ("body_atlas", "big_body_atlas", "slim_body_atlas")
FRAME = (0, 0, 14, 24)
ROW = 24
RAMP = [(0, (12, 17, 22)), (40, (30, 38, 46)), (80, (62, 72, 84)), (120, (112, 124, 137)), (160, (160, 171, 183)), (200, (201, 210, 219)), (255, (238, 243, 248))]


def alloy(pixel):
    r, g, b, a = pixel
    if a == 0:
        return pixel
    l = 0.299 * r + 0.587 * g + 0.114 * b
    for (l0, c0), (l1, c1) in zip(RAMP, RAMP[1:]):
        if l <= l1:
            t = (l - l0) / (l1 - l0)
            return tuple(round(c0[i] + (c1[i] - c0[i]) * t) for i in range(3)) + (a,)
    return RAMP[-1][1] + (a,)


def main():
    fresh = "fresh" in sys.argv[1:]
    for name in SHEETS:
        path = os.path.join(PARTS, name + ".png")
        img = Image.open(path).convert("RGBA")
        x0, y0, w, h = FRAME
        if not fresh and any(img.getpixel((x0 + x, y0 + ROW + y))[3] for y in range(h) for x in range(w)):
            continue
        for y in range(h):
            for x in range(w):
                img.putpixel((x0 + x, y0 + ROW + y), alloy(img.getpixel((x0 + x, y0 + y))))
        img.save(path)
        print(name, "alloy row added")


if __name__ == "__main__":
    main()

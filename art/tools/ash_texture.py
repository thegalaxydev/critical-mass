import os
import random
import sys

from PIL import Image

from plant_style import BLOCK

ASH = [(58, 66, 52), (74, 84, 64), (92, 104, 78), (112, 126, 92)]
GLOW = [(126, 214, 74), (188, 255, 120)]
SPECKS = ((3, 2), (11, 4), (6, 8), (13, 10), (2, 12), (9, 14), (14, 1), (7, 3), (1, 6), (10, 7), (4, 10), (12, 13), (6, 15), (15, 15))
PULSE = (1.0, 0.8, 0.55, 0.8)
FRAME_TIME = 16


def ash():
    rnd = random.Random(41)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            roll = rnd.random()
            img.putpixel((x, y), ASH[1 if roll < 0.55 else 2 if roll < 0.85 else 0] + (255,))
    for y in range(0, 16, 4):
        for x in range(0, 16, 4):
            ox, oy = (x + rnd.randrange(3)) % 16, (y + rnd.randrange(3)) % 16
            img.putpixel((ox, oy), ASH[3] + (255,))
    return img


def glow():
    sheet = Image.new("RGBA", (16, 16 * len(PULSE)))
    for frame in range(len(PULSE)):
        for i, (x, y) in enumerate(SPECKS):
            level = PULSE[(frame + i) % len(PULSE)]
            sheet.putpixel((x, y + 16 * frame), tuple(round(v * level) for v in GLOW[i % 2]) + (255,))
    return sheet


def main():
    if "fresh" not in sys.argv:
        print("left alone; pass fresh to repaint the fallout ash")
        return
    ash().save(os.path.join(BLOCK, "fallout_ash.png"))
    glow().save(os.path.join(BLOCK, "fallout_ash_glow.png"))
    with open(os.path.join(BLOCK, "fallout_ash_glow.png.mcmeta"), "w", encoding="utf8", newline="\n") as f:
        f.write('{\n "animation": {\n  "frametime": %d,\n  "interpolate": true\n }\n}\n' % FRAME_TIME)
    print("painted the fallout ash")


if __name__ == "__main__":
    main()

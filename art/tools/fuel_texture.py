import json
import math
import os
import sys

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "criticalmass", "textures", "fluid")
TONES = [(34, 24, 12), (52, 36, 16), (74, 52, 22), (100, 72, 30), (132, 98, 42)]
FRAMES = 16
ALPHA = 240


def tone(x, y, frame, size):
    wave = math.sin((x + 2 * y) * math.tau / size + frame * math.tau / FRAMES) + 0.6 * math.sin((3 * x - y) * math.tau / size - frame * math.tau / FRAMES)
    return TONES[max(0, min(len(TONES) - 1, int(round(1.6 + wave * 1.1))))]


def strip(size, drift):
    img = Image.new("RGBA", (size, size * FRAMES))
    for frame in range(FRAMES):
        for y in range(size):
            for x in range(size):
                img.putpixel((x, frame * size + y), tone(x % 16, (y - frame * drift) % 16, frame, 16) + (ALPHA,))
    return img


def main():
    if "fresh" not in sys.argv:
        print("left alone; pass fresh to repaint the fuel oil")
        return
    strip(16, 0).save(os.path.join(OUT, "fuel_oil_still.png"))
    strip(32, 2).save(os.path.join(OUT, "fuel_oil_flow.png"))
    json.dump({"animation": {"frametime": 4, "interpolate": True}}, open(os.path.join(OUT, "fuel_oil_still.png.mcmeta"), "w"))
    json.dump({"animation": {"frametime": 3}}, open(os.path.join(OUT, "fuel_oil_flow.png.mcmeta"), "w"))
    print("painted fuel oil")


if __name__ == "__main__":
    main()

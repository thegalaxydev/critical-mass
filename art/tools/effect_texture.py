import json
import math
import os
import random
import sys

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "criticalmass")
OUT = os.path.join(ASSETS, "textures", "particle")
DEFS = os.path.join(ASSETS, "particles")
PUFF_ALPHA = (0, 56, 128, 200)
SHEETS = {"steam": ["steam_0", "steam_1", "steam_2", "steam_3"], "plume": ["steam_0", "steam_1", "steam_2", "steam_3"], "oil_smoke": ["steam_0", "steam_1", "steam_2", "steam_3"],
          "foam": ["steam_0", "steam_1", "steam_2", "steam_3"],
          "arc_spark": ["spark"], "ember": ["spark"], "glow": ["glow"], "fireball": ["glow"], "shockwave": ["ring"], "cherenkov": ["glow"], "mote": ["mote"], "arc": ["streak"]}


def puff(seed):
    rng = random.Random(seed)
    size = 16
    lumps = [(8 + rng.uniform(-0.5, 0.5), 8 + rng.uniform(-0.5, 0.5), 3.6)]
    for _ in range(5):
        angle = rng.uniform(0, math.tau)
        reach = rng.uniform(1.4, 2.6)
        lumps.append((8 + math.cos(angle) * reach, 8 + math.sin(angle) * reach, rng.uniform(1.8, 2.6)))
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    for y in range(size):
        for x in range(size):
            depth = max(1 - math.hypot(x + 0.5 - cx, y + 0.5 - cy) / r for cx, cy, r in lumps)
            if depth <= 0:
                continue
            step = 1 if depth < 0.22 else 2 if depth < 0.5 else 3
            shade = 255 if x + y < 15 else 232 if x + y < 20 else 212
            img.putpixel((x, y), (shade, shade, shade, PUFF_ALPHA[step]))
    return img


def glow():
    size = 64
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    for y in range(size):
        for x in range(size):
            r = math.hypot(x + 0.5 - size / 2, y + 0.5 - size / 2) / (size / 2)
            if r < 1:
                img.putpixel((x, y), (255, 255, 255, round(255 * (1 - r) ** 2.2)))
    return img


def ring():
    size = 64
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    for y in range(size):
        for x in range(size):
            r = math.hypot(x + 0.5 - size / 2, y + 0.5 - size / 2) / (size / 2)
            edge = max(0.0, 1 - abs(r - 0.86) / 0.1)
            inner = max(0.0, 0.1 * (r / 0.86) ** 3) if r < 0.86 else 0
            a = max(edge ** 1.5, inner)
            if a > 0:
                img.putpixel((x, y), (255, 255, 255, round(255 * min(1, a))))
    return img


def streak():
    img = Image.new("RGBA", (16, 4), (0, 0, 0, 0))
    for x in range(16):
        a = math.exp(-((x + 0.5 - 8) / 2.6) ** 2)
        for y in range(4):
            img.putpixel((x, y), (255, 255, 255, round(255 * a)))
    return img


def dot(size, sharp):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    for y in range(size):
        for x in range(size):
            r = math.hypot(x + 0.5 - size / 2, y + 0.5 - size / 2) / (size / 2)
            if r < 1:
                img.putpixel((x, y), (255, 255, 255, round(255 * (1 - r) ** sharp)))
    return img


def spark():
    img = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
    for x, y, a in ((3, 3, 255), (4, 3, 255), (3, 4, 255), (4, 4, 255), (2, 3, 150), (5, 4, 150), (3, 2, 150), (4, 5, 150), (1, 3, 60), (6, 4, 60), (3, 1, 60), (4, 6, 60)):
        img.putpixel((x, y), (255, 255, 255, a))
    return img


def main():
    if "fresh" not in sys.argv:
        print("left alone; pass fresh to repaint the effect sprites")
        return
    os.makedirs(OUT, exist_ok=True)
    os.makedirs(DEFS, exist_ok=True)
    sprites = {"steam_%d" % i: puff(40 + i) for i in range(4)}
    sprites.update({"glow": glow(), "ring": ring(), "streak": streak(), "mote": dot(8, 1.4), "spark": spark()})
    for name, img in sprites.items():
        img.save(os.path.join(OUT, name + ".png"))
    for name, textures in SHEETS.items():
        with open(os.path.join(DEFS, name + ".json"), "w", newline="\n") as f:
            json.dump({"textures": ["criticalmass:" + t for t in textures]}, f, indent=2)
            f.write("\n")
    print("painted", len(sprites), "sprites and wrote", len(SHEETS), "particle definitions")


if __name__ == "__main__":
    main()

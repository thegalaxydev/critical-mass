import os
import random
import sys

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
TARGET = os.path.join(ROOT, "src", "main", "resources", "assets", "criticalmass", "textures", "entity")
NAME = "mutation_growths"
BONE = [(0x6f, 0x68, 0x53), (0x8f, 0x88, 0x6e), (0xb0, 0xa8, 0x8a), (0xc9, 0xc1, 0xa2), (0xe8, 0xe2, 0xc8)]
PUS = [(0x6a, 0x8a, 0x1c), (0x84, 0xa6, 0x24), (0x9c, 0xc0, 0x2a), (0xcb, 0xe4, 0x48), (0xee, 0xfa, 0x9a)]
HIDE = [(0x1d, 0x24, 0x16), (0x2b, 0x33, 0x20), (0x3b, 0x45, 0x2a), (0x4d, 0x5a, 0x36), (0x6b, 0x74, 0x47)]
FLESH = [(0x24, 0x0e, 0x12), (0x45, 0x19, 0x1e), (0x69, 0x29, 0x2b), (0x8c, 0x3f, 0x39), (0xae, 0x5f, 0x4e)]
QUADS = {(0, 0): (BONE, 2.6, 0.0), (32, 0): (PUS, 2.4, 0.55), (0, 32): (HIDE, 1.8, 0.0), (32, 32): (FLESH, 2.0, 0.0)}


def paint():
    rng = random.Random(4040)
    skin = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    glow = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    for (left, top), (tones, base, lit) in QUADS.items():
        above = [0] * 32
        for j in range(32):
            row = []
            while len(row) < 32:
                step = above[len(row)] if rng.random() < 0.6 else rng.choice((-1, 0, 0, 1))
                row += [step] * rng.choice((1, 2, 2, 3))
            row = row[:32]
            for i in range(32):
                level = base + row[i] + (0.6 if (i + j) % 7 == 0 and tones is not FLESH else 0)
                colour = tones[max(0, min(len(tones) - 1, int(round(level))))]
                skin.putpixel((left + i, top + j), colour + (255,))
                if lit:
                    glow.putpixel((left + i, top + j), tuple(int(v * lit) for v in colour) + (255,))
            above = row
    return skin, glow


def main():
    os.makedirs(TARGET, exist_ok=True)
    paths = [os.path.join(TARGET, NAME + ".png"), os.path.join(TARGET, NAME + "_glow.png")]
    if "fresh" not in sys.argv[1:] and all(os.path.exists(p) for p in paths):
        print("kept", paths[0], "(run with fresh to repaint)")
        return
    for image, path in zip(paint(), paths):
        image.save(path)
        print("painted", path)


if __name__ == "__main__":
    main()

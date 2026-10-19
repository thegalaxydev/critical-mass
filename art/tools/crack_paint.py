import json
import os
import shutil
import sys
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import art
import gen

DARK = ("steel", 0)
DEEP = ("ink", 0)
EDGE = ("iron", 5)
CHIP = ("steel", 1)
GLOW = ("amber", 3)
HOT = ("red", 2)

STAGES = {
    1: {
        DARK: [(4, 3), (5, 4), (5, 5), (6, 5), (7, 6), (11, 10), (12, 11), (10, 9)],
        EDGE: [(5, 3), (6, 4), (7, 5), (8, 6), (12, 10), (11, 9)],
    },
    2: {
        DARK: [(6, 6), (7, 6), (8, 9), (8, 10), (9, 11), (10, 11), (11, 12), (9, 4), (10, 4), (11, 5), (3, 2)],
        CHIP: [(7, 5), (9, 10), (6, 6)],
        EDGE: [(8, 6), (9, 9), (10, 10), (11, 11), (12, 12), (10, 3), (11, 4), (12, 5)],
    },
    3: {
        DARK: [(2, 10), (3, 10), (4, 11), (5, 11), (6, 12), (12, 2), (12, 3), (13, 4), (7, 7), (7, 8), (4, 4), (6, 4), (9, 12)],
        DEEP: [(5, 5), (6, 5), (8, 10), (9, 11)],
        GLOW: [(6, 5), (8, 10)],
        HOT: [(5, 5), (9, 11)],
        EDGE: [(3, 9), (4, 10), (5, 10), (6, 11), (13, 3), (14, 4)],
    },
}
ORDER = [EDGE, CHIP, DARK, DEEP, HOT, GLOW]


def render(stage):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for s in range(1, stage + 1):
        layer = STAGES[s]
        for key in ORDER:
            for (x, y) in layer.get(key, []):
                im.putpixel((x, y), art.P(*key))
    return im


if __name__ == "__main__":
    for stage in (1, 2, 3):
        im = render(stage)
        key = "casing_crack_%d" % stage
        final = os.path.join(art.ART, "final", key + ".png")
        im.save(final)
        art.preview(final, os.path.join(art.ART, "preview", key + ".png"), 16)
        json.dump({"src": None, "method": "Deterministic hand pixel crack overlay (tools/crack_paint.py); each stage adds to the previous one"}, open(os.path.join(art.ART, "final", key + ".json"), "w"))
        shutil.copyfile(final, os.path.join(gen.RES, "textures", "block", key + ".png"))
        print("crack", stage)

import json
import os
import shutil
import sys
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import art
import gen

KEYS = {
    ".": None, "O": ("brass", 1), "o": ("brass", 2), "b": ("brass", 3), "B": ("brass", 4), "H": ("brass", 5),
    "k": ("iron", 1), "i": ("iron", 2), "I": ("iron", 4), "J": ("iron", 5), "c": ("fabric", 5), "C": ("fabric", 3),
    "n": ("ink", 0), "q": ("rubber", 0), "g": ("rubber", 3), "m": ("rubber", 4), "h": ("iron", 5), "s": ("rubber", 1), "u": ("copper", 5), "v": ("copper", 3), "r": ("red", 2), "a": ("amber", 3), "R": ("rubber", 2),
}

SPRITES = {
    "sealing_gasket": ['................', '......qqqq......', '....qhhhhhmq....', '...qhhmmmmmmq...', '..qhmmuuuumggq..', '..hhmu....vggg..', '.qhmu......vssq.', '.qhmu......vssq.', '.qhmu......vssq.', '.qhmu......vssq.', '..mmmv....vsss..', '..qmggvvvvsssq..', '...qggssssssq...', '....qgsssssq....', '......qqqq......', '................'],
    "survey_meter": [
        "................",
        "....kkkk....kkk.",
        "...kJ..Jk...kJk.",
        "...kI..Ik...kIk.",
        ".OOOOOOOOOO.kIk.",
        ".OHBcccBBBO.kIk.",
        ".OBcccnrabO.kIk.",
        ".OBCcncc bO.kok.".replace(" ", "b"),
        ".OBcnCccbIO..R..",
        ".ObBcccbbiO..R..",
        ".ObbbbbbbbO.R...",
        ".ObboooobbO.R...",
        ".OobbbbbbbRR....",
        ".OoooooooiO.....",
        "..OOOOOOOO......",
        "................",
    ],
}


VERSION = {"sealing_gasket": 2, "survey_meter": 3}


def render(name):
    rows = SPRITES[name]
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        assert len(row) == 16, (name, y, len(row))
        for x, ch in enumerate(row):
            k = KEYS[ch]
            if k:
                im.putpixel((x, y), art.P(*k))
    return im


if __name__ == "__main__":
    for name in sys.argv[1:]:
        im = render(name)
        key = "item__" + name
        final = os.path.join(art.ART, "final", key + ".png")
        im.save(final)
        art.preview(final, os.path.join(art.ART, "preview", key + ".png"), 16)
        json.dump({"src": "item__%s-v%d.png" % (name, VERSION.get(name, 3)), "method": "Generated design (v%d) redrawn" % VERSION.get(name, 3) + " with a deterministic hand pixel pass (tools/pixel_edit.py)"}, open(os.path.join(art.ART, "final", key + ".json"), "w"))
        shutil.copyfile(final, os.path.join(gen.RES, "textures", "item", name + ".png"))
        print("pixel-edited", name)

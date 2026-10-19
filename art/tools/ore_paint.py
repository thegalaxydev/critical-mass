import io
import os
import sys
import zipfile

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
VANILLA = os.path.join(ROOT, "build", "moddev", "artifacts", "neoforge-21.1.262-client-extra-aka-minecraft-resources.jar")
RAW = os.path.join(ROOT, "src", "main", "resources", "assets", "criticalmass", "textures", "item", "raw_uranium.png")
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "criticalmass", "textures", "block")

PAIRS = {"uranium_ore": ("stone", "gold_ore"), "deepslate_uranium_ore": ("deepslate", "deepslate_gold_ore")}


def vanilla(z, name):
    return Image.open(io.BytesIO(z.read("assets/minecraft/textures/block/%s.png" % name))).convert("RGBA")


def luminance(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def palette():
    img = Image.open(RAW).convert("RGBA")
    cols = {}
    for px in img.get_flattened_data():
        if px[3] < 200:
            continue
        cols[px[:3]] = cols.get(px[:3], 0) + 1
    ranked = sorted(cols, key=luminance)
    if len(ranked) > 6:
        step = (len(ranked) - 1) / 5.0
        ranked = [ranked[round(i * step)] for i in range(6)]
    return ranked


def paint(z, name, base_name, mask_name, ramp):
    base = vanilla(z, base_name)
    mask = vanilla(z, mask_name)
    out = base.copy()
    spots = [(x, y) for y in range(16) for x in range(16) if mask.getpixel((x, y))[:3] != base.getpixel((x, y))[:3]]
    lums = [luminance(mask.getpixel(p)) for p in spots]
    lo, hi = min(lums), max(lums)
    for p, l in zip(spots, lums):
        t = 0 if hi == lo else (l - lo) / (hi - lo)
        c = ramp[min(len(ramp) - 1, int(t * len(ramp)))]
        out.putpixel(p, c + (255,))
    out.save(os.path.join(OUT, name + ".png"))
    return len(spots)


def main():
    ramp = palette()
    with zipfile.ZipFile(VANILLA) as z:
        for name, (base, mask) in PAIRS.items():
            print(name, paint(z, name, base, mask, ramp), "ore pixels")


if __name__ == "__main__":
    sys.exit(main())

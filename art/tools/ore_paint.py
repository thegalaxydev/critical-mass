import colorsys
import io
import os
import sys
import zipfile

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
VANILLA = os.path.join(ROOT, "build", "moddev", "artifacts", "neoforge-21.1.262-client-extra-aka-minecraft-resources.jar")
RAW = os.path.join(ROOT, "src", "main", "resources", "assets", "criticalmass", "textures", "item", "raw_uranium.png")
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "criticalmass", "textures", "block")


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


def brighten(ramp, value=1.3, sat=1.35):
    out = []
    for i, c in enumerate(ramp):
        if i == 0:
            out.append(c)
            continue
        h, s, v = colorsys.rgb_to_hsv(*[x / 255 for x in c])
        r, g, b = colorsys.hsv_to_rgb(h, min(1, s * sat), min(1, v * value))
        out.append((round(r * 255), round(g * 255), round(b * 255)))
    return out


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
    return out, spots


def transplant(z, name, base_name, source, spots):
    out = vanilla(z, base_name)
    for p in spots:
        out.putpixel(p, source.getpixel(p))
    out.save(os.path.join(OUT, name + ".png"))
    return len(spots)


def main():
    ramp = brighten(palette())
    with zipfile.ZipFile(VANILLA) as z:
        deep, spots = paint(z, "deepslate_uranium_ore", "deepslate", "deepslate_gold_ore", ramp)
        print("deepslate_uranium_ore", len(spots), "ore pixels")
        print("uranium_ore", transplant(z, "uranium_ore", "stone", deep, spots), "ore pixels from the deepslate ore")


if __name__ == "__main__":
    sys.exit(main())

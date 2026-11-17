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


def brighten(ramp, value=1.18, sat=1.7):
    out = []
    for c in ramp:
        h, s, v = colorsys.rgb_to_hsv(*[x / 255 for x in c])
        r, g, b = colorsys.hsv_to_rgb(h, min(1, s * sat), min(1, v * value))
        out.append((round(r * 255), round(g * 255), round(b * 255)))
    return out


def grey(c):
    return max(c[:3]) - min(c[:3]) < 12


def paint(z, name, base_name, mask_name, ramp):
    base = vanilla(z, base_name)
    mask = vanilla(z, mask_name)
    out = mask.copy()
    metal = sorted({mask.getpixel((x, y))[:3] for y in range(16) for x in range(16) if not grey(mask.getpixel((x, y)))}, key=luminance)
    for y in range(16):
        for x in range(16):
            c = mask.getpixel((x, y))[:3]
            if c in metal:
                t = metal.index(c) / max(1, len(metal) - 1)
                out.putpixel((x, y), ramp[round(t * (len(ramp) - 1))] + (255,))
    out.save(os.path.join(OUT, name + ".png"))
    return sum(1 for y in range(16) for x in range(16) if out.getpixel((x, y)) != base.getpixel((x, y)))


def main():
    if "fresh" not in sys.argv:
        print("left alone; pass fresh to repaint the uranium ores")
        return
    ramp = brighten(palette()[1:])
    with zipfile.ZipFile(VANILLA) as z:
        for name, base, mask in (("uranium_ore", "stone", "iron_ore"), ("deepslate_uranium_ore", "deepslate", "deepslate_iron_ore")):
            print(name, paint(z, name, base, mask, ramp), "pixels differ from", base)


if __name__ == "__main__":
    sys.exit(main())

import io
import os
import sys
import zipfile

from PIL import Image

from plant_style import BLOCK, ROOT

VANILLA = os.path.join(ROOT, "build", "moddev", "artifacts", "neoforge-21.1.262-client-extra-aka-minecraft-resources.jar")
TEXTURES = os.path.dirname(BLOCK)
FLUIDS = ["primary_coolant", "contaminated_coolant", "boric_acid", "fuel_oil"]


def luminance(c):
    return c[0] * 0.299 + c[1] * 0.587 + c[2] * 0.114


def ramp(fluid):
    still = Image.open(os.path.join(TEXTURES, "fluid", fluid + "_still.png")).convert("RGBA").crop((0, 0, 16, 16))
    tones = sorted({still.getpixel((x, y))[:3] for y in range(16) for x in range(16)}, key=luminance)
    return [tones[round(i * (len(tones) - 1) / 3)] for i in range(4)]


def bucket(empty, water, fluid):
    out = empty.copy()
    wet = [(x, y) for y in range(16) for x in range(16) if water.getpixel((x, y)) != empty.getpixel((x, y))]
    ranks = sorted({luminance(water.getpixel(p)) for p in wet})
    tones = ramp(fluid)
    for p in wet:
        rank = ranks.index(luminance(water.getpixel(p))) / max(1, len(ranks) - 1)
        out.putpixel(p, tones[round(rank * 3)] + (255,))
    return out


def main():
    with zipfile.ZipFile(VANILLA) as jar:
        empty = Image.open(io.BytesIO(jar.read("assets/minecraft/textures/item/bucket.png"))).convert("RGBA")
        water = Image.open(io.BytesIO(jar.read("assets/minecraft/textures/item/water_bucket.png"))).convert("RGBA")
    for fluid in FLUIDS:
        path = os.path.join(TEXTURES, "item", fluid + "_bucket.png")
        if os.path.exists(path) and ("fresh" not in sys.argv or (any(f in sys.argv for f in FLUIDS) and fluid not in sys.argv)):
            print("left alone; pass fresh to repaint", os.path.basename(path))
            continue
        bucket(empty, water, fluid).save(path)
        print("painted", os.path.basename(path))


if __name__ == "__main__":
    main()

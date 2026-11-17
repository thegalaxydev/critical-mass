import io
import os
import sys
import zipfile

from PIL import Image

from plant_style import BLOCK, CRUST, EMBER, ROOT

VANILLA = os.path.join(ROOT, "build", "moddev", "artifacts", "neoforge-21.1.262-client-extra-aka-minecraft-resources.jar")
SHEETS = {"charred_log": "oak_log", "charred_log_top": "oak_log_top"}
CRACKS = ((5, 3), (5, 4), (6, 5), (6, 6), (10, 9), (10, 10), (11, 11), (3, 12), (3, 13))


def charred(source, bark):
    out = Image.new("RGBA", source.size)
    tones = sorted({source.getpixel((x, y))[:3] for y in range(16) for x in range(16)}, key=sum)
    for y in range(16):
        for x in range(16):
            rank = tones.index(source.getpixel((x, y))[:3]) / max(1, len(tones) - 1)
            out.putpixel((x, y), CRUST[min(len(CRUST) - 1, 1 + round(rank * 3))] + (255,))
    if bark:
        for x, y in CRACKS:
            out.putpixel((x, y), EMBER[0] + (255,))
    return out


def main():
    if "fresh" not in sys.argv:
        print("left alone; pass fresh to repaint the charred log")
        return
    with zipfile.ZipFile(VANILLA) as jar:
        for name, source in SHEETS.items():
            img = Image.open(io.BytesIO(jar.read("assets/minecraft/textures/block/%s.png" % source))).convert("RGBA")
            charred(img, name == "charred_log").save(os.path.join(BLOCK, name + ".png"))
    print("painted the charred log")


if __name__ == "__main__":
    main()

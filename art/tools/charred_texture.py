import io
import os
import sys
import zipfile

from PIL import Image

from plant_style import BLOCK, CRUST, EMBER, ROOT

VANILLA = os.path.join(ROOT, "build", "moddev", "artifacts", "neoforge-21.1.262-client-extra-aka-minecraft-resources.jar")
ITEM = os.path.join(os.path.dirname(BLOCK), "item")
TIMBER = [(27, 23, 22), (38, 32, 30), (50, 42, 38), (64, 53, 47), (80, 66, 57)]
SHEETS = {
    "charred_log": ("block/oak_log", CRUST, BLOCK), "charred_log_top": ("block/oak_log_top", CRUST, BLOCK),
    "stripped_charred_log": ("block/stripped_oak_log", TIMBER, BLOCK), "stripped_charred_log_top": ("block/stripped_oak_log_top", TIMBER, BLOCK),
    "charred_planks": ("block/oak_planks", TIMBER, BLOCK), "charred_door_top": ("block/oak_door_top", TIMBER, BLOCK),
    "charred_door_bottom": ("block/oak_door_bottom", TIMBER, BLOCK), "charred_trapdoor": ("block/oak_trapdoor", TIMBER, BLOCK),
    "charred_door": ("item/oak_door", TIMBER, ITEM),
}
CRACKS = ((5, 3), (5, 4), (6, 5), (6, 6), (10, 9), (10, 10), (11, 11), (3, 12), (3, 13))


def charred(source, tones_out, bark):
    out = Image.new("RGBA", source.size)
    w, h = source.size
    tones = sorted({source.getpixel((x, y))[:3] for y in range(h) for x in range(w) if source.getpixel((x, y))[3]}, key=sum)
    for y in range(h):
        for x in range(w):
            pixel = source.getpixel((x, y))
            if not pixel[3]:
                continue
            rank = tones.index(pixel[:3]) / max(1, len(tones) - 1)
            out.putpixel((x, y), tones_out[min(len(tones_out) - 1, 1 + round(rank * 3))] + (pixel[3],))
    if bark:
        for x, y in CRACKS:
            out.putpixel((x, y), EMBER[0] + (255,))
    return out


def main():
    if "fresh" not in sys.argv:
        print("left alone; pass fresh to repaint the charred wood")
        return
    chosen = [name for name in SHEETS if name in sys.argv] or list(SHEETS)
    with zipfile.ZipFile(VANILLA) as jar:
        for name in chosen:
            source, tones, folder = SHEETS[name]
            img = Image.open(io.BytesIO(jar.read("assets/minecraft/textures/%s.png" % source))).convert("RGBA")
            charred(img, tones, name == "charred_log").save(os.path.join(folder, name + ".png"))
            print("painted", name)


if __name__ == "__main__":
    main()

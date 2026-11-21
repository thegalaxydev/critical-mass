import io
import os
import sys
import zipfile

from PIL import Image

from plant_style import BLOCK, CRUST, EMBER, ROOT

VANILLA = os.path.join(ROOT, "build", "moddev", "artifacts", "neoforge-21.1.262-client-extra-aka-minecraft-resources.jar")
TEXTURES = os.path.dirname(BLOCK)
TIMBER = [(27, 23, 22), (38, 32, 30), (50, 42, 38), (64, 53, 47), (80, 66, 57)]
BOARD = [(58, 49, 44), (76, 63, 55), (96, 80, 68), (116, 97, 82), (136, 115, 97)]
SHEETS = {
    "block/charred_log": ("block/oak_log", CRUST), "block/charred_log_top": ("block/oak_log_top", CRUST),
    "block/stripped_charred_log": ("block/stripped_oak_log", TIMBER), "block/stripped_charred_log_top": ("block/stripped_oak_log_top", TIMBER),
    "block/charred_planks": ("block/oak_planks", TIMBER), "block/charred_door_top": ("block/oak_door_top", TIMBER),
    "block/charred_door_bottom": ("block/oak_door_bottom", TIMBER), "block/charred_trapdoor": ("block/oak_trapdoor", TIMBER),
    "item/charred_door": ("item/oak_door", TIMBER), "item/charred_sign": ("item/oak_sign", TIMBER), "item/charred_hanging_sign": ("item/oak_hanging_sign", TIMBER),
    "entity/signs/charred": ("entity/signs/oak", BOARD), "entity/signs/hanging/charred": ("entity/signs/hanging/oak", BOARD),
    "gui/hanging_signs/charred": ("gui/hanging_signs/oak", BOARD),
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
    chosen = [name for name in SHEETS if name in sys.argv or os.path.basename(name) in sys.argv] or list(SHEETS)
    with zipfile.ZipFile(VANILLA) as jar:
        for name in chosen:
            source, tones = SHEETS[name]
            img = Image.open(io.BytesIO(jar.read("assets/minecraft/textures/%s.png" % source))).convert("RGBA")
            path = os.path.join(TEXTURES, name.replace("/", os.sep) + ".png")
            os.makedirs(os.path.dirname(path), exist_ok=True)
            charred(img, tones, name == "block/charred_log").save(path)
            print("painted", name)


if __name__ == "__main__":
    main()

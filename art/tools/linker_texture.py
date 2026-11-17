import os
import sys

from PIL import Image

from plant_style import BLOCK

ITEM = os.path.join(os.path.dirname(BLOCK), "item")
ICON = (
    "................",
    "..........r.....",
    "..........s.....",
    "..........s.....",
    ".....eeeeeS.....",
    "....ebbbbbbd....",
    "....ebGGGGbd....",
    "....ebGggGbd....",
    "....ebbbbbbd....",
    "....efbYbRfd....",
    "....efbybmfd....",
    "....effffffd....",
    "....effffffd....",
    ".....dddddd.....",
    "................",
    "................",
)


def tones():
    dosimeter = Image.open(os.path.join(ITEM, "dosimeter.png")).convert("RGBA")
    meter = Image.open(os.path.join(ITEM, "survey_meter.png")).convert("RGBA")
    pick = {"e": dosimeter.getpixel((5, 2)), "b": dosimeter.getpixel((5, 3)), "d": dosimeter.getpixel((11, 3)), "f": dosimeter.getpixel((5, 8)),
            "G": dosimeter.getpixel((6, 4)), "g": dosimeter.getpixel((7, 5)), "Y": dosimeter.getpixel((7, 8)), "y": dosimeter.getpixel((6, 10)),
            "R": meter.getpixel((4, 10)), "m": meter.getpixel((11, 9)), "r": meter.getpixel((4, 10))}
    pick["s"] = (126, 128, 133, 255)
    pick["S"] = (88, 90, 95, 255)
    return pick


def linker():
    pick = tones()
    img = Image.new("RGBA", (16, 16))
    for y, row in enumerate(ICON):
        for x, key in enumerate(row):
            if key != ".":
                img.putpixel((x, y), tuple(pick[key][:3]) + (255,))
    return img


def main():
    if "fresh" not in sys.argv:
        print("left alone; pass fresh to repaint the plant linker icon")
        return
    linker().save(os.path.join(ITEM, "plant_linker.png"))
    print("painted the plant linker icon")


if __name__ == "__main__":
    main()

import os

from PIL import Image

from pipe_paint import OUT, ROOT, alloy, luminance

NAMES = ["reinforced_valve", "reinforced_valve_open", "reinforced_valve_closed"]


def grey(p):
    r, g, b = p[:3]
    return max(r, g, b) - min(r, g, b) <= 8 and max(r, g, b) < 200


def main():
    for name in NAMES:
        src = Image.open(os.path.join(ROOT, "art", "originals", name + ".png")).convert("RGBA")
        out = src.copy()
        for y in range(src.height):
            for x in range(src.width):
                p = src.getpixel((x, y))
                if p[3] > 0 and grey(p):
                    out.putpixel((x, y), alloy(luminance(p)) + (p[3],))
        out.save(os.path.join(OUT, name + ".png"))
    print("painted " + ", ".join(NAMES))


if __name__ == "__main__":
    main()

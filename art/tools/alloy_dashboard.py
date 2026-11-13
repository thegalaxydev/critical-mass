import glob
import json
import math
import os
import zipfile

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
TEXTURES = os.path.join(ROOT, "src", "main", "resources", "assets", "criticalmass", "textures", "block")
SHAPES = ("single", "start", "middle", "end")
MODEL = "assets/loconautics/models/block/dashboard/gray/%s.json"
SIZE = 64
SCALE = SIZE / 16.0
SLOPE = math.tan(math.radians(22.5))
SLOPE_END = 14.0


def geometry():
    jars = sorted(glob.glob(os.path.join(ROOT, "libs", "loconautics-*.jar")))
    if not jars:
        raise SystemExit("put the Create: Loconautics jar in libs/ first")
    with zipfile.ZipFile(jars[-1]) as jar:
        return [e for shape in SHAPES for e in json.loads(jar.read(MODEL % shape))["elements"]]


def nine(tile, w, h, b):
    tw, th = tile.size
    b = max(0, min(b, (w - 1) // 2, (h - 1) // 2))
    out = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    inner = tile.crop((2, 2, tw - 2, th - 2))
    for y in range(b, h - b, inner.height):
        for x in range(b, w - b, inner.width):
            out.paste(inner.crop((0, 0, min(inner.width, w - b - x), min(inner.height, h - b - y))), (x, y))
    for i in range(b):
        for x in range(w):
            sx = 0 if x == 0 else tw - 1 if x == w - 1 else 2 + (x - 1) % (tw - 4)
            out.putpixel((x, i), tile.getpixel((sx, i)))
            out.putpixel((x, h - 1 - i), tile.getpixel((sx, th - 1 - i)))
        for y in range(h):
            sy = 0 if y == 0 else th - 1 if y == h - 1 else 2 + (y - 1) % (th - 4)
            out.putpixel((i, y), tile.getpixel((i, sy)))
            out.putpixel((w - 1 - i, y), tile.getpixel((tw - 1 - i, sy)))
    return out


def kind(element, direction):
    size = [abs(element["to"][i] - element["from"][i]) for i in range(3)]
    turned = element.get("rotation", {}).get("angle", 0) != 0
    if size[0] < 0.2:
        return "trim" if turned else "side"
    if turned:
        return "top" if direction == "up" else "lip"
    if size[2] < 0.2:
        return "back"
    return "base" if min(element["from"][1], element["to"][1]) == 0 else "front"


def paint(what, w, h, casing):
    if what == "top":
        plate = casing.crop((3, 3, 13, 7))
        out = Image.new("RGBA", (w, h))
        for y in range(0, h, plate.height):
            for x in range(0, w, plate.width):
                out.paste(plate, (x, y))
        for x in range(w):
            out.putpixel((x, 0), casing.getpixel((4, 7)))
            out.putpixel((x, h - 1), casing.getpixel((4, 2)))
        return out
    if what in ("lip", "trim"):
        out = Image.new("RGBA", (w, h))
        for y in range(h):
            for x in range(w):
                out.putpixel((x, y), casing.getpixel((1 + x % 14, min(y, 1) if h > 1 else 1)))
        return out
    if what == "front":
        out = nine(casing, w, h, 1)
        for y in range(2, h - 1, 2):
            for x in range(1, w - 1):
                out.putpixel((x, y), casing.getpixel((12, 7)))
        return out
    return nine(casing, w, h, 2 if what == "back" else 1)


def main():
    casing = Image.open(os.path.join(TEXTURES, "reactor_vessel_casing.png")).convert("RGBA").crop((0, 0, 16, 16))
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    for element in geometry():
        (x0, y0, z0), (x1, y1, z1) = element["from"], element["to"]
        y0, y1, z0, z1 = min(y0, y1), max(y0, y1), min(z0, z1), max(z0, z1)
        for direction, face in element["faces"].items():
            u0, v0, u1, v1 = [c * SCALE for c in face["uv"]]
            left, top, right, bottom = math.floor(min(u0, u1) + 1e-6), math.floor(min(v0, v1) + 1e-6), math.ceil(max(u0, u1) - 1e-6), math.ceil(max(v0, v1) - 1e-6)
            w, h = right - left, bottom - top
            if w <= 0 or h <= 0:
                continue
            what = kind(element, direction)
            tile = paint(what, w, h, casing)
            if what == "side" and y1 > 12.5:
                for py in range(h):
                    for px in range(w):
                        tu = (left + px + 0.5 - u0) / (u1 - u0)
                        tv = (top + py + 0.5 - v0) / (v1 - v0)
                        y = y1 - tv * (y1 - y0)
                        z = z1 - tu * (z1 - z0) if direction == "east" else z0 + tu * (z1 - z0)
                        if y + 0.5 > 16.0 - max(0.0, SLOPE_END - z) * SLOPE + 0.01:
                            tile.putpixel((px, py), (0, 0, 0, 0))
            img.paste(tile, (left, top))
    img.save(os.path.join(TEXTURES, "reactor_alloy_dashboard.png"))
    print("reactor_alloy_dashboard.png")


if __name__ == "__main__":
    main()

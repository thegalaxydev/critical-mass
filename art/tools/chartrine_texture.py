import os
import sys

from PIL import Image

from plant_style import BLOCK, ROOT

SOURCE = os.path.join(ROOT, "art", "generated", "chartrine-v1.png")
CELLS = 32
SIZE = 16
TONES = 9
VARIANTS = 4


def sampled():
    src = Image.open(SOURCE).convert("RGB")
    step = src.width / CELLS
    out = Image.new("RGB", (CELLS, CELLS))
    for y in range(CELLS):
        for x in range(CELLS):
            out.putpixel((x, y), src.getpixel((int((x + 0.5) * step), int((y + 0.5) * step))))
    return out


def ramp(img):
    flat = img.quantize(TONES, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE)
    palette = flat.getpalette()[:TONES * 3]
    tones = sorted({tuple(palette[i:i + 3]) for i in range(0, len(palette), 3)}, key=sum)
    return tones


def nearest(tones, colour):
    return min(range(len(tones)), key=lambda i: sum((tones[i][k] - colour[k]) ** 2 for k in range(3)))


def halved(img, tones):
    grid = [[nearest(tones, img.getpixel((x, y))) for x in range(CELLS)] for y in range(CELLS)]
    out = []
    for y in range(SIZE):
        row = []
        for x in range(SIZE):
            four = sorted(grid[2 * y + dy][2 * x + dx] for dy in (0, 1) for dx in (0, 1))
            lightest, darkest = four[3], four[0]
            row.append(darkest if four.count(darkest) >= 2 and darkest <= 1 else lightest if four.count(lightest) >= 2 and lightest >= len(tones) - 2 else four[(x + y) % 2 + 1])
        out.append(row)
    return out


def darkened(index, variant, top):
    if variant == 0:
        return index
    if variant == 1:
        return index - 1 if index >= top - 1 else index
    return max(0, index - (variant - 1) - (1 if index >= top - 1 else 0))


def tile(grid, tones, variant):
    out = Image.new("RGBA", (SIZE, SIZE))
    for y in range(SIZE):
        for x in range(SIZE):
            out.putpixel((x, y), tones[darkened(grid[y][x], variant, len(tones) - 1)] + (255,))
    return out


def main():
    img = sampled()
    tones = ramp(img)
    grid = halved(img, tones)
    preview = next((a for a in sys.argv[1:] if a.endswith(".png")), None)
    if preview:
        sheet = Image.new("RGBA", (VARIANTS * SIZE * 3, SIZE * 3))
        for v in range(VARIANTS):
            t = tile(grid, tones, v)
            for y in range(3):
                for x in range(3):
                    sheet.paste(t, (v * SIZE * 3 + x * SIZE, y * SIZE))
        sheet.resize((sheet.width * 6, sheet.height * 6), Image.NEAREST).save(preview)
    for v in range(VARIANTS):
        path = os.path.join(BLOCK, "chartrine_%d.png" % v)
        if os.path.exists(path) and "fresh" not in sys.argv:
            print("left alone; pass fresh to rebuild", os.path.basename(path))
            continue
        tile(grid, tones, v).save(path)
        print("wrote", os.path.basename(path))
    print(len(tones), "tones", ["#%02x%02x%02x" % t for t in tones])


if __name__ == "__main__":
    main()

import io
import os
import zipfile

from PIL import Image

from pipe_models import create_jar

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "criticalmass", "textures", "block")
RIM = [(19, 22, 32, 32)]


def luminance(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def steel_ramp(count):
    dark, light = (20, 22, 26), (146, 152, 158)
    return [tuple(round(dark[i] + (light[i] - dark[i]) * k / (count - 1)) for i in range(3)) for k in range(count)]


def in_rect(x, y, rects):
    return any(x0 <= x < x1 and y0 <= y < y1 for x0, y0, x1, y1 in rects)


def recolor(src, keep):
    tones = sorted({src.getpixel((x, y))[:3] for y in range(src.height) for x in range(src.width)
                    if src.getpixel((x, y))[3] > 0 and copper(src.getpixel((x, y)))}, key=luminance)
    steel = steel_ramp(len(tones))
    table = dict(zip(tones, steel))
    out = src.copy()
    for y in range(src.height):
        for x in range(src.width):
            p = src.getpixel((x, y))
            if p[3] == 0 or in_rect(x, y, keep) or p[:3] not in table:
                continue
            out.putpixel((x, y), table[p[:3]] + (p[3],))
    return out


def copper(p):
    r, g, b = p[:3]
    return r > g and r > b and r - b > 20


def main():
    with zipfile.ZipFile(create_jar()) as z:
        pipes = Image.open(io.BytesIO(z.read("assets/create/textures/block/pipes.png"))).convert("RGBA")
        connected = Image.open(io.BytesIO(z.read("assets/create/textures/block/pipes_connected.png"))).convert("RGBA")
    recolor(pipes, RIM).save(os.path.join(OUT, "reinforced_pipe.png"))
    recolor(connected, []).save(os.path.join(OUT, "reinforced_pipe_connected.png"))
    print("painted reinforced_pipe and reinforced_pipe_connected")


if __name__ == "__main__":
    main()

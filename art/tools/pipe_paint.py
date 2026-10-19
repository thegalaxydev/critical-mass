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


def in_rect(x, y, rects):
    return any(x0 <= x < x1 and y0 <= y < y1 for x0, y0, x1, y1 in rects)


def copper_tones(*images):
    tones = set()
    for src in images:
        for y in range(src.height):
            for x in range(src.width):
                p = src.getpixel((x, y))
                if p[3] > 0 and copper(p):
                    tones.add(p[:3])
    return sorted(tones, key=luminance)


def shared_table(*images):
    tones = copper_tones(*images)
    lo, hi = luminance(tones[0]), luminance(tones[-1])
    dark, light = (24, 26, 30), (150, 156, 162)
    table = {}
    for t in tones:
        k = (luminance(t) - lo) / (hi - lo)
        table[t] = tuple(round(dark[i] + (light[i] - dark[i]) * k) for i in range(3))
    return table


def recolor(src, keep, table):
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
    table = shared_table(pipes, connected)
    recolor(pipes, RIM, table).save(os.path.join(OUT, "reinforced_pipe.png"))
    recolor(connected, [], table).save(os.path.join(OUT, "reinforced_pipe_connected.png"))
    print("painted reinforced_pipe and reinforced_pipe_connected")


if __name__ == "__main__":
    main()

import os

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
BLOCK = os.path.join(ROOT, "src", "main", "resources", "assets", "criticalmass", "textures", "block")
BRASS = [(114, 71, 49), (158, 105, 71), (206, 160, 90), (247, 203, 108), (255, 235, 140)]
RED = [(104, 26, 28), (158, 40, 36), (196, 62, 50)]
GLASS = [(14, 18, 24), (22, 30, 40), (120, 170, 190)]
CRUST = [(16, 14, 15), (27, 24, 25), (39, 35, 36), (53, 47, 47), (70, 61, 58)]
EMBER = [(116, 40, 20), (190, 70, 22), (244, 138, 44), (255, 208, 112), (255, 240, 190)]


def source(name):
    return Image.open(os.path.join(BLOCK, name + ".png")).convert("RGBA")


def ramp(img, steps=7):
    tones = set()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = img.getpixel((x, y))
            if a:
                tones.add((r, g, b))
    ordered = sorted(tones, key=lambda c: c[0] + c[1] + c[2])
    return [ordered[round(i * (len(ordered) - 1) / (steps - 1))] for i in range(steps)]


def nine(img, w, h, border=2):
    out = Image.new("RGBA", (w, h))
    sw, sh = img.size
    for y in range(h):
        for x in range(w):
            sx = x if x < border else sw - (w - x) if x >= w - border else border + (x - border) % (sw - 2 * border)
            sy = y if y < border else sh - (h - y) if y >= h - border else border + (y - border) % (sh - 2 * border)
            out.putpixel((x, y), img.getpixel((sx, sy)))
    return out


def panel(casing, w, h):
    light, dark, corner = casing.getpixel((0, 1)), casing.getpixel((15, 1)), casing.getpixel((0, 0))
    shadow, fill = casing.getpixel((4, 2)), casing.getpixel((4, 4))
    out = Image.new("RGBA", (w, h))
    for y in range(h):
        for x in range(w):
            edge_x, edge_y = x in (0, w - 1), y in (0, h - 1)
            if edge_x and edge_y:
                c = corner
            elif x == w - 1 or y == h - 1:
                c = dark
            elif x == 0 or y == 0:
                c = light
            elif x == 1 or y == 1:
                c = shadow
            else:
                c = fill
            out.putpixel((x, y), c)
    return out


class Sheet:
    def __init__(self, size, islands):
        self.img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        self.islands = islands

    def paste(self, island, img):
        x, y, w, h = self.islands[island]
        assert img.size == (w, h), (island, img.size, (w, h))
        self.img.paste(img, (x, y))

    def put(self, island, x, y, color):
        ox, oy, w, h = self.islands[island]
        assert 0 <= x < w and 0 <= y < h, (island, x, y)
        self.img.putpixel((ox + x, oy + y), tuple(color[:3]) + (255,))

    def outline(self, island, x0, y0, x1, y1, color):
        for x in range(x0, x1 + 1):
            self.put(island, x, y0, color)
            self.put(island, x, y1, color)
        for y in range(y0, y1 + 1):
            self.put(island, x0, y, color)
            self.put(island, x1, y, color)

    def fill(self, island, x0, y0, x1, y1, color):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.put(island, x, y, color)

    def save(self, path):
        import create_style
        create_style.enliven(self.img)
        self.img.save(path)

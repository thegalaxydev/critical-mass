import base64
import io
import json
import math
import os
import sys

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ORIGINALS = os.path.join(ROOT, "art", "originals")
BLOCKS = os.path.join(ROOT, "src", "main", "resources", "assets", "criticalmass", "textures", "block")
MODEL = os.path.join(ROOT, "art", "blockbench", "steam_turbine.bbmodel")
SHEET = os.path.join(ROOT, "art", "blockbench", "steam_turbine.png")
SIZE = 128

ISLANDS = {
    "casing_outer_front": (2, 8, 16, 16),
    "casing_outer_middle": (20, 8, 16, 13),
    "casing_outer_back": (38, 8, 16, 9),
    "casing_inner_front": (56, 8, 16, 16),
    "casing_inner_middle": (74, 8, 16, 13),
    "casing_inner_back": (92, 8, 16, 9),
    "back_plate": (110, 8, 16, 16),
    "casing_rim_front": (2, 34, 16, 4),
    "casing_rim_middle": (20, 34, 13, 4),
    "casing_rim_back": (35, 34, 9, 4),
    "shaft_side": (48, 33, 46, 8),
    "shaft_end": (95, 33, 8, 8),
    "rotor_edge_front": (110, 34, 2, 11),
    "rotor_edge_middle": (114, 34, 2, 8),
    "rotor_edge_back": (118, 34, 2, 5),
    "stator_edge_front": (122, 34, 1, 13),
    "stator_edge_middle": (124, 34, 1, 9),
    "stator_edge_back": (126, 34, 1, 6),
    "stator_plate_back": (2, 56, 15, 15),
    "rotor_plate_back": (19, 56, 13, 13),
    "window_front": (84, 56, 16, 16),
    "window_middle": (102, 56, 16, 13),
    "window_back": (84, 76, 16, 9),
    "window_reveal": (102, 80, 16, 4),
    "exhaust_side": (3, 87, 9, 8),
    "exhaust_end": (14, 86, 10, 10),
    "stator_plate_front": (2, 97, 31, 31),
    "rotor_plate_front": (35, 98, 29, 29),
    "stator_plate_middle": (66, 97, 23, 23),
    "rotor_plate_middle": (91, 97, 21, 21),
}
KEPT = ("exhaust_side", "exhaust_end")
RIM_BOLTS = {"front": (3, 6, 9, 12), "middle": (3, 6, 9), "back": (4,)}
PLATE_ROWS = {16: range(16), 13: (0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 14, 15), 9: (0, 1, 2, 3, 4, 5, 6, 14, 15)}
ROTORS = {"front": (7.2, 16, 0.55), "middle": (6.0, 12, 0.5), "back": (4.3, 8, 0.4)}
STATORS = {"front": (7.6, 14, -0.6), "middle": (7.2, 10, -0.5), "back": (0, 0, 0)}

S = [(0x4b, 0x56, 0x61), (0x56, 0x63, 0x70), (0x65, 0x71, 0x7c), (0x72, 0x80, 0x8e), (0x85, 0x8d, 0x9b), (0x9e, 0xa8, 0xb2), (0xb0, 0xba, 0xc4), (0xc1, 0xca, 0xd3), (0xd0, 0xd9, 0xe2)]
N = [(0x11, 0x17, 0x1c), (0x14, 0x1c, 0x23), (0x19, 0x21, 0x28), (0x1f, 0x27, 0x2e), (0x23, 0x2b, 0x33), (0x28, 0x30, 0x38), (0x2c, 0x34, 0x3d), (0x36, 0x3e, 0x44), (0x3f, 0x48, 0x50), (0x44, 0x4e, 0x56)]
GLASS_EDGE = (0x53, 0x6f, 0x75)
PANE = (61, 92, 99, 80)
GLINT = (0xbb, 0xd8, 0xd7, 255)


def load(path):
    return Image.open(path).convert("RGBA")


def check_palette(steel, casing, glass, pipe):
    found = set()
    for img in (steel, casing, glass, pipe):
        px = img.load()
        found.update(px[x, y][:3] for y in range(img.height) for x in range(img.width) if px[x, y][3] == 255)
    missing = [c for c in S + N + [GLASS_EDGE] if c not in found]
    assert not missing, "tones no longer in the source textures: %s" % missing


class Sheet:
    def __init__(self):
        self.img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
        self.px = self.img.load()

    def put(self, island, i, j, colour):
        x, y, w, h = ISLANDS[island]
        if 0 <= i < w and 0 <= j < h:
            self.px[x + i, y + j] = colour if len(colour) == 4 else colour + (255,)

    def fill(self, island, colour):
        x, y, w, h = ISLANDS[island]
        for j in range(h):
            for i in range(w):
                self.put(island, i, j, colour)


def lit(dx, dy):
    r = math.hypot(dx, dy)
    return 0 if r == 0 else (-dx - dy) / (r * math.sqrt(2))


def casing(sheet, steel, casing_tex):
    for section in ("front", "middle", "back"):
        name = "casing_outer_" + section
        w, h = ISLANDS[name][2], ISLANDS[name][3]
        for j, row in enumerate(PLATE_ROWS[h]):
            for i in range(w):
                sheet.put(name, i, j, steel.getpixel((i, row)))
        for i, j in ((2, 2), (13, 2), (2, h - 3), (13, h - 3)):
            sheet.put(name, i, j, S[0])
            sheet.put(name, i - 1, j - 1, S[8])
            sheet.put(name, i, j + 1, S[4])
        name = "casing_inner_" + section
        w, h = ISLANDS[name][2], ISLANDS[name][3]
        band = (3, 4, 4, 4, 5, 5, 6, 5, 5, 4, 4, 4)
        for j in range(h):
            for i in range(w):
                tone = N[band[(i + j) % 12]]
                if j == 0 or i == w - 1:
                    tone = N[2]
                elif j == h - 1 or i == 0:
                    tone = N[7]
                elif i % 4 == 3:
                    tone = N[3]
                sheet.put(name, i, j, tone)
        name = "casing_rim_" + section
        w = ISLANDS[name][2]
        for i in range(w):
            sheet.put(name, i, 0, S[8])
            sheet.put(name, i, 1, S[6])
            sheet.put(name, i, 2, S[5])
            sheet.put(name, i, 3, S[2])
        for i in RIM_BOLTS[section]:
            sheet.put(name, i, 1, S[1])
    x, y = ISLANDS["back_plate"][:2]
    sheet.img.paste(casing_tex, (x, y))
    for j in range(3, 13):
        for i in range(3, 13):
            edge = min(i - 3, j - 3, 12 - i, 12 - j)
            if edge == 0:
                tone = N[9] if i == 3 or j == 3 else N[6]
            elif edge == 1:
                tone = N[8] if i == 4 or j == 4 else N[5]
            else:
                tone = N[2] if (i in (5, 10) and j in (5, 10)) else N[0]
            sheet.put("back_plate", i, j, tone)


def shaft(sheet):
    rows = (N[7], N[9], N[8], N[8], N[7], N[6], N[5], N[4])
    w = ISLANDS["shaft_side"][2]
    for i in range(w):
        for j in range(8):
            tone = rows[j]
            if i % 8 == 0:
                tone = N[3]
            elif i % 8 == 1 and 1 <= j <= 6:
                tone = N[9]
            sheet.put("shaft_side", i, j, tone)
    for j in range(8):
        for i in range(8):
            edge = min(i, j, 7 - i, 7 - j)
            if edge == 0:
                tone = N[9] if i == 0 or j == 0 else N[6]
            elif edge == 1:
                tone = N[5]
            else:
                tone = N[2] if (i in (2, 5) and j in (2, 5)) else N[0]
            sheet.put("shaft_end", i, j, tone)


def edges(sheet):
    for section in ("front", "middle", "back"):
        name = "rotor_edge_" + section
        for j in range(ISLANDS[name][3]):
            sheet.put(name, 0, j, S[7])
            sheet.put(name, 1, j, S[3] if j % 2 else S[5])
        sheet.fill("stator_edge_" + section, S[2])


def windows(sheet):
    for section in ("front", "middle", "back"):
        name = "window_" + section
        w, h = ISLANDS[name][2], ISLANDS[name][3]
        for j in range(h):
            for i in range(w):
                if 3 <= j < h - 3 and 2 <= i < w - 2:
                    sheet.put(name, i, j, PANE)
                    continue
                if j == 0 or i == 0:
                    tone = S[8]
                elif j == h - 1 or i == w - 1:
                    tone = S[3]
                elif j in (2, h - 3) or i in (1, w - 2):
                    tone = GLASS_EDGE if (2 <= j <= h - 3 and 1 <= i <= w - 2) else S[6]
                else:
                    tone = S[6]
                sheet.put(name, i, j, tone)
        for i, j in ((1, 1), (w - 2, 1), (1, h - 2), (w - 2, h - 2)):
            sheet.put(name, i, j, S[1])
        sheet.put(name, 11, 4, GLINT)
        if h - 6 >= 5:
            sheet.put(name, 10, 5, GLINT)
    for j, tone in enumerate((S[4], S[3], S[2], S[1])):
        for i in range(ISLANDS["window_reveal"][2]):
            sheet.put("window_reveal", i, j, tone)


def rotor(sheet, section):
    name = "rotor_plate_" + section
    size = ISLANDS[name][2]
    hub, count, sweep = ROTORS[section]
    c = size / 2
    outer = c - 0.2
    shroud = c - 1.7
    for j in range(size):
        for i in range(size):
            dx, dy = i + 0.5 - c, j + 0.5 - c
            r = math.hypot(dx, dy)
            light = lit(dx, dy)
            if r > outer:
                continue
            if r > shroud:
                sheet.put(name, i, j, S[max(2, min(7, round(4.5 + 2.5 * light)))])
            elif r <= hub:
                if r > hub - 1.1:
                    sheet.put(name, i, j, S[6] if light > 0.2 else (S[1] if light < -0.2 else S[3]))
                else:
                    sheet.put(name, i, j, S[4] if light > 0.45 else (S[2] if light < -0.55 else S[3]))
            else:
                sector = 2 * math.pi / count
                angle = math.atan2(dy, dx) - sweep * (r - hub) / max(0.1, shroud - hub)
                t = (angle % sector) / sector
                width = min(0.62, 1.7 / (r * sector))
                if t < width:
                    part = t / width
                    sheet.put(name, i, j, S[8] if part < 0.34 else (S[6] if part < 0.7 else S[3]))
    if hub > 6:
        for k in range(4):
            a = math.pi / 4 + k * math.pi / 2
            sheet.put(name, int(c + math.cos(a) * (hub - 2.3)), int(c + math.sin(a) * (hub - 2.3)), S[1])


def stator(sheet, section):
    name = "stator_plate_" + section
    size = ISLANDS[name][2]
    seal, count, sweep = STATORS[section]
    c = size / 2
    bore = 5.9
    for j in range(size):
        for i in range(size):
            dx, dy = i + 0.5 - c, j + 0.5 - c
            r = math.hypot(dx, dy)
            flat = max(abs(dx), abs(dy), (abs(dx) + abs(dy)) / math.sqrt(2))
            light = lit(dx, dy)
            if flat > c or r < bore:
                continue
            if flat > c - 2 or count == 0:
                if flat > c - 1:
                    tone = S[1] if light < 0 else S[3]
                elif r < bore + 1 and count == 0:
                    tone = S[4] if light > 0 else S[1]
                else:
                    tone = S[2]
                sheet.put(name, i, j, tone)
            elif r <= seal:
                sheet.put(name, i, j, S[4] if (r > seal - 1 and light > 0.1) else (S[1] if r > seal - 1 else S[2]))
            else:
                sector = 2 * math.pi / count
                angle = math.atan2(dy, dx) - sweep * (r - seal) / max(0.1, c - 2 - seal)
                t = (angle % sector) / sector
                width = min(0.7, 2.6 / (r * sector))
                part = t / width
                sheet.put(name, i, j, N[1] if t >= width else (N[7] if part < 0.3 else (N[5] if part < 0.72 else N[3])))


def keep(sheet, old):
    for name in KEPT:
        x, y, w, h = ISLANDS[name]
        sheet.img.paste(old.crop((x, y, x + w, y + h)), (x, y))


def check_model(model):
    for e in model["elements"]:
        for f, face in e["faces"].items():
            if face.get("texture") is None:
                continue
            u = face["uv"]
            x0, x1 = sorted((u[0], u[2]))
            y0, y1 = sorted((u[1], u[3]))
            assert any(x0 >= x - 1e-6 and y0 >= y - 1e-6 and x1 <= x + w + 1e-6 and y1 <= y + h + 1e-6 for x, y, w, h in ISLANDS.values()), "%s %s maps outside the known islands: %s" % (e["name"], f, u)


def main():
    if "fresh" not in sys.argv[1:]:
        print("The sheet is kept as it is. Run with 'fresh' to repaint it; that replaces any hand edits except the exhaust duct islands.")
        return
    steel = load(os.path.join(ORIGINALS, "steel_block.png"))
    casing_tex = load(os.path.join(ORIGINALS, "reactor_vessel_casing.png"))
    glass = load(os.path.join(ORIGINALS, "shielded_glass.png"))
    pipe = load(os.path.join(BLOCKS, "reinforced_pipe.png"))
    check_palette(steel, casing_tex, glass, pipe)
    model = json.load(open(MODEL, encoding="utf-8"))
    check_model(model)
    sheet = Sheet()
    casing(sheet, steel, casing_tex)
    shaft(sheet)
    edges(sheet)
    windows(sheet)
    for section in ("front", "middle", "back"):
        rotor(sheet, section)
        stator(sheet, section)
    keep(sheet, load(SHEET))
    buf = io.BytesIO()
    sheet.img.save(buf, format="PNG")
    with open(SHEET, "wb") as f:
        f.write(buf.getvalue())
    model["textures"][0]["source"] = "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode("ascii")
    with open(MODEL, "w", encoding="utf-8", newline="") as f:
        f.write(json.dumps(model, separators=(",", ":")))
    print("painted", SHEET)


if __name__ == "__main__":
    main()

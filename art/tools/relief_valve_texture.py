import os
import sys

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
BLOCK = os.path.join(ROOT, "src", "main", "resources", "assets", "criticalmass", "textures", "block")
PIPE = os.path.join(BLOCK, "reinforced_pipe.png")
OUT = os.path.join(BLOCK, "relief_valve.png")
ISLANDS = {
    "flange_top": (0, 0, 8, 8),
    "flange_side": (0, 9, 8, 1),
    "bonnet_side": (9, 0, 6, 3),
    "bonnet_top": (16, 0, 6, 6),
    "cap_side": (9, 4, 4, 1),
    "cap_top": (9, 6, 4, 4),
    "lever_top": (14, 6, 3, 1),
    "lever_side": (14, 8, 3, 1),
    "lever_end": (14, 10, 1, 1),
}
BRASS = [(114, 71, 49), (158, 105, 71), (206, 160, 90), (247, 203, 108), (255, 235, 140)]
RED = [(104, 26, 28), (158, 40, 36), (196, 62, 50)]


def steel():
    pipe = Image.open(PIPE).convert("RGBA")
    tones = {}
    for y in range(pipe.height):
        for x in range(pipe.width):
            r, g, b, a = pipe.getpixel((x, y))
            if a:
                tones[(r, g, b)] = tones.get((r, g, b), 0) + 1
    ramp = sorted(tones, key=lambda c: c[0] + c[1] + c[2])
    picks = [ramp[round(i * (len(ramp) - 1) / 6)] for i in range(7)]
    return picks


def paint():
    s = steel()
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))

    def put(island, x, y, color):
        ox, oy, w, h = ISLANDS[island]
        assert 0 <= x < w and 0 <= y < h, (island, x, y)
        img.putpixel((ox + x, oy + y), color + (255,))

    def plate(island, light, mid, dark, core=None):
        _, _, w, h = ISLANDS[island]
        for y in range(h):
            for x in range(w):
                edge_light = x == 0 or y == 0
                edge_dark = x == w - 1 or y == h - 1
                c = light if edge_light and not edge_dark else dark if edge_dark and not edge_light else mid
                if core is not None and 0 < x < w - 1 and 0 < y < h - 1:
                    c = core
                put(island, x, y, c)

    plate("flange_top", s[5], s[4], s[2], s[1])
    for x, y in ((0, 0), (7, 0), (0, 7), (7, 7)):
        put("flange_top", x, y, BRASS[2])
    put("flange_top", 0, 0, BRASS[3])
    put("flange_top", 7, 7, BRASS[1])
    for x, c in enumerate((s[2], s[4], s[5], s[4], s[4], s[5], s[4], s[2])):
        put("flange_side", x, 0, c)
    for x in range(6):
        rib = x in (1, 4)
        put("bonnet_side", x, 0, s[2] if x in (0, 5) else s[6] if rib else s[5])
        put("bonnet_side", x, 1, (BRASS[1], BRASS[2], BRASS[3], BRASS[2], BRASS[2], BRASS[1])[x])
        put("bonnet_side", x, 2, s[1] if x in (0, 5) else s[4] if rib else s[3])
    plate("bonnet_top", s[6], s[4], s[2], s[1])
    for x, c in enumerate((BRASS[1], BRASS[3], BRASS[2], BRASS[0])):
        put("cap_side", x, 0, c)
    plate("cap_top", BRASS[3], BRASS[2], BRASS[1], s[0])
    put("cap_top", 0, 0, BRASS[4])
    put("cap_top", 3, 3, BRASS[0])
    put("cap_top", 2, 2, (max(s[0][0], 21), max(s[0][1], 29), max(s[0][2], 36)))
    for x, c in enumerate((s[3], RED[1], RED[2])):
        put("lever_top", x, 0, c)
    for x, c in enumerate((s[2], RED[0], RED[1])):
        put("lever_side", x, 0, c)
    put("lever_end", 0, 0, RED[1])
    return img


def main():
    if "fresh" not in sys.argv and os.path.exists(OUT) and Image.open(OUT).size == (32, 32):
        print("left alone; pass fresh to repaint", OUT)
        return
    paint().save(OUT)
    print("painted", OUT)


if __name__ == "__main__":
    main()

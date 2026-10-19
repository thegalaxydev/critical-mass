import io
import os
import sys
import zipfile

from PIL import Image

from pipe_models import create_jar

HERE = os.path.dirname(os.path.abspath(__file__))
MOD = os.path.dirname(os.path.dirname(HERE))
OUT = os.path.join(MOD, "src", "main", "resources", "assets", "criticalmass", "textures", "block")
BLOCK = "assets/create/textures/block/"

BRASS_TO_IRON = {
    (0x59, 0x24, 0x24): (36, 37, 41),
    (0x72, 0x47, 0x31): (58, 59, 64),
    (0x9e, 0x69, 0x47): (88, 90, 95),
    (0xce, 0xa0, 0x5a): (126, 128, 133),
    (0xf7, 0xcb, 0x6c): (152, 154, 159),
    (0xff, 0xeb, 0x8c): (174, 176, 181),
    (0xff, 0xf9, 0xbf): (198, 200, 204),
}
STEEL = [(23, 25, 28), (30, 33, 38), (35, 38, 43), (39, 43, 48), (48, 54, 57), (53, 59, 62), (58, 64, 67), (68, 75, 76)]
IRON = [(36, 37, 41), (58, 59, 64), (88, 90, 95), (126, 128, 133), (152, 154, 159), (174, 176, 181), (198, 200, 204)]
BRASS = [(0x59, 0x24, 0x24), (0x72, 0x47, 0x31), (0x9e, 0x69, 0x47), (0xce, 0xa0, 0x5a), (0xf7, 0xcb, 0x6c), (0xff, 0xeb, 0x8c), (0xff, 0xf9, 0xbf)]
COPPER = [(0x3a, 0x1c, 0x14), (0x5a, 0x2a, 0x1d), (0x83, 0x40, 0x2c), (0xa7, 0x5a, 0x40), (0xc8, 0x74, 0x56), (0xe3, 0x82, 0x6c), (0xf4, 0xa9, 0x8f)]
HOLE = (12, 12, 14)
RED = [(74, 19, 16), (125, 31, 24), (184, 50, 42), (224, 87, 63), (255, 138, 106)]
BLUE = [(15, 47, 74), (22, 70, 110), (31, 100, 150), (58, 140, 192), (108, 184, 224)]
GREEN = [(23, 58, 26), (36, 96, 42), (63, 148, 67), (108, 199, 106), (168, 234, 160)]
AMBER = [(90, 48, 8), (148, 82, 14), (216, 138, 28), (255, 190, 74), (255, 227, 154)]
SCREEN = [(10, 22, 16), (14, 34, 22), (20, 48, 30)]


class Jar:
    def __init__(self):
        self.z = zipfile.ZipFile(create_jar())

    def tex(self, name):
        return Image.open(io.BytesIO(self.z.read(BLOCK + name + ".png"))).convert("RGBA")


def remap(img, table):
    out = img.copy()
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            p = px[x, y]
            if p[:3] in table:
                px[x, y] = table[p[:3]] + (p[3],)
    return out


def put(img, x, y, c):
    img.putpixel((x, y), tuple(c) + (255,))


def rect(img, x0, y0, x1, y1, c):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            put(img, x, y, c)


def bevel(img, x0, y0, x1, y1, ramp, base, raised=True):
    rect(img, x0, y0, x1, y1, ramp[base])
    hi, lo = (ramp[min(len(ramp) - 1, base + 1)], ramp[max(0, base - 1)]) if raised else (ramp[max(0, base - 1)], ramp[min(len(ramp) - 1, base + 1)])
    for x in range(x0, x1 + 1):
        put(img, x, y0, hi)
        put(img, x, y1, lo)
    for y in range(y0, y1 + 1):
        put(img, x0, y, hi)
        put(img, x1, y, lo)


def casing_frame(jar):
    return remap(jar.tex("railway_casing"), BRASS_TO_IRON)


def panel(img, x0, y0, x1, y1):
    rect(img, x0, y0, x1, y1, STEEL[4])
    for x in range(x0, x1 + 1):
        put(img, x, y0, STEEL[0])
    for y in range(y0, y1 + 1):
        put(img, x0, y, STEEL[0])
    for x in range(x0 + 1, x1 + 1):
        put(img, x, y0 + 1, STEEL[7])
        put(img, x, y1, STEEL[3])
    for y in range(y0 + 1, y1 + 1):
        put(img, x0 + 1, y, STEEL[7])
        put(img, x1, y, STEEL[3])


def rivet(img, x, y):
    put(img, x, y, IRON[5])
    put(img, x + 1, y, IRON[2])
    put(img, x, y + 1, IRON[2])
    put(img, x + 1, y + 1, IRON[1])


def vessel_casing(jar):
    img = casing_frame(jar)
    for x in range(3, 13):
        put(img, x, 7, STEEL[0])
        put(img, x, 8, STEEL[6])
    put(img, 3, 8, STEEL[3])
    put(img, 12, 8, STEEL[3])
    return img


def controller(jar):
    img = casing_frame(jar)
    bevel(img, 3, 3, 12, 12, BRASS, 3)
    rect(img, 4, 4, 11, 9, SCREEN[0])
    for x in range(4, 12):
        put(img, x, 4, (6, 12, 9))
    for x in range(4, 12):
        put(img, x, 9, SCREEN[1])
    trace = [7, 7, 6, 6, 5, 6, 7, 6]
    for i, y in enumerate(trace):
        put(img, 4 + i, y, GREEN[3])
        if y < 8:
            put(img, 4 + i, y + 1, SCREEN[2])
    put(img, 11, 6, GREEN[4])
    for i, ramp in enumerate((GREEN, AMBER, RED)):
        x = 5 + i * 2
        put(img, x, 11, ramp[3])
        put(img, x + 1, 11, ramp[1])
    return img


def disc(img, cx, cy, r, color_at):
    for y in range(int(cy - r - 1), int(cy + r + 2)):
        for x in range(int(cx - r - 1), int(cx + r + 2)):
            d = ((x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2) ** 0.5
            if d <= r:
                c = color_at(x, y, d)
                if c:
                    put(img, x, y, c)


def service_port(jar):
    img = casing_frame(jar)
    rect(img, 3, 3, 12, 12, STEEL[2])
    def hatch(x, y, d):
        if d > 4.4:
            return BRASS[4] if x + y < 16 else BRASS[2]
        if d > 3.6:
            return BRASS[3] if x + y < 16 else BRASS[1]
        return STEEL[5] if x + y < 15 else STEEL[3]
    disc(img, 8, 8, 5.2, hatch)
    for k in range(4, 12):
        put(img, k, 7, IRON[4])
        put(img, k, 8, IRON[2])
        put(img, 7, k, IRON[4])
        put(img, 8, k, IRON[2])
    put(img, 7, 7, IRON[5])
    put(img, 8, 8, IRON[1])
    put(img, 7, 8, IRON[3])
    put(img, 8, 7, IRON[3])
    return img


def signal_interface(jar):
    img = casing_frame(jar)
    bevel(img, 3, 3, 12, 12, BRASS, 3)
    rect(img, 4, 4, 11, 11, STEEL[1])
    for x in range(4, 12):
        put(img, x, 4, STEEL[0])
    crystal = {(7, 5): 4, (8, 5): 3, (6, 6): 4, (7, 6): 3, (8, 6): 3, (9, 6): 2, (6, 7): 3, (7, 7): 3, (8, 7): 2, (9, 7): 1,
               (6, 8): 2, (7, 8): 2, (8, 8): 1, (9, 8): 1, (7, 9): 1, (8, 9): 0}
    for (x, y), i in crystal.items():
        put(img, x, y, RED[i])
    for y in (10, 11):
        put(img, 5, y, BRASS[2])
        put(img, 10, y, BRASS[2])
    return img


def port_face(img, ox, oy, collar, jar_frame):
    frame = jar_frame.copy()
    rect(frame, 3, 3, 12, 12, COPPER[3])
    bevel(frame, 3, 3, 12, 12, COPPER, 3)
    bevel(frame, 4, 4, 11, 11, collar, 2)
    rect(frame, 5, 5, 10, 10, HOLE)
    for x in range(5, 11):
        put(frame, x, 5, (4, 4, 5))
    img.paste(frame, (ox, oy))


def coolant_port(jar):
    img = Image.open(os.path.join(OUT, "primary_coolant_port.png")).convert("RGBA")
    frame = casing_frame(jar)
    port_face(img, 0, 0, BLUE, frame)
    port_face(img, 16, 0, RED, frame)
    nozzle = Image.new("RGBA", (8, 8))
    bevel(nozzle, 0, 0, 7, 7, COPPER, 3)
    bevel(nozzle, 1, 1, 6, 6, RED, 2)
    rect(nozzle, 2, 2, 5, 5, HOLE)
    img.paste(nozzle, (0, 16))
    side = Image.new("RGBA", (2, 8))
    for y in range(8):
        put(side, 0, y, COPPER[4])
        put(side, 1, y, COPPER[2])
    img.paste(side, (8, 16))
    top = Image.new("RGBA", (8, 2))
    for x in range(8):
        put(top, x, 0, COPPER[4])
        put(top, x, 1, COPPER[3])
    img.paste(top, (10, 16))
    return img


CONCRETE = [(84, 86, 88), (98, 100, 102), (112, 114, 116), (124, 126, 128), (136, 138, 140), (152, 154, 156)]
PORES = [(2, 3), (11, 2), (6, 6), (13, 9), (4, 12), (9, 13), (14, 14), (1, 9)]


def concrete(jar):
    img = Image.new("RGBA", (16, 16))
    for ty in (0, 8):
        for tx in (0, 8):
            rect(img, tx, ty, tx + 7, ty + 7, CONCRETE[3])
            for k in range(8):
                put(img, tx + k, ty, CONCRETE[4])
                put(img, tx, ty + k, CONCRETE[4])
                put(img, tx + k, ty + 7, CONCRETE[1])
                put(img, tx + 7, ty + k, CONCRETE[1])
            put(img, tx, ty, CONCRETE[5])
            put(img, tx + 7, ty + 7, CONCRETE[0])
    for x, y in PORES:
        if x % 8 not in (0, 7) and y % 8 not in (0, 7):
            put(img, x, y, CONCRETE[1])
    return img


HAZARD = [(40, 34, 20), (212, 154, 28), (240, 192, 58)]
GLASS_TINT = (54, 92, 98)


def shielded_glass(jar):
    src = jar.tex("palettes/tiled_glass")
    img = Image.new("RGBA", (16, 16))
    px = src.load()
    for y in range(16):
        for x in range(16):
            p = px[x, y]
            if p[3] == 0:
                img.putpixel((x, y), GLASS_TINT + (120,))
                continue
            l = (p[0] + p[1] + p[2]) / 3
            edge = x in (0, 15) or y in (0, 15)
            if edge:
                c = IRON[3] if (x == 0 or y == 0) else IRON[1]
            elif l > 225:
                c = (200, 226, 228)
            else:
                c = STEEL[6] if l > 180 else STEEL[4]
            img.putpixel((x, y), c + (255,))
    for (x, y) in ((2, 3), (3, 2), (10, 3), (11, 2), (2, 11), (3, 10), (13, 12), (12, 13)):
        if img.getpixel((x, y))[3] < 255:
            img.putpixel((x, y), (168, 210, 212, 170))
    return img


def shielded_hatch(jar):
    img = remap(jar.tex("train_trapdoor"), BRASS_TO_IRON)
    for x in range(3, 13):
        stripe = ((x + 7) // 2) % 2 == 0
        put(img, x, 7, HAZARD[2] if stripe else HAZARD[0])
        put(img, x, 8, HAZARD[1] if stripe else HAZARD[0])
    for x in range(3, 13):
        put(img, x, 6, STEEL[0])
        put(img, x, 9, STEEL[6])
    return img


TEXTURES = {
    "reactor_vessel_casing": vessel_casing,
    "reactor_controller": controller,
    "reactor_service_port": service_port,
    "reactor_signal_interface": signal_interface,
    "primary_coolant_port": coolant_port,
    "shielding_concrete": concrete,
    "shielded_glass": shielded_glass,
    "shielded_hatch": shielded_hatch,
}


def main(names, dest):
    jar = Jar()
    for n in names or TEXTURES:
        TEXTURES[n](jar).save(os.path.join(dest, n + ".png"))
        print("painted", n)


if __name__ == "__main__":
    main(sys.argv[2:], sys.argv[1] if len(sys.argv) > 1 else OUT)

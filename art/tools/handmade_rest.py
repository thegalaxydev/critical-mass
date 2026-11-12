import json
import os

from PIL import Image

from handmade import AMBER, BLUE, BRASS, COPPER, GREEN, HOLE, IRON, OUT, RED, STEEL, bevel, put, rect
from handmade_plant import PALE, disc, opening

MODELS = os.path.join(os.path.dirname(OUT), "..", "models", "block")
ANDESITE = [(0x40, 0x45, 0x43), (0x50, 0x53, 0x51), (0x60, 0x63, 0x5f), (0x70, 0x75, 0x70), (0x82, 0x87, 0x84), (0x9a, 0xa4, 0x9d), (0xa8, 0xb3, 0xab)]
LEAD = [(0x26, 0x28, 0x32), (0x36, 0x39, 0x46), (0x48, 0x4c, 0x5c), (0x5d, 0x62, 0x74), (0x76, 0x7c, 0x90), (0x93, 0x99, 0xad)]
CERAMIC = [(0x4a, 0x40, 0x38), (0x6b, 0x5f, 0x53), (0x8f, 0x82, 0x73), (0xb3, 0xa6, 0x94), (0xd2, 0xc7, 0xb4), (0xe9, 0xe1, 0xd0)]
WOOD = [(0x55, 0x3a, 0x1f), (0x61, 0x4b, 0x2e), (0x70, 0x52, 0x2e), (0x7a, 0x5a, 0x34), (0x82, 0x61, 0x3a), (0x88, 0x65, 0x39)]
HAZARD = [(40, 34, 20), (212, 154, 28), (240, 192, 58)]
CLEAR = (0, 0, 0, 0)
WELL = (14, 14, 17)
CAP = [(0x7a, 0x3e, 0x14), (0xd9, 0xad, 0x23), (0xe3, 0xc5, 0x4e)]
ROD = [(0x40, 0x42, 0x49), (0x52, 0x61, 0x79), (0x98, 0xa4, 0xb7), (0xc0, 0xd3, 0xd2)]


def faces(model, size):
    m = json.load(open(os.path.join(MODELS, model + ".json"), encoding="utf8"))
    k = size / 16
    out = []
    for e in m["elements"]:
        for d, f in e["faces"].items():
            if f.get("texture") != "#0":
                continue
            u0, v0, u1, v1 = f["uv"]
            x0, x1 = sorted((u0 * k, u1 * k))
            y0, y1 = sorted((v0 * k, v1 * k))
            out.append((e.get("name"), d, int(round(x0)), int(round(y0)), max(1, int(round(x1 - x0))), max(1, int(round(y1 - y0)))))
    return out


def tile(w, h):
    return Image.new("RGBA", (w, h))


def plate(ramp, base=3):
    def paint(w, h):
        img = tile(w, h)
        if w >= 3 and h >= 3:
            bevel(img, 0, 0, w - 1, h - 1, ramp, base)
        else:
            rect(img, 0, 0, w - 1, h - 1, ramp[base])
            for x in range(w):
                put(img, x, 0, ramp[min(len(ramp) - 1, base + 1)])
        return img
    return paint


def casing(frame, inner, interior=None):
    def paint(w, h):
        img = tile(w, h)
        bevel(img, 0, 0, w - 1, h - 1, frame, 3)
        if w >= 6 and h >= 6:
            bevel(img, 1, 1, w - 2, h - 2, frame, 4)
            rect(img, 2, 2, w - 3, h - 3, inner[1])
            for x in range(2, w - 2):
                put(img, x, 2, inner[0])
            for y in range(2, h - 2):
                put(img, 2, y, inner[0])
            for x in range(3, w - 2):
                put(img, x, h - 3, inner[2])
            if interior:
                interior(img, 3, 3, w - 4, h - 4)
        return img
    return paint


def port(frame, collar, flange=COPPER):
    def paint(w, h):
        img = casing(frame, STEEL)(w, h)
        s = min(w, h) - 4
        if s >= 4:
            opening(img, (w - s) // 2, (h - s) // 2, s, collar, flange)
        return img
    return paint


def socket(frame, inner=STEEL):
    def paint(w, h):
        img = casing(frame, inner)(w, h)
        cx, cy = w / 2, h / 2
        disc(img, cx, cy, min(w, h) / 2 - 2.2, lambda x, y, d: (BRASS[4] if x + y < cx + cy - 1 else BRASS[2]) if d > min(w, h) / 2 - 3.6 else
             (STEEL[0] if d > 1.6 else IRON[3]))
        return img
    return paint


def chevrons(frame, right=True):
    def paint(w, h):
        img = casing(frame, STEEL)(w, h)
        cy = h // 2
        for i, cx in enumerate((w // 2 - 2, w // 2 + 1)):
            for k in range(0, 3):
                x = cx + (k if right else -k)
                if 0 < x < w - 1:
                    put(img, x, cy - 2 + k, BRASS[4])
                    put(img, x, cy + 2 - k, BRASS[3])
        return img
    return paint


def ribs(ramp, every=3):
    def paint(w, h):
        img = tile(w, h)
        rect(img, 0, 0, w - 1, h - 1, ramp[3])
        for x in range(w):
            for y in range(h):
                if y % every == 0:
                    put(img, x, y, IRON[4] if y == 0 else IRON[2])
                elif x == 0:
                    put(img, x, y, ramp[5])
                elif x == w - 1:
                    put(img, x, y, ramp[1])
        return img
    return paint


def solid(c):
    def paint(w, h):
        img = tile(w, h)
        rect(img, 0, 0, w - 1, h - 1, c)
        return img
    return paint


def collar_ring(collar):
    def paint(w, h):
        img = tile(w, h)
        for y in range(h):
            for x in range(w):
                put(img, x, y, collar[3] if y == 0 else collar[2] if y < h - 1 else collar[1])
        return img
    return paint


def apply(name, size, spec, base=None):
    path = os.path.join(OUT, name + ".png")
    img = Image.open(path).convert("RGBA") if base is None else base
    if img.size != (size, size):
        img = img.resize((size, size), Image.NEAREST)
    for model in spec["models"]:
        for el, d, x, y, w, h in faces(model, size):
            painter = spec["faces"].get((el, d)) or spec["faces"].get((el, "*"))
            if painter:
                img.paste(painter(w, h), (x, y))
    return img


def pump(jar):
    spec = {"models": ["circulation_pump", "circulation_pump_vertical"], "faces": {
        ("outlet", "north"): port(COPPER, RED),
        ("inlet", "south"): port(COPPER, BLUE),
        ("outlet", "*"): collar_ring(COPPER),
        ("inlet", "*"): collar_ring(COPPER),
        ("pipe", "*"): collar_ring(COPPER),
        ("housing", "north"): casing(ANDESITE, STEEL),
        ("housing", "south"): casing(ANDESITE, STEEL),
        ("housing", "east"): chevrons(ANDESITE),
        ("housing", "west"): chevrons(ANDESITE),
        ("housing", "up"): socket(ANDESITE),
        ("housing", "down"): socket(ANDESITE),
    }}
    return apply("circulation_pump", 64, spec)


def turbine(jar):
    spec = {"models": ["steam_turbine"], "faces": {
        ("end_plate", "east"): socket(IRON),
        ("end_plate", "west"): socket(IRON),
        ("end_plate", "north"): plate(IRON, 3),
        ("end_plate", "up"): plate(IRON, 4),
        ("drum_a", "north"): ribs(STEEL),
        ("drum_a", "up"): ribs(STEEL),
        ("drum_a", "east"): casing(IRON, STEEL),
        ("steam_inlet", "up"): port(IRON, RED),
        ("steam_inlet", "north"): collar_ring(RED),
        ("exhaust", "down"): port(IRON, PALE),
        ("exhaust", "north"): collar_ring(PALE),
    }}
    img = apply("steam_turbine", 32, spec)
    for y in range(14, 26):
        for x in (19, 20):
            put(img, x, y, STEEL[0] if x == 19 else STEEL[1])
    return img


def rod_drive(jar):
    spec = {"models": ["control_rod_drive"], "faces": {
        ("base", "up"): casing(IRON, STEEL),
        ("base", "down"): plate(IRON, 2),
        ("base", "north"): plate(IRON, 3),
        ("housing", "east"): casing(ANDESITE, STEEL),
        ("housing", "north"): casing(ANDESITE, STEEL),
        ("housing", "south"): socket(ANDESITE),
        ("housing", "up"): casing(ANDESITE, STEEL),
        ("rack", "north"): rack_teeth,
        ("rack", "up"): plate(IRON, 4),
    }}
    return apply("control_rod_drive", 64, spec)


def rack_teeth(w, h):
    img = tile(w, h)
    for y in range(h):
        for x in range(w):
            put(img, x, y, IRON[5] if y % 2 == 0 else IRON[2])
    return img


def channel(window, top):
    side = casing(IRON, STEEL, window)(16, 16)
    t = casing(IRON, STEEL, top)(16, 16)
    img = Image.new("RGBA", (32, 32))
    img.paste(side, (0, 0))
    img.paste(t, (16, 0))
    return img


def rods(ramp, cols=(4, 7, 10)):
    def draw(img, x0, y0, x1, y1):
        rect(img, x0, y0, x1, y1, STEEL[0])
        for c in cols:
            for y in range(y0, y1 + 1):
                put(img, c, y, ramp[4])
                put(img, c + 1, y, ramp[2])
        for x in range(x0, x1 + 1):
            put(img, x, (y0 + y1) // 2, IRON[4])
    return draw


def rod_ends(ramp, ring=None):
    def draw(img, x0, y0, x1, y1):
        rect(img, x0, y0, x1, y1, STEEL[1])
        for cy in (4, 7, 10):
            for cx in (4, 7, 10):
                put(img, cx, cy, ramp[4])
                put(img, cx + 1, cy, ramp[3])
                put(img, cx, cy + 1, ramp[3])
                put(img, cx + 1, cy + 1, ramp[1])
    return draw


def fuel_channel(jar):
    return channel(rods(CERAMIC), rod_ends(CERAMIC))


def control_channel(jar):
    def window(img, x0, y0, x1, y1):
        rods(LEAD, cols=(4, 7))(img, x0, y0, x1, y1)
        for y in range(y0, y1 + 1):
            put(img, 11, y, BRASS[4] if y > 0 else BRASS[2])
            put(img, 12, y, BRASS[2])
        put(img, 11, 6, AMBER[3])
        put(img, 12, 6, AMBER[2])
    return channel(window, rod_ends(LEAD))


def coolant_channel(jar):
    def bores(img, x0, y0, x1, y1):
        rect(img, x0, y0, x1, y1, STEEL[1])
        for cx, cy in ((4, 4), (9, 4), (4, 9), (9, 9)):
            bevel(img, cx, cy, cx + 2, cy + 2, COPPER, 3)
            put(img, cx + 1, cy + 1, HOLE)
    return channel(rods(COPPER), bores)


def bench(jar):
    def worktop(w, h):
        img = casing(BRASS, ANDESITE)(w, h)
        for x in range(4, w - 4):
            put(img, x, h // 2, ANDESITE[1])
        rect(img, w // 2 - 3, h // 2 - 2, w // 2 + 2, h // 2 + 1, ANDESITE[4])
        bevel(img, w // 2 - 3, h // 2 - 2, w // 2 + 2, h // 2 + 1, ANDESITE, 4)
        return img
    spec = {"models": ["fuel_assembly_bench"], "faces": {
        ("top", "up"): worktop,
        ("top", "down"): plate(ANDESITE, 2),
        ("top", "north"): edge_band(BRASS, ANDESITE),
        ("top", "south"): edge_band(BRASS, ANDESITE),
        ("leg", "north"): plate(IRON, 3),
        ("leg", "down"): plate(IRON, 2),
        ("clamp_jig", "up"): plate(BRASS, 4),
        ("clamp_jig", "north"): plate(BRASS, 3),
        ("clamp_jig", "east"): plate(BRASS, 2),
    }}
    return apply("fuel_assembly_bench", 64, spec)


def edge_band(top, body):
    def paint(w, h):
        img = tile(w, h)
        for y in range(h):
            for x in range(w):
                put(img, x, y, top[4] if y == 0 else top[2] if y == 1 else body[3] if y < h - 1 else body[1])
        return img
    return paint


def spent_rack(jar):
    img = Image.new("RGBA", (32, 32))
    bevel(img, 0, 0, 15, 15, IRON, 3)
    bevel(img, 1, 1, 14, 14, IRON, 2, raised=False)
    rect(img, 2, 2, 13, 13, IRON[3])
    for y in (5, 10):
        for x in range(2, 14):
            put(img, x, y, IRON[4])
            put(img, x, y + 1, IRON[2])
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        put(img, x, y, IRON[5])
    for x in range(0, 16):
        put(img, x, 15, IRON[1])
    bevel(img, 16, 0, 31, 15, IRON, 2)
    rect(img, 18, 2, 29, 13, IRON[1])
    rect(img, 0, 16, 3, 19, IRON[5])
    rect(img, 4, 16, 7, 19, IRON[4])
    rect(img, 8, 16, 11, 19, WELL)
    cap = [[ROD[0], CAP[1], CAP[1], ROD[0]], [CAP[1], CAP[2], CAP[1], CAP[0]], [CAP[1], CAP[1], CAP[0], CAP[0]], [ROD[0], CAP[0], CAP[0], ROD[0]]]
    for y, row in enumerate(cap):
        for x, c in enumerate(row):
            put(img, 16 + x, 16 + y, c)
    body = [[CAP[0]] * 4, [CAP[2], CAP[1], CAP[1], CAP[0]]] + [[ROD[1], ROD[3], ROD[2], ROD[1]] if y % 4 else [ROD[0]] * 4 for y in range(2, 11)] + [[CAP[1], CAP[1], CAP[1], CAP[0]], [CAP[0]] * 4]
    for y, row in enumerate(body):
        for x, c in enumerate(row):
            put(img, 20 + x, 16 + y, c)
    return img


def cask(jar):
    def body(w, h):
        img = tile(w, h)
        rect(img, 0, 0, w - 1, h - 1, LEAD[2])
        for x in range(w):
            put(img, x, 0, LEAD[4])
            put(img, x, h - 1, LEAD[0])
        for y in range(h):
            put(img, 0, y, LEAD[3])
            put(img, w - 1, y, LEAD[1])
        for y in (2, h - 3):
            for x in range(w):
                put(img, x, y, IRON[3] if y == 2 else IRON[1])
        band = h // 2
        for x in range(w):
            stripe = (x // 2) % 2 == 0
            put(img, x, band - 1, HAZARD[2] if stripe else HAZARD[0])
            put(img, x, band, HAZARD[1] if stripe else HAZARD[0])
        return img

    def lid(w, h):
        img = tile(w, h)
        bevel(img, 0, 0, w - 1, h - 1, LEAD, 3)
        bevel(img, 2, 2, w - 3, h - 3, LEAD, 2, raised=False)
        for x, y in ((1, 1), (w - 2, 1), (1, h - 2), (w - 2, h - 2), (w // 2, 1), (w // 2, h - 2), (1, h // 2), (w - 2, h // 2)):
            put(img, x, y, IRON[5])
        return img
    spec = {"models": ["shielded_cask"], "faces": {
        ("body", "north"): body,
        ("body", "down"): plate(LEAD, 1),
        ("lid", "up"): lid,
        ("lid", "north"): plate(LEAD, 4),
        ("lid", "down"): plate(LEAD, 1),
        ("lug", "up"): plate(IRON, 4),
        ("lug", "north"): plate(IRON, 3),
        ("lug", "east"): plate(IRON, 2),
    }}
    return apply("shielded_cask", 32, spec)


def siren(jar):
    def mouth(w, h):
        img = tile(w, h)
        bevel(img, 0, 0, w - 1, h - 1, BRASS, 4)
        rect(img, 1, 1, w - 2, h - 2, HOLE)
        for x in range(1, w - 1):
            put(img, x, h // 2, (30, 30, 34))
        return img

    def lamp(lit):
        ramp = RED if lit else [(40, 14, 12), (64, 22, 18), (92, 30, 24), (120, 40, 32), (140, 52, 40)]
        def paint(w, h):
            img = tile(w, h)
            rect(img, 0, 0, w - 1, h - 1, ramp[2])
            put(img, 0, 0, ramp[4])
            if w > 1:
                put(img, 1, 0, ramp[3])
            return img
        return paint
    img = Image.open(os.path.join(OUT, "alarm_siren.png")).convert("RGBA")
    spec = {"models": ["alarm_siren"], "faces": {
        ("base", "north"): casing(IRON, STEEL),
        ("base", "east"): plate(IRON, 2),
        ("base", "up"): plate(IRON, 3),
        ("horn", "east"): plate(BRASS, 3),
        ("horn", "up"): plate(BRASS, 4),
        ("horn", "north"): mouth,
        ("beacon", "up"): lamp(False),
        ("beacon", "north"): lamp(False),
    }}
    img = apply("alarm_siren", 32, spec, img)
    spec_lit = {"models": ["alarm_siren_powered"], "faces": {
        ("beacon", "up"): lamp(True),
        ("beacon", "north"): lamp(True),
    }}
    return apply("alarm_siren", 32, spec_lit, img)


def gauge(img, cx, cy, r, needle):
    disc(img, cx, cy, r, lambda x, y, d: (BRASS[3] if x + y < cx + cy else BRASS[1]) if d > r - 1 else (232, 228, 214))
    for k in range(1, int(r)):
        put(img, int(cx + needle[0] * k), int(cy + needle[1] * k), RED[2])
    put(img, int(cx), int(cy), STEEL[0])


def instrument_panel(jar):
    def front(w, h):
        img = casing(IRON, STEEL)(w, h)
        gauge(img, 4.5, 5, 2.6, (0.7, -0.7))
        gauge(img, 9.5, 5, 2.6, (-0.7, -0.7))
        for i, ramp in enumerate((GREEN, AMBER, RED)):
            put(img, 3 + i * 3, 10, ramp[3])
            put(img, 4 + i * 3, 10, ramp[1])
        return img
    spec = {"models": ["instrumentation_panel"], "faces": {
        ("panel", "north"): front,
        ("panel", "south"): plate(IRON, 2),
        ("panel", "east"): plate(IRON, 3),
        ("panel", "up"): plate(IRON, 4),
    }}
    return apply("instrumentation_panel", 32, spec)


def docking(jar):
    img = Image.open(os.path.join(OUT, "docking_service_connector.png")).convert("RGBA")

    def face(connected):
        def paint(w, h):
            out = casing(BRASS, STEEL)(w, h)
            opening(out, 4, 4, 8, GREEN if connected else AMBER, IRON)
            for y in (6, 7, 8, 9):
                put(out, 2, y, IRON[4] if connected else IRON[2])
                put(out, 13, y, IRON[4] if connected else IRON[2])
            return out
        return paint
    spec = {"models": ["docking_service_connector", "docking_service_connector_connected"], "faces": {
        ("clamp", "north"): plate(IRON, 4),
        ("clamp", "east"): plate(IRON, 3),
        ("clamp", "up"): plate(IRON, 4),
    }}
    img = apply("docking_service_connector", 32, spec, img)
    img.paste(face(False)(16, 16), (0, 0))
    img.paste(face(True)(16, 16), (0, 16))
    img.paste(casing(BRASS, STEEL)(16, 16), (16, 0))
    return img


REST = {
    "circulation_pump": pump,
    "steam_turbine": turbine,
    "control_rod_drive": rod_drive,
    "fuel_channel": fuel_channel,
    "control_channel": control_channel,
    "coolant_channel": coolant_channel,
    "fuel_assembly_bench": bench,
    "spent_fuel_rack": spent_rack,
    "shielded_cask": cask,
    "alarm_siren": siren,
    "instrumentation_panel": instrument_panel,
    "docking_service_connector": docking,
}

from PIL import Image

from handmade import BLUE, BRASS, COPPER, HOLE, IRON, RED, STEEL, bevel, put, rect, remap

PALE = [(70, 74, 78), (120, 126, 130), (170, 176, 180), (206, 212, 214), (232, 236, 238)]
TANK = [(30, 33, 38), (39, 43, 48), (48, 54, 57), (58, 64, 67), (68, 75, 76), (80, 86, 90), (94, 100, 104), (110, 116, 120)]
COPPER_TONES = [(0x90, 0x49, 0x31), (0x9a, 0x50, 0x38), (0xa7, 0x5a, 0x40), (0xb2, 0x62, 0x47), (0xc2, 0x6b, 0x4c), (0xc8, 0x74, 0x56), (0xd6, 0x7b, 0x5b), (0xe3, 0x82, 0x6c)]
WATER = [(16, 52, 84), (24, 78, 120), (40, 110, 160), (70, 150, 200), (120, 190, 230)]
WOOD = [(0x55, 0x3a, 0x1f), (0x61, 0x4b, 0x2e), (0x70, 0x52, 0x2e), (0x7a, 0x5a, 0x34), (0x82, 0x61, 0x3a), (0x88, 0x65, 0x39)]
GAP = (16, 18, 20)
CASING_DARK = (0x44, 0x41, 0x40)
CASING_MID = (0x4b, 0x49, 0x48)


def blank(w, h):
    return Image.new("RGBA", (w, h))


def disc(img, cx, cy, r, color_at):
    for y in range(int(cy - r - 1), int(cy + r + 2)):
        for x in range(int(cx - r - 1), int(cx + r + 2)):
            if not (0 <= x < img.width and 0 <= y < img.height):
                continue
            d = ((x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2) ** 0.5
            if d <= r:
                c = color_at(x, y, d)
                if c:
                    put(img, x, y, c)


def opening(img, x0, y0, size, collar, flange=COPPER):
    x1, y1 = x0 + size - 1, y0 + size - 1
    bevel(img, x0, y0, x1, y1, flange, 3)
    bevel(img, x0 + 1, y0 + 1, x1 - 1, y1 - 1, collar, 2)
    rect(img, x0 + 2, y0 + 2, x1 - 2, y1 - 2, HOLE)
    for x in range(x0 + 2, x1 - 1):
        put(img, x, y0 + 2, (4, 4, 5))


def copper_face(jar, collar):
    img = jar.tex("copper_casing")
    rect(img, 3, 3, 12, 12, CASING_DARK)
    rect(img, 4, 4, 11, 11, CASING_MID)
    opening(img, 4, 4, 8, collar)
    return img


def tube_side(jar):
    img = jar.tex("copper_casing")
    rect(img, 3, 3, 12, 12, STEEL[1])
    tubes = (4, 7, 10)
    for y in tubes:
        for x in range(3, 13):
            put(img, x, y, COPPER[5])
            put(img, x, y + 1, COPPER[3])
    for x in (6, 9):
        for y in range(3, 13):
            put(img, x, y, IRON[4] if y in (3, 6, 9, 12) else IRON[3])
    return img


def fin_side(jar):
    img = jar.tex("copper_casing")
    for x in range(3, 13):
        for y in range(3, 13):
            if x % 2 == 1:
                put(img, x, y, COPPER[5] if y < 5 else COPPER[4] if y < 10 else COPPER[3])
            else:
                put(img, x, y, STEEL[0])
    return img


def atlas4(nw, ne, sw, se):
    img = blank(32, 32)
    for tile, pos in ((nw, (0, 0)), (ne, (16, 0)), (sw, (0, 16)), (se, (16, 16))):
        if tile is not None:
            img.paste(tile, pos)
    return img


def heat_exchanger(jar):
    return atlas4(copper_face(jar, RED), copper_face(jar, BLUE), tube_side(jar), copper_face(jar, PALE))


def condenser(jar):
    return atlas4(copper_face(jar, PALE), copper_face(jar, BLUE), fin_side(jar), copper_face(jar, WATER))


def emergency_tank(jar):
    table = {c: TANK[i] for i, c in enumerate(COPPER_TONES)}
    side = remap(jar.tex("fluid_tank"), table)
    for x in range(16):
        edge = x in (0, 15)
        put(side, x, 0, RED[2] if edge else RED[3])
        put(side, x, 1, RED[2])
        put(side, x, 2, RED[1])
        put(side, x, 13, RED[2] if edge else RED[3])
        put(side, x, 14, RED[2])
        put(side, x, 15, RED[1])
    rect(side, 6, 5, 9, 10, STEEL[0])
    for y in range(6, 10):
        put(side, 7, y, WATER[3] if y == 6 else WATER[2])
        put(side, 8, y, WATER[2] if y == 6 else WATER[1])
    put(side, 7, 6, (160, 210, 235))
    top = remap(jar.tex("fluid_tank_top"), table)

    def wheel(x, y, d):
        if 3.2 < d <= 4.3:
            return RED[3] if x + y < 16 else RED[1]
        return None

    disc(top, 8, 8, 4.3, wheel)
    for k in range(5, 11):
        put(top, k, 7, RED[2])
        put(top, 7, k, RED[2])
    rect(top, 7, 7, 8, 8, IRON[4])
    put(top, 8, 8, IRON[2])
    return atlas4(side, top, None, None)


def radiator(jar):
    front = blank(16, 16)
    bevel(front, 0, 0, 15, 15, IRON, 3)
    for x in range(1, 15):
        for y in range(1, 15):
            if x % 2 == 1:
                put(front, x, y, COPPER[5] if y < 4 else COPPER[4] if y < 11 else COPPER[3])
            else:
                put(front, x, y, HOLE)
    for x in range(1, 15):
        put(front, x, 7, IRON[3])
        put(front, x, 8, IRON[1])
    back = blank(16, 16)
    bevel(back, 0, 0, 15, 15, IRON, 3)
    rect(back, 2, 2, 13, 13, STEEL[4])
    for x in range(2, 14):
        put(back, x, 2, STEEL[7])
        put(back, x, 13, STEEL[2])
    for x in (3, 12):
        for y in (3, 12):
            put(back, x, y, IRON[4])
    edge = blank(4, 16)
    for y in range(16):
        for x in range(4):
            put(edge, x, y, COPPER[4] if x in (1, 2) else IRON[2])
    top = blank(16, 4)
    for x in range(16):
        for y in range(4):
            put(top, x, y, (COPPER[4] if x % 2 else STEEL[0]) if y in (1, 2) else IRON[3])
    img = blank(32, 32)
    img.paste(front, (0, 0))
    img.paste(back, (16, 0))
    img.paste(edge, (0, 16))
    img.paste(top, (4, 16))
    return img


def boiler_plate(jar):
    top = jar.tex("copper_casing")
    rect(top, 3, 3, 12, 12, STEEL[1])
    coil = set()
    for x in (4, 6, 8, 10):
        for y in range(4, 12):
            coil.add((x, y))
    for x in range(4, 7):
        coil.add((x, 11))
    for x in range(6, 9):
        coil.add((x, 4))
    for x in range(8, 11):
        coil.add((x, 11))
    for x, y in coil:
        put(top, x, y, COPPER[5])
    for x, y in coil:
        if (x + 1, y) not in coil and x + 1 <= 12:
            put(top, x + 1, y, COPPER[2])
    bottom = blank(16, 16)
    bevel(bottom, 0, 0, 15, 15, IRON, 3)
    rect(bottom, 2, 2, 13, 13, STEEL[4])
    strip_front = blank(16, 6)
    strip_side = blank(16, 6)
    for strip in (strip_front, strip_side):
        for x in range(16):
            for y in range(6):
                put(strip, x, y, IRON[4] if y == 0 else IRON[1] if y == 5 else STEEL[3])
    opening(strip_front, 5, 0, 6, RED)
    for x in range(2, 14):
        put(strip_side, x, 2, COPPER[4])
        put(strip_side, x, 3, COPPER[2])
    img = blank(32, 32)
    img.paste(top, (0, 0))
    img.paste(bottom, (16, 0))
    img.paste(strip_front, (0, 16))
    img.paste(strip_side, (16, 16))
    return img


def cooling_tower(jar):
    base = jar.tex("andesite_casing")
    side = base.copy()
    for y in range(2, 14):
        for x in range(2, 14):
            slat = (y - 2) % 3
            put(side, x, y, GAP if slat == 2 else WOOD[4] if slat == 0 else WOOD[2])
    top = base.copy()
    for y in range(2, 14):
        for x in range(2, 14):
            on = (x - 2) % 3 != 2 and (y - 2) % 3 != 2
            put(top, x, y, (WOOD[3] if (x + y) % 2 else WOOD[2]) if on else GAP)
    return atlas4(side, top, None, None)


def region(img, x0, y0, w, h, ramp, base=3):
    bevel(img, x0, y0, x0 + w - 1, y0 + h - 1, ramp, base)


def relief_valve(jar):
    img = blank(32, 32)
    region(img, 0, 0, 10, 10, COPPER)
    opening(img, 1, 1, 8, IRON)
    for x, y in ((1, 1), (8, 1), (1, 8), (8, 8)):
        put(img, x, y, IRON[4])
    region(img, 10, 0, 10, 10, COPPER)
    for x, y in ((11, 1), (18, 1), (11, 8), (18, 8)):
        put(img, x, y, IRON[4])
    region(img, 20, 0, 4, 10, COPPER, 2)
    region(img, 14, 10, 10, 4, COPPER, 2)
    region(img, 24, 0, 8, 8, BRASS)
    rect(img, 26, 2, 29, 5, BRASS[2])
    put(img, 27, 3, BRASS[5])
    region(img, 0, 10, 6, 8, BRASS)
    region(img, 6, 10, 8, 6, BRASS)
    for x in range(24, 32):
        for y in range(10, 14):
            put(img, x, y, IRON[5] if (y + (x // 2)) % 2 == 0 else IRON[2])
    rect(img, 0, 18, 3, 21, IRON[3])
    put(img, 4, 18, HOLE)
    put(img, 5, 18, HOLE)
    put(img, 4, 19, HOLE)
    put(img, 5, 19, HOLE)
    return img


def bypass_valve(jar):
    img = blank(32, 32)
    for x0 in (0, 10, 20):
        region(img, x0, 0, 10, 10, COPPER)
        rect(img, x0 + 2, 2, x0 + 7, 7, COPPER[3])
        for x, y in ((x0 + 1, 1), (x0 + 8, 1), (x0 + 1, 8), (x0 + 8, 8)):
            put(img, x, y, IRON[4])
    region(img, 0, 10, 8, 8, IRON)
    opening(img, 0, 10, 8, PALE, IRON)
    region(img, 8, 10, 8, 8, IRON)
    opening(img, 8, 10, 8, RED, IRON)
    for x in range(0, 6):
        for y in range(18, 26):
            put(img, x, y, IRON[4] if x in (0, 3) else IRON[2])
    for x in range(6, 17):
        for y in range(18, 21):
            put(img, x, y, IRON[4] if y == 18 else IRON[2])
    rect(img, 6, 18, 8, 25, IRON[3])
    rect(img, 17, 18, 18, 19, IRON[3])

    for cx in (20, 28):
        bevel(img, cx - 4, 10, cx + 3, 17, STEEL, 2)
        disc(img, cx, 14, 3.6, lambda x, y, d, cx=cx: (RED[3] if x + y < cx + 14 else RED[1]) if d > 2.6 else None)
        for k in range(cx - 3, cx + 3):
            put(img, k, 13, RED[2])
        for k in range(11, 17):
            put(img, cx - 1, k, RED[2])
        put(img, cx - 1, 13, IRON[4])
    for x in range(19, 27):
        put(img, x, 18, RED[2])
    return img


PLANT = {
    "heat_exchanger": heat_exchanger,
    "condenser": condenser,
    "emergency_coolant_tank": emergency_tank,
    "radiator_panel": radiator,
    "boiler_heating_plate": boiler_plate,
    "cooling_tower_packing": cooling_tower,
    "relief_valve": relief_valve,
    "steam_bypass_valve": bypass_valve,
}

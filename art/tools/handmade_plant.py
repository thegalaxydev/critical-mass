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


PLANT = {
    "cooling_tower_packing": cooling_tower,
}

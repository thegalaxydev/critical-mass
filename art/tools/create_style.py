import math


def rgb(*values):
    return [tuple(int(v[i:i + 2], 16) for i in (1, 3, 5)) for v in values]


BRASS = rgb("#592424", "#724731", "#9e6947", "#af794f", "#bf8e55", "#c79758", "#cea05a", "#d7aa5e", "#e4b763", "#eec168", "#f7cb6c", "#fbdc7d", "#ffeb8c")
COPPER = rgb("#5a2a1d", "#83402c", "#904931", "#9a5038", "#a75a40", "#b26247", "#c26b4c", "#c87456", "#d67b5b", "#e3826c", "#f4a98f")
IRON = rgb("#2b2b2e", "#353438", "#3f3e42", "#444444", "#494848", "#4f4f4f", "#595858", "#676161", "#757171", "#8d8b8b", "#989797")
CAVITY = rgb("#3a1212", "#511c1c", "#5e2521", "#703d2e")
TUBE = (0.36, 0.55, 0.73, 0.91, 0.91, 0.82, 0.73, 0.64, 0.55, 0.36)
CASING = rgb("#171f26", "#1c242c", "#1f272e", "#232b33", "#2a333d", "#333e49", "#3f4d5b")
PLATE_STEEL = rgb("#868e97", "#929aa1", "#9ea5ac", "#a6aeb6")
FUEL_YELLOW = rgb("#d19e27", "#edb832", "#f2c842")
FAMILIES = (CASING, PLATE_STEEL, FUEL_YELLOW, COPPER, BRASS)
SHEEN_AREA = 20
SHEEN_SIDE = 3
SHEEN_FILL = 0.6
SHEEN_EVERY = 12


def tone(ramp, value):
    return ramp[max(0, min(len(ramp) - 1, round(max(0.0, min(1.0, value)) * (len(ramp) - 1))))]


def stretch(ramp, n):
    out = []
    for i in range(n):
        t = i * (len(ramp) - 1) / (n - 1)
        a, b = ramp[int(t)], ramp[min(len(ramp) - 1, int(t) + 1)]
        f = t - int(t)
        out.append(tuple(round(a[k] + (b[k] - a[k]) * f) for k in range(3)))
    return out


def tube(n):
    out = []
    for i in range(n):
        t = i * (len(TUBE) - 1) / max(1, n - 1)
        a, b = TUBE[int(t)], TUBE[min(len(TUBE) - 1, int(t) + 1)]
        out.append(a + (b - a) * (t - int(t)))
    return out


def strip(w, h):
    if (w, h) == (2, 2):
        return {(0, 0): 0.9, (1, 0): 0.62, (0, 1): 0.55, (1, 1): 0.25}
    out = {}
    along = tube(w if h <= 2 and w >= h else h)
    for y in range(h):
        for x in range(w):
            if h <= 2 and w >= h:
                v = along[x] - (0.3 if y == 1 else 0.0)
            else:
                v = 0.86 - 0.3 * y / max(1, h - 1) - (0.3 if x == 1 else 0.0) + (0.08 if y == 0 else -0.1 if y == h - 1 else 0.0)
            out[x, y] = v
    return out


def small(w, h):
    out = {}
    for y in range(h):
        for x in range(w):
            top, left, bottom, right = y == 0, x == 0, y == h - 1, x == w - 1
            if top and left:
                v = 0.95
            elif bottom and right:
                v = 0.12
            elif (top and right) or (bottom and left):
                v = 0.5
            elif top or left:
                v = 0.85
            elif bottom or right:
                v = 0.3
            else:
                v = 0.6 + 0.1 * math.cos(2 * math.pi * (x + y - 3) / 8.0)
            out[x, y] = v
    return out


def plate(w, h, streaked=False):
    out = {}
    for y in range(h):
        for x in range(w):
            top, left, bottom, right = y == 0, x == 0, y == h - 1, x == w - 1
            if top and left:
                v = 0.08
            elif top + left + bottom + right == 2:
                v = 0.0
            elif top or left:
                v = 0.17
            elif bottom or right:
                v = 0.08
            elif x in (1, w - 2) and y in (1, h - 2):
                v = 0.5
            elif x == 1 or y == 1:
                v = 0.92 if (x + y) % 3 else 1.0
            elif x == w - 2 or y == h - 2:
                v = 0.83 if (x + y) % 2 else 0.68
            elif streaked:
                swell = math.sin(math.pi * (x - 2) / max(1, w - 5))
                if y == 2:
                    v = 0.24 + 0.1 * swell
                else:
                    v = (0.6 if y % 2 else 0.47) + 0.14 * swell - (0.08 if (x * 3 + y * 5) % 7 == 0 else 0.0)
            else:
                v = 0.58 + 0.17 * math.cos(2 * math.pi * (x + y - 7.5) / 8.0)
                v -= 0.12 * max(0, 2 - (x - 2) - (y - 2)) + 0.12 * max(0, 2 - (w - 3 - x) - (h - 3 - y))
            out[x, y] = v
    return out


def field(w, h, streaked=False):
    if min(w, h) <= 2:
        return strip(w, h)
    if min(w, h) <= 4:
        return small(w, h)
    return plate(w, h, streaked)


def regions(img):
    w, h = img.size
    px = img.load()
    seen = set()
    for y in range(h):
        for x in range(w):
            if (x, y) in seen or px[x, y][3] == 0:
                continue
            colour = px[x, y]
            stack = [(x, y)]
            seen.add((x, y))
            cells = []
            while stack:
                a, b = stack.pop()
                cells.append((a, b))
                for q in ((a + 1, b), (a - 1, b), (a, b + 1), (a, b - 1)):
                    if 0 <= q[0] < w and 0 <= q[1] < h and q not in seen and px[q] == colour:
                        seen.add(q)
                        stack.append(q)
            yield colour, cells


def sheen(u, v, w, h):
    centre = (w + h) / 2.0 - 1
    d = min(abs(u + v - centre - SHEEN_EVERY * k) for k in (-2, -1, 0, 1, 2)) if w > 2 * h or h > 2 * w else abs(u + v - centre)
    if min(w, h) < 5:
        return 1 if d < 1.0 else 0
    return 2 if d < 1.0 else 1 if d < 2.5 else 0


def enliven(img):
    px = img.load()
    changed = 0
    for colour, cells in list(regions(img)):
        family = next((f for f in FAMILIES if colour[:3] in f), None)
        if family is None or len(cells) < SHEEN_AREA:
            continue
        x0, y0 = min(c[0] for c in cells), min(c[1] for c in cells)
        w, h = max(c[0] for c in cells) - x0 + 1, max(c[1] for c in cells) - y0 + 1
        if min(w, h) < SHEEN_SIDE or len(cells) < SHEEN_FILL * w * h:
            continue
        base = family.index(colour[:3])
        for x, y in cells:
            rim = min(w, h) >= 5 and (x in (x0, x0 + w - 1) or y in (y0, y0 + h - 1))
            lift = max(0, min(len(family) - 1, base + sheen(x - x0, y - y0, w, h) - (1 if rim else 0)))
            if lift != base:
                px[x, y] = family[lift] + (colour[3],)
                changed += 1
    return changed

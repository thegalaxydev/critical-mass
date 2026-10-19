import colorsys
import json
import os
import sys
from collections import Counter

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
MOD = os.path.dirname(os.path.dirname(HERE))
TEXTURES = os.path.join(MOD, "src", "main", "resources", "assets", "criticalmass", "textures", "block")
PASSES = 3


def hex2rgb(c):
    return tuple(int(c[i:i + 2], 16) for i in (1, 3, 5))


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def material_index(pal):
    out = {}
    for mat, ramp in pal.items():
        for i, c in enumerate(ramp):
            out.setdefault(hex2rgb(c), mat)
    return out


def near_material(c, pal_index, ramps):
    if c in pal_index:
        return pal_index[c]
    best, bd = None, 1e9
    for mat, ramp in ramps.items():
        for r in ramp:
            d = sum((a - b) ** 2 for a, b in zip(c, r))
            if d < bd:
                best, bd = mat, d
    return best if bd < 400 else None


GREY = [(l - 3, l - 1, l + 3) for l in (20, 31, 43, 56, 70, 86, 104, 124, 146, 172, 200, 228)]


def family(c):
    r, g, b = [v / 255 for v in c]
    mx, mn = max(r, g, b), min(r, g, b)
    if mx < 0.1:
        return None
    sat = (mx - mn) / mx
    if sat < 0.16:
        return "grey"
    hue = colorsys.rgb_to_hsv(r, g, b)[0] * 360
    if 12 <= hue < 30 and sat < 0.75:
        return "copper"
    if 30 <= hue < 52 and sat < 0.8:
        return "brass"
    return None


def snap(c, ramp):
    return min(ramp, key=lambda r: abs(lum(r) - lum(c)))


def requantize(img, ramps, lock=None):
    px = img.load()
    lk = lock.load() if lock else None
    fam = {"grey": GREY, "copper": ramps["copper"], "brass": ramps["brass"]}
    n = 0
    for y in range(img.height):
        for x in range(img.width):
            p = px[x, y]
            if p[3] < 128:
                continue
            f = family(p[:3])
            if f != "grey":
                f = None
            if f:
                q = snap(p[:3], fam[f])
                if q != p[:3]:
                    px[x, y] = q + (p[3],)
                    n += 1
    return n


def runs(img, x, y):
    px = img.load()
    c = px[x, y]
    hl = 1
    i = x - 1
    while i >= 0 and px[i, y] == c:
        hl += 1
        i -= 1
    i = x + 1
    while i < img.width and px[i, y] == c:
        hl += 1
        i += 1
    vl = 1
    j = y - 1
    while j >= 0 and px[x, j] == c:
        vl += 1
        j -= 1
    j = y + 1
    while j < img.height and px[x, j] == c:
        vl += 1
        j += 1
    return hl, vl


NOISE_BLOCKOUTS = {"shielding_concrete"}


def restore_thin(img, blockout):
    if blockout is None or blockout.size != img.size:
        return 0
    px = img.load()
    bp = blockout.load()
    n = 0
    for y in range(img.height):
        for x in range(img.width):
            b = bp[x, y]
            p = px[x, y]
            if b[3] < 128 or p[3] < 128 or family(b[:3]) != "grey" or family(p[:3]) != "grey":
                continue
            hl, vl = runs(blockout, x, y)
            if min(hl, vl) <= 2 and max(hl, vl) >= 3:
                q = snap(b[:3], GREY)
                if q != p[:3]:
                    px[x, y] = q + (p[3],)
                    n += 1
    return n


def smooth(img, lock):
    px = img.load()
    lk = lock.load() if lock else None
    updates = {}
    for y in range(1, img.height - 1):
        for x in range(1, img.width - 1):
            p = px[x, y]
            if p[3] < 128 or (lk and lk[x, y] > 0):
                continue
            f = family(p[:3])
            if f is None or max(runs(img, x, y)) >= 3:
                continue
            around = [px[x + dx, y + dy][:3] for dx in (-1, 0, 1) for dy in (-1, 0, 1) if (dx or dy) and px[x + dx, y + dy][3] >= 128]
            same = [c for c in around if family(c) == f]
            if len(same) < 7:
                continue
            top, count = Counter(same).most_common(1)[0]
            if count >= 5 and top != p[:3]:
                updates[(x, y)] = top
    for q, c in updates.items():
        px[q] = c + (px[q][3],)
    return len(updates)


def despeckle(img, lock, pal, blockout=None):
    ramps = {k: [hex2rgb(c) for c in v] for k, v in pal.items()}
    requantize(img, ramps, lock)
    restore_thin(img, blockout)
    smooth(img, lock)
    ramps = dict(ramps, grey=GREY)
    pal = dict(pal, grey=["#%02x%02x%02x" % c for c in GREY])
    idx = material_index(pal)
    w, hgt = img.size
    px = img.load()
    lk = lock.load() if lock else None
    mat = {}
    for y in range(hgt):
        for x in range(w):
            p = px[x, y]
            if p[3] < 128 or (lk and lk[x, y] > 0):
                continue
            m = near_material(p[:3], idx, ramps)
            if m and m not in ("ink",):
                mat[(x, y)] = m
    changed = 0
    for _ in range(PASSES):
        updates = {}
        for (x, y), m in mat.items():
            c = px[x, y][:3]
            if max(runs(img, x, y)) >= 3:
                continue
            n4 = [(x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)]
            n8 = n4 + [(x + 1, y + 1), (x - 1, y - 1), (x + 1, y - 1), (x - 1, y + 1)]
            same_mat4 = [px[q][:3] for q in n4 if mat.get(q) == m]
            same_mat8 = [px[q][:3] for q in n8 if mat.get(q) == m]
            lr = [(x - 1, y), (x + 1, y)]
            ud = [(x, y - 1), (x, y + 1)]
            line = None
            for a, b in (lr, ud):
                if mat.get(a) == m and mat.get(b) == m and px[a][:3] == px[b][:3] != c:
                    line = px[a][:3]
                    break
            if line is not None and sum(1 for q in same_mat8 if q == c) <= 1:
                updates[(x, y)] = line
                continue
            if len(same_mat4) < 3:
                continue
            if sum(1 for q in same_mat8 if q == c) >= 2:
                continue
            votes = Counter(same_mat4).most_common()
            if votes[0][1] >= 2:
                updates[(x, y)] = votes[0][0]
        for q, c in updates.items():
            px[q] = c + (px[q][3],)
        changed += len(updates)
        if not updates:
            break
    return changed


def run(scratch, dest, names):
    pal = json.load(open(os.path.join(scratch, "art", "palette.json"), encoding="utf8"))
    for n in names:
        src = os.path.join(TEXTURES, n + ".png")
        img = Image.open(src).convert("RGBA")
        lp = os.path.join(scratch, "art", "lock", n + ".png")
        lock = Image.open(lp).convert("L") if os.path.exists(lp) else None
        if lock and lock.size != img.size:
            lock = None
        bp = os.path.join(scratch, "art", "blockout", n + ".png")
        blockout = Image.open(bp).convert("RGBA") if os.path.exists(bp) and n not in NOISE_BLOCKOUTS else None
        k = despeckle(img, lock, pal, blockout)
        img.save(os.path.join(dest, n + ".png"))
        print(n, k, "pixels merged")


if __name__ == "__main__":
    run(sys.argv[1], sys.argv[2], sys.argv[3:])

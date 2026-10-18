import json
import os
import subprocess
import sys
import datetime
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ART = os.path.join(ROOT, "art")
PAL = json.load(open(os.path.join(ART, "palette.json"), encoding="utf8"))
IMAGE_TOOL = os.path.expanduser("~/tools/image_tool.py")


def hex2rgb(h):
    return tuple(int(h[i:i + 2], 16) for i in (1, 3, 5))


RAMPS = {k: [hex2rgb(h) for h in v] for k, v in PAL.items()}
COLOR_MATERIAL = {}
for name, cols in RAMPS.items():
    for i, c in enumerate(cols):
        COLOR_MATERIAL.setdefault(c, (name, i))


def P(mat, i):
    r = RAMPS[mat]
    i = max(0, min(len(r) - 1, i if i >= 0 else len(r) + i))
    return r[i] + (255,)


class Canvas:
    def __init__(self, w, h):
        self.w, self.h = w, h
        self.img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        self.lock = Image.new("L", (w, h), 0)
        self.d = ImageDraw.Draw(self.img)
        self.ld = ImageDraw.Draw(self.lock)

    def px(self, x, y, c, lock=False):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.img.putpixel((x, y), c)
            if lock:
                self.lock.putpixel((x, y), 255)

    def rect(self, x0, y0, x1, y1, c, lock=False):
        self.d.rectangle([x0, y0, x1, y1], fill=c)
        if lock:
            self.ld.rectangle([x0, y0, x1, y1], fill=255)

    def outline(self, x0, y0, x1, y1, c, lock=False):
        self.d.rectangle([x0, y0, x1, y1], outline=c)
        if lock:
            self.ld.rectangle([x0, y0, x1, y1], outline=255)

    def hline(self, x0, x1, y, c, lock=False):
        self.rect(x0, y, x1, y, c, lock)

    def vline(self, x, y0, y1, c, lock=False):
        self.rect(x, y0, x, y1, c, lock)

    def clear(self, x0, y0, x1, y1, lock=True):
        self.d.rectangle([x0, y0, x1, y1], fill=(0, 0, 0, 0))
        if lock:
            self.ld.rectangle([x0, y0, x1, y1], fill=255)

    def ellipse(self, x0, y0, x1, y1, c, lock=False, outline=None):
        self.d.ellipse([x0, y0, x1, y1], fill=c, outline=outline)
        if lock:
            self.ld.ellipse([x0, y0, x1, y1], fill=255)

    def bevel(self, x0, y0, x1, y1, mat, base=3, lock_border=True):
        self.rect(x0, y0, x1, y1, P(mat, base))
        self.hline(x0, x1, y0, P(mat, base + 2))
        self.vline(x0, y0, y1, P(mat, base + 1))
        self.hline(x0, x1, y1, P(mat, base - 2))
        self.vline(x1, y0, y1, P(mat, base - 1))
        self.outline(x0, y0, x1, y1, None, lock_border)

    def frame(self, x0, y0, x1, y1, mat, width=2, base=3):
        for i in range(width):
            self.hline(x0 + i, x1 - i, y0 + i, P(mat, base + 2 - i))
            self.vline(x0 + i, y0 + i, y1 - i, P(mat, base + 1 - i))
            self.hline(x0 + i, x1 - i, y1 - i, P(mat, base - 2 + i))
            self.vline(x1 - i, y0 + i, y1 - i, P(mat, base - 1 + i))

    def rivet(self, x, y, mat="iron", lock=True):
        self.px(x, y, P(mat, -2), lock)
        self.px(x + 1, y + 1, P(mat, 1), lock) if False else None


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def nearest(c, allowed):
    best, bd = None, 1e18
    for a in allowed:
        dl = lum(c) - lum(a)
        d = (c[0] - a[0]) ** 2 * 0.3 + (c[1] - a[1]) ** 2 * 0.4 + (c[2] - a[2]) ** 2 * 0.3 + dl * dl * 1.2
        if d < bd:
            bd, best = d, a
    return best


def block_median(src, x0, y0, x1, y1):
    m = 0.22
    bx0, by0 = x0 + (x1 - x0) * m, y0 + (y1 - y0) * m
    bx1, by1 = x1 - (x1 - x0) * m, y1 - (y1 - y0) * m
    crop = src.crop((int(bx0), int(by0), max(int(bx0) + 1, int(bx1)), max(int(by0) + 1, int(by1)))).convert("RGB")
    data = list(crop.getdata())
    data.sort(key=lum)
    return data[len(data) // 2]


def _components(blockout, lockmask):
    W, H = blockout.size
    comp = [[-1] * W for _ in range(H)]
    comps = []
    for y in range(H):
        for x in range(W):
            b = blockout.getpixel((x, y))
            if comp[y][x] != -1 or b[3] == 0 or lockmask.getpixel((x, y)):
                continue
            mat = COLOR_MATERIAL.get(b[:3], (None, 0))[0]
            stack = [(x, y)]
            comp[y][x] = len(comps)
            pts = []
            while stack:
                cx, cy = stack.pop()
                pts.append((cx, cy))
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = cx + dx, cy + dy
                    if 0 <= nx < W and 0 <= ny < H and comp[ny][nx] == -1:
                        nb = blockout.getpixel((nx, ny))
                        if nb[3] and not lockmask.getpixel((nx, ny)) and COLOR_MATERIAL.get(nb[:3], (None, 0))[0] == mat:
                            comp[ny][nx] = len(comps)
                            stack.append((nx, ny))
            comps.append((mat, pts))
    return comps


def _rep(src, x, y, bw, bh):
    cols = []
    for sy in range(5):
        for sx in range(5):
            fx = (x + 0.15 + 0.7 * (sx + 0.5) / 5) * bw
            fy = (y + 0.15 + 0.7 * (sy + 0.5) / 5) * bh
            cols.append(src.getpixel((min(src.width - 1, int(fx)), min(src.height - 1, int(fy))))[:3])
    cols.sort(key=lum)
    return cols


def finalize(spec, blockout, lockmask, generated_png, out_png, accents, cleanup=True, materials_free=True, tone=0.9):
    src = Image.open(generated_png).convert("RGBA")
    W, H = blockout.size
    bw, bh = src.width / W, src.height / H
    out = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    if materials_free:
        used = set()
        for c in blockout.getdata():
            if c[3]:
                m = COLOR_MATERIAL.get(c[:3], (None, 0))[0]
                if m and m != "ink":
                    used.add(m)
        for m in list(used):
            used.update(accents.get(m, []))
        used.update(accents.get("*", []))
        pal = [c for m in sorted(used) for c in RAMPS[m]] + RAMPS["ink"]
        for y in range(H):
            for x in range(W):
                b = blockout.getpixel((x, y))
                if b[3] == 0:
                    continue
                if lockmask.getpixel((x, y)):
                    out.putpixel((x, y), b)
                    continue
                votes = {}
                for c in _rep(src, x, y, bw, bh):
                    q = nearest(c, pal)
                    votes[q] = votes.get(q, 0) + 1
                out.putpixel((x, y), max(votes.items(), key=lambda kv: kv[1])[0] + (255,))
        if cleanup:
            _cleanup(out, lockmask)
        out.save(out_png)
        return out
    for y in range(H):
        for x in range(W):
            b = blockout.getpixel((x, y))
            if b[3] and lockmask.getpixel((x, y)):
                out.putpixel((x, y), b)
    for mat, pts in _components(blockout, lockmask):
        allowed = list(RAMPS[mat]) if mat else []
        for k in accents.get(mat, []) + accents.get("*", []):
            allowed += RAMPS[k]
        reps = {}
        for (x, y) in pts:
            reps[(x, y)] = _rep(src, x, y, bw, bh)
        generated_l = sorted(lum(c[len(c) // 2]) for c in reps.values())
        blk_l = sorted(lum(blockout.getpixel(pt)[:3]) for pt in pts)
        off = (generated_l[len(generated_l) // 2] - blk_l[len(blk_l) // 2]) * tone if mat else 0
        for (x, y), cols in reps.items():
            if not allowed:
                out.putpixel((x, y), blockout.getpixel((x, y)))
                continue
            votes = {}
            for c in cols:
                adj = tuple(max(0, min(255, int(v - off))) for v in c)
                q = nearest(adj, allowed)
                votes[q] = votes.get(q, 0) + 1
            out.putpixel((x, y), max(votes.items(), key=lambda kv: kv[1])[0] + (255,))
    if cleanup:
        _cleanup(out, lockmask)
    out.save(out_png)
    return out


def _cleanup(out, lockmask):
    W, H = out.size
    if True:
        px = out.load()
        lk = lockmask.load()
        changed = []
        for y in range(H):
            for x in range(W):
                if lk[x, y] or px[x, y][3] == 0:
                    continue
                n = [px[x + dx, y + dy] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)) if 0 <= x + dx < W and 0 <= y + dy < H]
                if len(n) < 4 or px[x, y] in n:
                    continue
                counts = {}
                for c in n:
                    counts[c] = counts.get(c, 0) + 1
                top, cnt = max(counts.items(), key=lambda kv: kv[1])
                if cnt >= 3 and top[3] == 255:
                    changed.append((x, y, top))
        for x, y, c in changed:
            px[x, y] = c


def preview(png, out, scale=16):
    im = Image.open(png).convert("RGBA")
    bg = Image.new("RGBA", im.size, (54, 57, 63, 255))
    bg.alpha_composite(im)
    bg.resize((im.width * scale, im.height * scale), Image.NEAREST).save(out)


def template_fit(blockout, out, texel=30, canvas=1024):
    import json as _j
    w, h = blockout.width * texel, blockout.height * texel
    if w > canvas or h > canvas:
        texel = canvas // max(blockout.size)
        w, h = blockout.width * texel, blockout.height * texel
    big = blockout.resize((w, h), Image.NEAREST)
    bg = Image.new("RGBA", (canvas, canvas), (255, 0, 255, 255))
    ox, oy = (canvas - w) // 2, (canvas - h) // 2
    bg.alpha_composite(big, (ox, oy))
    bg.convert("RGB").save(out)
    _j.dump({"canvas": canvas, "texel": texel, "ox": ox, "oy": oy, "w": w, "h": h}, open(out[:-4] + ".json", "w"))
    return texel


def crop_fit(generated_png, template_png, out_png):
    import json as _j, os as _o
    meta_path = template_png[:-4] + ".json"
    if not _o.path.exists(meta_path):
        return generated_png
    m = _j.load(open(meta_path))
    src = Image.open(generated_png).convert("RGBA")
    s = src.width / m["canvas"]
    box = (round(m["ox"] * s), round(m["oy"] * s), round((m["ox"] + m["w"]) * s), round((m["oy"] + m["h"]) * s))
    src.crop(box).save(out_png)
    return out_png


def template(blockout, out, target=1024):
    k = max(1, target // max(blockout.size))
    big = blockout.resize((blockout.width * k, blockout.height * k), Image.NEAREST)
    bg = Image.new("RGBA", big.size, (255, 0, 255, 255))
    bg.alpha_composite(big)
    bg.convert("RGB").save(out)
    return k


def run_generator(prompt, out, refs, edit=True, transparent=False, force=False):
    cmd = [sys.executable, IMAGE_TOOL, "--out", out, "--prompt", prompt]
    for r in refs:
        cmd += ["--ref", r]
    if edit:
        cmd.append("--edit")
    if transparent:
        cmd.append("--transparent")
    if force:
        cmd.append("--force")
    r = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8", errors="replace")
    return r.returncode, (r.stdout + r.stderr)[-2000:]


def log_source(name, prompt, files, used_by):
    path = os.path.join(ART, "SOURCES.jsonl")
    with open(path, "a", encoding="utf8") as f:
        f.write(json.dumps({"texture": name, "date": datetime.date.today().isoformat(), "model": "generator via image tool generate",
                            "prompt": prompt, "files": files, "used_by": used_by}) + "\n")

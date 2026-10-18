from PIL import Image
from art import RAMPS, COLOR_MATERIAL, nearest, lum

ALL = [c for k, v in RAMPS.items() for c in v]


def item_palette(src, mask_fn, max_colors=14, min_share=0.015):
    small = src.resize((96, 96), Image.BOX).convert("RGBA")
    counts = {}
    total = 0
    for y in range(96):
        for x in range(96):
            if not mask_fn(x / 96, y / 96):
                continue
            c = small.getpixel((x, y))
            if c[3] < 128 or (c[0] > 230 and c[1] < 40 and c[2] > 230):
                continue
            q = nearest(c[:3], ALL)
            counts[q] = counts.get(q, 0) + 1
            total += 1
    pal = [c for c, n in sorted(counts.items(), key=lambda kv: -kv[1]) if n >= total * min_share][:max_colors]
    return pal or ALL


def reduce_sprite(src_path, blockout, lockmask, out_path, samples=7, outline=True):
    src = Image.open(src_path).convert("RGBA")
    W, H = blockout.size
    bw, bh = src.width / W, src.height / H
    inside = lambda fx, fy: blockout.getpixel((min(W - 1, int(fx * W)), min(H - 1, int(fy * H))))[3] > 0
    pal = item_palette(src, inside)
    out = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    for y in range(H):
        for x in range(W):
            b = blockout.getpixel((x, y))
            if b[3] == 0:
                continue
            if lockmask.getpixel((x, y)):
                out.putpixel((x, y), b)
                continue
            votes = {}
            for sy in range(samples):
                for sx in range(samples):
                    fx = (x + 0.2 + 0.6 * (sx + 0.5) / samples) * bw
                    fy = (y + 0.2 + 0.6 * (sy + 0.5) / samples) * bh
                    c = src.getpixel((min(src.width - 1, int(fx)), min(src.height - 1, int(fy))))
                    if c[0] > 230 and c[1] < 40 and c[2] > 230:
                        continue
                    q = nearest(c[:3], pal)
                    votes[q] = votes.get(q, 0) + 1
            if not votes:
                out.putpixel((x, y), b)
                continue
            out.putpixel((x, y), max(votes.items(), key=lambda kv: kv[1])[0] + (255,))
    if outline:
        snap = out.copy()
        for y in range(H):
            for x in range(W):
                if snap.getpixel((x, y))[3] == 0 or lockmask.getpixel((x, y)):
                    continue
                edge = any(not (0 <= x + dx < W and 0 <= y + dy < H) or snap.getpixel((x + dx, y + dy))[3] == 0
                           for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
                if not edge:
                    continue
                inner = [snap.getpixel((x + dx, y + dy)) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (-1, -1), (1, -1), (-1, 1))
                         if 0 <= x + dx < W and 0 <= y + dy < H and snap.getpixel((x + dx, y + dy))[3] > 0]
                base = snap.getpixel((x, y))[:3]
                ref = max(inner, key=lambda c: lum(c)) if inner else base
                mat, idx = COLOR_MATERIAL.get(tuple(ref[:3]), (None, 0))
                if mat is None:
                    mat, idx = COLOR_MATERIAL.get(nearest(ref[:3], ALL), ("ink", 0))
                ramp = RAMPS[mat]
                col = ramp[max(0, min(idx - 3, 1))] if len(ramp) > 2 else ramp[0]
                out.putpixel((x, y), col + (255,))
    px = out.load()
    lk = lockmask.load()
    changes = []
    for y in range(1, H - 1):
        for x in range(1, W - 1):
            if lk[x, y] or px[x, y][3] == 0:
                continue
            n = [px[x + dx, y + dy] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))]
            if any(c[3] == 0 for c in n) or px[x, y] in n:
                continue
            counts = {}
            for c in n:
                counts[c] = counts.get(c, 0) + 1
            top, cnt = max(counts.items(), key=lambda kv: kv[1])
            if cnt >= 3:
                changes.append((x, y, top))
    for x, y, c in changes:
        px[x, y] = c
    out.save(out_path)
    return out

import math
from art import P
import paint

T = (0, 0, 0, 0)


def comp(c, r, base=("steel", 3), frame=None, ops=(), outline=True):
    x0, y0 = r.x, r.y
    x1, y1 = r.x + r.w - 1, r.y + r.h - 1
    c.rect(x0, y0, x1, y1, P(*base))
    if frame and r.w > 2 * frame[1] and r.h > 2 * frame[1]:
        mat, w = frame
        for i in range(w):
            c.hline(x0 + i, x1 - i, y0 + i, P(mat, 5 - i))
            c.vline(x0 + i, y0 + i, y1 - i, P(mat, 4 - i))
            c.hline(x0 + i, x1 - i, y1 - i, P(mat, 1 + i))
            c.vline(x1 - i, y0 + i, y1 - i, P(mat, 2 + i))
    if outline and r.w >= 4 and r.h >= 4:
        c.outline(x0, y0, x1, y1, P(base[0], 0), True)
    for op in ops:
        OPS[op[0]](c, x0, y0, r, *op[1:])


def op_rect(c, x0, y0, r, ax, ay, bx, by, mat, i, lock=False):
    c.rect(x0 + ax, y0 + ay, x0 + bx, y0 + by, P(mat, i), lock)


def op_inset(c, x0, y0, r, ax, ay, bx, by, mat, base=3):
    c.rect(x0 + ax, y0 + ay, x0 + bx, y0 + by, P(mat, base))
    c.hline(x0 + ax, x0 + bx, y0 + ay, P(mat, max(0, base - 2)), True)
    c.vline(x0 + ax, y0 + ay, y0 + by, P(mat, max(0, base - 1)))
    c.hline(x0 + ax, x0 + bx, y0 + by, P(mat, base + 2))
    c.vline(x0 + bx, y0 + ay, y0 + by, P(mat, base + 1))


def op_clear(c, x0, y0, r, ax, ay, bx, by):
    c.clear(x0 + ax, y0 + ay, x0 + bx, y0 + by)


def op_hole(c, x0, y0, r, ax, ay, bx, by, rim=None):
    if rim:
        c.rect(x0 + ax - 1, y0 + ay - 1, x0 + bx + 1, y0 + by + 1, P(rim, 3))
        c.hline(x0 + ax - 1, x0 + bx + 1, y0 + ay - 1, P(rim, 5))
        c.hline(x0 + ax - 1, x0 + bx + 1, y0 + by + 1, P(rim, 1))
    c.rect(x0 + ax, y0 + ay, x0 + bx, y0 + by, P("ink", 0), True)


def op_ring(c, x0, y0, r, cx, cy, rad, mat, fill=None):
    for y in range(r.h):
        for x in range(r.w):
            d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if rad - 1.2 <= d <= rad:
                c.px(x0 + x, y0 + y, P(mat, 4 if (x - cx) + (y - cy) < 0 else 2))
            elif fill and d < rad - 1.2:
                c.px(x0 + x, y0 + y, P(*fill))


def op_bolts(c, x0, y0, r, pts, mat="iron"):
    for (x, y) in pts:
        c.px(x0 + x, y0 + y, P(mat, -1), True)


def op_lamps(c, x0, y0, r, y, xs, cols, lit=False):
    for x, col in zip(xs, cols):
        c.px(x0 + x, y0 + y, P(col, 4 if lit else 1), True)
        c.px(x0 + x + 1, y0 + y, P(col, 3 if lit else 1), True)


def op_bars(c, x0, y0, r, orient, start, end, step, mat, gaps=True, width=1, a=None, b=None):
    a = 0 if a is None else a
    b = (r.h - 1 if orient == "v" else r.w - 1) if b is None else b
    for k in range(start, end + 1):
        on = (k - start) % step < width
        for t in range(a, b + 1):
            x, y = (k, t) if orient == "v" else (t, k)
            if on:
                c.px(x0 + x, y0 + y, P(mat, 4 if (k - start) % step == 0 else 2))
            elif gaps:
                c.px(x0 + x, y0 + y, T, True)


def op_stripe(c, x0, y0, r, y, mat="hazard", hazard=True, h=1):
    for yy in range(y, y + h):
        for x in range(r.w):
            if hazard:
                col = P("hazard", 2) if ((x + yy) // 2) % 2 == 0 else P("rubber", 1)
            else:
                col = P(mat, 3)
            c.px(x0 + x, y0 + yy, col)


def op_grid(c, x0, y0, r, ax, ay, n, m, cell, mat, i, gap_mat=None):
    for gy in range(m):
        for gx in range(n):
            px, py = ax + gx * cell, ay + gy * cell
            c.rect(x0 + px, y0 + py, x0 + px + cell - 2, y0 + py + cell - 2, P(mat, i))
            c.px(x0 + px, y0 + py, P(mat, min(5, i + 2)))
            if gap_mat:
                c.hline(x0 + px, x0 + px + cell - 1, y0 + py + cell - 1, P(*gap_mat))


def op_px(c, x0, y0, r, pts, mat, i, lock=True):
    for (x, y) in pts:
        c.px(x0 + x, y0 + y, P(mat, i), lock)


def op_noise(c, x0, y0, r, mat, levels, seed=7):
    import random
    rnd = random.Random(seed)
    for y in range(r.h):
        for x in range(r.w):
            if rnd.random() < 0.18:
                c.px(x0 + x, y0 + y, P(mat, rnd.choice(levels)))


def op_alpha(c, x0, y0, r, alpha):
    for y in range(r.h):
        for x in range(r.w):
            p = c.img.getpixel((x0 + x, y0 + y))
            if p[3]:
                c.img.putpixel((x0 + x, y0 + y), p[:3] + (alpha,))
                c.lock.putpixel((x0 + x, y0 + y), 255)


OPS = {k[3:]: v for k, v in globals().items() if k.startswith("op_")}
paint.PAINTERS["comp"] = comp

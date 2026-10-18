from art import P, Canvas


def _sub(c, r):
    return r.x, r.y, r.x + r.w - 1, r.y + r.h - 1


def plate(c, r, mat="steel", base=3, rivets=False, seam=None, frame=1, outline=True, lock_outline=False):
    x0, y0, x1, y1 = _sub(c, r)
    c.rect(x0, y0, x1, y1, P(mat, base))
    if frame and r.w >= 3 and r.h >= 3:
        c.hline(x0, x1, y0, P(mat, base + 2))
        c.vline(x0, y0, y1, P(mat, base + 1))
        c.hline(x0, x1, y1, P(mat, base - 2))
        c.vline(x1, y0, y1, P(mat, base - 1))
    if outline and r.w >= 4 and r.h >= 4:
        c.outline(x0, y0, x1, y1, P(mat, 0), lock_outline)
    if seam == "h" and r.h >= 6:
        m = y0 + r.h // 2
        c.hline(x0 + 1, x1 - 1, m - 1, P(mat, base - 2))
        c.hline(x0 + 1, x1 - 1, m, P(mat, base + 1))
    if seam == "v" and r.w >= 6:
        m = x0 + r.w // 2
        c.vline(m - 1, y0 + 1, y1 - 1, P(mat, base - 2))
        c.vline(m, y0 + 1, y1 - 1, P(mat, base + 1))
    if rivets and r.w >= 6 and r.h >= 6:
        for (x, y) in ((x0 + 2, y0 + 2), (x1 - 2, y0 + 2), (x0 + 2, y1 - 2), (x1 - 2, y1 - 2)):
            c.px(x, y, P("iron", -2), True)
            c.px(x, y + 1, P("iron", 1), True)


def casing(c, r, mat="steel", frame_mat="iron"):
    x0, y0, x1, y1 = _sub(c, r)
    c.rect(x0, y0, x1, y1, P(frame_mat, 3))
    c.outline(x0, y0, x1, y1, P(frame_mat, 0), True)
    c.hline(x0 + 1, x1 - 1, y0 + 1, P(frame_mat, 5))
    c.vline(x0 + 1, y0 + 1, y1 - 1, P(frame_mat, 4))
    c.hline(x0 + 1, x1 - 1, y1 - 1, P(frame_mat, 2))
    c.vline(x1 - 1, y0 + 1, y1 - 1, P(frame_mat, 2))
    c.rect(x0 + 2, y0 + 2, x1 - 2, y1 - 2, P(mat, 3))
    c.hline(x0 + 2, x1 - 2, y0 + 2, P(mat, 0), True)
    c.vline(x0 + 2, y0 + 2, y1 - 2, P(mat, 1), True)
    c.hline(x0 + 3, x1 - 2, y1 - 2, P(mat, 5))
    c.vline(x1 - 2, y0 + 3, y1 - 2, P(mat, 4))
    m = y0 + r.h // 2
    c.hline(x0 + 3, x1 - 3, m - 1, P(mat, 1), True)
    c.hline(x0 + 3, x1 - 3, m, P(mat, 4))
    for (x, y) in ((x0 + 1, y0 + 1), (x1 - 1, y0 + 1), (x0 + 1, y1 - 1), (x1 - 1, y1 - 1), (x0 + r.w // 2, y0), (x0 + r.w // 2, y1)):
        c.px(x, y, P("iron", 6), True)


def socket(c, r, mat="steel", ring="brass", hole=4, clear=False):
    plate(c, r, mat, rivets=r.w >= 10)
    x0, y0, x1, y1 = _sub(c, r)
    cx, cy = x0 + r.w // 2, y0 + r.h // 2
    rr = hole // 2 + 2
    c.rect(cx - rr, cy - rr, cx + rr - 1, cy + rr - 1, P(ring, 3))
    c.hline(cx - rr, cx + rr - 1, cy - rr, P(ring, 5))
    c.vline(cx - rr, cy - rr, cy + rr - 1, P(ring, 4))
    c.hline(cx - rr, cx + rr - 1, cy + rr - 1, P(ring, 1))
    c.vline(cx + rr - 1, cy - rr, cy + rr - 1, P(ring, 2))
    for (x, y) in ((cx - rr, cy - rr), (cx + rr - 1, cy - rr), (cx - rr, cy + rr - 1), (cx + rr - 1, cy + rr - 1)):
        c.px(x, y, P(mat, 2))
    if clear:
        c.clear(cx - hole // 2, cy - hole // 2, cx + hole // 2 - 1, cy + hole // 2 - 1)
    else:
        c.rect(cx - hole // 2, cy - hole // 2, cx + hole // 2 - 1, cy + hole // 2 - 1, P("ink", 0), True)


def flange(c, r, mat="copper", hole=None, bolts=True, collar=None):
    x0, y0, x1, y1 = _sub(c, r)
    c.rect(x0, y0, x1, y1, P(mat, 3))
    c.outline(x0, y0, x1, y1, P(mat, 1), True)
    c.hline(x0 + 1, x1 - 1, y0 + 1, P(mat, 5))
    c.vline(x0 + 1, y0 + 1, y1 - 1, P(mat, 4))
    if collar:
        c.outline(x0 + 1, y0 + 1, x1 - 1, y1 - 1, P(collar, 2), True)
    h = hole if hole is not None else max(2, min(r.w, r.h) - 4)
    cx, cy = x0 + r.w // 2, y0 + r.h // 2
    c.rect(cx - h // 2, cy - h // 2, cx + (h + 1) // 2 - 1, cy + (h + 1) // 2 - 1, P("ink", 0), True)
    c.hline(cx - h // 2, cx + (h + 1) // 2 - 1, cy - h // 2 - 1, P(mat, 0), True)
    if bolts and r.w >= 8:
        for (x, y) in ((x0 + 1, y0 + 1), (x1 - 1, y0 + 1), (x0 + 1, y1 - 1), (x1 - 1, y1 - 1)):
            c.px(x, y, P("iron", -1), True)


def pipe_side(c, r, mat="copper", along="h", bands=True):
    x0, y0, x1, y1 = _sub(c, r)
    c.rect(x0, y0, x1, y1, P(mat, 3))
    if along == "h":
        c.hline(x0, x1, y0, P(mat, 1), True)
        if r.h > 2:
            c.hline(x0, x1, y0 + 1, P(mat, 5))
        c.hline(x0, x1, y1, P(mat, 1), True)
        if r.h > 3:
            c.hline(x0, x1, y1 - 1, P(mat, 2))
        if bands and r.w >= 6:
            for bx in (x0 + 1, x1 - 2):
                c.vline(bx, y0, y1, P("iron", 3))
                c.vline(bx + 1, y0, y1, P("iron", 1))
    else:
        c.vline(x0, y0, y1, P(mat, 1), True)
        if r.w > 2:
            c.vline(x0 + 1, y0, y1, P(mat, 5))
        c.vline(x1, y0, y1, P(mat, 1), True)
        if r.w > 3:
            c.vline(x1 - 1, y0, y1, P(mat, 2))
        if bands and r.h >= 6:
            for by in (y0 + 1, y1 - 2):
                c.hline(x0, x1, by, P("iron", 3))
                c.hline(x0, x1, by + 1, P("iron", 1))


def arrow(c, r, mat="steel", arrow_mat="brass", point="up"):
    plate(c, r, mat, rivets=False)
    x0, y0, x1, y1 = _sub(c, r)
    cx = x0 + r.w // 2
    cy = y0 + r.h // 2
    pts = []
    vertical = point in ("up", "down")
    length = r.h if vertical else r.w
    across = r.w if vertical else r.h
    half = min(3, (across - 2) // 2)
    n = 2 if length >= 8 else 1
    s = 1 if point in ("up", "left") else -1
    start = 1 if s == 1 else length - 2
    for k in range(n):
        base = start + s * k * 3
        for j in range(half + 1):
            along = base + s * j
            for side in (-1, 1):
                off = j if side < 0 else j - 1
                a = (cx - 1 - off) if side < 0 else (cx + off)
                if vertical:
                    pts.append((a, y0 + along))
                else:
                    pts.append(((x0 + along), cy - 1 - off if side < 0 else cy + off))
    for (x, y) in pts:
        c.px(x, y, P(arrow_mat, 4), True)


def hub(c, r, mat="brass", notches=True):
    x0, y0, x1, y1 = _sub(c, r)
    c.rect(x0, y0, x1, y1, P(mat, 3))
    c.outline(x0, y0, x1, y1, P(mat, 1), True)
    c.hline(x0 + 1, x1 - 1, y0 + 1, P(mat, 5))
    c.vline(x0 + 1, y0 + 1, y1 - 1, P(mat, 4))
    if notches and r.w >= 6:
        for (x, y) in ((x0 + r.w // 2, y0), (x0 + r.w // 2 - 1, y1), (x0, y0 + r.h // 2), (x1, y0 + r.h // 2 - 1)):
            c.px(x, y, P(mat, 0), True)
    cx, cy = x0 + r.w // 2, y0 + r.h // 2
    c.rect(cx - 1, cy - 1, cx, cy, P("iron", 2), True)


def edge(c, r, mat="brass", base=2):
    x0, y0, x1, y1 = _sub(c, r)
    c.rect(x0, y0, x1, y1, P(mat, base))
    if r.h >= 2:
        c.hline(x0, x1, y0, P(mat, base + 2))
    if r.w >= 4:
        for x in range(x0 + 1, x1, 3):
            c.vline(x, y0, y1, P(mat, base - 1))


def slots(c, r, mat="steel", bar_mat="brass"):
    x0, y0, x1, y1 = _sub(c, r)
    c.rect(x0, y0, x1, y1, P(mat, 3))
    c.hline(x0, x1, y0, P(bar_mat, 4), True)
    c.hline(x0, x1, y1, P(bar_mat, 1), True)
    for y in range(y0 + 1, y1):
        for x in range(x0, x1 + 1):
            if (y - y0) % 3 == 0:
                c.px(x, y, P(bar_mat, 3), True)
            else:
                c.px(x, y, (0, 0, 0, 0), True)


def blade_disc(c, r, mat="brass", blade="iron"):
    x0, y0, x1, y1 = _sub(c, r)
    cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
    R = min(r.w, r.h) / 2
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            dx, dy = x + 0.5 - cx - 0.5, y + 0.5 - cy - 0.5
            d = (dx * dx + dy * dy) ** 0.5
            if d <= R * 0.35:
                c.px(x, y, P(mat, 4 if dx + dy < 0 else 2), True)
            elif d <= R - 0.2:
                import math
                a = math.atan2(dy, dx)
                k = int(((a + math.pi) / (2 * math.pi)) * 12) % 2
                c.px(x, y, P(blade, 5 if k else 2), True)
            else:
                c.px(x, y, (0, 0, 0, 0), True)


def dial(c, x0, y0, size, bezel, angle):
    import math
    x1, y1 = x0 + size - 1, y0 + size - 1
    c.ellipse(x0, y0, x1, y1, P(bezel, 3))
    c.ellipse(x0 + 1, y0 + 1, x1 - 1, y1 - 1, P("fabric", 5))
    cx, cy = x0 + (size - 1) / 2, y0 + (size - 1) / 2
    for a in (210, 150, 90, 30, -30):
        rx = cx + math.cos(math.radians(a)) * (size / 2 - 1.6)
        ry = cy - math.sin(math.radians(a)) * (size / 2 - 1.6)
        c.px(int(round(rx)), int(round(ry)), P("fabric", 1), True)
    c.px(int(round(cx + math.cos(math.radians(-30)) * (size / 2 - 1.6))), int(round(cy + math.sin(math.radians(30)) * (size / 2 - 1.6))), P("red", 2), True)
    for t in range(0, int(size / 2 - 1)):
        nx = cx + math.cos(math.radians(angle)) * t
        ny = cy - math.sin(math.radians(angle)) * t
        c.px(int(round(nx)), int(round(ny)), P("ink", 0), True)
    c.px(int(cx), int(cy), P(bezel, 1), True)


def gauge_panel(c, r, mat="steel", bezel="brass"):
    plate(c, r, mat, base=2, rivets=False, lock_outline=True)
    x0, y0, x1, y1 = _sub(c, r)
    dial(c, x0 + 1, y0 + 1, 9, bezel, 215)
    for i, bx in enumerate((x0 + 11, x0 + 12 + 0)):
        pass
    for bx in (x0 + 10, x0 + 12):
        c.rect(bx - 0, y0 + 1, bx + 1, y0 + 9, P(bezel, 3))
        c.vline(bx, y0 + 2, y0 + 8, P("ink", 0), True)
        for ty in (y0 + 2, y0 + 5, y0 + 8):
            c.px(bx + 1, ty, P(bezel, 1), True)
    c.vline(x0 + 12, y0 + 2, y0 + 8, P("ink", 0), True)
    by = y0 + 11
    for i, col in enumerate(("green", "amber", "red")):
        lx = x0 + 2 + i * 4
        c.rect(lx - 1, by - 1, lx + 2, by + 1, P(bezel, 2))
        c.px(lx, by, P(col, 1), True)
        c.px(lx + 1, by, P(col, 1), True)


PAINTERS = {k: v for k, v in globals().items() if callable(v) and not k.startswith("_") and k not in ("P", "Canvas")}


def paint_model(model):
    c = Canvas(model.atlas_size, model.atlas_size)
    for r in model.regions.values():
        PAINTERS[r.kind](c, r, **r.params)
    return c

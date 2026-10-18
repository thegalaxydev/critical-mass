from art import P, Canvas

T = (0, 0, 0, 0)


def box_faces(u, v, w, h, d):
    return {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h), "back": (u + d + w + d, v + d, w, h),
    }


def fill(c, rect, col, lock=False):
    x, y, w, h = rect
    c.rect(x, y, x + w - 1, y + h - 1, col, lock)


def fabric_face(c, rect, base=4, seams=True):
    x, y, w, h = rect
    fill(c, rect, P("fabric", base))
    if w >= 3 and h >= 3:
        c.hline(x, x + w - 1, y, P("fabric", base + 1))
        c.vline(x + w - 1, y, y + h - 1, P("fabric", base - 1))
        c.hline(x, x + w - 1, y + h - 1, P("fabric", base - 2))


def layer1():
    c = Canvas(64, 32)
    head = box_faces(0, 0, 8, 8, 8)
    for k, r in head.items():
        fabric_face(c, r)
    x, y, w, h = head["front"]
    fill(c, (x + 1, y + 2, 6, 3), T, True)
    c.hline(x + 1, x + 6, y + 1, P("brass", 4), True)
    c.vline(x, y + 1, y + 5, P("brass", 3))
    c.vline(x + 7, y + 1, y + 5, P("brass", 3))
    c.hline(x + 1, x + 6, y + 5, P("brass", 2), True)
    fill(c, (x + 2, y + 6, 4, 2), P("iron", 3))
    c.px(x + 3, y + 7, P("rubber", 1), True)
    c.px(x + 4, y + 7, P("rubber", 1), True)
    for side in ("right", "left"):
        sx, sy, sw, sh = head[side]
        c.vline(sx + 3, sy, sy + sh - 1, P("hazard", 2))
        c.px(sx + 5, sy + 4, P("brass", 4), True)
    bx, by, bw, bh = head["back"]
    c.vline(bx + 3, by, by + bh - 1, P("fabric", 2))
    c.vline(bx + 4, by, by + bh - 1, P("fabric", 5))
    tx, ty, tw, th = head["top"]
    c.hline(tx, tx + tw - 1, ty + 3, P("fabric", 2))
    for k, r in box_faces(32, 0, 8, 8, 8).items():
        fill(c, r, T, True)
    body = box_faces(16, 16, 8, 12, 4)
    for k, r in body.items():
        fabric_face(c, r)
    fx, fy, fw, fh = body["front"]
    c.vline(fx + 3, fy, fy + fh - 1, P("brass", 3))
    c.vline(fx + 4, fy, fy + fh - 1, P("brass", 2))
    c.hline(fx, fx + fw - 1, fy + 5, P("hazard", 2))
    for (px, py) in ((fx + 1, fy + 2), (fx + 6, fy + 2), (fx + 1, fy + 9), (fx + 6, fy + 9)):
        c.px(px, py, P("brass", 5), True)
    bx, by, bw, bh = body["back"]
    c.hline(bx, bx + bw - 1, by + 5, P("hazard", 2))
    fill(c, (bx + 2, by + 1, 4, 3), P("fabric", 2))
    for side in ("right", "left"):
        sx, sy, sw, sh = body[side]
        c.hline(sx, sx + sw - 1, sy + 5, P("hazard", 2))
    arm = box_faces(40, 16, 4, 12, 4)
    for k, r in arm.items():
        fabric_face(c, r)
    for k in ("right", "front", "left", "back"):
        x, y, w, h = arm[k]
        c.hline(x, x + w - 1, y + 4, P("hazard", 2))
        fill(c, (x, y + 9, w, 3), P("rubber", 2))
        c.hline(x, x + w - 1, y + 9, P("rubber", 3))
    fill(c, arm["bottom"], P("rubber", 1))
    leg = box_faces(0, 16, 4, 12, 4)
    for k, r in leg.items():
        fill(c, r, T, True)
    for k in ("right", "front", "left", "back"):
        x, y, w, h = leg[k]
        fill(c, (x, y + 6, w, 6), P("rubber", 2))
        c.hline(x, x + w - 1, y + 6, P("rubber", 3))
        c.hline(x, x + w - 1, y + 7, P("hazard", 2))
        c.hline(x, x + w - 1, y + 11, P("rubber", 0))
        fill(c, (x, y, w, 6), T, True)
    fill(c, leg["bottom"], P("rubber", 0))
    c.px(leg["front"][0] + 1, leg["front"][1] + 9, P("brass", 4), True)
    return c


def layer2():
    c = Canvas(64, 32)
    for k, r in box_faces(0, 0, 8, 8, 8).items():
        fill(c, r, T, True)
    for k, r in box_faces(32, 0, 8, 8, 8).items():
        fill(c, r, T, True)
    for k, r in box_faces(40, 16, 4, 12, 4).items():
        fill(c, r, T, True)
    body = box_faces(16, 16, 8, 12, 4)
    for k, r in body.items():
        fill(c, r, T, True)
    for k in ("right", "front", "left", "back"):
        x, y, w, h = body[k]
        fabric_face(c, (x, y + 7, w, 5), 3)
        c.hline(x, x + w - 1, y + 8, P("brass", 3))
    c.px(body["front"][0] + 3, body["front"][1] + 8, P("brass", 5), True)
    c.px(body["front"][0] + 4, body["front"][1] + 8, P("brass", 5), True)
    fill(c, body["bottom"], P("fabric", 2))
    leg = box_faces(0, 16, 4, 12, 4)
    for k, r in leg.items():
        fabric_face(c, r)
    for k in ("right", "front", "left", "back"):
        x, y, w, h = leg[k]
        c.hline(x, x + w - 1, y + 5, P("hazard", 2))
        fill(c, (x, y + 10, w, 2), P("rubber", 2))
    fill(c, leg["top"], P("fabric", 3))
    return c


DESCRIPTIONS = {
    "protective_layer_1": (
        "64x32 Minecraft armor texture sheet (outer layer) using the standard humanoid UV layout. Top-left 32x16 = hood (head cube: top, bottom, right, front, left, back faces): "
        "off-white rubberised canvas, the front face has a brass-rimmed open face window (keep the window transparent magenta) and a small iron respirator below it, yellow hazard strips on the sides. "
        "Top-right 32x16 region stays fully transparent magenta. Middle (x16-40, y16-32) = suit torso: canvas with a brass zip down the front, yellow hazard stripe across, brass rivets. "
        "Right block (x40-56, y16-32) = sleeves: canvas with a yellow band and black rubber gloves at the bottom. Left block (x0-16, y16-32) = boots: only the lower half is painted as black rubber boots with a yellow band; the upper half stays transparent magenta."
    ),
    "protective_layer_2": (
        "64x32 Minecraft armor texture sheet (inner leggings layer) using the standard humanoid UV layout. Only two areas are painted, everything else stays transparent magenta: "
        "the lower part of the torso block (x16-40, y16-32) as a canvas waist band with a brass belt and buckle, and the leg block (x0-16, y16-32) as off-white rubberised canvas trousers with a yellow knee band and black rubber ankle cuffs."
    ),
}

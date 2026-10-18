import json
import math
import os
import sys
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
MOD = os.environ.get("CCM_MOD", r"D:\Minecraft\create-critical-mass-1.21.1")
RES = os.path.join(MOD, "src", "main", "resources", "assets")
REFS = os.path.join(ROOT, "refs", "create", "assets")

SHADE = {"up": 1.0, "down": 0.5, "north": 0.8, "south": 0.8, "east": 0.6, "west": 0.6}
NORMAL = {"north": (0, 0, -1), "south": (0, 0, 1), "east": (1, 0, 0), "west": (-1, 0, 0), "up": (0, 1, 0), "down": (0, -1, 0)}


def tex_path(ref):
    ns, path = ref.split(":") if ":" in ref else ("minecraft", ref)
    ov = os.environ.get("ISO_TEX_OVERRIDE")
    if ov and ns == "criticalmass":
        q = os.path.join(ov, path.split("/")[-1] + ".png")
        if os.path.exists(q):
            return q
    for base in (RES, REFS):
        p = os.path.join(base, ns, "textures", path + ".png")
        if os.path.exists(p):
            return p
    return None


_cache = {}


def load_tex(ref):
    if ref not in _cache:
        p = tex_path(ref)
        im = Image.open(p).convert("RGBA") if p else Image.new("RGBA", (16, 16), (255, 0, 255, 255))
        if im.height > im.width:
            im = im.crop((0, 0, im.width, im.width))
        _cache[ref] = im
    return _cache[ref]


def load_model(ref):
    ns, path = ref.split(":")
    p = os.path.join(RES, ns, "models", path + ".json")
    return json.load(open(p, encoding="utf8"))


def resolve(textures, key):
    seen = 0
    while key.startswith("#") and seen < 8:
        key = textures.get(key[1:], key)
        seen += 1
    return key


def face_corners(frm, to, d):
    x0, y0, z0 = frm
    x1, y1, z1 = to
    if d == "north":
        return [(x1, y1, z0), (x0, y1, z0), (x0, y0, z0), (x1, y0, z0)]
    if d == "south":
        return [(x0, y1, z1), (x1, y1, z1), (x1, y0, z1), (x0, y0, z1)]
    if d == "east":
        return [(x1, y1, z1), (x1, y1, z0), (x1, y0, z0), (x1, y0, z1)]
    if d == "west":
        return [(x0, y1, z0), (x0, y1, z1), (x0, y0, z1), (x0, y0, z0)]
    if d == "up":
        return [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)]
    return [(x0, y0, z1), (x1, y0, z1), (x1, y0, z0), (x0, y0, z0)]


def rot_point(p, yaw, pitch, center=(8, 8, 8)):
    x, y, z = (p[i] - center[i] for i in range(3))
    cy, sy = math.cos(yaw), math.sin(yaw)
    x, z = x * cy - z * sy, x * sy + z * cy
    cp, sp = math.cos(pitch), math.sin(pitch)
    y, z = y * cp - z * sp, y * sp + z * cp
    return x, y, z


def rot_elem(p, rot):
    if not rot:
        return p
    a = math.radians(rot["angle"])
    o = rot["origin"]
    x, y, z = (p[i] - o[i] for i in range(3))
    c, s = math.cos(a), math.sin(a)
    if rot["axis"] == "y":
        x, z = x * c + z * s, -x * s + z * c
    elif rot["axis"] == "x":
        y, z = y * c - z * s, y * s + z * c
    else:
        x, y = x * c - y * s, x * s + y * c
    return (x + o[0], y + o[1], z + o[2])


def homography(src_pts, dst_pts):
    import numpy as np
    A = []
    B = []
    for (x, y), (u, v) in zip(dst_pts, src_pts):
        A.append([x, y, 1, 0, 0, 0, -u * x, -u * y])
        A.append([0, 0, 0, x, y, 1, -v * x, -v * y])
        B += [u, v]
    return list(np.linalg.solve(np.array(A, float), np.array(B, float)))


def uv_rect(face, tex):
    u0, v0, u1, v1 = face.get("uv", [0, 0, 16, 16])
    W, H = tex.size
    pts = [(u0 * W / 16, v0 * H / 16), (u1 * W / 16, v0 * H / 16), (u1 * W / 16, v1 * H / 16), (u0 * W / 16, v1 * H / 16)]
    r = (face.get("rotation", 0) // 90) % 4
    return pts[r:] + pts[:r] if r else pts


def render(model_refs, size=256, yaw_deg=225, pitch_deg=30, supersample=4, scale=None, offset=(0, 0, 0)):
    yaw, pitch = math.radians(yaw_deg), -math.radians(pitch_deg)
    S = size * supersample
    k = scale or (S / 28.0)
    canvas = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    quads = []
    if isinstance(model_refs, str):
        model_refs = [model_refs]
    for ref in model_refs:
        xf = None
        if isinstance(ref, tuple):
            ref, xf = ref
        m = load_model(ref)
        texs = m.get("textures", {})
        for el in m.get("elements", []):
            for d, face in el["faces"].items():
                corners = [rot_elem(c, el.get("rotation")) for c in face_corners(el["from"], el["to"], d)]
                n = NORMAL[d]
                if el.get("rotation"):
                    a, b, cc = corners[0], corners[1], corners[2]
                    u = [b[i] - a[i] for i in range(3)]
                    v = [cc[i] - a[i] for i in range(3)]
                    n = (u[1] * v[2] - u[2] * v[1], u[2] * v[0] - u[0] * v[2], u[0] * v[1] - u[1] * v[0])
                    ln = math.sqrt(sum(c * c for c in n)) or 1
                    n = tuple(-c / ln for c in n)
                if xf:
                    corners = [xf(c) for c in corners]
                    n = tuple(xf((8 + n[0], 8 + n[1], 8 + n[2]))[i] - xf((8, 8, 8))[i] for i in range(3))
                rn = rot_point((8 + n[0], 8 + n[1], 8 + n[2]), yaw, pitch)
                r0 = rot_point((8, 8, 8), yaw, pitch)
                if rn[2] - r0[2] <= 1e-6:
                    pass
                proj = [rot_point((c[0] + offset[0], c[1] + offset[1], c[2] + offset[2]), yaw, pitch) for c in corners]
                facing = (rn[2] - r0[2])
                if facing > -1e-6:
                    continue
                depth = sum(p[2] for p in proj) / 4
                tex = load_tex(resolve(texs, face["texture"]))
                shade = SHADE[d] if not el.get("rotation") else 0.85
                if el.get("shade") is False:
                    shade = 1.0
                quads.append((depth, proj, tex, uv_rect(face, tex), shade))
    quads.sort(key=lambda q: -q[0])
    for depth, proj, tex, uvs, shade in quads:
        dst = [(S / 2 + p[0] * k, S / 2 - p[1] * k) for p in proj]
        try:
            coeffs = homography(uvs, dst)
        except Exception:
            continue
        warped = tex.transform((S, S), Image.PERSPECTIVE, coeffs, Image.NEAREST)
        from PIL import ImageDraw
        mask = Image.new("L", (S, S), 0)
        ImageDraw.Draw(mask).polygon(dst, fill=255)
        r, g, b, a = warped.split()
        if shade != 1.0:
            r = r.point(lambda v: int(v * shade))
            g = g.point(lambda v: int(v * shade))
            b = b.point(lambda v: int(v * shade))
        from PIL import ImageChops
        a = ImageChops.multiply(a, mask)
        canvas.alpha_composite(Image.merge("RGBA", (r, g, b, a)))
    return canvas.resize((size, size), Image.LANCZOS) if supersample > 1 else canvas


if __name__ == "__main__":
    out = sys.argv[1]
    refs = sys.argv[2:]
    yaw = float(os.environ.get("YAW", 225))
    render(refs, int(os.environ.get("SIZE", 384)), yaw).save(out)
    print(out)

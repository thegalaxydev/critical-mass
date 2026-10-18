import math

DIRS = ["north", "south", "east", "west", "up", "down"]
VEC = {"north": (0, 0, -1), "south": (0, 0, 1), "east": (1, 0, 0), "west": (-1, 0, 0), "up": (0, 1, 0), "down": (0, -1, 0)}
AXIS_OF = {"north": "z", "south": "z", "east": "x", "west": "x", "up": "y", "down": "y"}


def face_dims(frm, to, d):
    sx, sy, sz = (abs(to[i] - frm[i]) for i in range(3))
    if d in ("north", "south"):
        return sx, sy
    if d in ("east", "west"):
        return sz, sy
    return sx, sz


class Region:
    def __init__(self, key, w, h, kind, params):
        self.key, self.w, self.h, self.kind, self.params = key, w, h, kind, params
        self.x = self.y = 0


class Model:
    def __init__(self, name, atlas_name=None, render_type=None, ambient=True, share=None):
        self.name = name
        self.atlas = share.atlas if share else (atlas_name or name)
        self.elements = []
        self.share = share
        self.regions = share.regions if share else {}
        self.render_type = render_type
        self.ambient = ambient
        self.atlas_size = None
        self.extra_textures = {}

    def region(self, key, w, h, kind, **params):
        w, h = int(round(w)), int(round(h))
        if key in self.regions:
            r = self.regions[key]
            if (r.w, r.h) == (w, h):
                return r
            key = "%s_%dx%d" % (key, w, h)
            if key in self.regions:
                return self.regions[key]
        r = Region(key, w, h, kind, params)
        self.regions[key] = r
        return r

    def box(self, frm, to, faces, rotation=None, shade=True, name=None):
        el = {"from": list(frm), "to": list(to), "faces": {}, "name": name}
        for d, f in faces.items():
            if f is None:
                continue
            if isinstance(f, tuple) and f[0] == "ext":
                el["faces"][d] = {"ext": f[1], "uv": f[2]} | ({"rotation": f[3]} if len(f) > 3 else {})
                continue
            key, kind, params = f[0], f[1], (f[2] if len(f) > 2 else {})
            w, h = face_dims(frm, to, d)
            reg = self.region(key, w, h, kind, **{k: v for k, v in params.items() if not k.startswith("_")})
            el["faces"][d] = {"region": reg.key} | ({"rotation": params.get("_rot")} if params.get("_rot") else {})
        if rotation:
            el["rotation"] = rotation
        if not shade:
            el["shade"] = False
        self.elements.append(el)
        return el

    def pack(self):
        if self.share:
            self.atlas_size = self.share.pack() if self.share.atlas_size is None else self.share.atlas_size
            return self.atlas_size
        regs = sorted(self.regions.values(), key=lambda r: (-r.h, -r.w))
        area = sum(max(1, r.w) * max(1, r.h) for r in regs)
        size = 16
        while True:
            if size * size >= area and self._try_pack(regs, size):
                break
            size *= 2
            if size > 256:
                raise ValueError("atlas too large for " + self.name)
        self.atlas_size = size
        return size

    def _try_pack(self, regs, size):
        x = y = shelf = 0
        placed = []
        for r in regs:
            w, h = max(1, r.w), max(1, r.h)
            if w > size or h > size:
                return False
            if x + w > size:
                x = 0
                y += shelf
                shelf = 0
            if y + h > size:
                return False
            placed.append((r, x, y))
            x += w
            shelf = max(shelf, h)
        for r, px, py in placed:
            r.x, r.y = px, py
        return True

    def to_json(self, ns, texture_root="block"):
        if self.share:
            self.atlas_size = self.share.atlas_size or self.share.pack()
        if self.atlas_size is None:
            self.pack()
        S = self.atlas_size
        tex = {"0": "%s:%s/%s" % (ns, texture_root, self.atlas), "particle": "%s:%s/%s" % (ns, texture_root, self.atlas)}
        tex.update(self.extra_textures)
        ext_keys = {}
        els = []
        for el in self.elements:
            faces = {}
            for d, f in el["faces"].items():
                if "ext" in f:
                    if f["ext"] not in ext_keys:
                        k = "e%d" % len(ext_keys)
                        ext_keys[f["ext"]] = k
                        tex[k] = f["ext"]
                    face = {"uv": f["uv"], "texture": "#" + ext_keys[f["ext"]]}
                    if f.get("rotation"):
                        face["rotation"] = f["rotation"]
                else:
                    r = self.regions[f["region"]]
                    u0, v0 = r.x * 16 / S, r.y * 16 / S
                    u1, v1 = (r.x + max(1, r.w)) * 16 / S, (r.y + max(1, r.h)) * 16 / S
                    face = {"uv": [round(u0, 4), round(v0, 4), round(u1, 4), round(v1, 4)], "texture": "#0"}
                    if f.get("rotation"):
                        face["rotation"] = f["rotation"]
                faces[d] = face
            e = {"from": el["from"], "to": el["to"], "faces": faces}
            if el.get("rotation"):
                e["rotation"] = el["rotation"]
            if el.get("shade") is False:
                e["shade"] = False
            if el.get("name"):
                e["name"] = el["name"]
            els.append(e)
        out = {"parent": "minecraft:block/block", "textures": tex, "elements": els}
        if self.render_type:
            out["render_type"] = self.render_type
        if not self.ambient:
            out["ambientocclusion"] = False
        return out


def rot_x(v, a):
    x, y, z = v
    for _ in range((a // 90) % 4):
        x, y, z = x, z, -y
    return (x, y, z)


def rot_y(v, a):
    x, y, z = v
    for _ in range((a // 90) % 4):
        x, y, z = -z, y, x
    return (x, y, z)


def apply_xy(v, x, y):
    return rot_y(rot_x(v, x), y)


def dir_of(v):
    for d, dv in VEC.items():
        if dv == tuple(int(round(c)) for c in v):
            return d
    return None


def axis_of_vec(v):
    return "xyz"[[abs(c) for c in v].index(max(abs(c) for c in v))]


def solve_rotation(authored_facing, authored_axis, want_facing, want_axis):
    av = VEC[authored_facing]
    axv = {"x": (1, 0, 0), "y": (0, 1, 0), "z": (0, 0, 1)}[authored_axis] if authored_axis else None
    for x in (0, 90, 180, 270):
        for y in (0, 90, 180, 270):
            f = dir_of(apply_xy(av, x, y))
            if f != want_facing:
                continue
            if axv is not None and axis_of_vec(apply_xy(axv, x, y)) != want_axis:
                continue
            return x, y
    return None


def rotate_model_about_z(model_json, quarter_turns=1):
    import copy
    m = copy.deepcopy(model_json)

    def rp(p):
        x, y, z = p
        for _ in range(quarter_turns % 4):
            x, y = 16 - y, x
        return [x, y, z]

    face_map = {"up": "west", "west": "down", "down": "east", "east": "up", "north": "north", "south": "south"}
    for el in m["elements"]:
        a, b = rp(el["from"]), rp(el["to"])
        el["from"] = [min(a[i], b[i]) for i in range(3)]
        el["to"] = [max(a[i], b[i]) for i in range(3)]
        faces = el["faces"]
        for _ in range(quarter_turns % 4):
            nf = {}
            for d, f in faces.items():
                nd = face_map[d]
                f = dict(f)
                if d in ("north", "south"):
                    f["rotation"] = (f.get("rotation", 0) + (90 if d == "north" else 270)) % 360
                    if f["rotation"] == 0:
                        f.pop("rotation")
                nf[nd] = f
            faces = nf
        el["faces"] = faces
        if "rotation" in el:
            r = el["rotation"]
            ax = {"x": "y", "y": "x", "z": "z"}[r["axis"]]
            o = rp(r["origin"])
            el["rotation"] = {"angle": r["angle"] * (-1 if r["axis"] == "y" else 1), "axis": ax, "origin": o}
    return m

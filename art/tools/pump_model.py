import base64
import json
import os
import sys

from PIL import Image

import iso

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SOURCE = os.path.join(ROOT, "art", "blockbench", "mechanical_pump.bbmodel")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "criticalmass")
NAME = "circulation_pump"
TEXTURE = "criticalmass:block/" + NAME
CENTRE = (8.0, 8.0, 8.0)
DIRECTIONS = ("north", "south", "east", "west", "up", "down")
OUTLET_NORTH = ("y", -1, CENTRE)
OUTLET_UP = ("z", -1, CENTRE)
SIDEWAYS = ("z", 1, CENTRE)
VARIANTS = {
    "north": ({}, "false"), "south": ({"y": 180}, "false"), "east": ({"y": 90}, "true"), "west": ({"y": 270}, "true"),
    "up": ({"x": 270}, "false"), "down": ({"x": 90}, "false"),
}


def spin(v, axis, quarters):
    x, y, z = v
    for _ in range(quarters % 4):
        if axis == "y":
            x, z = z, -x
        elif axis == "x":
            y, z = -z, y
        else:
            x, y = -y, x
    return x, y, z


def move(point, steps):
    p = tuple(point)
    for axis, quarters, centre in steps:
        v = spin(tuple(p[i] - centre[i] for i in range(3)), axis, quarters)
        p = tuple(round(v[i] + centre[i], 5) for i in range(3))
    return p


def direct(vector, steps):
    v = tuple(vector)
    for axis, quarters, _ in steps:
        v = spin(v, axis, quarters)
    return v


def close(a, b):
    return all(abs(a[i] - b[i]) < 1e-4 for i in range(3))


def bake(frm, to, faces, steps):
    corners = [move((x, y, z), steps) for x in (frm[0], to[0]) for y in (frm[1], to[1]) for z in (frm[2], to[2])]
    lo = [min(c[i] for c in corners) for i in range(3)]
    hi = [max(c[i] for c in corners) for i in range(3)]
    out = {}
    for old, f in faces.items():
        moved = [move(c, steps) for c in iso.face_corners(frm, to, old)]
        r = (f.get("rotation", 0) // 90) % 4
        for new in DIRECTIONS:
            target = iso.face_corners(lo, hi, new)
            if not all(any(close(m, t) for t in target) for m in moved):
                continue
            shifts = set()
            for j, t in enumerate(target):
                i = next(k for k, m in enumerate(moved) if close(m, t))
                shifts.add((i + r - j) % 4)
            assert len(shifts) == 1, (old, new, shifts)
            g = {"uv": f["uv"], "texture": f["texture"]}
            shift = shifts.pop()
            if shift:
                g["rotation"] = shift * 90
            out[new] = g
            break
        else:
            raise AssertionError(("no face", old))
    return lo, hi, out


def quarter_turns(angle):
    q = angle / 90.0
    assert abs(q - round(q)) < 1e-6, angle
    return int(round(q))


def tidy(value):
    r = round(value, 4)
    return int(r) if r == int(r) else r


def faces_of(cube, scale):
    out = {}
    for d, f in cube["faces"].items():
        if f.get("texture") is None:
            continue
        g = {"uv": [tidy(v * scale) for v in f["uv"]], "texture": "#0"}
        if f.get("rotation"):
            g["rotation"] = f["rotation"]
        out[d] = g
    return out


def element(name, frm, to, faces, rotation=None):
    e = {"name": name, "from": [tidy(v) for v in frm], "to": [tidy(v) for v in to], "faces": faces}
    if rotation:
        e["rotation"] = rotation
    return e


def body_element(cube, scale, extra):
    rot = cube.get("rotation") or [0, 0, 0]
    assert abs(rot[0]) < 1e-6 and abs(rot[2]) < 1e-6, (cube["name"], rot)
    steps = [("y", quarter_turns(rot[1]), tuple(cube.get("origin") or CENTRE))] + extra
    frm, to, faces = bake(cube["from"], cube["to"], faces_of(cube, scale), steps)
    return element(cube["name"], frm, to, faces)


def wheel_element(cube, scale, extra):
    rot = cube.get("rotation") or [0, 0, 0]
    own = rot[1]
    origin = tuple(cube.get("origin") or CENTRE)
    assert abs(rot[0]) < 1e-6 and abs(own) in (0, 22.5, 45), (cube["name"], rot)
    steps = [("z", quarter_turns(rot[2]), origin)] + extra
    frm, to, faces = bake(cube["from"], cube["to"], faces_of(cube, scale), steps)
    rotation = None
    if own:
        axis = direct((0, 1, 0), steps)
        name = "xyz"[next(i for i in range(3) if abs(axis[i]) > 0.5)]
        sign = 1 if sum(axis) > 0 else -1
        rotation = {"angle": own * sign, "axis": name, "origin": [tidy(v) for v in move(origin, steps)]}
    return element(cube["name"], frm, to, faces, rotation)


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=1)
        f.write("\n")


def main():
    project = json.load(open(SOURCE, encoding="utf-8"))
    scale = 16.0 / project["resolution"]["width"]
    cubes = {c["uuid"]: c for c in project["elements"]}
    names = {g["uuid"]: g["name"] for g in project.get("groups", [])}

    def gather(node):
        if isinstance(node, str):
            return [cubes[node]]
        return [c for child in node.get("children", []) for c in gather(child)]
    body_cubes, wheel_cubes = [], []
    for node in project["outliner"]:
        name = "" if isinstance(node, str) else node.get("name") or names.get(node.get("uuid"), "")
        (wheel_cubes if name.startswith("flywheel") else body_cubes).extend(gather(node))
    assert body_cubes and wheel_cubes, (len(body_cubes), len(wheel_cubes))
    source = project["textures"][0]["source"]
    png = os.path.join(ASSETS, "textures", "block", NAME + ".png")
    with open(png, "wb") as f:
        f.write(base64.b64decode(source.split(",", 1)[1]))
    textures = {"0": TEXTURE, "particle": TEXTURE}
    body = [body_element(c, scale, [OUTLET_NORTH]) for c in body_cubes]
    sideways = [body_element(c, scale, [OUTLET_NORTH, SIDEWAYS]) for c in body_cubes]
    wheel = [wheel_element(c, scale, [OUTLET_UP]) for c in wheel_cubes]
    mounted = [wheel_element(c, scale, [OUTLET_NORTH]) for c in wheel_cubes]
    models = os.path.join(ASSETS, "models", "block")
    write(os.path.join(models, NAME + ".json"), {"parent": "minecraft:block/block", "textures": textures, "elements": body})
    write(os.path.join(models, NAME + "_sideways.json"), {"parent": "minecraft:block/block", "textures": textures, "elements": sideways})
    write(os.path.join(models, NAME + "_rotor.json"), {"textures": textures, "elements": wheel})
    whole = {"parent": "minecraft:block/block", "textures": textures, "elements": body + mounted}
    write(os.path.join(models, NAME + "_item.json"), whole)
    write(os.path.join(ASSETS, "models", "item", NAME + ".json"), {"parent": "criticalmass:block/" + NAME + "_item"})
    variants = {}
    for facing, (rotation, upright) in VARIANTS.items():
        for along in ("true", "false"):
            variant = {"model": "criticalmass:block/" + NAME + ("" if along == upright else "_sideways")}
            variant.update(rotation)
            variants["axis_along_first=%s,facing=%s" % (along, facing)] = variant
    write(os.path.join(ASSETS, "blockstates", NAME + ".json"), {"variants": variants})
    for stale in (os.path.join(models, NAME + "_vertical.json"), os.path.join(ASSETS, "textures", "block", NAME + "_rotor.png")):
        if os.path.exists(stale):
            os.remove(stale)
    print(len(body), "body cubes,", len(wheel), "wheel cubes")
    spans = [[min(e["from"][i] for e in body + mounted) for i in range(3)], [max(e["to"][i] for e in body + mounted) for i in range(3)]]
    print("extent", spans)
    if len(sys.argv) > 1:
        sheets = []
        for model in (whole, {"textures": textures, "elements": sideways + mounted}, {"textures": textures, "elements": wheel}):
            iso.load_model = lambda ref, model=model: model
            iso._cache.clear()
            sheets += [iso.render(TEXTURE, 300, 225, 30), iso.render(TEXTURE, 300, 315, 30)]
        sheet = Image.new("RGBA", (300 * len(sheets), 300), (88, 110, 74, 255))
        for i, v in enumerate(sheets):
            sheet.alpha_composite(v, (300 * i, 0))
        sheet.save(sys.argv[1])


if __name__ == "__main__":
    main()

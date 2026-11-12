import base64
import json
import math
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
BODY_TURN = ("y", -1)
WHEEL_TURN = ("x", 1)
ROTATIONS = {"north": {}, "south": {"y": 180}, "east": {"y": 90}, "west": {"y": 270}, "up": {"x": 270}, "down": {"x": 90}}


def turn(point, axis, quarters):
    x, y, z = (point[i] - CENTRE[i] for i in range(3))
    for _ in range(quarters % 4):
        if axis == "y":
            x, z = z, -x
        elif axis == "x":
            y, z = -z, y
        else:
            x, y = -y, x
    return (round(x + CENTRE[0], 5), round(y + CENTRE[1], 5), round(z + CENTRE[2], 5))


def close(a, b):
    return all(abs(a[i] - b[i]) < 1e-4 for i in range(3))


def bake(frm, to, faces, axis, quarters):
    if quarters % 4 == 0:
        return list(frm), list(to), {d: dict(f) for d, f in faces.items()}
    corners = [turn(c, axis, quarters) for c in ((x, y, z) for x in (frm[0], to[0]) for y in (frm[1], to[1]) for z in (frm[2], to[2]))]
    lo = [min(c[i] for c in corners) for i in range(3)]
    hi = [max(c[i] for c in corners) for i in range(3)]
    out = {}
    for old, f in faces.items():
        moved = [turn(c, axis, quarters) for c in iso.face_corners(frm, to, old)]
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


def body_element(cube, scale):
    rot = cube.get("rotation") or [0, 0, 0]
    assert abs(rot[0]) < 1e-6 and abs(rot[2]) < 1e-6, (cube["name"], rot)
    frm, to, faces = bake(cube["from"], cube["to"], faces_of(cube, scale), "y", quarter_turns(rot[1]) + BODY_TURN[1])
    return element(cube["name"], frm, to, faces)


def wheel_element(cube, scale, around_pipe):
    rot = cube.get("rotation") or [0, 0, 0]
    own = rot[1]
    assert abs(rot[0]) < 1e-6 and abs(own) in (0, 22.5, 45), (cube["name"], rot)
    faces = faces_of(cube, scale)
    if not around_pipe:
        return element(cube["name"], cube["from"], cube["to"], faces, {"angle": own, "axis": "y", "origin": list(CENTRE)} if own else None)
    frm, to, faces = bake(cube["from"], cube["to"], faces, WHEEL_TURN[0], WHEEL_TURN[1])
    return element(cube["name"], frm, to, faces, {"angle": own, "axis": "z", "origin": list(CENTRE)} if own else None)


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=1)
        f.write("\n")


def main():
    project = json.load(open(SOURCE, encoding="utf-8"))
    scale = 16.0 / project["resolution"]["width"]
    cubes = {c["uuid"]: c for c in project["elements"]}
    groups = {}
    for node in project["outliner"]:
        if isinstance(node, dict):
            name = node.get("name") or next(g["name"] for g in project.get("groups", []) if g["uuid"] == node["uuid"])
            groups[name] = [cubes[c] for c in node["children"] if isinstance(c, str)]
    assert "flywheel" in groups and len(groups) > 1, list(groups)
    body_cubes = [c for name, members in groups.items() if name != "flywheel" for c in members]
    source = project["textures"][0]["source"]
    png = os.path.join(ASSETS, "textures", "block", NAME + ".png")
    with open(png, "wb") as f:
        f.write(base64.b64decode(source.split(",", 1)[1]))
    textures = {"0": TEXTURE, "particle": TEXTURE}
    body = [body_element(c, scale) for c in body_cubes]
    wheel = [wheel_element(c, scale, False) for c in groups["flywheel"]]
    mounted = [wheel_element(c, scale, True) for c in groups["flywheel"]]
    models = os.path.join(ASSETS, "models", "block")
    write(os.path.join(models, NAME + ".json"), {"parent": "minecraft:block/block", "textures": textures, "elements": body})
    write(os.path.join(models, NAME + "_rotor.json"), {"textures": textures, "elements": wheel})
    whole = {"parent": "minecraft:block/block", "textures": textures, "elements": body + mounted}
    write(os.path.join(models, NAME + "_item.json"), whole)
    write(os.path.join(ASSETS, "models", "item", NAME + ".json"), {"parent": "criticalmass:block/" + NAME + "_item"})
    variants = {}
    for facing, rotation in ROTATIONS.items():
        for along in ("true", "false"):
            variant = {"model": "criticalmass:block/" + NAME}
            variant.update(rotation)
            variants["axis_along_first=%s,facing=%s" % (along, facing)] = variant
    write(os.path.join(ASSETS, "blockstates", NAME + ".json"), {"variants": variants})
    for stale in (os.path.join(models, NAME + "_vertical.json"), os.path.join(ASSETS, "textures", "block", NAME + "_rotor.png")):
        if os.path.exists(stale):
            os.remove(stale)
    print(len(body), "body cubes,", len(wheel), "wheel cubes")
    if len(sys.argv) > 1:
        iso.load_model = lambda ref: whole
        iso._cache.clear()
        views = [iso.render(TEXTURE, 320, 225, 30), iso.render(TEXTURE, 320, 315, 30), iso.render(TEXTURE, 320, 180, 0), iso.render(TEXTURE, 320, 270, 0)]
        sheet = Image.new("RGBA", (320 * len(views), 320), (88, 110, 74, 255))
        for i, v in enumerate(views):
            sheet.alpha_composite(v, (320 * i, 0))
        sheet.save(sys.argv[1])


if __name__ == "__main__":
    main()

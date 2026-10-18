import copy
import json
import os

from modelgen import solve_rotation

NS = "criticalmass"
FACINGS = ["north", "south", "east", "west", "up", "down"]
HFACINGS = ["north", "east", "south", "west"]


def dir_axis_rotation_axis(facing, along_first):
    a = {"north": "z", "south": "z", "east": "x", "west": "x", "up": "y", "down": "y"}[facing]
    if a == "x":
        return "y" if along_first else "z"
    if a == "y":
        return "x" if along_first else "z"
    return "x" if along_first else "y"


def bs_simple(name, model=None):
    return {"variants": {"": {"model": "%s:block/%s" % (NS, model or name)}}}


def bs_dir_axis(model_a, axis_a, model_b, axis_b):
    variants = {}
    for f in FACINGS:
        for af in (True, False):
            want = dir_axis_rotation_axis(f, af)
            for model, ax in ((model_a, axis_a), (model_b, axis_b)):
                r = solve_rotation("north", ax, f, want)
                if r:
                    v = {"model": "%s:block/%s" % (NS, model)}
                    if r[0]:
                        v["x"] = r[0]
                    if r[1]:
                        v["y"] = r[1]
                    variants["axis_along_first=%s,facing=%s" % (str(af).lower(), f)] = v
                    break
            else:
                raise ValueError("no rotation for %s %s" % (f, af))
    return {"variants": variants}


def bs_h_axis(model):
    return {"variants": {"axis=x": {"model": "%s:block/%s" % (NS, model)},
                         "axis=z": {"model": "%s:block/%s" % (NS, model), "y": 90}}}


def bs_horizontal(model, prop="facing"):
    ys = {"north": 0, "east": 90, "south": 180, "west": 270}
    out = {}
    for f in HFACINGS:
        v = {"model": "%s:block/%s" % (NS, model)}
        if ys[f]:
            v["y"] = ys[f]
        out["%s=%s" % (prop, f)] = v
    return {"variants": out}


def bs_facing(model):
    rot = {"north": (0, 0), "east": (0, 90), "south": (0, 180), "west": (0, 270), "up": (270, 0), "down": (90, 0)}
    out = {}
    for f in FACINGS:
        v = {"model": "%s:block/%s" % (NS, model)}
        x, y = rot[f]
        if x:
            v["x"] = x
        if y:
            v["y"] = y
        out["facing=%s" % f] = v
    return {"variants": out}


def y_to_x(el):
    e = copy.deepcopy(el)
    fx, fy, fz = el["from"]
    tx, ty, tz = el["to"]
    e["from"] = [fy, 16 - tx, fz]
    e["to"] = [ty, 16 - fx, tz]
    remap = {"up": "east", "down": "west", "east": "down", "west": "up", "north": "north", "south": "south"}
    faces = {}
    for d, f in el["faces"].items():
        f = dict(f)
        if d not in ("up", "down"):
            f["rotation"] = (f.get("rotation", 0) + 90) % 360
            if f["rotation"] == 0:
                f.pop("rotation")
        faces[remap[d]] = f
    e["faces"] = faces
    return e


def merged_item_model(housing, rotor, rotor_to_x=True):
    out = {"parent": "minecraft:block/block", "textures": dict(housing["textures"]), "elements": list(housing["elements"])}
    rename = {}
    for k, v in rotor["textures"].items():
        if k == "particle":
            continue
        nk = "r_" + k
        rename["#" + k] = "#" + nk
        out["textures"][nk] = v
    for el in rotor["elements"]:
        el = y_to_x(el) if rotor_to_x else copy.deepcopy(el)
        for f in el["faces"].values():
            f["texture"] = rename.get(f["texture"], f["texture"])
        out["elements"].append(el)
    if "render_type" in housing or "render_type" in rotor:
        out["render_type"] = housing.get("render_type", rotor.get("render_type"))
    return out


def item_generated(name, handheld=False):
    return {"parent": "minecraft:item/handheld" if handheld else "minecraft:item/generated", "textures": {"layer0": "%s:item/%s" % (NS, name)}}


def write(res, rel, data):
    path = os.path.join(res, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf8") as f:
        json.dump(data, f, indent=1)


def flip_z(el):
    e = copy.deepcopy(el)
    fz, tz = el["from"][2], el["to"][2]
    e["from"][2], e["to"][2] = 16 - tz, 16 - fz
    f = dict(e["faces"])
    if "north" in f or "south" in f:
        n, s = f.pop("north", None), f.pop("south", None)
        if n:
            f["south"] = n
        if s:
            f["north"] = s
    e["faces"] = f
    return e

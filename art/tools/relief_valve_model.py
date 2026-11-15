import json
import os
import sys

from PIL import Image

import iso

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SOURCE = os.path.join(ROOT, "art", "blockbench", "relief_valve.bbmodel")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "criticalmass")
NAME = "relief_valve"
MODEL = "criticalmass:block/" + NAME + "/"
BODY = ("hub", "valve")
STUB = ("stub_north",)
FLIP = {"north": "south", "south": "north", "east": "west", "west": "east", "up": "up", "down": "down"}
FACING = {"up": {}, "down": {"x": 180}, "north": {"x": 90}, "south": {"x": 90, "y": 180}, "east": {"x": 90, "y": 90}, "west": {"x": 90, "y": 270}}
SIDE = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}, "up": {"x": 270}, "down": {"x": 90}}


def tidy(value):
    r = round(value, 4)
    return int(r) if r == int(r) else r


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=1)
        f.write("\n")


def turned(element):
    faces = {}
    for d, face in element["faces"].items():
        g = dict(face)
        if d in ("up", "down"):
            g["rotation"] = (g.get("rotation", 0) + 180) % 360
            if not g["rotation"]:
                g.pop("rotation")
        faces[FLIP[d]] = g
    frm, to = element["from"], element["to"]
    return {"name": element["name"] + "_south", "from": [tidy(16 - to[0]), frm[1], tidy(16 - to[2])], "to": [tidy(16 - frm[0]), to[1], tidy(16 - frm[2])], "faces": faces}


def main():
    project = json.load(open(SOURCE, encoding="utf-8"))
    names = [t["name"].rsplit(".", 1)[0] for t in project["textures"]]
    scales = [16.0 / t.get("uv_width", project["resolution"]["width"]) for t in project["textures"]]
    cubes = {c["uuid"]: c for c in project["elements"]}
    groups = {g["uuid"]: g["name"] for g in project["groups"]}
    parts = {}
    for node in project["outliner"]:
        if not isinstance(node, dict):
            continue
        out = parts.setdefault(groups[node["uuid"]], [])
        for child in node["children"]:
            cube = cubes[child]
            assert not any(cube.get("rotation") or [0, 0, 0]), cube["name"]
            faces = {}
            for d, face in cube["faces"].items():
                if face.get("texture") is None:
                    continue
                g = {"uv": [tidy(v * scales[face["texture"]]) for v in face["uv"]], "texture": "#%d" % face["texture"]}
                if face.get("rotation"):
                    g["rotation"] = face["rotation"]
                faces[d] = g
            out.append({"name": cube["name"], "from": [tidy(v) for v in cube["from"]], "to": [tidy(v) for v in cube["to"]], "faces": faces})
    for needed in BODY + STUB:
        assert parts.get(needed), "the model needs a group called " + needed
    textures = {str(i): "criticalmass:block/" + n for i, n in enumerate(names)}
    textures["particle"] = "criticalmass:block/" + names[0]

    def model(elements):
        used = {f["texture"][1:] for e in elements for f in e["faces"].values()}
        return {"parent": "minecraft:block/block", "textures": {k: v for k, v in textures.items() if k in used or k == "particle"}, "elements": elements}

    body = [e for g in BODY for e in parts[g]]
    stub = [e for g in STUB for e in parts[g]]
    folder = os.path.join(ASSETS, "models", "block", NAME)
    write(os.path.join(folder, "body.json"), model(body))
    write(os.path.join(folder, "stub.json"), model(stub))
    write(os.path.join(folder, "item.json"), model(body + stub + [turned(e) for e in stub]))
    write(os.path.join(ASSETS, "models", "item", NAME + ".json"), {"parent": MODEL + "item"})
    old = os.path.join(ASSETS, "models", "block", NAME + ".json")
    if os.path.exists(old):
        os.remove(old)
    multipart = [{"when": {"facing": f}, "apply": dict({"model": MODEL + "body"}, **turn)} for f, turn in FACING.items()]
    multipart += [{"when": {d: "true"}, "apply": dict({"model": MODEL + "stub"}, **turn)} for d, turn in SIDE.items()]
    write(os.path.join(ASSETS, "blockstates", NAME + ".json"), {"multipart": multipart})
    print(len(body), "body cubes,", len(stub), "stub cubes")
    if len(sys.argv) > 1:
        iso._cache.clear()
        ref = MODEL + "item"
        views = [iso.render(ref, 360, 225, 25), iso.render(ref, 360, 135, 25), iso.render(ref, 360, 90, 0), iso.render(ref, 360, 180, 80)]
        sheet = Image.new("RGBA", (360 * len(views), 360), (88, 110, 74, 255))
        for i, v in enumerate(views):
            sheet.alpha_composite(v, (360 * i, 0))
        sheet.save(sys.argv[1])


if __name__ == "__main__":
    main()

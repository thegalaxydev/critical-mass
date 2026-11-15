import io
import json
import os
import zipfile

from PIL import Image

import iso
import pipe_bbmodel
from pipe_models import create_jar

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "criticalmass")
BLOCKBENCH = os.path.join(ROOT, "art", "blockbench")
FLIP = {"north": "south", "south": "north", "east": "west", "west": "east", "up": "up", "down": "down"}


def tidy(value):
    r = round(value, 4)
    return int(r) if r == int(r) else r


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=1)
        f.write("\n")


def load(name):
    project = json.load(open(os.path.join(BLOCKBENCH, name + ".bbmodel"), encoding="utf-8"))
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
    textures = {str(i): "criticalmass:block/" + n for i, n in enumerate(names)}
    return parts, textures


def model(elements, textures, particle, **extra):
    used = {f["texture"][1:] for e in elements for f in e["faces"].values()}
    out = {"parent": "minecraft:block/block", "textures": dict({k: v for k, v in textures.items() if k in used}, particle=particle), "elements": elements}
    out.update(extra)
    return out


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
    return {"name": element.get("name", "part") + "_turned", "from": [tidy(16 - to[0]), frm[1], tidy(16 - to[2])], "to": [tidy(16 - frm[0]), to[1], tidy(16 - frm[2])], "faces": faces}


def lifted(element, dy):
    out = dict(element)
    out["from"] = [element["from"][0], tidy(element["from"][1] + dy), element["from"][2]]
    out["to"] = [element["to"][0], tidy(element["to"][1] + dy), element["to"][2]]
    if "rotation" in element:
        r = dict(element["rotation"])
        r["origin"] = [r["origin"][0], tidy(r["origin"][1] + dy), r["origin"][2]]
        out["rotation"] = r
    return out


def borrowed(ref, prefix):
    with zipfile.ZipFile(create_jar()) as jar:
        loaded = pipe_bbmodel.load(ref, jar)
    textures = {}
    elements = []
    for el in loaded["elements"]:
        el = json.loads(json.dumps(el))
        for face in el["faces"].values():
            key = prefix + face["texture"][1:]
            textures[key] = pipe_bbmodel.resolve(face["texture"], loaded["textures"])
            face["texture"] = "#" + key
        elements.append(el)
    return elements, textures


def preview(ref, path, views=((225, 25), (135, 25), (45, 25), (180, 80)), size=360):
    iso._cache.clear()
    with zipfile.ZipFile(create_jar()) as jar:
        for value in iso.load_model(ref).get("textures", {}).values():
            if value.startswith("create:"):
                iso._cache[value] = Image.open(io.BytesIO(jar.read("assets/create/textures/%s.png" % value.split(":", 1)[1]))).convert("RGBA")
    shots = [iso.render(ref, size, yaw, pitch) for yaw, pitch in views]
    sheet = Image.new("RGBA", (size * len(shots), size), (88, 110, 74, 255))
    for i, shot in enumerate(shots):
        sheet.alpha_composite(shot, (size * i, 0))
    sheet.save(path)

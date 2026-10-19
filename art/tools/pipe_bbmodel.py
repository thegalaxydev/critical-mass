import base64
import json
import os
import uuid
import zipfile

from pipe_models import ASSETS, create_jar

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(ROOT, "art", "blockbench", "reinforced_pipe.bbmodel")
TEXTURES = ["reinforced_pipe", "reinforced_pipe_connected"]
PARTS = [("item", "reinforced_pipe/item", True), ("connection_north", "reinforced_pipe/connection/north", False),
         ("straight_core", "reinforced_pipe/lr_x", False), ("rim_north", "reinforced_pipe/rim/north", False), ("junction_casing", "reinforced_pipe/casing", False)]


def uid():
    return str(uuid.uuid4())


def load(name, jar):
    ns, path = name.split(":") if ":" in name else ("minecraft", name)
    if ns == "criticalmass":
        model = json.load(open(os.path.join(ASSETS, "models", path + ".json"), encoding="utf-8"))
    elif ns == "create":
        model = json.loads(jar.read("assets/create/models/%s.json" % path))
    else:
        return {"textures": {}, "elements": []}
    parent = model.get("parent")
    if parent and ":" not in parent and parent.startswith("block/"):
        parent = "minecraft:" + parent
    base = load(parent, jar) if parent else {"textures": {}, "elements": []}
    textures = dict(base.get("textures", {}))
    textures.update(model.get("textures", {}))
    return {"textures": textures, "elements": model.get("elements", base.get("elements", []))}


def resolve(ref, textures):
    while ref.startswith("#"):
        ref = textures.get(ref[1:], "")
    return ref


def element(name, raw, textures, visible):
    faces = {}
    for side, f in raw["faces"].items():
        tex = resolve(f["texture"], textures).split("/")[-1]
        face = {"uv": f["uv"], "texture": TEXTURES.index(tex) if tex in TEXTURES else None}
        if "rotation" in f:
            face["rotation"] = f["rotation"]
        faces[side] = face
    e = {"name": name, "box_uv": False, "render_order": "default", "locked": False, "export": True, "scope": 0,
         "allow_mirror_modeling": True, "from": raw["from"], "to": raw["to"], "autouv": 0, "color": 0,
         "origin": [8, 8, 8], "faces": faces, "type": "cube", "uuid": uid(), "visibility": visible}
    r = raw.get("rotation")
    if r:
        rot = [0, 0, 0]
        rot["xyz".index(r["axis"])] = r["angle"]
        e["rotation"] = rot
        e["origin"] = r["origin"]
    return e


def texture(i, name):
    path = os.path.join(ASSETS, "textures", "block", name + ".png")
    return {"path": path, "name": name + ".png", "relative_path": os.path.relpath(path, os.path.dirname(OUT)).replace(os.sep, "/"),
            "folder": "block", "namespace": "criticalmass", "id": str(i), "group": "", "scope": 0, "width": 32, "height": 32,
            "uv_width": 16, "uv_height": 16, "particle": i == 0, "use_as_default": False, "layers_enabled": False, "sync_to_project": "",
            "file_format": "png", "render_mode": "default", "render_sides": "auto", "wrap_mode": "limited", "pbr_channel": "color",
            "fps": 7, "frame_time": 1, "frame_order_type": "loop", "frame_order": "", "frame_interpolate": False, "visible": True,
            "internal": False, "saved": True, "uuid": uid(),
            "source": "data:image/png;base64," + base64.b64encode(open(path, "rb").read()).decode()}


def main():
    elements, groups, outliner = [], [], []
    with zipfile.ZipFile(create_jar()) as jar:
        for group_name, model, visible in PARTS:
            m = load("criticalmass:block/" + model, jar)
            children = []
            for i, el in enumerate(m["elements"]):
                e = element("%s_%d" % (group_name, i), el, m["textures"], visible)
                elements.append(e)
                children.append(e["uuid"])
            g = {"name": group_name, "uuid": uid(), "export": True, "locked": False, "scope": 0, "origin": [8, 8, 8],
                 "rotation": [0, 0, 0], "color": 0, "children": [], "reset": False, "shade": True, "mirror_uv": False,
                 "visibility": visible, "autouv": 0, "isOpen": True}
            groups.append(g)
            outliner.append({"uuid": g["uuid"], "isOpen": True, "children": children})
    model = {"meta": {"format_version": "5.0", "model_format": "java_block", "box_uv": False}, "name": "reinforced_pipe",
             "parent": "block/block", "ambientocclusion": True, "front_gui_light": False, "visible_box": [1, 1, 0],
             "variable_placeholders": "", "variable_placeholder_buttons": [], "unhandled_root_fields": {},
             "resolution": {"width": 16, "height": 16}, "elements": elements, "groups": groups, "outliner": outliner,
             "textures": [texture(i, n) for i, n in enumerate(TEXTURES)]}
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8", newline="\n") as f:
        json.dump(model, f, indent=1)
    print(OUT, len(elements), "cubes")


if __name__ == "__main__":
    main()

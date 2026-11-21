import base64
import json
import os
import sys
import uuid

import bbmodel
from area_monitor_texture import ISLANDS, NAME, SIZE

M = "criticalmass:block/"
SOURCE = os.path.join(bbmodel.BLOCKBENCH, NAME + ".bbmodel")
TEXTURE = os.path.join(bbmodel.ASSETS, "textures", "block", NAME + ".png")
FACING = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}}
GLOW = {"block_light": 15, "sky_light": 15, "ambient_occlusion": False}
GUI = {"gui": {"rotation": [30, 225, 0], "translation": [0, -0.5, 0], "scale": [0.9, 0.9, 0.9]},
       "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, -5], "scale": [1, 1, 1]},
       "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 3], "scale": [0.5, 0.5, 0.5]},
       "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 3], "scale": [0.375, 0.375, 0.375]},
       "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 2, 3], "scale": [0.4, 0.4, 0.4]}}
PER_PIXEL = 16.0 / SIZE


def uv(island):
    x, y, w, h = ISLANDS[island]
    return [x * PER_PIXEL, y * PER_PIXEL, (x + w) * PER_PIXEL, (y + h) * PER_PIXEL]


def cube(name, frm, to, **faces):
    return {"name": name, "from": frm, "to": to, "faces": faces}


def blockout():
    body = [
        cube("body", [3, 3, 12], [13, 14, 16], north="front", east="side", west="side", up="top", down="bottom"),
        cube("bezel", [4, 8, 11.5], [12, 13, 12], north="dial", east="rim_short", west="rim_short", up="rim_long", down="rim_long"),
        cube("tube", [4, 1, 13], [12, 3, 15], north="tube", south="tube", up="tube", down="tube", east="tube_end", west="tube_end"),
    ]
    lamp = [cube("lamp", [7, 14, 13], [9, 16, 15], north="lamp", south="lamp", east="lamp", west="lamp", up="lamp")]
    lit = [cube("lamp_lit", [7, 14, 13], [9, 16, 15], north="lamp_lit", south="lamp_lit", east="lamp_lit", west="lamp_lit", up="lamp_lit")]
    needle = [cube("needle", [7.75, 9.25, 11.3], [8.25, 12, 11.45], north="needle", south="needle", east="needle", west="needle", up="needle", down="needle")]
    return {"body": body, "lamp": lamp, "lamp_lit": lit, "needle": needle}


def uid():
    return str(uuid.uuid4())


def write_source():
    groups, elements, outliner = [], [], []
    for group, cubes in blockout().items():
        entry = {"name": group, "uuid": uid(), "export": True, "locked": False, "scope": 0, "origin": [8, 8, 8], "rotation": [0, 0, 0], "color": 0, "children": [], "reset": False,
                 "shade": True, "mirror_uv": False, "visibility": True, "autouv": 0, "isOpen": True}
        ids = []
        for raw in cubes:
            faces = {side: {"uv": [0, 0, 0, 0], "texture": None} for side in bbmodel.FLIP}
            for side, island in raw["faces"].items():
                faces[side] = {"uv": uv(island), "texture": 0}
            element = {"name": raw["name"], "box_uv": False, "render_order": "default", "locked": False, "export": True, "scope": 0, "allow_mirror_modeling": True,
                       "from": raw["from"], "to": raw["to"], "autouv": 0, "color": 0, "origin": [8, 8, 8], "faces": faces, "type": "cube", "uuid": uid(), "visibility": group != "lamp_lit"}
            elements.append(element)
            ids.append(element["uuid"])
        groups.append(entry)
        outliner.append({"uuid": entry["uuid"], "isOpen": True, "children": ids})
    texture = {"path": TEXTURE, "name": NAME + ".png", "relative_path": "../../src/main/resources/assets/criticalmass/textures/block/" + NAME + ".png", "folder": "block", "namespace": "criticalmass",
               "id": "0", "group": "", "scope": 0, "width": SIZE, "height": SIZE, "uv_width": 16, "uv_height": 16, "particle": True, "use_as_default": False, "layers_enabled": False,
               "sync_to_project": "", "file_format": "png", "render_mode": "default", "render_sides": "auto", "wrap_mode": "limited", "pbr_channel": "color", "fps": 7, "frame_time": 1,
               "frame_order_type": "loop", "frame_order": "", "frame_interpolate": False, "visible": True, "internal": False, "saved": True, "uuid": uid(),
               "source": "data:image/png;base64," + base64.b64encode(open(TEXTURE, "rb").read()).decode()}
    project = {"meta": {"format_version": "5.0", "model_format": "java_block", "box_uv": False}, "name": NAME, "parent": "block/block", "ambientocclusion": True, "front_gui_light": False,
               "visible_box": [1, 1, 0], "variable_placeholders": "", "variable_placeholder_buttons": [], "unhandled_root_fields": {}, "resolution": {"width": 16, "height": 16},
               "elements": elements, "groups": groups, "outliner": outliner, "textures": [texture]}
    with open(SOURCE, "w", encoding="utf-8", newline="\n") as f:
        json.dump(project, f, indent=1)
    print("wrote", SOURCE)


def lit(elements):
    return [dict(e, shade=False, neoforge_data=dict(GLOW)) for e in elements]


def export(preview):
    parts, textures = bbmodel.load(NAME)
    for group in ("body", "lamp", "lamp_lit", "needle"):
        assert parts.get(group), "the model needs a group called " + group
    particle = textures["0"]
    block = os.path.join(bbmodel.ASSETS, "models", "block")
    bbmodel.write(os.path.join(block, NAME + ".json"), bbmodel.model(parts["body"], textures, particle))
    bbmodel.write(os.path.join(block, NAME + "_lamp.json"), bbmodel.model(parts["lamp"], textures, particle))
    bbmodel.write(os.path.join(block, NAME + "_lamp_lit.json"), bbmodel.model(lit(parts["lamp_lit"]), textures, particle))
    needle = bbmodel.model(parts["needle"], textures, particle)
    needle.pop("parent")
    bbmodel.write(os.path.join(block, NAME + "_needle.json"), needle)
    bbmodel.write(os.path.join(block, NAME + "_item.json"), bbmodel.model(parts["body"] + parts["lamp"] + parts["needle"], textures, particle, display=GUI))
    bbmodel.write(os.path.join(bbmodel.ASSETS, "models", "item", NAME + ".json"), {"parent": M + NAME + "_item"})
    multipart = []
    for facing, turn in FACING.items():
        multipart.append({"when": {"facing": facing}, "apply": dict({"model": M + NAME}, **turn)})
        multipart.append({"when": {"facing": facing, "alarm": "false"}, "apply": dict({"model": M + NAME + "_lamp"}, **turn)})
        multipart.append({"when": {"facing": facing, "alarm": "true"}, "apply": dict({"model": M + NAME + "_lamp_lit"}, **turn)})
    bbmodel.write(os.path.join(bbmodel.ASSETS, "blockstates", NAME + ".json"), {"multipart": multipart})
    print(len(parts["body"]), "body cubes")
    if preview:
        bbmodel.preview(M + NAME + "_item", preview, views=((0, 0), (35, 20), (325, 20), (0, 70)), size=320)


def main():
    args = [a for a in sys.argv[1:] if a not in ("blockout", "fresh")]
    if "blockout" in sys.argv:
        if os.path.exists(SOURCE) and "fresh" not in sys.argv:
            print("left alone; pass fresh to write the blockout over", SOURCE)
        else:
            write_source()
    export(args[0] if args else None)


if __name__ == "__main__":
    main()

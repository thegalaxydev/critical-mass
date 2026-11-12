import json
import os
import sys
import zipfile

from PIL import Image

import pipe_bbmodel as pb
from pipe_models import create_jar

OUT = os.path.join(pb.ROOT, "art", "blockbench")
PIECES = {
    "reinforced_pipe_junction": [("junction", "reinforced_pipe/casing")],
    "reinforced_pipe_flange": [("flange", "reinforced_pipe/rim/north"), ("stub", "reinforced_pipe/rim_connector/north")],
}


def build(name, parts, preview):
    elements, cubes, groups, outliner, textures = [], [], [], [], {}
    with zipfile.ZipFile(create_jar()) as jar:
        for group_name, path in parts:
            model = pb.load("criticalmass:block/" + path, jar)
            textures = model["textures"]
            children = []
            for i, el in enumerate(model["elements"]):
                el = dict(el)
                if el.get("rotation") and not el["rotation"].get("angle"):
                    el.pop("rotation")
                elements.append(el)
                cube = pb.element("%s_%d" % (group_name, i), el, model["textures"], True)
                cubes.append(cube)
                children.append(cube["uuid"])
            group = {"name": group_name, "uuid": pb.uid(), "export": True, "locked": False, "scope": 0, "origin": [8, 8, 8], "rotation": [0, 0, 0], "color": 0, "children": [],
                     "reset": False, "shade": True, "mirror_uv": False, "visibility": True, "autouv": 0, "isOpen": True}
            groups.append(group)
            outliner.append({"uuid": group["uuid"], "isOpen": True, "children": children})
    java = {"parent": "minecraft:block/block", "textures": textures, "elements": elements}
    with open(os.path.join(OUT, name + ".json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump(java, f, indent=1)
        f.write("\n")
    project = {"meta": {"format_version": "5.0", "model_format": "java_block", "box_uv": False}, "name": name, "parent": "block/block", "ambientocclusion": True,
               "front_gui_light": False, "visible_box": [1, 1, 0], "variable_placeholders": "", "variable_placeholder_buttons": [], "unhandled_root_fields": {},
               "resolution": {"width": 16, "height": 16}, "elements": cubes, "groups": groups, "outliner": outliner,
               "textures": [pb.texture(i, n) for i, n in enumerate(pb.TEXTURES)]}
    with open(os.path.join(OUT, name + ".bbmodel"), "w", encoding="utf-8", newline="\n") as f:
        json.dump(project, f, indent=1)
    print(name, len(cubes), "cubes")
    if preview:
        import iso
        iso.load_model = lambda ref: java
        ref = "criticalmass:block/" + name
        views = [iso.render(ref, 320, 215, 25), iso.render(ref, 320, 180, 0), iso.render(ref, 320, 35, 25)]
        sheet = Image.new("RGBA", (320 * len(views), 320), (88, 110, 74, 255))
        for i, v in enumerate(views):
            sheet.alpha_composite(v, (320 * i, 0))
        sheet.save(os.path.join(OUT, name + "_preview.png"))


if __name__ == "__main__":
    for piece, parts in PIECES.items():
        build(piece, parts, len(sys.argv) > 1)

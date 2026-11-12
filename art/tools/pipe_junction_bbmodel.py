import json
import os
import sys
import zipfile

from PIL import Image

import pipe_bbmodel as pb
from pipe_models import create_jar

NAME = "reinforced_pipe_junction"
SOURCE = "criticalmass:block/reinforced_pipe/casing"
OUT = os.path.join(pb.ROOT, "art", "blockbench")


def main():
    with zipfile.ZipFile(create_jar()) as jar:
        model = pb.load(SOURCE, jar)
    java = {"parent": "minecraft:block/block", "textures": model["textures"], "elements": model["elements"]}
    with open(os.path.join(OUT, NAME + ".json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump(java, f, indent=1)
        f.write("\n")
    cubes = [pb.element("junction_%d" % i, el, model["textures"], True) for i, el in enumerate(model["elements"])]
    group = {"name": "junction", "uuid": pb.uid(), "export": True, "locked": False, "scope": 0, "origin": [8, 8, 8], "rotation": [0, 0, 0], "color": 0, "children": [], "reset": False,
             "shade": True, "mirror_uv": False, "visibility": True, "autouv": 0, "isOpen": True}
    project = {"meta": {"format_version": "5.0", "model_format": "java_block", "box_uv": False}, "name": NAME, "parent": "block/block", "ambientocclusion": True,
               "front_gui_light": False, "visible_box": [1, 1, 0], "variable_placeholders": "", "variable_placeholder_buttons": [], "unhandled_root_fields": {},
               "resolution": {"width": 16, "height": 16}, "elements": cubes, "groups": [group],
               "outliner": [{"uuid": group["uuid"], "isOpen": True, "children": [c["uuid"] for c in cubes]}],
               "textures": [pb.texture(i, n) for i, n in enumerate(pb.TEXTURES)]}
    with open(os.path.join(OUT, NAME + ".bbmodel"), "w", encoding="utf-8", newline="\n") as f:
        json.dump(project, f, indent=1)
    print(len(cubes), "cubes")
    if len(sys.argv) > 1:
        import iso
        iso.load_model = lambda ref: java
        views = [iso.render(SOURCE, 320, 225, 30), iso.render(SOURCE, 320, 180, 0)]
        sheet = Image.new("RGBA", (320 * len(views), 320), (88, 110, 74, 255))
        for i, v in enumerate(views):
            sheet.alpha_composite(v, (320 * i, 0))
        sheet.save(sys.argv[1])


if __name__ == "__main__":
    main()

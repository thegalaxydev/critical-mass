import base64
import json
import os
import sys
import uuid

from PIL import Image

from handmade import BRASS, STEEL
from handmade_rest import ANDESITE

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(ROOT, "art", "blockbench")
NAME = "pump_flywheel"
TEXTURE = "criticalmass:block/" + NAME
CENTRE = [8, 8, 8]
INSET = 0.02
SIDES = ("north", "east", "south", "west")
UP_TURN = {"north": 0, "east": 90, "south": 180, "west": 270}
DOWN_TURN = {"south": 0, "east": 90, "north": 180, "west": 270}
OPPOSITE = {"north": "south", "south": "north", "east": "west", "west": "east"}
LIP_TOP, LIP_OUTER, LIP_INNER, LIP_END = [0, 0, 6, 1], [0, 2, 6, 4], [0, 3, 6, 4], [6, 0, 8, 1]
BAND_TOP, BAND_SIDE, BAND_END = [0, 1, 6, 2], [0, 4, 6, 6], [6, 1, 8, 2]
SPOKE_TOP, SPOKE_SIDE, SPOKE_END = [0, 6, 1, 8.5], [1, 6, 3.5, 7.5], [1, 6, 2, 7.5]
HUB_TOP, HUB_SIDE = [8, 0, 13, 5], [8, 5, 13, 7]


def paint():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))

    def row(x0, x1, y, colours):
        for x in range(x0, x1):
            img.putpixel((x, y), colours[(x - x0) % len(colours)] + (255,))
    row(0, 6, 0, [BRASS[4], BRASS[5], BRASS[4], BRASS[4], BRASS[5], BRASS[4]])
    row(0, 6, 1, [BRASS[2], BRASS[2], BRASS[1], BRASS[2], BRASS[2], BRASS[1]])
    row(0, 6, 2, [BRASS[4]])
    row(0, 6, 3, [BRASS[3]])
    row(0, 6, 4, [BRASS[2]])
    row(0, 6, 5, [BRASS[1]])
    row(6, 8, 0, [BRASS[3]])
    row(6, 8, 1, [BRASS[2]])
    for y, c in ((6, ANDESITE[4]), (7, ANDESITE[5]), (8, ANDESITE[4])):
        img.putpixel((0, y), c + (255,))
    row(1, 4, 6, [ANDESITE[3]])
    row(1, 4, 7, [ANDESITE[2]])
    for x in range(8, 13):
        for y in range(0, 5):
            edge = x in (8, 12) or y in (0, 4)
            corner = x in (8, 12) and y in (0, 4)
            c = BRASS[3] if corner else BRASS[4] if edge else ANDESITE[2]
            img.putpixel((x, y), c + (255,))
    img.putpixel((10, 2), STEEL[1] + (255,))
    for x in (9, 11):
        img.putpixel((x, 2), ANDESITE[3] + (255,))
    for y in (1, 3):
        img.putpixel((10, y), ANDESITE[3] + (255,))
    row(8, 13, 5, [BRASS[3]])
    row(8, 13, 6, [BRASS[2]])
    return img


def face(uv, turn=0):
    f = {"uv": uv, "texture": "#0"}
    if turn:
        f["rotation"] = turn
    return f


def radial(side, near, far, half, y0, y1):
    if side == "north":
        return [8 - half, y0, 8 - far], [8 + half, y1, 8 - near]
    if side == "south":
        return [8 - half, y0, 8 + near], [8 + half, y1, 8 + far]
    if side == "east":
        return [8 + near, y0, 8 - half], [8 + far, y1, 8 + half]
    return [8 - far, y0, 8 - half], [8 - near, y1, 8 + half]


def part(name, side, near, far, half, y0, y1, top, outer, inner, end, turned):
    lo, hi = radial(side, near, far, half, y0 + (INSET if turned else 0), y1 - (INSET if turned else 0))
    faces = {"up": face(top, UP_TURN[side]), "down": face(top, DOWN_TURN[side]), side: face(outer), OPPOSITE[side]: face(inner)}
    for other in SIDES:
        if other not in faces:
            faces[other] = face(end)
    e = {"name": name, "from": lo, "to": hi, "faces": faces}
    if turned:
        e["rotation"] = {"angle": 45, "axis": "y", "origin": CENTRE}
    return e


def elements():
    out = []
    for turned in (False, True):
        tag = "_diagonal" if turned else ""
        for side in SIDES:
            out.append(part("rim_lip_%s%s" % (side, tag), side, 6, 7, 2.9, 7, 9, LIP_TOP, LIP_OUTER, LIP_INNER, LIP_END, turned))
            out.append(part("rim_band_%s%s" % (side, tag), side, 5, 6, 2.5, 7.25, 8.75, BAND_TOP, BAND_SIDE, BAND_SIDE, BAND_END, turned))
        for side in SIDES:
            out.append(part("spoke_%s%s" % (side, tag), side, 2.5, 5, 0.5, 7.25, 8.75, SPOKE_TOP, SPOKE_END, SPOKE_END, SPOKE_SIDE, turned))
        y0, y1 = (7 + INSET, 9 - INSET) if turned else (7, 9)
        hub = {"name": "hub" + tag, "from": [5.5, y0, 5.5], "to": [10.5, y1, 10.5],
               "faces": {"up": face(HUB_TOP), "down": face(HUB_TOP), "north": face(HUB_SIDE), "south": face(HUB_SIDE), "east": face(HUB_SIDE), "west": face(HUB_SIDE)}}
        if turned:
            hub["rotation"] = {"angle": 45, "axis": "y", "origin": CENTRE}
        out.append(hub)
    return out


def uid():
    return str(uuid.uuid4())


def bb_element(raw):
    faces = {}
    for side, f in raw["faces"].items():
        entry = {"uv": f["uv"], "texture": 0}
        if "rotation" in f:
            entry["rotation"] = f["rotation"]
        faces[side] = entry
    e = {"name": raw["name"], "box_uv": False, "render_order": "default", "locked": False, "export": True, "scope": 0, "allow_mirror_modeling": True,
         "from": raw["from"], "to": raw["to"], "autouv": 0, "color": 0, "origin": CENTRE, "faces": faces, "type": "cube", "uuid": uid(), "visibility": True}
    if raw.get("rotation"):
        e["rotation"] = [0, raw["rotation"]["angle"], 0]
    return e


def bb_texture(path):
    return {"path": path, "name": NAME + ".png", "relative_path": NAME + ".png", "folder": "block", "namespace": "criticalmass", "id": "0", "group": "", "scope": 0,
            "width": 16, "height": 16, "uv_width": 16, "uv_height": 16, "particle": True, "use_as_default": False, "layers_enabled": False, "sync_to_project": "",
            "file_format": "png", "render_mode": "default", "render_sides": "auto", "wrap_mode": "limited", "pbr_channel": "color", "fps": 7, "frame_time": 1,
            "frame_order_type": "loop", "frame_order": "", "frame_interpolate": False, "visible": True, "internal": True, "saved": True, "uuid": uid(),
            "source": "data:image/png;base64," + base64.b64encode(open(path, "rb").read()).decode()}


def main():
    os.makedirs(OUT, exist_ok=True)
    png = os.path.join(OUT, NAME + ".png")
    paint().save(png)
    parts = elements()
    java = {"parent": "minecraft:block/block", "textures": {"0": TEXTURE, "particle": TEXTURE}, "elements": parts}
    with open(os.path.join(OUT, NAME + ".json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump(java, f, indent=1)
        f.write("\n")
    cubes = [bb_element(e) for e in parts]
    group = {"name": "flywheel", "uuid": uid(), "export": True, "locked": False, "scope": 0, "origin": CENTRE, "rotation": [0, 0, 0], "color": 0, "children": [], "reset": False,
             "shade": True, "mirror_uv": False, "visibility": True, "autouv": 0, "isOpen": True}
    project = {"meta": {"format_version": "5.0", "model_format": "java_block", "box_uv": False}, "name": NAME, "parent": "block/block", "ambientocclusion": True,
               "front_gui_light": False, "visible_box": [1, 1, 0], "variable_placeholders": "", "variable_placeholder_buttons": [], "unhandled_root_fields": {},
               "resolution": {"width": 16, "height": 16}, "elements": cubes, "groups": [group],
               "outliner": [{"uuid": group["uuid"], "isOpen": True, "children": [c["uuid"] for c in cubes]}], "textures": [bb_texture(png)]}
    with open(os.path.join(OUT, NAME + ".bbmodel"), "w", encoding="utf-8", newline="\n") as f:
        json.dump(project, f, indent=1)
    print(len(parts), "cubes")
    if len(sys.argv) > 1:
        os.environ["ISO_TEX_OVERRIDE"] = OUT
        import iso
        iso.load_model = lambda ref: java
        views = [iso.render(TEXTURE, 320, 225, 55, scale=320 * 4 / 20.0), iso.render(TEXTURE, 320, 225, 12, scale=320 * 4 / 20.0), iso.render(TEXTURE, 320, 180, 89, scale=320 * 4 / 20.0)]
        sheet = Image.new("RGBA", (320 * len(views), 320), (88, 110, 74, 255))
        for i, v in enumerate(views):
            sheet.alpha_composite(v, (320 * i, 0))
        sheet.save(sys.argv[1])
        print("preview", sys.argv[1])


if __name__ == "__main__":
    main()

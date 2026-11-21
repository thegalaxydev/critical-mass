import base64
import json
import math
import os
import sys
import uuid

import bbmodel
import create_style
from controller_texture import palette
from create_style import BRASS, CASING, CASING_LIGHT, COPPER
from plant_style import BLOCK, Sheet

NAME = "diesel_engine"
M = "criticalmass:block/"
SIZE = 64
SOURCE = os.path.join(bbmodel.BLOCKBENCH, NAME + ".bbmodel")
TEXTURE = os.path.join(BLOCK, NAME + ".png")
FACES = ("north", "south", "east", "west", "up", "down")
DARK, STEEL, GOLD, RUST, RED = "dark", "steel", "brass", "copper", "red"
RAMPS = {DARK: CASING, STEEL: CASING_LIGHT, GOLD: BRASS, RUST: COPPER}
MID = {DARK: 3, STEEL: 3, GOLD: 6, RUST: 5}
INK = (12, 14, 17)
TURNS = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}, "up": {"x": 270}, "down": {"x": 90}}
CUBES = (
    ("rail_west", (2, 0, 1.5), (4.5, 1.5, 15), DARK, ()),
    ("rail_east", (11.5, 0, 1.5), (14, 1.5, 15), DARK, ()),
    ("sump", (5, 1, 4.5), (11, 3, 12.5), DARK, ("up",)),
    ("block", (3.5, 3, 3), (12.5, 10.5, 13.5), DARK, ()),
    ("housing", (3, 4.5, 1.5), (13, 11.5, 3), STEEL, ()),
    ("housing_tall", (4.5, 2.5, 1.75), (11.5, 13.5, 2.75), STEEL, ()),
    ("boss", (5.5, 5.5, 0.75), (10.5, 10.5, 1.5), GOLD, ("south",)),
    ("head", (4.5, 10.5, 3.5), (11.5, 12.5, 13), STEEL, ("down",)),
    ("rocker", (5.5, 12.5, 4), (10.5, 13.5, 12.5), DARK, ("down",)),
    ("manifold", (11.5, 10.5, 4.5), (13, 12, 11.5), RUST, ("west",)),
    ("stack", (11.5, 12, 9.5), (13.5, 16, 11.5), DARK, ("down",)),
    ("stack_band", (11.25, 13.5, 9.25), (13.75, 14.25, 11.75), STEEL, ()),
    ("filter", (2, 9, 5.5), (4.5, 12, 10.5), STEEL, ("east",)),
    ("filter_cap", (2.5, 12, 7), (4, 12.5, 9), GOLD, ("down",)),
    ("starter", (12.5, 4, 5), (14.25, 6.5, 9.5), RUST, ("west",)),
    ("filler", (6, 13.5, 5), (8, 14.25, 7), RED, ("down",)),
    ("shroud", (4.5, 3.5, 13.5), (11.5, 11.5, 14.25), DARK, ("north", "south")),
    ("radiator", (3, 1.5, 14.25), (13, 13.5, 16), STEEL, ()),
    ("radiator_cap", (7, 13.5, 14.5), (9, 14.25, 15.75), GOLD, ("down",)),
)


def extent(frm, to, face):
    dx, dy, dz = (to[i] - frm[i] for i in range(3))
    return (dx, dy) if face in ("north", "south") else (dz, dy) if face in ("east", "west") else (dx, dz)


def unwrap():
    wanted = []
    for name, frm, to, material, hidden in CUBES:
        for face in FACES:
            if face not in hidden:
                w, h = extent(frm, to, face)
                wanted.append((name, face, w, h))
    wanted.sort(key=lambda f: (-math.ceil(f[3]), -math.ceil(f[2])))
    islands, x, y, row = {}, 0, 0, 0
    for name, face, w, h in wanted:
        pw, ph = math.ceil(w), math.ceil(h)
        if x + pw > SIZE:
            x, y, row = 0, y + row, 0
        assert y + ph <= SIZE, "the sheet is full at %s %s" % (name, face)
        islands[name + "." + face] = (x, y, pw, ph)
        x, row = x + pw, max(row, ph)
    return islands


def tone(material, step):
    ramp = RAMPS[material]
    return ramp[max(0, min(len(ramp) - 1, MID[material] + step))]


def plain(sheet, key, material, face):
    _, _, w, h = sheet.islands[key]
    red = palette("red")
    lift = 1 if face == "up" else -1 if face in ("down", "south") else 0
    for y in range(h):
        for x in range(w):
            light = x == 0 or y == 0
            dark = x == w - 1 or y == h - 1
            edge = (1 if light and not dark else -2 if dark and not light else 0) if min(w, h) > 1 else 0
            if material == RED:
                sheet.put(key, x, y, red[max(0, min(4, 2 + edge + lift))])
            else:
                sheet.put(key, x, y, tone(material, edge + lift))


def detail(sheet, name, face, material):
    key = name + "." + face
    _, _, w, h = sheet.islands[key]
    if name == "radiator" and face == "south":
        sheet.fill(key, 1, 2, w - 2, h - 2, CASING[0])
        for y in range(2, h - 1, 2):
            for x in range(1, w - 1):
                sheet.put(key, x, y, CASING[2])
        for x in (3, w - 4):
            for y in range(2, h - 1):
                sheet.put(key, x, y, tone(STEEL, -1))
    if name == "radiator" and face == "up":
        for x in range(1, w - 1):
            sheet.put(key, x, 0, tone(STEEL, 2))
    if name == "housing" and face == "north":
        sheet.fill(key, 2, 1, w - 3, h - 2, tone(STEEL, -1))
        for x, y in ((1, 1), (w - 2, 1), (1, h - 2), (w - 2, h - 2)):
            sheet.put(key, x, y, tone(GOLD, 0))
    if name == "housing_tall" and face == "north":
        sheet.fill(key, 1, 1, w - 2, h - 2, tone(STEEL, -1))
        for x, y in ((w // 2, 1), (w // 2, h - 2)):
            sheet.put(key, x, y, tone(GOLD, 0))
    if name == "boss" and face == "north":
        sheet.fill(key, 1, 1, w - 2, h - 2, INK)
    if name == "block" and face in ("east", "west"):
        sheet.outline(key, 2, 2, w - 3, h - 3, tone(DARK, 1))
        sheet.fill(key, 3, 3, w - 4, h - 4, tone(DARK, -1))
        for x in (3, w - 4):
            sheet.put(key, x, 3, tone(STEEL, 1))
            sheet.put(key, x, h - 4, tone(STEEL, -1))
    if name == "head" and face in ("east", "west"):
        for x in range(2, w - 1, 3):
            sheet.put(key, x, 1, tone(DARK, 1))
    if name == "rocker" and face == "up":
        for y in range(1, h - 1):
            sheet.put(key, w // 2, y, tone(DARK, 2))
        for y in (1, h - 2):
            sheet.put(key, 1, y, tone(STEEL, 2))
            sheet.put(key, w - 2, y, tone(STEEL, 0))
    if name == "manifold" and face in ("east", "up"):
        for x in range(1, w - 1, 2):
            for y in range(h):
                if face == "east" or y in (0, h - 1):
                    sheet.put(key, x, y, tone(RUST, -2))
    if name == "stack" and face == "up":
        sheet.fill(key, 0, 0, w - 1, h - 1, INK)
        sheet.outline(key, 0, 0, w - 1, h - 1, tone(DARK, 0)) if min(w, h) > 2 else None
    if name == "stack" and face != "up":
        for x in range(w):
            sheet.put(key, x, 0, tone(DARK, -2))
    if name == "filter" and face in ("north", "south", "west"):
        for y in range(1, h - 1, 2):
            for x in range(w):
                sheet.put(key, x, y, tone(DARK, 0))
    if name == "starter" and face in ("east", "up"):
        for x in range(1, w - 1, 2):
            for y in range(h):
                sheet.put(key, x, y, tone(RUST, -3))
    if name == "shroud" and face in ("east", "west", "up", "down"):
        for y in range(h):
            for x in range(w):
                sheet.put(key, x, y, CASING[1])


def paint(islands):
    sheet = Sheet(SIZE, islands)
    for name, frm, to, material, hidden in CUBES:
        for face in FACES:
            if face not in hidden:
                plain(sheet, name + "." + face, material, face)
                detail(sheet, name, face, material)
    return sheet


def elements(islands):
    out = []
    per_pixel = 16.0 / SIZE
    for name, frm, to, material, hidden in CUBES:
        faces = {}
        for face in FACES:
            if face in hidden:
                continue
            x, y, _, _ = islands[name + "." + face]
            w, h = extent(frm, to, face)
            faces[face] = {"uv": [bbmodel.tidy(x * per_pixel), bbmodel.tidy(y * per_pixel), bbmodel.tidy((x + w) * per_pixel), bbmodel.tidy((y + h) * per_pixel)], "texture": "#0"}
        out.append({"name": name, "from": list(frm), "to": list(to), "faces": faces})
    return out


def uid():
    return str(uuid.uuid4())


def write_source(parts):
    cubes = []
    for raw in parts:
        faces = {side: {"uv": [0, 0, 0, 0], "texture": None} for side in FACES}
        for side, face in raw["faces"].items():
            faces[side] = {"uv": face["uv"], "texture": 0}
        cubes.append({"name": raw["name"], "box_uv": False, "render_order": "default", "locked": False, "export": True, "scope": 0, "allow_mirror_modeling": True,
                      "from": raw["from"], "to": raw["to"], "autouv": 0, "color": 0, "origin": [8, 8, 8], "faces": faces, "type": "cube", "uuid": uid(), "visibility": True})
    group = {"name": "body", "uuid": uid(), "export": True, "locked": False, "scope": 0, "origin": [8, 8, 8], "rotation": [0, 0, 0], "color": 0, "children": [], "reset": False,
             "shade": True, "mirror_uv": False, "visibility": True, "autouv": 0, "isOpen": True}
    texture = {"path": TEXTURE, "name": NAME + ".png", "relative_path": "../../src/main/resources/assets/criticalmass/textures/block/" + NAME + ".png", "folder": "block", "namespace": "criticalmass",
               "id": "0", "group": "", "scope": 0, "width": SIZE, "height": SIZE, "uv_width": 16, "uv_height": 16, "particle": True, "use_as_default": False, "layers_enabled": False,
               "sync_to_project": "", "file_format": "png", "render_mode": "default", "render_sides": "auto", "wrap_mode": "limited", "pbr_channel": "color", "fps": 7, "frame_time": 1,
               "frame_order_type": "loop", "frame_order": "", "frame_interpolate": False, "visible": True, "internal": False, "saved": True, "uuid": uid(),
               "source": "data:image/png;base64," + base64.b64encode(open(TEXTURE, "rb").read()).decode()}
    project = {"meta": {"format_version": "5.0", "model_format": "java_block", "box_uv": False}, "name": NAME, "parent": "block/block", "ambientocclusion": True, "front_gui_light": False,
               "visible_box": [1, 1, 0], "variable_placeholders": "", "variable_placeholder_buttons": [], "unhandled_root_fields": {}, "resolution": {"width": 16, "height": 16},
               "elements": cubes, "groups": [group], "outliner": [{"uuid": group["uuid"], "isOpen": True, "children": [c["uuid"] for c in cubes]}], "textures": [texture]}
    with open(SOURCE, "w", encoding="utf-8", newline="\n") as f:
        json.dump(project, f, indent=1)
    print("wrote", SOURCE)


def export(preview):
    parts, textures = bbmodel.load(NAME)
    assert parts.get("body"), "the model needs a group called body"
    block = os.path.join(bbmodel.ASSETS, "models", "block")
    bbmodel.write(os.path.join(block, NAME + ".json"), bbmodel.model(parts["body"], textures, textures["0"]))
    bbmodel.write(os.path.join(bbmodel.ASSETS, "models", "item", NAME + ".json"), {"parent": M + NAME})
    bbmodel.write(os.path.join(bbmodel.ASSETS, "blockstates", NAME + ".json"), {"variants": {"facing=" + side: dict({"model": M + NAME}, **turn) for side, turn in TURNS.items()}})
    print(len(parts["body"]), "cubes")
    if preview:
        bbmodel.preview(M + NAME, preview, views=((35, 25), (325, 25), (145, 25), (215, 25)), size=360)


def main():
    args = [a for a in sys.argv[1:] if a != "fresh"]
    if "fresh" in sys.argv:
        islands = unwrap()
        paint(islands).save(TEXTURE)
        print("painted", TEXTURE)
        write_source(elements(islands))
    else:
        print("left the texture and the Blockbench source alone; pass fresh to rebuild them")
    export(args[0] if args else None)


if __name__ == "__main__":
    main()

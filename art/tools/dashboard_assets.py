import base64
import io
import json
import os
import sys
import uuid

from PIL import Image

from handmade import BRASS, STEEL

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
MODELS = os.path.join(ROOT, "art", "blockbench")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "criticalmass")
M = "criticalmass:"
TILT = -22.5
PIVOT = [8, 14, 16]
FACINGS = {"north": 0, "east": 90, "south": 180, "west": 270}
PIECES = {"button": ("base", "cap"), "switch": ("base", "paddle"), "lever": ("base", "handle"), "gauge": ("body", "needle"), "alarm": ("base", "lamp", "lamp_lit")}
SWATCH = {"steel": (0, 0), "dark": (4, 0), "brass": (8, 0), "red": (12, 0), "cream": (16, 0), "face": (20, 0), "needle": (24, 0), "lamp": (0, 4), "lit": (4, 4), "green": (8, 4)}


def uid():
    return str(uuid.uuid4())


def planar(frm, to, d):
    x0, y0, z0 = frm
    x1, y1, z1 = to
    return {"north": [16 - x1, 16 - y1, 16 - x0, 16 - y0], "south": [x0, 16 - y1, x1, 16 - y0], "west": [z0, 16 - y1, z1, 16 - y0],
            "east": [16 - z1, 16 - y1, 16 - z0, 16 - y0], "up": [x0, z0, x1, z1], "down": [x0, 16 - z1, x1, 16 - z0]}[d]


def el(name, frm, to, faces, tilt=None):
    out = {}
    for d, (x, y, w, h) in faces.items():
        flip = d.endswith("!")
        out[d.rstrip("!")] = [x + w, y, x, y + h] if flip else [x, y, x + w, y + h]
    return {"name": name, "from": frm, "to": to, "faces": out, "rotation": [TILT, 0, 0] if tilt else None, "origin": tilt or [8, 8, 8]}


def swatch(name, frm, to, colours):
    out = {}
    for d in ("north", "south", "east", "west", "up", "down"):
        sx, sy = SWATCH[colours.get(d, colours["all"])]
        out[d] = [sx + 0.5, sy + 0.5, sx + 3.5, sy + 3.5]
    return {"name": name, "from": frm, "to": to, "faces": out, "rotation": None, "origin": [4, 1, 4]}


BOARD_TOP, BOARD_EDGE, FRONT, BASE_FRONT, BASE_SIDE, BOARD_UNDER, BASE_BOTTOM = (0, 0, 16, 10), (0, 10, 16, 3), (0, 13, 16, 6), (0, 19, 16, 2), (0, 21, 12, 2), (0, 23, 16, 10), (0, 33, 16, 12)
BASE_TOP, CHEEK, RIM, CHEEK_FRONT, CHEEK_BACK, BACK_OUT, BACK_IN, BACK_TOP = (16, 0, 16, 12), (16, 16, 12, 14), (29, 16, 1, 13), (30, 16, 1, 9), (31, 16, 1, 14), (32, 0, 16, 14), (48, 0, 16, 14), (32, 14, 16, 1)
RIM_SIDE = (16, 31, 13, 2)
SLOPE_FROM = 11.0
SLOPE = 5.0 / 12.0


def cheek(x0, outer):
    inner = "east" if outer == "west" else "west"
    flipped = {"west": "west", "east": "east!"}
    return [el("panel", [x0, 2, 4], [x0 + 1, 16, 16], {flipped[outer]: CHEEK, flipped[inner]: CHEEK, "south": CHEEK_BACK}),
            el("front", [x0, 2, 4], [x0 + 1, 11, 5], {"north": CHEEK_FRONT}),
            el("rim", [x0, 15, 3.01], [x0 + 1, 16, 16], {"up": RIM, "north": (29, 16, 1, 1)}, [8, 16, 16]),
            el("trim_west", [x0 - 0.02, 14, 3.01], [x0, 16, 16], {"west": RIM_SIDE}, [8, 16, 16]),
            el("trim_east", [x0 + 1, 14, 3.01], [x0 + 1.02, 16, 16], {"east!": RIM_SIDE}, [8, 16, 16])]


def desk():
    return {
        "body": [el("base", [0, 0, 4], [16, 2, 16], {"up": BASE_TOP, "down": BASE_BOTTOM, "north": BASE_FRONT, "south": BASE_FRONT, "east": BASE_SIDE, "west": BASE_SIDE}),
                 el("front", [0, 2, 8], [16, 8, 16], {"north": FRONT}),
                 el("back", [0, 2, 15], [16, 16, 16], {"south": BACK_OUT, "north": BACK_IN, "up": BACK_TOP}),
                 el("board", [0, 11, 6], [16, 14, 16], {"up": BOARD_TOP, "north": BOARD_EDGE, "down": BOARD_UNDER}, PIVOT)],
        "cheek_west": cheek(0, "west"),
        "cheek_east": cheek(15, "east"),
    }


def parts():
    return {
        "button": {"base": [swatch("plate", [1, 0, 1], [7, 1, 7], {"all": "steel"}), swatch("collar", [2.5, 1, 2.5], [5.5, 1.5, 5.5], {"all": "brass"})],
                   "cap": [swatch("cap", [3, 1, 3], [5, 2.5, 5], {"all": "red"})]},
        "switch": {"base": [swatch("plate", [2, 0, 1], [6, 1, 7], {"all": "steel"}), swatch("boot", [3, 1, 3], [5, 1.5, 5], {"all": "dark"})],
                   "paddle": [swatch("paddle", [3.25, 1, 3.5], [4.75, 4, 4.5], {"all": "cream"})]},
        "lever": {"base": [swatch("plate", [2, 0, 1], [6, 1, 7], {"all": "steel"}), swatch("slot", [3.5, 1, 1.5], [4.5, 1.25, 6.5], {"all": "dark"})],
                  "handle": [swatch("stick", [3.5, 1, 3.5], [4.5, 4.5, 4.5], {"all": "brass"}), swatch("knob", [3, 4.5, 3], [5, 6, 5], {"all": "red"})]},
        "gauge": {"body": [swatch("case", [1, 0, 1], [7, 1.5, 7], {"all": "brass", "up": "face"})],
                  "needle": [swatch("needle", [3.75, 1.55, 1.75], [4.25, 1.8, 4.25], {"all": "needle"})]},
        "alarm": {"base": [swatch("plate", [1, 0, 1], [7, 1, 7], {"all": "steel"})],
                  "lamp": [swatch("lens", [2, 1, 2], [6, 2.5, 6], {"all": "lamp"})],
                  "lamp_lit": [swatch("lens", [2, 1, 2], [6, 2.5, 6], {"all": "lit"})]},
    }


def nine(tile, w, h, b):
    tw, th = tile.size
    out = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    inner = tile.crop((b, b, tw - b, th - b))
    for y in range(b, h - b, inner.height):
        for x in range(b, w - b, inner.width):
            out.paste(inner.crop((0, 0, min(inner.width, w - b - x), min(inner.height, h - b - y))), (x, y))
    top, bottom = tile.crop((b, 0, tw - b, b)), tile.crop((b, th - b, tw - b, th))
    for x in range(b, w - b, top.width):
        cut = min(top.width, w - b - x)
        out.paste(top.crop((0, 0, cut, b)), (x, 0))
        out.paste(bottom.crop((0, 0, cut, b)), (x, h - b))
    left, right = tile.crop((0, b, b, th - b)), tile.crop((tw - b, b, tw, th - b))
    for y in range(b, h - b, left.height):
        cut = min(left.height, h - b - y)
        out.paste(left.crop((0, 0, b, cut)), (0, y))
        out.paste(right.crop((0, 0, b, cut)), (w - b, y))
    for (sx, sy), (dx, dy) in (((0, 0), (0, 0)), ((tw - b, 0), (w - b, 0)), ((0, th - b), (0, h - b)), ((tw - b, th - b), (w - b, h - b))):
        out.paste(tile.crop((sx, sy, sx + b, sy + b)), (dx, dy))
    return out


def desk_texture():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    casing = Image.open(os.path.join(ASSETS, "textures", "block", "reactor_vessel_casing.png")).convert("RGBA").crop((0, 0, 16, 16))
    iron = rail = wood = casing
    slats = Image.new("RGBA", (16, 16))
    plate = casing.crop((3, 3, 13, 13))
    for y in range(0, 16, 10):
        for x in range(0, 16, 10):
            slats.paste(plate, (x, y))
    for x in range(16):
        for y in (0, 15):
            slats.putpixel((x, y), casing.getpixel((x, 2)))
    for y in range(16):
        for x in (0, 15):
            slats.putpixel((x, y), casing.getpixel((2, y)))

    def put(region, tile):
        img.paste(tile, (region[0], region[1]))
    put(BOARD_TOP, nine(rail, 16, 10, 2))
    put(BOARD_EDGE, nine(rail, 16, 3, 1))
    put(FRONT, nine(slats, 16, 6, 1))
    put(BASE_FRONT, nine(iron, 16, 2, 1))
    put(BASE_SIDE, nine(iron, 12, 2, 1))
    put(BOARD_UNDER, nine(iron, 16, 10, 1))
    put(BASE_BOTTOM, nine(iron, 16, 12, 1))
    put(BASE_TOP, nine(wood, 16, 12, 1))
    put(BACK_OUT, nine(iron, 16, 14, 2))
    put(BACK_IN, nine(rail, 16, 14, 2))
    put(BACK_TOP, nine(rail, 16, 1, 0).crop((0, 0, 16, 1)))
    side = nine(iron, 12, 14, 1)
    edge = [rail.getpixel((x, 0)) for x in range(16)]
    for c in range(12):
        top = 16.0 - (SLOPE_FROM + (c + 0.5) * SLOPE) + 1.0
        for r in range(14):
            if r + 0.5 < top:
                side.putpixel((c, r), (0, 0, 0, 0))
    for r in range(14):
        if side.getpixel((0, r))[3]:
            side.putpixel((0, r), edge[4])
        side.putpixel((11, r), edge[6])
    for c in range(12):
        side.putpixel((c, 13), edge[2 + c])
    put(CHEEK, side)
    put(RIM, rail.crop((0, 2, 1, 15)))
    put(RIM_SIDE, rail.crop((1, 0, 14, 2)))
    put(CHEEK_FRONT, rail.crop((0, 3, 1, 12)))
    put(CHEEK_BACK, rail.crop((0, 1, 1, 15)))
    return img


def part_texture():
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    colours = {"steel": STEEL[5], "dark": STEEL[0], "brass": BRASS[3], "red": (200, 48, 40), "cream": (232, 228, 214), "face": (236, 232, 220), "needle": (190, 30, 30),
               "lamp": (96, 24, 20), "lit": (255, 88, 60), "green": (72, 190, 84)}
    for name, (sx, sy) in SWATCH.items():
        r, g, b = colours[name]
        for x in range(4):
            for y in range(4):
                k = 1.12 if (x, y) == (0, 0) else 0.86 if x == 3 or y == 3 else 1.0
                img.putpixel((sx + x, sy + y), (min(255, int(r * k)), min(255, int(g * k)), min(255, int(b * k)), 255))
    return img


def bb_cube(raw):
    faces = {d: {"uv": uv, "texture": 0} for d, uv in raw["faces"].items()}
    for d in ("north", "south", "east", "west", "up", "down"):
        faces.setdefault(d, {"uv": [0, 0, 0, 0], "texture": None})
    cube = {"name": raw["name"], "box_uv": False, "render_order": "default", "locked": False, "export": True, "scope": 0, "allow_mirror_modeling": True,
            "from": raw["from"], "to": raw["to"], "autouv": 0, "color": 0, "origin": raw["origin"], "faces": faces, "type": "cube", "uuid": uid(), "visibility": True}
    if raw["rotation"]:
        cube["rotation"] = raw["rotation"]
    return cube


def write_bbmodel(path, name, groups, png):
    size = png.width
    cubes, group_list, outliner = [], [], []
    for group_name, raws in groups.items():
        made = [bb_cube(r) for r in raws]
        cubes += made
        g = {"name": group_name, "uuid": uid(), "export": True, "locked": False, "scope": 0, "origin": [8, 8, 8], "rotation": [0, 0, 0], "color": 0, "children": [],
             "reset": False, "shade": True, "mirror_uv": False, "visibility": True, "autouv": 0, "isOpen": True}
        group_list.append(g)
        outliner.append({"uuid": g["uuid"], "name": group_name, "isOpen": True, "children": [c["uuid"] for c in made]})
    buffer = io.BytesIO()
    png.save(buffer, "PNG")
    texture = {"name": name + ".png", "relative_path": "", "folder": "block", "namespace": "criticalmass", "id": "0", "group": "", "scope": 0, "width": size, "height": size,
               "uv_width": size, "uv_height": size, "particle": True, "use_as_default": False, "layers_enabled": False, "sync_to_project": "", "file_format": "png",
               "render_mode": "default", "render_sides": "auto", "wrap_mode": "limited", "pbr_channel": "color", "fps": 7, "frame_time": 1, "frame_order_type": "loop",
               "frame_order": "", "frame_interpolate": False, "visible": True, "internal": True, "saved": True, "uuid": uid(),
               "source": "data:image/png;base64," + base64.b64encode(buffer.getvalue()).decode()}
    project = {"meta": {"format_version": "5.0", "model_format": "java_block", "box_uv": False}, "name": name, "parent": "", "ambientocclusion": True, "front_gui_light": False,
               "visible_box": [1, 1, 0], "variable_placeholders": "", "variable_placeholder_buttons": [], "unhandled_root_fields": {}, "resolution": {"width": size, "height": size},
               "elements": cubes, "groups": group_list, "outliner": outliner, "textures": [texture]}
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(project, f, separators=(",", ":"))


def tidy(value):
    r = round(value, 4)
    return int(r) if r == int(r) else r


def read_groups(path):
    project = json.load(open(path, encoding="utf-8"))
    scale = 16.0 / project["textures"][0].get("uv_width", project["resolution"]["width"])
    cubes = {c["uuid"]: c for c in project["elements"]}
    names = {g["uuid"]: g["name"] for g in project.get("groups", [])}

    def gather(node):
        if isinstance(node, str):
            return [cubes[node]]
        return [c for child in node.get("children", []) for c in gather(child)]
    groups = {}
    for node in project["outliner"]:
        name = "" if isinstance(node, str) else node.get("name") or names.get(node.get("uuid"), "")
        elements = []
        for cube in gather(node):
            faces = {}
            for d, face in cube["faces"].items():
                if face.get("texture") is None:
                    continue
                g = {"uv": [tidy(v * scale) for v in face["uv"]], "texture": "#0"}
                if face.get("rotation"):
                    g["rotation"] = face["rotation"]
                faces[d] = g
            if not faces:
                continue
            element = {"name": cube["name"], "from": [tidy(v) for v in cube["from"]], "to": [tidy(v) for v in cube["to"]], "faces": faces}
            turns = [(axis, angle) for axis, angle in zip("xyz", cube.get("rotation") or [0, 0, 0]) if abs(angle) > 1e-6]
            assert len(turns) <= 1, (path, cube["name"], cube.get("rotation"))
            if turns:
                element["rotation"] = {"angle": turns[0][1], "axis": turns[0][0], "origin": [tidy(v) for v in cube.get("origin") or [8, 8, 8]]}
            elements.append(element)
        groups.setdefault(name, []).extend(elements)
    return groups, base64.b64decode(project["textures"][0]["source"].split(",", 1)[1])


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=1)
        f.write("\n")


def shifted(elements, dx, dy, dz):
    out = []
    for e in elements:
        e = json.loads(json.dumps(e))
        for key in ("from", "to"):
            e[key] = [tidy(e[key][0] + dx), tidy(e[key][1] + dy), tidy(e[key][2] + dz)]
        if e.get("rotation"):
            o = e["rotation"]["origin"]
            e["rotation"]["origin"] = [tidy(o[0] + dx), tidy(o[1] + dy), tidy(o[2] + dz)]
        out.append(e)
    return out


def main():
    fresh = "fresh" in sys.argv[1:]
    desk_file = os.path.join(MODELS, "dashboard.bbmodel")
    if fresh or not os.path.exists(desk_file):
        write_bbmodel(desk_file, "dashboard", desk(), desk_texture())
    groups, png = read_groups(desk_file)
    for needed in ("body", "cheek_west", "cheek_east"):
        assert needed in groups, needed
    with open(os.path.join(ASSETS, "textures", "block", "dashboard.png"), "wb") as f:
        f.write(png)
    textures = {"0": M + "block/dashboard", "particle": M + "block/dashboard"}
    shapes = {"single": ("cheek_west", "cheek_east"), "start": ("cheek_west",), "end": ("cheek_east",), "middle": ()}
    whole = {}
    for shape, cheeks in shapes.items():
        elements = groups["body"] + [e for c in cheeks for e in groups[c]]
        whole[shape] = {"parent": "minecraft:block/block", "render_type": "minecraft:cutout", "textures": textures, "elements": elements}
        write(os.path.join(ASSETS, "models", "block", "dashboard", shape + ".json"), whole[shape])
    variants = {}
    for facing, turn in FACINGS.items():
        for cw in ("false", "true"):
            for ccw in ("false", "true"):
                shape = "middle" if cw == ccw == "true" else "start" if cw == "true" else "end" if ccw == "true" else "single"
                v = {"model": M + "block/dashboard/" + shape}
                if turn:
                    v["y"] = turn
                variants["facing=%s,join_ccw=%s,join_cw=%s" % (facing, ccw, cw)] = v
    write(os.path.join(ASSETS, "blockstates", "dashboard.json"), {"variants": variants})
    write(os.path.join(ASSETS, "models", "item", "dashboard.json"), {"parent": M + "block/dashboard/single"})
    held = {"gui": {"rotation": [30, 225, 0], "translation": [0, 3, 0], "scale": [1.1, 1.1, 1.1]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.5, 0.5, 0.5]},
            "fixed": {"rotation": [-90, 0, 0], "translation": [0, 0, -4], "scale": [1, 1, 1]},
            "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.5, 0.5, 0.5]},
            "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 4, 0], "scale": [0.6, 0.6, 0.6]}}
    for part, pieces in PIECES.items():
        part_file = os.path.join(MODELS, "dashboard_" + part + ".bbmodel")
        if fresh or not os.path.exists(part_file):
            write_bbmodel(part_file, "dashboard_" + part, parts()[part], part_texture())
        part_groups, part_png = read_groups(part_file)
        with open(os.path.join(ASSETS, "textures", "block", "dashboard_" + part + ".png"), "wb") as f:
            f.write(part_png)
        tex = {"0": M + "block/dashboard_" + part, "particle": M + "block/dashboard_" + part}
        shown = []
        for piece in pieces:
            assert piece in part_groups, (part, piece, sorted(part_groups))
            write(os.path.join(ASSETS, "models", "block", "dashboard", part + "_" + piece + ".json"), {"render_type": "minecraft:cutout", "textures": tex, "elements": part_groups[piece]})
            if piece != "lamp_lit":
                shown += part_groups[piece]
        write(os.path.join(ASSETS, "models", "item", "dashboard_" + part + ".json"),
              {"parent": "minecraft:block/block", "render_type": "minecraft:cutout", "textures": tex, "elements": shifted(shown, 4, 5, 4), "display": held})
        print(part, {p: len(part_groups[p]) for p in pieces})
    print("dashboard", {k: len(v) for k, v in groups.items()})
    previews = [a for a in sys.argv[1:] if a.endswith(".png")]
    if previews:
        import iso
        iso._cache.clear()
        board = whole["single"]
        slots = []
        for i, part in enumerate(("button", "gauge")):
            part_groups, _ = read_groups(os.path.join(MODELS, "dashboard_" + part + ".bbmodel"))
            for piece in PIECES[part][:2]:
                for e in shifted(part_groups[piece], 8 * i, 14, 7):
                    e.pop("rotation", None)
                    slots.append((part, e))
        sheet_models = {"single": whole["single"], "start": whole["start"], "middle": whole["middle"], "end": whole["end"]}
        iso.load_model = lambda ref: sheet_models[ref]
        views = [iso.render("single", 480, 35, 25), iso.render("single", 480, 325, 20), iso.render("single", 480, 270, 0), iso.render("single", 480, 145, 25)]

        def lift(dx):
            return lambda c: (c[0] + dx, c[1], c[2])
        views.append(iso.render([("start", lift(-16)), ("middle", lift(0)), ("end", lift(16))], 480, 25, 25, scale=480 * 4 / 56.0))
        sheet = Image.new("RGBA", (480 * len(views), 480), (88, 110, 74, 255))
        for i, v in enumerate(views):
            sheet.alpha_composite(v, (480 * i, 0))
        sheet.save(previews[0])


if __name__ == "__main__":
    main()

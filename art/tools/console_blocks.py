import math
import os

from PIL import Image

from handmade import BRASS, IRON, STEEL, bevel, put, rect
from handmade_rest import casing, plate

M = "criticalmass:"
QUADS = {"a": [0, 0, 8, 8], "b": [8, 0, 16, 8], "c": [0, 8, 8, 16], "d": [8, 8, 16, 16]}
ROTATIONS = {"north": 0, "east": 90, "south": 180, "west": 270}
SLOPE = 22.5
RISE = math.tan(math.radians(SLOPE))
RUN = 16 / math.cos(math.radians(SLOPE))
LIP = 1.2
PARTS = ("front_bottom", "back_bottom", "front_top", "back_top")
GLASS = (10, 20, 14, 255)


def atlas(quads):
    img = Image.new("RGBA", (32, 32))
    for key, painter in quads.items():
        u0, v0, _, _ = QUADS[key]
        img.paste(painter(16, 16), (u0 * 2, v0 * 2))
    return img


def console_texture():
    def body(w, h):
        img = casing(IRON, STEEL)(w, h)
        rect(img, 2, 12, 13, 13, IRON[1])
        for x in range(3, 13, 2):
            put(img, x, 12, IRON[0])
        return img

    def desk(w, h):
        img = plate(IRON, 1)(w, h)
        rect(img, 0, 0, 15, 15, IRON[1])
        rect(img, 0, 0, 15, 0, IRON[3])
        rect(img, 0, 15, 15, 15, IRON[0])
        rect(img, 0, 0, 0, 15, IRON[2])
        rect(img, 15, 0, 15, 15, IRON[0])
        return img

    def trim(w, h):
        img = plate(STEEL, 2)(w, h)
        rect(img, 0, 7, 15, 8, BRASS[2])
        return img

    def part(w, h):
        img = Image.new("RGBA", (w, h), (214, 214, 208, 255))
        rect(img, 0, 0, 15, 0, (245, 245, 240))
        rect(img, 0, 15, 15, 15, (150, 150, 146))
        rect(img, 0, 0, 0, 15, (232, 232, 226))
        rect(img, 15, 0, 15, 15, (170, 170, 165))
        return img
    return atlas({"a": body, "b": desk, "c": trim, "d": part})


def screen_texture():
    def front(w, h):
        img = Image.new("RGBA", (w, h), GLASS)
        for y in range(1, 16, 2):
            rect(img, 0, y, 15, y, (12, 26, 17))
        return img

    def bezel(w, h):
        img = Image.new("RGBA", (w, h), IRON[1] + (255,) if len(IRON[1]) == 3 else IRON[1])
        rect(img, 0, 0, 15, 0, IRON[3])
        rect(img, 0, 15, 15, 15, IRON[0])
        return img

    def back(w, h):
        img = casing(IRON, STEEL)(w, h)
        put(img, 12, 12, BRASS[3])
        return img
    return atlas({"a": front, "b": back, "c": bezel, "d": back})


def face(quad, cull=None, uv=None):
    f = {"uv": uv or QUADS[quad], "texture": "#0"}
    if cull:
        f["cullface"] = cull
    return f


def box(lo, hi, quad, top=None, rotation=None, cull=()):
    faces = {}
    for d in ("north", "south", "east", "west", "up", "down"):
        faces[d] = face(top if top and d == "up" else quad, d if d in cull else None)
    e = {"from": lo, "to": hi, "faces": faces}
    if rotation:
        e["rotation"] = rotation
    return e


def slab(start):
    top = LIP + start * RISE
    e = box([0, top - 1, 0], [16, top, RUN], "c", rotation={"origin": [8, top, 0], "axis": "x", "angle": -SLOPE})
    e["faces"]["up"] = face("b", uv=[8, 0, 16, 4] if start == 0 else [8, 4, 16, 8])
    return e


def steps(start, end=16):
    out = []
    for k in range(4):
        z0, z1 = k * 4, min(end, k * 4 + 4)
        top = LIP + (start + z0) * RISE - 1.15
        if top > 0.05 and z1 > z0:
            out.append(box([0, 0, z0], [16, top, z1], "a", cull=("down",)))
    return out


def console_parts():
    cube = [box([0, 0, 0], [16, 16, 16], "a", cull=("north", "south", "east", "west", "up", "down"))]
    front_top = steps(0) + [slab(0)]
    back_top = steps(16, 15) + [box([0, 0, 15], [16, 16, 16], "a", top="c", cull=("down", "south")), slab(16)]
    return {"front_bottom": cube, "back_bottom": cube, "front_top": front_top, "back_top": back_top}


def shifted(elements, dy, dz):
    out = []
    for e in elements:
        n = dict(e)
        n["from"] = [e["from"][0], e["from"][1] + dy, e["from"][2] + dz]
        n["to"] = [e["to"][0], e["to"][1] + dy, e["to"][2] + dz]
        n["faces"] = {k: {a: b for a, b in v.items() if a != "cullface"} for k, v in e["faces"].items()}
        if "rotation" in e:
            r = dict(e["rotation"])
            r["origin"] = [r["origin"][0], r["origin"][1] + dy, r["origin"][2] + dz]
            n["rotation"] = r
        out.append(n)
    return out


def control_partials():
    cap = {d: face("d") for d in ("north", "south", "east", "west", "up", "down")}
    dark = {d: face("a", uv=[1, 1, 3, 3]) for d in ("north", "south", "east", "west", "up", "down")}

    def el(lo, hi, faces):
        return {"from": lo, "to": hi, "faces": faces}
    return {
        "console/button": [el([-1.5, 0, -1.5], [1.5, 1.6, 1.5], cap)],
        "console/button_collar": [el([-2, 0, -2], [2, 0.5, 2], dark)],
        "console/big_button": [el([-2.6, 0.9, -2.6], [2.6, 2.6, 2.6], cap), el([-1, 0, -1], [1, 0.9, 1], cap)],
        "console/big_button_collar": [el([-3.3, 0, -3.3], [3.3, 0.6, 3.3], dark)],
        "console/switch_base": [el([-1.6, 0, -2.4], [1.6, 0.5, 2.4], dark)],
        "console/switch_lever": [el([-0.5, 0, -0.5], [0.5, 3.2, 0.5], cap), el([-0.8, 2.6, -0.8], [0.8, 3.6, 0.8], cap)],
        "console/dial": [el([-1.7, 0, -1.7], [1.7, 1.5, 1.7], cap),
                         dict(el([-1.7, 0, -1.7], [1.7, 1.5, 1.7], cap), rotation={"origin": [0, 0, 0], "axis": "y", "angle": 45})],
        "console/dial_pointer": [el([-0.35, 1.5, -2.2], [0.35, 1.9, 0.4], cap)],
        "console/dial_collar": [el([-2.7, 0, -2.7], [2.7, 0.35, 2.7], cap)],
        "console/lamp": [el([-1.6, 0, -1.1], [1.6, 0.7, 1.1], cap)],
    }


def write_all(write, assets, data, out):
    name = "control_console"
    console_texture().save(os.path.join(out, name + ".png"))
    screen_texture().save(os.path.join(out, "plant_screen.png"))
    tex = {"0": M + "block/" + name, "particle": M + "block/" + name}
    parts = console_parts()
    variants = {}
    for part, elements in parts.items():
        write(os.path.join(assets, "models", "block", "console", part + ".json"), {"parent": "minecraft:block/block", "textures": tex, "elements": elements})
        for facing, y in ROTATIONS.items():
            v = {"model": M + "block/console/" + part}
            if y:
                v["y"] = y
            variants["facing=%s,part=%s" % (facing, part)] = v
    write(os.path.join(assets, "blockstates", name + ".json"), {"variants": variants})
    for path, elements in control_partials().items():
        write(os.path.join(assets, "models", "block", path + ".json"), {"textures": tex, "elements": elements})
    module = shifted(parts["front_bottom"], -8, -8) + shifted(parts["back_bottom"], -8, 8) + shifted(parts["front_top"], 8, -8) + shifted(parts["back_top"], 8, 8)
    small = {"rotation": [30, 45, 0], "translation": [0, -1, 0], "scale": [0.38, 0.38, 0.38]}
    hand = {"rotation": [75, 225, 0], "translation": [0, 2.5, 0], "scale": [0.22, 0.22, 0.22]}
    display = {"gui": small, "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.2, 0.2, 0.2]},
               "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.35, 0.35, 0.35]},
               "thirdperson_righthand": hand, "thirdperson_lefthand": hand,
               "firstperson_righthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.25, 0.25, 0.25]},
               "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.25, 0.25, 0.25]}}
    write(os.path.join(assets, "models", "item", name + ".json"), {"parent": "minecraft:block/block", "textures": tex, "elements": module, "display": display})
    write(os.path.join(data, "criticalmass", "loot_table", "blocks", name + ".json"), {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{
        "type": "minecraft:item", "name": M + name, "conditions": [{"condition": "minecraft:block_state_property", "block": M + name, "properties": {"column": "2", "part": "front_bottom"}}]}]}]})

    stex = {"0": M + "block/plant_screen", "particle": M + "block/plant_screen"}
    screen = box([0, 0, 13], [16, 16, 16], "b", cull=("south",))
    screen["faces"]["north"] = face("a")
    edges = {"top": ("up", [0, 15, 12.4], [16, 16, 13]), "bottom": ("down", [0, 0, 12.4], [16, 1, 13]),
             "left": ("left", [15, 0, 12.4], [16, 16, 13]), "right": ("right", [0, 0, 12.4], [1, 16, 13])}
    models = {"plant_screen": [screen]}
    for edge, (_, lo, hi) in edges.items():
        models["plant_screen_" + edge] = [box(lo, hi, "c")]
    for model, elements in models.items():
        write(os.path.join(assets, "models", "block", model + ".json"), {"parent": "minecraft:block/block", "textures": stex, "elements": elements})
    multipart = []
    for d, y in ROTATIONS.items():
        def apply(model):
            return {"model": M + "block/" + model, "y": y} if y else {"model": M + "block/" + model}
        multipart.append({"when": {"facing": d}, "apply": apply("plant_screen")})
        for edge, (prop, _, _) in edges.items():
            multipart.append({"when": {"facing": d, prop: "false"}, "apply": apply("plant_screen_" + edge)})
    write(os.path.join(assets, "blockstates", "plant_screen.json"), {"multipart": multipart})
    write(os.path.join(assets, "models", "item", "plant_screen.json"),
          {"parent": "minecraft:block/block", "textures": stex, "elements": [screen] + [box(lo, hi, "c") for _, lo, hi in edges.values()]})
    write(os.path.join(data, "criticalmass", "loot_table", "blocks", "plant_screen.json"), {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{
        "type": "minecraft:item", "name": M + "plant_screen"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})


LANG = {
    "block.criticalmass.control_console": "Control Console",
    "block.criticalmass.plant_screen": "Plant Screen",
    "criticalmass.console.no_turn": "No room to turn the Control Console here",
    "criticalmass.console.no_space": "A Control Console needs a clear space 5 wide, 2 deep and 2 high",
    "criticalmass.goggles.console": "Control Console",
    "criticalmass.console.reactor": "Reactor: %s",
    "criticalmass.console.turbine": "Turbine: %s",
    "criticalmass.console.generator": "Generator: %s",
    "criticalmass.console.unbound": "not bound",
    "criticalmass.console.bound_at": "bound at %s %s %s",
    "criticalmass.console.hint": "Bind machines with a Plant Linker. Switches 1 to 4 power the lower back blocks from left to right, dials 1 to 4 the upper ones.",
    "criticalmass.console.outputs": "The fifth back column gives the alarm signal below and the trip signal above.",
    "criticalmass.console.needs.reactor": "Bind a Reactor Controller first",
    "criticalmass.console.needs.turbine": "Bind a Steam Turbine first",
    "criticalmass.console.needs.generator": "Bind a Generator first",
    "criticalmass.console.label.scram": "SCRAM",
    "criticalmass.console.label.reactor_reset": "RESET",
    "criticalmass.console.label.rod_in": "IN",
    "criticalmass.console.label.rod_out": "OUT",
    "criticalmass.console.label.turbine_trip": "TRIP",
    "criticalmass.console.label.turbine_reset": "RESET",
    "criticalmass.console.label.breaker": "BREAKER",
    "criticalmass.console.label.ack": "ACK",
    "criticalmass.console.section.0": "REACTOR",
    "criticalmass.console.section.1": "TURBINE",
    "criticalmass.console.section.2": "ALARMS",
    "criticalmass.console.section.3": "SWITCHES",
    "criticalmass.console.section.4": "DIALS",
    "criticalmass.console.result.switch_on": "Switch %s on",
    "criticalmass.console.result.switch_off": "Switch %s off",
    "criticalmass.console.result.dial_on": "Dial %s on",
    "criticalmass.console.result.dial_off": "Dial %s off",
    "criticalmass.goggles.screen": "Plant Screen",
    "criticalmass.screen.powered": "Powered: %s FE stored",
    "criticalmass.screen.unpowered": "No power: feed Forge Energy into any screen of the wall",
    "criticalmass.screen.size": "Screen wall %s by %s: %s lines of %s characters",
}

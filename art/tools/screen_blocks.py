import os

from PIL import Image

from handmade import BRASS, IRON, STEEL, put, rect
from handmade_rest import casing

M = "criticalmass:"
QUADS = {"a": [0, 0, 8, 8], "b": [8, 0, 16, 8], "c": [0, 8, 8, 16], "d": [8, 8, 16, 16]}
ROTATIONS = {"north": 0, "east": 90, "south": 180, "west": 270}
GLASS = (10, 20, 14, 255)


def atlas(quads):
    img = Image.new("RGBA", (32, 32))
    for key, painter in quads.items():
        u0, v0, _, _ = QUADS[key]
        img.paste(painter(16, 16), (u0 * 2, v0 * 2))
    return img


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


def write_all(write, assets, data, out):
    screen_texture().save(os.path.join(out, "plant_screen.png"))
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
    "block.criticalmass.plant_screen": "Plant Screen",
    "criticalmass.goggles.screen": "Plant Screen",
    "criticalmass.screen.powered": "Powered by the shaft behind the wall",
    "criticalmass.screen.unpowered": "No power: turn a shaft into the back of any screen of the wall",
    "criticalmass.screen.size": "Screen wall %s by %s: %s lines of %s characters",
    "criticalmass.screen.unlinked": "Shows Display Link text. Link a Reactor Controller with a Plant Linker for the reactor display, which needs a wall %s wide and %s high",
    "criticalmass.screen.linked": "Reactor display for the controller at %s %s %s",
    "criticalmass.screen.too_small": "Linked, but the reactor display needs a wall %s wide and %s high",
    "criticalmass.screen.link.too_small": "Linked, but this wall is %s by %s. The reactor display needs %s by %s",
    "criticalmass.screen.link.refused": "A screen wall links to a Reactor Controller or a Steam Turbine",
    "criticalmass.screen.link.cleared": "Screen wall unlinked",
    "criticalmass.screen.board.too_small": "REACTOR DISPLAY NEEDS %s WIDE BY %s HIGH",
    "criticalmass.screen.board.size": "THIS WALL IS %s BY %s",
    "criticalmass.screen.no_reactor": "NO REACTOR FOUND",
    "criticalmass.screen.trend.power": "NEUTRON POWER %%",
    "criticalmass.screen.trend.pressure": "PRESSURE bar",
    "criticalmass.screen.trend.coolant": "COOLANT °C",
    "criticalmass.screen.trend.xenon": "XENON %%",
    "criticalmass.screen.trend.past": "-2 MIN",
    "criticalmass.screen.trend.now": "NOW",
    "criticalmass.screen.row.neutron": "NEUTRON POWER",
    "criticalmass.screen.row.power": "HEAT OUTPUT",
    "criticalmass.screen.row.period": "PERIOD",
    "criticalmass.screen.row.reactivity": "REACTIVITY",
    "criticalmass.screen.row.temperature": "FUEL",
    "criticalmass.screen.row.coolant_temperature": "COOLANT",
    "criticalmass.screen.row.pressure": "PRESSURE",
    "criticalmass.screen.row.flow": "FLOW",
    "criticalmass.screen.row.rods_a": "RODS IN",
    "criticalmass.screen.row.xenon": "XENON",
    "criticalmass.screen.row.boron": "BORON",
    "criticalmass.screen.row.turbine": "TURBINE",
    "criticalmass.screen.row.output": "SHAFT LOAD",
    "criticalmass.screen.events": "EVENTS",
    "criticalmass.screen.events.none": "NOTHING TO REPORT",
    "criticalmass.screen.event.cleared": "CLEARED: %s",
}

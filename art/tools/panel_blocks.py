import os

from PIL import Image

from handmade import BRASS, IRON, STEEL, put, rect
from handmade_rest import casing, plate

M = "criticalmass:"
QUADS = {"a": [0, 0, 8, 8], "b": [8, 0, 16, 8], "c": [0, 8, 8, 16], "d": [8, 8, 16, 16]}
ROTATIONS = {"north": 0, "east": 90, "south": 180, "west": 270}
FACE_Z = 13
RIM = 0.6
GLASS = (10, 20, 14, 255)
SWATCHES = {"red": (0xD9, 0x38, 0x2B), "green": (0x3F, 0xA3, 0x4D), "cream": (0xE8, 0xE4, 0xD6), "amber": (0xE0, 0xA0, 0x30),
            "blue": (0x40, 0x80, 0xD0), "dark": (0x30, 0x34, 0x3A), "lamp": (0x58, 0x18, 0x12), "black": (0x15, 0x17, 0x1A), "light": (0x24, 0x5A, 0x20)}
PANELS = {
    "scram_panel": ("SCRAM Panel", [("big", 5.0, 7.8, "red"), ("button", 12.0, 7.8, "green")]),
    "rod_panel": ("Rod Control Panel", [("button", 4.5, 7.8, "cream"), ("button", 11.5, 7.8, "amber")]),
    "turbine_panel": ("Turbine Panel", [("button", 4.5, 7.8, "red"), ("button", 11.5, 7.8, "green")]),
    "breaker_panel": ("Breaker Panel", [("switch", 8, 6.0, "cream"), ("lamp", 8, 11.2, "light")]),
    "alarm_panel": ("Alarm Panel", [("lamp", u, v, "lamp") for v in (11.2, 7.8) for u in (3.5, 8, 12.5)] + [("button", 4.5, 3.4, "blue")]),
    "lever_panel": ("Lever Panel", [("switch", 8, 6.0, "cream"), ("lamp", 8, 11.2, "light")]),
    "dial_panel": ("Dial Panel", [("dial", 8, 7.8, "dark")]),
}


def atlas(quads):
    img = Image.new("RGBA", (32, 32))
    for key, painter in quads.items():
        u0, v0, _, _ = QUADS[key]
        img.paste(painter(16, 16), (u0 * 2, v0 * 2))
    return img


def panel_texture():
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


def swatch_texture():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 255))
    for i, color in enumerate(SWATCHES.values()):
        x, y = (i % 4) * 4, (i // 4) * 4
        rect(img, x, y, x + 3, y + 3, color)
    return img


def swatch(name):
    i = list(SWATCHES).index(name)
    x, y = (i % 4) * 4, (i // 4) * 4
    return {"uv": [x + 0.5, y + 0.5, x + 3.5, y + 3.5], "texture": "#1"}


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


def panel_elements():
    body = box([0, 0, FACE_Z], [16, 16, 16], "a", cull=("south",))
    body["faces"]["north"] = face("b")
    frame = [box([0, 15, FACE_Z - RIM], [16, 16, FACE_Z], "c"), box([0, 0, FACE_Z - RIM], [16, 1, FACE_Z], "c"),
             box([15, 1, FACE_Z - RIM], [16, 15, FACE_Z], "c"), box([0, 1, FACE_Z - RIM], [1, 15, FACE_Z], "c")]
    for side in frame[2:]:
        for f in side["faces"].values():
            f["uv"] = [0, 8, 8, 11]
    return [body] + frame


def on_face(u, v, half_u, half_v, height, faces, lift=0):
    return {"from": [16 - u - half_u, v - half_v, FACE_Z - lift - height], "to": [16 - u + half_u, v + half_v, FACE_Z - lift], "faces": faces}


def static_controls(controls):
    out = []
    for kind, u, v, color in controls:
        faces = {d: swatch(color) for d in ("north", "south", "east", "west", "up", "down")}
        black = {d: swatch("black") for d in ("north", "south", "east", "west", "up", "down")}
        if kind == "big":
            out += [on_face(u, v, 3.3, 3.3, 0.6, black), on_face(u, v, 2.6, 2.6, 2.0, faces, 0.6)]
        elif kind == "button":
            out += [on_face(u, v, 2, 2, 0.5, black), on_face(u, v, 1.5, 1.5, 1.6, faces)]
        elif kind == "switch":
            out += [on_face(u, v, 2.4, 3.6, 0.7, black), on_face(u, v - 1.2, 0.9, 0.9, 4.2, faces)]
        elif kind == "dial":
            out += [on_face(u, v, 4, 4, 0.5, black), on_face(u, v, 2.6, 2.6, 2.2, faces), on_face(u - 1.3, v, 1.9, 0.5, 0.6, {d: swatch("cream") for d in faces}, 2.2)]
        else:
            out.append(on_face(u, v, 1.6, 1.1, 0.7, faces))
    return out


def control_partials():
    cap = {d: face("d") for d in ("north", "south", "east", "west", "up", "down")}
    dark = {d: face("a", uv=[1, 1, 3, 3]) for d in ("north", "south", "east", "west", "up", "down")}

    def el(lo, hi, faces):
        return {"from": lo, "to": hi, "faces": faces}
    return {
        "panel/button": [el([-1.5, 0, -1.5], [1.5, 1.6, 1.5], cap)],
        "panel/button_collar": [el([-2, 0, -2], [2, 0.5, 2], dark)],
        "panel/big_button": [el([-2.6, 0.9, -2.6], [2.6, 2.6, 2.6], cap), el([-1, 0, -1], [1, 0.9, 1], cap)],
        "panel/big_button_collar": [el([-3.3, 0, -3.3], [3.3, 0.6, 3.3], dark)],
        "panel/switch_base": [el([-1.6, 0, -2.4], [1.6, 0.5, 2.4], dark)],
        "panel/switch_lever": [el([-0.5, 0, -0.5], [0.5, 3.2, 0.5], cap), el([-0.8, 2.6, -0.8], [0.8, 3.6, 0.8], cap)],
        "panel/dial": [el([-1.7, 0, -1.7], [1.7, 1.5, 1.7], cap),
                       dict(el([-1.7, 0, -1.7], [1.7, 1.5, 1.7], cap), rotation={"origin": [0, 0, 0], "axis": "y", "angle": 45})],
        "panel/dial_pointer": [el([-0.35, 1.5, -2.2], [0.35, 1.9, 0.4], cap)],
        "panel/dial_collar": [el([-2.7, 0, -2.7], [2.7, 0.35, 2.7], cap)],
        "panel/lamp": [el([-1.6, 0, -1.1], [1.6, 0.7, 1.1], cap)],
    }


def write_all(write, assets, data, out):
    panel_texture().save(os.path.join(out, "control_panel.png"))
    swatch_texture().save(os.path.join(out, "control_panel_parts.png"))
    screen_texture().save(os.path.join(out, "plant_screen.png"))
    tex = {"0": M + "block/control_panel", "particle": M + "block/control_panel"}
    write(os.path.join(assets, "models", "block", "control_panel.json"), {"parent": "minecraft:block/block", "textures": tex, "elements": panel_elements()})
    for path, elements in control_partials().items():
        write(os.path.join(assets, "models", "block", path + ".json"), {"textures": tex, "elements": elements})
    for name, (_, controls) in PANELS.items():
        write(os.path.join(assets, "blockstates", name + ".json"),
              {"variants": {"facing=" + d: ({"model": M + "block/control_panel", "y": y} if y else {"model": M + "block/control_panel"}) for d, y in ROTATIONS.items()}})
        write(os.path.join(assets, "models", "item", name + ".json"), {"parent": "minecraft:block/block", "textures": dict(tex, **{"1": M + "block/control_panel_parts"}),
                                                                         "elements": panel_elements() + static_controls(controls)})
        write(os.path.join(data, "criticalmass", "loot_table", "blocks", name + ".json"), {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{
            "type": "minecraft:item", "name": M + name, "functions": [{"function": "minecraft:copy_components", "source": "block_entity", "include": ["minecraft:custom_name"]}]}],
            "conditions": [{"condition": "minecraft:survives_explosion"}]}]})

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


LANG = dict({"block.criticalmass." + name: title for name, (title, _) in PANELS.items()}, **{
    "block.criticalmass.plant_screen": "Plant Screen",
    "criticalmass.panel.title.scram": "REACTOR",
    "criticalmass.panel.title.rods": "RODS",
    "criticalmass.panel.title.turbine": "TURBINE",
    "criticalmass.panel.title.breaker": "BREAKER",
    "criticalmass.panel.title.alarms": "ALARMS",
    "criticalmass.panel.title.lever": "LEVER",
    "criticalmass.panel.title.dial": "DIAL",
    "criticalmass.panel.label.scram": "SCRAM",
    "criticalmass.panel.label.reset": "RESET",
    "criticalmass.panel.label.rod_in": "IN",
    "criticalmass.panel.label.rod_out": "OUT",
    "criticalmass.panel.label.trip": "TRIP",
    "criticalmass.panel.label.ack": "ACK",
    "criticalmass.panel.unbound.reactor": "Not linked: use a Plant Linker on a Reactor Controller, then on this panel",
    "criticalmass.panel.unbound.turbine": "Not linked: use a Plant Linker on a Steam Turbine, then on this panel",
    "criticalmass.panel.unbound.generator": "Not linked: use a Plant Linker on a Generator, then on this panel",
    "criticalmass.panel.bound": "Linked to the machine at %s %s %s",
    "criticalmass.panel.lever_on": "On: redstone signal 15",
    "criticalmass.panel.lever_off": "Off",
    "criticalmass.panel.dial_on": "On: redstone signal %s",
    "criticalmass.panel.dial_off": "Off, set to %s",
    "criticalmass.panel.hint.scram": "SCRAM trips the reactor, RESET clears the trip. A redstone pulse presses SCRAM",
    "criticalmass.panel.hint.rods": "IN and OUT step every rod bank by 0.5%",
    "criticalmass.panel.hint.turbine": "TRIP shuts the steam off, RESET clears the trip. A redstone pulse presses TRIP",
    "criticalmass.panel.hint.breaker": "The switch closes and opens the generator breaker",
    "criticalmass.panel.hint.alarms": "New alarms flash and give a redstone signal until ACK is pressed. A comparator counts the active alarms",
    "criticalmass.panel.hint.lever": "Use to switch its redstone signal on and off",
    "criticalmass.panel.hint.dial": "Use to switch on and off, sneak-use to raise the signal strength",
    "criticalmass.panel.result.unbound": "This panel is not linked to a machine",
    "criticalmass.panel.result.scram": "Reactor tripped by hand",
    "criticalmass.panel.result.reset": "Rods commanded in and trip reset",
    "criticalmass.panel.result.reset_refused": "Rods commanded in; reset refused (clear the fault and the controller signal first)",
    "criticalmass.panel.result.jog": "Rods stepped 0.5%",
    "criticalmass.panel.result.turbine_trip": "Turbine tripped by hand",
    "criticalmass.panel.result.turbine_reset": "Turbine trip reset",
    "criticalmass.panel.result.breaker_open": "Generator breaker open",
    "criticalmass.panel.result.breaker_close": "Generator breaker closed",
    "criticalmass.panel.result.acknowledged": "Alarms acknowledged",
    "criticalmass.panel.result.on": "Switched on",
    "criticalmass.panel.result.off": "Switched off",
    "criticalmass.panel.result.dial_on": "Switched on at signal %s",
    "criticalmass.panel.result.strength": "Signal strength set to %s",
    "criticalmass.goggles.screen": "Plant Screen",
    "criticalmass.screen.powered": "Powered: %s FE stored",
    "criticalmass.screen.unpowered": "No power: feed Forge Energy into any screen of the wall",
    "criticalmass.screen.size": "Screen wall %s by %s: %s lines of %s characters",
    "criticalmass.screen.unlinked": "Shows Display Link text. Link a Reactor Controller with a Plant Linker for the reactor display, which needs a wall %s wide and %s high",
    "criticalmass.screen.linked": "Reactor display for the controller at %s %s %s",
    "criticalmass.screen.too_small": "Linked, but the reactor display needs a wall %s wide and %s high",
    "criticalmass.screen.link.too_small": "Linked, but this wall is %s by %s. The reactor display needs %s by %s",
    "criticalmass.screen.link.refused": "A screen wall links to a Reactor Controller, a Steam Turbine or a Generator",
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
    "criticalmass.screen.row.output": "OUTPUT",
    "criticalmass.screen.events": "EVENTS",
    "criticalmass.screen.events.none": "NOTHING TO REPORT",
    "criticalmass.screen.event.cleared": "CLEARED: %s",
})

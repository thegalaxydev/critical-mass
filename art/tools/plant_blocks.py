import json
import os

from PIL import Image

from handmade import AMBER, BLUE, BRASS, COPPER, GREEN, IRON, OUT, RED, STEEL, bevel, put, rect
from handmade_plant import disc, opening
from handmade_rest import LEAD, casing, plate

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
RES = os.path.join(ROOT, "src", "main", "resources")
ASSETS = os.path.join(RES, "assets", "criticalmass")
DATA = os.path.join(RES, "data")
M = "criticalmass:"
ROTATIONS = {"north": 0, "east": 90, "south": 180, "west": 270}
QUADS = {"a": [0, 0, 8, 8], "b": [8, 0, 16, 8], "c": [0, 8, 8, 16], "d": [8, 8, 16, 16]}


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", newline="\n", encoding="utf8") as f:
        json.dump(obj, f, indent=1)
        f.write("\n")


def cube(name, faces, render_type=None):
    model = {"parent": "minecraft:block/block", "textures": {"0": M + "block/" + name, "particle": M + "block/" + name},
             "elements": [{"from": [0, 0, 0], "to": [16, 16, 16],
                           "faces": {d: {"uv": QUADS[q], "texture": "#0", "cullface": d} for d, q in faces.items()}}]}
    if render_type:
        model["render_type"] = render_type
    return model


def horizontal(name):
    return {"variants": {"facing=" + d: ({"model": M + "block/" + name, "y": y} if y else {"model": M + "block/" + name}) for d, y in ROTATIONS.items()}}


def loot(name):
    return {"type": "minecraft:block",
            "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": M + name}], "conditions": [{"condition": "minecraft:survives_explosion"}]}],
            "random_sequence": "criticalmass:blocks/" + name}


def atlas(quads):
    img = Image.new("RGBA", (32, 32))
    for key, painter in quads.items():
        u0, v0, _, _ = QUADS[key]
        img.paste(painter(16, 16), (u0 * 2, v0 * 2))
    return img


def generator_texture():
    def front(w, h):
        img = casing(IRON, STEEL)(w, h)
        for y in (4, 7, 10):
            rect(img, 4, y, 11, y, COPPER[3])
            put(img, 4, y, COPPER[5])
            put(img, 11, y, COPPER[1])
        put(img, 12, 12, AMBER[3])
        put(img, 3, 12, BRASS[4])
        return img

    def back(w, h):
        img = casing(IRON, STEEL)(w, h)
        opening(img, 4, 4, 8, BRASS, IRON)
        return img

    def side(w, h):
        img = casing(IRON, STEEL)(w, h)
        for x in range(3, 13):
            ramp = COPPER if x % 2 else STEEL
            for y in range(4, 12):
                put(img, x, y, ramp[4 if y == 4 else 1 if y == 11 else 3] if x % 2 else STEEL[2])
        rect(img, 3, 7, 12, 8, IRON[2])
        return img

    def top(w, h):
        img = plate(IRON, 3)(w, h)
        for x in range(3, 13, 2):
            rect(img, x, 3, x, 12, IRON[1])
            rect(img, x + 1, 3, x + 1, 12, IRON[4])
        return img
    return atlas({"a": front, "b": back, "c": side, "d": top})


def viewport_base():
    img = casing(IRON, STEEL)(16, 16)
    for y in range(3, 13):
        for x in range(3, 13):
            img.putpixel((x, y), (118, 158, 178, 64))
    for k in range(4):
        img.putpixel((4 + k, 7 - k), (206, 232, 242, 120))
        img.putpixel((8 + k, 11 - k), (206, 232, 242, 96))
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
        put(img, x, y, IRON[5])
    return img


def viewport_textures():
    base = viewport_base()
    out = {"reactor_viewport": base}
    for stage in (1, 2, 3):
        crack = Image.open(os.path.join(OUT, "casing_crack_%d.png" % stage)).convert("RGBA").resize((16, 16), Image.NEAREST)
        img = base.copy()
        img.alpha_composite(crack)
        out["reactor_viewport_crack_%d" % stage] = img
    return out


def viewport_models():
    models = {}
    for stage in range(4):
        name = "reactor_viewport" if stage == 0 else "reactor_viewport_crack_%d" % stage
        models[name] = {"parent": "minecraft:block/cube_all", "textures": {"all": M + "block/" + name}, "render_type": "minecraft:translucent"}
    return models


def viewport_state(name):
    return {"variants": {"crack=%d" % i: {"model": M + "block/" + (name if i == 0 else "%s_crack_%d" % (name, i))} for i in range(4)}}


PILLAR = {"y": {}, "x": {"x": 90, "y": 90}, "z": {"x": 90}}
WINDOW_DARK = (0x17, 0x19, 0x1c)
GLOWS = {1: {(0x17, 0x19, 0x1c): (14, 44, 84), (0x1e, 0x21, 0x26): (18, 54, 98), (0x23, 0x26, 0x2b): (16, 48, 90), (0xd2, 0xc7, 0xb4): (196, 222, 240), (0x8f, 0x82, 0x73): (112, 150, 190)},
         2: {(0x17, 0x19, 0x1c): (26, 96, 168), (0x1e, 0x21, 0x26): (34, 112, 186), (0x23, 0x26, 0x2b): (30, 104, 176), (0xd2, 0xc7, 0xb4): (232, 246, 255), (0x8f, 0x82, 0x73): (150, 204, 240)},
         3: {(0x17, 0x19, 0x1c): (126, 34, 14), (0x1e, 0x21, 0x26): (146, 44, 16), (0x23, 0x26, 0x2b): (136, 38, 14), (0xd2, 0xc7, 0xb4): (255, 226, 160), (0x8f, 0x82, 0x73): (244, 150, 70)}}


def channel_variants():
    control = Image.open(os.path.join(OUT, "control_channel.png")).convert("RGBA")
    for level in range(5):
        img = control.copy()
        rows = round(10 * level / 4)
        for y in range(3, 13):
            if y == 7:
                continue
            if y - 3 >= rows:
                for x in (4, 5, 7, 8):
                    put(img, x, y, WINDOW_DARK)
            put(img, 11, y, (0xf7, 0xcb, 0x6c))
            put(img, 12, y, (0x9e, 0x69, 0x47))
        marker = 3 + min(9, rows)
        if marker == 7:
            marker = 8
        put(img, 11, marker, (0xff, 0xbe, 0x4a))
        put(img, 12, marker, (0xd8, 0x8a, 0x1c))
        name = "control_channel_rod_%d" % level
        img.save(os.path.join(OUT, name + ".png"))
        write(os.path.join(ASSETS, "models", "block", name + ".json"), {"parent": M + "block/control_channel", "textures": {"0": M + "block/" + name, "particle": M + "block/" + name}})
    write(os.path.join(ASSETS, "blockstates", "control_channel.json"), {"variants": {
        "axis=%s,rod=%d" % (axis, level): dict({"model": M + "block/control_channel_rod_%d" % level}, **rot) for axis, rot in PILLAR.items() for level in range(5)}})
    fuel = Image.open(os.path.join(OUT, "fuel_channel.png")).convert("RGBA")
    for level, table in GLOWS.items():
        img = fuel.copy()
        for y in range(img.height):
            for x in range(img.width):
                c = img.getpixel((x, y))[:3]
                if c in table:
                    put(img, x, y, table[c])
        name = "fuel_channel_glow_%d" % level
        img.save(os.path.join(OUT, name + ".png"))
        write(os.path.join(ASSETS, "models", "block", name + ".json"), {"parent": M + "block/fuel_channel", "textures": {"0": M + "block/" + name, "particle": M + "block/" + name}})
    write(os.path.join(ASSETS, "blockstates", "fuel_channel.json"), {"variants": {
        "axis=%s,glow=%d" % (axis, level): dict({"model": M + "block/" + ("fuel_channel" if level == 0 else "fuel_channel_glow_%d" % level)}, **rot) for axis, rot in PILLAR.items() for level in range(4)}})


def rod_partials():
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        band = y % 8 == 0
        put(img, 0, y, BRASS[3] if band else LEAD[4])
        put(img, 1, y, BRASS[1] if band else LEAD[2])
    bevel(img, 4, 0, 7, 1, BRASS, 3)
    bevel(img, 4, 2, 7, 5, BRASS, 3)
    img.save(os.path.join(OUT, "control_rod_shaft.png"))
    tex = {"0": M + "block/control_rod_shaft", "particle": M + "block/control_rod_shaft"}
    side = {"uv": [0, 0, 2, 16], "texture": "#0"}
    write(os.path.join(ASSETS, "models", "block", "control_rod_shaft.json"), {"textures": tex, "elements": [
        {"from": [7, 0, 7], "to": [9, 16, 9], "faces": {d: side for d in ("north", "south", "east", "west")}}]})
    cap_side = {"uv": [4, 0, 8, 2], "texture": "#0"}
    cap_top = {"uv": [4, 2, 8, 6], "texture": "#0"}
    write(os.path.join(ASSETS, "models", "block", "control_rod_cap.json"), {"textures": tex, "elements": [
        {"from": [6, 0, 6], "to": [10, 2, 10], "faces": {"north": cap_side, "south": cap_side, "east": cap_side, "west": cap_side, "up": cap_top, "down": cap_top}}]})


GLASS_DARK = (20, 22, 26)
ITEM_OUT = os.path.join(os.path.dirname(OUT), "item")


def panel_model(name, x0, y0, x1, y1):
    w, h = x1 - x0, y1 - y0
    plain = {"uv": [8, 0, 8 + w / 2, h / 2], "texture": "#0"}
    edge = {"uv": [8, 0, 9.5, h / 2], "texture": "#0"}
    cap = {"uv": [8, 0, 8 + w / 2, 1.5], "texture": "#0"}
    return {"parent": "minecraft:block/block", "textures": {"0": M + "block/" + name, "particle": M + "block/" + name},
            "elements": [{"from": [x0, y0, 13], "to": [x1, y1, 16], "faces": {
                "north": {"uv": [x0 / 2, (16 - y1) / 2, x1 / 2, (16 - y0) / 2], "texture": "#0"},
                "south": plain, "east": edge, "west": edge, "up": cap, "down": cap}}]}


def panel_atlas(front):
    img = atlas({"a": front, "b": plate(IRON, 3)})
    return img


def gauge_texture():
    def front(w, h):
        img = Image.new("RGBA", (w, h))
        bevel(img, 1, 1, 14, 14, IRON, 3)
        rect(img, 2, 2, 13, 13, STEEL[2])
        rect(img, 2, 2, 13, 3, GLASS_DARK)
        rect(img, 2, 12, 13, 13, GLASS_DARK)
        disc(img, 8, 7.9, 3.7, lambda x, y, d: (BRASS[3] if x + y < 15.5 else BRASS[1]) if d > 2.9 else (232, 228, 214))
        for x, y in ((5, 9), (5, 6), (7, 5), (8, 5), (10, 6), (10, 9)):
            put(img, x, y, (150, 144, 128))
        return img
    return panel_atlas(front)


def button_texture():
    def front(w, h):
        img = Image.new("RGBA", (w, h))
        bevel(img, 4, 4, 11, 11, IRON, 3)
        rect(img, 5, 5, 10, 10, STEEL[1])
        rect(img, 5, 5, 10, 5, GLASS_DARK)
        return img
    return panel_atlas(front)


def annunciator_texture():
    def front(w, h):
        img = Image.new("RGBA", (w, h))
        bevel(img, 1, 1, 14, 14, IRON, 3)
        rect(img, 2, 2, 13, 13, IRON[1])
        rect(img, 2, 3, 13, 7, (24, 26, 30))
        rect(img, 2, 9, 13, 13, (24, 26, 30))
        for x in (5, 9):
            for y in list(range(3, 8)) + list(range(9, 14)):
                put(img, x, y, (34, 37, 42))
        return img
    return panel_atlas(front)


def linker_item():
    img = Image.new("RGBA", (16, 16))
    for k in range(7):
        put(img, 3 + k, 12 - k, BRASS[3])
        put(img, 4 + k, 12 - k, BRASS[1])
        put(img, 3 + k, 11 - k, BRASS[4])
    bevel(img, 9, 2, 13, 6, IRON, 3)
    put(img, 11, 4, BLUE[4])
    put(img, 10, 4, BLUE[2])
    put(img, 12, 4, BLUE[2])
    put(img, 11, 3, BLUE[3])
    put(img, 11, 5, BLUE[1])
    put(img, 2, 13, BRASS[1])
    os.makedirs(ITEM_OUT, exist_ok=True)
    img.save(os.path.join(ITEM_OUT, "plant_linker.png"))
    path = os.path.join(ASSETS, "models", "item", "plant_linker.json")
    with open(path, "w", newline="\n", encoding="utf8") as f:
        json.dump({"parent": "minecraft:item/handheld", "textures": {"layer0": M + "item/plant_linker"}}, f, indent=2)
        f.write("\n")


SIDES = {"north": "a", "south": "a", "east": "a", "west": "a", "up": "b", "down": "c"}
CLEAR = (0, 0, 0, 0)


def basin_texture():
    def side(w, h):
        img = casing(IRON, STEEL)(w, h)
        rect(img, 3, 9, 12, 12, BLUE[1])
        rect(img, 3, 9, 12, 9, BLUE[3])
        return img

    def top(w, h):
        img = plate(IRON, 3)(w, h)
        rect(img, 2, 2, 13, 13, BLUE[1])
        for k in range(2, 14, 3):
            rect(img, k, 2, k, 13, BLUE[0])
            rect(img, 2, k, 13, k, BLUE[0])
        for x, y in ((4, 5), (9, 4), (6, 10), (11, 9)):
            put(img, x, y, BLUE[3])
        return img
    return atlas({"a": side, "b": top, "c": plate(IRON, 2)})


def distributor_texture():
    def side(w, h):
        img = casing(IRON, STEEL)(w, h)
        rect(img, 2, 6, 13, 8, COPPER[3])
        rect(img, 2, 6, 13, 6, COPPER[5])
        rect(img, 2, 8, 13, 8, COPPER[1])
        for x in (4, 7, 10):
            put(img, x, 10, COPPER[2])
            put(img, x, 11, BLUE[3])
            put(img, x, 12, BLUE[2])
        return img

    def top(w, h):
        img = plate(IRON, 3)(w, h)
        rect(img, 2, 2, 13, 13, STEEL[1])
        rect(img, 7, 2, 8, 13, COPPER[3])
        rect(img, 2, 7, 13, 8, COPPER[3])
        for x, y in ((4, 4), (11, 4), (4, 11), (11, 11)):
            put(img, x, y, COPPER[4])
        return img

    def bottom(w, h):
        img = plate(IRON, 2)(w, h)
        rect(img, 2, 2, 13, 13, STEEL[0])
        for x in (4, 7, 10):
            for y in (4, 7, 10):
                put(img, x, y, COPPER[4])
                put(img, x + 1, y + 1, COPPER[1])
        return img
    return atlas({"a": side, "b": top, "c": bottom})


def fan_texture():
    def side(w, h):
        img = plate(IRON, 3)(w, h)
        for y in range(3, 13):
            for x in range(3, 13):
                if y % 2 == 1:
                    img.putpixel((x, y), CLEAR)
                else:
                    put(img, x, y, IRON[2])
        return img

    def top(w, h):
        img = plate(IRON, 3)(w, h)
        for y in range(2, 14):
            for x in range(2, 14):
                if (x in (7, 8)) or (y in (7, 8)):
                    put(img, x, y, IRON[2])
                else:
                    img.putpixel((x, y), CLEAR)
        bevel(img, 5, 5, 10, 10, IRON, 3)
        return img

    def blade(w, h):
        img = plate(IRON, 4)(w, h)
        rect(img, 0, 0, 15, 15, IRON[4])
        for x in range(0, 16, 4):
            rect(img, x, 0, x, 15, IRON[2])
        return img
    return atlas({"a": side, "b": top, "c": top, "d": blade})


def fan_models():
    name = "tower_fan"
    tex = {"0": M + "block/" + name, "particle": M + "block/" + name}
    blade = {"uv": QUADS["d"], "texture": "#0"}
    faces = {d: blade for d in ("north", "south", "east", "west", "up", "down")}
    return {name: cube(name, SIDES, "minecraft:cutout"),
            "tower_fan_rotor": {"textures": tex, "elements": [
                {"from": [1, 7, 6.5], "to": [15, 8, 9.5], "faces": faces},
                {"from": [6.5, 7, 1], "to": [9.5, 8, 15], "faces": faces},
                {"from": [6, 5, 6], "to": [10, 16, 10], "faces": faces}]}}


def feedwater_texture():
    def side(w, h):
        img = casing(COPPER, STEEL)(w, h)
        rect(img, 7, 3, 8, 12, BLUE[1])
        rect(img, 7, 7, 8, 12, BLUE[3])
        for y in (3, 12):
            put(img, 6, y, BRASS[3])
            put(img, 9, y, BRASS[3])
        return img

    def top(w, h):
        img = plate(COPPER, 3)(w, h)
        opening(img, 4, 4, 8, BLUE, COPPER)
        return img
    return atlas({"a": side, "b": top, "c": plate(COPPER, 2)})


def pressurizer_texture():
    def side(w, h):
        img = casing(IRON, STEEL)(w, h)
        rect(img, 2, 4, 13, 4, BRASS[3])
        rect(img, 2, 11, 13, 11, BRASS[2])
        for k, (x, y) in enumerate(((4, 8), (5, 7), (6, 8), (7, 9), (8, 8), (9, 7), (10, 8), (11, 9))):
            put(img, x, y, AMBER[3] if k % 2 else AMBER[2])
        put(img, 12, 6, RED[3])
        put(img, 3, 6, GREEN[3])
        return img

    def top(w, h):
        img = plate(IRON, 3)(w, h)
        opening(img, 5, 5, 6, BRASS, IRON)
        for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
            put(img, x, y, IRON[5])
        return img
    return atlas({"a": side, "b": top, "c": plate(IRON, 2)})


def simple_state(name):
    return {"variants": {"": {"model": M + "block/" + name}}}


BLOCKS = {
    "tower_basin": {"lang": "Cooling Tower Basin", "texture": basin_texture, "model": lambda n: cube(n, SIDES), "state": simple_state},
    "tower_distributor": {"lang": "Cooling Tower Distributor", "texture": distributor_texture, "model": lambda n: cube(n, SIDES), "state": simple_state},
    "tower_fan": {"lang": "Cooling Tower Fan", "texture": fan_texture, "models": fan_models, "state": simple_state},
    "feedwater_tank": {"lang": "Feedwater Tank", "texture": feedwater_texture, "model": lambda n: cube(n, SIDES), "state": simple_state},
    "pressurizer": {"lang": "Pressurizer", "texture": pressurizer_texture, "model": lambda n: cube(n, SIDES), "state": simple_state},
    "plant_gauge": {"lang": "Gauge", "texture": gauge_texture, "model": lambda n: panel_model(n, 1, 1, 15, 15), "state": horizontal},
    "control_button": {"lang": "Control Button", "texture": button_texture, "model": lambda n: panel_model(n, 4, 4, 12, 12), "state": horizontal},
    "annunciator": {"lang": "Annunciator", "texture": annunciator_texture, "model": lambda n: panel_model(n, 1, 1, 15, 15), "state": horizontal},
    "reactor_viewport": {
        "lang": "Reactor Viewport",
        "textures": viewport_textures,
        "models": viewport_models,
        "state": viewport_state,
    },
    "generator": {
        "lang": "Generator",
        "texture": generator_texture,
        "model": lambda n: cube(n, {"north": "a", "south": "b", "east": "c", "west": "c", "up": "d", "down": "d"}),
        "state": horizontal,
    },
}

LANG = {
    "criticalmass.basin.ok": "Collecting",
    "criticalmass.basin.low": "Water low: add makeup water",
    "criticalmass.basin.dry": "Dry",
    "criticalmass.feedwater.ok": "Level normal",
    "criticalmass.feedwater.low": "Level low: add makeup water",
    "criticalmass.feedwater.empty": "Empty",
    "criticalmass.tower.no_fill": "No fill below: stack Cooling Tower Packing under the distributor",
    "criticalmass.tower.no_basin": "No basin under the fill",
    "criticalmass.goggles.tower_fill": "Fill: %s of up to %s blocks",
    "criticalmass.goggles.tower_airflow": "Airflow: %s%%",
    "criticalmass.goggles.tower_fan": "Cooling Tower Fan",
    "criticalmass.goggles.pressurizer": "Pressurizer",
    "criticalmass.goggles.pressurizer_pressure": "Primary pressure: %s bar (set to %s)",
    "criticalmass.goggles.pressurizer_boiling": "Coolant boils at %s \u00b0C",
    "criticalmass.pressurizer.setpoint": "Pressure setpoint",
    "criticalmass.pressurizer.no_vessel": "Not touching a reactor vessel",
    "criticalmass.pressurizer.off": "Heaters off: redstone signal",
    "criticalmass.pressurizer.heating": "Heaters on: raising pressure",
    "criticalmass.pressurizer.holding": "Holding pressure",
    "criticalmass.pressurizer.no_power": "Pressure falling: heaters need Forge Energy",
    "criticalmass.alarm.pressure": "Vessel coolant boiling: pressure too low for its temperature",
    "item.criticalmass.plant_linker": "Plant Linker",
    "criticalmass.linker.tooltip": "Use on a machine to remember it, then on a gauge, button or annunciator to bind it",
    "criticalmass.linker.tooltip_stored": "Remembers the block at %s %s %s (sneak-use in the air to clear)",
    "criticalmass.linker.stored": "Remembered %s at %s %s %s",
    "criticalmass.linker.bound": "Bound to %s at %s %s %s",
    "criticalmass.linker.refused": "That panel cannot use the remembered block",
    "criticalmass.linker.nothing_stored": "Use the linker on a machine first",
    "criticalmass.linker.cleared": "Linker cleared",
    "criticalmass.panel.unbound": "Not bound: use a Plant Linker",
    "criticalmass.panel.bound": "Bound to %s at %s %s %s",
    "criticalmass.goggles.gauge": "Gauge",
    "criticalmass.goggles.button": "Control Button",
    "criticalmass.goggles.annunciator": "Annunciator",
    "criticalmass.gauge.setpoint": "Alarm setpoint (% of scale)",
    "criticalmass.gauge.reading": "%s: %s %s",
    "criticalmass.gauge.no_reading": "%s: no reading from this target",
    "criticalmass.gauge.scale": "Scale %s to %s %s",
    "criticalmass.gauge.alarm_above": "Alarm at or above %s%% of scale",
    "criticalmass.gauge.alarm_below": "Alarm at or below %s%% of scale",
    "criticalmass.gauge.channel_set": "Gauge shows %s",
    "criticalmass.gauge.hint": "Use to change what it shows; sneak-use to flip the alarm side",
    "criticalmass.channel.temperature": "Temperature",
    "criticalmass.channel.coolant_temperature": "Coolant temperature",
    "criticalmass.channel.pressure": "Pressure",
    "criticalmass.channel.level": "Level",
    "criticalmass.channel.flow": "Flow",
    "criticalmass.channel.power": "Power",
    "criticalmass.channel.rods_a": "Rod bank A insertion",
    "criticalmass.channel.rods_b": "Rod bank B insertion",
    "criticalmass.channel.rods_c": "Rod bank C insertion",
    "criticalmass.channel.rods_d": "Rod bank D insertion",
    "criticalmass.channel.speed": "Speed",
    "criticalmass.channel.output": "Output",
    "criticalmass.channel.temperature.short": "TEMP",
    "criticalmass.channel.coolant_temperature.short": "COOLANT",
    "criticalmass.channel.pressure.short": "PRESSURE",
    "criticalmass.channel.level.short": "LEVEL",
    "criticalmass.channel.flow.short": "FLOW",
    "criticalmass.channel.power.short": "POWER",
    "criticalmass.channel.rods_a.short": "RODS A",
    "criticalmass.channel.rods_b.short": "RODS B",
    "criticalmass.channel.rods_c.short": "RODS C",
    "criticalmass.channel.rods_d.short": "RODS D",
    "criticalmass.channel.speed.short": "SPEED",
    "criticalmass.channel.output.short": "OUTPUT",
    "criticalmass.button.function": "Button function",
    "criticalmass.button.does": "Function: %s",
    "criticalmass.button.hint": "Use to press; a redstone pulse presses it too",
    "criticalmass.button.scram": "SCRAM the reactor",
    "criticalmass.button.reset": "Reset the reactor trip",
    "criticalmass.button.bank_a_in": "Rod bank A in 5%",
    "criticalmass.button.bank_a_out": "Rod bank A out 5%",
    "criticalmass.button.bank_b_in": "Rod bank B in 5%",
    "criticalmass.button.bank_b_out": "Rod bank B out 5%",
    "criticalmass.button.bank_c_in": "Rod bank C in 5%",
    "criticalmass.button.bank_c_out": "Rod bank C out 5%",
    "criticalmass.button.bank_d_in": "Rod bank D in 5%",
    "criticalmass.button.bank_d_out": "Rod bank D out 5%",
    "criticalmass.button.all_rods_in": "All rod banks fully in",
    "criticalmass.button.turbine_trip": "Trip the turbine",
    "criticalmass.button.turbine_reset": "Reset the turbine trip",
    "criticalmass.button.breaker_open": "Open the generator breaker",
    "criticalmass.button.breaker_close": "Close the generator breaker",
    "criticalmass.button.acknowledge": "Acknowledge alarms",
    "criticalmass.button.scram.short": "SCRAM",
    "criticalmass.button.reset.short": "RESET",
    "criticalmass.button.bank_a_in.short": "A IN",
    "criticalmass.button.bank_a_out.short": "A OUT",
    "criticalmass.button.bank_b_in.short": "B IN",
    "criticalmass.button.bank_b_out.short": "B OUT",
    "criticalmass.button.bank_c_in.short": "C IN",
    "criticalmass.button.bank_c_out.short": "C OUT",
    "criticalmass.button.bank_d_in.short": "D IN",
    "criticalmass.button.bank_d_out.short": "D OUT",
    "criticalmass.button.all_rods_in.short": "RODS IN",
    "criticalmass.button.turbine_trip.short": "TRIP",
    "criticalmass.button.turbine_reset.short": "RESET",
    "criticalmass.button.breaker_open.short": "OPEN",
    "criticalmass.button.breaker_close.short": "CLOSE",
    "criticalmass.button.acknowledge.short": "ACK",
    "criticalmass.button.result.unbound": "Button is not bound to anything",
    "criticalmass.button.result.wrong_target": "The bound block cannot do that",
    "criticalmass.button.result.scram": "Reactor tripped by hand",
    "criticalmass.button.result.reset": "Rods commanded in and trip reset",
    "criticalmass.button.result.reset_refused": "Rods commanded in; reset refused (clear the fault and the controller signal first)",
    "criticalmass.button.result.rods_in": "All rod banks commanded in",
    "criticalmass.button.result.jog": "Rod bank target moved 5%",
    "criticalmass.button.result.turbine_trip": "Turbine tripped by hand",
    "criticalmass.button.result.turbine_reset": "Turbine trip reset",
    "criticalmass.button.result.breaker_open": "Generator breaker open",
    "criticalmass.button.result.breaker_close": "Generator breaker closed",
    "criticalmass.button.result.acknowledged": "Alarms acknowledged",
    "criticalmass.alarm.none": "No alarms",
    "criticalmass.alarm.hint": "Use to acknowledge. New alarms flash and give a redstone signal until acknowledged",
    "criticalmass.alarm.trip": "Reactor trip",
    "criticalmass.alarm.low_flow": "No coolant flow through a hot core",
    "criticalmass.alarm.core_hot": "Fuel overheating",
    "criticalmass.alarm.low_coolant": "Vessel coolant below half",
    "criticalmass.alarm.casing": "Casing damaged or breached",
    "criticalmass.alarm.trip.short": "TRIP",
    "criticalmass.alarm.low_flow.short": "FLOW",
    "criticalmass.alarm.core_hot.short": "HOT",
    "criticalmass.alarm.low_coolant.short": "LEVEL",
    "criticalmass.alarm.casing.short": "SHELL",
    "criticalmass.alarm.pressure.short": "BOIL",
    "criticalmass.goggles.rod_bank": "Rod bank %s (use with an empty hand to change)",
    "criticalmass.rod.bank_set": "Rod bank %s",
    "criticalmass.generator.load": "Electrical load",
    "criticalmass.generator.breaker_open": "Breaker open: full redstone signal",
    "criticalmass.goggles.generator": "Generator",
    "criticalmass.goggles.generator_output": "Output: %s FE/t (set to %s)",
    "criticalmass.goggles.generator_stored": "Buffer: %s / %s FE",
}


def lang(entries):
    path = os.path.join(ASSETS, "lang", "en_us.json")
    data = json.load(open(path, encoding="utf8"))
    changed = False
    for k, v in entries.items():
        if data.get(k) != v:
            data[k] = v
            changed = True
    if changed:
        with open(path, "w", newline="\n", encoding="utf8") as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
            f.write("\n")


def pickaxe(names):
    path = os.path.join(DATA, "minecraft", "tags", "block", "mineable", "pickaxe.json")
    data = json.load(open(path, encoding="utf8"))
    for n in names:
        if M + n not in data["values"]:
            data["values"].append(M + n)
    with open(path, "w", newline="\n", encoding="utf8") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def main():
    entries = dict(LANG)
    for name, d in BLOCKS.items():
        if d.get("texture"):
            d["texture"]().save(os.path.join(OUT, name + ".png"))
        for tex_name, tex in (d["textures"]() if d.get("textures") else {}).items():
            tex.save(os.path.join(OUT, tex_name + ".png"))
        for model_name, model in (d["models"]() if d.get("models") else {name: d["model"](name)}).items():
            write(os.path.join(ASSETS, "models", "block", model_name + ".json"), model)
        write(os.path.join(ASSETS, "blockstates", name + ".json"), d["state"](name))
        write(os.path.join(ASSETS, "models", "item", name + ".json"), {"parent": M + "block/" + d.get("item", name)})
        write(os.path.join(DATA, "criticalmass", "loot_table", "blocks", name + ".json"), loot(name))
        entries["block.criticalmass." + name] = d["lang"]
    channel_variants()
    rod_partials()
    linker_item()
    pickaxe(BLOCKS)
    lang(entries)
    print("plant blocks:", ", ".join(BLOCKS))


if __name__ == "__main__":
    main()

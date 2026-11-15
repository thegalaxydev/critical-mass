import json
import os

from PIL import Image

from handmade import AMBER, BLUE, BRASS, COPPER, GREEN, IRON, OUT, RED, STEEL, bevel, put, rect
from handmade_plant import disc, opening
from handmade_rest import LEAD, casing, plate
import screen_blocks

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


GLASS_DARK = (20, 22, 26)
ITEM_OUT = os.path.join(os.path.dirname(OUT), "item")


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




def centrifuge_texture():
    def frame(w, h):
        img = plate(IRON, 3)(w, h)
        rect(img, 0, 0, 15, 1, IRON[4])
        rect(img, 0, 14, 15, 15, IRON[1])
        for x in (2, 13):
            put(img, x, 7, IRON[5])
            put(img, x, 8, IRON[1])
        rect(img, 5, 6, 10, 9, BRASS[2])
        rect(img, 6, 7, 9, 8, BRASS[4])
        return img

    def cap(w, h):
        img = plate(IRON, 3)(w, h)
        bevel(img, 4, 4, 11, 11, STEEL, 3)
        rect(img, 6, 6, 9, 9, IRON[1])
        for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
            put(img, x, y, IRON[5])
        return img

    def drum(w, h):
        img = plate(STEEL, 3)(w, h)
        for x in range(0, 16, 4):
            rect(img, x, 0, x, 15, STEEL[4])
            rect(img, x + 3, 0, x + 3, 15, STEEL[1])
        rect(img, 0, 3, 15, 4, BRASS[3])
        rect(img, 0, 11, 15, 12, BRASS[2])
        for x in (1, 6, 11):
            put(img, x, 7, GREEN[4])
            put(img, x + 1, 7, GREEN[2])
        return img
    return atlas({"a": frame, "b": cap, "c": cap, "d": drum})


def centrifuge_models():
    name = "centrifuge"
    tex = {"0": M + "block/" + name, "particle": M + "block/" + name}

    def box(lo, hi, quad, caps=None):
        return {"from": lo, "to": hi, "faces": {d: {"uv": QUADS[caps if caps and d in ("up", "down") else quad], "texture": "#0"} for d in ("north", "south", "east", "west", "up", "down")}}
    base = [box([0, 0, 0], [16, 3, 16], "a", caps="b"), box([0, 13, 0], [16, 16, 16], "a", caps="b")]
    base += [box([x, 3, z], [x + 2, 13, z + 2], "a") for x in (0, 14) for z in (0, 14)]
    rotor = [box([3, 3, 3], [13, 13, 13], "d", caps="b"), box([2.5, 5, 2.5], [13.5, 6, 13.5], "a"), box([2.5, 10, 2.5], [13.5, 11, 13.5], "a")]
    return {name: {"parent": "minecraft:block/block", "textures": tex, "elements": base},
            "centrifuge_rotor": {"textures": tex, "elements": rotor},
            "centrifuge_item": {"parent": "minecraft:block/block", "textures": tex, "elements": base + rotor}}


def uranium_items():
    import colorsys
    src = Image.open(os.path.join(ITEM_OUT, "uranium_concentrate.png")).convert("RGBA")

    def tinted(hue, saturation, value):
        img = src.copy()
        for x in range(img.width):
            for y in range(img.height):
                r, g, b, a = img.getpixel((x, y))
                if a == 0:
                    continue
                h, s, v = colorsys.rgb_to_hsv(r / 255.0, g / 255.0, b / 255.0)
                h = hue if hue is not None else h
                r2, g2, b2 = colorsys.hsv_to_rgb(h, min(1.0, s * saturation), min(1.0, v * value))
                img.putpixel((x, y), (int(r2 * 255), int(g2 * 255), int(b2 * 255), a))
        return img
    for item, img in (("enriched_uranium", tinted(0.24, 0.95, 1.08)), ("depleted_uranium", tinted(None, 0.18, 0.72))):
        img.save(os.path.join(ITEM_OUT, item + ".png"))
        write(os.path.join(ASSETS, "models", "item", item + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": M + "item/" + item}})


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


def corium_textures():
    import random
    out = {}
    for name, hot in (("corium", False), ("corium_hot", True)):
        rnd = random.Random(7)
        img = Image.new("RGBA", (16, 16))
        for y in range(16):
            for x in range(16):
                v = rnd.random()
                put(img, x, y, (34, 30, 30) if v < 0.5 else (48, 42, 40) if v < 0.85 else (24, 21, 22))
        cracks = [(2, 3), (3, 3), (4, 4), (5, 5), (6, 5), (7, 6), (8, 7), (9, 7), (10, 8), (11, 9), (12, 9), (13, 10), (5, 11), (6, 12), (7, 12), (8, 13), (10, 3), (11, 2), (12, 2), (3, 9), (4, 10)]
        for x, y in cracks:
            put(img, x, y, (255, 150, 40) if hot else (86, 60, 44))
        if hot:
            for x, y in cracks[::3]:
                put(img, x, y, (255, 226, 130))
                if y + 1 < 16:
                    put(img, x, y + 1, (190, 70, 20))
        out[name] = img
    return out


def corium_models():
    return {"corium": {"parent": "minecraft:block/cube_all", "textures": {"all": M + "block/corium"}},
            "corium_hot": {"parent": "minecraft:block/cube_all", "textures": {"all": M + "block/corium_hot"}}}


def simple_state(name):
    return {"variants": {"": {"model": M + "block/" + name}}}


BLOCKS = {
    "corium": {"lang": "Corium", "textures": corium_textures, "models": corium_models,
               "state": lambda n: {"variants": {"hot=false": {"model": M + "block/corium"}, "hot=true": {"model": M + "block/corium_hot"}}}},
    "ruined_channel": {"lang": "Ruined Fuel Channel", "model": lambda n: {"parent": M + "block/fuel_channel", "textures": {"0": M + "block/ruined_channel", "particle": M + "block/ruined_channel"}}, "state": simple_state},
    "centrifuge": {"lang": "Centrifuge", "texture": centrifuge_texture, "models": centrifuge_models, "state": simple_state, "item": "centrifuge_item"},
    "pressurizer": {"lang": "Pressurizer", "texture": pressurizer_texture, "model": lambda n: cube(n, SIDES), "state": simple_state},
    "reactor_viewport": {
        "lang": "Reactor Viewport",
        "textures": viewport_textures,
        "models": viewport_models,
        "state": viewport_state,
    },
}

LANG = {
    "criticalmass.reactor.status.blocked_flow": "Start blocked: get coolant flowing through the vessel first",
    "criticalmass.reactor.status.blocked_coolant": "Start blocked: fill the vessel at least half full of coolant",
    "criticalmass.goggles.corium": "Corium",
    "criticalmass.goggles.corium_decay": "Decay heat: %s HU/t",
    "criticalmass.goggles.tower_rejecting": "Whole tower: %s HU/t",
    "criticalmass.circuit.tank_end": "Stopped: a Fluid Tank in the loop needs a second pipe to carry on from",
    "criticalmass.goggles.tower_airflow": "Airflow: %s%%",
    "block.criticalmass.reinforced_pipe": "Reinforced Pipe",
    "block.criticalmass.radiator_panel": "Radiator",
    "criticalmass.radiator.covered": "All four sides covered",
    "criticalmass.goggles.radiator_faces": "%s of 4 sides open, airflow x%s",
    "block.criticalmass.reinforced_valve": "Reinforced Valve",
    "criticalmass.valve.open": "Open",
    "criticalmass.valve.closed": "Closed: no coolant passes",
    "item.criticalmass.enriched_uranium": "Enriched Uranium",
    "item.criticalmass.depleted_uranium": "Depleted Uranium",
    "criticalmass.centrifuge.idle": "Waiting for Uranium Concentrate",
    "criticalmass.centrifuge.slow": "Too slow: needs %s RPM",
    "criticalmass.centrifuge.full": "Output full",
    "criticalmass.centrifuge.running": "Enriching",
    "criticalmass.centrifuge.contents": "Concentrate %s, enriched %s, depleted %s",
    "criticalmass.centrifuge.progress": "Batch %s%% done",
    "criticalmass.goggles.pressurizer": "Pressurizer",
    "criticalmass.goggles.pressurizer_pressure": "Primary pressure: %s bar (set to %s)",
    "criticalmass.goggles.pressurizer_boiling": "Coolant boils at %s \u00b0C",
    "criticalmass.pressurizer.setpoint": "Pressure setpoint",
    "criticalmass.pressurizer.no_vessel": "Not touching a reactor vessel",
    "criticalmass.pressurizer.off": "Heaters off: redstone signal",
    "criticalmass.pressurizer.heating": "Heaters on: raising pressure",
    "criticalmass.pressurizer.holding": "Holding pressure",
    "criticalmass.pressurizer.no_heat": "Pressure falling: heaters need a lit Blaze Burner underneath",
    "criticalmass.alarm.pressure": "Vessel coolant boiling: pressure too low for its temperature",
    "item.criticalmass.plant_linker": "Plant Linker",
    "criticalmass.linker.tooltip": "Use on a machine to remember it, then on a control panel to link it",
    "criticalmass.linker.tooltip_stored": "Remembers the block at %s %s %s (sneak-use in the air to clear)",
    "criticalmass.linker.stored": "Remembered %s at %s %s %s",
    "criticalmass.linker.bound": "Linked to %s at %s %s %s",
    "criticalmass.linker.refused": "This panel cannot use the remembered block",
    "criticalmass.linker.nothing_stored": "Use the linker on a machine first",
    "criticalmass.linker.cleared": "Linker cleared",
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
    linker_item()
    uranium_items()
    screen_blocks.write_all(write, ASSETS, DATA, OUT)
    entries.update(screen_blocks.LANG)
    pickaxe(list(BLOCKS) + ["plant_screen", "reinforced_pipe", "reinforced_valve", "cooling_tower_packing"])
    lang(entries)
    print("plant blocks:", ", ".join(BLOCKS))


if __name__ == "__main__":
    main()

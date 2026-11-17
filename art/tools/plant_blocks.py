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



PILLAR = {"y": {}, "x": {"x": 90, "y": 90}, "z": {"x": 90}}
WINDOW_DARK = (0x17, 0x19, 0x1c)
GLOWS = {1: {(0x17, 0x19, 0x1c): (14, 44, 84), (0x1e, 0x21, 0x26): (18, 54, 98), (0x23, 0x26, 0x2b): (16, 48, 90), (0xd2, 0xc7, 0xb4): (196, 222, 240), (0x8f, 0x82, 0x73): (112, 150, 190)},
         2: {(0x17, 0x19, 0x1c): (26, 96, 168), (0x1e, 0x21, 0x26): (34, 112, 186), (0x23, 0x26, 0x2b): (30, 104, 176), (0xd2, 0xc7, 0xb4): (232, 246, 255), (0x8f, 0x82, 0x73): (150, 204, 240)},
         3: {(0x17, 0x19, 0x1c): (126, 34, 14), (0x1e, 0x21, 0x26): (146, 44, 16), (0x23, 0x26, 0x2b): (136, 38, 14), (0xd2, 0xc7, 0xb4): (255, 226, 160), (0x8f, 0x82, 0x73): (244, 150, 70)}}


ITEM_OUT = os.path.join(os.path.dirname(OUT), "item")
SIDES = {"north": "a", "south": "a", "east": "a", "west": "a", "up": "b", "down": "c"}
CLEAR = (0, 0, 0, 0)




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


def simple_state(name):
    return {"variants": {"": {"model": M + "block/" + name}}}


BLOCKS = {
    "centrifuge": {"lang": "Centrifuge"},
    "pressurizer": {"lang": "Pressurizer"},
}

LANG = {
    "criticalmass.reactor.status.blocked_flow": "Start blocked: get coolant flowing through the vessel first",
    "criticalmass.reactor.status.blocked_coolant": "Start blocked: fill the vessel at least half full of coolant",
    "criticalmass.circuit.tank_end": "Stopped: a Fluid Tank in the loop needs a second pipe to carry on from",
    "block.criticalmass.reinforced_pipe": "Reinforced Pipe",
    "block.criticalmass.radiator_panel": "Radiator",
    "criticalmass.radiator.covered": "All four sides covered",
    "block.criticalmass.reinforced_valve": "Reinforced Valve",
    "criticalmass.valve.open": "Open",
    "criticalmass.valve.closed": "Closed: no coolant passes",
    "item.criticalmass.enriched_uranium": "Enriched Uranium",
    "item.criticalmass.depleted_uranium": "Depleted Uranium",
    "criticalmass.centrifuge.idle": "Waiting for Uranium Concentrate",
    "criticalmass.centrifuge.slow": "Too slow: needs %s RPM",
    "criticalmass.centrifuge.full": "Output full",
    "criticalmass.centrifuge.running": "Enriching",
    "criticalmass.pressurizer.setpoint": "Pressure setpoint",
    "criticalmass.pressurizer.no_vessel": "Not touching a reactor vessel",
    "criticalmass.pressurizer.off": "Heaters off: redstone signal",
    "criticalmass.pressurizer.heating": "Heaters on: raising pressure",
    "criticalmass.pressurizer.holding": "Holding pressure",
    "criticalmass.pressurizer.no_heat": "Pressure falling: heaters need a lit Blaze Burner underneath",
    "criticalmass.alarm.pressure": "Vessel coolant boiling: pressure too low for its temperature",
    "item.criticalmass.plant_linker": "Plant Linker",
    "criticalmass.linker.tooltip": "Use on a machine to remember it, then on a Plant Screen to link it",
    "criticalmass.linker.tooltip_stored": "Remembers the block at %s %s %s (sneak-use in the air to clear)",
    "criticalmass.linker.stored": "Remembered %s at %s %s %s",
    "criticalmass.linker.bound": "Linked to %s at %s %s %s",
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
        entries["block.criticalmass." + name] = d["lang"]
        if not d.get("state"):
            continue
        for model_name, model in (d["models"]() if d.get("models") else {name: d["model"](name)}).items():
            write(os.path.join(ASSETS, "models", "block", model_name + ".json"), model)
        write(os.path.join(ASSETS, "blockstates", name + ".json"), d["state"](name))
        write(os.path.join(ASSETS, "models", "item", name + ".json"), {"parent": M + "block/" + d.get("item", name)})
        write(os.path.join(DATA, "criticalmass", "loot_table", "blocks", name + ".json"), loot(name))
    uranium_items()
    screen_blocks.write_all(write, ASSETS, DATA, OUT)
    entries.update(screen_blocks.LANG)
    pickaxe(list(BLOCKS) + ["plant_screen", "reinforced_pipe", "reinforced_valve", "cooling_tower_packing"])
    lang(entries)
    print("plant blocks:", ", ".join(BLOCKS))


if __name__ == "__main__":
    main()

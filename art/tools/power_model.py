import os
import sys

from PIL import Image

import bbmodel

M = "criticalmass:block/"
BLOCK = os.path.join(bbmodel.ASSETS, "models", "block")
STATES = os.path.join(bbmodel.ASSETS, "blockstates")
ITEMS = os.path.join(bbmodel.ASSETS, "models", "item")
FACING = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}}
POINTING = {"up": {}, "down": {"x": 180}, "north": {"x": 90}, "east": {"x": 90, "y": 90}, "south": {"x": 90, "y": 180}, "west": {"x": 90, "y": 270}}
GLOW = {"block_light": 15, "sky_light": 15, "ambient_occlusion": False}


def item(name, parent):
    bbmodel.write(os.path.join(ITEMS, name + ".json"), {"parent": M + parent})


def flipped(element):
    out = dict(element)
    out["from"] = [element["from"][0], element["from"][1], bbmodel.tidy(16 - element["to"][2])]
    out["to"] = [element["to"][0], element["to"][1], bbmodel.tidy(16 - element["from"][2])]
    out["name"] = element["name"].replace("north", "south")
    return out


def lit(elements):
    return [dict(e, shade=False, neoforge_data=dict(GLOW)) if e["name"].endswith("_lit") else e for e in elements]


def insulator():
    parts, textures = bbmodel.load("insulator")
    bbmodel.write(os.path.join(BLOCK, "insulator.json"), bbmodel.model(parts["body"], textures, textures["0"]))
    item("insulator", "insulator")
    bbmodel.write(os.path.join(STATES, "insulator.json"), {"variants": {"facing=" + side: dict({"model": M + "insulator"}, **turn) for side, turn in POINTING.items()}})
    bbmodel.write(os.path.join(ITEMS, "power_wire.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": "criticalmass:item/power_wire"}})


def generator():
    parts, textures = bbmodel.load("generator")
    bbmodel.write(os.path.join(BLOCK, "generator.json"), bbmodel.model(parts["body"], textures, textures["1"]))
    item("generator", "generator")
    bbmodel.write(os.path.join(STATES, "generator.json"), {"variants": {"axis=y": {"model": M + "generator"}, "axis=z": {"model": M + "generator", "x": 90},
                                                                    "axis=x": {"model": M + "generator", "x": 90, "y": 90}}})


def breaker():
    parts, textures = bbmodel.load("circuit_breaker")
    particle = textures["1"]
    bbmodel.write(os.path.join(BLOCK, "circuit_breaker.json"), bbmodel.model(parts["body"] + lit(parts["open"]), textures, particle))
    bbmodel.write(os.path.join(BLOCK, "circuit_breaker_closed.json"), bbmodel.model(parts["body"] + lit(parts["closed"]), textures, particle))
    item("circuit_breaker", "circuit_breaker")
    variants = {}
    for facing, turn in FACING.items():
        variants["closed=false,facing=" + facing] = dict({"model": M + "circuit_breaker"}, **turn)
        variants["closed=true,facing=" + facing] = dict({"model": M + "circuit_breaker_closed"}, **turn)
    bbmodel.write(os.path.join(STATES, "circuit_breaker.json"), {"variants": variants})


def grid():
    parts, textures = bbmodel.load("grid_connection")
    bbmodel.write(os.path.join(BLOCK, "grid_connection.json"), bbmodel.model(parts["body"], textures, textures["1"]))
    item("grid_connection", "grid_connection")
    bbmodel.write(os.path.join(STATES, "grid_connection.json"), {"variants": {"": {"model": M + "grid_connection"}}})


def motor():
    parts, textures = bbmodel.load("electric_motor")
    bbmodel.write(os.path.join(BLOCK, "electric_motor.json"), bbmodel.model(parts["body"], textures, textures["0"]))
    item("electric_motor", "electric_motor")
    turns = dict(FACING, up={"x": 270}, down={"x": 90})
    bbmodel.write(os.path.join(STATES, "electric_motor.json"), {"variants": {"facing=" + side: dict({"model": M + "electric_motor"}, **turn) for side, turn in turns.items()}})


def bus():
    parts, textures = bbmodel.load("bus_bar")
    bbmodel.write(os.path.join(BLOCK, "bus_bar.json"), bbmodel.model(parts["body"], textures, textures["0"]))
    item("bus_bar", "bus_bar")
    bbmodel.write(os.path.join(STATES, "bus_bar.json"), {"variants": {"axis=y": {"model": M + "bus_bar"}, "axis=z": {"model": M + "bus_bar", "x": 90},
                                                                  "axis=x": {"model": M + "bus_bar", "x": 90, "y": 90}}})


def transformer():
    parts, textures = bbmodel.load("transformer")
    bbmodel.write(os.path.join(BLOCK, "transformer.json"), bbmodel.model(parts["body"], textures, textures["0"]))
    item("transformer", "transformer")
    bbmodel.write(os.path.join(STATES, "transformer.json"), {"variants": {"facing=" + side: dict({"model": M + "transformer"}, **turn) for side, turn in FACING.items()}})
    for name in ("power_cable", "transmission_line"):
        bbmodel.write(os.path.join(ITEMS, name + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": "criticalmass:item/" + name}})


def switches():
    for name in ("disconnect_switch", "earthing_switch"):
        parts, textures = bbmodel.load(name)
        bbmodel.write(os.path.join(BLOCK, name + ".json"), bbmodel.model(parts["body"] + parts["open"], textures, textures["0"]))
        bbmodel.write(os.path.join(BLOCK, name + "_closed.json"), bbmodel.model(parts["body"] + parts["closed"], textures, textures["0"]))
        item(name, name + "_closed")
        variants = {}
        for facing, turn in FACING.items():
            variants["closed=false,facing=" + facing] = dict({"model": M + name}, **turn)
            variants["closed=true,facing=" + facing] = dict({"model": M + name + "_closed"}, **turn)
        bbmodel.write(os.path.join(STATES, name + ".json"), {"variants": variants})


def emergency():
    parts, textures = bbmodel.load("station_battery")
    bbmodel.write(os.path.join(BLOCK, "station_battery.json"), bbmodel.model(parts["body"], textures, textures["1"]))
    item("station_battery", "station_battery")
    bbmodel.write(os.path.join(STATES, "station_battery.json"), {"variants": {"facing=" + side: dict({"model": M + "station_battery"}, **turn) for side, turn in FACING.items()}})
    parts, textures = bbmodel.load("diesel_engine")
    bbmodel.write(os.path.join(BLOCK, "diesel_engine.json"), bbmodel.model(parts["body"], textures, textures["0"]))
    item("diesel_engine", "diesel_engine")
    turns = dict(FACING, up={"x": 270}, down={"x": 90})
    bbmodel.write(os.path.join(STATES, "diesel_engine.json"), {"variants": {"facing=" + side: dict({"model": M + "diesel_engine"}, **turn) for side, turn in turns.items()}})
    parts, textures = bbmodel.load("transfer_switch")
    variants = {}
    for position in ("normal", "standby", "off"):
        name = "transfer_switch_" + position
        bbmodel.write(os.path.join(BLOCK, name + ".json"), bbmodel.model(parts["body"] + lit(parts[position]), textures, textures["1"]))
        for facing, turn in FACING.items():
            variants["facing=%s,position=%s" % (facing, position)] = dict({"model": M + name}, **turn)
    item("transfer_switch", "transfer_switch_normal")
    bbmodel.write(os.path.join(STATES, "transfer_switch.json"), {"variants": variants})


def arrester():
    parts, textures = bbmodel.load("surge_arrester")
    bbmodel.write(os.path.join(BLOCK, "surge_arrester.json"), bbmodel.model(parts["body"], textures, textures["0"]))
    item("surge_arrester", "surge_arrester")
    bbmodel.write(os.path.join(STATES, "surge_arrester.json"), {"variants": {"": {"model": M + "surge_arrester"}}})


def main():
    insulator()
    arrester()
    generator()
    breaker()
    grid()
    motor()
    bus()
    transformer()
    switches()
    emergency()
    print("exported the insulator, wires, generator, breaker, grid connection, motor, bus bar and transformer")
    if len(sys.argv) > 1:
        shots = []
        for ref, views in (("insulator", ((215, 25),)), ("generator", ((215, 25),)), ("circuit_breaker", ((35, 20),)), ("circuit_breaker_closed", ((35, 20),)), ("grid_connection", ((215, 25),)), ("electric_motor", ((215, 25),)), ("electric_motor", ((35, 25),)), ("bus_bar", ((215, 25),)), ("transformer", ((215, 25),)), ("transformer", ((35, 25),)), ("disconnect_switch", ((215, 25),)), ("disconnect_switch_closed", ((215, 25),)), ("earthing_switch", ((215, 25),)), ("earthing_switch_closed", ((215, 25),)), ("station_battery", ((35, 20),)), ("diesel_engine", ((215, 25),)), ("diesel_engine", ((35, 25),)), ("transfer_switch_normal", ((35, 20),)),
                       ("transfer_switch_standby", ((35, 20),)), ("surge_arrester", ((215, 25),))):
            path = sys.argv[1].replace(".png", "_" + ref + ".png")
            bbmodel.preview(M + ref, path, views=views, size=260)
            with Image.open(path) as shot:
                shots.append(shot.copy())
            os.remove(path)
        sheet = Image.new("RGBA", (sum(s.width for s in shots), shots[0].height))
        x = 0
        for shot in shots:
            sheet.paste(shot, (x, 0))
            x += shot.width
        sheet.save(sys.argv[1])


if __name__ == "__main__":
    main()

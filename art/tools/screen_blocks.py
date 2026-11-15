import glob
import io
import json
import os
import sys
import zipfile

from PIL import Image

from handmade import OUT, rect

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ORIGINALS = os.path.join(ROOT, "art", "originals")
M = "criticalmass:"
BOARD = "create:block/display_board/"
ROTATIONS = {"north": 0, "east": 90, "south": 180, "west": 270}
GLASS = (10, 20, 14, 255)
FRAME = {"side": "flap_display_side", "top": "flap_display_top", "inside": "flap_display_inside", "back": "dark_metal_block"}
STEEL_FROM = "steel_block"


def front():
    img = Image.new("RGBA", (16, 16), GLASS)
    for y in range(1, 16, 2):
        rect(img, 0, y, 15, y, (12, 26, 17))
    return img


def properties():
    out = {}
    with open(os.path.join(ROOT, "gradle.properties"), encoding="utf8") as f:
        for line in f:
            if "=" in line and not line.startswith("#"):
                key, value = line.strip().split("=", 1)
                out[key] = value
    return out


def create_jar():
    p = properties()
    name = "create-%s-%s-slim.jar" % (p["minecraft_version"], p["create_version"])
    found = glob.glob(os.path.join(os.path.expanduser("~"), ".gradle", "caches", "modules-2", "files-2.1", "com.simibubi.create", "*", p["create_version"], "*", name))
    if not found:
        raise SystemExit("Create's jar is not in the Gradle cache: build the project once, then run this again")
    return found[0]


def brightness(c):
    return (c[0] + c[1] + c[2]) / 3.0


def steel_ramp():
    img = Image.open(os.path.join(OUT, STEEL_FROM + ".png")).convert("RGBA")
    return sorted({img.getpixel((x, y))[:3] for y in range(img.height) for x in range(img.width)}, key=brightness)


def recolour(sources):
    ramp = steel_ramp()
    seen = [brightness(img.getpixel((x, y))) for img in sources.values() for y in range(img.height) for x in range(img.width) if img.getpixel((x, y))[3] > 0]
    low, high = min(seen), max(seen)
    out = {}
    for name, img in sources.items():
        steel = Image.new("RGBA", img.size, (0, 0, 0, 0))
        for y in range(img.height):
            for x in range(img.width):
                c = img.getpixel((x, y))
                if c[3] == 0:
                    continue
                at = (brightness(c) - low) / (high - low) * (len(ramp) - 1)
                a, b = ramp[int(at)], ramp[min(len(ramp) - 1, int(at) + 1)]
                t = at - int(at)
                steel.putpixel((x, y), tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (c[3],))
        out[name] = steel
    return out


def frame(out, fresh):
    paths = {name: os.path.join(ORIGINALS, "plant_screen_" + name + ".png") for name in FRAME}
    if fresh or not all(os.path.exists(p) for p in paths.values()):
        with zipfile.ZipFile(create_jar()) as jar:
            sources = {name: Image.open(io.BytesIO(jar.read("assets/create/textures/block/" + source + ".png"))).convert("RGBA") for name, source in FRAME.items()}
        for name, img in recolour(sources).items():
            if fresh or not os.path.exists(paths[name]):
                img.save(paths[name])
                print("recoloured", os.path.basename(paths[name]))
    for name, path in paths.items():
        Image.open(path).save(os.path.join(out, "plant_screen_" + name + ".png"))


def write_all(write, assets, data, out, fresh=False):
    front().save(os.path.join(out, "plant_screen.png"))
    frame(out, fresh)
    textures = {"2": M + "block/plant_screen", "3": M + "block/plant_screen_back", "4": M + "block/plant_screen_side", "5": M + "block/plant_screen_top",
                "6": M + "block/plant_screen_inside", "particle": M + "block/plant_screen_back"}
    write(os.path.join(assets, "models", "block", "plant_screen.json"), {"parent": BOARD + "block", "render_type": "minecraft:cutout_mipped", "textures": textures})
    write(os.path.join(assets, "models", "item", "plant_screen.json"), {"parent": BOARD + "item", "render_type": "minecraft:cutout_mipped", "textures": textures})
    for edge in ("top", "bottom", "left", "right"):
        old = os.path.join(assets, "models", "block", "plant_screen_" + edge + ".json")
        if os.path.exists(old):
            os.remove(old)
    variants = {"facing=" + d: ({"model": M + "block/plant_screen", "y": y} if y else {"model": M + "block/plant_screen"}) for d, y in ROTATIONS.items()}
    write(os.path.join(assets, "blockstates", "plant_screen.json"), {"variants": variants})
    write(os.path.join(data, "criticalmass", "loot_table", "blocks", "plant_screen.json"), {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{
        "type": "minecraft:item", "name": M + "plant_screen"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})


LANG = {
    "block.criticalmass.plant_screen": "Plant Screen",
    "criticalmass.screen.unpowered": "No power: mesh a turning cogwheel with any screen of the wall",
    "criticalmass.screen.unlinked": "Not linked: showing Display Link text",
    "criticalmass.screen.linked": "Showing the reactor at %s %s %s",
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


def main():
    res = os.path.join(ROOT, "src", "main", "resources")

    def write(path, obj):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", newline="\n", encoding="utf8") as f:
            json.dump(obj, f, indent=1)
            f.write("\n")
    write_all(write, os.path.join(res, "assets", "criticalmass"), os.path.join(res, "data"), OUT, "fresh" in sys.argv[1:])
    print("plant screen: model, blockstate, item and textures")


if __name__ == "__main__":
    main()

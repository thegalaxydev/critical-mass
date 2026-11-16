import glob
import json
import os
import sys
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "criticalmass")
SOURCE = "assets/loconautics/"
DESK = "reactor_alloy_dashboard"
SHAPES = ("single", "start", "middle", "end")
PARTS = ("back", "body", "big_body", "slim_body", "plate", "slim_plate", "face", "button_cap", "button_big_cap", "lever_handle", "lever_knob", "lever_ball_base",
         "lever_ball_handle", "lever_ball_knob", "switch_paddle", "needle_default", "needle_classic", "needle_vertical", "needle_linear", "bulb", "lever_bulb", "switch_bulb",
         "glass_body", "glass_slim")
SHEETS = ("gauge_atlas", "button_atlas", "big_button_atlas", "lever_atlas", "switch_atlas", "body_atlas", "big_body_atlas", "slim_body_atlas", "plate_atlas", "slim_plate_atlas", "glass")
ITEMS = {"gauge_button": "dashboard_button", "gauge_lever": "dashboard_lever", "gauge_switch": "dashboard_switch", "locomotive_gauge": "dashboard_gauge"}
FRESH = "fresh" in sys.argv[1:]


def retarget(model):
    textures = model.get("textures", {})
    for key, value in textures.items():
        if value.startswith("loconautics:block/controls/"):
            textures[key] = "criticalmass:block/dashboard_part/" + value.rsplit("/", 1)[1]
        elif value.startswith("loconautics:block/dashboard/"):
            textures[key] = "criticalmass:block/" + DESK
    return model


def put(path, data):
    if os.path.exists(path) and not FRESH:
        return
    os.makedirs(os.path.dirname(path), exist_ok=True)
    if isinstance(data, bytes):
        with open(path, "wb") as f:
            f.write(data)
    else:
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            json.dump(data, f, indent=1)
            f.write("\n")
    print(os.path.relpath(path, ROOT).replace("\\", "/"))


def main():
    jars = sorted(glob.glob(os.path.join(ROOT, "libs", "loconautics-*.jar")))
    if not jars:
        raise SystemExit("put the Create: Loconautics jar in libs/ first")
    with zipfile.ZipFile(jars[-1]) as jar:
        for shape in SHAPES:
            put(os.path.join(ASSETS, "models", "block", DESK, shape + ".json"), retarget(json.loads(jar.read(SOURCE + "models/block/dashboard/gray/%s.json" % shape))))
        for part in PARTS:
            put(os.path.join(ASSETS, "models", "block", "dashboard_part", part + ".json"), retarget(json.loads(jar.read(SOURCE + "models/block/controls/gauge/%s.json" % part))))
        for sheet in SHEETS:
            put(os.path.join(ASSETS, "textures", "block", "dashboard_part", sheet + ".png"), jar.read(SOURCE + "textures/block/controls/%s.png" % sheet))
        put(os.path.join(ASSETS, "textures", "gui", "dashboard_part.png"), jar.read(SOURCE + "textures/gui/selector/panel.png"))
        for theirs, ours in ITEMS.items():
            put(os.path.join(ASSETS, "models", "item", ours + ".json"), retarget(json.loads(jar.read(SOURCE + "models/item/%s.json" % theirs))))


if __name__ == "__main__":
    main()

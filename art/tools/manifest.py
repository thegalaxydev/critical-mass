import datetime
import glob
import json
import os
import re

from PIL import Image

MOD = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
RES = os.path.join(MOD, "src", "main", "resources", "assets", "criticalmass")
DOCS = os.path.join(MOD, "docs", "art")
NS = "criticalmass:"
REL = "src/main/resources/assets/criticalmass/"

AUTHOR = "author"
SCRIPT = "script"
GENERATED = "generated"
LOCONAUTICS = "Create: Loconautics, with permission"
UNRECORDED = "not recorded"

ART = {
    "reactor_vessel_casing": AUTHOR, "shielding_concrete": AUTHOR, "shielded_glass": AUTHOR, "steel_block": AUTHOR, "reactor_alloy_block": AUTHOR,
    "open_waste_canister": AUTHOR, "sealed_waste_canister": AUTHOR, "incomplete_waste_canister": AUTHOR, "reinforced_valve": AUTHOR,
    "circulation_pump": AUTHOR, "radiator_panel": AUTHOR, "cooling_tower_packing": AUTHOR,
    "steam_turbine": "author's model, script texture", "steam_turbine_structure": "author's model, script texture",
    "relief_valve": "script, approved",
    "reactor_controller": SCRIPT, "reactor_signal_interface": SCRIPT, "primary_coolant_port": SCRIPT, "shielded_hatch": SCRIPT, "reactor_service_port": SCRIPT,
    "heat_exchanger": SCRIPT, "condenser": SCRIPT, "boiler_heating_plate": SCRIPT, "generator": SCRIPT, "circuit_breaker": SCRIPT, "grid_connection": SCRIPT,
    "insulator": SCRIPT, "electric_motor": SCRIPT, "bus_bar": SCRIPT, "transformer": SCRIPT, "disconnect_switch": SCRIPT, "earthing_switch": SCRIPT,
    "station_battery": SCRIPT, "diesel_engine": SCRIPT, "transfer_switch": SCRIPT, "surge_arrester": SCRIPT, "burnt_equipment": SCRIPT,
    "fuel_channel": SCRIPT, "control_channel": SCRIPT, "coolant_channel": SCRIPT, "ruined_channel": SCRIPT, "spent_fuel_rack": SCRIPT, "shielded_cask": SCRIPT,
    "control_rod_drive": SCRIPT, "pressurizer": SCRIPT, "pressurizer_top": SCRIPT, "centrifuge": SCRIPT, "reactor_viewport": SCRIPT, "corium": SCRIPT,
    "plant_screen": SCRIPT, "reinforced_pipe": SCRIPT, "uranium_ore": SCRIPT, "deepslate_uranium_ore": SCRIPT,
    "reactor_alloy_dashboard": UNRECORDED, "part_panel": "none of its own", "charred_log": SCRIPT,
    "steel_ingot": AUTHOR, "steel_nugget": AUTHOR, "steel_plate": AUTHOR, "control_rod": AUTHOR, "turbine_blade": AUTHOR, "sealing_gasket": AUTHOR,
    "welding_torch": AUTHOR, "dosimeter": AUTHOR, "survey_meter": AUTHOR, "decontamination_compound": AUTHOR, "ceramic_fuel_pellet": AUTHOR,
    "reactor_alloy_ingot": AUTHOR, "reactor_alloy_sheet": AUTHOR,
    "raw_uranium": GENERATED, "uranium_concentrate": GENERATED, "fuel_cladding": GENERATED, "empty_fuel_assembly": GENERATED, "incomplete_fuel_assembly": GENERATED,
    "fuel_assembly": GENERATED, "spent_fuel_assembly": GENERATED, "damaged_fuel_assembly": GENERATED,
    "protective_hood": GENERATED, "protective_suit": GENERATED, "protective_leggings": GENERATED, "protective_boots": GENERATED,
    "enriched_uranium": SCRIPT, "depleted_uranium": SCRIPT, "plant_linker": SCRIPT, "power_wire": SCRIPT, "power_cable": SCRIPT, "transmission_line": SCRIPT,
    "dashboard_alarm": UNRECORDED, "dashboard_button": LOCONAUTICS, "dashboard_switch": LOCONAUTICS, "dashboard_lever": LOCONAUTICS, "dashboard_gauge": LOCONAUTICS,
    "primary_coolant": GENERATED, "contaminated_coolant": GENERATED, "steam": GENERATED, "fuel_oil": SCRIPT,
}
OTHER_ART = [("block/dashboard_part/alarm", UNRECORDED), ("block/dashboard_part/", LOCONAUTICS), ("gui/dashboard_part", LOCONAUTICS), ("models/armor/", GENERATED), ("particle/", SCRIPT)]
OWNERS = {"block/control_rod_cap": "control_rod_drive", "block/control_rod_shaft": "control_rod_drive"}
DIRECT = {
    "block/casing_crack_1": "reactor_vessel_casing", "block/casing_crack_2": "reactor_vessel_casing", "block/casing_crack_3": "reactor_vessel_casing",
    "block/reactor_vessel_casing_connected": "reactor_vessel_casing", "block/shielded_glass_connected": "shielded_glass",
}
NO_ITEM = {"burnt_equipment", "part_panel", "pressurizer_top", "steam_turbine_structure"}
LEGEND = ("The Art column says whose work each texture is. Only `author` is the author's own art. `script` was drawn by a script in `art/tools/` and `generated` is a cleaned generated image: "
          "both are placeholders waiting for the author's art, and neither is hand-made. `approved` marks a placeholder the author has accepted as it is. "
          "`not recorded` means the record does not say who made it. How each one was made is in `art/SOURCES.md`.")


def names(folder, ext):
    base = os.path.join(RES, folder)
    return sorted(os.path.relpath(f, base).replace(os.sep, "/")[:-len(ext)] for f in glob.glob(os.path.join(base, "**", "*" + ext), recursive=True))


def strings(node, key):
    if isinstance(node, dict):
        for k, v in node.items():
            if k == key and isinstance(v, str):
                yield v
            else:
                yield from strings(v, key)
    elif isinstance(node, list):
        for v in node:
            yield from strings(v, key)


def read(path):
    return json.load(open(path, encoding="utf8"))


class Use:
    def __init__(self):
        self.models = set()
        self.own = set()
        self.borrowed = set()
        self.missing = set()

    def model(self, ref):
        if not ref.startswith(NS) or ref in self.models:
            return
        path = os.path.join(RES, "models", ref[len(NS):] + ".json")
        if not os.path.exists(path):
            self.missing.add("model " + ref)
            return
        self.models.add(ref)
        node = read(path)
        for tex in node.get("textures", {}).values():
            if tex.startswith("#"):
                continue
            if tex.startswith(NS):
                self.own.add(tex[len(NS):])
                if not os.path.exists(os.path.join(RES, "textures", tex[len(NS):] + ".png")):
                    self.missing.add("texture " + tex)
            else:
                self.borrowed.add(tex if ":" in tex else "minecraft:" + tex)
        for key in ("parent", "model"):
            for inner in strings(node, key):
                self.model(inner)


def texture(name):
    path = os.path.join(RES, "textures", name + ".png")
    size = list(Image.open(path).size) if os.path.exists(path) else None
    return {"path": REL + "textures/" + name + ".png", "size": size, "animated": os.path.exists(path + ".mcmeta")}


def record(kind, key, name, use):
    return {"key": key, "registry_id": NS + key, "type": kind, "name": name, "art": ART.get(key, UNRECORDED),
            "textures": [texture(t) for t in sorted(use.own)], "borrowed_textures": sorted(use.borrowed),
            "models": sorted(REL + "models/" + m[len(NS):] + ".json" for m in use.models), "missing": sorted(use.missing)}


def other_art(name):
    for prefix, label in OTHER_ART:
        if name.startswith(prefix):
            return label
    return UNRECORDED


def versions():
    out = {}
    for line in open(os.path.join(MOD, "gradle.properties"), encoding="utf8"):
        m = re.match(r"(\w+)_version=(.+)", line.strip())
        if m:
            out[m.group(1)] = m.group(2)
    return out


def build():
    lang = read(os.path.join(RES, "lang", "en_us.json"))
    states = names("blockstates", ".json")
    records, taken_models, taken_textures, unnamed = [], set(), set(), []
    blocks = {}
    for key in states:
        use = Use()
        for ref in set(strings(read(os.path.join(RES, "blockstates", key + ".json")), "model")):
            use.model(ref)
        if key not in NO_ITEM:
            use.model(NS + "item/" + key)
        blocks[key] = use
    for name in names("models/block", ".json"):
        ref = NS + "block/" + name
        if any(ref in use.models for use in blocks.values()):
            continue
        owner = OWNERS.get("block/" + name) or max((k for k in states if name == k or name.startswith(k + "_") or name.startswith(k + "/")), key=len, default=None)
        if owner:
            blocks[owner].model(ref)
    for name, owner in DIRECT.items():
        blocks[owner].own.add(name)
    for key in states:
        use = blocks[key]
        title = lang.get("block.criticalmass." + key)
        if not title:
            unnamed.append(NS + key)
        rec = record("block", key, title, use)
        rec["blockstate"] = REL + "blockstates/" + key + ".json"
        rec["item"] = key not in NO_ITEM
        records.append(rec)
        taken_models |= use.models
        taken_textures |= use.own
    for key in names("models/item", ".json"):
        if key in states:
            continue
        use = Use()
        use.model(NS + "item/" + key)
        title = lang.get("item.criticalmass." + key) or lang.get("block.criticalmass." + key)
        if not title:
            unnamed.append(NS + key)
        records.append(record("item", key, title, use))
        taken_models |= use.models
        taken_textures |= use.own
    for key in sorted({n.split("/")[1].rsplit("_", 1)[0] for n in names("textures", ".png") if n.startswith("fluid/")}):
        use = Use()
        use.own = {"fluid/%s_still" % key, "fluid/%s_flow" % key}
        title = lang.get("fluid.criticalmass." + key) or lang.get("fluid_type.criticalmass." + key)
        records.append(record("fluid", key, title, use))
        taken_textures |= use.own
    parts = Use()
    for name in names("models", ".json"):
        if NS + name not in taken_models:
            parts.model(NS + name)
    others = []
    for name in names("textures", ".png"):
        if name in taken_textures:
            continue
        used = name in parts.own or name.startswith(("gui/", "models/armor/", "particle/")) or name == "block/dashboard_part/alarm_atlas_lit"
        others.append({**texture(name), "art": other_art(name), "used": used})
    sounds = []
    for key, node in read(os.path.join(RES, "sounds.json")).items():
        files = [s if isinstance(s, str) else s.get("name") for s in node.get("sounds", [])]
        sounds.append({"key": key, "files": [REL + "sounds/" + f.split(":")[-1] + ".ogg" for f in files], "subtitle": lang.get(node.get("subtitle", ""), ""),
                       "missing": [f for f in files if not os.path.exists(os.path.join(RES, "sounds", f.split(":")[-1] + ".ogg"))], "art": GENERATED if key == "nuclear_blast" else SCRIPT})
    problems = [r["registry_id"] + ": " + m for r in records for m in r["missing"]] + ["missing " + m for m in sorted(parts.missing)]
    problems += ["no name in en_us.json: " + u for u in unnamed] + ["sound file missing: " + f for s in sounds for f in s["missing"]]
    problems += ["texture nothing uses: " + o["path"].split("/textures/")[-1] for o in others if not o["used"]]
    problems += ["no art record: " + r["registry_id"] for r in records if r["key"] not in ART]
    v = versions()
    manifest = {"generated": datetime.date.today().isoformat(), "minecraft": v.get("minecraft"), "neoforge": v.get("neo"), "create": v.get("create"),
                "aeronautics": v.get("aeronautics"), "sable": v.get("sable"), "flywheel": v.get("flywheel"), "loconautics": v.get("loconautics"),
                "records": records, "other_textures": others, "part_models": sorted(REL + "models/" + m[len(NS):] + ".json" for m in parts.models), "sounds": sounds,
                "problems": problems}
    json.dump(manifest, open(os.path.join(DOCS, "ASSET_MANIFEST.json"), "w", encoding="utf8", newline="\n"), indent=1)
    write_md(manifest)
    return manifest


def sized(t):
    return "`%s`%s" % (t["path"].split("/textures/")[-1], " " + "x".join(map(str, t["size"])) if t["size"] else " missing")


def write_md(manifest):
    out = ["# Asset manifest", "",
           "Made %s by `art/tools/manifest.py` from the blockstates, models, textures, sounds and names in `src/main/resources/assets/criticalmass`. The same list with every model path is in `ASSET_MANIFEST.json`." % manifest["generated"], "",
           "Minecraft %s, NeoForge %s, Create %s, Aeronautics %s, Sable %s, Loconautics %s." % tuple(manifest[k] for k in ("minecraft", "neoforge", "create", "aeronautics", "sable", "loconautics")), "", LEGEND, ""]
    for kind, title in (("block", "Blocks"), ("item", "Items"), ("fluid", "Fluids")):
        recs = [r for r in manifest["records"] if r["type"] == kind]
        out += ["## %s (%d)" % (title, len(recs)), "", "| Registry ID | Name | Art | Textures | Other mods' textures | Models |", "|---|---|---|---|---|---|"]
        for r in recs:
            name = (r["name"] or "") + (" (no item)" if r.get("item") is False else "")
            out.append("| `%s` | %s | %s | %s | %s | %d |" % (r["registry_id"], name, r["art"], "<br>".join(sized(t) for t in r["textures"]) or "none",
                                                              "<br>".join("`%s`" % b for b in r["borrowed_textures"]), len(r["models"])))
        out.append("")
    out += ["## Other textures (%d)" % len(manifest["other_textures"]), "", "Dashboard part sheets, the dashboard screen, the worn suit layers and effect sprites.", "", "| Texture | Art |", "|---|---|"]
    out += ["| %s | %s |" % (sized(t), t["art"]) for t in manifest["other_textures"]]
    out += ["", "## Sounds (%d)" % len(manifest["sounds"]), "", "All are synthesised by `art/tools/sfx.py`, except the alarm, which is `art/tools/alarm_sound.py`, and the nuclear explosion, which is a generated sound effect.", "", "| Sound | Subtitle | Art | File |", "|---|---|---|---|"]
    out += ["| `%s%s` | %s | %s | %s |" % (NS, s["key"], s["subtitle"], s["art"], "<br>".join("`%s`" % f.split("/assets/criticalmass/")[-1] for f in s["files"])) for s in manifest["sounds"]]
    out += ["", "## Problems", ""]
    out += ["- " + p for p in manifest["problems"]] or ["None: every model and texture a blockstate, item or sound points at exists, every texture is used, and everything has a name and an art record."]
    open(os.path.join(DOCS, "ASSET_MANIFEST.md"), "w", encoding="utf8", newline="\n").write("\n".join(out) + "\n")


if __name__ == "__main__":
    m = build()
    print("records:", len(m["records"]), "other textures:", len(m["other_textures"]), "sounds:", len(m["sounds"]))
    for p in m["problems"]:
        print("problem:", p)

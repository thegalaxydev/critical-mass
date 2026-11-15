import base64
import datetime
import glob
import io
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import art
import specs
import gen
from PIL import Image

ROOT = art.ROOT
MOD = gen.MOD
RES = gen.RES
DOCS = os.path.join(MOD, "docs", "art")
CONSOLE_SRC = os.path.join(ROOT, "build")

KINETIC = {
    "circulation_pump": {
        "role": "consumer", "impact_su": "4.0 per RPM (CMStress.CIRCULATION_PUMP_IMPACT, provisional)",
        "block_class": "DirectionalAxisKineticBlock (facing = outlet, axis_along_first selects the shaft axis perpendicular to facing)",
        "shaft_faces": "both faces on the rotation axis (hasShaftTowards: face.axis == rotation axis)",
        "axis": "perpendicular to the fluid axis; 2 options per facing",
        "placement": "6 facings x 2 shaft orientations; facing defaults to looking direction, shift reverses",
        "rotating": "circulation_pump_rotor partial: Create shaft (create:block/axis) plus two brass drive hubs on the shaft faces",
        "stationary": "housing, copper pipe body, red outlet and blue inlet flanges",
        "direction": "rotor turns exactly with network speed and direction (Create SingleAxisRotatingVisual / KineticBlockEntityRenderer); 1:1 with input shaft",
        "renderer": "Flywheel visual SingleAxisRotatingVisual.of(CIRCULATION_PUMP_ROTOR); fallback BER AxialRotorRenderer when visualization is unsupported",
    },
    "steam_turbine": {
        "role": "generator (GeneratingKineticBlockEntity) running on steam energy (heat above saturated water); 16 RPM whenever it delivers capacity; admission cut by redstone governor or a disconnected shaft",
        "impact_su": "capacity = delivered heat x 4 SU per HU/t (CMThermal.SU_PER_HEAT), independent of RPM; 0 with no delivered heat",
        "block_class": "HorizontalKineticBlock (blockstate property 'facing' = the side the big end and the shaft face)",
        "shaft_faces": "the front end of the three-block structure, through a shaft pass-through part",
        "axis": "horizontal, along the facing",
        "placement": "big end faces the player; needs a clear three-block space and fills it with invisible structure blocks, as Create's large water wheel does; steam goes in through the back-centre part and out through the bottom-centre part",
        "rotating": "steam_turbine_rotor partial: the shaft and six bladed discs, seen through the open front and the side windows",
        "stationary": "three stepped octagonal casing rings with an open framed window in each side, five stator rings and the back plate",
        "direction": "rotor follows network speed and direction 1:1",
        "renderer": "Flywheel visual SingleAxisRotatingVisual.of(STEAM_TURBINE_ROTOR); fallback BER AxialRotorRenderer",
    },
    "control_rod_drive": {
        "role": "consumer", "impact_su": "1.0 per RPM (CMStress.CONTROL_ROD_DRIVE_IMPACT, provisional)",
        "block_class": "HorizontalKineticBlock (blockstate 'facing'; front = latch lever, faces the player when placed)",
        "shaft_faces": "back face only (hasShaftTowards: face == facing.getOpposite())",
        "axis": "facing axis",
        "placement": "4 horizontal facings",
        "rotating": "control_rod_drive_pinion partial: half shaft from the back face plus the pinion gear",
        "stationary": "base plate, brass housing, red latch, rack (drawn at the stored full-insertion position; rod movement is not implemented)",
        "direction": "pinion follows network speed and direction 1:1; the rack does not move because insertion control is not implemented (M4 tasks)",
        "renderer": "Flywheel OrientedRotatingVisual (UP to input side); fallback BER InputSideRotorRenderer",
    },
}

STATE_NOTES = {
    "primary_coolant_port": "mode=inlet|outlet toggled with Create's wrench (CoolantPortBlock.onWrenched); inlet = blue collar, outlet = red collar",
    "docking_service_connector": "connected=true model (green lamp, clamp jaws) exists, but nothing sets the state yet: docking mechanics are not implemented (reachable only by command or debug stick)",
    "reactor_vessel_casing": "crack=0..3 overlay models (textures/block/casing_crack_1..3, Minecraft's breaking stages 3, 6 and 9 recoloured to the author's stage 5, drawn translucent over the casing) follow integrity tracked by the controller: 75%+ intact, 50-75% stage 1, 25-50% stage 2, under 25% stage 3",
    "relief_valve": "venting=true|false; inline on pipe (faces off the vent axis), vents through its front at 8 bar until 6 bar, redstone output while venting",
    "shielded_hatch": "vanilla iron-trapdoor behaviour: open/close by redstone only; models use vanilla trapdoor templates",
    "fuel_channel": "rotated pillar (axis)", "control_channel": "rotated pillar (axis)", "coolant_channel": "rotated pillar (axis)",
}

REFS = {
    "reactor_vessel_casing": ["create:block/andesite_casing", "simulated:block/iron_gearbox"],
    "circulation_pump": ["create:block/pump", "create:block/pipes", "create:block/gearbox"],
    "steam_turbine": ["create:block/engine", "aeronautics:block/propeller_bearing", "create:block/axis"],
    "control_rod_drive": ["aeronautics:block/gyroscopic_propeller_bearing", "create:block/gearbox"],
    "docking_service_connector": ["simulated:block/dock", "simulated:block/dock_powered"],
    "relief_valve": ["aeronautics:block/steam_vent", "create:block/fluid_valve"],
    "heat_exchanger": ["create:block/fluid_tank_top", "create:block/pipes"],
    "primary_coolant_port": ["create:block/pipes"],
}

ISSUES = {
    "reactor_vessel_casing": ["No connected textures: tiled vessels show a frame on every block. Connected-texture support is a follow-up."],
    "steam_turbine": ["Takes up three blocks each way, less the four back corners. The texture is a placeholder."],
    "control_rod_drive": ["The rack is static: rod insertion control is not implemented."],
    "docking_service_connector": ["Connected state exists only as a blockstate; docking behaviour is not implemented."],
    "shielded_glass": ["Translucent; depth sorting against other translucent blocks has not been checked."],
    "crushed_uranium": ["Create already ships create:crushed_raw_uranium (enabled by uranium tags); this addon item may be redundant. Needs a design decision."],
    "survey_meter": ["Item has no behaviour yet (measurement is task tsk-073)."],
    "dosimeter": ["Item has no behaviour yet (task tsk-074)."],
    "protective_hood": ["First-person sleeves are not applicable: vanilla does not render armor on first-person arms.", "Slim (Alex) model: checked in the 1.21.1 client code, not with an Alex skin in game. PlayerRenderer bakes PLAYER_SLIM_INNER/OUTER_ARMOR for slim players, and LayerDefinitions registers them with the same layer definitions as the wide model, so the outfit geometry and texture are identical."],
    "fuel_assembly": ["No remaining-fuel overlay: fuel item data is not implemented yet (tsk-070)."],
}

CONTENT_FILES = [os.path.join(CONSOLE_SRC, "p2_data.js"), os.path.join(CONSOLE_SRC, "p2b_content.js")]


def console_ids():
    m = {}
    for f in CONTENT_FILES:
        t = open(f, encoding="utf8").read()
        for mm in re.finditer(r'id: "((?:blk|itm|fld)-[a-z0-9-]+)", name: "([^"]+)",(?: ownership: "[a-z]+",)? registryId: "([^"]+)"', t):
            cid, name, reg = mm.groups()
            if reg.startswith("criticalmass:"):
                m[reg.split(":")[1]] = (cid, name)
        for mm in re.finditer(r'protectiveItem\("(itm-[a-z-]+)", "([^"]+)", "([a-z]+)"', t):
            m["protective_" + mm.group(3)] = (mm.group(1), mm.group(2))
    return m


def console_tasks():
    tasks = {}
    for f in CONTENT_FILES:
        t = open(f, encoding="utf8").read()
        for mm in re.finditer(r'T2?\("(tsk-\d+)", "ms-\d", "([^"]+)", "[A-Za-z]+", \[[^\]]*\], \[([^\]]*)\]', t):
            tid, title, links = mm.groups()
            for l in re.findall(r'"([a-z]+-[a-z0-9-]+)"', links):
                tasks.setdefault(l, []).append(tid)
    return tasks


def final_source(key):
    side = os.path.join(art.ART, "final", key + ".json")
    if os.path.exists(side):
        return json.load(open(side))
    vs = sorted(glob.glob(os.path.join(art.ART, "generated", key + "-v*.png")), key=lambda f: int(re.search(r"-v(\d+)\.png$", f).group(1)))
    return {"src": os.path.basename(vs[-1]) if vs else None}


def png_data_uri(path, size=None, upscale=None):
    im = Image.open(path).convert("RGBA")
    if size:
        im = im.resize(size, Image.LANCZOS)
    if upscale:
        im = im.resize((im.width * upscale, im.height * upscale), Image.NEAREST)
    buf = io.BytesIO()
    im.save(buf, "PNG", optimize=True)
    return "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode()


def build(qa=None, render_icons=True):
    import iso
    qa = qa or {}
    models = gen.build_models()
    ids = console_ids()
    tasks = console_tasks()
    icons_dir = os.path.join(DOCS, "icons")
    os.makedirs(icons_dir, exist_ok=True)
    records = []
    atlases_of = {}
    for name, m in models.items():
        atlases_of.setdefault(name, set()).add(m.atlas)
    for name, d in specs.BLOCK_DEFS.items():
        model_names = [n for n in models if n == name or n.startswith(name + "_")]
        if name == "shielded_hatch":
            model_names = ["shielded_hatch"]
        atlases = sorted({models[n].atlas for n in model_names})
        tex = []
        for a in atlases:
            p = os.path.join(RES, "textures", "block", a + ".png")
            size = Image.open(p).size if os.path.exists(p) else None
            src = final_source(a)
            mdl = next(models[n] for n in model_names if models[n].atlas == a)
            regions = {r.key: [r.x, r.y, r.w, r.h] for r in (mdl.share or mdl).regions.values()}
            tex.append({"path": "src/main/resources/assets/criticalmass/textures/block/%s.png" % a, "exists": os.path.exists(p), "size": size, "generated_source": src.get("src"), "uv_regions": regions})
        partials = [n for n in model_names if n.endswith(("_rotor", "_pinion"))]
        cid, cname = ids.get(name, (None, d["lang"]))
        icon_path = os.path.join(icons_dir, "block_" + name + ".png")
        if render_icons:
            refs = ["criticalmass:block/" + n for n in model_names if not n.endswith(("_rotor", "_pinion", "_powered", "_connected", "_outlet", "_arm", "_vertical"))]
            if name == "shielded_hatch":
                refs = ["criticalmass:block/shielded_hatch"]
            extra = []
            if name in ("circulation_pump", "steam_turbine"):
                extra = [("criticalmass:block/%s_rotor" % name, lambda c: (c[1], 16 - c[0], c[2]))]
            if name == "control_rod_drive":
                extra = [("criticalmass:block/control_rod_drive_pinion", lambda c: (c[0], 16 - c[2], c[1]))]
            try:
                iso.render(refs + extra, 96, 45).save(icon_path)
            except Exception as e:
                print("icon failed", name, e)
        state = d["state"][0]
        records.append({
            "key": name, "registry_id": "criticalmass:" + name, "type": "block", "console_id": cid, "name": cname,
            "textures": tex, "block_models": ["src/main/resources/assets/criticalmass/models/block/%s.json" % n for n in model_names],
            "blockstate": "src/main/resources/assets/criticalmass/blockstates/%s.json" % name,
            "item_model": "src/main/resources/assets/criticalmass/models/item/%s.json" % name,
            "inventory_icon": "item model %s (%s)" % (name, "merged housing + rotor, static" if name in ("circulation_pump", "steam_turbine") else "parent: block model"),
            "placed_appearance": "blockstate %s (%s)" % (name, state),
            "state_notes": STATE_NOTES.get(name, ""),
            "partials": partials, "kinetic": KINETIC.get(name),
            "references": REFS.get(name, ["see docs/art/REFERENCES.md"]),
            "generation": "generated" if all(t["generated_source"] for t in tex) else ("partial" if any(t["generated_source"] for t in tex) else "blockout-only"),
            "integration": "integrated" if all(t["exists"] for t in tex) else "missing-texture",
            "verification": qa.get(name, {"iso": False, "in_game": False, "moving_body": False}),
            "issues": ISSUES.get(name, []),
            "related_tasks": sorted(set(tasks.get(cid, []) + ["tsk-068"])) if cid else ["tsk-068"],
            "icon": os.path.relpath(icon_path, MOD).replace(os.sep, "/"),
        })
    for name, d in specs.ITEM_DEFS.items():
        p = os.path.join(RES, "textures", "item", name + ".png")
        cid, cname = ids.get(name, (None, d["lang"]))
        src = final_source("item__" + name)
        rec = {
            "key": name, "registry_id": "criticalmass:" + name, "type": "item", "console_id": cid, "name": cname,
            "textures": [{"path": "src/main/resources/assets/criticalmass/textures/item/%s.png" % name, "exists": os.path.exists(p), "size": Image.open(p).size if os.path.exists(p) else None, "generated_source": src.get("src")}],
            "item_model": "src/main/resources/assets/criticalmass/models/item/%s.json" % name,
            "presentation": "flat sprite, %s" % ("item/handheld" if d.get("handheld") else "item/generated"),
            "references": ["create:item/precision_mechanism", "create:item/brass_sheet"] if not name.startswith("protective") else ["create:item/goggles", "create:item/copper_diving_helmet"],
            "generation": "generated" if src.get("src") else "blockout-only",
            "integration": "integrated" if os.path.exists(p) else "missing-texture",
            "verification": qa.get(name, {"iso": False, "in_game": False}),
            "issues": ISSUES.get(name, []),
            "related_tasks": sorted(set(tasks.get(cid, []) + ["tsk-067", "tsk-068"])) if cid else ["tsk-068"],
            "icon": "src/main/resources/assets/criticalmass/textures/item/%s.png" % name,
        }
        if name.startswith("protective_"):
            layer = 2 if name == "protective_leggings" else 1
            rec["equipment"] = {"texture": "src/main/resources/assets/criticalmass/textures/models/armor/protective_layer_%d.png" % layer, "size": [64, 32],
                                "registration": "ArmorMaterial 'criticalmass:protective' (Registries.ARMOR_MATERIAL) with Layer('criticalmass:protective'); 1.21.1 humanoid armor layers"}
        records.append(rec)
    records.append({"key": "contaminated_filter", "registry_id": "TBD", "type": "item", "console_id": "itm-filter", "name": "Contaminated Filter", "textures": [],
                    "generation": "deferred", "integration": "deferred", "verification": {}, "issues": ["Deferred: depends on a filter/purification system that is not designed. No art made on purpose."],
                    "related_tasks": [], "icon": None})
    for n, lang in specs.FLUID_LANG.items():
        st = os.path.join(RES, "textures", "fluid", n + "_still.png")
        src = final_source("fluid__" + n)
        cid = {"primary_coolant": "fld-primary-coolant", "steam": "fld-steam", "contaminated_coolant": "fld-contaminated-coolant"}[n]
        records.append({"key": n, "registry_id": "criticalmass:" + n, "type": "fluid", "console_id": cid, "name": lang,
                        "textures": [{"path": "src/main/resources/assets/criticalmass/textures/fluid/%s_still.png" % n, "exists": os.path.exists(st), "size": [16, 256], "animated": "16 frames, frametime 3, interpolated"},
                                     {"path": "src/main/resources/assets/criticalmass/textures/fluid/%s_flow.png" % n, "exists": os.path.exists(st.replace("_still", "_flow")), "size": [32, 512], "animated": "16 frames scrolling, frametime 2"}],
                        "registration": "FluidType + BaseFlowingFluid source/flowing, no world block, no bucket; IClientFluidTypeExtensions still/flow textures",
                        "generation": "generated" if src.get("src") else "blockout-only", "integration": "integrated" if os.path.exists(st) else "missing-texture",
                        "verification": qa.get(n, {"in_game": False}), "issues": ["No world block or bucket by design; it is only visible inside Create tanks and pipes (animation confirmed in a tank in game). Textures are stitched into the block atlas through assets/minecraft/atlases/blocks.json (directory source fluid/)."],
                        "related_tasks": ["tsk-078", "tsk-079", "tsk-080"], "icon": "src/main/resources/assets/criticalmass/textures/fluid/%s_still.png" % n})
    records.append({"key": "water", "registry_id": "minecraft:water", "type": "fluid", "console_id": "fld-water", "name": "Water", "textures": [], "generation": "vanilla", "integration": "vanilla", "verification": {}, "issues": [], "related_tasks": [], "icon": None})
    manifest = {"generated": datetime.datetime.now().isoformat(timespec="seconds"), "minecraft": "1.21.1", "neoforge": "21.1.262", "create": "6.0.10-280", "aeronautics": "1.3.2", "sable": "2.0.6", "flywheel": "1.0.6",
                "density": "16 texels per block; items 16x16", "records": records}
    os.makedirs(DOCS, exist_ok=True)
    json.dump(manifest, open(os.path.join(DOCS, "ASSET_MANIFEST.json"), "w"), indent=1)
    write_md(manifest)
    return manifest


def yesno(v):
    return {True: "yes", False: "no"}.get(v, str(v)) if not isinstance(v, str) else v


def write_md(manifest):
    out = ["# Asset manifest", "", "Generated %s from `art/tools/manifest.py`. Machine-readable version: `ASSET_MANIFEST.json` (UV regions, model and texture paths, kinetic specs)." % manifest["generated"], "",
           "Minecraft %s, NeoForge %s, Create %s, Aeronautics %s, Sable %s. %s." % (manifest["minecraft"], manifest["neoforge"], manifest["create"], manifest["aeronautics"], manifest["sable"], manifest["density"]), ""]
    for kind, title in (("block", "Blocks"), ("item", "Items"), ("fluid", "Fluids")):
        recs = [r for r in manifest["records"] if r["type"] == kind]
        out += ["## %s (%d)" % (title, len(recs)), "", "| Registry ID | Textures | Generation | Integration | Verified | Issues |", "|---|---|---|---|---|---|"]
        for r in recs:
            tex = "<br>".join("`%s` %s" % (t["path"].split("/textures/")[-1], "x".join(map(str, t["size"])) if t.get("size") else "") for t in r.get("textures", [])) or "none"
            ver = ", ".join("%s: %s" % (k, yesno(v)) for k, v in r.get("verification", {}).items()) or "n/a"
            iss = "<br>".join(r.get("issues", [])) or ""
            out.append("| `%s` | %s | %s | %s | %s | %s |" % (r["registry_id"], tex, r["generation"], r["integration"], ver, iss.replace("|", "/")))
        out.append("")
    kin = [r for r in manifest["records"] if r.get("kinetic")]
    out += ["## Kinetic blocks", ""]
    for r in kin:
        out += ["### `%s`" % r["registry_id"], ""] + ["- **%s**: %s" % (k.replace("_", " "), v) for k, v in r["kinetic"].items()] + ["- **partials**: " + ", ".join(r["partials"]), ""]
    out += ["## Inventory icon vs placed appearance", ""]
    for r in manifest["records"]:
        if r["type"] == "block":
            out.append("- `%s`: inventory = %s; placed = %s%s" % (r["key"], r["inventory_icon"], r["placed_appearance"], ("; " + r["state_notes"]) if r.get("state_notes") else ""))
    out += ["", "## Equipment", ""]
    for r in manifest["records"]:
        if r.get("equipment"):
            out.append("- `%s`: `%s` (%s), %s" % (r["key"], r["equipment"]["texture"].split("/textures/")[-1], "x".join(map(str, r["equipment"]["size"])), r["equipment"]["registration"]))
    open(os.path.join(DOCS, "ASSET_MANIFEST.md"), "w", encoding="utf8").write(chr(10).join(out) + chr(10))


if __name__ == "__main__":
    qa = json.load(open(sys.argv[1])) if len(sys.argv) > 1 and os.path.exists(sys.argv[1]) else {}
    m = build(qa)
    print("records:", len(m["records"]))

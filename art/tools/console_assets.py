import base64
import io
import json
import os
import sys
from PIL import Image

MOD = r"D:\Minecraft\create-critical-mass-1.21.1"
DOCS = os.path.join(MOD, "docs", "art")


def uri(path, max_w=None, jpeg=False, crop_square=False, upscale=None):
    im = Image.open(path).convert("RGBA")
    if crop_square and im.height > im.width:
        im = im.crop((0, 0, im.width, im.width))
    if upscale:
        im = im.resize((im.width * upscale, im.height * upscale), Image.NEAREST)
    if max_w and im.width > max_w:
        im = im.resize((max_w, round(im.height * max_w / im.width)), Image.LANCZOS)
    buf = io.BytesIO()
    if jpeg:
        bg = Image.new("RGB", im.size, (32, 34, 38))
        bg.paste(im, mask=im.split()[3])
        bg.save(buf, "JPEG", quality=82, optimize=True)
        return "data:image/jpeg;base64," + base64.b64encode(buf.getvalue()).decode()
    im.save(buf, "PNG", optimize=True)
    return "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode()


def build(out_path, gallery):
    m = json.load(open(os.path.join(DOCS, "ASSET_MANIFEST.json"), encoding="utf8"))
    recs = []
    for r in m["records"]:
        icon = None
        if r.get("icon"):
            p = os.path.join(MOD, r["icon"])
            if os.path.exists(p):
                icon = uri(p, crop_square=(r["type"] == "fluid"))
        ver = r.get("verification") or {}
        recs.append({
            "id": r.get("console_id"), "key": r["key"], "name": r["name"], "type": r["type"], "registry": r["registry_id"], "icon": icon,
            "generation": r["generation"], "integration": r["integration"], "verification": ver,
            "textures": [t["path"] + (" (%dx%d)" % tuple(t["size"]) if t.get("size") else "") + ("" if t.get("exists", True) else " [missing]") for t in r.get("textures", [])],
            "models": r.get("block_models", []) + ([r["item_model"]] if r.get("item_model") else []),
            "blockstate": r.get("blockstate"), "inventory": r.get("inventory_icon") or r.get("presentation"), "placed": r.get("placed_appearance"),
            "state_notes": r.get("state_notes", ""), "partials": r.get("partials", []), "kinetic": r.get("kinetic"), "equipment": r.get("equipment"),
            "registration": r.get("registration"), "references": r.get("references", []), "issues": r.get("issues", []), "tasks": r.get("related_tasks", []),
        })
    gal = []
    for title, caption, path in gallery:
        if os.path.exists(path):
            gal.append({"title": title, "caption": caption, "img": uri(path, max_w=720, jpeg=True)})
    snap = {"generated": m["generated"], "versions": {k: m[k] for k in ("minecraft", "neoforge", "create", "aeronautics", "sable", "flywheel")}, "density": m["density"], "records": recs, "gallery": gal}
    json.dump(snap, open(out_path, "w", encoding="utf8"), separators=(",", ":"))
    print(out_path, len(recs), "records", len(gal), "gallery images", os.path.getsize(out_path) // 1024, "KB")


if __name__ == "__main__":
    out = sys.argv[1]
    gallery = json.load(open(sys.argv[2], encoding="utf8")) if len(sys.argv) > 2 else []
    build(out, gallery)

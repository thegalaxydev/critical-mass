import glob
import json
import os
import re
import shutil
import sys
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
ART = os.path.join(ROOT, "art")
MOD = r"D:\Minecraft\create-critical-mass-1.21.1"
REPO_ART = os.path.join(MOD, "art")
DOCS = os.path.join(MOD, "docs", "art")


def main():
    os.makedirs(os.path.join(REPO_ART, "generated"), exist_ok=True)
    os.makedirs(os.path.join(REPO_ART, "tools"), exist_ok=True)
    os.makedirs(DOCS, exist_ok=True)
    for f in glob.glob(os.path.join(HERE, "*.py")):
        shutil.copyfile(f, os.path.join(REPO_ART, "tools", os.path.basename(f)))
    shutil.copyfile(os.path.join(ART, "palette.json"), os.path.join(DOCS, "palette.json"))
    used = {}
    for side in glob.glob(os.path.join(ART, "final", "*.json")):
        key = os.path.basename(side)[:-5]
        used[key] = json.load(open(side))
    log = [json.loads(l) for l in open(os.path.join(ART, "SOURCES.jsonl"), encoding="utf8") if l.strip()]
    latest = {}
    for e in log:
        latest[(e["texture"], e["files"][0])] = e
    rows = []
    for key, info in sorted(used.items()):
        src = info.get("src")
        if not src:
            continue
        raw = os.path.join(ART, "generated", src)
        if os.path.exists(raw):
            im = Image.open(raw).convert("RGB")
            im.thumbnail((512, 512), Image.LANCZOS)
            im.save(os.path.join(REPO_ART, "generated", src.replace(".png", ".jpg")), "JPEG", quality=85)
        entry = None
        for (tex, f), e in latest.items():
            if tex == key and f.replace("\\", "/").endswith(src):
                entry = e
        rows.append((key, src, info.get("method", "Generated edit of the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)"), entry))
    out = ["# Generated art sources", "",
           "Every texture in `src/main/resources/assets/criticalmass/textures/` that came from generation is listed here.",
           "Model: `generator` via the image tool built-in `generate` tool (`image_tool.py --edit`). Raw outputs (downscaled to 512 px JPEG) are in `art/generated/`.",
           "All art is a candidate for replacement by hand-made art later.", ""]
    for key, src, method, e in rows:
        kind, _, name = key.partition("__") if "__" in key else ("block", "", key)
        target = {"item": "textures/item/%s.png", "armor": "textures/models/armor/%s.png", "fluid": "textures/fluid/%s_still.png + _flow.png", "block": "textures/block/%s.png"}[kind if kind in ("item", "armor", "fluid") else "block"] % (name or key)
        out += ["## " + (name or key), "", "- Used by: `%s`" % target, "- Raw output: `art/generated/%s`" % src.replace(".png", ".jpg"), "- Processing: " + method]
        if e:
            out += ["- Date: " + e["date"], "- Prompt:", "", "```", e["prompt"], "```"]
        out.append("")
    open(os.path.join(REPO_ART, "SOURCES.md"), "w", encoding="utf8").write("\n".join(out))
    open(os.path.join(REPO_ART, "README.md"), "w", encoding="utf8").write(
        "# Art pipeline\n\n`tools/` holds the exact scripts used to produce this mod's art (see `docs/art/STYLE.md`). "
        "They expect a working directory containing `refs/` (assets extracted from the Create and Aeronautics jars, for local study only) and write to `art/`. "
        "`generated/` keeps downscaled raw generated outputs for provenance; `SOURCES.md` lists prompts and processing.\n")
    print("published", len(rows), "sources")


if __name__ == "__main__":
    main()

import json
import os
import sys
import subprocess

here = os.path.dirname(os.path.abspath(__file__))
root = os.path.dirname(here)
out = sys.argv[1]
files = sys.argv[2:]
items = []
from PIL import Image
for f in files:
    im = Image.open(f)
    m = max(im.size)
    s = 16 if m <= 16 else 8 if m <= 32 else 4 if m <= 64 else 2 if m <= 128 else 1
    items.append([f, os.path.basename(f).replace(".png", "") + " " + "%dx%d" % im.size, None, s])
spec = os.path.join(root, "qa", "_contact.json")
json.dump({"items": items, "cols": int(os.environ.get("COLS", 4))}, open(spec, "w"))
subprocess.run([sys.executable, os.path.join(here, "sheet.py"), spec, out], check=True)

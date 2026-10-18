import sys
html, snap = sys.argv[1], sys.argv[2]
t = open(html, encoding="utf8").read()
s = open(snap, encoding="utf8").read()
assert t.count("__ASSET_SNAPSHOT__") == 1
t = t.replace("__ASSET_SNAPSHOT__", s)
open(html, "w", encoding="utf8").write(t)
print("injected", len(s) // 1024, "KB")

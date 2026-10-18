import sys

path, marker = sys.argv[1], sys.argv[2]
t = open(path, encoding="utf8").read()
i = t.index(marker)
head, seg = t[:i], t[i:]
out = []
in_str = False
esc = False
for ch in seg:
    if in_str:
        if esc:
            esc = False
            out.append(ch)
            continue
        if ch == "\\":
            esc = True
            out.append(ch)
            continue
        if ch == '"':
            in_str = False
            out.append(ch)
            continue
        if ch == "\n":
            out.append("\\n")
            continue
        out.append(ch)
    else:
        if ch == '"':
            in_str = True
        out.append(ch)
open(path, "w", encoding="utf8").write(head + "".join(out))
print("fixed")

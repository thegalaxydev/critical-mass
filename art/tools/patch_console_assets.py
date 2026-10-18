import sys
import os

B = sys.argv[1]


def patch(fn, pairs):
    p = os.path.join(B, fn)
    t = open(p, encoding="utf8").read()
    for a, b in pairs:
        if b in t and a not in t:
            continue
        assert t.count(a) == 1, (fn, a[:80], t.count(a))
        t = t.replace(a, b)
    open(p, "w", encoding="utf8").write(t)


patch("p5_views.js", [
    ('["recipes", "Recipes and processing"], ["systems", "Systems reference"]', '["recipes", "Recipes and processing"], ["assets", "Assets and art"], ["systems", "Systems reference"]'),
    ('pageHead("Section 04 · Systems reference", "Systems reference", null)', 'pageHead("Section 05 · Systems reference", "Systems reference", null)'),
    ('pageHead("Section 04 · Systems reference", "Systems reference", "Editable', 'pageHead("Section 05 · Systems reference", "Systems reference", "Editable'),
    ('pageHead("Section 05 · Aeronautics and compatibility"', 'pageHead("Section 06 · Aeronautics and compatibility"'),
    ('pageHead("Section 06 · Development tasks"', 'pageHead("Section 07 · Development tasks"'),
    ('pageHead("Section 07 · Decisions and notes"', 'pageHead("Section 08 · Decisions and notes"'),
    ('pageHead("Section 08 · Backup and settings"', 'pageHead("Section 09 · Backup and settings"'),
    ('const counts = { catalog: [allContent().length, "content records"], recipes: [state.recipes.length, "recipes and processes"],',
     'const counts = { catalog: [allContent().length, "content records"], recipes: [state.recipes.length, "recipes and processes"], assets: [ASSET_SNAPSHOT.records.filter(r => r.generation === "generated").length + "/" + ASSET_SNAPSHOT.records.filter(r => !["deferred", "vanilla"].includes(r.generation)).length, "assets generated"],'),
])

patch("p5b_catalog.js", [
    ('      sortHeader("Name", "name"), showType ? sortHeader("Type", "type") : null,', '      h("th", { scope: "col", class: "icon-col" }, h("span", { class: "visually-hidden" }, "Icon")), sortHeader("Name", "name"), showType ? sortHeader("Type", "type") : null,'),
    ('      return h("tr", {},\n        h("td", {}, h("button", { type: "button", class: "linkish", onclick: () => openRecord(r.id) }, r.name),',
     '      return h("tr", {},\n        h("td", { class: "icon-col" }, assetIcon(r.id, 32)),\n        h("td", {}, h("button", { type: "button", class: "linkish", onclick: () => openRecord(r.id) }, r.name),'),
    ('      h("div", { class: "card-top" }, h("div", {}, h("h3", {}, h("button", { type: "button", class: "linkish", onclick: () => openRecord(r.id) }, r.name)),',
     '      h("div", { class: "card-top" }, assetIcon(r.id, 48, "card-icon"), h("div", {}, h("h3", {}, h("button", { type: "button", class: "linkish", onclick: () => openRecord(r.id) }, r.name)),'),
    ('    return h("button", { type: "button", class: "link-chip", onclick: () => openInPlace(line.id), title: f ? f.K.label : "Missing record" },\n      h("span", { class: "ref-kind" }, f ? f.K.label : "Missing"),',
     '    return h("button", { type: "button", class: "link-chip", onclick: () => openInPlace(line.id), title: f ? f.K.label : "Missing record" },\n      assetFor(line.id) && assetFor(line.id).icon ? assetIcon(line.id, 16, "chip-icon") : h("span", { class: "ref-kind" }, f ? f.K.label : "Missing"),'),
])

patch("p4_editor.js", [
    ('  if (key === "tsk") {\n    const statusSel = body.querySelector("#ed-status");',
     '  if (!isNew && ["blk", "itm", "fld"].includes(key) && typeof assetSection === "function" && assetFor(id)) {\n    body.append(assetSection(id));\n  }\n  if (key === "tsk") {\n    const statusSel = body.querySelector("#ed-status");'),
    ('      h("div", {}, h("div", { class: "eyebrow" }, key === "meta" ? "Overview" : KINDS[key].label), h("h2", { id: "editor-title" }, editorTitle(key, isNew)), meta.length ? h("div", {}, meta) : null),',
     '      h("div", { class: "editor-title-row" }, !isNew && ["blk", "itm", "fld"].includes(key) && typeof assetFor === "function" && assetFor(id) ? assetIcon(id, 40) : null, h("div", {}, h("div", { class: "eyebrow" }, key === "meta" ? "Overview" : KINDS[key].label), h("h2", { id: "editor-title" }, editorTitle(key, isNew)), meta.length ? h("div", {}, meta) : null)),'),
])

patch("p1_head.html", [
    ('.scrim{display:none}', '''.asset-icon{image-rendering:pixelated;image-rendering:crisp-edges;display:inline-block;flex:none;object-fit:contain;vertical-align:middle}
.asset-icon.empty{border:1px dashed var(--line-strong);border-radius:4px;opacity:.5}
td.icon-col,th.icon-col{width:44px;padding-right:0}
.card-top .card-icon{margin-right:2px}
.chip-icon{margin:-2px 0}
.editor-title-row{display:flex;gap:12px;align-items:center}
.asset-head{display:flex;gap:16px;align-items:flex-start;margin-bottom:12px}
.asset-checks{list-style:none;margin:8px 0 4px;padding:0;font-size:.84rem;display:flex;flex-direction:column;gap:3px}
.asset-checks li{display:flex;gap:8px;align-items:center}
.asset-checks .lamp{margin-top:0}
.asset-list{margin:0;padding-left:16px}
.asset-list li{margin:1px 0;overflow-wrap:anywhere}
table.asset-table{min-width:1100px}
table.asset-table td:nth-child(7){min-width:260px}
table.asset-table td:nth-child(8){min-width:240px}
.gallery{display:grid;grid-template-columns:repeat(auto-fill,minmax(min(320px,100%),1fr));gap:14px}
.gallery figure{margin:0;background:var(--surface-2);border:1px solid var(--line);border-radius:var(--radius);overflow:hidden}
.gallery img{display:block;width:100%;height:auto;image-rendering:auto}
.gallery figcaption{padding:8px 10px;font-size:.82rem;display:flex;flex-direction:column;gap:2px;color:var(--text-muted)}
.gallery figcaption strong{color:var(--text)}
.scrim{display:none}'''),
])
print("patched console")

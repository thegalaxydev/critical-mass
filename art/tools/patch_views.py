import sys

p = sys.argv[1]
t = open(p, encoding='utf8').read()


def rep(a, b, count=1):
    global t
    assert t.count(a) == count, (t.count(a), a[:90])
    t = t.replace(a, b)


start = t.index('function resetCatalogFilters()')
end = t.index('function viewSystems()')
t = t[:start] + t[end:]

rep('''  ["overview", "Overview"], ["catalog", "Block catalog"], ["systems", "Systems reference"], ["compat", "Aeronautics and compatibility"],''',
'''  ["overview", "Overview"], ["catalog", "Content catalog"], ["recipes", "Recipes and processing"], ["systems", "Systems reference"], ["compat", "Aeronautics and compatibility"],''')

rep('''function openRecord(id) {''', '''function openInPlace(id) {
  const key = kindKey(id);
  if (!findRecord(id)) { toast("Record " + id + " was not found.", "warn"); return; }
  if (key === "sys") { navigate("systems", id); return; }
  openEditor(key, id);
}
function openRecord(id) {''')

rep('''  const counts = { catalog: [state.blocks.length, "blocks"],''', '''  const counts = { catalog: [allContent().length, "content records"], recipes: [state.recipes.length, "recipes and processes"],''')

rep('''  const incomplete = state.blocks.filter(b => blockMissing(b).length);
  const verified = state.blocks.filter(b => b.status === "Verified").length;''', '''  const incomplete = allContent().filter(x => contentMissing(x.key, x.r).length);
  const addonFluids = state.fluids.filter(isAddonFluid);
  const vanillaFluids = state.fluids.length - addonFluids.length;
  const progressGroups = [
    ["Blocks", state.blocks, () => { resetCatalogFilters(); setCatalogTab("blk"); navigate("catalog"); }],
    ["Standalone items", state.items, () => { resetCatalogFilters(); setCatalogTab("itm"); navigate("catalog"); }],
    ["Addon fluids", addonFluids, () => { resetCatalogFilters(); setCatalogTab("fld"); ui.catalog.ownership = "addon"; navigate("catalog"); }],
    ["Recipes and processes", state.recipes, () => navigate("recipes")]
  ];
  const progressTile = ([label, recs, go]) => {
    const v = recs.filter(r => r.status === "Verified").length;
    const beyond = recs.filter(r => !["Proposed", "Deferred"].includes(r.status)).length;
    return tile(label + " verified", h("span", {}, String(v), h("small", {}, " / " + recs.length)), plural(beyond, "record") + " past Proposed" + (label === "Addon fluids" && vanillaFluids ? " · vanilla water referenced, not counted" : ""), v === recs.length && recs.length ? "ok" : "", go, meter(v, recs.length, { thin: true, label: label + " verified" }));
  };''')

rep('''    tile("Blocks verified", h("span", {}, String(verified), h("small", {}, " / " + state.blocks.length)), plural(state.blocks.filter(b => b.status === "Proposed").length, "block") + " still only proposed", verified === state.blocks.length && verified ? "ok" : "", () => navigate("catalog")),''',
'''    progressGroups.map(progressTile),''')

rep('''    tile("Specs with TBD fields", String(incomplete.length), "blocks missing at least one specification field", incomplete.length ? "warn" : "ok", () => { resetCatalogFilters(); ui.catalog.missing = "__any"; navigate("catalog"); })''',
'''    tile("Specs with TBD fields", String(incomplete.length), ["blk", "itm", "fld"].map(k => incomplete.filter(x => x.key === k).length + " " + CONTENT[k].plural.toLowerCase()).join(" · "), incomplete.length ? "warn" : "ok", () => { resetCatalogFilters(); setCatalogTab("all"); ui.catalog.missing = "__any"; navigate("catalog"); })''')

old_status_start = t.index('  const statusCounts = BLOCK_STATUSES.map(')
old_status_end = t.index('  const readyInMs = ')
t = t[:old_status_start] + '''  const statusUsed = BLOCK_STATUSES.filter(st => progressGroups.some(([, recs]) => recs.some(r => r.status === st)));
  const statusPanel = panel("Implementation status by type", { eyebrow: "Catalog and recipes", id: "ov-status", actions: [h("button", { type: "button", class: "btn small", onclick: () => navigate("catalog") }, "Open catalog")] },
    h("p", { class: "small muted" }, "Implementation status of catalog records. Task checklist completion is tracked separately above."),
    h("div", { class: "stack", style: "gap:14px" }, progressGroups.map(([label, recs, go]) => {
      const counts = BLOCK_STATUSES.map(st => [st, recs.filter(r => r.status === st).length]).filter(([, n]) => n);
      const total = recs.length || 1;
      return h("div", {},
        h("div", { class: "meter-row", style: "margin-bottom:5px" }, h("button", { type: "button", class: "linkish plain small", onclick: go }, label), h("span", { class: "mono small faint" }, counts.map(([st, n]) => n + " " + st.toLowerCase()).join(" · ") || "none")),
        h("div", { class: "stackbar", role: "img", "aria-label": label + ": " + (counts.map(([st, n]) => st + " " + n).join(", ") || "no records") },
          counts.map(([st, n]) => h("span", { class: "bg-" + tone(st), style: "width:" + (n / total * 100) + "%", title: st + ": " + n }))));
    })),
    h("ul", { class: "list-plain legend", style: "margin-top:12px" }, statusUsed.map(st => h("li", {},
      h("button", { type: "button", class: "linkish plain small", onclick: () => { resetCatalogFilters(); setCatalogTab("all"); ui.catalog.status = st; navigate("catalog"); }, title: "Show catalog records with this status" },
        h("span", { class: "swatch bg-" + tone(st), "aria-hidden": "true", style: "margin-right:6px" }), st))))
  );

''' + t[old_status_end:]

rep('''pageHead("Section 03 · Systems reference", "Systems reference", null)''', '''pageHead("Section 04 · Systems reference", "Systems reference", null)''')
rep('''pageHead("Section 03 · Systems reference", "Systems reference", "Editable''', '''pageHead("Section 04 · Systems reference", "Systems reference", "Editable''')
rep('''pageHead("Section 04 · Aeronautics and compatibility"''', '''pageHead("Section 05 · Aeronautics and compatibility"''')
rep('''pageHead("Section 05 · Development tasks"''', '''pageHead("Section 06 · Development tasks"''')
rep('''pageHead("Section 06 · Decisions and notes"''', '''pageHead("Section 07 · Decisions and notes"''')
rep('''pageHead("Section 07 · Backup and settings"''', '''pageHead("Section 08 · Backup and settings"''')

rep('''  const rows = [["Blocks", "blocks"], ["Systems", "systems"],''', '''  const rows = [["Blocks", "blocks"], ["Items", "items"], ["Fluids", "fluids"], ["Recipes and processes", "recipes"], ["Systems", "systems"],''')

rep('''      r.ok ? importSummaryTable(r.data) : null,''', '''      r.ok && r.migration ? h("div", { class: "banner info" }, h("div", { class: "grow" }, h("strong", {}, "Older export will be upgraded"), h("span", {}, migrationSummary(r.migration)))) : null,
      r.ok ? importSummaryTable(r.data) : null,''')

rep('''const VIEWS = { overview: viewOverview, catalog: viewCatalog, systems: viewSystems,''', '''const VIEWS = { overview: viewOverview, catalog: viewCatalog, recipes: viewRecipes, systems: viewSystems,''')

rep('''          h("p", { class: "muted" }, "Downloads one JSON file containing every block, system page, compatibility row, task, decision, the scratchpad, and preferences, with schema version " + SCHEMA_VERSION + " and stable record IDs."),''',
'''          h("p", { class: "muted" }, "Downloads one JSON file containing every block, item, fluid, recipe, system page, compatibility row, task, decision, the scratchpad, and preferences, with schema version " + SCHEMA_VERSION + " and stable record IDs. Schema 1 exports from the earlier console import through the same upgrade."),''')

rep('''          h("p", { class: "muted" }, "Replaces all project data in this browser with the original seeded specifications, tasks, and decisions. Preferences are kept. This cannot be undone, so export first."),''',
'''          h("p", { class: "muted" }, "Replaces all project data in this browser with the original seeded blocks, items, fluids, recipes, tasks, and decisions. Preferences are kept. This cannot be undone, so export first."),''')

open(p, 'w', encoding='utf8').write(t)
print("patched", p)

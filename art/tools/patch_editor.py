import sys

p = sys.argv[1]
t = open(p, encoding='utf8').read()


def rep(a, b):
    global t
    assert t.count(a) == 1, a[:90]
    t = t.replace(a, b)


rep('''const LINKABLE_KINDS = ["blk", "sys", "fail", "cmp", "aer", "dsg", "dec"];''',
'''const LINKABLE_KINDS = ["blk", "itm", "fld", "rcp", "sys", "fail", "cmp", "aer", "dsg", "dec"];
const validateRegistryId = v => !v || /^tbd$/i.test(v) || REGISTRY_RE.test(v) ? null : "Registry ID must be lowercase namespace:path (for example criticalmass:fuel_assembly) or TBD.";
const relationsSection = { title: "Recipes and processing", fields: [{ k: "__relations", label: "Recipe relationships", type: "relations", span: true }] };''')

rep('''      { k: "registryId", label: "Proposed registry ID", type: "text", mono: true, hint: "Proposal only. Format namespace:path, or TBD.", validate: v => !v || /^tbd$/i.test(v) || REGISTRY_RE.test(v) ? null : "Registry ID must be lowercase namespace:path (for example criticalmass:fuel_channel) or TBD." },''',
'''      { k: "registryId", label: "Proposed registry ID", type: "text", mono: true, hint: "Proposal only. Format namespace:path, or TBD.", validate: validateRegistryId },''')

rep('''      { k: "compat", label: "Compatibility dependencies", type: "checks", span: true, options: () => state.compat.map(c => [c.id, c.integration]) }
    ] },
    { title: "Verification", fields: [
      specArea("acceptance", "Acceptance criteria", { rows: 4, span: true, hint: "One testable criterion per line. At least two recommended." }),''',
'''      { k: "compat", label: "Compatibility dependencies", type: "checks", span: true, options: () => state.compat.map(c => [c.id, c.integration]) }
    ] },
    relationsSection,
    { title: "Verification", fields: [
      specArea("acceptance", "Acceptance criteria", { rows: 4, span: true, hint: "One testable criterion per line. At least two recommended." }),''')

rep('''  tsk: () => [
    { title: "Task", fields: [''', '''  itm: () => [
    { title: "Identity", fields: [
      { k: "name", label: "Display name", type: "text", required: true },
      { k: "registryId", label: "Proposed registry ID", type: "text", mono: true, hint: "Proposal only. Format namespace:path, or TBD.", validate: validateRegistryId },
      { k: "category", label: "Category", type: "select", options: () => ITEM_CATEGORIES },
      { k: "status", label: "Implementation status", type: "select", options: () => BLOCK_STATUSES },
      { k: "milestoneId", label: "Milestone", type: "select", options: milestoneOptions },
      { k: "applicability", label: "Stationary/mobile applicability", type: "select", options: () => APPLICABILITY }
    ] },
    { title: "Specification", fields: [
      specArea("purpose", "Purpose", { span: true }), specArea("stackSize", "Proposed stack size", { rows: 1, hint: "Proposal. Stateful items should be nonstackable." }),
      specArea("acquisition", "Acquisition or crafting process"), specArea("outputs", "Outputs or state transitions", { span: true }),
      specArea("persistentData", "Durability or persistent data", { hint: "Write N/A when not applicable." }), specArea("radiation", "Radiation/contamination behavior", { hint: "Write None when not applicable." }),
      specArea("tooltip", "Inventory and tooltip behavior"), specArea("visual", "Texture/model direction")
    ] },
    { title: "Integration", fields: [
      { k: "machines", label: "Machines or blocks that use it", type: "refs", kinds: ["blk"], span: true },
      { k: "compat", label: "Compatibility dependencies", type: "checks", span: true, options: () => state.compat.map(c => [c.id, c.integration]) }
    ] },
    relationsSection,
    { title: "Verification", fields: [
      specArea("acceptance", "Acceptance criteria", { rows: 4, span: true, hint: "One testable criterion per line." }),
      { k: "__tasks", label: "Related tasks", type: "backlinks", span: true, hint: "Links are stored on the tasks, so both sides stay consistent." },
      { k: "notes", label: "Notes", type: "textarea", rows: 3, span: true }
    ] }
  ],
  fld: () => [
    { title: "Identity", fields: [
      { k: "name", label: "Display name", type: "text", required: true },
      { k: "ownership", label: "Ownership", type: "select", options: () => OWNERSHIP, hint: "Vanilla fluids are referenced, not counted as addon content." },
      { k: "registryId", label: "Registry ID (where applicable)", type: "text", mono: true, hint: "Addon IDs are proposals. Vanilla: minecraft:...", validate: validateRegistryId },
      { k: "category", label: "Category", type: "select", options: () => FLUID_CATEGORIES },
      { k: "status", label: "Implementation status", type: "select", options: () => BLOCK_STATUSES },
      { k: "milestoneId", label: "Milestone", type: "select", options: milestoneOptions },
      { k: "applicability", label: "Stationary/mobile applicability", type: "select", options: () => APPLICABILITY }
    ] },
    { title: "Circuit", fields: [
      specArea("purpose", "Purpose and circuit", { span: true }), specArea("production", "Production and consumption", { span: true }),
      specArea("equipment", "Compatible tanks, pipes, ports, and machines", { span: true }),
      { k: "machines", label: "Addon blocks that handle it", type: "refs", kinds: ["blk"], span: true }
    ] },
    { title: "Behaviour and policy", fields: [
      specArea("thermal", "Thermal-state representation"), specArea("pressure", "Pressure-handling notes"),
      specArea("contamination", "Contamination behavior"), specArea("worldPlacement", "World-placement policy"),
      specArea("container", "Bucket/container policy"), specArea("compatibility", "Compatibility requirements"),
      { k: "compat", label: "Compatibility dependencies", type: "checks", span: true, options: () => state.compat.map(c => [c.id, c.integration]) }
    ] },
    relationsSection,
    { title: "Verification", fields: [
      specArea("acceptance", "Acceptance criteria", { rows: 4, span: true, hint: "One testable criterion per line." }),
      { k: "__tasks", label: "Related tasks", type: "backlinks", span: true },
      { k: "notes", label: "Notes", type: "textarea", rows: 3, span: true }
    ] }
  ],
  rcp: () => [
    { title: "Process", fields: [
      { k: "title", label: "Title", type: "text", required: true, span: true },
      { k: "kind", label: "Kind", type: "select", options: () => RECIPE_KINDS, hint: "Reactor operation, cooling/storage, and thermal processes are state changes, not crafting recipes." },
      { k: "status", label: "Proposal/verification status", type: "select", options: () => RECIPE_STATUSES },
      { k: "machineRef", label: "Machine (catalog record)", type: "select", options: () => [["", "None: external machine or method below"]].concat(allContent().map(x => [x.r.id, CONTENT[x.key].label + ": " + x.r.name])) },
      { k: "method", label: "Machine or processing method", type: "text", hint: "For example: Create mechanical press." },
      { k: "time", label: "Processing time", type: "text", placeholder: "TBD" },
      { k: "conditions", label: "Required conditions", type: "textarea", rows: 2, span: true }
    ] },
    { title: "Inputs and outputs", fields: [
      { k: "inputs", label: "Inputs", single: "input", type: "io", span: true },
      { k: "outputs", label: "Outputs", single: "output", type: "io", span: true },
      { k: "byproducts", label: "Byproducts", single: "byproduct", type: "io", span: true }
    ] },
    { title: "Tracking", fields: [
      { k: "__tasks", label: "Related tasks", type: "backlinks", span: true },
      { k: "notes", label: "Notes", type: "textarea", rows: 3, span: true }
    ] }
  ],
  tsk: () => [
    { title: "Task", fields: [''')

rep('''const DELETABLE = new Set(["blk", "tsk", "dec", "cmp", "aer", "fail"]);
const DUPLICABLE = new Set(["blk", "tsk", "dec"]);''', '''const DELETABLE = new Set(["blk", "itm", "fld", "rcp", "tsk", "dec", "cmp", "aer", "fail"]);
const DUPLICABLE = new Set(["blk", "itm", "fld", "rcp", "tsk", "dec"]);''')

rep('''    case "tsk": return { title: "", status: "Todo",''', '''    case "itm": return I({ id: "", name: "", registryId: "criticalmass:", category: ITEM_CATEGORIES[0], milestoneId: ms, stackSize: "", persistentData: "", radiation: "", tooltip: "", visual: "", purpose: "", acquisition: "", outputs: "", acceptance: "" });
    case "fld": return F({ id: "", name: "", ownership: "addon", registryId: "criticalmass:", category: FLUID_CATEGORIES[0], milestoneId: ms, compat: [], purpose: "", production: "", equipment: "", thermal: "", pressure: "", contamination: "", worldPlacement: "", container: "", compatibility: "", acceptance: "" });
    case "rcp": return R({ id: "", title: "", kind: "Crafting", inputs: [], outputs: [], time: "" });
    case "tsk": return { title: "", status: "Todo",''')

rep('''function buildField(f, values, ctx) {''', '''function contentOptgroups() {
  return CONTENT_KEYS.map(k => {
    const og = h("optgroup", { label: CONTENT[k].plural });
    state[CONTENT[k].coll].forEach(r => og.append(h("option", { value: r.id }, r.name)));
    return og;
  });
}
function ioControl(f, initial) {
  const rows = initial.map(x => ({ id: x.id || "", label: x.label || "", qty: x.qty || "" }));
  const list = h("div", { class: "io-rows" });
  const addBtn = h("button", { type: "button", class: "btn small" }, "Add " + f.single);
  function draw(focusIndex, focusPart) {
    list.replaceChildren();
    if (!rows.length) list.append(h("div", { class: "chip-empty" }, "None"));
    rows.forEach((row, i) => {
      const n = f.single + " " + (i + 1);
      const sel = h("select", { "aria-label": n + ": catalog record", onchange: e => { row.id = e.target.value; draw(i, "sel"); } }, h("option", { value: "" }, "External or not in catalog"), contentOptgroups());
      sel.value = row.id;
      const lab = h("input", { type: "text", value: row.label, placeholder: "Name of external entry", "aria-label": n + ": external name", hidden: !!row.id, oninput: e => { row.label = e.target.value; } });
      const qty = h("input", { type: "text", value: row.qty, placeholder: "Quantity, e.g. 2, 250 mB, TBD", "aria-label": n + ": quantity", oninput: e => { row.qty = e.target.value; } });
      const rm = h("button", { type: "button", class: "btn small ghost", "aria-label": "Remove " + n, onclick: () => { rows.splice(i, 1); draw(Math.max(0, i - 1), "sel"); } }, "Remove");
      list.append(h("div", { class: "io-row" }, sel, lab, qty, rm));
      if (focusIndex === i) (focusPart === "sel" ? sel : qty).focus();
    });
    if (focusIndex != null && !rows.length) addBtn.focus();
  }
  addBtn.addEventListener("click", () => { rows.push({ id: "", label: "", qty: "1" }); draw(rows.length - 1, "sel"); });
  draw();
  return { el: h("div", { class: "refs" }, list, h("div", {}, addBtn)), get: () => rows.map(r => ({ id: r.id, label: r.id ? "" : r.label.trim(), qty: r.qty.trim() })) };
}
function relationList(title, recs, empty) {
  return h("div", {}, h("h4", {}, title), recs.length
    ? h("div", { class: "link-chips" }, recs.map(r => h("button", { type: "button", class: "link-chip", onclick: () => openInPlace(r.id) }, h("span", { class: "ref-kind" }, r.kind), h("span", {}, r.title))))
    : h("span", { class: "chip-empty" }, empty));
}

function buildField(f, values, ctx) {''')

rep('''    case "refs":
    case "backlinks": {''', '''    case "relations": {
      const rel = ctx.id ? recipeRelations(ctx.id) : { producedBy: [], usedIn: [], machineFor: [] };
      const el = h("div", { class: wrapCls + " kv" },
        relationList("Produced by", rel.producedBy, ctx.id ? "No recipe or process produces this." : "Save the record first."),
        relationList("Used in", rel.usedIn, ctx.id ? "Not used as an input by any recipe or process." : "Save the record first."),
        ctx.key === "blk" ? relationList("Machine for", rel.machineFor, "Not the machine for any recipe or process.") : null);
      return { el, get: () => null, key: f.k, focusEl: null };
    }
    case "io": {
      const io = ioControl(f, v || []);
      const fs = h("fieldset", { class: wrapCls }, h("legend", {}, f.label), io.el, hint);
      return { el: fs, get: io.get, key: f.k, focusEl: fs.querySelector("select") || fs.querySelector("button") };
    }
    case "refs":
    case "backlinks": {''')

rep('''function openEditor(key, id, preset) {
  const dlg = $("#editor-dialog");
  if (dlg.open) {
    if (ui.editor && ui.editor.key === key && ui.editor.id === id) return;
    dlg.close();
  }''', '''async function openEditor(key, id, preset) {
  const dlg = $("#editor-dialog");
  if (dlg.open) {
    if (ui.editor && ui.editor.key === key && ui.editor.id === id) return;
    if (editorDirty()) {
      const ok = await confirmDialog({ title: "Discard unsaved changes?", message: "Opening another record will discard the edits in the current form.", confirmLabel: "Discard and open", cancelLabel: "Keep editing", tone: "danger" });
      if (!ok) return;
    }
    ui.editor = null;
    dlg.close();
  }''')

rep('''  if (ctx.key === "tsk") {
    const selfId = ctx.id || "__new__";''', '''  if (ctx.key === "rcp") {
    const ioErr = (key, label) => {
      const c = ctx.controls.find(x => x.key === key);
      const lines = values[key] || [];
      lines.forEach((line, i) => { if (!line.id && !line.label) errors.push({ c, msg: label + " " + (i + 1) + " needs a catalog record or an external name." }); });
      return { c, lines };
    };
    const ins = ioErr("inputs", "Input");
    const outs = ioErr("outputs", "Output");
    ioErr("byproducts", "Byproduct");
    if (!ins.lines.length) errors.push({ c: ins.c, msg: "Add at least one input." });
    if (!outs.lines.length) errors.push({ c: outs.c, msg: "Add at least one output." });
  }
  if (ctx.key === "tsk") {
    const selfId = ctx.id || "__new__";''')

rep('''  for (const b of state.blocks) if (b.compat.includes(id)) refs.push(b.name + " (block compatibility)");
  return refs;''', '''  for (const k of CONTENT_KEYS) for (const r of state[CONTENT[k].coll]) {
    if (r.compat.includes(id)) refs.push(r.name + " (compatibility dependency)");
    if (r.machines && r.machines.includes(id)) refs.push(r.name + " (machine reference)");
  }
  for (const r of state.recipes) {
    if (["inputs", "outputs", "byproducts"].some(f => r[f].some(x => x.id === id))) refs.push(r.title + " (recipe entry, kept as a plain label)");
    if (r.machineRef === id) refs.push(r.title + " (recipe machine)");
  }
  return refs;''')

rep('''  for (const b of state.blocks) b.compat = b.compat.filter(x => x !== id);
  if (state.prefs.lastRecord === id) state.prefs.lastRecord = "";''', '''  const label = recordLabel(id);
  for (const k of CONTENT_KEYS) for (const r of state[CONTENT[k].coll]) {
    r.compat = r.compat.filter(x => x !== id);
    if (r.machines) r.machines = r.machines.filter(x => x !== id);
  }
  for (const r of state.recipes) {
    for (const f of ["inputs", "outputs", "byproducts"]) r[f] = r[f].map(x => x.id === id ? { id: "", label: label + " (deleted)", qty: x.qty } : x);
    if (r.machineRef === id) { r.machineRef = ""; if (!r.method) r.method = label + " (deleted)"; }
  }
  if (state.prefs.lastRecord === id) state.prefs.lastRecord = "";''')

rep('''  if (key === "blk") {
    copy.name = (copy.name || "Block") + " (copy)";''', '''  if (key === "blk" || key === "itm" || key === "fld") {
    copy.name = (copy.name || K.label) + " (copy)";''')
rep('''  } else if (key === "dec") {
    copy.title = copy.title + " (copy)";
    copy.status = "Open";
  }''', '''  } else if (key === "dec") {
    copy.title = copy.title + " (copy)";
    copy.status = "Open";
  } else if (key === "rcp") {
    copy.title = copy.title + " (copy)";
    copy.status = "Proposed";
  }''')

rep('''  const ok = await confirmDialog({
    title: "Delete " + f.K.label.toLowerCase() + "?",''', '''  if (findRecord(id) == null) return;
  const ok = await confirmDialog({
    title: "Delete " + f.K.label.toLowerCase() + "?",''')

open(p, 'w', encoding='utf8').write(t)
print("patched", p)

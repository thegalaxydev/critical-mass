import sys

p = sys.argv[1]
t = open(p, encoding='utf8').read()


def rep(a, b):
    global t
    assert t.count(a) == 1, a[:90]
    t = t.replace(a, b)


rep('''  blk: { coll: "blocks", label: "Block", plural: "Blocks", section: "catalog", title: r => r.name },''',
'''  blk: { coll: "blocks", label: "Block", plural: "Blocks", section: "catalog", title: r => r.name },
  itm: { coll: "items", label: "Item", plural: "Items", section: "catalog", title: r => r.name },
  fld: { coll: "fluids", label: "Fluid", plural: "Fluids", section: "catalog", title: r => r.name },
  rcp: { coll: "recipes", label: "Recipe/process", plural: "Recipes and processes", section: "recipes", title: r => r.title },''')

rep('''const SPECS = {''', '''const CONTENT = {
  blk: { coll: "blocks", label: "Block", plural: "Blocks", categories: () => CATEGORIES, spec: SPEC_FIELDS },
  itm: { coll: "items", label: "Item", plural: "Items", categories: () => ITEM_CATEGORIES, spec: ITEM_SPEC_FIELDS },
  fld: { coll: "fluids", label: "Fluid", plural: "Fluids", categories: () => FLUID_CATEGORIES, spec: FLUID_SPEC_FIELDS }
};
const CONTENT_KEYS = Object.keys(CONTENT);

const SPECS = {''')

rep('''  systems: { prefix: "sys",''', '''  items: {
    prefix: "itm", required: ["name"],
    fields: {
      name: "s", registryId: "s", category: ITEM_CATEGORIES, status: BLOCK_STATUSES, milestoneId: "s", applicability: APPLICABILITY.map(a => a[0]), compat: "a", machines: "a",
      purpose: "s", stackSize: "s", acquisition: "s", outputs: "s", persistentData: "s", radiation: "s", tooltip: "s", visual: "s", acceptance: "s", notes: "s", updatedAt: "d"
    },
    defaults: { category: ITEM_CATEGORIES[0], status: "Proposed", applicability: "both" }
  },
  fluids: {
    prefix: "fld", required: ["name"],
    fields: {
      name: "s", ownership: OWNERSHIP.map(o => o[0]), registryId: "s", category: FLUID_CATEGORIES, status: BLOCK_STATUSES, milestoneId: "s", applicability: APPLICABILITY.map(a => a[0]), compat: "a", machines: "a",
      purpose: "s", production: "s", equipment: "s", thermal: "s", pressure: "s", contamination: "s", worldPlacement: "s", container: "s", compatibility: "s", acceptance: "s", notes: "s", updatedAt: "d"
    },
    defaults: { ownership: "addon", category: FLUID_CATEGORIES[0], status: "Proposed", applicability: "both" }
  },
  recipes: {
    prefix: "rcp", required: ["title"],
    fields: { title: "s", kind: RECIPE_KINDS, status: RECIPE_STATUSES, inputs: "io", outputs: "io", byproducts: "io", machineRef: "s", method: "s", time: "s", conditions: "s", notes: "s", updatedAt: "d" },
    defaults: { kind: "Crafting", status: "Proposed" }
  },
  systems: { prefix: "sys",''')

rep('''  catalog: { q: "", category: "", status: "", milestone: "", applicability: "", compat: "", missing: "", sort: "name", dir: 1 },''',
'''  catalog: { tab: "all", q: "", category: "", status: "", milestone: "", applicability: "", compat: "", missing: "", ownership: "", sort: "name", dir: 1 },
  recipes: { q: "", kind: "", status: "", involves: "" },''')

rep('''function isMissing(v) {''', '''function contentKeyOf(id) { const k = kindKey(id); return CONTENT[k] ? k : null; }
function allContent() { return CONTENT_KEYS.flatMap(k => state[CONTENT[k].coll].map(r => ({ key: k, r }))); }
function contentMissing(key, r) { return CONTENT[key].spec.filter(([k]) => isMissing(r[k])).map(([k]) => k); }
function isAddonFluid(f) { return f.ownership !== "vanilla"; }
function ioLabel(line) { return line.id ? recordLabel(line.id) : (line.label || "Unnamed entry"); }
function recipeRelations(id) {
  const producedBy = [], usedIn = [], machineFor = [];
  for (const r of state.recipes) {
    if (r.outputs.some(x => x.id === id) || r.byproducts.some(x => x.id === id)) producedBy.push(r);
    if (r.inputs.some(x => x.id === id)) usedIn.push(r);
    if (r.machineRef === id) machineFor.push(r);
  }
  return { producedBy, usedIn, machineFor };
}
function isMissing(v) {''')

rep('''function blockMissing(b) { return SPEC_FIELDS.filter(([k]) => isMissing(b[k])).map(([k]) => k); }''',
'''function blockMissing(b) { return contentMissing("blk", b); }''')

rep('''    } else if (t === "a") {''', '''    } else if (t === "io") {
      if (v == null) out[k] = [];
      else if (!Array.isArray(v)) errs.push(label + " field " + k + " must be a list of entries.");
      else {
        out[k] = [];
        v.forEach((line, j) => {
          if (!line || typeof line !== "object" || Array.isArray(line)) { errs.push(label + " " + k + "[" + j + "] is not an entry object."); return; }
          const id = line.id == null ? "" : line.id;
          const lab = line.label == null ? "" : line.label;
          const qty = line.qty == null ? "" : line.qty;
          if (typeof id !== "string" || typeof lab !== "string" || (typeof qty !== "string" && typeof qty !== "number")) { errs.push(label + " " + k + "[" + j + "] has fields of the wrong type."); return; }
          if (!id && !lab.trim()) { errs.push(label + " " + k + "[" + j + "] needs a catalog reference or a label."); return; }
          if (lab.length > 500 || String(qty).length > 500) { errs.push(label + " " + k + "[" + j + "] text is too long."); return; }
          out[k].push({ id, label: lab, qty: String(qty) });
        });
      }
    } else if (t === "a") {''')

rep('''  if (errors.length) return { ok: false, errors, warnings };

  const data = { schemaVersion: SCHEMA_VERSION, app: APP_ID };''', '''  if (errors.length) return { ok: false, errors, warnings };
  let migration = null;
  if (raw.schemaVersion < SCHEMA_VERSION) {
    try {
      const mig = migrateProject(raw);
      raw = mig.data;
      migration = mig.report;
    } catch (e) {
      return { ok: false, errors: ["Migration from schema " + raw.schemaVersion + " failed: " + ((e && e.message) || "unknown error")], warnings };
    }
  }

  const data = { schemaVersion: SCHEMA_VERSION, app: APP_ID };''')

rep('''  if (!m) errors.push("meta section is missing.");''', '''  if (!m) errors.push("meta section is missing.");
  const appliedSeeds = m && Array.isArray(m.appliedSeeds) ? m.appliedSeeds.filter(x => typeof x === "string") : [];''')
rep('''    currentMilestoneId: m && typeof m.currentMilestoneId === "string" ? m.currentMilestoneId : "",''', '''    currentMilestoneId: m && typeof m.currentMilestoneId === "string" ? m.currentMilestoneId : "",
    appliedSeeds,''')
rep('''  if (errors.length) return { ok: false, errors, warnings };

  const msIds''', '''  if (errors.length) return { ok: false, errors, warnings, migration };

  const msIds''')

rep('''  for (const t of data.tasks) {
    pruneRefs(t, "deps",''', '''  for (const coll of ["items", "fluids"]) {
    for (const r of data[coll]) {
      pruneRefs(r, "compat", id => id.startsWith("cmp-") && allIds.has(id), "compatibility reference(s)");
      pruneRefs(r, "machines", id => id.startsWith("blk-") && allIds.has(id), "machine reference(s)");
      if (r.milestoneId && !msIds.has(r.milestoneId)) { warnings.push(r.id + ": unknown milestone cleared."); r.milestoneId = ""; }
    }
  }
  const contentIds = new Set(CONTENT_KEYS.flatMap(k => data[CONTENT[k].coll].map(r => r.id)));
  for (const r of data.recipes) {
    for (const f of ["inputs", "outputs", "byproducts"]) {
      r[f].forEach((line, j) => { if (line.id && !contentIds.has(line.id)) errors.push("Recipe \\"" + r.id + "\\" " + f + "[" + j + "] references missing catalog record \\"" + line.id + "\\"."); });
    }
    if (r.machineRef && !contentIds.has(r.machineRef)) errors.push("Recipe \\"" + r.id + "\\" machine references missing catalog record \\"" + r.machineRef + "\\".");
  }
  for (const t of data.tasks) {
    pruneRefs(t, "deps",''')

rep('''  const cycle = findCycleInTasks(data.tasks);
  if (cycle) errors.push("Circular task dependency: " + cycle.join(" → ") + ".");
  if (errors.length) return { ok: false, errors, warnings };
  return { ok: true, errors, warnings, data };''', '''  const cycle = findCycleInTasks(data.tasks);
  if (cycle) errors.push("Circular task dependency: " + cycle.join(" → ") + ".");
  if (errors.length) return { ok: false, errors, warnings, migration };
  return { ok: true, errors, warnings, data, migration };''')

open(p, 'w', encoding='utf8').write(t)
print("patched", p)

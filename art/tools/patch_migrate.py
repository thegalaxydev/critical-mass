import sys

p = sys.argv[1]
t = open(p, encoding='utf8').read()


def rep(a, b):
    global t
    assert t.count(a) == 1, a[:90]
    t = t.replace(a, b)


rep('''function addBanner(b) {''', '''function migrateProject(raw) {
  const data = JSON.parse(JSON.stringify(raw));
  const report = { from: raw.schemaVersion, to: SCHEMA_VERSION, added: { items: 0, fluids: 0, recipes: 0, tasks: 0, decisions: 0, systems: 0 }, adjusted: [] };
  if (data.schemaVersion === 1) {
    migrateV1toV2(data, report);
    data.schemaVersion = 2;
  }
  if (data.schemaVersion !== SCHEMA_VERSION) throw new Error("no migration path from schema " + data.schemaVersion);
  return { data, report };
}
function migrateV1toV2(data, report) {
  if (!data.meta || typeof data.meta !== "object" || Array.isArray(data.meta)) throw new Error("meta section is missing");
  for (const k of ["items", "fluids", "recipes"]) {
    if (data[k] == null) data[k] = [];
    else if (!Array.isArray(data[k])) throw new Error("\\"" + k + "\\" exists but is not a list");
  }
  const applied = Array.isArray(data.meta.appliedSeeds) ? data.meta.appliedSeeds.filter(x => typeof x === "string") : [];
  if (!applied.includes("core-v1")) applied.push("core-v1");
  if (!applied.includes("content-v2")) {
    applySeedSet(data, CONTENT_V2_SEED(), report);
    applied.push("content-v2");
  }
  data.meta.appliedSeeds = applied;
}
function applySeedSet(data, seed, report) {
  const ids = new Set();
  for (const key of COLLECTION_KEYS) if (Array.isArray(data[key])) for (const r of data[key]) if (r && typeof r.id === "string") ids.add(r.id);
  const added = [];
  for (const [coll, recs] of Object.entries(seed)) {
    if (!Array.isArray(data[coll])) throw new Error("\\"" + coll + "\\" is missing or not a list");
    for (const rec of recs) {
      if (ids.has(rec.id)) continue;
      data[coll].push(rec);
      ids.add(rec.id);
      added.push({ coll, rec });
      report.added[coll] = (report.added[coll] || 0) + 1;
    }
  }
  const keep = (rec, field) => {
    if (!Array.isArray(rec[field])) return;
    const before = rec[field].length;
    rec[field] = rec[field].filter(id => ids.has(id));
    if (rec[field].length !== before) report.adjusted.push(rec.id + ": dropped " + (before - rec[field].length) + " " + field + " reference(s) to records not in this project");
  };
  for (const { coll, rec } of added) {
    if (coll === "tasks") { keep(rec, "deps"); keep(rec, "links"); }
    else if (coll === "decisions") keep(rec, "related");
    else if (coll === "systems") keep(rec, "relatedBlocks");
    else if (coll === "items" || coll === "fluids") { keep(rec, "machines"); keep(rec, "compat"); }
    else if (coll === "recipes") {
      for (const f of ["inputs", "outputs", "byproducts"]) {
        rec[f] = rec[f].map(line => {
          if (!line.id || ids.has(line.id)) return line;
          report.adjusted.push(rec.id + ": " + line.id + " is not in this project, kept as a plain label");
          return { id: "", label: line.id + " (not in this project)", qty: line.qty };
        });
      }
      if (rec.machineRef && !ids.has(rec.machineRef)) { report.adjusted.push(rec.id + ": machine " + rec.machineRef + " is not in this project"); rec.machineRef = ""; }
    }
  }
}
function migrationSummary(report) {
  const a = report.added;
  const parts = [["items", "item"], ["fluids", "fluid"], ["recipes", "recipe/process", "recipes/processes"], ["tasks", "task"], ["decisions", "decision"], ["systems", "system page"]]
    .filter(([k]) => a[k]).map(([k, one, many]) => plural(a[k], one, many));
  return "Schema " + report.from + " → " + report.to + ". Added " + (parts.length ? parts.join(", ") : "no new records") + "." + (report.adjusted.length ? " " + plural(report.adjusted.length, "reference") + " adjusted for records you had removed." : "");
}
function downloadText(text, filename) {
  const blob = new Blob([text], { type: "application/json" });
  const url = URL.createObjectURL(blob);
  const a = h("a", { href: url, download: filename });
  document.body.append(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 2000);
}

function addBanner(b) {''')

rep('''  const res = validateProject(parsed);
  if (!res.ok) return recoverUnreadable(raw, res.errors);''', '''  const res = validateProject(parsed);
  const oldSchema = parsed && typeof parsed.schemaVersion === "number" && parsed.schemaVersion < SCHEMA_VERSION;
  if (!res.ok) return oldSchema ? migrationFailed(raw, parsed.schemaVersion, res.errors) : recoverUnreadable(raw, res.errors);
  if (res.migration) {
    let backedUp = false;
    try { localStorage.setItem(MIGRATION_BACKUP_KEY, raw); backedUp = localStorage.getItem(MIGRATION_BACKUP_KEY) === raw; } catch (e) { backedUp = false; }
    if (!backedUp) {
      ui.saveBlocked = true;
      addBanner({
        id: "migration", tone: "bad", title: "Upgrade paused: a backup could not be written",
        text: "Your schema " + parsed.schemaVersion + " data is still stored unchanged. The upgraded view below is not being saved. Download the original data, free some browser storage for this page, then reload to retry the upgrade.",
        actions: [{ label: "Download original data", fn: () => downloadText(raw, "critical-mass-console-schema" + parsed.schemaVersion + "-original.json") }, { label: "Reload and retry", fn: () => location.reload() }]
      });
      return res.data;
    }
    ui.migrated = res.migration;
    addBanner({
      id: "migration", tone: "info", title: "Project upgraded to schema " + SCHEMA_VERSION,
      text: migrationSummary(res.migration) + " Your previous data is kept as a backup in this browser under \\"" + MIGRATION_BACKUP_KEY + "\\".",
      actions: [{ label: "Download pre-upgrade backup", fn: () => { let b = null; try { b = localStorage.getItem(MIGRATION_BACKUP_KEY); } catch (e) { b = null; } if (b) downloadText(b, "critical-mass-console-before-schema-" + SCHEMA_VERSION + ".json"); else toast("The backup is no longer in browser storage.", "warn"); } }],
      dismissible: true
    });
  }''')

rep('''function recoverUnreadable(raw, errors) {''', '''function migrationFailed(raw, fromVersion, errors) {
  ui.saveBlocked = true;
  addBanner({
    id: "migration", tone: "bad", title: "Upgrade from schema " + fromVersion + " failed; your data was not changed",
    text: "Reason: " + errors.slice(0, 3).join(" ") + " Nothing in browser storage was overwritten and saving is paused. Download the original data to keep it safe, then report the reason above.",
    actions: [{ label: "Download original data", fn: () => downloadText(raw, "critical-mass-console-schema" + fromVersion + "-original.json") }]
  });
  return createSeed();
}
function recoverUnreadable(raw, errors) {''')

rep('''  const blob = new Blob([JSON.stringify(data, null, 2)], { type: "application/json" });
  const url = URL.createObjectURL(blob);
  const a = h("a", { href: url, download: "critical-mass-console-" + todayYMD() + ".json" });
  document.body.append(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 2000);
  toast(''', '''  downloadText(JSON.stringify(data, null, 2), "critical-mass-console-" + todayYMD() + ".json");
  toast(''')

open(p, 'w', encoding='utf8').write(t)
print("patched", p)

import sys

p = sys.argv[1]
t = open(p, encoding='utf8').read()
anchor = '.scrim{display:none}'
assert t.count(anchor) == 1
css = '''.tone-faint{color:var(--text-faint);background:transparent;border-color:var(--line-strong);border-style:dashed}
.bg-faint{background:repeating-linear-gradient(135deg,var(--line-strong) 0 3px,transparent 3px 6px)}
.tabs{display:flex;flex-wrap:wrap;gap:4px;border-bottom:1px solid var(--line);margin-bottom:16px}
.tabs [role="tab"]{display:inline-flex;align-items:center;gap:8px;background:none;border:1px solid transparent;border-bottom:none;border-radius:5px 5px 0 0;padding:8px 14px;margin-bottom:-1px;cursor:pointer;color:var(--text-muted);font-weight:550;font-size:.9rem}
.tabs [role="tab"]:hover{color:var(--text);background:var(--surface-2)}
.tabs [role="tab"][aria-selected="true"]{color:var(--text);background:var(--surface);border-color:var(--line);box-shadow:inset 0 2px 0 var(--brass)}
.tab-count{font-family:var(--mono);font-size:.72rem;color:var(--text-faint)}
.totals{display:flex;flex-wrap:wrap;gap:6px 22px;margin:-6px 0 16px;font-size:.88rem;color:var(--text-muted)}
.totals strong{font-family:var(--mono);color:var(--text);font-size:1rem;margin-right:4px}
.io-rows{display:flex;flex-direction:column;gap:8px}
.io-row{display:flex;flex-wrap:wrap;gap:8px;align-items:center}
.io-row select{flex:2 1 220px;width:auto}
.io-row input[type="text"]{flex:1 1 150px;width:auto}
.io-row .btn{flex:none}
.chain{display:flex;flex-wrap:wrap;align-items:center;gap:6px}
.chain-row h3{margin-bottom:6px}
.chain-step{background:none;border:1px dashed var(--line-strong);border-radius:4px;padding:2px 8px;font-size:.78rem;color:var(--text-muted);cursor:pointer}
.chain-step:hover{color:var(--brass-strong);border-color:var(--brass)}
.link-chip .mono{font-size:.72rem}
.ref-chip.static .mono{font-size:.72rem;margin-left:4px}
ul.legend{display:flex;flex-wrap:wrap;gap:6px 16px}
table.recipe-table{min-width:1180px}
table.recipe-table td:nth-child(1){min-width:200px}
table.recipe-table td:nth-child(2),table.recipe-table td:nth-child(4){min-width:230px}
table.content-table{min-width:980px}
'''
t = t.replace(anchor, css + anchor)
open(p, 'w', encoding='utf8').write(t)
print("patched", p)

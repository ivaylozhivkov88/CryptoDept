import os, json
from pathlib import Path

ROOT = Path(r"d:\cryptodept")
SKIP_DIRS = {'.git','node_modules','__pycache__','.venv','venv','dist','build','.next'}
SKIP_EXT  = {'.pyc','.pyo','.lock','.map','.ico','.png','.jpg','.gif','.woff','.ttf','.eot','.svg'}
TEXT_EXT  = {'.py','.js','.ts','.tsx','.jsx','.json','.yaml','.yml','.toml','.cfg',
             '.ini','.env','.md','.txt','.html','.css','.sh','.bat','.sql','.xml'}
MAX_FILE  = 8_000  # chars per file

out = []

# ── 1. STRUCTURE ──────────────────────────────────────────────────────────────
tree = []
for p in sorted(ROOT.rglob("*")):
    if any(s in p.parts for s in SKIP_DIRS): continue
    rel = p.relative_to(ROOT)
    depth = len(rel.parts) - 1
    icon = "📁" if p.is_dir() else "📄"
    tree.append("  " * depth + f"{icon} {p.name}")
out.append("=== STRUCTURE ===\n" + "\n".join(tree))

# ── 2. KEY CONFIG FILES ───────────────────────────────────────────────────────
key_names = {'package.json','requirements.txt','pyproject.toml','setup.py',
             'docker-compose.yml','Dockerfile','.env.example','config.yaml',
             'config.yml','settings.py','config.py','README.md'}
configs = []
for name in key_names:
    f = ROOT / name
    if f.exists():
        configs.append(f"-- {name} --\n{f.read_text(errors='ignore')[:2000]}")
if configs:
    out.append("=== KEY CONFIGS ===\n" + "\n".join(configs))

# ── 3. ALL TEXT FILES CONTENT ─────────────────────────────────────────────────
files_out = []
for p in sorted(ROOT.rglob("*")):
    if p.is_dir(): continue
    if any(s in p.parts for s in SKIP_DIRS): continue
    if p.suffix.lower() in SKIP_EXT: continue
    if p.suffix.lower() not in TEXT_EXT: continue
    try:
        content = p.read_text(errors='ignore').strip()
        if not content: continue
        rel = p.relative_to(ROOT)
        snippet = content[:MAX_FILE]
        if len(content) > MAX_FILE:
            snippet += f"\n... [truncated {len(content)-MAX_FILE} chars]"
        files_out.append(f"### {rel}\n{snippet}")
    except Exception as e:
        files_out.append(f"### {rel} [ERROR: {e}]")
if files_out:
    out.append("=== FILES ===\n" + "\n\n".join(files_out))

# ── 4. WRITE REPORT ───────────────────────────────────────────────────────────
report = "\n\n".join(out)
out_path = ROOT / "_project_dump.txt"
out_path.write_text(report, encoding="utf-8")
print(f"✅ Done → {out_path}  ({len(report):,} chars)")
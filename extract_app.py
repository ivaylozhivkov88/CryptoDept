import os

ROOT = r"D:\CryptoDept\app\src\main\java\com\cryptodept"
OUTPUT = r"D:\CryptoDept\CLAUDE_SCREENS_DUMP.txt"

# ─── САМО UI ФАЙЛОВЕ — всичко което се вижда на екрана ────────
TARGET_PATTERNS = {

    "01_DASHBOARD_ALL": lambda p, f:
        "dashboard" in p.lower() and f.endswith(".kt"),

    "02_MARKETS_SCREEN": lambda p, f:
        "market" in p.lower() and f.endswith(".kt") and
        not "viewmodel" in f.lower(),

    "03_ANALYSIS_PREDICTION": lambda p, f:
        any(k in p.lower() for k in ["analysis", "prediction", "oracle"]) and
        f.endswith(".kt") and "viewmodel" not in f.lower(),

    "04_WHALE_TRACKER": lambda p, f:
        "whale" in p.lower() and f.endswith(".kt") and
        "viewmodel" not in f.lower(),

    "05_ALERTS_SCREEN": lambda p, f:
        "alert" in p.lower() and f.endswith(".kt") and
        "viewmodel" not in f.lower(),

    "06_PORTFOLIO": lambda p, f:
        "portfolio" in p.lower() and f.endswith(".kt") and
        "viewmodel" not in f.lower(),

    "07_SETTINGS_ALL": lambda p, f:
        "setting" in p.lower() and f.endswith(".kt"),

    "08_ONBOARDING_TUTORIAL": lambda p, f:
        any(k in p.lower() for k in ["onboard", "tutorial", "boot"]) and
        f.endswith(".kt"),

    "09_PAYWALL_TIER_UI": lambda p, f:
        any(k in f.lower() for k in [
            "paywall", "upgrade", "progate", "tier", "promo"
        ]) and f.endswith(".kt"),

    "10_TOOLS_HUB": lambda p, f:
        any(k in p.lower() for k in [
            "tools", "backtester", "correlation", "entry",
            "risk", "seasonal", "strategy", "comparison",
            "signal", "planner", "mtf"
        ]) and f.endswith(".kt"),

    "11_PSYCHOLOGY_TILT": lambda p, f:
        any(k in p.lower() for k in ["psychology", "tilt"]) and
        f.endswith(".kt"),

    "12_SCREENSAVERS": lambda p, f:
        any(k in f.lower() for k in [
            "bloomberg", "matrix", "heatmap", "screensaver"
        ]) and f.endswith(".kt"),

    "13_NEWS_CALENDAR": lambda p, f:
        any(k in p.lower() for k in ["news", "calendar", "event"]) and
        f.endswith(".kt"),

    "14_DEFI_MACRO": lambda p, f:
        any(k in p.lower() for k in ["defi", "macro", "derivatives"]) and
        f.endswith(".kt") and "viewmodel" not in f.lower(),

    "15_WATCHLIST": lambda p, f:
        "watchlist" in p.lower() and f.endswith(".kt"),

    "16_SHARED_COMPONENTS": lambda p, f:
        any(k in p.lower() for k in ["component", "composable", "widget"]) and
        f.endswith(".kt"),

    "17_NAVIGATION_SCREENS": lambda p, f:
        any(k in f.lower() for k in [
            "navgraph", "bottomnav", "navigation", "screen.kt"
        ]) and f.endswith(".kt"),

    "18_THEME_COLORS": lambda p, f:
        any(k in f.lower() for k in [
            "theme", "color", "terminal", "typography",
            "phosphor", "crt"
        ]) and f.endswith(".kt"),

    "19_MAIN_ACTIVITY_APP": lambda p, f:
        f in ("MainActivity.kt", "CryptoDeptApplication.kt"),

    "20_AGENT_HUB_AI": lambda p, f:
        any(k in p.lower() for k in ["agent", "ai", "coach"]) and
        f.endswith(".kt") and "viewmodel" not in f.lower(),

    "21_GLOSSARY_SEARCH": lambda p, f:
        any(k in p.lower() for k in ["glossary", "search"]) and
        f.endswith(".kt"),

    "22_PROFILE_ACCOUNT": lambda p, f:
        any(k in p.lower() for k in ["profile", "account", "auth"]) and
        f.endswith(".kt"),

    "23_JOURNAL_TRADE": lambda p, f:
        any(k in p.lower() for k in ["journal", "trade", "tradeplan"]) and
        f.endswith(".kt") and "viewmodel" not in f.lower(),

    "24_WIDGET_HOME": lambda p, f:
        "widget" in p.lower() and f.endswith(".kt"),

    "25_MANIFEST_STRINGS": lambda p, f:
        f in ("AndroidManifest.xml", "strings.xml"),
}

SKIP_DIRS = {
    ".gradle", ".idea", ".kotlin", "build", "generated",
    "androidTest", "test", "sampledata", "release",
    "node_modules", ".git", "__pycache__",
}

SKIP_EXTENSIONS = {
    ".bin", ".png", ".jpg", ".jpeg", ".webp",
    ".aab", ".apk", ".lock", ".class",
}

# По-висок лимит за screens — те са по-големи
MAX_FILE_KB = 150
MAX_TOTAL_MB = 10

def should_skip_dir(path):
    return bool(set(path.replace("\\", "/").split("/")) & SKIP_DIRS)

def get_lang(f):
    return {".kt": "kotlin", ".xml": "xml", ".md": "markdown",
            ".js": "javascript", ".json": "json"}.get(
        os.path.splitext(f)[1].lower(), "text")

# ─── СКЕНИРАЙ ─────────────────────────────────────────────────
SCAN_ROOTS = [
    r"D:\CryptoDept\app\src\main\java\com\cryptodept",
    r"D:\CryptoDept\app\src\main\res",
    r"D:\CryptoDept\app\src\main",
]

seen = set()
all_files = []

for scan_root in SCAN_ROOTS:
    if not os.path.exists(scan_root):
        continue
    for dirpath, dirnames, filenames in os.walk(scan_root):
        dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS]
        if should_skip_dir(dirpath):
            continue
        for filename in filenames:
            ext = os.path.splitext(filename)[1].lower()
            if ext in SKIP_EXTENSIONS:
                continue
            full = os.path.join(dirpath, filename)
            if full in seen:
                continue
            seen.add(full)
            try:
                size = os.path.getsize(full)
                all_files.append((full, filename, size))
            except:
                pass

# ─── КАТЕГОРИЗИРАЙ ────────────────────────────────────────────
categorized = {k: [] for k in TARGET_PATTERNS}
uncategorized = []

for full, filename, size in all_files:
    norm = full.replace("\\", "/").lower()
    matched = False
    for cat, matcher in TARGET_PATTERNS.items():
        try:
            if matcher(norm, filename):
                categorized[cat].append((full, filename, size))
                matched = True
                break
        except:
            pass
    if not matched:
        uncategorized.append((full, filename, size))

# ─── ЗАПИС ────────────────────────────────────────────────────
total_bytes = 0
max_bytes = MAX_TOTAL_MB * 1024 * 1024
skipped_large = []
skipped_limit = []
lines = []

lines.append("=" * 80)
lines.append("CRYPTODEPT — SCREENS & UI FULL DUMP")
lines.append(f"MAX FILE: {MAX_FILE_KB}KB | MAX TOTAL: {MAX_TOTAL_MB}MB")
lines.append("=" * 80)

# Summary
lines.append("\n## SUMMARY\n")
total = sum(len(v) for v in categorized.values()) + len(uncategorized)
lines.append(f"Total files: {total}\n")
for cat, files in sorted(categorized.items()):
    if files:
        kb = sum(s for _, _, s in files) / 1024
        lines.append(f"  {cat}: {len(files)} files ({kb:.1f} KB)")
lines.append(f"  UNCATEGORIZED: {len(uncategorized)} files")
lines.append("\n" + "=" * 80)

def write_category(cat_name, files):
    global total_bytes
    lines.append(f"\n{'#' * 60}")
    lines.append(f"## {cat_name} ({len(files)} files)")
    lines.append(f"{'#' * 60}\n")

    # Сортирай: Screen файловете първи, после компонентите
    sorted_files = sorted(files, key=lambda x: (
        0 if "screen" in x[1].lower() else 1,
        x[2]
    ))

    for full, filename, size in sorted_files:
        if total_bytes >= max_bytes:
            skipped_limit.append(full)
            continue

        size_kb = size / 1024
        if size_kb > MAX_FILE_KB:
            skipped_large.append((full, size_kb))
            rel = os.path.relpath(full, r"D:\CryptoDept")
            lines.append(f"[SKIPPED — {size_kb:.1f}KB > {MAX_FILE_KB}KB] {rel}")
            lines.append(f"[PREVIEW — first 100 lines]")
            try:
                with open(full, "r", encoding="utf-8", errors="replace") as f:
                    preview = "".join(f.readlines()[:100])
                lines.append(f"```{get_lang(filename)}")
                lines.append(preview)
                lines.append("```\n")
                total_bytes += min(size, 8000)
            except:
                pass
            continue

        rel = os.path.relpath(full, r"D:\CryptoDept")
        lines.append(f"### FILE: {rel} ({size_kb:.1f} KB)")
        lines.append(f"```{get_lang(filename)}")
        try:
            with open(full, "r", encoding="utf-8", errors="replace") as f:
                content = f.read()
            lines.append(content)
            total_bytes += size
        except Exception as e:
            lines.append(f"[ERROR: {e}]")
        lines.append("```\n")

for cat, files in sorted(categorized.items()):
    if files:
        write_category(cat, files)

# Uncategorized — само списък
if uncategorized:
    lines.append(f"\n{'#' * 60}")
    lines.append(f"## UNCATEGORIZED — LIST ONLY ({len(uncategorized)} files)")
    lines.append(f"{'#' * 60}")
    for full, filename, size in sorted(uncategorized, key=lambda x: x[0]):
        rel = os.path.relpath(full, r"D:\CryptoDept")
        lines.append(f"  {rel} ({size/1024:.1f}KB)")

# Footer
lines.append("\n" + "=" * 80)
lines.append(f"TOTAL WRITTEN: {total_bytes/1024/1024:.2f} MB")
lines.append(f"LARGE FILES — 100-LINE PREVIEW ({len(skipped_large)}):")
for path, kb in sorted(skipped_large, key=lambda x: -x[1]):
    lines.append(f"  {os.path.relpath(path, r'D:\CryptoDept')} ({kb:.1f}KB)")
if skipped_limit:
    lines.append(f"SIZE LIMIT HIT — {len(skipped_limit)} files not included")
lines.append("=" * 80)

with open(OUTPUT, "w", encoding="utf-8") as f:
    f.write("\n".join(lines))

print(f"\n✅ Записано: {OUTPUT}")
print(f"📦 {total_bytes/1024/1024:.2f} MB от {MAX_TOTAL_MB}MB")
print(f"📂 Категории: {sum(1 for v in categorized.values() if v)}")
print(f"⏭️  Preview (>{MAX_FILE_KB}KB): {len(skipped_large)}")
print(f"❌ Лимит: {len(skipped_limit)}")
print(f"❓ Uncategorized: {len(uncategorized)}")
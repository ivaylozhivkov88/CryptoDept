#!/usr/bin/env python3
"""
Android Studio Project Scraper
Извлича структура, код и конфигурации от Android проект
и ги записва в компактен текстов файл за анализ от AI.

Използване: python scrape_android_project.py [папка] [изходен_файл]
По подразбиране: d:/cryptodept -> project_dump.txt
"""

import os
import sys
import re
from pathlib import Path
from datetime import datetime

# ──────────────────────────────────────────────
# НАСТРОЙКИ
# ──────────────────────────────────────────────

ROOT = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("d:/cryptodept")
OUT  = Path(sys.argv[2]) if len(sys.argv) > 2 else Path("project_dump.txt")

# Файлове, които четем изцяло (по разширение)
CODE_EXTENSIONS = {
    ".kt", ".java", ".xml", ".gradle", ".kts",
    ".json", ".yaml", ".yml", ".properties",
    ".pro",   # proguard
    ".toml",  # version catalogs
}

# Файлове, на които само показваме пътя (бинарни / ненужни)
SKIP_EXTENSIONS = {
    ".png", ".jpg", ".jpeg", ".gif", ".webp", ".svg", ".ico",
    ".ttf", ".otf", ".woff",
    ".keystore", ".jks",
    ".class", ".dex", ".aar", ".jar", ".zip", ".so",
    ".iml", ".DS_Store",
}

# Папки, които пропускаме изцяло
SKIP_DIRS = {
    ".git", ".idea", ".gradle", "build", "intermediates",
    "generated", ".cxx", "__pycache__", "node_modules",
    "captures", ".kotlin",
}

# Максимален брой редове на файл (0 = без ограничение)
MAX_LINES_PER_FILE = 300

# Максимален размер на файл в байтове (файлове над това → само пътя)
MAX_FILE_SIZE = 80_000

# XML файлове, от които вземаме само skeleton (без стойности на атрибути)
XML_SKELETON_ONLY = {"strings.xml", "colors.xml"}

# ──────────────────────────────────────────────
# ПОМОЩНИ ФУНКЦИИ
# ──────────────────────────────────────────────

def should_skip_dir(d: Path) -> bool:
    return d.name in SKIP_DIRS or d.name.startswith(".")

def should_skip_file(f: Path) -> bool:
    if f.suffix.lower() in SKIP_EXTENSIONS:
        return True
    if f.name.startswith("."):
        return True
    # Пропускаме генерирани R.java / BuildConfig
    if f.name in {"R.java", "BuildConfig.java", "R.kt"}:
        return True
    return False

def relative(p: Path) -> str:
    try:
        return str(p.relative_to(ROOT)).replace("\\", "/")
    except ValueError:
        return str(p)

def strip_comments_kotlin(src: str) -> str:
    """Премахва block и line коментари от Kotlin/Java."""
    src = re.sub(r'/\*[\s\S]*?\*/', '', src)
    src = re.sub(r'//[^\n]*', '', src)
    return src

def strip_comments_xml(src: str) -> str:
    return re.sub(r'<!--[\s\S]*?-->', '', src)

def compact_blank_lines(src: str) -> str:
    """Повече от 1 празен ред → 1."""
    return re.sub(r'\n{3,}', '\n\n', src)

def read_file_compact(f: Path) -> str:
    """Прочита файл и го компресира."""
    size = f.stat().st_size
    if size > MAX_FILE_SIZE:
        return f"[ПРОПУСНАТ: {size:,} байта — над лимита]\n"

    try:
        src = f.read_text(encoding="utf-8", errors="replace")
    except Exception as e:
        return f"[ГРЕШКА ПРИ ЧЕТЕНЕ: {e}]\n"

    ext = f.suffix.lower()

    # XML обработка
    if ext == ".xml":
        src = strip_comments_xml(src)
        if f.name in XML_SKELETON_ONLY:
            # Само имената на ключовете, без стойности
            keys = re.findall(r'name="([^"]+)"', src)
            return "keys: " + ", ".join(keys) + "\n" if keys else "[празен]\n"
        src = compact_blank_lines(src)

    # Kotlin / Java
    elif ext in {".kt", ".java"}:
        src = strip_comments_kotlin(src)
        src = compact_blank_lines(src)

    # Gradle — пропускаме dependencies блока ако е много дълъг
    elif ext in {".gradle", ".kts"}:
        src = strip_comments_kotlin(src)
        src = compact_blank_lines(src)

    lines = src.splitlines()
    # Изрязваме празни редове в началото/края
    while lines and not lines[0].strip():
        lines.pop(0)
    while lines and not lines[-1].strip():
        lines.pop()

    if MAX_LINES_PER_FILE and len(lines) > MAX_LINES_PER_FILE:
        half = MAX_LINES_PER_FILE // 2
        omitted = len(lines) - MAX_LINES_PER_FILE
        lines = (
            lines[:half]
            + [f"\n... [{omitted} реда пропуснати] ...\n"]
            + lines[-half:]
        )

    return "\n".join(lines) + "\n"

def build_tree(root: Path) -> list[str]:
    """Генерира компактно дърво на проекта."""
    lines = []

    def walk(path: Path, prefix: str = ""):
        try:
            entries = sorted(path.iterdir(), key=lambda e: (e.is_file(), e.name.lower()))
        except PermissionError:
            return
        dirs  = [e for e in entries if e.is_dir()  and not should_skip_dir(e)]
        files = [e for e in entries if e.is_file() and not should_skip_file(e)
                 and e.suffix.lower() not in SKIP_EXTENSIONS]

        for i, d in enumerate(dirs):
            connector = "└── " if (i == len(dirs) - 1 and not files) else "├── "
            lines.append(f"{prefix}{connector}{d.name}/")
            extension = "    " if connector.startswith("└") else "│   "
            walk(d, prefix + extension)

        for i, f in enumerate(files):
            connector = "└── " if i == len(files) - 1 else "├── "
            size = f.stat().st_size
            size_str = f"{size:,}b" if size < 1024 else f"{size//1024}KB"
            lines.append(f"{prefix}{connector}{f.name} ({size_str})")

    lines.append(f"{root.name}/")
    walk(root)
    return lines

def collect_files(root: Path):
    """Итерира всички релевантни файлове."""
    for dirpath, dirnames, filenames in os.walk(root):
        dirnames[:] = [
            d for d in sorted(dirnames)
            if not should_skip_dir(Path(dirpath) / d)
        ]
        for fn in sorted(filenames):
            f = Path(dirpath) / fn
            if not should_skip_file(f):
                yield f

# ──────────────────────────────────────────────
# СПЕЦИАЛИЗИРАНИ СЕКЦИИ
# ──────────────────────────────────────────────

def extract_manifest_summary(root: Path) -> str | None:
    """Търси AndroidManifest.xml и извлича ключова информация."""
    manifests = list(root.rglob("AndroidManifest.xml"))
    if not manifests:
        return None
    # Предпочитаме main/
    main = next((m for m in manifests if "main" in str(m)), manifests[0])
    try:
        src = main.read_text(encoding="utf-8", errors="replace")
    except Exception:
        return None

    package    = re.search(r'package="([^"]+)"', src)
    activities = re.findall(r'<activity[^>]*android:name="([^"]+)"', src)
    services   = re.findall(r'<service[^>]*android:name="([^"]+)"', src)
    receivers  = re.findall(r'<receiver[^>]*android:name="([^"]+)"', src)
    providers  = re.findall(r'<provider[^>]*android:name="([^"]+)"', src)
    permissions = re.findall(r'uses-permission[^>]*android:name="([^"]+)"', src)
    features   = re.findall(r'uses-feature[^>]*android:name="([^"]+)"', src)
    min_sdk    = re.search(r'android:minSdkVersion="([^"]+)"', src)
    target_sdk = re.search(r'android:targetSdkVersion="([^"]+)"', src)

    lines = ["=== MANIFEST РЕЗЮМЕ ==="]
    if package:    lines.append(f"Package    : {package.group(1)}")
    if min_sdk:    lines.append(f"minSdk     : {min_sdk.group(1)}")
    if target_sdk: lines.append(f"targetSdk  : {target_sdk.group(1)}")
    if activities: lines.append(f"Activities : {', '.join(activities)}")
    if services:   lines.append(f"Services   : {', '.join(services)}")
    if receivers:  lines.append(f"Receivers  : {', '.join(receivers)}")
    if providers:  lines.append(f"Providers  : {', '.join(providers)}")
    if permissions:lines.append(f"Permissions: {', '.join(permissions)}")
    if features:   lines.append(f"Features   : {', '.join(features)}")
    return "\n".join(lines)

def extract_dependencies(root: Path) -> str:
    """Извлича dependencies от всички build.gradle файлове."""
    results = []
    for f in root.rglob("build.gradle*"):
        if any(d in f.parts for d in SKIP_DIRS):
            continue
        try:
            src = f.read_text(encoding="utf-8", errors="replace")
        except Exception:
            continue
        deps = re.findall(
            r'(implementation|api|testImplementation|androidTestImplementation|'
            r'kapt|ksp|annotationProcessor|classpath|runtimeOnly|compileOnly)'
            r'\s*[\(\s]["\']([^"\']+)["\']',
            src
        )
        if deps:
            results.append(f"  [{relative(f)}]")
            for dep_type, dep in deps:
                results.append(f"    {dep_type}: {dep}")
    return "\n".join(results) if results else "  (не са намерени)"

def extract_kotlin_signatures(src: str) -> str:
    """Само сигнатури: class/object/interface/fun/val/var (без тела)."""
    lines = src.splitlines()
    sig_lines = []
    brace_depth = 0
    in_sig_mode = False

    SIG_PATTERN = re.compile(
        r'^\s*(public|private|protected|internal|override|abstract|sealed|'
        r'data|open|suspend|inline|companion|lateinit|const)?\s*'
        r'(class|object|interface|fun|val|var|enum|typealias)\s+'
    )

    for line in lines:
        stripped = line.strip()
        if SIG_PATTERN.match(line) or SIG_PATTERN.match(stripped):
            sig_lines.append(line.rstrip())

    return "\n".join(sig_lines)

# ──────────────────────────────────────────────
# ГЛАВНА ФУНКЦИЯ
# ──────────────────────────────────────────────

PRIORITY_FILES = {
    # Файлове с висок приоритет — четем изцяло
    "settings.gradle", "settings.gradle.kts",
    "build.gradle", "build.gradle.kts",
    "gradle.properties",
    "local.properties",
    "AndroidManifest.xml",
    "proguard-rules.pro",
    "google-services.json",
    "libs.versions.toml",
    "network_security_config.xml",
}

LAYOUT_LIMIT = 20  # Максимален брой layout XML файлове (останалите → само пътя)

def main():
    if not ROOT.exists():
        print(f"[ГРЕШКА] Папката не съществува: {ROOT}")
        sys.exit(1)

    print(f"Сканиране: {ROOT}")
    lines_out = []

    # ── ХЕДЪР ──────────────────────────────────
    lines_out += [
        f"# ANDROID PROJECT DUMP",
        f"# Проект : {ROOT.name}",
        f"# Дата   : {datetime.now().strftime('%Y-%m-%d %H:%M')}",
        f"# Пътека : {ROOT}",
        "",
    ]

    # ── ДЪРВО ──────────────────────────────────
    lines_out.append("═" * 60)
    lines_out.append("СТРУКТУРА НА ПРОЕКТА")
    lines_out.append("═" * 60)
    lines_out += build_tree(ROOT)
    lines_out.append("")

    # ── MANIFEST РЕЗЮМЕ ─────────────────────────
    manifest_summary = extract_manifest_summary(ROOT)
    if manifest_summary:
        lines_out.append("═" * 60)
        lines_out.append(manifest_summary)
        lines_out.append("")

    # ── DEPENDENCIES ────────────────────────────
    lines_out.append("═" * 60)
    lines_out.append("DEPENDENCIES")
    lines_out.append("═" * 60)
    lines_out.append(extract_dependencies(ROOT))
    lines_out.append("")

    # ── ФАЙЛОВЕ ─────────────────────────────────
    lines_out.append("═" * 60)
    lines_out.append("ФАЙЛОВЕ")
    lines_out.append("═" * 60)

    layout_count = 0
    all_files = list(collect_files(ROOT))
    total = len(all_files)
    print(f"Намерени файлове: {total}")

    for i, f in enumerate(all_files, 1):
        rel = relative(f)
        ext = f.suffix.lower()

        if i % 50 == 0:
            print(f"  {i}/{total}...")

        # ── Заглавие на файла
        lines_out.append(f"\n{'─' * 50}")
        lines_out.append(f"FILE: {rel}")
        lines_out.append(f"{'─' * 50}")

        # ── Layout XML — ограничаваме броя
        is_layout = "layout" in rel.lower() and ext == ".xml"
        if is_layout:
            layout_count += 1
            if layout_count > LAYOUT_LIMIT:
                lines_out.append(f"[ПРОПУСНАТ layout #{layout_count} — над лимита {LAYOUT_LIMIT}]")
                continue

        # ── Kotlin/Java — skeleton режим за модели/репозитории
        if ext in {".kt", ".java"} and f.name not in PRIORITY_FILES:
            try:
                src = f.read_text(encoding="utf-8", errors="replace")
            except Exception as e:
                lines_out.append(f"[ГРЕШКА: {e}]")
                continue

            # За дълги файлове → само сигнатури
            if src.count("\n") > 150:
                src_clean = strip_comments_kotlin(src)
                sig = extract_kotlin_signatures(src_clean)
                lines_out.append("[РЕЖИМ: само сигнатури (файлът е дълъг)]")
                lines_out.append(compact_blank_lines(sig))
            else:
                lines_out.append(read_file_compact(f))
            continue

        # ── Всички останали текстови файлове
        if ext in CODE_EXTENSIONS or f.name in PRIORITY_FILES:
            lines_out.append(read_file_compact(f))
        else:
            size = f.stat().st_size
            lines_out.append(f"[БИНАРЕН/НЕПОЗНАТ: {size:,} байта]")

    # ── СТАТИСТИКА ──────────────────────────────
    lines_out.append("\n" + "═" * 60)
    lines_out.append("СТАТИСТИКА")
    lines_out.append("═" * 60)

    ext_counts: dict[str, int] = {}
    total_code_lines = 0
    for f in all_files:
        ext_counts[f.suffix.lower()] = ext_counts.get(f.suffix.lower(), 0) + 1
        if f.suffix.lower() in {".kt", ".java"}:
            try:
                total_code_lines += sum(1 for _ in f.open(encoding="utf-8", errors="replace"))
            except Exception:
                pass

    lines_out.append(f"Общо файлове (без binary/build): {total}")
    lines_out.append(f"Редове Kotlin/Java код: ~{total_code_lines:,}")
    for ext, cnt in sorted(ext_counts.items(), key=lambda x: -x[1]):
        lines_out.append(f"  {ext or '(без разш.)':15} {cnt}")

    # ── ЗАПИС ───────────────────────────────────
    output = "\n".join(lines_out)
    OUT.write_text(output, encoding="utf-8")

    size_kb = OUT.stat().st_size / 1024
    print(f"\n✓ Записано в: {OUT}")
    print(f"  Размер    : {size_kb:.1f} KB")
    print(f"  Редове    : {output.count(chr(10)):,}")

if __name__ == "__main__":
    main()

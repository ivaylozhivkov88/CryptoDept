"""
CryptoDept Project Collector v4.0 (FINAL)
==========================================
Адаптиран специално за CryptoDept проекта.
Базиран на v3 + всички научени уроци от Tarot работата.

Четири режима на работа:

  python collect_cryptodept.py skeleton    # Само структура (бърз преглед)
  python collect_cryptodept.py progress    # Прогрес report — кои промпти са изпълнени
  python collect_cryptodept.py critical    # Skeleton + критичните файлове
  python collect_cryptodept.py full        # Всичко (с auto-chunking)
  python collect_cryptodept.py             # По подразбиране = full

Как да го ползваш:
1. Постави този скрипт в D:\\ (или където искаш)
2. Отвори PowerShell/CMD в директорията на скрипта
3. Изпълни: python collect_cryptodept.py full
4. Ще получиш CryptoDept_DUMP_FULL.txt в същата директория

Промени спрямо v3:
- ПЪЛЕН progress check за всички 30 промпта от MASTERPLAN_v5
- Включва build.gradle.kts, libs.versions.toml, proguard-rules.pro (които project_summary.txt
  пропуска — критични за оценка!)
- По-добра detection за CryptoDept-специфични файлове
- Stats per architecture layer
- Auto-chunking при full mode
- Secret scanner (включва Anthropic, OpenAI, Etherscan, CoinGecko ключове)
"""

import os
import sys
import re
from pathlib import Path
from datetime import datetime
from collections import defaultdict

# ==========================================================
# КОНФИГУРАЦИЯ
# ==========================================================

ROOT_DIR = r"D:\CryptoDept"
OUTPUT_DIR = "."
PROJECT_NAME = "CryptoDept"

MAX_OUTPUT_SIZE_BYTES = 5 * 1024 * 1024
MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024

TEXT_EXTENSIONS = {
    # Android / Java / Kotlin
    ".java", ".kt", ".kts",
    # XML
    ".xml",
    # Gradle
    ".gradle", ".pro",
    # Properties / config
    ".properties", ".cfg", ".conf", ".ini",
    # JSON / YAML / TOML
    ".json", ".yaml", ".yml", ".toml",
    # Web / scripts
    ".html", ".css", ".js", ".ts", ".jsx", ".tsx",
    ".py", ".sh", ".bat", ".ps1",
    # Текстови
    ".txt", ".md", ".markdown", ".rst",
    # SQL
    ".sql",
    # Misc
    ".env", ".gitignore", ".editorconfig",
}

NO_EXT_TEXT_FILES = {
    "Dockerfile", "Makefile", "LICENSE", "README", "CHANGELOG",
    "gradlew",
}

EXCLUDED_DIRS = {
    "build", ".gradle", ".idea", ".git", ".kotlin",
    "node_modules", "__pycache__", ".dart_tool",
    "captures", ".cxx", "out",
    "external_libs", ".gradle_old",
}

EXCLUDED_FILES = {
    ".DS_Store", "Thumbs.db", "local.properties",
    "google-services.json",
    "release.jks", "release.keystore", "signing_key",
    "keystore.properties",
}

AUTOGEN_PATTERNS = [
    re.compile(r"^R\.java$"),
    re.compile(r"^BuildConfig\.java$"),
    re.compile(r"^.*_Impl\.java$"),
    re.compile(r"^Hilt_.*\.java$"),
    re.compile(r"^.*_HiltModules\.java$"),
    re.compile(r"^.*_Factory\.java$"),
    re.compile(r"^.*_MembersInjector\.java$"),
    re.compile(r"^Dagger.*Component\.java$"),
]

AUTOGEN_PATH_SUBSTRINGS = [
    "merged-not-compiled-resources",
    "mergeDebugResources", "mergeReleaseResources",
    "packageDebugResources", "packageReleaseResources",
    "lint-cache", "lint_vital_partial_results", "lint_vital_report_lint_model",
    "incremental",
    "merged_manifest", "merged_manifests",
    "packaged_manifests", "packaged_res",
    "generated/res", "generated\\res",
    "intermediates",
    "outputs/apk", "outputs\\apk",
    "test-results",
    "tmp/", "tmp\\",
    ".transforms",
]

CRITICAL_FILES_REGEX = [
    re.compile(r"build\.gradle(\.kts)?$"),
    re.compile(r"settings\.gradle(\.kts)?$"),
    re.compile(r"AndroidManifest\.xml$"),
    re.compile(r"gradle\.properties$"),
    re.compile(r"libs\.versions\.toml$"),
    re.compile(r"proguard-rules\.pro$"),
    re.compile(r"detekt\.yml$"),
    re.compile(r"detekt-baseline\.xml$"),
    re.compile(r"network_security_config\.xml$"),
    re.compile(r"remote_config_defaults\.xml$"),
    re.compile(r"backup_rules\.xml$"),
    re.compile(r"data_extraction_rules\.xml$"),
    re.compile(r"shortcuts\.xml$"),
    re.compile(r".*Application\.kt$"),
    re.compile(r"MainActivity\.kt$"),
    re.compile(r".*Module\.kt$"),
    re.compile(r".*Repository\.kt$"),
    re.compile(r".*RepositoryImpl\.kt$"),
    re.compile(r".*ViewModel\.kt$"),
    re.compile(r".*UseCase\.kt$"),
    re.compile(r".*Database\.kt$"),
    re.compile(r".*Dao\.kt$"),
    re.compile(r".*Entity\.kt$"),
    re.compile(r"README\.md$"),
    re.compile(r".*\.md$"),
]

API_KEY_PATTERNS = [
    (re.compile(r"AIza[A-Za-z0-9_\-]{30,}"), "Google/Firebase API key"),
    (re.compile(r"sk-[A-Za-z0-9]{32,}"), "OpenAI/Anthropic API key"),
    (re.compile(r"sk-ant-[A-Za-z0-9_\-]{50,}"), "Anthropic Claude API key"),
    (re.compile(r"sk-proj-[A-Za-z0-9_\-]{50,}"), "OpenAI project key"),
    (re.compile(r"Bearer\s+[A-Za-z0-9_\-\.]{30,}"), "Bearer token"),
    (re.compile(r"[A-Z0-9]{34}"), "Possible Etherscan/Helius hex key"),
    (re.compile(r"CG-[A-Za-z0-9]{32,}"), "CoinGecko Pro API key"),
]

# ==========================================================
# PROGRESS CHECK PATTERNS — за CryptoDept Masterplan v5
# ==========================================================

PROGRESS_PROMPTS = [
    # ФАЗА A — Existing
    {"id": "#127", "name": "Custom Composite Alerts", "phase": "A — Existing",
     "files": [r"CompositeAlert.*\.kt$", r"AlertEvaluat.*\.kt$"]},
    {"id": "#140", "name": "Heatmap Screensaver + Cycle", "phase": "A — Existing",
     "files": [r"Heatmap.*\.kt$", r"Treemap.*\.kt$", r"ScreensaverCycle.*\.kt$"]},

    # ФАЗА B — Security
    {"id": "#200", "name": "API Keys → BuildConfig", "phase": "B — Security",
     "files": [r"build\.gradle\.kts$"], "needs_content": "BuildConfig.GEMINI_API_KEY"},
    {"id": "#201", "name": "Encrypted Local Storage", "phase": "B — Security",
     "files": [r"SecurePrefs.*\.kt$", r"PreferencesService\.kt$"]},
    {"id": "#202", "name": "Network Security + Pinning", "phase": "B — Security",
     "files": [r"network_security_config\.xml$"]},
    {"id": "#210", "name": "ProGuard / R8", "phase": "B — Security",
     "files": [r"proguard-rules\.pro$"], "min_size": 1000},
    {"id": "#220", "name": "Root / Tamper Detection", "phase": "B — Security",
     "files": [r"RootDetect.*\.kt$", r"TamperCheck.*\.kt$"]},

    # ФАЗА C — Architecture
    {"id": "#270", "name": "Hilt DI Migration", "phase": "C — Architecture",
     "files": [r".*Module\.kt$"], "min_count": 5},
    {"id": "#280", "name": "Compose Migration", "phase": "C — Architecture",
     "files": [r".*Screen\.kt$"], "min_count": 20},

    # ФАЗА D — Testing + Crash
    {"id": "#310", "name": "Unit Tests Foundation", "phase": "D — Testing",
     "files": [r".*Test\.kt$"], "min_count": 5},
    {"id": "#320", "name": "Firebase Crashlytics + Analytics", "phase": "D — Testing",
     "files": [r"FirebaseModule\.kt$", r"AnalyticsService\.kt$", r"FirebaseAnalytics.*\.kt$"]},
    {"id": "#340", "name": "Production Stability", "phase": "D — Testing",
     "files": [r"RetryInterceptor\.kt$", r"RateLimitInterceptor\.kt$"]},

    # ФАЗА E — UX
    {"id": "#330", "name": "Welcome Screen + Onboarding", "phase": "E — UX",
     "files": [r"OnboardingScreen\.kt$", r"BootSequence.*\.kt$"]},
    {"id": "#331", "name": "Empty/Loading/Error States", "phase": "E — UX",
     "files": [r"EmptyState\.kt$", r"ErrorState\.kt$", r"Skeleton.*\.kt$"]},
    {"id": "#332", "name": "Smart Defaults + Disclosure", "phase": "E — UX",
     "files": [r"ProGate\.kt$"]},

    # ФАЗА F — Performance
    {"id": "#230", "name": "Coil Migration", "phase": "F — Performance",
     "files": [r"CoinIconLoader\.kt$", r"ImageModule\.kt$"]},
    {"id": "#240", "name": "Database Performance", "phase": "F — Performance",
     "files": [r"CryptoDatabase\.kt$"]},
    {"id": "#241", "name": "Compose Performance", "phase": "F — Performance",
     "files": [r".*Screen\.kt$"], "min_count": 20},
    {"id": "#250", "name": "Code Quality (Detekt + Ktlint)", "phase": "F — Performance",
     "files": [r"detekt\.yml$", r"detekt-baseline\.xml$"]},
    {"id": "#260", "name": "Error Handling Layer", "phase": "F — Performance",
     "files": [r"CryptoResult\.kt$", r"ErrorMessageMapper\.kt$", r"DomainError\.kt$"]},

    # ФАЗА G — Backend Prep
    {"id": "#300", "name": "API Abstraction Layer", "phase": "G — Backend Prep",
     "files": [r"AIProviderRouter\.kt$", r"EndpointsConfig\.kt$", r"AuthInterceptor\.kt$"]},
    {"id": "#301", "name": "Subscription Architecture", "phase": "G — Backend Prep",
     "files": [r"BillingService\.kt$", r"PaywallScreen\.kt$", r"ProGate\.kt$"]},
    {"id": "#302", "name": "Remote Config", "phase": "G — Backend Prep",
     "files": [r"RemoteConfigService\.kt$", r"remote_config_defaults\.xml$"]},

    # ФАЗА H — Free APIs
    {"id": "#150", "name": "Free Whale Tracking", "phase": "H — Free APIs",
     "files": [r"EtherscanWhaleClient\.kt$", r"HeliusWhaleClient\.kt$", r"MempoolWhaleClient\.kt$"]},
    {"id": "#151", "name": "Free News & Sentiment", "phase": "H — Free APIs",
     "files": [r"RssNewsParser\.kt$", r"RedditClient\.kt$", r"LocalSentimentScorer\.kt$"]},

    # ФАЗА I — Content
    {"id": "#500", "name": "Content Generation Templates", "phase": "I — Content",
     "files": [r"DailyRecapPromptBuilder\.kt$", r"VideoPromptBuilder\.kt$",
               r"ThumbnailPromptBuilder\.kt$", r"FacebookPostPromptBuilder\.kt$",
               r"WhaleNarratorPromptBuilder\.kt$", r"NewsletterPromptBuilder\.kt$",
               r"ContentStudioScreen\.kt$"]},

    # ФАЗА J — Advanced
    {"id": "#600", "name": "Voice Commands", "phase": "J — Advanced",
     "files": [r"SpeechManager\.kt$", r"VoiceCommandParser\.kt$", r"TerminalTtsService\.kt$"]},
    {"id": "#610", "name": "Widget Enhancements", "phase": "J — Advanced",
     "files": [r"CryptoDeptWidget\.kt$", r"WidgetConfigActivity\.kt$"]},
    {"id": "#620", "name": "iOS-style Polish", "phase": "J — Advanced",
     "files": [r"HapticService\.kt$", r"GlitchEffect\.kt$", r"TerminalAudio.*\.kt$"]},
    {"id": "#630", "name": "Achievement System", "phase": "J — Advanced",
     "files": [r"AchievementEngine\.kt$", r"Achievement\.kt$", r"AchievementsScreen\.kt$"]},
]

# ==========================================================
# ЛОГИКА
# ==========================================================

def is_autogen_path(path_str):
    p = path_str.replace("\\", "/")
    for sub in AUTOGEN_PATH_SUBSTRINGS:
        if sub.replace("\\", "/") in p:
            return True
    return False


def is_autogenerated(filename):
    for pattern in AUTOGEN_PATTERNS:
        if pattern.match(filename):
            return True
    return False


def is_critical_file(rel_path):
    for pattern in CRITICAL_FILES_REGEX:
        if pattern.search(rel_path):
            return True
    return False


def is_text_file(filepath):
    if filepath.name in EXCLUDED_FILES:
        return False
    if is_autogenerated(filepath.name):
        return False
    if filepath.suffix.lower() in TEXT_EXTENSIONS:
        return True
    if filepath.name in NO_EXT_TEXT_FILES:
        return True
    return False


def should_skip_dir(dirname):
    return dirname in EXCLUDED_DIRS


def human_size(num_bytes):
    n = float(num_bytes)
    for unit in ["B", "KB", "MB", "GB"]:
        if n < 1024.0:
            return f"{n:.2f} {unit}"
        n /= 1024.0
    return f"{n:.2f} TB"


def collect_all_files(root):
    """Връща списък от всички текстови файлове, които не са autogen."""
    result = []
    for current_root, dirs, files in os.walk(root):
        dirs[:] = [d for d in dirs if not should_skip_dir(d)]
        if is_autogen_path(current_root):
            continue
        for f in files:
            if f in EXCLUDED_FILES:
                continue
            if is_autogenerated(f):
                continue
            full_path = Path(current_root) / f
            if is_autogen_path(str(full_path)):
                continue
            if is_text_file(full_path):
                result.append(full_path)
    return result


def collect_tree(root):
    lines = []
    for current_root, dirs, files in os.walk(root):
        dirs[:] = sorted([d for d in dirs if not should_skip_dir(d)])
        if is_autogen_path(current_root):
            continue
        files = sorted([f for f in files if f not in EXCLUDED_FILES])

        rel = Path(current_root).relative_to(root)
        depth = 0 if str(rel) == "." else len(rel.parts)
        indent = "│   " * depth
        folder_name = root.name if str(rel) == "." else rel.parts[-1]
        lines.append(f"{indent}📁 {folder_name}/")

        for f in files:
            file_path = Path(current_root) / f
            if is_autogen_path(str(file_path)):
                continue
            try:
                size = file_path.stat().st_size
                size_str = human_size(size)
            except OSError:
                size_str = "?"
            marker = ""
            if is_autogenerated(f):
                marker = " [AUTOGEN]"
            elif is_critical_file(str(file_path.relative_to(root)).replace("\\", "/")):
                marker = " [CRITICAL]"
            lines.append(f"{indent}│   📄 {f}  ({size_str}){marker}")
    return lines


def scan_for_secrets(root):
    findings = []
    for file_path in collect_all_files(root):
        try:
            if file_path.stat().st_size > MAX_FILE_SIZE_BYTES:
                continue
            with open(file_path, "r", encoding="utf-8", errors="ignore") as fh:
                content = fh.read()
            for pattern, label in API_KEY_PATTERNS:
                matches = pattern.findall(content)
                if matches:
                    rel = file_path.relative_to(root)
                    for match in matches[:3]:
                        preview = match[:20] + "..." if len(match) > 20 else match
                        findings.append((str(rel), label, preview))
        except Exception:
            continue
    return findings


def progress_check(root):
    """Сканира за следи от изпълнени промпти."""
    all_files = collect_all_files(root)
    file_paths_with_size = []
    for f in all_files:
        try:
            size = f.stat().st_size
        except OSError:
            size = 0
        rel_path = str(f.relative_to(root)).replace("\\", "/")
        file_paths_with_size.append((rel_path, size, f))

    results = []
    for prompt in PROGRESS_PROMPTS:
        matched_files = []
        unique_patterns_matched = 0

        for pattern_str in prompt["files"]:
            pattern = re.compile(pattern_str)
            pattern_matched_at_least_once = False
            for fp, size, full_path in file_paths_with_size:
                fname = fp.split("/")[-1]
                if pattern.search(fname) or pattern.search(fp):
                    # Min size check (за proguard rules)
                    if "min_size" in prompt and size < prompt["min_size"]:
                        continue
                    # Content check (за #200)
                    if "needs_content" in prompt:
                        try:
                            with open(full_path, "r", encoding="utf-8", errors="ignore") as fh:
                                if prompt["needs_content"] not in fh.read():
                                    continue
                        except Exception:
                            continue
                    matched_files.append(fp)
                    pattern_matched_at_least_once = True
            if pattern_matched_at_least_once:
                unique_patterns_matched += 1

        # Дедуп
        matched_files = list(dict.fromkeys(matched_files))

        # Determine done state
        min_count = prompt.get("min_count", 1)
        expected_patterns = len(prompt["files"])

        if "min_count" in prompt:
            is_done = len(matched_files) >= min_count
        elif expected_patterns > 1:
            # При множество patterns — done ако поне expected-1 са намерени
            is_done = unique_patterns_matched >= max(1, expected_patterns - 1)
        else:
            is_done = len(matched_files) > 0

        results.append({
            "id": prompt["id"],
            "name": prompt["name"],
            "phase": prompt["phase"],
            "matched_count": len(matched_files),
            "matched_files": matched_files[:5],
            "is_done": is_done,
        })

    return results


def folder_stats(root):
    """Брои файлове по основните директории."""
    all_files = collect_all_files(root)
    stats = defaultdict(lambda: {"count": 0, "size": 0})

    for f in all_files:
        rel = str(f.relative_to(root)).replace("\\", "/")
        category = "other"
        if "src/main/java/com/cryptodept/data/api" in rel:
            category = "data: API clients"
        elif "src/main/java/com/cryptodept/data/repository" in rel:
            category = "data: Repository impls"
        elif "src/main/java/com/cryptodept/data/db" in rel:
            category = "data: Database (Room)"
        elif "src/main/java/com/cryptodept/data" in rel:
            category = "data: other"
        elif "src/main/java/com/cryptodept/domain/usecase" in rel:
            category = "domain: UseCases"
        elif "src/main/java/com/cryptodept/domain/repository" in rel:
            category = "domain: Repository interfaces"
        elif "src/main/java/com/cryptodept/domain/model" in rel:
            category = "domain: Models"
        elif "src/main/java/com/cryptodept/domain" in rel:
            category = "domain: other"
        elif "src/main/java/com/cryptodept/ui" in rel:
            category = "ui: Composables"
        elif "src/main/java/com/cryptodept/viewmodel" in rel:
            category = "viewmodels"
        elif "src/main/java/com/cryptodept/di" in rel:
            category = "DI modules"
        elif "src/main/java/com/cryptodept/service" in rel:
            category = "services"
        elif "src/main/java/com/cryptodept/util" in rel:
            category = "utilities"
        elif "src/main/java/com/cryptodept/widget" in rel:
            category = "widget"
        elif "src/test" in rel:
            category = "unit tests"
        elif "src/androidTest" in rel:
            category = "instrumentation tests"
        elif rel.endswith(".md"):
            category = "documentation"
        elif "res/values" in rel and "strings" in rel:
            category = "translations (strings.xml)"
        elif "res/" in rel:
            category = "resources (xml)"
        elif rel.endswith(".gradle.kts") or rel.endswith(".kts") or rel.endswith(".toml") or rel.endswith(".pro"):
            category = "build config"

        try:
            size = f.stat().st_size
        except OSError:
            size = 0
        stats[category]["count"] += 1
        stats[category]["size"] += size

    return dict(stats)


def collect_file_contents(root, mode):
    sections = []
    stats = {
        "total_files_seen": 0,
        "text_files_read": 0,
        "skipped_critical_mode": 0,
        "binary_or_skipped": 0,
        "too_large_skipped": 0,
        "autogen_skipped": 0,
        "read_errors": 0,
        "total_bytes_read": 0,
    }

    for current_root, dirs, files in os.walk(root):
        dirs[:] = [d for d in dirs if not should_skip_dir(d)]
        if is_autogen_path(current_root):
            continue
        files = sorted(files)

        for f in files:
            stats["total_files_seen"] += 1
            file_path = Path(current_root) / f
            rel = file_path.relative_to(root)
            rel_str = str(rel).replace("\\", "/")

            if f in EXCLUDED_FILES:
                stats["binary_or_skipped"] += 1
                continue

            if is_autogenerated(f) or is_autogen_path(str(file_path)):
                stats["autogen_skipped"] += 1
                continue

            if not is_text_file(file_path):
                stats["binary_or_skipped"] += 1
                continue

            if mode == "critical" and not is_critical_file(rel_str):
                stats["skipped_critical_mode"] += 1
                continue

            if mode == "skeleton":
                continue

            try:
                size = file_path.stat().st_size
            except OSError:
                stats["read_errors"] += 1
                continue

            if size > MAX_FILE_SIZE_BYTES:
                stats["too_large_skipped"] += 1
                sections.append(
                    f"\n\n{'='*100}\n"
                    f"⚠️  FILE TOO LARGE — SKIPPED: {rel}  ({human_size(size)})\n"
                    f"{'='*100}\n"
                )
                continue

            content = None
            for encoding in ("utf-8", "utf-8-sig", "cp1251", "latin-1"):
                try:
                    with open(file_path, "r", encoding=encoding) as fh:
                        content = fh.read()
                    break
                except UnicodeDecodeError:
                    continue
                except Exception:
                    break

            if content is None:
                stats["read_errors"] += 1
                continue

            stats["text_files_read"] += 1
            stats["total_bytes_read"] += size

            sections.append(
                f"\n\n{'='*100}\n"
                f"📄 FILE: {rel}\n"
                f"📏 SIZE: {human_size(size)}  |  LINES: {content.count(chr(10)) + 1}\n"
                f"{'='*100}\n"
                f"{content}\n"
            )

    return sections, stats


def write_chunked_output(base_filename, header, sections, max_size):
    written_files = []
    base, ext = os.path.splitext(base_filename)

    total_size = sum(len(s.encode("utf-8")) for s in sections) + len(header.encode("utf-8"))

    if total_size <= max_size:
        with open(base_filename, "w", encoding="utf-8") as f:
            f.write(header)
            f.writelines(sections)
        written_files.append(base_filename)
        return written_files

    chunk_index = 1
    current_chunk = [header]
    current_size = len(header.encode("utf-8"))

    for section in sections:
        section_size = len(section.encode("utf-8"))
        if current_size + section_size > max_size and len(current_chunk) > 1:
            path = f"{base}_part{chunk_index}{ext}"
            with open(path, "w", encoding="utf-8") as f:
                f.writelines(current_chunk)
            written_files.append(path)
            chunk_index += 1
            current_chunk = [f"=== CONTINUED FROM PART {chunk_index - 1} ===\n\n"]
            current_size = len(current_chunk[0].encode("utf-8"))
        current_chunk.append(section)
        current_size += section_size

    if current_chunk:
        path = f"{base}_part{chunk_index}{ext}"
        with open(path, "w", encoding="utf-8") as f:
            f.writelines(current_chunk)
        written_files.append(path)

    return written_files


def main():
    mode = "full"
    if len(sys.argv) > 1:
        mode = sys.argv[1].lower()
    if mode not in ("skeleton", "progress", "critical", "full"):
        print(f"❌ Невалиден режим: {mode}")
        print("Използване: python collect_cryptodept.py [skeleton|progress|critical|full]")
        sys.exit(1)

    root = Path(ROOT_DIR)
    if not root.exists():
        print(f"❌ Директорията {ROOT_DIR} не съществува!")
        sys.exit(1)
    if not root.is_dir():
        print(f"❌ {ROOT_DIR} не е директория!")
        sys.exit(1)

    output_file = f"{PROJECT_NAME}_DUMP_{mode.upper()}.txt"

    print(f"🔍 Сканирам {ROOT_DIR} в режим: {mode.upper()}")

    # ==========================================================
    # PROGRESS MODE
    # ==========================================================
    if mode == "progress":
        print("📊 Изпълнявам progress check...")
        results = progress_check(root)
        secrets = scan_for_secrets(root)
        f_stats = folder_stats(root)

        lines = []
        lines.append("=" * 100)
        lines.append(f"{PROJECT_NAME.upper()} — PROGRESS REPORT")
        lines.append(f"Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        lines.append("=" * 100)
        lines.append("")

        # Folder stats
        lines.append("=" * 100)
        lines.append("📁 СТРУКТУРА ПО СЛОЕВЕ")
        lines.append("=" * 100)
        for cat in sorted(f_stats.keys()):
            data = f_stats[cat]
            lines.append(f"  {cat:35s} → {data['count']:4d} files  ({human_size(data['size'])})")
        total_count = sum(d["count"] for d in f_stats.values())
        total_size = sum(d["size"] for d in f_stats.values())
        lines.append(f"  {'TOTAL':35s} → {total_count:4d} files  ({human_size(total_size)})")
        lines.append("")

        # Secrets
        if secrets:
            lines.append("=" * 100)
            lines.append("🚨 НАМЕРЕНИ ПОТЕНЦИАЛНИ SECRETS — ПРОВЕРИ ВЕДНАГА!")
            lines.append("=" * 100)
            for path, label, preview in secrets:
                lines.append(f"  ⚠️  {path}  →  {label}: {preview}")
            lines.append("")
        else:
            lines.append("✅ Няма открити hardcoded secrets в кода.")
            lines.append("")

        # Progress per phase
        lines.append("=" * 100)
        lines.append("📋 PROGRESS PER PHASE")
        lines.append("=" * 100)
        lines.append("")

        phases = {}
        for r in results:
            phases.setdefault(r["phase"], []).append(r)

        total_done = 0
        total_count_p = 0
        for phase in sorted(phases.keys()):
            lines.append(f"\n{'─' * 80}")
            lines.append(f"  {phase}")
            lines.append(f"{'─' * 80}")
            for r in phases[phase]:
                marker = "✅" if r["is_done"] else "❌"
                if r["is_done"]:
                    total_done += 1
                total_count_p += 1
                lines.append(f"  {marker} {r['id']:6s} {r['name']:40s}  ({r['matched_count']} files matched)")
                if r["matched_files"]:
                    for mf in r["matched_files"][:3]:
                        lines.append(f"        → {mf}")

        lines.append("")
        lines.append("=" * 100)
        pct = 100 * total_done / total_count_p if total_count_p > 0 else 0
        lines.append(f"📊 SUMMARY: {total_done}/{total_count_p} prompts completed ({pct:.1f}%)")
        lines.append("=" * 100)

        with open(output_file, "w", encoding="utf-8") as f:
            f.write("\n".join(lines))

        print()
        print("=" * 60)
        print(f"✅ ГОТОВО! Прочети {output_file}")
        print("=" * 60)
        print(f"📊 Промпти изпълнени: {total_done}/{total_count_p} ({pct:.1f}%)")
        if secrets:
            print(f"🚨 Намерени secrets: {len(secrets)}")
        print(f"📁 Общо файлове: {total_count}")
        print("=" * 60)
        return

    # ==========================================================
    # SKELETON / CRITICAL / FULL modes
    # ==========================================================
    print("📁 Изграждам структура на проекта ...")
    tree_lines = collect_tree(root)

    print("🔐 Сканирам за hardcoded secrets ...")
    secrets = scan_for_secrets(root)

    print("📊 Изчислявам folder stats ...")
    f_stats = folder_stats(root)

    content_sections = []
    stats = {}
    if mode == "skeleton":
        print("📋 Skeleton mode — пропускам съдържанието")
        stats = {
            "total_files_seen": 0, "text_files_read": 0, "binary_or_skipped": 0,
            "too_large_skipped": 0, "autogen_skipped": 0, "read_errors": 0,
            "total_bytes_read": 0, "skipped_critical_mode": 0,
        }
        for current_root, dirs, files in os.walk(root):
            dirs[:] = [d for d in dirs if not should_skip_dir(d)]
            if is_autogen_path(current_root):
                continue
            for f in files:
                stats["total_files_seen"] += 1
    else:
        print(f"📖 Чета съдържанието ({mode} mode) ...")
        content_sections, stats = collect_file_contents(root, mode)

    # Build header
    header_lines = []
    header_lines.append("=" * 100 + "\n")
    header_lines.append(f"{PROJECT_NAME.upper()} — PROJECT DUMP  ({mode.upper()} MODE)\n")
    header_lines.append("=" * 100 + "\n")
    header_lines.append(f"Generated:        {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
    header_lines.append(f"Root directory:   {ROOT_DIR}\n")
    header_lines.append(f"Mode:             {mode}\n")
    header_lines.append("\n")

    # Folder stats
    header_lines.append("=" * 100 + "\n")
    header_lines.append("📁 СТРУКТУРА ПО СЛОЕВЕ\n")
    header_lines.append("=" * 100 + "\n")
    for cat in sorted(f_stats.keys()):
        data = f_stats[cat]
        header_lines.append(f"  {cat:35s} → {data['count']:4d} files  ({human_size(data['size'])})\n")
    total_count = sum(d["count"] for d in f_stats.values())
    total_size = sum(d["size"] for d in f_stats.values())
    header_lines.append(f"  {'TOTAL':35s} → {total_count:4d} files  ({human_size(total_size)})\n")
    header_lines.append("\n")

    # Stats
    header_lines.append("STATS:\n")
    for key, value in stats.items():
        if "bytes" in key:
            header_lines.append(f"  {key}: {human_size(value)}\n")
        else:
            header_lines.append(f"  {key}: {value}\n")
    header_lines.append("\n")

    # Secrets
    if secrets:
        header_lines.append("=" * 100 + "\n")
        header_lines.append("🚨 POTENTIAL SECRETS — ПРОВЕРИ!\n")
        header_lines.append("=" * 100 + "\n")
        for path, label, preview in secrets:
            header_lines.append(f"  ⚠️  {path}  →  {label}: {preview}\n")
        header_lines.append("\n")
    else:
        header_lines.append("✅ Няма открити hardcoded secrets.\n\n")

    header_lines.append("=" * 100 + "\n")
    header_lines.append("PROJECT TREE\n")
    header_lines.append("=" * 100 + "\n")
    header_lines.extend(line + "\n" for line in tree_lines)
    header_lines.append("\n")

    if mode != "skeleton":
        header_lines.append("=" * 100 + "\n")
        header_lines.append("FILE CONTENTS\n")
        header_lines.append("=" * 100 + "\n")

    header_str = "".join(header_lines)

    print(f"💾 Записвам в {output_file} ...")
    written_files = write_chunked_output(output_file, header_str, content_sections, MAX_OUTPUT_SIZE_BYTES)

    print()
    print("=" * 60)
    print("✅ ГОТОВО!")
    print("=" * 60)
    print(f"📋 Режим:                 {mode.upper()}")
    print(f"📄 Изходни файлове:       {len(written_files)}")
    for wf in written_files:
        try:
            size = os.path.getsize(wf)
            print(f"     • {wf}  ({human_size(size)})")
        except OSError:
            print(f"     • {wf}  (?)")
    if stats.get("text_files_read"):
        print(f"📊 Прочетени файлове:     {stats['text_files_read']}")
    if stats.get("autogen_skipped"):
        print(f"⏭️  Autogen пропуснати:   {stats['autogen_skipped']}")
    if stats.get("skipped_critical_mode"):
        print(f"⏭️  Non-critical:         {stats['skipped_critical_mode']}")
    if stats.get("binary_or_skipped"):
        print(f"⏭️  Binary пропуснати:    {stats['binary_or_skipped']}")
    if stats.get("too_large_skipped"):
        print(f"⚠️  Прекалено големи:     {stats['too_large_skipped']}")
    if stats.get("read_errors"):
        print(f"❌ Грешки при четене:    {stats['read_errors']}")
    if secrets:
        print(f"\n🚨 ВНИМАНИЕ: Намерени са {len(secrets)} потенциални secrets!")
    print("=" * 60)


if __name__ == "__main__":
    main()
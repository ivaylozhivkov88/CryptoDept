import os
import re

# Patterns to scan for
PATTERNS = {
    "Google/Gemini API Key": r"AIzaSy[A-Za-z0-9_-]{33}",
    "OpenAI API Key": r"sk-[a-zA-Z0-9]{48}",
    "Bearer Token": r"Bearer\s+[A-Za-z0-9]{32,}",
    "Generic Hex Secret": r"[a-fA-F0-9]{32,64}",
    "Common API Key Variable": r"(?i)(api_key|secret|token|password)\s*=\s*['\"]([^'\"]+)['\"]"
}

IGNORE_DIRS = {'.git', '.gradle', '.idea', 'build', 'node_modules', 'captures'}
EXTENSIONS = {'.kt', '.java', '.xml', '.gradle', '.kts', '.properties'}

def audit():
    results = []
    project_root = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))

    print(f"--- STARTING SECRETS AUDIT IN {project_root} ---")

    for root, dirs, files in os.walk(project_root):
        # Skip ignored directories
        dirs[:] = [d for d in dirs if d not in IGNORE_DIRS]

        for file in files:
            if any(file.endswith(ext) for ext in EXTENSIONS):
                path = os.path.join(root, file)
                try:
                    with open(path, 'r', encoding='utf-8', errors='ignore') as f:
                        content = f.read()
                        for name, pattern in PATTERNS.items():
                            matches = re.finditer(pattern, content)
                            for match in matches:
                                # For common variables, we only care about the value part
                                if name == "Common API Key Variable":
                                    val = match.group(2)
                                    if len(val) < 10: continue # Likely not a real secret

                                line_no = content.count('\n', 0, match.start()) + 1
                                snippet = match.group(0)
                                if len(snippet) > 50: snippet = snippet[:47] + "..."

                                results.append(f"[{name}] Found in {os.path.relpath(path, project_root)} at line {line_no}: {snippet}")
                except Exception as e:
                    print(f"Could not read {path}: {e}")

    report_path = os.path.join(project_root, "secrets_audit_report.txt")
    with open(report_path, "w", encoding="utf-8") as f:
        f.write("\n".join(results))

    print(f"--- AUDIT COMPLETE. Found {len(results)} potential secrets. ---")
    print(f"Report saved to: {report_path}")

if __name__ == "__main__":
    audit()

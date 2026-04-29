import os

PROJECT_DIR = r"D:\CryptoDept\app\src\main\java\com\cryptodept"
OUTPUT_FILE = r"D:\CryptoDept\cryptodept_sources.txt"

def collect_kotlin_files(root_dir):
    kotlin_files = []
    for dirpath, dirnames, filenames in os.walk(root_dir):
        dirnames[:] = [d for d in dirnames if d != 'build']
        for filename in filenames:
            if filename.endswith(".kt"):
                kotlin_files.append(os.path.join(dirpath, filename))
    return sorted(kotlin_files)

def main():
    files = collect_kotlin_files(PROJECT_DIR)
    total = len(files)
    print(f"Found {total} Kotlin files...")

    with open(OUTPUT_FILE, "w", encoding="utf-8") as out:
        out.write(f"# CryptoDept Source Files — {total} files\n")
        out.write("=" * 80 + "\n\n")

        for i, filepath in enumerate(files, 1):
            rel_path = filepath.replace(PROJECT_DIR, "").lstrip("\\")

            out.write(f"\n{'=' * 80}\n")
            out.write(f"FILE {i}/{total}: {rel_path}\n")
            out.write(f"{'=' * 80}\n\n")

            try:
                with open(filepath, "r", encoding="utf-8") as f:
                    content = f.read()
                out.write(content)
            except Exception as e:
                out.write(f"[ERROR reading file: {e}]\n")

            out.write("\n")

            if i % 10 == 0:
                print(f"  Processed {i}/{total}...")

    size_kb = os.path.getsize(OUTPUT_FILE) / 1024
    print(f"\nDone!")
    print(f"Output: {OUTPUT_FILE}")
    print(f"Size: {size_kb:.1f} KB")
    print(f"Files: {total}")

if __name__ == "__main__":
    main()

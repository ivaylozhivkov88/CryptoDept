import os

# Основна директория
ROOT_DIR = r"D:\CryptoDept"
# Изходен файл - тук ще отиде целият код
OUTPUT_FILE = os.path.join(ROOT_DIR, "CryptoDept_TOTAL_CODE.txt")

# Разширения, които дефинират "ПЪЛЕН КОД"
# Добавил съм .kt (Kotlin), .gradle, .xml, .properties и конфигурации
EXTENSIONS = {
    '.kt', '.xml', '.gradle', '.properties', 
    '.pro', '.json', '.yml', '.yaml', '.md'
}

# Папки, които съдържат само временен кеш и бинарни боклуци (изключваме ги за бързина)
IGNORE_DIRS = {'.git', '.gradle', '.externalNativeBuild', 'build-cache', 'captures'}

def generate_total_dump():
    print(f"--- Генериране на ПЪЛЕН ДЪМП (Source + Resources) ---")
    print(f"Цел: {OUTPUT_FILE}")
    
    file_count = 0
    
    with open(OUTPUT_FILE, "w", encoding="utf-8") as outfile:
        for root, dirs, files in os.walk(ROOT_DIR):
            # Избягваме системните папки на Git и Gradle кеша
            dirs[:] = [d for d in dirs if d not in IGNORE_DIRS]
            
            for file in files:
                ext = os.path.splitext(file)[1].lower()
                
                if ext in EXTENSIONS:
                    file_path = os.path.join(root, file)
                    rel_path = os.path.relpath(file_path, ROOT_DIR)
                    
                    try:
                        # Четем съдържанието
                        with open(file_path, "r", encoding="utf-8", errors="ignore") as infile:
                            content = infile.read()
                            
                            # Добавяме ясен маркер за начало на файла
                            outfile.write(f"\n\n{'#'*100}\n")
                            outfile.write(f"### PATH: {rel_path}\n")
                            outfile.write(f"{'#'*100}\n\n")
                            
                            outfile.write(content)
                            file_count += 1
                            print(f"[+] Добавен: {rel_path}")
                            
                    except Exception as e:
                        print(f"[!] Грешка при: {rel_path} -> {e}")

    print(f"\n" + "="*50)
    print(f"ГОТОВО! Събрани са {file_count} файла.")
    print(f"Общият брой редове е в {OUTPUT_FILE}")
    print("="*50)

if __name__ == "__main__":
    generate_total_dump()
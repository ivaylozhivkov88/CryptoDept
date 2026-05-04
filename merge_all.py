import os

def merge_android_project(root_dir, output_file):
    # Разширения, които ни интересуват (код, ресурси и документация)
    valid_extensions = {'.kt', '.java', '.xml', '.gradle', '.kts', '.md', '.txt', '.properties'}
    
    # Папки, които искаме да игнорираме (Gradle, build и системни)
    ignore_dirs = {'.gradle', '.idea', '.kotlin', 'build', 'gradle'}
    
    # Файлове, които да прескочим (включително самия изходен файл)
    ignore_files = {output_file, 'merge_all.py', 'gradlew', 'gradlew.bat'}

    with open(output_file, 'w', encoding='utf-8') as outfile:
        for root, dirs, files in os.walk(root_dir):
            # Модифицираме dirs на място, за да прескочим ненужните папки
            dirs[:] = [d for d in dirs if d not in ignore_dirs]

            for file in files:
                ext = os.path.splitext(file)[1].lower()
                if ext in valid_extensions and file not in ignore_files:
                    full_path = os.path.join(root, file)
                    rel_path = os.path.relpath(full_path, root_dir)
                    
                    try:
                        with open(full_path, 'r', encoding='utf-8', errors='ignore') as infile:
                            content = infile.read()
                            
                            # Форматиране на заглавието за всеки файл
                            outfile.write(f"\n\n{'='*80}\n")
                            outfile.write(f"ФАЙЛ: {rel_path}\n")
                            outfile.write(f"{'='*80}\n\n")
                            
                            outfile.write(content)
                        print(f"Добавен: {rel_path}")
                    except Exception as e:
                        print(f"Грешка при {rel_path}: {e}")

    print(f"\n--- ГОТОВО! Проектът е обединен в {output_file} ---")

# Изпълнение
if __name__ == "__main__":
    # Скриптът ще работи в папката, в която се намира (D:/CryptoDept)
    current_directory = os.path.dirname(os.path.abspath(__file__))
    output_name = "CryptoDept_Full_Code.txt"
    
    merge_android_project(current_directory, output_name)
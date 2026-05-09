import os

def collect_android_project_info(project_path, output_file="project_summary.txt"):
    # Дефинираме разширенията, които ни интересуват
    valid_extensions = ('.kt', '.java', '.xml', '.gradle')
    # Папки, които искаме да игнорираме (за да не става файлът твърде голям)
    ignored_dirs = {'build', '.gradle', '.idea', 'androidTest', 'test', 'res/drawable', 'res/mipmap'}

    with open(output_file, 'w', encoding='utf-8') as f:
        for root, dirs, files in os.walk(project_path):
            # Филтрираме игнорираните директории
            dirs[:] = [d for d in dirs if d not in ignored_dirs]
            
            for file in files:
                if file.endswith(valid_extensions):
                    file_path = os.path.join(root, file)
                    relative_path = os.path.relpath(file_path, project_path)
                    
                    f.write(f"\n{'='*80}\n")
                    f.write(f"FILE: {relative_path}\n")
                    f.write(f"{'='*80}\n\n")
                    
                    try:
                        with open(file_path, 'r', encoding='utf-8') as code_file:
                            f.write(code_file.read())
                    except Exception as e:
                        f.write(f"Could not read file: {e}\n")
                    f.write("\n")

    print(f"Готово! Цялата информация е събрана в: {output_file}")

# Стартиране (промени пътя към твоята папка)
if __name__ == "__main__":
    path_to_project = input("Въведи пълния път до Android проекта си: ")
    collect_android_project_info(path_to_project)
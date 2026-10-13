#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import os
import glob
import re

JAVA_DIR = os.path.join(
    os.path.dirname(__file__),
    "..",
    "demo",
    "src",
    "main",
    "java",
    "goddrinksjava"
)
OUTPUT_FILE = os.path.join(
    os.path.dirname(__file__),
    "..",
    "demo",
    "src",
    "main",
    "java",
    "world",
    "eyes",
    "witness",
    "data",
    "GodDrinksJavaSource.kt"
)

def escape_for_kotlin_multiline(text: str) -> str:
    # 替换 Kotlin 三引号冲突与模板字符串引用
    escaped = text.replace('"""', '"" + "\\\"\\\"\\\"" + """')
    escaped = escaped.replace("$", "\\$")
    return escaped

def main():
    java_files = sorted(glob.glob(os.path.join(JAVA_DIR, "*.java")))
    if not java_files:
        print(f"Error: No java files found in {JAVA_DIR}")
        return

    all_files_dict = {}
    combined_texts = []

    for file_path in java_files:
        filename = os.path.basename(file_path)
        with open(file_path, "r", encoding="utf-8") as f:
            content = f.read().strip()
            all_files_dict[filename] = content
            combined_texts.append(f"// ==================== [FILE: {filename}] ====================\n" + content)

    full_combined = "\n\n".join(combined_texts)

    out_content = []
    out_content.append("package world.eyes.witness.data\n")
    out_content.append("import world.eyes.witness.director.SceneType\n")
    out_content.append("import kotlin.random.Random\n\n")
    out_content.append("object GodDrinksJavaSource {\n")
    out_content.append("    val ALL_FILES: Map<String, String> = mapOf(")

    for filename, code in all_files_dict.items():
        escaped_code = escape_for_kotlin_multiline(code)
        out_content.append(f'        "{filename}" to """{escaped_code}""".trimIndent(),\n')

    out_content.append("    )\n\n")

    escaped_full = escape_for_kotlin_multiline(full_combined)
    out_content.append(f'    val FULL_CODE: String = """{escaped_full}""".trimIndent()\n\n')

    # 生成代码段列表
    snippets = []
    for code in all_files_dict.values():
        lines = code.split("\n")
        chunk = []
        for line in lines:
            line_str = line.strip()
            if not line_str or line_str.startswith("package ") or line_str.startswith("import "):
                continue
            chunk.append(line)
            if len(chunk) >= 5:
                snippets.append("\n".join(chunk))
                chunk = []
        if chunk:
            snippets.append("\n".join(chunk))

    out_content.append("    val CODE_SNIPPETS: List<String> = listOf(\n")
    for snip in snippets:
        esc_snip = escape_for_kotlin_multiline(snip)
        out_content.append(f'        """{esc_snip}""".trimIndent(),\n')
    out_content.append("    )\n\n")

    out_content.append("""    fun getRandomSnippet(): String {
        if (CODE_SNIPPETS.isEmpty()) return "// [goddrinksjava.Runtime: running]"
        return CODE_SNIPPETS[Random.nextInt(CODE_SNIPPETS.size)]
    }

    fun getCodeForScene(scene: SceneType): String {
        return when (scene) {
            SceneType.BOOT_SEQUENCE,
            SceneType.WORLD_BOOT_INSTRUMENTAL -> ALL_FILES["World.java"] ?: FULL_CODE
            SceneType.GEOMETRY_MATH -> (ALL_FILES["Circle.java"] ?: "") + "\n\n" + (ALL_FILES["PointSet.java"] ?: "") + "\n\n" + (ALL_FILES["SineWave.java"] ?: "")
            SceneType.CURRENT_CONVERSION -> ALL_FILES["Thing.java"] ?: FULL_CODE
            SceneType.SIMULATION_EXECUTION -> (ALL_FILES["Lovable.java"] ?: "") + "\n\n" + (ALL_FILES["World.java"] ?: "")
            SceneType.BIOLOGY_EXISTENCE -> (ALL_FILES["Eggplant.java"] ?: "") + "\n\n" + (ALL_FILES["Tomato.java"] ?: "") + "\n\n" + (ALL_FILES["TabbyCat.java"] ?: "")
            SceneType.GENDER_AND_ROLES -> ALL_FILES["Thing.java"] ?: FULL_CODE
            SceneType.ISOLATION_AND_ERROR -> (ALL_FILES["Memory.java"] ?: "") + "\n\n" + (ALL_FILES["World.java"] ?: "")
            SceneType.ARGUMENT_STACK,
            SceneType.RAPID_EXECUTION_STORM,
            SceneType.DESPERATE_EXECUTION -> ALL_FILES["World.java"] ?: FULL_CODE
            SceneType.ALGEBRAIC_LOVE -> ALL_FILES["Lovable.java"] ?: FULL_CODE
            SceneType.OPEN_LOOP_SHUTDOWN -> ALL_FILES["GodDrinksJava.java"] ?: FULL_CODE
        }
    }
}
""")

    os.makedirs(os.path.dirname(OUTPUT_FILE), exist_ok=True)
    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        f.write("".join(out_content))

    print(f"Successfully generated {OUTPUT_FILE} from {len(java_files)} Java files.")

if __name__ == "__main__":
    main()
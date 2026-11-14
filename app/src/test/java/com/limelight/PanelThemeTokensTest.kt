package com.limelight

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** 面板资源只能引用 panel_* 语义色，不得写十六进制，保证主题可换、容器可换。 */
class PanelThemeTokensTest {
    private val res = File("src/main/res")
    private val hex = Regex("""(?<![&\w])#[0-9A-Fa-f]{6,8}\b""")

    private fun panelFiles(): List<File> =
        listOf("layout", "drawable", "color", "values").flatMap { dir ->
            File(res, dir).listFiles { f -> f.name.startsWith("companion_") && f.extension == "xml" }
                ?.toList().orEmpty()
        }

    @Test fun panelResourcesContainNoHardcodedColors() {
        val files = panelFiles()
        assertTrue("no companion_* resources found, test path is wrong", files.isNotEmpty())
        val offenders = files.flatMap { f ->
            f.readLines().mapIndexedNotNull { i, line ->
                // XML 注释里的说明文字不算
                if (!line.trim().startsWith("<!--") && hex.containsMatchIn(line)) "${f.name}:${i + 1}: ${line.trim()}" else null
            }
        }
        assertTrue("hardcoded colors in panel resources:\n" + offenders.joinToString("\n"), offenders.isEmpty())
    }

    @Test fun everyPanelColorReferenceIsDefined() {
        val defined = File(res, "values/panel_colors.xml").readText()
            .let { Regex("""<color name="(\w+)">""").findAll(it).map { m -> m.groupValues[1] }.toSet() }
        val used = panelFiles().flatMap { f -> Regex("""@color/(panel_\w+)""").findAll(f.readText()).map { it.groupValues[1] }.toList() }.toSet()
        assertTrue("undefined tokens: ${used - defined}", (used - defined).isEmpty())
    }
}

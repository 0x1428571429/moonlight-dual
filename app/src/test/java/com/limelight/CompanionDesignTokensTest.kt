package com.limelight

import com.limelight.ui.companion.CompanionDimens
import com.limelight.ui.companion.CompanionType
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 副屏设计规范（COMPANION_DESIGN_SPEC.md）的防回退检查。 */
class CompanionDesignTokensTest {
    private val res = File("src/main/res")
    private val src = File("src/main/java/com/limelight")

    /** 副屏 Compose 界面只能用 CompanionTheme.colors，不得写 Color(0x...)。 */
    private val companionComposeFiles = listOf(
        "gamemenu/CompanionGameMenu.kt",
        "ui/companion/CompanionDesign.kt"
    )

    @Test fun companionComposeHasNoHardcodedColors() {
        val hex = Regex("""Color\(0x[0-9A-Fa-f]+""")
        val offenders = companionComposeFiles.map { File(src, it) }.flatMap { f ->
            assertTrue("missing ${f.path}", f.exists())
            f.readLines().mapIndexedNotNull { i, line ->
                if (hex.containsMatchIn(line)) "${f.name}:${i + 1}: ${line.trim()}" else null
            }
        }
        assertTrue("hardcoded colors in companion UI:\n" + offenders.joinToString("\n"), offenders.isEmpty())
    }

    @Test fun composeTokensMatchXmlDimens() {
        val xml = File(res, "values/companion_type.xml").readText()
        val dimens = Regex("""<dimen name="(\w+)">([\d.]+)(sp|dp)</dimen>""").findAll(xml)
            .associate { it.groupValues[1] to it.groupValues[2].toFloat() }
        val expected = mapOf(
            "companion_text_hero" to CompanionType.hero.value,
            "companion_text_display" to CompanionType.display.value,
            "companion_text_value" to CompanionType.value.value,
            "companion_text_title" to CompanionType.title.value,
            "companion_text_tile" to CompanionType.tile.value,
            "companion_text_label" to CompanionType.label.value,
            "companion_text_caption" to CompanionType.caption.value,
            "companion_text_hint" to CompanionType.hint.value,
            "companion_screen_padding" to CompanionDimens.screenPadding.value,
            "companion_gap" to CompanionDimens.gap.value,
            "companion_tile_padding" to CompanionDimens.tilePadding.value,
            "companion_radius_tile" to CompanionDimens.radiusTile.value,
            "companion_radius_control" to CompanionDimens.radiusControl.value,
            "companion_radius_chip" to CompanionDimens.radiusChip.value,
            "companion_touch_min" to CompanionDimens.touchMin.value,
            "companion_header_height" to CompanionDimens.headerHeight.value,
            "companion_rail_width" to CompanionDimens.railWidth.value,
            "companion_icon_chip" to CompanionDimens.iconChip.value,
            "companion_icon" to CompanionDimens.icon.value
        )
        expected.forEach { (name, value) ->
            assertEquals("$name in companion_type.xml", value, dimens[name] ?: error("$name missing"), 0.001f)
        }
    }
}

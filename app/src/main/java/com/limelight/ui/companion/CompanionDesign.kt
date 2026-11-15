package com.limelight.ui.companion

// 副屏（双屏掌机第二块屏）设计规范的 Compose 入口。规范全文：COMPANION_DESIGN_SPEC.md。
// 颜色只来自 res/values/panel_colors.xml；字号/尺寸与 res/values/companion_type.xml 一一对应。
// 只用于双屏的副屏界面，单屏界面不使用。

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limelight.R

@Immutable
class CompanionColors(
    val background: Color,
    val glow: Color,
    val panel: Color,
    val card: Color,
    val cardRaised: Color,
    val outline: Color,
    val outlineAccent: Color,
    val accent: Color,
    val accentLight: Color,
    val icon: Color,
    val iconChip: Color,
    val selectedTop: Color,
    val selectedBottom: Color,
    val focusTop: Color,
    val focusBottom: Color,
    val track: Color,
    val switchKnobOff: Color,
    val textStrong: Color,
    val textPrimary: Color,
    val textSoft: Color,
    val textMuted: Color,
    val textOnAccent: Color,
    val ok: Color,
    val danger: Color,
    val dangerSoft: Color,
    val dangerOutline: Color
)

object CompanionTheme {
    val colors: CompanionColors
        @Composable @ReadOnlyComposable get() = CompanionColors(
            background = colorResource(R.color.panel_background),
            glow = colorResource(R.color.panel_glow),
            panel = colorResource(R.color.panel_surface),
            card = colorResource(R.color.panel_card),
            cardRaised = colorResource(R.color.panel_card_raised),
            outline = colorResource(R.color.panel_outline_strong),
            outlineAccent = colorResource(R.color.panel_outline_accent),
            accent = colorResource(R.color.panel_accent),
            accentLight = colorResource(R.color.panel_accent_light),
            icon = colorResource(R.color.panel_icon),
            iconChip = colorResource(R.color.panel_icon_chip),
            selectedTop = colorResource(R.color.panel_selected_top),
            selectedBottom = colorResource(R.color.panel_selected_bottom),
            focusTop = colorResource(R.color.panel_focus_top),
            focusBottom = colorResource(R.color.panel_focus_bottom),
            track = colorResource(R.color.panel_track),
            switchKnobOff = colorResource(R.color.panel_switch_knob_off),
            textStrong = colorResource(R.color.panel_text_strong),
            textPrimary = colorResource(R.color.panel_text_primary),
            textSoft = colorResource(R.color.panel_text_soft),
            textMuted = colorResource(R.color.panel_text_muted),
            textOnAccent = colorResource(R.color.panel_text_on_accent),
            ok = colorResource(R.color.panel_ok),
            danger = colorResource(R.color.panel_danger),
            dangerSoft = colorResource(R.color.panel_danger_soft),
            dangerOutline = colorResource(R.color.panel_danger_outline)
        )
}

/** 字号阶梯（sp）。副屏上最小可读字号是 caption 12sp；hint 只用于按键提示。 */
object CompanionType {
    val hero: TextUnit = 64.sp
    val display: TextUnit = 40.sp
    val value: TextUnit = 22.sp
    val title: TextUnit = 20.sp
    val tile: TextUnit = 16.sp
    val label: TextUnit = 14.sp
    val caption: TextUnit = 12.sp
    val hint: TextUnit = 10.sp

    /** 用于放进圆形/方形徽标里的单个字符：去掉字体内边距，按行高居中，保证视觉上下居中。 */
    val centeredGlyph = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.Both
        )
    )
}

object CompanionDimens {
    val screenPadding = 10.dp
    val gap = 8.dp
    val tilePadding = 10.dp
    val radiusTile = 12.dp
    val radiusControl = 10.dp
    val radiusChip = 7.dp
    val touchMin = 44.dp
    val headerHeight = 44.dp
    val railWidth = 92.dp
    val iconChip = 36.dp
    val icon = 20.dp

    /** 外框粗细：默认 / 选中 / 手柄焦点。 */
    val strokeDefault = 1.dp
    val strokeSelected = 1.5.dp
    val strokeFocus = 2.5.dp
}

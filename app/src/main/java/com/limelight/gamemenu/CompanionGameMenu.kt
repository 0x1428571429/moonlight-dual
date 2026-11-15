package com.limelight.gamemenu

// Game Menu layout for the companion (second) screen, e.g. AYN Thor's 3.9" bottom display.
// Full-screen, touch-first: a left rail with three pages (quick / input / video) plus
// pause and quit, and large Steam-styled tiles on the right. Submenus keep the
// standard layout; this only replaces the main page.

import android.view.HapticFeedbackConstants
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limelight.R
import com.limelight.preferences.compactResolutionLabel
import com.limelight.ui.companion.CompanionColors
import com.limelight.ui.companion.CompanionDimens
import com.limelight.ui.companion.CompanionTheme
import com.limelight.ui.companion.CompanionType
import kotlin.math.abs

internal enum class CompanionMenuPage { QUICK, INPUT, VIDEO }

private val TileShape = RoundedCornerShape(CompanionDimens.radiusTile)

/** 颜色只来自 panel_* token（COMPANION_DESIGN_SPEC.md）。 */
private val Palette: CompanionColors
    @Composable @ReadOnlyComposable get() = CompanionTheme.colors

private const val ICON_MOUSE_MODE = "mouse_mode"
private const val ICON_PAN_ZOOM = "game_menu_pan_zoom"
private const val ICON_CROWN = "crown_function_menu"
private const val ICON_PERF_OVERLAY = "game_menu_toggle_performance_overlay"
private const val ICON_PERF_STYLE = "game_menu_perf_overlay_style"
private const val ICON_VIRTUAL_CONTROLLER = "game_menu_toggle_virtual_controller"
private const val ICON_SCREEN_KEYBOARD = "game_menu_toggle_keyboard"
private const val ICON_HOST_KEYBOARD = "game_menu_toggle_host_keyboard"
private const val ICON_SEND_KEYS = "game_menu_send_keys"
private const val ICON_MICROPHONE = "microphone_input"
private const val ICON_DISCONNECT = "game_menu_disconnect"
private const val ICON_QUIT = "game_menu_disconnect_and_quit"

/** Common bitrate presets in kbps; only those within the stream's range are shown. */
private val BITRATE_PRESETS_KBPS = listOf(10_000, 20_000, 30_000, 50_000, 80_000, 150_000)

internal fun nextCompanionPage(page: CompanionMenuPage, forward: Boolean): CompanionMenuPage {
    val pages = CompanionMenuPage.entries
    val step = if (forward) 1 else pages.size - 1
    return pages[(page.ordinal + step) % pages.size]
}

/** Returns the index of [percent]'s successor in [supported], wrapping around. */
internal fun nextDisplayScalePercent(supported: List<Int>, percent: Int?): Int? {
    if (supported.isEmpty()) return null
    val current = supported.indices.minByOrNull { abs(supported[it] - (percent ?: supported.first())) } ?: 0
    return supported[(current + 1) % supported.size]
}

@Composable
internal fun CompanionGameMenu(
    state: GameMenuComposeUiState,
    callbacks: GameMenuCallbacks,
    initialFocusRequester: FocusRequester,
    initialFocusModifier: Modifier
) {
    // GameMenuTheme scales fonts by 1.3 for the phone dialog; this layout is sized for the
    // companion screen in absolute dp/sp, so undo that here.
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, density.fontScale / 1.3f)) {
        var page by rememberSaveable { mutableStateOf(CompanionMenuPage.QUICK) }
        val inputModeManager = LocalInputModeManager.current
        LaunchedEffect(page) {
            if (inputModeManager.inputMode == InputMode.Keyboard) {
                runCatching { initialFocusRequester.requestFocus() }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Palette.glow, Palette.background),
                        center = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, 0f),
                        radius = 900f
                    )
                )
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.ButtonL1 -> { page = nextCompanionPage(page, forward = false); true }
                        Key.ButtonR1 -> { page = nextCompanionPage(page, forward = true); true }
                        else -> false
                    }
                }
                .padding(CompanionDimens.screenPadding),
            verticalArrangement = Arrangement.spacedBy(CompanionDimens.gap)
        ) {
            CompanionHeader(state, callbacks)
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(CompanionDimens.gap)
            ) {
                CompanionRail(
                    page = page,
                    onPage = { page = it },
                    state = state,
                    callbacks = callbacks,
                    modifier = Modifier.width(CompanionDimens.railWidth).fillMaxHeight()
                )
                val pageModifier = Modifier.weight(1f).fillMaxHeight()
                val firstTile = Modifier.focusRequester(initialFocusRequester).then(initialFocusModifier)
                when (page) {
                    CompanionMenuPage.QUICK -> QuickPage(state, callbacks, firstTile, pageModifier)
                    CompanionMenuPage.INPUT -> InputPage(state, callbacks, firstTile, pageModifier)
                    CompanionMenuPage.VIDEO -> VideoPage(state, callbacks, firstTile, pageModifier)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Header and rail

@Composable
private fun CompanionHeader(state: GameMenuComposeUiState, callbacks: GameMenuCallbacks) {
    Row(
        modifier = Modifier.fillMaxWidth().height(CompanionDimens.headerHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CompanionDimens.gap)
    ) {
        SteamSurface(
            onClick = callbacks.onDismiss,
            modifier = Modifier.fillMaxHeight().testTag("companionBack"),
            shape = RoundedCornerShape(CompanionDimens.radiusControl)
        ) {
            Row(
                modifier = Modifier.fillMaxHeight().padding(start = 9.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ButtonGlyph("B")
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.gm_companion_back),
                    color = Palette.textPrimary, fontSize = CompanionType.label, fontWeight = FontWeight.Bold,
                    style = CompanionType.centeredGlyph.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
        Text(
            text = stringResource(R.string.gm_companion_title),
            color = Palette.textPrimary,
            fontSize = CompanionType.title,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        val bitrate = state.bitrate
        StatusChip(BitrateCardController.formatBitrateMbps(bitrate.selectedBitrateKbps), dot = true)
        StatusChip(compactResolutionLabel(bitrate.resolutionLabel))
        StatusChip("${bitrate.fpsLabel} FPS")
    }
}

/** 手柄按键徽标（圆形，字母水平+垂直居中）。 */
@Composable
private fun ButtonGlyph(letter: String) {
    Box(
        modifier = Modifier.size(22.dp).clip(CircleShape).background(Palette.textPrimary),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter,
            color = Palette.textOnAccent,
            fontSize = CompanionType.caption,
            lineHeight = CompanionType.caption,
            textAlign = TextAlign.Center,
            style = CompanionType.centeredGlyph
        )
    }
}

@Composable
private fun StatusChip(text: String, dot: Boolean = false) {
    Row(
        modifier = Modifier
            .height(26.dp)
            .clip(RoundedCornerShape(CompanionDimens.radiusChip))
            .background(Palette.card)
            .border(1.dp, Palette.outlineAccent, RoundedCornerShape(CompanionDimens.radiusChip))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (dot) {
            Box(Modifier.size(5.dp).clip(CircleShape).background(Palette.ok))
            Spacer(Modifier.width(4.dp))
        }
        Text(text, color = Palette.textSoft, fontSize = CompanionType.caption, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun CompanionRail(
    page: CompanionMenuPage,
    onPage: (CompanionMenuPage) -> Unit,
    state: GameMenuComposeUiState,
    callbacks: GameMenuCallbacks,
    modifier: Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(CompanionDimens.gap)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(18.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            KeyHint("LB")
            Text(
                " ${stringResource(R.string.gm_companion_switch_hint)} ",
                color = Palette.textMuted, fontSize = CompanionType.hint, lineHeight = 12.sp, fontWeight = FontWeight.Bold
            )
            KeyHint("RB")
        }
        RailTab(R.drawable.ic_gm_tab_quick, R.string.gm_companion_tab_quick, page == CompanionMenuPage.QUICK) {
            onPage(CompanionMenuPage.QUICK)
        }
        RailTab(R.drawable.ic_gm_tab_input, R.string.gm_companion_tab_input, page == CompanionMenuPage.INPUT) {
            onPage(CompanionMenuPage.INPUT)
        }
        RailTab(R.drawable.ic_gm_tab_video, R.string.gm_companion_tab_video, page == CompanionMenuPage.VIDEO) {
            onPage(CompanionMenuPage.VIDEO)
        }
        Spacer(Modifier.weight(1f))
        state.options.firstOrNull { it.iconKey == ICON_DISCONNECT }?.let { option ->
            RailAction(R.drawable.ic_gm_pause, option.label.trim(), danger = false) {
                callbacks.onOptionClick(option)
            }
        }
        state.options.firstOrNull { it.iconKey == ICON_QUIT }?.let { option ->
            RailAction(R.drawable.ic_gm_power, option.label.trim(), danger = true) {
                callbacks.onOptionClick(option)
            }
        }
    }
}

@Composable
private fun KeyHint(text: String) {
    Box(
        modifier = Modifier
            .border(1.dp, Palette.textMuted, RoundedCornerShape(4.dp))
            .padding(horizontal = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Palette.textMuted, fontSize = CompanionType.hint, lineHeight = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RailTab(@DrawableRes icon: Int, label: Int, selected: Boolean, onClick: () -> Unit) {
    SteamSurface(
        onClick = onClick,
        selected = selected,
        role = Role.Tab,
        modifier = Modifier.fillMaxWidth().height(54.dp)
    ) { active ->
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painterResource(icon), contentDescription = null,
                tint = if (active) Palette.accentLight else Palette.textMuted,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.height(3.dp))
            Text(
                stringResource(label),
                color = if (active) Palette.textStrong else Palette.textMuted,
                fontSize = CompanionType.label, fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun RailAction(@DrawableRes icon: Int, label: String, danger: Boolean, onClick: () -> Unit) {
    SteamSurface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(51.dp),
        background = if (danger) Palette.dangerSoft else Palette.card,
        border = if (danger) Palette.dangerOutline else Palette.outlineAccent
    ) { active ->
        val color = if (danger) Palette.danger else Palette.textPrimary
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painterResource(icon), contentDescription = null,
                tint = if (active && !danger) Palette.accentLight else color,
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.height(2.dp))
            Text(label, color = color, fontSize = CompanionType.label, fontWeight = FontWeight.ExtraBold, maxLines = 1)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Pages

private class TileSpec(
    val key: String,
    val title: String,
    @param:DrawableRes val icon: Int,
    val iconText: String? = null,
    val subtitle: String? = null,
    val enabled: Boolean = true,
    val dashed: Boolean = false,
    val toggle: Boolean? = null,
    val onLongClick: (() -> Unit)? = null,
    val onClick: () -> Unit
)

@Composable
private fun QuickPage(
    state: GameMenuComposeUiState,
    callbacks: GameMenuCallbacks,
    firstTile: Modifier,
    modifier: Modifier
) {
    val tiles = buildList {
        state.quickActions.forEach { action ->
            add(TileSpec(
                key = "qa:${action.id}",
                title = action.label,
                icon = action.iconRes,
                iconText = action.iconText,
                enabled = action.enabled,
                onClick = { callbacks.onQuickAction(action.id) }
            ))
        }
        listOf(ICON_SCREEN_KEYBOARD, ICON_HOST_KEYBOARD, ICON_SEND_KEYS).forEach { key ->
            state.options.firstOrNull { it.iconKey == key }?.let { option ->
                add(optionTile(option, callbacks))
            }
        }
        state.superOptions.forEach { option ->
            add(TileSpec(
                key = "super:${option.label}",
                title = option.label,
                icon = 0,
                iconText = firstActionCharacter(option.label),
                onClick = { callbacks.onOptionClick(option) }
            ))
        }
        if (state.superOptions.isEmpty()) {
            add(TileSpec(
                key = "super:empty",
                title = "",
                icon = R.drawable.ic_gm_plus,
                dashed = true,
                onClick = callbacks.onEmptySuperCommandClick
            ))
        }
    }
    val superLabel = stringResource(R.string.gm_companion_super_command)
    TileGrid(
        tiles = tiles.map { if (it.dashed) it.copyTitle(superLabel) else it },
        firstTile = firstTile,
        modifier = modifier
    )
}

private fun TileSpec.copyTitle(newTitle: String) = TileSpec(
    key, newTitle, icon, iconText, subtitle, enabled, dashed, toggle, onLongClick, onClick
)

@Composable
private fun InputPage(
    state: GameMenuComposeUiState,
    callbacks: GameMenuCallbacks,
    firstTile: Modifier,
    modifier: Modifier
) {
    val opacityLabel = stringResource(R.string.gm_companion_opacity)
    val usbLabel = stringResource(R.string.gm_companion_usb)
    val cardsLabel = stringResource(R.string.game_menu_card_config_title)
    val gyroLabel = stringResource(R.string.gm_companion_gyro)
    var opacityAnchor by remember { mutableStateOf(GameMenuOpacityAnchor(0, 0)) }
    val mouseMode = state.options.firstOrNull { it.iconKey == ICON_MOUSE_MODE }
    val tiles = buildList {
        listOf(ICON_PAN_ZOOM, ICON_CROWN, ICON_VIRTUAL_CONTROLLER).forEach { key ->
            state.options.firstOrNull { it.iconKey == key }?.let { add(optionTile(it, callbacks)) }
        }
        add(TileSpec(
            key = "gyro",
            title = gyroLabel,
            icon = R.drawable.ic_gm_gyro,
            toggle = state.gyro.enabled,
            onClick = { callbacks.onGyroEnabled(!state.gyro.enabled) }
        ))
        state.deviceQuickOptions.forEach { add(optionTile(it, callbacks)) }
        state.options.firstOrNull { it.iconKey == ICON_MICROPHONE }?.let { add(optionTile(it, callbacks)) }
        add(TileSpec(key = "cards", title = cardsLabel, icon = R.drawable.ic_gm_cards, onClick = callbacks.onEditCards))
        add(TileSpec(
            key = "opacity",
            title = opacityLabel,
            subtitle = "${state.gameMenuOpacity}%",
            icon = R.drawable.ic_opacity,
            onClick = { callbacks.onEditOpacity(opacityAnchor) }
        ))
        if (state.usbForwardingEnabled) {
            add(TileSpec(key = "usb", title = usbLabel, icon = R.drawable.ic_usb_type_generic, onClick = callbacks.onUsbDevices))
        }
    }
    BoxWithConstraints(modifier) {
        val rowHeight = (maxHeight - CompanionDimens.gap * 2) / 3
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(CompanionDimens.gap)
        ) {
            if (mouseMode != null) {
                val segmented = mouseMode.inlineControl as? GameMenu.InlineControl.Segmented
                SegmentCard(
                    title = mouseMode.label.trim(),
                    icon = callbacks.iconForOption(mouseMode.iconKey),
                    segments = segmented?.segments.orEmpty(),
                    onSegment = callbacks.onSegmentClick,
                    onTitleClick = mouseMode.runnable?.let { { callbacks.onOptionClick(mouseMode) } },
                    firstSegment = firstTile,
                    modifier = Modifier.fillMaxWidth().height(rowHeight)
                )
            }
            tiles.chunked(3).forEachIndexed { rowIndex, rowTiles ->
                Row(
                    modifier = Modifier.fillMaxWidth().height(rowHeight),
                    horizontalArrangement = Arrangement.spacedBy(CompanionDimens.gap)
                ) {
                    rowTiles.forEachIndexed { index, tile ->
                        val first = mouseMode == null && rowIndex == 0 && index == 0
                        SpecTile(
                            spec = tile,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                                .then(if (first) firstTile else Modifier)
                                .then(
                                    if (tile.key == "opacity") Modifier.onGloballyPositioned { coordinates ->
                                        val position = coordinates.positionInWindow()
                                        opacityAnchor = GameMenuOpacityAnchor(
                                            centerX = (position.x + coordinates.size.width / 2f).toInt(),
                                            bottomY = (position.y + coordinates.size.height).toInt()
                                        )
                                    } else Modifier
                                )
                        )
                    }
                    repeat(3 - rowTiles.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun VideoPage(
    state: GameMenuComposeUiState,
    callbacks: GameMenuCallbacks,
    firstTile: Modifier,
    modifier: Modifier
) {
    val bitrate = state.bitrate
    val perfOverlay = state.options.firstOrNull { it.iconKey == ICON_PERF_OVERLAY }
    val perfStyle = state.options.firstOrNull { it.iconKey == ICON_PERF_STYLE }
    BoxWithConstraints(modifier) {
        val rowHeight = (maxHeight - CompanionDimens.gap * 2) / 3
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(CompanionDimens.gap)
        ) {
            BitrateBlock(
                state = bitrate,
                callbacks = callbacks,
                firstButton = firstTile,
                modifier = Modifier.fillMaxWidth().height(rowHeight)
            )
            Row(
                modifier = Modifier.fillMaxWidth().height(rowHeight),
                horizontalArrangement = Arrangement.spacedBy(CompanionDimens.gap)
            ) {
                SpecTile(
                    spec = TileSpec(
                        key = "abr",
                        title = stringResource(R.string.title_adaptive_bitrate),
                        icon = R.drawable.ic_gm_adaptive,
                        toggle = bitrate.adaptiveBitrate,
                        onLongClick = callbacks.onAdaptiveBitrateInfo,
                        onClick = { callbacks.onBitrateAdaptive(!bitrate.adaptiveBitrate) }
                    ),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                if (perfOverlay != null) {
                    SegmentCard(
                        title = perfOverlay.label.trim(),
                        icon = callbacks.iconForOption(perfOverlay.iconKey),
                        segments = (perfOverlay.inlineControl as? GameMenu.InlineControl.Segmented)?.segments.orEmpty(),
                        onSegment = callbacks.onSegmentClick,
                        onTitleClick = null,
                        modifier = Modifier.weight(2f).fillMaxHeight()
                    )
                }
            }
            DisplayRow(bitrate, callbacks, Modifier.fillMaxWidth().height(rowHeight))
            if (perfStyle != null) {
                SegmentCard(
                    title = perfStyle.label.trim(),
                    icon = callbacks.iconForOption(perfStyle.iconKey),
                    segments = (perfStyle.inlineControl as? GameMenu.InlineControl.Segmented)?.segments.orEmpty(),
                    onSegment = callbacks.onSegmentClick,
                    onTitleClick = null,
                    modifier = Modifier.fillMaxWidth().height(rowHeight)
                )
            }
        }
    }
}

@Composable
private fun DisplayRow(state: BitrateCardState, callbacks: GameMenuCallbacks, modifier: Modifier) {
    val scale = state.displayScale
    val scalePercent = scale.pendingPercent ?: scale.currentPercent
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(CompanionDimens.gap)) {
        val cell = Modifier.weight(1f).fillMaxHeight()
        ValueTile(stringResource(R.string.display_spec_resolution), compactResolutionLabel(state.resolutionLabel),
            onClick = callbacks.onOpenResolutionSheet, modifier = cell)
        ValueTile(stringResource(R.string.display_spec_fps), state.fpsLabel,
            onClick = callbacks.onOpenFrameRateSheet, modifier = cell)
        ValueTile(stringResource(R.string.display_spec_screen), state.screenModeLabel,
            onClick = callbacks.onOpenScreenModeSheet, modifier = cell, valueSize = CompanionType.tile)
        if (scale.available || scale.loading || scale.errorCode != null) {
            ValueTile(
                label = stringResource(R.string.display_scale_title),
                value = scalePercent?.let { "$it%" } ?: "—",
                note = if (scalePercent != null && scalePercent == scale.recommendedPercent) {
                    stringResource(R.string.display_scale_recommended)
                } else null,
                loading = scale.loading,
                onClick = {
                    if (scale.available && !scale.loading) {
                        nextDisplayScalePercent(scale.supportedPercents, scalePercent)?.let(callbacks.onSetDisplayScale)
                    } else if (!scale.loading) {
                        callbacks.onRefreshDisplayScale()
                    }
                },
                modifier = cell
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Building blocks

private fun optionTile(option: GameMenu.MenuOption, callbacks: GameMenuCallbacks): TileSpec {
    val toggle = option.inlineControl as? GameMenu.InlineControl.Toggle
    val dedicatedToggle = toggle?.toggleAction != null
    return TileSpec(
        key = "opt:${option.iconKey ?: option.label}",
        title = option.label.trim(),
        icon = callbacks.iconForOption(option.iconKey),
        iconText = firstActionCharacter(option.label.trim()),
        toggle = toggle?.checked,
        // Crown: tap toggles, long-press opens its submenu (the row chevron in the phone layout).
        onLongClick = if (dedicatedToggle && option.runnable != null) {
            { callbacks.onOptionClick(option) }
        } else null,
        onClick = {
            if (toggle != null && dedicatedToggle) callbacks.onInlineToggle(toggle)
            else callbacks.onOptionClick(option)
        }
    )
}

@Composable
private fun TileGrid(tiles: List<TileSpec>, firstTile: Modifier, modifier: Modifier) {
    BoxWithConstraints(modifier) {
        val rowHeight = (maxHeight - CompanionDimens.gap * 2) / 3
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(CompanionDimens.gap)
        ) {
            tiles.chunked(3).forEachIndexed { rowIndex, rowTiles ->
                Row(
                    modifier = Modifier.fillMaxWidth().height(rowHeight),
                    horizontalArrangement = Arrangement.spacedBy(CompanionDimens.gap)
                ) {
                    rowTiles.forEachIndexed { index, tile ->
                        SpecTile(
                            spec = tile,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                                .then(if (rowIndex == 0 && index == 0) firstTile else Modifier)
                        )
                    }
                    repeat(3 - rowTiles.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/**
 * Clickable Steam surface. Focused (controller) and pressed (touch) states use the Steam
 * blue gradient; [selected] keeps the gradient with a light-blue outline.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SteamSurface(
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    selected: Boolean = false,
    enabled: Boolean = true,
    role: Role = Role.Button,
    shape: Shape = TileShape,
    background: Color = Palette.card,
    border: Color = Palette.outlineAccent,
    dashed: Boolean = false,
    content: @Composable (active: Boolean) -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val pressed by interaction.collectIsPressedAsState()
    val haptic = LocalGameMenuHapticFeedback.current
    val active = focused || pressed || selected
    val fill = when {
        focused || pressed -> Brush.verticalGradient(listOf(Palette.focusTop, Palette.focusBottom))
        selected -> Brush.verticalGradient(listOf(Palette.selectedTop, Palette.selectedBottom))
        dashed -> SolidColor(Color.Transparent)
        else -> SolidColor(background)
    }
    val outline = when {
        focused -> Palette.accentLight
        selected -> Palette.accentLight
        else -> border
    }
    val outlineWidth = when {
        focused -> CompanionDimens.strokeFocus
        selected -> CompanionDimens.strokeSelected
        else -> CompanionDimens.strokeDefault
    }
    val dashColor = Palette.accentLight.copy(alpha = 0.35f)
    Box(
        modifier = modifier
            .clip(shape)
            .background(fill)
            .then(
                if (dashed && !active) {
                    Modifier.drawBehind {
                        val stroke = 1.5.dp.toPx()
                        drawRoundRect(
                            color = dashColor,
                            topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
                            size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                            cornerRadius = CornerRadius(CompanionDimens.radiusTile.toPx()),
                            style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)))
                        )
                    }
                } else {
                    Modifier.border(outlineWidth, outline, shape)
                }
            )
            .then(
                if (onClick != null) {
                    Modifier.combinedClickable(
                        interactionSource = interaction,
                        indication = null,
                        enabled = enabled,
                        role = role,
                        onLongClick = onLongClick?.let {
                            {
                                haptic(HapticFeedbackConstants.LONG_PRESS)
                                it()
                            }
                        },
                        onClick = {
                            haptic(HapticFeedbackConstants.VIRTUAL_KEY)
                            onClick()
                        }
                    )
                } else Modifier
            )
    ) {
        content(active)
    }
}

@Composable
private fun SpecTile(spec: TileSpec, modifier: Modifier) {
    val on = spec.toggle == true
    SteamSurface(
        onClick = spec.onClick,
        onLongClick = spec.onLongClick,
        enabled = spec.enabled,
        dashed = spec.dashed,
        role = if (spec.toggle != null) Role.Switch else Role.Button,
        modifier = modifier
    ) { active ->
        Column(
            modifier = Modifier.fillMaxSize().padding(CompanionDimens.tilePadding),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.Top) {
                IconChip(
                    icon = spec.icon,
                    fallbackText = spec.iconText,
                    highlighted = active && spec.toggle == null,
                    tint = when {
                        spec.toggle == null -> Palette.icon
                        on -> Palette.accentLight
                        else -> Palette.textSoft
                    },
                    chipColor = if (on) Palette.accent.copy(alpha = 0.22f) else Palette.iconChip
                )
                Spacer(Modifier.weight(1f))
                if (spec.toggle != null) SteamSwitch(on)
            }
            Column {
                Text(
                    text = spec.title,
                    color = when {
                        !spec.enabled -> Palette.textMuted
                        spec.dashed -> Palette.accentLight
                        spec.toggle == false -> Palette.textSoft
                        else -> Palette.textPrimary
                    },
                    fontSize = CompanionType.tile,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                spec.subtitle?.let {
                    Text(it, color = Palette.textMuted, fontSize = CompanionType.caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun IconChip(
    @DrawableRes icon: Int,
    fallbackText: String?,
    highlighted: Boolean,
    tint: Color,
    chipColor: Color,
    size: Dp = CompanionDimens.iconChip,
    iconSize: Dp = CompanionDimens.icon
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(if (highlighted) Palette.accentLight else chipColor),
        contentAlignment = Alignment.Center
    ) {
        val color = if (highlighted) Palette.background else tint
        if (icon != 0) {
            Icon(painterResource(icon), contentDescription = null, tint = color, modifier = Modifier.size(iconSize))
        } else {
            Text(fallbackText.orEmpty(), color = color, fontSize = CompanionType.tile, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun SteamSwitch(checked: Boolean) {
    Box(
        modifier = Modifier
            .width(38.dp)
            .height(22.dp)
            .clip(CircleShape)
            .background(if (checked) Palette.accent else Palette.track),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            Modifier
                .padding(3.dp)
                .size(16.dp)
                .clip(CircleShape)
                .background(if (checked) Palette.textStrong else Palette.switchKnobOff)
        )
    }
}

@Composable
private fun PanelCard(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .clip(TileShape)
            .background(Palette.panel)
            .border(1.dp, Palette.outlineAccent, TileShape)
            .padding(9.dp),
        content = content
    )
}

@Composable
private fun PanelTitle(
    title: String,
    @DrawableRes icon: Int,
    note: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .then(
                if (onClick != null) Modifier.clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button, onClick = onClick)
                else Modifier
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconChip(icon, null, highlighted = false, tint = Palette.icon, chipColor = Palette.iconChip,
            size = 28.dp, iconSize = 17.dp)
        Spacer(Modifier.width(7.dp))
        Text(title, color = Palette.textPrimary, fontSize = CompanionType.tile, fontWeight = FontWeight.ExtraBold, maxLines = 1)
        if (onClick != null) {
            Text(" ›", color = Palette.textMuted, fontSize = CompanionType.title, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.weight(1f))
        trailing()
        note?.let { Text(it, color = Palette.textMuted, fontSize = CompanionType.caption, maxLines = 1) }
    }
}

@Composable
private fun SegmentCard(
    title: String,
    @DrawableRes icon: Int,
    segments: List<GameMenu.SegmentOption>,
    onSegment: (GameMenu.SegmentOption) -> Unit,
    onTitleClick: (() -> Unit)?,
    modifier: Modifier,
    firstSegment: Modifier = Modifier
) {
    PanelCard(modifier) {
        PanelTitle(title, icon, onClick = onTitleClick)
        Spacer(Modifier.height(7.dp))
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            segments.forEachIndexed { index, segment ->
                SteamSurface(
                    onClick = { onSegment(segment) },
                    selected = segment.selected,
                    role = Role.RadioButton,
                    shape = RoundedCornerShape(CompanionDimens.radiusControl),
                    modifier = Modifier.weight(1f).fillMaxHeight().then(if (index == 0) firstSegment else Modifier)
                ) { active ->
                    Box(Modifier.fillMaxSize().padding(horizontal = 2.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = segment.label,
                            color = if (active) Palette.textStrong else Palette.textSoft,
                            fontSize = if (segment.label.length > 4) CompanionType.caption else CompanionType.label,
                            fontWeight = if (segment.selected) FontWeight.ExtraBold else FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BitrateBlock(
    state: BitrateCardState,
    callbacks: GameMenuCallbacks,
    firstButton: Modifier,
    modifier: Modifier
) {
    val minKbps = BitrateCardController.progressToBitrateKbps(0, state.maxProgress)
    val selected = BitrateCardController.formatBitrateMbps(state.selectedBitrateKbps)
    val number = selected.substringBefore(' ')
    val step = { delta: Int ->
        val next = (state.progress.toInt() + delta).coerceIn(0, state.maxProgress)
        if (next != state.progress.toInt()) {
            callbacks.onBitrateProgress(next.toFloat())
            callbacks.onBitrateApply()
        }
    }
    val presets = BITRATE_PRESETS_KBPS.filter { it in minKbps..state.maxBitrateKbps }
    PanelCard(modifier) {
        PanelTitle(
            title = stringResource(R.string.gm_companion_bitrate),
            icon = R.drawable.ic_gm_bitrate,
            note = "${BitrateCardController.formatBitrateMbps(minKbps).substringBefore(' ')} – " +
                BitrateCardController.formatBitrateMbps(state.maxBitrateKbps)
        ) {
            if (state.bitrateUpdating) {
                CircularProgressIndicator(
                    color = Palette.accentLight, strokeWidth = 2.dp,
                    modifier = Modifier.size(14.dp).testTag("bitrateUpdating")
                )
                Spacer(Modifier.width(6.dp))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(number, color = Palette.textPrimary, fontSize = CompanionType.display, fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.testTag("bitrateReadout"))
            Text(" Mbps", color = Palette.textMuted, fontSize = CompanionType.label, modifier = Modifier.padding(top = 14.dp))
            Spacer(Modifier.weight(1f))
            StepButton(R.drawable.ic_gm_minus, primary = false, modifier = firstButton) { step(-1) }
            Spacer(Modifier.width(7.dp))
            StepButton(R.drawable.ic_gm_plus, primary = true) { step(1) }
        }
        if (presets.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(28.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                presets.forEach { kbps ->
                    val isSelected = kbps == state.selectedBitrateKbps
                    SteamSurface(
                        onClick = {
                            callbacks.onBitrateProgress(
                                BitrateCardController.bitrateToProgress(kbps, state.maxProgress).toFloat()
                            )
                            callbacks.onBitrateApply()
                        },
                        background = if (isSelected) Palette.accentLight else Palette.card,
                        shape = RoundedCornerShape(CompanionDimens.radiusChip),
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) { active ->
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "${kbps / 1000}",
                                color = if (isSelected && !active) Palette.background else Palette.textSoft,
                                fontSize = CompanionType.label,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepButton(@DrawableRes icon: Int, primary: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    SteamSurface(
        onClick = onClick,
        background = if (primary) Palette.accent else Palette.cardRaised,
        shape = RoundedCornerShape(CompanionDimens.radiusControl),
        modifier = modifier.width(50.dp).height(40.dp)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), contentDescription = null,
                tint = if (primary) Palette.textStrong else Palette.textSoft, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun ValueTile(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier,
    note: String? = null,
    loading: Boolean = false,
    valueSize: TextUnit = CompanionType.value
) {
    SteamSurface(onClick = onClick, modifier = modifier) {
        Box(Modifier.fillMaxSize().padding(CompanionDimens.tilePadding)) {
            Text("›", color = Palette.textMuted, fontSize = CompanionType.title, modifier = Modifier.align(Alignment.TopEnd))
            Column(modifier = Modifier.align(Alignment.CenterStart)) {
                Text(label, color = Palette.textMuted, fontSize = CompanionType.caption, maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(value, color = Palette.textPrimary, fontSize = valueSize, fontWeight = FontWeight.ExtraBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (loading) {
                        Spacer(Modifier.width(5.dp))
                        CircularProgressIndicator(color = Palette.accentLight, strokeWidth = 2.dp,
                            modifier = Modifier.size(12.dp))
                    }
                }
                note?.let { Text(it, color = Palette.textMuted, fontSize = CompanionType.caption, maxLines = 1) }
            }
        }
    }
}

# 副屏设计规范（双屏专用）

适用范围：双屏掌机（如 AYN Thor）**第二块屏**上的所有界面：浏览副屏 `BrowseScreenCompanion`、串流副屏 `SecondScreenCompanion`、副屏游戏菜单 `CompanionGameMenu`，以及以后新增的副屏页面。
**单屏界面不适用**，单屏保持上游原有布局与交互（见 [AGENTS.md](AGENTS.md)）。

参考实现：`gamemenu/CompanionGameMenu.kt`；设计稿：`design/game-menu-companion.html`。

## 1. 屏幕与单位

| 项 | 值 |
| --- | --- |
| 基准屏 | AYN Thor 副屏 3.9 寸，1240×1080 px，density 2.306 ≈ **538×468 dp** |
| 交互 | 触屏为主，手柄为辅（焦点可见，A 确认 / B 返回 / LB·RB 切页） |
| 单位 | 布局一律 dp / sp 绝对值，**不做整体缩放**（不要再乘 `COMPANION_ZOOM` 之类的系数） |
| 设计稿换算 | 设计稿 px ÷ 2.3 ≈ dp |

## 2. 颜色

唯一来源：`res/values/panel_colors.xml`（`panel_*`）。Compose 用 `CompanionTheme.colors`，View 用 `@color/panel_*`。**禁止十六进制**（`PanelThemeTokensTest`、`CompanionDesignTokensTest` 会拦）。

| 角色 | token | Compose | 用法 |
| --- | --- | --- | --- |
| 页面底 | `panel_background` | `background` | 整屏底色 |
| 光晕 | `panel_glow` | `glow` | 右上角径向渐变到 `background` |
| 展示面板 | `panel_surface` | `panel` | 只读信息、包含多个控件的大卡片（码率、操作方式） |
| 可点卡片 | `panel_card` | `card` | 所有可点击方块的默认底 |
| 次级按钮 | `panel_card_raised` | `cardRaised` | −、次要操作 |
| 描边 | `panel_outline_accent` | `outlineAccent` | 卡片默认 1dp 描边 |
| 强调 | `panel_accent` | `accent` | 主按钮（+）、开关打开 |
| 强调浅 | `panel_accent_light` | `accentLight` | 选中/焦点描边、选中图标、选中预设 |
| 选中渐变 | `panel_selected_top/bottom` | `selectedTop/Bottom` | 当前页签、当前选项 |
| 焦点/按下渐变 | `panel_focus_top/bottom` | `focusTop/Bottom` | 手柄焦点、手指按下 |
| 图标 | `panel_icon` / `panel_icon_chip` | `icon` / `iconChip` | 图标颜色 / 图标底块 |
| 开关关 | `panel_track` / `panel_switch_knob_off` | `track` / `switchKnobOff` | |
| 文字 | `panel_text_strong` / `_primary` / `_soft` / `_muted` / `_on_accent` | `textStrong` … | 选中文字 / 正文 / 次要 / 说明 / 浅底上的字 |
| 状态 | `panel_ok` | `ok` | 在线、正常 |
| 危险 | `panel_danger` / `_soft` / `_outline` | `danger` … | 退出串流等破坏性操作（字 / 底 / 描边） |

规则：
- 可点的东西用 `card`，纯展示用 `panel`，用户一眼能分出"能不能点"。
- 不用大面积彩色色块；颜色只用于状态（选中、开关、危险、正常）。
- 危险操作统一红色系，且不放在网格中间，放在左栏底部。

## 3. 字号

唯一来源：`res/values/companion_type.xml`（`companion_text_*`）↔ `CompanionType`。

| 级别 | sp | 字重 | 用途 |
| --- | --- | --- | --- |
| hero | 64 | ExtraBold | 整屏唯一的主指标（实时帧率） |
| display | 40 | ExtraBold | 卡片里的大数字（码率） |
| value | 22 | ExtraBold | 数值格（分辨率、帧率、RTT） |
| title | 20 | ExtraBold | 页面标题 |
| tile | 16 | ExtraBold | 方块标题、面板标题、数值格的长文字值（如"跟随主机"） |
| label | 14 | Bold | 页签、分段选项、按钮、返回 |
| caption | 12 | Regular/Bold | 副标题、说明、状态标签、数值格的名称 |
| hint | 10 | Bold | **只用于**按键提示（LB/RB） |

- 最小可读字号 12sp；除按键提示外不得更小。
- 方块标题单行，超出省略；分段选项最多两行，超过 4 个汉字降到 caption。
- 圆形/方形徽标里的单个字符（如 B 键）用 `CompanionType.centeredGlyph`（去字体内边距、按行高居中），否则会偏下。

## 4. 尺寸与布局

| token | dp | 说明 |
| --- | --- | --- |
| `screenPadding` | 10 | 屏幕外边距 |
| `gap` | 8 | 卡片间距（分段内部 5） |
| `tilePadding` | 10 | 方块内边距 |
| `radiusTile` / `radiusControl` / `radiusChip` | 12 / 10 / 7 | 方块 / 按钮与分段 / 标签与预设 |
| `touchMin` | 44 | 最小触控尺寸（宽高都要满足） |
| `headerHeight` | 44 | 顶栏高度 |
| `railWidth` | 92 | 左栏宽度 |
| `iconChip` / `icon` | 36 / 20 | 图标底块 / 图标 |
| 描边 | 1 / 1.5 / 2.5 | 默认 / 选中 / 手柄焦点 |

页面骨架（538×468dp 一屏放完，不滚动为目标）：

```
┌ 顶栏 44：[B 返回] 标题 ……………… 状态标签(码率·分辨率·帧率) ┐
├ 左栏 92 ─┬ 内容区：3 行网格，行高 = (可用高 − 2×gap) / 3 ─────┤
│ LB 切换 RB│  3 列方块；宽卡片占整行；超过 3 行才允许纵向滚动     │
│ 页签×3    │                                                     │
│   …       │                                                     │
│ 暂停      │                                                     │
│ 退出(红)  │                                                     │
└──────────┴─────────────────────────────────────────────────────┘
```

方块结构：左上图标底块（36dp），右上可选开关（38×22dp），底部标题（tile）+ 可选副标题（caption）。

## 5. 交互状态

| 状态 | 底 | 描边 | 文字/图标 |
| --- | --- | --- | --- |
| 默认 | `card` | 1dp `outlineAccent` | `textPrimary` / `icon` |
| 按下（触屏） | 焦点渐变 | 同默认 | 图标底块变 `accentLight`，图标 `textOnAccent` |
| 焦点（手柄） | 焦点渐变 | 2.5dp `accentLight` | 同按下 |
| 选中 | 选中渐变 | 1.5dp `accentLight` | `textStrong` |
| 开关开 / 关 | `card` | 同默认 | 开：图标 `accentLight`，开关 `accent`；关：文字 `textSoft` |
| 禁用 | `card` | 同默认 | `textMuted` |
| 危险 | `dangerSoft` | `dangerOutline` | `danger` |

- 点击：`HapticFeedbackConstants.VIRTUAL_KEY`；长按：`LONG_PRESS`。
- 开关点一下立即生效，菜单不关闭；会离开当前界面的操作（键盘、发送按键、子菜单）才关闭或跳转。
- 有子菜单的开关：点 = 开/关，长按 = 进入子菜单；面板标题带 `›` 表示可进入详情。
- 手柄：A 确认、B 返回、LB/RB 循环切换左栏页签，切页后焦点落在第一个方块；触屏模式下不显示焦点。
- 不用 Ripple；按下反馈只用上面的渐变。

## 6. 新增副屏界面检查清单

1. 只在双屏路径生效（有副屏时才走新代码），单屏代码与行为不变。
2. 颜色全部来自 `panel_*`；需要新颜色先加 token 并在本文档登记。
3. 字号只用第 3 节的 8 级；尺寸用第 4 节 token。
4. 触控目标 ≥ 44dp；一屏放下，不依赖滚动。
5. 手柄能走通：焦点可见、B 可返回。
6. 在 Thor 实机副屏截图核对（`adb exec-out screencap -p -d <副屏 display id>`）。

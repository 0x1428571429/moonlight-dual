# Moonlight Dual 单屏 / 双屏自适应设计

状态：P1–P5 已完成（P4 为代码核对，未做真实串流验证）。

## 1. 现状（调研结论）

| 项 | 双屏 | 单屏 |
| --- | --- | --- |
| 浏览态 | 主屏=主机网格；副屏 `BrowseScreenCompanion`（主机/串流设置/菜单 3 tab）；主屏隐藏工具栏和左侧栏（`setMainToolbarVisible(false)`） | 主屏保留工具栏（设置/关于/EasyTier/更多）和左侧栏，上游原样 |
| 串流态 | 副屏 `SecondScreenCompanion`（监控/码率/输入/快捷 4 tab） | 只有 `GameMenu`（手柄/长按呼出），没有监控、码率条等面板 |
| 样式 | 副屏面板硬编码颜色 ~100 处（`#2F9BFF`、`#F7F3F7`、`#FF070C12` 等），不走主题 | 走主题（`appAccent`、`ui_shell_*`） |

问题：
1. 同一份面板内容只能放在副屏 `Presentation` 里，单屏用不了。
2. 面板颜色绕开主题，换主题或放到别的容器就不一致。
3. 单屏浏览态风格是上游原样，和双屏不统一；单屏串流缺监控/码率面板。

## 2. 设计原则

1. **内容只写一份，容器可换。** 面板内容（浏览 3 页、串流 4 页）与"放在哪块屏"解耦。
2. **主题 token 化。** 面板只引用语义 token，不出现十六进制；任何容器里长得一样，换主题只改 token。
3. **双屏行为零变化。** 双屏下现有流程不动，只换底层实现（颜色来源、容器抽象）。
4. **不遮挡游戏。** 单屏串流时面板是抽屉，默认关闭，呼出才出现。

## 3. 主题 token（复用的核心）

`minSdk` 是 22，颜色选择器（`res/color`）里 `?attr` 要 API 23，所以 token 用**颜色资源**而不是主题属性：
`app/src/main/res/values/panel_colors.xml` 是唯一来源，名字全部是语义名（`panel_*`）：

| 分组 | token |
| --- | --- |
| 底与描边 | `panel_background`、`panel_surface`、`panel_outline`、`panel_outline_strong`、`panel_track` |
| 文字 | `panel_text_strong`（纯白）、`panel_text_primary`、`panel_text_secondary`、`panel_text_soft`、`panel_text_muted`、`panel_text_subtle`、`panel_text_on_accent`、`panel_tab_text` |
| 强调 | `panel_accent`、`panel_accent_soft`、`panel_accent_light`、`panel_accent_pressed` |
| 状态与图表 | `panel_ok`、`panel_series_1..4` |

规则：
- `companion_*` 的 layout / drawable / color / style 与 `BrowseScreenCompanion`、`SecondScreenCompanion` 一律引用 `@color/panel_*`（代码里用 `ContextCompat.getColor`），禁止十六进制。
- 换主题：只改 `panel_colors.xml`，或用 `values-night` 等限定目录覆盖；面板放到副屏、侧栏、抽屉里都是同一套颜色。
- 单测 `PanelThemeTokensTest` 扫描 `companion_*` 资源，出现十六进制或引用了未定义的 token 即失败。
- 已完成（P1）：双屏截图与改前一致。

## 4. 布局方案（以原来的单屏为基础）

修正：应用原本就是单屏，双屏是后来叠加的。所以**单屏沿用原来的布局方式**，只套用我们后来的样式；双屏在此基础上多一块副屏面板。不新做侧栏/抽屉容器。

| | 单屏 | 双屏 |
| --- | --- | --- |
| 浏览态主屏 | 原布局：横屏=左侧栏（设置/关于/EasyTier/更多）+ 标题 + 主机网格 + 手柄提示；竖屏=顶部工具栏 + Hosts 标题 + 网格 | 同一套，隐藏工具栏和侧栏（`setMainToolbarVisible(false)`），入口改到副屏"菜单"页 |
| 副屏 | 无 | `BrowseScreenCompanion`（主机/串流设置/菜单） |
| 串流态 | 原 `GameMenu`（手柄/呼出） | 副屏 `SecondScreenCompanion`（监控/码率/输入/快捷）；`GameMenu` 保留 |

样式统一靠 token：单屏布局与双屏面板引用同一套 `panel_*` 颜色，所以两种模式看起来是同一个产品。

## 5. 实施方案（分阶段，各自提交 + 验证）

| 阶段 | 内容 | 状态 |
| --- | --- | --- |
| P1 主题 token | `panel_colors.xml`；`companion_*` 资源与代码去硬编码；防回退单测 | 完成 |
| P2/P3 单屏样式对齐 | 单屏主界面（`activity_pc_view` 横/竖、`steam_rail_bg`）颜色并入 token；竖屏补上与横屏一致的壁纸遮罩和 Hosts 标题；单测覆盖这些文件 | 完成，模拟器截图核对 |
| P4 串流态单屏 | 结论：沿用原 `GameMenu`，不合并。核对代码：`GameMenu` 已含码率卡（`BitrateCardController`，含自适应）、音频触觉、陀螺仪、性能浮层样式（迷你/标准/详细）；监控在单屏本来就是主屏上的性能浮层。副屏 4 tab 只是把同样的功能搬到第二块屏，单屏无需重复 | 完成（未真实串流验证） |
| P5 收尾 | 更新 `DUAL_CHANGES.md`；回归测试 | 完成 |

已放弃的方案：把副屏面板抽成可内嵌的"侧栏/底栏/抽屉"容器。原因：单屏本来就有完整入口，新容器会改动 `PcView`/`Game` 这类上游冲突热点，收益小。

## 6. 约束
- `PcView`、`Game` 是上游冲突热点：样式改动放在资源文件里，Kotlin 只动必要处。
- 串流态的真实验证需要能连主机（`tools/mock-sunshine` 或 0x77，先 `win-status` 预检）。

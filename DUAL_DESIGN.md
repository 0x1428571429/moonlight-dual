# Moonlight Dual 单屏 / 双屏自适应设计

状态：设计 + 方案（2026-11-14）。P1 已完成，P2–P5 待做；每阶段单独提交。

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

## 4. 自适应布局

决策函数（纯函数，可单测）：

```
layoutFor(hasSecondDisplay, windowWidthDp, isStreaming) -> PanelLayout
  hasSecondDisplay                      -> OnSecondDisplay      (现状，不变)
  !hasSecondDisplay && !isStreaming:
      width >= 600dp (横屏/平板/掌机)    -> SideRail            (左侧 tab 栏 + 内容)
      else (手机竖屏)                    -> BottomBar           (底部 tab 栏)
  !hasSecondDisplay && isStreaming      -> Drawer               (画面全屏，面板从右/下滑出)
```

### 浏览态（未串流）
```
双屏：主屏=主机网格 | 副屏=[主机][串流设置][菜单]            （不变）
单屏横屏：┌────┬───────────────────┐   单屏竖屏：┌──────────────┐
          │主机│ 0x77-H ● 在线      │            │  内容        │
          │设置│ [主机卡片网格]     │            │              │
          │菜单│                   │            ├──────────────┤
          └────┴───────────────────┘            │主机│设置│菜单│
```
- 单屏时 3 个 tab 的页面就是副屏那 3 页，原样复用；"主机"页即网格，不再重复一份。
- 工具栏入口（关于/EasyTier/恢复会话/显示未配对）全部收进"菜单"页，与双屏一致，主屏工具栏隐藏。

### 串流态（单屏）
```
┌──────────────────────────┐
│        游戏画面       ┌──┤  呼出：悬浮球 / 边缘滑动 / Start+Select 长按
│                       │监│  抽屉内容 = 副屏 4 个 tab（监控/码率/输入/快捷）
│                       │码│  与 GameMenu 的关系：GameMenu 保留给手柄卡片式设置，
└───────────────────────┴──┘  抽屉负责"监控 + 快捷操作"，两者互不重复
```
- 抽屉半透明，打开时不暂停串流，点空白/返回关闭。

## 5. 组件抽象

- `CompanionPanel`（接口）：`createView(context): View`、`onShown/onHidden`、`handleKey`、`cycleTab`。浏览 3 页和串流 4 页各一个实现，内容与现在的 `BrowsePresentation` / `CompanionPresentation` 里的逻辑相同，只是不再继承 `Presentation`。
- `PanelHost`（容器）：
  - `SecondDisplayHost`：用 `Presentation`（现有行为）。
  - `SideRailHost` / `BottomBarHost`：把面板 View 加进主屏布局的容器。
  - `DrawerHost`：浮在 `Game` 画面上的抽屉。
- `PanelLayoutPolicy`：上面的决策函数；窗口尺寸变化（折叠、分屏、旋转）时重算并换容器，面板状态（当前 tab）保留。
- 触发：`PcView.showBrowseCompanion()` 与 `Game.updateSecondScreenCompanion()` 改为调用 policy 选 host，不再直接 `find` 后创建。

## 6. 实施方案（分阶段，各自提交 + 验证）

| 阶段 | 内容 | 验证 |
| --- | --- | --- |
| P1 主题 token（已完成） | 新增 `panel_colors.xml`；`companion_*` 布局/drawable/style 去硬编码；Presentation 用主题包装；加"禁止十六进制"单测 | 双屏截图与现状一致（视觉无回归）；单测通过 |
| P2 面板抽象 | 抽出 `CompanionPanel` / `PanelHost`；双屏改走 `SecondDisplayHost`，行为不变 | 双屏模拟器：浏览 3 tab、串流 4 tab 可用 |
| P3 单屏浏览 | `PanelLayoutPolicy` + `SideRailHost`/`BottomBarHost`；单屏隐藏旧工具栏，入口并入"菜单"页 | 单屏横屏/竖屏截图；各入口可点开 |
| P4 单屏串流 | `DrawerHost` + 悬浮球/边缘滑动/组合键呼出；与 `GameMenu` 分工 | 单屏串流（mock-sunshine 或真机）下抽屉可开关、不挡画面 |
| P5 收尾 | 更新 `DUAL_CHANGES.md`、sync skill 的冲突热点表；回归仪器测试 | 失败数不高于基线 22 |

风险与约束：
- `PcView`、`Game` 是上游冲突热点，改动要小而集中，放在新文件里，主文件只改调用点。
- 串流态的真实验证需要能连主机（`tools/mock-sunshine` 或 0x77，先 `win-status` 预检）。
- 手柄导航：侧栏/抽屉要接入现有的焦点与 `handleKey` 逻辑，B 键关闭抽屉。

## 7. 待定
- 单屏竖屏手机是否也做（默认做，成本低：只是 BottomBar）。
- 抽屉默认呼出方式（默认悬浮球 + 边缘滑动 + Start+Select 长按，设置里可单独关闭）。

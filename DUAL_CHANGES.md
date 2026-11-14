# Moonlight Dual 改动记录

仓库：origin = 0x1428571429/moonlight-dual；上游 = qiin2333/moonlight-vplus。同步流程见 `.claude/skills/sync-upstream/SKILL.md`。

## 1. 分叉点与我们的提交
- 分叉点：上游 `4532ae6d`
- 我们的提交：`b4e36838` "feat: Moonlight Dual - Steam-style UI for dual-screen handhelds"（170 个文件，+7340 / -1458）

内容概要：
- 双屏掌机的 Steam 风格 UI：主屏 + 副屏 Companion（`BrowseScreenCompanion`、`SecondScreenCompanion`、`CompanionDisplays`、`CompanionPreviewActivity`）。
- 主题固定为深色 + 粉色（`AppTheme`），删除主题面板 `AppThemeSheet.kt`。
- 串流中游戏菜单与快捷操作重做（`gamemenu/*`、`cq_*`/`ic_candy_*` 图标）。
- 新应用图标 `dual_icon_*`，启动页、AppView、PC 网格（`PcGridAdapter`）、性能浮层 / PerfHud、网络质量面板、About 对话框样式调整。
- 移除 About 的 QQ 入口（`onQq` 参数）。
- README / README_EN、`app/build.gradle`、debug manifest、`.gitignore` 相应调整。

## 2. 冲突热点（上游改这些文件时要人工合并）
AppView, AppVisibility, BrowseScreenCompanion, CompanionDisplays, Game, LimelightApplication, PcView, PerformanceOverlayManager, SecondScreenCompanion, GitHubCrownProfileStorePublisher, gamemenu/*（CuteFeatureGuideCard, GameMenu, GameMenuOptions, GameMenuQuickActions, GameMenuScreen）, grid/PcGridAdapter, handbook/HandbookRepository, networkquality/NetworkQualitySheet, nvstream/NvConnection, perfhud/*, preferences/SponsorQrSaver, preferences/StreamSettings, services/StreamNotificationService, ui/AboutDialog, ui/SparklineView, ui/theme/AppShapes, utils/AboutDialogLauncher, utils/AppActionSheet, utils/AppTheme, utils/easytier/*，以及大量 `res/drawable`、`res/layout/companion_*`、`res/color`。

## 3. 分叉后的本地提交（分支 sync-upstream，尚未 push）
| 提交 | 说明 |
| --- | --- |
| 1adfd398 | fix(pcview)：`completeOnCreate` 之前不显示浏览副屏，修 `pcGridAdapter` 未初始化崩溃 |
| 9d72722a | style(companion)：选中 tab 文字白色；7 个 tab 统一 `CompanionTab` 样式 |
| a0725738 | test(androidTest)：AboutDialogInputTest 去掉已删除的 `onQq` 参数 |
| 0f428bfd / 3d9c0f44 | 合并上游 |

## 4. 同步记录
| 日期 | 上游到 | 冲突 | 结果 |
| --- | --- | --- | --- |
| 2026-11-14 | 12.15.7（`7a7069cd`，共 10 个提交） | `AppThemeSheet.kt`（保持删除；上游新增的暗黑桌面图标 `LauncherIcon` 已并入但无 UI 入口） | 构建通过；单元测试 1361 全过；模拟器冒烟：启动、副屏 3 tab 正常 |

## 5. 测试状态（2026-11-14）
- 单元测试：1361 通过。
- 仪器测试：约 304 个，45 个失败，分类：
  - 约 8 个测试过时：我们把主题固定为深色+粉色（AppThemeTest 5、PcCardDecorTest 3），需更新用例。
  - 约 10 个 R8 混淆造成（NoSuchFieldError ×7、NoSuchMethodError ×3），属 debug 构建配置。
  - 约 17 个窗口焦点类：单屏模拟器上同样失败，代码非我们改动，疑似环境问题。
  - 约 10 个未诊断：About 对话框 ×3、ViewFeatureGuideFocusTest ×5、StreamViewInputTest、RemoteImeAvoidanceTest、HandbookControllerLinkTest。
- 未验证：配对/串流、串流中副屏（4 个 tab）、触觉修复、设置子页、EasyTier 流程。

## 6. 待办
- 修过时测试 + 让 R8 不裁剪测试所需成员，重跑仪器测试。
- 用 `tools/mock-sunshine` 或真实主机验证配对与串流。
- 决定是否 push `sync-upstream` 并合入 master。
- 键盘选择器的 tab 是否也统一样式。

# AGENTS.md — Moonlight Dual

> 始终用中文回复用户。

## 项目定位（必须遵守）

Moonlight Dual 是 [moonlight-vplus](https://github.com/qiin2333/moonlight-vplus) 的分支，**只做双屏掌机（如 AYN Thor）的加强**。

- **单屏**：保持上游原有布局和交互，只允许套用统一的颜色 token（`panel_*`）。不要为单屏重做布局、改交互流程。
- **双屏**：在单屏基础上多一块副屏界面（浏览副屏、串流副屏、副屏游戏菜单）。新功能和新布局都放在副屏。
- 代码上两条路径分开：副屏专用的界面写成独立文件（如 `gamemenu/CompanionGameMenu.kt`），只在"有副屏"时进入（例如 `game.companionMenuDisplay() != null` → `fullscreen`）；单屏走原代码，改动后单屏行为必须逐字节不变。
- 上游冲突热点（`Game`、`PcView`、`gamemenu/*` 等）只做最小接线，逻辑放到新文件里。热点清单见 [DUAL_CHANGES.md](DUAL_CHANGES.md)。

## 副屏设计规范

所有副屏界面按 [COMPANION_DESIGN_SPEC.md](COMPANION_DESIGN_SPEC.md) 设计和实现：
- 颜色：`res/values/panel_colors.xml`（Compose 用 `CompanionTheme.colors`），禁止十六进制。
- 字号 / 尺寸：`res/values/companion_type.xml` ↔ `ui/companion/CompanionDesign.kt`（`CompanionType`、`CompanionDimens`）。
- 风格：Steam 深蓝；可点 = 蓝灰卡片，展示 = 深色面板；不用大面积彩色色块。
- 新颜色、新字号先改规范和 token，再写界面。

## 相关文档

| 文件 | 内容 |
| --- | --- |
| [DUAL_DESIGN.md](DUAL_DESIGN.md) | 单屏/双屏自适应设计与阶段 |
| [DUAL_CHANGES.md](DUAL_CHANGES.md) | 与上游的差异、冲突热点、同步记录、测试状态 |
| [COMPANION_DESIGN_SPEC.md](COMPANION_DESIGN_SPEC.md) | 副屏设计规范（颜色、字号、尺寸、交互） |
| `design/*.html` | 设计稿（浏览器打开，`?p=` 切页） |
| `.claude/skills/sync-upstream` | 同步上游流程 |

## 构建与测试

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
sh ./gradlew :app:assembleNonRootDebug -PaudioHapticsSdkDir="$PWD/../moonlight-audio-haptics"
sh ./gradlew :app:testNonRootDebugUnitTest -PaudioHapticsSdkDir="$PWD/../moonlight-audio-haptics"
```

- APK：`app/build/outputs/apk/nonRoot/debug/app-nonRoot-debug.apk`，包名 `com.limelight.vplus_debug`。
- 规范防回退：`PanelThemeTokensTest`、`CompanionDesignTokensTest`。

## 实机验证（AYN Thor）

- `adb install -r <apk>`；副屏截图 `adb exec-out screencap -p -d 4630946482288158084`；副屏点击 `adb shell input -d 4 tap x y`（副屏 1240×1080 px）。
- adb 注入的手柄键（如 `BUTTON_R1`）不一定送到副屏窗口，手柄交互要用真手柄确认。
- 串流需要主机在线（用户的 Windows 机 0x77）。

---
name: sync-upstream
description: 把上游 qiin2333/moonlight-vplus 的修复同步进我们的 Moonlight Dual 分支，且不破坏本地改动；含编译、双屏模拟、测试的固定做法。同步上游、合并 upstream、验证构建时使用。
---

# 同步上游 moonlight-vplus -> Moonlight Dual

我们的改动 = 上游 `4532ae6d` 之上的一个大提交 `b4e36838`（Steam 风格双屏 UI，170 个文件）。
完整改动清单见 `DUAL_CHANGES.md`。**同步永远在独立分支做，不直接动 master。**

## 远端
- `origin`   = https://github.com/0x1428571429/moonlight-dual.git（我们的）
- `upstream` = https://github.com/qiin2333/moonlight-vplus.git（上游）
缺 upstream 时：`git remote add upstream https://github.com/qiin2333/moonlight-vplus.git`

## 流程
1. 工作区干净：`git status`（有改动先提交或 stash）。
2. `git fetch upstream`，看有什么新东西：`git log --oneline HEAD..upstream/master`。
3. 新建/复用同步分支：`git switch -c sync-YYYYMMDD`（或已有 `sync-upstream`）。
4. `git merge upstream/master`（用 merge，不 rebase：我们是单个大提交，rebase 会反复冲突）。
5. 解冲突，按下面的"冲突策略"。
6. 构建 -> 单元测试 -> 模拟器冒烟（见下）。全部通过再让用户决定是否 push / 合入 master。
7. **不经用户同意不 push、不强推。**

## 冲突策略
- 我们重写/删除过的文件（见 DUAL_CHANGES.md 热点表）：以**我们的版本为主**，只把上游的功能/修复性改动手工带入。
- `AppThemeSheet.kt` 我们已删（主题固定为深色+粉色，没有设置入口）。上游若再改它，保持删除；它引入的新能力（如暗黑桌面图标 `LauncherIcon`）合入但没有 UI 入口，记入 DUAL_CHANGES.md。
- `AboutDialog` 参数以我们的为准（已移除 `onQq`）；上游若新增参数，同步改 `AboutDialogInputTest` 等 androidTest 调用处。
- 文案/资源：`res/values-*` 新增的字符串保留上游。
- 版本号、`chore(release)` 提交：直接取上游。
- 合并后 `grep -rn "<<<<<<<\|>>>>>>>" app/src` 必须为空。

## 构建（Mac）
前置：JDK 17、Android SDK（`local.properties` 的 `sdk.dir=~/Library/Android/sdk`）、NDK `28.2.13676358`、CMake `3.22.1`。
```bash
git submodule update --init --recursive
# 音频触觉 SDK 要固定到 CI 指定的提交（以 .github/workflows/android-ci.yml 的 AUDIO_HAPTICS_SDK_REF 为准，目前 b3f97c3bb7500ea7b1985aea568e5c7b40308d3b）
git -C ../moonlight-audio-haptics fetch && git -C ../moonlight-audio-haptics checkout <REF>
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
sh ./gradlew :app:assembleNonRootDebug -PaudioHapticsSdkDir="$PWD/../moonlight-audio-haptics"
```
- gradlew 没有执行权限，用 `sh ./gradlew`。
- 报 `audioHapticsSdkDir is required` = 没传 `-P`。
- APK：`app/build/outputs/apk/nonRoot/debug/app-nonRoot-debug.apk`，包名 `com.limelight.vplus_debug`，入口 `com.limelight.PcView`。

## 测试
- 单元：`sh ./gradlew :app:testNonRootDebugUnitTest -PaudioHapticsSdkDir=...`（当前 1361 个全过）。
- 仪器：`:app:connectedNonRootDebugAndroidTest`（约 304 个、30 分钟；先起模拟器）。当前基线 45 个失败，分类见 DUAL_CHANGES.md "测试状态"。**新增失败才算回归**，对照基线比较。

## 双屏模拟（Pixel_9_API36）
```bash
adb shell wm size 1920x1080; adb shell wm density 320
adb shell settings put global overlay_display_devices "1240x1080/320"   # 关闭：none；wm size reset
adb exec-out screencap -p > main.png                                     # 主屏
adb exec-out screencap -d 11529215046417674965 -p > second.png           # 副屏
adb shell input -d 2 tap 693 83     # 副屏 tab：693=主机 913=串流设置 1131=菜单
```
副屏要是 PRESENTATION 且物理面积小于主屏，`CompanionDisplays.find` 才会选中它。
冒烟清单：启动不崩 -> 主机列表 -> 副屏三个 tab（选中文字白色、样式一致）-> 设置各页 -> About。
串流/配对需要真实主机：可用 `tools/mock-sunshine`，或经用户同意再连 0x77（先 `win-status` 预检）。

## 同步后收尾
1. 更新 `DUAL_CHANGES.md`：同步记录表加一行（日期、上游版本、冲突文件、结果）。
2. 新踩的坑写进本文件。
3. 提交（commit 信息写清上游合到哪个提交/版本）。

## 已知坑
- PcView：`onResume` 里 `showBrowseCompanion()` 须在 `completeOnCreate` 之后（有 `completeOnCreateCalled` 守卫），否则 `pcGridAdapter` 未初始化崩溃。上游改 PcView 时留意此守卫别被冲掉。
- Debug 构建开了 R8 (`minifyEnabled true`)，会让部分仪器测试 NoSuchField/NoSuchMethod（`device_keyboard_scroll`、`keyboard_key_picker`、`MicrophoneMenuState.copy$default`）——是构建配置问题，不是功能回归。
- 副屏 tab 统一用 `@style/CompanionTab`（`values/styles.xml`），新增 tab 不要再手写属性；选中色 `companion_tab_text.xml` 为白色。

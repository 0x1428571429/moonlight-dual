#!/usr/bin/env bash
# Moonlight Dual 打包与发布。
#
#   scripts/dual-release.sh version <tag>     打印 tag 推导出的 versionName / versionCode
#   scripts/dual-release.sh build   <tag>     构建签名的 release APK + 发布说明到 build/dual-release/
#   scripts/dual-release.sh publish <序号>    本地一条龙：检查 → 构建 → 打 tag → 推送 → 建 GitHub Release
#
# tag 格式：dual-v<上游版本>-<序号>，如 dual-v12.15.7-1
#   versionName = 12.15.7-dual.1
#   versionCode = MAJOR*10^7 + MINOR*10^5 + PATCH*10^3 + 序号（序号 1..999）
# 包名：com.limelight.dual（release 后缀 .dual），可与上游 Moonlight V+ 共存。
#
# 签名：读取环境变量 KEYSTORE_PATH / KEYSTORE_PASSWORD / KEY_ALIAS / KEY_PASSWORD；
# 本地没设时自动 source ~/.config/moonlight-dual/signing.env。
# 密钥丢了就无法覆盖升级已安装的版本，务必备份 ~/.config/moonlight-dual/。
set -euo pipefail

REPO="${DUAL_REPO:-0x1428571429/moonlight-dual}"
FORK_POINT=4532ae6d
OUT_DIR=build/dual-release
ROOT=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
cd "$ROOT"

die() { echo "错误：$*" >&2; exit 1; }

parse_tag() {
  local tag=$1
  [[ "$tag" =~ ^dual-v([0-9]+)\.([0-9]+)\.([0-9]+)-([0-9]+)$ ]] ||
    die "tag '$tag' 不符合 dual-v<MAJOR.MINOR.PATCH>-<序号>"
  local ma=$((10#${BASH_REMATCH[1]})) mi=$((10#${BASH_REMATCH[2]})) pa=$((10#${BASH_REMATCH[3]})) n=$((10#${BASH_REMATCH[4]}))
  (( n >= 1 && n <= 999 )) || die "序号必须在 1..999"
  (( mi <= 99 && pa <= 99 )) || die "MINOR/PATCH 必须 ≤ 99"
  BASE="$ma.$mi.$pa"
  VERSION_NAME="$BASE-dual.$n"
  VERSION_CODE=$(( ma * 10000000 + mi * 100000 + pa * 1000 + n ))
}

upstream_version() {
  sed -nE 's/^[[:space:]]*versionName "([0-9.]+)".*/\1/p' app/build.gradle | head -n1
}

load_signing() {
  if [ -z "${KEYSTORE_PATH:-}" ] && [ -f "$HOME/.config/moonlight-dual/signing.env" ]; then
    # shellcheck disable=SC1091
    source "$HOME/.config/moonlight-dual/signing.env"
  fi
  for v in KEYSTORE_PATH KEYSTORE_PASSWORD KEY_ALIAS KEY_PASSWORD; do
    [ -n "${!v:-}" ] || die "缺少签名变量 $v（见脚本开头说明）"
  done
  [ -f "$KEYSTORE_PATH" ] || die "找不到 keystore：$KEYSTORE_PATH"
  export KEYSTORE_PATH KEYSTORE_PASSWORD KEY_ALIAS KEY_PASSWORD
}

find_apksigner() {
  local sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
  if [ -z "$sdk" ] && [ -f local.properties ]; then
    sdk=$(sed -n 's/^sdk.dir=//p' local.properties)
  fi
  ls -d "$sdk"/build-tools/*/apksigner 2>/dev/null | sort -V | tail -n1
}

write_notes() {
  local tag=$1 apk=$2 notes=$3
  local prev range sha
  prev=$(git tag --list 'dual-v*' --sort=-creatordate | grep -Fxv "$tag" | head -n1 || true)
  range="${prev:-$FORK_POINT}..HEAD"
  sha=$(shasum -a 256 "$apk" | awk '{print $1}')
  {
    echo "## Moonlight Dual $VERSION_NAME"
    echo
    echo "双屏掌机（AYN Thor 等）增强版，基于 Moonlight V+ $BASE。单屏交互与上游保持一致。"
    echo
    echo "### 更新内容（${prev:-首个 Dual 版本}以来）"
    git log --first-parent --no-merges --pretty=format:'- %s' "$range"
    echo
    echo
    echo "### 安装"
    echo "- 包名 \`com.limelight.dual\`，可与 Moonlight / Moonlight V+ 共存。"
    echo "- 后续版本使用同一签名，可直接覆盖升级。"
    echo
    echo "### SHA256"
    echo '```text'
    echo "$sha  $(basename "$apk")"
    echo '```'
  } > "$notes"
}

cmd_build() {
  local tag=$1
  parse_tag "$tag"
  local up; up=$(upstream_version)
  [ "$up" = "$BASE" ] || echo "提示：tag 基础版本 $BASE 与 build.gradle 的上游版本 $up 不同" >&2
  load_signing

  local sdk_dir="${AUDIO_HAPTICS_SDK_DIR:-$ROOT/../moonlight-audio-haptics}"
  [ -d "$sdk_dir" ] || die "找不到 moonlight-audio-haptics：$sdk_dir"

  echo "构建 $VERSION_NAME ($VERSION_CODE)…"
  sh ./gradlew :app:assembleNonRootRelease --no-daemon --stacktrace \
    -PaudioHapticsSdkDir="$sdk_dir" \
    -PdualVersionName="$VERSION_NAME" \
    -PdualVersionCode="$VERSION_CODE" \
    -PreleaseAppIdSuffix=.dual

  local src; src=$(ls app/build/outputs/apk/nonRoot/release/*.apk | head -n1)
  [ -n "$src" ] || die "没有产出 APK"
  rm -rf "$OUT_DIR"; mkdir -p "$OUT_DIR"
  APK="$OUT_DIR/Moonlight-Dual-$VERSION_NAME.apk"
  cp "$src" "$APK"
  [ -f app/build/outputs/mapping/nonRootRelease/mapping.txt ] &&
    gzip -9c app/build/outputs/mapping/nonRootRelease/mapping.txt > "$OUT_DIR/mapping-$VERSION_NAME.txt.gz"

  local signer; signer=$(find_apksigner)
  if [ -n "$signer" ]; then
    "$signer" verify --print-certs "$APK" | grep -m1 'certificate SHA-256' || die "APK 签名校验失败"
  else
    echo "提示：没找到 apksigner，跳过签名校验" >&2
  fi
  write_notes "$tag" "$APK" "$OUT_DIR/release-notes.md"
  echo "产物："; ls -l "$OUT_DIR"
}

cmd_publish() {
  local n=$1
  local up; up=$(upstream_version)
  local tag="dual-v$up-$n"
  parse_tag "$tag"

  command -v gh >/dev/null || die "需要 gh"
  [ -z "$(git status --porcelain --untracked-files=no)" ] || die "工作区有未提交改动"
  [ "$(git rev-parse --abbrev-ref HEAD)" = master ] || die "请在 master 上发布"
  git fetch -q origin master
  [ "$(git rev-parse HEAD)" = "$(git rev-parse origin/master)" ] || die "本地 master 与 origin/master 不一致，先推送/拉取"
  git rev-parse -q --verify "refs/tags/$tag" >/dev/null && die "tag $tag 已存在"
  gh release view "$tag" -R "$REPO" >/dev/null 2>&1 && die "Release $tag 已存在"

  cmd_build "$tag"

  git tag -a "$tag" -m "Moonlight Dual $VERSION_NAME"
  git push origin "refs/tags/$tag"
  # 打 tag 后重新生成说明，保证区间正确
  write_notes "$tag" "$APK" "$OUT_DIR/release-notes.md"

  local extra=()
  [ -f "$OUT_DIR/mapping-$VERSION_NAME.txt.gz" ] && extra+=("$OUT_DIR/mapping-$VERSION_NAME.txt.gz")
  gh release create "$tag" -R "$REPO" \
    --title "Moonlight Dual $VERSION_NAME" \
    --notes-file "$OUT_DIR/release-notes.md" \
    "$APK" ${extra[@]+"${extra[@]}"}
}

case "${1:-}" in
  version) parse_tag "${2:?tag}"; echo "$VERSION_NAME $VERSION_CODE" ;;
  build)   cmd_build "${2:?tag}" ;;
  publish) cmd_publish "${2:?序号}" ;;
  *) sed -n '2,15p' "$0"; exit 1 ;;
esac

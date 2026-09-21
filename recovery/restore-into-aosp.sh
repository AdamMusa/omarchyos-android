#!/usr/bin/env bash
# Run inside the builder container: overlays the recovered Omarchy tree onto a synced
# AOSP checkout at /workspace and rebuilds pieces that lived outside git.
set -Eeuo pipefail
SRC=${SRC:-/omarchy-src}
WS=${WS:-/workspace}
cd "$WS"
T=$(mktemp -d)

echo "== 1. Omarchy sources"
rsync -a --exclude .git \
  "$SRC"/device "$SRC"/packages "$SRC"/manifests "$WS"/
[ -e "$SRC"/frameworks ] && rsync -a "$SRC"/frameworks "$WS"/ || true
cp "$SRC"/omarchy "$WS"/omarchy

echo "== 2. OmarchyCode base (AOSP packages/apps/Terminal)"
git clone -q --depth 1 https://android.googlesource.com/platform/packages/apps/Terminal "$T/Terminal"
rm -rf "$T/Terminal/.git"
cp -Rn "$T/Terminal"/. "$WS/packages/apps/OmarchyCode"/

echo "== 3. libvterm (android-11.0.0_r26)"
git clone -q --depth 1 --branch android-11.0.0_r26 https://android.googlesource.com/platform/external/libvterm "$T/libvterm"
V="$WS/packages/apps/OmarchyCode/third_party/libvterm"
cp -R "$T/libvterm/include" "$T/libvterm/src" "$T/libvterm/LICENSE" "$V"/

echo "== 4. Upstream patches"
P="$SRC/recovery/upstream-patches"
order=(
  packages/apps/OmarchyCode/Android.bp.patch
  packages/apps/OmarchyCode/AndroidManifest.xml.patch
  packages/apps/OmarchyCode/jni/Android.bp.patch
  packages/apps/OmarchyCode/jni/com_android_terminal_Terminal.cpp.patch
  packages/apps/OmarchyCode/res/layout/activity.xml.patch
  packages/apps/OmarchyCode/res/menu/activity.xml.patch
  packages/apps/OmarchyCode/res/values/strings.xml.patch
  packages/apps/OmarchyCode/src/com/android/terminal/Terminal.java.patch
  packages/apps/OmarchyCode/src/com/android/terminal/TerminalService.java.patch
  packages/apps/OmarchyCode/src/com/android/terminal/TerminalView.java.patch
  packages/apps/Settings/AndroidManifest.xml.patch
  packages/apps/Settings/res/values/strings.xml.patch
  packages/apps/Settings/src/com/android/settings/FallbackHome.java.patch
  frameworks/base/packages/SystemUI/src/com/android/systemui/qs/panels/ui/viewmodel/toolbar/EditModeButtonViewModel.kt.patch
  frameworks/base/packages/SettingsLib/Spa/spa/src/com/android/settingslib/spa/framework/theme/SettingsRadius.kt.patch
  frameworks/base/packages/SettingsLib/Spa/spa/src/com/android/settingslib/spa/framework/theme/SettingsShape.kt.patch
  frameworks/base/packages/SettingsLib/Spa/spa/src/com/android/settingslib/spa/framework/theme/SettingsTheme.kt.patch
  frameworks/base/packages/SystemUI/src/com/android/systemui/navigationbar/gestural/NavigationHandle.java.patch
  frameworks/base/packages/SystemUI/src/com/android/systemui/statusbar/phone/PhoneStatusBarView.java.patch
  frameworks/base/packages/SystemUI/src/com/android/systemui/statusbar/pipeline/shared/ui/composable/StatusBarRoot.kt.patch
  frameworks/base/packages/SystemUI/compose/core/src/com/android/compose/theme/PlatformTheme.kt.patch
  frameworks/base/packages/SystemUI/compose/features/src/com/android/systemui/shade/ui/composable/ShadeHeader.kt.patch
  frameworks/base/packages/SystemUI/compose/features/src/com/android/systemui/qs/ui/composable/QuickSettingsScene.kt.patch
  frameworks/base/packages/SystemUI/compose/features/src/com/android/systemui/keyguard/ui/composable/elements/ElementProviderModule.kt.patch
  frameworks/base/packages/SystemUI/compose/features/src/com/android/systemui/keyguard/ui/composable/elements/NotificationStackElementProvider.kt.patch
  frameworks/base/packages/SystemUI/src/com/android/systemui/statusbar/phone/KeyguardStatusBarView.java.patch
  frameworks/base/packages/SettingsLib/Spa/spa/src/com/android/settingslib/spa/widget/scaffold/SettingsTopAppBar.kt.patch
  packages/apps/Settings/src/com/android/settings/core/SettingsBaseActivity.java.patch
  device/generic/goldfish/build/tools/mk_combined_img.py.patch
  device/generic/goldfish/sepolicy/system_ext/private/platform_app.te.patch
)
for p in "${order[@]}"; do
  python3 "$SRC/recovery/apply_codex_patch.py" "$WS" "$P/$p" || { echo "PATCH FAILED: $p"; FAILED=1; }
done

echo "== 5. Codex + Claude Code prebuilts (aarch64 musl)"
C="$WS/packages/apps/OmarchyCode/prebuilts"
V=$C/codex/vendor/aarch64-unknown-linux-musl
mkdir -p "$V/bin" "$V/codex-path" "$V/codex-resources/zsh/bin" "$C/claude/musl"
curl -fsSL https://registry.npmjs.org/@openai/codex/-/codex-0.152.1-linux-arm64.tgz -o "$T/codex.tgz"
tar -xzf "$T/codex.tgz" -C "$T"
PV="$T/package/vendor/aarch64-unknown-linux-musl"
cp "$PV/bin/codex" "$PV/bin/codex-code-mode-host" "$V/bin/"
cp "$PV/codex-package.json" "$V/"
cp "$PV/codex-path/rg" "$V/codex-path/"
cp "$PV/codex-resources/bwrap" "$V/codex-resources/"
cp "$PV/codex-resources/zsh/bin/zsh" "$V/codex-resources/zsh/bin/"
curl -fsSL https://downloads.claude.ai/claude-code-releases/2.1.258/linux-arm64-musl/claude -o "$C/claude/claude.real"
echo "d3bef6ba403fe3efba684fa93e1bb26bc7a08c054bd516639d54e77c77bb9821  $C/claude/claude.real" | sha256sum -c -
curl -fsSL https://dl-cdn.alpinelinux.org/alpine/v3.22/main/aarch64/musl-1.2.5-r12.apk -o "$T/musl.apk"
tar -xzf "$T/musl.apk" -C "$T" lib/ld-musl-aarch64.so.1 2>/dev/null || tar -xzf "$T/musl.apk" -C "$T"
cp -L "$T/lib/ld-musl-aarch64.so.1" "$C/claude/musl/ld-musl-aarch64.so.1"
cp -L "$T/lib/ld-musl-aarch64.so.1" "$C/claude/musl/libc.musl-aarch64.so.1"
chmod 0755 "$V"/bin/* "$V/codex-path/rg" "$V/codex-resources/bwrap" "$V/codex-resources/zsh/bin/zsh" "$C/claude/claude.real" "$C"/claude/musl/*

echo "== 6. Boot animation"
python3 -c 'import PIL' 2>/dev/null || sudo apt-get install -y -qq python3-pil fonts-dejavu-core >/dev/null
python3 "$WS/device/omarchy/bootanimation/generate.py" --output "$WS/device/omarchy/bootanimation/bootanimation.zip"

rm -rf "$T"
[ -z "${FAILED:-}" ] && echo "RESTORE OK" || { echo "RESTORE HAD PATCH FAILURES"; exit 3; }

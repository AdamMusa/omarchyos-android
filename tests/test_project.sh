#!/usr/bin/env bash
set -Eeuo pipefail

project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
failures=0

pass() {
  printf 'ok - %s\n' "$1"
}

fail() {
  printf 'not ok - %s\n' "$1" >&2
  failures=$((failures + 1))
}

assert_file() {
  local path="$1"
  [[ -f "$project_root/$path" ]] && pass "$path exists" || fail "$path exists"
}

assert_contains() {
  local path="$1" pattern="$2" description="$3"
  rg -q -- "$pattern" "$project_root/$path" && pass "$description" || fail "$description"
}

assert_not_contains() {
  local path="$1" pattern="$2" description="$3"
  if rg -qi -- "$pattern" "$project_root/$path"; then
    fail "$description"
  else
    pass "$description"
  fi
}

bash -n "$project_root/omarchy" && pass 'omarchy command has valid shell syntax' || fail 'omarchy command has valid shell syntax'

assert_file README.md
assert_file docs/ARCHITECTURE.md
assert_file device/omarchy/virtual/AndroidProducts.mk
assert_file packages/apps/OmarchyCore/Android.bp
assert_file packages/apps/OmarchyCode/Android.bp
assert_file packages/apps/OmarchyCode/scripts/codex
assert_file packages/apps/OmarchyCode/scripts/claude
assert_file packages/apps/OmarchyCode/compat/resolv_compat.c
assert_file packages/apps/OmarchyCode/res/raw/omarchy_mobile_skill.md

assert_contains omarchy '-no-metrics' 'emulator launcher does not block on a metrics prompt'
assert_contains omarchy '-crash-report-mode disabled' 'emulator launcher does not block on crash-report setup'

assert_contains docs/ARCHITECTURE.md 'signed with the same certificate' 'plugin trust boundary is documented'
assert_contains docs/ARCHITECTURE.md 'Runtime Resource Overlay' 'theme architecture uses Android RROs'
assert_contains device/omarchy/virtual/omarchy_common.mk 'OmarchyCore' 'product installs the plugin host'
assert_contains device/omarchy/virtual/omarchy_common.mk 'OmarchyCode' 'product installs the native Code workspace'
assert_not_contains device/omarchy/virtual/omarchy_common.mk 'OmarchyAgent' 'product no longer ships the simulated Agent'
assert_not_contains device/omarchy 'quickshell|novnc|wayvnc' 'Android product has no Linux shell or VNC dependency'
assert_contains packages/apps/OmarchyCore/src/os/omarchy/core/PluginRegistry.java 'checkSignatures' 'plugin registry verifies the OS signature'
assert_contains packages/apps/OmarchyCore/src/os/omarchy/core/PluginRegistry.java 'FLAG_SYSTEM' 'plugin registry accepts only system packages'
assert_contains packages/apps/OmarchyCore/src/os/omarchy/core/ThemeController.java 'ThemeManager' 'themes use Android framework Theme Service'
assert_contains packages/apps/OmarchyCore/src/os/omarchy/core/ThemeController.java 'KEY_PENDING' 'theme switching records crash-safe pending state'
assert_contains packages/apps/OmarchyCore/src/os/omarchy/core/PluginRegistry.java 'application.hasCode' 'theme registry rejects executable theme packs'
assert_contains packages/apps/OmarchyCore/src/os/omarchy/core/PluginEventDispatcher.java 'IOmarchyPlugin' 'plugin events cross the Binder boundary'
assert_contains packages/apps/OmarchyCore/contract/src/os/omarchy/plugin/PluginContract.java 'API_VERSION' 'plugin contract is versioned'
assert_contains device/omarchy/virtual/omarchy_common.mk 'OmarchyThemeCatppuccinLatteCore' 'product includes dark and light reference themes'
assert_contains packages/apps/OmarchyCode/AndroidManifest.xml 'android:process=":plugin"' 'Code plugin runs out of process'
assert_contains packages/apps/OmarchyCode/AndroidManifest.xml 'stateAlwaysHidden' 'Code keyboard stays hidden until explicit input'
assert_contains packages/apps/OmarchyCode/src/com/android/terminal/TerminalView.java 'showKeyboard' 'Code terminal exposes explicit keyboard focus'
assert_not_contains packages/apps/OmarchyCode 'ic_media_previous' 'Code navigation never uses a media transport icon'
assert_contains packages/apps/OmarchyCode/src/com/android/terminal/TerminalActivity.java 'ic_ab_back_material' 'Code terminal uses the platform Material back arrow'
assert_contains packages/apps/OmarchyCode/src/com/android/terminal/ProjectActivity.java 'ic_ab_back_material' 'Code project viewer uses the platform Material back arrow'
assert_contains packages/apps/OmarchyCode/scripts/codex 'danger-full-access' 'Codex relies on the outer Android app sandbox'
assert_contains packages/apps/OmarchyCode/scripts/claude 'ld-musl-aarch64' 'Claude uses its pinned musl runtime loader'
assert_contains packages/apps/OmarchyCode/scripts/claude '/system_ext/lib64/omarchy-code/claude/libomarchy_resolv_compat\.so' 'Claude preloads the installed Android DNS compatibility library'
assert_contains packages/apps/OmarchyCode/src/com/android/terminal/TerminalService.java 'getDnsServers' 'Claude DNS follows Android network properties'
assert_contains packages/apps/OmarchyCode/scripts/claude 'CLAUDE_CODE_PROXY_RESOLVES_HOSTS' 'Claude delegates hostname resolution to the Android bridge'
assert_contains packages/apps/OmarchyCode/src/com/android/terminal/TerminalService.java 'Proxy-Authorization' 'Claude bridge requires per-session proxy authentication'
assert_contains packages/apps/OmarchyCode/src/com/android/terminal/TerminalService.java 'InetAddress\.getByName\("127\.0\.0\.1"\)' 'Claude bridge listens on the IPv4 address advertised to the CLI'
assert_contains packages/apps/OmarchyCode/src/com/android/terminal/TerminalService.java 'target\.port != 443' 'Claude bridge permits only TLS tunnels'
assert_contains packages/apps/OmarchyCode/src/com/android/terminal/ProjectManifest.java 'isWithin' 'generated projects stay inside the Code workspace'
assert_contains packages/apps/OmarchyCode/src/com/android/terminal/PublishActivity.java 'requestPinShortcut' 'generated apps use Android launcher pinning'
assert_contains packages/apps/OmarchyCode/src/com/android/terminal/TerminalService.java 'consumePublishRequest' 'publish requests return through the app-owned service'
assert_not_contains packages/apps/OmarchyCode/scripts/omarchy-mobile '/system/bin/am' 'publish bridge does not grant a shell Activity Manager path'
assert_contains device/generic/goldfish/sepolicy/system_ext/private/omarchy_code_app.te 'create_pty\(omarchy_code_app\)' 'Code receives an isolated pseudo-terminal domain'
assert_contains device/generic/goldfish/sepolicy/system_ext/private/seapp_contexts 'name=com.android.terminal domain=omarchy_code_app' 'PTY policy is scoped to the Code package'
assert_not_contains device/generic/goldfish/build/tools/mk_combined_img.py 'os.path.exists(output_filename) and len(partitions) == 2' 'emulator image rebuilds never reuse stale GPT boundaries'
assert_contains device/generic/goldfish/build/tools/mk_combined_img.py 'gpt_tail = prebuilt_gpt_dir \+ "/tail.img"' 'emulator image uses the matching GPT tail image'
assert_contains packages/apps/Settings/AndroidManifest.xml 'OmarchySettingsActivity' 'forked Android Settings exposes Omarchy controls'
assert_not_contains packages/apps/OmarchyCore 'EditText' 'non-Code Omarchy surfaces have no editable fields'
assert_contains frameworks/base/packages/SystemUI/src/com/android/systemui/qs/panels/ui/viewmodel/toolbar/EditModeButtonViewModel.kt 'setEditTooltipShown\(true\)' 'SystemUI suppresses the first-open Quick Settings coachmark'
assert_contains device/omarchy/overlay/frameworks/base/packages/SystemUI/res/values/config.xml 'quick_settings_large_tiles_default_split' 'Omarchy defaults Quick Settings to compact mobile tiles'
assert_contains device/omarchy/virtual/omarchy_common.mk 'PRODUCT_PACKAGE_OVERLAYS' 'the product applies shell resource policy at build time'
assert_contains device/omarchy/overlay/packages/apps/Launcher3/res/xml/default_workspace_5x5.xml 'SettingsHomepageActivity' 'phone home defaults include reliable system routing'
assert_not_contains device/omarchy/overlay/packages/apps/Launcher3/res/xml/default_workspace_5x5.xml 'com.android.terminal' 'Code remains in installed apps instead of occupying Home'

for fork_path in frameworks/base frameworks/libs/systemui packages/apps/Launcher3 \
    packages/apps/Settings packages/apps/ThemePicker; do
  branch="$(git -C "$project_root/$fork_path" branch --show-current 2>/dev/null || true)"
  if [[ "$branch" == "omarchy/android-latest-release" ]]; then
    pass "$fork_path is on the Omarchy fork branch"
  else
    fail "$fork_path is on the Omarchy fork branch"
  fi
done

python3 - "$project_root" <<'PY' && pass 'Android XML files are well formed' || fail 'Android XML files are well formed'
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

root = Path(sys.argv[1])
paths = [
    *root.glob("device/omarchy/**/*.xml"),
    *root.glob("packages/apps/Omarchy*/**/*.xml"),
    *root.glob("packages/overlays/OmarchyThemes/**/*.xml"),
]
if not paths:
    raise SystemExit("no Android XML files found")
for path in paths:
    ET.parse(path)
PY

if [[ "$failures" -ne 0 ]]; then
  printf '%s project check(s) failed\n' "$failures" >&2
  exit 1
fi

printf 'All project checks passed.\n'

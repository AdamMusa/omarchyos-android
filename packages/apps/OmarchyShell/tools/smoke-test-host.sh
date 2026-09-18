#!/usr/bin/env bash
# Loads the vendored Omarchy shell against quickshell-compat using the Linux
# host Qt, offscreen. It cannot render a phone UI, but it resolves every import
# and reports exactly which Quickshell APIs are still missing — the fastest way
# to drive the port without waiting on an Android build.
set -Eeuo pipefail
source /toolchain/env.sh
APP=${APP:-/workspace/packages/apps/OmarchyShell}
export QT_QPA_PLATFORM=offscreen
export QML_IMPORT_PATH="$APP/quickshell-compat:$APP/third_party/omarchy"
export QML2_IMPORT_PATH="$QML_IMPORT_PATH"
export OMARCHY_PATH="$APP/third_party/omarchy"
timeout "${TIMEOUT:-60}" "$QT_HOST/bin/qml" \
  -I "$APP/quickshell-compat" \
  -I "$APP/third_party/omarchy" \
  "$APP/third_party/omarchy/shell/shell.qml" 2>&1 | head -"${LINES:-80}"

#!/usr/bin/env bash
set -euo pipefail
root=$(cd "$(dirname "$0")/.." && pwd)
classes=$(mktemp -d)
trap 'rm -rf "$classes"' EXIT
javac -d "$classes" \
  "$root/app/android/src/os/omarchy/shell/ThemePalette.java" \
  "$root/app/android/src/os/omarchy/shell/ThemeArchive.java" \
  "$root/tools/theme-tests/ThemeDataTest.java"
java -cp "$classes" os.omarchy.shell.ThemeDataTest \
  "$root/third_party/omarchy/themes" "$root/mobile/wallpapers"

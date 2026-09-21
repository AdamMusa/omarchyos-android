#!/usr/bin/env bash
# Builds OmarchyShell for arm64 Android using the Qt toolchain and leaves the
# APK in /workspace/.build/android-shell.
set -Eeuo pipefail
source /toolchain/env.sh
A=${A:-/workspace/packages/apps/OmarchyShell}
B=${B:-/workspace/.build/android-shell}
mkdir -p "$B"
cd "$B"
bash "$A/tools/build-screensaver.sh" "$B/screensaver-native"

# Older staged desktop assets may include dangling symlinks. Qt enumerates
# this directory during configure, before the asset staging target can fix it.
if [[ -d "$A/app/android/assets/omarchy" ]]; then
  find "$A/app/android/assets/omarchy" -xtype l -delete
fi

"$QT_ANDROID/bin/qt-cmake" -S "$A" -B "$B" -G Ninja \
  -DCMAKE_BUILD_TYPE=Release \
  -DQT_HOST_PATH="$QT_HOST" \
  -DANDROID_SDK_ROOT="$ANDROID_SDK_ROOT" \
  -DANDROID_NDK_ROOT="$ANDROID_NDK_ROOT" \
  -DQT_ANDROID_ABIS=arm64-v8a \
  -DOMARCHY_DREAM_BINARY="$B/screensaver-native/aarch64-linux-android/release/libomarchy_dream.so"

cmake --build "$B" -j"${JOBS:-6}"
cmake --build "$B" --target apk
find "$B" -name "*.apk" -printf "%p %k KB\n"

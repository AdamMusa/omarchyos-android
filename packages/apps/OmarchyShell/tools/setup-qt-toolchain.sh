#!/usr/bin/env bash
# Installs the Qt-for-Android toolchain used to build OmarchyShell.
# Runs inside the AOSP builder container; everything lands under /toolchain.
set -Eeuo pipefail
T=${T:-/toolchain}
QT_VER=${QT_VER:-6.8.3}
NDK_VER=${NDK_VER:-26.1.10909125}
SDK_PLATFORM=${SDK_PLATFORM:-android-35}
BUILD_TOOLS=${BUILD_TOOLS:-35.0.0}
sudo mkdir -p "$T" && sudo chown "$(id -u):$(id -g)" "$T"
cd "$T"

echo "== aqtinstall"
python3 -m venv "$T/venv"
"$T/venv/bin/pip" -q install --upgrade pip aqtinstall

echo "== Qt $QT_VER (linux host + android_arm64_v8a)"
"$T/venv/bin/aqt" install-qt linux desktop "$QT_VER" linux_gcc_64 -O "$T/qt" -m qtshadertools
"$T/venv/bin/aqt" install-qt linux android "$QT_VER" android_arm64_v8a -O "$T/qt" -m qtshadertools

echo "== Android SDK command-line tools"
mkdir -p "$T/android-sdk/cmdline-tools"
curl -fsSL https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -o "$T/cmdline.zip"
unzip -q -o "$T/cmdline.zip" -d "$T/android-sdk/cmdline-tools"
mv -n "$T/android-sdk/cmdline-tools/cmdline-tools" "$T/android-sdk/cmdline-tools/latest" 2>/dev/null || true
rm -f "$T/cmdline.zip"

export ANDROID_SDK_ROOT="$T/android-sdk"
export JAVA_HOME=${JAVA_HOME:-/workspace/prebuilts/jdk/jdk21/linux-x86}
SDKMGR="$T/android-sdk/cmdline-tools/latest/bin/sdkmanager"
yes | "$SDKMGR" --sdk_root="$ANDROID_SDK_ROOT" --licenses >/dev/null || true
"$SDKMGR" --sdk_root="$ANDROID_SDK_ROOT" "platforms;$SDK_PLATFORM" "build-tools;$BUILD_TOOLS" "platform-tools" "ndk;$NDK_VER"

echo "== gradle"
curl -fsSL https://services.gradle.org/distributions/gradle-8.10-bin.zip -o "$T/gradle.zip"
unzip -q -o "$T/gradle.zip" -d "$T"; rm -f "$T/gradle.zip"

cat > "$T/env.sh" <<ENV
export T=$T
export QT_HOST=$T/qt/$QT_VER/gcc_64
export QT_ANDROID=$T/qt/$QT_VER/android_arm64_v8a
export ANDROID_SDK_ROOT=$T/android-sdk
export ANDROID_NDK_ROOT=$T/android-sdk/ndk/$NDK_VER
export JAVA_HOME=/workspace/prebuilts/jdk/jdk21/linux-x86
export PATH=$T/gradle-8.10/bin:\$JAVA_HOME/bin:\$PATH
ENV
echo "TOOLCHAIN OK"; du -sh "$T"/* 2>/dev/null

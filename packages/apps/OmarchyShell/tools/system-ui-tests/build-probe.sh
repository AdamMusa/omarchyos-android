#!/usr/bin/env bash
# Local disposable QA app; never included in an Omarchy product.
set -Eeuo pipefail
: "${ANDROID_SDK_ROOT:?Set ANDROID_SDK_ROOT to an Android SDK with API 35}"
[[ $# == 1 ]] || { echo "Usage: $0 OUTPUT_DIRECTORY" >&2; exit 2; }
source_dir=$(cd "$(dirname "$0")" && pwd)
mkdir -p "$1"
output=$(cd "$1" && pwd)
sdk_tools="$ANDROID_SDK_ROOT/build-tools/35.0.0"
framework="$ANDROID_SDK_ROOT/platforms/android-35/android.jar"
mkdir -p "$output/classes" "$output/dex"
"$sdk_tools/aapt2" link -o "$output/unsigned.apk" --manifest "$source_dir/AndroidManifest.xml" -I "$framework"
javac -source 8 -target 8 -cp "$framework" -d "$output/classes" "$source_dir/"*.java
find "$output/classes" -name '*.class' -print0 | xargs -0 "$sdk_tools/d8" --lib "$framework" --output "$output/dex"
(cd "$output/dex" && zip -q "$output/unsigned.apk" classes.dex)
"$sdk_tools/zipalign" -f 4 "$output/unsigned.apk" "$output/aligned.apk"
if [[ ! -f "$output/probe.jks" ]]; then
    keytool -genkeypair -keystore "$output/probe.jks" -storepass android -keypass android -alias probe \
        -keyalg RSA -validity 3650 -dname 'CN=Omarchy Local UI Validation'
fi
"$sdk_tools/apksigner" sign --ks "$output/probe.jks" --ks-pass pass:android --key-pass pass:android \
    --out "$output/probe.apk" "$output/aligned.apk"
echo "$output/probe.apk"

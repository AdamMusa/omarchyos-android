#!/usr/bin/env bash
# Incremental platform-API build for an existing Linux AOSP/Android SDK toolchain.
set -Eeuo pipefail
[[ $# == 3 ]] || { echo "Usage: $0 AOSP_ROOT PRODUCT_OUT OUTPUT_DIR" >&2; exit 2; }
app=$(cd "$(dirname "$0")/.." && pwd)
aosp=$(realpath "$1")
product=$(realpath "$2")
mkdir -p "$3"
result=$(realpath "$3")
: "${ANDROID_SDK_ROOT:?Set ANDROID_SDK_ROOT and use JDK 17 or newer}"
bt="$ANDROID_SDK_ROOT/build-tools/35.0.0"
framework="$aosp/out/soong/.intermediates/frameworks/base/framework-minus-apex/android_common/combined/framework.jar"
contract="$aosp/out/soong/.intermediates/packages/apps/OmarchyCore/contract/omarchy-plugin-contract/android_common/javac/omarchy-plugin-contract.jar"
mkdir -p "$result/gen" "$result/classes" "$result/dex"
"$bt/aapt2" compile --dir "$app/res" -o "$result/resources.zip"
"$bt/aapt2" link -I "$product/system/framework/framework-res.apk" \
  --manifest "$app/AndroidManifest.xml" --min-sdk-version 35 --target-sdk-version 35 \
  --java "$result/gen" -o "$result/unsigned.apk" "$result/resources.zip"
find "$app/src" "$result/gen" -name '*.java' > "$result/sources.txt"
javac -source 17 -target 17 \
  -cp "$framework:$aosp/prebuilts/sdk/current/system/android.jar:$contract" \
  -d "$result/classes" @"$result/sources.txt"
jar cf "$result/classes.jar" -C "$result/classes" .
"$bt/d8" --min-api 35 --lib "$ANDROID_SDK_ROOT/platforms/android-35/android.jar" \
  --output "$result/dex" "$result/classes.jar" "$contract"
(cd "$result/dex" && zip -q -u "$result/unsigned.apk" classes*.dex)
"$bt/zipalign" -f -p 4 "$result/unsigned.apk" "$result/aligned.apk"
"$bt/apksigner" sign --key "$aosp/build/target/product/security/platform.pk8" \
  --cert "$aosp/build/target/product/security/platform.x509.pem" \
  --out "$result/OmarchyCore.apk" "$result/aligned.apk"
echo "Built $result/OmarchyCore.apk with the development platform key"

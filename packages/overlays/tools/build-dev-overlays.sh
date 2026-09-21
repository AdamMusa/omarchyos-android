#!/usr/bin/env bash
# Compile code-free OS overlays against an existing matching development image.
set -Eeuo pipefail
[[ $# == 3 ]] || { echo "Usage: $0 AOSP_ROOT PRODUCT_OUT OUTPUT_DIR" >&2; exit 2; }
aosp=$(realpath "$1")
product=$(realpath "$2")
mkdir -p "$3"
output=$(realpath "$3")
source_dir=$(cd "$(dirname "$0")/.." && pwd)
: "${ANDROID_SDK_ROOT:?Source the Android toolchain environment first}"
build_tools="$ANDROID_SDK_ROOT/build-tools/35.0.0"
for module in OmarchyBoot OmarchyFramework OmarchySystemUI OmarchySettings OmarchyOverview; do
  work="$output/$module"
  mkdir -p "$work"
  "$build_tools/aapt2" compile --dir "$source_dir/$module/res" -o "$work/resources.zip"
  "$build_tools/aapt2" link -o "$work/unsigned.apk" --manifest "$source_dir/$module/AndroidManifest.xml" \
    -I "$product/system/framework/framework-res.apk" --auto-add-overlay \
    --min-sdk-version 35 --target-sdk-version 35 "$work/resources.zip"
  "$build_tools/zipalign" -f -p 4 "$work/unsigned.apk" "$work/aligned.apk"
  certificate=platform
  # Launcher3 uses the product default signing certificate in this dev image.
  [[ "$module" != OmarchyOverview ]] || certificate=testkey
  "$build_tools/apksigner" sign --key "$aosp/build/target/product/security/$certificate.pk8" \
    --cert "$aosp/build/target/product/security/$certificate.x509.pem" \
    --out "$output/$module.apk" "$work/aligned.apk"
done

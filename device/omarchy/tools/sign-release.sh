#!/usr/bin/env bash
# Build signed OmarchyOS release artifacts from a lunch'ed `user` build:
#   OMARCHY_BUILD_VARIANT=user ./omarchy build emulator  (or a device target)
#   m target-files-package otatools
#   device/omarchy/tools/sign-release.sh
# Replaces every AOSP test key (APKs, APEXes, verified boot) with the keys from
# make-release-keys.sh, then emits a signed target-files, full OTA and images.
set -Eeuo pipefail
KEYS=${OMARCHY_KEYS:-$HOME/.omarchy-keys}
: "${ANDROID_PRODUCT_OUT:?lunch first}"
[ "$(get_build_var TARGET_BUILD_VARIANT 2>/dev/null || echo user)" = user ] \
  || echo "warning: signing a non-user build" >&2
OUT=${OMARCHY_RELEASE_OUT:-$ANDROID_PRODUCT_OUT/release}
mkdir -p "$OUT"
TF=$(ls -t "$ANDROID_PRODUCT_OUT"/obj/PACKAGING/target_files_intermediates/*-target_files*.zip | head -1)
apex_args=()
while read -r name; do
  apex_args+=(--extra_apks "$name=$KEYS/apex" --extra_apex_payload_key "$name=$KEYS/apex.pem")
done < <(unzip -p "$TF" META/apexkeys.txt | sed -n 's/^name="\([^"]*\)".*/\1/p')
PATH="$ANDROID_HOST_OUT/bin:$PATH" sign_target_files_apks -o \
  -d "$KEYS" \
  --avb_vbmeta_key "$KEYS/avb.pem" --avb_vbmeta_algorithm SHA256_RSA4096 \
  --avb_system_key "$KEYS/avb.pem" --avb_system_algorithm SHA256_RSA4096 \
  --avb_vendor_key "$KEYS/avb.pem" --avb_vendor_algorithm SHA256_RSA4096 \
  --extra_apks GmsCompatLib.apk="$KEYS/gmscompat_lib" \
  "${apex_args[@]}" \
  "$TF" "$OUT/signed-target_files.zip"
PATH="$ANDROID_HOST_OUT/bin:$PATH" ota_from_target_files -k "$KEYS/releasekey" \
  "$OUT/signed-target_files.zip" "$OUT/omarchyos-ota.zip"
PATH="$ANDROID_HOST_OUT/bin:$PATH" img_from_target_files \
  "$OUT/signed-target_files.zip" "$OUT/omarchyos-img.zip"
sha256sum "$OUT"/*.zip | tee "$OUT/SHA256SUMS"

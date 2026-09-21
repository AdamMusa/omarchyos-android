#!/usr/bin/env bash
# Incrementally rebuild a development emulator image from an existing AOSP build.
# Does not modify the original images or userdata. Not a release-image builder.
set -Eeuo pipefail
if [[ $# != 5 ]]; then
  echo "Usage: $0 AOSP_ROOT PRODUCT_OUT PLATFORM_SIGNED_SHELL_APK PLATFORM_SIGNED_CORE_APK OUTPUT_DIR" >&2
  exit 2
fi
source_root=$(cd "$(dirname "$0")/../../../.." && pwd)
aosp=$(realpath "$1")
product=$(realpath "$2")
apk=$(realpath "$3")
core_apk=$(realpath "$4")
mkdir -p "$5"
result=$(realpath "$5")
[[ "$result" != "$product" && ! -e "$result/system_ext" ]] || {
  echo "Use a fresh output directory, separate from PRODUCT_OUT" >&2; exit 1;
}
export PATH="$aosp/out/host/linux-x86/bin:$aosp/prebuilts/jdk/jdk21/linux-x86/bin:$PATH"
cd "$aosp"
for tool in build_image build_super_image avbtool sgdisk; do command -v "$tool" >/dev/null; done

# Copy the built partition tree; Android's image builder supplies ownership,
# filesystem configuration, SELinux labels, and a fresh verified-boot hashtree.
cp -a --reflink=auto "$product/system_ext" "$result/system_ext"
shell_dir="$result/system_ext/priv-app/OmarchyShell"
mkdir -p "$shell_dir/lib/arm64" "$result/system_ext/etc/preferred-apps"
cp "$apk" "$shell_dir/OmarchyShell.apk"
cp "$core_apk" "$result/system_ext/priv-app/OmarchyCore/OmarchyCore.apk"
# PackageManager keys its system-package parse cache by the package directory's
# modification time. A preserved old directory time can hide the new manifest.
touch "$shell_dir" "$result/system_ext/priv-app/OmarchyCore"
python3 - "$apk" "$shell_dir/lib/arm64" <<'PY'
import pathlib, sys, zipfile
with zipfile.ZipFile(sys.argv[1]) as archive:
    libs = [n for n in archive.namelist()
            if n.startswith('lib/arm64-v8a/') and n.endswith('.so')]
    if not libs:
        raise SystemExit('Missing ARM64 native libraries')
    for name in libs:
        (pathlib.Path(sys.argv[2]) / pathlib.Path(name).name).write_bytes(archive.read(name))
PY
python3 "$source_root/packages/apps/OmarchyShell/prebuilt/extract-bash.py" \
  "$apk" "$result/system_ext/bin/bash"
cp "$source_root/device/omarchy/permissions/privapp-permissions-omarchy.xml" \
  "$result/system_ext/etc/permissions/privapp-permissions-omarchy.xml"
cp "$source_root/device/omarchy/permissions/omarchy-home.xml" \
  "$result/system_ext/etc/preferred-apps/omarchy-home.xml"
build_image "$result/system_ext" \
  "$product/obj/PACKAGING/system_ext_intermediates/system_ext_image_info.txt" \
  "$result/system_ext.img" "$product/system"

# Reuse the matching built partitions, regenerating only the changed partition
# and its container/verification metadata. All signing uses emulator test keys.
for part in system system_dlkm product vendor vendor_boot; do
  ln -s "$product/$part.img" "$result/$part.img"
done
python3 - "$product/obj/PACKAGING/superimage_debug_intermediates/misc_info.txt" "$result" <<'PY'
from pathlib import Path
import sys
result = Path(sys.argv[2])
lines = []
for line in Path(sys.argv[1]).read_text().splitlines():
    key, _, value = line.partition('=')
    if key in {p + '_image' for p in ('system', 'system_dlkm', 'system_ext', 'product', 'vendor')}:
        value = str(result / (key[:-6] + '.img'))
    lines.append(key + '=' + value)
(result / 'super-info.txt').write_text('\n'.join(lines) + '\n')
PY
build_super_image "$result/super-info.txt" "$result/super.img"
avbtool extract_public_key --key external/avb/test/data/testkey_rsa2048.pem \
  --output "$result/system.avbpubkey"
avbtool make_vbmeta_image \
  --include_descriptors_from_image "$result/vendor_boot.img" \
  --chain_partition "system:1:$result/system.avbpubkey" \
  --include_descriptors_from_image "$result/vendor.img" \
  --include_descriptors_from_image "$result/product.img" \
  --include_descriptors_from_image "$result/system_ext.img" \
  --include_descriptors_from_image "$result/system_dlkm.img" \
  --algorithm SHA256_RSA4096 --key external/avb/test/data/testkey_rsa4096.pem \
  --padding_size 4096 --rollback_index 0 --output "$result/vbmeta.img"
truncate -s 65536 "$result/vbmeta.img"
printf '%s vbmeta 1\n%s super 2\n' "$result/vbmeta.img" "$result/super.img" > "$result/image-config.txt"
python3 device/generic/goldfish/build/tools/mk_combined_img.py \
  -i "$result/image-config.txt" -o "$result/system-qemu.img"
bash device/generic/goldfish/build/tools/mk_vbmeta_boot_params.sh \
  "$result/vbmeta.img" "$result/system.img" "$result/VerifiedBootParams.textproto"
avbtool verify_image --image "$result/vbmeta.img" --follow_chain_partitions
echo "Built $result/system-qemu.img and VerifiedBootParams.textproto; userdata is unchanged."

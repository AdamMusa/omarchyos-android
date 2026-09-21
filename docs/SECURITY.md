# Security model and release signing

## Build variants

| Variant | Use | Root / adb root | SELinux |
| --- | --- | --- | --- |
| `userdebug` (default) | development, emulator iteration | yes | enforcing |
| `user` | anything handed to someone else | no | enforcing |

```sh
OMARCHY_BUILD_VARIANT=user ./omarchy build emulator
```

Sandboxed Play behaves differently on user builds: `GmsCompat.isDevBuild()` is
false there, so dev-only hooks and the dev GmsCompatLib key are rejected.

## Release keys

AOSP builds are signed with the public **test keys** in
`build/make/target/product/security`. Anyone can sign an "update" with those
keys, so test-key images must never be released.

1. `device/omarchy/tools/make-release-keys.sh` (run once, from the AOSP root in
   the builder) creates keys in `~/.omarchy-keys`, outside the tree: releasekey,
   platform, shared, media, networkstack, sdk_sandbox, bluetooth, nfc,
   gmscompat_lib, the AVB key and the APEX key. Back this folder up offline.
   `*.pk8` is git-ignored.
2. `m target-files-package otatools`, then `device/omarchy/tools/sign-release.sh`
   re-signs every APK, APEX and vbmeta with those keys, and writes a signed
   target-files, a full OTA, flashable images and `SHA256SUMS`.

The Omarchy plugin registry trusts packages signed with the **platform** key.
Release keys therefore also decide which plugins and themes a release device
accepts.

## Verified boot

On a physical device, release images are signed with `avb.pem`. Flash
`avb_pkmd.bin` as the custom AVB key, then relock the bootloader. The device
then refuses modified system images, and hardware key attestation reports the
OmarchyOS key. That report is what lets apps check that they're running on
genuine OmarchyOS (see [sandboxed Google Play](SANDBOXED_PLAY.md)).

## Still to do

- Signed OTA update client and server (monthly AOSP security patches).
- Audit of OmarchyCode's bundled `codex` and `claude` binaries and the plugin
  host's permissions.
- Physical device port (the emulator has no hardware-backed attestation).

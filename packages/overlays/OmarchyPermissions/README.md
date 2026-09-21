# Native permission appearance

This static, code-free system overlay changes PermissionController's panel,
title and action-button styles. Permission decisions, layouts, obscured-touch
filtering and one-time permission handling remain in Android.

The Android 12 and Android 16 style variants are repeated with matching resource
qualifiers. Overlay builds must retain them: without `--no-resource-deduping`
and `--no-resource-removal`, AAPT can discard a versioned style and let the
platform's qualified style win. Soong's `runtime_resource_overlay` uses these
flags; the incremental emulator overlay builder does too.

Validate the installed system image, since Android rejects a sideloaded update
to a static overlay. Runtime evidence is tracked in `docs/SYSTEM_UI_VALIDATION.md`.

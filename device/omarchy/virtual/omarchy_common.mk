# Copyright 2026 OmarchyOS
# SPDX-License-Identifier: Apache-2.0

PRODUCT_SOONG_NAMESPACES += \
    packages/apps/OmarchyCore \
    packages/apps/OmarchyCode \
    packages/overlays/OmarchyThemes

PRODUCT_PACKAGE_OVERLAYS += device/omarchy/overlay

PRODUCT_PACKAGES += \
    OmarchyCore \
    OmarchyCode \
    omarchy_codex_cli \
    omarchy_codex_code_mode_host \
    omarchy_codex_rg \
    omarchy_codex_bwrap \
    omarchy_codex_zsh \
    omarchy_claude_cli \
    omarchy_claude_musl_loader \
    omarchy_claude_musl_libc \
    libomarchy_resolv_compat \
    OmarchyThemeMintCore \
    OmarchyThemeTokyoNightCore \
    OmarchyThemeCatppuccinCore \
    OmarchyThemeEverforestCore \
    OmarchyThemeGruvboxCore \
    OmarchyThemeCatppuccinLatteCore

PRODUCT_COPY_FILES += \
    device/omarchy/permissions/omarchy_features.xml:$(TARGET_COPY_OUT_PRODUCT)/etc/permissions/omarchy_features.xml \
    device/omarchy/permissions/privapp-permissions-omarchy.xml:$(TARGET_COPY_OUT_SYSTEM_EXT)/etc/permissions/privapp-permissions-omarchy.xml \
    packages/apps/OmarchyCode/scripts/codex:$(TARGET_COPY_OUT_SYSTEM_EXT)/bin/codex \
    packages/apps/OmarchyCode/scripts/claude:$(TARGET_COPY_OUT_SYSTEM_EXT)/bin/claude \
    packages/apps/OmarchyCode/scripts/omarchy-mobile:$(TARGET_COPY_OUT_SYSTEM_EXT)/bin/omarchy-mobile \
    packages/apps/OmarchyCode/prebuilts/codex/vendor/aarch64-unknown-linux-musl/codex-package.json:$(TARGET_COPY_OUT_SYSTEM_EXT)/bin/omarchy-code/vendor/aarch64-unknown-linux-musl/codex-package.json \
    packages/apps/OmarchyCode/THIRD_PARTY_NOTICES.md:$(TARGET_COPY_OUT_SYSTEM_EXT)/etc/omarchy/code/THIRD_PARTY_NOTICES.md \
    device/omarchy/bootanimation/bootanimation.zip:$(TARGET_COPY_OUT_PRODUCT)/media/bootanimation.zip \
    device/omarchy/bootanimation/NOTICE.md:$(TARGET_COPY_OUT_PRODUCT)/etc/omarchy/bootanimation/NOTICE.md

PRODUCT_BRAND := Omarchy
PRODUCT_MANUFACTURER := OmarchyOS

# Keep Android's actual Launcher/SystemUI/Settings packages. Omarchy maintains
# fork branches in those projects instead of drawing a second shell over them.

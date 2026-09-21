# Copyright 2026 OmarchyOS
# SPDX-License-Identifier: Apache-2.0

PRODUCT_SOONG_NAMESPACES += \
    packages/apps/OmarchyCore \
    packages/apps/OmarchyCode \
    packages/overlays/OmarchyThemes

PRODUCT_PACKAGE_OVERLAYS += device/omarchy/overlay

PRODUCT_PACKAGES += \
    OmarchyBootOverlay \
    OmarchyFrameworkOverlay \
    OmarchySystemUIOverlay \
    OmarchySettingsOverlay \
    OmarchyOverviewOverlay \
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
    packages/apps/OmarchyShell/third_party/omarchy/default/fonts/omarchy/omarchy.ttf:$(TARGET_COPY_OUT_PRODUCT)/fonts/omarchy.ttf \
    packages/apps/OmarchyShell/third_party/omarchy/default/fonts/omarchy/README.md:$(TARGET_COPY_OUT_PRODUCT)/etc/omarchy/fonts/OMARCHY-ICONS-NOTICE.md \
    packages/apps/OmarchyShell/third_party/omarchy/LICENSE:$(TARGET_COPY_OUT_PRODUCT)/etc/omarchy/fonts/OMARCHY-LICENSE \
    device/omarchy/fonts/fonts_customization.xml:$(TARGET_COPY_OUT_PRODUCT)/etc/fonts_customization.xml \
    packages/apps/OmarchyShell/fonts/JetBrainsMonoNerdFont-Regular.ttf:$(TARGET_COPY_OUT_PRODUCT)/fonts/JetBrainsMonoNerdFont-Regular.ttf \
    packages/apps/OmarchyShell/fonts/JetBrainsMonoNerdFont-Bold.ttf:$(TARGET_COPY_OUT_PRODUCT)/fonts/JetBrainsMonoNerdFont-Bold.ttf \
    packages/apps/OmarchyShell/fonts/OFL.txt:$(TARGET_COPY_OUT_PRODUCT)/etc/omarchy/fonts/OFL.txt \
    packages/apps/OmarchyShell/fonts/NERD-FONTS-LICENSE.txt:$(TARGET_COPY_OUT_PRODUCT)/etc/omarchy/fonts/NERD-FONTS-LICENSE.txt \
    device/omarchy/permissions/omarchy-home.xml:$(TARGET_COPY_OUT_SYSTEM_EXT)/etc/preferred-apps/omarchy-home.xml \
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

# OmarchyShell is the phone home screen; retain Android's overview provider.
#
# Launcher3QuickStep must remain installed and enabled for the platform's
# TouchInteractionService and RecentsActivity. Select Omarchy as the HOME
# activity through the preferred-apps configuration; never disable Quickstep.
# SystemUI supplies keyguard, system dialogs and navigation chrome.
#
# Everything else Android ships stays installed and is reached through
# Omarchy's own menu: Settings, Camera, Contacts and the rest are listed from
# PackageManager and started with an ordinary intent. OmarchyOS wraps Android's
# features in Omarchy's style; it does not reimplement them or invent a second
# app format.
PRODUCT_PACKAGES += \
    OmarchyShell \
    omarchy_bash \
    Launcher3QuickStep

# Gesture navigation, not three buttons: back, home and recents then work over
# every app without the shell having to draw navigation of its own.
PRODUCT_PACKAGES += \
    NavigationBarModeGesturalOverlay

PRODUCT_PRODUCT_PROPERTIES += \
    ro.omarchy.shell=os.omarchy.shell

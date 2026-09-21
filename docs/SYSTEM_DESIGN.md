# Omarchy system design

Omarchy's UI belongs to the Android product, not a floating application overlay.
The native status bar, gesture indicator, Settings and overview still use Android's
lifecycle, input, accessibility and security infrastructure.

## Components

- SystemUI's phone status bar draws the Omarchy menu mark, a centered day/24-hour clock, and the
  settings gear, matching Home's original Omarchy layout. Back mounts beside the
  mark when the foreground task is not Home, without moving the mark or gear.
  Home and service pages use this same SystemUI surface; the
  shell no longer draws a second bar on an Omarchy product. The mark, clock and
  gear open the existing Omarchy menu, calendar and phone settings menu through
  Android's selected Home application. Long-pressing the gear opens Quick Settings.
  Home gestures retain their normal Android behavior. The ordinary battery, transport and notification icon row is omitted.
  Notifications remain in the shade. Active-call/recording chips and privacy
  indicators retain their Android lifecycle and visibility.
- Native Compose bottom sheets host the same Omarchy bar in their modal window.
  The bar remains fixed when a service panel grows to full height; only its body
  scrolls or drags. Back goes to the modal, while logo, clock and gear dismiss it
  before opening the corresponding Home panel. Long-pressing its gear dismisses
  the modal before opening Quick Settings. Products without Omarchy retain
  the original bottom-sheet layout.
- The gesture indicator uses the current Omarchy accent as a thin line, including
  the Launcher3 taskbar used by current phone builds. Android continues to handle Back, Home, overview and gesture animations.
- Native service pages omit the separate Android back/title header. Back lives
  in the Omarchy system bar. Page-specific toolbar actions remain in the service
  content, while empty action rows collapse. Setup flows keep their own controls
  and page titles remain available to accessibility services.
- Product font customization supplies the original Omarchy icon font and
  JetBrains Mono for Omarchy and Android's
  named Settings type families. Multilingual and emoji fallback fonts remain.
- Quick Settings uses the selected theme's exact semantic colors and compact
  tile corners. Its ordinary battery/icon header is replaced by Omarchy branding;
  privacy indicators remain separately managed by Android.
- Code-free framework/SystemUI/overview overlays apply compact corners and native
  surface/text roles. Shared Settings preference cards and main-switch cards use
  the same compact corners. Settings' Compose shape definitions are patched separately;
  resource overlays cannot replace values compiled into Compose code.
- Theme application registers per-user fabricated overlays for Android semantic
  colors, SystemUI contrast colors and Omarchy tokens in one transaction. Both
  light and dark resource variants use the chosen palette, so applications cannot
  switch the system bar back to a stock palette. Text roles meet 4.5:1 contrast;
  colors with insufficient contrast are adjusted toward black or white only as
  far as needed, retaining their hue. See [THEME_VALIDATION.md](THEME_VALIDATION.md)
  for the selected-palette upgrade check.

The platform cannot force arbitrary third-party app content to use Omarchy fonts
or layouts. App-owned toolbars remain app-owned. This design changes OS-owned UI;
it does not replace apps, disable their controls or remove security prompts.

## Building

Restore the fork patches with `recovery/restore-into-aosp.sh` in the matching full
AOSP checkout before building SystemUI and Settings. These patches are gated by
Omarchy product identity. The product includes OmarchyFrameworkOverlay,
OmarchySystemUIOverlay, OmarchySettingsOverlay, OmarchyOverviewOverlay and
OmarchyPermissionsOverlay alongside its boot overlay.

`packages/overlays/tools/build-dev-overlays.sh` builds resource overlays against
an existing matching development image. It uses platform test keys, except the
Launcher3 overlay which matches Launcher3's product-default development key.
Production signing must use the corresponding release certificates.

For incremental emulator images, pass the built overlay directory as the sixth
argument of `packages/apps/OmarchyShell/tools/rebuild-emulator-image.sh`. The
script rebuilds system, system_ext and product, including framework services,
OEM fonts and boot animation,
and regenerates the verified-boot metadata. Source changes to SystemUI/Settings
must first be compiled into the input product tree; overlays alone do not add the
native Omarchy bar.

## Verification

The palette unit test covers all 22 bundled theme palettes and text contrast.
The root-only AndroidPaletteProbe invokes Core's real palette service on a
matching development emulator; it is never included in the shipping OS.

UI checks must cover Settings and third-party apps, Home/Back/overview, the full and compact
clock labels, cutouts, landscape, large text, screen lock and active privacy
indicators. A successful overlay installation alone is not a completed native
SystemUI build or a device release qualification.

### September 20 emulator validation

SystemUI and Settings were compiled in the matching Linux AOSP checkout, using
cached unchanged dependencies. The rebuilt, verified-boot image contains both
native components, the five static design overlays, product fonts, and Shell v2.
Home runs directly from its privileged system APK; its Bash runtime is installed
as an executable in `system_ext/bin`.

The device navigation test passed on Home and Wi-Fi, Bluetooth, Battery Saver,
Display, and Sound: logo, clock, and gear bounds match; Back mounts only outside
Home, returns to Home, and unmounts; SystemUI stays in the same process. The
menu, calendar, and settings gear route to the original Omarchy panels. Visual
checks covered a 320dp width with 200% text, landscape, a simulated center cutout,
Quick Settings, and the camera's active privacy chip and dot. Test display settings
were restored. Immersive apps can still hide the system bar and reveal the same
Omarchy bar with an edge swipe, following Android's normal behavior.

The repeatable navigation check is documented in
`packages/apps/OmarchyShell/tools/navigation-tests/README.md`.
`./omarchy test` retains 17 existing incomplete-checkout/fork-branch failures;
this incremental build is not a clean full-source build or physical-device
qualification. The remaining boot-transition limitation is recorded in
[BOOT_STARTUP.md](BOOT_STARTUP.md).

The service-navigation check was repeated on the v16 image after introducing the
modal host. Home, five service pages, nested Bluetooth pairing, overview, the
notification shade, Quick Settings and tile editing retained matching navbar
control bounds. Scrolling kept the bar fixed, Back returned through the nested
service to Home, and SystemUI kept its process. The local result is
`out/mobile-check/native-v16-navigation-check.log`.

The native power menu uses Omarchy outlines and compact corners. Long-pressing
Power opens this menu by default while preserving any explicit user preference
for an assistant. See [POWER_MENU.md](POWER_MENU.md) for the implementation and
non-destructive runtime check.

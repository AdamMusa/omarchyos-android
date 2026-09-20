# Android phone profile

`apply.py` stages the phone adaptations after the Qt compatibility patches.
The vendored desktop sources remain unchanged. The profile uses Omarchy's
colors, typography, menu and calendar, with Android owning apps and services.

- The bar has a menu, Omarchy’s day-and-time clock and calendar, phone settings and battery status. Touch
  targets are at least 48 logical pixels. The clock is constrained to the
  space between the edge groups; it cannot paint over their controls.
- The menu lists Android launcher activities. Launching calls PackageManager
  and Android intents directly. App management opens Android's app settings.
  No Linux package catalog, Windows installer or desktop workspace menu is
  exposed. APK installation remains Android Package Installer's responsibility.
- Settings entries open Android Wi-Fi, Bluetooth, sound, display, battery,
  storage, accessibility and security screens. Unsupported desktop polling
  services are disabled rather than loading Linux commands on every interval.
- Native Android navigation stays visible. Keep Launcher3QuickStep installed
  and enabled for gesture/overview support, and choose Omarchy as HOME.
- The home uses the original Omarchy Tokyo Night wordmark wallpaper, converted
  to PNG for the bundled Qt decoder and fitted without cropping in portrait.
- The profile migrates the desktop bar once, backing up `shell.json` to
  `shell.json.before-mobile`. Theme and wallpaper preferences are retained.

## Simulator requirements

Copy `data/misc/modem_simulator/` from the **same product build** alongside the
kernel and images. `./omarchy run emulator` links it beside the runtime ramdisk.
Without it, the emulator disables its modem and Android's radio service
restarts continuously, blocking phone-dependent parts of Settings and SystemUI.
The launcher sets `qemu.hw.mainkeys=0` so Android supplies on-screen navigation.
Host GPU rendering is the default: software graphics blocked SurfaceFlinger
during startup and triggered SystemUI ANRs in the simulator.
`OMARCHY_EMULATOR_GPU` remains available for other hosts.

On the emulator, shader disk caching is disabled before Qt starts. A cached
program binary was crashing the emulator GL encoder on every launcher restart.
Physical devices retain caching. See [Qt's cache controls](https://doc.qt.io/qt-6/qquickgraphicsconfiguration.html)
and [AOSP's modem setup](https://android.googlesource.com/platform/external/qemu/+/refs/heads/emu-32-release/android-qemu2-glue/main.cpp).

## Verification

Build the shell with `tools/build-apk.sh` in the Linux Qt builder. Stage in a
writable source copy because Android assets and native libraries are generated
under `app/android`. `python3 tests/test_emulator_launcher.py` from the repository
root checks navigation arguments, modem configuration and persistent user data
without launching an emulator. Run `./omarchy test` in a full restored AOSP
workspace for the remaining platform checks.

## Themes

Open **Omarchy → Themes**. Installed includes all 22 themes in the pinned
Omarchy source tree, with their original palette and one official wallpaper
per theme. Apply changes the shell colors, artwork, Android light/dark mode,
Android dynamic color seed and the Core semantic resources through a fabricated
Runtime Resource Overlay. Android apps choose whether to use system colors.

Marketplace uses the official `omacom/omarchy-theme-registry` catalog. A bundled
snapshot loads offline; Refresh checks for new community themes. Install fetches
the selected GitHub repository at a resolved commit, validates its color data,
and imports palette and artwork into private per-user storage. Downloaded themes
survive shell updates. The installer never installs desktop apps or executes
repository scripts, QML, binaries, plugins or hooks. Legacy Alacritty palettes
are supported; repositories without a valid palette produce an error and leave
the active theme intact. Large archives are bounded for phone memory/storage.

Source revisions and wallpaper URLs are recorded in `theme-sources.json` and
`wallpapers/sources.json`. `tools/sync-mobile-themes.py` refreshes bundled assets
on macOS using sips for image conversion. Review and pin source updates before
shipping. Omarchy's license remains in the vendored tree; downloaded theme
LICENSE/COPYING and README files are retained when provided.

Run `tools/test-themes.sh` with JDK 17+ to check all bundled palettes and artwork,
legacy conversion, invalid color handling, archive path validation, duplicate
files, size limits and exclusion of executable repository files.

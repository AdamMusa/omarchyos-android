# Mobile Omarchy theme validation

Validated on the arm64 Android emulator, September 20, 2026.

- All 22 bundled palettes parse and round-trip, and each has its official artwork.
- Data tests reject malformed colors, archive traversal, duplicate paths and
  oversized palette files; executable repository files are excluded.
- The phone picker fits 411dp and 320dp widths. Search is explicitly focused;
  opening the picker does not open the keyboard.
- The official registry refresh returns 191 community themes. Aetheria installs
  from its resolved GitHub revision into private theme storage; only palette,
  decoded artwork, provenance, README and license files are retained.
- Catppuccin Latte applies a light shell and light Android Settings. Its Android
  seed is `#1e66f5`, and the Core RRO surface resolves to `#eff1f5`.
- Aetheria applies a dark shell and imported wallpaper; its Core RRO surface
  resolves to `#0e091d`. The theme and artwork survive a device reboot and shell
  APK update.
- Tokyo Night applies its official winding-road wallpaper and dark mode. Core surface
  resolves to `#1a1b26`; the per-user fabricated overlay is enabled.
- Emulator launcher tests pass. No shell/Core crashes or SystemUI ANRs occurred
  in the final reboot and apply checks.

## Build limits in this checkout

The shell APK builds with the Qt Android toolchain. The changed Core classes
compile against the simulator build's platform framework and existing Core
classes, then are merged into its unchanged resource package, dexed, aligned
and platform-signed. The Core update installs through Android's staged update
path for persistent system apps and a reboot.

A complete AOSP rebuild remains blocked by the Linux source tree's unrelated
missing modules and framework stub dependency cycle. `./omarchy test` still
reports 17 pre-existing checks failing because the recovered Mac checkout lacks
some AOSP files and fork branches. These failures are not treated as passing.

## Wallpaper correction

The initial package chose the optional Omarchy wordmark for most defaults and
letterboxed the artwork on phones. Default wallpaper selection now follows
upstream filename order, uses the actual first artwork for every theme, and
renders with aspect-fill cropping. The Installed picker shows portrait wallpaper
previews. All 22 JPEG assets are decoded by the data tests, and source URLs remain
pinned in `mobile/wallpapers/sources.json`.

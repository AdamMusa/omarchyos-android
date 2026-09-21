# Omarchy screensaver

Open **Omarchy → Phone settings → Screensaver**, select **Omarchy**, and use
Android's preview and activation controls. Swipe up or press Android Back to wake.
Android retains its lock-screen behavior; waking does not bypass a configured lock.

The screensaver uses the **same ttfx engine as upstream Omarchy**, including its
random text effects and original gradients, with Omarchy's bundled ASCII wordmark.
The unmodified engine is pinned to version 0.3.2, commit
`7203e354498462064b7c0a89375051f65cf2ce99`, with its MIT license and attribution.
A new effect starts after the previous animation finishes. The old logo-and-clock
layout has been replaced.

Android renders the engine's full-color text frames on a native Canvas. The
simulation runs at 60 steps per second with a maximum of 30 displayed frames per
second. Work happens on one background thread, with no frame queue on the UI
thread. Disabling Android animations displays a static centered wordmark.

## Android integration

`OmarchyDreamService` uses Android's native screensaver lifecycle in a separate
process. It loads only the native effects library and bundled wordmark; it does not start
Qt, the marketplace, a network request, or desktop commands. The renderer and native effect are released
when the dream stops. Android controls power, charging eligibility, and keyguard.
Home no longer requests to turn the screen on or appear above the lock screen.

The product overlay selects Omarchy as the default dream and enables the sleep
trigger. This overlay needs the next complete image build; changing a default
does not override an existing user's saved settings.

The development emulator was configured explicitly with Omarchy selected,
screensavers enabled while charging, a two-minute screen timeout, and developer
"stay awake while charging" disabled. Its charger is connected and ambient light
is set to 500 lux. AOSP's low-light mode can substitute its own dim clock in dark
conditions; that platform behavior is retained.

## Building the animation engine

The Linux build requires Rust with the `aarch64-linux-android` target in addition
to the existing Qt/Android toolchain. `tools/build-apk.sh` invokes
`tools/build-screensaver.sh` with locked Cargo dependencies and the Android NDK.
The library is loaded only in the separate screensaver process, not at Home boot.

Rust tests verify changing frames and canvas bounds. The Java decoder test covers
true-color and indexed-color SGR, attributes, reset, clipping, and ignored terminal
commands. On the emulator, successive captured frames show the actual effect
changing without a Qt engine in the Dream process.

## Earlier lifecycle validation (September 20, 2026)

- Built and installed the APK, selected Omarchy in native Screensaver settings,
  and opened those settings through the Omarchy phone menu.
- Three start/wake/Home cycles restored the same wallpaper and Home process.
- Ten Settings-to-Home checks passed (two rounds each of Wi-Fi, Bluetooth, sound,
  display, and screensaver settings). The regression check identifies the actual
  Home activity so the screensaver cannot be mistaken for Home.
- Temporarily shortened idle timeout to verify automatic activation while
  charging, then restored the two-minute timeout.
- Checked a 320 dp-wide portrait screen and a 2400×1080 landscape screen for
  clipping; restored the emulator's 1080×2400 / 420 dpi configuration afterward.
- The earlier clock renderer used about 19 MB; this is not a measurement of ttfx.

Battery life, long-duration display behavior, accessibility, and secured-device
transitions still require physical-device validation. See
[startup and remaining performance work](PERFORMANCE.md) for broader limitations.

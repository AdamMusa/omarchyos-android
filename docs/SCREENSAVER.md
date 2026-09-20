# Omarchy screensaver

Open **Omarchy → Phone settings → Screensaver**, select **Omarchy**, and use
Android's preview and activation controls. Swipe up or press Android Back to wake.
Android retains its lock-screen behavior; waking does not bypass a configured lock.

The screensaver displays Omarchy's original ASCII wordmark, the selected theme's
accent and foreground colors, and the system clock/date on a black background.
It follows Android's 12/24-hour preference. The artwork slowly moves, fits portrait
and landscape layouts, and redraws once per second. Very dark theme text receives
a light fallback so it remains visible on black.

## Android integration

`OmarchyDreamService` uses Android's native screensaver lifecycle in a separate
process. It reads only the active palette and bundled wordmark; it does not start
Qt, the marketplace, a network request, or desktop commands. Redraw callbacks stop
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

## Validation (September 20, 2026)

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
- Observed about 19 MB of proportional memory usage for the screensaver process.

Battery life, long-duration display behavior, accessibility, and secured-device
transitions still require physical-device validation. See
[startup and remaining performance work](PERFORMANCE.md) for broader limitations.

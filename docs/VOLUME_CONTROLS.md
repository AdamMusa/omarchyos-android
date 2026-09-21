# Native Omarchy volume controls

Omarchy uses Android's native compact volume dialog and expanded volume panel.
The selected theme supplies their colors and typography. Product resources give
the panel, ringer selection and slider tracks compact 4dp corners; the expanded
panel's Settings and Done buttons use the same small shape as other Omarchy
system controls. Android still owns audio routing, volume policy, stream ranges,
accessibility and dismissal.

The compact and expanded Compose slider tracks read the existing native volume
corner resource instead of hard-coding their own shape. Its platform default
stays 12dp, so products without the Omarchy overlay keep their existing shape.
The shared Compose button default uses the theme's small shape only on the
Omarchy product; explicit button-shape overrides remain effective.

Build SystemUI and the OmarchySystemUI overlay into the system image. Updating
only the overlay does not replace the compiled Compose slider code, and Android
rejects sideloaded updates to these static product overlays.

For an English development emulator with no active media playback, build and
install the disposable UI probe as described in
`packages/apps/OmarchyShell/tools/system-ui-tests/README.md`, then run:

```sh
python3 packages/apps/OmarchyShell/tools/test-system-volume.py \
  --serial emulator-5556 \
  --probe-dex out/mobile-check/system-ui-window-probe.dex \
  --screenshots out/volume-check
```

The probe requests the real panel with Android's public AudioManager API. The
check reads the native media stream, tests volume keys and slider touch, checks
accessible slider values, and confirms Done and Back return to the calling app
without restarting SystemUI. It restores the original media level and dismissal
timeout, then returns Home. Remove the temporary `os.omarchy.uicheck` app after
validation; it is never included in the OS image.

The v15 development image includes the compiled SystemUI changes and static
overlay version 5. The installed SystemUI APK hash matches the build, and Android
resolves the shared corner resource to 4dp. Visual inspection confirms the compact
ringer selection, both slider tracks, and expanded Settings/Done buttons use the
new shapes. The runtime check passes at normal text size and at 200% text size
on a 320dp-wide display, including native stream changes and Done/Back dismissal.
It also passes after applying Catppuccin Latte through the phone's theme picker;
the light surface and readable text update without losing the new control shapes.

The light-theme screenshot exposes a separate palette-fidelity issue: Latte's
blue accent has about 4.34:1 contrast against its background, so the existing
`SystemPalette.readable` fallback replaces it with black to meet 4.5:1. A
contrast adjustment that retains the accent's hue is still needed; the functional
light-theme pass does not establish exact palette fidelity.

At enlarged text size, the expanded panel becomes full height and covers the
Omarchy navbar. Its body scrolls and its bottom actions remain reachable, but
keeping the same navbar visible and usable inside that modal is still required.
The output description also retains Android's single-line marquee. These are
remaining layout issues, not completed navbar or large-text design work.

Local screenshots and logs are under `out/mobile-check/native-v15-volume` and
`native-v15-volume-large` and `native-v15-volume-light`, with their adjacent
`-check.log` files. These checks
do not qualify Bluetooth audio, physical hardware keys or a complete phone's
audio stack.

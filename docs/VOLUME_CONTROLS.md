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

The v15 light-theme check exposed an accent-fidelity issue: Latte's blue was
replaced with black to meet 4.5:1 contrast. Core 41, included in v16, corrects
this by minimally darkening the blue instead. Its automatic upgrade and native
resource values are recorded in [THEME_VALIDATION.md](THEME_VALIDATION.md).

The v16 image fixes the expanded modal covering the Omarchy navbar. The native
Compose bottom-sheet host mounts the shared bar above its body, with the same
control bounds as the ordinary SystemUI bar. Navbar Back closes the modal and
returns to the calling app. Logo, gear and clock dismiss it before opening the
Omarchy menu, phone settings menu and calendar; screenshots confirm all three
destinations. The test also verifies hardware Back, Done, slider touch and volume
keys without restarting SystemUI.

The modal check passes at normal font size and at 200% text on a 320dp-wide
display. At large text sizes, its body scrolls to reach the Alarm row while the
bar and bottom actions remain fixed. The output description still uses Android's
single-line marquee; replacing that behavior remains separate layout work.
The dark-theme landscape check also confirms matching compact/modal bar bounds
and navbar Back returning to the calling app. Landscape stream scrolling and
physical-device audio routing are outside that targeted check.

The v17 image additionally fixes long-pressing the modal gear: the dialog now
closes before Quick Settings opens. Previously the shade opened behind it. The
final regression check confirms the shade receives focus, no volume sliders
remain above it, and the same navbar is visible. Compact/expanded stream readback,
slider touch, volume keys, Done, both Back paths, and all three Home-panel
destinations pass on the installed dark-theme image without a SystemUI restart.
The installed SystemUI hash matches the rebuilt APK.

Local screenshots and logs are under `out/mobile-check/native-v15-volume` and
`native-v15-volume-large` and `native-v15-volume-light`. The modal-navbar checks
are in `native-v16-volume-light`, `native-v16-volume-large`, and
`native-v16-volume-landscape`, with adjacent
`-check.log` files. Final shortcut evidence is in `native-v17-volume` and
`native-v17-volume-check.log`. These checks
do not qualify Bluetooth audio, physical hardware keys or a complete phone's
audio stack.

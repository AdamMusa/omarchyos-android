# System navigation check

This development-device test opens five Settings services, reads the native
SystemUI accessibility tree, and presses Back. It checks the fixed logo, clock,
and gear positions, conditional Back, return to Home, and SystemUI process
stability. It also opens Bluetooth pairing and checks that Back returns one level
to Connected devices while remaining mounted, then disappears on Home. It does
not pair a device, install an app or become part of the product.

Compile `NativeBarProbe.java` with `javac` against an Android SDK `android.jar`,
then convert all generated class files to `classes.dex` with that SDK's `d8`.
Run from the repository root:

```sh
python3 packages/apps/OmarchyShell/tools/test-system-navigation.py \
  --serial DEVICE --probe-dex /absolute/path/to/classes.dex
```

Use the ordinary English locale and default font size for the exact full-clock
bounds check. Separately check compact widths, large text, rotation, cutouts,
privacy indicators, and Home menu/calendar/settings actions visually.

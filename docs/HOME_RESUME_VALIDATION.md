# Home rendering after Android settings

The shell remained alive and its QML surfaces retained their correct sizes and
visibility, but Android Back from Wi-Fi settings produced only the window's
`#101010` clear color. Touching Home did not recover the picture.

The Android host now releases the Qt scene graph and graphics resources when the
application becomes hidden or suspended, and schedules a frame when active again.
This follows Qt's [render resource lifecycle](https://doc.qt.io/qt-6.8/qquickwindow.html#releaseResources).
It preserves the QML scene, selected theme and process; transient focus loss does
not trigger a rebuild. No activity restart, polling redraw or service-specific
workaround is used.

## Regression check

With Omarchy as the default Home and the emulator unlocked:

```
python3 packages/apps/OmarchyShell/tools/test-home-resume.py --serial emulator-5556
```

The device check opens Wi-Fi, Bluetooth, Sound and Display settings twice each,
presses Android Back, verifies Home is resumed in the same process, and compares
visible screen samples with the initial Home frame. It excludes the clock and
native gesture bar. Use a static wallpaper and avoid interacting during the run.
A blank initial frame is rejected instead of becoming a passing reference.

Validated on the arm64 simulator with the final platform-signed Qt APK. All eight
returns restored 100% of sampled Home pixels. The actual Omarchy Phone settings
menu routes were also checked for Wi-Fi, Bluetooth and Sound. Wi-Fi also
returned correctly through the native swipe-back gesture and Android Home.
The crash buffer remained empty.

The shell builds successfully and theme/data and emulator launcher tests pass.
`./omarchy test` still reports the same 17 existing missing-file/fork-branch
failures documented in THEME_VALIDATION.md; a complete AOSP image rebuild is
not claimed.

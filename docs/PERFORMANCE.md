# Startup and remaining performance work

Validated on the ARM64 Android Emulator on September 20, 2026. These are
development observations, not physical-phone performance guarantees.

## Why initialization takes time

There are separate startup costs:

1. `./omarchy run emulator` uses `-no-snapshot`, so it cold-boots Android on every
   emulator launch. Keeping phone data does not skip framework and service startup.
2. First installation, or changed packaged assets, unpacks the shell and themes
   into private storage. Qt then loads the shell and may populate its QML cache.
3. Later Home launches can reuse those files and caches. Returning from another
   app should resume the existing Home process, rather than repeat initialization.

Previously the asset deployment marker used the build timestamp. Rebuilding only
Java or native code therefore needlessly unpacked unchanged assets. The marker
now hashes the staged asset names and contents. Desktop fontconfig/Hyprland probes
and the unavailable desktop plugin watcher are also disabled in the phone profile.
Unused desktop theme screenshots are excluded; actual wallpapers and all 22
bundled palettes remain available.

## Measurements and checks

- Shell APK decreased from 54,216 KiB to approximately 43,492 KiB (about 20%).
- Changed assets: 556 files unpacked in 957 ms; QML load completed 5,743 ms after
  the native startup timer began on the first changed-assets launch.
- Subsequent launches reused assets in 1–18 ms. Observed native-start-to-QML-load
  times ranged from 395–1,423 ms. A Java-only APK update reused the asset revision.
- These timers exclude Android boot and Java/native-library startup and do not
  measure the first fully rendered frame. They are not end-to-end boot benchmarks.
- Asset revision tests cover timestamp stability and content/path/deletion changes.
- Emulator launcher tests and the bundled theme/import tests passed. The Qt shell
  APK built, installed, and ran successfully.
- Repeated Settings-to-Home checks passed without a process restart. Screensaver
  lifecycle and layout checks are documented in [Screensaver](SCREENSAVER.md).

Runtime logs identify `assets reused`, `assets deployed`, `paths ready`, and
`QML loaded`. Compare equivalent cold/warm runs and record device, build, and
thermal conditions before drawing performance conclusions.

## Still needed before a smooth, production-ready phone release

- Restore the missing AOSP files and fork branches: `./omarchy test` still reports
  17 existing failures. Resolve the missing modules and framework stub dependency
  cycle that currently block a full Linux image rebuild.
- Investigate startup/update stability. During this development session, one
  Home baseline was blank, while a fresh launch subsequently passed the repeated
  navigation checks. A separate Android `ConfigurationController` null-resources
  exception occurred around development APK replacement, before application
  initialization. Neither observation has a confirmed root cause; successful
  later runs do not close them.
- Run longer cold-boot, APK/OS-update, sleep/wake, rotation, low-memory, and
  app-switching tests, including frame timing and startup traces.
- Validate physical-device drivers and phone services, battery drain, temperature,
  accessibility/font scaling, keyguard security, and update recovery per device.

Android provides the underlying services; the device port, integration, and
validation determine whether they work reliably on a particular phone.

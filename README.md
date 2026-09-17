# OmarchyOS

OmarchyOS is a real Android Open Source Project fork for phones. It keeps
Android's hardware abstraction, application compatibility, permissions, power
management, telephony, camera, sensors, Wi-Fi, Bluetooth, and emulator support,
then replaces the product experience through fork-owned platform code.

This is not the earlier Quickshell/noVNC prototype. The checkout is initialized
from the official `android-latest-release` manifest. The native Android Emulator
and Cuttlefish are the supported virtual-device paths.

## What Omarchy owns

- Android framework integration for a signed plugin registry
- SystemUI surfaces, notification shade, quick settings, lock screen, and mobile
  navigation
- Launcher, app library, recents integration, and installed-app routing
- Settings and ThemePicker integration
- A native Code workspace that ships the official Codex and Claude Code ARM64
  runtimes, real PTYs, provider-owned authentication, and writable project files
- An Omarchy mobile skill and guarded publishing bridge for sandboxed generated
  apps, themes, and extension projects
- Six Runtime Resource Overlay theme packs without executable theme code
- Dynamic light/dark semantic colors shared by SystemUI, Launcher, Settings, and
  signed Omarchy apps
- Per-user extension state and Binder lifecycle events without in-process loading
- Compact Quick Settings and phone-width Launcher defaults applied as product
  overlays, not a replacement launcher APK
- Virtual products for Android Emulator and Cuttlefish

See [the architecture](docs/ARCHITECTURE.md) for the trust model and component
boundaries.

## Start here

```sh
./omarchy doctor
./omarchy sync-core
./omarchy fork
./omarchy test
```

The initial `sync-core` downloads the repositories Omarchy modifies. A complete
build needs the rest of AOSP, a case-sensitive filesystem, and a Linux build
host:

```sh
./omarchy sync-full
./omarchy build emulator
./omarchy run emulator
```

For a Linux/KVM virtual phone:

```sh
./omarchy build cuttlefish
./omarchy run cuttlefish
```

`run emulator` opens the native Android Emulator window. `run cuttlefish` uses
Cuttlefish's WebRTC viewer; neither command uses noVNC. The emulator command
uses a persistent phone-data image and suppresses desktop metrics/crash-report
prompts so one invocation reaches the phone UI. Set `OMARCHY_WIPE_DATA=1` for a
clean first boot.

The project control files may remain at this path on macOS, but the complete AOSP
workspace must be synced on Linux or case-sensitive APFS. Modern AOSP contains
source paths that differ only by letter case, and Android image compilation is a
Linux-host workflow.

## Current stage

The repository is being built in vertical slices. The foundation now includes
the signed plugin contract, per-user theme switching, six semantic palettes,
platform settings entry point, authentic ASCII boot animation, native Code
workspace, mobile shell ownership, and virtual-device products. Physical-device
support is added with
a device-specific kernel, bootloader integration, vendor binaries where legally
available, and Android HAL implementations; no single device tree can support
every phone.

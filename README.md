# OmarchyOS

**An Android phone operating system with the Omarchy home screen, style, and themes.**

OmarchyOS brings Omarchy to everyday phone use while keeping the Android platform
that powers calls, messages, Android apps, connectivity, camera, notifications,
and device settings. It is built on the Android Open Source Project (AOSP), with
an Omarchy home screen adapted for touch and phone-sized displays.

Android continues to manage applications, permissions, hardware services, battery
use, and system navigation. Open **Omarchy → Apps** for installed applications,
**Phone settings** for device controls, and **Themes** to change the look.

**Current status:** the Omarchy experience is running and being tested in the
native Android Emulator. The Android phone capabilities below are retained in
the platform; physical-device and carrier-dependent features still require a
matching device port and testing. This project does not yet claim a fully
validated, production-ready physical phone release.

## Everyday phone capabilities

| Feature | What OmarchyOS provides | Validation and requirements |
| --- | --- | --- |
| Calls, contacts, and messaging | Android telephony and the phone, contacts, and messaging apps supplied by the selected Android product. | Real calls, SMS/MMS, SIM/eSIM, mobile data, IMS/VoLTE, and emergency calling require a supported modem, device port, carrier support, and physical-device testing. |
| Android apps | Installed-app library, normal Android app launching, APK installation through Android's package installer, app permissions, and app management. | The shell uses Android PackageManager and intents. Individual apps may require hardware or services outside this build. |
| Wi-Fi and Bluetooth | Android connectivity services and their native settings screens, reached directly from the Omarchy menu. | Opening settings and returning Home are verified. Real networks, pairing, Bluetooth audio, and tethering/hotspot behavior need device testing. |
| Camera, microphone, and media | Android camera, audio, media, and permission infrastructure for compatible apps. | Capture quality, recording, speakers, headphones, and hardware codecs depend on the device's drivers and hardware support. |
| Location, sensors, and NFC | Android location and sensor APIs, with NFC available in products that include its hardware and services. | GPS/GNSS, motion sensors, contactless features, and related apps need a compatible device and validation. |
| Notifications and multitasking | Android notifications, Quick Settings, lock screen, app tasks, and overview integration. | SystemUI and Launcher3QuickStep remain part of the product. Omarchy supplies Home and the installed-app menu. |
| Back, Home, and gestures | Native Android navigation across the home screen, apps, and settings. | Repeated Back checks from Wi-Fi, Bluetooth, Sound, and Display passed; swipe-back and Home return were also checked. |
| Display, sound, and battery | Native settings for brightness/display, sound, battery saving, and power management. | Settings integration is present; charging, battery life, suspend, and thermal behavior require physical-device testing. |
| Files and storage | Android storage services, file access permissions, document pickers, and storage settings. | Available through Android and the apps included in the chosen product; removable storage depends on hardware. |
| Security and privacy | Android app sandboxing, runtime permissions, security settings, and the platform's lock-screen and privacy mechanisms. | Biometrics, encryption, verified boot, and device security certification require validation for each physical-device port. |
| Accessibility and input | Android accessibility settings, input methods, and phone layouts that adapt to available width. | The theme picker was checked at 320dp and 411dp widths. Broader assistive-technology and font-scaling coverage remains part of validation. |

The repository does not bundle Google Play or Google Mobile Services. Compatibility
with apps that depend on those services is not guaranteed. Supported physical
phones, update delivery, and certification will be documented per device as those
ports are completed.

## The Omarchy experience

- **Phone home screen:** Omarchy typography, menu, day-and-time clock, calendar,
  settings shortcut, and battery status, adapted to touch targets and phone widths.
- **All 22 default themes from the pinned Omarchy source:** original palettes and
  each theme's first upstream wallpaper, with portrait previews and full-screen
  cropping that preserves image proportions.
- **Omarchy theme marketplace:** browse the official community catalog, install
  theme palettes and artwork, and retain downloaded themes through shell updates.
  The bundled catalog is available offline; refresh and downloads require internet.
- **System color integration:** themes apply shell colors, Android light/dark mode,
  system dynamic colors, and Omarchy app colors through Android resource overlays.
  Third-party apps decide whether to follow system colors.
- **Safe theme imports:** community themes supply color data and images. Their
  repository scripts, desktop installers, and executable hooks are not run. A
  theme without artwork uses its own background color and is labeled accordingly.
- **On-device Code workspace:** official pinned ARM64 Codex and Claude Code
  runtimes, real terminals, provider-owned sign-in, and writable project files
  inside an Android app sandbox.
- **Platform extensions:** a signed plugin registry with per-user state and
  isolated services. Executable plugins do not load into SystemUI or
  `system_server`.

See the [mobile profile and themes](packages/apps/OmarchyShell/mobile/README.md)
and [architecture](docs/ARCHITECTURE.md) for implementation details.

## Start here

```sh
./omarchy doctor
./omarchy sync-core
./omarchy fork
./omarchy test
```

The checkout follows the official `android-latest-release` manifest. The initial
`sync-core` downloads the repositories Omarchy modifies. A complete build needs
the rest of AOSP, a case-sensitive filesystem, and a Linux build host:

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
Cuttlefish's WebRTC viewer. The emulator uses a persistent phone-data image and
suppresses desktop metrics/crash-report prompts so one invocation reaches the
phone UI. Set `OMARCHY_WIPE_DATA=1` only when you intend to erase emulator data
for a clean first boot.

macOS can host the native Emulator and this orchestration checkout. The full
AOSP source tree needs a case-sensitive filesystem because some source paths
differ only by letter case; Android images must be built on Linux.

## Verification and device support

The latest simulator checks cover all 22 bundled palettes and wallpaper assets,
community-theme installation, light/dark theme application, theme persistence,
compact theme-picker layouts, and returning Home from native Android settings.
See [theme validation](docs/THEME_VALIDATION.md) and
[Home navigation validation](docs/HOME_RESUME_VALIDATION.md) for the results and
reproduction steps.

The shell APK builds successfully. A full AOSP rebuild is currently blocked by
missing modules and a framework stub dependency cycle in the Linux source tree.
The recovered Mac checkout also has 17 existing project-check failures caused by
missing AOSP files and fork branches. These are tracked limitations, not passing
checks.

A physical-phone release needs a device-specific kernel, bootloader integration,
vendor components, and Android hardware implementations, followed by end-to-end
phone, security, power, and update testing. Emulator results do not establish
compatibility with every phone. See the [roadmap](docs/ROADMAP.md) for hardware
bring-up and remaining validation work.

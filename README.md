# OmarchyOS

**OmarchyOS — Omarchy for phones.**

OmarchyOS is a mobile operating system built on the Android Open Source Project
(AOSP) for its native app ecosystem and core phone capabilities. Android provides
telephony, messaging, connectivity, camera and audio services, hardware support,
permissions, and power management.

OmarchyOS brings the Omarchy home screen, themes, marketplace, and on-device Code
workspace together into a phone experience. Android is the underlying platform
that supplies the phone services and application compatibility.

Open **Omarchy → Apps** for installed applications, **Phone settings** for device
controls, and **Themes** to change the look.

## Phone capabilities powered by Android

The following platform capabilities are retained in OmarchyOS. Availability on a
particular phone depends on its hardware, device port, included apps, and carrier;
see [device support and validation](#verification-and-device-support).

| Capability | Android foundation retained by OmarchyOS |
| --- | --- |
| Calls, contacts, and messages | Telephony, contacts, and messaging support, with the phone apps included by the selected Android product. |
| Android apps | APK installation, installed-app browsing and launching, app management, and permissions. |
| Internet and connectivity | Wi-Fi, Bluetooth, mobile networking, and the Android connectivity settings and services included by the device product. |
| Camera and media | Camera, microphone, audio playback, video, and media APIs for compatible apps. |
| Location and sensors | Location and sensor APIs, plus NFC where the device includes its hardware and services. |
| Notifications and multitasking | Notifications, Quick Settings, lock screen, app tasks, and overview integration. |
| Phone navigation | Native Android Back, Home, recent apps, and gesture navigation. |
| Display, sound, and power | Display and sound controls, battery settings, battery saving, and power management. |
| Files and storage | Storage services, document pickers, file permissions, and storage management. |
| Security and privacy | App sandboxing, runtime permissions, security settings, and lock-screen and privacy infrastructure. |
| Accessibility and input | Android accessibility services, input methods, and accessibility settings. |

## The OmarchyOS experience

- **Phone home screen:** Omarchy typography, menu, day-and-time clock, calendar,
  settings shortcut, and battery status, adapted to touch targets and phone widths.
- **Omarchy screensaver:** a moving Omarchy wordmark, theme colors, and clock,
  with Android managing idle activation and the lock screen. Configure it under
  **Phone settings → Screensaver**; see [screensaver controls](docs/SCREENSAVER.md).
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

The current Omarchy experience runs in the native Android Emulator. Retaining
Android's platform capabilities is separate from validating every feature on a
physical phone; a production-ready physical-phone release is still in development.

Real calls, SMS/MMS, SIM/eSIM, mobile data, IMS/VoLTE, emergency calling, camera
capture, wireless connections, GPS, NFC, biometrics, and charging need compatible
hardware and device-specific testing. Encryption, verified boot, battery life,
thermal behavior, accessibility, and update delivery also require validation for
each device port. Supported phones and certification will be documented per release.

Google Play and Google Mobile Services are not bundled in this repository.
Apps that require those services need a compatible build that provides them.

The latest simulator checks cover all 22 bundled palettes and wallpaper assets,
community-theme installation, light/dark theme application, theme persistence,
compact theme-picker layouts, and returning Home from native Android settings.
See [theme validation](docs/THEME_VALIDATION.md) and
[Home navigation validation](docs/HOME_RESUME_VALIDATION.md) for the results and
reproduction steps.

See [startup performance and remaining work](docs/PERFORMANCE.md) for initialization
measurements, improvements, and unresolved stability observations.

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

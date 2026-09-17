# OmarchyOS architecture

## Foundation

OmarchyOS follows the upstream `android-latest-release` manifest and maintains
local fork branches for the Android projects it changes. It does not replace
Android's Linux kernel, Binder, Package Manager, permission model, app runtime,
or hardware abstraction layers with a web runtime.

## Extension model

The Omarchy plugin system is a small, explicit operating-system contract:

1. An executable plugin is a separate system APK that declares one Omarchy plugin service.
2. The registry discovers services through Package Manager.
3. The registry accepts only packages signed with the same certificate as the
   operating system and protected by a signature permission.
4. The host binds through AIDL. Plugins never load code into `system_server`,
   SystemUI, Launcher, or another plugin.
5. Every plugin declares a versioned manifest and capabilities such as `service`,
   `service`, `settings-panel`, `systemui-tile`, or `launcher-widget`.
6. The host validates the Binder identity and runtime capabilities before it
   delivers lifecycle events such as `host-ready` or `theme-changed`.
7. Enabled state is stored per Android user. Trusted built-ins default to on,
   while the user can disable optional extensions from Settings.
8. User-installed applications cannot become privileged plugins.

This keeps a failed plugin restartable and prevents a theme or Code tool from gaining
ambient access to every system capability.

## Themes

Theme packs are code-free Runtime Resource Overlay APKs. Their signed manifest
contains a schema version, namespaced id, display metadata, dark/light mode,
semantic preview colors, Android palette seeds, and a tonal style. There is no
theme service and no theme DEX to execute.

Theme activation is staged for the current Android user: validate every overlay,
record the pending selection, update Android's Theme Service, update day/night
mode, enable the RRO exclusively, and finalize the selection. If a step fails,
the previous Android palette and display mode are restored. Recording the
pending selection before the RRO switch also makes process relaunches recoverable.

This mirrors Omarchy's palette-first and staged activation model while using
Android-native primitives. Android's generated system color resources carry the
palette through SystemUI, Settings, Launcher, widgets, and compatible apps; RRO
tokens cover Omarchy-owned surfaces. Theme changes are then dispatched to
enabled extensions as a bounded Binder event, the mobile equivalent of a theme
hook.

Omarchy-owned apps consume those generated light/dark semantic resources rather
than duplicating fixed hex palettes. This lets an isolated extension change with
the phone without giving the theme pack executable code or loading extension
code into the shell process.

Omarchy starts with semantic tokens rather than screen-specific colors:

- surface, elevated surface, border, text, muted text
- accent, positive, warning, destructive
- compact, regular, and comfortable corner/spacing scales
- icon shape and system motion levels

The reference image currently includes Mint, Tokyo Night, Catppuccin,
Everforest, Gruvbox, and Catppuccin Latte. The palettes are based on the same
semantic color files used by Omarchy; Latte also verifies that light-mode status
and navigation bars switch correctly.

## Mobile shell

SystemUI owns status bars, notifications, quick settings, lock screen, power
dialogs, privacy indicators, and navigation. Launcher owns Home, installed apps,
widgets, and app launch routing. Omarchy modifies those actual Android projects;
it does not draw a second fake phone UI over Android.

The shell follows several invariants:

- no editable control is focused when Home, quick settings, lock screen, or the
  app library opens;
- touch targets remain accessible without turning every action into a giant
  card;
- layouts adapt to width, insets, cutouts, orientation, and font scaling;
- back, predictive back, deep links, and task restoration use Android routing;
- Code appears as an installed app; its touch-first hub never opens the keyboard,
  and a PTY invokes the keyboard only after an explicit terminal or keyboard tap;
- fresh phone layouts keep reliable Phone, Messages, Settings, Clock, and Camera
  routes in the dock while Code remains in the installed-app library;
- Quick Settings starts with compact touch targets and never covers first open
  with a resize coachmark; sizing remains user-adjustable in explicit edit mode.

## On-device Code workspace

Code is a platform-signed system application but runs its tools inside a normal
Android application UID. Its native PTY starts the official pinned ARM64 Codex
and Claude Code executables in a private writable workspace. Provider login and
token refresh remain inside each upstream CLI; no provider secret is embedded in
the image or passed through Omarchy services.

The child processes can create, edit, and remove workspace files like a desktop
coding tool. The Android UID sandbox is the outer execution boundary, so a CLI
configured for unrestricted workspace execution still cannot silently read
another app's data or control privileged hardware services. Both CLIs receive an
`omarchy-mobile` Agent Skill in their documented user skill directory.

Publishing crosses an explicit user-visible bridge. Sandboxed local app projects
are validated to remain under the workspace and use Android's launcher shortcut
confirmation. Semantic theme and executable plugin projects can be staged on the
device, but global overlays and privileged capabilities still require review,
platform signing, and an OS image or approved package installation. Generated
code is never loaded into SystemUI or `system_server`.

## Hardware

Common Android framework code talks to standardized HAL interfaces. A physical
phone still requires a matching device tree and hardware implementation for its
SoC, modem, camera sensors, audio codec, display, biometric hardware, Wi-Fi,
Bluetooth, GNSS, NFC, and power system. Generic System Images can validate
Treble-compatible framework behavior, but they do not replace those vendor
components.

## Virtual devices

- Android Emulator/goldfish is the native desktop window path and the preferred
  interactive option on Apple silicon after a Linux image build.
- Cuttlefish is the high-fidelity Linux/KVM path used for automated and remote
  testing. Its viewer is WebRTC, not VNC.

Both paths exercise the real Android framework and APKs. Browser mocks remain
useful only for visual prototyping.

The authoritative build checkout lives on a case-sensitive Linux filesystem.
macOS can host the native Emulator and keep this orchestration workspace, but a
case-insensitive APFS checkout cannot represent every AOSP source path correctly.

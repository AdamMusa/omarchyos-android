# quickshell-compat

The Quickshell API surface, implemented with plain Qt Quick so Omarchy's shell
QML runs unmodified on Android. Types are split exactly like upstream's modules
(`Quickshell`, `Quickshell.Io`, `Quickshell.Wayland`, `Quickshell.Hyprland`,
`Quickshell.Services.*`) so `import Quickshell` in Omarchy's files resolves here.

Where upstream talks to a Linux daemon, the compat type reads Android instead
(see app/src/AndroidBridge). Where a concept has no Android equivalent — Wayland
layer surfaces, Hyprland IPC — the type keeps its API and maps onto the nearest
Android behaviour, documented per file.

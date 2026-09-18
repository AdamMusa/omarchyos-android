# OmarchyShell

The OmarchyOS phone shell: Omarchy 4's real Quickshell QML, running on Android.

Quickshell itself is a Wayland/X11 desktop toolkit and cannot be Android's system
shell, so this app pairs Omarchy's unmodified QML with `quickshell-compat`, a
Qt Quick implementation of the Quickshell API surface backed by Android services:

| Quickshell API            | Android backing                                   |
|---------------------------|---------------------------------------------------|
| ShellRoot / PanelWindow   | Qt Quick windows owned by the shell process        |
| Variants / Region         | per-display window variants, input regions         |
| Io.Process                | QProcess against /system_ext/bin/omarchy-* shims   |
| Io.FileView               | QFile + file watcher (theme + shell.json state)    |
| Io.IpcHandler             | local socket, reachable from omarchy-mobile        |
| Services.Pipewire         | AudioManager                                       |
| Services.UPower           | BatteryManager                                     |
| Services.Mpris            | MediaSessionManager                                |
| Services.Notifications    | NotificationListenerService                        |
| Networking / Bluetooth    | ConnectivityManager / WifiManager / BluetoothAdapter|
| Services.Pam / Polkit     | BiometricPrompt / KeyguardManager                  |
| Hyprland workspaces       | ActivityManager tasks + recents                    |

Layout:

    app/                 Qt Quick host app (Home + overlay surfaces)
    quickshell-compat/   the Quickshell API implemented for Android
    third_party/omarchy/ vendored Omarchy 4 shell QML + themes (upstream, unmodified)
    tools/               toolchain setup and build scripts

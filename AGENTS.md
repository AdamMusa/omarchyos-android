# OmarchyOS Android fork

This tree is an AOSP `repo` workspace. Treat every directory containing its own
`.git` file or directory as an independent Git project.

## Product rules

- OmarchyOS is a phone operating system based on AOSP, not a web shell.
- Keep hardware support behind Android HALs and device trees. Never hard-code a
  single test terminal, board, screen size, or input device into shared UI code.
- System UI must remain usable at compact phone widths and with font scaling.
- The keyboard may open only for an explicitly focused editable field.
- Plugins run out of process and are admitted only when signed by the OS key.
- Themes use Android Runtime Resource Overlays (RROs); theme APKs contain no code.
- Do not load third-party DEX into `system_server` or SystemUI.
- Preserve Android security, permission, multi-user, accessibility, and lifecycle
  behavior when changing framework or application code.

## Source ownership

- `frameworks/base`, `frameworks/libs/systemui`, `packages/apps/Launcher3`,
  `packages/apps/Settings`, and `packages/apps/ThemePicker` are fork-owned.
- New Omarchy platform applications live below `packages/apps/Omarchy*`.
- Runtime themes live below `packages/overlays/OmarchyThemes`.
- Product definitions live below `device/omarchy`.

Run `./omarchy test` before handing off a change. Android images must be built on
Linux; macOS is supported as a native Android Emulator host for built images.

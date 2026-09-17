# Product roadmap

## Slice 1 — fork foundation

- initialize official AOSP manifest and fork branches
- add Omarchy virtual products
- add signed plugin registry and AIDL contracts
- add runtime theme controller and six reference themes
- add Settings and ThemePicker entry points
- establish native Emulator and Cuttlefish smoke tests

## Slice 1.5 — Omarchy extension architecture

- discover signed executable plugins and code-free RRO theme packs separately
- validate versioned, namespaced manifests and runtime Binder identities
- persist enabled state and active theme per Android user
- stage theme changes with rollback and process-relaunch recovery
- connect theme seeds to Android's system-wide dynamic color service
- deliver bounded `host-ready`, `plugin-enabled`, and `theme-changed` events
- provide a compact mobile Settings surface with no automatic text focus

## Slice 2 — mobile shell

- redesign status bar, notification shade, quick settings, lock screen, power UI,
  volume UI, and navigation in SystemUI
- redesign Launcher Home, app library, folders, widgets, and recents surfaces
- enforce keyboard/focus, insets, accessibility, and responsive-layout tests

## Slice 3 — core phone applications

- phone, dialer keypad, in-call UI, contacts, messaging, camera, gallery, clock,
  calculator, files, browser handoff, and the installed Code workspace
- platform-consistent empty, offline, permission, loading, and error states
- end-to-end intent routing and task restoration tests

## Slice 4 — hardware bring-up

- choose the first unlockable, AOSP-friendly reference phone
- integrate its kernel/device tree and redistributable vendor components
- validate telephony, IMS/emergency calling, camera, audio, sensors, connectivity,
  location, biometrics, encryption, verified boot, suspend, thermals, and OTA
- run CTS, VTS, STS, GTS where applicable, and long-duration power tests

Supporting “all phones” is a portfolio effort: shared Omarchy framework and UI
code plus a separately maintained bring-up for every hardware family.

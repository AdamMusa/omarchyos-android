# Native system UI checks

This disposable, normally signed app exercises real Android permission prompts,
notifications and framework dialogs. It is not shipped in the OS and requests no
platform privileges. Build it with `build-probe.sh OUTPUT_DIRECTORY`, then install
the resulting `probe.apk` on a development emulator.

Launch `os.omarchy.uicheck/.ProbeActivity` with a `mode` string extra:

- `permission` requests microphone access. Verify Deny and Only this time through
  the actual prompt, and inspect the `OmarchyUiCheck` log plus the package's runtime
  permission flags. Check large text and light/dark themes.
- `notification` posts a native notification after notification permission is
  granted to this test app. Verify it renders after a cold boot with the lock
  screen disabled, before opening the lock screen even once. Verify expansion,
  its Check action button, content tap, dismissal and Clear all.
- `dialog` opens an Android AlertDialog with Cancel and Done actions.

The notification tap/action logs `notification result=opened` or `action`.
Permission callbacks log Android's result codes (`-1` denied, `0` granted).
Save screenshots and dumps with the build being tested. Restore any temporary
display, theme and lock settings. Uninstall `os.omarchy.uicheck` after validation
to remove its permissions, notification channel and notifications.

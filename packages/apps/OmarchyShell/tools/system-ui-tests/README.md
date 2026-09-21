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
- `volume` shows a button that requests Android's native volume panel with
  `ADJUST_SAME` and `FLAG_SHOW_UI`. The request preserves the media volume and
  logs its before/after values. It uses the ordinary `MODIFY_AUDIO_SETTINGS`
  permission. Test volume keys while this activity is foreground, then restore
  the original media volume. Do not use `cmd media_session volume --set` as a
  substitute: this development image rejects its shell/package attribution,
  even though the command prints that it is setting the volume.

The build also generates `dex/classes.dex` containing `SystemUiWindowProbe`, a
development-only helper for inspecting SystemUI accessibility windows. Unlike a
default UiAutomator dump, it includes the unfocused volume overlay and reports
slider ranges. With the probe APK installed on an English development emulator:

```sh
python3 packages/apps/OmarchyShell/tools/test-system-volume.py \
  --serial emulator-5556 --probe-dex OUTPUT_DIRECTORY/dex/classes.dex \
  --screenshots out/volume-check
```

This checks compact and expanded panels, media-volume key and slider-touch changes and readback,
accessible slider ranges, and Done/Back dismissal. It temporarily changes the
media level and extends the panel timeout, restoring both in cleanup. Run on a
development emulator with no active media playback.
Both panels must expose one Omarchy navbar with matching control positions. The
expanded modal hosts the shared native bar inside its own accessibility window:
its Back button must dismiss the panel, and logo, gear and clock must close the
modal before opening Home's menu, phone settings menu or calendar. The test saves
those destinations for visual inspection as well as checking the resumed Home.
Long-pressing the gear must also dismiss the modal before opening Quick Settings;
the check verifies that the shade receives focus and no volume rows remain above it.
At large text sizes, the check scrolls the volume rows to reach all five streams
and confirms the navbar stays fixed while the body moves.

The notification tap/action logs `notification result=opened` or `action`.
Permission callbacks log Android's result codes (`-1` denied, `0` granted).
Save screenshots and dumps with the build being tested. Restore any temporary
display, theme and lock settings. Uninstall `os.omarchy.uicheck` after validation
to remove its permissions, notification channel and notifications.

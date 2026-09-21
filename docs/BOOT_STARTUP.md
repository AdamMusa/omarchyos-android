# Omarchy startup surfaces

The boot animation, Android's direct-boot Home, the starting window and the Qt
startup curtain share Omarchy artwork. The direct-boot Home keeps Android's
unlock and Home-role behavior, but draws immediately instead of waiting for a
progress timeout or a wallpaper service.

The shell keeps an opaque native curtain above Qt while assets, plugins and the
wallpaper become ready. Two completed Qt frame swaps release the curtain and
report Home fully drawn. A wallpaper load error permits the existing solid-color
fallback; it does not leave startup waiting forever. After 15 seconds the curtain
announces that startup is taking longer. System navigation remains available.

The original screensaver artwork is used for the wordmark. The generated boot
animation is a build artifact; generate it before a normal AOSP build. The
incremental emulator image tool generates its own copy in the output partition.
The boot overlay must be installed in the system image and active during direct
boot. A sideloaded development overlay is not an equivalent test.

Validate a cold boot and a Home process restart, inspect the first-frame log,
and record the transition from boot animation through Home. Keep testing secure
lock/unlock separately; boot branding must not bypass authentication.

On the September 20 native-design image, Home reached its first complete frame
from the base system APK after installing the missing system Bash executable.
A cold-boot recording showed Omarchy artwork followed by the Omarchy Home, with
no stock launcher. It also exposed a brief black interval between the direct-boot
Home and the shell curtain. That handoff still needs work before claiming a fully
continuous boot animation; successful Home startup alone does not verify it.

The v5 incremental image moves the startup curtain into an activity-owned window
above Qt's SurfaceView. Its first draw is logged, and a warm Home process restart
reaches the completed frame and removes that window. FallbackHome now launches
the resolved Home before finishing. The cold-boot recording still contains a
black interval before the shell activity's curtain appears; these changes improve
the curtain's visibility but do not yet eliminate the earlier handoff gap.

Android suppresses Home splash windows in two places:
`ActivityRecord.getStartingWindowType` in the framework and
`PhoneStartingWindowTypeAlgorithm` in WindowManager Shell. Enabling the first
alone in v7 still leaves a black interval in the cold-boot recording. Both
policies must permit the configured Omarchy Home, gated by the Omarchy product
brand and `ro.omarchy.shell`; other launchers retain Android's default policy.
Both policy changes are present in the v9 image. Its logs confirm Android creates
and transfers the Omarchy starting surface, and Home reaches its first complete
frame. A cold-boot recording still shows a black interval earlier in the handoff;
this does not establish a continuous boot transition.

For incremental builds, rebuild `wm_shell_protolog_src` as well as
`WindowManager-Shell` and `SystemUI`. The phone algorithm is compiled from that
generated source archive; rebuilding its consumer against a cached archive can
silently retain the old Home policy. Verify the Omarchy gate in the generated
source and compiled class before assembling an image.

Shell version 7 explicitly requests the icon splash style. Without this, Android
inherits the solid-color starting style from the direct-boot Home and omits the
Omarchy artwork. The splash asset uses a square vector with an internal safe area
so the complete wordmark scales together; fixed-size layer-list children can be
cropped when Android rasterizes the icon at its system-selected size.

The v10 image includes the vector correction and passes built-in Home protection
and the full native navbar/navigation check. The boot-gap limitation above still
applies; those functional checks are not proof of an uninterrupted visual handoff.

## Reproducible boot evidence

Reboot a development emulator and capture until the actual Home-frame signal:

```sh
python3 packages/apps/OmarchyShell/tools/capture-system-boot.py \
  --serial emulator-5556 --size 540x1200 --output out/boot-check
```

Use a new output directory. The tool verifies that the kernel boot ID changes,
records with a host-clock timeout, and preserves raw log bytes. It saves a
screenshot at Home's readiness signal and another after settling. It never
unlocks the device or treats the readiness log as proof of a correct picture.
Inspect the video and both screenshots; also compare the actual emulator window
when available. The encoder can lag or omit the final frames at full resolution.

The v17 check reproduced a wallpaper flash followed by a black interval and then
the completed Home. The reduced-resolution video contains the black interval at
media timestamps 33.002–35.195 seconds. Its independent screenshot taken at the
Home-ready signal is also black. Logs report Home ready at 47.695 seconds, before
the activity receives its native status-bar insets at 49.315 seconds. This is
evidence that Qt's initial frame count alone releases startup too early; it is
not proof of the whole rendering cause. Local evidence is under
`out/mobile-check/native-v17-boot-scaled` (the initial recorder named its
signal-time screenshot `home.png`).

Individual host-clock observations were 56.704 seconds to the Home signal while
recording at full resolution, 37.627 seconds without recording, and 50.233 seconds
while recording at 540x1200. These are single diagnostic runs of `adb reboot` on
the same image, not a startup benchmark or a measurement of physical-phone boot
time. Recording perturbs the experiment. The Mac was locked, so direct host-window
inspection was unavailable; neither the boot gap nor overall startup latency is
declared resolved by these measurements.

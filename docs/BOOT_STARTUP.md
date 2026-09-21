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

A source trace identifies another part of the gap: Android's
`ActivityRecord.getStartingWindowType` excludes Home activities from normal
splash starting windows. The shell's configured splash therefore cannot cover
the interval before its activity creates the native curtain. An Omarchy-only
framework change needs its own build and cold-boot validation; it has not yet
been applied.

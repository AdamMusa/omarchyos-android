# Native system UI validation

This checklist tracks runtime behavior, not just successful compilation or
overlay installation. Emulator validation does not qualify physical phone hardware.

| Surface | Evidence and remaining work |
| --- | --- |
| Built-in Home | The v17 emulator image launches Shell version 7 from its privileged base APK with the installed Bash executable. Core 41 also resolves to its base system APK. After a fresh reboot, `test-system-home.py --check-app-info` passes the default-Home/package checks and confirms native Settings disables Disable and omits ordinary Uninstall. Local result: `out/mobile-check/native-v17-home-check.log`. |
| Home and service navbar | The v16 navigation check passes Home, five Settings services, nested Bluetooth pairing, Overview, notifications, Quick Settings and tile editing without restarting SystemUI. Back pops nested pages and disappears on Home; logo, clock and gear keep their bounds. The final v17 Home/Connected devices check and screenshots confirm the same bar with Back added beside the logo in the service. Local evidence: `out/mobile-check/native-v16-navigation-check.log`, `native-v17-navbar-check.log`, and `native-v17-navbar/{home,bluetooth}.png`. |
| Quick Settings/shade header | Home and the shade reuse the same native OmarchyBar. The v6 screenshot checks confirm identical Tokyo Night navbar colors and control bounds across Home, Wi-Fi, Overview, notifications, Quick Settings and tile editing. The navbar stays outside the scrolling Quick Settings body. Privacy indicators retain their native path. Compact 320dp-width checks at 200% font scale pass without overlapping or duplicate controls, including tile editing; service and shade layouts also pass in landscape. Home remains portrait-oriented. |
| Native notifications | The v6 rebuilt image renders notification rows after a cold boot with keyguard disabled, before opening the lock screen. Native content and action taps pass. The scene notification container now initializes at SystemUI startup instead of waiting for keyguard composition; footer controls use the compact Omarchy shape. |
| Permission prompts | The v6 system image renders outlined Omarchy action buttons, compact corners and monospace text. Native Deny and Only this time callbacks pass; denial remains reachable at 200% text size and under Catppuccin Latte. Version-qualified styles are retained in the built overlay, and obscured-touch filtering stays in the native permission layout. |
| Power menu | The v14 image defaults long-press Power to native Global Actions and renders compact outlined action buttons. The power-menu check passes action presence, Back dismissal, user-preference override and stable SystemUI. Visual checks confirm complete wrapped labels at 200% text size in compact portrait and landscape layouts. See POWER_MENU.md. |
| Volume controls | The v17 modal hosts the same native navbar. Back and Done dismiss to the calling app; logo, clock and gear dismiss before opening Home panels, and gear long-press opens Quick Settings in front. Native keys, slider touch and accessible ranges pass without a SystemUI restart. The v16 large-text check reaches all rows by scrolling while the navbar stays fixed; its landscape bar/Back check also passes. Core 41 preserves Latte's blue accent, including an automatic migration of the already-selected palette. The output description still uses a marquee. See VOLUME_CONTROLS.md and THEME_VALIDATION.md. |
| Framework dialogs | The v6 native AlertDialog renders Omarchy typography and surfaces; its Done action dismisses normally. |
| Lock screen | The v4 image removes the carrier/navbar overlap and registers the native clock font family with JetBrains Mono. Applying Tokyo Night and Catppuccin Latte updates Android's lock wallpaper; restarting Home preserves the wallpaper ID. Opening the Omarchy menu while securely locked does not bypass keyguard, and entering the correct PIN unlocks normally. The temporary emulator PIN was removed after testing. |
| Boot handoff | The v9 cold-boot recording still contains a black interval before Home. The native curtain draws and is removed after the first complete Home frame; the earlier handoff gap remains unresolved. See BOOT_STARTUP.md. |
| Overview | The v6 image retains native Overview and shows the same Omarchy navbar. Its colors and control bounds match Home, and Back returns normally. |
| Screensaver | Native Omarchy `ttfx` animation and wake were validated; see [SCREENSAVER.md](SCREENSAVER.md). |

The local QA app in `packages/apps/OmarchyShell/tools/system-ui-tests` exercises
native notification, permission and dialog paths. It is not included in the OS.

The v17 navigation check was repeated against the Home navbar reference. It
passes the original Omarchy mark and gear glyphs, non-overlapping controls,
matching Home/service control bounds, nested Back navigation, Overview, the
shade, Quick Settings and tile editing. Back disappears on returning Home and
SystemUI retains its process. Local result:
`out/mobile-check/native-v17-navbar-reference.log`.

The v16 full-navbar regression and v17 modal regression cover the previously
missing full-height modal bar and its navigation actions. These targeted checks
do not establish that every native surface or every imported theme has been
qualified on the final image.

The recovered Mac checkout currently reports 17 missing-source/fork-branch
failures from `./omarchy test`. These are unresolved checks, not passes. Native
components are being built in the matching Linux tree with cached unchanged
dependencies; a clean full-source build remains unverified.

The later v6 light-theme stress pass exposed a Home decor initialization crash
and an independent `TaskSnapshotPersister` failure in `system_server` when the
emulator could not CPU-map a graphics buffer. Shell version 6 fixes the decor
crash and passes dark/light theme relaunch checks. The v7 image also passes a synthetic GPU-only snapshot regression against its
installed services.jar: direct CPU mapping fails, but guarded conversion
completes. Catppuccin Latte navbar colors and geometry now pass across Home,
Wi-Fi, Overview, notifications, Quick Settings and tile editing without a
system_server restart. The later v9 image passes the same snapshot regression
with Shell version 7 in its base system package. These
checks cover this regression, not general device stability. The cold-boot
handoff still requires further work; see BOOT_STARTUP.md.

A later v7 idle period during host compilation ended in an Android watchdog
restart at 02:06 on September 21. The watchdog named CpuMonitorService (74-second
handler delay), and its report recorded CPU pressure `some avg10=91.76`, with no
memory pressure. The thread dump was in native message polling; it did not show
the earlier task-snapshot exception or an Omarchy stack. Host contention is a
possible contributor, not a proven root cause. The v9 unloaded navigation, secure
PIN and snapshot checks retained the boot
process IDs for system_server and SystemUI with an empty crash buffer. That short
run does not explain or rule out the v7 watchdog event. Do not disable or relax
the watchdog to hide it.

The v10 follow-up reproduced a Settings ANR while opening the Settings homepage
and testing a volume key (September 21, 03:03). Its reason was “Application does
not have a focused window”; the main-thread sample was runnable in Android's
Looper/compatibility check. Guest CPU utilization reached 99%, with CPU pressure
`some avg10=61.51`, I/O pressure `some avg10=15.33`, and no memory pressure. This
is additional unresolved emulator stability evidence, not a confirmed Omarchy
code defect or proof that host contention is the sole cause.

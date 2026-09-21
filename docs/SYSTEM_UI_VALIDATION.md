# Native system UI validation

This checklist tracks runtime behavior, not just successful compilation or
overlay installation. Emulator validation does not qualify physical phone hardware.

| Surface | Evidence and remaining work |
| --- | --- |
| Built-in Home | The v3 emulator image launches the privileged base APK and installed Bash executable. `test-system-home.py` checks default Home and package protection. |
| Home and service navbar | The v3 navigation check passes Home, five Settings services, and nested Bluetooth pairing. Back pops one page and disappears on Home; logo, clock and gear keep their bounds. |
| Quick Settings/shade header | Selected colors and compact tiles are visible. The expanded header still uses a different clock/date/branding layout from the Home navbar; unify it before claiming identical navigation everywhere. |
| Native notifications | The v4 rebuilt image renders notification rows after a cold boot with keyguard disabled, before opening the lock screen. Native content and action taps pass. The scene notification container now initializes at SystemUI startup instead of waiting for keyguard composition; footer controls use the compact Omarchy shape. |
| Permission prompts | Native denial works. The Omarchy permission overlay has compiled; verify actual Deny and Only this time choices, large text and both palette modes in the rebuilt image. |
| Framework dialogs | The baseline Android AlertDialog renders Omarchy typography and surfaces. Recheck actions in the rebuilt image. |
| Lock screen | Baseline carrier text overlaps the navbar, and the clock/wallpaper differ from Home. Carrier, product font and native wallpaper changes are pending rebuilt-image verification, including authentication. |
| Boot handoff | Baseline cold boot exposes a brief black interval. The revised FallbackHome handoff has compiled; record and inspect the rebuilt image before declaring the gap fixed. |
| Overview | The v3 image uses the product overview overlay and retains native task switching. Include it in the final regression pass. |
| Screensaver | Native Omarchy `ttfx` animation and wake were validated; see [SCREENSAVER.md](SCREENSAVER.md). |

The local QA app in `packages/apps/OmarchyShell/tools/system-ui-tests` exercises
native notification, permission and dialog paths. It is not included in the OS.

The recovered Mac checkout currently reports 17 missing-source/fork-branch
failures from `./omarchy test`. These are unresolved checks, not passes. Native
components are being built in the matching Linux tree with cached unchanged
dependencies; a clean full-source build remains unverified.

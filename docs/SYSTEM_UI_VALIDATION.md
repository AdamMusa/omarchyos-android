# Native system UI validation

This checklist tracks runtime behavior, not just successful compilation or
overlay installation. Emulator validation does not qualify physical phone hardware.

| Surface | Evidence and remaining work |
| --- | --- |
| Built-in Home | The v6 emulator image launches the privileged base APK and installed Bash executable. Omarchy Core also resolves to its base system APK. `test-system-home.py` checks default Home and package protection. |
| Home and service navbar | The v6 navigation check passes Home, five Settings services, nested Bluetooth pairing, Overview, notifications, Quick Settings and tile editing. Back pops nested pages and disappears on Home; logo, clock and gear keep their bounds. |
| Quick Settings/shade header | Home and the shade reuse the same native OmarchyBar. The v6 screenshot checks confirm identical Tokyo Night navbar colors and control bounds across Home, Wi-Fi, Overview, notifications, Quick Settings and tile editing. The navbar stays outside the scrolling Quick Settings body. Privacy indicators retain their native path. Compact 320dp-width checks at 200% font scale pass without overlapping or duplicate controls, including tile editing; service and shade layouts also pass in landscape. Home remains portrait-oriented. |
| Native notifications | The v6 rebuilt image renders notification rows after a cold boot with keyguard disabled, before opening the lock screen. Native content and action taps pass. The scene notification container now initializes at SystemUI startup instead of waiting for keyguard composition; footer controls use the compact Omarchy shape. |
| Permission prompts | The v6 system image renders outlined Omarchy action buttons, compact corners and monospace text. Native Deny and Only this time callbacks pass; denial remains reachable at 200% text size and under Catppuccin Latte. Version-qualified styles are retained in the built overlay, and obscured-touch filtering stays in the native permission layout. |
| Framework dialogs | The v6 native AlertDialog renders Omarchy typography and surfaces; its Done action dismisses normally. |
| Lock screen | The v4 image removes the carrier/navbar overlap and registers the native clock font family with JetBrains Mono. Applying Tokyo Night and Catppuccin Latte updates Android's lock wallpaper; restarting Home preserves the wallpaper ID. Opening the Omarchy menu while securely locked does not bypass keyguard, and entering the correct PIN unlocks normally. The temporary emulator PIN was removed after testing. |
| Boot handoff | The v6 cold-boot recording still contains a black interval before Home. The native curtain draws and is removed after the first complete Home frame; the earlier handoff gap remains unresolved. See BOOT_STARTUP.md. |
| Overview | The v6 image retains native Overview and shows the same Omarchy navbar. Its colors and control bounds match Home, and Back returns normally. |
| Screensaver | Native Omarchy `ttfx` animation and wake were validated; see [SCREENSAVER.md](SCREENSAVER.md). |

The local QA app in `packages/apps/OmarchyShell/tools/system-ui-tests` exercises
native notification, permission and dialog paths. It is not included in the OS.

The recovered Mac checkout currently reports 17 missing-source/fork-branch
failures from `./omarchy test`. These are unresolved checks, not passes. Native
components are being built in the matching Linux tree with cached unchanged
dependencies; a clean full-source build remains unverified.

The later v6 light-theme stress pass exposed a Home decor initialization crash
and an independent `TaskSnapshotPersister` failure in `system_server` when the
emulator could not CPU-map a graphics buffer. Shell version 6 fixes the decor
crash and passes dark/light theme relaunch checks. The snapshot recovery and
Home starting-window framework changes still require validation in the next
system image; the earlier v6 navigation pass is not a general stability claim.

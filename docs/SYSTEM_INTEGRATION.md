# Built-in Omarchy Home

Android implements Home, System UI, Settings, and permission controllers as system
packages. Omarchy's APK format is compatible with that architecture; installing
only a launcher on ordinary Android is not the same as shipping this OS fork.

The September 20 development emulator initially contained a sideloaded Omarchy
shell in `/data/app` without a base copy in its older system image. That was an
integration gap, despite the product already listing the shell as a system app.

The product now explicitly installs:

- Platform-signed Omarchy Home in `/system_ext/priv-app/OmarchyShell`.
- Its privileged-permission allowlist on the same partition.
- A preferred-Home configuration for newly provisioned users.
- A platform Core boot hook that assigns the Home role when it has no holder,
  including after a new user's first unlock. It verifies that Omarchy is a
  platform-signed system package and preserves an existing launcher choice.
  The running service also watches for an empty Home role after setup finishes.
- Quickstep for Android's gesture navigation and overview implementation.

Home has no ordinary app-drawer entry and is restricted to internal installation.
Android Settings protects bundled Home components against user-facing disable
and uninstall actions. System packages can still appear in Apps or recent usage;
that visibility should not be hidden to imply integration. Developer root access
and deliberately modified images are outside normal user-facing protections.

## Building and checking

The normal path remains a full AOSP product build with the platform-signed shell
prebuilt supplied. The prebuilt no longer declares a nonexistent allowlist module:
the product installs that shared XML through `PRODUCT_COPY_FILES`.

For incremental **development emulator** work with a matching existing Linux
AOSP build, `packages/apps/OmarchyShell/tools/rebuild-emulator-image.sh` takes
`AOSP_ROOT PRODUCT_OUT PLATFORM_SIGNED_SHELL_APK PLATFORM_SIGNED_CORE_APK OUTPUT_DIR [BUILT_OVERLAY_DIR]`.
The Core APK can be built incrementally with
`packages/apps/OmarchyCore/tools/build-dev-apk.sh` and the same platform tree.
The image tool reconstructs `system_ext` and `product` (OEM fonts and boot artwork),
the dynamic-partition container, signed verified-boot metadata, and the combined
emulator image. It retains verified-boot hashtrees and uses the emulator test keys;
it is not a production signing pipeline. Existing input images and userdata remain
unchanged. Native ARM64 libraries are installed beside the system APK.
The bundled Bash executable is also installed as `/system_ext/bin/bash`; leaving
it only in the system app's non-executable library directory prevents plugin
discovery and keeps the Home startup curtain visible.

Deploy the generated `system-qemu.img` and matching `VerifiedBootParams.textproto`
together with the emulator stopped. Keep the old pair for rollback and retain the
persistent userdata. Do not replace this step with a bare `adb install`: that only
tests the application, not its integration into the system image.

Run `python3 packages/apps/OmarchyShell/tools/test-system-home.py --serial DEVICE`
after booting. Also inspect Home's App info page, verify the disabled Disable
control and absence of ordinary Uninstall, and test Home/Back/overview. A complete
source rebuild and broader device validation remain tracked in [PERFORMANCE.md](PERFORMANCE.md).

## Development image validation

On the September 20 emulator, the shell has SYSTEM and PRIVILEGED flags and a
base APK on the read-only system partition. App info has no ordinary Uninstall
and its Disable control is disabled. Ten Settings/Back cycles restored Home.
A disposable secondary user received Omarchy after setup; changing its Home role
to Quickstep remained effective. That test user was removed afterward. The Core
hook waits for setup completion and excludes a temporary setup-wizard Home role.

`./omarchy test` still reports 17 existing incomplete-source/fork-branch checks;
these are not a successful full AOSP rebuild. See the performance document for
those build limitations.

The later native-design image was tested with Shell v2 running directly from its
base system APK. Five static design overlays are active after reboot. This older
development userdata contained stale `updated-package` records left by previously
sideloaded versions of those overlays; they prevented PackageManager from loading
the built-in copies. Their five stale update records were removed while Android
services were stopped, after backing up the package database. Other package
records and user data were retained. This was a development-data recovery, not a
shipping boot hook or a reason to edit package databases on normal devices.

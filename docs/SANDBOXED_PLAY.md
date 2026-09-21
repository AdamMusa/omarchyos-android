# Sandboxed Google Play

OmarchyOS runs Google Play the way GrapheneOS does. The OS ships a
compatibility layer (`gmscompat`). Google's own Play services, Play Store and
Google Services Framework are installed by the user as **ordinary, unprivileged
apps**:

- They run in the normal `untrusted_app` SELinux domain. The `gmscore_app`
  domain is disabled.
- They get no privileged or signature permissions and no special access to
  identifiers, other apps' data or the system. They only get the permissions
  the user grants, as with any other app.
- They can be installed per user or profile, turned off, or uninstalled.

The compatibility layer changes how the OS handles Play services, not what
Play services can reach. Calls that would need privileges it doesn't have are
redirected, stubbed or handed to the user as a prompt.

## Where the code comes from

The code is GrapheneOS's MIT/Apache-licensed implementation, branch `17`
(based on `android-17.0.0_r1`):

| Source | Location in this repo |
| --- | --- |
| GmsCompat app (`app.grapheneos.gmscompat`) + GmsCompatLib | `packages/apps/GmsCompat` (upstream rev in `GmsCompat.rev`) |
| GmsCompatConfig (prebuilt config APK) | `external/GmsCompatConfig` |
| Framework and module changes | `patches/sandboxed-play/<aosp path>/*.patch` |

`patches/sandboxed-play/frameworks/base` has the gmscompat commits plus the
GrapheneOS infrastructure they depend on: PackageHooks, GosPackageState,
AppBindArgs, ext settings, special runtime permissions, BinderWrapper and
related commits. The dependency set was found by cherry-picking onto
`android-17.0.0_r1` and adding each commit that a conflict showed was needed.
GrapheneOS changes that gmscompat doesn't need were left out: the HTTPS time
source, UART and prototype-device warnings, the browser package name, the
OTHER_SENSORS permission and forwarded notifications.

Apply the patches inside the builder after `repo sync`:

```sh
docker exec omarchy-aosp-builder bash -lc 'cd /workspace && /omarchy-src/device/omarchy/tools/apply-sandboxed-play.sh'
```

The script is idempotent. It skips any patch whose subject is already in a
project's history, and it stops at the first patch that doesn't apply cleanly.

## Installing Play

GmsCompat recognizes Google's apps by their signing certificates, so install
the official APKs: Google Services Framework, then Play services, then the Play
Store.

```sh
adb install-multiple <GSF split APKs>
adb install-multiple <Play services split APKs>
adb install-multiple <Play Store split APKs>
```

After that, open **Settings > Apps > Sandboxed Google Play** (the GmsCompat
app) to review the permissions Play has and to handle compatibility prompts.
An in-OS installer in the Omarchy marketplace is planned. GrapheneOS's own App
Store is not included, because it downloads from GrapheneOS's servers.

## What still won't work: Play Integrity

Sandboxed Play runs most Play-dependent apps: push notifications, maps,
sign-in, in-app purchases and Android Auto. It **does not** make OmarchyOS pass
Google's Play Integrity `MEETS_DEVICE_INTEGRITY` or `MEETS_STRONG_INTEGRITY`
verdicts. Those verdicts require a Google-certified OS build, and only Google
issues that certification. As on GrapheneOS, apps that require that
certification refuse to run, for example Revolut logins since December 2024,
Google Wallet tap-to-pay and some banking and streaming apps.

OmarchyOS does **not** fake certification: no spoofed fingerprints, no leaked
attestation keys and no hidden-root tricks. Faking it gets around a
security check that banks rely on, breaks Google's and the app's terms, puts
users' accounts at risk, and stops working whenever Google revokes the keys.
The supported path is the one GrapheneOS takes: on real hardware with a locked
bootloader and OmarchyOS's own verified-boot key, apps can check OmarchyOS
with Android's hardware key attestation API. Some banks accept that.

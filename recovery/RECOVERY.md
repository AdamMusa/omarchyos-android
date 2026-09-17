# OmarchyOS Android: recovery notes

Recovered on 2026-09-16 from the Codex thread "Add OS simulator run script"
(share cx_6aab400a83b48191ac305e05fa289201) and its local rollout log in
~/.codex/sessions/2026/08/29/. The original workspace was deleted on
purpose, and the deletion bypassed the Trash.

## What was restored
- **123 source files, complete.** They are rebuilt by replaying every Codex
  `apply_patch` in order. Each file matches the diffs in the ChatGPT share by
  SHA-256. This covers the product makefiles, OmarchyCore (plugin manager, AIDL
  contract, theme controller), OmarchyAgent, the OmarchyCode hub, project and
  publish activities with the claude/codex/omarchy-mobile scripts, the theme RROs
  (catppuccin, catppuccin_latte, tokyo_night, everforest, gruvbox), the
  SystemUI/Launcher3 overlays, permissions, sepolicy, the boot animation
  generator, the Linux builder Dockerfile, the `./omarchy` tool, docs and tests.
- **15 changes to upstream AOSP files**, saved in `recovery/upstream-patches/`
  in Codex apply_patch format. These files started as AOSP sources, so the
  patches only apply on top of a fresh checkout:
  - `packages/apps/OmarchyCode/*` (Terminal.java, TerminalService.java,
    TerminalView.java, jni/, layout, menu, strings, Android.bp, manifest) is a
    fork of AOSP `packages/apps/Terminal`. Copy that project into
    `packages/apps/OmarchyCode` first, then apply the patches in order.
  - Settings `AndroidManifest.xml` and `strings.xml`, SystemUI
    `EditModeButtonViewModel.kt`, goldfish `mk_combined_img.py` and `platform_app.te`.

## What could not be restored
- The AOSP checkout (android-latest-release), `.repo`, `out/`, emulator
  images, signing keys, the Docker/Colima builder VM and its volumes.
- Files created only by shell commands or binaries (for example the generated
  bootanimation.zip; run `device/omarchy/bootanimation/generate.py` to make it again).
- The Linux/Quickshell `omarchyos` project. It was not part of this recovery,
  but its diffs are in the same share and rollout if you need them later.

## Rebuild path
1. Build on case-sensitive Linux (x86-64, about 500 GB free, 64 GB RAM recommended),
   or in the Linux builder from `device/omarchy/tools/linux-builder/Dockerfile`.
2. Run `./omarchy sync` to get android-latest-release, then copy these trees over it.
3. Apply `recovery/upstream-patches/*` (Codex or Claude can apply these hunks).
4. Run `./omarchy build`, then `./omarchy run emulator` (target omarchy_sdk_phone64_arm64).

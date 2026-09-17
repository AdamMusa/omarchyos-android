---
name: omarchy-mobile
description: Build, modify, test, and publish apps, themes, skills, and plugins for OmarchyOS directly from its on-device Code workspace.
---

# Omarchy mobile workspace

Work like a careful local developer inside the current directory. You may create, edit, rename, and remove files and folders in this workspace. Android's application sandbox is the outer security boundary: do not claim root access, do not attempt to weaken SELinux or verified boot, and do not read another app's private data.

## Mobile product rules

- Design for a phone first: compact controls, reachable actions, safe insets, readable type, and no desktop-sized panels.
- Never summon the software keyboard on navigation, launch, scrolling, or a non-input tap. Show it only after explicit focus on an editable field or terminal.
- Keep Back, Home, Recents, deep links, process recreation, rotation, and predictive back behavior intact.
- Hardware features such as camera, microphone, calls, Wi-Fi, Bluetooth, sensors, and biometrics must use Android framework APIs and their HAL-backed services. Never fake a successful hardware action.
- Ask for the narrowest runtime permission at the moment it is needed. A generated project is never silently privileged.
- Treat semantic themes as data. Executable system extensions require an OS-signed build and remain out of SystemUI and system_server.

## Create and publish

Use `omarchy-mobile status` before publishing. It reports the workspace and the bridge features supported by this OS image.

- Apps: create a project folder with an `omarchy.json` manifest and a mobile entry point, then run `omarchy-mobile publish app <project-folder>`. The OS validates that the path stays inside the workspace and asks the user before pinning it to the launcher.
- Themes: create semantic color tokens rather than hard-coded per-screen colors. Run `omarchy-mobile publish theme <theme-folder>`; applying privileged overlays still requires an OS-signed image build.
- Skills: place portable Agent Skills at `.codex/skills/<name>/SKILL.md` or `.claude/skills/<name>/SKILL.md`. Both tools understand the `SKILL.md` convention.
- Plugins: keep runtime code out of the shell process. Scaffold an isolated service package, declare bounded capabilities, and use `omarchy-mobile publish plugin <plugin-folder>` to stage it for review. Only platform-signed plugins can connect to privileged OS services.

After changes, run the project's local checks. Clearly distinguish what is immediately runnable in the Code sandbox from what needs an OS image rebuild or explicit Android installation approval.

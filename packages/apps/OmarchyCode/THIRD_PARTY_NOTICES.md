# Omarchy Code runtime notices

The OS image vendors pinned upstream command-line runtimes so the emulator and
device builds are reproducible and do not execute a first-boot downloader.

## OpenAI Codex CLI

- Package: `@openai/codex@0.152.1-linux-arm64`
- Upstream: <https://github.com/openai/codex>
- Main binary SHA-256: `285958769bc41cd70812524ca96631bf679635f4f1c08320ab1f31d958e81b24`
- License: Apache-2.0

Authentication is performed by the official Codex CLI. OmarchyOS does not
collect, proxy, or embed a user API key.

## Anthropic Claude Code

- Version: `2.1.258`
- Upstream: <https://code.claude.com/docs/en/overview>
- Linux ARM64 musl binary SHA-256: `d3bef6ba403fe3efba684fa93e1bb26bc7a08c054bd516639d54e77c77bb9821`
- Terms and license: see the upstream Claude Code distribution and Anthropic terms.

Authentication is performed by the official Claude Code CLI. OmarchyOS does
not collect or proxy user credentials.

## musl libc runtime loader

- Package source: Alpine Linux v3.22 `musl-1.2.5-r12`
- Loader SHA-256: `b5afeb0dcc9e22e92f1088566b0d1c1562ef567a4abebaf4b0fd14000800b8ed`
- License: MIT

The loader is included only to execute the official Linux ARM64 musl build of
Claude Code on Android's ARM64 kernel.

## libvterm

- Android source revision: `2caab416f758b648c4034e9f22cb7b76c37203f0`
- Upstream: <https://android.googlesource.com/platform/external/libvterm/>
- Copyright (c) 2008 Paul Evans
- License: MIT (see `third_party/libvterm/LICENSE`)

The library renders the real PTY byte stream; it is not a simulated terminal UI.

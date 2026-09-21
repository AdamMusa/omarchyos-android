# Omarchy screensaver engine

Unmodified source from [omacom/ttfx](https://github.com/omacom/ttfx), revision
`7203e354498462064b7c0a89375051f65cf2ce99` (version 0.3.2), under the included MIT
license. Omarchy's `bin/omarchy-screensaver` uses this engine for its random text
effects. This is the Rust port of ChrisBuilds' TerminalTextEffects.

OmarchyOS links the engine into a separate native Android screensaver library.
The Android host replaces terminal output with Canvas rendering; upstream effect
implementations are unchanged. Android owns idle activation, wake, and keyguard.

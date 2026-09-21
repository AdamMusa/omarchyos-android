#!/usr/bin/env bash
# Build the unchanged upstream effects with a small JNI host for Android Dream.
set -Eeuo pipefail
source_dir=$(cd "$(dirname "$0")/.." && pwd)
output=${1:?Pass a Cargo target output directory}
: "${ANDROID_NDK_ROOT:?Android NDK is required}"
command -v cargo >/dev/null || { echo 'Install Rust with the aarch64-linux-android target first' >&2; exit 1; }
export CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER="$ANDROID_NDK_ROOT/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android34-clang"
cargo build --manifest-path "$source_dir/screensaver-native/Cargo.toml" --locked \
  --release --target aarch64-linux-android --target-dir "$output"

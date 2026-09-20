#!/usr/bin/env bash
# Builds GNU bash for aarch64 Android.
#
# Omarchy's shell drives the OS through bash: the plugin scan alone uses
# process substitution, [[ ]] and local, none of which Android's mksh has. So
# OmarchyOS ships a real bash rather than rewriting upstream's scripts.
set -Eeuo pipefail
source /toolchain/env.sh
VER=${VER:-5.2.37}
OUT=${OUT:-/toolchain/bash-android}
SRC=/toolchain/src
mkdir -p "$SRC" "$OUT"

if [ ! -d "$SRC/bash-$VER" ]; then
  curl -fsSL "https://ftp.gnu.org/gnu/bash/bash-$VER.tar.gz" -o "$SRC/bash.tar.gz"
  tar -xzf "$SRC/bash.tar.gz" -C "$SRC"
fi

TOOLCHAIN="$ANDROID_NDK_ROOT/toolchains/llvm/prebuilt/linux-x86_64/bin"
API=${API:-34}
export CC="$TOOLCHAIN/aarch64-linux-android$API-clang"
export AR="$TOOLCHAIN/llvm-ar"
export RANLIB="$TOOLCHAIN/llvm-ranlib"
export STRIP="$TOOLCHAIN/llvm-strip"
# bash 5.2 still has K&R-era declarations in its bundled termcap/readline;
# clang 17 makes those hard errors by default.
export CFLAGS="-Os -fPIE -Wno-implicit-function-declaration -Wno-implicit-int -Wno-int-conversion -Wno-incompatible-function-pointer-types"
export LDFLAGS="-static -fPIE -pie"

cd "$SRC/bash-$VER"
make distclean >/dev/null 2>&1 || true
./configure --host=aarch64-linux-android --without-bash-malloc \
  --disable-nls --enable-static-link \
  bash_cv_getcwd_malloc=yes bash_cv_job_control_missing=present \
  bash_cv_sys_named_pipes=present bash_cv_func_sigsetjmp=present \
  bash_cv_unusable_rtsigs=no bash_cv_printf_a_format=yes \
  bash_cv_ulimit_maxfds=yes bash_cv_dev_fd=standard \
  > "$OUT/configure.log" 2>&1

make -j"${JOBS:-6}" > "$OUT/make.log" 2>&1
"$STRIP" bash -o "$OUT/libbash.so"
file "$OUT/libbash.so"
ls -lh "$OUT/libbash.so"

/*
 * Copyright 2026 OmarchyOS
 * SPDX-License-Identifier: Apache-2.0
 *
 * Compatibility shim for Linux CLI runtimes that parse /etc/resolv.conf
 * directly instead of using Android's netd-backed resolver.
 */

typedef unsigned int mode_t;
typedef __builtin_va_list va_list;

extern void *dlsym(void *handle, const char *name);
extern char *getenv(const char *name);

#define RTLD_NEXT ((void *)-1L)
#define O_CREAT_VALUE 00000100
#define O_TMPFILE_VALUE 020000000

typedef int (*open_fn)(const char *path, int flags, ...);
typedef int (*openat_fn)(int directory, const char *path, int flags, ...);

static int path_equals(const char *left, const char *right) {
    if (left == 0 || right == 0) {
        return 0;
    }
    while (*left != '\0' && *right != '\0' && *left == *right) {
        left++;
        right++;
    }
    return *left == *right;
}

static const char *redirect_resolver_path(const char *path) {
    if (!path_equals(path, "/etc/resolv.conf")) {
        return path;
    }
    const char *replacement = getenv("OMARCHY_RESOLV_CONF");
    return replacement != 0 && replacement[0] != '\0' ? replacement : path;
}

static mode_t optional_mode(int flags, va_list arguments) {
    if ((flags & (O_CREAT_VALUE | O_TMPFILE_VALUE)) == 0) {
        return 0;
    }
    return (mode_t)__builtin_va_arg(arguments, int);
}

int open(const char *path, int flags, ...) {
    static open_fn next_open;
    if (next_open == 0) {
        next_open = (open_fn)dlsym(RTLD_NEXT, "open");
    }
    va_list arguments;
    __builtin_va_start(arguments, flags);
    mode_t mode = optional_mode(flags, arguments);
    __builtin_va_end(arguments);
    return next_open(redirect_resolver_path(path), flags, mode);
}

int open64(const char *path, int flags, ...) {
    static open_fn next_open64;
    if (next_open64 == 0) {
        next_open64 = (open_fn)dlsym(RTLD_NEXT, "open64");
        if (next_open64 == 0) {
            next_open64 = (open_fn)dlsym(RTLD_NEXT, "open");
        }
    }
    va_list arguments;
    __builtin_va_start(arguments, flags);
    mode_t mode = optional_mode(flags, arguments);
    __builtin_va_end(arguments);
    return next_open64(redirect_resolver_path(path), flags, mode);
}

int openat(int directory, const char *path, int flags, ...) {
    static openat_fn next_openat;
    if (next_openat == 0) {
        next_openat = (openat_fn)dlsym(RTLD_NEXT, "openat");
    }
    va_list arguments;
    __builtin_va_start(arguments, flags);
    mode_t mode = optional_mode(flags, arguments);
    __builtin_va_end(arguments);
    return next_openat(directory, redirect_resolver_path(path), flags, mode);
}

int openat64(int directory, const char *path, int flags, ...) {
    static openat_fn next_openat64;
    if (next_openat64 == 0) {
        next_openat64 = (openat_fn)dlsym(RTLD_NEXT, "openat64");
        if (next_openat64 == 0) {
            next_openat64 = (openat_fn)dlsym(RTLD_NEXT, "openat");
        }
    }
    va_list arguments;
    __builtin_va_start(arguments, flags);
    mode_t mode = optional_mode(flags, arguments);
    __builtin_va_end(arguments);
    return next_openat64(directory, redirect_resolver_path(path), flags, mode);
}

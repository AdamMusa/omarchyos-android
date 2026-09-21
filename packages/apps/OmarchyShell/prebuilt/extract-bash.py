#!/usr/bin/env python3
"""Install the Shell APK's ARM64 bash as a real system executable."""
from pathlib import Path
import sys
import zipfile


def extract(apk, destination):
    with zipfile.ZipFile(apk) as archive:
        binary = archive.read("lib/arm64-v8a/libbash.so")
    if binary[:4] != b"\x7fELF" or binary[4:6] != b"\x02\x01" or binary[18:20] != b"\xb7\x00":
        raise ValueError("The APK must contain an ARM64 ELF bash executable")
    output = Path(destination)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_bytes(binary)
    output.chmod(0o755)


if __name__ == "__main__":
    extract(*sys.argv[1:])

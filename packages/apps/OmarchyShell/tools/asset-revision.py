#!/usr/bin/env python3
"""Fingerprint staged assets so native-only APK updates do not unpack them again."""
import hashlib
from pathlib import Path
import sys


def revision(tree):
    digest = hashlib.sha256()
    for path in sorted(tree.rglob('*')):
        if path.is_symlink() or not path.is_file() or path.name == 'asset-revision.txt':
            continue
        digest.update(path.relative_to(tree).as_posix().encode() + b'\0')
        with path.open('rb') as source:
            while chunk := source.read(1024 * 1024):
                digest.update(chunk)
        digest.update(b'\0')
    return digest.hexdigest()


if __name__ == '__main__':
    tree = Path(sys.argv[1])
    (tree / 'asset-revision.txt').write_text(revision(tree) + '\n')

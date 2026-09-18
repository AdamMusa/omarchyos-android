#!/usr/bin/env python3
"""Apply Codex '*** Begin Patch' files (Update File hunks) relative to a root directory."""
import re, sys, pathlib

def find_seq(lines, seq, start):
    n = len(seq)
    if n == 0:
        return start
    for norm in (lambda s: s, lambda s: s.rstrip(), lambda s: s.strip()):
        want = [norm(s) for s in seq]
        for i in range(start, len(lines) - n + 1):
            if [norm(s) for s in lines[i:i + n]] == want:
                return i
    return -1

def apply_file(root, rel, body):
    path = root / rel
    lines = path.read_text().split("\n")
    pos = 0
    hunks, cur, anchor = [], None, None
    for b in body:
        if b.startswith("@@"):
            cur = {"anchor": b[2:].strip(), "lines": []}
            hunks.append(cur)
        elif b.startswith("*** "):
            continue
        else:
            if cur is None:
                cur = {"anchor": "", "lines": []}
                hunks.append(cur)
            cur["lines"].append(b if b[:1] in " +-" else " " + b)
    for h in hunks:
        if h["anchor"]:
            a = find_seq(lines, [h["anchor"]], pos)
            if a < 0:
                raise SystemExit(f"{rel}: anchor not found: {h['anchor']!r}")
            pos = a + 1
        old = [l[1:] for l in h["lines"] if l[:1] in " -"]
        new = [l[1:] for l in h["lines"] if l[:1] in " +"]
        while old and old[-1] == "" and new and new[-1] == "":
            old.pop(); new.pop()
        i = find_seq(lines, old, pos)
        if i < 0:
            raise SystemExit(f"{rel}: hunk context not found: " + " | ".join(x.strip() for x in old[:3]))
        lines[i:i + len(old)] = new
        pos = i + len(new)
    path.write_text("\n".join(lines))

def main():
    root = pathlib.Path(sys.argv[1])
    for pf in sys.argv[2:]:
        text = pathlib.Path(pf).read_text()
        for m in re.finditer(r"\*\*\* Begin Patch\n(.*?)\*\*\* End Patch", text, re.S):
            L = m.group(1).split("\n")
            hdr = re.match(r"\*\*\* Update File: (.*)", L[0])
            if not hdr:
                raise SystemExit(f"unsupported patch header in {pf}: {L[0]}")
            body = L[1:]
            if body and body[-1] == "":
                body = body[:-1]
            try:
                apply_file(root, hdr.group(1).strip(), body)
                print(f"applied {hdr.group(1).strip()}")
            except SystemExit as e:
                # Codex retried some patches after failures; a block whose context never
                # existed is skipped exactly like the original failed apply_patch call.
                print(f"SKIPPED block in {hdr.group(1).strip()}: {str(e).splitlines()[0]}")

main()

#!/usr/bin/env bash
# Run inside the builder container. Applies the sandboxed Google Play
# (GrapheneOS gmscompat) patch series to the AOSP projects in $WS and copies
# the GmsCompat app and its config into place. Safe to re-run: a patch whose
# subject is already in a project's history is skipped.
set -Eeuo pipefail
SRC=${SRC:-/omarchy-src}
WS=${WS:-/workspace}
P="$SRC/patches/sandboxed-play"
export GIT_COMMITTER_NAME="OmarchyOS" GIT_COMMITTER_EMAIL="build@omarchy.os"

rsync -a --delete --exclude .git "$SRC/packages/apps/GmsCompat/" "$WS/packages/apps/GmsCompat/"
rsync -a --delete --exclude .git "$SRC/external/GmsCompatConfig/" "$WS/external/GmsCompatConfig/"

fail=0
while IFS= read -r dir; do
  proj="${dir#"$P"/}"
  [ -d "$WS/$proj/.git" ] || [ -f "$WS/$proj/.git" ] || { echo "MISSING PROJECT $proj"; fail=1; continue; }
  # Omarchy's own edits to fork-owned projects may be uncommitted working-tree
  # changes (recovery/restore-into-aosp.sh applies them that way). Record them
  # as a commit first so `git am -3` can merge around them.
  if [ -n "$(git -C "$WS/$proj" status --porcelain)" ]; then
    git -C "$WS/$proj" add -A
    git -C "$WS/$proj" -c user.name=OmarchyOS -c user.email=build@omarchy.os \
      commit -q -m "OmarchyOS: record working-tree changes before sandboxed Play"
    echo "commit $proj: recorded existing Omarchy changes"
  fi
  for patch in "$dir"/*.patch; do
    subj=$(sed -n 's/^Subject: \[PATCH[^]]*\] //p' "$patch" | head -1)
    if git -C "$WS/$proj" log --format=%s -n 400 | grep -qxF "$subj"; then
      echo "skip   $proj: $subj"; continue
    fi
    if git -C "$WS/$proj" -c user.name=OmarchyOS -c user.email=build@omarchy.os am -3 --keep-cr "$patch" >/tmp/am.log 2>&1; then
      echo "apply  $proj: $subj"
    else
      echo "FAILED $proj: $subj"; sed -n '1,30p' /tmp/am.log
      git -C "$WS/$proj" am --abort || true
      fail=1; break
    fi
  done
done < <(find "$P" -name '*.patch' -printf '%h\n' | sort -u)
exit $fail

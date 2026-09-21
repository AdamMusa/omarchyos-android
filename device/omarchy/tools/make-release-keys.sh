#!/usr/bin/env bash
# Generate OmarchyOS release signing keys. Run once, inside the builder, from
# the AOSP root. Keys are written OUTSIDE the source tree (default
# ~/.omarchy-keys) and must never be committed. Back them up offline: losing
# them means installed devices can no longer receive updates.
set -Eeuo pipefail
KEYS=${OMARCHY_KEYS:-$HOME/.omarchy-keys}
SUBJECT=${OMARCHY_KEY_SUBJECT:-'/C=US/ST=Illinois/L=Chicago/O=OmarchyOS/OU=OmarchyOS/CN=OmarchyOS'}
[ -f build/make/envsetup.sh ] || { echo "run from the AOSP root" >&2; exit 1; }
mkdir -p "$KEYS" && chmod 700 "$KEYS"
cd "$KEYS"
# make_key prompts for a password; an empty one keeps CI signing unattended.
for k in releasekey platform shared media networkstack sdk_sandbox bluetooth nfc gmscompat_lib; do
  [ -f "$k.pk8" ] && { echo "keep $k"; continue; }
  printf '\n' | "$OLDPWD/development/tools/make_key" "$k" "$SUBJECT" || true
done
[ -f avb.pem ] || openssl genrsa -out avb.pem 4096
[ -f avb_pkmd.bin ] || "$OLDPWD/external/avb/avbtool.py" extract_public_key --key avb.pem --output avb_pkmd.bin
[ -f apex.pem ] || openssl genrsa -out apex.pem 4096
[ -f apex.x509.pem ] || openssl req -new -x509 -sha256 -key apex.pem -out apex.x509.pem -days 10000 -subj "$SUBJECT"
[ -f apex.pk8 ] || openssl pkcs8 -in apex.pem -topk8 -outform DER -out apex.pk8 -nocrypt
chmod 600 "$KEYS"/*
echo "Keys in $KEYS"

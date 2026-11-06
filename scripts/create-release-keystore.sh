#!/usr/bin/env bash
# One-time: create the Nexus Music release key and point local.properties at it.
#
# The key is the app's identity. Every future release must be signed with it, or Android refuses
# to install the update over the existing app. Back up BOTH the .jks file and local.properties'
# nexus.* lines somewhere safe (password manager + offline copy). Neither is ever committed:
# the keystore lives outside the repository and local.properties is git-ignored.
#
# Usage: scripts/create-release-keystore.sh [keystore-path]
#   default path: ~/.nexus-music/nexus-release.jks
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
KS="${1:-$HOME/.nexus-music/nexus-release.jks}"
ALIAS="nexus"

if [ -e "$KS" ]; then
  echo "Refusing to overwrite an existing keystore: $KS" >&2
  exit 1
fi
if grep -qE '^[[:space:]]*nexus\.keystore\.' "$ROOT/local.properties" 2>/dev/null; then
  echo "local.properties already has nexus.keystore.* settings; not touching them." >&2
  exit 1
fi
command -v keytool >/dev/null || { echo "keytool not found (it ships with the JDK)" >&2; exit 1; }

mkdir -p "$(dirname "$KS")"
# 32 random URL-safe characters. keytool's PKCS12 uses one password for store and key.
PASS="$(head -c 48 /dev/urandom | base64 | tr -d '/+=\n' | cut -c1-32)"
export NEXUS_GEN_PASS="$PASS"

keytool -genkeypair -v \
  -keystore "$KS" -storetype PKCS12 \
  -alias "$ALIAS" -keyalg RSA -keysize 4096 -validity 36500 \
  -dname "CN=Nexus Music, O=Nexus Music" \
  -storepass:env NEXUS_GEN_PASS -keypass:env NEXUS_GEN_PASS >/dev/null

KS_FOR_PROPS="$KS"
command -v cygpath >/dev/null && KS_FOR_PROPS="$(cygpath -m "$KS")"
{
  [ -s "$ROOT/local.properties" ] && [ -n "$(tail -c1 "$ROOT/local.properties")" ] && echo
  echo "# Nexus Music release signing (scripts/build-release.sh). Back these up with the keystore."
  echo "nexus.keystore.file=$KS_FOR_PROPS"
  echo "nexus.keystore.password=$PASS"
  echo "nexus.key.alias=$ALIAS"
  echo "nexus.key.password=$PASS"
} >> "$ROOT/local.properties"

echo "Created $KS (alias '$ALIAS') and added nexus.* signing settings to local.properties."
echo "Certificate:"
keytool -list -v -keystore "$KS" -storepass:env NEXUS_GEN_PASS | grep -E "SHA256:|Owner:" | head -n 2
echo
echo "BACK UP the keystore file and the nexus.* lines in local.properties now."

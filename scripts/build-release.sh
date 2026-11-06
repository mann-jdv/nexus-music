#!/usr/bin/env bash
# Nexus Music release build: assemble, zipalign, sign, verify, and copy to dist/ as
# NexusMusic-<version>[-full][-<abi>].apk.
#
# Same steps as upstream's build_and_sign_apk.sh (kept untouched so syncs never conflict on it),
# with Nexus differences:
#   * signing material comes from the environment or local.properties, never from the repo;
#   * --foss really builds FOSS (-PisFullBuild=false); upstream's flag only renamed the file;
#   * passwords reach apksigner through env: references, never on the command line;
#   * works with Windows build-tools (apksigner.bat, zipalign.exe) as well as Linux/macOS.
#
# Signing settings (environment wins over local.properties):
#   NEXUS_KEYSTORE_FILE      / nexus.keystore.file       path to the .jks (absolute, or relative to the repo)
#   NEXUS_KEYSTORE_PASSWORD  / nexus.keystore.password
#   NEXUS_KEY_ALIAS          / nexus.key.alias
#   NEXUS_KEY_PASSWORD       / nexus.key.password        (defaults to the keystore password)
# Create a key once with scripts/create-release-keystore.sh.
#
# Usage: scripts/build-release.sh [--foss | --full | --all]
#   --foss  (default) FOSS build, no Sentry/GMS/Last.fm
#   --full  Full build; needs SENTRY_DSN, LASTFM_API_KEY, LASTFM_SECRET, DISCORD_CLIENT_ID in local.properties
#   --all   FOSS, plus Full when those keys are present
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

VARIANTS=(foss)
case "${1:-}" in
  ""|--foss) VARIANTS=(foss) ;;
  --full) VARIANTS=(full) ;;
  --all) VARIANTS=(foss full-if-keys) ;;
  -h|--help) sed -n '2,24p' "$0"; exit 0 ;;
  *) echo "Unknown option: $1 (try --help)" >&2; exit 2 ;;
esac

# --- local.properties reader (plain key=value; no Java escapes needed for our keys) -------------
prop() {
  [ -f local.properties ] || return 0
  grep -E "^[[:space:]]*$1[[:space:]]*=" local.properties | tail -n 1 | sed -E 's/^[^=]*=[[:space:]]*//' | tr -d '\r'
}

export NEXUS_KEYSTORE_FILE="${NEXUS_KEYSTORE_FILE:-$(prop nexus.keystore.file)}"
export NEXUS_KEYSTORE_PASSWORD="${NEXUS_KEYSTORE_PASSWORD:-$(prop nexus.keystore.password)}"
export NEXUS_KEY_ALIAS="${NEXUS_KEY_ALIAS:-$(prop nexus.key.alias)}"
export NEXUS_KEY_PASSWORD="${NEXUS_KEY_PASSWORD:-$(prop nexus.key.password)}"
[ -n "$NEXUS_KEY_PASSWORD" ] || export NEXUS_KEY_PASSWORD="$NEXUS_KEYSTORE_PASSWORD"

missing=()
[ -n "$NEXUS_KEYSTORE_FILE" ] || missing+=(NEXUS_KEYSTORE_FILE)
[ -n "$NEXUS_KEYSTORE_PASSWORD" ] || missing+=(NEXUS_KEYSTORE_PASSWORD)
[ -n "$NEXUS_KEY_ALIAS" ] || missing+=(NEXUS_KEY_ALIAS)
if [ ${#missing[@]} -gt 0 ]; then
  echo "Signing is not configured; missing: ${missing[*]}" >&2
  echo "Set them as environment variables or nexus.* keys in local.properties (see --help)," >&2
  echo "or create a key with scripts/create-release-keystore.sh." >&2
  exit 1
fi
case "$NEXUS_KEYSTORE_FILE" in /*|[A-Za-z]:*) ;; *) NEXUS_KEYSTORE_FILE="$ROOT/$NEXUS_KEYSTORE_FILE" ;; esac
[ -f "$NEXUS_KEYSTORE_FILE" ] || { echo "Keystore not found: $NEXUS_KEYSTORE_FILE" >&2; exit 1; }

# --- tools --------------------------------------------------------------------------------------
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
[ -n "$SDK" ] || SDK="$(prop sdk.dir)"
[ -n "$SDK" ] || { echo "Set ANDROID_HOME (or sdk.dir in local.properties)" >&2; exit 1; }
command -v cygpath >/dev/null && SDK="$(cygpath -u "$SDK")"
BT="$SDK/build-tools/$(ls "$SDK/build-tools" | sort -V | tail -n 1)"
APKSIGNER="$BT/apksigner"; [ -f "$APKSIGNER" ] || APKSIGNER="$BT/apksigner.bat"
ZIPALIGN="$BT/zipalign"; [ -f "$ZIPALIGN" ] || ZIPALIGN="$BT/zipalign.exe"
for t in "$APKSIGNER" "$ZIPALIGN"; do [ -f "$t" ] || { echo "Not found: $t" >&2; exit 1; }; done
GRADLEW=./gradlew

# Lint's release check crashes inside its usage-analytics logger (NoClassDefFoundError:
# com/google/protobuf/ProtocolMessageEnum) whenever the machine has opted in to Android Studio
# usage statistics (~/.android/analytics.settings). Give the build its own Android user home so
# the check runs, without touching the user's setting. Override with NEXUS_ANDROID_USER_HOME.
export ANDROID_USER_HOME="${NEXUS_ANDROID_USER_HOME:-$ROOT/build/nexus-android-user-home}"
mkdir -p "$ANDROID_USER_HOME"
command -v cygpath >/dev/null && export ANDROID_USER_HOME="$(cygpath -w "$ANDROID_USER_HOME")"

# exportLibraryDefinitions rewrites this tracked file for the variant being built (the FOSS list
# has no Cast/GMS). The APK must carry that list, but the repo keeps upstream's copy so syncs do not
# conflict on it: restore it on exit if it was clean when we started.
LIBS_JSON="composeApp/src/commonMain/composeResources/files/aboutlibraries.json"
if git diff --quiet -- "$LIBS_JSON" 2>/dev/null; then
  trap 'git checkout -q -- "$LIBS_JSON" 2>/dev/null || true' EXIT
fi

VERSION="$(grep -E '^nexus.version=' nexus.properties | cut -d= -f2 | tr -d '[:space:]')-nexus"
DIST="$ROOT/dist"
mkdir -p "$DIST"

has_full_keys() {
  local k
  for k in SENTRY_DSN LASTFM_API_KEY LASTFM_SECRET DISCORD_CLIENT_ID; do
    [ -n "$(prop "$k")" ] || return 1
  done
}

build_variant() {
  local variant="$1" full suffix
  case "$variant" in
    foss) full=false; suffix="" ;;
    full) full=true; suffix="-full"
      has_full_keys || { echo "Full build needs SENTRY_DSN, LASTFM_API_KEY, LASTFM_SECRET and DISCORD_CLIENT_ID in local.properties" >&2; exit 1; } ;;
    full-if-keys)
      if has_full_keys; then build_variant full; else echo "== Skipping Full: local.properties has no Full-build keys"; fi
      return ;;
  esac

  echo "== Building $variant release $VERSION"
  local out="androidApp/build/outputs/apk/release"
  rm -rf "$out"
  # Same order as upstream CI: the open-source licenses screen reads these definitions.
  "$GRADLEW" -PisFullBuild=$full exportLibraryDefinitions --console=plain
  "$GRADLEW" -PisFullBuild=$full :androidApp:assembleRelease --console=plain

  local apks; apks=$(find "$out" -name "*.apk" | sort)
  [ -n "$apks" ] || { echo "No APKs in $out" >&2; exit 1; }
  local apk abi name aligned signed
  for apk in $apks; do
    # androidApp-arm64-v8a-release-unsigned.apk -> arm64-v8a ; androidApp-universal-... -> universal
    abi="$(basename "$apk" .apk)"; abi="${abi#androidApp-}"; abi="${abi%-release-unsigned}"; abi="${abi%-release}"
    if [ "$abi" = "universal" ]; then name="NexusMusic-$VERSION$suffix.apk"; else name="NexusMusic-$VERSION$suffix-$abi.apk"; fi
    aligned="$out/aligned-$name"
    signed="$DIST/$name"
    "$ZIPALIGN" -f -p 4 "$apk" "$aligned"
    "$APKSIGNER" sign --alignment-preserved \
      --ks "$NEXUS_KEYSTORE_FILE" --ks-key-alias "$NEXUS_KEY_ALIAS" \
      --ks-pass env:NEXUS_KEYSTORE_PASSWORD --key-pass env:NEXUS_KEY_PASSWORD \
      --out "$signed" "$aligned"
    "$APKSIGNER" verify "$signed"
    rm -f "$aligned" "$signed.idsig"
    echo "   signed: dist/$name"
  done
  "$APKSIGNER" verify --print-certs "$DIST/NexusMusic-$VERSION$suffix.apk" | grep -E "SHA-256|DN:" | head -n 2
}

for v in "${VARIANTS[@]}"; do build_variant "$v"; done
echo "== Done. APKs in dist/"

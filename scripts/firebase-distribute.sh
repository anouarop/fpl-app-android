#!/usr/bin/env bash
# Distribute a build to Firebase App Distribution testers.
# Requires local.properties entries: firebaseAppId, firebaseServiceCredentials,
# firebaseTesters (see README / Firebase setup). Credentials are NOT committed.
#
# Usage:
#   ./scripts/firebase-distribute.sh        # upload debug APK
#   ./scripts/firebase-distribute.sh release # upload release AAB
#
# Alternative (Firebase CLI) if installed + `firebase login`:
#   firebase appdistribution:distribute app/build/outputs/apk/debug/app-debug.apk \
#     --app "$FIREBASE_APP_ID" --testers "$FIREBASE_TESTERS" --release-notes "..."

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(dirname "$SCRIPT_DIR")"

cd "$ROOT"
# shellcheck disable=SC1091
[ -f env.sh ] && source env.sh

VARIANT="${1:-debug}"
case "$VARIANT" in
  debug)   ./gradlew appDistributionUploadDebug ;;
  release) ./gradlew appDistributionUploadRelease ;;
  *) echo "Unknown variant: $VARIANT (use 'debug' or 'release')" >&2; exit 1 ;;
esac

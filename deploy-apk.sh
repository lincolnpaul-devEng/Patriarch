#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# Automated Deployment Script for Campus Network
# Targets: Firebase App Distribution & Firebase Public Hosting (campus.apk)
# ==============================================================================

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

TARGET="${1:-hosting}"
TARGET_LOWER="$(echo "$TARGET" | tr '[:upper:]' '[:lower:]')"
CUSTOM_NOTES="${2:-}"

echo "======================================================================"
echo "🚀 Campus Network: Deploying target '$TARGET'..."
echo "======================================================================"

deploy_hosting() {
    echo "📦 [1/3] Building Release APK..."
    ./gradlew assembleRelease --no-daemon

    RELEASE_APK="$PROJECT_DIR/app/build/outputs/apk/release/app-release.apk"
    if [[ ! -f "$RELEASE_APK" ]]; then
        RELEASE_APK="$PROJECT_DIR/app/build/outputs/apk/release/app-release-unsigned.apk"
    fi

    echo "🔐 [2/3] Preparing public/campus.bin from: $RELEASE_APK"
    mkdir -p "$PROJECT_DIR/public"

    # If unsigned, sign with release keystore
    if [[ "$RELEASE_APK" == *"-unsigned.apk" ]]; then
        APKSIGNER="/home/lincoln/Android/Sdk/build-tools/35.0.0/apksigner"
        "$APKSIGNER" sign --ks "$PROJECT_DIR/app/release-key.jks" \
            --ks-pass pass:campusnetwork \
            --ks-key-alias campus \
            --key-pass pass:campusnetwork \
            --out "$PROJECT_DIR/public/campus.bin" \
            "$RELEASE_APK"
    else
        cp "$RELEASE_APK" "$PROJECT_DIR/public/campus.bin"
    fi

    echo "🌐 [3/3] Deploying to Firebase Hosting (public download URL)..."
    firebase deploy --only hosting

    echo ""
    echo "======================================================================"
    echo "🎉 Live Deployment URLs:"
    echo "👉 Landing Page + QR Code : https://modified-silo-476908-b3.web.app"
    echo "👉 Direct APK Download     : https://modified-silo-476908-b3.web.app/campus.apk"
    echo "======================================================================"
}

deploy_app_distribution() {
    VARIANT="${1:-release}"
    case "$VARIANT" in
        release)
            BUILD_TASK="assembleRelease"
            UPLOAD_TASK="appDistributionUploadRelease"
            ;;
        debug)
            BUILD_TASK="assembleDebug"
            UPLOAD_TASK="appDistributionUploadDebug"
            ;;
        *)
            echo "❌ Unknown variant: $VARIANT. Use 'release' or 'debug'."
            exit 1
            ;;
    esac

    # Ensure release-notes.txt exists
    RELEASE_NOTES_FILE="$PROJECT_DIR/release-notes.txt"
    if [[ -n "$CUSTOM_NOTES" ]]; then
        echo "📝 Updating release-notes.txt with custom notes..."
        echo "$CUSTOM_NOTES" > "$RELEASE_NOTES_FILE"
    elif [[ ! -f "$RELEASE_NOTES_FILE" ]]; then
        echo "📝 Generating default release-notes.txt..."
        cat <<EOF > "$RELEASE_NOTES_FILE"
- Campus Network v1.0.0 ($VARIANT build)
- Build timestamp: $(date -u +"%Y-%m-%d %H:%M:%S UTC")
- Automated upload via Firebase App Distribution
EOF
    fi

    # Check Authentication
    if [[ -n "${GOOGLE_APPLICATION_CREDENTIALS:-}" ]]; then
        echo "🔑 Using Service Account: $GOOGLE_APPLICATION_CREDENTIALS"
    elif [[ -n "${FIREBASE_TOKEN:-}" ]]; then
        echo "🔑 Using CI Token: FIREBASE_TOKEN"
    else
        echo "ℹ️  Using Firebase CLI session credentials."
    fi

    echo "📦 Running: ./gradlew $BUILD_TASK $UPLOAD_TASK"
    ./gradlew "$BUILD_TASK" "$UPLOAD_TASK" --no-daemon

    echo "✅ App Distribution upload finished successfully!"
}

case "$TARGET_LOWER" in
    hosting)
        deploy_hosting
        ;;
    distribution|appdist|release|debug)
        deploy_app_distribution "$TARGET_LOWER"
        ;;
    all)
        deploy_hosting
        deploy_app_distribution "release"
        ;;
    *)
        echo "Usage: $0 [hosting|distribution|release|debug|all] [\"custom release notes\"]"
        exit 1
        ;;
esac

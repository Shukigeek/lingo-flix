#!/usr/bin/env bash
# LingoFlix Automated Release Script for Linux / WSL

set -e

# 0. Check for Keystore
KEYSTORE_PATH="app/release.keystore"
if [ ! -f "$KEYSTORE_PATH" ]; then
    echo "ERROR: release.keystore not found at $KEYSTORE_PATH!"
    echo "Please make sure the keystore file is inside the 'app' folder."
    exit 1
fi

# 1. Get current version info
GRADLE_FILE="app/build.gradle.kts"
CURRENT_VERSION=$(grep -oP 'versionName = "\K[^"]+' "$GRADLE_FILE" || true)
CURRENT_CODE=$(grep -oP 'versionCode = \K\d+' "$GRADLE_FILE" || true)

if [ -z "$CURRENT_VERSION" ] || [ -z "$CURRENT_CODE" ]; then
    echo "ERROR: Could not find version info in $GRADLE_FILE"
    exit 1
fi

echo "Current Version: $CURRENT_VERSION (Code: $CURRENT_CODE)"
read -r -p "Enter new version name (leave empty to keep $CURRENT_VERSION): " NEW_VERSION
if [ -z "$NEW_VERSION" ]; then
    NEW_VERSION="$CURRENT_VERSION"
fi
NEW_CODE=$((CURRENT_CODE + 1))

echo "Target Version: $NEW_VERSION (Code: $NEW_CODE)"

# 3. Update build.gradle.kts
sed -i -E "s/versionCode = [0-9]+/versionCode = $NEW_CODE/" "$GRADLE_FILE"
sed -i -E "s/versionName = \"[^\"]+\"/versionName = \"$NEW_VERSION\"/" "$GRADLE_FILE"

# 4. Build APK
echo "Building Signed APK..."
chmod +x ./gradlew
./gradlew assembleRelease

APK_PATH="app/build/outputs/apk/release/app-release.apk"
if [ ! -f "$APK_PATH" ]; then
    echo "ERROR: Could not find APK at $APK_PATH"
    exit 1
fi

# 5. GitHub Release
echo "Creating GitHub Release..."
gh release delete "v$NEW_VERSION" --yes 2>/dev/null || true
git push --delete origin "v$NEW_VERSION" 2>/dev/null || true

gh release create "v$NEW_VERSION" "$APK_PATH" --title "Release v$NEW_VERSION" --notes "Automated release of LingoFlix v$NEW_VERSION"

# 6. Update HTML files if present
echo "Updating landing-page.html..."
NEW_DOWNLOAD_URL="https://github.com/Shukigeek/lingo-flix/releases/download/v$NEW_VERSION/app-release.apk"
for HTML_FILE in landing-page.html index.html; do
    if [ -f "$HTML_FILE" ]; then
        sed -i -E "s|https://github.com/Shukigeek/lingo-flix/releases/download/[^/]+/app-release\.apk|$NEW_DOWNLOAD_URL|g" "$HTML_FILE"
    fi
done

# 7. Git Commit & Push
echo "Committing and Pushing to Git..."
git add .
git commit -m "Release v$NEW_VERSION (Code: $NEW_CODE)"
git push

echo "Successfully released v$NEW_VERSION!"

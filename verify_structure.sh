#!/bin/bash

echo "=== Verifying Photo Compressor Project Structure ==="
echo ""

echo "✓ Checking root files..."
[ -f "settings.gradle.kts" ] && echo "  ✓ settings.gradle.kts" || echo "  ✗ settings.gradle.kts MISSING"
[ -f "build.gradle.kts" ] && echo "  ✓ build.gradle.kts" || echo "  ✗ build.gradle.kts MISSING"
[ -f "gradlew" ] && echo "  ✓ gradlew" || echo "  ✗ gradlew MISSING"
[ -f ".gitignore" ] && echo "  ✓ .gitignore" || echo "  ✗ .gitignore MISSING"
[ -f "README.md" ] && echo "  ✓ README.md" || echo "  ✗ README.md MISSING"

echo ""
echo "✓ Checking app module files..."
[ -f "app/build.gradle.kts" ] && echo "  ✓ app/build.gradle.kts" || echo "  ✗ app/build.gradle.kts MISSING"
[ -f "app/src/main/AndroidManifest.xml" ] && echo "  ✓ AndroidManifest.xml" || echo "  ✗ AndroidManifest.xml MISSING"
[ -f "app/proguard-rules.pro" ] && echo "  ✓ proguard-rules.pro" || echo "  ✗ proguard-rules.pro MISSING"

echo ""
echo "✓ Checking Kotlin source files..."
[ -f "app/src/main/java/com/photocompressor/kbsize/App.kt" ] && echo "  ✓ App.kt" || echo "  ✗ App.kt MISSING"
[ -f "app/src/main/java/com/photocompressor/kbsize/MainActivity.kt" ] && echo "  ✓ MainActivity.kt" || echo "  ✗ MainActivity.kt MISSING"
[ -f "app/src/main/java/com/photocompressor/kbsize/BillingManager.kt" ] && echo "  ✓ BillingManager.kt" || echo "  ✗ BillingManager.kt MISSING"
[ -f "app/src/main/java/com/photocompressor/kbsize/AdsManager.kt" ] && echo "  ✓ AdsManager.kt" || echo "  ✗ AdsManager.kt MISSING"
[ -f "app/src/main/java/com/photocompressor/kbsize/ImageCompressor.kt" ] && echo "  ✓ ImageCompressor.kt" || echo "  ✗ ImageCompressor.kt MISSING"
[ -f "app/src/main/java/com/photocompressor/kbsize/MediaStoreSaver.kt" ] && echo "  ✓ MediaStoreSaver.kt" || echo "  ✗ MediaStoreSaver.kt MISSING"

echo ""
echo "✓ Checking theme files..."
[ -f "app/src/main/java/com/photocompressor/kbsize/ui/theme/Color.kt" ] && echo "  ✓ Color.kt" || echo "  ✗ Color.kt MISSING"
[ -f "app/src/main/java/com/photocompressor/kbsize/ui/theme/Type.kt" ] && echo "  ✓ Type.kt" || echo "  ✗ Type.kt MISSING"
[ -f "app/src/main/java/com/photocompressor/kbsize/ui/theme/Theme.kt" ] && echo "  ✓ Theme.kt" || echo "  ✗ Theme.kt MISSING"

echo ""
echo "✓ Checking resource files..."
[ -f "app/src/main/res/drawable/ic_launcher.xml" ] && echo "  ✓ ic_launcher.xml" || echo "  ✗ ic_launcher.xml MISSING"
[ -f "app/src/main/res/drawable/ic_launcher_foreground.xml" ] && echo "  ✓ ic_launcher_foreground.xml" || echo "  ✗ ic_launcher_foreground.xml MISSING"
[ -f "app/src/main/res/values/colors.xml" ] && echo "  ✓ colors.xml" || echo "  ✗ colors.xml MISSING"

echo ""
echo "=== Line counts ==="
wc -l app/src/main/java/com/photocompressor/kbsize/*.kt | tail -1

echo ""
echo "=== Verification Complete ==="

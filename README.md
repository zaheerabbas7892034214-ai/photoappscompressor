# Photo Compressor – KB Size

A complete Android app built with Kotlin and Jetpack Compose (Material3) that compresses images to specific target sizes using a binary search algorithm.

## Features

- **Image Selection & Preview**: Pick images using modern ActivityResultContracts
- **Target Size Selection**: Choose from preset sizes (50KB, 100KB, 200KB, 500KB, 1MB) or enter custom size
- **Smart Compression**: Binary search algorithm for JPEG quality optimization
- **Dimension Scaling**: Automatic downscaling if target size cannot be achieved with quality adjustment alone
- **Results Display**: View compression statistics including percentage reduction
- **Save & Share**: Save to MediaStore and share compressed images
- **Pro Version**: Remove ads via Google Play Billing v6+
- **AdMob Integration**: Banner and interstitial ads for free users

## Technical Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material3
- **Min SDK**: 24
- **Target SDK**: 34
- **Billing**: Google Play Billing Library 6.2.0
- **Ads**: Google AdMob 22.6.0
- **Image Loading**: Coil 2.5.0
- **Async**: Kotlin Coroutines 1.7.3

## Project Structure

```
app/src/main/java/com/photocompressor/kbsize/
├── App.kt                  - Application class with AdMob initialization
├── MainActivity.kt         - Main UI with Compose
├── BillingManager.kt       - Google Play Billing v6+ implementation
├── AdsManager.kt           - Banner and interstitial ad handling
├── ImageCompressor.kt      - Binary search compression algorithm
├── MediaStoreSaver.kt      - Save images to MediaStore
└── ui/theme/              - Material3 theme files
    ├── Color.kt
    ├── Type.kt
    └── Theme.kt
```

## Setup Instructions

### 1. AdMob Setup

Replace test Ad IDs with your real AdMob IDs:

**In `app/src/main/AndroidManifest.xml`:**
```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="YOUR_ADMOB_APP_ID"/>
```

**In `app/src/main/java/com/photocompressor/kbsize/AdsManager.kt`:**
- Banner Ad Unit ID (line ~23): Replace `ca-app-pub-3940256099942544/6300978111`
- Interstitial Ad Unit ID (line ~44): Replace `ca-app-pub-3940256099942544/1033173712`

**How to get AdMob IDs:**
1. Go to [AdMob Console](https://apps.admob.com/)
2. Create an app
3. Create ad units for Banner and Interstitial
4. Copy the App ID and Ad Unit IDs

### 2. Google Play Billing Setup

**Create Product in Google Play Console:**
1. Go to [Google Play Console](https://play.google.com/console)
2. Select your app
3. Navigate to: Monetize → In-app products
4. Create new product:
   - **Product ID**: `pro_upgrade` (must match exactly)
   - **Type**: One-time purchase (non-consumable)
   - **Name**: "Pro Upgrade" or your choice
   - **Description**: "Remove ads and support development"
   - **Price**: Set your desired price (e.g., $4.99)
5. Activate the product

**Testing Billing:**
1. Add license testers in Google Play Console:
   - Settings → License testing
   - Add your Gmail accounts
2. Create an internal testing track:
   - Release → Testing → Internal testing
   - Upload signed APK/AAB
   - Add testers
3. Install app from Play Store (internal testing)
4. Real billing cannot be tested in debug builds from Android Studio

**Note**: The app will show errors about product not found until you create the product in Play Console and publish to testing track.

### 3. Permissions

The app automatically requests:
- **Android 13+**: `READ_MEDIA_IMAGES`
- **Android 12 and below**: `READ_EXTERNAL_STORAGE`

No additional setup needed. Users will be prompted when selecting images.

### 4. Building the App

```bash
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Build release AAB (for Play Store)
./gradlew bundleRelease

# Install on device
./gradlew installDebug
```

### 5. Signing Configuration (for Release)

Create `keystore.properties` in root directory:
```properties
storePassword=YOUR_KEYSTORE_PASSWORD
keyPassword=YOUR_KEY_PASSWORD
keyAlias=YOUR_KEY_ALIAS
storeFile=YOUR_KEYSTORE_FILE
```

Then update `app/build.gradle.kts` to include signing config (not included in this template).

## Compression Algorithm

The app uses a **binary search algorithm** to find the optimal JPEG quality:

1. Start with quality range 20-100
2. Binary search to find quality that gets closest to target size
3. If target not achievable at quality 20, downscale dimensions by 10%
4. Repeat until target reached or minimum size (50x50px)
5. Maximum 10 compression attempts for safety

## Known Limitations

1. **Billing Testing**: Cannot test real billing in debug builds. Must publish to internal testing track.
2. **AdMob Test Mode**: Uses test ad units by default. Replace with real IDs for production.
3. **No Subscription Support**: Only one-time purchases implemented.
4. **JPEG Only**: Compression uses JPEG format (lossy compression).

## Troubleshooting

### "Product not found" error
- Create `pro_upgrade` product in Google Play Console
- Ensure product is activated
- Test only on internal testing track with uploaded APK/AAB

### Ads not showing
- Replace test ad units with real IDs
- Check internet connection
- AdMob needs time to activate new ad units (can take hours)

### Permission denied
- Check AndroidManifest.xml has correct permissions
- On Android 13+, ensure READ_MEDIA_IMAGES is declared
- Request permission before picking image

### Build errors
- Run `./gradlew clean`
- Sync Gradle files
- Check that all dependencies are accessible
- Ensure Gradle version 8.2+ and AGP 8.2+

## License

This project is provided as-is for educational and commercial use.

## Support

For issues with:
- **Google Play Billing**: [Billing Documentation](https://developer.android.com/google/play/billing)
- **AdMob**: [AdMob Help Center](https://support.google.com/admob)
- **Android Development**: [Android Developers](https://developer.android.com)

# Firebase Setup Guide for CryptoDept

To enable Crashlytics and Analytics, follow these steps:

1.  **Create a Firebase Project:**
    *   Go to [Firebase Console](https://console.firebase.google.com/).
    *   Click "Add project" and name it `CryptoDept`.
    *   Enable Google Analytics for the project.

2.  **Register the Android App:**
    *   Click the Android icon to add an app.
    *   Package name: `com.cryptodept` (verify in `build.gradle.kts`).
    *   App nickname: `CryptoDept Master`.
    *   SHA-1: Get it using `./gradlew signingReport`.

3.  **Download Configuration:**
    *   Download `google-services.json`.
    *   Place it in the `D:/CryptoDept/app/` directory.

4.  **Enable Crashlytics:**
    *   In the Firebase Console, go to "Release & Monitor" -> "Crashlytics".
    *   Click "Enable Crashlytics".

5.  **Enable Performance Monitoring:**
    *   Go to "Release & Monitor" -> "Performance".
    *   Click "Enable".

6.  **Verify Integration:**
    *   Run the app.
    *   Check the Firebase Console for a "Connection successful" message.

**Note:** The `google-services.json` file is ignored by Git to protect project-specific IDs.

# GainTrack Android App

GainTrack is a private, local-first Android health and weight-gain tracker built with Kotlin and Jetpack Compose. It combines personalized meal and workout planning, daily logging, weight-progress analytics, recurring shopping, CSV portability, reminders, and guided onboarding in one offline-first app.

The current application version is **2.2.0** (`versionCode 5`) and its Android application ID is `com.gaintrack.personal`.

## Main features

- Weight-gain-focused onboarding with 1, 2, 3, 6, 9, or 12-month goal paths.
- Personalized calorie, protein, diet, training-day, equipment, and meditation targets.
- Daily dashboard for meals, extra food, workout status, sleep, weight, and notes.
- Overdue-meal deficit calculation and next-meal scheduling based on the user's routine.
- Editable weekday/Sunday meal plans and custom workout exercises.
- Exercise form guidance, common mistakes, target muscles, equipment, and learning links.
- Manual meditation-minute tracking with weekly completion history.
- Calendar logbook and historical day details.
- Weight trajectory and health analytics for weekly and multi-month ranges.
- Recurring shopping templates, purchase history, expense tracking, and monthly budgets.
- Daily, exercise, and expense CSV exports plus compatible daily CSV import.
- Optional daily notifications and first-unlock check-in reminders.
- Screen tours that can be replayed from Settings.
- Bundled motivation artwork with optional Pexels-powered online imagery.
- Backward-compatible migration for legacy shopping and tracking data.

## Privacy and networking

Health and tracker state is stored locally on the Android device. The app does not include an account system, analytics SDK, advertising SDK, or remote application database.

Internet access is used only for optional quote and motivation-image fetching. When no Pexels API key is configured, bundled/fallback imagery remains available. Exercise learning actions may open YouTube search URLs in the user's browser.

The app requests these Android permissions:

| Permission | Purpose |
| --- | --- |
| `INTERNET` | Optional quote and image requests. |
| `ACCESS_NETWORK_STATE` | Detect whether optional network content can be requested. |
| `POST_NOTIFICATIONS` | Daily, meal, and motivation notifications on supported Android versions. |
| `RECEIVE_BOOT_COMPLETED` | Restore enabled reminder scheduling after device restart. |
| `SYSTEM_ALERT_WINDOW` | Optional first-unlock health check-in overlay. |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` | Keep the optional unlock reminder service active. |

Overlay access and notifications remain controlled by the user through Android system settings.

## Technology

| Area | Choice |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | Single-activity UI, Android `ViewModel`, local repository/state flow |
| Persistence | Local Android preferences with migration-compatible serialized state |
| Background work | AndroidX WorkManager and an optional foreground service |
| Image loading | Coil 3 |
| Build | Gradle 9.1 wrapper, Android Gradle Plugin 9.0.1 |
| Android SDK | compile/target SDK 36, minimum SDK 26 |
| Java | JDK 17 |
| Tests | JUnit 4 unit tests and Compose instrumentation tests |

## Project structure

```text
gaintrack_app/
|-- app/
|   |-- build.gradle.kts
|   `-- src/
|       |-- main/
|       |   |-- AndroidManifest.xml
|       |   |-- java/com/gaintrack/personal/
|       |   |   |-- data/          # Models, storage, CSV, analytics and tracking
|       |   |   |-- reminder/      # Workers, notifications and unlock service
|       |   |   |-- ui/            # Shared Compose UI, theme and tours
|       |   |   |-- ui/screens/    # Today, plan, progress, logbook, shopping, settings
|       |   |   |-- MainActivity.kt
|       |   |   `-- MainViewModel.kt
|       |   `-- res/
|       |-- test/                   # JVM unit tests
|       `-- androidTest/            # Compose instrumentation tests
|-- binary_assets/                  # Lossless chunks and artwork manifest
|-- tools/RestoreBinaryAssets.java  # Reconstructs and verifies bundled PNG artwork
|-- gradle/wrapper/                 # Reproducible Gradle wrapper
|-- build.gradle.kts
`-- settings.gradle.kts
```

## Requirements

Install the following before opening the project:

- Android Studio with Android SDK Platform 36 and build tools.
- JDK 17. Android Studio's compatible embedded JDK can also be used.
- Git.
- An Android 8.0/API 26 or newer emulator/device.

Confirm Java is available:

```powershell
java -version
```

The output should report Java 17.

## Clone and run locally

### 1. Clone the repository

```powershell
git clone https://github.com/ankitsingh-013245/gaintrack-app.git
cd gaintrack-app
```

### 2. Restore bundled artwork

The three original PNG artworks are versioned as small binary parts because some TLS/security environments reject large Git uploads. They are reconstructed without recompression or quality loss:

```powershell
java tools/RestoreBinaryAssets.java
```

The tool verifies every restored file's byte length and SHA-256 digest before installing it under `app/src/main/res/drawable-nodpi/`.

### 3. Configure the Android SDK

Android Studio normally creates `local.properties` automatically. For a command-line setup, create it locally with your SDK path, for example:

```properties
sdk.dir=C:\\Users\\YOUR_NAME\\AppData\\Local\\Android\\Sdk
```

`local.properties` is intentionally ignored by Git because paths differ between computers.

### 4. Open in Android Studio

1. Start Android Studio.
2. Select **Open**.
3. Choose the cloned `gaintrack-app` folder.
4. Allow Gradle sync to finish.
5. Select the `app` run configuration and an API 26+ device.
6. Click **Run**.

You can also build from PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug
```

On macOS or Linux:

```bash
./gradlew :app:assembleDebug
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Optional Pexels API key

The app works without a Pexels API key. To enable rotating live motivation imagery, place the property in your user-level Gradle configuration so it is not committed:

```properties
PEXELS_API_KEY=your_real_key_here
```

Windows location:

```text
C:\Users\YOUR_NAME\.gradle\gradle.properties
```

macOS/Linux location:

```text
~/.gradle/gradle.properties
```

Do not add real credentials to the repository's tracked `gradle.properties` file.

## Tests

Run JVM unit tests:

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

The suite covers personalized plans, meal timing, weekly weigh-ins, analytics, budget calculations, shopping recurrence, legacy purchase migration, and the seeded workout/meal plan.

Run Compose instrumentation tests on a connected emulator or device:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

Build and unit-test together:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

## CSV portability

GainTrack can export daily tracker data, exercise logs, and expenses as CSV files. It can import the original `daily-tracker.csv` format and GainTrack daily exports. Review imported data inside the app after importing, especially when a CSV was edited manually.

## Reminder behavior

- Daily and meal reminders use WorkManager.
- Enabled schedules are restored after a device restart.
- The optional unlock reminder requires overlay permission and runs as a foreground service.
- Users can disable reminders from the app or revoke permissions from Android settings.

Android background restrictions vary by manufacturer, so aggressive battery optimization may delay scheduled work.

## Release build and signing

The repository does not include a production signing key. Never commit keystores, passwords, or `key.properties`.

Before publishing a release:

1. Create and securely back up a release keystore.
2. Add a local signing configuration outside version control.
3. Review permissions and foreground-service declarations against current Google Play policy.
4. Test notification and overlay permission flows on supported Android versions.
5. Run JVM and device tests.
6. Build a signed Android App Bundle and test it through an internal track.
7. Publish an accurate privacy policy describing local health data and optional network requests.

## Troubleshooting

### `JAVA_HOME` or Java version errors

Point `JAVA_HOME` and Android Studio's Gradle JDK to JDK 17, then retry Gradle sync.

### Android SDK not found

Open the project in Android Studio or create a local `local.properties` with a valid `sdk.dir` value.

### Missing `art_*.png` resource

Run the asset restoration command from the repository root:

```powershell
java tools/RestoreBinaryAssets.java
```

### Notifications do not appear

Grant notification permission on Android 13+, verify the reminder is enabled, and check device battery restrictions.

### Unlock reminder does not appear

Enable the feature in GainTrack and grant **Display over other apps** permission in Android settings.

### Pexels images do not rotate

Verify `PEXELS_API_KEY` is present in the user-level Gradle properties file, rebuild the app, and confirm network access. Bundled/fallback imagery is used when the key is missing or a request fails.

## Generated files excluded from Git

Gradle caches, build outputs, IDE metadata, `local.properties`, signing material, logs, and reconstructed PNG artwork are excluded. A fresh clone restores artwork with `java tools/RestoreBinaryAssets.java` and regenerates build output through Gradle.

## License

No open-source license has been declared. Unless the repository owner adds one, copyright law reserves reuse and redistribution rights.

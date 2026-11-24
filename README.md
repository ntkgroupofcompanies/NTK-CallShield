# NTK CallShield

**NTK CallShield** is an offline-first Android Caller ID, spam call screener, and SMS link checker built with Kotlin, Jetpack Compose, and Room.

## Key Capabilities

- **On-device Call Screening (`CallScreeningService`):** Evaluates incoming calls locally with Bloom-filter lookup, normalized number matching, repeated-caller bypass, and privacy audit logging.
- **Live Phone Sync (`ContentResolver`):** Syncs your device Contacts, Call Log, and SMS Inbox directly on your phone without uploading personal records.
- **Click-Number-to-Calls Navigation:** Tap any phone number across Contacts, Reverse Lookup, Call rows, or Safe/Block lists to open filtered call history for that number.
- **Adaptive Material 3 Theming:** Defaults to System Light/Dark mode, with optional Light, Dark, True Black (AMOLED), and Android 12+ dynamic wallpaper color overrides persisted in DataStore.
- **Zero-Idle Power Architecture:** Eliminates idle GPU frame loops, persistent background services, and unnecessary WakeLocks or alarms.

## Build and Test

Run unit and Compose Robolectric tests:

```bash
./gradlew :app:testDebugUnitTest
```

Build the debug APK:

```bash
./gradlew :app:assembleDebug
```

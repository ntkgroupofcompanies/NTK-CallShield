# NTK CallShield

**NTK CallShield** is an offline-first Android Caller ID, spam call blocker, and SMS link checker built with Kotlin, Jetpack Compose, and Room. **Know before you answer.**

---

## Download & Install on Android

You do **not** need Android Studio, coding knowledge, or a computer to use NTK CallShield. Download the finished app directly onto any phone running **Android 10 or newer**:

- **[Download NTK CallShield v1.0 APK (Android 10+, Release-Signed, 48.30 MB)](https://github.com/appdevmanwill/NTK-CallShield/releases/latest/download/NTK-CallShield-v1.0.apk)**
- **[View GitHub Releases Page, SHA-256 Checksum & Signing Fingerprint](https://github.com/appdevmanwill/NTK-CallShield/releases/latest)**

### Quick 4-Step Phone Install
1. Tap the direct APK download link above on your Android phone.
2. When the download finishes, open the `.apk` file from your browser downloads or your phone's **Files** app.
3. If Android asks for permission to install unknown apps, tap **Settings**, turn on **Allow from this source**, and tap **Back**.
4. Tap **Install**, then open **NTK CallShield**.

> **Note on Google Play Protect:** This APK is built in Release mode (`debuggable=false`) and signed with a private 2048-bit RSA key (`APK Signature Scheme v2`). If Google Play Protect shows a one-time **Scan app** prompt the first time you sideload a newly published key outside Google Play, allow the scan to confirm the package is clean.

### First-Time Phone Setup
1. Set **NTK CallShield** as your default **Caller ID & Spam** app when prompted so incoming calls screen in real time.
2. Allow **Contacts**, **Call Log**, and **SMS** access when prompted to see your real phonebook, call history, and inbox scans.
3. Optional: turn on **Display over other apps** to view the floating Caller ID banner during calls.

---

## Key Features

- **On-Device Call Screening (`CallScreeningService`):** Screens incoming numbers locally using Bloom-filter lookup, normalized E.164 matching, repeated-caller bypass, and privacy audit logs.
- **Live Phone Sync (`ContentResolver`):** Syncs your phone Contacts, Call Log, and SMS Inbox locally on your device without uploading personal records.
- **Click-Number-to-Calls Navigation:** Tap any phone number across Contacts, Reverse Lookup, Call rows, or Safe and Block lists to jump straight to filtered call history for that number.
- **Link & SMS Scam Scanner:** Flags suspicious phishing URLs, bank impersonation numbers, and SMS pumping patterns directly on your device.
- **Adaptive Material 3 Theming:** Follows your phone's Light or Dark mode automatically by default, with optional Light, Dark, True Black (AMOLED), and Android 12+ Material You wallpaper colors in Settings.
- **Zero-Idle Power Architecture:** Designed for zero idle GPU frame draws, zero persistent background services, and zero unnecessary WakeLocks or alarms.

---

## Build from Source (For Developers)

Run unit and Compose Robolectric tests:

```bash
./gradlew :app:testDebugUnitTest
```

Build the debug APK:

```bash
./gradlew :app:assembleDebug
```

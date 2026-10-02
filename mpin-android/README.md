# MPIN Generator for Android

A native Android app (Java) that generates secure **4- or 6-digit MPINs**. Each MPIN is tagged with a **label / purpose**. The app is protected by **fingerprint or your phone's PIN / pattern / password**, and it can **email every new MPIN to your Gmail**.

## Features

| | |
|---|---|
| 🔒 **App lock** | Opens on a lock screen. Unlock with your **fingerprint** (or face unlock, if your phone counts it as strong), or your phone's **PIN, pattern or password**. Uses Android's `BiometricPrompt`, so it's the same secure system dialog banking apps use. |
| ⏱ **Auto-lock** | Locks again when you leave the app: immediately, or after 30 s / 1 min / 5 min (Settings). There's also a 🔒 **Lock now** button. |
| 🔢 **Generator** | 4 or 6 digits only, from `SecureRandom`. Weak PINs are never issued (`1111`, `1234`, `4321`, `1212`, `1122`, `2580`...). |
| 🏷 **Label / Purpose** | Required for every MPIN (up to 60 characters). Recent labels are suggested as you type. |
| ✨ **UI** | Dark Material 3 design, a slot-machine digit reveal, copy (the clipboard clears after 45 s), show/hide, and history with each MPIN's email status. |
| 📊 **Excel export** | Saves `mpin_data_<date>.xlsx` (#, Label / Purpose, MPIN, Length, Generated At, Email) wherever you choose. MPINs are stored as text, so leading zeros are kept. |
| ✉️ **Gmail alerts** | Each new MPIN is emailed to your registered Gmail with **JavaMail** (the API Spring's `JavaMailSender` wraps). Emails are sent in the background and retried automatically if you're offline. |
| 🛡 **Privacy** | MPINs and your Gmail App Password are **encrypted** with an AES-256 key in the Android Keystore. Screenshots and the recents preview are blocked. Backups are disabled. |
| 🖼 **Icon** | A padlock with "123" on a teal-to-indigo gradient (adaptive icon, with a monochrome version for themed icons). |

## Build and install

1. Install **Android Studio** (Ladybug 2024.2 or newer).
2. **File → Open** and select this `mpin-android` folder. Wait for the Gradle sync to finish.
3. Connect your phone with **USB debugging** on (or start an emulator), then press **Run ▶**.

To make an APK you can copy to a phone: **Build → Build App Bundle(s) / APK(s) → Build APK(s)**. The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

Requirements: Android 8.0 (API 26) or newer, and a screen lock set up on the phone. If the phone has no screen lock, the app asks you to set one up first.

## Set up Gmail alerts

Gmail doesn't let apps sign in with your normal password, so you need an **App Password**:

1. Turn on **2-Step Verification** in your Google Account.
2. Go to <https://myaccount.google.com/apppasswords>, create one named "MPIN Generator", and copy the 16 characters.
3. In the app, open **Settings** and turn on **Email me when an MPIN is generated**. Enter your Gmail address and the App Password, then tap **Send test email**.

The email includes the label, MPIN, length, time and device name. If you'd rather not send the full MPIN, turn off **Include the full MPIN in the email**, and only the last 2 digits are shown.

## Project layout

```
app/src/main/java/com/mpin/app/
├── MpinApp.java                 app start, hooks up the auto-lock
├── core/                        plain Java, unit-tested
│   ├── MpinGenerator.java       SecureRandom + weak-PIN filter
│   ├── Labels.java              label clean-up rules
│   ├── MpinRecord.java
│   ├── XlsxWriter.java          tiny .xlsx writer (no Apache POI needed)
│   └── EmailContent.java        email subject / text / HTML
├── data/
│   ├── CryptoBox.java           Android Keystore AES-GCM file encryption
│   ├── MpinRepository.java      encrypted MPIN history
│   ├── EmailSettings.java
│   └── SettingsStore.java
├── security/AppLock.java        lock state + auto-lock timer
├── mail/
│   ├── GmailSender.java         JavaMail → smtp.gmail.com:587 (STARTTLS)
│   └── EmailWorker.java         WorkManager job: send, retry when offline
└── ui/
    ├── LockActivity.java        fingerprint / PIN / pattern unlock
    ├── SecureActivity.java      FLAG_SECURE + redirect to lock screen
    ├── MainActivity.java        generator, history, export
    ├── HistoryAdapter.java
    └── SettingsActivity.java    Gmail + auto-lock settings
```

Run the unit tests with `./gradlew test`.

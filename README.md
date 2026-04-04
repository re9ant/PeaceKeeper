# PeaceKeeper 🔕

PeaceKeeper is a lightweight, privacy-friendly Android application designed to give you back control of your phone's ringtone. It leverages Android's modern `CallScreeningService` API to silently intercept and mute unwanted calls without completely blocking them or hanging up on the caller.

## Download & Install 📥

You can download the ready-to-run Android App (APK) directly from our repository without needing to build it yourself!

**[➡️ Download PeaceKeeper.apk Here](https://github.com/re9ant/PeaceKeeper/raw/main/downloads/PeaceKeeper.apk)**

1. Download the `PeaceKeeper.apk` file to your Android phone.
2. Open it to install (you may need to tap "Allow from this source" in your settings).
3. Open the app and tap **Setup Permissions** to grant Contact access and set PeaceKeeper as your default Call ID & Spam app.

## Features ✨

- **Mute Unknown Callers:** A simple toggle switch to automatically silence the ringtone for any incoming call from a number that is not saved in your Contacts.
- **Specific Contact Muting:** A "Mute List" where you can explicitly pick contacts from your address book. If they call, your phone will remain silent.
- **Battery Efficient:** Uses the native Telecom framework. PeaceKeeper only wakes up for a split second when a call occurs, consuming virtually 0% battery footprint in the background.

## How It Works 🛠️

Unlike older call blockers that require constant background execution, PeaceKeeper relies on `ROLE_CALL_SCREENING`. When a call arrives, the Android OS passes the number to PeaceKeeper:
1. It normalizes the incoming phone number.
2. It checks your explicitly muted specific contacts.
3. It checks your device's Contact Book to see if the number is saved.
4. If the number is an unknown caller (and the toggle is enabled) or manually blacklisted, it sends a `CallResponse` with `setSilenceCall(true)` back to the OS.

## Development 💻

If you want to build the project yourself:
1. Clone this repository.
2. Open the project in **Android Studio**.
3. Let Gradle sync and hit "Run".

**Requirements:**
- **Minimum SDK:** 29 (Android 10)
- **Permissions Required:** `READ_CONTACTS`, `BIND_SCREENING_SERVICE`

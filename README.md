# PeaceKeeper 🔕

PeaceKeeper is a lightweight, privacy-friendly Android application designed to give you back control of your phone's ringtone. It leverages Android's modern `CallScreeningService` API to silently intercept and mute unwanted calls without completely blocking them or hanging up on the caller.

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

## Installation 📱

1. Clone this repository.
2. Open the project in **Android Studio**.
3. Build the APK or App Bundle.
4. Install on your Android 10+ device.
5. Open the app and tap **Setup Permissions** to grant Contact access and set PeaceKeeper as your default Call ID & Spam app.

## Requirements

- **Minimum SDK:** 29 (Android 10)
- **Permissions:** `READ_CONTACTS`, `BIND_SCREENING_SERVICE`

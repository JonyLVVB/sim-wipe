# SIM Wipe

A one-button Android app that deletes every text message and the whole call history from a
phone, then shows how many are left.

It is meant for a shop or bench phone that temporarily holds other people's SIM cards (for
example while a number is being moved to a new phone) and must not keep their messages or
calls afterwards.

> **Status: early and not yet proven.** The app installs and runs on a Galaxy A16 5G
> (Android 16, One UI 8), but the wipe has not yet been tested against real messages and
> calls. Do not rely on it until you have checked it on your own phone.

## What it does

1. Tap **WIPE MESSAGES + CALLS**.
2. Android asks to make SIM Wipe the default SMS app. Android only lets the default SMS app
   delete texts, so this is required every time; it also acts as the confirmation.
3. The app deletes all SMS, MMS and conversation threads, and the call log, then recounts
   them and shows **PASS** (all zero) or **FAIL** with the numbers left. A count that cannot
   be read is shown as `?` and treated as a failure.
4. Tap **Give texting back to Messages** to reopen Google Messages, which asks to become the
   default SMS app again.

## Limits

- **RCS chats are not wiped.** They are stored inside Google Messages, where no other app
  can reach them. Turn RCS off on a phone used this way; see
  [Turning RCS off for good](#turning-rcs-off-for-good).
- Google Messages keeps its own cached copy of texts. It is expected to drop them once the
  system message store is empty and it is the default app again; check this on your phone.
- A text that arrives during the few seconds SIM Wipe holds the SMS role is saved for
  Messages. A picture message arriving in that window is not downloaded.
- Deleted rows are removed from the system databases; this is not a forensic-grade erase.

## Turning RCS off for good

### Why

RCS ("chat features") messages live only inside Google Messages, so SIM Wipe cannot delete
them. RCS is also registered per phone number: when a customer's SIM goes in, Messages may
switch RCS on for that number by itself. Their incoming chats can then arrive on this phone,
stay there after the wipe, and keep the number registered to this phone for a while after
the SIM has moved on.

The RCS switch in Messages (profile picture > Messages settings > RCS chats) only appears
while a SIM is inserted and may turn itself back on for the next SIM. It is not a one-time
fix.

Google Messages needs the **Carrier Services** app (`com.google.android.ims`) for RCS. With
Carrier Services disabled, RCS cannot register for any SIM. Plain SMS/MMS and normal calls do
not depend on it.

### How

With USB debugging on and the phone connected to a computer with
[adb](https://developer.android.com/tools/adb):

```
adb shell pm disable-user --user 0 com.google.android.ims
```

On some phones you can also do it without a computer: Settings > Apps > Carrier Services >
Disable (or Force stop, then Disable). If the button is greyed out, use adb.

To undo:

```
adb shell pm enable com.google.android.ims
```

### Check it worked

1. Insert a SIM, wait a minute, then open Messages > profile picture > Messages settings.
   RCS chats should be unavailable or show as not connected.
2. Send the phone a normal text and call it, to confirm SMS and calls still work.

If a customer's number was already registered for RCS on this phone, they can release it at
[messages.google.com/disable-chat](https://messages.google.com/disable-chat).

> Not yet verified on the Galaxy A16 5G test phone. Carrier Services can also handle carrier
> features such as Wi-Fi calling on some carriers; if something stops working, re-enable it.

## Permissions

- Read SMS, receive SMS/MMS/WAP push: needed to qualify as (and behave as) a default SMS app.
- Read and write call log: to delete and count call history.
- **No internet permission**, and Android backup is disabled, so nothing leaves the phone.

## Build

No Gradle. `build.ps1` (Windows PowerShell) uses the Android SDK command-line tools directly:
`aapt2`, `javac`, `d8`, `zipalign`, `apksigner`.

Requirements: Android SDK with build-tools 36.0.0 and platform android-36 in
`%LOCALAPPDATA%\Android\Sdk`, and a JDK 11 or newer (Android Studio's bundled one is used by
default).

```powershell
powershell -ExecutionPolicy Bypass -File build.ps1
```

The first build creates its own signing key (`signing.jks`, `signing.pass`). Keep both:
an update only installs over an older copy when it is signed with the same key. An APK you
build yourself will therefore not install over the released one without uninstalling first.

## Install

```
adb install simwipe.apk
```

Requires Android 10 or newer.

## License

MIT. See [LICENSE](LICENSE).

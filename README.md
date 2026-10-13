# SIM Wipe

A one-button Android app that deletes every text message and the whole call history from a
phone, then shows how many are left.

It is meant for a shop or bench phone that temporarily holds other people's SIM cards (for
example while a number is being moved to a new phone) and must not keep their messages or
calls afterwards.

It is not tied to any phone model: it uses only standard Android APIs and should work on any
phone running Android 10 or newer.

> **Status: early and not yet proven.** The app installs and runs, but the wipe has not yet
> been tested against real messages and calls. Do not rely on it until you have checked it
> on your own phone.

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
  can reach them. Keep RCS off on a phone used this way; see
  [Keeping RCS off](#keeping-rcs-off-disabling-carrier-services).
- Google Messages keeps its own cached copy of texts. It is expected to drop them once the
  system message store is empty and it is the default app again; check this on your phone.
- A text that arrives during the few seconds SIM Wipe holds the SMS role is saved for
  Messages. A picture message arriving in that window is not downloaded.
- Deleted rows are removed from the system databases; this is not a forensic-grade erase.

## Keeping RCS off: disabling Carrier Services

### Why

SIM Wipe can only delete what Android stores in the shared SMS/MMS database. RCS chats
("chat features" in Google Messages) are different:

- They are stored only inside Google Messages, so the wipe cannot remove them and can
  report PASS while RCS chats are still on the phone.
- RCS registers the SIM's phone number with the RCS service. On a phone that holds other
  people's SIMs, that can attach a customer's number to this phone, deliver their incoming
  chats here, and leave the number registered for a while after the SIM has moved on.
- The RCS switch in Messages is per number and is hidden when no SIM is inserted, so it
  cannot be turned off once in advance, and no other app is allowed to change it.

On phones where Google Messages gets RCS through the separate **Carrier Services** app
(package `com.google.android.ims`), disabling that app stops RCS from connecting for every
SIM, without touching normal SMS, MMS or calls.

### How

On the phone:

1. Settings > Apps.
2. Open the filter/sort menu and turn on **Show system apps**.
3. Open **Carrier Services** (the Google one).
4. Tap **Disable**. If only **Uninstall** is offered, uninstall it, and also turn off
   auto-update for it in the Play Store so it does not come back.

Or from a computer:

```
adb shell pm disable-user --user 0 com.google.android.ims
```

To undo: `adb shell pm enable com.google.android.ims`, or enable it again in Settings.

### Check that it worked

- Not every phone has Carrier Services. Check with
  `adb shell pm list packages com.google.android.ims`; if nothing is listed, RCS is built
  into Google Messages itself and this step does nothing.
- Newer versions of Google Messages may not depend on Carrier Services even when it is
  installed. With a SIM inserted, open Messages > profile picture > Messages settings >
  RCS chats and confirm the status is off or unavailable rather than "Connected".
- Disable only Google's Carrier Services. Leave the phone maker's or carrier's own IMS
  packages alone (for example `com.sec.imsservice` on Samsung); those carry normal calls
  and texts over LTE/5G.

If RCS still connects, the options are to turn off **RCS chats** in Messages for each SIM,
or not to use Google Messages as the default SMS app on that phone. If a number does get
registered, Google's deregistration page is <https://messages.google.com/disable-chat>.

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

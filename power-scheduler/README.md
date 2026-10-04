# Power Scheduler

An Android app that powers your phone off (and, on supported phones, back on) at
times you choose, for phones that don't have the built-in "Scheduled power on/off"
setting.

## What you need to know first

Android does not let ordinary apps turn the phone off or on. That feature only
exists when the phone maker builds it in. This app works around it like this:

| Feature | Requirement | Works on |
| --- | --- | --- |
| Scheduled power **off** | Root, **or** the accessibility helper (no root) | Most phones |
| Scheduled power **on** | Root, plus hardware support | Some rooted phones only: use the in-app **Test** to check |

* **Power off** first gives you a notification with a 60-second countdown and a
  **Cancel** button. Then:
  * **With root:** runs `svc power shutdown` (a normal, clean shutdown).
  * **Without root:** the accessibility helper turns the screen on, opens the
    power menu and taps **Power off** (and the confirm button, if there is one),
    just like you would. Power menus differ between brands, so press
    **Test: power off now** to check it works on your phone. It can't work if
    your phone asks for your PIN before powering off; turn that setting off.
* **Power on** writes the wake-up time to the phone's hardware clock
  (`/sys/class/rtc/rtc0/wakealarm`) just before the app shuts the phone down.
  Whether the phone actually boots at that time depends on its hardware and
  bootloader; many phones ignore it. It only applies when *this app* powered
  the phone off.

Without root, nothing can power a phone back **on** by itself. Your phone's
built-in **Bedtime mode** (Settings → Digital Wellbeing, or "Modes") can silence
it overnight instead, so it's still on in the morning with your alarm.

## Install

1. Open the **Actions** tab on GitHub → **Power Scheduler APK** → the latest
   run → download the `power-scheduler-apk` artifact and unzip it.
2. Copy `app-release.apk` to the phone and open it (allow "Install unknown apps"
   when asked).
3. Allow notifications so you see the countdown.
4. **No root:** tap **Open accessibility settings**, find **Power Scheduler**
   (sometimes under "Installed apps" or "Downloaded apps") and turn it on.
   On Android 13+ the switch may be greyed out for apps installed from an APK:
   go to Settings → Apps → Power Scheduler → ⋮ (top right) →
   **Allow restricted settings**, then try again.
   **Rooted:** tap **Check root** and grant root in your root manager.
5. Press **Test: power off now** to make sure it works.

## Use

* Turn on **Scheduled power off**, tap the time to change it, and pick the days.
* Turn on **Scheduled power on** and set a wake time (try **Test power on** first:
  it shuts the phone down and asks it to come back in 3 minutes).
* Schedules survive reboots and time zone changes.

## Build it yourself

Needs JDK 17 and the Android SDK (API 35):

```sh
cd power-scheduler
./gradlew assembleRelease
# APK: app/build/outputs/apk/release/app-release.apk
```

The release build is signed with the debug key so it installs directly.

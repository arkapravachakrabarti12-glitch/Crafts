# Power Scheduler

An Android app that powers your phone off (and, on supported phones, back on) at
times you choose, for phones that don't have the built-in "Scheduled power on/off"
setting.

## What you need to know first

Android does not let ordinary apps turn the phone off or on. That feature only
exists when the phone maker builds it in. This app works around it like this:

| Feature | Requirement | Works on |
| --- | --- | --- |
| Scheduled power **off** | **Root** | Any rooted phone |
| Scheduled power **on** | Root, plus hardware support | Some phones only: use the in-app **Test** to check |

* **Power off** runs `svc power shutdown` (a normal, clean shutdown) as root.
  You get a notification with a 60-second countdown and a **Cancel** button first.
* **Power on** writes the wake-up time to the phone's hardware clock
  (`/sys/class/rtc/rtc0/wakealarm`) just before the app shuts the phone down.
  Whether the phone actually boots at that time depends on its hardware and
  bootloader; many phones ignore it. It only applies when *this app* powered
  the phone off.

Without root, the app can't power the phone off. Nothing else on Android can
either, short of the maker adding the feature.

## Install

1. Open the **Actions** tab on GitHub → **Power Scheduler APK** → the latest
   run → download the `power-scheduler-apk` artifact and unzip it.
2. Copy `app-release.apk` to the phone and open it (allow "Install unknown apps"
   when asked).
3. Open **Power Scheduler**, tap **Check root** and grant root in your root
   manager (Magisk, KernelSU, …).
4. Allow notifications so you see the countdown.

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

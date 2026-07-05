# Nokia 7.2 Double-Tap Screen Control — Implementation Plan

**Goal:** Native Java Android app — tap-to-sleep + shake-to-wake + full volume control (ring/notification/alarm/media/call) — to replace broken power **and** volume side buttons, built for father, works on any Android phone (not Nokia-specific), no third-party dependencies.

---

## 1. Tech Stack (Final)

| Item | Choice |
|---|---|
| Language | Java |
| IDE | Android Studio Quail 1 (2026.1.1+) |
| UI | XML layouts + ViewBinding + Material Components (AndroidX) |
| Build | Gradle (build.gradle, Groovy) |
| minSdk | 26 (Android 8.0) |
| targetSdk | latest stable |
| Dependencies | AndroidX / Google official libraries only — zero third-party |

---

## 2. Permissions Required

| Permission | Purpose | Android version |
|---|---|---|
| `BIND_ACCESSIBILITY_SERVICE` | Detect taps on home/lock screen | Pie (9)+ |
| `SYSTEM_ALERT_WINDOW` (Draw over other apps) | Invisible overlay to catch taps | All |
| `PACKAGE_USAGE_STATS` (Usage Access) | Detect when user is on home screen | All |
| `BIND_DEVICE_ADMIN` | `lockNow()` fallback | <Pie, and lock action itself |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` | Keep shake-listener alive | All |
| `RECEIVE_BOOT_COMPLETED` | Restart service after reboot | All |
| `VIBRATE` | Feedback on trigger | All |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Prevent Doze from killing service | All |
| `ACCESS_NOTIFICATION_POLICY` | Full ringer/silent control for Ring & Notification volume | Android 7+ |
| `MODIFY_AUDIO_SETTINGS` | Change stream volumes via AudioManager | All |
| `POST_NOTIFICATIONS` | Show persistent volume/status notification | Android 13+ |

---

## 3. Final Feature List

**Core — Screen Control**
1. Tap-to-sleep — configurable 2 or 3 taps on home/lock screen → screen locks
2. Shake-to-wake — accelerometer-based, works with screen fully off
3. Volume-button-wake — secondary fallback wake gesture (only useful if volume keys still work)
4. Sensitivity calibration — separate sliders for tap force & shake threshold
5. Foreground service — persistent, restarts on boot, survives Doze

**Core — Volume Control (primary target: broken side buttons)**
6. In-app volume panel — independent sliders for Ring, Notification, Alarm, Media/Music, Call volume (via `AudioManager`)
7. Floating volume bubble — small draggable always-on-top overlay (reuses Draw-Over-Apps permission) that expands into quick sliders, reachable without opening the app
8. Per-stream mute/unmute toggle buttons
9. Silent / Vibrate / Normal ringer mode switch (needs Do-Not-Disturb access)
10. Persistent notification with inline +/- volume buttons for the most-used stream (configurable)

**Usability / Safety**
11. Guided onboarding wizard — step-by-step permission requests in plain language, explains *why* each is needed
12. Safe-exclusion list — screen-lock gesture disabled during calls / camera app / typing
13. Built-in safe-uninstall flow — auto-revokes Device Admin before uninstall (avoids the "can't uninstall" trap)
14. Vibration feedback on every successful trigger
15. Crash-safety watchdog — periodic check (WorkManager) restarts the service if the OS kills it
16. OEM auto-start guidance screen — detects manufacturer (Xiaomi/Samsung/Vivo/Oppo/etc.) and shows the extra manual whitelist step those OEMs require, since this is now a universal app

**Personalization (for father)**
17. Hindi / English toggle
18. "Simple Mode" screen — large text, big on/off switches, minimal options, volume bubble on by default
19. Test/Demo button — try tap and volume gestures live, for setup verification

**Extras**
20. Battery usage indicator — transparency on background drain
21. Home-screen-only vs anywhere toggle for tap detection

---

## 4. Architecture / File List

```
app/
├── AndroidManifest.xml
├── MainActivity.java                  → Simple Mode home screen
├── onboarding/
│   ├── OnboardingActivity.java        → step-by-step permission wizard
│   └── PermissionStepFragment.java
├── service/
│   ├── TapAccessibilityService.java   → detects tap pattern, triggers lock
│   ├── ShakeWakeService.java          → foreground service, accelerometer listener
│   ├── ServiceWatchdogWorker.java     → WorkManager, restarts if killed
│   └── BootReceiver.java              → BOOT_COMPLETED → restart services
├── admin/
│   └── ScreenLockAdminReceiver.java   → DeviceAdminReceiver, lockNow()
├── volume/
│   ├── VolumeController.java          → wraps AudioManager, one method per stream
│   ├── VolumeBubbleService.java       → floating overlay bubble + expandable slider panel
│   └── VolumeNotification.java        → persistent notification with inline +/- buttons
├── settings/
│   ├── SettingsActivity.java          → sensitivity, tap count, exclusions, language, stream toggles
│   └── SettingsPrefs.java             → SharedPreferences wrapper
├── ui/
│   ├── TestDemoActivity.java          → live gesture + volume test screen
│   ├── OemGuidanceActivity.java       → manufacturer-specific autostart help
│   └── UninstallHelperActivity.java   → revoke admin, guide uninstall
└── res/
    ├── layout/ (activity_main.xml, activity_onboarding.xml, activity_settings.xml, view_volume_bubble.xml, ...)
    ├── values/ (strings.xml, values-hi/strings.xml for Hindi)
    └── xml/ (accessibility_service_config.xml, device_admin.xml)
```

---

## 5. Build Order (resume from any checkpoint)

- [ ] **Phase 1** — Project setup: new project, `AndroidManifest.xml`, `build.gradle`, all permission declarations
- [ ] **Phase 2** — `ScreenLockAdminReceiver` + `device_admin.xml` (Device Admin lock capability)
- [ ] **Phase 3** — `TapAccessibilityService` + `accessibility_service_config.xml` (tap detection → lock)
- [ ] **Phase 4** — `ShakeWakeService` (accelerometer foreground service, wake on shake)
- [ ] **Phase 5** — `VolumeController` + `VolumeBubbleService` (floating overlay volume sliders — core new feature)
- [ ] **Phase 6** — `VolumeNotification` (persistent notification with inline +/- buttons)
- [ ] **Phase 7** — `BootReceiver` + `ServiceWatchdogWorker` (persistence/reliability)
- [ ] **Phase 8** — `OnboardingActivity` (guided permission wizard, incl. Do-Not-Disturb access)
- [ ] **Phase 9** — `MainActivity` Simple Mode UI
- [ ] **Phase 10** — `SettingsActivity` (sensitivity, tap count, exclusions, stream toggles, Hindi/English)
- [ ] **Phase 11** — `TestDemoActivity` + `OemGuidanceActivity` + `UninstallHelperActivity`
- [ ] **Phase 12** — End-to-end testing on physical device(s) + polish

---

## 6. Known Risks

- **TalkBack risk:** Accessibility service must only listen for tap gestures — never touch screen-reader/announcement APIs (a competing app triggered this accidentally).
- **Device Admin blocks normal uninstall** — Phase 11 handles safe revocation first.
- **Battery optimization/Doze** may suspend the shake service — must request `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` exemption during onboarding.
- **Silent ringer mode via code requires `ACCESS_NOTIFICATION_POLICY`** (Do-Not-Disturb access) on Android 7+, otherwise `setStreamVolume` throws `SecurityException` when Ring/Notification is set to 0.
- **App is now universal, not Nokia-only** — OEMs like Xiaomi/Samsung/Vivo/Oppo restrict background services more aggressively than stock Android One; Phase 11's OEM guidance screen exists specifically for this.
- True off-screen tap-to-wake is hardware/kernel-gated — shake is the reliable wake path; volume-button-wake only helps if the volume keys themselves still work.

---

## 7. Session Progress Tracker

| File | Status | Notes |
|---|---|---|
| AndroidManifest.xml | Not started | |
| build.gradle | Not started | |
| ScreenLockAdminReceiver.java | Not started | |
| TapAccessibilityService.java | Not started | |
| ShakeWakeService.java | Not started | |
| VolumeController.java | Not started | |
| VolumeBubbleService.java | Not started | |
| VolumeNotification.java | Not started | |
| BootReceiver.java | Not started | |
| ServiceWatchdogWorker.java | Not started | |
| OnboardingActivity.java | Not started | |
| MainActivity.java | Not started | |
| SettingsActivity.java | Not started | |
| TestDemoActivity.java | Not started | |
| OemGuidanceActivity.java | Not started | |
| UninstallHelperActivity.java | Not started | |

*Update this table's Status column as we complete each file — paste this doc back in a new session to resume exactly where we left off.*

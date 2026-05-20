# UPI Payment Speaker — User Stories

This document is the working backlog. Stories are grouped by the six milestones from `docs/PLAN.md` §15 and tagged with the source-tree areas they touch. Each story has explicit acceptance criteria so we can tell when it's done.

Conventions:
- **As a / I want / So that** — standard format.
- **AC** — acceptance criteria. All bullets must be true to mark the story done.
- **Refs** — links into `docs/PLAN.md` so the implementation rationale is one click away.

---

## Milestone 1 — Skeleton

### M1-S1 — Project scaffold compiles and installs
- **As a** developer
- **I want** a clean Android Gradle scaffold with `play` and `sideload` flavours
- **So that** I can start iterating without fighting build config
- **AC**
  - `./gradlew :app:assemblePlayDebug` and `:app:assembleSideloadDebug` both build green.
  - `applicationId` differs between flavours (`.play` vs `.sideload`).
  - Lint passes with no errors (warnings ok).
- **Refs** PLAN §10, §11

### M1-S2 — Persistent foreground service
- **As a** shopkeeper
- **I want** a sticky notification telling me the app is listening
- **So that** I trust it is running and the OS leaves it alone
- **AC**
  - Service has `foregroundServiceType="mediaPlayback"`.
  - Notification channel is `IMPORTANCE_LOW` (no sound).
  - Notification shows "Listening — N transactions today" and updates when N changes.
  - Tapping the notification opens MainActivity.
- **Refs** PLAN §5.1

### M1-S3 — Boot self-heal
- **As a** user
- **I want** the listener to restart automatically after a reboot or app update
- **So that** I do not have to remember to open the app each morning
- **AC**
  - `BootReceiver` fires on `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIME_SET`, `TIMEZONE_CHANGED`.
  - The foreground service is restarted within 5 s of the broadcast.
  - The watchdog alarm is re-scheduled.
- **Refs** PLAN §5.2, §5.7

---

## Milestone 2 — Path A end-to-end

### M2-S1 — Listen for notifications from PhonePe
- **As a** merchant
- **I want** to hear "X rupees received from Y" when a PhonePe payment lands
- **So that** I do not have to look at my phone for each transaction
- **AC**
  - `UpiNotificationListener` is enabled in system settings.
  - When PhonePe posts an "Received ₹X from Y" notification, TTS speaks the amount within 5 s.
  - Duplicate notifications within 60 s for the same transaction are not spoken twice.
- **Refs** PLAN §3 Path A, §7

### M2-S2 — Listen for notifications from Google Pay
- **As a** merchant — same as M2-S1 for GPay (`com.google.android.apps.nbu.paisa.user`).

### M2-S3 — TTS engine warm-up + audio focus
- **As a** user
- **I want** the first announcement of the day to be as fast as later ones
- **So that** I don't miss the very first payment because of a cold-start delay
- **AC**
  - `TtsEngine` runs a no-op utterance immediately after `INIT_SUCCESS`.
  - Announcements use `AudioFocusRequest` with `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`.
  - Focus is abandoned in `UtteranceProgressListener.onDone`.
- **Refs** PLAN §7, §8

### M2-S4 — Indian-numbering number-to-words (English)
- **As a** user
- **I want** "12500" spoken as "twelve thousand five hundred"
- **So that** the announcement sounds natural
- **AC**
  - 0 → "zero", 1 → "one", 19 → "nineteen", 21 → "twenty one"
  - 100 → "one hundred", 250 → "two hundred and fifty"
  - 12500 → "twelve thousand five hundred"
  - 100000 → "one lakh", 1500000 → "fifteen lakh"
  - 10000000 → "one crore"
  - Unit tests cover ≥ 25 cases.
- **Refs** PLAN §7 step 4

### M2-S5 — Transaction log
- **As a** user
- **I want** to see the last ~20 announcements on the home screen
- **So that** I can confirm the app actually heard what it said it heard
- **AC**
  - SQLite log stores every announcement with status (SPOKEN / FAILED / DUPLICATE).
  - Home screen lists the most recent rows.
  - Log entries are NOT included in Android cloud backup (PII).
- **Refs** PLAN §13

---

## Milestone 3 — Reliability layer

### M3-S1 — Watchdog heartbeat
- **As a** user on a Vivo device
- **I want** the app to notice when the OS has killed it and recover
- **So that** I don't have to re-open the app every few hours
- **AC**
  - `WatchdogReceiver` fires every 30 min via `AlarmManager.setAlarmClock()`.
  - On each tick: foreground service is verified running (and restarted if not); listener binding is verified (and rebind dance is run if not).
  - Heartbeat survives Doze (use of `setAlarmClock` is verified with `adb shell dumpsys deviceidle force-idle`).
- **Refs** PLAN §5.3

### M3-S2 — Restriction state detection
- **As a** user
- **I want** a clear in-app warning whenever a permission has been silently revoked
- **So that** I don't keep using the app thinking it works
- **AC**
  - `RestrictionDetector.snapshot()` returns true health iff all checks pass.
  - Failing checks render a red banner on the home screen with a "Fix" button.
  - The same warnings update the foreground notification text.
  - Re-checked on `onResume` of MainActivity and on every watchdog tick.
- **Refs** PLAN §5.8

### M3-S3 — Onboarding wizard
- **As a** new user
- **I want** a step-by-step setup that explains what each permission is for
- **So that** I am not overwhelmed and don't deny a critical permission by mistake
- **AC**
  - Wizard skips steps whose permission is already granted.
  - On `onResume` it re-evaluates and continues from the first ungranted step.
  - Last step is a "Test announcement" that proves end-to-end works.
  - All deep-link Intents fall back to `ACTION_APPLICATION_DETAILS_SETTINGS` on `ActivityNotFoundException`.
- **Refs** PLAN §9

### M3-S4 — Vivo (Funtouch + Origin) survival
- **As a** Vivo user
- **I want** explicit step-by-step instructions for autostart, background power consumption, and "lock in recents"
- **So that** the app actually survives iManager
- **AC**
  - `OemDetector.detect()` returns `VIVO_FUNTOUCH` or `VIVO_ORIGIN` correctly on each device variant.
  - Vivo step opens the autostart manager **and** the background power whitelist (two activities).
  - In-app instructions include the manual "lock in recents" step (with screenshot).
  - 24 h soak test on a real Vivo device passes — announcement within 5 s of trigger across the full period.
- **Refs** PLAN §5.7, §12

### M3-S5 — Battery-optimisation exemption
- **As a** user
- **I want** to grant battery-optimisation exemption from inside the app
- **So that** Doze does not defer our watchdog and announcements
- **AC**
  - The wizard launches the system whitelist dialog via `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.
  - Subsequent `PowerManager.isIgnoringBatteryOptimizations()` returns true.
- **Refs** PLAN §5.6

---

## Milestone 4 — Provider + language breadth

### M4-S1 — Paytm extractor
- **AC** Paytm notifications produce a Transaction with correct direction / amount / payer for ≥ 5 sample notifications captured from production.

### M4-S2 — BHIM, Amazon Pay, WhatsApp Pay extractors
- **AC** Each produces correct Transactions for ≥ 3 sample notifications.

### M4-S3 — Bank-app extractors (HDFC, ICICI, SBI, Axis)
- **AC** Each bank app's UPI credit notification is parsed correctly for ≥ 3 sample notifications.

### M4-S4 — Hindi TTS
- **AC** `Locale("hi","IN")` is selectable in settings; number-to-words produces correct Hindi ("दो सौ पचास").

### M4-S5 — Marathi / Tamil / Telugu / Kannada / Bengali TTS
- **AC** Each language can be selected; TTS speaks the announcement using the chosen locale; if voice data is missing, prompt the user to install it.

### M4-S6 — Bluetooth speaker routing
- **As a** shopkeeper using a small UPI BT speaker
- **I want** the announcement to play through the connected BT speaker, not the phone speaker
- **So that** customers can hear it
- **AC**
  - When a BT A2DP device is connected, TTS routes through it by default.
  - A toggle "Force phone speaker" overrides BT routing.

---

## Milestone 5 — Sideload SMS path

### M5-S1 — SMS_RECEIVED listener
- **As a** sideload user
- **I want** announcements to fire on bank SMS even when the bank app does not push a notification
- **AC**
  - Only present in the `sideload` flavour (manifest + source set).
  - Multi-PDU SMS are concatenated before parsing.
  - Same dedupe logic as Path A — same transaction never spoken twice.

### M5-S2 — Bank-SMS regex pack
- **AC** Coverage for the top 10 Indian banks; each bank has a unit test with ≥ 3 real-world SMS samples.

---

## Milestone 6 — Hardening

### M6-S1 — Long-soak test (7 days, real Vivo device)
- **AC**
  - 5 test UPI credits/day at random times.
  - ≥ 99 % announced within 5 s of notification appearing.
  - Persisted log matches the test driver's record.
- **Refs** PLAN §12

### M6-S2 — Doze + restricted-bucket drills
- **AC**
  - With `dumpsys deviceidle force-idle`, announcement still happens within 30 s.
  - With `am set-standby-bucket <pkg> restricted`, the warning banner appears.

### M6-S3 — Play Store submission
- **AC**
  - `play` variant uploads without policy errors.
  - Permissions declaration form completed for `QUERY_ALL_PACKAGES`, `SCHEDULE_EXACT_ALARM`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`.
  - 30-s screen recording attached demonstrating audible TTS announcements.

### M6-S4 — Privacy policy
- **AC**
  - Plain-language privacy policy hosted at a stable URL.
  - Linked from Step 1 of onboarding.
  - States explicitly: "All processing is on-device. We never send your transactions, notifications, or SMS to any server."

---

## Cross-cutting / non-goals (v1)

- **Cloud sync** of transaction log — explicitly out of scope.
- **UPI payment initiation** — out of scope.
- **Accessibility-service path (Path C)** — designed for but disabled by default; revisit only if Path A + B prove insufficient in field testing.
- **Compose UI migration** — deferred; ViewBinding is fine for v1.
- **Hilt DI** — deferred; manual instantiation through service constructors is fine until we add more modules.

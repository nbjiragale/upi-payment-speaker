# UPI Payment Speaker — Android Implementation Plan

A reliability-first plan for an Android app that listens for UPI payment SMS/notifications and speaks the amount aloud via TTS, with special focus on surviving Vivo (Funtouch / Origin OS) background restrictions.

> Source for OEM-survival mechanics: your attached spec `Android Background Execution & OEM Restrictions.pdf`. The plan below adapts those mechanics (the PDF's example was a water-reminder app) to an *event-driven* UPI listener.

---

## 1. Product scope & success criteria

**Core use case** — A shopkeeper / merchant keeps the phone in a drawer or pocket. When a UPI payment lands (via PhonePe, GPay, Paytm, BHIM, bank app, or SMS from the bank), the phone says *"Two hundred and fifty rupees received from Ramesh"* through the speaker (built-in or paired Bluetooth speaker), even when:

- the screen is off,
- the app is not in the foreground / not in recents,
- the device has been idle for hours (Doze),
- the device is a Vivo / Xiaomi / Oppo with aggressive OEM killers.

**Non-goals** — initiating UPI payments, ledger / accounting, transaction history sync to cloud (those are optional later features). Reliability of the announcement is the only KPI for v1.

**Success metric** — In a 7-day test on a Vivo phone configured per the onboarding wizard, **≥ 99 %** of incoming UPI credits are announced within **5 seconds** of the notification appearing.

---

## 2. Why the PDF's playbook applies (and where it differs)

The PDF was written for a *time-triggered* reminder app — its hardest problem is waking the CPU at a precise time after long idle periods. Our problem is *event-triggered* — we wait for an external trigger (SMS or push notification) and react quickly.

| Concern | PDF (time-triggered) | UPI speaker (event-triggered) |
|---|---|---|
| Primary trigger | `AlarmManager.setAlarmClock()` self-rescheduling loop | OS-delivered `SMS_RECEIVED` broadcast **or** `NotificationListenerService` |
| Doze survival | Must fight Doze to wake at T+2h | OS already wakes the device when an SMS arrives or a high-importance notification posts |
| OEM-kill risk | High — app must self-reschedule | High but different — listener service is *bound by the system*; we must stop the OEM from force-stopping the app between events |
| Battery profile | Continuous wake-ups | Mostly idle; only brief CPU burst per transaction |

So we **keep almost everything in the PDF** (autostart whitelisting, deep-link onboarding, battery-opt exemption, BootReceiver, foreground service hygiene, self-healing on `BOOT_COMPLETED` / `MY_PACKAGE_REPLACED` / `TIME_SET` / `TIMEZONE_CHANGED`) and add two things specific to event-triggered apps:

1. A **NotificationListenerService** as the primary trigger.
2. A **watchdog alarm** (`setAlarmClock()` every 30 min) that doesn't *do* anything user-facing — it just checks that the listener is bound and the FG service is alive, rebinds / restarts if not. This is the same self-healing loop from the PDF, repurposed as a heartbeat.

---

## 3. Recommended trigger architecture (defence in depth)

Use **multiple independent input paths**. Any one of them is enough to announce a payment; together they make missed payments very unlikely.

### Path A — `NotificationListenerService` (primary)
Listens to system notifications from PhonePe, GPay, Paytm, BHIM, bank apps, and Gmail. Pros:
- Doesn't need `RECEIVE_SMS`, so Play Store distribution is possible (see §6).
- One permission ("Notification access") covers *all* providers.
- Catches the same notifications the user sees in their tray — robust to format changes in any one app.

Cons:
- On Vivo / MIUI / ColorOS the system *unbinds* the service if the app is force-stopped or removed from recents. Mitigation: §5.

### Path B — `SMS BroadcastReceiver` (secondary)
Listens for `android.provider.Telephony.SMS_RECEIVED` from the bank's SMS sender ID (e.g. `VK-HDFCBK`, `VM-SBIINB`). Pros:
- SMS broadcasts wake even cold-started / force-stopped apps in most OEM stacks (special-cased by the framework).
- Useful when the user has notifications muted for the UPI app but bank SMS still arrives.

Cons:
- Requires `RECEIVE_SMS` + `READ_SMS`, which **Google Play heavily restricts** — only default SMS / Phone / Assistant handlers qualify. We do **not** qualify, so Path B is only available for sideloaded distribution (direct APK, MDM, or alternative stores). Plan to ship a Play-Store build *without* SMS permissions and a sideload build *with* SMS support.

### Path C — `MyAccessibilityService` (escape hatch — optional)
Reads on-screen text from notification shade / status bar / app overlays. Powerful but:
- Accessibility services are heavily scrutinised by Play (declaration form required).
- Heavy battery cost (constant view-tree inspection).

Recommend keeping Path C **off by default**, behind a developer toggle, only for users who can't get Path A or B working.

### Path D — Direct integration with payment apps (not feasible)
PhonePe / GPay / Paytm do not expose a transaction webhook API to third-party apps on the same device. Out of scope.

> **Decision**: ship with Path A always on, Path B available only in the sideload build, Path C as an opt-in fallback.

---

## 4. Component map

```
┌──────────────────────────────────────────────────────────────────────┐
│  AndroidManifest                                                     │
│   - UpiNotificationListener  (NotificationListenerService)           │
│   - SmsReceiver              (BroadcastReceiver, SMS_RECEIVED)       │
│   - BootReceiver             (BOOT_COMPLETED / MY_PACKAGE_REPLACED / │
│                               TIME_SET / TIMEZONE_CHANGED)           │
│   - WatchdogReceiver         (alarmClock heartbeat)                  │
│   - SpeakerForegroundService (FGS, type=mediaPlayback|specialUse)    │
└──────────────────────────────────────────────────────────────────────┘
              │ binds at boot / first launch
              ▼
┌──────────────────────────────────────────────────────────────────────┐
│  SpeakerForegroundService  (the only process holding state)          │
│   - TtsEngine (lazy init + warm-up on first launch)                  │
│   - TransactionParser  (regex pipelines per provider + per bank)     │
│   - AnnouncementQueue  (serial queue → AudioFocus → speak)           │
│   - RestrictionDetector (re-checks bucket / battery-opt / autostart) │
│   - WatchdogScheduler   (setAlarmClock heartbeat, every 30 min)      │
└──────────────────────────────────────────────────────────────────────┘
```

Everything that does real work runs **inside** the foreground service so we have a single sticky notification, a single TTS instance, and a single in-memory queue. The receivers are thin: they enqueue an `Intent` to the FGS and return.

---

## 5. Reliability mechanics (Vivo-first)

This is the heart of the plan. Apply *every* one of these — none alone is sufficient on Vivo.

### 5.1 Foreground service that the user actually wants
- `android:foregroundServiceType="mediaPlayback"` — most honest type for a TTS speaker and survives best because the OS treats `mediaPlayback` FGS as user-visible. Falls back to `specialUse` with a written justification in `<property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE" />` if Play review pushes back on `mediaPlayback`.
- Persistent notification text: *"Listening for UPI payments — N today"* with a one-tap **Mute / Resume** action so the user perceives value (and OEMs leave it alone).
- Channel `IMPORTANCE_LOW` so the icon is in the status bar but no sound; we play TTS separately.

### 5.2 Self-healing NotificationListener
On any of (a) FGS start, (b) `BOOT_COMPLETED`, (c) `MY_PACKAGE_REPLACED`, (d) watchdog heartbeat — run the standard rebind dance:

```
PackageManager.setComponentEnabledSetting(
    UpiNotificationListener,
    COMPONENT_ENABLED_STATE_DISABLED, DONT_KILL_APP)
PackageManager.setComponentEnabledSetting(
    UpiNotificationListener,
    COMPONENT_ENABLED_STATE_ENABLED, DONT_KILL_APP)
if (SDK_INT >= N) NotificationListenerService.requestRebind(componentName)
```

This is the documented workaround for the MIUI / Funtouch / ColorOS unbind bug.

### 5.3 Watchdog alarm (heartbeat)
A `setAlarmClock()` alarm every **30 min** (well above the 15-min throttle floor) that wakes a tiny receiver. Per the PDF: `setAlarmClock()` forces the platform to exit Doze. The receiver:
1. Verifies `SpeakerForegroundService` is running (`ActivityManager.getRunningServices`) — restart if not.
2. Verifies the listener is bound (`NotificationManager.getEnabledNotificationListeners()` + a ping-flag the listener writes to DataStore on `onListenerConnected()`); rebinds if stale.
3. Re-schedules the next heartbeat.

This is exactly the "self-healing scheduling loop" diagram in the PDF — repurposed as a liveness check instead of a notification trigger.

### 5.4 Dual-alarm pre-warm — *not needed*
The PDF's pre-warm-then-exact-alarm trick exists to defeat Doze drift on time-triggered alarms. SMS and notification broadcasts are themselves wake-ups, so we don't need it for the trigger path. We *do* still use `setAlarmClock()` for the watchdog because the watchdog *is* a time-triggered job.

### 5.5 Wake locks — short and timed
Inside the FGS `onStartCommand`, acquire `PARTIAL_WAKE_LOCK` for max 15 s, release after the utterance finishes (`UtteranceProgressListener.onDone`). Never hold a wake lock indefinitely.

### 5.6 Battery optimisation exemption
Request `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` during onboarding. Without this, Doze deep mode will still defer the watchdog alarm.

### 5.7 OEM-specific deep-link onboarding
Reuse the deep-link matrix from page 9 of your PDF. For Vivo specifically — open all three:

| Setting | Package | Component |
|---|---|---|
| Background Startup Manager (autostart) | `com.vivo.permissionmanager` | `.activity.BgStartUpManagerActivity` |
| High Background Power Consumption whitelist | `com.iqoo.secure` | `.ui.phoneoptimize.AddWhiteListActivity` |
| Battery optimisation = "Don't optimise" | system `Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | — |

Additionally — and these don't have stable deep-link components, so we surface a guided screen with screenshots:
- Settings → Battery → **Background power consumption management** → set our app to **Allow background high power consumption**.
- Recent apps → swipe down on our card → tap the **lock icon** so iManager doesn't sweep it.
- Settings → Apps → our app → **Permissions → Notifications** → enable, then **Display over other apps** if Path C is enabled.
- Settings → Notification & status bar → **Notification access** → enable our listener.

### 5.8 Detect that survival actually worked
At every FGS start, run a `RestrictionDetector` based on the PDF's `BackgroundRestrictionDetector` example:
- `canScheduleExactAlarms()`,
- `PowerManager.isIgnoringBatteryOptimizations()`,
- `UsageStatsManager.getAppStandbyBucket()` (warn if `STANDBY_BUCKET_RESTRICTED == 45`),
- Listener-bound flag from DataStore.

Surface an in-app banner *and* update the FGS notification text to *"⚠ Battery optimisation re-enabled — tap to fix"* whenever any check fails. The PDF's note that OEMs can silently re-enable restrictions is real — re-check on every `onResume` of the main Activity and on every watchdog tick.

### 5.9 Don't trip BAL restrictions (Android 14–16)
The PDF flags this. Anywhere we build a `PendingIntent` that may launch an Activity from the background:
```
PendingIntent.getActivity(..., flags)
    .send(ActivityOptions.makeBasic()
        .setPendingIntentBackgroundActivityStartMode(MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
        .toBundle())
```
But almost everything in our flow is *notify + speak*, no Activity launches. The one exception: tap-the-notification-to-open-app → that's a foreground launch, fine.

### 5.10 FGS type timeouts (Android 15+)
The PDF mentions `dataSync` = 6 h and `mediaProcessing` = 24 h. `mediaPlayback` does **not** have a system-imposed time limit so long as we are genuinely playing audio. We satisfy this by playing TTS through the media stream when announcing. If we ship with `specialUse`, document the use case in the manifest property.

---

## 6. Permissions matrix

| Permission | Required for | Build variant |
|---|---|---|
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Path A (primary trigger) | both |
| `POST_NOTIFICATIONS` (Android 13+) | sticky FGS notification | both |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK` (Android 14+) | FGS | both |
| `SCHEDULE_EXACT_ALARM` | watchdog heartbeat — request via "Alarms & reminders" settings, per the PDF | both |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | bypass Doze deferral | both |
| `RECEIVE_BOOT_COMPLETED` | restart on reboot | both |
| `WAKE_LOCK` | hold CPU briefly during TTS | both |
| `MODIFY_AUDIO_SETTINGS` / `BLUETOOTH_CONNECT` | route TTS to BT speaker | both |
| `RECEIVE_SMS` + `READ_SMS` | Path B | **sideload only** |
| `BIND_ACCESSIBILITY_SERVICE` | Path C | both, but disabled by default |
| `QUERY_ALL_PACKAGES` | detecting installed UPI apps for parser routing | both — declare use case for Play |

Do **not** declare `USE_EXACT_ALARM` — per the PDF and Play policy, that's restricted to alarm clocks / calendars and will be rejected for our use case. Use `SCHEDULE_EXACT_ALARM` with the standard "Alarms & reminders" settings deep-link and graceful fallback to `setAndAllowWhileIdle`.

---

## 7. The announcement pipeline

When a trigger fires (Path A / B / C):

1. **Dedupe** — hash `(provider, transactionId | smsBody hash, amount, timestamp)`; ignore if already seen within 60 s. Both SMS and the in-app push notification often arrive for the same transaction.
2. **Parse** — provider-specific extractor:
   - PhonePe / GPay / Paytm — title + bigText contain `"Received ₹250 from Ramesh"` patterns.
   - Bank SMS — regex per sender ID, e.g. HDFC: `Rs\.([\d,]+)\s+credited.*UPI`.
   - Cache the *last-seen* notification per provider package and diff on `extras["android.bigText"]`.
3. **Filter** — only **credits** to the user's account by default (configurable: also announce debits). Drop OTP messages.
4. **Build utterance** — Locale-aware: English / Hindi / Marathi / Tamil / Telugu / Kannada / Bengali (`Locale("hi","IN")`). Number-to-words for Indian numbering system ("250" → "दो सौ पचास" / "two hundred and fifty"). Truncate payer name to first word.
5. **Enqueue + speak** — `AudioFocusRequest` (transient may-duck on `STREAM_MUSIC`), `TextToSpeech.speak(QUEUE_ADD)`, release focus on `onDone`. Serial queue prevents two announcements stepping on each other.
6. **Record** — append to local SQLite log (Room) with status `SPOKEN` / `FAILED_TTS` / `FAILED_AUDIO_FOCUS`. Persisted log is also our debug evidence when the user reports "it didn't speak".

---

## 8. TTS engine considerations

- Initialise `TextToSpeech` once inside the FGS; keep alive.
- Pick the default engine but expose a setting to switch to Google TTS (best Hindi voices) — install link if missing.
- Pre-warm on FGS start: `tts.speak(" ", QUEUE_FLUSH, params, "warmup")` so the first real utterance isn't delayed by engine cold-start.
- Audio routing — when a Bluetooth speaker is connected, route through it automatically; expose a "Always use speaker" toggle to override (use `AudioManager.setSpeakerphoneOn` only when no BT device is connected — manipulating routing while BT is connected is unreliable across OEMs).
- Volume control — respect Do Not Disturb by default; expose an "override DND" toggle (requires `ACCESS_NOTIFICATION_POLICY`).

---

## 9. Onboarding wizard (UX)

Follow the PDF's stepwise pattern exactly — never ask for everything at once. Verify each step in `onResume()` and let the user retry.

```
Step 1  Welcome + "Why we need these permissions" explainer
Step 2  Grant Notification Access (Path A)        [system settings]
Step 3  Disable Battery Optimisation              [system dialog]
Step 4  Allow Exact Alarms                        [system settings]
Step 5  (Vivo / Xiaomi / Oppo / Samsung detected) Manufacturer-specific steps:
        - Autostart / Auto-launch
        - Background high power consumption whitelist
        - Lock in recent apps
        Each step shows a short GIF / screenshot of the actual setting screen.
Step 6  Test announcement   →   the app speaks "Test: two hundred and fifty rupees received"
        ↑ this is the demo that proves end-to-end works AND triggers the OS to remember our audio FGS.
Step 7  Done. Sticky notification appears in tray.
```

Detect the OEM with `Build.MANUFACTURER` and `Build.BRAND`. For Vivo also check `Build.VERSION.INCREMENTAL` for Funtouch vs Origin OS variants (the deep-link components are slightly different on Origin OS; fall back to `Settings.ACTION_APPLICATION_DETAILS_SETTINGS` if the explicit Intent throws `ActivityNotFoundException`).

Re-run the wizard whenever the watchdog detects a regression (the PDF's "silently re-enabled by Android" footnote 41 is a real risk on OEMs).

---

## 10. Project structure

```
app/
 ├─ ui/                                    Jetpack Compose
 │   ├─ onboarding/                        per-OEM wizard
 │   ├─ home/                              status + transaction log
 │   └─ settings/                          languages, audio, advanced
 ├─ service/
 │   ├─ SpeakerForegroundService.kt        the one long-lived service
 │   ├─ UpiNotificationListener.kt         Path A
 │   ├─ SmsReceiver.kt                     Path B  (sideload variant)
 │   └─ UpiAccessibilityService.kt         Path C  (opt-in)
 ├─ alarm/
 │   ├─ BootReceiver.kt                    BOOT_COMPLETED / MY_PACKAGE_REPLACED / TIME_*
 │   ├─ WatchdogScheduler.kt               setAlarmClock heartbeat
 │   └─ WatchdogReceiver.kt                liveness check + rebind
 ├─ tts/
 │   ├─ TtsEngine.kt
 │   ├─ NumberToWords.kt                   Indian numbering, multi-lingual
 │   └─ AnnouncementQueue.kt
 ├─ parser/
 │   ├─ Transaction.kt                     sealed class CREDIT / DEBIT
 │   ├─ Providers.kt                       per-package extractors
 │   └─ BankSmsRegex.kt                    by sender-ID
 ├─ data/
 │   ├─ TransactionLog (Room)
 │   ├─ Settings (DataStore)
 │   └─ RestrictionState (DataStore)
 ├─ oem/
 │   ├─ OemDetector.kt                     Build.MANUFACTURER / BRAND
 │   ├─ DeepLinks.kt                       PDF page-9 matrix
 │   └─ RestrictionDetector.kt             PDF page-7/8 detector
 └─ build variants:
     ├─ play     (no RECEIVE_SMS)
     └─ sideload (with RECEIVE_SMS, optional)
```

**Tech choices** — Kotlin, AGP latest, `minSdk = 26` (Oreo), `targetSdk = 36` (Android 16), Jetpack Compose, Hilt, Room, DataStore, kotlinx-coroutines, kotlinx-serialization, Timber.

---

## 11. Distribution strategy

| Channel | Build | Notes |
|---|---|---|
| Google Play | `play` variant | No SMS perms. Submit foreground-service-type declaration. Expect review questions on `mediaPlayback` justification; reply with the "we play audible TTS announcements" rationale, attach screen recording. |
| Direct APK (recommended for merchants) | `sideload` variant | Includes SMS path. Distribute via your own website / WhatsApp / merchant onboarding kit. |
| MDM / enterprise | `sideload` variant signed with the org key | Highest reliability — MDM policies can pre-whitelist autostart on Vivo. |

---

## 12. Testing matrix

A bug only matters if it survives the OEM. Always test on real hardware — Vivo emulator images don't have iManager.

| Phase | Tests |
|---|---|
| **Unit** | TransactionParser per provider × per language × number-to-words edge cases (₹1, ₹1.5, ₹1,00,000, ₹0). |
| **Integration (foreground)** | App in foreground → manually post a fake notification via `adb shell cmd notification post` mimicking PhonePe / GPay payload → assert TTS spoke. |
| **Integration (background)** | Same, app swiped from recents. |
| **OEM matrix** | Real-device gauntlet — one device per row, all of these must pass: |
| | Vivo (Funtouch ≥14) — primary target |
| | Vivo (Origin OS) |
| | Xiaomi (HyperOS / MIUI 14+) |
| | Oppo / Realme (ColorOS 14+) |
| | Samsung (OneUI 6+) |
| | Pixel / Stock Android 15 |
| **Long-soak** | 7-day deployment, phone idle in drawer, 5 test UPI credits per day at random times — assert ≥ 99 % announced ≤ 5 s after notification, persisted log matches. |
| **Doze-forcing** | `adb shell dumpsys deviceidle force-idle` → fire trigger → must still announce within 30 s (watchdog interval). |
| **Bucket-restricted** | `adb shell am set-standby-bucket <pkg> restricted` → must detect the bucket and surface the warning banner. |
| **Reboot** | Power-cycle → assert FGS comes back within 2 min on each OEM. |
| **App update** | Push a new APK over the running app → assert listener rebinds and watchdog reschedules. |
| **Force-stop recovery** | `adb shell am force-stop <pkg>` → user must reopen the app once; then it should self-heal (this is *expected* behaviour per AOSP — document it in the wizard). |

Build a tiny **fake-payment generator** companion app for QA — it posts notifications with the exact extras structure of each UPI provider, no real money needed.

---

## 13. Privacy & store policy

- Bank SMS / notifications contain PII. **Process on-device only**, never POST anywhere unless the user opts into cloud sync (out of scope for v1).
- Add a clear privacy policy URL, list permissions and *why*, and link it from Step 1 of onboarding.
- For Play submission: write a separate "use of foreground service" doc and a "use of notification listener" doc explaining the use case in one paragraph each. Both are reviewer-friendly.

---

## 14. Risks & open questions

1. **NotificationListenerService still gets unbound on a force-stopped app on Vivo even after the rebind dance.** No software fix exists — only the "lock in recents" + autostart + battery whitelist combo works. Set realistic user expectations in the onboarding wizard.
2. **Origin OS 4 (newer Vivo flagships)** — some deep-link Intents from the PDF target the old `com.vivo.permissionmanager` package which has been renamed in Origin OS. Implement a graceful fallback chain: try explicit Intent → on `ActivityNotFoundException`, try the next known package → finally fall back to the generic `Settings.ACTION_APPLICATION_DETAILS_SETTINGS`.
3. **Play Store mediaPlayback rejection risk.** Mitigation: pre-record a 30-second screen capture demonstrating audible TTS playback, attach it to the declaration form.
4. **Some banks send transaction SMS only (no push notification).** Captured by Path B in the sideload build; in the Play build we miss these and need to recommend the user install the bank's official app and enable its notifications.
5. **TTS engine missing for regional languages.** Detect on first run, prompt the user to install Google TTS.
6. **Background mediaPlayback FGS playing through Bluetooth conflicting with the user's music app.** Mitigation: use transient audio focus and `STREAM_NOTIFICATION` as the route, not `STREAM_MUSIC`, for the *announcement itself*.

---

## 15. Phased delivery

**Milestone 1 — Skeleton (1 week)**
- Project scaffold, build variants, Compose shell, Hilt wiring.
- FGS that posts a sticky "Listening…" notification.
- BootReceiver re-launching the FGS.

**Milestone 2 — Path A end-to-end (1 week)**
- `NotificationListenerService` + rebind dance.
- TransactionParser for PhonePe and GPay only.
- TTS engine + Hindi + English + number-to-words.
- Announce on real PhonePe / GPay credits on stock Android.

**Milestone 3 — Reliability layer (1.5 weeks)**
- Watchdog + setAlarmClock heartbeat.
- RestrictionDetector + banner.
- Full onboarding wizard with per-OEM deep links (Vivo first).
- Battery-opt exemption.

**Milestone 4 — Provider + language breadth (1 week)**
- Add Paytm, BHIM, SBI YONO, HDFC, ICICI, Axis bank apps.
- Add Marathi, Tamil, Telugu, Kannada, Bengali.
- Audio routing (BT vs speaker).

**Milestone 5 — Sideload SMS path (3 days)**
- Path B BroadcastReceiver behind the `sideload` build flavour.
- Bank-sender-ID regex pack.

**Milestone 6 — Hardening (1 week)**
- Long-soak on Vivo + Xiaomi + Oppo + Samsung.
- Fix issues found in OEM matrix.
- Play Console submission with declarations.

Total ≈ 6 weeks for a polished v1 (assuming a single experienced Android engineer and access to all OEM test devices).

---

## 16. What to do *before* writing a line of code

1. **Acquire test hardware** — at minimum: one Vivo Funtouch device, one Vivo Origin OS device, one Xiaomi (HyperOS), one Oppo / Realme (ColorOS), one Samsung (OneUI), one Pixel. Without real devices this app cannot be validated.
2. **Buy or simulate UPI traffic** — set up a sender account that can push ≥ 5 test credits/day to your test phone, or write the fake-notification generator described in §12.
3. **Confirm Play strategy now, not later** — decide whether you want SMS support enough to ship a sideload-only build. That decision changes the manifest and review process and is easier to make up front.
4. **Decide on monetisation early** — if this becomes a paid/subscription app, RECEIVE_SMS-bearing builds cannot be on Play even with payment justification. Plan distribution accordingly.

---

## Appendix A — Direct mapping back to the PDF

| PDF section | How we use it |
|---|---|
| Exact alarm framework (page 1) | Watchdog uses `setAlarmClock()`, declares `SCHEDULE_EXACT_ALARM`, falls back to `setAndAllowWhileIdle`. Never declares `USE_EXACT_ALARM`. |
| BAL restrictions (page 1–2) | Anywhere we send a PendingIntent that could launch an Activity, opt into MODE_BACKGROUND_ACTIVITY_START_ALLOWED. |
| FGS types and timeouts (page 2) | `mediaPlayback` (primary) / `specialUse` (fallback). No `dataSync` or `mediaProcessing`. |
| Doze & Standby (page 2) | Battery-opt exemption + `setAlarmClock` for heartbeat. |
| OEM killers — Vivo / Xiaomi / Oppo / Samsung (page 3–5) | RestrictionDetector + onboarding deep links + "lock in recents" instructions. |
| Workarounds: CDM, SyncAdapters, invisible UI (page 6) | All rejected as primary mechanism, exactly as the PDF recommends. |
| BootReceiver, MY_PACKAGE_REPLACED, TIME_SET (page 7) | Implemented verbatim. |
| Dual-alarm pre-warm (page 7) | Not used — we are event-triggered. Documented in §5.4. |
| RestrictionDetector example code (page 7–8) | Adapted into our `oem.RestrictionDetector`. |
| Deep-link Intents (page 9) | Adapted into `oem.DeepLinks` with fallback chain. |
| Onboarding wizard pattern (page 9–10) | Adapted to UPI use case in §9. |
| Self-healing scheduling loop (page 10) | Repurposed as watchdog heartbeat in §5.3. |
| Kotlin code blueprints (page 11–17) | Same code shape; receivers + scheduler + receiver + notification dispatcher. |

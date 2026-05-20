# UPI Payment Speaker

Android app that **speaks the amount aloud** when a UPI payment notification or SMS arrives — so a shopkeeper can hear "two hundred and fifty rupees received" without looking at the phone.

Reliability-first design. Survives **Doze, App Standby, and OEM-specific killers** (Vivo iManager / Xiaomi Power Keeper / Oppo App Quick Freeze / Samsung Sleeping Apps). See `docs/PLAN.md` for the full implementation specification and `docs/USER_STORIES.md` for the milestone-grouped backlog.

## Architecture in one paragraph

A single foreground service (`type=mediaPlayback`) holds the TTS engine and announcement queue. A `NotificationListenerService` (Path A) and an optional sideload-only `SmsReceiver` (Path B) feed it parsed transactions. A `setAlarmClock` watchdog ticks every 30 minutes to verify the listener is bound and the FGS is alive; a `BootReceiver` self-heals after reboot, app update, or clock change. An onboarding wizard deep-links the user into the per-OEM autostart, battery-whitelist, and notification-access settings — with `ActivityNotFoundException` fallbacks for ROM variants that have moved the components (e.g. Origin OS 4).

## Build variants

| Variant | What it includes | Distribution |
|---|---|---|
| `play` | Path A only (no SMS perms) | Google Play |
| `sideload` | Path A + Path B (`RECEIVE_SMS`, `SmsReceiver`) | Direct APK / MDM |

```bash
./gradlew :app:assemblePlayDebug
./gradlew :app:assembleSideloadDebug
```

## Layout

```
app/
 ├─ src/main/                 — both flavours
 │   └─ java/.../upispeaker/
 │       ├─ service/          SpeakerForegroundService, UpiNotificationListener
 │       ├─ alarm/            BootReceiver, WatchdogReceiver, WatchdogScheduler
 │       ├─ tts/              TtsEngine, AnnouncementQueue, NumberToWords
 │       ├─ parser/           Transaction, Providers (per-app extractors)
 │       ├─ data/             Settings, TransactionLog
 │       ├─ oem/              OemDetector, DeepLinks, RestrictionDetector
 │       └─ ui/               MainActivity, OnboardingActivity
 ├─ src/play/                 — Play-flavour manifest stub (no SMS perms)
 └─ src/sideload/             — Sideload-flavour manifest + SmsReceiver source
```

## Status

Scaffold (Milestone 1). Path A extraction logic is stubbed — see `TODO(M2)` markers in `parser/Providers.kt`. Most pieces are in place and the project compiles, but real transaction parsing and TTS warm-up tuning land in later milestones.

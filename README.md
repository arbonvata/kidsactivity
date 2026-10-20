# Kids Tracker Activity

**Version 1.0** · minSdk 32 · [Changelog](CHANGELOG.md)

An Android app for parents to track school schedules and extracurriculars for
multiple children — with reminders that fire before activities start and after
school ends (pickup time).

## About the app

Every kid gets their own schedule built from two kinds of activities:

- **School activities** — marked with a flag, they always repeat weekly and
  remind you *after* they end (e.g. "School pickup — Leo: School ended 10 min
  ago"). Their end time can be overridden per day.
- **Regular activities** — repeat weekly on the selected weekdays
  (e.g. football every Monday 17:00) and remind you *before* they start.
  Unchecking "Repeats weekly" turns one into a **one-off** that belongs to a
  single calendar week, chosen with a date picker.

Anything can be muted (alarms off) without deleting it, and the schedule views
keep showing muted activities dimmed.

### Features

- **Today / Week / Month / Daily views** across all kids, updated live
  (reactive ObjectBox queries — no manual refresh anywhere).
- **School-day overrides** — mark No-School days (holidays, trips) and custom
  early-release end times on a calendar; overrides hide or shift only school
  activities for that kid and day.
- **Pickup arrangements** — optional location, responsible adult and notes per
  activity, shown on Today and in expanded reminder notifications, and backed up.
- **Individual-date changes** — in Edit Activity, cancel a scheduled date or
  change its start/end times, then Save. Restore the usual schedule by removing
  the exception. Changes apply to views, conflicts, reminders and JSON backups.
- **Conflict detection** — same-kid activity overlaps across the next 30 days,
  with one tap from a conflict to the editor. Respects one-off weeks and
  school overrides.
- **Exact alarms** — `AlarmManager.setExactAndAllowWhileIdle()`, rescheduled
  on boot (`BootReceiver`) and re-synced reactively whenever kids, activities,
  overrides or timing settings change. Notifications deep-link into the
  activity's edit screen. The channel plays on the alarm audio stream with
  vibration.
- **Backup & restore** — export all data to JSON, share it (WhatsApp, email,
  …), and restore by merging or replacing, all in one transaction.
- **Settings** — notification / exact-alarm / battery-optimization permission
  status with one-tap requests, and sliders for reminder offsets
  (school: 5–30 min after end, activities: 5–60 min before start), persisted
  in DataStore.
- **Localized** — the UI (including the reminder notifications, formatted
  when they fire) is translated into Danish, German, Spanish, French and
  Swedish. All copy lives in `res/values/strings*.xml` (~200 strings, plurals
  included); drop in a `values-<locale>/` folder to translate another
  language.

## Tech stack

| Layer | Choice |
| --- | --- |
| UI | Kotlin, Jetpack Compose, Material 3 |
| Architecture | MVVM — one ViewModel per screen; stateful parent + stateless content composables (previewable without fakes) |
| DI | Koin |
| Persistence | ObjectBox (via `legacy-kapt` on AGP 9), DataStore for alarm settings |
| Background | AlarmManager exact alarms + BootReceiver, notification deep links |
| Navigation | Navigation Compose |
| Testing | JUnit Jupiter, MockK, kotlinx-coroutines-test; ktlint for style |

Min SDK 32 (Android 12L), target/compile SDK 37, JVM 11.

## Project structure

```
app/src/main/java/com/arbonvata/kidstrackeractivity/
├── alarms/        # scheduling, notification channel, boot receiver, sync coordinator
├── data/
│   ├── objectbox/ # Kid, Activity, DailySchedule entities
│   ├── repository/# Kid / Activity / ScheduleOverride / AlarmSettings / Backup repos
│   └── Conflict.kt, ActivityOccurrence.kt  # occurrence expansion + conflict detection (pure)
├── di/            # Koin module
└── presentation/  # screens (Compose), one ViewModel per screen, shared formatters
```

Core scheduling logic lives in `data/ActivityOccurrence.kt`
(`expandActivitiesForDateRange`) — the single expansion used by every view,
the alarm scheduler and conflict detection, so weekly/one-off windows and
school overrides behave identically everywhere.

## Building and testing

```bash
./gradlew assembleDebug          # build the app
./gradlew testDebugUnitTest      # unit tests
./gradlew :app:ktlintCheck       # style (must stay green)
```

Open the project in Android Studio, pick a device or emulator (Android 12L+),
and run the `app` configuration.

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
- **Calendar sharing and import** — open More → Share / Export. Select a child
  and share/save an `.ics` file. To share just one saved activity, open its
  editor and tap Share this activity as calendar. Weekly activities include
  their repeating series and exceptions. On the receiving device, save the file, open
  the same screen and choose Import calendar. Preview the activities and select
  an existing child or enter a new child’s name before confirming. Imports add
  activities and skip matching duplicates; they never replace existing data.
  Weekly patterns, locations, pickup details, reminder mute flags and date
  exceptions transfer between copies of this app. School-day overrides become
  exceptions on the exported school activities. Weekly sharing starts from the
  export date (or an imported future start date); dated activities retain their
  dates. Reminder timing uses the receiving device’s settings.
  This is file sharing rather than ongoing synchronization. The importer
  supports timed single events and weekly interval-1 rules, including finite
  COUNT/UNTIL rules (expanded into dated activities). Other recurrence patterns,
  all-day/overnight events and changes that move an occurrence to another day
  are rejected before any data is written. Recurring events must be floating
  local times or use the device’s IANA time zone; UTC/named-zone single events
  convert to local time. UTF-8 files are limited to 2 MB and 2,000 events.
  Format reference: [iCalendar (RFC 5545)](https://www.rfc-editor.org/rfc/rfc5545).
- **Duplicate and copy** — use Duplicate activity in Edit Activity to copy a
  saved activity to the same child or a sibling, with an editable name. Use
  Copy schedule on a child’s screen to copy all activities to another child.
  Weekly patterns and pickup details are preserved; one-offs move to a chosen
  week. Date exceptions and school overrides are not copied.
- **Family agenda widget** — add it from your launcher’s widget picker. Shows
  the next activity or school pickup and up to four remaining activities today.
  Tap the next activity to edit, or Refresh to update. Updates live while the
  app process runs and periodically (about 30 minutes) otherwise.
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

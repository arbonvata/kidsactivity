# Changelog

The app version lives in `versionName` in `app/build.gradle.kts` (shown on the
in-app About screen); this file records what each release shipped.

## Unreleased

- **Added**: resizable Family agenda home-screen widget with next activity or
  school pickup, remaining daily agenda, live updates and manual refresh.

- **Added**: activity locations, pickup adults and notes, displayed on Today
  and in expanded reminders and preserved in backups.

- **Added**: cancel or change the times of an individual activity date without
  changing its weekly schedule; exceptions are included in backups.

- **Fixed**: the Save button on the Add/Edit Activity screen no longer falls
  off the bottom on small screens (or with large font sizes) when a one-off
  non-school activity is being edited — the form scrolls and Save stays
  pinned above it.
- **Fixed**: reminders no longer silently stop after a week without opening
  the app — a rolling "keeper" alarm now refreshes the schedule just before
  the 7-day horizon expires, so reminders keep themselves alive without an
  app launch or reboot.
- **Fixed**: reminder notifications are formatted at fire time, so they show
  in the app's language and pick up kid/activity renames made after
  scheduling, instead of the English text captured when the alarm was set.
- **Fixed**: alarms already queued in the system are cancelled when the
  exact-alarm permission is missing, instead of continuing to fire with
  stale data — and they are rescheduled as soon as the permission is granted
  again, even while the app stays open.
- **Fixed**: conflict detection now reports every overlapping pair on a
  kid's day, not just schedule-adjacent ones — a long activity overlapping
  two shorter ones no longer hides one of the clashes.
- **Fixed**: restoring a backup validates every time and date before
  touching the database — malformed rows are rejected up front instead of
  crashing every screen after a "successful" import, and corrupt rows from
  older files are skipped rather than crashing the schedule views.
- **Fixed**: the Add/Edit Activity form rejects an end time at or before the
  start time.
- **Fixed**: restoring a backup that omits the "repeats weekly" flag no
  longer turns weekly activities into one-offs that vanish after their week.
- **Fixed**: the app now actually applies its own theme — dark mode and
  dynamic color work at runtime instead of only in previews.
- **Added**: Danish, German, Spanish, French and Swedish translations for
  every screen, including the reminder notifications and restore messages.
- **Changed**: the Today tab refreshes once a minute, so it keeps up across
  midnight and the "up next" card stays current while the screen is open.

## 1.0 — 2026-09-27

First release.

- **Kids**: add, edit and delete kids; deleting cascades to their activities
  and school-day overrides.
- **Activities**: weekly recurrence on selected weekdays; school-activity flag
  (weekly by nature, reminds after school ends); one-off mode with a
  calendar-week date picker; mute/unmute without deleting.
- **Schedule views**: Today dashboard with up-next highlight and first-run
  welcome, Week, Month and Daily (per kid selection) — all live-updating.
- **School overrides**: No-School days and custom end times marked on a
  monthly calendar; they hide or shift only school activities.
- **Conflicts**: same-kid overlaps across the next 30 days, respecting
  one-off weeks and school overrides; one tap to the editor.
- **Reminders**: exact alarms (alarm audio stream + vibration), configurable
  offsets (school end +5–30 min, activity start −5–60 min), rescheduled on
  boot and on any data change; notifications deep-link to the activity; a
  Today-dashboard banner appears while the required permissions (exact
  alarms, notifications) are missing, and a one-time dialog walks through
  every permission reminders need — notifications, exact alarms and battery
  optimization — in one guided chain of system surfaces.
- **Backup & restore**: JSON export, share, and merge-or-replace import.
- **Settings**: notification / exact-alarm / battery-optimization permission
  status with one-tap requests; routes to the system notification settings
  when the permission dialog was permanently denied and states plainly that
  reminders won't fire until the required permissions are granted;
  reminder-timing sliders (persisted).
- **About screen** and localization-ready English strings (~200).

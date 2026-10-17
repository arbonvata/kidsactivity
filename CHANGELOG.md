# Changelog

The app version lives in `versionName` in `app/build.gradle.kts` (shown on the
in-app About screen); this file records what each release shipped.

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

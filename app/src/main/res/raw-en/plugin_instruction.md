3-Chime Alarm is the alarm plugin of AutoJs6 and also an alarm clock app that works on its own without AutoJs6. Alarms repeat by RFC 5545 calendar rules (for example the last Friday of every month, or every 3 days from a given date); a ringing alarm can read text aloud, start a script or notify a running script. Timers and the stopwatch, holiday skipping and dismiss challenges come in later versions.

> Version 1.0.0 provides alarms, the plugin screens, the plugin tools and the `alarms` script module. Enabling the plugin in the Plugin Center, the plugin tools and the `alarms` module need AutoJs6 6.8.1 (internal build 6641) or later; with older AutoJs6 versions (including 6.8.0) the plugin works only as a standalone alarm clock app. See the roadmap for the progress of timers, the stopwatch, holidays and dismiss challenges.

### Features

- Alarms: one-time or repeating alarms. Repeat rules cover weekdays, the nth weekday of a month, the end of a month, day intervals, end dates and counts; single dates can be excluded, or only the next ring skipped.
- Ringing: a system ringtone or imported local audio, spoken text, independent volume with a fade-in, custom vibration, snooze and auto silence, and a full screen ringing screen on the lock screen; alarms due at the same time ring one after another.
- On time: alarms are rescheduled automatically after a reboot, a time or time zone change and a plugin update, and ring even before the first unlock after a reboot; an alarm missed while the device was off rings within 5 minutes, later it only leaves a notification.
- Script hooks: an alarm can register script files that AutoJs6 starts on ring, snooze, dismiss and other events, so scripts need not keep running; a running script can also listen to these events live.
- Plugin tools: add, query, change and remove alarms and handle a ringing alarm through the MCP Server plugin or `plugins.tools.call` (nine `alarm_*` tools).
- System clock: set, show, dismiss and snooze requests from other apps can be handled by the plugin.
- Screens: language, night mode and theme color follow AutoJs6 by default and can be set separately; the launcher icon can be switched; the settings screen sets the default ringtone, vibration, snooze and auto silence.
- Timers and stopwatch (planned for 1.1.0): several timers in parallel with pause, resume and extra time; laps for the stopwatch.
- Holidays (planned for 1.2.0): built-in open holiday data for many regions; alarms skip public holidays and ring on make-up workdays; a `beforeRing` script hook can also decide whether to ring.
- Dismiss challenges (planned for 1.3.0): arithmetic, shaking, typing a sentence or a script condition, each with a safety valve so that the alarm can always be dismissed.

Usage, permissions and the FAQ are on the [project page](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm); the script API is in the [AutoJs6 documentation](https://docs.autojs6.com/#/alarms).

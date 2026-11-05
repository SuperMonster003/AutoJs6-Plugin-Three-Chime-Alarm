3-Chime Alarm is the alarm plugin of AutoJs6 and also a complete clock app that works without AutoJs6: alarms, several parallel countdown timers and a stopwatch. Alarms repeat by RFC 5545 calendar rules (for example the last Friday of every month, or every 3 days from a given date), can skip public holidays, and can start a script or notify a running script when they ring.

> This is a development preview: the AutoJs6 Plugin Center can discover the plugin, but alarms, timers, the stopwatch, the plugin screens and the `alarms` script module are not delivered yet, and the icon is a candidate design that may still change. The text below describes the design goals of each version; see the roadmap for progress.

### Features

- Alarms: one-time or repeating alarms. Repeat rules cover weekdays, the nth weekday of a month, the last day of a month, intervals in days, end dates and occurrence counts, and just the next occurrence can be skipped.
- Ringing: system ringtones or local audio, spoken text, its own volume with a gradual increase, custom vibration patterns, a full screen alert on the lock screen, and snooze.
- Script hooks: an alarm can register a script file that AutoJs6 starts when the alarm rings, so the script does not have to keep running; a running script can also listen to ring, snooze and dismiss events in real time.
- Timers and stopwatch (1.1.0): several timers in parallel that can be paused, resumed and extended and that survive a reboot; the stopwatch records laps.
- Holidays (1.2.0): built-in open holiday data for many regions to skip public holidays and still ring on make-up workdays; a `beforeRing` script hook can also decide whether an alarm rings.
- Dismiss challenges (1.3.0): arithmetic, shaking, retyping a sentence or a script condition, each with a safety valve so that an alarm can always be stopped.

Progress and design are available on the [project page](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm).

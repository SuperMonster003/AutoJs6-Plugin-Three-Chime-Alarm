******

### Release history

******

# v1.1.0

###### 2026/11/12

* `Hint` alarms.timers and alarms.stopwatch in scripts need AutoJs6 6.8.1 (internal build 6641) or later and this plugin 1.1.0 or later; with plugin 1.0.x the calls fail with a message asking to update the plugin
* `Feature` Timers: up to 20 timers at once, with a preset length (1 minute to 1 hour) or a custom one (1 second to 100 hours) and an optional label; timers can be paused, resumed, extended by 1 minute and reset, and ring like alarms when they end
* `Feature` A running timer shows its countdown in the notification shade, where it can be paused, extended by 1 minute or cancelled; timers keep counting after a reboot, and a timer that ended while the device was off rings after the boot (if it is more than 5 minutes overdue, only a missed notification is left)
* `Feature` Stopwatch: start, pause, resume and reset, up to 100 laps, with the fastest and the slowest lap marked; while it runs it shows in the notification shade, which can be turned off in the "Timers and stopwatch" group of the settings
* `Feature` The bottom navigation switches between the alarms, timers and stopwatch pages
* `Feature` New alarms.timers and alarms.stopwatch in the AutoJs6 script module (needs AutoJs6 6.8.1): starting, querying, pausing, resuming, adding or removing time, resetting and cancelling timers, scripts registered for a timer to start on ring and other events, and controlling the stopwatch with laps (see [AutoJs6 documentation](https://docs.autojs6.com/#/alarms))
* `Feature` New plugin tools timer_start, timer_list and timer_control to start, list, pause, resume, extend, reset and cancel timers through the MCP Server plugin or plugins.tools.call
* `Feature` System clock: timer requests are handled too; other apps can have the plugin start a timer, open the timers page or dismiss a ringing timer

# v1.0.1

###### 2026/11/08

* `Fix` When another app or an AutoJs6 script sent two alarm requests of the same kind in quick succession (for example two new alarms in a row, or two calls of alarms.system.setAlarm aimed at this plugin) while no plugin screen was open, the second request was ignored without any message and its alarm was never created; every request is now handled; on Android 7.0 - 8.1 this needs the "reorder running apps" permission (granted at install)

# v1.0.0

###### 2026/11/07

* `Hint` Enabling the plugin in the Plugin Center, the plugin tools and the alarms script module need AutoJs6 6.8.1 (internal build 6641) or later; with older AutoJs6 versions (for example 6.8.0) the plugin works only as a standalone alarm clock app
* `Hint` On ColorOS (OPPO, OnePlus, realme), turn on auto launch for the plugin in both the app details and Settings > Apps > Auto launch, otherwise alarms do not ring once the plugin process has been cleaned up (see README > FAQ)
* `Feature` Alarms: one-time or repeating alarms with RFC 5545 calendar rules (weekdays, the nth weekday of a month, the end of a month, day intervals, end dates and counts); dates can be excluded, or only the next ring skipped
* `Feature` Ringing: a system ringtone or imported local audio, spoken text, independent volume with a fade-in, custom vibration, snooze and auto silence, a full screen ringing screen on the lock screen, and alarms due at the same time ringing one after another
* `Feature` Scheduling: alarms are rescheduled after a reboot, a time or time zone change and a plugin update, ring before the first unlock, and a missed alarm rings within 5 minutes or else leaves a notification
* `Feature` Plugin screens: alarm list, edit screen, ringing screen, permissions page and settings; language, night mode and theme color follow AutoJs6 by default, and the launcher icon can be switched
* `Feature` Settings: default ringtone, vibration, snooze and auto silence, time format, automatic update checks, and the optional "Keep running in background" (a silent notification that makes cleanup by the system less likely)
* `Feature` The permissions page checks notifications, exact alarms, full screen alerts, battery optimization, background restrictions and the vendor's auto start one by one, with extra guide rows for the auto launch list and banner notifications on ColorOS
* `Feature` AutoJs6 script module alarms / $alarms (needs AutoJs6 6.8.1, Rhino only): adding, querying, changing and removing alarms, event listeners, scripts registered for AutoJs6 to start when an event happens, and the ringtone list (see [AutoJs6 documentation](https://docs.autojs6.com/#/alarms))
* `Feature` Plugin tools alarm_add, alarm_list, alarm_update, alarm_remove, alarm_set_enabled, alarm_skip_next, alarm_upcoming, alarm_dismiss and alarm_snooze, callable through the MCP Server plugin or plugins.tools.call
* `Feature` System clock: handles the set, show, dismiss and snooze alarm requests of other apps
* `Feature` A single APK for every device, including devices with 16 KB memory pages
* `Dependency` Attach AndroidX Core KTX 1.15.0
* `Dependency` Attach AndroidX Activity Compose 1.12.2
* `Dependency` Attach Jetpack Compose BOM 2026.09.00 (plugin screens)
* `Dependency` Attach Kotlin Coroutines Android 1.11.0
* `Dependency` Attach Material Color Utilities 4.1.1 (theme colors)
* `Dependency` Attach lib-recur 0.17.1 (calculates the repeat rules of alarms)
* `Dependency` Attach Desugar JDK Libs 2.1.5 (date and time zone calculations on Android 7.x)

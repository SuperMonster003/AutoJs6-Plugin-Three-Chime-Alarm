******

### Release history

******

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

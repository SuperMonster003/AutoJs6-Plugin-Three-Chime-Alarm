3-Chime Alarm is an alarm, timer and stopwatch app that also works on its own, without AutoJs6. Alarms can repeat by calendar rules, skip holidays or ring only on holidays, read text aloud when they ring, and require a challenge to be completed before they can be dismissed. With AutoJs6, scripts can manage alarms and timers, and chosen scripts can start automatically when an alarm rings or another event occurs.

### Getting started

1. Open 3-Chime Alarm. When the home screen shows the "Alarms may not ring properly" notice, tap it and go through the "Permissions and background" page item by item.
2. Tap the "New alarm" button in the lower right corner, set the time, repeat and sound, then tap "Save".
3. Use the bottom navigation to switch to "Timers" or "Stopwatch"; tap the "Settings" button in the top bar to set defaults such as sound, vibration and snooze.
4. To use the plugin in scripts, enable it in the "Plugin center" of AutoJs6.

### Main features

- **Repeat**: choose "Every day", "Monday to Friday", "Weekends" or "Choose days"; "Advanced rule" supports calendar rules such as the nth weekday of a month, the end of a month or every few days, and "Skipped dates" can be added.
- **Ringing**: use a system sound or an imported local audio file, and set the text for "Read aloud", vibration and snooze; a full screen ringing screen appears on the lock screen, and alarms that go off at the same time ring one after another.
- **Holidays**: the "Holidays" row on the edit page offers "Skip holidays", "Only on holidays" or "Only on workdays" (makeup workdays included). Data for 250 countries and regions is built in, and updates are checked weekly.
- **Dismiss challenge**: the "Dismiss challenge" row can require "Math problems", "Shake" or "Type text" to be completed before the alarm can be dismissed. Every challenge keeps two ways out: "Auto stop" and "Emergency dismiss".
- **Timers and stopwatch**: up to 20 timers run at once and can be controlled from the notification with "Pause", "+1 min" or "Cancel"; the stopwatch has a "Lap" button and marks the fastest and slowest laps.
- **On time**: alarms are rescheduled automatically after a restart or a change of time or time zone, and ring even before the first unlock after booting; an alarm missed while the device was off still rings if it is turned on within 5 minutes, otherwise a notification is left.
- **System clock requests**: handles requests from other apps such as calendars and voice assistants to set an alarm, set a timer or dismiss an alarm.
- **Appearance**: "Language", "Night mode" and "Theme color" follow AutoJs6 by default and can also be set separately in the settings; the "Launcher icon" can be switched as well.
- **Plugin tools**: create, query and change alarms, control timers and look up holidays through the MCP Server plugin or `plugins.tools.call`.

### Using it in scripts

Rhino scripts in AutoJs6 use the alarms and timers through the global module `alarms`. The example below creates a 7:30 alarm for workdays, registers a script to run when the alarm is dismissed, then starts a 25 minute timer:

```js
let a = alarms.add({ time: '07:30', repeat: [1, 2, 3, 4, 5], label: 'Wake up' });
console.log(a.repeatText, new Date(a.nextTrigger));
a.bind('./after-wake.js', { events: ['dismiss'] });

let t = alarms.timers.start('25m', { label: 'Pomodoro' });
console.log(t.id, new Date(t.endsAt).toLocaleTimeString());
```

- Alarms and timers created by scripts also appear in the plugin and can be changed there; scripts can also read and change alarms created in the plugin.
- A script registered with `bind` is started by AutoJs6 when the event occurs and does not need to keep running; a running script can listen in real time with `alarms.on('ring', ...)`.
- Node.js and Bun scripts have no `alarms` module; they can use `engines.execScript` to run a Rhino script that uses it.
- See [AutoJs6 docs > alarms](https://docs.autojs6.com/#/alarms) for the full usage, including holidays, letting a script decide before an alarm rings and dismiss challenges.

### Requirements

- Android 7.0 or later. Alarms, timers and the stopwatch do not need AutoJs6.
- The `alarms` module and the plugin tools need AutoJs6 6.8.1 or later, with this plugin enabled in the "Plugin center".
- The exact alarm permission lets alarms ring on time, and on Android 12 and 12L "Alarms & reminders" must be allowed in the system settings; the notification permission shows ringing alarms and running timers.
- The full screen alert permission shows the ringing screen on the lock screen; the overlay permission is optional and opens the ringing screen when the system does not show full screen alerts.
- On the "Permissions and background" page, it is best to turn off battery optimization, lift background restrictions and allow autostart, so that the system does not stop the plugin in the background.

### FAQ

**Alarms when AutoJs6 is not running**

The plugin schedules alarms itself, so they ring as usual when AutoJs6 is not running. Registered scripts are started by AutoJs6, which the plugin wakes up; after AutoJs6 has been force stopped, scripts do not start until AutoJs6 is opened again.

**Why the Plugin center of AutoJs6 6.8.0 cannot enable this plugin**

Enabling this plugin in the Plugin center needs AutoJs6 6.8.1 or later. On earlier versions of AutoJs6, the plugin still works as a standalone alarm app.

**When alarms do not ring on time**

Tap the notice on the home screen and go through the "Permissions and background" page item by item; on ColorOS, also handle the "Auto launch list" row on the same page. After the plugin has been force stopped, alarms that are due do not ring; opening the plugin once restores them.

**When a dismiss challenge cannot be completed**

While the ringing screen is in the foreground (including on the lock screen), pressing a volume key 5 times within 4 seconds dismisses the alarm in an emergency; this can be turned off in the settings. Otherwise the alarm stops after the auto stop duration, which is at least 30 minutes unless set separately.

Permissions, system clock requests and holiday data are described in detail on the [project page](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm).

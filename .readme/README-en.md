<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-chime-alarm-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Alarm clock, timers and stopwatch with calendar recurrence rules, holidays, dismiss challenges, spoken ringing and script hooks when an alarm rings</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Languages

******

The README.md file is currently available in the following languages:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-zh-Hant-TW.md)
- English [en] # current
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/.readme/README-ar.md)

******

### Introduction

******

3-Chime Alarm is the alarm plugin of AutoJs6 and also an alarm clock app that works on its own without AutoJs6. Alarms repeat by RFC 5545 calendar rules (for example the last Friday of every month, or every 3 days from a given date); a ringing alarm can read text aloud, start a script or notify a running script. Since 1.1.0 it also offers several timers running at once and a stopwatch. Since 1.2.0 an alarm can skip holidays, ring only on holidays or ring on every workday (makeup workdays included), and a script can decide before the ring whether this one rings. Since 1.3.0 an alarm can require solving math problems, shaking the phone, typing a sentence or a confirmation by a script before it can be dismissed, always keeping two ways out: auto stop and emergency dismiss.

The plugin schedules and rings alarms itself with Android exact alarms, so alarms ring on time even when AutoJs6 is not running. AutoJs6 scripts query, create, change and listen to the alarms of the plugin through the global module `alarms`, and use the timers and the stopwatch through `alarms.timers` and `alarms.stopwatch`; the sub-namespace `alarms.system` talks to the system clock app directly and works without the plugin. The plugin also accepts set-alarm and set-timer requests from other apps.

> Version 1.3.0 provides alarms (with holiday rules, decisions by a script before the ring and dismiss challenges), timers and the stopwatch, the plugin screens, the plugin tools and the `alarms` script module. Enabling the plugin in the Plugin Center, the plugin tools and the `alarms` module need AutoJs6 6.8.1 (internal build 6641) or later; with older AutoJs6 versions (including 6.8.0) the plugin works only as a standalone alarm clock app.

******

### Features

******

- Alarms: one-time or repeating alarms. Repeat rules cover weekdays, the nth weekday of a month, the end of a month, day intervals, end dates and counts; single dates can be excluded, or only the next ring skipped.
- Ringing: a system ringtone or imported local audio, spoken text, independent volume with a fade-in, custom vibration, snooze and auto silence, and a full screen ringing screen on the lock screen; alarms due at the same time ring one after another.
- On time: alarms are rescheduled automatically after a reboot, a time or time zone change and a plugin update, and ring even before the first unlock after a reboot; an alarm missed while the device was off rings within 5 minutes, later it only leaves a notification.
- Script hooks: an alarm or a timer can register script files that AutoJs6 starts on ring, snooze, dismiss and other events, so scripts need not keep running; a running script can also listen to these events live.
- Plugin tools: add, query, change and remove alarms (dismiss challenges included, except the script condition), handle a ringing alarm, start, list and control timers, and look up holidays through the MCP Server plugin or `plugins.tools.call` (nine `alarm_*` tools, three `timer_*` tools and `holiday_check`).
- System clock: set, show, dismiss and snooze requests for alarms and set, show and dismiss requests for timers from other apps can be handled by the plugin.
- Screens: language, night mode and theme color follow AutoJs6 by default and can be set separately; the launcher icon can be switched; the settings screen sets the default ringtone, vibration, snooze and auto silence, and whether emergency dismiss is allowed.
- Timers and stopwatch: up to 20 timers at once with pause, resume, extra time and reset; a running timer shows its countdown in the notification shade, where it can be paused, extended by 1 minute or cancelled; timers keep counting across a reboot and ring like alarms. The stopwatch records laps (up to 100) and marks the fastest and the slowest lap. The bottom navigation switches between alarms, timers and the stopwatch.
- Holidays: an alarm can be set to "Skip holidays", "Only on holidays" or "Only on workdays" (rings on every workday, makeup workdays included, never on weekends or holidays). Data for 250 countries and regions (with more than 600 subdivisions) from 2020 to 2035 is built in, generated from the open source project python-holidays; updates are checked every week and downloaded data is signature checked. Years not yet announced are estimated by rule and marked "estimated"; in a year without data the alarm rings by its repeat rule alone.
- Decided by a script before the ring: a registered script is started by AutoJs6 before the ring (60 seconds by default, 15 seconds to 5 minutes) and can skip this ring, or let it ring with a different label, sound, spoken text or vibration this time only; when the script does not decide in time, fails or AutoJs6 is unavailable, the alarm rings as usual.
- Dismiss challenges: an alarm can require solving math problems (easy, medium or hard, 3 problems by default), shaking the phone (30 shakes by default) or typing a sentence (your own, or a random built-in one) before it can be dismissed, and can allow or forbid snoozing during the challenge; while a challenge is in effect, the "Dismiss" button of the notification and dismiss requests from other apps open the challenge on the ringing screen. Every challenge keeps two ways out: auto stop (when not set on the alarm, the longer of the default on the settings screen and 30 minutes, with the time left shown on the ringing screen) and emergency dismiss (5 presses of a volume key within 4 seconds while the ringing screen is in front, also on the lock screen; can be turned off on the settings screen). AutoJs6 scripts can also set a script condition: the alarm rings until a script dismisses it, and when no script can run (for example before the first unlock after a reboot, or after the script has ended) it falls back to the chosen challenge at once.

******

### Screenshots

******

Screenshots taken on a Sony XQ-DQ72; the light theme in the top row, the dark theme in the bottom row.

| Alarm list | Edit alarm | Ringing | Settings |
| :---: | :---: | :---: | :---: |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/list-light.png?raw=true" alt="list-light" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/edit-light.png?raw=true" alt="edit-light" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/ring-light.png?raw=true" alt="ring-light" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/settings-light.png?raw=true" alt="settings-light" width="200" /> |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/list-dark.png?raw=true" alt="list-dark" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/edit-dark.png?raw=true" alt="edit-dark" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/ring-dark.png?raw=true" alt="ring-dark" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/settings-dark.png?raw=true" alt="settings-dark" width="200" /> |

| Timers | Stopwatch |
| :---: | :---: |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/timers-light.png?raw=true" alt="timers-light" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/stopwatch-light.png?raw=true" alt="stopwatch-light" width="200" /> |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/timers-dark.png?raw=true" alt="timers-dark" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/docs/images/screenshots/stopwatch-dark.png?raw=true" alt="stopwatch-dark" width="200" /> |

******

### Usage

******

1. Download the APK from [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/releases) and install it. There is a single package for every ABI.
2. Open the plugin and go through the permissions page: notifications, exact alarms (to be allowed by hand on Android 12 and 12L), full screen alerts (possibly not granted from Android 14), and battery optimization, background restrictions and the vendor's auto start. It then works like an ordinary clock app.
3. To use the `alarms` module in scripts or the plugin tools, upgrade AutoJs6 to 6.8.1 (internal build 6641) or later and enable the plugin in the Plugin Center (category "Tools" of the install wizard).

> The plugin supports devices running Android 7.0 (API 24) or later. There is a single package for every device; its only native library comes from the Compose UI toolkit and is aligned to 16 KB memory pages, so it also runs on devices with 16 KB pages.

******

### Script API

******

Scripts use these features through the global module `alarms` (also `$alarms`, Rhino only); the full reference is in the [AutoJs6 documentation](https://docs.autojs6.com/#/alarms):

```javascript
let a = alarms.add({ time: "07:30", repeat: "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR", label: "Wake up" });
a.bind("./after-wake.js", { events: ["dismiss"] });

alarms.add({ time: "18:00", repeat: "FREQ=MONTHLY;BYDAY=-1FR", label: "Monthly report" });
alarms.add({ time: "06:10", date: "2026-11-04", repeat: { freq: "daily", interval: 3 }, tag: "shift" });

let w = alarms.add({ time: "07:30", holidays: { region: "CN", mode: "workdays" }, label: "Work" });
w.bind("./weather-check.js", { events: ["beforeRing"], leadSeconds: 120 });

alarms.on("ring", (e) => toast(e.label));

alarms.timers.start("25m", { label: "Tea" });

alarms.system.setAlarm({ hour: 6, minute: 50, label: "Flight" });
```

- `alarms.add(options)` creates an alarm and returns its snapshot; `alarms.list`, `get`, `update`, `remove`, `setEnabled`, `skipNext` and `cancelSkip` manage every alarm of the plugin, including the ones created on the plugin screens.
- `repeat` takes an RRULE string (for example `FREQ=MONTHLY;BYDAY=-1FR`), an array of weekday numbers or a structured object; `alarms.preview(options)` lists the next ring times so that a rule can be checked.
- `alarms.bind(id, script, options)` (or `bind` of an alarm snapshot) registers a script that AutoJs6 starts when an event happens; the script reads the event through `alarms.currentEvent()`. `alarms.on('ring', listener)` listens live while a script runs.
- `alarms.ringing`, `dismiss` and `snooze` handle a ringing alarm; `alarms.ringtones.list()` lists the available ringtones.
- `alarms.timers.start(duration, options)` starts a timer (milliseconds or a string such as `"25m"` or `"1h 30m"`, from 1 second to 100 hours) and returns a timer snapshot with the methods `pause`, `resume`, `addTime`, `reset`, `cancel` and `bind`; `alarms.stopwatch` starts, pauses, resumes, records laps of and resets the stopwatch. Both need plugin 1.1.0 or later.
- `alarms.holidays` reads the holiday data (`regions`, `isHoliday`, `isWorkday`, `list`, `info`, `refresh`); the `holidays` option of an alarm (for example `{ region: 'CN', mode: 'workdays' }`) sets its holiday rule, and the `warnings` of `alarms.capabilities()` point out missing data and estimated years. Needs plugin 1.2.0 or later.
- `alarms.bind(id, script, { events: ['beforeRing'], leadSeconds: 120 })` registers a script started before the ring; in it, `skip(reason)` of `alarms.currentEvent()` skips this ring and `ring(overrides)` lets it ring with a different label, sound, spoken text or vibration this time; `alarms.on('beforeRing', listener)` listens live, with the same two methods on the event. Needs plugin 1.2.0 or later.
- The `dismiss` option sets a dismiss challenge, for example `alarms.add({ time: '07:00', dismiss: { challenge: 'math', math: { difficulty: 3, count: 5 }, allowSnooze: false } })`; with `challenge: 'script'` a script dismisses the alarm by calling `alarms.dismiss(id)`, and `script.fallback` names the challenge used when no script can run. `alarms.ringing()` and the `ring` and `dismiss` events carry the `challenge` in effect, and the `by` of a `dismiss` event is `'challenge'` when the challenge was completed. Needs plugin 1.3.0 or later.
- `alarms.system` calls the system clock app directly (`setAlarm`, `setTimer`, `showAlarms`, `dismissAlarm`, `next` and more) and needs no plugin.

******

### System clock

******

The plugin can act as the alarm clock app of the system and handle the standard clock requests of other apps (for example calendars, voice assistants and automation tools). The requesting app must hold the `com.android.alarm.permission.SET_ALARM` permission:

- Set alarm (`SET_ALARM`): reads the time, label, days, vibration and ringtone; a request that skips the UI is saved directly, otherwise a prefilled edit screen opens.
- Show alarms (`SHOW_ALARMS`): opens the alarm list.
- Dismiss alarm (`DISMISS_ALARM`): ends a ringing or snoozed alarm; when nothing rings, an alarm due within 24 hours skips that ring instead; when several alarms match, the user chooses.
- Snooze (`SNOOZE_ALARM`): snoozes the ringing alarm.
- Set timer (`SET_TIMER`): reads the length (1 second to 100 hours) and the label and starts a timer; a reset timer with the same length and label starts again instead of a new one; unless the request skips the UI, the timers page opens too, and without a length the new timer dialog opens.
- Show timers (`SHOW_TIMERS`): opens the timers page.
- Dismiss timer (`DISMISS_TIMER`): ends the ringing timers and leaves running timers alone.
- `alarms.system` works the other way round: a script sends requests to the default clock app of the system (this plugin or another clock app) without the plugin; differences between vendors' clock apps are described in the AutoJs6 documentation.

******

### Compatibility

******

The plugin supports Android 7.0 (API 24) and later. Enabling it in the Plugin Center, the plugin tools and the `alarms` module need AutoJs6 6.8.1 (internal build 6641) or later. With older AutoJs6 versions (for example 6.8.0, internal build 6640), the Plugin Center reports that a newer AutoJs6 is required and cannot enable the plugin, and the plugin tools and the `alarms` module are unavailable; the plugin still works as a standalone alarm clock app and still handles system clock requests. The `alarms` module supports the Rhino engine only, and its `alarms.system` needs no plugin.

Verified on these devices before the 1.0.0 release (data in appendix D of the roadmap); the timers and the stopwatch of 1.1.0 were also verified on a Sony XQ-AT72 and a Sony G8441, including timers that keep counting across a reboot (see P7 of the roadmap); the holidays and the decisions by a script before the ring of 1.2.0 were verified on a Sony XQ-AT72 together with AutoJs6 (see P8 of the roadmap); the dismiss challenges of 1.3.0 were verified on a Sony XQ-AT72 together with AutoJs6, including emergency dismiss on the lock screen and every fallback of the script condition (see P9 of the roadmap):

| Device | Android | Result |
| --- | --- | --- |
| Sony XQ-AT72 | 12 (API 31) | Passed: median ring delay 44 ms; ringing on the lock screen, ringing after a reboot, time, time zone and daylight saving changes, plugin tools called through MCP Server |
| Sony XQ-DQ72 | 13 (API 33) | Passed: median ring delay 13 ms; full screen ringing on the lock screen, ringing screen shown about 1.1 s after the due time when the process had been killed |
| Redmi 22120RN86C (MIUI) | 13 (API 33) | Passed: median ring delay 40 ms; the system ended the plugin process several times after boot, the alarm still rang on time |
| Xiaomi Pad 23046RP50C | 15 (API 35) | Passed: median ring delay 32 ms; rings with a heads-up notification without the full screen alert permission, rings on time after a reboot before unlocking |
| OnePlus OPD2413 (ColorOS 15) | 15 (API 35) | Passed (auto launch required): median ring delay 23 ms; after turning the plugin on in Settings > Apps > Auto launch, alarms ring on time even after the process is cleaned up (see FAQ) |
| Sony G8441 | 9 (API 28) | Passed: median ring delay 41 ms; ringing on the lock screen, rings on time after a reboot before unlocking |
| Android emulators | 7.0 - 16 (API 24 - 36) | Passed: complete flow on the lowest version, ringing before the first unlock, 16 KB memory pages |

******

### FAQ

******

#### Can I use it without AutoJs6?

Yes. The plugin has complete alarm screens and works on its own as an alarm clock app. AutoJs6 is only needed when scripts manage alarms, react to them or use the plugin tools.

#### Do alarms ring when AutoJs6 is not running?

Yes. The plugin schedules and rings alarms with Android exact alarms and does not depend on the AutoJs6 process. When a script is registered, the plugin wakes AutoJs6 to start it at ring time.

#### Why can the Plugin Center of AutoJs6 6.8.0 not enable the plugin?

Enabling the plugin in the Plugin Center, the plugin tools and the `alarms` module need AutoJs6 6.8.1 (internal build 6641) or later, while 6.8.0 is internal build 6640. Upgrade AutoJs6 to enable it; until then the plugin works as a standalone alarm clock app.

#### An alarm did not ring on time. What should I check?

Open the permissions page of the plugin first, check that notifications and exact alarms are allowed, and turn off battery optimization, lift background restrictions and allow auto start as the page describes. Do Not Disturb lets alarms through by default; if you changed its settings, make sure alarms are still allowed. An alarm missed because the device was off or the system delayed it rings within 5 minutes; after that it stays silent and leaves a notification.

#### Alarms do not ring on OPPO, OnePlus or realme (ColorOS). What can I do?

ColorOS has two independent switches, and both must be on for 3-Chime Alarm: the autostart, associated launch and background activity switches in the app details, and the list in Settings > Apps > Auto launch. With only the first ones on, the system blocks due alarms once the plugin process has been cleaned up (for example swiped away in recent tasks). With script hooks, AutoJs6 needs both switches as well. Do not swipe the plugin away in recent tasks; you can lock it there. The permissions page of the plugin has matching guide rows on ColorOS, and "Keep running in background" on the settings screen keeps a silent notification to make cleanup less likely, at a slightly higher battery cost.

#### On ColorOS a locked device only plays the ringtone and shows a notification, without the ringing screen. Why?

ColorOS turns the "Banner" notifications of newly installed apps off, and the system then holds the full screen ringing screen back. Turn "Banner" on in the notification settings of the plugin; the "Banner notifications" row of the permissions page opens them directly. Allowing "Display over other apps" on the permissions page also works as a fallback.

#### Alarms stopped ringing after I force stopped the plugin?

"Force stop" makes the system clear all alarm schedules of the plugin, and nothing rings while it stays stopped; this is an Android rule. Open the plugin once from the launcher to restore the schedules; alarms that are less than 5 minutes overdue then ring.

#### Does Xiaomi (MIUI, HyperOS) need special settings?

On a Redmi (MIUI) test device the system ended the plugin process several times after boot, and the system still started the plugin at the due time and the alarm rang on time. Setting up auto start and the battery saver as the permissions page describes is still recommended.

#### Do alarms ring after a reboot before the device is unlocked? What about hooked scripts?

Alarms ring, because the plugin runs before the first unlock. Hooked scripts are not started, because AutoJs6 cannot run scripts before the first unlock, and they are not started after the unlock either; an alarm with the script condition as its dismiss challenge therefore falls back to its chosen challenge at once.

#### What happens to timers and the stopwatch after a reboot?

Timers keep their end time from before the shutdown, so the time while the device is off counts too. A timer that ended while the device was off rings after the boot if it is less than 5 minutes overdue, otherwise it leaves a missed notification. When the system corrects its clock after the boot, the end time can move by a few seconds with it. The stopwatch is paused after a reboot and counts up to the boot, so the time the device was off is included (a reboot usually takes less than a minute).

#### Is the holiday data accurate? Does the alarm ring on makeup workdays?

Holidays and makeup workdays are only as accurate as the upstream data (the open source project python-holidays). With "Only on workdays" the alarm rings on makeup workdays; with "Skip holidays" only the repeat rule counts (a Monday to Friday alarm does not ring on a Saturday makeup workday). Years not yet announced are estimated by rule, marked "estimated" on the settings and edit screens, and corrected by a data update once announced; updates are checked every week, and the settings screen can check right away.

#### What if the `beforeRing` script does not decide in time?

The alarm rings as configured. The deadline is 5 seconds before the due time by default; when no decision has come by then, the script fails, AutoJs6 is unavailable (for example force-stopped) or the device has not been unlocked since a reboot, the alarm rings as usual. A ring the holiday rule already skips starts no `beforeRing` script.

#### With a dismiss challenge, does the alarm ring forever if I cannot solve the problems or shake hard enough?

No. Every challenge keeps two ways out: when auto stop is not set on the alarm, the alarm stops by itself after the longer of the default on the settings screen and 30 minutes, and the ringing screen shows the time left; while the ringing screen is in front (also on the lock screen), 5 presses of a volume key (up or down) within 4 seconds dismiss it at once. During a challenge the volume keys only count presses and do not change the volume. Emergency dismiss is on by default and can be turned off in the "Dismiss challenges" group of the settings screen; with it off, only auto stop ends the ringing until the challenge is done.

******

### Permissions and security

******

Permissions requested by the plugin and what they are for:

- Exact alarms (`USE_EXACT_ALARM`, `SCHEDULE_EXACT_ALARM` on Android 12 and 12L): ring on time. Granted at install from Android 13; on Android 12 and 12L allow "Alarms & reminders" in the system settings.
- Notifications (`POST_NOTIFICATIONS`): show ringing and missed alarms and running timers and the stopwatch. Without it alarms and timers still sound, but there is no notification and no ringing screen.
- Full screen alerts (`USE_FULL_SCREEN_INTENT`): show the ringing screen on the lock screen. Possibly not granted from Android 14; without it alarms ring with a heads-up notification.
- Display over other apps (`SYSTEM_ALERT_WINDOW`, optional): opens the ringing screen when the system holds the full screen alert back (for example with the banner notifications of ColorOS off); only allowed by the user on the permissions page when needed.
- Boot (`RECEIVE_BOOT_COMPLETED`): reschedules after a reboot so that alarms ring before the first unlock.
- Foreground service (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SYSTEM_EXEMPTED`): keeps the alarm sounding while it rings; also keeps the plugin running when "Keep running in background" is on in the settings (off by default).
- Vibration, wake lock and audio settings (`VIBRATE`, `WAKE_LOCK`, `MODIFY_AUDIO_SETTINGS`): vibrate, keep the device awake while ringing, and restore the independent alarm volume after ringing.
- Reorder running apps (`REORDER_TASKS`, Android 7.0 - 8.1 only): when other apps or scripts send several alarm requests at almost the same time, brings a request the system did not start to the front, so every request is handled. Granted at install.
- Network (`INTERNET`, `ACCESS_NETWORK_STATE`): only for the update check and the holiday data refresh. The update check runs when the alarm list opens, at the chosen frequency, and holiday data is checked once a week; both skip metered networks by default. Scheduling, ringing and the AutoJs6 link never use the network.
- AutoJs6 plugin permission (`org.autojs.permission.PLUGIN`): communicates with AutoJs6.

The plugin keeps its permission surface as small as possible:

- Protected entry points: the capability service and the activation entry are protected by the AutoJs6 plugin permission (`org.autojs.permission.PLUGIN`), so other apps cannot call them directly; the entry that accepts system clock requests requires the caller to hold `com.android.alarm.permission.SET_ALARM`.
- Network: used only to check for plugin updates and holiday data (on GitHub; automatic checks for both can be turned off on the settings screen); downloaded holiday data is signature checked, and alarms and personal data are never uploaded.
- Storage: alarms are stored in the private directory of the plugin and are excluded from cloud backup; local ringtones are imported through the system file picker without a storage permission.
- Script access: through the `alarms` module, scripts can read and change every alarm and timer of the plugin, including those created on its screens; registered scripts are stored in AutoJs6, and the plugin screens only mark an alarm as having a script binding and can remove it.

******

### Plugin interface

******

The following information targets AutoJs6 host and plugin developers; the host uses these identifiers to discover the plugin and negotiate capabilities:

```text
application id: io.github.supermonster003.autojs6.plugin.three.chime.alarm
plugin id: three-chime-alarm
engine: alarm
variant: default
info service: org.autojs.plugin.INFO (category three-chime-alarm)
capability service: org.autojs.plugin.ALARM (category alarm)
wake action: org.autojs.plugin.action.WAKE
minimum host build: 6641
native libraries: libandroidx.graphics.path.so only (Compose graphics-path, 4 ABIs, uncompressed, 16 KB aligned; a single APK for every ABI)
```

******

### Roadmap

******

Plans, design decisions and progress are tracked as a checklist in ROADMAP.md. Unchecked items are plans, not features of the current version.

- [View ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/ROADMAP.md)

******

### Release history

******

#### v1.3.0

_2026/11/14_

- `Hint` Dismiss challenges set on the plugin screens need no AutoJs6; setting them in scripts (the dismiss option of alarms) and the script condition need AutoJs6 6.8.1 (internal build 6641) or later and this plugin 1.3.0 or later; with plugin 1.2.x or earlier, a call with the dismiss option fails with a message asking to update the plugin
- `Hint` An alarm with a dismiss challenge can always be ended: auto stop (at least 30 minutes when not set on the alarm) and emergency dismiss (5 presses of a volume key within 4 seconds on the ringing screen) are always available; emergency dismiss can be turned off on the settings screen, and then only auto stop ends the ringing until the challenge is done
- `Feature` Dismiss challenges: an alarm can require solving math problems (easy, medium or hard, 3 problems by default), shaking the phone (30 shakes by default) or typing a sentence (your own, or a random built-in one) before it can be dismissed, and can allow or forbid snoozing during the challenge; set it in the "Dismiss challenge" row of the edit screen, and the challenge takes the place of the dismiss slider on the ringing screen
- `Feature` While a challenge is in effect, the "Dismiss" button of the notification and dismiss requests from other apps no longer dismiss the alarm directly but open the challenge on the ringing screen; when snoozing is not allowed the notification shows no "Snooze" button; on a device without an accelerometer the shake challenge becomes math problems
- `Feature` Safety valves: when an alarm with a challenge does not set auto stop, auto stop takes the longer of the default on the settings screen and 30 minutes, and the ringing screen shows the time left; 5 presses of a volume key within 4 seconds while the ringing screen is in front (also on the lock screen) dismiss the alarm in an emergency, and during a challenge the volume keys only count presses and do not change the volume; the new "Dismiss challenges" group of the settings screen can turn emergency dismiss off (on by default)
- `Feature` Script condition: set by an AutoJs6 script, the alarm rings until a script dismisses it; when the device has not been unlocked since a reboot, there is no script, AutoJs6 is unavailable, or the scripts failed to start or have ended, the alarm falls back at once to the chosen challenge (a normal dismiss by default), and the reason is recorded in the plugin log
- `Feature` New in the AutoJs6 script module: the dismiss option of alarms (the kind of challenge, the difficulty and number of math problems, the number of shakes, the text to type, the challenge used when no script can run, and whether snoozing is allowed), the challenge in alarms.ringing() and in the ring / dismiss events, and by: 'challenge' in the dismiss event (needs AutoJs6 6.8.1; see the [AutoJs6 documentation](https://docs.autojs6.com/#/alarms))
- `Feature` The plugin tools alarm_add and alarm_update accept the dismiss option and can set the math, shake and type challenges (the script condition excepted)

#### v1.2.0

_2026/11/13_

- `Hint` alarms.holidays, the holidays option of alarms and the beforeRing event in scripts need AutoJs6 6.8.1 (internal build 6641) or later and this plugin 1.2.0 or later; with plugin 1.1.x or earlier, calls fail with a message asking to update the plugin
- `Hint` Holidays and makeup workdays are only as accurate as the upstream data (the open source project python-holidays); years not yet announced are estimated by rule and marked "estimated", and are corrected by a data update once announced
- `Feature` Holidays: alarms get the options "Skip holidays", "Only on holidays" and "Only on workdays" (rings on every workday, makeup workdays included, never on weekends or holidays); holiday data for 250 countries and regions (with more than 600 subdivisions) from 2020 to 2035 is built in, with the makeup workdays of mainland China and Taiwan
- `Feature` The settings screen chooses the default holiday region (first taken from the system region), and each alarm can use its own region; the alarm list shows how many holidays the next ring skips; estimated years are marked "estimated", as days off and makeup workdays may still change
- `Feature` Holiday data is checked for updates every week (can be turned off on the settings screen; metered networks are not used by default) and can be checked by hand on the settings screen; downloaded data is signature checked and the current data stays in use when the check fails; in a year without data an alarm rings by its repeat rule alone and is never silenced for that reason
- `Feature` Decided by a script before the ring (beforeRing): a registered script is started before the ring (60 seconds by default, 15 seconds to 5 minutes) and can skip this ring, or let it ring with a different label, sound, spoken text or vibration this time only; when the script does not decide in time, fails or AutoJs6 is unavailable, the alarm rings as usual
- `Feature` New in the AutoJs6 script module: alarms.holidays (list regions, tell holidays and workdays, list a year's holidays, show and refresh the data), the holidays option of alarms, skip / ring of the beforeRing event, and warnings in alarms.capabilities() about missing data and estimated years (needs AutoJs6 6.8.1; see the [AutoJs6 documentation](https://docs.autojs6.com/#/alarms))
- `Feature` New plugin tool holiday_check tells through the MCP Server plugin or plugins.tools.call whether a day is a holiday or a workday in a region and the holiday's name; alarm_add and alarm_update accept the holidays option

#### v1.1.0

_2026/11/12_

- `Hint` alarms.timers and alarms.stopwatch in scripts need AutoJs6 6.8.1 (internal build 6641) or later and this plugin 1.1.0 or later; with plugin 1.0.x the calls fail with a message asking to update the plugin
- `Feature` Timers: up to 20 timers at once, with a preset length (1 minute to 1 hour) or a custom one (1 second to 100 hours) and an optional label; timers can be paused, resumed, extended by 1 minute and reset, and ring like alarms when they end
- `Feature` A running timer shows its countdown in the notification shade, where it can be paused, extended by 1 minute or cancelled; timers keep counting after a reboot, and a timer that ended while the device was off rings after the boot (if it is more than 5 minutes overdue, only a missed notification is left)
- `Feature` Stopwatch: start, pause, resume and reset, up to 100 laps, with the fastest and the slowest lap marked; while it runs it shows in the notification shade, which can be turned off in the "Timers and stopwatch" group of the settings
- `Feature` The bottom navigation switches between the alarms, timers and stopwatch pages
- `Feature` New alarms.timers and alarms.stopwatch in the AutoJs6 script module (needs AutoJs6 6.8.1): starting, querying, pausing, resuming, adding or removing time, resetting and cancelling timers, scripts registered for a timer to start on ring and other events, and controlling the stopwatch with laps (see [AutoJs6 documentation](https://docs.autojs6.com/#/alarms))
- `Feature` New plugin tools timer_start, timer_list and timer_control to start, list, pause, resume, extend, reset and cancel timers through the MCP Server plugin or plugins.tools.call
- `Feature` System clock: timer requests are handled too; other apps can have the plugin start a timer, open the timers page or dismiss a ringing timer

##### For more release history, refer to

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/app/src/main/assets/doc/CHANGELOG-en.md)

******

### Build

******

This section targets developers who want to build the plugin from source.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
.\gradlew.bat :app:appendDigestToReleasedFiles
py .python\generate_markdown.py --check
py .python\generate_icon_studio.py --check
py .python\check_repository.py
```

******

### License

******

The project code is licensed under the [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/LICENSE). Third-party components and data are listed in the third-party notices. The holiday data is generated from the open source project python-holidays (MIT License); its full license text and list of contributors are published together with the data.

- [Third-party notices (THIRD_PARTY_NOTICES.md)](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/THIRD_PARTY_NOTICES.md)

******

### Related links

******

- AutoJs6 project: https://github.com/SuperMonster003/AutoJs6

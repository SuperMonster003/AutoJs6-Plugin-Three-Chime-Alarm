<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-chime-alarm-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Alarm clock, timers and stopwatch with calendar recurrence rules, holiday skipping and script hooks when an alarm rings</p>

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

3-Chime Alarm is the alarm plugin of AutoJs6 and also a complete clock app that works without AutoJs6: alarms, several parallel countdown timers and a stopwatch. Alarms repeat by RFC 5545 calendar rules (for example the last Friday of every month, or every 3 days from a given date), can skip public holidays, and can start a script or notify a running script when they ring.

The plugin schedules and rings alarms itself with Android exact alarms, so alarms ring on time even when AutoJs6 is not running. AutoJs6 scripts query, create, change and listen to the alarms of the plugin through the global module `alarms`; the sub-namespace `alarms.system` talks to the system clock app directly and works without the plugin. The plugin also accepts set-alarm requests from other apps.

> This is a development preview: the AutoJs6 Plugin Center can discover the plugin, but alarms, timers, the stopwatch, the plugin screens and the `alarms` script module are not delivered yet, and the icon is a candidate design that may still change. The text below describes the design goals of each version; see the roadmap for progress.

******

### Features

******

- Alarms: one-time or repeating alarms. Repeat rules cover weekdays, the nth weekday of a month, the last day of a month, intervals in days, end dates and occurrence counts, and just the next occurrence can be skipped.
- Ringing: system ringtones or local audio, spoken text, its own volume with a gradual increase, custom vibration patterns, a full screen alert on the lock screen, and snooze.
- Script hooks: an alarm can register a script file that AutoJs6 starts when the alarm rings, so the script does not have to keep running; a running script can also listen to ring, snooze and dismiss events in real time.
- Timers and stopwatch (1.1.0): several timers in parallel that can be paused, resumed and extended and that survive a reboot; the stopwatch records laps.
- Holidays (1.2.0): built-in open holiday data for many regions to skip public holidays and still ring on make-up workdays; a `beforeRing` script hook can also decide whether an alarm rings.
- Dismiss challenges (1.3.0): arithmetic, shaking, retyping a sentence or a script condition, each with a safety valve so that an alarm can always be stopped.

******

### Usage

******

1. Download the APK from [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/releases) and install it. There is a single package for every ABI.
2. Open the plugin and grant the notification, exact alarm and full screen alert permissions when asked; it then works like an ordinary clock app.
3. To use the `alarms` module in scripts, upgrade AutoJs6 to internal build 6641 (6.8.1) or later and enable the plugin in the Plugin Center.

> The plugin supports devices running Android 7.0 (API 24) or later. The APK contains no native libraries, so there is a single package and the 16 KB memory page size check does not apply.

******

### Script API

******

Scripts use these features through the global module `alarms` (also available as `$alarms`) (1.0.0 design draft; the documentation of the released version is authoritative):

```javascript
let a = alarms.add({ time: "07:30", repeat: "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR", label: "Wake up" });
a.bind("./after-wake.js", { events: ["dismiss"] });

alarms.add({ time: "18:00", repeat: "FREQ=MONTHLY;BYDAY=-1FR", label: "Monthly report" });
alarms.add({ time: "06:10", date: "2026-11-04", repeat: { freq: "daily", interval: 3 }, tag: "shift" });

alarms.on("ring", (e) => toast(e.label));

alarms.system.setAlarm({ hour: 6, minute: 50, label: "Flight" });
```

- `alarms.add(options)` creates an alarm and returns an alarm object; `alarms.list`, `get`, `update`, `remove`, `setEnabled` and `skipNext` manage every alarm of the plugin, including those created on the plugin screens.
- `repeat` accepts an RRULE string (such as `FREQ=MONTHLY;BYDAY=-1FR`) or a structured object; `alarms.preview(options)` lists the upcoming ring times so that a rule can be checked.
- `alarm.bind(script, { events })` registers a script that AutoJs6 starts when the alarm rings, with the event in `alarms.currentEvent()`; `alarms.on('ring', listener)` listens while the script runs.
- `alarms.system` calls the system clock app directly (`setAlarm`, `setTimer`, `showAlarms`, `next` and more) and works without the plugin.

******

### FAQ

******

#### Can I use it without AutoJs6?

Yes. The plugin has complete clock screens and works on its own as an alarm clock, timer and stopwatch. AutoJs6 is only needed when scripts manage alarms or react to them.

#### Do alarms ring when AutoJs6 is not running?

Yes. The plugin schedules and rings alarms with Android exact alarms and does not depend on the AutoJs6 process. When a script is registered, the plugin wakes AutoJs6 to start it at ring time.

#### Why does the plugin need the exact alarm and full screen alert permissions?

Exact alarms keep ring times accurate (alarm clock apps receive this permission at install time), the full screen alert shows the ringing screen on the lock screen, and notifications show the ringing alarm. If the system revokes one of them, the permission page of the plugin explains how to turn it back on.

******

### Permissions and security

******

The plugin keeps its permission surface as small as possible:

- Protected entry points: the capability service and the activation entry are protected by the AutoJs6 plugin permission (`org.autojs.permission.PLUGIN`), so other apps cannot call them directly; the entry that accepts system clock requests requires the caller to hold `com.android.alarm.permission.SET_ALARM`.
- Network: version 1.0.0 requests no network permission; alarms, timers and the stopwatch work entirely on the device.
- Storage: alarms are stored in the private directory of the plugin and are excluded from cloud backup; local ringtones are imported through the system file picker without a storage permission.
- Script access: through the `alarms` module, scripts can read and change every alarm of the plugin, including those created on its screens; registered scripts are stored in AutoJs6, and the plugin screens only mark an alarm as having a script binding and can remove it.

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
native libraries: none (a single APK for every ABI; the 16 KB page size check does not apply)
```

******

### Roadmap

******

Plans, design decisions and progress are tracked as a checklist in ROADMAP.md. Unchecked items are plans, not features of the current version.

- [View ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/ROADMAP.md)

******

### Release history

******

#### v1.0.0

_2026/11/05_

- `Hint` Development preview: the AutoJs6 Plugin Center can discover the plugin; alarms, timers, the stopwatch, the plugin screens and the alarms script module are not delivered yet
- `Hint` Requires AutoJs6 6.8.1 (6641) or later (the exact minimum build will be filled in once the host changes land)
- `Hint` The current icon is a candidate design and may still change
- `Feature` Plugin skeleton: Wake activation protocol, Plugin Center identity service and alarm capability service
- `Feature` No native libraries: a single APK for every device
- `Feature` README, Plugin Center instructions and changelogs in 10 languages, generated from JSON sources
- `Dependency` Attach AndroidX Core KTX 1.15.0
- `Dependency` Attach lib-recur 0.17.1 (calculates the repeat rules of alarms)
- `Dependency` Attach Desugar JDK Libs 2.1.5 (date and time zone calculations on Android 7.x)

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

The project code is licensed under the [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/LICENSE). Third-party components and data are listed in the third-party notices.

- [Third-party notices (THIRD_PARTY_NOTICES.md)](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Chime-Alarm/blob/master/THIRD_PARTY_NOTICES.md)

******

### Related links

******

- AutoJs6 project: https://github.com/SuperMonster003/AutoJs6

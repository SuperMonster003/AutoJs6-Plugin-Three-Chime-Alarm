"""Contract checks of 3-Chime Alarm through the AutoJs6 `alarms` module on one device (roadmap P2.7).

    py .python/device_smoke.py <serial> --contract [--incompatible-apk <file>]
    py .python/device_contract.py <serial> [--modes full,permission,ui] [--incompatible-apk <file>] [--cleanup]

The host must be a build with the alarm contract (versionCode 6641 or later, roadmap Q12). Every mode pushes one
script to /sdcard/Download/autojs6-chime-contract, runs it through RunIntentActivity after bringing the host launcher
activity to the front, waits for its result file and prints one PASS / FAIL / SKIP line per check.

- unavailable (run by device_smoke before it installs, only when the plugin is not installed): isAvailable() is
  false, add / list fail with PLUGIN_UNAVAILABLE, an unknown key still fails with INVALID_OPTIONS first.
- full: isAvailable() and capabilities(); the examples of roadmap appendix A section 12 (example 1 as written fails
  with PLUGIN_INCOMPATIBLE: the host refuses the option of the 1.2.0 feature holidays before the call; then as in
  1.0.0 with a weekday rule and its binding; the monthly and the shift rule with preview(); alarms.holidays and alarms.timers are absent, the list / skipNext /
  cancelSkip part runs on the test alarms; the pomodoro as a one-shot alarm with speech; alarms.system.setAlarm to
  the plugin's package and next()); the error codes INVALID_OPTIONS, INVALID_RULE, NOT_FOUND (host and plugin side),
  IO_FAILED (missing file on the host, a file that is not audio in the plugin), LIMIT_EXCEEDED (a 21 MiB file on the
  host, a long label, the 301st alarm in the plugin; the bulk alarms are disabled and removed again); a silent
  one-shot alarm rings about 75 s later: the `ring` and `dismiss` listeners, the bound script for `change`, `ring`
  and `dismiss` (it writes the event it was started with), dismiss() from the listener; PLUGIN_DISABLED through the
  host's Plugin Center switch (set off and back by the script). Every alarm the script made is removed.
- permission (API 31 - 32 only): the exact alarm appop of the plugin is denied, an enabled alarm fails with
  PERMISSION_REQUIRED, a disabled one is accepted; the original appop mode is restored afterwards.
- ui (silent, nothing rings): alarms.openPlugin() and each page of AlarmPluginActions.PAGES open the plugin (checked
  in its ChimeOpen log lines), an unknown page fails with INVALID_OPTIONS; a disabled alarm bound to a script is
  unbound in the alarm list through its menu by HomeUnbindProbe (the androidTest APK is installed for the run and
  uninstalled after it), then the host lists no binding for it (roadmap D20, T27, T81).
- incompatible (--incompatible-apk): a build of the plugin whose requiresHostVersion is above the host's versionCode
  is installed over the normal one, add / list fail with PLUGIN_INCOMPATIBLE, then the normal APK is installed again.
- --cleanup: removes alarms left by an interrupted run (tags p27*, sample-*, shift; the labels of the examples).

The alarm rings silently without vibration; its ring screen shows for a few seconds. Nothing here answers a system
dialog or taps the screen; no token or full text is printed.
"""
import argparse
import json
import re
import subprocess
import sys
import tempfile
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import device_smoke as smoke  # noqa: E402

PLUGIN = smoke.PLUGIN
HOST = smoke.HOST
TEST_PACKAGE = PLUGIN + ".test"
RUNNER = f"{TEST_PACKAGE}/androidx.test.runner.AndroidJUnitRunner"
UNBIND_PROBE = f"{PLUGIN}.HomeUnbindProbe#unbind"
PAGES = ("alarms", "timers", "stopwatch", "permissions", "settings")
REMOTE = "/sdcard/Download/autojs6-chime-contract"
RING_LEAD_SECONDS = 75
RING_WAIT_SECONDS = 150
MIN_HOST = 6641

SCRIPT = """
var DIR = '@DIR@';
var cfg = JSON.parse(files.read(DIR + '/config.json'));
var checks = [];
var created = [];
var started = Date.now();
var Store = Packages.org.autojs.autojs.core.plugin.center.PluginEnableStore.INSTANCE;

function record(name, state, detail) {
    checks.push([name, state, String(detail === undefined || detail === null ? '' : detail).slice(0, 240)]);
}
function describe(e) {
    if (e && e.code) return '[' + e.code + '] ' + e.message;
    return String(e && e.message ? e.message : e);
}
function assert(cond, message) { if (!cond) throw new Error(message); }
function check(name, fn) {
    try { record(name, 'PASS', fn()); return true; } catch (e) { record(name, 'FAIL', describe(e)); return false; }
}
function expectCode(name, code, fn) {
    try {
        var value = fn();
        if (value && value.id) created.push(value.id);
        record(name, 'FAIL', 'no error, expected ' + code);
    } catch (e) {
        var ok = e instanceof alarms.AlarmError && e.code === code;
        record(name, ok ? 'PASS' : 'FAIL', ok ? code + (e.hint ? ', with hint' : '') : describe(e));
    }
}
function arr(x) {
    if (x === undefined || x === null) return [];
    if (Array.isArray(x)) return x;
    var out = [];
    for (var i = 0; i < x.size(); i++) out.push(x.get(i));
    return out;
}
function add(options) { var a = alarms.add(options); created.push(a.id); return a; }
function removeCreated() {
    var removed = 0;
    created.forEach(function (id) { try { if (alarms.remove(id)) removed++; } catch (e) { /* already gone */ } });
    created = [];
    return removed;
}
function exists(name) { return files.exists(DIR + '/' + name); }
function readJson(name) { return JSON.parse(files.read(DIR + '/' + name)); }
function save() {
    files.write(DIR + '/result.json', JSON.stringify({ mode: cfg.mode, seconds: Math.round((Date.now() - started) / 1000), checks: checks }));
    files.write(DIR + '/done', '1');
}
function availability(expected, label) {
    check('isAvailable ' + expected + ' (' + label + ')', function () {
        var v = alarms.isAvailable();
        assert(v === expected, 'got ' + v);
        return String(v);
    });
}

function modeUnavailable() {
    availability(false, 'not installed');
    expectCode('add answers PLUGIN_UNAVAILABLE', 'PLUGIN_UNAVAILABLE', function () { return alarms.add({ time: '07:30', label: 'p27-unavailable' }); });
    expectCode('list answers PLUGIN_UNAVAILABLE', 'PLUGIN_UNAVAILABLE', function () { return alarms.list(); });
    expectCode('unknown key fails first with INVALID_OPTIONS', 'INVALID_OPTIONS', function () { return alarms.add({ time: '07:30', bogus: 1 }); });
    save();
}

function modeIncompatible() {
    availability(false, 'incompatible');
    expectCode('add answers PLUGIN_INCOMPATIBLE', 'PLUGIN_INCOMPATIBLE', function () { return alarms.add({ time: '07:30', label: 'p27-incompatible', enabled: false }); });
    expectCode('list answers PLUGIN_INCOMPATIBLE', 'PLUGIN_INCOMPATIBLE', function () { return alarms.list(); });
    save();
}

function modePermission() {
    check('capabilities: exactAlarm false', function () {
        var p = alarms.capabilities().permissions;
        assert(p && p.exactAlarm === false, 'permissions ' + JSON.stringify(p));
        return JSON.stringify(p);
    });
    expectCode('enabled alarm answers PERMISSION_REQUIRED', 'PERMISSION_REQUIRED', function () {
        return alarms.add({ at: Date.now() + 36e5, label: 'p27-permission', tag: 'p27' });
    });
    var off = null;
    check('disabled alarm is accepted', function () {
        off = add({ at: Date.now() + 36e5, label: 'p27-permission-off', tag: 'p27', enabled: false });
        return off.id;
    });
    if (off) expectCode('enabling it answers PERMISSION_REQUIRED', 'PERMISSION_REQUIRED', function () { return alarms.setEnabled(off.id, true); });
    check('test alarms removed', function () { return removeCreated() + ' removed'; });
    save();
}

function modeCleanup() {
    var labels = ['起床', '写月报', '番茄钟', '赶飞机'];
    var removed = 0;
    arr(alarms.list()).forEach(function (x) {
        var tag = String(x.tag || ''), label = String(x.label || '');
        var ours = tag.indexOf('p27') === 0 || tag.indexOf('sample-') === 0 || tag === 'shift' || label.indexOf('p27-') === 0
            || (labels.indexOf(label) >= 0 && x.source !== 'user');
        if (ours) { try { if (alarms.remove(x.id)) removed++; } catch (e) { /* gone */ } }
    });
    record('leftover test alarms removed', 'PASS', removed + ' removed, ' + arr(alarms.list()).length + ' alarms left');
    save();
}

function examples() {
    expectCode('example 1 as written: the host refuses holidays of 1.2.0', 'PLUGIN_INCOMPATIBLE', function () {
        return alarms.add({
            time: '07:30', holidays: { region: 'CN', mode: 'workdays' }, label: '起床',
            sound: { source: 'default', volume: 0.8, rampSeconds: 30 }, vibrate: [0, 500, 500, 500], snooze: { minutes: 9, max: 3 },
        });
    });
    var a = null;
    check('example 1 in 1.0.0: weekdays by repeat', function () {
        a = add({
            time: '07:30', repeat: 'FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR', label: '起床',
            sound: { source: 'default', volume: 0.8, rampSeconds: 30 }, vibrate: [0, 500, 500, 500], snooze: { minutes: 9, max: 3 },
        });
        assert(a.time === '07:30' && a.label === '起床' && a.source === 'script', 'time ' + a.time + ', source ' + a.source);
        assert(a.sound && a.sound.volume === 0.8 && a.sound.rampSeconds === 30, 'sound ' + JSON.stringify(a.sound));
        assert(a.snooze && a.snooze.minutes === 9 && a.snooze.max === 3, 'snooze ' + JSON.stringify(a.snooze));
        assert(arr(a.vibrate).length === 4, 'vibrate ' + JSON.stringify(a.vibrate));
        var next = new Date(a.nextTrigger);
        assert(next.getHours() === 7 && next.getMinutes() === 30 && next.getDay() >= 1 && next.getDay() <= 5, 'next ' + next);
        return 'next ' + next.toISOString() + ', repeat ' + a.repeat;
    });
    if (a) check('example 1: bind ./after-wake.js on dismiss', function () {
        var b = a.bind('./after-wake.js', { events: ['dismiss'] });
        assert(arr(b.events).join(',') === 'dismiss', 'events ' + JSON.stringify(b.events));
        var listed = arr(alarms.bindings(a.id));
        assert(listed.length === 1, listed.length + ' bindings');
        var again = alarms.get(a.id);
        assert(again.hasBindings === true, 'hasBindings ' + again.hasBindings);
        return String(b.script).split('/').pop() + ' [' + arr(b.events).join(',') + '], hasBindings true';
    });

    check('example 2: last Friday of the month 18:00', function () {
        var options = { time: '18:00', repeat: 'FREQ=MONTHLY;BYDAY=-1FR' };
        var m = add({ time: '18:00', repeat: 'FREQ=MONTHLY;BYDAY=-1FR', label: '写月报' });
        var times = arr(alarms.preview(options, 3));
        assert(times.length === 3, times.length + ' previews');
        times.forEach(function (t) {
            var d = new Date(t);
            assert(d.getDay() === 5 && d.getHours() === 18 && d.getMinutes() === 0, 'preview ' + d);
            assert(new Date(t + 7 * 864e5).getMonth() !== d.getMonth(), 'not the last Friday: ' + d);
        });
        assert(m.nextTrigger === times[0], 'nextTrigger ' + m.nextTrigger + ', preview ' + times[0]);
        return m.repeat + ', next ' + new Date(m.nextTrigger).toISOString();
    });
    check('example 2: every 3 days from 2026-11-04 06:10', function () {
        var s = add({ time: '06:10', date: '2026-11-04', repeat: { freq: 'daily', interval: 3 }, tag: 'shift' });
        var times = arr(alarms.preview({ time: '06:10', date: '2026-11-04', repeat: { freq: 'daily', interval: 3 } }, 3));
        var first = new Date(2026, 10, 4, 6, 10).getTime();
        assert(times.length === 3, times.length + ' previews');
        times.forEach(function (t) {
            var days = Math.round((t - first) / 864e5);
            var d = new Date(t);
            assert(days >= 0 && days % 3 === 0 && d.getHours() === 6 && d.getMinutes() === 10, 'preview ' + d + ' (day ' + days + ')');
        });
        assert(s.tag === 'shift' && s.nextTrigger === times[0], 'tag ' + s.tag + ', nextTrigger ' + s.nextTrigger);
        return s.repeat + ', next ' + new Date(s.nextTrigger).toISOString();
    });
    check('example 2: alarms.holidays absent in 1.0.0', function () {
        assert(alarms.holidays === undefined || alarms.holidays === null, typeof alarms.holidays);
        return 'alarms.holidays ' + typeof alarms.holidays;
    });
    check('example 2: list({ source: user, enabled: true })', function () {
        var users = arr(alarms.list({ source: 'user', enabled: true }));
        users.forEach(function (x) { assert(x.source === 'user' && x.enabled === true, 'listed ' + x.source + ' ' + x.enabled); });
        return users.length + ' user alarms (left alone; skipNext runs on the shift test alarm)';
    });
    check('example 2: skipNext and cancelSkip of listed alarms', function () {
        var listed = arr(alarms.list({ tag: 'shift', enabled: true }));
        assert(listed.length >= 1, 'none listed');
        var skipped = listed.map(function (x) { return x.skipNext(); });
        skipped.forEach(function (x) { assert(x.skipping === true, 'skipping ' + x.skipping); });
        var restored = skipped.map(function (x) { return x.cancelSkip(); });
        restored.forEach(function (x) { assert(x.skipping === false, 'after cancelSkip ' + x.skipping); });
        return listed.length + ' skipped and restored';
    });

    check('example 3: alarms.timers absent in 1.0.0', function () {
        assert(alarms.timers === undefined || alarms.timers === null, typeof alarms.timers);
        return 'alarms.timers ' + typeof alarms.timers;
    });
    check('example 3 in 1.0.0: pomodoro as a one-shot alarm', function () {
        var due = Date.now() + 25 * 60e3;
        var p = add({ at: due, label: '番茄钟', speak: { text: '休息一下', mode: 'only' } });
        assert(Math.abs(p.nextTrigger - due) < 1000, 'nextTrigger ' + p.nextTrigger + ', due ' + due);
        assert(p.speak && p.speak.text === '休息一下' && p.speak.mode === 'only', 'speak ' + JSON.stringify(p.speak));
        return 'in ' + Math.round((p.nextTrigger - Date.now()) / 1000) + ' s, speak mode ' + p.speak.mode;
    });
    check('example 3: alarms.system.setAlarm to 3-Chime Alarm', function () {
        alarms.system.setAlarm({ hour: 6, minute: 50, label: '赶飞机', package: cfg.pkg });
        var found = null;
        for (var i = 0; i < 20 && !found; i++) {
            sleep(500);
            found = arr(alarms.list({ source: 'intent' })).filter(function (x) { return x.label === '赶飞机' && x.time === '06:50'; })[0] || null;
        }
        assert(found, 'not listed after 10 s');
        created.push(found.id);
        return 'saved with source ' + found.source + ', enabled ' + found.enabled;
    });
    check('example 3: alarms.system.next()', function () {
        var next = alarms.system.next();
        assert(next && next.triggerTime > Date.now(), 'next ' + JSON.stringify(next));
        return new Date(next.triggerTime).toISOString() + ' ' + next.packageName;
    });
}

function errors() {
    expectCode('INVALID_OPTIONS: unknown key', 'INVALID_OPTIONS', function () { return alarms.add({ time: '07:30', ring: true }); });
    expectCode('INVALID_OPTIONS: time and at together', 'INVALID_OPTIONS', function () { return alarms.add({ time: '07:30', at: Date.now() + 6e4 }); });
    expectCode('INVALID_RULE: unknown FREQ', 'INVALID_RULE', function () { return alarms.add({ time: '07:30', repeat: 'FREQ=SOMETIMES' }); });
    expectCode('NOT_FOUND: update of an unknown id (host)', 'NOT_FOUND', function () { return alarms.update('p27-missing-id', { label: 'x' }); });
    expectCode('NOT_FOUND: skipNext of an unknown id (plugin)', 'NOT_FOUND', function () { return alarms.skipNext('p27-missing-id'); });
    check('get of an unknown id is null', function () {
        var g = alarms.get('p27-missing-id');
        assert(g === null || g === undefined, 'got ' + JSON.stringify(g));
        return 'null';
    });
    expectCode('IO_FAILED: missing ringtone file (host)', 'IO_FAILED', function () {
        return alarms.add({ time: '07:30', enabled: false, label: 'p27-io', sound: { source: DIR + '/missing.mp3' } });
    });
    expectCode('IO_FAILED: ringtone that is not audio (plugin)', 'IO_FAILED', function () {
        return alarms.add({ time: '07:30', enabled: false, label: 'p27-io', sound: { source: DIR + '/not-audio.mp3' } });
    });
    expectCode('LIMIT_EXCEEDED: 21 MiB ringtone (host)', 'LIMIT_EXCEEDED', function () {
        return alarms.add({ time: '07:30', enabled: false, label: 'p27-limit', sound: { source: DIR + '/big.mp3' } });
    });
    expectCode('LIMIT_EXCEEDED: label of 101 characters', 'LIMIT_EXCEEDED', function () {
        return alarms.add({ time: '07:30', enabled: false, label: new Array(102).join('x') });
    });
    check('LIMIT_EXCEEDED: the 301st alarm (plugin)', function () {
        var t = Date.now();
        var before = arr(alarms.list()).length;
        var bulk = [], code = null;
        while (code === null && before + bulk.length < 320) {
            try {
                bulk.push(alarms.add({ time: '05:00', enabled: false, tag: 'p27-bulk', label: 'p27-bulk-' + bulk.length }).id);
            } catch (e) {
                code = e.code || describe(e);
            }
        }
        var total = before + bulk.length;
        var removed = 0;
        bulk.forEach(function (id) { try { if (alarms.remove(id)) removed++; } catch (e) { /* counted below */ } });
        assert(code === 'LIMIT_EXCEEDED' && total === 300, 'stopped with ' + code + ' at ' + total + ' alarms');
        assert(removed === bulk.length, removed + ' of ' + bulk.length + ' removed');
        return 'refused at alarm ' + (total + 1) + ' (' + bulk.length + ' added and removed in ' + Math.round((Date.now() - t) / 1000) + ' s)';
    });
}

function disabledCheck() {
    var before = Store.isEnabled(context, cfg.pkg, true);
    try {
        Store.setEnabled(context, cfg.pkg, false);
        availability(false, 'switched off in the Plugin Center');
        expectCode('list answers PLUGIN_DISABLED', 'PLUGIN_DISABLED', function () { return alarms.list(); });
        expectCode('add answers PLUGIN_DISABLED', 'PLUGIN_DISABLED', function () { return alarms.add({ time: '07:30', label: 'p27-disabled', enabled: false }); });
    } finally {
        Store.setEnabled(context, cfg.pkg, before);
    }
    availability(true, 'switched on again');
}

function ringCheck(done) {
    var lead = cfg.ringLeadSeconds;
    var dueAt = Date.now() + lead * 1000;
    var r = null;
    var seen = {};
    if (!check('ring alarm with a bound script', function () {
        r = add({ at: dueAt, label: 'p27-ring', tag: 'p27', sound: { source: 'silent' }, vibrate: false });
        var b = r.bind('./bound.js', { events: ['ring', 'dismiss', 'change'] });
        r = r.update({ label: 'p27-ring-bound' });
        return 'due in ' + lead + ' s, events [' + arr(b.events).join(',') + ']';
    })) { done(); return; }
    alarms.on('ring', function (e) {
        if (e.alarmId !== r.id) return;
        seen.ring = Date.now();
        seen.ringing = arr(alarms.ringing()).filter(function (x) { return x.id === r.id; }).length;
        try { seen.dismissed = alarms.dismiss(r.id); } catch (err) { seen.dismissError = describe(err); }
    });
    alarms.on('dismiss', function (e) {
        if (e.alarmId !== r.id) return;
        seen.dismiss = Date.now();
        seen.by = e.by;
    });
    var deadline = dueAt + cfg.ringWaitSeconds * 1000;
    var timer = setInterval(function () {
        var complete = seen.dismiss && exists('bound-ring.json') && exists('bound-dismiss.json') && exists('bound-change.json');
        if (!complete && Date.now() < deadline) return;
        clearInterval(timer);
        alarms.off();
        try {
            ringResults(r, dueAt, seen);
        } catch (err) {
            record('ring results', 'FAIL', describe(err));
        }
        done();
    }, 1000);
}

function ringResults(r, dueAt, seen) {
    record('ring event reaches the listener', seen.ring ? 'PASS' : 'FAIL',
        seen.ring ? Math.round((seen.ring - dueAt) / 100) / 10 + ' s after due, ringing() lists it: ' + seen.ringing : 'no ring event');
    record('dismiss() from the listener', seen.dismiss && !seen.dismissError ? 'PASS' : 'FAIL',
        seen.dismissError || ('answered ' + seen.dismissed + ', dismiss event by ' + seen.by));
    ['change', 'ring', 'dismiss'].forEach(function (type) {
        var name = 'bound-' + type + '.json';
        if (!exists(name)) { record('bound script started for ' + type, 'FAIL', 'not started'); return; }
        var got = readJson(name);
        var e = got.event || {};
        var ok = e.type === type && e.alarmId === r.id;
        record('bound script started for ' + type, ok ? 'PASS' : 'FAIL', 'currentEvent ' + e.type + (e.by ? ' by ' + e.by : '') + (ok ? '' : ', alarm ' + e.alarmId));
    });
    check('one-shot alarm disabled after the ring', function () {
        var after = alarms.get(r.id);
        assert(after && after.enabled === false, 'enabled ' + (after && after.enabled));
        return 'enabled false';
    });
}

function modeFull() {
    if (!check('isAvailable true (installed and enabled)', function () { var v = alarms.isAvailable(); assert(v === true, 'got ' + v); return 'true'; })) {
        save();
        return;
    }
    check('capabilities', function () {
        var c = alarms.capabilities();
        var features = arr(c.features);
        assert(c.contractVersion === 1 && features.indexOf('alarms') >= 0 && c.packageName === cfg.pkg, 'contract ' + c.contractVersion + ', features ' + features);
        return c.pluginVersionName + ' (' + c.pluginVersionCode + '), features [' + features.join(',') + '], permissions ' + JSON.stringify(c.permissions);
    });
    examples();
    errors();
    ringCheck(function () {
        try {
            disabledCheck();
        } catch (e) {
            record('PLUGIN_DISABLED check', 'FAIL', describe(e));
        }
        check('test alarms removed', function () {
            var n = removeCreated();
            var left = arr(alarms.list()).filter(function (x) { return String(x.tag || '').indexOf('p27') === 0 || String(x.label || '').indexOf('p27-') === 0; });
            assert(left.length === 0, left.length + ' p27 alarms left');
            return n + ' removed';
        });
        save();
    });
}

function modeUi() {
    [null].concat(cfg.pages).forEach(function (page) {
        check('openPlugin(' + (page === null ? '' : "'" + page + "'") + ') accepted', function () {
            if (page === null) alarms.openPlugin(); else alarms.openPlugin(page);
            sleep(1500);
            return 'started';
        });
    });
    expectCode("openPlugin('unknown-page') answers INVALID_OPTIONS", 'INVALID_OPTIONS', function () { return alarms.openPlugin('unknown-page'); });
    var u = null;
    check('a disabled alarm bound to ./bound.js', function () {
        var a = add({ time: '06:00', label: 'p27-unbind', tag: 'p27', enabled: false });
        a.bind('./bound.js', { events: ['ring', 'dismiss'] });
        var again = alarms.get(a.id);
        assert(again.hasBindings === true && again.enabled === false, 'hasBindings ' + again.hasBindings + ', enabled ' + again.enabled);
        u = a;
        return 'hasBindings true, enabled false';
    });
    if (u) {
        files.write(DIR + '/stage-unbind', u.id);
        var end = Date.now() + 240e3;
        while (!exists('go') && Date.now() < end) sleep(1000);
        check('host bindings gone after the unbind in the alarm list', function () {
            assert(exists('go'), 'no go file in 240 s');
            var listed = arr(alarms.bindings(u.id));
            var again = alarms.get(u.id);
            assert(listed.length === 0 && again.hasBindings === false, listed.length + ' bindings, hasBindings ' + again.hasBindings);
            return '0 bindings, hasBindings false';
        });
    }
    check('test alarms removed', function () { return removeCreated() + ' removed'; });
    save();
}

function modeSwitch(on) {
    var before = Store.isEnabled(context, cfg.pkg, false);
    Store.setEnabled(context, cfg.pkg, on);
    var now = Store.isEnabled(context, cfg.pkg, !on);
    record('Plugin Center switch ' + (on ? 'on' : 'off'), now === on ? 'PASS' : 'FAIL', 'was ' + before + ', now ' + now);
    save();
}

try {
    if (cfg.mode === 'switch-on' || cfg.mode === 'switch-off') modeSwitch(cfg.mode === 'switch-on');
    else if (cfg.mode === 'unavailable') modeUnavailable();
    else if (cfg.mode === 'incompatible') modeIncompatible();
    else if (cfg.mode === 'permission') modePermission();
    else if (cfg.mode === 'cleanup') modeCleanup();
    else if (cfg.mode === 'ui') modeUi();
    else modeFull();
} catch (e) {
    record('script', 'FAIL', describe(e));
    try { removeCreated(); } catch (ignored) { /* best effort */ }
    save();
}
"""

BOUND = """
var e = alarms.currentEvent();
var type = e && e.type ? String(e.type) : 'none';
files.write('@DIR@/bound-' + type + '.json', JSON.stringify({ event: e, at: Date.now() }));
"""


class Report:
    def __init__(self):
        self.passed = 0
        self.failed = 0

    def line(self, name, state, detail=""):
        if state == "FAIL":
            self.failed += 1
        elif state == "PASS":
            self.passed += 1
        print(f"    {name}: {state} {detail}".rstrip())
        return state != "FAIL"


def adb(serial, *args, check=False, timeout=120):
    return smoke.adb(serial, *args, check=check, timeout=timeout)


def installed(serial):
    return "package:" in adb(serial, "shell", "pm", "path", PLUGIN)


def push_files(serial, mode):
    adb(serial, "shell", "mkdir", "-p", REMOTE)
    adb(serial, "shell", "rm", "-f", f"{REMOTE}/done", f"{REMOTE}/result.json", f"{REMOTE}/bound-ring.json",
        f"{REMOTE}/bound-dismiss.json", f"{REMOTE}/bound-change.json", f"{REMOTE}/bound-none.json", f"{REMOTE}/stage-unbind", f"{REMOTE}/go")
    folder = Path(tempfile.gettempdir()) / f"chime-contract-{serial.replace(':', '_')}"
    folder.mkdir(exist_ok=True)
    config = {"mode": mode, "pkg": PLUGIN, "ringLeadSeconds": RING_LEAD_SECONDS, "ringWaitSeconds": RING_WAIT_SECONDS, "pages": list(PAGES)}
    texts = {
        "contract.js": SCRIPT.replace("@DIR@", REMOTE),
        "bound.js": BOUND.replace("@DIR@", REMOTE),
        "after-wake.js": BOUND.replace("@DIR@", REMOTE),
        "config.json": json.dumps(config),
        "not-audio.mp3": "This file is plain text, not audio." + chr(10),
    }
    for name, text in texts.items():
        (folder / name).write_text(text, encoding="utf-8", newline="")
        adb(serial, "push", str(folder / name), f"{REMOTE}/{name}", check=True)
        (folder / name).unlink()
    folder.rmdir()
    if mode == "full" and "21" != adb(serial, "shell", f"ls -l {REMOTE}/big.mp3 2>/dev/null | awk '{{print int($5 / 1048576)}}'").strip():
        adb(serial, "shell", "dd", "if=/dev/zero", f"of={REMOTE}/big.mp3", "bs=1048576", "count=21")


def run_mode(serial, mode, report, timeout=900, stage=None):
    """Runs the script in [mode] and prints its checks; False when it gave no result. [stage] gets the alarm id the
    script writes to `stage-unbind` (mode ui) and must write `go` when it is done."""
    print(f"  contract {mode}:")
    push_files(serial, mode)
    adb(serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP")
    adb(serial, "shell", "am", "start", "-n", smoke.launcher(serial))
    time.sleep(2)
    adb(serial, "shell", "am", "start", "-a", "android.intent.action.VIEW", "-t", "application/x-javascript",
        "-d", f"file://{REMOTE}/contract.js", "-n", f"{HOST}/org.autojs.autojs.external.open.RunIntentActivity")
    deadline = time.time() + timeout
    while time.time() < deadline and adb(serial, "shell", "cat", f"{REMOTE}/done").strip() != "1":
        staged = adb(serial, "shell", f"cat {REMOTE}/stage-unbind 2>/dev/null").strip() if stage else ""
        if staged:
            adb(serial, "shell", "rm", "-f", f"{REMOTE}/stage-unbind")
            try:
                stage(staged)
            finally:
                adb(serial, "shell", f"echo 1 > {REMOTE}/go")
        time.sleep(3)
    raw = adb(serial, "shell", "cat", f"{REMOTE}/result.json")
    if not raw.startswith("{"):
        return report.line(f"contract {mode}", "FAIL", f"no result in {timeout} s (unlock the device; AutoJs6 needs all-files access)")
    result = json.loads(raw)
    for name, state, detail in result["checks"]:
        report.line(name, state, detail)
    print(f"    ({mode}: {result.get('seconds')} s)")
    return True


def appop_mode(serial):
    match = re.search(r"SCHEDULE_EXACT_ALARM: (\w+)", adb(serial, "shell", "appops", "get", PLUGIN, "SCHEDULE_EXACT_ALARM"))
    return match.group(1) if match else "default"


def permission(serial, report):
    sdk = int(adb(serial, "shell", "getprop", "ro.build.version.sdk").strip() or 0)
    if not 31 <= sdk <= 32:
        report.line("contract permission", "SKIP", f"API {sdk}: the exact alarm permission is revocable on API 31 - 32 only")
        return
    original = appop_mode(serial)
    try:
        adb(serial, "shell", "appops", "set", PLUGIN, "SCHEDULE_EXACT_ALARM", "deny")
        time.sleep(2)
        run_mode(serial, "permission", report, timeout=300)
    finally:
        adb(serial, "shell", "appops", "set", PLUGIN, "SCHEDULE_EXACT_ALARM", original)
    report.line(f"exact alarm appop restored ({original})", "PASS" if appop_mode(serial) == original else "FAIL", appop_mode(serial))


def unbind_in_list(serial, alarm_id, report):
    """Runs HomeUnbindProbe: the alarm list unbinds [alarm_id] through its menu, as a user would (roadmap D20, T27)."""
    out = adb(serial, "shell", "am", "instrument", "-w", "-r", "-e", "class", UNBIND_PROBE, "-e", "chimeUnbind", alarm_id, RUNNER, timeout=180)
    values = dict(re.findall(r"INSTRUMENTATION_STATUS: (chime\.[\w.-]+)=(.*)", out))
    if not values:
        failure = re.findall(r"INSTRUMENTATION_STATUS: stack=(.*)", out) or out.splitlines()[-1:]
        report.line("alarm list unbind (HomeUnbindProbe)", "FAIL", (failure or ["no output"])[0][:200])
        return
    report.line("alarm list: the alarm shows as bound", "PASS" if values.get("chime.boundBefore") == "true" else "FAIL", f"boundBefore {values.get('chime.boundBefore')}")
    report.line("alarm list: unbind drops the plugin's hint", "PASS" if values.get("chime.boundAfter") == "false" else "FAIL", f"boundAfter {values.get('chime.boundAfter')}")
    report.line("alarm list: the binding mark is gone", "PASS" if values.get("chime.markGone") == "true" else "FAIL", f"markGone {values.get('chime.markGone')}")


def ui(serial, report):
    """The ui mode: every openPlugin page reaches the plugin (its ChimeOpen log lines) and the alarm list unbinds an
    alarm through the host (HomeUnbindProbe of the androidTest APK, installed for the run and uninstalled after it)."""
    tests = sorted((smoke.ROOT / "app" / "build" / "outputs" / "apk" / "androidTest" / "debug").glob("*.apk"), key=lambda p: p.stat().st_mtime)
    if not tests:
        report.line("contract ui", "FAIL", "build :app:assembleDebugAndroidTest first")
        return
    out = subprocess.run(["adb", "-s", serial, "install", "-r", "-t", str(tests[-1])], capture_output=True, text=True, timeout=300)
    if not report.line(f"install {tests[-1].name}", "PASS" if out.returncode == 0 and "Success" in out.stdout else "FAIL", (out.stdout + out.stderr).strip()[-120:]):
        return
    try:
        since = adb(serial, "shell", "date", "+%m-%d_%H:%M:%S.000").strip().replace("_", " ")
        run_mode(serial, "ui", report, timeout=420, stage=lambda alarm_id: unbind_in_list(serial, alarm_id, report))
        opened = re.findall(r"open page=(\S+)", adb(serial, "shell", f"logcat -d -s ChimeOpen -t '{since}'"))
        expected = ["(none)", *PAGES]
        report.line("openPlugin pages reached the plugin", "PASS" if opened == expected else "FAIL", f"[{', '.join(opened)}]")
    finally:
        out = subprocess.run(["adb", "-s", serial, "uninstall", TEST_PACKAGE], capture_output=True, text=True, timeout=120)
        report.line("androidTest APK uninstalled", "PASS" if out.returncode == 0 else "FAIL")


def incompatible(serial, apk, normal, report):
    ok, detail, _ = smoke.install(serial, Path(apk))
    if not report.line("install the incompatible build", "PASS" if ok else "FAIL", f"{Path(apk).name}: {detail}"):
        return
    try:
        run_mode(serial, "incompatible", report, timeout=300)
    finally:
        ok, detail, _ = smoke.install(serial, normal)
        report.line("normal build installed again", "PASS" if ok else "FAIL", f"{normal.name}: {detail}, versionCode {smoke.installed_version(serial, PLUGIN)}")


def host_ready(serial, report):
    host = smoke.installed_version(serial, HOST)
    return report.line("host with the alarm contract", "PASS" if host and host >= MIN_HOST else "FAIL", f"AutoJs6 versionCode {host}, needs {MIN_HOST}")


def before_install(serial, report):
    """The unavailable mode, while the plugin is not installed (device_smoke calls it before installing)."""
    if not host_ready(serial, report):
        return False
    if installed(serial):
        report.line("contract unavailable", "SKIP", "the plugin is installed")
        return True
    return run_mode(serial, "unavailable", report, timeout=300)


def after_smoke(serial, args, report, apk):
    """The full, permission and incompatible modes on the installed plugin (device_smoke calls it after the smoke)."""
    if not host_ready(serial, report):
        return False
    smoke.adb(serial, "shell", "am", "force-stop", PLUGIN, check=False)
    run_mode(serial, "full", report)
    permission(serial, report)
    if getattr(args, "incompatible_apk", None):
        incompatible(serial, args.incompatible_apk, apk, report)
    adb(serial, "shell", "rm", "-rf", REMOTE)
    return report.failed == 0


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("serial")
    parser.add_argument("--modes", default="full,permission",
                        help="comma-separated: unavailable, full, permission, ui, switch-on / switch-off (the plugin's Plugin Center switch, "
                             "as the switch on the screen sets it) (default: full,permission)")
    parser.add_argument("--incompatible-apk", metavar="FILE", help="a build whose requiresHostVersion is above the host's versionCode")
    parser.add_argument("--apk", help="the normal APK installed again after --incompatible-apk (default: the newest debug APK)")
    parser.add_argument("--cleanup", action="store_true", help="only remove the alarms an interrupted run left")
    args = parser.parse_args(argv)
    report = Report()
    print(f"{smoke.label(args.serial)}:")
    if not host_ready(args.serial, report):
        return 1
    if args.cleanup:
        run_mode(args.serial, "cleanup", report, timeout=300)
    else:
        for mode in [m.strip() for m in args.modes.split(",") if m.strip()]:
            if mode == "permission":
                permission(args.serial, report)
            elif mode == "ui":
                ui(args.serial, report)
            else:
                run_mode(args.serial, mode, report)
        if args.incompatible_apk:
            incompatible(args.serial, args.incompatible_apk, smoke.pick_apk(args.apk), report)
    adb(args.serial, "shell", "rm", "-rf", REMOTE)
    print(f"{args.serial}: CONTRACT {'PASS' if report.failed == 0 else 'FAIL'} ({report.passed} passed, {report.failed} failed)")
    return 0 if report.failed == 0 else 1


if __name__ == "__main__":
    sys.exit(main())

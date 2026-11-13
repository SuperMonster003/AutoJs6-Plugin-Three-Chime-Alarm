"""The plugin tools of 3-Chime Alarm on one device (roadmap P4.1, since 1.1.0 also P7.6, since 1.2.0 also P8.7).

    py .python/device_tools.py <serial> [--modes provider,host] [--apk <debug apk>] [--no-install]

- provider: installs the debug APK over the installed one (`adb install -r`, never uninstalled) and the androidTest APK,
  runs ChimeToolServiceTest (the provider through its Binder interface, every call through Parcels: discovery and the
  capability, the catalog, the nine alarm tools on test alarms 30 days ahead, the three timer tools on a 25-minute timer,
  `holiday_check` and the `holidays` option on a disabled alarm, refusals and tool errors), then uninstalls the
  androidTest APK.
- host: needs a host with the alarm contract (versionCode 6641 or later, roadmap Q12): pushes one script to
  /sdcard/Download/autojs6-chime-tools and runs it through RunIntentActivity. It checks `plugins.tools.list()` (the nine
  `chime_alarm_*`, the three `chime_timer_*` tools and `chime_holiday_check`, their risks, no stopwatch tool) and
  `plugins.tools.providers()` (the plugin as an available official provider), then
  calls every tool through `plugins.tools.call` on alarms tagged p41-host (add -> list -> update -> set_enabled ->
  skip_next -> upcoming -> remove), checks that `alarms.get` sees the source `tool`, and that the host refuses an unknown
  argument and an out-of-range count before the plugin while the plugin refuses a local ringtone path and an hourly rule.
  The timer tools run on a 25-minute timer labelled p76-host (start -> list -> pause -> add -> resume -> reset -> cancel);
  the host refuses a duration below 1 s and the plugin refuses text without a unit and add without duration. Without the
  exact alarm permission only NOT_READY is checked. `chime_holiday_check` answers CN 2026-10-01 (a holiday with a name),
  CN 2026-10-10 (a makeup workday) and today, and refuses CN 2019 with HOLIDAY_DATA_MISSING; `chime_alarm_add` takes a
  `holidays` filter (region canonical), `chime_alarm_update` replaces it whole and clears it, and the plugin refuses
  `workdays` with `at`. Every alarm and timer the script made is removed; nothing rings (the alarms lie 30 days ahead or
  stay disabled, the timer is cancelled long before it ends, dismiss / snooze name an unknown id).

Output: one PASS / FAIL / SKIP line per check with counts and codes only; never tokens, labels of other alarms or full
tool lists. Nothing here taps the screen or answers a dialog.
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
TEST_CLASS = f"{PLUGIN}.ChimeToolServiceTest"
REMOTE = "/sdcard/Download/autojs6-chime-tools"
MIN_HOST = 6641
TOOLS = [
    "alarm_add", "alarm_list", "alarm_update", "alarm_remove", "alarm_set_enabled", "alarm_skip_next", "alarm_upcoming", "alarm_dismiss", "alarm_snooze",
    "timer_start", "timer_list", "timer_control", "holiday_check",
]

SCRIPT = """
var DIR = '@DIR@';
var TAG = 'p41-host';
var checks = [];
var created = [];
var timers = [];
var started = Date.now();
function record(name, state, detail) {
    checks.push([name, state, String(detail === undefined || detail === null ? '' : detail).slice(0, 200)]);
}
function assert(cond, message) { if (!cond) throw new Error(message); }
function check(name, fn) {
    try { record(name, 'PASS', fn()); return true; } catch (e) { record(name, 'FAIL', String(e && e.message ? e.message : e)); return false; }
}
function plain(x) { return JSON.parse(JSON.stringify(x)); }
function call(name, args) {
    var answer = plain(plugins.tools.call('chime_' + name, args));
    var result = answer.result;
    if (typeof result === 'string') result = JSON.parse(result);
    return { result: result, ms: answer.durationMs };
}
function refused(name, args, needle) {
    try {
        call(name, args);
    } catch (e) {
        var message = String(e && e.message ? e.message : e);
        assert(message.indexOf(needle) >= 0, 'refused without ' + needle + ': ' + message.slice(0, 120));
        return needle;
    }
    throw new Error('no error, expected ' + needle);
}
function day(offset) {
    var d = new Date(Date.now() + offset * 86400000);
    function two(n) { return (n < 10 ? '0' : '') + n; }
    return d.getFullYear() + '-' + two(d.getMonth() + 1) + '-' + two(d.getDate());
}
var date = day(30);
var exact = !!alarms.capabilities().permissions.exactAlarm;

check('chime tools listed', function () {
    var mine = plain(plugins.tools.list()).filter(function (t) { return t.shortName === 'chime'; });
    var names = mine.map(function (t) { return t.tool; }).sort();
    assert(names.join(',') === @TOOLS@.slice().sort().join(','), 'got ' + names.join(','));
    var risks = {};
    mine.forEach(function (t) { risks[t.tool] = t.risk; });
    assert(risks.alarm_list === 'readonly' && risks.alarm_upcoming === 'readonly' && risks.alarm_add === 'write' && risks.alarm_remove === 'destructive', JSON.stringify(risks));
    assert(risks.timer_start === 'write' && risks.timer_list === 'readonly' && risks.timer_control === 'write', JSON.stringify(risks));
    assert(risks.holiday_check === 'readonly', 'holiday_check ' + risks.holiday_check);
    assert(names.filter(function (n) { return n.indexOf('stopwatch') >= 0; }).length === 0, 'a stopwatch tool');
    return names.length + ' tools, list/upcoming/timer_list/holiday_check readonly, remove destructive, no stopwatch';
});
check('provider available and official', function () {
    var mine = plain(plugins.tools.providers()).filter(function (p) { return p.packageName === '@PKG@'; });
    assert(mine.length === 1, 'providers ' + mine.length);
    assert(String(mine[0].status).toUpperCase() === 'AVAILABLE' && String(mine[0].trust).toUpperCase() === 'OFFICIAL', mine[0].status + ' ' + mine[0].trust + ' ' + (mine[0].detail || ''));
    return mine[0].status + ' ' + mine[0].trust + ', shortName ' + mine[0].shortName;
});
var id = null, second = null;
check('alarm_add', function () {
    var a = call('alarm_add', { time: '04:10', date: date, repeat: [1, 2, 3, 4, 5], label: 'p41 host', tag: TAG, enabled: exact,
        sound: { source: 'silent' }, snooze: { minutes: 5 } });
    var alarm = a.result.alarm;
    id = alarm.id; created.push(id);
    assert(alarm.time === '04:10' && alarm.repeat === 'FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR' && alarm.source === 'tool', JSON.stringify(alarm).slice(0, 160));
    var b = call('alarm_add', { at: date + 'T04:20:00', tag: TAG, enabled: false });
    second = b.result.alarm.id; created.push(second);
    return 'two alarms, exact ' + exact + ', ' + a.ms + ' ms';
});
check('alarms.get sees source tool', function () {
    var a = alarms.get(id);
    assert(a !== null && a.source === 'tool' && a.updatedBy === 'tool', a === null ? 'null' : a.source + '/' + a.updatedBy);
    return 'source ' + a.source;
});
check('alarm_list', function () {
    var r = call('alarm_list', { tag: TAG, source: 'tool' }).result;
    assert(r.total === 2 && r.alarms.length === 2 && r.truncated === false, 'total ' + r.total);
    return 'total ' + r.total;
});
check('alarm_update', function () {
    var r = call('alarm_update', { id: id, label: 'p41 host changed', sound: { rampSeconds: 20 } }).result.alarm;
    assert(r.label === 'p41 host changed' && r.sound.source === 'silent' && r.sound.rampSeconds === 20 && r.updatedBy === 'tool', JSON.stringify(r.sound));
    return 'label and sound merged';
});
check('alarm_set_enabled', function () {
    var r = call('alarm_set_enabled', { id: second, enabled: exact }).result.alarm;
    assert(r.enabled === exact, 'enabled ' + r.enabled);
    return 'enabled ' + r.enabled;
});
if (exact) {
    check('alarm_skip_next and cancel', function () {
        assert(call('alarm_skip_next', { id: id }).result.alarm.skipping === true, 'not skipping');
        assert(call('alarm_skip_next', { id: id, cancel: true }).result.alarm.skipping === false, 'still skipping');
        return 'skip and undo';
    });
    check('alarm_upcoming', function () {
        var r = call('alarm_upcoming', { count: 3, tag: TAG }).result.occurrences;
        assert(r.length === 3 && r[0].time.indexOf(date) === 0, r.length + ' ' + (r[0] && r[0].time));
        return r.length + ' rings, first on ' + date;
    });
} else {
    record('alarm_skip_next and alarm_upcoming', 'SKIP', 'no exact alarm permission: the test alarms stay disabled');
}
check('alarm_dismiss / alarm_snooze unknown id', function () {
    assert(call('alarm_dismiss', { id: 'a_missing' }).result.handled === false, 'dismiss handled');
    assert(call('alarm_snooze', { id: 'a_missing', minutes: 3 }).result.handled === false, 'snooze handled');
    return 'handled false twice';
});
check('host refuses an unknown argument', function () { return refused('alarm_add', { time: '04:00', bogus: 1 }, 'bogus'); });
check('host refuses count 101', function () { return refused('alarm_upcoming', { count: 101 }, 'count'); });
check('plugin refuses a local ringtone path', function () { return refused('alarm_add', { time: '04:00', enabled: false, sound: { source: '/sdcard/a.mp3' } }, 'INVALID_OPTIONS'); });
check('plugin refuses an hourly rule', function () { return refused('alarm_add', { time: '04:00', enabled: false, repeat: 'FREQ=HOURLY' }, 'INVALID_RULE'); });
check('plugin answers NOT_FOUND for an unknown id', function () { return refused('alarm_update', { id: 'a_missing', label: 'x' }, 'NOT_FOUND'); });
check('alarm_remove', function () {
    assert(call('alarm_remove', { id: id }).result.removed === true, 'first');
    assert(call('alarm_remove', { id: second }).result.removed === true, 'second');
    assert(call('alarm_remove', { id: id }).result.removed === false, 'again');
    created = [];
    assert(call('alarm_list', { tag: TAG }).result.total === 0, 'left over');
    return 'removed, again false, none left';
});
var TIMER = 'p76-host';
if (exact) {
    var timer = null;
    check('timer_start', function () {
        var r = call('timer_start', { duration: '25m', label: TIMER, sound: { source: 'silent' } });
        timer = r.result.timer;
        timers.push(timer.id);
        assert(timer.id.indexOf('t_') === 0 && timer.state === 'running' && timer.durationMillis === 1500000 && timer.label === TIMER, JSON.stringify(timer).slice(0, 160));
        assert(typeof timer.endsAt === 'number' && typeof timer.endsAtTime === 'string', 'no endsAt');
        return 'running, ' + timer.remaining + ' left, ' + r.ms + ' ms';
    });
    check('timer_list', function () {
        var r = call('timer_list', {}).result.timers;
        assert(r.filter(function (t) { return t.id === timer.id; }).length === 1, 'not listed');
        return r.length + ' timers';
    });
    check('timer_control pause / add / resume / reset', function () {
        var paused = call('timer_control', { id: timer.id, action: 'pause' }).result.timer;
        assert(paused.state === 'paused' && paused.endsAt === undefined, 'pause ' + paused.state);
        var shorter = call('timer_control', { id: timer.id, action: 'add', duration: '-1m' }).result.timer;
        assert(shorter.remainingMillis === paused.remainingMillis - 60000, 'add -1m ' + shorter.remainingMillis);
        var longer = call('timer_control', { id: timer.id, action: 'add', duration: 120000 }).result.timer;
        assert(longer.remainingMillis === paused.remainingMillis + 60000, 'add 120000 ' + longer.remainingMillis);
        assert(call('timer_control', { id: timer.id, action: 'resume' }).result.timer.state === 'running', 'resume');
        var reset = call('timer_control', { id: timer.id, action: 'reset' }).result.timer;
        assert(reset.state === 'paused' && reset.remainingMillis === 1500000, 'reset ' + reset.state + ' ' + reset.remainingMillis);
        return 'four actions';
    });
    check('host refuses duration 999', function () { return refused('timer_start', { duration: 999 }, 'duration'); });
    check('plugin refuses text without a unit', function () { return refused('timer_start', { duration: '25' }, 'INVALID_OPTIONS'); });
    check('plugin refuses add without duration', function () { return refused('timer_control', { id: timer.id, action: 'add' }, 'add needs duration'); });
    check('timer NOT_FOUND for an unknown id', function () { return refused('timer_control', { id: 't_missing', action: 'pause' }, 'NOT_FOUND'); });
    check('timer_control cancel', function () {
        assert(call('timer_control', { id: timer.id, action: 'cancel' }).result.removed === true, 'first');
        assert(call('timer_control', { id: timer.id, action: 'cancel' }).result.removed === false, 'again');
        timers = [];
        return 'removed, again false';
    });
} else {
    check('timer_start NOT_READY without exact alarms', function () { return refused('timer_start', { duration: '25m', label: TIMER }, 'PERMISSION_REQUIRED'); });
}
check('holiday_check holiday / makeup workday / today', function () {
    var national = call('holiday_check', { date: '2026-10-01', region: 'cn' });
    var r = national.result;
    assert(r.region === 'CN' && r.isHoliday === true && r.isWorkday === false && typeof r.name === 'string' && r.name.length > 0, JSON.stringify(r).slice(0, 160));
    var makeup = call('holiday_check', { date: '2026-10-10', region: 'CN' }).result;
    assert(makeup.isWorkday === true && makeup.isHoliday === false && makeup.name === undefined, JSON.stringify(makeup).slice(0, 160));
    var today = call('holiday_check', {}).result;
    assert(today.date === day(0) && typeof today.region === 'string', JSON.stringify(today).slice(0, 160));
    return 'CN 10-01 holiday, 10-10 makeup workday, today ' + today.region + ' estimated ' + r.estimated + ', ' + national.ms + ' ms';
});
check('plugin refuses a year outside the holiday data', function () { return refused('holiday_check', { date: '2019-01-01', region: 'CN' }, 'HOLIDAY_DATA_MISSING'); });
check('holiday_check refuses a 9-character date', function () { return refused('holiday_check', { date: '2026-10-1' }, 'date'); });
check('alarm_add / alarm_update holidays', function () {
    var a = call('alarm_add', { time: '04:30', date: date, tag: TAG, enabled: false, holidays: { mode: 'workdays', region: 'cn' } }).result.alarm;
    created.push(a.id);
    assert(a.holidays && a.holidays.mode === 'workdays' && a.holidays.region === 'CN', JSON.stringify(a.holidays));
    var u = call('alarm_update', { id: a.id, holidays: { mode: 'skip' } }).result.alarm;
    assert(u.holidays.mode === 'skip' && u.holidays.region === undefined, 'replaced ' + JSON.stringify(u.holidays));
    var c = call('alarm_update', { id: a.id, clear: ['holidays'] }).result.alarm;
    assert(c.holidays === undefined, 'cleared ' + JSON.stringify(c.holidays));
    assert(call('alarm_remove', { id: a.id }).result.removed === true, 'removed');
    created = created.filter(function (x) { return x !== a.id; });
    return 'canonical region, replaced whole, cleared';
});
check('plugin refuses workdays with at', function () { return refused('alarm_add', { at: date + 'T04:30:00', enabled: false, holidays: { mode: 'workdays' } }, 'INVALID_OPTIONS'); });
created.forEach(function (x) { try { alarms.remove(x); } catch (e) { /* gone */ } });
timers.forEach(function (x) { try { call('timer_control', { id: x, action: 'cancel' }); } catch (e) { /* gone */ } });
files.write(DIR + '/result.json', JSON.stringify({ seconds: Math.round((Date.now() - started) / 1000), checks: checks }));
files.write(DIR + '/done', '1');
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


def newest(folder):
    apks = sorted(folder.glob("*.apk"), key=lambda p: p.stat().st_mtime)
    return apks[-1] if apks else None


def provider(serial, args, report):
    print("  provider (ChimeToolServiceTest):")
    if not args.no_install:
        apk = smoke.pick_apk(args.apk)
        ok, detail, _ = smoke.install(serial, apk)
        if not report.line(f"install {apk.name} (-r)", "PASS" if ok else "FAIL", f"{detail}, versionCode {smoke.installed_version(serial, PLUGIN)}"):
            return
    tests = newest(smoke.ROOT / "app" / "build" / "outputs" / "apk" / "androidTest" / "debug")
    if tests is None:
        report.line("androidTest APK", "FAIL", "build :app:assembleDebugAndroidTest first")
        return
    out = subprocess.run(["adb", "-s", serial, "install", "-r", "-t", str(tests)], capture_output=True, text=True, timeout=300)
    if not report.line(f"install {tests.name}", "PASS" if out.returncode == 0 and "Success" in out.stdout else "FAIL", (out.stdout + out.stderr).strip()[-120:]):
        return
    try:
        raw = adb(serial, "shell", "am", "instrument", "-w", "-r", "-e", "class", TEST_CLASS, RUNNER, timeout=600)
        results = {}
        current = None
        for line in raw.splitlines():
            m = re.match(r"INSTRUMENTATION_STATUS: test=(\S+)", line)
            if m:
                current = m.group(1)
            m = re.match(r"INSTRUMENTATION_STATUS_CODE: (-?\d+)", line)
            if m and current:
                code = int(m.group(1))
                if code != 1:
                    results[current] = code
        failures = re.findall(r"INSTRUMENTATION_STATUS: stack=([^\n]*)", raw)
        for name, code in results.items():
            report.line(name, "PASS" if code == 0 else ("SKIP" if code == -3 else "FAIL"), "" if code == 0 else f"status {code}")
        if failures:
            report.line("first failure", "FAIL", failures[0][:200])
        summary = re.search(r"OK \((\d+) tests?\)|Tests run: (\d+),\s+Failures: (\d+)", raw)
        report.line("ChimeToolServiceTest", "PASS" if summary and summary.group(1) else "FAIL", summary.group(0) if summary else raw.strip().splitlines()[-1:] )
        logs = adb(serial, "shell", "logcat", "-d", "-s", "ChimeDeviceRun")
        for line in [l for l in logs.splitlines() if "tools " in l][-3:]:
            print(f"      {line.split('tools ', 1)[1]}")
    finally:
        out = subprocess.run(["adb", "-s", serial, "uninstall", TEST_PACKAGE], capture_output=True, text=True, timeout=120)
        report.line("androidTest APK uninstalled", "PASS" if out.returncode == 0 else "FAIL")


def host(serial, report):
    print("  host (plugins.tools.call):")
    version = smoke.installed_version(serial, HOST)
    if not report.line("host with the alarm contract", "PASS" if version and version >= MIN_HOST else "FAIL", f"AutoJs6 versionCode {version}, needs {MIN_HOST}"):
        return
    adb(serial, "shell", "rm", "-rf", REMOTE)
    adb(serial, "shell", "mkdir", "-p", REMOTE)
    local = Path(tempfile.gettempdir()) / f"chime-tools-{serial.replace(':', '_')}.js"
    local.write_text(SCRIPT.replace("@DIR@", REMOTE).replace("@PKG@", PLUGIN).replace("@TOOLS@", json.dumps(TOOLS)), encoding="utf-8", newline="")
    adb(serial, "push", str(local), f"{REMOTE}/tools.js", check=True)
    local.unlink()
    adb(serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP")
    adb(serial, "shell", "am", "start", "-n", smoke.launcher(serial))
    time.sleep(2)
    adb(serial, "shell", "am", "start", "-a", "android.intent.action.VIEW", "-t", "application/x-javascript",
        "-d", f"file://{REMOTE}/tools.js", "-n", f"{HOST}/org.autojs.autojs.external.open.RunIntentActivity")
    deadline = time.time() + 180
    while time.time() < deadline and adb(serial, "shell", "cat", f"{REMOTE}/done").strip() != "1":
        time.sleep(2)
    raw = adb(serial, "shell", "cat", f"{REMOTE}/result.json")
    adb(serial, "shell", "rm", "-rf", REMOTE)
    if not raw.startswith("{"):
        report.line("host script", "FAIL", "no result in 180 s (unlock the device; AutoJs6 needs all-files access)")
        return
    result = json.loads(raw)
    for name, state, detail in result["checks"]:
        report.line(name, state, detail)
    print(f"    (host: {result.get('seconds')} s)")


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("serial")
    parser.add_argument("--modes", default="provider,host", help="comma-separated: provider, host (default: both)")
    parser.add_argument("--apk", help="the plugin APK to install (default: the newest debug APK)")
    parser.add_argument("--no-install", action="store_true", help="keep the installed plugin (provider mode)")
    args = parser.parse_args(argv)
    report = Report()
    print(f"{smoke.label(args.serial)}:")
    for mode in [m.strip() for m in args.modes.split(",") if m.strip()]:
        if mode == "provider":
            provider(args.serial, args, report)
        elif mode == "host":
            host(args.serial, report)
        else:
            report.line(f"mode {mode}", "FAIL", "unknown mode")
    print(f"{args.serial}: TOOLS {'PASS' if report.failed == 0 else 'FAIL'} ({report.passed} passed, {report.failed} failed)")
    return 0 if report.failed == 0 else 1


if __name__ == "__main__":
    sys.exit(main())

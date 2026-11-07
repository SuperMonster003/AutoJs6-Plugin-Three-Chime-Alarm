"""Validation runs of 3-Chime Alarm on one device for roadmap P5 (P5.2 smoke items, P5.3 kill / restart cases, P5.5 links,
P5.6 baselines). The host must be a build with the alarm contract (versionCode 6641 or later, roadmap Q12) except for
`coldstart`, which only needs the plugin's debug hook.

    py .python/device_p5.py <serial> drift [--count 10] [--gap 20] [--lead 50]
    py .python/device_p5.py <serial> links
    py .python/device_p5.py <serial> listen
    py .python/device_p5.py <serial> hoststop [--lead 45]
    py .python/device_p5.py <serial> hostkill [--lead 45]
    py .python/device_p5.py <serial> due [--lead 240] [--tag p5-reboot]      (adds a bound alarm and exits)
    py .python/device_p5.py <serial> rebootcheck [--tag p5-reboot]          (after the reboot: did it ring, was the script started)
    py .python/device_p5.py <serial> coldstart [--count 3] [--kill am|force-stop] [--screen off|on]
    py .python/device_p5.py <serial> fgs
    py .python/device_p5.py <serial> cleanup
    py .python/device_p5.py <serial> quiet | unquiet

- drift: `count` silent one-shot alarms `gap` seconds apart, the first `lead` seconds ahead (whole seconds); an
  `alarms.on('ring')` listener records each event's `firedAt - scheduledAt` and dismisses it at once; the first alarm is
  bound to a script that writes `engines.myEngine().execArgv.alarm`, which must match the listener's event (type,
  alarmId, occurrence, scheduledAt). Prints each deviation and P50 / max in ms (roadmap R3, P5.6).
- links (appendix A section 5.3): alarm M is bound to a script that is deleted before the ring (skipped: ChimeTrigger
  `missing=1`, the host's Outcome counts it missing); alarm D is bound to a counting script for ring / snooze / dismiss and is
  snoozed for 1 minute at its first ring and dismissed at the second (two occurrences, each (occurrence, event) started
  once); alarm B has two bound scripts (both started once).
- listen: a script registers `alarms.on('ring')`; the plugin service's connections are counted (`dumpsys activity
  services`) before, while it listens and after it has exited (the callback is unregistered and the binding released).
- hoststop: an alarm bound to a script is due `lead` seconds ahead; the host is force-stopped; the alarm still rings
  (plugin log), ChimeTrigger reports no host answer, and the script was never started (checked after the host is
  started again).
- hostkill: the same alarm, but the host process is killed (`am kill` from the home screen, not stopped): the plugin's
  ALARM_TRIGGER broadcast starts the host again, which starts the bound script (ChimeTrigger `launched=1`; roadmap
  P5.4; on ColorOS this shows whether the host needs the auto start permission).
- due / rebootcheck: the reboot case of R3 and P5.3 (direct boot; with a secure lock screen, ChimeTrigger
  `scriptSkipped=locked`, no late start after the unlock). Between them the caller reboots the device.
- coldstart: the plugin process is killed (`am kill`) before a silent probe alarm of the debug hook is due; prints the
  time from the due instant to the new process, the ring session, the resumed ring screen and its first frame (the
  `ring screen drawn` line; logcat epoch times; screen off by default). With `--kill force-stop` it shows the system
  rule instead: no ring while the package is stopped; a background start within 5 minutes rings late (through the
  kick wake-up of roadmap T97 when the direct foreground service start is refused).
- fgs: a silent probe of the debug hook rings; the foreground service type of the ring service is read from
  `dumpsys activity services` (roadmap T6: `systemExempted`, 0x400, from API 34; no type below API 34).
- cleanup: removes the alarms an interrupted run left (tag p5*), and the debug probes.
- quiet / unquiet: turns the system sounds that a run can set off (lock screen sound on screen off / on, charging sound
  of a simulated plug-in, touch sounds) off and saves their values; `unquiet` puts the saved values back. The probes of
  these runs are silent already; the sounding checks of device_ring.py wait until the device may make sounds again.

Output: PASS / FAIL / SKIP per check and the key numbers only. Nothing here answers a system dialog or unlocks the
device; runs that start a script need the device unlocked.
"""
import argparse
import json
import re
import statistics
import subprocess
import sys
import tempfile
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import device_smoke as smoke  # noqa: E402

PLUGIN = smoke.PLUGIN
HOST = smoke.HOST
SERVICE = f"{PLUGIN}/.service.ThreeChimeAlarmService"
HOOK = f"{PLUGIN}/.ring.RingDebugReceiver"
HOOK_ACTION = f"{PLUGIN}.debug.RING_PROBE"
REMOTE = "/sdcard/Download/autojs6-chime-p5"
MIN_HOST = 6641

SCRIPT = r"""
var DIR = '@DIR@';
var cfg = JSON.parse(files.read(DIR + '/config.json'));
var checks = [];
var values = {};
var created = [];
var started = Date.now();

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
function arr(x) {
    if (x === undefined || x === null) return [];
    if (Array.isArray(x)) return x;
    var out = [];
    for (var i = 0; i < x.size(); i++) out.push(x.get(i));
    return out;
}
function add(options) { var a = alarms.add(options); created.push(a.id); return a; }
function removeTagged(prefix) {
    var removed = 0;
    arr(alarms.list()).forEach(function (x) {
        if (String(x.tag || '').indexOf(prefix) === 0) { try { if (alarms.remove(x.id)) removed++; } catch (e) { /* gone */ } }
    });
    return removed;
}
function exists(name) { return files.exists(DIR + '/' + name); }
function readJson(name) { return JSON.parse(files.read(DIR + '/' + name)); }
function lines(name) { return exists(name) ? String(files.read(DIR + '/' + name)).split('\n').filter(function (l) { return l.length > 0; }) : []; }
function save() {
    files.write(DIR + '/result.json', JSON.stringify({ mode: cfg.mode, seconds: Math.round((Date.now() - started) / 1000), checks: checks, values: values }));
    files.write(DIR + '/done', '1');
}
function stage(name) {
    files.write(DIR + '/stage', name);
    var end = Date.now() + 120e3;
    while (!exists('go-' + name) && Date.now() < end) sleep(500);
}
function wholeSecond(t) { return Math.ceil(t / 1000) * 1000; }
function silent(extra) {
    var o = { sound: { source: 'silent' }, vibrate: false, snooze: false, tag: cfg.tag || 'p5' };
    for (var k in extra) o[k] = extra[k];
    return o;
}
function poll(until, deadline, finish) {
    var timer = setInterval(function () {
        if (!until() && Date.now() < deadline) return;
        clearInterval(timer);
        alarms.off();
        try { finish(); } catch (err) { record('results', 'FAIL', describe(err)); }
        try { created.forEach(function (id) { try { alarms.remove(id); } catch (e) { /* gone */ } }); } catch (e) { /* best effort */ }
        save();
    }, 1000);
}

function modeDrift() {
    var base = wholeSecond(Date.now() + cfg.lead * 1000);
    var ids = [], due = {}, got = {};
    for (var i = 0; i < cfg.count; i++) {
        var a = add(silent({ at: base + i * cfg.gap * 1000, label: 'p5-drift-' + i }));
        ids.push(a.id);
        due[a.id] = base + i * cfg.gap * 1000;
    }
    alarms.get(ids[0]).bind('./exec.js', { events: ['ring'] });
    values.bound = ids[0];
    record('drift alarms added', 'PASS', ids.length + ' alarms, first in ' + Math.round((base - Date.now()) / 1000) + ' s, gap ' + cfg.gap + ' s');
    alarms.on('ring', function (e) {
        if (!due[e.alarmId] || got[e.alarmId]) return;
        got[e.alarmId] = { s: e.scheduledAt, f: e.firedAt, now: Date.now(), occ: String(e.occurrence), kind: e.kind };
        try { alarms.dismiss(e.alarmId); } catch (err) { got[e.alarmId].err = describe(err); }
    });
    var deadline = due[ids[ids.length - 1]] + 120e3;
    poll(function () { return Object.keys(got).length === ids.length && exists('exec-ring.json'); }, deadline, function () {
        var deltas = [];
        ids.forEach(function (id, i) {
            var g = got[id];
            if (!g) { record('ring ' + i, 'FAIL', 'no ring event'); return; }
            var ok = g.s === due[id] && g.kind === 'alarm' && !g.err;
            deltas.push(g.f - g.s);
            record('ring ' + i, ok ? 'PASS' : 'FAIL', 'firedAt - scheduledAt ' + (g.f - g.s) + ' ms, listener +' + (g.now - g.s) + ' ms'
                + (g.s === due[id] ? '' : ', scheduledAt ' + g.s + ' != ' + due[id]) + (g.err ? ', dismiss ' + g.err : ''));
        });
        values.deltas = deltas;
        if (!exists('exec-ring.json')) { record('bound script execArgv.alarm', 'FAIL', 'not started'); return; }
        var x = readJson('exec-ring.json'), g0 = got[ids[0]] || {};
        var a = x.alarm || {};
        var ok = a.type === 'ring' && a.alarmId === ids[0] && String(a.occurrence) === g0.occ && a.scheduledAt === due[ids[0]] && a.kind === 'alarm';
        record('bound script execArgv.alarm', ok ? 'PASS' : 'FAIL', 'type ' + a.type + ', alarmId ' + (a.alarmId === ids[0]) + ', occurrence ' + (String(a.occurrence) === g0.occ)
            + ', scheduledAt ' + (a.scheduledAt === due[ids[0]]) + ', firedAt ' + (a.firedAt === g0.f) + ', currentEvent ' + x.current);
    });
}

function modeLinks() {
    var base = wholeSecond(Date.now() + 50e3);
    var m = add(silent({ at: base, label: 'p5-missing' }));
    var d = add(silent({ at: base + 30e3, label: 'p5-dup', snooze: { minutes: 1, max: 2 } }));
    var b = add(silent({ at: base + 60e3, label: 'p5-multi' }));
    check('bindings', function () {
        m.bind('./gone.js', { events: ['ring'] });
        files.remove(DIR + '/gone.js');
        d.bind('./count.js', { events: ['ring', 'snooze', 'dismiss'] });
        b.bind('./s1.js', { events: ['ring'] });
        b.bind('./s2.js', { events: ['ring'] });
        return 'missing ' + arr(alarms.bindings(m.id)).length + ' (file deleted ' + !exists('gone.js') + '), dup ' + arr(alarms.bindings(d.id)).length + ', multi ' + arr(alarms.bindings(b.id)).length;
    });
    values.ids = { missing: m.id, dup: d.id, multi: b.id };
    var seen = { m: 0, d: [], b: 0 };
    alarms.on('ring', function (e) {
        if (e.alarmId === m.id) { seen.m++; alarms.dismiss(m.id); }
        else if (e.alarmId === b.id) { seen.b++; alarms.dismiss(b.id); }
        else if (e.alarmId === d.id) {
            seen.d.push(String(e.occurrence));
            if (seen.d.length === 1) { try { seen.snoozed = alarms.snooze(d.id, 1); } catch (err) { seen.snoozeError = describe(err); } }
            else alarms.dismiss(d.id);
        }
    });
    poll(function () { return seen.d.length >= 2 && lines('count.txt').length >= 4 && exists('s1.json') && exists('s2.json'); }, base + 240e3, function () {
        record('missing script: the alarm rings', seen.m === 1 ? 'PASS' : 'FAIL', seen.m + ' ring events');
        var c = lines('count.txt');
        var distinct = {};
        c.forEach(function (l) { distinct[l] = (distinct[l] || 0) + 1; });
        var once = Object.keys(distinct).every(function (k) { return distinct[k] === 1; });
        var occs = seen.d;
        record('snooze: the second ring is a new occurrence', occs.length === 2 && occs[0] !== occs[1] ? 'PASS' : 'FAIL',
            occs.length + ' rings, snoozed ' + seen.snoozed + (seen.snoozeError ? ', ' + seen.snoozeError : ''));
        var expected = occs.length === 2 ? ['ring ' + occs[0], 'snooze ' + occs[0], 'ring ' + occs[1], 'dismiss ' + occs[1]] : [];
        var match = expected.length === 4 && expected.every(function (k) { return distinct[k] === 1; }) && c.length === 4;
        record('one start per (occurrence, event)', match && once ? 'PASS' : 'FAIL', c.length + ' starts: ' + c.map(function (l) { return l.split(' ')[0] + (occs.indexOf(l.split(' ')[1]) + 1); }).join(', '));
        var s1 = exists('s1.json') ? readJson('s1.json') : null, s2 = exists('s2.json') ? readJson('s2.json') : null;
        record('two scripts on one alarm: both started', s1 && s2 && s1.n === 1 && s2.n === 1 ? 'PASS' : 'FAIL', 's1 ' + (s1 ? s1.n : 0) + ', s2 ' + (s2 ? s2.n : 0) + ', rings ' + seen.b);
    });
}

function modeListen() {
    alarms.on('ring', function () { /* only registered */ });
    stage('listening');
    alarms.off();
    record('listener registered and removed', 'PASS', '');
    save();
}

function modeDue() {
    var at = wholeSecond(Date.now() + cfg.lead * 1000);
    check('bound alarm due', function () {
        var a = add({ at: at, label: 'p5-due', tag: cfg.tag, sound: { source: 'silent' }, vibrate: false, snooze: false });
        a.bind('./late.js', { events: ['ring'] });
        values.id = a.id;
        values.at = at;
        return 'in ' + Math.round((at - Date.now()) / 1000) + ' s';
    });
    save();
}

function modeCheck() {
    var found = arr(alarms.list()).filter(function (x) { return x.tag === cfg.tag; });
    check('alarm still listed', function () {
        assert(found.length === 1, found.length + ' alarms tagged ' + cfg.tag);
        var a = found[0];
        values.id = a.id;
        values.enabled = a.enabled;
        return 'enabled ' + a.enabled;
    });
    values.late = exists('late-ring.json');
    values.removed = removeTagged(cfg.tag);
    save();
}

function modeCleanup() {
    record('leftover test alarms removed', 'PASS', removeTagged('p5') + ' removed');
    save();
}

/** Adds cfg.alarms ([{key, options, bind}]); `options.inSeconds` becomes a whole-second `at`. Used by device_events.py. */
function modeAdd() {
    values.alarms = {};
    cfg.alarms.forEach(function (spec) {
        check('add ' + spec.key, function () {
            var o = {};
            for (var k in spec.options) o[k] = spec.options[k];
            if (o.inSeconds !== undefined) { o.at = wholeSecond(Date.now() + o.inSeconds * 1000); delete o.inSeconds; }
            if (o.tag === undefined) o.tag = cfg.tag;
            var a = add(o);
            if (spec.bind) a.bind(spec.bind, { events: ['ring'] });
            values.alarms[spec.key] = { id: a.id, next: a.nextTrigger, at: o.at || null };
            return 'next ' + (a.nextTrigger ? new Date(a.nextTrigger).toISOString() : 'none');
        });
    });
    save();
}

/** Reports the alarms tagged cfg.tag and which `late-*` files the bound scripts wrote; removes them with cfg.remove. */
function modeState() {
    values.alarms = {};
    arr(alarms.list()).forEach(function (x) {
        if (x.tag !== cfg.tag) return;
        values.alarms[String(x.label)] = { id: x.id, enabled: x.enabled, next: x.nextTrigger || null };
    });
    values.late = exists('late-ring.json');
    if (cfg.remove) values.removed = removeTagged(cfg.tag);
    save();
}

try {
    if (cfg.mode === 'add') modeAdd();
    else if (cfg.mode === 'state') modeState();
    else if (cfg.mode === 'drift') modeDrift();
    else if (cfg.mode === 'links') modeLinks();
    else if (cfg.mode === 'listen') modeListen();
    else if (cfg.mode === 'due') modeDue();
    else if (cfg.mode === 'check') modeCheck();
    else modeCleanup();
} catch (e) {
    record('script', 'FAIL', describe(e));
    try { created.forEach(function (id) { alarms.remove(id); }); } catch (ignored) { /* best effort */ }
    save();
}
"""

EXEC = r"""
// execArgv.alarm may be a script object or a Java map; numbers are compared as numbers, the rest as strings.
function field(o, k) { if (o === undefined || o === null) return undefined; try { if (typeof o.get === 'function') return o.get(k); } catch (e) { /* not a map */ } return o[k]; }
var a = engines.myEngine().execArgv.alarm;
if (typeof a === 'string') a = JSON.parse(a);
var out = {};
['type', 'kind', 'alarmId', 'occurrence'].forEach(function (k) { var v = field(a, k); out[k] = v === undefined || v === null ? null : String(v); });
['scheduledAt', 'firedAt'].forEach(function (k) { var v = field(a, k); out[k] = v === undefined || v === null ? null : Number(v); });
var c = alarms.currentEvent();
files.write('@DIR@/exec-ring.json', JSON.stringify({ alarm: out, current: c ? String(c.type) : 'null' }));
"""

COUNT = r"""
var e = alarms.currentEvent();
files.append('@DIR@/count.txt', (e ? e.type + ' ' + e.occurrence : 'none') + '\n');
"""

MULTI = r"""
var p = '@DIR@/@NAME@.json';
var n = files.exists(p) ? JSON.parse(files.read(p)).n : 0;
files.write(p, JSON.stringify({ n: n + 1, type: String(alarms.currentEvent().type) }));
"""

LATE = r"""
files.write('@DIR@/late-ring.json', JSON.stringify({ at: Date.now() }));
"""

EPOCH = re.compile(r"^\s*(\d+\.\d+)\s+\d+\s+\d+\s+\w\s+([\w.]+)\s*:\s?(.*)$")


class Report:
    def __init__(self):
        self.passed = 0
        self.failed = 0

    def line(self, name, state, detail=""):
        if state == "FAIL":
            self.failed += 1
        elif state == "PASS":
            self.passed += 1
        print(f"    {name}: {state} {detail}".rstrip(), flush=True)
        return state != "FAIL"


def adb(serial, *args, timeout=120):
    return smoke.adb(serial, *args, check=False, timeout=timeout)


def device_ms(serial):
    """The device clock in ms (mksh arithmetic is 32-bit on some devices, toybox of API 24 has no %N)."""
    raw = adb(serial, "shell", "date", "+%s%N").strip()
    return int(raw[:-6]) if raw.isdigit() and len(raw) > 13 else int(raw.rstrip("N") or 0) * 1000


def logs(serial, tags, since_ms):
    """Log lines (epoch seconds, tag, message) of [tags] since [since_ms] (device clock)."""
    text = adb(serial, "shell", "logcat", "-d", "-v", "epoch", "-s", *(f"{t}:*" for t in tags), timeout=60)
    out = []
    for line in text.splitlines():
        m = EPOCH.match(line)
        # The upper bound drops lines written while a test had the clock set ahead (device_events.py dst). zh-CN: 上界排除时钟被拨快时写下的行.
        if m and since_ms <= float(m.group(1)) * 1000 <= (since_ms if since_ms > 0 else time.time() * 1000) + 3_600_000:
            out.append((float(m.group(1)), m.group(2), m.group(3)))
    return out


def push(serial, mode, config, fresh=True):
    if fresh:
        adb(serial, "shell", "rm", "-rf", REMOTE)
    else:
        adb(serial, "shell", f"rm -f {REMOTE}/done {REMOTE}/result.json {REMOTE}/stage {REMOTE}/go-*")
    adb(serial, "shell", "mkdir", "-p", REMOTE)
    folder = Path(tempfile.mkdtemp(prefix="chime-p5-"))
    texts = {
        "p5.js": SCRIPT.replace("@DIR@", REMOTE),
        "config.json": json.dumps({"mode": mode, **config}),
        "exec.js": EXEC.replace("@DIR@", REMOTE),
        "count.js": COUNT.replace("@DIR@", REMOTE),
        "s1.js": MULTI.replace("@DIR@", REMOTE).replace("@NAME@", "s1"),
        "s2.js": MULTI.replace("@DIR@", REMOTE).replace("@NAME@", "s2"),
        "gone.js": "files.write('@DIR@/gone-ran', '1');\n".replace("@DIR@", REMOTE),
        "late.js": LATE.replace("@DIR@", REMOTE),
    }
    for name, text in texts.items():
        (folder / name).write_text(text, encoding="utf-8", newline="")
        adb(serial, "push", str(folder / name), f"{REMOTE}/{name}")
        (folder / name).unlink()
    folder.rmdir()


def start_script(serial):
    adb(serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP")
    adb(serial, "shell", "am", "start", "-n", smoke.launcher(serial))
    time.sleep(2)
    adb(serial, "shell", "am", "start", "-a", "android.intent.action.VIEW", "-t", "application/x-javascript",
        "-d", f"file://{REMOTE}/p5.js", "-n", f"{HOST}/org.autojs.autojs.external.open.RunIntentActivity")


def run_mode(serial, mode, report, config=None, timeout=900, stages=None, keep=False, fresh=True):
    """Runs the script in [mode]; [stages] maps a stage name to a callback run while the script waits. Answers the
    result's `values`, or None without a result."""
    print(f"  {mode}:", flush=True)
    push(serial, mode, config or {}, fresh)
    start_script(serial)
    deadline = time.time() + timeout
    while time.time() < deadline and adb(serial, "shell", "cat", f"{REMOTE}/done").strip() != "1":
        staged = adb(serial, "shell", f"cat {REMOTE}/stage 2>/dev/null").strip() if stages else ""
        if staged:
            adb(serial, "shell", "rm", "-f", f"{REMOTE}/stage")
            try:
                stages[staged]()
            finally:
                adb(serial, "shell", f"echo 1 > {REMOTE}/go-{staged}")
        time.sleep(2)
    raw = adb(serial, "shell", "cat", f"{REMOTE}/result.json")
    if not raw.startswith("{"):
        report.line(mode, "FAIL", f"no result in {timeout} s (unlock the device; AutoJs6 needs all-files access)")
        return None
    result = json.loads(raw)
    for name, state, detail in result["checks"]:
        report.line(name, state, detail)
    print(f"    ({mode}: {result.get('seconds')} s)", flush=True)
    if not keep:
        adb(serial, "shell", "rm", "-rf", REMOTE)
    return result.get("values") or {}


def host_ready(serial, report):
    host = smoke.installed_version(serial, HOST)
    return report.line("host with the alarm contract", "PASS" if host and host >= MIN_HOST else "FAIL", f"AutoJs6 versionCode {host}, needs {MIN_HOST}")


def percentile50(values):
    return statistics.median(values) if values else None


def drift(serial, args, report):
    since = device_ms(serial)
    values = run_mode(serial, "drift", report, {"count": args.count, "gap": args.gap, "lead": args.lead, "tag": "p5-drift"},
                      timeout=args.lead + args.count * args.gap + 240)
    deltas = (values or {}).get("deltas") or []
    if deltas:
        report.line("drift", "PASS" if len(deltas) == args.count else "FAIL",
                    f"n {len(deltas)}, P50 {percentile50(deltas):.0f} ms, max {max(deltas)} ms, min {min(deltas)} ms")
    bound = (values or {}).get("bound")
    trigger = [m for _, _, m in logs(serial, ["ChimeTrigger"], since) if bound and m.startswith(f"ring {bound} -> ")]
    report.line("ChimeTrigger answer for the bound ring", "PASS" if len(trigger) == 1 and "launched=1 duplicates=0" in trigger[0] else "FAIL",
                "; ".join(m.split(": ", 1)[-1] for m in trigger) or "no line")


def links(serial, args, report):
    since = device_ms(serial)
    values = run_mode(serial, "links", report, {"tag": "p5-links"}, timeout=420)
    ids = (values or {}).get("ids") or {}
    trigger = [m for _, _, m in logs(serial, ["ChimeTrigger"], since)]
    missing = [m for m in trigger if m.startswith(f"ring {ids.get('missing')} ")]
    report.line("missing script: ChimeTrigger reports it skipped", "PASS" if missing and "launched=0" in missing[0] and "missing=1" in missing[0] else "FAIL",
                missing[0].split(": ", 1)[-1] if missing else "no answer logged")
    host = [m for _, _, m in logs(serial, ["AlarmTriggerReceiver"], since)]
    gone = [m for m in host if "bound script is gone" in m and m.endswith("/gone.js")]
    counted = [m for i, m in enumerate(host) if f"ring alarm={ids.get('missing')} " in m and i + 1 < len(host) and "missing=1" in host[i + 1]]
    report.line("missing script: the host skips it", "PASS" if gone or counted else "FAIL",
                " ".join(x for x in ("gone line" if gone else "", "Outcome missing=1" if counted else "") if x) or "no line")
    dup = [m.split(": ", 1)[-1] for m in trigger if m.startswith(("ring ", "snooze ", "dismiss ")) and f" {ids.get('dup')} " in m]
    report.line("ChimeTrigger for the snoozed alarm", "PASS" if dup and all("duplicates=0" in m for m in dup) else "FAIL", f"{len(dup)} answers, duplicates {sum(int(x) for m in dup for x in re.findall(r'duplicates=(\d+)', m))}")


def connections(serial):
    """Connections to the plugin's capability service (only AutoJs6 binds it), from `dumpsys activity services`."""
    text = adb(serial, "shell", "dumpsys", "activity", "services", PLUGIN, timeout=60)
    return len(set(re.findall(r"ConnectionRecord\{(\w+) u0 \S+ " + re.escape(PLUGIN) + r"/\.service\.ThreeChimeAlarmService", text)))


def listen(serial, args, report):
    adb(serial, "shell", "am", "force-stop", HOST)
    time.sleep(2)
    before = connections(serial)
    seen = {}

    def during():
        time.sleep(3)
        seen["during"] = connections(serial)

    run_mode(serial, "listen", report, {}, timeout=200, stages={"listening": during})
    time.sleep(8)
    after = connections(serial)
    time.sleep(60)
    later = connections(serial)
    during = seen.get("during", 0)
    report.line("listener binds the capability service", "PASS" if during > after else "FAIL", f"connections before {before}, listening {during}, 8 s after the exit {after}, 68 s after {later}")
    report.line("listener exit releases its connection", "PASS" if during - after >= 1 and later <= after else "FAIL",
                f"{during - after} released; the rest is the host's pooled call connection")


QUIET = (("system", "lockscreen_sounds_enabled"), ("system", "sound_effects_enabled"), ("secure", "charging_sounds_enabled"),
         ("global", "charging_sounds_enabled"), ("global", "power_sounds_enabled"), ("global", "dock_sounds_enabled"))


def quiet_file(serial):
    return Path(tempfile.gettempdir()) / f"chime-p5-quiet-{serial}.json"


def quiet(serial, report):
    path = quiet_file(serial)
    saved = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
    for space, key in QUIET:
        name = f"{space}/{key}"
        if name not in saved:
            saved[name] = adb(serial, "shell", "settings", "get", space, key).strip()
        if saved[name] not in ("null", "", "0"):
            adb(serial, "shell", "settings", "put", space, key, "0")
    path.write_text(json.dumps(saved), encoding="utf-8")
    report.line("system sounds off", "PASS", ", ".join(f"{k.split('/')[1]} {v}" for k, v in saved.items() if v not in ("null", "")))


def unquiet(serial, report):
    path = quiet_file(serial)
    if not path.exists():
        report.line("system sounds restored", "PASS", "nothing saved")
        return
    saved = json.loads(path.read_text(encoding="utf-8"))
    for name, value in saved.items():
        space, key = name.split("/")
        if value not in ("null", "", "0"):
            adb(serial, "shell", "settings", "put", space, key, value)
    back = {name: adb(serial, "shell", "settings", "get", *name.split("/")).strip() for name in saved}
    report.line("system sounds restored", "PASS" if back == saved else "FAIL", ", ".join(f"{k.split('/')[1]} {v}" for k, v in back.items() if v not in ("null", "")))
    if back == saved:
        path.unlink()


def hook(serial, op, **extras):
    token = str(int(time.time() * 1000) % 10_000_000)
    command = ["shell", "am", "broadcast", "-f", "32", "-n", HOOK, "-a", HOOK_ACTION, "--es", "op", op, "--es", "token", token]
    for key, value in extras.items():
        command += ["--ei" if isinstance(value, int) else "--es", key, str(value)]
    adb(serial, *command)
    deadline = time.time() + 20
    while time.time() < deadline:
        for line in reversed(adb(serial, "shell", "logcat", "-d", "-v", "brief", "-s", "ChimeRingDebug:I").splitlines()):
            found = re.search(rf"token={token} (.*)$", line)
            if found:
                return dict(re.findall(r"(\w+)=(\S+)", found.group(1)))
        time.sleep(0.5)
    return {}


def hoststop(serial, args, report):
    values = run_mode(serial, "due", report, {"lead": args.lead, "tag": "p5-hoststop"}, timeout=120, keep=True)
    if not values or not values.get("id"):
        return
    since = device_ms(serial)
    adb(serial, "shell", "am", "force-stop", HOST)
    report.line("host force-stopped", "PASS" if not adb(serial, "shell", "pidof", HOST) else "FAIL", "")
    wait = values["at"] - device_ms(serial) + 25_000
    time.sleep(max(wait, 0) / 1000)
    ring = [m for _, _, m in logs(serial, ["ChimeRing"], since) if m.startswith(f"ring session {values['id']} occurrence")]
    report.line("the plugin rings with the host stopped", "PASS" if ring else "FAIL", "ring session started" if ring else "no ring session")
    trigger = [m for _, _, m in logs(serial, ["ChimeTrigger"], since) if m.startswith(f"ring {values['id']} ")]
    report.line("ChimeTrigger: no host answer", "PASS" if trigger and ("scriptSkipped=unavailable" in trigger[0] or "not sent" in trigger[0]) else "FAIL",
                trigger[0].split(": ", 1)[-1] if trigger else "no line")
    report.line("host still stopped after the ring", "PASS" if not adb(serial, "shell", "pidof", HOST) else "FAIL", "")
    hook(serial, "dismiss", id=values["id"])
    after = run_mode(serial, "check", report, {"tag": "p5-hoststop"}, timeout=120, keep=True, fresh=False)
    report.line("bound script never started", "PASS" if after is not None and not after.get("late") else "FAIL", f"late start {after.get('late') if after else '?'}")
    adb(serial, "shell", "rm", "-rf", REMOTE)


def hostkill(serial, args, report):
    values = run_mode(serial, "due", report, {"lead": args.lead, "tag": "p5-hostkill"}, timeout=120, keep=True)
    if not values or not values.get("id"):
        return
    adb(serial, "shell", "input", "keyevent", "KEYCODE_HOME")
    time.sleep(3)
    adb(serial, "shell", "am", "kill", HOST)
    time.sleep(2)
    if adb(serial, "shell", "pidof", HOST):
        adb(serial, "shell", "am", "kill-all")
        time.sleep(2)
    killed = not adb(serial, "shell", "pidof", HOST)
    report.line("host process killed (not stopped)", "PASS" if killed else "FAIL", "" if killed else "still running after am kill")
    since = device_ms(serial)
    time.sleep(max(values["at"] - since + 30_000, 0) / 1000)
    trigger = [m for _, _, m in logs(serial, ["ChimeTrigger"], since) if m.startswith(f"ring {values['id']} -> ")]
    report.line("ALARM_TRIGGER woke the host and it started the script", "PASS" if trigger and "launched=1" in trigger[0] else "FAIL",
                trigger[0].split(": ", 1)[-1] if trigger else "no ChimeTrigger line")
    hook(serial, "dismiss", id=values["id"])
    after = run_mode(serial, "check", report, {"tag": "p5-hostkill"}, timeout=120, keep=True, fresh=False)
    report.line("bound script wrote its file", "PASS" if after is not None and after.get("late") else "FAIL", f"late-ring {after.get('late') if after else '?'}")
    adb(serial, "shell", "rm", "-rf", REMOTE)


def due(serial, args, report):
    values = run_mode(serial, "due", report, {"lead": args.lead, "tag": args.tag}, timeout=120, keep=True)
    if values and values.get("id"):
        state = Path(tempfile.gettempdir()) / f"chime-p5-due-{serial.replace(':', '_')}.json"
        state.write_text(json.dumps({**values, "since": device_ms(serial)}), encoding="utf-8")
        print(f"    due at device time {values['at']} ({values['id']}); reboot now, then run rebootcheck after the due time", flush=True)


def rebootcheck(serial, args, report):
    """Reads this boot's log (run it while the device is still locked with --locked-only; the results are kept), then
    after the unlock checks that the bound script was started only when the host answered (no late start)."""
    state = Path(tempfile.gettempdir()) / f"chime-p5-due-{serial.replace(':', '_')}.json"
    saved = json.loads(state.read_text(encoding="utf-8")) if state.exists() else {}
    if not saved:
        report.line("due state", "FAIL", "run due first")
        return
    alarm = saved["id"]
    lines = logs(serial, ["ChimeRing", "ChimeTrigger", "ChimeSchedule"], 0)
    ring = [(t, m) for t, _, m in lines if m.startswith(f"ring session {alarm} occurrence")]
    boot = [m for _, _, m in lines if ("BOOT_COMPLETED" in m or m.startswith("start after boot")) and "reconciled" in m]
    trigger = [m for _, _, m in lines if m.startswith(f"ring {alarm} -> ")]
    if ring or trigger:
        saved["phase"] = {"ring": round(ring[0][0] * 1000 - saved["at"]) if ring else None, "boot": boot[:2],
                          "trigger": trigger[0].split(": ", 1)[-1] if trigger else None}
        state.write_text(json.dumps(saved), encoding="utf-8")
    phase = saved.get("phase") or {}
    report.line("rang after the reboot", "PASS" if phase.get("ring") is not None else "FAIL",
                f"{phase.get('ring')} ms after due" if phase.get("ring") is not None else "no ring session logged")
    report.line("boot reconcile logged", "PASS" if phase.get("boot") else "FAIL", "; ".join(m[:90] for m in phase.get("boot") or []))
    report.line("ChimeTrigger answer", "PASS" if phase.get("trigger") else "FAIL", phase.get("trigger") or "no line")
    if args.locked_only:
        return
    values = run_mode(serial, "check", report, {"tag": args.tag}, timeout=180, fresh=False)
    if values is not None:
        answer = phase.get("trigger") or ""
        expected_late = "launched=1" in answer
        report.line("bound script started only if the host answered", "PASS" if bool(values.get("late")) == expected_late else "FAIL",
                    f"late-ring {values.get('late')}, answer {answer or '-'}; removed {values.get('removed')}")
        state.unlink()


def pid(serial):
    return adb(serial, "shell", "pidof", PLUGIN).strip()


def coldstart(serial, args, report):
    """`am kill`: the process start, the ring session, the resumed and the first drawn ring screen after the due instant.
    `force-stop`: no ring while the package is stopped (the system drops its alarms, roadmap T60); started again within
    the 5 minute window, the plugin rings late (T16)."""
    results = []
    options = __import__("base64").b64encode(json.dumps({"sound": {"source": "silent"}, "vibrate": False}).encode()).decode()
    for i in range(args.count):
        answer = hook(serial, "add", label=f"p24-probe-cold-{i}", inSeconds=args.lead, options64=options)
        if "added" not in answer:
            report.line(f"cold start {i}: probe", "FAIL", str(answer)[:120])
            continue
        at = int(answer["at"])
        alarm = answer["added"]
        time.sleep(3)
        adb(serial, "shell", "input", "keyevent", "KEYCODE_HOME")
        time.sleep(2)
        if args.kill == "force-stop":
            adb(serial, "shell", "am", "force-stop", PLUGIN)
        else:
            adb(serial, "shell", "am", "kill", PLUGIN)
            if pid(serial):
                adb(serial, "shell", "run-as", PLUGIN, "sh", "-c", "kill -9 $(pidof " + PLUGIN + ")")
        time.sleep(1)
        killed = not pid(serial)
        if args.screen == "off":
            adb(serial, "shell", "input", "keyevent", "KEYCODE_SLEEP")
        since = device_ms(serial)
        time.sleep(max(at - since + 25_000, 0) / 1000)
        lines = logs(serial, ["ChimeRing", "ActivityManager"], since)
        start = next((t for t, tag, m in lines if tag == "ActivityManager" and "Start proc" in m and PLUGIN in m), None)
        session = next((t for t, tag, m in lines if m.startswith(f"ring session {alarm} occurrence")), None)
        resumed = next((t for t, tag, m in lines if m.startswith(f"ring screen resumed {alarm}")), None)
        drawn = next((t for t, tag, m in lines if m.startswith(f"ring screen drawn {alarm}")), None)
        ms = (lambda t: f"{t * 1000 - at:.0f}" if t else "-")
        if args.kill == "force-stop":
            report.line(f"force-stop {i} (killed {killed}): no ring while stopped", "PASS" if session is None else "FAIL", f"ring session +{ms(session)} ms")
            # A background start without an exemption (a broadcast, as a host binding would be): the late ring cannot start
            # the foreground service directly and starts it from the kick wake-up (roadmap T97).
            restart = device_ms(serial)
            hook(serial, "state")
            time.sleep(8)
            later = logs(serial, ["ChimeSchedule", "ChimeRing"], restart)
            late = [m for _, _, m in later if m.startswith(f"ring {alarm} ")]
            refused = any("ring service start refused" in m for _, _, m in later)
            kicked = any(m.startswith("ring kick: sounding=" + alarm) for _, _, m in later)
            sounded = any(m.startswith(f"ring session {alarm} occurrence") for _, _, m in later)
            report.line(f"force-stop {i}: late ring after a background start", "PASS" if late and sounded else "FAIL",
                        (late[0].split(" ", 2)[-1] if late else "no ring") + f", direct start refused {refused}, kick {kicked}, ring session {sounded}")
            adb(serial, "shell", "input", "keyevent", "KEYCODE_HOME")
        else:
            visible = drawn if args.screen == "off" else True
            report.line(f"cold start {i} (am kill, killed {killed})", "PASS" if killed and session and visible else "FAIL",
                        f"process +{ms(start)} ms, ring session +{ms(session)} ms, screen resumed +{ms(resumed)} ms, first frame +{ms(drawn)} ms")
            if drawn:
                results.append(drawn * 1000 - at)
        hook(serial, "clear")
        adb(serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP")
        time.sleep(3)
    if results:
        report.line("due to the first frame of the ring screen", "PASS", f"n {len(results)}, P50 {percentile50(results):.0f} ms, max {max(results):.0f} ms")


def fgs(serial, args, report):
    sdk = int(adb(serial, "shell", "getprop", "ro.build.version.sdk").strip() or 0)
    options = __import__("base64").b64encode(json.dumps({"sound": {"source": "silent"}, "vibrate": False}).encode()).decode()
    answer = hook(serial, "add", label="p24-probe-fgs", inSeconds=15, options64=options)
    if "added" not in answer:
        report.line("probe", "FAIL", str(answer)[:120])
        return
    since = device_ms(serial)
    deadline = time.time() + 60
    text = ""
    while time.time() < deadline:
        if any(m.startswith(f"ring session {answer['added']} ") for _, _, m in logs(serial, ["ChimeRing"], since)):
            time.sleep(2)
            text = adb(serial, "shell", "dumpsys", "activity", "services", f"{PLUGIN}/.ring.RingService", timeout=60)
            break
        time.sleep(2)
    hook(serial, "clear")
    if not text:
        report.line("ring service running", "FAIL", "no ring session in 60 s")
        return
    foreground = "isForeground=true" in text
    types = sorted(set(re.findall(r"(?:foregroundServiceType|types)=(0x[0-9a-fA-F]+|\d+)", text)))
    value = int(types[0], 0) if types else None
    expected = 0x400 if sdk >= 34 else None
    ok = foreground and (value == expected if expected is not None else value in (None, 0))
    report.line(f"ring service foreground type (API {sdk})", "PASS" if ok else "FAIL",
                f"isForeground {foreground}, type {hex(value) if value is not None else 'none'}, expected {hex(expected) if expected is not None else 'none'}")


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("serial")
    parser.add_argument("command", choices=["drift", "links", "listen", "hoststop", "hostkill", "due", "rebootcheck", "coldstart", "fgs", "cleanup", "quiet", "unquiet"])
    parser.add_argument("--count", type=int, default=None)
    parser.add_argument("--gap", type=int, default=20)
    parser.add_argument("--lead", type=int, default=None)
    parser.add_argument("--tag", default="p5-reboot")
    parser.add_argument("--kill", choices=["am", "force-stop"], default="am")
    parser.add_argument("--screen", choices=["off", "on"], default="off")
    parser.add_argument("--locked-only", action="store_true", help="rebootcheck: only read the logs (the device is still locked)")
    args = parser.parse_args(argv)
    report = Report()
    print(f"{smoke.label(args.serial)}: {args.command}", flush=True)
    if args.command == "coldstart":
        args.count = args.count or 3
        args.lead = args.lead or 40
        coldstart(args.serial, args, report)
    elif args.command == "fgs":
        fgs(args.serial, args, report)
    elif args.command == "quiet":
        quiet(args.serial, report)
    elif args.command == "unquiet":
        unquiet(args.serial, report)
    else:
        if not host_ready(args.serial, report):
            return 1
        if args.command == "drift":
            args.count = args.count or 10
            args.lead = args.lead or 50
            drift(args.serial, args, report)
        elif args.command == "links":
            links(args.serial, args, report)
        elif args.command == "listen":
            listen(args.serial, args, report)
        elif args.command == "hoststop":
            args.lead = args.lead or 45
            hoststop(args.serial, args, report)
        elif args.command == "hostkill":
            args.lead = args.lead or 45
            hostkill(args.serial, args, report)
        elif args.command == "due":
            args.lead = args.lead or 240
            due(args.serial, args, report)
        elif args.command == "rebootcheck":
            rebootcheck(args.serial, args, report)
        else:
            run_mode(args.serial, "cleanup", report, {}, timeout=120)
            hook(args.serial, "clear")
    print(f"{args.serial}: P5 {args.command} {'PASS' if report.failed == 0 else 'FAIL'} ({report.passed} passed, {report.failed} failed)", flush=True)
    return 0 if report.failed == 0 else 1


if __name__ == "__main__":
    sys.exit(main())

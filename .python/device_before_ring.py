"""End-to-end runs of the ring-time decisions of 3-Chime Alarm 1.2.0 (roadmap P8.4) on one device with a host that has the
`beforeRing` script API (AutoJs6 versionCode 6641 or later with host commit 3abe4f2f8f).

    py .python/device_before_ring.py <serial> cases [--lead 30] [--gap 40] [--only key,key]
    py .python/device_before_ring.py <serial> hoststop [--lead 30]
    py .python/device_before_ring.py <serial> locked [--lead 30]
    py .python/device_before_ring.py <serial> all [--locked]
    py .python/device_before_ring.py <serial> cleanup

Every case is a silent one-shot alarm (label `p84-<key>`, tag `p84`, sound `silent` at volume 0, no vibration, no
snooze, auto silence after 1 minute) bound to a decider script on `beforeRing` (`leadSeconds` = --lead) and to a recorder script on `ring` / `skip`
that appends each event to `events.txt`. The deciders write a marker `ran-<key>.json` (event type, the value their
decision call returned, the time, the deadline).

- weather-skip / weather-ring: `weather-brief.js`, the example of appendix A section 5.4 and of the documentation, reading
  the local fake data `./weather.json` next to it (`dayOff` true / false). Skip: no ring session, a `skip` event with
  `reason` 'script' and `detail` '今天休息'. Ring: the session speaks after the (silent) sound, `ring` event as usual.
- overrides: `ring({ label: 'p84-brief', sound: { source: 'default', volume: 0 }, speak })` changes only this ring
  (session sound / speak, the ringing label, the `ring` event's label); the alarm keeps its own label; a second call
  (`skip`) from the same script returns false.
- Nothing is heard or felt: the speech of the device copies names the language `zz-ZZ` that no engine has (the speech
  fails at once; the session then falls back to the default sound, which volume 0 keeps at gain 0), and no case
  vibrates (the `vibrate` override is covered by BeforeRingTest). The alarm stream volume is checked to be back afterwards.
- nodecide (the script decides nothing), late (`skip` 1 s after the deadline returns false), error (the script throws):
  the alarm rings as configured (no sound, no vibration, no speech).
- holiday: a daily alarm at the same time with `holidays { region, mode: 'skip' }` for a region where today is a holiday
  (bundled data): no `beforeRing` is sent, the decider never runs, a `skip` event with `reason` 'holiday', the next
  trigger is a later day.
- Per case: the `beforeRing` wake-up is logged once (`ChimeSchedule`), its lateness against `at - lead`, `deadline` =
  `at - 5 s`, the host answer (`ChimeTrigger launched=1`), and the time from the wake-up to the decision.
- hoststop: the weather-skip case alone with the host force-stopped before the wake-up: ChimeTrigger reports no host
  (`scriptSkipped=unavailable`), the alarm rings as configured, the host stays stopped, the decider never ran and the
  bound listener hears no event (no broadcast reaches a force-stopped package). The
  accessibility settings are saved before and put back afterwards (a force stop clears them), and the AutoJs6 service
  is checked to be bound again.
- locked: the weather-ring case with the screen turned off right after arming (keyguard on if the device locks at once):
  the host starts the decider all the same and the ring speaks; reports `keyguard` of the ringing state. The device is
  left locked: unlock it, then run `cleanup` (the collect step needs a script run).
- all: cases, then hoststop (then locked with --locked).
- cleanup: removes alarms tagged p84 and the pushed folder.

Output: PASS / FAIL / SKIP per check with the key numbers only. Nothing here answers a dialog or unlocks the device. The
focused window is brought back to the one before the run when it was an activity that can be started again.

zh-CN: 3-Chime Alarm 1.2.0 响铃前决定 (路线图 P8.4) 的单机端到端联调, 需要带 `beforeRing` 脚本 API 的宿主 (versionCode 6641 及以上, 含宿主提交
3abe4f2f8f). 每个用例是一个静音的一次性闹钟 (标签 `p84-<key>`, 标记 `p84`, 不振动, 不贪睡, 1 分钟后自动停止), 以 `beforeRing` 绑定决定脚本
(`leadSeconds` 为 --lead), 并以 `ring` / `skip` 绑定记录脚本 (把事件追加到 `events.txt`); 决定脚本写标记 `ran-<key>.json`.
weather-skip / weather-ring 使用附录 A 第 5.4 节与文档中的 `weather-brief.js` 示例, 读取同目录的本地假数据 `./weather.json`; overrides 只改
本次响铃; 全程不出声不振动 (闹钟音量 0, 设备副本的朗读使用不存在的语言 `zz-ZZ`, 振动覆盖由 BeforeRingTest 覆盖); nodecide / late / error 照常响; holiday 为今天是节假日的地区的每日闹钟, 不发 `beforeRing`, 直接 `skip` (`reason` 'holiday').
hoststop 在唤醒前强行停止宿主, locked 在布置后关屏. 输出只有每项的 PASS / FAIL / SKIP 与关键数值; 不代为点按对话框, 也不解锁设备.
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
import device_p5 as p5  # noqa: E402
import device_smoke as smoke  # noqa: E402

HOST = smoke.HOST
REMOTE = "/sdcard/Download/autojs6-chime-p84"
TAG = "p84"
LOG_TAGS = ["ChimeSchedule", "ChimeTrigger", "ChimeRing"]
DEADLINE_MARGIN_MS = 5_000
# A language no engine has: the speech fails at once, so nothing is heard (the session still logs `speak`).
# zh-CN: 没有引擎支持的语言: 朗读立即失败, 因此不出声 (会话日志仍记录 `speak`).
MUTE_LANGUAGE = "zz-ZZ"
ACCESSIBILITY = (("secure", "enabled_accessibility_services"), ("secure", "accessibility_enabled"))

MAIN = r"""
var DIR = '@DIR@';
var cfg = JSON.parse(files.read(DIR + '/config.json'));
var checks = [];
var values = {};
var started = Date.now();

function record(name, state, detail) { checks.push([name, state, String(detail === undefined || detail === null ? '' : detail).slice(0, 240)]); }
function describe(e) { return e && e.code ? '[' + e.code + '] ' + e.message : String(e && e.message ? e.message : e); }
function assert(cond, message) { if (!cond) throw new Error(message); }
function check(name, fn) { try { record(name, 'PASS', fn()); return true; } catch (e) { record(name, 'FAIL', describe(e)); return false; } }
function arr(x) {
    if (x === undefined || x === null) return [];
    if (Array.isArray(x)) return x;
    var out = [];
    for (var i = 0; i < x.size(); i++) out.push(x.get(i));
    return out;
}
function tagged() { return arr(alarms.list()).filter(function (x) { return String(x.tag || '') === cfg.tag; }); }
function removeTagged() {
    var removed = 0;
    tagged().forEach(function (x) { try { if (alarms.remove(x.id)) removed++; } catch (e) { /* gone */ } });
    return removed;
}
function save() {
    files.write(DIR + '/result.json', JSON.stringify({ mode: cfg.mode, seconds: Math.round((Date.now() - started) / 1000), checks: checks, values: values }));
    files.write(DIR + '/done', '1');
}
function pad(n) { return (n < 10 ? '0' : '') + n; }
function iso(t) { return t ? new Date(t).toISOString() : 'none'; }

/** A region whose data has today as a holiday: the bundled data has 2026-11-13 in ME, IR and SH. */
function holidayRegion() {
    var preferred = ['ME', 'IR', 'SH'];
    var codes = preferred.concat(arr(alarms.holidays.regions()).map(function (r) { return String(r.code); }));
    for (var i = 0; i < codes.length; i++) {
        try { if (alarms.holidays.isHoliday(null, codes[i])) return codes[i]; } catch (e) { /* no data for that region */ }
    }
    throw new Error('no region has today as a holiday');
}

function modeArm() {
    check('plugin features', function () {
        var caps = alarms.capabilities();
        var features = arr(caps.features).map(String);
        assert(features.indexOf('beforeRing') >= 0 && features.indexOf('holidays') >= 0, 'features ' + features.join(','));
        values.plugin = String(caps.pluginVersionName);
        return caps.pluginVersionName + ' (' + caps.pluginVersionCode + '), ' + features.join(',');
    });
    var removed = removeTagged();
    if (removed) record('leftover p84 alarms removed', 'PASS', removed);
    var base = Math.ceil((Date.now() + cfg.first * 1000) / 1000) * 1000;
    values.cases = {};
    cfg.cases.forEach(function (c) {
        check('arm ' + c.key, function () {
            var at = base + c.offset * 1000;
            var o = { label: 'p84-' + c.key, tag: cfg.tag, sound: { source: 'silent', volume: 0 }, vibrate: false, snooze: false, autoSilenceMinutes: 1 };
            if (c.kind === 'holiday') {
                var d = new Date(at);
                o.time = pad(d.getHours()) + ':' + pad(d.getMinutes()) + ':' + pad(d.getSeconds());
                o.repeat = { freq: 'daily' };
                o.holidays = { region: holidayRegion(), mode: 'skip' };
                values.region = o.holidays.region;
            } else {
                o.at = at;
            }
            var a = alarms.add(o);
            a.bind(DIR + '/' + c.key + '/' + c.file, { events: ['beforeRing'], leadSeconds: cfg.lead });
            a.bind(DIR + '/rec.js', { events: ['ring', 'skip'] });
            values.cases[c.key] = { id: a.id, at: at, next: a.nextTrigger || null };
            return a.id + ' in ' + Math.round((at - Date.now()) / 1000) + ' s, next ' + iso(a.nextTrigger) + (o.holidays ? ', region ' + o.holidays.region : '');
        });
    });
    save();
}

function modeCollect() {
    values.alarms = {};
    tagged().forEach(function (x) { values.alarms[String(x.id)] = { label: x.label === null ? null : String(x.label), enabled: x.enabled, next: x.nextTrigger || null }; });
    values.removed = removeTagged();
    save();
}

try {
    if (cfg.mode === 'arm') modeArm();
    else if (cfg.mode === 'collect') modeCollect();
    else { values.removed = removeTagged(); save(); }
} catch (e) {
    record('script', 'FAIL', describe(e));
    save();
}
"""

# The decider of appendix A section 5.4 (the documentation's example) and a marker for the check.
WEATHER = r"""
/* weather-brief.js, bound to the alarm's beforeRing event (roadmap appendix A section 5.4). */
let e = alarms.currentEvent();
let decided = null;
if (e !== null && e.type === 'beforeRing') {
    /* weather.json: the local fake data of this run. */
    let w = JSON.parse(files.read('./weather.json'));
    if (w.dayOff) {
        decided = e.skip('今天休息');
    } else {
        decided = e.ring({ speak: { text: `早上好, 今天${w.text}, 最高 ${w.high} 度`, mode: 'after', language: '@MUTE@' } });
    }
}
files.write('@DIR@/ran-@KEY@.json', JSON.stringify({ type: e ? String(e.type) : null, decided: decided, at: Date.now(),
    deadline: e ? e.deadline : null, occurrence: e ? String(e.occurrence) : null, enumerable: e ? Object.keys(e).indexOf('skip') >= 0 : null }));
"""

OVERRIDES = r"""
let e = alarms.currentEvent();
let decided = e.ring({ label: 'p84-brief', sound: { source: 'default', volume: 0 }, speak: { text: 'p84', mode: 'after', language: '@MUTE@' } });
let again = e.skip('second');
files.write('@DIR@/ran-@KEY@.json', JSON.stringify({ type: String(e.type), decided: decided, again: again, at: Date.now(), deadline: e.deadline, occurrence: String(e.occurrence) }));
"""

NODECIDE = r"""
let e = alarms.currentEvent();
files.write('@DIR@/ran-@KEY@.json', JSON.stringify({ type: String(e.type), decided: null, at: Date.now(), deadline: e.deadline, occurrence: String(e.occurrence) }));
"""

LATE = r"""
let e = alarms.currentEvent();
while (Date.now() < e.deadline + 1000) sleep(200);
let decided = e.skip('late');
files.write('@DIR@/ran-@KEY@.json', JSON.stringify({ type: String(e.type), decided: decided, at: Date.now(), deadline: e.deadline, occurrence: String(e.occurrence) }));
"""

ERROR = r"""
let e = alarms.currentEvent();
files.write('@DIR@/ran-@KEY@.json', JSON.stringify({ type: String(e.type), decided: null, at: Date.now(), deadline: e.deadline, occurrence: String(e.occurrence) }));
throw new Error('p84 decider error');
"""

RECORDER = r"""
let e = alarms.currentEvent();
function text(v) { return v === undefined || v === null ? null : String(v); }
files.append('@DIR@/events.txt', JSON.stringify({ type: text(e.type), alarmId: text(e.alarmId), occurrence: text(e.occurrence), label: text(e.label),
    reason: text(e.reason), detail: text(e.detail), scheduledAt: e.scheduledAt === undefined ? null : Number(e.scheduledAt), at: Date.now() }) + '\n');
"""

WEATHER_SKIP = {"dayOff": True, "text": "晴", "high": 20}
WEATHER_RING = {"dayOff": False, "text": "小雨", "high": 18}

# key -> (kind, decider file, decider source, weather data, what the ring looks like or None when it must not ring)
CASES = {
    "weather-skip": ("weather", "weather-brief.js", WEATHER, WEATHER_SKIP, None),
    "weather-ring": ("weather", "weather-brief.js", WEATHER, WEATHER_RING, {"sound": "false", "vibrate": "false", "speak": "after", "volume": "0.0"}),
    "overrides": ("overrides", "decide.js", OVERRIDES, None, {"sound": "true", "vibrate": "false", "speak": "after", "volume": "0.0"}),
    "nodecide": ("nodecide", "decide.js", NODECIDE, None, {"sound": "false", "vibrate": "false", "speak": "none", "volume": "0.0"}),
    "late": ("late", "decide.js", LATE, None, {"sound": "false", "vibrate": "false", "speak": "none", "volume": "0.0"}),
    "error": ("error", "decide.js", ERROR, None, {"sound": "false", "vibrate": "false", "speak": "none", "volume": "0.0"}),
    "holiday": ("holiday", "weather-brief.js", WEATHER, WEATHER_RING, None),
}


def now_ms():
    return int(time.time() * 1000)


def shell_cat(serial, path):
    return p5.adb(serial, "shell", f"cat {path} 2>/dev/null").strip()


def push(serial, mode, config, keys, fresh):
    if fresh:
        p5.adb(serial, "shell", "rm", "-rf", REMOTE)
    else:
        p5.adb(serial, "shell", f"rm -f {REMOTE}/done {REMOTE}/result.json")
    p5.adb(serial, "shell", "mkdir", "-p", REMOTE)
    texts = {"p84.js": MAIN.replace("@DIR@", REMOTE), "config.json": json.dumps({"mode": mode, **config})}
    if fresh:
        texts["rec.js"] = RECORDER.replace("@DIR@", REMOTE)
        for key in keys:
            _, file, source, weather, _ = CASES[key]
            texts[f"{key}/{file}"] = source.replace("@DIR@", REMOTE).replace("@KEY@", key).replace("@MUTE@", MUTE_LANGUAGE)
            if weather is not None:
                texts[f"{key}/weather.json"] = json.dumps(weather, ensure_ascii=False)
    folder = Path(tempfile.mkdtemp(prefix="chime-p84-"))
    try:
        for name, text in texts.items():
            local = folder / name.replace("/", "__")
            local.write_text(text, encoding="utf-8", newline="")
            if "/" in name:
                p5.adb(serial, "shell", "mkdir", "-p", f"{REMOTE}/{name.rsplit('/', 1)[0]}")
            p5.adb(serial, "push", str(local), f"{REMOTE}/{name}")
            local.unlink()
    finally:
        folder.rmdir()


def run_main(serial, report, mode, config, keys=(), fresh=False, timeout=180):
    """Runs p84.js in [mode] through the host and reports its checks; answers its `values`, or None without a result."""
    print(f"  {mode}:", flush=True)
    push(serial, mode, {"tag": TAG, **config}, keys, fresh)
    p5.adb(serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP")
    p5.adb(serial, "shell", "am", "start", "-n", smoke.launcher(serial))
    time.sleep(2)
    p5.adb(serial, "shell", "am", "start", "-a", "android.intent.action.VIEW", "-t", "application/x-javascript",
           "-d", f"file://{REMOTE}/p84.js", "-n", f"{HOST}/org.autojs.autojs.external.open.RunIntentActivity")
    deadline = time.time() + timeout
    while time.time() < deadline and shell_cat(serial, f"{REMOTE}/done") != "1":
        time.sleep(2)
    raw = shell_cat(serial, f"{REMOTE}/result.json")
    if not raw.startswith("{"):
        report.line(mode, "FAIL", f"no result in {timeout} s (unlock the device; AutoJs6 needs all-files access)")
        return None
    result = json.loads(raw)
    for name, state, detail in result["checks"]:
        report.line(name, state, detail)
    return result.get("values") or {}


def fields(message):
    return dict(re.findall(r"(\w+)=(\S+)", message))


def marker(serial, key):
    raw = shell_cat(serial, f"{REMOTE}/ran-{key}.json")
    return json.loads(raw) if raw.startswith("{") else None


def events(serial):
    out = []
    for line in shell_cat(serial, f"{REMOTE}/events.txt").splitlines():
        if line.startswith("{"):
            out.append(json.loads(line))
    return out


def wait_device(serial, offset, device_t):
    """Sleeps until the device clock reaches [device_t] (ms); [offset] is device minus local."""
    rest = device_t - offset - now_ms()
    if rest > 0:
        time.sleep(rest / 1000)


def check_wakeup(report, key, case, lead, lines, host_expect):
    """The beforeRing wake-up of one case: once, on time, the deadline, the host's answer. Answers the send time (ms)."""
    alarm_id, at = case["id"], case["at"]
    sent = [(t, m) for t, tag, m in lines if tag == "ChimeSchedule" and m.startswith(f"beforeRing {alarm_id}: sent")]
    if not sent:
        report.line(f"{key}: beforeRing sent once", "FAIL", "no wake-up line")
        return None
    t, message = sent[0]
    late = int(t * 1000) - (at - lead * 1000)
    deadline = int(fields(message).get("deadline", "0").rstrip(","))
    report.line(f"{key}: beforeRing sent once", "PASS" if len(sent) == 1 and -1_000 <= late <= 10_000 else "FAIL",
                f"{len(sent)} line(s), {late:+d} ms against at - {lead} s")
    report.line(f"{key}: deadline = scheduledAt - 5 s", "PASS" if deadline == at - DEADLINE_MARGIN_MS else "FAIL", f"{deadline - at:+d} ms")
    trigger = [m.split(": ", 1)[-1] for _, tag, m in lines if tag == "ChimeTrigger" and m.startswith(f"beforeRing {alarm_id} -> ")]
    ok = bool(trigger) and host_expect in trigger[0]
    report.line(f"{key}: host answer", "PASS" if ok else "FAIL", trigger[0] if trigger else "no ChimeTrigger line")
    return int(t * 1000)


def check_ring(serial, report, key, case, expect, lines):
    """The ring session of one case against [expect] (None: it must not ring); dismisses it."""
    alarm_id = case["id"]
    sessions = [m for _, tag, m in lines if tag == "ChimeRing" and m.startswith(f"ring session {alarm_id} occurrence=")]
    if expect is None:
        report.line(f"{key}: does not ring", "PASS" if not sessions else "FAIL", f"{len(sessions)} ring session(s)")
        return
    if not sessions:
        report.line(f"{key}: rings", "FAIL", "no ring session")
        return
    got = fields(sessions[-1])
    wrong = [f"{name} {got.get(name)} != {value}" for name, value in expect.items() if got.get(name) != value]
    report.line(f"{key}: rings with " + ", ".join(f"{k}={v}" for k, v in expect.items()), "PASS" if not wrong else "FAIL",
                "; ".join(wrong) or f"player={got.get('player')}")
    state = p5.hook(serial, "state")
    if state.get("sounding") == alarm_id:
        case["ringing"] = {"label": state.get("label"), "keyguard": state.get("keyguard"), "volume": state.get("volume")}
    p5.hook(serial, "dismiss", id=alarm_id)


def check_marker(report, key, kind, mark, sent_ms):
    if kind == "holiday":
        report.line(f"{key}: decider never runs", "PASS" if mark is None else "FAIL", "no marker" if mark is None else "marker written")
        return
    if mark is None:
        report.line(f"{key}: decider ran", "FAIL", "no marker")
        return
    latency = f", {mark['at'] - sent_ms} ms after the wake-up" if sent_ms else ""
    detail = f"type {mark.get('type')}, returned {mark.get('decided')}{latency}"
    expected = {"weather": True, "overrides": True, "nodecide": None, "error": None, "late": False}[kind]
    ok = mark.get("type") == "beforeRing" and mark.get("decided") == expected
    if kind == "weather":
        ok = ok and mark.get("enumerable") is False
        detail += f", skip enumerable {mark.get('enumerable')}"
    if kind == "overrides":
        ok = ok and mark.get("again") is False
        detail += f", second call {mark.get('again')}"
    if kind == "late":
        detail += f", called {mark['at'] - mark['deadline']} ms after the deadline"
    report.line(f"{key}: decider ran and returned {expected}", "PASS" if ok else "FAIL", detail)


def check_events(report, key, kind, case, recorded, collected):
    alarm_id = case["id"]
    mine = [e for e in recorded if e.get("alarmId") == alarm_id]
    skips = [e for e in mine if e.get("type") == "skip"]
    rings = [e for e in mine if e.get("type") == "ring"]
    if CASES[key][4] is None:
        reason, detail = ("holiday", None) if kind == "holiday" else ("script", "今天休息")
        ok = len(skips) == 1 and not rings and skips[0].get("reason") == reason and skips[0].get("detail") == detail
        got = f"skip reason {skips[0].get('reason')}, detail {skips[0].get('detail')}" if skips else "no skip event"
        report.line(f"{key}: skip event (reason {reason})", "PASS" if ok else "FAIL", f"{got}, {len(rings)} ring event(s)")
        if kind == "holiday":
            later = (case.get("next") or 0) - case["at"]
            report.line(f"{key}: next trigger is a later day", "PASS" if later >= 12 * 3_600_000 else "FAIL", f"{later / 3_600_000:.1f} h after this one")
        return
    label = "p84-brief" if kind == "overrides" else f"p84-{key}"
    ok = len(rings) == 1 and not skips and rings[0].get("label") == label
    report.line(f"{key}: ring event with label {label}", "PASS" if ok else "FAIL",
                f"{len(rings)} ring, {len(skips)} skip event(s), label {rings[0].get('label') if rings else '-'}")
    if kind == "overrides":
        ringing = case.get("ringing") or {}
        report.line(f"{key}: ringing label is the override", "PASS" if ringing.get("label") == "p84-brief" else "FAIL",
                    f"label {ringing.get('label')}, alarm stream volume {ringing.get('volume')}")
        kept = (collected or {}).get(alarm_id)
        report.line(f"{key}: the alarm keeps its own label", "PASS" if kept is None or kept.get("label") == f"p84-{key}" else "FAIL",
                    "removed after the ring" if kept is None else f"label {kept.get('label')}")


def run_cases(serial, args, report, keys, host_expect="launched=1", before_wakeup=None, after_arm=None):
    """Arms [keys], follows each case to its ring, then checks the events and collects the alarms."""
    offsets = {key: i * args.gap for i, key in enumerate(keys)}
    since = p5.device_ms(serial)
    config = {"lead": args.lead, "first": args.lead + 30,
              "cases": [{"key": k, "kind": CASES[k][0], "file": CASES[k][1], "offset": offsets[k]} for k in keys]}
    values = run_main(serial, report, "arm", config, keys, fresh=True)
    cases = (values or {}).get("cases") or {}
    if len(cases) != len(keys):
        return None
    if after_arm:
        after_arm()
    offset = p5.device_ms(serial) - now_ms()
    if before_wakeup:
        before_wakeup(min(c["at"] for c in cases.values()) - args.lead * 1000)
    sent = {}
    for key in sorted(keys, key=lambda k: cases[k]["at"]):
        case = cases[key]
        wait_device(serial, offset, case["at"] + 7_000)
        lines = p5.logs(serial, LOG_TAGS, since)
        kind = CASES[key][0]
        if kind == "holiday":
            woke = [m for _, tag, m in lines if tag in ("ChimeSchedule", "ChimeTrigger") and m.startswith(f"beforeRing {case['id']}")]
            report.line(f"{key}: no beforeRing", "PASS" if not woke else "FAIL", "; ".join(woke) or "no wake-up, no host call")
        else:
            sent[key] = check_wakeup(report, key, case, args.lead, lines, host_expect)
        check_ring(serial, report, key, case, CASES[key][4], lines)
    wait_device(serial, offset, max(c["at"] for c in cases.values()) + 15_000)
    recorded = events(serial)
    return {"cases": cases, "sent": sent, "recorded": recorded, "since": since}


def finish(serial, report, keys, state, collect=True):
    collected = None
    if collect:
        values = run_main(serial, report, "collect", {})
        collected = (values or {}).get("alarms")
        report.line("p84 alarms removed", "PASS" if values is not None else "FAIL", f"{(values or {}).get('removed')} removed")
    for key in keys:
        kind = CASES[key][0]
        if state["sent"].get(key) is not None or kind == "holiday" or state.get("no_decider"):
            mark = marker(serial, key)
            if state.get("no_decider"):
                report.line(f"{key}: decider never ran", "PASS" if mark is None else "FAIL", "no marker" if mark is None else "marker written")
            else:
                check_marker(report, key, kind, mark, state["sent"].get(key))
        if state.get("host_stopped"):
            # The system delivers no broadcast to a force-stopped package (ROADMAP P5), so the bound listener hears nothing.
            # zh-CN: 系统不向被强行停止的应用投递广播 (ROADMAP P5), 绑定的监听脚本收不到任何事件.
            alarm_id = state["cases"][key]["id"]
            mine = [e for e in state["recorded"] if e.get("alarmId") == alarm_id]
            report.line(f"{key}: no event reaches the stopped host", "PASS" if not mine else "FAIL", f"{len(mine)} event(s)")
        else:
            check_events(report, key, kind, state["cases"][key], state["recorded"], collected)
    lines = p5.logs(serial, LOG_TAGS, state["since"])
    for key in keys:
        if CASES[key][4] is None:
            late = [m for _, tag, m in lines if tag == "ChimeRing" and m.startswith(f"ring session {state['cases'][key]['id']} ")]
            report.line(f"{key}: still no ring at the end", "PASS" if not late else "FAIL", f"{len(late)} ring session(s)")
    if collect:
        p5.adb(serial, "shell", "rm", "-rf", REMOTE)


def cases(serial, args, report):
    keys = [k for k in CASES if not args.only or k in args.only.split(",")]
    p5.quiet(serial, report)
    try:
        state = run_cases(serial, args, report, keys)
        if state:
            finish(serial, report, keys, state)
    finally:
        p5.unquiet(serial, report)


def saved_accessibility(serial):
    return {f"{space}/{key}": p5.adb(serial, "shell", "settings", "get", space, key).strip() for space, key in ACCESSIBILITY}


def restore_accessibility(serial, report, saved):
    for name, value in saved.items():
        if value not in ("null", ""):
            p5.adb(serial, "shell", "settings", "put", *name.split("/"), value)
    back = saved_accessibility(serial)
    report.line("accessibility settings restored", "PASS" if back == saved else "FAIL", "" if back == saved else f"{back} != {saved}")
    if HOST in saved.get("secure/enabled_accessibility_services", ""):
        started = time.time()
        bound = False
        while not bound and time.time() - started < 30:
            text = p5.adb(serial, "shell", "dumpsys", "accessibility", timeout=60)
            bound = any("label=AutoJs6" in line for line in text.splitlines() if "Bound services" in line)
            if not bound:
                time.sleep(2)
        report.line("AutoJs6 accessibility service bound again", "PASS" if bound else "FAIL", f"{time.time() - started:.0f} s")


def alarm_volume(serial):
    """The alarm stream volume index, or None when the command does not answer it."""
    match = re.search(r"volume is (\d+)", p5.adb(serial, "shell", "cmd", "media_session", "volume", "--stream", "4", "--get"))
    return int(match.group(1)) if match else None


def focused(serial):
    """The component of the focused window (`package/activity`), or None."""
    match = re.search(r"mCurrentFocus=Window\{\S+ \S+ ([\w.]+/[\w.$]+)\}", p5.adb(serial, "shell", "dumpsys", "window", timeout=60))
    return match.group(1) if match else None


def hoststop(serial, args, report):
    keys = ["weather-skip"]
    accessibility = saved_accessibility(serial)
    stopped = {}

    def stop(wakeup_at):
        p5.adb(serial, "shell", "input", "keyevent", "KEYCODE_HOME")
        time.sleep(2)
        p5.adb(serial, "shell", "am", "force-stop", HOST)
        stopped["ok"] = not p5.adb(serial, "shell", "pidof", HOST).strip()
        report.line("host force-stopped before the wake-up", "PASS" if stopped["ok"] else "FAIL",
                    f"{(wakeup_at - p5.device_ms(serial)) / 1000:.0f} s before it")

    original = CASES["weather-skip"]
    CASES["weather-skip"] = (original[0], original[1], original[2], original[3], {"sound": "false", "vibrate": "false", "speak": "none", "volume": "0.0"})
    p5.quiet(serial, report)
    try:
        state = run_cases(serial, args, report, keys, host_expect="scriptSkipped=unavailable", before_wakeup=stop)
        if state:
            report.line("host still stopped after the ring", "PASS" if not p5.adb(serial, "shell", "pidof", HOST).strip() else "FAIL", "")
            state["no_decider"] = True
            state["host_stopped"] = True
            finish(serial, report, keys, state)
    finally:
        CASES["weather-skip"] = original
        restore_accessibility(serial, report, accessibility)
        p5.unquiet(serial, report)


def keyguard_locked(serial):
    text = p5.adb(serial, "shell", "dumpsys", "window", "policy", timeout=60)
    return bool(re.search(r"(isKeyguardShowing|mShowingLockscreen|showing)=true", text))


def locked(serial, args, report):
    keys = ["weather-ring"]

    def sleep_screen():
        p5.adb(serial, "shell", "input", "keyevent", "KEYCODE_HOME")
        time.sleep(1)
        p5.adb(serial, "shell", "input", "keyevent", "KEYCODE_SLEEP")
        time.sleep(3)
        report.line("screen off after arming", "PASS", f"keyguard {keyguard_locked(serial)}")

    p5.quiet(serial, report)
    try:
        state = run_cases(serial, args, report, keys, after_arm=sleep_screen)
        if state:
            ringing = state["cases"]["weather-ring"].get("ringing") or {}
            report.line("weather-ring: rang with the device locked", "PASS" if ringing.get("keyguard") == "true" else "SKIP",
                        f"keyguard {ringing.get('keyguard')} while ringing")
            still = keyguard_locked(serial)
            finish(serial, report, keys, state, collect=not still)
            if still:
                print("    the device is locked: unlock it, then run `cleanup` to remove the p84 alarm and the pushed files", flush=True)
    finally:
        p5.unquiet(serial, report)


def cleanup(serial, args, report):
    values = run_main(serial, report, "cleanup", {}, fresh=False)
    report.line("p84 alarms removed", "PASS" if values is not None else "FAIL", f"{(values or {}).get('removed')} removed")
    p5.adb(serial, "shell", "rm", "-rf", REMOTE)


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("serial")
    parser.add_argument("command", choices=["cases", "hoststop", "locked", "all", "cleanup"])
    parser.add_argument("--lead", type=int, default=30, help="leadSeconds of the beforeRing bindings (15 - 300)")
    parser.add_argument("--gap", type=int, default=40, help="seconds between the cases")
    parser.add_argument("--only", default="", help="cases: a comma separated subset of " + ",".join(CASES))
    parser.add_argument("--locked", action="store_true", help="all: run locked at the end")
    args = parser.parse_args(argv)
    report = p5.Report()
    print(f"{smoke.label(args.serial)}: before ring {args.command}", flush=True)
    if args.command != "cleanup" and not p5.host_ready(args.serial, report):
        return 1
    volume, focus = alarm_volume(args.serial), focused(args.serial)
    if args.command in ("cases", "all"):
        cases(args.serial, args, report)
    if args.command in ("hoststop", "all"):
        hoststop(args.serial, args, report)
    if args.command == "locked" or (args.command == "all" and args.locked):
        locked(args.serial, args, report)
    if args.command == "cleanup":
        cleanup(args.serial, args, report)
    after = alarm_volume(args.serial)
    report.line("alarm stream volume as before", "PASS" if after == volume else "FAIL", f"{volume} -> {after}")
    if focus and not (args.command == "locked" or args.locked) and focused(args.serial) != focus:
        p5.adb(args.serial, "shell", "am", "start", "-n", focus)
        time.sleep(2)
    now = focused(args.serial)
    report.line("focused window as before", "PASS" if now == focus else "SKIP", f"{focus} -> {now}")
    print(f"{args.serial}: P8.4 {args.command} {'PASS' if report.failed == 0 else 'FAIL'} ({report.passed} passed, {report.failed} failed)", flush=True)
    return 0 if report.failed == 0 else 1


if __name__ == "__main__":
    sys.exit(main())

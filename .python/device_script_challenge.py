"""End-to-end runs of the `script` dismiss challenge of 3-Chime Alarm 1.3.0 (roadmap P9.2, T284; the protocol of
chime-p9-protocol.md section 4) on one device: each fallback condition once, and the normal path (a script dismisses the
round) through the host's `scriptStatus` answers (host decisions T312 - T314).

    py .python/device_script_challenge.py <serial> cases [--only key,key] [--lead 25]
    py .python/device_script_challenge.py <serial> hoststop [--lead 25]
    py .python/device_script_challenge.py <serial> oldhost [--lead 25]
    py .python/device_script_challenge.py <serial> sample [--sample PATH] [--lead 25]
    py .python/device_script_challenge.py <serial> locked [--lead 240] [--no-reboot]
    py .python/device_script_challenge.py <serial> cleanup

The plugin must be a debug build (the rounds are read and closed through its RingDebugReceiver). Every case is a silent
one-shot alarm (label `p24-probe-p92-<key>`, the probe prefix of the hook, tag `p92`, sound `silent` at volume 0, no
vibration, no snooze, auto silence after 1 minute) with `dismiss: { challenge: 'script' }`, armed alone `lead` seconds
ahead by a host script; the round is closed with the hook's `clear` afterwards (the user's dismiss for a round that fell
back to `none`, otherwise the removal ends it, T66). Per case: the plugin's ring answer line, the `scriptStatus` lines,
the fallback line `script challenge <occurrence> fell back to <fallback>: <reason>` (`adb logcat -s ChimeTrigger`) and
its delay after the ring session started, and the challenge in effect read from the hook.

cases (a host with the `scriptStatus` answer, AutoJs6 with host commit e323d560d5):
- normal: one bound `ring` script that waits 8 s and calls `alarms.dismiss(id)`: the host answers `launched=1`, a poll
  answers `tracked=true running=1 started=1 failed=0`, the challenge stays `script` while it waits, no fallback; the
  round ends with `dismiss` `by: 'script'`, `challenge: 'script'` (a recorder bound to `dismiss`); the script saw the
  `ring` event's and `alarms.ringing()`'s `challenge` 'script'.
- listener: no binding, a live `alarms.on('ring')` listener dismisses 3 s after the ring: `ring to 0 hosts`, no fallback,
  the listener's `dismiss` event has `by: 'script'`, `challenge: 'script'`.
- listenergone: no binding, the live listener leaves 3 s after the ring without dismissing: `noScript` (the last `ring`
  listener left), the round shows its fallback `shake`.
- ended: two bound scripts, one deleted before the ring and one that exits after 7 s: the host answers `launched=1
  missing=1`, the polls answer `running=1 started=1 failed=1` and then `running=0 started=1 failed=1` (T314: `failed`
  counts the missing script), `ended` about 10 s after the ring, fallback `math`.
- launchfailed: the only bound script is deleted before the ring: `launched=0 missing=1`, `launchFailed` at once, the
  default fallback `none` (the alarm has no `script` group).
- noscript: no binding and no live listener: `ring to 0 hosts`, `noScript` at once, fallback `type`.
hoststop: one bound script, the host force-stopped before the ring: no broadcast reaches a stopped package, so the ring
  is not answered and the round falls back at once (`unavailable`); the accessibility settings are put back afterwards.
oldhost (a host without the `scriptStatus` answer): one bound script that runs 20 s: the host answers `launched=1`, the
  poll gets no answer, `unavailable` about 5 s after the answer (T312).
sample: the host sample "拔下充电器才能关闭 [v6.8.1+].js" (from the host checkout, or --sample) end to end: run directly it
  creates its daily alarm (`script` challenge, fallback `math`, no snooze) bound to itself; the alarm is moved to ring
  `lead` seconds ahead (and a recorder bound to `dismiss`); started by the ring while the device charges, the sample
  waits; then the unplug is simulated (`dumpsys battery unplug` and status discharging) and the sample dismisses the
  round (`by: 'script'`); `dumpsys battery reset` afterwards and the alarm is removed. Pulling a real cable is a manual
  test item.
locked: arms one bound alarm `lead` seconds ahead and reboots the device (with a secure lock screen the user stays
  locked until the unlock); after the boot, while `dumpsys user` still reads RUNNING_LOCKED, the ring falls back at once
  (`locked`). The device is left locked: the user unlocks it, then runs `cleanup`. With --no-reboot the caller reboots.
cleanup: removes the p92 alarms and the sample's alarm, the pushed folder, and resets a simulated battery state.

Output: PASS / FAIL / SKIP per check with the key values only. Nothing here answers a dialog or unlocks the device.

zh-CN: 3-Chime Alarm 1.3.0 `script` 关闭挑战 (路线图 P9.2, T284, 协议 chime-p9-protocol.md 第 4 节) 的单机端到端联调: 每个退回条件各触发一次,
并经宿主的 `scriptStatus` 应答 (宿主 T312 - T314) 跟踪正常路径 (脚本关闭本轮). 插件须为 debug 包 (经 RingDebugReceiver 读取并结束响铃).
每个用例是一个静音的一次性闹钟 (标签以钩子的探针前缀开头, 标记 `p92`, 不振动, 不贪睡, 1 分钟后自动停止), 由宿主脚本单独布置在 `lead` 秒后,
之后用钩子的 `clear` 结束 (退回 `none` 的一轮由用户关闭, 其余由删除结束, T66). 每个用例检查插件的 ring 应答行, `scriptStatus` 行, 退回行
`script challenge <occurrence> fell back to <fallback>: <reason>` 及其相对响铃开始的延迟, 以及钩子读出的生效挑战.
cases 需要带 `scriptStatus` 应答的宿主 (含宿主提交 e323d560d5): normal (绑定脚本 8 秒后调用 `alarms.dismiss`), listener (实时监听者
3 秒后关闭), listenergone (实时监听者 3 秒后离开, `noScript`), ended (一个被删的绑定脚本加一个 7 秒后结束的, 轮询的 `failed` 计入缺失的
脚本, T314), launchfailed (唯一的绑定脚本被删), noscript (既无绑定也无监听). hoststop: 响铃前强行停止宿主, ring 无应答, 立即
`unavailable`. oldhost: 不认识 `scriptStatus` 的旧宿主, 应答后约 5 秒 `unavailable` (T312). sample: 宿主样例 "拔下充电器才能关闭"
端到端, 充电中由响铃启动的样例等待, 模拟拔线 (`dumpsys battery unplug`) 后由样例关闭, 之后 `dumpsys battery reset`; 真实拔线列入手测.
locked: 布置后重启设备, 首次解锁前响铃立即退回 (`locked`), 设备保持锁定, 由用户解锁后运行 `cleanup`. 输出只有每项的 PASS / FAIL / SKIP
与关键数值; 不代为点按对话框, 也不解锁设备.
"""
import argparse
import json
import re
import sys
import tempfile
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import device_before_ring as br  # noqa: E402
import device_p5 as p5  # noqa: E402
import device_smoke as smoke  # noqa: E402

HOST = smoke.HOST
REMOTE = "/sdcard/Download/autojs6-chime-p92"
TAG = "p92"
# RingDebugReceiver.LABEL_PREFIX: the hook's `clear` ends and removes these alarms. zh-CN: 钩子的 `clear` 结束并删除这些闹钟.
LABEL = "p24-probe-p92"
SAMPLE_TAG = "sample-unplug-to-dismiss"
SAMPLE = Path(__file__).resolve().parents[2] / "AutoJs6" / "app" / "src" / "main" / "assets-app" / "sample" / "3-Chime Alarm" / "拔下充电器才能关闭 [v6.8.1+].js"
LOG_TAGS = ["ChimeTrigger", "ChimeRing"]
STATE = Path(tempfile.gettempdir()) / "chime-p92-locked-{serial}.json"

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
function byTag(tag) { return arr(alarms.list()).filter(function (x) { return String(x.tag || '') === tag; }); }
function removeTag(tag) {
    var removed = 0;
    byTag(tag).forEach(function (x) { try { if (alarms.remove(x.id)) removed++; } catch (e) { /* gone */ } });
    return removed;
}
function save() {
    files.write(DIR + '/result.json', JSON.stringify({ mode: cfg.mode, seconds: Math.round((Date.now() - started) / 1000), checks: checks, values: values }));
    files.write(DIR + '/done', '1');
}
function wholeSecond(t) { return Math.ceil(t / 1000) * 1000; }
function pad(n) { return (n < 10 ? '0' : '') + n; }
function options(c, at) {
    var dismiss = { challenge: 'script' };
    if (c.fallback) dismiss.script = { fallback: c.fallback };
    return { at: at, label: cfg.label + '-' + c.key, tag: cfg.tag, sound: { source: 'silent', volume: 0 }, vibrate: false, snooze: false, autoSilenceMinutes: 1, dismiss: dismiss };
}

function arm(c) {
    var removed = removeTag(cfg.tag);
    if (removed) record('leftover p92 alarms removed', 'PASS', removed);
    var a = null;
    check('arm ' + c.key, function () {
        var at = wholeSecond(Date.now() + cfg.lead * 1000);
        a = alarms.add(options(c, at));
        (c.binds || []).forEach(function (b) { a.bind(DIR + '/' + b.file, { events: b.events }); });
        (c.remove || []).forEach(function (f) { files.remove(DIR + '/' + f); });
        values.id = a.id;
        values.at = at;
        var d = a.dismiss;
        assert(d && d.challenge === 'script', 'dismiss ' + JSON.stringify(d));
        return a.id + ' in ' + Math.round((at - Date.now()) / 1000) + ' s, dismiss ' + JSON.stringify(d) + ', ' + arr(alarms.bindings(a.id)).length + ' binding(s)'
            + ((c.remove || []).length ? ', deleted ' + c.remove.join(' ') : '');
    });
    return a;
}

function modeArm() {
    arm(cfg.case);
    save();
}

/* A live listener: dismisses the round or leaves it [delay] ms after the ring. zh-CN: 实时监听者: 响铃 [delay] 毫秒后关闭本轮或离开. */
function modeListen() {
    var c = cfg.case;
    var a = arm(c);
    if (!a) { save(); return; }
    var mine = {};
    files.write(DIR + '/armed', JSON.stringify({ id: a.id, at: values.at }));
    alarms.on('ring', function (e) {
        if (e.alarmId !== a.id || mine.ring) return;
        var r = arr(alarms.ringing()).filter(function (x) { return x.id === a.id; })[0];
        mine.ring = { challenge: e.challenge === undefined ? 'undefined' : e.challenge, ringing: r ? r.challenge : 'absent', at: Date.now() };
        setTimeout(function () {
            if (c.act === 'dismiss') {
                try { mine.dismissed = alarms.dismiss(a.id); } catch (err) { mine.error = describe(err); }
            } else {
                mine.left = Date.now();
                alarms.off();
            }
        }, c.delay);
    });
    alarms.on('dismiss', function (e) {
        if (e.alarmId === a.id) mine.dismiss = { by: e.by === undefined ? 'undefined' : e.by, challenge: e.challenge === undefined ? 'undefined' : e.challenge };
    });
    var deadline = values.at + 60e3;
    var timer = setInterval(function () {
        var finished = mine.left || (mine.ring && (mine.dismiss || mine.error));
        if (!finished && Date.now() < deadline) return;
        clearInterval(timer);
        alarms.off();
        values.listener = mine;
        save();
    }, 500);
}

/* Runs the host sample directly (it creates its alarm), then moves the alarm to ring soon. zh-CN: 直接运行宿主样例 (建闹钟), 再把闹钟挪到稍后响. */
function modeSample() {
    var removed = removeTag(cfg.sampleTag);
    if (removed) record('leftover sample alarms removed', 'PASS', removed);
    var alarm = null;
    check('sample run directly creates its alarm', function () {
        engines.execScriptFile(DIR + '/unplug.js');
        var end = Date.now() + 20e3;
        while (byTag(cfg.sampleTag).length === 0 && Date.now() < end) sleep(500);
        var found = byTag(cfg.sampleTag);
        assert(found.length === 1, found.length + ' alarms with tag ' + cfg.sampleTag);
        alarm = found[0];
        var d = alarm.dismiss;
        assert(d && d.challenge === 'script' && d.script && d.script.fallback === 'math' && d.allowSnooze === false, 'dismiss ' + JSON.stringify(d));
        var b = arr(alarms.bindings(alarm.id));
        assert(b.length === 1 && String(b[0].script).indexOf('unplug.js') >= 0, 'bindings ' + JSON.stringify(b));
        return alarm.id + ' ' + alarm.time + ' daily, dismiss ' + JSON.stringify(d) + ', bound to ' + b[0].script;
    });
    if (alarm) {
        check('sample alarm moved to ring soon', function () {
            var at = wholeSecond(Date.now() + cfg.lead * 1000);
            var t = new Date(at);
            var time = pad(t.getHours()) + ':' + pad(t.getMinutes()) + ':' + pad(t.getSeconds());
            var moved = alarms.update(alarm.id, { time: time });
            alarms.get(alarm.id).bind(DIR + '/rec.js', { events: ['dismiss'] });
            values.id = alarm.id;
            values.at = moved.nextTrigger;
            assert(moved.nextTrigger === at, 'nextTrigger ' + moved.nextTrigger + ' != ' + at);
            return time + ', in ' + Math.round((at - Date.now()) / 1000) + ' s, ' + arr(alarms.bindings(alarm.id)).length + ' bindings';
        });
    }
    save();
}

function modeCleanup() {
    values.removed = removeTag(cfg.tag) + removeTag(cfg.sampleTag);
    save();
}

try {
    if (cfg.mode === 'arm') modeArm();
    else if (cfg.mode === 'listen') modeListen();
    else if (cfg.mode === 'sample') modeSample();
    else modeCleanup();
} catch (e) {
    record('script', 'FAIL', describe(e));
    save();
}
"""

# The bound `ring` script of `normal`: reads the challenge, waits, dismisses. zh-CN: normal 的绑定脚本: 读挑战, 等待, 关闭.
WAIT_DISMISS = r"""
let e = alarms.currentEvent();
let r = alarms.ringing().filter((x) => x.id === e.alarmId)[0];
let out = { type: String(e.type), challenge: e.challenge === undefined ? 'undefined' : e.challenge, ringing: r ? r.challenge : 'absent', started: Date.now() };
sleep(@WAIT@);
try { out.dismissed = alarms.dismiss(e.alarmId); } catch (err) { out.error = String(err); }
out.at = Date.now();
files.write('@DIR@/ran-@KEY@.json', JSON.stringify(out));
"""

# A bound `ring` script that ends without dismissing. zh-CN: 不关闭就结束的绑定脚本.
BRIEF = r"""
files.write('@DIR@/ran-@KEY@.json', JSON.stringify({ started: Date.now() }));
sleep(@WAIT@);
"""

RECORDER = r"""
let e = alarms.currentEvent();
function text(v) { return v === undefined || v === null ? null : String(v); }
files.append('@DIR@/events.txt', JSON.stringify({ type: text(e.type), alarmId: text(e.alarmId), occurrence: text(e.occurrence),
    by: text(e.by), challenge: text(e.challenge), at: Date.now() }) + '\n');
"""

GONE = "files.write('@DIR@/gone-ran', '1');\n"

# key -> the case: fallback (None: the default `none`), bindings, files deleted after binding, live listener act,
# expected fallback reason (None: a script dismisses), ring answer, poll answers in order, fallback delay window (s).
CASES = {
    "normal": {"fallback": "math", "binds": [("wait.js", ["ring"]), ("rec.js", ["dismiss"])], "remove": [], "reason": None,
               "answer": "launched=1 duplicates=0 missing=0", "polls": ["tracked=true running=1 started=1 failed=0"]},
    "listener": {"fallback": "math", "binds": [], "remove": [], "act": "dismiss", "reason": None, "answer": None, "polls": []},
    "listenergone": {"fallback": "shake", "binds": [], "remove": [], "act": "leave", "reason": "noScript", "answer": None, "polls": [],
                     "window": (3, 8)},
    "ended": {"fallback": "math", "binds": [("gone.js", ["ring"]), ("brief.js", ["ring"])], "remove": ["gone.js"], "reason": "ended",
              "answer": "launched=1 duplicates=0 missing=1",
              "polls": ["tracked=true running=1 started=1 failed=1", "tracked=true running=0 started=1 failed=1"], "window": (8, 14)},
    "launchfailed": {"fallback": None, "binds": [("gone.js", ["ring"])], "remove": ["gone.js"], "reason": "launchFailed",
                     "answer": "launched=0 duplicates=0 missing=1", "polls": [], "window": (0, 3)},
    "noscript": {"fallback": "type", "binds": [], "remove": [], "reason": "noScript", "answer": None, "polls": [], "window": (0, 2)},
}
HOSTSTOP = {"fallback": None, "binds": [("brief.js", ["ring"])], "remove": [], "reason": "unavailable", "answer": "scriptSkipped=",
            "polls": [], "window": (0, 3)}
OLDHOST = {"fallback": None, "binds": [("long.js", ["ring"])], "remove": [], "reason": "unavailable", "answer": "launched=1 duplicates=0 missing=0",
           "polls": ["no answer"], "window": (4, 9)}
LOCKED = {"fallback": None, "binds": [("brief.js", ["ring"])], "remove": [], "reason": "locked", "answer": None, "polls": [], "window": (0, 2)}


def now_ms():
    return int(time.time() * 1000)


def cat(serial, path):
    return p5.adb(serial, "shell", f"cat {path} 2>/dev/null").strip()


def push(serial, texts, fresh):
    if fresh:
        p5.adb(serial, "shell", "rm", "-rf", REMOTE)
    else:
        p5.adb(serial, "shell", f"rm -f {REMOTE}/done {REMOTE}/result.json {REMOTE}/armed")
    p5.adb(serial, "shell", "mkdir", "-p", REMOTE)
    folder = Path(tempfile.mkdtemp(prefix="chime-p92-"))
    try:
        for name, text in texts.items():
            local = folder / name
            local.write_text(text, encoding="utf-8", newline="")
            p5.adb(serial, "push", str(local), f"{REMOTE}/{name}")
            local.unlink()
    finally:
        folder.rmdir()


def case_files(key, spec):
    texts = {}
    for file, _ in spec["binds"]:
        source = {"wait.js": WAIT_DISMISS, "brief.js": BRIEF, "long.js": BRIEF, "rec.js": RECORDER, "gone.js": GONE}[file]
        wait = {"wait.js": 8000, "brief.js": 7000, "long.js": 20000}.get(file, 0)
        texts[file] = source.replace("@DIR@", REMOTE).replace("@KEY@", key).replace("@WAIT@", str(wait))
    return texts


def start(serial, name="p92.js"):
    p5.adb(serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP")
    cold = not p5.adb(serial, "shell", "pidof", HOST).strip()
    p5.adb(serial, "shell", "am", "start", "-n", smoke.launcher(serial))
    # A cold start (after a force stop) may drop a run request sent too early. zh-CN: 冷启动 (强行停止之后) 时过早发出的运行请求可能丢失.
    time.sleep(6 if cold else 2)
    p5.adb(serial, "shell", "am", "start", "-a", "android.intent.action.VIEW", "-t", "application/x-javascript",
           "-d", f"file://{REMOTE}/{name}", "-n", f"{HOST}/org.autojs.autojs.external.open.RunIntentActivity")


def config_text(mode, **extra):
    return json.dumps({"mode": mode, "tag": TAG, "label": LABEL, "sampleTag": SAMPLE_TAG, **extra})


def results(serial, report, mode, timeout):
    deadline = time.time() + timeout
    while time.time() < deadline and cat(serial, f"{REMOTE}/done") != "1":
        time.sleep(2)
    raw = cat(serial, f"{REMOTE}/result.json")
    if not raw.startswith("{"):
        report.line(mode, "FAIL", f"no result in {timeout} s (unlock the device; AutoJs6 needs all-files access)")
        return None
    result = json.loads(raw)
    for name, state, detail in result["checks"]:
        report.line(name, state, detail)
    return result.get("values") or {}


def run_main(serial, report, mode, extra_texts=None, fresh=False, timeout=120, **config):
    texts = {"p92.js": MAIN.replace("@DIR@", REMOTE), "config.json": config_text(mode, **config), **(extra_texts or {})}
    push(serial, texts, fresh)
    start(serial)
    return results(serial, report, mode, timeout)


def lines_since(serial, since):
    return p5.logs(serial, LOG_TAGS, since)


def wait_for(serial, since, predicate, timeout):
    """The first log line (time, tag, message) since [since] that [predicate] accepts, or None after [timeout] s."""
    deadline = time.time() + timeout
    while True:
        for line in lines_since(serial, since):
            if predicate(line[1], line[2]):
                return line
        if time.time() >= deadline:
            return None
        time.sleep(1)


def follow(serial, report, key, spec, alarm_id, since, timeout=40):
    """Follows the round of [alarm_id]: the ring session, the ring answer, the polls, the fallback or its absence. Answers
    a dict with `occurrence`, `ring` (s) and `fallback` (s or None)."""
    ring = wait_for(serial, since, lambda tag, m: tag == "ChimeRing" and m.startswith(f"ring session {alarm_id} occurrence="), timeout)
    if ring is None:
        report.line(f"{key}: rings", "FAIL", "no ring session")
        return None
    occurrence = re.search(r"occurrence=(\S+)", ring[2]).group(1)
    report.line(f"{key}: rings", "PASS", f"occurrence {occurrence}")
    out = {"occurrence": occurrence, "ring": ring[0], "fallback": None}
    fell = f"script challenge {occurrence} fell back to "
    window = spec.get("window")
    if spec["reason"] is not None:
        found = wait_for(serial, since, lambda tag, m: tag == "ChimeTrigger" and m.startswith(fell), (window[1] + 6) if window else 20)
        if found is None:
            report.line(f"{key}: falls back ({spec['reason']})", "FAIL", "no fallback line")
        else:
            delay = found[0] - ring[0]
            fallback, reason = found[2][len(fell):].split(": ", 1)
            expected_fallback = spec["fallback"] or "none"
            ok = reason == spec["reason"] and fallback == expected_fallback and window[0] - 0.5 <= delay <= window[1]
            report.line(f"{key}: falls back to {expected_fallback} ({spec['reason']})", "PASS" if ok else "FAIL",
                        f"{fallback}: {reason}, {delay:.1f} s after the ring (window {window[0]} - {window[1]} s)")
            out["fallback"] = found[0]
    elif spec["polls"]:
        # Without a fallback nothing else waits for the first poll, 5 s after the ring answer. zh-CN: 不退回时没有别的步骤等首次轮询 (应答后 5 秒).
        polled = f"scriptStatus {occurrence} -> "
        wait_for(serial, since, lambda tag, m: tag == "ChimeTrigger" and m.startswith(polled), 12)
    lines = lines_since(serial, since)
    sent = [m for _, tag, m in lines if tag == "ChimeTrigger" and m.startswith(f"script challenge {occurrence}: ring to ")]
    hosts = sent[0].rsplit("ring to ", 1)[-1] if sent else "-"
    want_hosts = "0 hosts" if not any("ring" in events for _, events in spec["binds"]) else "1 hosts"
    report.line(f"{key}: ring sent ({want_hosts})", "PASS" if hosts == want_hosts else "FAIL", f"ring to {hosts}")
    answers = [m.split(": ", 1)[-1] for _, tag, m in lines if tag == "ChimeTrigger" and m.startswith(f"ring {alarm_id} -> ")]
    if spec["answer"] is None:
        report.line(f"{key}: no host asked", "PASS" if not answers else "FAIL", "; ".join(answers) or "no ring answer line")
    else:
        ok = bool(answers) and answers[0].startswith(spec["answer"])
        report.line(f"{key}: ring answer {spec['answer']}", "PASS" if ok else "FAIL", answers[0] if answers else "no ring answer line")
    polls = [m.split(": ", 1)[-1] for _, tag, m in lines if tag == "ChimeTrigger" and m.startswith(f"scriptStatus {occurrence} -> ")]
    if spec["polls"] or polls:
        ok = polls[:len(spec["polls"])] == spec["polls"] and len(polls) == len(spec["polls"])
        report.line(f"{key}: scriptStatus answers", "PASS" if ok else "FAIL", " | ".join(polls) or "no scriptStatus line")
    return out


def close(serial, report, key, alarm_id, since):
    if p5.hook(serial, "state").get("sounding") == alarm_id:
        p5.hook(serial, "clear")
    ended = wait_for(serial, since, lambda tag, m: tag == "ChimeRing" and m == f"ring session {alarm_id} ended", 15)
    report.line(f"{key}: round ended", "PASS" if ended else "FAIL", "" if ended else "no `ring session ... ended` line")


def state_challenge(serial, report, key, alarm_id, expected):
    state = p5.hook(serial, "state")
    ok = state.get("sounding") == alarm_id and state.get("challenge") == expected
    report.line(f"{key}: challenge in effect {expected}", "PASS" if ok else "FAIL", f"sounding={state.get('sounding')} challenge={state.get('challenge')}")


def events(serial):
    return [json.loads(line) for line in cat(serial, f"{REMOTE}/events.txt").splitlines() if line.startswith("{")]


def script_dismissed(serial, report, key, alarm_id, since):
    ended = wait_for(serial, since, lambda tag, m: tag == "ChimeRing" and m == f"ring session {alarm_id} ended", 15)
    report.line(f"{key}: the script dismissed the round", "PASS" if ended else "FAIL", "" if ended else "still ringing")
    deadline = time.time() + 15
    mine = []
    while time.time() < deadline and not mine:
        mine = [e for e in events(serial) if e.get("alarmId") == alarm_id and e.get("type") == "dismiss"]
        time.sleep(1)
    ok = len(mine) == 1 and mine[0].get("by") == "script" and mine[0].get("challenge") == "script"
    report.line(f"{key}: dismiss event by script, challenge script", "PASS" if ok else "FAIL",
                f"by {mine[0].get('by')}, challenge {mine[0].get('challenge')}" if mine else "no dismiss event recorded")
    if not ended:
        close(serial, report, key, alarm_id, since)


def run_case(serial, args, report, key, spec, before_ring=None):
    since = p5.device_ms(serial)
    print(f"  {key}:", flush=True)
    case = {"key": key, "fallback": spec["fallback"], "binds": [{"file": f, "events": e} for f, e in spec["binds"]], "remove": spec["remove"],
            "act": spec.get("act"), "delay": 3000}
    texts = case_files(key, spec)
    if spec.get("act"):
        push(serial, {"p92.js": MAIN.replace("@DIR@", REMOTE), "config.json": config_text("listen", lead=args.lead, case=case), **texts}, fresh=True)
        start(serial)
        deadline = time.time() + 40
        armed = ""
        while time.time() < deadline and not armed.startswith("{") and cat(serial, f"{REMOTE}/done") != "1":
            armed = cat(serial, f"{REMOTE}/armed")
            time.sleep(1)
        values = json.loads(armed) if armed.startswith("{") else results(serial, report, "listen", 5)
    else:
        values = run_main(serial, report, "arm", texts, fresh=True, lead=args.lead, case=case)
    alarm_id = (values or {}).get("id")
    if not alarm_id:
        if spec.get("act"):
            report.line(f"arm {key}", "FAIL", "the listener did not arm")
        return
    if before_ring:
        before_ring(values["at"])
    if key == "normal" or key == "listener":
        ring = wait_for(serial, since, lambda tag, m: tag == "ChimeRing" and m.startswith(f"ring session {alarm_id} occurrence="), args.lead + 40)
        if ring:
            time.sleep(1.5)
            state_challenge(serial, report, key, alarm_id, "script")
    out = follow(serial, report, key, spec, alarm_id, since, timeout=args.lead + 40)
    if out is None:
        close(serial, report, key, alarm_id, since)
        return
    if spec["reason"] is None:
        fell = [m for _, tag, m in lines_since(serial, since) if tag == "ChimeTrigger" and m.startswith(f"script challenge {out['occurrence']} ")
                and ("fell back" in m)]
        report.line(f"{key}: no fallback", "PASS" if not fell else "FAIL", "; ".join(fell))
        if key == "normal":
            script_dismissed(serial, report, key, alarm_id, since)
            mark = cat(serial, f"{REMOTE}/ran-normal.json")
            mark = json.loads(mark) if mark.startswith("{") else {}
            ok = mark.get("type") == "ring" and mark.get("challenge") == "script" and mark.get("ringing") == "script" and mark.get("dismissed") is True
            report.line(f"{key}: the bound script saw challenge script and dismissed", "PASS" if ok else "FAIL",
                        f"event {mark.get('challenge')}, ringing {mark.get('ringing')}, dismiss returned {mark.get('dismissed')}"
                        + (f", {mark['at'] - mark['started']} ms after its start" if mark.get("at") else "") + (f", {mark.get('error')}" if mark.get("error") else ""))
        else:
            values = results(serial, report, "listen", 60) or {}
            got = values.get("listener") or {}
            ring, dismiss = got.get("ring") or {}, got.get("dismiss") or {}
            ok = ring.get("challenge") == "script" and ring.get("ringing") == "script" and got.get("dismissed") is True \
                and dismiss.get("by") == "script" and dismiss.get("challenge") == "script"
            report.line(f"{key}: the listener dismissed, dismiss by script, challenge script", "PASS" if ok else "FAIL",
                        f"ring {ring.get('challenge')}, ringing {ring.get('ringing')}, dismiss returned {got.get('dismissed')}, "
                        f"event by {dismiss.get('by')} challenge {dismiss.get('challenge')}" + (f", {got.get('error')}" if got.get("error") else ""))
            ended = wait_for(serial, since, lambda tag, m: tag == "ChimeRing" and m == f"ring session {alarm_id} ended", 10)
            report.line(f"{key}: round ended", "PASS" if ended else "FAIL", "")
            if not ended:
                close(serial, report, key, alarm_id, since)
        return
    if out["fallback"] is not None:
        state_challenge(serial, report, key, alarm_id, spec["fallback"] or "none")
    if spec.get("act") == "leave":
        got = (results(serial, report, "listen", 30) or {}).get("listener") or {}
        if got.get("left") and out["fallback"] is not None:
            report.line(f"{key}: fallback after the listener left", "PASS" if out["fallback"] * 1000 >= got["left"] - 200 else "FAIL",
                        f"{out['fallback'] * 1000 - got['left']:.0f} ms after it left; ring challenge {(got.get('ring') or {}).get('challenge')}")
    close(serial, report, key, alarm_id, since)


def cases(serial, args, report):
    keys = [k for k in CASES if not args.only or k in args.only.split(",")]
    for key in keys:
        run_case(serial, args, report, key, CASES[key])
    # The probes dismissed by a script stay as disabled one-shot alarms; the hook removes every probe. zh-CN: 由脚本关闭的探针留下已禁用的一次性闹钟, 钩子删除全部探针.
    p5.hook(serial, "clear")
    p5.adb(serial, "shell", "rm", "-rf", REMOTE)


def hoststop(serial, args, report):
    accessibility = br.saved_accessibility(serial)

    def stop(at):
        p5.adb(serial, "shell", "input", "keyevent", "KEYCODE_HOME")
        time.sleep(2)
        p5.adb(serial, "shell", "am", "force-stop", HOST)
        stopped = not p5.adb(serial, "shell", "pidof", HOST).strip()
        report.line("host force-stopped before the ring", "PASS" if stopped else "FAIL", f"{(at - p5.device_ms(serial)) / 1000:.0f} s before it")

    try:
        run_case(serial, args, report, "hoststop", HOSTSTOP, before_ring=stop)
        report.line("host still stopped", "PASS" if not p5.adb(serial, "shell", "pidof", HOST).strip() else "FAIL", "")
        ran = cat(serial, f"{REMOTE}/ran-hoststop.json")
        report.line("hoststop: the bound script never ran", "PASS" if not ran else "FAIL", "no marker" if not ran else "marker written")
    finally:
        br.restore_accessibility(serial, report, accessibility)
        p5.hook(serial, "clear")
        p5.adb(serial, "shell", "rm", "-rf", REMOTE)


def oldhost(serial, args, report):
    host = smoke.installed_version(serial, HOST)
    updated = re.search(r"lastUpdateTime=(.+)", p5.adb(serial, "shell", "dumpsys", "package", HOST))
    print(f"    INFO host versionCode {host}, lastUpdateTime {updated.group(1).strip() if updated else '-'}", flush=True)
    since = p5.device_ms(serial)
    run_case(serial, args, report, "oldhost", OLDHOST)
    lines = lines_since(serial, since)
    answer = [t for t, tag, m in lines if tag == "ChimeTrigger" and m.startswith("ring ") and "launched=1" in m]
    fell = [t for t, tag, m in lines if tag == "ChimeTrigger" and "fell back to none: unavailable" in m]
    if answer and fell:
        delay = fell[0] - answer[0]
        report.line("oldhost: fallback about 5 s after the ring answer", "PASS" if 4 <= delay <= 8 else "FAIL", f"{delay:.1f} s")
    time.sleep(15)
    p5.hook(serial, "clear")
    p5.adb(serial, "shell", "rm", "-rf", REMOTE)


def battery(serial):
    text = p5.adb(serial, "shell", "dumpsys", "battery")
    status = re.search(r"status: (\d+)", text)
    powered = [k for k in ("AC", "USB", "Wireless") if re.search(rf"{k} powered: true", text)]
    return (int(status.group(1)) if status else None), powered


def sample(serial, args, report):
    path = Path(args.sample) if args.sample else SAMPLE
    if not path.exists():
        report.line("sample file", "FAIL", f"{path} missing (pass --sample)")
        return
    status, powered = battery(serial)
    report.line("device charging before the ring", "PASS" if status in (2, 5) else "FAIL", f"status {status}, powered {','.join(powered) or 'none'}")
    if status not in (2, 5):
        return
    since = p5.device_ms(serial)
    texts = {"unplug.js": path.read_text(encoding="utf-8"), "rec.js": RECORDER.replace("@DIR@", REMOTE)}
    print("  sample:", flush=True)
    values = run_main(serial, report, "sample", texts, fresh=True, lead=args.lead)
    alarm_id = (values or {}).get("id")
    if not alarm_id or not values.get("at"):
        return
    spec = {"fallback": "math", "binds": [("unplug.js", ["ring"])], "reason": None, "answer": "launched=1 duplicates=0 missing=0",
            "polls": ["tracked=true running=1 started=1 failed=0"]}
    try:
        ring = wait_for(serial, since, lambda tag, m: tag == "ChimeRing" and m.startswith(f"ring session {alarm_id} occurrence="), args.lead + 40)
        if ring is None:
            report.line("sample: rings", "FAIL", "no ring session")
            return
        time.sleep(7)
        state_challenge(serial, report, "sample", alarm_id, "script")
        occurrence = re.search(r"occurrence=(\S+)", ring[2]).group(1)
        polls = [m.split(": ", 1)[-1] for _, tag, m in lines_since(serial, since) if tag == "ChimeTrigger" and m.startswith(f"scriptStatus {occurrence} -> ")]
        report.line("sample: waiting while charging (scriptStatus)", "PASS" if polls[:1] == ["tracked=true running=1 started=1 failed=0"] else "FAIL",
                    " | ".join(polls) or "no scriptStatus line")
        p5.adb(serial, "shell", "dumpsys", "battery", "unplug")
        p5.adb(serial, "shell", "dumpsys", "battery", "set", "status", "3")
        unplugged = p5.device_ms(serial)
        report.line("unplug simulated", "PASS", "dumpsys battery unplug, status 3 (discharging)")
        out = follow(serial, report, "sample", spec, alarm_id, since, timeout=5)
        script_dismissed(serial, report, "sample", alarm_id, since)
        ended = wait_for(serial, since, lambda tag, m: tag == "ChimeRing" and m == f"ring session {alarm_id} ended", 1)
        if ended and out:
            report.line("sample: dismissed after the unplug", "PASS" if 0 <= ended[0] * 1000 - unplugged <= 6000 else "FAIL",
                        f"{ended[0] * 1000 - unplugged:.0f} ms after it (the sample checks every 2 s)")
    finally:
        p5.adb(serial, "shell", "dumpsys", "battery", "reset")
        status, powered = battery(serial)
        report.line("battery state reset", "PASS" if status in (2, 5) and powered else "FAIL", f"status {status}, powered {','.join(powered) or 'none'}")
        values = run_main(serial, report, "cleanup", fresh=False)
        report.line("sample alarm removed", "PASS" if values and values.get("removed") == 1 else "FAIL", f"{(values or {}).get('removed')} removed")
        p5.adb(serial, "shell", "rm", "-rf", REMOTE)


def user_state(serial):
    match = re.search(r"State: (RUNNING_\w+)", p5.adb(serial, "shell", "dumpsys", "user", timeout=60))
    return match.group(1) if match else "?"


def locked(serial, args, report):
    state_file = Path(str(STATE).format(serial=serial.replace(":", "_")))
    case = {"key": "locked", "fallback": None, "binds": [{"file": "brief.js", "events": ["ring"]}], "remove": [], "delay": 3000}
    print("  locked:", flush=True)
    values = run_main(serial, report, "arm", case_files("locked", LOCKED), fresh=True, lead=args.lead, case=case)
    if not values or not values.get("id"):
        return
    offset = p5.device_ms(serial) - now_ms()
    state_file.write_text(json.dumps({**values, "offset": offset}), encoding="utf-8")
    if args.no_reboot:
        print(f"    due at device time {values['at']} ({values['id']}); reboot now and keep it locked", flush=True)
        return
    p5.adb(serial, "reboot", timeout=60)
    time.sleep(10)
    p5.adb(serial, "wait-for-device", timeout=300)
    deadline = time.time() + 300
    while time.time() < deadline and p5.adb(serial, "shell", "getprop", "sys.boot_completed").strip() != "1":
        time.sleep(3)
    report.line("rebooted", "PASS" if time.time() < deadline else "FAIL", f"{(values['at'] - offset - now_ms()) / 1000:.0f} s before the alarm")
    report.line("user locked after the boot", "PASS" if user_state(serial) == "RUNNING_LOCKED" else "FAIL", user_state(serial))
    rest = values["at"] - offset - now_ms() + 15_000
    if rest > 0:
        time.sleep(rest / 1000)
    still = user_state(serial)
    report.line("still locked after the ring", "PASS" if still == "RUNNING_LOCKED" else "FAIL", still)
    follow(serial, report, "locked", LOCKED, values["id"], 0, timeout=60)
    print("    the device is locked: unlock it, then run `cleanup` (the alarm ends by itself after 1 minute)", flush=True)


def cleanup(serial, args, report):
    p5.adb(serial, "shell", "dumpsys", "battery", "reset")
    cleared = p5.hook(serial, "clear")
    values = run_main(serial, report, "cleanup", fresh=True)
    report.line("p92 and sample alarms removed", "PASS" if values is not None else "FAIL",
                f"{(values or {}).get('removed')} removed, {cleared.get('cleared')} probes cleared")
    p5.adb(serial, "shell", "rm", "-rf", REMOTE)
    state_file = Path(str(STATE).format(serial=serial.replace(":", "_")))
    if state_file.exists():
        state_file.unlink()


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("serial")
    parser.add_argument("command", choices=["cases", "hoststop", "oldhost", "sample", "locked", "cleanup"])
    parser.add_argument("--lead", type=int, default=None, help="seconds from arming to the ring (25; locked 240)")
    parser.add_argument("--only", default="", help="cases: a comma separated subset of " + ",".join(CASES))
    parser.add_argument("--sample", default="", help="sample: the path of the host sample")
    parser.add_argument("--no-reboot", action="store_true", help="locked: arm only, the caller reboots")
    args = parser.parse_args(argv)
    if args.lead is None:
        args.lead = 240 if args.command == "locked" else 25
    report = p5.Report()
    print(f"{smoke.label(args.serial)}: script challenge {args.command}", flush=True)
    if args.command != "cleanup" and not (p5.host_ready(args.serial, report) and br.debug_hook_ready(args.serial, report)):
        return 1
    volume = br.alarm_volume(args.serial)
    {"cases": cases, "hoststop": hoststop, "oldhost": oldhost, "sample": sample, "locked": locked, "cleanup": cleanup}[args.command](args.serial, args, report)
    if args.command != "locked":
        after = br.alarm_volume(args.serial)
        report.line("alarm stream volume as before", "PASS" if after == volume else "FAIL", f"{volume} -> {after}")
    print(f"{args.serial}: P9.2 {args.command} {'PASS' if report.failed == 0 else 'FAIL'} ({report.passed} passed, {report.failed} failed)", flush=True)
    return 0 if report.failed == 0 else 1


if __name__ == "__main__":
    sys.exit(main())

"""Time and system event cases of 3-Chime Alarm on one device (roadmap P5.3; the clock / time zone / permission cases
are device_schedule.py `run`). The host must be a build with the alarm contract (versionCode 6641 or later); the alarms
are silent and dismissed through the debug hook.

    py .python/device_events.py <serial> dst
    py .python/device_events.py <serial> replaced
    py .python/device_events.py <serial> doze
    py .python/device_events.py <serial> off          (adds the alarms and powers the device off: the maintainer powers it on)
    py .python/device_events.py <serial> offcheck     (after the power on, still locked)
    py .python/device_events.py <serial> offfinish    (after the unlock)
    py .python/device_events.py <serial> restore      (puts the clock and zone settings back after an interrupted run)

- dst: zone America/New_York with the automatic time and zone off. Spring forward: the clock at 2027-03-14 01:58 EST,
  a daily 02:30 alarm (a time the day skips) is due at 03:00 EDT, the first instant after the jump (roadmap T9), rings
  there and moves to 02:30 EDT the next day. Fall back: the clock at 2026-11-01 01:57 EDT, a daily 01:59 alarm rings at
  01:59 EDT; the clock moved to 01:57 EST (the repeated hour), it does not ring again at 01:59 EST and is due the next day.
  The real time (from this computer), the zone and the automatic settings are put back afterwards.
- replaced: a daily alarm 3 hours ahead; `adb install -r` of the same debug APK; MY_PACKAGE_REPLACED reached the plugin
  and the alarm clock entry is still at the same instant.
- doze: a silent one-shot alarm 90 s ahead, the screen off, the battery unplugged and `dumpsys deviceidle force-idle`;
  the alarm rings on time (`setAlarmClock` is not deferred); deviceidle and the battery are reset afterwards.
- off / offcheck / offfinish: alarm A 1 minute after the power off, alarm B 8 minutes after it, bound to a script. The
  device is powered on about 10 minutes after the power off: A is missed (`miss ... off`, one notification), B rings late
  inside the 5 minute window (roadmap T16, T59); before the first unlock the host does not start the script
  (ChimeTrigger `scriptSkipped=locked`) and does not start it after the unlock either.

Output: PASS / FAIL per check and the key values only. Nothing here answers a dialog or unlocks the device.
"""
import argparse
import json
import re
import sys
import tempfile
import time
from datetime import datetime, timezone, timedelta
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import device_p5 as p5  # noqa: E402
import device_smoke as smoke  # noqa: E402

PLUGIN = smoke.PLUGIN
NY = "America/New_York"


def adb(serial, *args, timeout=120):
    return smoke.adb(serial, *args, check=False, timeout=timeout)


def java_hash(text):
    """String.hashCode() of Java: the id of the plugin's missed notification for an alarm (AndroidAlarmSchedulePort)."""
    h = 0
    for ch in text:
        h = (31 * h + ord(ch)) & 0xFFFFFFFF
    return h - (1 << 32) if h >= 1 << 31 else h


def state_path(serial, name):
    return Path(tempfile.gettempdir()) / f"chime-events-{name}-{serial.replace(':', '_')}.json"


def utc(*parts):
    return int(datetime(*parts, tzinfo=timezone.utc).timestamp() * 1000)


def iso(millis):
    return datetime.fromtimestamp(millis / 1000, timezone.utc).strftime("%Y-%m-%d %H:%M:%S UTC") if millis else "none"


def settings(serial):
    return {
        "auto_time": adb(serial, "shell", "settings", "get", "global", "auto_time"),
        "auto_time_zone": adb(serial, "shell", "settings", "get", "global", "auto_time_zone"),
        "zone": adb(serial, "shell", "getprop", "persist.sys.timezone"),
    }


def restore(serial, saved, report):
    adb(serial, "shell", "cmd", "alarm", "set-time", str(int(time.time() * 1000)))
    adb(serial, "shell", "cmd", "alarm", "set-timezone", saved["zone"])
    adb(serial, "shell", "settings", "put", "global", "auto_time_zone", saved["auto_time_zone"])
    adb(serial, "shell", "settings", "put", "global", "auto_time", saved["auto_time"])
    time.sleep(2)
    now = settings(serial)
    skew = abs(p5.device_ms(serial) - int(time.time() * 1000))
    report.line("clock and zone settings restored", "PASS" if now == saved and skew < 5000 else "FAIL",
                f"zone {now['zone']}, auto_time {now['auto_time']}, auto_time_zone {now['auto_time_zone']}, skew {skew} ms")
    state_path(serial, "settings").unlink(missing_ok=True)


def ring_lines(serial, alarm, since):
    return [(t, m) for t, _, m in p5.logs(serial, ["ChimeSchedule"], since) if m.startswith(f"ring {alarm} ")]


def wait_ring(serial, alarm, since, until_device_ms, extra=20_000):
    deadline = time.time() + max(until_device_ms - p5.device_ms(serial), 0) / 1000 + extra / 1000
    while time.time() < deadline:
        found = ring_lines(serial, alarm, since)
        if found:
            return found
        time.sleep(2)
    return ring_lines(serial, alarm, since)


def add(serial, report, specs, tag):
    values = p5.run_mode(serial, "add", report, {"alarms": specs, "tag": tag}, timeout=150, keep=True)
    return (values or {}).get("alarms") or {}


def state(serial, report, tag, remove=False):
    values = p5.run_mode(serial, "state", report, {"tag": tag, "remove": remove}, timeout=150, keep=True, fresh=False)
    return values or {}


def dst(serial, args, report):
    saved = settings(serial)
    state_path(serial, "settings").write_text(json.dumps(saved), encoding="utf-8")
    try:
        adb(serial, "shell", "settings", "put", "global", "auto_time", "0")
        adb(serial, "shell", "settings", "put", "global", "auto_time_zone", "0")
        adb(serial, "shell", "cmd", "alarm", "set-timezone", NY)
        # Spring forward: 2027-03-14 02:00 EST -> 03:00 EDT (07:00 UTC). zh-CN: 春季拨快.
        adb(serial, "shell", "cmd", "alarm", "set-time", str(utc(2027, 3, 14, 6, 58, 0)))
        time.sleep(2)
        since = p5.device_ms(serial)
        made = add(serial, report, [{"key": "spring", "options": {"time": "02:30", "repeat": "FREQ=DAILY", "label": "p5-dst-spring", "sound": {"source": "silent"}, "vibrate": False, "snooze": False}}], "p5-dst")
        spring = made.get("spring") or {}
        jump = utc(2027, 3, 14, 7, 0, 0)
        report.line("spring: 02:30 that day is due at 03:00 EDT", "PASS" if spring.get("next") == jump else "FAIL", f"next {iso(spring.get('next'))}")
        rang = wait_ring(serial, spring.get("id"), since, jump)
        report.line("spring: rings after the jump", "PASS" if rang else "FAIL", rang[0][1].split(" ", 2)[-1] if rang else "no ring")
        p5.hook(serial, "dismiss", id=spring.get("id"))
        time.sleep(3)
        after = state(serial, report, "p5-dst", remove=True).get("alarms", {}).get("p5-dst-spring", {})
        report.line("spring: next is 02:30 EDT the day after", "PASS" if after.get("next") == utc(2027, 3, 15, 6, 30, 0) else "FAIL", f"next {iso(after.get('next'))}")
        # Fall back: 2026-11-01 02:00 EDT -> 01:00 EST; 01:59 happens at 05:59 and at 06:59 UTC. zh-CN: 秋季拨回.
        adb(serial, "shell", "cmd", "alarm", "set-time", str(utc(2026, 11, 1, 5, 57, 0)))
        time.sleep(2)
        since = p5.device_ms(serial)
        made = add(serial, report, [{"key": "fall", "options": {"time": "01:59", "repeat": "FREQ=DAILY", "label": "p5-dst-fall", "sound": {"source": "silent"}, "vibrate": False, "snooze": False}}], "p5-dst")
        fall = made.get("fall") or {}
        first = utc(2026, 11, 1, 5, 59, 0)
        report.line("fall: due at the first 01:59 (EDT)", "PASS" if fall.get("next") == first else "FAIL", f"next {iso(fall.get('next'))}")
        rang = wait_ring(serial, fall.get("id"), since, first)
        report.line("fall: rings at 01:59 EDT", "PASS" if rang else "FAIL", rang[0][1].split(" ", 2)[-1] if rang else "no ring")
        p5.hook(serial, "dismiss", id=fall.get("id"))
        time.sleep(3)
        adb(serial, "shell", "cmd", "alarm", "set-time", str(utc(2026, 11, 1, 6, 57, 0)))
        time.sleep(2)
        since = p5.device_ms(serial)
        again = wait_ring(serial, fall.get("id"), since, utc(2026, 11, 1, 6, 59, 0), extra=40_000)
        report.line("fall: no second ring at 01:59 EST", "PASS" if not again else "FAIL", "silent" if not again else again[0][1])
        after = state(serial, report, "p5-dst", remove=True).get("alarms", {}).get("p5-dst-fall", {})
        report.line("fall: next is 01:59 EST the day after", "PASS" if after.get("next") == utc(2026, 11, 2, 6, 59, 0) else "FAIL", f"next {iso(after.get('next'))}")
    finally:
        restore(serial, saved, report)
        p5.hook(serial, "clear")
        p5.run_mode(serial, "cleanup", report, {}, timeout=150)


def clock_entries(serial):
    text = adb(serial, "shell", "dumpsys", "alarm", timeout=60)
    return sorted(int(m) for m in re.findall(r"RTC_WAKEUP #\d+: Alarm\{\S+ type \d+ origWhen (\d+) whenElapsed \S+ " + re.escape(PLUGIN), text))


def replaced(serial, args, report):
    hours_ahead = datetime.now() + timedelta(hours=3)
    made = add(serial, report, [{"key": "r", "options": {"time": hours_ahead.strftime("%H:%M"), "repeat": "FREQ=DAILY", "label": "p5-replaced", "sound": {"source": "silent"}, "vibrate": False}}], "p5-replaced")
    alarm = made.get("r") or {}
    before = clock_entries(serial)
    report.line("entry before the update", "PASS" if alarm.get("next") in before else "FAIL", f"next {iso(alarm.get('next'))}, {len(before)} plugin RTC_WAKEUP entries")
    since = p5.device_ms(serial)
    ok, detail, _ = smoke.install(serial, smoke.pick_apk(args.apk))
    report.line("install -r of the same APK", "PASS" if ok else "FAIL", detail)
    deadline = time.time() + 30
    lines = []
    while time.time() < deadline and not lines:
        lines = [m for _, _, m in p5.logs(serial, ["ChimeSchedule"], since) if "MY_PACKAGE_REPLACED" in m]
        time.sleep(2)
    report.line("MY_PACKAGE_REPLACED reconciled", "PASS" if lines else "FAIL", lines[0][:90] if lines else "no line")
    after = clock_entries(serial)
    report.line("entry after the update", "PASS" if alarm.get("next") in after else "FAIL", f"{len(after)} plugin RTC_WAKEUP entries")
    state(serial, report, "p5-replaced", remove=True)


def doze(serial, args, report):
    # Deep idle is refused while an alarm clock is due within the next hour (DeviceIdleController MIN_TIME_TO_ALARM),
    # so the device goes idle first and the alarm is scheduled inside it. zh-CN: 一小时内有闹钟时系统不进深度 idle, 因此先进入 idle 再排闹钟.
    adb(serial, "shell", "input", "keyevent", "KEYCODE_SLEEP")
    adb(serial, "shell", "dumpsys", "battery", "unplug")
    time.sleep(5)
    try:
        idle = adb(serial, "shell", "dumpsys", "deviceidle", "force-idle")
        report.line("deviceidle force-idle", "PASS" if "deep idle mode" in idle else "FAIL", idle.splitlines()[0][:80] if idle else "")
        options = {"sound": {"source": "silent"}, "vibrate": False}
        answer = p5.hook(serial, "add", label="p24-probe-doze", inSeconds=90, options64=__import__("base64").b64encode(json.dumps(options).encode()).decode())
        if "added" not in answer:
            report.line("probe", "FAIL", str(answer)[:120])
            return
        at, alarm = int(answer["at"]), answer["added"]
        # Informational: the system leaves deep idle once an alarm clock is due within the hour. zh-CN: 仅记录.
        print(f"    INFO deep idle state after scheduling: {adb(serial, 'shell', 'dumpsys', 'deviceidle', 'get', 'deep')}", flush=True)
        since = p5.device_ms(serial)
        rang = wait_ring(serial, alarm, since, at, extra=60_000)
        late = re.search(r"late=(-?\d+)", rang[0][1]) if rang else None
        report.line("alarm clock scheduled in deep idle rings on time", "PASS" if late and int(late.group(1)) < 5000 else "FAIL", f"late {late.group(1)} ms" if late else "no ring in 60 s after due")
    finally:
        adb(serial, "shell", "dumpsys", "deviceidle", "unforce")
        adb(serial, "shell", "dumpsys", "battery", "reset")
        p5.hook(serial, "clear")
        adb(serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP")


def off(serial, args, report):
    made = add(serial, report, [
        {"key": "a", "options": {"inSeconds": 60 + 30, "label": "p5-off-miss", "sound": {"source": "silent"}, "vibrate": False, "snooze": False}},
        {"key": "b", "options": {"inSeconds": 8 * 60 + 30, "label": "p5-off-late", "sound": {"source": "silent"}, "vibrate": False, "snooze": False}, "bind": "./late.js"},
    ], "p5-off")
    if len(made) != 2:
        return
    off_at = p5.device_ms(serial)
    state_path(serial, "off").write_text(json.dumps({"a": made["a"], "b": made["b"], "off": off_at}), encoding="utf-8")
    adb(serial, "reboot", "-p")
    print(f"    powered off at {iso(off_at)}; A due {iso(made['a']['at'])}, B due {iso(made['b']['at'])}; power on at about {iso(off_at + 10 * 60_000)} (9 - 11 minutes after)", flush=True)


def offcheck(serial, args, report):
    saved = json.loads(state_path(serial, "off").read_text(encoding="utf-8"))
    a, b = saved["a"], saved["b"]
    lines = p5.logs(serial, ["ChimeSchedule", "ChimeRing", "ChimeTrigger"], 0)
    boot = [(t, m) for t, _, m in lines if ("BOOT_COMPLETED" in m or m.startswith("start after boot")) and "reconciled" in m]
    report.line("boot reconcile", "PASS" if boot else "FAIL", "; ".join(f"{m[:110]}" for _, m in boot[:2]))
    missed = any(f"miss {a['id']} off" in m for _, m in boot)
    report.line("A missed while off (miss off)", "PASS" if missed else "FAIL", f"{(boot[0][0] * 1000 - a['at']) / 60000:.1f} min after due" if boot else "")
    ring_a = [m for _, _, m in lines if m.startswith(f"ring {a['id']} ")]
    report.line("A does not ring", "PASS" if not ring_a else "FAIL", ring_a[0] if ring_a else "")
    ring_b = [m for _, _, m in lines if m.startswith(f"ring {b['id']} ")]
    report.line("B rings late inside the window", "PASS" if ring_b else "FAIL", ring_b[0].split(" ", 2)[-1] if ring_b else "no ring")
    trigger = [m for _, _, m in lines if m.startswith(f"ring {b['id']} -> ")]
    report.line("B before the first unlock: script skipped", "PASS" if trigger and "scriptSkipped=locked" in trigger[0] else "FAIL", trigger[0].split(": ", 1)[-1] if trigger else "no ChimeTrigger line")
    notes = adb(serial, "shell", "dumpsys", "notification", "--noredact")
    key = f"|{PLUGIN}|{java_hash(a['id'])}|missed|"
    report.line("missed notification for A", "PASS" if key in notes else "FAIL", "posted" if key in notes else "not in dumpsys notification")
    p5.hook(serial, "dismiss", id=b["id"])


def offfinish(serial, args, report):
    values = state(serial, report, "p5-off", remove=True)
    report.line("bound script not started after the unlock", "PASS" if not values.get("late") else "FAIL", f"late-ring {values.get('late')}, removed {values.get('removed')}")
    state_path(serial, "off").unlink(missing_ok=True)
    adb(serial, "shell", "rm", "-rf", p5.REMOTE)


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("serial")
    parser.add_argument("command", choices=["dst", "replaced", "doze", "off", "offcheck", "offfinish", "restore"])
    parser.add_argument("--apk", help="replaced: the APK installed over the plugin (default: the newest debug APK)")
    args = parser.parse_args(argv)
    report = p5.Report()
    print(f"{smoke.label(args.serial)}: {args.command}", flush=True)
    if args.command == "restore":
        path = state_path(args.serial, "settings")
        if path.exists():
            restore(args.serial, json.loads(path.read_text(encoding="utf-8")), report)
    elif args.command in ("offcheck",):
        offcheck(args.serial, args, report)
    elif p5.host_ready(args.serial, report):
        {"dst": dst, "replaced": replaced, "doze": doze, "off": off, "offfinish": offfinish}[args.command](args.serial, args, report)
    print(f"{args.serial}: P5.3 {args.command} {'PASS' if report.failed == 0 else 'FAIL'} ({report.passed} passed, {report.failed} failed)", flush=True)
    return 0 if report.failed == 0 else 1


if __name__ == "__main__":
    sys.exit(main())

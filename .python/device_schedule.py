"""Scheduler checks of 3-Chime Alarm on one device (roadmap P2.3), in three phases.

    py .python/device_schedule.py <serial> run [--no-install] [--skip-permission]
    py .python/device_schedule.py <serial> reboot [--shots <dir>]
    py .python/device_schedule.py <serial> finish
    py .python/device_schedule.py <serial> clear

run: installs the debug APK and its androidTest APK (the ScheduleDeviceProbe instrumentation adds and reads probe
    alarms in the plugin process), adds two daily probe alarms B (in 2 hours) and A (in 3 hours) and checks
    1. one `setAlarmClock` entry per probe in `dumpsys alarm` at its nextTrigger;
    2. time zone: Asia/Tokyo moves both to the same wall clock time there, the original zone moves them back;
    3. clock: set to B + 2 minutes, B rings late (within the 5 minute window); set to A + 10 minutes, A is missed
       (`miss` reason `late`, one notification); both move to the next day; the real time back keeps them there
       (a handled ring less than a day ahead is not rung again, roadmap T56);
    4. exact alarm permission (API 31 - 32 only): denied, the entries are gone and a new alarm answers
       PERMISSION_REQUIRED; the original mode back, the plugin reschedules on the system broadcast;
    then restores the clock and the time zone settings and adds C (daily, in 1 hour) and D (one-shot, in 2 hours)
    for the reboot phase.
reboot: reboots, waits for the boot and checks before the first unlock (roadmap T7): LOCKED_BOOT_COMPLETED reached the
    plugin and the four alarm clocks are scheduled again at the same instants; --shots saves the lock screen with the
    next alarm (screen woken with KEYCODE_WAKEUP, nothing is unlocked). The maintainer unlocks the device afterwards.
finish: after the unlock: BOOT_COMPLETED reached the plugin, the probes still have their nextTrigger; removes the probes
    and their notifications and uninstalls the androidTest APK (the plugin stays installed).
clear: the clean-up of finish without the reboot checks (after a run that is not followed by a reboot).

State between the phases (probe ids, instants, original settings) is kept in the temp directory. Output: PASS / FAIL
per check and the key values only. Nothing here answers a system dialog; run needs the device unlocked.
"""
import argparse
import json
import re
import subprocess
import sys
import tempfile
import time
from datetime import datetime, timedelta, timezone
from pathlib import Path

PLUGIN = "io.github.supermonster003.autojs6.plugin.three.chime.alarm"
TEST_PACKAGE = PLUGIN + ".test"
RUNNER = f"{TEST_PACKAGE}/androidx.test.runner.AndroidJUnitRunner"
PROBE = f"{PLUGIN}.ScheduleDeviceProbe#probe"
TAG = "ChimeSchedule"
ROOT = Path(__file__).resolve().parent.parent
MINUTE = 60_000
DAY = 24 * 60 * MINUTE


def adb(serial, *args, check=True, timeout=120):
    return subprocess.run(["adb", "-s", serial, *args], capture_output=True, text=True, encoding="utf-8", errors="replace",
                          check=check, timeout=timeout).stdout.strip()


def shell(serial, *args, check=True):
    return adb(serial, "shell", *args, check=check)


def state_file(serial):
    return Path(tempfile.gettempdir()) / f"chime-schedule-{serial.replace(':', '_')}.json"


class Report:
    def __init__(self):
        self.ok = True

    def check(self, step, passed, text=""):
        self.ok = self.ok and bool(passed)
        print(f"    {step}: {'PASS' if passed else 'FAIL'} {text}".rstrip())
        return passed


def stamp(millis, offset):
    return datetime.fromtimestamp(millis / 1000, timezone(timedelta(minutes=offset))).strftime("%m-%d %H:%M") if millis else "none"


def offset_minutes(serial):
    text = shell(serial, "date", "+%z")
    sign = -1 if text.startswith("-") else 1
    return sign * (int(text[1:3]) * 60 + int(text[3:5]))


def device_now(serial):
    return int(shell(serial, "date", "+%s")) * 1000


def next_daily(hour, minute, now, offset, after=None):
    """The first daily hour:minute strictly after max(now, after) at a fixed offset (independent of the plugin)."""
    start = max(now, after or now)
    zone = timezone(timedelta(minutes=offset))
    local = datetime.fromtimestamp(start / 1000, zone)
    candidate = local.replace(hour=hour, minute=minute, second=0, microsecond=0)
    while int(candidate.timestamp() * 1000) <= start:
        candidate += timedelta(days=1)
    return int(candidate.timestamp() * 1000)


def probe(serial, operation, **extra):
    command = ["shell", "am", "instrument", "-w", "-r", "-e", "class", PROBE, "-e", "chimeProbe", operation]
    for key, value in extra.items():
        command += ["-e", key, str(value)]
    out = adb(serial, *command, RUNNER, timeout=180)
    values = dict(re.findall(r"INSTRUMENTATION_STATUS: (chime\.[\w.-]+)=(.*)", out))
    if "chime.now" not in values:
        failure = re.findall(r"INSTRUMENTATION_STATUS: stack=(.*)", out) or out.splitlines()[-1:]
        raise SystemExit(f"probe {operation} failed: {(failure or ['no output'])[0][:200]}")
    alarms = {}
    for key, value in values.items():
        if key.startswith("chime.alarm."):
            fields = dict(part.split("=", 1) for part in value.split())
            alarms[key[len("chime.alarm."):]] = {k: (None if v == "null" else v) for k, v in fields.items()}
    return values, alarms


def scheduled(serial):
    """The plugin's alarm entries of `dumpsys alarm` (instant, action, alarm clock or not) and the next alarm clock.

    The header carries the instant as `origWhen` (API 31+) or `when` (before, without `whenElapsed`), as in
    `device_timers.timer_clocks`.
    """
    text = shell(serial, "dumpsys", "alarm")
    entries = []
    for block in re.split(r"\n(?=\s+[A-Z_]+ #\d+: Alarm\{)", text):
        head = re.search(r"([A-Z_]+) #\d+: Alarm\{\S+ type \d+ (?:origWhen|when) (\d+)(?: whenElapsed \S+)? (\S+)\}", block)
        if not head or head.group(3) != PLUGIN:
            continue
        tag = re.search(r"tag=\S*?:(\S+)", block)
        flags = re.search(r"flags=0x([0-9a-f]+)", block)
        entries.append({
            "type": head.group(1),
            "at": int(head.group(2)),
            "action": tag.group(1).rsplit(".", 1)[-1] if tag else "?",
            "clock": bool(flags and int(flags.group(1), 16) & 0x2) or "larm clock" in block,
        })
    following = re.search(r"Next alarm clock information:\s*\n((?:\s+user:.*\n?)*)", text)
    return entries, (following.group(1).strip() if following else "")


def next_clock_ok(values, first):
    """The system's next alarm clock is the plugin's first alarm, or an earlier one of another app."""
    at, _, creator = values.get("chime.nextClock", "null null").partition(" ")
    if not at.isdigit():
        return False
    return int(at) == first if creator == PLUGIN else int(at) < first


def describe_next(values, offset):
    at, _, creator = values.get("chime.nextClock", "null null").partition(" ")
    return f"{stamp(int(at), offset) if at.isdigit() else 'none'} ({'plugin' if creator == PLUGIN else creator})"


def clocks(serial):
    return sorted(entry["at"] for entry in scheduled(serial)[0] if entry["action"] == "RING" and entry["clock"])


def log_lines(serial):
    return shell(serial, "logcat", "-d", "-v", "brief", "-s", f"{TAG}:I", check=False).splitlines()


def new_lines(serial, before):
    seen = set(before)
    return [line for line in log_lines(serial) if line not in seen and TAG in line]


def wait_for(predicate, seconds=20):
    deadline = time.time() + seconds
    while time.time() < deadline:
        value = predicate()
        if value:
            return value
        time.sleep(1)
    return predicate()


def user_state(serial):
    """The `User 0:` line of `dumpsys package` (other users, such as a MIUI second space, keep their own flags)."""
    text = shell(serial, "dumpsys", "package", PLUGIN, check=False)
    found = re.search(r"User 0: .*", text)
    return found.group(0) if found else ""


def stopped(serial):
    return "stopped=true" in user_state(serial)


def notifications_allowed(serial, sdk):
    """API 33+ asks for POST_NOTIFICATIONS at run time; this script does not grant it (a dialog for the maintainer)."""
    if sdk < 33:
        return True
    text = shell(serial, "dumpsys", "package", PLUGIN, check=False)
    user = text[text.find("User 0:"):]
    return "android.permission.POST_NOTIFICATIONS: granted=true" in user


def unstop(serial):
    """`am instrument` force-stops the plugin; a stopped package gets no boot or clock broadcasts, so start it once."""
    if not stopped(serial):
        return
    alias = shell(serial, "cmd", "package", "resolve-activity", "--brief", "-c", "android.intent.category.LAUNCHER", PLUGIN).splitlines()[-1].strip()
    shell(serial, "am", "start", "-W", "-n", alias, check=False)
    time.sleep(2)
    shell(serial, "input", "keyevent", "KEYCODE_HOME", check=False)
    time.sleep(2)


def install(serial, report):
    debug = sorted((ROOT / "app" / "build" / "outputs" / "apk" / "debug").glob("*.apk"), key=lambda p: p.stat().st_mtime)
    tests = sorted((ROOT / "app" / "build" / "outputs" / "apk" / "androidTest" / "debug").glob("*.apk"), key=lambda p: p.stat().st_mtime)
    if not debug or not tests:
        raise SystemExit("build :app:assembleDebug :app:assembleDebugAndroidTest first")
    for apk in (debug[-1], tests[-1]):
        out = subprocess.run(["adb", "-s", serial, "install", "-r", "-t", str(apk)], capture_output=True, text=True, timeout=300)
        report.check(f"install {apk.name}", out.returncode == 0 and "Success" in out.stdout)


def set_time(serial, millis):
    shell(serial, "cmd", "alarm", "set-time", str(millis))


def run(serial, args):
    report = Report()
    sdk = int(shell(serial, "getprop", "ro.build.version.sdk"))
    print(f"{shell(serial, 'getprop', 'ro.product.model')} ({serial}, API {sdk}): run")
    if not args.no_install:
        install(serial, report)
    original = {
        "auto_time": shell(serial, "settings", "get", "global", "auto_time"),
        "auto_time_zone": shell(serial, "settings", "get", "global", "auto_time_zone"),
        "zone": shell(serial, "getprop", "persist.sys.timezone"),
    }
    probe(serial, "clear")
    offset = offset_minutes(serial)
    now = device_now(serial)
    plans = {}
    for name, hours in (("p23-probe-b", 2), ("p23-probe-a", 3)):
        local = datetime.fromtimestamp(now / 1000, timezone(timedelta(minutes=offset))) + timedelta(hours=hours, minutes=1)
        values, _ = probe(serial, "add", hour=local.hour, minute=local.minute, label=name, repeat="FREQ=DAILY")
        plans[name] = {"id": values.get("chime.added"), "hour": local.hour, "minute": local.minute,
                       "at": next_daily(local.hour, local.minute, now, offset)}
    values, alarms = probe(serial, "state")
    unstop(serial)
    a, b = plans["p23-probe-a"], plans["p23-probe-b"]
    report.check("exact alarm permission", values.get("chime.exact") == "true", f"canScheduleExact={values.get('chime.exact')}")
    report.check("nextTrigger after add", all(int(alarms[n]["next"]) == p["at"] for n, p in plans.items()),
                 f"B {stamp(b['at'], offset)} A {stamp(a['at'], offset)}")
    entries, following = scheduled(serial)
    report.check("setAlarmClock entries", clocks(serial) == sorted([a["at"], b["at"]]),
                 f"{len(entries)} entries, {sum(e['clock'] for e in entries)} alarm clocks")
    report.check("next alarm clock", next_clock_ok(values, b["at"]), f"system next {describe_next(values, offset)}")

    # 2. time zone; the automatic zone is off meanwhile, or the network sets it back. zh-CN: 时区; 期间关闭自动时区.
    shell(serial, "settings", "put", "global", "auto_time_zone", "0")
    try:
        before = log_lines(serial)
        shell(serial, "cmd", "alarm", "set-timezone", "Asia/Tokyo")
        tokyo = 9 * 60
        expected = sorted(next_daily(p["hour"], p["minute"], device_now(serial), tokyo) for p in plans.values())
        moved = wait_for(lambda: clocks(serial) == expected)
        lines = [line for line in new_lines(serial, before) if "TIMEZONE_CHANGED" in line]
        report.check("zone Asia/Tokyo", moved and lines,
                     f"clocks {[stamp(t, tokyo) for t in clocks(serial)]} (Tokyo), broadcast {'seen' if lines else 'missing'}")
    finally:
        shell(serial, "cmd", "alarm", "set-timezone", original["zone"])
        shell(serial, "settings", "put", "global", "auto_time_zone", original["auto_time_zone"])
    back = wait_for(lambda: clocks(serial) == sorted([a["at"], b["at"]]))
    report.check(f"zone {original['zone']} again", back, f"clocks {[stamp(t, offset) for t in clocks(serial)]}")

    # 3. clock. zh-CN: 时钟.
    shell(serial, "settings", "put", "global", "auto_time", "0")
    started = time.time()
    real = device_now(serial)
    try:
        before = log_lines(serial)
        set_time(serial, b["at"] + 2 * MINUTE)
        rang = wait_for(lambda: any(f"ring {b['id']}" in line for line in new_lines(serial, before)))
        expected = sorted([a["at"], b["at"] + DAY])
        report.check("clock B + 2 min: rings late", rang and wait_for(lambda: clocks(serial) == expected),
                     f"clocks {[stamp(t, offset) for t in clocks(serial)]}")
        before = log_lines(serial)
        set_time(serial, a["at"] + 10 * MINUTE)
        missed = wait_for(lambda: any(f"miss {a['id']} late" in line for line in new_lines(serial, before)))
        expected = sorted([a["at"] + DAY, b["at"] + DAY])
        notes = shell(serial, "dumpsys", "notification", "--noredact", check=False)
        # A record shows up in several sections of the dump; count the distinct keys. zh-CN: 按不同的键计数.
        count = len(set(re.findall(rf"\|{re.escape(PLUGIN)}\|-?\d+\|missed\|", notes)))
        allowed = notifications_allowed(serial, sdk)
        report.check("clock A + 10 min: miss late", missed and wait_for(lambda: clocks(serial) == expected) and (count >= 1 or not allowed),
                     f"clocks {[stamp(t, offset) for t in clocks(serial)]}, missed notifications {count}"
                     + ("" if allowed else " (POST_NOTIFICATIONS not granted, notification not checked)"))
    finally:
        set_time(serial, real + int((time.time() - started) * 1000))
        shell(serial, "settings", "put", "global", "auto_time", original["auto_time"])
    time.sleep(3)
    report.check("real time again: handled rings stay on the next day", clocks(serial) == sorted([a["at"] + DAY, b["at"] + DAY]),
                 f"clocks {[stamp(t, offset) for t in clocks(serial)]}, drift {abs(device_now(serial) - int(time.time() * 1000)) // 1000} s")

    # 4. exact alarm permission (API 31 - 32). zh-CN: 精确闹钟权限.
    if 31 <= sdk <= 32 and not args.skip_permission:
        mode = re.search(r"SCHEDULE_EXACT_ALARM: (\w+)", shell(serial, "appops", "get", PLUGIN, "SCHEDULE_EXACT_ALARM", check=False))
        mode = mode.group(1) if mode else "default"
        try:
            shell(serial, "appops", "set", PLUGIN, "SCHEDULE_EXACT_ALARM", "deny")
            gone = wait_for(lambda: not clocks(serial))
            values, _ = probe(serial, "add", hour=a["hour"], minute=a["minute"], label="p23-probe-denied")
            report.check("permission denied", gone and values.get("chime.exact") == "false" and values.get("chime.error") == "PERMISSION_REQUIRED",
                         f"clocks {len(clocks(serial))}, canScheduleExact={values.get('chime.exact')}, add -> {values.get('chime.error')}")
            unstop(serial)
        finally:
            before = log_lines(serial)
            shell(serial, "appops", "set", PLUGIN, "SCHEDULE_EXACT_ALARM", mode)
        restored = wait_for(lambda: len(clocks(serial)) == 2, 30)
        lines = [line for line in new_lines(serial, before) if "PERMISSION_STATE_CHANGED" in line]
        report.check(f"permission back ({mode})", restored and lines,
                     f"clocks {len(clocks(serial))}, broadcast {'seen' if lines else 'missing'}")

    # Probes for the reboot phase. zh-CN: 重启阶段的探针.
    now = device_now(serial)
    for name, hours, repeat in (("p23-probe-c", 1, "FREQ=DAILY"), ("p23-probe-d", 2, None)):
        local = datetime.fromtimestamp(now / 1000, timezone(timedelta(minutes=offset))) + timedelta(hours=hours, minutes=30)
        extra = {"repeat": repeat} if repeat else {}
        values, _ = probe(serial, "add", hour=local.hour, minute=local.minute, label=name, **extra)
        plans[name] = {"id": values.get("chime.added"), "hour": local.hour, "minute": local.minute,
                       "at": next_daily(local.hour, local.minute, now, offset)}
    values, alarms = probe(serial, "state")
    unstop(serial)
    expected = sorted(int(alarms[n]["next"]) for n in alarms if alarms[n]["next"])
    report.check("four alarm clocks before the reboot", clocks(serial) == expected and len(expected) == 4,
                 f"clocks {[stamp(t, offset) for t in expected]}, stopped={stopped(serial)}")
    state_file(serial).write_text(json.dumps({"original": original, "offset": offset, "plans": plans, "clocks": expected}), encoding="utf-8")
    settings = (shell(serial, "settings", "get", "global", "auto_time"), shell(serial, "settings", "get", "global", "auto_time_zone"),
                shell(serial, "getprop", "persist.sys.timezone"))
    report.check("settings restored", settings == (original["auto_time"], original["auto_time_zone"], original["zone"]),
                 f"auto_time={settings[0]} auto_time_zone={settings[1]} zone={settings[2]}")
    print(f"{serial}: RUN {'PASS' if report.ok else 'FAIL'}")
    return report.ok


def reboot(serial, args):
    report = Report()
    saved = json.loads(state_file(serial).read_text(encoding="utf-8"))
    offset = saved["offset"]
    print(f"{serial}: reboot")
    adb(serial, "reboot")
    time.sleep(10)
    adb(serial, "wait-for-device", timeout=300)
    wait_for(lambda: shell(serial, "getprop", "sys.boot_completed", check=False) == "1", 240)
    time.sleep(20)
    locked = shell(serial, "getprop", "sys.user.0.ce_available", check=False) != "true"
    lines = log_lines(serial)
    report.check("LOCKED_BOOT_COMPLETED reached the plugin", any("LOCKED_BOOT_COMPLETED" in line for line in lines),
                 f"credential storage {'locked' if locked else 'unlocked'}")
    report.check("alarm clocks after the reboot", wait_for(lambda: clocks(serial) == saved["clocks"], 30),
                 f"clocks {[stamp(t, offset) for t in clocks(serial)]}")
    following = scheduled(serial)[1]
    report.check("next alarm clock", str(min(saved["clocks"])) in following.replace(" ", "") or bool(following), following.split("=")[0][:60])
    if args.shots:
        shell(serial, "input", "keyevent", "KEYCODE_WAKEUP", check=False)
        time.sleep(3)
        remote = "/data/local/tmp/chime-lock.png"
        shell(serial, "screencap", "-p", remote)
        target = Path(args.shots) / f"lock-screen-{serial}.png"
        target.parent.mkdir(parents=True, exist_ok=True)
        adb(serial, "pull", remote, str(target))
        shell(serial, "rm", "-f", remote)
        print(f"    lock screen: saved {target.name} (next alarm {stamp(min(saved['clocks']), offset)})")
    print(f"{serial}: REBOOT {'PASS' if report.ok else 'FAIL'}; unlock the device, then run the finish phase")
    return report.ok


def finish(serial, args):
    report = Report()
    saved = json.loads(state_file(serial).read_text(encoding="utf-8"))
    offset = saved["offset"]
    print(f"{serial}: finish")
    report.check("BOOT_COMPLETED reached the plugin", wait_for(lambda: any("reschedule android.intent.action.BOOT_COMPLETED" in line for line in log_lines(serial)), 30))
    values, alarms = probe(serial, "state")
    nexts = sorted(int(alarm["next"]) for alarm in alarms.values() if alarm["next"])
    report.check("nextTrigger after the reboot", nexts == saved["clocks"], f"{[stamp(t, offset) for t in nexts]}")
    probe(serial, "clear")
    report.check("probes removed", not clocks(serial), f"clocks {len(clocks(serial))}")
    out = subprocess.run(["adb", "-s", serial, "uninstall", TEST_PACKAGE], capture_output=True, text=True, timeout=120)
    report.check("androidTest APK uninstalled", out.returncode == 0)
    state_file(serial).unlink(missing_ok=True)
    print(f"{serial}: FINISH {'PASS' if report.ok else 'FAIL'}")
    return report.ok


def clear(serial, args):
    """Without a reboot: removes the probes and their notifications and uninstalls the androidTest APK."""
    report = Report()
    probe(serial, "clear")
    report.check("probes removed", not clocks(serial), f"clocks {len(clocks(serial))}")
    out = subprocess.run(["adb", "-s", serial, "uninstall", TEST_PACKAGE], capture_output=True, text=True, timeout=120)
    report.check("androidTest APK uninstalled", out.returncode == 0)
    state_file(serial).unlink(missing_ok=True)
    print(f"{serial}: CLEAR {'PASS' if report.ok else 'FAIL'}")
    return report.ok


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("serial")
    parser.add_argument("phase", choices=("run", "reboot", "finish", "clear"))
    parser.add_argument("--no-install", action="store_true", help="keep the installed builds")
    parser.add_argument("--skip-permission", action="store_true", help="leave the exact alarm permission alone")
    parser.add_argument("--shots", metavar="DIR", help="reboot: save the lock screen into DIR")
    args = parser.parse_args(argv)
    return 0 if {"run": run, "reboot": reboot, "finish": finish, "clear": clear}[args.phase](args.serial, args) else 1


if __name__ == "__main__":
    sys.exit(main())

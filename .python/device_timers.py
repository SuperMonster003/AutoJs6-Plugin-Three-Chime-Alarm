"""Timer restore across a reboot on one device (roadmap P7.1).

    py .python/device_timers.py <serial> run [--no-install] [--short 30]
    py .python/device_timers.py <serial> clear

run: installs the debug APK (`adb install -r`, unless --no-install) and drives the debug hook RingDebugReceiver
    (direct boot aware in debug builds, answers in the log). It starts three silent probe timers without vibration:
    A runs 30 minutes, B is paused with 10 minutes left, C runs --short seconds and is started right before the reboot,
    so that it ends while the device is off. Before the reboot it checks the alarm clock of A and C in `dumpsys alarm`
    and the ongoing notifications; then it reboots and checks, before the first unlock (the device may keep a secure lock
    screen; nothing is unlocked):
    1. the boot count grew by one;
    2. A runs with the same end (`endsAt`, wall clock, within 1 s since every reconcile realigns it from the
       elapsed-realtime clock): the time while the device was off counts (T110), its time left shrank by the wall time
       between the two readings, its alarm clock is set again and it carries the new boot. The clock the device kept
       while off can be seconds off until network time corrects it shortly after the boot; within one boot the
       elapsed-realtime clock counts (T110), so that correction moves the end by the same amount (roadmap Q36). The
       script measures the device clock against the PC's before the reboot and as soon as adb is back, accepts the end
       either unmoved or moved by that difference, and compares the alarm clock with A's end read again;
    3. B is still paused with exactly the time left it had;
    4. C, past its end by less than the 5 minute window, rings late (T16, log `ChimeTimer ring <id> ... late=`); the
       hook dismisses it like the notification button, and the dismissed timer is removed;
    5. the ongoing notifications of A and B are posted again;
    then cancels A and B and checks that no timer alarm clock or notification is left.
clear: cancels every probe timer (after a run that stopped half-way).

The plugin stays installed. Output: PASS / FAIL per check and the key values only. Nothing here taps the screen or
answers a dialog; a ring of C shows the silent ring screen for a few seconds before it is dismissed.
"""
import argparse
import re
import subprocess
import sys
import time
from pathlib import Path

PLUGIN = "io.github.supermonster003.autojs6.plugin.three.chime.alarm"
HOOK = f"{PLUGIN}/.ring.RingDebugReceiver"
HOOK_ACTION = f"{PLUGIN}.debug.RING_PROBE"
ROOT = Path(__file__).resolve().parent.parent
PREFIX = "p71-probe"
MINUTE = 60_000
WINDOW = 5 * MINUTE


def adb(serial, *args, check=True, timeout=120):
    out = subprocess.run(["adb", "-s", serial, *args], capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=timeout)
    if check and out.returncode != 0:
        raise SystemExit(f"adb {' '.join(args[:3])} failed: {(out.stderr or out.stdout).strip()[:200]}")
    return out.stdout.replace("\r", "").strip()


def shell(serial, *args, check=True):
    return adb(serial, "shell", *args, check=check)


class Report:
    def __init__(self):
        self.failed = 0

    def check(self, step, passed, text=""):
        print(f"{'PASS' if passed else 'FAIL'} {step}" + (f": {text}" if text else ""), flush=True)
        if not passed:
            self.failed += 1
        return passed


class Hook:
    def __init__(self, serial):
        self.serial = serial
        self.token = int(time.time()) % 100000 * 100

    def __call__(self, op, seconds=20, **extras):
        """Sends the debug hook an operation and answers its log line as a dict (None when it does not answer)."""
        self.token += 1
        token = str(self.token)
        command = ["am", "broadcast", "-f", "32", "-n", HOOK, "-a", HOOK_ACTION, "--es", "op", op, "--es", "token", token]
        for key, value in extras.items():
            if isinstance(value, bool):
                command += ["--ez", key, "true" if value else "false"]
            elif isinstance(value, int):
                command += ["--el", key, str(value)]
            elif value is not None:
                command += ["--es", key, str(value)]
        shell(self.serial, *command, check=False)
        deadline = time.time() + seconds
        while time.time() < deadline:
            text = shell(self.serial, "logcat", "-d", "-v", "brief", "-s", "ChimeRingDebug:I", check=False)
            for line in reversed(text.splitlines()):
                found = re.search(rf"token={token} (.*)$", line)
                if found:
                    return dict(part.split("=", 1) for part in found.group(1).split(" ") if "=" in part)
            time.sleep(0.5)
        return None


def timers(hook, seconds=20):
    """The probe timers by label: {label: {id, state, remaining, endsAt, boot}}, with `now` and `bootCount`."""
    answer = hook("timers", seconds=seconds)
    if answer is None:
        return None
    found = {}
    if answer.get("timers", "none") != "none":
        for item in answer["timers"].split(","):
            label, timer_id, state, *rest = item.split(":")
            values = dict(part.split("=", 1) for part in rest)
            found[label] = {"id": timer_id, "state": state, "remaining": int(values["remaining"]),
                            "endsAt": None if values["endsAt"] == "null" else int(values["endsAt"]),
                            "boot": None if values["boot"] == "null" else int(values["boot"])}
    return {"now": int(answer["now"]), "bootCount": None if answer["bootCount"] == "null" else int(answer["bootCount"]), "timers": found}


def timer_clocks(serial):
    """The instants of the plugin's TIMER_RING alarm clocks in `dumpsys alarm` (`origWhen` on API 31+, `when` before)."""
    text = shell(serial, "dumpsys", "alarm")
    found = set()
    for block in re.split(r"\n(?=\s+[A-Z_]+ #\d+: Alarm\{)", text):
        head = re.search(r"[A-Z_]+ #\d+: Alarm\{\S+ type \d+ (?:origWhen|when) (\d+)(?: whenElapsed \S+)? (\S+)\}", block)
        if head and head.group(2) == PLUGIN and "TIMER_RING" in block:
            found.add(int(head.group(1)))
    return sorted(found)


def clock_near(serial, at, tolerance=1000):
    """A timer alarm clock within [tolerance] ms of [at] (the end may be realigned by a few ms, T110)."""
    return at is not None and any(abs(clock - at) <= tolerance for clock in timer_clocks(serial))


def timer_notifications(serial):
    text = shell(serial, "dumpsys", "notification", check=False)
    return len(re.findall(rf"NotificationRecord\([^\n]*pkg={re.escape(PLUGIN)}[^\n]*tag=timer\b", text))


def clock_offset(serial):
    """The device's wall clock minus the PC's in ms (the median of three readings, each against the middle of its adb call)."""
    offsets = []
    for _ in range(3):
        sent = time.time()
        answer = shell(serial, "date", "+%s%3N", check=False)
        received = time.time()
        if answer.isdigit():
            offsets.append(int(answer) - round((sent + received) / 2 * 1000))
    return sorted(offsets)[len(offsets) // 2] if offsets else None


def wait_for(predicate, seconds=20):
    deadline = time.time() + seconds
    while time.time() < deadline:
        value = predicate()
        if value:
            return value
        time.sleep(1)
    return predicate()


def install(serial, report):
    debug = sorted((ROOT / "app" / "build" / "outputs" / "apk" / "debug").glob("*.apk"), key=lambda p: p.stat().st_mtime)
    if not debug:
        raise SystemExit("build :app:assembleDebug first")
    out = subprocess.run(["adb", "-s", serial, "install", "-r", "-t", str(debug[-1])], capture_output=True, text=True, timeout=300)
    return report.check(f"install {debug[-1].name}", out.returncode == 0 and "Success" in out.stdout)


def start(hook, report, name, millis, paused=False):
    answer = hook("timer_start", label=f"{PREFIX}-{name}", millis=millis, paused=paused) or {"error": "no answer"}
    report.check(f"start {name} ({millis // 1000} s{', paused' if paused else ''})", "started" in answer,
                 f"state {answer.get('state')}" if "started" in answer else answer.get("error", ""))
    return answer.get("started")


def run(serial, args):
    report = Report()
    hook = Hook(serial)
    if not args.no_install and not install(serial, report):
        return False
    shell(serial, "logcat", "-c", check=False)
    hook("timer_clear")
    if not start(hook, report, "a", 30 * MINUTE) or not start(hook, report, "b", 10 * MINUTE, paused=True):
        hook("timer_clear")
        return False
    before = timers(hook)
    a, b = before["timers"][f"{PREFIX}-a"], before["timers"][f"{PREFIX}-b"]
    report.check("before: A runs, B paused", a["state"] == "running" and b["state"] == "paused" and b["endsAt"] is None,
                 f"A {a['remaining'] // 1000} s left, B {b['remaining'] // 1000} s left, boot {before['bootCount']}")
    report.check("before: alarm clock of A", wait_for(lambda: clock_near(serial, a["endsAt"]), 10), f"clocks {len(timer_clocks(serial))}")
    report.check("before: two ongoing notifications", wait_for(lambda: timer_notifications(serial) == 2, 10), f"{timer_notifications(serial)}")
    c_id = start(hook, report, "c", args.short * 1000)
    if not c_id:
        hook("timer_clear")
        return False

    offset_before = clock_offset(serial)
    print(f"{serial}: reboot", flush=True)
    rebooted = time.time()
    adb(serial, "reboot", check=False)
    time.sleep(10)
    adb(serial, "wait-for-device", timeout=300)
    offset_boot = clock_offset(serial)
    # How far the clock the device kept while off is from the clock before the reboot (0 when unknown).
    # zh-CN: 关机期间设备保持的时钟与重启前时钟之差 (未知时为 0).
    boot_error = offset_boot - offset_before if offset_boot is not None and offset_before is not None else 0
    wait_for(lambda: shell(serial, "getprop", "sys.boot_completed", check=False) == "1", 240)
    boot_seconds = time.time() - rebooted
    after = wait_for(lambda: timers(hook, seconds=5), 90)
    if not report.check("after: the hook answers before the first unlock", after is not None,
                        f"credential storage {'unlocked' if shell(serial, 'getprop', 'sys.user.0.ce_available', check=False) == 'true' else 'locked'}, booted in {boot_seconds:.0f} s"):
        return False
    report.check("after: boot count", after["bootCount"] == (before["bootCount"] or 0) + 1, f"{before['bootCount']} -> {after['bootCount']}")
    a2, b2 = after["timers"].get(f"{PREFIX}-a"), after["timers"].get(f"{PREFIX}-b")
    if not report.check("after: A and B restored", a2 is not None and b2 is not None, ",".join(sorted(after["timers"]))):
        hook("timer_clear")
        return False
    elapsed = after["now"] - before["now"]
    drift = (a["remaining"] - a2["remaining"]) - elapsed
    # Within one boot the end follows the elapsed-realtime clock and `endsAt` is realigned to the wall clock on every
    # reconcile (T110), so it may move by a few milliseconds; after the reboot it must not move further.
    # zh-CN: 同一次开机内到点时刻以开机时长时钟为准, 每次对账都把 `endsAt` 校准到墙钟 (T110), 可能移动几毫秒; 重启后不应再有更大的移动.
    # A correction of the boot clock before this reading moves the end by -boot_error and adds boot_error to the drift.
    # zh-CN: 在本次读数之前校正开机时钟会让到点时刻移动 -boot_error, 偏差多出 boot_error.
    moved = None if a2["endsAt"] is None else a2["endsAt"] - a["endsAt"]
    unmoved = moved is not None and abs(moved) <= 1000 and abs(drift) <= 1500
    corrected = moved is not None and abs(boot_error) > 1000 and abs(moved + boot_error) <= 1000 and abs(drift - boot_error) <= 1500
    report.check("after: A keeps its end, the time while off counts",
                 a2["state"] == "running" and (unmoved or corrected),
                 f"{elapsed // 1000} s between the readings, A {a2['remaining'] // 1000} s left (drift {drift} ms), end moved {moved} ms, "
                 f"boot clock {boot_error:+d} ms{' (corrected before the reading)' if corrected and not unmoved else ''}")
    report.check("after: A carries the new boot", a2["boot"] == after["bootCount"], f"boot {a2['boot']}")
    def clock_of_a():
        # A's end read again: a correction of the wall clock after the first reading realigns it (T110).
        # zh-CN: 重新读取 A 的到点时刻: 第一次读数之后校正墙钟会重新校准它 (T110).
        current = timers(hook, seconds=5)
        a_now = current["timers"].get(f"{PREFIX}-a") if current else None
        return a_now is not None and clock_near(serial, a_now["endsAt"]) and a_now["endsAt"]
    clock_end = wait_for(clock_of_a, 20)
    report.check("after: alarm clock of A", bool(clock_end),
                 f"clocks {len(timer_clocks(serial))}" + (f", end moved {clock_end - a['endsAt']} ms since the reboot" if clock_end else ""))
    report.check("after: B still paused with the same time left", b2["state"] == "paused" and b2["remaining"] == b["remaining"],
                 f"B {b2['remaining']} ms (before {b['remaining']} ms)")

    def ring_line():
        text = shell(serial, "logcat", "-d", "-v", "brief", "-s", "ChimeTimer:I", check=False)
        return next((line for line in text.splitlines() if f"ring {c_id} " in line), None)
    line = wait_for(ring_line, 60)
    late = int(re.search(r"late=(-?\d+)", line).group(1)) if line else None
    report.check("after: C rings late within the window (T16)", line is not None and 0 <= late < WINDOW,
                 f"late {late // 1000} s" if line else "no ring line in the log")
    if line:
        state = wait_for(lambda: (lambda s: s if f"{PREFIX}-c:ringing" in s.get("ringing", "") else None)(hook("state") or {}), 10) or {}
        report.check("after: C is the ringing timer", bool(state), f"ringing {state.get('ringing')}")
        hook("dismiss", id=c_id)
        gone = wait_for(lambda: f"{PREFIX}-c" not in ((timers(hook, seconds=5) or {"timers": {"?": 0}})["timers"]), 20)
        report.check("after: the dismissed C is removed", bool(gone))
    report.check("after: ongoing notifications of A and B", wait_for(lambda: timer_notifications(serial) == 2, 20), f"{timer_notifications(serial)}")

    cleared = hook("timer_clear") or {}
    report.check("clean-up: probe timers cancelled", cleared.get("cleared") == "2", f"cleared {cleared.get('cleared')}")
    report.check("clean-up: no timer alarm clock or notification left",
                 wait_for(lambda: not timer_clocks(serial) and timer_notifications(serial) == 0, 10),
                 f"clocks {len(timer_clocks(serial))}, notifications {timer_notifications(serial)}")
    print(f"{serial}: {'all passed' if report.failed == 0 else f'{report.failed} failed'}", flush=True)
    return report.failed == 0


def clear(serial, args):
    answer = Hook(serial)("timer_clear") or {}
    print(f"{serial}: cleared {answer.get('cleared', 'no answer')}")
    return "cleared" in answer


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("serial")
    parser.add_argument("phase", choices=("run", "clear"))
    parser.add_argument("--no-install", action="store_true")
    parser.add_argument("--short", type=int, default=30, help="run: seconds of timer C, started right before the reboot")
    args = parser.parse_args(argv)
    return 0 if {"run": run, "clear": clear}[args.phase](args.serial, args) else 1


if __name__ == "__main__":
    sys.exit(main())

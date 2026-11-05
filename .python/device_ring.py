"""Ringing checks of 3-Chime Alarm on one device (roadmap P2.4).

    py .python/device_ring.py <serial> run [--no-install] [--cases nonotify,lock,degraded,speech,dnd] [--macaw]
    py .python/device_ring.py <serial> clear

run: installs the debug APK (its debug-only RingDebugReceiver is the hook: `am instrument` would force-stop the ring
    under test, so the script sends shell broadcasts and reads the answers from the log) and runs the cases:
    nonotify (API 33+ without the notification permission, roadmap T69): the ring sounds with no notification and no
        ringing screen;
    lock: a one-shot probe with `sound.volume` 0.3 and a 1 minute snooze rings with the screen off: the screen turns
        on, the ringing screen is resumed over the lock screen, an alarm player is active, the device vibrates, the
        wake lock is held and the alarm stream is at the probe's volume (the user's volume saved); the notification
        snooze action snoozes it (the volume comes back, the screen closes, the service stops); the snooze round rings
        again on the lock screen and the dismiss action ends it (the one-shot alarm retires);
    degraded (API 34+): the full screen intent permission is denied with `appops`: the ring sounds with a notification
        and no ringing screen; the original mode comes back afterwards;
    speech: `after` with the system engine, `before` with the 3-Macaw Speech engine (--macaw, when installed) and
        `only` with an engine that is not installed (the default sound plays instead, roadmap T63);
    dnd: Do Not Disturb set to priority (alarms allowed by default) does not stop the ring; the original mode comes back.
    Cases that need a permission the plugin lacks (notifications on API 33+) are reported as skipped: granting is the
    maintainer's step on the device.
clear: dismisses and removes the probe alarms and restores the saved settings of an interrupted run.

Output: PASS / FAIL / SKIP per check and the key values only. Nothing here answers a system dialog.
"""
import argparse
import base64
import json
import re
import subprocess
import sys
import tempfile
import time
from pathlib import Path

PLUGIN = "io.github.supermonster003.autojs6.plugin.three.chime.alarm"
HOOK = f"{PLUGIN}/.ring.RingDebugReceiver"
HOOK_ACTION = f"{PLUGIN}.debug.RING_PROBE"
MACAW = "io.github.supermonster003.autojs6.plugin.three.macaw.speech"
TAGS = ("ChimeRing", "ChimeRingDebug", "ChimeSchedule")
ROOT = Path(__file__).resolve().parent.parent
LINE = re.compile(r"^(\d\d-\d\d \d\d:\d\d:\d\d\.\d+)\s+\w/(\w+)\(\s*\d+\):\s?(.*)$")


def adb(serial, *args, check=True, timeout=120):
    out = subprocess.run(["adb", "-s", serial, *args], capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=timeout)
    if check and out.returncode != 0:
        raise SystemExit(f"adb {' '.join(args[:3])} failed: {(out.stderr or out.stdout).strip()[:200]}")
    return out.stdout.replace("\r", "")


def shell(serial, *args, check=True):
    return adb(serial, "shell", *args, check=check)


def state_file(serial):
    return Path(tempfile.gettempdir()) / f"chime-device-ring-{serial}.json"


class Report:
    def __init__(self):
        self.failed = 0

    def check(self, step, passed, text=""):
        print(f"{'PASS' if passed else 'FAIL'} {step}" + (f": {text}" if text else ""), flush=True)
        if not passed:
            self.failed += 1
        return passed

    @staticmethod
    def skip(step, why):
        print(f"SKIP {step}: {why}", flush=True)


class Device:
    def __init__(self, serial):
        self.serial = serial
        self.sdk = int(shell(serial, "getprop", "ro.build.version.sdk").strip())
        self.token = int(time.time()) % 100000 * 100
        self.seen = set(self.logs())
        self.recent = []
        self.pending = []

    def logs(self):
        text = shell(self.serial, "logcat", "-d", "-v", "time", "-s", *(f"{tag}:I" for tag in TAGS), check=False)
        return [line for line in text.splitlines() if LINE.match(line)]

    def new_logs(self):
        """Log lines (tag, message) since the previous call."""
        fresh = [line for line in self.logs() if line not in self.seen]
        self.seen.update(fresh)
        return [(m.group(2), m.group(3)) for m in map(LINE.match, fresh)]

    def hook(self, op, **extras):
        """Sends the debug hook an operation and answers its log line as a dict."""
        self.token += 1
        token = str(self.token)
        command = ["am", "broadcast", "-f", "32", "-n", HOOK, "-a", HOOK_ACTION, "--es", "op", op, "--es", "token", token]
        for key, value in extras.items():
            if isinstance(value, int):
                command += ["--ei", key, str(value)]
            elif isinstance(value, dict):
                command += ["--es", key + "64", base64.b64encode(json.dumps(value).encode()).decode()]
            elif value is not None:
                command += ["--es", key, str(value)]
        shell(self.serial, *command)
        deadline = time.time() + 20
        while time.time() < deadline:
            text = shell(self.serial, "logcat", "-d", "-v", "brief", "-s", "ChimeRingDebug:I", check=False)
            for line in reversed(text.splitlines()):
                found = re.search(rf"token={token} (.*)$", line)
                if found:
                    return parse(found.group(1))
            time.sleep(0.5)
        raise SystemExit(f"hook {op} did not answer")

    def resumed_ring_screen(self):
        text = shell(self.serial, "dumpsys", "activity", "activities", check=False)
        lines = [line for line in text.splitlines() if re.search(r"mResumedActivity|topResumedActivity|ResumedActivity:", line)]
        return any("RingActivity" in line for line in lines)

    def wake_lock_held(self):
        return "ThreeChimeAlarm:ring" in shell(self.serial, "dumpsys", "power", check=False)

    def service_running(self):
        text = shell(self.serial, "dumpsys", "activity", "services", PLUGIN, check=False)
        return "ring.RingService" in text

    def vibrating(self):
        if self.sdk >= 31:
            text = shell(self.serial, "dumpsys", "vibrator_manager", check=False)
            current = re.search(r"(?s)mCurrentVibration=(.*?)\n\s*m[A-Z]", text) or re.search(r"(?s)CurrentVibration:?(.*?)\n\n", text)
            body = current.group(1) if current else ""
            return PLUGIN in body or ("ALARM" in body and "null" not in body[:12])
        text = shell(self.serial, "dumpsys", "vibrator", check=False)
        current = re.search(r"mCurrentVibration=(.*)", text)
        return bool(current) and "null" not in current.group(1)[:8]

    def screen_off(self):
        shell(self.serial, "input", "keyevent", "KEYCODE_SLEEP", check=False)
        time.sleep(4)

    def wake(self):
        shell(self.serial, "input", "keyevent", "KEYCODE_WAKEUP", check=False)
        time.sleep(1)

    def zen(self):
        return shell(self.serial, "settings", "get", "global", "zen_mode").strip()

    def set_zen(self, mode):
        name = {"0": "off", "1": "priority", "2": "none", "3": "alarms"}.get(mode, "off")
        shell(self.serial, "cmd", "notification", "set_dnd", name, check=False)


def parse(text):
    return dict(part.split("=", 1) for part in text.split(" ") if "=" in part)


def expected_index(volume, low, high):
    return low + int(volume * (high - low) + 0.5)


def playing(state: dict) -> bool:
    """An active player with alarm usage; below API 26 the plugin cannot list players and reports `unknown`, so a
    sounding ring counts."""
    players = state.get("alarmPlayers", "0")
    if players == "unknown":
        return state.get("sounding", "none") != "none"
    return int(players) >= 1


def wait_log(device, predicate, seconds):
    """Waits for a log line (tag, message) matching [predicate] that no earlier wait took; answers it, or None."""
    deadline = time.time() + seconds
    while True:
        fresh = device.new_logs()
        device.recent.extend(fresh)
        device.pending.extend(fresh)
        for index, (tag, message) in enumerate(device.pending):
            if predicate(tag, message):
                del device.pending[index]
                return message
        if time.time() >= deadline:
            return None
        time.sleep(1)


def install(serial, report):
    debug = sorted((ROOT / "app" / "build" / "outputs" / "apk" / "debug").glob("*.apk"), key=lambda p: p.stat().st_mtime)
    if not debug:
        raise SystemExit("build :app:assembleDebug first")
    out = subprocess.run(["adb", "-s", serial, "install", "-r", "-t", str(debug[-1])], capture_output=True, text=True, timeout=300)
    report.check(f"install {debug[-1].name}", out.returncode == 0 and "Success" in out.stdout)


def ring(device, report, label, options, seconds=35, screen_off=False):
    """Adds a probe ringing in [seconds] and waits until its session starts; answers (id, session line)."""
    added = device.hook("add", label=label, inSeconds=seconds, options=options)
    if not report.check(f"{label} added", "added" in added, added.get("error", "")):
        return None, None
    if screen_off:
        device.screen_off()
    session = wait_log(device, lambda tag, m: tag == "ChimeRing" and m.startswith(f"ring session {added['added']} "), seconds + 60)
    report.check(f"{label} rings", session is not None, session.split(" occurrence=")[0] if session else "no ring session in the log")
    time.sleep(3)
    return added["added"], session


def end(device, report, label, alarm_id, how="dismiss"):
    device.hook(how, id=alarm_id)
    handled = wait_log(device, lambda tag, m: tag == "ChimeRing" and m.startswith(f"{how} by user:"), 15)
    report.check(f"{label} {how} action", handled is not None and handled.endswith("handled=true"), handled or "no answer")
    ended = wait_log(device, lambda tag, m: tag == "ChimeRing" and m == f"ring session {alarm_id} ended", 15)
    report.check(f"{label} round ended", ended is not None)
    time.sleep(2)
    return device.hook("state")


def case_lock(device, report, saved):
    state = device.hook("state")
    if state.get("notifications") != "true":
        Report.skip("lock", "notifications are not allowed (API 33+ runtime permission, the maintainer's step)")
        return
    report.check("full screen intent allowed", state.get("fullScreen") == "true", f"fullScreen={state.get('fullScreen')}")
    user_volume = int(state["volume"])
    low, high = int(state["volumeMin"]), int(state["volumeMax"])
    target = expected_index(0.3, low, high)
    if target == user_volume:
        target_volume = 0.8
        target = expected_index(target_volume, low, high)
    else:
        target_volume = 0.3
    options = {"sound": {"volume": target_volume}, "snooze": {"minutes": 1, "max": 3}}
    alarm_id, session = ring(device, report, "p24-probe-lock", options, screen_off=True)
    if not alarm_id:
        return
    notification = wait_log(device, lambda tag, m: m.startswith(f"ring notification {alarm_id} "), 10) or ""
    report.check("notification posted with full screen intent", "posted=true" in notification and "fullScreen=true" in notification, notification.split(alarm_id)[-1].strip())
    state = device.hook("state")
    report.check("screen turned on", state.get("interactive") == "true")
    report.check("ringing screen over the lock screen", device.resumed_ring_screen() and state.get("keyguard") == "true", f"resumed={device.resumed_ring_screen()} keyguard={state.get('keyguard')}")
    report.check("alarm player active", playing(state), f"alarmPlayers={state.get('alarmPlayers')} {session.split(' player=')[-1].split(' ')[0]}")
    report.check("independent volume", state.get("volume") == str(target) and state.get("saved") == str(user_volume), f"volume={state.get('volume')} (want {target} of {low} - {high}) saved={state.get('saved')} (user {user_volume})")
    report.check("vibrating", device.vibrating())
    report.check("wake lock held", device.wake_lock_held())

    state = end(device, report, "lock", alarm_id, "snooze")
    report.check("snoozed", "p24-probe-lock:snoozed:1" in state.get("ringing", ""), state.get("ringing", ""))
    report.check("volume restored after snooze", state.get("volume") == str(user_volume) and state.get("saved") == "none", f"volume={state.get('volume')} saved={state.get('saved')}")
    report.check("ringing screen closed", not device.resumed_ring_screen())
    time.sleep(3)
    report.check("service stopped, wake lock released", not device.service_running() and not device.wake_lock_held())

    device.screen_off()
    session = wait_log(device, lambda tag, m: tag == "ChimeRing" and m.startswith(f"ring session {alarm_id} "), 90)
    report.check("snooze round rings again", session is not None and "~1" in session, (session or "no ring").split(" player=")[0].split("occurrence=")[-1])
    time.sleep(3)
    state = device.hook("state")
    report.check("snooze round on the lock screen", device.resumed_ring_screen() and state.get("keyguard") == "true" and state.get("interactive") == "true")
    state = end(device, report, "lock", alarm_id, "dismiss")
    probe = next((p for p in state.get("probes", "").split(",") if p.startswith("p24-probe-lock:")), "")
    report.check("dismissed one-shot alarm retired", "enabled=false" in probe and state.get("sounding") == "none", probe.split(":", 2)[-1])
    report.check("volume restored after dismiss", state.get("volume") == str(user_volume) and state.get("saved") == "none", f"volume={state.get('volume')}")
    device.wake()


def case_degraded(device, report, saved):
    if device.sdk < 34:
        Report.skip("degraded", f"API {device.sdk} has no full screen intent permission")
        return
    state = device.hook("state")
    if state.get("notifications") != "true":
        Report.skip("degraded", "notifications are not allowed")
        return
    original = re.search(r"USE_FULL_SCREEN_INTENT: (\w+)", shell(device.serial, "appops", "get", PLUGIN, "USE_FULL_SCREEN_INTENT"))
    saved["fullScreenMode"] = original.group(1) if original else "default"
    save(device.serial, saved)
    shell(device.serial, "appops", "set", PLUGIN, "USE_FULL_SCREEN_INTENT", "deny")
    try:
        state = device.hook("state")
        report.check("full screen intent denied", state.get("fullScreen") == "false")
        alarm_id, _ = ring(device, report, "p24-probe-degraded", {}, screen_off=True)
        if not alarm_id:
            return
        notification = wait_log(device, lambda tag, m: m.startswith(f"ring notification {alarm_id} "), 10) or ""
        report.check("notification without full screen intent", "posted=true" in notification and "fullScreen=false" in notification, notification.split(alarm_id)[-1].strip())
        state = device.hook("state")
        report.check("no ringing screen", not device.resumed_ring_screen())
        report.check("sound plays", playing(state), f"alarmPlayers={state.get('alarmPlayers')} interactive={state.get('interactive')}")
        end(device, report, "degraded", alarm_id)
    finally:
        shell(device.serial, "appops", "set", PLUGIN, "USE_FULL_SCREEN_INTENT", saved.pop("fullScreenMode"))
        save(device.serial, saved)
        device.wake()


def case_nonotify(device, report):
    """Roadmap T69: without the notification permission (API 33+) the ring still sounds, with no notification or screen."""
    state = device.hook("state")
    if device.sdk < 33 or state.get("notifications") == "true":
        Report.skip("nonotify", "notifications are allowed")
        return
    alarm_id, session = ring(device, report, "p24-probe-nonotify", {}, screen_off=True)
    if not alarm_id:
        return
    notification = wait_log(device, lambda tag, m: m.startswith(f"ring notification {alarm_id} "), 10) or ""
    report.check("no notification without the permission", "posted=false" in notification, notification.split(alarm_id)[-1].strip())
    state = device.hook("state")
    report.check("sound plays without notification", playing(state) and not device.resumed_ring_screen(), f"alarmPlayers={state.get('alarmPlayers')}")
    end(device, report, "nonotify", alarm_id)
    device.wake()


def case_speech(device, report, macaw):
    cases = [("p24-probe-system", {"text": "Chime test, system engine.", "mode": "after"}, "(system engine)")]
    if macaw:
        cases.append(("p24-probe-macaw", {"text": "Chime test, Macaw engine.", "mode": "before", "engine": MACAW}, f"(engine {MACAW})"))
    for label, speak, engine in cases:
        alarm_id, _ = ring(device, report, label, {"speak": speak})
        if not alarm_id:
            continue
        ready = wait_log(device, lambda tag, m: tag == "ChimeRing" and (m.startswith("speech ready") or m.startswith("speech unavailable")), 25)
        report.check(f"{label} engine ready", ready is not None and ready.endswith(engine), ready or "no speech line")
        spoken = wait_log(device, lambda tag, m: tag == "ChimeRing" and (m.startswith("speech done") or m.startswith("speech error") or m.startswith("speech unavailable")), 40)
        report.check(f"{label} spoken", spoken == "speech done", spoken or "no end of speech")
        state = device.hook("state")
        report.check(f"{label} sound plays", playing(state), f"alarmPlayers={state.get('alarmPlayers')}")
        end(device, report, label, alarm_id)
    alarm_id, session = ring(device, report, "p24-probe-noengine", {"speak": {"text": "Chime test.", "mode": "only", "engine": "com.example.missing.tts"}})
    if alarm_id:
        missing = wait_log(device, lambda tag, m: m.startswith("speech unavailable"), 10)
        report.check("missing engine is skipped", missing == "speech unavailable: engine not installed", missing or "no line")
        time.sleep(2)
        state = device.hook("state")
        report.check("speech only ring plays the default sound instead", playing(state), f"alarmPlayers={state.get('alarmPlayers')}")
        end(device, report, "noengine", alarm_id)


def case_dnd(device, report, saved):
    saved.setdefault("zen", device.zen())
    save(device.serial, saved)
    device.set_zen("1")
    try:
        report.check("do not disturb on (priority)", device.zen() == "1")
        alarm_id, _ = ring(device, report, "p24-probe-dnd", {})
        if not alarm_id:
            return
        state = device.hook("state")
        report.check("rings under do not disturb", playing(state) and state.get("sounding") == alarm_id, f"alarmPlayers={state.get('alarmPlayers')}")
        end(device, report, "dnd", alarm_id)
    finally:
        device.set_zen(saved["zen"])
        report.check("do not disturb restored", device.zen() == saved["zen"], f"zen_mode={device.zen()}")
        saved.pop("zen")
        save(device.serial, saved)


def save(serial, saved):
    state_file(serial).write_text(json.dumps(saved), encoding="utf-8")


def run(serial, args):
    report = Report()
    if not args.no_install:
        install(serial, report)
    device = Device(serial)
    saved = {}
    save(serial, saved)
    device.wake()
    device.hook("clear")
    state = device.hook("state")
    print(f"INFO api={device.sdk} volume={state.get('volume')} of {state.get('volumeMin')} - {state.get('volumeMax')} zen={device.zen()} notifications={state.get('notifications')} fullScreen={state.get('fullScreen')}")
    cases = args.cases.split(",")
    try:
        if "lock" in cases:
            case_lock(device, report, saved)
        if "nonotify" in cases:
            case_nonotify(device, report)
        if "degraded" in cases:
            case_degraded(device, report, saved)
        if "speech" in cases:
            installed = MACAW in shell(serial, "pm", "list", "packages", MACAW)
            if args.macaw and not installed:
                Report.skip("macaw", "3-Macaw Speech is not installed")
            case_speech(device, report, args.macaw and installed)
        if "dnd" in cases:
            case_dnd(device, report, saved)
    finally:
        cleared = device.hook("clear")
        print(f"INFO probes cleared={cleared.get('cleared')}")
        restore(device, saved)
    return report.failed


def restore(device, saved):
    if "fullScreenMode" in saved:
        shell(device.serial, "appops", "set", PLUGIN, "USE_FULL_SCREEN_INTENT", saved["fullScreenMode"])
    if "zen" in saved:
        device.set_zen(saved["zen"])
    save(device.serial, {})


def clear(serial, args):
    device = Device(serial)
    cleared = device.hook("clear")
    path = state_file(serial)
    saved = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
    restore(device, saved)
    print(f"INFO probes cleared={cleared.get('cleared')} restored={sorted(saved)}")
    return 0


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("serial")
    sub = parser.add_subparsers(dest="command", required=True)
    run_parser = sub.add_parser("run")
    run_parser.add_argument("--no-install", action="store_true")
    run_parser.add_argument("--cases", default="nonotify,lock,degraded,speech,dnd")
    run_parser.add_argument("--macaw", action="store_true", help="also speak with the 3-Macaw Speech engine")
    sub.add_parser("clear")
    args = parser.parse_args(argv)
    failed = {"run": run, "clear": clear}[args.command](args.serial, args)
    print("RESULT " + ("PASS" if not failed else f"FAIL ({failed})"))
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())

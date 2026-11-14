"""Dismiss challenge checks of 3-Chime Alarm on one device (roadmap P9.1, P9.3, 1.3.0).

    py .python/device_challenges.py <serial> run [--no-install] [--cases shake,emergency,autostop]
    py .python/device_challenges.py <serial> clear

run: installs the debug APK (unless --no-install) and rings silent probes (`sound` silent at volume 0, no vibration)
    with the screen off, so the full screen intent opens the ringing screen over the lock screen, through the debug
    hook of `device_ring.py`:
    shake (emulators only: the shakes are injected with `adb emu sensor set acceleration`): a `shake` challenge of 5
        shakes; five shakes of about 2.4 g solve it (`by: 'challenge'`) and the round ends;
    emergency: a `math` challenge; four presses of volume up (`input keyevent`) followed by a pause longer than the
        4 second window neither dismiss it nor change the alarm volume; then five presses mixing up and down dismiss
        it (`by: 'user'`) and the user's alarm volume is back afterwards; skipped when the emergency dismiss setting is
        off on the device (the script never changes the user's settings); the lock screen state is printed (an
        emulator has no secure lock screen, the secure case is the maintainer's step on a real device);
    autostop: a `math` challenge with `autoSilenceMinutes` 1 ends by itself within about 75 seconds with no answer.
    The ring screen and notification lines of the log tell what happened (tag ChimeRing); probes carry the label
    prefix of `device_ring.py` and are removed afterwards.
clear: dismisses and removes the probe alarms of an interrupted run.

Output: PASS / FAIL / SKIP per check and the key values only. Nothing here answers a system dialog.
"""
import argparse
import subprocess
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from device_ring import Device, Report, install, ring, shell, wait_log  # noqa: E402

SILENT = {"sound": {"source": "silent", "volume": 0}, "vibrate": False}
SHAKES = 5
# EmergencyPresses.REQUIRED and WINDOW_MILLIS (seconds).
PRESSES = 5
EMERGENCY_WINDOW = 4
# RingDebugReceiver.LABEL_PREFIX: the hook adds and clears only alarms with this label prefix.
LABEL_PREFIX = "p24-probe"


def probe(label):
    return f"{LABEL_PREFIX}-p9-{label}"


def screen_resumed(device, alarm_id, seconds=20):
    line = wait_log(device, lambda tag, m: tag == "ChimeRing" and m == f"ring screen resumed {alarm_id}", seconds)
    return line is not None or device.resumed_ring_screen()


def challenge_ring(device, report, label, dismiss, extra=None):
    options = dict(SILENT, dismiss=dismiss, **(extra or {}))
    alarm_id, _ = ring(device, report, label, options, seconds=25, screen_off=True)
    if alarm_id is None:
        return None
    state = device.hook("state")
    report.check(f"{label} challenge in effect", state.get("challenge") == dismiss["challenge"], f"challenge={state.get('challenge')}")
    report.check(f"{label} ring screen over the lock screen", screen_resumed(device, alarm_id), f"keyguard={state.get('keyguard')}")
    return alarm_id


def ended(device, alarm_id, seconds):
    return wait_log(device, lambda tag, m: tag == "ChimeRing" and m == f"ring session {alarm_id} ended", seconds) is not None


def case_shake(device, report):
    if not device.serial.startswith("emulator-"):
        Report.skip("shake", "the shakes are injected with `adb emu sensor`, emulators only")
        return
    label = probe("shake")
    alarm_id = challenge_ring(device, report, label, {"challenge": "shake", "shakeCount": SHAKES})
    if alarm_id is None:
        return
    for _ in range(SHAKES + 1):
        sensor(device, "0:24:0")
        time.sleep(0.45)
        sensor(device, "0:9.81:0")
        time.sleep(0.45)
    solved = wait_log(device, lambda tag, m: tag == "ChimeRing" and m.startswith("challenge shake solved on the ring screen:"), 15)
    report.check(f"{label} solved by {SHAKES} shakes", solved is not None and solved.endswith("handled=true"), solved or "no solve line")
    report.check(f"{label} round ended", ended(device, alarm_id, 15))


def sensor(device, value):
    subprocess.run(["adb", "-s", device.serial, "emu", "sensor", "set", "acceleration", value], capture_output=True, timeout=30)


def case_emergency(device, report):
    if device.hook("state").get("emergencyDismiss") != "true":
        Report.skip("emergency", "the emergency dismiss setting is off on this device")
        return
    label = probe("emergency")
    user_volume = device.hook("state").get("volume")
    alarm_id = challenge_ring(device, report, label, {"challenge": "math"})
    if alarm_id is None:
        return
    # While it rings the alarm stream holds the probe volume (the minimum here), so volume up would show an adjustment.
    before = device.hook("state").get("volume")
    shell(device.serial, "input", "keyevent", *(["KEYCODE_VOLUME_UP"] * (PRESSES - 1)), check=False)
    time.sleep(EMERGENCY_WINDOW + 0.5)
    middle = device.hook("state")
    report.check(
        f"{label} {PRESSES - 1} presses neither dismiss nor change the volume",
        middle.get("sounding") == alarm_id and middle.get("volume") == before,
        f"sounding={middle.get('sounding')} volume {before} -> {middle.get('volume')}",
    )
    shell(device.serial, "input", "keyevent", *(["KEYCODE_VOLUME_UP", "KEYCODE_VOLUME_DOWN"] * 3)[:PRESSES], check=False)
    handled = wait_log(device, lambda tag, m: tag == "ChimeRing" and m.startswith("emergency dismiss on the ring screen:"), 15)
    report.check(f"{label} {PRESSES} volume key presses (up and down) dismiss", handled is not None and handled.endswith("handled=true"), handled or "no emergency line")
    report.check(f"{label} round ended", ended(device, alarm_id, 15))
    after = device.hook("state").get("volume")
    report.check(f"{label} user alarm volume restored", after == user_volume, f"{user_volume} -> {after}")


def case_autostop(device, report):
    label = probe("autostop")
    alarm_id = challenge_ring(device, report, label, {"challenge": "math"}, {"autoSilenceMinutes": 1})
    if alarm_id is None:
        return
    started = time.time()
    seen = len(device.recent)
    done = ended(device, alarm_id, 90)
    answered = [m for tag, m in device.recent[seen:] if tag == "ChimeRing" and ("solved on the ring screen" in m or "emergency dismiss" in m)]
    report.check(f"{label} ends by itself", done and not answered, f"after {time.time() - started:.0f} s" if done else "still ringing")
    state = device.hook("state")
    report.check(f"{label} nothing sounds", state.get("sounding") == "none", f"sounding={state.get('sounding')}")


def run(serial, args):
    report = Report()
    if not args.no_install:
        install(serial, report)
    device = Device(serial)
    device.wake()
    device.hook("clear")
    state = device.hook("state")
    print(
        f"INFO api={device.sdk} notifications={state.get('notifications')} fullScreen={state.get('fullScreen')} "
        f"emergencyDismiss={state.get('emergencyDismiss')} volume={state.get('volume')}",
        flush=True,
    )
    cases = args.cases.split(",")
    try:
        if state.get("notifications") != "true":
            Report.skip("all", "the plugin may not post notifications: the full screen intent cannot open the ring screen")
            return report.failed
        if "shake" in cases:
            case_shake(device, report)
        if "emergency" in cases:
            case_emergency(device, report)
        if "autostop" in cases:
            case_autostop(device, report)
    finally:
        cleared = device.hook("clear")
        print(f"INFO probes cleared={cleared.get('cleared')}")
        device.wake()
    return report.failed


def clear(serial, args):
    device = Device(serial)
    cleared = device.hook("clear")
    print(f"INFO probes cleared={cleared.get('cleared')}")
    return 0


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("serial")
    sub = parser.add_subparsers(dest="command", required=True)
    run_parser = sub.add_parser("run")
    run_parser.add_argument("--no-install", action="store_true")
    run_parser.add_argument("--cases", default="shake,emergency,autostop")
    sub.add_parser("clear")
    args = parser.parse_args(argv)
    failed = {"run": run, "clear": clear}[args.command](args.serial, args)
    print("RESULT " + ("PASS" if not failed else f"FAIL ({failed})"))
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())

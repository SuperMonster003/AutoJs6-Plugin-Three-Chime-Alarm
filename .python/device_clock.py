"""System clock intent checks of 3-Chime Alarm on one device (roadmap P2.6).

    py .python/device_clock.py <serial> [--no-install] [--keep-test-package]

1. install: `adb install -r -t` of the newest debug APK and its androidTest APK (the plugin keeps its data).
2. resolution: `cmd package query-activities` finds the plugin's ClockIntentActivity for SET_ALARM, SHOW_ALARMS,
   DISMISS_ALARM and SNOOZE_ALARM.
3. shell: the activity requires com.android.alarm.permission.SET_ALARM as the AOSP clock does. Where the shell does
   not hold it (Android 9 of the G8441), `am start` of each action is refused. Where it does (HyperOS 2 / Android 15
   of the Xiaomi Pad), the shell is an outside caller: SET_ALARM with EXTRA_SKIP_UI saves the `p24-probe-clock` alarm
   and the same request enables it instead of adding another, DISMISS_ALARM by its label skips its ring within a day,
   an unknown search mode and snooze duration 0 answer INVALID_OPTIONS, SNOOZE_ALARM without a ring answers
   `ringing=0`. The shell never sends a request that opens a window or may touch an alarm that is not a probe
   (SHOW_ALARMS, SET_ALARM without a time, DISMISS_ALARM without a label). The AutoJs6 host is checked in roadmap P2.7.
4. instrumentation (needs an unlocked screen: behind the keyguard the dialog pages do not show, and HyperOS freezes
   the test process, so a locked screen stops the run before it): ClockIntentActivityTest sends the four actions from the plugin's own uid (saving and enabling,
   the filled and the new alarm page, the alarm list, the too early dismissal and the chooser among several matches,
   refused extras, a ringing alarm snoozed for 1 minute and its round ended); its `p26-probe` alarms are removed. One
   alarm rings for a few seconds at the alarm volume.
5. leftovers: the plugin is started once again (`am instrument` force-stops it), and the androidTest APK is
   uninstalled unless --keep-test-package.

Output: PASS / FAIL / SKIP per item and the key values only. Nothing here answers a system dialog or unlocks the
device.
"""
import argparse
import subprocess
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import device_binder as binder  # noqa: E402
import device_ring as ringing  # noqa: E402
import device_schedule as schedule  # noqa: E402

PLUGIN = schedule.PLUGIN
ACTIVITY = f"{PLUGIN}/.intent.ClockIntentActivity"
TEST = f"{PLUGIN}.ClockIntentActivityTest"
ACTIONS = ["SET_ALARM", "SHOW_ALARMS", "DISMISS_ALARM", "SNOOZE_ALARM"]
PERMISSION = "com.android.alarm.permission.SET_ALARM"
NAMES = {"SET_ALARM": "setAlarm", "SHOW_ALARMS": "showAlarms", "DISMISS_ALARM": "dismissAlarm", "SNOOZE_ALARM": "snoozeAlarm"}
EXTRA = "android.intent.extra.alarm."
LABEL = "p24-probe-clock"


class Device(ringing.Device):
    """The ring device with the ChimeIntent outcome lines in its log. zh-CN: 日志中带 ChimeIntent 结果行的设备."""

    def logs(self):
        tags = (*ringing.TAGS, "ChimeIntent")
        text = ringing.shell(self.serial, "logcat", "-d", "-v", "time", "-s", *(f"{tag}:I" for tag in tags), check=False)
        return [line for line in text.splitlines() if ringing.LINE.match(line)]


def resolves(serial, action):
    out = ringing.shell(serial, "cmd", "package", "query-activities", "--brief", "-a", f"android.intent.action.{action}", check=False)
    return ACTIVITY in out or f"{PLUGIN}/{PLUGIN}.intent.ClockIntentActivity" in out


def shell_holds_permission(serial):
    return f"{PERMISSION}: granted=true" in ringing.shell(serial, "dumpsys", "package", "com.android.shell", check=False)


def am_start(serial, action, *extras):
    out = subprocess.run(["adb", "-s", serial, "shell", "am", "start", "-n", ACTIVITY, "-a", f"android.intent.action.{action}", *extras],
                         capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=60)
    return out.stdout + out.stderr


def shell_refused(serial, action):
    """`am start` of [action] from the shell; answers (refused for the permission, short reason)."""
    text = am_start(serial, action)
    refused = "Permission Denial" in text and PERMISSION in text
    return refused, "Permission Denial (SET_ALARM)" if refused else (text.strip().splitlines() or ["no output"])[-1][:120]


def outcome(device, action, *extras):
    """Sends [action] from the shell and answers the outcome of its ChimeIntent line."""
    text = am_start(device.serial, action, *extras)
    if "Permission Denial" in text or "Error:" in text:
        return "refused by am start"
    name = NAMES[action]
    line = ringing.wait_log(device, lambda tag, m: tag == "ChimeIntent" and m.startswith(name + " "), 15)
    return line[len(name) + 1:] if line else "no outcome in the log"


def probe_state(device):
    for entry in device.hook("state").get("probes", "").split(","):
        parts = entry.split(":")
        if parts[0] == LABEL and len(parts) > 1:
            return {"id": parts[1], **dict(part.split("=", 1) for part in parts[2:] if "=" in part)}
    return None


def shell_intents(serial, report):
    """The shell as an outside caller (it holds SET_ALARM); only requests that open no window and touch only the probe."""
    device = Device(serial)
    device.hook("clear")
    try:
        local = time.localtime(time.time() + 2 * 3600)
        extras = ["--ei", EXTRA + "HOUR", str(local.tm_hour), "--ei", EXTRA + "MINUTES", str(local.tm_min),
                  "--es", EXTRA + "MESSAGE", LABEL, "--ez", EXTRA + "VIBRATE", "false", "--ez", EXTRA + "SKIP_UI", "true"]
        added = outcome(device, "SET_ALARM", *extras)
        report.check("shell SET_ALARM saves", added.startswith("added="), added)
        alarm_id = added.removeprefix("added=")
        again = outcome(device, "SET_ALARM", *extras)
        report.check("shell SET_ALARM again enables the same alarm", again == f"enabled={alarm_id}", again)
        state = probe_state(device)
        report.check("one enabled probe", state is not None and state.get("enabled") == "true", "" if state else "not listed")
        by_label = ["--es", EXTRA + "SEARCH_MODE", "android.label", "--es", EXTRA + "MESSAGE", LABEL]
        dismissed = outcome(device, "DISMISS_ALARM", *by_label)
        report.check("shell DISMISS_ALARM by label skips within a day", dismissed == f"matched=1 {alarm_id}:skipped", dismissed)
        state = probe_state(device)
        report.check("the skipped one-shot probe is disabled", state is not None and state.get("enabled") == "false", "" if state else "not listed")
        unknown = outcome(device, "DISMISS_ALARM", "--es", EXTRA + "SEARCH_MODE", "soon")
        report.check("shell unknown search mode refused", unknown.startswith("error=INVALID_OPTIONS"), unknown[:60])
        zero = outcome(device, "SNOOZE_ALARM", "--ei", EXTRA + "SNOOZE_DURATION", "0")
        report.check("shell snooze duration 0 refused", zero.startswith("error=INVALID_OPTIONS"), zero[:60])
        if device.hook("state").get("ringing", "none") == "none":
            idle = outcome(device, "SNOOZE_ALARM")
            report.check("shell SNOOZE_ALARM without a ring", idle == "ringing=0", idle)
        else:
            report.skip("shell SNOOZE_ALARM without a ring", "an alarm rings")
    finally:
        print(f"INFO p24-probe alarms cleared={device.hook('clear').get('cleared')}")


def probes(serial):
    return [entry for entry in ringing.Device(serial).hook("state").get("probes", "none").split(",") if entry != "none"]


def run(serial, args):
    report = ringing.Report()
    print(f"{serial}: {schedule.adb(serial, 'shell', 'getprop', 'ro.product.model').strip()} (API "
          f"{schedule.adb(serial, 'shell', 'getprop', 'ro.build.version.sdk').strip()})")
    if not args.no_install:
        install = schedule.Report()
        schedule.install(serial, install)
        if not install.ok:
            return False
    schedule.unstop(serial)
    for action in ACTIONS:
        report.check(f"{action} resolves to ClockIntentActivity", resolves(serial, action))
    if shell_holds_permission(serial):
        print("INFO the shell holds SET_ALARM: it calls as an outside app")
        shell_intents(serial, report)
    else:
        for action in ACTIONS:
            refused, reason = shell_refused(serial, action)
            report.check(f"{action} from the shell refused", refused, reason)

    if not report.check("screen unlocked", Device(serial).hook("state").get("keyguard") == "false", "unlock the device, then run again"):
        print(f"{serial}: CLOCK FAIL")
        return False
    try:
        results, summary = binder.instrument(serial, [TEST])
    except subprocess.TimeoutExpired:
        results, summary = {}, ["timed out after 600 s"]
        ringing.shell(serial, "am", "force-stop", PLUGIN, check=False)
    for name, failure in results.items():
        report.check(name, failure is None, "" if failure is None else failure)
    report.check("tests ran", len(results) > 0, f"{len(results)} tests, {' / '.join(summary) or 'no summary'}")
    schedule.unstop(serial)
    left = [entry for entry in probes(serial) if entry.startswith("p26-probe")]
    report.check("no probe alarms left", not left, f"{len(left)} left")
    if not args.keep_test_package:
        out = subprocess.run(["adb", "-s", serial, "uninstall", schedule.TEST_PACKAGE], capture_output=True, text=True, timeout=120)
        report.check("androidTest APK uninstalled", out.returncode == 0)
    ok = report.failed == 0
    print(f"{serial}: CLOCK {'PASS' if ok else 'FAIL'}")
    return ok


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("serial")
    parser.add_argument("--no-install", action="store_true", help="keep the installed builds")
    parser.add_argument("--keep-test-package", action="store_true", help="leave the androidTest APK installed")
    args = parser.parse_args(argv)
    return 0 if run(args.serial, args) else 1


if __name__ == "__main__":
    sys.exit(main())

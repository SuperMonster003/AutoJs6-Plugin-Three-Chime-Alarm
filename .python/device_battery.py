"""Standby cost of 3-Chime Alarm on one device (roadmap P5.6): two equal screen-off windows on the same device, the first
with three enabled alarms that are not due during the window, the second without alarms.

    py .python/device_battery.py <serial> [--hours 4] [--out DIR]

The device stays on USB, so the battery is not really drained: the run simulates the unplug (`dumpsys battery unplug`)
and resets `batterystats` at the start of each window, so that the system estimates the drain and lets the device doze.
At the end of each window it saves the `batterystats` dump to DIR and prints the estimated whole-device drain (mAh), the
plugin uid's estimated power (mAh), its wake-up alarm count and its CPU time. The alarms are probes of the plugin's
debug hook (one-shot, silent, due 11 - 12 hours after the start; removed at the end), so the host is not needed and the
device may stay locked. The battery state, the stay-awake setting and the screen are left as they were found.

Output: one line per window with the key numbers only. Nothing here answers a dialog or unlocks the device.
"""
import argparse
import base64
import json
import re
import sys
import time
from datetime import datetime
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import device_p5 as p5  # noqa: E402
import device_smoke as smoke  # noqa: E402

PLUGIN = smoke.PLUGIN


def adb(serial, *args, timeout=300):
    return smoke.adb(serial, *args, check=False, timeout=timeout)


def uid_name(serial):
    found = re.search(r"userId=(\d+)", adb(serial, "shell", "dumpsys", "package", PLUGIN))
    if not found:
        return None
    uid = int(found.group(1))
    return f"u0a{uid - 10000}" if uid >= 10000 else str(uid)


def summary(text, uid):
    """Whole-device computed drain, the plugin uid's estimate, wake-up alarms and CPU time from a batterystats dump."""
    drain = re.search(r"Computed drain: ([\d.]+)", text)
    power = re.search(rf"^\s+UID {uid}: ([\d.]+)", text, re.M)
    block = re.search(rf"^  {uid}:\n((?:    .*\n)+)", text, re.M)
    wakeups = cpu = None
    if block:
        wakeups = sum(int(n) for n in re.findall(r"Wakeup alarm .*?: (\d+) times", block.group(1)))
        found = re.search(r"Total cpu time: u=(.*?) s=(.*?)\s*$", block.group(1), re.M)
        cpu = f"u={found.group(1)} s={found.group(2)}" if found else None
    return {
        "computedDrainMah": float(drain.group(1)) if drain else None,
        "pluginMah": float(power.group(1)) if power else 0.0,
        "pluginWakeupAlarms": wakeups or 0,
        "pluginCpu": cpu,
    }


def window(serial, name, hours, out, uid, report):
    adb(serial, "shell", "dumpsys", "batterystats", "--reset")
    started = datetime.now().strftime("%H:%M")
    time.sleep(hours * 3600)
    text = adb(serial, "shell", "dumpsys", "batterystats", timeout=600)
    (out / f"batterystats-{name}.txt").write_text(text, encoding="utf-8")
    values = summary(text, uid)
    report.line(f"window {name} ({started} - {datetime.now().strftime('%H:%M')}, {hours} h)", "PASS" if values["computedDrainMah"] is not None else "FAIL",
                f"device computed drain {values['computedDrainMah']} mAh, plugin {values['pluginMah']} mAh, wake-up alarms {values['pluginWakeupAlarms']}, cpu {values['pluginCpu']}")
    return values


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("serial")
    parser.add_argument("--hours", type=float, default=4)
    parser.add_argument("--out", default=".")
    args = parser.parse_args(argv)
    serial, out = args.serial, Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    report = p5.Report()
    print(f"{smoke.label(serial)}: battery windows of {args.hours} h", flush=True)
    uid = uid_name(serial)
    stay = adb(serial, "shell", "settings", "get", "global", "stay_on_while_plugged_in").strip()
    # The simulated plug-in at the end would play the charging sound; it is off for the run and restored afterwards.
    chime = adb(serial, "shell", "settings", "get", "secure", "charging_sounds_enabled").strip()
    options = base64.b64encode(json.dumps({"sound": {"source": "silent"}, "vibrate": False}).encode()).decode()
    results = {}
    try:
        adb(serial, "shell", "settings", "put", "secure", "charging_sounds_enabled", "0")
        adb(serial, "shell", "settings", "put", "global", "stay_on_while_plugged_in", "0")
        for i, hours in enumerate((11, 11.5, 12)):
            answer = p5.hook(serial, "add", label=f"p24-probe-battery-{i}", inSeconds=int(hours * 3600), options64=options)
            report.line(f"alarm {i} enabled, due in {hours} h", "PASS" if "added" in answer else "FAIL", answer.get("added", str(answer)[:80]))
        adb(serial, "shell", "input", "keyevent", "KEYCODE_SLEEP")
        adb(serial, "shell", "dumpsys", "battery", "unplug")
        results["alarms"] = window(serial, "alarms", args.hours, out, uid, report)
        cleared = p5.hook(serial, "clear")
        report.line("alarms removed", "PASS" if cleared.get("cleared") == "3" else "FAIL", f"cleared {cleared.get('cleared')}")
        adb(serial, "shell", "input", "keyevent", "KEYCODE_SLEEP")
        results["none"] = window(serial, "none", args.hours, out, uid, report)
    finally:
        adb(serial, "shell", "dumpsys", "battery", "reset")
        time.sleep(5)
        adb(serial, "shell", "settings", "put", "global", "stay_on_while_plugged_in", stay)
        if chime and chime != "null":
            adb(serial, "shell", "settings", "put", "secure", "charging_sounds_enabled", chime)
        p5.hook(serial, "clear")
    (out / "battery-summary.json").write_text(json.dumps(results, indent=1), encoding="utf-8")
    a, n = results.get("alarms") or {}, results.get("none") or {}
    if a.get("computedDrainMah") is not None and n.get("computedDrainMah") is not None:
        print(f"    difference (alarms - none): device {a['computedDrainMah'] - n['computedDrainMah']:+.1f} mAh, plugin {a['pluginMah'] - n['pluginMah']:+.2f} mAh", flush=True)
    print(f"{serial}: P5.6 battery {'PASS' if report.failed == 0 else 'FAIL'} ({report.passed} passed, {report.failed} failed)", flush=True)
    return 0 if report.failed == 0 else 1


if __name__ == "__main__":
    sys.exit(main())

"""Permissions page walk of 3-Chime Alarm on one device (roadmap P3.4).

    py .python/device_permissions.py <serial> [--no-install] [--keep-test-package] [--rows all|tag,...]

1. install: `adb install -r -t` of the newest debug APK and its androidTest APK (the plugin keeps its data and its
   granted permissions; nothing is uninstalled before).
2. screen unlocked (the debug hook's keyguard state): behind the keyguard the pages do not show.
3. direct landing: `am start` of the frozen OPEN action with `page=permissions`, as `alarms.openPlugin('permissions')`
   sends it, resumes `.ui.permissions.PermissionsActivity`.
4. PermissionsJumpProbe (androidTest): every row's state, the alarm list banner shown exactly while a row is
   missing and opening the page, the OPEN action again, then a tap on each tappable row (all by default) with the
   system page it opened: the page the row chose and the component on top; a fallback to the app details is marked.
   The notification row is skipped (SKIP) while it would show the runtime permission dialog, which only the
   maintainer answers on the device.
5. the plugin is started once again (`am instrument` force-stops it) and the androidTest APK is uninstalled unless
   --keep-test-package (the plugin stays installed).

Output: PASS / FAIL / SKIP per item and the key values only. Nothing here answers a system dialog, taps the screen
outside the plugin's own page or changes a permission; nothing rings.
"""
import argparse
import re
import subprocess
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import device_ring as ringing  # noqa: E402
import device_schedule as schedule  # noqa: E402

PLUGIN = schedule.PLUGIN
PROBE = f"{PLUGIN}.PermissionsJumpProbe#walk"
PAGE = f"{PLUGIN}/.ui.permissions.PermissionsActivity"
OPEN = "org.autojs.plugin.alarm.OPEN"
RESUMED = re.compile(r"[Rr]esumedActivity[:=]\s*ActivityRecord\{\S+ u\d+ ([^\s}]+)")


def top_activity(serial):
    found = RESUMED.search(schedule.shell(serial, "dumpsys", "activity", "activities", check=False))
    return found.group(1) if found else None


def line(report, step, state, text=""):
    if state == "SKIP":
        print(f"    {step}: SKIP {text}".rstrip())
        return True
    return report.check(step, state == "PASS", text)


def probe(serial, rows):
    out = schedule.adb(serial, "shell", "am", "instrument", "-w", "-r", "-e", "class", PROBE, "-e", "chimePermissions", rows,
                       schedule.RUNNER, check=False, timeout=300)
    values = dict(re.findall(r"INSTRUMENTATION_STATUS: (chime\.[\w.-]+)=(.*)", out))
    failure = (re.findall(r"INSTRUMENTATION_STATUS: stack=(.*)", out) or [""])[0][:200]
    return values, failure


def run(serial, args):
    report = schedule.Report()
    sdk = int(schedule.shell(serial, "getprop", "ro.build.version.sdk"))
    print(f"{serial}: {schedule.shell(serial, 'getprop', 'ro.product.model')} "
          f"({schedule.shell(serial, 'getprop', 'ro.product.manufacturer')}, API {sdk})")
    if not args.no_install:
        schedule.install(serial, report)
        if not report.ok:
            return False
    try:
        keyguard = ringing.Device(serial).hook("state").get("keyguard")
        if not report.check("screen unlocked", keyguard == "false", "unlock the device, then run again"):
            return False

        schedule.shell(serial, "am", "start", "-W", "-a", OPEN, "-p", PLUGIN, "--es", "page", "permissions", check=False)
        deadline = time.time() + 10
        top = top_activity(serial)
        while top != PAGE and time.time() < deadline:
            time.sleep(0.5)
            top = top_activity(serial)
        report.check("openPlugin('permissions') lands on the page", top == PAGE, f"top {top}")
        schedule.shell(serial, "input", "keyevent", "KEYCODE_HOME", check=False)

        values, failure = probe(serial, args.rows)
        if not values:
            report.check("PermissionsJumpProbe", False, failure or "no output")
            return False
        rows = [row for row in values.get("chime.rows", "").split(",") if row]
        missing = [row for row in values.get("chime.missing", "").split(",") if row]
        print(f"    rows (API {values.get('chime.sdk')}): " + ", ".join(f"{row}={values.get(f'chime.state.{row}')}" for row in rows))
        banner = values.get("chime.banner") == "true"
        report.check("alarm list banner shown exactly while a row is missing", banner == bool(missing),
                     f"banner {banner}, missing [{', '.join(missing)}]")
        if banner:
            report.check("banner opens the page", values.get("chime.bannerOpens") == "true")
        report.check("OPEN action reaches the page from the plugin", values.get("chime.direct") == "true")
        for row in rows:
            jump = values.get(f"chime.jump.{row}")
            if jump is None:
                continue
            if jump == "notActionable":
                line(report, f"jump {row}", "SKIP", "not tappable on this version")
            elif jump.startswith("skipped:"):
                line(report, f"jump {row}", "SKIP", "would show the runtime permission dialog (the maintainer answers it)")
            else:
                parts = jump.split("|")
                opened, landed = parts[0], parts[1] if len(parts) > 1 else "unknown"
                left = landed != "unknown" and not landed.startswith(f"{PLUGIN}/")
                note = " (fallback to the app details)" if "fallback" in parts else ""
                line(report, f"jump {row}", "PASS" if left and opened != "none" else "FAIL", f"{opened} -> {landed}{note}")
    finally:
        schedule.shell(serial, "input", "keyevent", "KEYCODE_HOME", check=False)
        schedule.unstop(serial)
        if not args.keep_test_package:
            out = subprocess.run(["adb", "-s", serial, "uninstall", schedule.TEST_PACKAGE], capture_output=True, text=True, timeout=120)
            report.check("androidTest APK uninstalled", out.returncode == 0)
    print(f"{serial}: PERMISSIONS {'PASS' if report.ok else 'FAIL'}")
    return report.ok


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("serial")
    parser.add_argument("--no-install", action="store_true", help="keep the installed builds")
    parser.add_argument("--keep-test-package", action="store_true", help="leave the androidTest APK installed")
    parser.add_argument("--rows", default="all", help="the rows to tap: all, or tags such as exactAlarm,battery")
    args = parser.parse_args(argv)
    return 0 if run(args.serial, args) else 1


if __name__ == "__main__":
    sys.exit(main())

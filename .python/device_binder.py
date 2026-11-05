"""Binder service checks of 3-Chime Alarm on one device (roadmap P2.5).

    py .python/device_binder.py <serial> [--no-install] [--keep-test-package]

1. install: `adb install -r -t` of the newest debug APK and its androidTest APK (the plugin keeps its data).
2. tests: `am instrument` of ThreeChimeAlarmContractTest (discovery, binding, descriptors, getInfo, capabilities) and
   AlarmPluginBinderTest (add -> get -> list -> update -> setAlarmEnabled -> remove through Parcels with the change
   events, refused requests and their error codes, limits, UNSUPPORTED methods, bounded and released event callbacks,
   ringtone imports that close the descriptor and outlive the original file, binding hints, repeated binds that leave no
   descriptor open); one PASS / FAIL line per test, a failure with its first line only.
3. leftovers: the plugin's `dumpsys alarm` entries before and after are the same count (the tests remove their
   `p25-probe` alarms), and the plugin is started once again (`am instrument` force-stops it, and a stopped package
   gets no boot or clock broadcasts).
4. the androidTest APK is uninstalled unless --keep-test-package (the plugin stays installed).

Output: PASS / FAIL per item and the key numbers only. Nothing here answers a system dialog; unlock the device first.
"""
import argparse
import re
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import device_schedule as schedule  # noqa: E402

PLUGIN = schedule.PLUGIN
TESTS = [f"{PLUGIN}.ThreeChimeAlarmContractTest", f"{PLUGIN}.AlarmPluginBinderTest"]


def instrument(serial, classes=TESTS):
    """Runs the test [classes] and answers {test: None (passed) | first failure line} and the runner's summary."""
    out = schedule.adb(serial, "shell", "am", "instrument", "-w", "-r", "-e", "class", ",".join(classes), schedule.RUNNER,
                       check=False, timeout=600)
    results, current = {}, {}
    for line in out.splitlines():
        status = re.match(r"INSTRUMENTATION_STATUS: (\w+)=(.*)", line)
        if status:
            current[status.group(1)] = status.group(2)
            continue
        code = re.match(r"INSTRUMENTATION_STATUS_CODE: (-?\d+)", line)
        if code and "test" in current:
            name = f"{current.get('class', '').rsplit('.', 1)[-1]}.{current['test']}"
            value = int(code.group(1))
            if value == 0:
                results[name] = None
            elif value in (-1, -2):
                results[name] = (current.get("stack", "").splitlines() or ["failed"])[0][:200]
            elif value == -3:
                results[name] = "skipped"
            current = {}
    summary = re.findall(r"^(OK \(\d+ tests?\)|FAILURES!!!|Tests run: .*|INSTRUMENTATION_FAILED: .*)$", out, re.M)
    return results, summary


def run(serial, args):
    report = schedule.Report()
    print(f"{serial}: {schedule.adb(serial, 'shell', 'getprop', 'ro.product.model')} (API "
          f"{schedule.adb(serial, 'shell', 'getprop', 'ro.build.version.sdk')})")
    if not args.no_install:
        schedule.install(serial, report)
        if not report.ok:
            return False
    before = len(schedule.scheduled(serial)[0])
    results, summary = instrument(serial)
    for name, failure in results.items():
        report.check(name, failure is None, "" if failure is None else failure)
    report.check("tests ran", len(results) > 0, f"{len(results)} tests, {' / '.join(summary) or 'no summary'}")
    schedule.unstop(serial)
    after = None
    for _ in range(10):
        after = len(schedule.scheduled(serial)[0])
        if after == before:
            break
        schedule.time.sleep(2)
    report.check("alarm entries unchanged", after == before, f"plugin entries in dumpsys alarm: {before} before, {after} after")
    if not args.keep_test_package:
        out = subprocess.run(["adb", "-s", serial, "uninstall", schedule.TEST_PACKAGE], capture_output=True, text=True, timeout=120)
        report.check("androidTest APK uninstalled", out.returncode == 0)
    print(f"{serial}: BINDER {'PASS' if report.ok else 'FAIL'}")
    return report.ok


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("serial")
    parser.add_argument("--no-install", action="store_true", help="keep the installed builds")
    parser.add_argument("--keep-test-package", action="store_true", help="leave the androidTest APK installed")
    return 0 if run(parser.parse_args(argv).serial, parser.parse_args(argv)) else 1


if __name__ == "__main__":
    sys.exit(main())

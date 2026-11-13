"""Real holiday data refresh of 3-Chime Alarm on one device (roadmap P8.2, T172 - T175).

    py .python/device_holidays.py <serial> [--no-install] [--keep-test-package]

1. install: `adb install -r -t` of the newest debug APK and its androidTest APK (the plugin keeps its data).
2. refresh: `am instrument` of HolidayRefreshProbe with `-e chimeHolidayRefresh 1`. In the plugin process the probe binds
   the capability service as the host does, reads `holidayInfo` and the `holidayDataVersion` capability, runs
   `refreshHolidays` over the network (the signed catalog on raw.githubusercontent.com, changed calendar files from the
   GitHub release; an unchanged catalog downloads no file) and reads both again.
3. checks: the refresh completed within 120 s without an error, the capability names the data version after it, and
   the catalog version did not go back. A newer published catalog replaces the plugin's data, as the weekly check would.
4. the plugin is started once again (`am instrument` force-stops it, and a stopped package gets no boot or clock
   broadcasts), and the androidTest APK is uninstalled unless --keep-test-package (the plugin stays installed).

Output: PASS / FAIL per item and the key numbers only (versions, source, progress, time). The device needs network.
"""

import argparse
import re
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import device_schedule as schedule  # noqa: E402

PROBE = f"{schedule.PLUGIN}.HolidayRefreshProbe#refresh"


def refresh(serial):
    """Runs the probe and answers its `chime.*` report and the runner's first failure line (None when it passed)."""
    out = schedule.adb(serial, "shell", "am", "instrument", "-w", "-r", "-e", "class", PROBE, "-e", "chimeHolidayRefresh", "1",
                       schedule.RUNNER, check=False, timeout=300)
    values = dict(re.findall(r"INSTRUMENTATION_STATUS: (chime\.\w+)=(.*)", out))
    stack = re.findall(r"INSTRUMENTATION_STATUS: stack=(.*)", out)
    passed = re.search(r"^OK \(1 test\)$", out, re.M) is not None
    failure = None if passed else (stack or re.findall(r"^(INSTRUMENTATION_FAILED: .*|Tests run: .*)$", out, re.M) or ["no result"])[0][:200]
    return values, failure


def run(serial, args):
    report = schedule.Report()
    print(f"{serial}: {schedule.adb(serial, 'shell', 'getprop', 'ro.product.model')} (API "
          f"{schedule.adb(serial, 'shell', 'getprop', 'ro.build.version.sdk')})")
    if not args.no_install:
        schedule.install(serial, report)
        if not report.ok:
            return False
    values, failure = refresh(serial)
    report.check("probe ran", values.get("chime.done") == "true", failure or "")
    if values:
        print(f"    before: {values.get('chime.before')} (capability {values.get('chime.capabilityBefore')})")
        report.check("refresh completed", values.get("chime.outcome") == "completed",
                     f"{values.get('chime.outcome')}, {values.get('chime.elapsedMs')} ms, progress {values.get('chime.progress')}")
        if "chime.answer" in values:
            print(f"    answer: {values['chime.answer']}")
        print(f"    after: {values.get('chime.after')} (capability {values.get('chime.capabilityAfter')})")
        after = re.search(r"version=(\S+)", values.get("chime.after", ""))
        report.check("capability names the data version", after is not None and after.group(1) == values.get("chime.capabilityAfter"))
    report.check("probe assertions", failure is None, failure or "")
    schedule.unstop(serial)
    if not args.keep_test_package:
        out = subprocess.run(["adb", "-s", serial, "uninstall", schedule.TEST_PACKAGE], capture_output=True, text=True, timeout=120)
        report.check("androidTest APK uninstalled", out.returncode == 0)
    print(f"{serial}: HOLIDAY REFRESH {'PASS' if report.ok else 'FAIL'}")
    return report.ok


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("serial")
    parser.add_argument("--no-install", action="store_true", help="keep the installed builds")
    parser.add_argument("--keep-test-package", action="store_true", help="leave the androidTest APK installed")
    args = parser.parse_args(argv)
    return 0 if run(args.serial, args) else 1


if __name__ == "__main__":
    sys.exit(main())

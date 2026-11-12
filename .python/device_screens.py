"""Screenshots of 3-Chime Alarm on one device (roadmap P3.6 and the E4 checks of P3.1 - P3.5 and P7.4).

    py .python/device_screens.py <serial> --out DIR [--screens all|pages|clock|notice|ring] [--modes light,dark] [--no-install]
                                 [--keep-test-package] [--direct-seconds N]

1. install: `adb install -r -t` of the newest debug APK and its androidTest APK (the plugin keeps its data and its
   granted permissions; nothing is uninstalled before).
2. screen unlocked unless only `ring` is asked for (the debug hook's keyguard state): behind the keyguard the pages
   do not show.
3. ScreenshotProbe (androidTest): adds four sample alarms, sets the plugin night mode to each mode in turn and captures
   the alarm list, the editor, the ringtones page and the settings page (`pages`), the timers page, its custom duration
   dialog and the stopwatch page with three sample timers and, while the stopwatch is idle, four sample laps (`clock`,
   1.1.0), the ring notification while the
   device is in use on its home screen with the expanded shade, once in the system theme (`notice`), then the ringing
   screen on the lock screen (`ring`: the screen is turned off before a silent sample alarm rings; `direct` marks a
   screen started directly because the full-screen intent did not show it). It restores the night mode, removes the
   samples and resets a stopwatch it started.
   The pages that read the ringtones in the background are captured once their rows are enabled (the time is printed).
4. the captures are pulled into DIR as `<page>-<mode>.png` and removed from the device; the plugin is started once
   again (`am instrument` force-stops it) and the androidTest APK is uninstalled unless --keep-test-package.

Output: PASS / FAIL per capture with its size, plus the alarms the list shows besides the samples. After `ring` the device
stays at its lock screen, which only the maintainer unlocks. Nothing rings aloud and nothing vibrates.
"""
import argparse
import re
import struct
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import device_ring as ringing  # noqa: E402
import device_schedule as schedule  # noqa: E402

PLUGIN = schedule.PLUGIN
PROBE = f"{PLUGIN}.ScreenshotProbe#capture"
DEVICE_DIR = "/data/local/tmp/chime-screens"


def png_size(path):
    with open(path, "rb") as file:
        head = file.read(24)
    if len(head) < 24 or head[:8] != b"\x89PNG\r\n\x1a\n":
        return None
    return struct.unpack(">II", head[16:24])


def probe(serial, screens, modes, direct_seconds=None):
    extra = ["-e", "chimeRingDirectSeconds", str(direct_seconds)] if direct_seconds else []
    out = schedule.adb(serial, "shell", "am", "instrument", "-w", "-r", "-e", "class", PROBE, "-e", "chimeScreens", screens,
                       "-e", "chimeModes", modes, *extra, schedule.RUNNER, check=False, timeout=900 + 2 * (direct_seconds or 0))
    values = dict(re.findall(r"INSTRUMENTATION_STATUS: (chime\.[\w.-]+)=(.*)", out))
    failure = (re.findall(r"INSTRUMENTATION_STATUS: stack=(.*)", out) or [""])[0][:200]
    return values, failure


def run(serial, args):
    report = schedule.Report()
    sdk = int(schedule.shell(serial, "getprop", "ro.build.version.sdk"))
    print(f"{serial}: {schedule.shell(serial, 'getprop', 'ro.product.model')} "
          f"({schedule.shell(serial, 'getprop', 'ro.product.manufacturer')}, API {sdk}, "
          f"{schedule.shell(serial, 'wm', 'size').split(':')[-1].strip()})")
    out_dir = Path(args.out)
    out_dir.mkdir(parents=True, exist_ok=True)
    if not args.no_install:
        schedule.install(serial, report)
        if not report.ok:
            return False
    try:
        if args.screens != "ring":
            keyguard = ringing.Device(serial).hook("state").get("keyguard")
            if not report.check("screen unlocked", keyguard == "false", "" if keyguard == "false" else "unlock the device, then run again"):
                return False
        values, failure = probe(serial, args.screens, args.modes, args.direct_seconds)
        if not values:
            report.check("ScreenshotProbe", False, failure or "no output")
            return False
        if failure:
            report.check("ScreenshotProbe finished", False, failure)
        if "chime.error" in values:
            report.check("ScreenshotProbe without errors", False, values["chime.error"])
        others = int(values.get("chime.others", "0"))
        print(f"    language {values.get('chime.language')}, alarms besides the samples: {others}"
              + (" (they show in the list captures)" if others else ""))
        if "chime.timers.others" in values:
            print(f"    timers besides the samples: {values['chime.timers.others']}, "
                  f"sample laps: {'yes' if values.get('chime.stopwatch.sample') == 'true' else 'no (the stopwatch was in use)'}")
        for key in sorted(k for k in values if k.startswith("chime.shot.")):
            name, where = key[len("chime.shot."):], values[key]
            if where.startswith("error:"):
                report.check(name, False, where[len("error:"):])
                continue
            target = out_dir / f"{name}.png"
            pulled = subprocess.run(["adb", "-s", serial, "pull", where, str(target)], capture_output=True, text=True, timeout=120)
            size = png_size(target) if pulled.returncode == 0 and target.exists() else None
            mode = name.rsplit("-", 1)[-1]
            how = ""
            if name.startswith("ring-"):
                how = (f", {values.get(f'chime.ring.{mode}')} (full-screen allowed {values.get(f'chime.fullScreen.{mode}')}, "
                       f"notification carries it {values.get(f'chime.fullScreenIntent.{mode}')})")
            if f"chime.ready.{name}" in values:
                how += f", rows enabled after {values[f'chime.ready.{name}']} ms"
            if name == "notice-system":
                how += ", ring screen opened instead" if values.get("chime.notice.screen") == "true" else ", heads-up"
            report.check(name, size is not None, f"{size[0]}x{size[1]}, {target.stat().st_size // 1024} KiB{how}" if size else "pull failed")
        for key in sorted(k for k in values if k.startswith("chime.record.")):
            print(f"    ring notification record ({key[len('chime.record.'):]}): {values[key]}")
        report.check("night mode restored", values.get("chime.restored") == "true")
    finally:
        schedule.shell(serial, "rm", "-rf", DEVICE_DIR, check=False)
        schedule.unstop(serial)
        if not args.keep_test_package:
            out = subprocess.run(["adb", "-s", serial, "uninstall", schedule.TEST_PACKAGE], capture_output=True, text=True, timeout=120)
            report.check("androidTest APK uninstalled", out.returncode == 0)
    if args.screens in ("ring", "all"):
        print("    note: the device is at its lock screen now")
    print(f"{serial}: SCREENS {'PASS' if report.ok else 'FAIL'}")
    return report.ok


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("serial")
    parser.add_argument("--out", required=True, help="the directory for the captures")
    parser.add_argument("--screens", default="all", choices=["all", "pages", "clock", "notice", "ring"])
    parser.add_argument("--modes", default="light,dark", help="plugin night modes, from light and dark")
    parser.add_argument("--direct-seconds", type=int, help="wait this long for a ring screen started directly (a system "
                                                                 "lock screen prompt answered by the maintainer)")
    parser.add_argument("--no-install", action="store_true", help="keep the installed builds")
    parser.add_argument("--keep-test-package", action="store_true", help="leave the androidTest APK installed")
    args = parser.parse_args(argv)
    return 0 if run(args.serial, args) else 1


if __name__ == "__main__":
    sys.exit(main())

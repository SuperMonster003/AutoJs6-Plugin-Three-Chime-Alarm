"""Skeleton smoke run of 3-Chime Alarm on one device (roadmap P0.4; roadmap P4.1 and P5 extend it to the 1.0.0 checks).

    py .python/device_smoke.py <serial> [--apk <file>] [--no-install] [--screenshot <dir>] [--contract [--incompatible-apk <file>]]

1. host: the AutoJs6 versionCode, compared with requiresHostVersion (reported only: the INFO service answers any host,
   the `alarms` module of roadmap P1.4 needs the required build).
2. install: `adb install -r` of the APK, by default the newest debug APK of app/build/outputs/apk/debug, else the newest
   APK of releases/ (debug and release builds share the signing key, so the update keeps data). ColorOS and MIUI /
   HyperOS show a confirmation screen for USB installs: the maintainer confirms it on the phone.
3. discovery: `cmd package query-services` resolves exactly one service of the plugin for org.autojs.plugin.INFO /
   three-chime-alarm and for org.autojs.plugin.ALARM / alarm.
4. activation and getInfo: an AutoJs6 script (RunIntentActivity, as in 3-Macaw Speech) starts the Wake activity (it
   needs org.autojs.permission.PLUGIN, which the shell lacks), binds the INFO service explicitly and calls getInfo(),
   then binds the ALARM service, checks the IAlarmPlugin descriptor and, on a host with the alarm contract, reads the
   capabilities (alarmFeatures [alarms, timers, stopwatch] since 1.1.0, the permission states) and calls listAlarms once
   (roadmap P2.5, P7.3). The
   Wake start is confirmed from the ActivityManager log lines after a marker.
5. --screenshot <dir>: the script opens AutoJs6's Plugin Center and the screen is saved as
   plugin-center-<serial>-system-<light|dark>.png. The name records the system night mode, which picks the launcher
   icon variant; the Plugin Center itself follows the AutoJs6 theme setting, so check the picture for its theme. The
   launcher differs per vendor, so its screenshot stays manual.
6. --contract (roadmap P2.7, see device_contract.py): before the install, while the plugin is not installed, the
   `alarms` module answers PLUGIN_UNAVAILABLE; after the smoke, the appendix A section 12 examples, the error codes,
   a ring with listeners and a bound script, PLUGIN_DISABLED, PERMISSION_REQUIRED (API 31 - 32) and, with
   --incompatible-apk, PLUGIN_INCOMPATIBLE run through the AutoJs6 `alarms` module (host versionCode 6641 or later).

Output: PASS / FAIL per step and the key values; no description text, token or tool list is printed.
A secure lock screen keeps activities from starting: unlock the device first. Nothing here answers a system dialog.
"""
import argparse
import hashlib
import json
import re
import subprocess
import sys
import tempfile
import time
import uuid
from pathlib import Path

HOST = "org.autojs.autojs6"
PLUGIN = "io.github.supermonster003.autojs6.plugin.three.chime.alarm"
PLUGIN_ID = "three-chime-alarm"
REMOTE = "/sdcard/Download/autojs6-chime-run"
ROOT = Path(__file__).resolve().parent.parent
DISCOVERY = (
    ("org.autojs.plugin.INFO", PLUGIN_ID, f"{PLUGIN}/.service.ThreeChimeAlarmInfoService"),
    ("org.autojs.plugin.ALARM", "alarm", f"{PLUGIN}/.service.ThreeChimeAlarmService"),
)
CAPABILITY_DESCRIPTOR = "org.autojs.plugin.alarm.api.IAlarmPlugin"
# T25: the features the plugin declares, alarms since 1.0.0, timers and stopwatch since 1.1.0 (roadmap P7.3).
FEATURES = "alarms,timers,stopwatch"
PLUGIN_CENTER = "org.autojs.autojs.core.plugin.center.PluginCenterActivity"

SCRIPT = """
var out = "%(out)s";
var pkg = "%(pkg)s";
var r = { steps: {}, errors: [] };
files.write(out + ".started", String(Date.now()));
function done() { files.write(out, JSON.stringify(r)); }
function attempt(name, fn) {
    try { r.steps[name] = fn(); } catch (e) { r.steps[name] = { error: String(e.message || e) }; }
}
var Intent = android.content.Intent, ComponentName = android.content.ComponentName;
var TimeUnit = java.util.concurrent.TimeUnit;
var bound = [];
function bind(cls) {
    var latch = new java.util.concurrent.CountDownLatch(1);
    var ref = { binder: null };
    var connection = new android.content.ServiceConnection({
        onServiceConnected: function (name, binder) { ref.binder = binder; latch.countDown(); },
        onServiceDisconnected: function (name) {}
    });
    var intent = new Intent().setComponent(new ComponentName(pkg, pkg + cls));
    if (!context.bindService(intent, connection, android.content.Context.BIND_AUTO_CREATE)) throw new Error("bindService false " + cls);
    bound.push(connection);
    if (!latch["await"](10, TimeUnit.SECONDS) || ref.binder == null) throw new Error("no binder " + cls);
    return ref.binder;
}
attempt("wake", function () {
    var t = Date.now();
    context.startActivity(new Intent().setComponent(new ComponentName(pkg, pkg + ".WakeActivity")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    sleep(1500);
    return { ms: Date.now() - t };
});
attempt("info", function () {
    var t = Date.now();
    var binder = bind(".service.ThreeChimeAlarmInfoService");
    var descriptor = String(binder.getInterfaceDescriptor());
    var info;
    try {
        info = Packages.org.autojs.plugin.common.api.IPluginInfoProvider.Stub.asInterface(binder).getInfo();
    } catch (e) {
        // A host without the generated proxy class: the same call as a raw Parcel (getInfo is the first transaction).
        var data = android.os.Parcel.obtain(), reply = android.os.Parcel.obtain();
        data.writeInterfaceToken(descriptor);
        binder.transact(android.os.IBinder.FIRST_CALL_TRANSACTION, data, reply, 0);
        reply.readException();
        info = reply.readInt() !== 0 ? Packages.org.autojs.plugin.common.api.PluginInfo.CREATOR.createFromParcel(reply) : null;
        data.recycle(); reply.recycle();
    }
    if (info == null) throw new Error("getInfo returned null");
    var caps = info.capabilities;
    var keys = [];
    if (caps != null) { var it = caps.keySet().iterator(); while (it.hasNext()) keys.push(String(it.next())); }
    keys.sort();
    return {
        ms: Date.now() - t, descriptor: descriptor, name: String(info.name), id: String(info.id), engine: String(info.engine),
        variant: String(info.variant), versionName: String(info.versionName), versionCode: Number(info.versionCode),
        versionDate: String(info.versionDate), descriptionChars: info.description == null ? -1 : String(info.description).length,
        abis: info.supportedAbis == null ? -1 : info.supportedAbis.length, capabilityKeys: keys.join(","),
        requiresHostVersion: caps == null ? -1 : Number(caps.getLong("requiresHostVersion", -1)),
        contractVersion: caps == null ? -1 : caps.getInt("alarmContractVersion", -1),
        features: caps == null || caps.getStringArray("alarmFeatures") == null ? -1 : caps.getStringArray("alarmFeatures").length
    };
});
attempt("alarm", function () {
    var binder = bind(".service.ThreeChimeAlarmService");
    var out = { descriptor: String(binder.getInterfaceDescriptor()) };
    var plugin = null;
    try { plugin = Packages.org.autojs.plugin.alarm.api.IAlarmPlugin.Stub.asInterface(binder); } catch (e) { plugin = null; }
    if (plugin != null) {
        // A host with the alarm contract (roadmap P1.1): the capabilities and one listAlarms call through the proxy.
        var caps = plugin.getCapabilities();
        var features = caps.getStringArray("alarmFeatures");
        var names = [];
        if (features != null) for (var i = 0; i < features.length; i++) names.push(String(features[i]));
        out.features = names.join(",");
        var permissions = caps.getBundle("alarmPermissions");
        out.permissions = permissions == null ? "" : ["exactAlarm", "notifications", "fullScreen"].map(function (k) {
            return k + "=" + permissions.getBoolean(k);
        }).join(",");
        var list = plugin.listAlarms(null);
        out.listError = list.getString("errorCode") == null ? "" : String(list.getString("errorCode"));
        out.proxy = true;
    } else {
        // An older host: getInfo of IAlarmPlugin (the first transaction) as a raw Parcel.
        var data = android.os.Parcel.obtain(), reply = android.os.Parcel.obtain();
        data.writeInterfaceToken("%(descriptor)s");
        out.firstCallHandled = binder.transact(android.os.IBinder.FIRST_CALL_TRANSACTION, data, reply, 0);
        data.recycle(); reply.recycle();
        out.proxy = false;
    }
    return out;
});
bound.forEach(function (c) { try { context.unbindService(c); } catch (e) { r.errors.push("unbind " + e); } });
if (%(center)s) {
    attempt("pluginCenter", function () {
        context.startActivity(new Intent().setClassName("%(host)s", "%(center_class)s").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        return { started: true };
    });
}
done();
"""


def adb(serial, *args, check=True, timeout=120):
    return subprocess.run(["adb", "-s", serial, *args], capture_output=True, text=True, encoding="utf-8", errors="replace",
                          check=check, timeout=timeout).stdout.strip()


def label(serial):
    return f"{adb(serial, 'shell', 'getprop', 'ro.product.model')} ({serial}, API {adb(serial, 'shell', 'getprop', 'ro.build.version.sdk')})"


def sha256(path):
    digest = hashlib.sha256()
    with open(path, "rb") as stream:
        while chunk := stream.read(1024 * 1024):
            digest.update(chunk)
    return digest.hexdigest()


def required_host_version():
    match = re.search(r"^val requiredHostVersion = (\d+)$", (ROOT / "app" / "build.gradle.kts").read_text(encoding="utf-8"), re.M)
    return int(match.group(1))


def pick_apk(given):
    if given:
        return Path(given)
    for folder, pattern in ((ROOT / "app" / "build" / "outputs" / "apk" / "debug", "*.apk"), (ROOT / "releases", "*.apk")):
        apks = sorted(folder.glob(pattern), key=lambda path: path.stat().st_mtime, reverse=True)
        if apks:
            return apks[0]
    raise SystemExit("no APK: build with :app:assembleDebug or pass --apk")


def installed_version(serial, package):
    match = re.search(r"versionCode=(\d+)", adb(serial, "shell", "dumpsys", "package", package, check=False))
    return int(match.group(1)) if match else None


def install(serial, apk):
    started = time.time()
    try:
        out = subprocess.run(["adb", "-s", serial, "install", "-r", str(apk)], capture_output=True, text=True, timeout=300)
        ok = out.returncode == 0 and "Success" in out.stdout
        detail = "Success" if ok else ((out.stdout + out.stderr).strip().splitlines() or ["no output"])[-1]
    except subprocess.TimeoutExpired:
        ok, detail = False, "timed out (a confirmation screen on the phone?)"
    return ok, detail, time.time() - started


def launcher(serial):
    """AutoJs6's own launcher activity (a debug host with LeakCanary lists a second launcher entry)."""
    listed = adb(serial, "shell", "cmd", "package", "query-activities", "--brief", "-a", "android.intent.action.MAIN",
                 "-c", "android.intent.category.LAUNCHER", HOST)
    own = [line.strip() for line in listed.splitlines() if line.strip().startswith(HOST + "/") and "leakcanary" not in line]
    if own:
        return own[0]
    return adb(serial, "shell", "cmd", "package", "resolve-activity", "--brief", "-c", "android.intent.category.LAUNCHER", HOST).splitlines()[-1].strip()


def discover(serial, action, category):
    listed = adb(serial, "shell", "cmd", "package", "query-services", "--brief", "-a", action, "-c", category, check=False)
    return [line.strip() for line in listed.splitlines() if line.strip().startswith(PLUGIN + "/")]


def night_mode(serial):
    return "dark" if adb(serial, "shell", "cmd", "uimode", "night", check=False).strip().endswith("yes") else "light"


def run_script(serial, center):
    values = {"out": f"{REMOTE}/smoke.json", "pkg": PLUGIN, "descriptor": CAPABILITY_DESCRIPTOR, "center": "true" if center else "false",
              "host": HOST, "center_class": PLUGIN_CENTER}
    local = Path(tempfile.gettempdir()) / f"chime-smoke-{serial.replace(':', '_')}.js"
    local.write_text(SCRIPT % values, encoding="utf-8")
    adb(serial, "shell", "rm", "-rf", REMOTE, check=False)
    adb(serial, "shell", "mkdir", "-p", REMOTE)
    adb(serial, "push", str(local), f"{REMOTE}/smoke.js")
    local.unlink(missing_ok=True)
    adb(serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP")
    adb(serial, "shell", "am", "start", "-n", launcher(serial))
    time.sleep(2)
    adb(serial, "shell", "am", "start", "-a", "android.intent.action.VIEW", "-t", "application/x-javascript",
        "-d", f"file://{REMOTE}/smoke.js", "-n", f"{HOST}/org.autojs.autojs.external.open.RunIntentActivity")
    deadline = time.time() + 60
    result = None
    while time.time() < deadline:
        raw = adb(serial, "shell", "cat", f"{REMOTE}/smoke.json", check=False)
        if raw.startswith("{"):
            result = json.loads(raw)
            break
        time.sleep(1)
    adb(serial, "shell", "rm", "-rf", REMOTE, check=False)
    return result


def run(serial, args):
    name = label(serial)
    ok = True

    def report(step, passed, text):
        nonlocal ok
        ok = ok and passed
        print(f"    {step}: {'PASS' if passed else 'FAIL'} {text}")

    required = required_host_version()
    host = installed_version(serial, HOST)
    print(f"{name}: AutoJs6 versionCode {host}, plugin requires {required} ({'met' if host and host >= required else 'below, the alarms module stays unavailable'})")
    if not host:
        print(f"{name}: SMOKE FAIL (AutoJs6 is not installed)")
        return False
    apk = pick_apk(args.apk)
    contract_report = None
    if args.contract:
        import device_contract as contract
        contract_report = contract.Report()
        ok = contract.before_install(serial, contract_report) and ok
    if args.no_install:
        print(f"    install: SKIPPED (--no-install), installed versionCode {installed_version(serial, PLUGIN)}")
    else:
        passed, detail, seconds = install(serial, apk)
        report("install", passed, f"{apk.name} ({apk.stat().st_size} bytes, SHA-256 {sha256(apk)[:16]}...) in {seconds:.1f} s, "
                                  f"versionCode {installed_version(serial, PLUGIN)}; {detail}")
        if not passed:
            return False
    for action, category, expected in DISCOVERY:
        found = discover(serial, action, category)
        report(f"discovery {action.rsplit('.', 1)[-1]}", found == [expected], f"{action} / {category}: {len(found)} service(s) {found}")
    adb(serial, "shell", "am", "force-stop", PLUGIN, check=False)
    marker = f"chime-smoke-{uuid.uuid4().hex[:8]}"
    adb(serial, "shell", "log", "-t", "ChimeSmoke", marker, check=False)
    mode = night_mode(serial)
    r = run_script(serial, bool(args.screenshot))
    if r is None:
        print(f"{name}: SMOKE FAIL (no script result: unlock the device; AutoJs6 needs all-files access)")
        return False
    steps = r.get("steps", {})
    log = adb(serial, "shell", "logcat", "-d", "-v", "brief", check=False, timeout=60)
    after = log[log.rfind(marker):] if marker in log else ""
    wake_started = bool(re.search(rf"START .*cmp={re.escape(PLUGIN)}/\.WakeActivity", after))
    wake = steps.get("wake", {})
    report("wake", not wake.get("error") and wake_started,
           f"Wake activity start {'logged' if wake_started else 'not found in the log'} {wake.get('error') or ''}".rstrip())
    info = steps.get("info", {})
    expected_info = info.get("id") == PLUGIN_ID and info.get("engine") == "alarm" and info.get("variant") == "default"
    report("getInfo", not info.get("error") and expected_info and info.get("abis") == 0 and info.get("requiresHostVersion") == required,
           f"{info.get('name')} {info.get('id')} / {info.get('engine')} / {info.get('variant')}, {info.get('versionName')} ({info.get('versionCode')}, "
           f"{info.get('versionDate')}), description {info.get('descriptionChars')} chars, ABIs {info.get('abis')}, requiresHostVersion "
           f"{info.get('requiresHostVersion')}, contract {info.get('contractVersion')}, features {info.get('features')}, capability keys "
           f"[{info.get('capabilityKeys')}], {info.get('ms')} ms {info.get('error') or ''}".rstrip())
    alarm = steps.get("alarm", {})
    if alarm.get("proxy"):
        alarm_ok = alarm.get("features") == FEATURES and alarm.get("listError") == ""
        alarm_detail = (f"features [{alarm.get('features')}], permissions [{alarm.get('permissions')}], listAlarms "
                        f"{alarm.get('listError') or 'ok'}")
    else:
        alarm_ok = alarm.get("firstCallHandled") is True
        alarm_detail = f"no IAlarmPlugin proxy in the host, first call handled {alarm.get('firstCallHandled')}"
    report("alarm binder", not alarm.get("error") and alarm.get("descriptor") == CAPABILITY_DESCRIPTOR and alarm_ok,
           f"descriptor {alarm.get('descriptor')}, {alarm_detail} {alarm.get('error') or ''}".rstrip())
    if r.get("errors"):
        report("script", False, f"errors {[e[:160] for e in r['errors']]}")
    if args.screenshot:
        center = steps.get("pluginCenter", {})
        time.sleep(4)
        folder = Path(args.screenshot)
        folder.mkdir(parents=True, exist_ok=True)
        target = folder / f"plugin-center-{serial.replace(':', '_')}-system-{mode}.png"
        with open(target, "wb") as stream:
            subprocess.run(["adb", "-s", serial, "exec-out", "screencap", "-p"], stdout=stream, check=False, timeout=60)
        report("screenshot", not center.get("error") and target.stat().st_size > 0, f"{target.name} ({target.stat().st_size} bytes, system {mode}) {center.get('error') or ''}".rstrip())
    if contract_report is not None:
        ok = contract.after_smoke(serial, args, contract_report, apk) and ok
        print(f"    contract: {contract_report.passed} passed, {contract_report.failed} failed")
    print(f"{name}: SMOKE {'PASS' if ok else 'FAIL'} ({apk.name})")
    return ok


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("serial")
    parser.add_argument("--apk", help="APK to install (default: the newest debug APK, else the newest release)")
    parser.add_argument("--no-install", action="store_true", help="keep the installed build")
    parser.add_argument("--screenshot", metavar="DIR", help="open the Plugin Center afterwards and save the screen into DIR")
    parser.add_argument("--contract", action="store_true", help="run the roadmap P2.7 contract checks through the AutoJs6 alarms module")
    parser.add_argument("--incompatible-apk", metavar="FILE", help="with --contract: a build whose requiresHostVersion is above the host's")
    parser.add_argument("--feature-apk", metavar="FILE", help="with --contract: a release before 1.1.0 for the feature gate of timers (roadmap P7.3)")
    arguments = parser.parse_args(argv)
    return 0 if run(arguments.serial, arguments) else 1


if __name__ == "__main__":
    sys.exit(main())

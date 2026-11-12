"""The chime tools through MCP Server from the PC (roadmap P4.2).

    py .python/mcp_tools.py <serial> [--wait-pairing 180]

Uses the evidence client of the sibling repository AutoJs6-Plugin-MCP-Server (tools/evidence: mcp_client.py,
device.py, tool_calls.py; standard library only). Prerequisites on the phone, done by the maintainer: the MCP Server
listener runs with AutoJs6 attached, developer mode of MCP Server is on (the token is read from dumpsys and kept in
memory only) or AUTOJS6_MCP_TOKEN is set, and the write switch of the chime tool group is on (alarm_add and
alarm_remove are write / destructive tools). The first tool call asks for pairing; the script prints the fingerprint and
waits while the maintainer allows it on the phone.

Checks: `tools/list` names the twelve chime tools (the nine alarm tools, since 1.1.0 also the three timer tools of roadmap
P7.6) with MCP annotations from their risk, then `chime_alarm_add` (a weekday
alarm 30 days ahead, tag p42-mcp), `chime_alarm_list`, `chime_alarm_upcoming` and `chime_alarm_remove` (the test alarm
is removed, also after a failure). Output: PASS / FAIL per check with counts and ids only, never the token or the tool list.
"""
import argparse
import datetime
import json
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
EVIDENCE = ROOT.parent / "AutoJs6-Plugin-MCP-Server" / "tools" / "evidence"
sys.path.insert(0, str(EVIDENCE))

from device import Device, DeviceError  # noqa: E402
from mcp_client import McpClient, McpError, PAIRING_REQUIRED  # noqa: E402

CLIENT_NAME = "chime-p42-evidence"
TAG = "p42-mcp"
PAIRING_WAIT = [180]
TOOLS = [
    "alarm_add", "alarm_list", "alarm_update", "alarm_remove", "alarm_set_enabled", "alarm_skip_next", "alarm_upcoming", "alarm_dismiss", "alarm_snooze",
    "timer_start", "timer_list", "timer_control",
]


class Report:
    def __init__(self):
        self.passed = 0
        self.failed = 0

    def line(self, name, ok, detail=""):
        self.passed += 1 if ok else 0
        self.failed += 0 if ok else 1
        print(f"    {name}: {'PASS' if ok else 'FAIL'} {detail}".rstrip())
        return ok


def call(client, name, arguments):
    """(ok, decoded result, elapsed ms) of one tools/call; the result comes as JSON text without an output schema."""
    started = time.perf_counter()
    deadline = time.time() + PAIRING_WAIT[0]
    announced = False
    while True:
        try:
            result, _ = client.request("tools/call", {"name": "chime_" + name, "arguments": arguments}, timeout=60)
            break
        except McpError as e:
            if e.code != PAIRING_REQUIRED or time.time() > deadline:
                raise
            if not announced:
                print(f"    ACTION: allow the client {CLIENT_NAME} (fingerprint {e.pairing_fingerprint}) on the phone")
                announced = True
            time.sleep(3)
            started = time.perf_counter()
    text = next((item.get("text", "") for item in result.get("content", []) if item.get("type") == "text"), "")
    body = result.get("structuredContent")
    if body is None:
        try:
            body = json.loads(text)
        except ValueError:
            body = {"text": text[:160]}
    if isinstance(body, dict) and "result" in body and isinstance(body["result"], dict):
        body = body["result"]
    return not result.get("isError"), body, round((time.perf_counter() - started) * 1000)


def initialize(client, wait):
    deadline = time.time() + wait
    announced = False
    while True:
        try:
            return client.initialize()
        except McpError as e:
            if e.code != PAIRING_REQUIRED or time.time() > deadline:
                raise
            if not announced:
                print(f"    ACTION: allow the client {CLIENT_NAME} (fingerprint {e.pairing_fingerprint}) on the phone")
                announced = True
            time.sleep(3)


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("serial")
    parser.add_argument("--wait-pairing", type=int, default=180)
    args = parser.parse_args(argv)
    PAIRING_WAIT[0] = args.wait_pairing
    report = Report()
    device = Device(args.serial)
    state = device.plugin_state()
    print(f"{args.serial}:")
    if not report.line("listener running with AutoJs6 attached", state["running"] and state.get("host") == "available", f"state {state.get('state')}, host {state.get('host')}"):
        return 1
    token, source = device.read_token("auto")
    report.line("token read", True, f"source {source}")
    created = []
    with device.forward(state["port"]) as local_port, McpClient(f"http://127.0.0.1:{local_port}/mcp", token, CLIENT_NAME) as client:
        init = initialize(client, args.wait_pairing)
        report.line("initialize", True, f"protocol {init.get('protocolVersion')}")
        listed, _elapsed, _metas = client.list_all("tools/list", "tools")
        mine = {t["name"][len("chime_"):]: t for t in listed if t["name"].startswith("chime_")}
        report.line("tools/list names the chime tools", sorted(mine) == sorted(TOOLS), f"{len(mine)} of {len(TOOLS)}")
        annotations = {n: (t.get("annotations") or {}) for n, t in mine.items()}
        report.line("annotations follow the risks",
                    annotations.get("alarm_list", {}).get("readOnlyHint") is True and annotations.get("alarm_upcoming", {}).get("readOnlyHint") is True
                    and annotations.get("alarm_remove", {}).get("destructiveHint") is True and annotations.get("alarm_add", {}).get("readOnlyHint") is not True
                    and annotations.get("timer_list", {}).get("readOnlyHint") is True and annotations.get("timer_control", {}).get("readOnlyHint") is not True,
                    "list / upcoming / timer_list read-only, remove destructive")
        day = (datetime.date.today() + datetime.timedelta(days=30)).isoformat()
        try:
            ok, body, ms = call(client, "alarm_add", {"time": "04:30", "date": day, "repeat": [1, 2, 3, 4, 5], "label": "p42 mcp", "tag": TAG,
                                                      "sound": {"source": "silent"}})
            alarm = body.get("alarm") or {}
            if alarm.get("id"):
                created.append(alarm["id"])
            report.line("chime_alarm_add", ok and alarm.get("source") == "tool" and alarm.get("repeat") == "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR",
                        f"{ms} ms, id {alarm.get('id')}, source {alarm.get('source')}, nextTriggerTime {alarm.get('nextTriggerTime')}" if ok else str(body)[:160])
            ok, body, ms = call(client, "alarm_list", {"tag": TAG})
            report.line("chime_alarm_list", ok and body.get("total") == 1 and [a.get("id") for a in body.get("alarms", [])] == created,
                        f"{ms} ms, total {body.get('total')}")
            ok, body, ms = call(client, "alarm_upcoming", {"count": 3, "tag": TAG})
            occurrences = body.get("occurrences") or []
            report.line("chime_alarm_upcoming", ok and len(occurrences) == 3 and all(o.get("alarmId") in created for o in occurrences),
                        f"{ms} ms, {len(occurrences)} rings, first {occurrences[0].get('time') if occurrences else None}")
        finally:
            for alarm_id in list(created):
                ok, body, ms = call(client, "alarm_remove", {"id": alarm_id})
                if report.line("chime_alarm_remove", ok and body.get("removed") is True, f"{ms} ms, removed {body.get('removed')}"):
                    created.remove(alarm_id)
            ok, body, _ = call(client, "alarm_list", {"tag": TAG})
            report.line("no test alarm left", ok and body.get("total") == 0, f"total {body.get('total')}")
    print(f"{args.serial}: MCP TOOLS {'PASS' if report.failed == 0 else 'FAIL'} ({report.passed} passed, {report.failed} failed)")
    return 0 if report.failed == 0 else 1


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (DeviceError, McpError) as e:
        print(f"FAIL {type(e).__name__}: {str(e)[:200]}")
        sys.exit(1)

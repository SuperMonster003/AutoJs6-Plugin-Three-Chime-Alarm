"""Copies the signed holiday calendar release of AutoJs6-Shared-Assets into the plugin (roadmap P8.2).

    py .python/sync_holiday_snapshot.py [--source <holiday-calendars dir>] [--check]

The plugin bundles the release that is current when it is built (roadmap P8.2): the catalog, its signature envelope, every
calendar file under the release asset name the catalog gives (`holiday-calendar-<code>-<hash8>.json`) and the two
python-holidays notices the MIT license requires. The default source is the sibling checkout
`../AutoJs6-Shared-Assets/holiday-calendars`. Every file is checked against the size and SHA-256 of the catalog before it is
copied, calendar files of an older snapshot are removed, and nothing else in the target is touched. `--check` copies
nothing and fails when the bundled snapshot differs from the source.

zh-CN: 把 AutoJs6-Shared-Assets 已签名的节假日日历发布复制进插件 (路线图 P8.2). 插件内置构建时的当前发布: 目录, 签名信封, 以目录给出的
Release 资源名保存的每个日历文件, 以及 MIT 许可要求的两个 python-holidays 声明. 复制前按目录核对每个文件的大小与 SHA-256, 删除旧快照的
日历文件, 目标中的其他文件不动. `--check` 不复制, 内置快照与来源不同时失败.
"""
import argparse
import hashlib
import json
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TARGET = ROOT / "app" / "src" / "main" / "assets" / "holiday-calendars"
DEFAULT_SOURCE = ROOT.parent / "AutoJs6-Shared-Assets" / "holiday-calendars"
FIXED = {
    "catalog-v1.json": Path("catalogs") / "catalog-v1.json",
    "catalog-v1.sig.json": Path("catalogs") / "catalog-v1.sig.json",
    "LICENSE-python-holidays.txt": Path("LICENSE-python-holidays.txt"),
    "CONTRIBUTORS-python-holidays.txt": Path("CONTRIBUTORS-python-holidays.txt"),
}
CALENDAR_PREFIX = "holiday-calendar-"


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def plan(source: Path) -> dict:
    """Maps every target file name to its source path, checking sizes and hashes. zh-CN: 目标文件名到来源路径, 并核对大小与哈希."""
    catalog = json.loads((source / FIXED["catalog-v1.json"]).read_text(encoding="utf-8"))
    files = {name: source / relative for name, relative in FIXED.items()}
    for region in catalog["regions"]:
        entry = region["file"]
        path = source / "data" / f"{region['code']}.json"
        data = path.read_bytes()
        if len(data) != entry["size"] or hashlib.sha256(data).hexdigest() != entry["sha256"]:
            raise SystemExit(f"FAIL {path.name} differs from the catalog (size or sha256)")
        files[entry["name"]] = path
    return files


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, default=DEFAULT_SOURCE)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    files = plan(args.source)
    present = {p.name for p in TARGET.glob("*")} if TARGET.is_dir() else set()
    stale = sorted(n for n in present if n.startswith(CALENDAR_PREFIX) and n not in files)
    changed = sorted(n for n, p in files.items() if not (TARGET / n).is_file() or sha256(TARGET / n) != sha256(p))
    if args.check:
        ok = not stale and not changed
        print(f"{'PASS' if ok else 'FAIL'} holiday snapshot: {len(files)} files, {len(changed)} differ, {len(stale)} stale")
        return 0 if ok else 1
    TARGET.mkdir(parents=True, exist_ok=True)
    for name in stale:
        (TARGET / name).unlink()
    for name in changed:
        shutil.copyfile(files[name], TARGET / name)
    print(f"PASS holiday snapshot: {len(files)} files, {len(changed)} copied, {len(stale)} removed")
    return 0


if __name__ == "__main__":
    sys.exit(main())

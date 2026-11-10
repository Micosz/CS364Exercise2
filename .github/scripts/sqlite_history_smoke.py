"""Exercise the real Activity and read its test database on a fresh CI emulator."""

from pathlib import Path
import re
import sqlite3
import subprocess
import tempfile
import time
from xml.etree import ElementTree

PACKAGE = "th.ac.tu.step"
EVIDENCE = Path("build/sqlite-history")


def adb(*args, binary=False):
    return subprocess.check_output(["adb", *args], text=not binary, timeout=30)


def screen():
    result = adb("shell", "uiautomator", "dump", "/sdcard/stepbystep-test.xml")
    if "ERROR" in result:
        raise RuntimeError(result)
    content = adb("exec-out", "cat", "/sdcard/stepbystep-test.xml")
    (EVIDENCE / "last-screen.xml").write_text(content, encoding="utf-8")
    return list(ElementTree.fromstring(content).iter("node"))


def until(predicate, description):
    deadline = time.monotonic() + 25
    while time.monotonic() < deadline:
        try:
            result = predicate(screen())
            if result:
                return result
        except (ElementTree.ParseError, RuntimeError, subprocess.CalledProcessError):
            pass
        time.sleep(0.4)
    raise AssertionError("Could not verify " + description)


def by_id(nodes, name):
    return [n for n in nodes if n.get("resource-id") == PACKAGE + ":id/" + name]


def by_text(nodes, label):
    return [n for n in nodes if n.get("text") == label]


def tap(node):
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.attrib["bounds"]))
    adb("shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))


def tap_id(name):
    tap(until(lambda nodes: by_id(nodes, name), name)[0])


def tap_text(label):
    tap(until(lambda nodes: by_text(nodes, label), label)[0])


def launch():
    adb("shell", "am", "start", "-W", "-n", PACKAGE + "/.MainActivity")


def rows():
    with tempfile.TemporaryDirectory() as tmp:
        file = Path(tmp) / "step_by_step.db"
        data = adb("exec-out", "run-as", PACKAGE, "cat", "databases/" + file.name, binary=True)
        assert data.startswith(b"SQLite format 3"), "App did not create the expected SQLite file"
        file.write_bytes(data)
        for suffix in ("-wal", "-shm"):
            name = file.name + suffix
            exists = subprocess.run(["adb", "shell", "run-as", PACKAGE, "ls", "databases/" + name],
                                    stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=30)
            if exists.returncode == 0:
                (Path(tmp) / name).write_bytes(adb("exec-out", "run-as", PACKAGE, "cat", "databases/" + name, binary=True))
        db = sqlite3.connect(file)
        try:
            return db.execute(
                "SELECT id, started_at, duration_ms, steps, shakes FROM sessions ORDER BY id DESC"
            ).fetchall()
        finally:
            db.close()


def duration_label(record):
    seconds = record[2] // 1000
    return f"ระยะเวลา {seconds // 60} นาที {seconds % 60:02d} วินาที"


def visible_rounds(records):
    expected = [duration_label(record) for record in records]

    def matching(nodes):
        items = by_id(nodes, "itemDuration")
        return items if [n.get("text") for n in items] == expected else None

    return until(matching, "history durations in newest-first order: " + str(expected))


def empty_history():
    until(lambda nodes: by_id(nodes, "empty") and not by_id(nodes, "itemDuration"), "empty history")


def record_round(seconds):
    tap_id("action")
    until(lambda nodes: by_text(nodes, "จบและบันทึกกิจกรรม"), "active round")
    time.sleep(seconds)
    tap_id("action")
    until(lambda nodes: by_text(nodes, "เริ่มกิจกรรม"), "saved round")


def request_delete(item):
    tap(item)
    tap_text("ลบกิจกรรม")
    until(lambda nodes: by_text(nodes, "ลบกิจกรรมนี้?"), "delete confirmation")


def main():
    launch()
    tap_id("historyTab")
    empty_history()
    assert rows() == [], "Fresh install already has saved sessions"
    tap_id("trackTab")
    for seconds in (1, 5, 9):
        record_round(seconds)
    before = rows()
    assert len(before) == 3 and len({row[0] for row in before}) == 3
    assert all(row[1] > 0 and row[2] > 0 and min(row[3:]) >= 0 for row in before)
    assert len({duration_label(row) for row in before}) == 3, "Test rounds must be distinguishable"
    tap_id("historyTab")
    cards = visible_rounds(before)
    print("PASS: three distinct rounds saved to SQLite and shown newest first", flush=True)

    adb("shell", "am", "force-stop", PACKAGE)
    launch()
    tap_id("historyTab")
    cards = visible_rounds(before)
    assert rows() == before
    print("PASS: all rounds persisted after force-stop and relaunch", flush=True)

    tap(cards[1])
    record = before[1]
    expected = f"{duration_label(record)}\nก้าวเดิน {record[3]} ก้าว\nเขย่า {record[4]} ครั้ง"
    until(lambda nodes: [n for n in by_id(nodes, "detailMetrics") if n.get("text") == expected],
          "middle-round details match SQLite")
    tap_text("ลบกิจกรรม")
    until(lambda nodes: by_text(nodes, "ลบกิจกรรมนี้?"), "delete confirmation")
    tap_text("ยกเลิก")
    cards = visible_rounds(before)
    assert rows() == before
    print("PASS: details match SQLite and cancelling deletion preserves all rows", flush=True)

    request_delete(cards[1])
    tap_text("ลบ")
    remaining = [before[0], before[2]]
    cards = visible_rounds(remaining)
    assert rows() == remaining
    adb("shell", "am", "force-stop", PACKAGE)
    launch()
    tap_id("historyTab")
    cards = visible_rounds(remaining)
    assert rows() == remaining
    print("PASS: only the selected middle ID was deleted and stays deleted after relaunch", flush=True)

    for index in range(len(remaining)):
        request_delete(cards[0])
        tap_text("ลบ")
        pending = remaining[index + 1:]
        if pending:
            cards = visible_rounds(pending)
        else:
            empty_history()
        assert rows() == pending
    print("PASS: deleting the final row shows empty history and an empty SQLite table", flush=True)


if __name__ == "__main__":
    try:
        main()
    except Exception:
        try:
            (EVIDENCE / "failure.png").write_bytes(adb("exec-out", "screencap", "-p", binary=True))
        except (subprocess.SubprocessError, OSError):
            pass
        raise

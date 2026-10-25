#!/usr/bin/env python3
"""Refuse local deployment while a launcher or direct Java server is active."""

import argparse
from pathlib import Path
import shutil
import subprocess
import sys


def java_process_ids(output):
    """Parse lsof's process/command fields and its implicit descriptor fields."""
    processes = []
    current = None
    for line in output.splitlines():
        if line.startswith("p") and line[1:].isdigit():
            current = {"pid": line[1:], "command": None, "cwd": False}
            processes.append(current)
        elif current is not None and line.startswith("c") and line[1:] and current["command"] is None:
            current["command"] = line[1:]
        elif current is not None and line == "fcwd":
            current["cwd"] = True
        else:
            raise RuntimeError("Cannot verify stopped server: unexpected lsof output; deployment refused")
    if not processes or any(not process["command"] or not process["cwd"] for process in processes):
        raise RuntimeError("Cannot verify stopped server: incomplete lsof output; deployment refused")
    return [process["pid"] for process in processes
            if Path(process["command"]).name.lower() in ("java", "javaw")]


def verify_stopped_server(server):
    server = Path(server).resolve(strict=True)
    if not server.is_dir():
        raise RuntimeError(f"Server directory is not a directory: {server}")
    launcher_lock = server / "paperscript/locks/server-launch"
    if launcher_lock.exists() or launcher_lock.is_symlink():
        raise RuntimeError(f"Stop the maintained launcher before deploying: {server}")
    lsof = shutil.which("lsof")
    if not lsof:
        raise RuntimeError("Cannot verify stopped server: lsof is unavailable; deployment refused")
    try:
        result = subprocess.run(
            [lsof, "-nP", "+c", "0", "-a", "-d", "cwd", "-Fpc", "--", str(server)],
            text=True, capture_output=True, timeout=15)
    except (OSError, subprocess.SubprocessError) as error:
        raise RuntimeError(f"Cannot verify stopped server: lsof inspection failed; deployment refused ({error})") from error
    # lsof exits 1 with no output when no process has this directory as its cwd.
    if result.returncode == 1 and not result.stdout and not result.stderr:
        return server
    if result.returncode != 0 or result.stderr:
        detail = result.stderr.strip() or f"exit status {result.returncode}"
        raise RuntimeError(f"Cannot verify stopped server: lsof inspection failed; deployment refused ({detail})")
    running = java_process_ids(result.stdout)
    if running:
        raise RuntimeError(f"Stop Java process(es) {', '.join(running)} in {server} before deploying")
    return server


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("server", type=Path)
    args = parser.parse_args()
    server = verify_stopped_server(args.server)
    print(f"Verified stopped server for deployment: {server}")


if __name__ == "__main__":
    try:
        main()
    except (OSError, RuntimeError) as error:
        sys.exit(f"Deployment preflight failed: {error}")

#!/usr/bin/env python3
"""Exercise the release jar on both installed JDKs in isolated local Paper copies."""

import argparse
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import socket
import subprocess
import threading
import time
import zipfile


ROOT = Path(__file__).resolve().parents[1]
JAVA_ROOT = Path("/Library/Java/JavaVirtualMachines")
RUNTIMES = ("25.0.4.1", "26.0.2.1")


def properties(text):
    return dict(line.split("=", 1) for line in text.splitlines()
                if "=" in line and not line.startswith("#"))


def sha256(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def free_port():
    with socket.socket() as sock:
        sock.bind(("127.0.0.1", 0))
        return sock.getsockname()[1]


def prepare_server(source, destination, artifact, metadata):
    """Copy fixtures; never start or edit the maintained world's files."""
    state = json.loads((source / "paperscript/state.json").read_text())
    if (state["current_version"] != metadata["paperTarget"]
            or str(state["current_build"]) != metadata["paperBuild"]):
        raise RuntimeError("Maintained Paper version/build does not match the artifact")
    paper = source / state["current_jar"]
    if sha256(paper) != state["current_sha256"]:
        raise RuntimeError("Maintained Paper checksum mismatch")
    if not re.search(r"(?m)^eula=true$", (source / "eula.txt").read_text()):
        raise RuntimeError("The maintained test server must already have an accepted EULA")
    destination.mkdir(parents=True)
    shutil.copy2(paper, destination / "paper.jar")
    server_properties = properties((source / "server.properties").read_text())
    world = server_properties.get("level-name", "world")
    if Path(world).name != world:
        raise RuntimeError("Smoke fixtures require a world folder inside the server directory")
    for name in ("libraries", "versions", "cache", "config", world, "bluemap"):
        if (source / name).is_dir():
            shutil.copytree(source / name, destination / name,
                            ignore=shutil.ignore_patterns("session.lock", "logs", "*.log"))
    for name in ("eula.txt", "bukkit.yml", "spigot.yml"):
        shutil.copy2(source / name, destination / name)
    server_properties.update({"server-ip": "127.0.0.1", "server-port": str(free_port()),
                              "enable-rcon": "false", "enable-query": "false",
                              "online-mode": "true", "pause-when-empty-seconds": "-1"})
    (destination / "server.properties").write_text(
        "".join(f"{key}={value}\n" for key, value in server_properties.items()))
    plugins = destination / "plugins"
    plugins.mkdir()
    for pattern in ("bluemap-*.jar", "PlaceholderAPI-*.jar"):
        matches = list((source / "plugins").glob(pattern))
        if len(matches) != 1:
            raise RuntimeError(f"Expected exactly one {pattern} in the maintained server")
        shutil.copy2(matches[0], plugins / matches[0].name)
    shutil.copy2(artifact, plugins / artifact.name)
    shutil.copytree(source / "plugins/BlueMap", plugins / "BlueMap")
    # Rendering/API checks do not require a second HTTP listener.
    (plugins / "BlueMap/webserver.conf").write_text("enabled: false\n")
    (plugins / "PlaceholderAPI").mkdir()
    (plugins / "PlaceholderAPI/config.yml").write_text("check_updates: false\n")
    (plugins / "1MB-MapHide").mkdir()
    with zipfile.ZipFile(artifact) as jar:
        config = jar.read("config.yml").decode()
    (plugins / "1MB-MapHide/config.yml").write_text(
        "# Preserve this smoke-test comment.\n" + config + "\nsmoke-custom-key: preserved\n")
    return state["current_sha256"]


def run_server(directory, java_home, version, metadata):
    env = dict(os.environ, JAVA_HOME=str(java_home))
    env["PATH"] = str(java_home / "bin") + os.pathsep + env.get("PATH", "")
    runtime = subprocess.run(["java", "-version"], env=env, text=True,
                             capture_output=True, check=True).stderr.strip()
    if f'java version "{version}"' not in runtime:
        raise RuntimeError(f"Expected Java {version}: {runtime}")
    (directory / "java-version.txt").write_text(runtime + "\n")
    print(f"Starting Paper {metadata['paperTarget']} build {metadata['paperBuild']} on {version}", flush=True)
    lines = []
    checks = []
    with (directory / "console.log").open("w") as log:
        process = subprocess.Popen(
            ["java", "-Xms512M", "-Xmx2G", "-XX:ActiveProcessorCount=4",
             "-Dfile.encoding=UTF-8", "-Dterminal.ansi=false", "-Dterminal.jline=false",
             "-Dapple.awt.UIElement=true", "-jar", "paper.jar", "--nogui"],
            cwd=directory, env=env, text=True, stdin=subprocess.PIPE,
            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, bufsize=1)

        def read_output():
            for line in process.stdout:
                lines.append(line)
                log.write(line)
                log.flush()

        reader = threading.Thread(target=read_output, daemon=True)
        reader.start()

        def expect(label, expected, start=0, timeout=30):
            deadline = time.monotonic() + timeout
            while time.monotonic() < deadline:
                output = "".join(lines[start:])
                if all(value in output for value in expected):
                    checks.append(label)
                    print(f"  PASS {version}: {label}", flush=True)
                    return
                if process.poll() is not None:
                    break
                time.sleep(0.05)
            raise RuntimeError(f"{label}: missing {expected}\n" + "".join(lines[-25:]))

        def command(value, *expected):
            start = len(lines)
            process.stdin.write(value + "\n")
            process.stdin.flush()
            expect(value, expected, start)

        try:
            expect("startup and plugin loading", ["Done (", "[1MB-MapHide] 1MB-MapHide enabled",
                                                  "PlaceholderAPI expansion registered"], timeout=180)
            expect("BlueMap ready", ["[BlueMap] Loaded!"], timeout=180)
            command("bmpc status", f"Build: {metadata['buildNumber']}",
                    f"API {metadata['paperApiVersion']}", f"Java: {version}",
                    "BlueMap: 5.22, API:", f"{metadata['paperTarget']}-{metadata['paperBuild']}-")
            command("bmpc info", f"build {metadata['buildNumber']}", "Target: Java 25 / Paper 26.2")
            command("bmpc version", f"build {metadata['buildNumber']}", "1MB-MapHide info")
            for value, expected in (("bmpc help", "(page 1/"),
                                    ("bmpc help 2", "(page 2/"),
                                    ("bmpc debug status", "Force perms:"),
                                    ("bmpc debug commands", "Command routes"),
                                    ("bmpc debug permissions", "Permission debug"),
                                    ("bmpc debug placeholders", "PlaceholderAPI enabled: true"),
                                    ("bmpc config", "Config values")):
                command(value, expected)
            for value in ("bmpc", "bmpc toggle", "bmpc show", "bmpc hide", "map hide"):
                command(value, "Only players can use this command.")
            command("bmpc hide MapHideMissing", 'Player "MapHideMissing" was not found.')
            command("bmpc help invalid", "Invalid page:")
            command("bmpc config set unknown-key invalid", "Unknown config key:")
            command("papi parse --null %maphide_state%|%maphide_default_visibility%|%maphide_toggle_back_seconds%|%maphide_language%",
                    "unknown|visible|0|EN")
            command("bmpc config set default-visibility hide", "Updated default-visibility to hide.")
            command("bmpc config set toggle-back-after-seconds 2", "Updated toggle-back-after-seconds to 2.")
            command("bmpc reload", "Configuration and translations reloaded.")
            command("papi parse --null %maphide_default_visibility%|%maphide_toggle_back_seconds%",
                    "hidden|2")
            command("bmpc config set default-visibility show", "Updated default-visibility to show.")
            command("bmpc config set toggle-back-after-seconds 0", "Updated toggle-back-after-seconds to 0.")
            config = (directory / "plugins/1MB-MapHide/config.yml").read_text()
            if "# Preserve this smoke-test comment." not in config or "smoke-custom-key: preserved" not in config:
                raise RuntimeError("Config reload discarded a custom comment or unknown key")
            checks.append("config comments and custom keys preserved")
            command("bmpc status", f"Java: {version}", "BlueMap: 5.22, API:")
        finally:
            if process.poll() is None:
                process.stdin.write("stop\n")
                process.stdin.flush()
                try:
                    process.wait(timeout=60)
                except subprocess.TimeoutExpired:
                    process.terminate()
                    try:
                        process.wait(timeout=10)
                    except subprocess.TimeoutExpired:
                        process.kill()
                        process.wait()
                    raise RuntimeError("Paper did not shut down cleanly within 60 seconds")
            reader.join(timeout=5)
        expect("clean shutdown", ["[1MB-MapHide] 1MB-MapHide disabled", "[BlueMap] Saved and stopped!",
                                  "All dimensions are saved"])
        if process.returncode != 0:
            raise RuntimeError(f"Paper exited with {process.returncode}")
        failures = [line.strip() for line in lines if re.search(
            r"(?:/|\s)(?:ERROR|SEVERE)\]|Exception|UnsupportedClassVersionError|NoClassDefFoundError|NoSuchMethodError", line)]
        if failures:
            raise RuntimeError("Errors in server log:\n" + "\n".join(failures))
    return {"runtime": runtime, "checks": checks, "exit_code": process.returncode,
            "console_log": str(directory / "console.log")}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--server", type=Path, default=ROOT / "servers/Paper-26.2")
    parser.add_argument("--output", type=Path)
    for major, version in zip((25, 26), RUNTIMES):
        parser.add_argument(f"--java{major}-home", type=Path,
                            default=JAVA_ROOT / f"jdk-{version}.jdk/Contents/Home")
    args = parser.parse_args()
    config = properties((ROOT / "gradle.properties").read_text())
    build = (ROOT / "build-number.txt").read_text().strip()
    artifact = ROOT / "build/libs" / (
        f"1MB-BlueMap-MapHide-v{config['pluginVersion']}-{build}-j{config['javaTarget']}-{config['paperTarget']}.jar")
    with zipfile.ZipFile(artifact) as jar:
        metadata = properties(jar.read("build-info.properties").decode())
    if any(metadata[key] != config[key] for key in ("pluginVersion", "javaTarget", "paperTarget", "paperBuild", "paperApiVersion")) or metadata["buildNumber"] != build:
        raise RuntimeError("Rebuild first: artifact metadata does not match the checkout")
    stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ")
    output = (args.output or ROOT / f"servers/verification/build-{build}-{stamp}").resolve()
    output.mkdir(parents=True, exist_ok=False)
    report = {"artifact": str(artifact), "sha256": sha256(artifact), "metadata": metadata, "runs": []}
    for java_home, version in zip((args.java25_home, args.java26_home), RUNTIMES):
        directory = output / f"java-{version}"
        report["paper_sha256"] = prepare_server(args.server.resolve(), directory, artifact, metadata)
        report["runs"].append(run_server(directory, java_home.resolve(), version, metadata))
        (output / "results.json").write_text(json.dumps(report, indent=2) + "\n")
    print(f"PASS: both runtimes. Results: {output / 'results.json'}", flush=True)


if __name__ == "__main__":
    main()

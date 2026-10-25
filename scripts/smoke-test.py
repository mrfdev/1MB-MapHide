#!/usr/bin/env python3
"""Exercise Paper 26.3 STABLE in disposable localhost servers on installed Oracle JDK 27."""

import argparse
from datetime import datetime, timezone
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

from paper_fixture import properties, sha256, verify_server
from java_toolchains import resolve_jdk

ROOT = Path(__file__).resolve().parents[1]


def resolve_test_runtimes(java_homes=None):
    """Validate every requested runtime before creating a disposable server."""
    if java_homes:
        return [resolve_jdk(27, override=str(home)) for home in java_homes]
    return [resolve_jdk(27)]


def resolve_server_path(server, config):
    return (server or ROOT / config["localPaperServerDir"]).resolve()


def free_port():
    with socket.socket() as sock:
        sock.bind(("127.0.0.1", 0))
        return sock.getsockname()[1]


def unexpected_errors(lines, metadata, missing_bluemap=False):
    failures = [line.strip() for line in lines if re.search(
        r"(?:/|\s)(?:ERROR|SEVERE)\]|Exception|UnsupportedClassVersionError|NoClassDefFoundError|NoSuchMethodError", line)]
    if missing_bluemap:
        artifact = (f"1MB-BlueMap-MapHide-v{metadata['pluginVersion']}-{metadata['buildNumber']}"
                    f"-j{metadata['javaTarget']}-{metadata['paperTarget']}.jar")
        header = f"[ModernPluginLoadingStrategy] Could not load 'plugins/{artifact}' in 'plugins'"
        exception = ("org.bukkit.plugin.UnknownDependencyException: Unknown/missing dependency plugins: "
                     "[BlueMap]. Please download and install these plugins to run '1MB-MapHide'.")
        failures = [line for line in failures if not (line.endswith(header) or line == exception)]
    return failures


def prepare_server(source, destination, artifact, metadata):
    """Copy fixtures; never start or edit the maintained world's files."""
    config = properties((ROOT / "gradle.properties").read_text())
    paper = verify_server(source, config)
    if not re.search(r"(?m)^eula=true$", (source / "eula.txt").read_text()):
        raise RuntimeError("The maintained test server must already have an accepted EULA")
    destination.mkdir(parents=True)
    shutil.copy2(paper, destination / "paper.jar")
    server_properties = properties((source / "server.properties").read_text())
    world = server_properties.get("level-name", "world")
    if Path(world).name != world:
        raise RuntimeError("Smoke fixtures require a world folder inside the server directory")
    # Generate new worlds/configuration even if the source fixture was used manually.
    for name in ("libraries", "versions", "cache", "bluemap"):
        if (source / name).is_dir():
            shutil.copytree(source / name, destination / name,
                            ignore=shutil.ignore_patterns("session.lock", "logs", "*.log"))
    for name in ("eula.txt",):
        if (source / name).is_file():
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
    (plugins / "bStats").mkdir()
    (plugins / "bStats/config.yml").write_text("enabled: false\n")
    (plugins / "PlaceholderAPI").mkdir()
    (plugins / "PlaceholderAPI/config.yml").write_text("check_updates: false\n")
    (plugins / "1MB-MapHide").mkdir()
    with zipfile.ZipFile(artifact) as jar:
        plugin_config = jar.read("config.yml").decode()
    (plugins / "1MB-MapHide/config.yml").write_text(
        "# Preserve this smoke-test comment.\n" + plugin_config + "\nsmoke-custom-key: preserved\n")
    return config["paperSha256"]


def stop_server(process, stop_timeout=60, terminate_timeout=10, kill_timeout=5):
    """Reap the child even when it has closed stdin; forced shutdown still fails verification."""
    if process.poll() is not None:
        return
    try:
        process.stdin.write("stop\n")
        process.stdin.flush()
    except OSError:
        # The child can close stdin before exiting. Still wait and enforce the deadline.
        pass
    try:
        process.wait(timeout=stop_timeout)
    except subprocess.TimeoutExpired:
        try:
            process.terminate()
        except ProcessLookupError:
            pass
        try:
            process.wait(timeout=terminate_timeout)
        except subprocess.TimeoutExpired:
            try:
                process.kill()
            except ProcessLookupError:
                pass
            try:
                process.wait(timeout=kill_timeout)
            except subprocess.TimeoutExpired as error:
                raise RuntimeError("Paper could not be reaped after forced shutdown") from error
        raise RuntimeError(f"Paper did not shut down cleanly within {stop_timeout} seconds")


def run_server(directory, java_home, version, metadata, phase="initial", papi=True, missing_bluemap=False):
    jdk = resolve_jdk(27, override=str(java_home))
    java_home = Path(jdk["home"])
    if jdk["version"] != version:
        raise RuntimeError(f"Java runtime changed after preflight: {version} -> {jdk['version']}")
    env = dict(os.environ, JAVA_HOME=str(java_home))
    env["PATH"] = str(java_home / "bin") + os.pathsep + env.get("PATH", "")
    runtime = subprocess.run([jdk["java"], "-version"], env=env, text=True,
                             capture_output=True, check=True).stderr.strip()
    if not re.search(rf'(?:java|openjdk) version "{re.escape(version)}"', runtime):
        raise RuntimeError(f"Expected Java {version}: {runtime}")
    (directory / "java-version.txt").write_text(runtime + "\n")
    print(f"Starting Paper {metadata['paperTarget']} build {metadata['paperBuild']} on {version} ({phase})", flush=True)
    lines = []
    checks = []
    bluemap = list((directory / "plugins").glob("bluemap-*.jar"))
    if missing_bluemap:
        bluemap = list((directory / "plugins").glob("bluemap-*.jar.disabled"))
    if len(bluemap) != 1:
        raise RuntimeError("Expected exactly one BlueMap runtime jar")
    with zipfile.ZipFile(bluemap[0]) as jar:
        manifest = jar.read("plugin.yml").decode()
        bluemap_version = json.loads(jar.read("de/bluecolored/bluemap/version.json"))["version"]
    bluemap_plugin_version = re.search(r"(?m)^version:\s*['\"]?([^'\"\s]+)", manifest).group(1)
    with (directory / f"console-{phase}.log").open("w") as log:
        process = subprocess.Popen(
            [jdk["java"], "-Xms512M", "-Xmx2G", "-XX:ActiveProcessorCount=4",
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
            if missing_bluemap:
                expect("missing BlueMap rejected", ["Done (", "Unknown/missing dependency plugins: [BlueMap]"], timeout=180)
                if "[1MB-MapHide] 1MB-MapHide enabled" in "".join(lines):
                    raise RuntimeError("MapHide enabled without its required BlueMap dependency")
            else:
                expected = ["Done (", "[1MB-MapHide] 1MB-MapHide enabled"]
                if papi:
                    expected.append("PlaceholderAPI expansion registered")
                expect("startup and plugin loading", expected, timeout=180)
                expect("BlueMap ready", [f"[BlueMap] Enabling BlueMap v{bluemap_plugin_version}",
                                         "[BlueMap] Loaded!"], timeout=180)
                if phase == "restart":
                    command("papi parse --null %maphide_default_visibility%|%maphide_toggle_back_seconds%",
                            "hidden|2")
                    checks.append("configuration persisted across process restart")
                if phase != "initial":
                    command("bmpc config set default-visibility show", "Updated default-visibility to show.")
                    command("bmpc config set toggle-back-after-seconds 0", "Updated toggle-back-after-seconds to 0.")
                command("bmpc status", f"Build: {metadata['buildNumber']}",
                        f"API {metadata['paperApiVersion']}", f"Java: {version}",
                        f"BlueMap: {bluemap_version}, API:", f"{metadata['paperTarget']}-{metadata['paperBuild']}-")
                command("bmpc info", f"build {metadata['buildNumber']}",
                        f"Target: Java {metadata['javaTarget']} / Paper {metadata['paperTarget']}")
                command("bmpc version", f"build {metadata['buildNumber']}", "1MB-MapHide info")
                for value, expected in (("bmpc help", "(page 1/"),
                                        ("bmpc help 2", "(page 2/"),
                                        ("bmpc debug status", "Force perms:"),
                                        ("bmpc debug commands", "Command routes"),
                                        ("bmpc debug permissions", "Permission debug"),
                                        ("bmpc debug placeholders", f"PlaceholderAPI enabled: {str(papi).lower()}"),
                                        ("bmpc config", "Config values")):
                    command(value, expected)
                for value in ("bmpc", "bmpc toggle", "bmpc show", "bmpc hide", "map hide"):
                    command(value, "Only players can use this command.")
                command("bmpc hide MapHideMissing", 'Player "MapHideMissing" was not found.')
                command("bmpc hide @e[type=invalid]", 'Player "@e[type=invalid]" was not found.')
                command("bmpc help invalid", "Invalid page:")
                command("bmpc config set unknown-key invalid", "Unknown config key:")
                if papi:
                    command("papi parse --null %maphide_state%|%maphide_default_visibility%|%maphide_toggle_back_seconds%|%maphide_language%",
                        "unknown|visible|0|EN")
                command("bmpc config set default-visibility hide", "Updated default-visibility to hide.")
                command("bmpc config set toggle-back-after-seconds 2", "Updated toggle-back-after-seconds to 2.")
                command("bmpc reload", "Configuration and translations reloaded.")
                if papi:
                    command("papi parse --null %maphide_default_visibility%|%maphide_toggle_back_seconds%",
                        "hidden|2")
                config = (directory / "plugins/1MB-MapHide/config.yml").read_text()
                if "# Preserve this smoke-test comment." not in config or "smoke-custom-key: preserved" not in config:
                    raise RuntimeError("Config reload discarded a custom comment or unknown key")
                checks.append("config comments and custom keys preserved")
                command("bmpc status", f"Java: {version}", f"BlueMap: {bluemap_version}, API:")
        finally:
            try:
                stop_server(process)
            finally:
                reader.join(timeout=5)
        shutdown = ["All RegionFile I/O tasks to complete"]
        if not missing_bluemap:
            shutdown += ["[1MB-MapHide] 1MB-MapHide disabled", "[BlueMap] Saved and stopped!"]
        expect("clean shutdown", shutdown)
        if process.returncode != 0:
            raise RuntimeError(f"Paper exited with {process.returncode}")
        failures = unexpected_errors(lines, metadata, missing_bluemap)
        if failures:
            raise RuntimeError("Errors in server log:\n" + "\n".join(failures))
    return {"runtime": runtime, "java_toolchain": jdk, "phase": phase, "checks": checks, "exit_code": process.returncode,
            "console_log": str(directory / f"console-{phase}.log")}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--server", type=Path, help="Defaults to localPaperServerDir in gradle.properties")
    parser.add_argument("--output", type=Path)
    parser.add_argument("--java-home", type=Path, action="append",
                        help="Installed Oracle JDK 27 home; repeat for multiple Oracle 27 installations. "
                             "Defaults to ONE_MB_TEST_JAVA_HOME or macOS java_home -F -v 27.")
    args = parser.parse_args()
    runtimes = resolve_test_runtimes(args.java_home)
    config = properties((ROOT / "gradle.properties").read_text())
    server = resolve_server_path(args.server, config)
    build = (ROOT / "build-number.txt").read_text().strip()
    artifact = ROOT / "build/libs" / (
        f"1MB-BlueMap-MapHide-v{config['pluginVersion']}-{build}-j{config['javaTarget']}-{config['paperTarget']}.jar")
    with zipfile.ZipFile(artifact) as jar:
        metadata = properties(jar.read("build-info.properties").decode())
    if any(metadata[key] != config[key] for key in ("pluginVersion", "javaTarget", "paperTarget", "paperBuild", "paperApiVersion", "paperChannel")) or metadata["buildNumber"] != build:
        raise RuntimeError("Rebuild first: artifact metadata does not match the checkout")
    stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ")
    output = (args.output or ROOT / f"servers/verification/build-{build}-{stamp}").resolve()
    output.mkdir(parents=True, exist_ok=False)
    report = {"artifact": str(artifact), "sha256": sha256(artifact), "metadata": metadata,
              "java_toolchains": runtimes, "runs": []}
    try:
        for index, jdk in enumerate(runtimes, start=1):
            java_home = Path(jdk["home"])
            version = jdk["version"]
            directory = output / f"java-{version}-{index}"
            report["paper_sha256"] = prepare_server(server, directory, artifact, metadata)
            for phase in ("initial", "restart", "without-placeholderapi", "without-bluemap"):
                papi = phase != "without-placeholderapi"
                missing = phase == "without-bluemap"
                if not papi:
                    dependency = next((directory / "plugins").glob("PlaceholderAPI-*.jar"))
                    dependency.rename(dependency.with_suffix(".jar.disabled"))
                if missing:
                    dependency = next((directory / "plugins").glob("PlaceholderAPI-*.jar.disabled"))
                    dependency.rename(dependency.with_suffix(""))
                    dependency = next((directory / "plugins").glob("bluemap-*.jar"))
                    dependency.rename(dependency.with_suffix(".jar.disabled"))
                report["runs"].append(run_server(directory, java_home.resolve(), version, metadata,
                                                 phase, papi, missing))
    except Exception as error:
        report["failure"] = str(error)
        raise
    finally:
        (output / "results.json").write_text(json.dumps(report, indent=2) + "\n")
    print(f"PASS: {len(report['runs'])} server lifecycles. Results: {output / 'results.json'}", flush=True)



if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""Prepare or verify a pinned, isolated Paper STABLE fixture without copying player data."""

import argparse
import hashlib
import json
from pathlib import Path
import re
import shutil
import tempfile
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
USER_AGENT = "MapHideCompatibility/2.0.2 (https://github.com/mrfdev/1MB-MapHide)"


def properties(text):
    return dict(line.split("=", 1) for line in text.splitlines()
                if "=" in line and not line.startswith("#"))


def sha256(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def validate_pins(config):
    if config["paperTarget"] != "26.3" or config["paperChannel"] != "STABLE":
        raise RuntimeError("The maintained target must remain Paper 26.3 STABLE")
    expected = f"{config['paperTarget']}.build.{config['paperBuild']}-stable"
    if config["paperApiVersion"] != expected:
        raise RuntimeError("Paper API does not match the stable server build")
    if not re.fullmatch(r"[0-9a-f]{64}", config["paperSha256"]):
        raise RuntimeError("Paper SHA-256 pin is invalid")


def verify_server(server, config):
    validate_pins(config)
    state = json.loads((server / "paper-release.json").read_text())
    for key in ("paperTarget", "paperBuild", "paperChannel", "paperApiVersion", "paperSha256"):
        if state.get(key) != config[key]:
            raise RuntimeError(f"Fixture {key} does not match the release pin")
    name = state["jar"]
    if Path(name).name != name or not name.endswith(".jar"):
        raise RuntimeError("Paper JAR must be inside the fixture")
    paper = server / name
    if paper.is_symlink() or sha256(paper) != config["paperSha256"]:
        raise RuntimeError("Paper JAR checksum mismatch")
    return paper


def fetch(url):
    return urllib.request.urlopen(urllib.request.Request(url, headers={"User-Agent": USER_AGENT}), timeout=60)


def runtime_plugins(config):
    bluemap = config["blueMapRuntimeVersion"]
    placeholderapi = config["placeholderApiVersion"]
    return {
        f"bluemap-{bluemap}-paper.jar": (
            "BlueMap-Minecraft/BlueMap", f"v{bluemap}", config["blueMapRuntimeSha256"]),
        f"PlaceholderAPI-{placeholderapi}.jar": (
            "PlaceholderAPI/PlaceholderAPI", placeholderapi, config["placeholderApiSha256"]),
    }


def verify_plugins(server, config):
    plugins = server / "plugins"
    pins = runtime_plugins(config)
    jars = sorted([*plugins.glob("bluemap-*.jar"), *plugins.glob("PlaceholderAPI-*.jar")])
    if {jar.name for jar in jars} != set(pins):
        raise RuntimeError("Fixture BlueMap and PlaceholderAPI JARs do not match the release pins")
    for jar in jars:
        if jar.is_symlink() or sha256(jar) != pins[jar.name][2]:
            raise RuntimeError(f"Fixture {jar.name} checksum mismatch")
    return jars


def prepare_server(source, destination, config):
    validate_pins(config)
    # Refuse existing destinations before downloading or copying anything.
    if destination.exists() or destination.is_symlink():
        raise RuntimeError(f"Refusing to replace an existing fixture: {destination}")
    if not re.search(r"(?m)^eula=true\s*$", (source / "eula.txt").read_text()):
        raise RuntimeError("Source fixture must already have an accepted Minecraft EULA")
    if not re.search(r"(?m)^accept-download:\s*true\s*$", (source / "plugins/BlueMap/core.conf").read_text()):
        raise RuntimeError("Source fixture must already accept BlueMap's resource download terms")
    url = f"https://fill.papermc.io/v3/projects/paper/versions/{config['paperTarget']}/builds/{config['paperBuild']}"
    with fetch(url) as response:
        build = json.load(response)
    download = build["downloads"]["server:default"]
    if (str(build["id"]) != config["paperBuild"] or build["channel"] != "STABLE"
            or download["checksums"]["sha256"] != config["paperSha256"]):
        raise RuntimeError("Official Fill metadata does not match the stable release pins")
    if not download["url"].startswith("https://fill-data.papermc.io/"):
        raise RuntimeError("Unexpected Paper download host")
    destination.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix=".paper-fixture-", dir=destination.parent) as staging:
        fixture = Path(staging) / destination.name
        (fixture / "plugins").mkdir(parents=True)
        paper = fixture / "paper.jar"
        with fetch(download["url"]) as response, paper.open("xb") as output:
            shutil.copyfileobj(response, output)
        if paper.stat().st_size != download["size"] or sha256(paper) != config["paperSha256"]:
            raise RuntimeError("Downloaded Paper JAR size/checksum mismatch")
        for name, (repo, tag, digest) in runtime_plugins(config).items():
            jar = fixture / "plugins" / name
            cached = source / "plugins" / name
            if cached.is_file() and sha256(cached) == digest:
                shutil.copy2(cached, jar)
            else:
                asset = f"https://github.com/{repo}/releases/download/{tag}/{name}"
                with fetch(asset) as response, jar.open("xb") as output:
                    shutil.copyfileobj(response, output)
        plugins = verify_plugins(fixture, config)
        populate_server(source, fixture, config)
        state = {key: config[key] for key in (
            "paperTarget", "paperBuild", "paperChannel", "paperApiVersion", "paperSha256")}
        state.update(jar="paper.jar", metadata_url=url, download=download,
                     plugins={jar.name: sha256(jar) for jar in plugins})
        (fixture / "paper-release.json").write_text(json.dumps(state, indent=2) + "\n")
        verify_server(fixture, config)
        if destination.exists() or destination.is_symlink():
            raise RuntimeError(f"Refusing to replace an existing fixture: {destination}")
        fixture.rename(destination)
    print(f"Prepared {destination}; no server has been started")


def populate_server(source, destination, config):
    shutil.copy2(source / "eula.txt", destination / "eula.txt")
    # Only runtime caches and plugin binaries; never worlds, users, credentials or plugin settings.
    for name in ("libraries", "versions", "cache"):
        if (source / name).is_dir():
            shutil.copytree(source / name, destination / name)
    (destination / "plugins/BlueMap").mkdir(parents=True)
    (destination / "plugins/BlueMap/core.conf").write_text(
        'accept-download: true\ndata: "bluemap"\nrender-thread-count: 1\nmetrics: false\n')
    (destination / "plugins/BlueMap/webserver.conf").write_text("enabled: false\n")
    (destination / "plugins/PlaceholderAPI").mkdir()
    (destination / "plugins/PlaceholderAPI/config.yml").write_text("check_updates: false\n")
    (destination / "plugins/bStats").mkdir()
    (destination / "plugins/bStats/config.yml").write_text("enabled: false\n")
    (destination / "bluemap").mkdir()
    client = source / f"bluemap/minecraft-client-{config['paperTarget']}.jar"
    if client.is_file():
        shutil.copy2(client, destination / "bluemap" / client.name)
    (destination / "server.properties").write_text(
        "server-ip=127.0.0.1\nserver-port=0\nenable-rcon=false\nenable-query=false\n"
        "online-mode=true\nlevel-name=world\n"
        "generate-structures=false\nview-distance=2\nsimulation-distance=2\n"
        "spawn-protection=0\npause-when-empty-seconds=-1\n")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", choices=("prepare", "verify"))
    parser.add_argument("--server", type=Path)
    parser.add_argument("--fixtures", type=Path, default=ROOT / "servers/Paper-26.3-stable-222")
    args = parser.parse_args()
    config = properties((ROOT / "gradle.properties").read_text())
    server = (args.server or ROOT / config["localPaperServerDir"]).absolute()
    if args.action == "prepare":
        prepare_server(args.fixtures.resolve(), server, config)
    else:
        paper = verify_server(server, config)
        verify_plugins(server, config)
        print(f"Verified Paper {config['paperTarget']} build {config['paperBuild']} STABLE: {paper}")


if __name__ == "__main__":
    main()

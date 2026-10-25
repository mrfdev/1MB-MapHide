#!/usr/bin/env python3
"""Select installed Oracle JDKs and launch this project's installed Gradle."""

import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]


def resolve_jdk(major: int, override: str | None = None) -> dict:
    variable = {25: "ONE_MB_BUILD_JAVA_HOME", 27: "ONE_MB_TEST_JAVA_HOME"}[major]
    selected = override if override is not None else os.environ.get(variable)
    if not selected:
        if sys.platform != "darwin":
            raise RuntimeError(f"Set {variable} to an installed Oracle JDK {major}; automatic discovery requires macOS")
        discovery = subprocess.run(["/usr/libexec/java_home", "-F", "-v", str(major)],
                                   capture_output=True, text=True)
        if discovery.returncode or not discovery.stdout.strip():
            raise RuntimeError(f"Installed Oracle JDK {major} unavailable; select it with {variable}. No JDK will be downloaded.")
        selected = discovery.stdout.strip()
    home = Path(selected).expanduser().resolve()
    java, javac = home / "bin/java", home / "bin/javac"
    if not all(path.is_file() and os.access(path, os.X_OK) for path in (java, javac)):
        raise RuntimeError(f"{variable}: {home} is not an installed JDK with executable java and javac")
    probe = subprocess.run([str(java), "-XshowSettings:properties", "-version"],
                           capture_output=True, text=True, timeout=30)
    properties = dict(re.findall(r"^\s*(java\.[\w.]+)\s*=\s*(.*?)\s*$",
                                 probe.stdout + probe.stderr, re.MULTILINE))
    if (probe.returncode or properties.get("java.specification.version") != str(major)
            or properties.get("java.vendor") != "Oracle Corporation"
            or Path(properties.get("java.home", "/missing")).resolve() != home):
        raise RuntimeError(f"{variable}: expected Oracle JDK {major} at {home}; found "
                           f"version={properties.get('java.version')}, vendor={properties.get('java.vendor')}, "
                           f"home={properties.get('java.home')}. No fallback or download is allowed.")
    compiler = subprocess.run([str(javac), "-version"], capture_output=True, text=True, timeout=30)
    compiler_version = re.search(r"^javac (\S+)", compiler.stdout + compiler.stderr, re.MULTILINE)
    if compiler.returncode or not compiler_version or compiler_version[1] != properties["java.version"]:
        raise RuntimeError(f"{variable}: javac does not match validated Oracle JDK {major} at {home}")
    return {"home": str(home), "java": str(java), "javac": str(javac), "major": major,
            "version": properties["java.version"], "runtime_version": properties["java.runtime.version"],
            "vendor": properties["java.vendor"]}


def validate_gradle_arguments(arguments):
    # Reject these before Gradle can start a daemon with an unvalidated JDK or download one.
    for argument in arguments:
        if ("org.gradle.java." in argument or "org.gradle.project.org.gradle.java." in argument
                or argument == "--configuration-cache"
                or "org.gradle.configuration-cache" in argument):
            raise RuntimeError("JDK selection and configuration caching are managed by build.sh; "
                               "use ONE_MB_BUILD_JAVA_HOME / ONE_MB_TEST_JAVA_HOME for installed JDK overrides")


def main():
    if sys.version_info < (3, 11):
        raise RuntimeError("Python 3.11+ is required")
    if sys.argv[1:] == ["--json"]:
        print(json.dumps({"build": resolve_jdk(25), "test": resolve_jdk(27)}))
        return
    arguments = sys.argv[1:]
    validate_gradle_arguments(arguments)
    criteria = ROOT / "gradle/gradle-daemon-jvm.properties"
    if criteria.exists():
        raise RuntimeError(f"Remove stale daemon JVM criteria from {criteria}; this project selects installed Oracle JDK 25 dynamically")
    build, test = resolve_jdk(25), resolve_jdk(27)
    gradle = shutil.which("gradle")
    if not gradle:
        raise RuntimeError("Install Gradle 9.8.0+ on PATH; build.sh never downloads build tools or JDKs")
    env = dict(os.environ, JAVA_HOME=build["home"], ONE_MB_BUILD_JAVA_HOME=build["home"],
               ONE_MB_TEST_JAVA_HOME=test["home"])
    env["PATH"] = str(Path(build["home"]) / "bin") + os.pathsep + env.get("PATH", "")
    for role, jdk in (("Gradle/compiler", build), ("Tests", test)):
        print(f"{role}: {jdk['vendor']} {jdk['runtime_version']} at {jdk['home']}", flush=True)
    os.chdir(ROOT)
    os.execve(gradle, [gradle, f"-Dorg.gradle.java.home={build['home']}", *arguments], env)


if __name__ == "__main__":
    try:
        main()
    except (RuntimeError, OSError, subprocess.SubprocessError) as error:
        sys.exit(f"Java toolchain error: {error}")

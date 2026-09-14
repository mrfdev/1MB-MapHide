# Installation And Building

## Requirements

- Paper server with BlueMap installed.
- Java 25 bytecode, built with JDK 25.0.4.1.
- Supported runtimes: Java 25.0.4.1 and Java 26.0.2.1. Live uses Java 26.
- Paper 26.2 build 84 stable is the maintained compile and release-test target.
- The exact compile coordinate is `io.papermc.paper:paper-api:26.2.build.84-stable`.
- BlueMapAPI `de.bluecolored:bluemap-api:2.8.0` and PlaceholderAPI `me.clip:placeholderapi:2.12.3` are resolved from their official Maven repositories.
- PlaceholderAPI is optional.

This release declares `api-version: 26.2` and is not intended for older Paper versions.

## Runtime Dependencies

Required:

- BlueMap

Optional:

- PlaceholderAPI, for `%maphide_*%` placeholders.
- A permissions plugin such as LuckPerms, for rank-specific access and forced visibility nodes.

## Install

1. Stop the server.
2. Put the built MapHide jar in `plugins/`.
3. Make sure BlueMap is also in `plugins/`.
4. Remove or disable old `BlueMapPlayerControl-*.jar`, `1MB-MapHide-*.jar`, or duplicate `1MB-BlueMap-MapHide-*.jar` files.
5. Start the server.
6. Review `plugins/1MB-MapHide/config.yml`.
7. Run `/bmpc info` and `/bmpc help`.
8. Test `/map hide` as a player.

## Update

1. Stop the server.
2. Disable or remove the old MapHide jar.
3. Install the new jar.
4. Start the server.
5. Run `/bmpc status`.
6. Review the startup log for MapHide, BlueMap, PlaceholderAPI, and command errors.

Existing `config.yml` values are preserved. Missing defaults and managed comments are added during reload/startup.

## Build

The normal local build command is:

```sh
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-25.0.4.1.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
gradle --no-daemon -PreleaseBuildNumber=029 clean build verifyLocalPaperServer
```

The jar name follows this pattern:

```text
1MB-BlueMap-MapHide-v2.0.1-<build>-j25-26.2.jar
```

`2.0.1` is the plugin version. The three-digit build number is stored in `build-number.txt`. An unpinned artifact build advances it once; `-PreleaseBuildNumber=029` makes release retries reuse build `029`. The `j25` and `26.2` parts identify the Java bytecode and Paper target.

The Gradle deploy task copies the jar only into the maintained local `servers/Paper-26.2/plugins/` folder and appends `.disabled` to older active MapHide jars.

The local server folder is not required to compile or package the plugin. It is only used by `deployServers` and `verifyLocalPaperServer`.

`gradle check` runs JDK selection, release-documentation, generated-artifact, and test tasks. The repository currently has no unit-test sources, so Gradle reports `test NO-SOURCE`; CI includes the task so future tests are not skipped. Compilation and test toolchains come from `JAVA_HOME`, with automatic discovery/download disabled. `verifyBuildJava` requires the configured JDK 25.0.4.1 for Gradle, the compiler, and the test launcher. CI installs that exact Oracle JDK and runs `clean build`.

`gradle verifyLocalPaperServer` validates PaperScript's stable channel, Paper build, installed jar, and saved SHA-256. Newer PaperScript stores a staged upgrade's checksum separately from the current jar. Both are checked without promoting the staged build. `-PlocalPaperServerDir=/path/to/fixture` selects an alternate verification fixture without changing the deployment destination.

## Runtime Verification

After the full build, run:

```sh
python3 scripts/smoke-test.py
```

The script requires Python 3.11 or newer and the maintained `servers/Paper-26.2` fixture: Paper build 84, its existing accepted EULA, cached server files/world, BlueMap 5.22 and its cached assets, and PlaceholderAPI 2.12.3. It copies these into a fresh timestamped `servers/verification/` directory for each run. The Minecraft listener binds to localhost and the copied BlueMap HTTP listener is disabled. Existing worlds, configuration, logs, historical results, and staged Paper jars are preserved.

By default it selects these exact local JDKs, setting both `JAVA_HOME` and `PATH` for each server process:

- `/Library/Java/JavaVirtualMachines/jdk-25.0.4.1.jdk/Contents/Home`
- `/Library/Java/JavaVirtualMachines/jdk-26.0.2.1.jdk/Contents/Home`

Use `--java25-home` and `--java26-home` for equivalent installations elsewhere, `--server` for a different fixture, and `--output` for a new results directory. The script validates the runtime versions and artifact/Paper metadata, then checks loading, BlueMap readiness, version/status/help/debug commands, console-only guards, missing players, invalid input, PlaceholderAPI expansion values, config edits and reloads, preserved comments/custom keys, and graceful shutdown with exit code 0. Logs and a JSON report are retained. A failed expectation, server error, or shutdown timeout fails the run.

These are local console smoke tests. Player joins, actual marker toggles, forced player permissions, and timer expiry require a connected player and are outside this automated check.

## Readiness Check

Use:

```text
/bmpc status
/bmpc debug status
/bmpc debug commands
/bmpc debug permissions
/bmpc debug placeholders
```

The status output should show the MapHide version/build, exact compiled Paper API, BlueMap version, server version, Java runtime, and target build.

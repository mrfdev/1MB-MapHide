# Installation And Building

## Requirements

- Paper server with BlueMap installed.
- Installed Oracle JDK 25 for Gradle and compilation, targeting Java 25 bytecode.
- Installed Oracle JDK 27 for all automated Java tests and Paper runtime verification.
- Installed Gradle 9.8.0+ and Python 3.11+ on PATH.
- Paper 26.3 build 41 alpha is the maintained compile and release-test target.
- The exact compile coordinate is `io.papermc.paper:paper-api:26.3.build.41-alpha`.
- BlueMapAPI `de.bluecolored:bluemap-api:2.8.0` and PlaceholderAPI `me.clip:placeholderapi:2.12.3` are resolved from their official Maven repositories.
- PlaceholderAPI is optional.

This release declares `api-version: 26.3` and is not intended for older Paper versions.

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
./build.sh --no-daemon -PreleaseBuildNumber=030 clean build verifyLocalPaperServer
```

The jar name follows this pattern:

```text
1MB-BlueMap-MapHide-v2.0.2-<build>-j25-26.3.jar
```

`2.0.2` is the plugin version. The three-digit build number is stored in `build-number.txt`. An unpinned artifact build advances it once; `-PreleaseBuildNumber=030` makes release retries reuse build `030`. The `j25` and `26.3` parts identify the Java bytecode and Paper target.

The Gradle deploy task copies the jar only into the maintained local `servers/Paper-26.3/plugins/` folder and appends `.disabled` to older active MapHide jars.

The local server folder is not required to compile or package the plugin. It is only used by `deployServers` and `verifyLocalPaperServer`.

`build.sh` is a local launcher for the already installed Gradle, not a downloading Gradle wrapper. It resolves `/usr/libexec/java_home -F -v 25` and `/usr/libexec/java_home -F -v 27` on every invocation, probes each installation's actual major version, Oracle Corporation vendor and matching `javac`, then sets Gradle's `JAVA_HOME` and daemon home to Oracle JDK 25. The shell's default Java does not select the build runtime.

All `JavaCompile` tasks, including test sources, explicitly select Oracle JDK 25 and `--release 25`. All `Test` tasks explicitly select Oracle JDK 27; their `JAVA_HOME` and `PATH` also select JDK 27 for child Java processes. `./build.sh --no-daemon verifyBuildJava` reports the actual paths, versions and vendors. Missing or invalid JDKs fail before any build tasks run.

To select a particular installed patch or use another machine, set `ONE_MB_BUILD_JAVA_HOME` and `ONE_MB_TEST_JAVA_HOME` to the installed Oracle homes. Both overrides undergo the same checks. For example, without embedding patch-version folders:

```sh
ONE_MB_BUILD_JAVA_HOME="$(/usr/libexec/java_home -F -v 25)" \
ONE_MB_TEST_JAVA_HOME="$(/usr/libexec/java_home -F -v 27)" \
./build.sh --no-daemon -PreleaseBuildNumber=030 clean build
```

Automatic Gradle toolchain discovery and downloads are disabled. Toolchains come only from the two validated environment variables. Settings reject conflicting project/system properties, alternate installation paths, and wrong bytecode targets before resolving toolchains. The launcher pins the Gradle daemon home and rejects JDK command-line overrides; use the two environment variables instead. Stale `gradle/gradle-daemon-jvm.properties` files are rejected because daemon criteria override `JAVA_HOME`. Configuration caching is disabled so validation runs each time. Raw `gradle` invocations must already have the same validated homes exported and run on Oracle 25; use `build.sh` for normal builds.

`./build.sh check` runs JDK selection, release/documentation drift checks, artifact/bytecode validation, Java toolchain regression tests, and Python resolver/smoke-selection regression tests. The Java tests execute a real Gradle test worker and a child JVM on Oracle 27 and inspect Java 25 class headers in plugin and test classes. Paper integration tests remain the separate smoke command below. The local GitHub Actions definition uses Oracle 25/27, Gradle 9.8.2, and the same launcher with explicit installed-home overrides from its setup steps. No remote CI run was triggered for this local change.

The selected Gradle must support Java 27 toolchains, which starts at 9.8.0 in the [official compatibility matrix](https://docs.gradle.org/current/userguide/compatibility.html). Toolchain tasks use the [official Gradle compiler and launcher APIs](https://docs.gradle.org/current/userguide/toolchains.html). See the [local verification record](verification/2026-10-25-oracle-toolchains.md) for the installed versions used.

`./build.sh verifyLocalPaperServer` validates PaperScript's explicitly selected ALPHA channel, Paper build, server directory, installed jar, and both saved and pinned official SHA-256. Newer PaperScript stores a staged upgrade's checksum separately from the current jar. Both are checked without promoting the staged build. `-PlocalPaperServerDir=/path/to/fixture` selects an alternate verification fixture without changing the deployment destination.

## Runtime Verification

After the full build, run:

```sh
python3 scripts/smoke-test.py
```

The script requires Python 3.11 or newer and the maintained `servers/Paper-26.3` fixture: Paper build 41 ALPHA, its existing accepted EULA, cached server files/world, `bluemap-5.27-paper.jar` (runtime 5.27, manifest 5.26), cached BlueMap assets, and PlaceholderAPI 2.12.3. It copies these into a fresh timestamped `servers/verification/` directory. The Minecraft listener binds to localhost on an available port; the copied BlueMap HTTP listener, RCON, and query listener are disabled. Only MapHide, BlueMap, and PlaceholderAPI are copied into this isolated smoke fixture, keeping external messaging integrations inactive.

The runtime is installed Oracle JDK 27, discovered using `java_home -F -v 27` or selected with `ONE_MB_TEST_JAVA_HOME`. Repeat `--java-home` to select installed Oracle 27 patches, use `--server` for another fixture, and `--output` for a new results directory. Other vendors and majors are rejected. The script validates runtime, artifact, channel, and official Paper checksum, then checks loading, BlueMap readiness, version/status/help/debug commands, console-only guards, missing players, invalid input, PlaceholderAPI values, config edits and reloads, comment/custom-key preservation, and graceful shutdown with exit code 0. Logs and a JSON report are retained. A failed expectation, server error, or shutdown timeout fails the run.

The optional CMI, CMILib, and Vault JARs are disabled in the maintained fixture after a third-party startup deadlock. Their data remains intact; see the verification record before re-enabling them. DiscordSRV is absent and other external messaging integrations stay inactive.

The maintained launcher `servers/Paper-26.3/1MB-minecraft.sh` selects Java 27 and Paper 26.3. Its PaperScript session name is `maphide-26-3-030`. Minecraft TCP uses `52227`; reserved query UDP, RCON TCP, and BlueMap HTTP ports are `52228`, `52229`, and `52230` respectively (those listeners are disabled for smoke testing). Check availability before subsequent manual launches. Start from this directory with `./1MB-minecraft.sh` and stop through the console with `stop`.

`servers/Paper-26.2` remains untouched rollback material. Its annotated tag is `v2.0.1-paper-26.2`. The new ALPHA target is deliberate; neither the build nor the launcher falls back to Paper 26.2. Use PaperScript's explicit `download --version 26.3 --build 41 --channel ALPHA` command for this pin; its `update` shortcut selects stable builds and is not the release workflow.

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

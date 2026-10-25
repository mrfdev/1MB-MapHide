# Installation And Building

## Requirements

- Paper server with BlueMap installed.
- Installed Oracle JDK 25 for Gradle and compilation, targeting Java 25 bytecode.
- Installed Oracle JDK 27 for all automated Java tests and Paper runtime verification.
- Installed Gradle 9.8.0+ and Python 3.11+ on PATH for build and fixture verification.
- Paper 26.3 build 222 stable is the maintained compile and release-test target.
- The exact compile coordinate is `io.papermc.paper:paper-api:26.3.build.222-stable`.
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
./build.sh --no-daemon -PreleaseBuildNumber=031 clean build
```

The jar name follows this pattern:

```text
1MB-BlueMap-MapHide-v2.0.2-<build>-j25-26.3.jar
```

`2.0.2` is the plugin version. The three-digit build number is stored in `build-number.txt`. An unpinned artifact build advances it once; `-PreleaseBuildNumber=031` makes release retries reuse build `031`. The `j25` and `26.3` parts identify the Java bytecode and Paper target.

A normal build does not deploy or start any server. `deployServers` explicitly copies the jar into the stopped `servers/Paper-26.3-stable-222/plugins/` fixture and appends `.disabled` to older active MapHide JARs. The `localPaperServerDir` Gradle property selects the development fixture for both verification and explicit deployment. Never point it at production.

Deployment preflight rejects a PaperScript launcher lock or a direct Java process using the fixture directory. It requires `lsof` and refuses deployment if process inspection fails. Keep the fixture stopped throughout deployment; the check does not coordinate with an external launcher starting concurrently.

`build.sh` is a local launcher for the already installed Gradle, not a downloading Gradle wrapper. It resolves `/usr/libexec/java_home -F -v 25` and `/usr/libexec/java_home -F -v 27` on every invocation, probes each installation's actual major version, Oracle Corporation vendor and matching `javac`, then sets Gradle's `JAVA_HOME` and daemon home to Oracle JDK 25. The shell's default Java does not select the build runtime.

All `JavaCompile` tasks, including test sources, explicitly select Oracle JDK 25 and `--release 25`. All `Test` tasks explicitly select Oracle JDK 27; their `JAVA_HOME` and `PATH` also select JDK 27 for child Java processes. `./build.sh --no-daemon verifyBuildJava` reports the actual paths, versions and vendors. Missing or invalid JDKs fail before any build tasks run.

To select a particular installed patch or use another machine, set `ONE_MB_BUILD_JAVA_HOME` and `ONE_MB_TEST_JAVA_HOME` to the installed Oracle homes. Both overrides undergo the same checks. For example, without embedding patch-version folders:

```sh
ONE_MB_BUILD_JAVA_HOME="$(/usr/libexec/java_home -F -v 25)" \
ONE_MB_TEST_JAVA_HOME="$(/usr/libexec/java_home -F -v 27)" \
./build.sh --no-daemon -PreleaseBuildNumber=031 clean build
```

Automatic Gradle toolchain discovery and downloads are disabled. Toolchains come only from the two validated environment variables. Settings reject conflicting project/system properties, alternate installation paths, and wrong bytecode targets before resolving toolchains. The launcher pins the Gradle daemon home and rejects JDK command-line overrides; use the two environment variables instead. Stale `gradle/gradle-daemon-jvm.properties` files are rejected because daemon criteria override `JAVA_HOME`. Configuration caching is disabled so validation runs each time. Raw `gradle` invocations must already have the same validated homes exported and run on Oracle 25; use `build.sh` for normal builds.

`./build.sh check` runs JDK selection, release/documentation drift checks, artifact/bytecode validation, Java toolchain regression tests, and Python fixture/resolver regression tests. The Java tests execute a real Gradle test worker and a child JVM on Oracle 27 and inspect Java 25 class headers in plugin and test classes. Paper integration tests remain the separate smoke command below.

The selected Gradle must support Java 27 toolchains, which starts at 9.8.0 in the [official compatibility matrix](https://docs.gradle.org/current/userguide/compatibility.html). Toolchain tasks use the [official Gradle compiler and launcher APIs](https://docs.gradle.org/current/userguide/toolchains.html). See the [local verification record](verification/2026-10-25-oracle-toolchains.md) for the installed versions used.

Paper API, BlueMap API, and PlaceholderAPI versions are pinned in `gradle.properties`; resolved transitive dependencies are recorded in `gradle.lockfile`. There is no paperweight/dev-bundle dependency or NMS use. Update the pins from official metadata and refresh the lock deliberately with `--write-locks` when upgrading.

Check `python3 --version` before building; Python 3.11 or newer must be on PATH for checksum verification and the JDK resolver.

## GitHub Actions

`.github/workflows/main.yml` runs on pushes, pull requests, and manual dispatch. It sets up Oracle 25/27 and Gradle 9.8.2, then uses the same launcher with explicit installed-home overrides and the checked-in release build number. CI runs `clean build` and uploads the plugin artifact. It does not deploy or start Paper servers.

GitHub disables inherited workflows on new forks until a maintainer enables them from the fork's **Actions** tab. If the page shows **Workflows aren't being run on this forked repository**, review the workflows and select **I understand my workflows, go ahead and enable them**. This activation was completed for `mrfdev/1MB-MapHide` on 2026-10-25. Before activation, GitHub recorded the push but created no build run, even though the Actions permissions API reported `enabled: true`, the workflow reported `active`, and a manual dispatch succeeded. See [GitHub's fork workflow documentation](https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows#workflows-in-forked-repositories).

After pushing, verify an automatic run for the exact commit from the repository root:

```sh
gh run list --workflow main.yml --commit "$(git rev-parse HEAD)" --event push \
  --json databaseId,headSha,event,status,conclusion,url
```

Wait for that run to complete successfully. An empty result means no push run was created; a successful `workflow_dispatch` run alone does not verify the push trigger. Identify the workflow by `main.yml` because GitHub may display its name from the default branch, which still calls it Java CI with Maven.

## Runtime Verification

Prepare the stable fixture once, then verify and test it:

```sh
python3 scripts/paper_fixture.py prepare
./build.sh --no-daemon verifyLocalPaperServer
python3 scripts/smoke-test.py
```

The preparer and smoke script both default to `localPaperServerDir`, currently `servers/Paper-26.3-stable-222`. Preparation refuses existing destinations. It checks the exact Fill build's STABLE channel, build ID, download size, and pinned SHA-256. `paper-release.json` records the verified build and source plugin checksums. `verifyLocalPaperServer` fails if the fixture is absent, has stale release metadata, or its JAR checksum differs from the release pin. It never silently skips verification.

Provisioning requires a source test fixture with an already accepted Minecraft EULA, already accepted BlueMap resource terms, one BlueMap runtime JAR and one PlaceholderAPI JAR. By default it reads `servers/Paper-26.3`; use `--fixtures /path/to/test-fixture` for another. It copies only binaries, runtime caches, the accepted EULA, and cached Minecraft client assets. Fresh configuration disables HTTP, RCON, query, telemetry, and external integrations. Worlds, player records, credentials, and other plugins' configuration are not imported. `--server /new/path` prepares a different new destination; use the matching `-PlocalPaperServerDir=/new/path` and smoke `--server /new/path` for verification.

The smoke script copies this stable fixture into a new timestamped `servers/verification/` directory and generates disposable worlds. Minecraft binds to localhost on an available port. It checks the release artifact and server checksum, plugin loading, BlueMap readiness, info/status/help/debug commands, player-only guards, missing players, invalid selectors/pages/config keys, PlaceholderAPI values, config edits/reloads, preserved comments/custom keys, and clean shutdown. It then restarts the same disposable world to verify persisted nondefault settings. Further lifecycles verify operation without PlaceholderAPI and the expected missing-BlueMap dependency rejection. Unexpected errors, linkage failures, shutdown timeouts, or nonzero exits fail the run; JSON results and separate console logs survive failures.

The default runtime is the installed Oracle JDK 27 discovered with `java_home -F -v 27`, or `ONE_MB_TEST_JAVA_HOME` when set. Repeat `--java-home /path/to/jdk` to choose other installed Oracle 27 patches (other vendors or majors are rejected), and use `--output /new/results/path` to select a new output directory. Tests stop every process they start. No existing launcher, shared test server, or production service is started or stopped.

`servers/Paper-26.3-stable` (build 175), `servers/Paper-26.3`, and `servers/Paper-26.2` remain historical fixtures. Their old launchers are not the current stable development workflow. The Paper 26.2 baseline is also preserved at `v2.0.1-paper-26.2`. Current verification is recorded in the [build 222 report](verification/2026-10-25-paper-26.3-stable-222.md).

These are console integration tests. Actual player marker changes, permission denials, forced permission conflicts, timer expiry, reconnects, and the full third-party stack need the [manual checks](verification/2026-10-11-paper-26.3-stable.md#manual-verification). Inventories, item metadata, economy transactions, and LuckPerms command delivery are not MapHide features.

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

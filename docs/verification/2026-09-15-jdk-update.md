# JDK update verification, 2026-09-15

## Release

- Plugin version: `2.0.1`, build `029` (advanced once from `028`; retries pinned to `029`).
- Artifact: `1MB-BlueMap-MapHide-v2.0.1-029-j25-26.2.jar`.
- Built and deployed copies: `build/libs/` and `servers/Paper-26.2/plugins/`.
- SHA-256: `7ce1dccb3f5eec4ca6785f870d629a45e0ff94658fe1e33a4dca00b670a82a31`.
- Paper API/runtime: `26.2.build.84-stable` / `26.2-84-26e81c4`.
- Paper jar SHA-256: `defe82c1c89067186895de34cf32983e9f5a2ea387cfe7597c020faebb98ca16`.
- BlueMap `5.22`, compile API `2.8.0`; this installed BlueMap reports runtime API label `DEV`.
- PlaceholderAPI `2.12.3`.

## Build and automated checks

```sh
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-25.0.4.1.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
gradle --no-daemon -PreleaseBuildNumber=029 clean build verifyLocalPaperServer
python3 scripts/smoke-test.py
```

The canonical full rebuild passed with Gradle `9.7.1`. Gradle, compilation, and the test launcher all selected Oracle JDK `25.0.4.1+1-LTS-5`. The semantic plugin version and Paper target were retained. Build retries reused `029`.

Release/documentation metadata, generated plugin metadata, and all 12 class files passed verification. Every class is version `69.0` (Java 25), with no preview bytecode. The built and deployed copies have the same checksum as the artifact used in both runtime tests.

Gradle ran the test task and reported `NO-SOURCE`: this repository has no unit-test sources. CI now runs `clean build` with Oracle JDK `25.0.4.1` and includes the test task. The existing Gradle `9.6.1` CI pin remains unchanged.

Six additional build-guard checks passed using separate fixtures:

- Legacy PaperScript state and filename configuration accepted.
- Incorrect current Paper checksum rejected.
- Incorrect staged Paper checksum rejected.
- Incorrect current Paper build rejected.
- Non-stable Paper channel rejected.
- Building with JDK `26.0.2.1` rejected with the required JDK `25.0.4.1` message. Java 26 remains a supported server runtime.

The maintained PaperScript state has build `121` staged. Its checksum is checked independently, without promoting it or changing the build `84` release target.

## Runtime matrix

| Runtime | Exact Oracle runtime/VM build | Paper startup and plugin loading | Smoke checks | Shutdown |
| --- | --- | --- | --- | --- |
| Java 25.0.4.1 | `25.0.4.1+1-LTS-5` | Passed | 30 passed | Graceful, exit 0 |
| Java 26.0.2.1 | `26.0.2.1+1-7` | Passed | 30 passed | Graceful, exit 0 |

Both JDKs identify their release date as `2026-08-18`. The Java 25 installation is `/Library/Java/JavaVirtualMachines/jdk-25.0.4.1.jdk/Contents/Home`; Java 26 is `/Library/Java/JavaVirtualMachines/jdk-26.0.2.1.jdk/Contents/Home`. Live uses Java 26; these checks ran locally.

Coverage includes MapHide enable/disable, BlueMap readiness, PlaceholderAPI registration, exact build/Paper/Java diagnostics, info/version alias, paged help, all debug categories, config listing, console-only player guards (including `/map hide`), missing-player handling, invalid inputs, placeholder evaluation, config updates, reloads, comment/custom-key preservation, final diagnostics, world saves, and clean process exit. No server errors or linkage exceptions occurred.

The first smoke attempt failed because the new test expected BlueMap's runtime API label to equal the compile dependency version. The assertion was corrected to verify BlueMap readiness/version, and the complete matrix passed on rerun. Logs preserve both attempts.

Existing upstream warnings remained: JOML's deprecated Unsafe call, OSHI's unrecognized macOS 27 name, the copied server's manual-save warning, and Paper's newer-build notice. They did not prevent loading or clean shutdown. No plugin runtime code changed.

These console smoke tests do not simulate connected players. Actual marker toggling, player permission enforcement, join behavior, and timer expiry were not exercised.

## Local evidence and preservation

- Build and guard logs: `servers/verification/jdk-update-029/`.
- Passing runtime report: `servers/verification/build-029-20260914T222329Z/results.json`.
- Per-runtime console and Java-version logs: subdirectories of that passing runtime report.
- Prior test logs, maintained world/config files, staged Paper jars, and disabled plugin artifacts were preserved. New runtime tests used fresh copied fixtures. The ignored local launcher comments were refreshed to the installed Java patch versions.
- This record is historical; later releases should add a new record rather than rewrite these results.

## Documentation checked

The official [Paper LLM index](https://docs.papermc.io/llms.txt) was the discovery source for [project setup](https://docs.papermc.io/paper/dev/project-setup/), [Java runtime requirements](https://docs.papermc.io/paper/getting-started/), and the [Paper 26.2 plugin lifecycle API](https://jd.papermc.io/paper/26.2/org/bukkit/plugin/java/JavaPlugin.html). The public 26.2 Javadoc alias currently describes build 123; build 84 was retained and verified from the pinned dependency, artifact metadata, saved checksum, and actual startup diagnostics.

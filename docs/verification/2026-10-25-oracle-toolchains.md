# Oracle JDK toolchain verification, 2026-10-25

Verification below was performed on the combined working tree, including the separate, pre-existing Paper 26.3 build 175 STABLE / release 031 upgrade. The Oracle-only commit leaves that upgrade uncommitted and retains the previous committed Paper build 41 ALPHA / release 030 pins. The four-lifecycle smoke suite, fixture tests, and dependency lockfile belong to the separate upgrade; their results below describe the combined working tree. Gradle and all Java compilation use Oracle 25; automated Java tests and disposable Paper integration servers use Oracle 27.

## Installed toolchains observed

These paths are evidence from this run, not installation pins. The launcher discovers both major versions afresh with `/usr/libexec/java_home -F -v 25` and `-v 27`, or validates explicit `ONE_MB_BUILD_JAVA_HOME` / `ONE_MB_TEST_JAVA_HOME` selections.

| Role | Actual runtime version | Vendor | Observed installation |
| --- | --- | --- | --- |
| Gradle 9.8.2 and all Java compilers | 25.0.5.0.3+1-LTS-2 | Oracle Corporation | `/Library/Java/JavaVirtualMachines/jdk-25.0.5.0.3.jdk/Contents/Home` |
| Gradle test worker, child JVM, and Paper smoke servers | 27.0.1.0.3+1-2 | Oracle Corporation | `/Library/Java/JavaVirtualMachines/jdk-27.0.1.0.3.jdk/Contents/Home` |

Actual compiler: `/Library/Java/JavaVirtualMachines/jdk-25.0.5.0.3.jdk/Contents/Home/bin/javac`. Actual test executable: `/Library/Java/JavaVirtualMachines/jdk-27.0.1.0.3.jdk/Contents/Home/bin/java`. Both `javac -version` probes matched their installation's `java.version`.

Gradle 9.8.2 was already installed; no build-tool or JDK installation was required. Gradle 9.8.0 is the minimum for Java 27 toolchains in the [official compatibility matrix](https://docs.gradle.org/current/userguide/compatibility.html). The build uses supported [compiler and test launcher toolchains](https://docs.gradle.org/current/userguide/toolchains.html).

## Verification

The initial clean build refreshed the dependency lock only to include JUnit 4.13.2, Hamcrest 1.3, and the test configurations. The final run used the normal locked workflow:

```sh
./build.sh --no-daemon --warning-mode all -PreleaseBuildNumber=031 clean build verifyLocalPaperServer
python3 scripts/smoke-test.py
```

- Clean build: passed, 12 tasks executed. No compiler or Gradle deprecation warnings appeared in the final build log.
- Java tests: 3 passed. They checked the real Gradle worker's Oracle vendor, Java 27 runtime and canonical installation path; plugin/test class headers; and a separately forked Oracle 27 JVM at the selected home.
- Python regression suite: 28 passed, covering fixture validation, smoke error handling, JDK discovery and rejection, and smoke runtime selection.
- Plugin artifact: all 12 classes have major version 69, minor version 0 (Java 25, no preview features). Both generated test classes also have version 69.0. All JavaCompile tasks explicitly use `--release 25` and the Oracle 25 compiler.
- Isolated Paper fixture: checksum and release-pin verification passed.
- Paper smoke suite: all 4 lifecycles passed, with 93 recorded checks. Initial startup, same-world restart/persistence, operation without PlaceholderAPI, and expected missing-BlueMap rejection all used the validated Oracle 27 installation and exited cleanly.
- Selection guards: 15/15 passed. Missing and wrong-major homes, project major/target overrides, launcher `-P`/`-D` download attempts, direct Gradle download attempts, and direct Gradle on Oracle 27 failed before build tasks. Unset or invalid shell `JAVA_HOME` still selected Oracle 25 for Gradle. Explicit installed-home overrides passed.
- `git diff --check` and `sh -n build.sh`: passed.

Artifact: `build/libs/1MB-BlueMap-MapHide-v2.0.2-031-j25-26.3.jar`

SHA-256: `b46e7edb3f2c67a8467cc504f635d51df4252cf993d8bf93c3548944cb65a87e`

Local evidence:

- [Clean build log](../../.scratch/java-toolchains/clean-build.log)
- [Java test report](../../build/reports/tests/test/index.html)
- [Java test XML with actual worker/fork metadata](../../build/test-results/test/TEST-com.onemb.toolchain.JavaToolchainTest.xml)
- [Guard results](../../.scratch/java-toolchains/guard-checks.json)
- [Smoke results](../../servers/verification/build-031-20261025T095855Z/results.json)
- [Smoke command log](../../.scratch/java-toolchains/paper-smoke.log)

## Configuration audit and limits

The stale project patch pin and `fromEnv=JAVA_HOME` single-JDK selection were replaced. No user-level Gradle properties, initialization scripts, daemon JVM criteria, IDE JVM overrides, or Maven build configuration needed migration. The shell selected Oracle 27, and Homebrew Gradle would otherwise default to Homebrew OpenJDK when JAVA_HOME was absent; the launcher explicitly selects Oracle 25 before invoking Gradle.

Toolchain auto-detection and auto-download are disabled. Conflicting project/system properties and daemon criteria fail clearly. Configuration caching is disabled so installation validation runs on every invocation. No JDK was downloaded, no server deployment occurred, and no existing shared or historical server was launched.

The active GitHub Actions definition was updated locally to supply Oracle 25/27 homes and Gradle 9.8.2 through the same launcher. It was not pushed or run remotely. No issues or pull requests were created. A subsequent user request authorized a local commit of only the Oracle setup, with the Paper upgrade kept separate.

Only one installed Oracle patch per required major was available during the final override checks, so switching between two real patch installations could not be exercised. Explicit selection of the installed homes and mocked patch-independent discovery passed. Historical verification records retain their original runtimes. Player-driven marker, permission, timer, reconnect, and full third-party-stack behavior remains covered by the existing manual verification scope in the separate Paper upgrade, not these console tests.

## Standalone commit verification

Before committing, the Oracle-only changes were applied to an isolated copy of the original committed Paper build 41 ALPHA / release 030 tree. `./build.sh --no-daemon -PreleaseBuildNumber=030 clean check` passed there: 3 Java tests, 15 Python toolchain/selection tests, release metadata checks, and all 12 plugin classes at Java 25 bytecode. No deployment or Paper server launch was performed from that snapshot. Its [check log](../../.scratch/java-toolchains/commit/selected-check.log) is retained locally. The existing stable upgrade, release-number change, fixture tooling/tests, stable verification record, and dependency lockfile were excluded from the commit.

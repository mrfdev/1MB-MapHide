## Agent skills

### Issue tracker

Issues and specs are tracked in GitHub Issues using the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Domain docs

This repository uses the single-context domain-doc layout. See `docs/agents/domain.md`.

## Maintained Paper release

- Target Paper 26.3 build 41 ALPHA and `io.papermc.paper:paper-api:26.3.build.41-alpha`. This experimental channel is explicitly authorized; do not silently substitute a stable 26.2 build.
- Use installed Gradle 9.8.0+ through `./build.sh`. It resolves `/usr/libexec/java_home -F -v 25` and `-v 27` on every invocation and validates actual versions, Oracle Corporation vendor, and matching javac. Gradle and all Java compilation use Oracle JDK 25 with `--release 25`; every Java test task and disposable Paper smoke server uses Oracle JDK 27.
- Select another installed patch with `ONE_MB_BUILD_JAVA_HOME` / `ONE_MB_TEST_JAVA_HOME`; never pin patch-version folders in executable configuration. Missing, wrong-major, or non-Oracle selections fail. Automatic toolchain detection/downloads are disabled. Do not override the validated toolchain properties or add daemon JVM criteria. Use `./build.sh verifyBuildJava` to print the actual paths, versions, and vendors. Configuration caching is disabled to preserve validation on every invocation.
- Release 2.0.2 reserves build `030`. Run `./build.sh --no-daemon -PreleaseBuildNumber=030 clean build verifyLocalPaperServer`; reuse that number on retries. The prepareBuildNumber task owns increments.
- Maintain `servers/Paper-26.3` on dynamically selected installed Oracle JDK 27; keep `servers/Paper-26.2` unchanged as rollback material. Stop the local launcher before building because build deploys locally.
- Run `python3 scripts/smoke-test.py` after building. It resolves and validates installed Oracle JDK 27 independently and uses disposable localhost servers. See `docs/installation.md` and `docs/verification/2026-10-25-oracle-toolchains.md` for the workflow and verification evidence. Historical records describe their original targets.
- Read the live Paper LLM index, official docs, roadmap, and exact target Javadocs for future upgrades. Pin the server checksum and actual channel, and review compiler deprecations and exploit-protection defaults.
- The shared test server must stay stopped during artifact replacement. Match this project's `1MB-MapHide` plugin identity and main class, replace only its JARs with backups and checksum verification, and never launch the shared server as part of deployment.

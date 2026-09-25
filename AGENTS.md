## Agent skills

### Issue tracker

Issues and specs are tracked in GitHub Issues using the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Domain docs

This repository uses the single-context domain-doc layout. See `docs/agents/domain.md`.

## Maintained Paper release

- Target Paper 26.3 build 41 ALPHA and `io.papermc.paper:paper-api:26.3.build.41-alpha`. This experimental channel is explicitly authorized; do not silently substitute a stable 26.2 build.
- Build with `JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-25.0.4.1.jdk/Contents/Home` and its `bin` prepended to `PATH`. Keep the Java 25 toolchain and bytecode target.
- Release 2.0.2 reserves build `030`. Run `gradle --no-daemon -PreleaseBuildNumber=030 clean build verifyLocalPaperServer`; reuse that number on retries. The prepareBuildNumber task owns increments.
- Maintain `servers/Paper-26.3` on `/Library/Java/JavaVirtualMachines/jdk-27.jdk/Contents/Home`; keep `servers/Paper-26.2` unchanged as rollback material. Stop the local launcher before building because build deploys locally.
- Run `python3 scripts/smoke-test.py` after building. See `docs/installation.md` and `docs/verification/2026-09-25-paper-26.3.md` for runtime scope and evidence. Historical records describe their original targets.
- Read the live Paper LLM index, official docs, roadmap, and exact target Javadocs for future upgrades. Pin the server checksum and actual channel, and review compiler deprecations and exploit-protection defaults.
- The shared test server must stay stopped during artifact replacement. Match this project's `1MB-MapHide` plugin identity and main class, replace only its JARs with backups and checksum verification, and never launch the shared server as part of deployment.

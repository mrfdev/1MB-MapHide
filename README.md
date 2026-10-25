# 1MB-MapHide

1MB-MapHide is a Paper addon for [BlueMap](https://github.com/BlueMap-Minecraft/BlueMap) that lets players control whether their live marker is shown on the map. It is a 1MB-customized fork of [TechnicJelle's BlueMapPlayerControl](https://github.com/TechnicJelle/BlueMapPlayerControl).

The main command is `/bmpc`. Players can use `/bmpc`, `/bmpc toggle`, `/bmpc show`, and `/bmpc hide`; admins can use target, status, config, and reload subcommands. `/map hide` is a configurable alias that runs the same self-toggle logic as `/bmpc toggle`.

Canonical player docs URL:

https://docs.1moreblock.com/custom-server-plugins/maphide/

## Documentation

- [Player guide](docs/player-guide.md)
- [Commands](docs/commands.md)
- [Permissions](docs/permissions.md)
- [Placeholders](docs/placeholders.md)
- [Configuration](docs/configuration.md)
- [Installation and building](docs/installation.md)
- [Integrations](docs/integrations.md)
- [Troubleshooting](docs/troubleshooting.md)

## Changelog Summary

Version `2.0.2` targets Paper 26.3 build 41 on its experimental ALPHA channel, with the exact API `io.papermc.paper:paper-api:26.3.build.41-alpha`. Build `030` retains Java 25 bytecode and is tested on Java 27. The Paper 26.2 baseline is preserved at annotated tag `v2.0.1-paper-26.2` and in `servers/Paper-26.2`. Plugin identity, commands, permissions, configuration, player visibility, and PlaceholderAPI behavior are unchanged.

## Supported Servers

This build targets Java 25 bytecode and the exact Paper API coordinate `io.papermc.paper:paper-api:26.3.build.41-alpha`. Paper 26.3 build 41 alpha is the maintained release-test server:

| Server | BlueMap plugin |
| --- | --- |
| Paper 26.3 build 41 alpha | `bluemap-5.27-paper.jar` (runtime 5.27, manifest 5.26) |

The generated plugin metadata declares `api-version: 26.3`, so this release is not intended for older Paper versions. Use an older MapHide build for Paper 26.1.2 or 1.21.11.

## Configuration

The Paper plugin name is `1MB-MapHide`, so the data folder is:

```text
plugins/1MB-MapHide/
```

Default files:

- `plugins/1MB-MapHide/config.yml`
- `plugins/1MB-MapHide/Translations/Locale_EN.yml`

Set `language: EN` in `config.yml`. To add French, create `Translations/Locale_FR.yml`, set `language: FR`, then restart or run `/bmpc reload`.

Important settings:

| Key | Default | Description |
| --- | --- | --- |
| `language` | `EN` | Translation file suffix. |
| `bmpc-toggle-alias` | `/map hide` | Alias command that runs `/bmpc toggle`; set to `""` to disable. |
| `default-visibility` | `show` | `show` or `hide` for newly handled joins. |
| `apply-default-visibility-on-first-join-only` | `true` | Applies default only to first joins when true. |
| `toggle-back-after-seconds` | `0` | Seconds after a self toggle before toggling again. `0` disables it. |
| `forced-permissions.enabled` | `false` | Enables forced visibility permission handling. |
| `forced-permissions.hide-node` | `maphide.forcehide` | Permission that forces a player hidden. |
| `forced-permissions.show-node` | `maphide.forceshow` | Permission that forces a player visible. |
| `forced-permissions.conflict-priority` | `hide` | Winner if a player has both force permissions. |
| `forced-permissions.check-interval-seconds` | `0` | Optional repeating force-permission check; only raise it if forced visibility is being undone. |

## Commands

| Command | Description | Permission |
| --- | --- | --- |
| `/bmpc` | Legacy self toggle. | `maphide.player`, `maphide.player.toggle` |
| `/bmpc help [page]` | Shows paged command help. | `maphide.player`, `maphide.player.help` |
| `/bmpc info` | Shows plugin info, starting commands, version/build, and docs URL. | `maphide.player`, `maphide.player.info` |
| `/bmpc version` | Alias for `/bmpc info`. | `maphide.player`, `maphide.player.info` |
| `/bmpc toggle` | Toggles your own BlueMap visibility. | `maphide.player`, `maphide.player.toggle` |
| `/bmpc show` | Shows your own BlueMap marker. | `maphide.player`, `maphide.player.show` |
| `/bmpc hide` | Hides your own BlueMap marker. | `maphide.player`, `maphide.player.hide` |
| `/map hide` | Configurable alias for `/bmpc toggle`. | `maphide.player`, `maphide.player.toggle` |
| `/bmpc toggle <player>` | Toggles another online player. | `maphide.admin.toggle` |
| `/bmpc show <player>` | Shows another online player. | `maphide.admin.show` |
| `/bmpc hide <player>` | Hides another online player. | `maphide.admin.hide` |
| `/bmpc status` | Shows plugin version, build number, BlueMap, server, Java, and target build information. | `maphide.admin.status` |
| `/bmpc status <player>` | Shows visibility, forced state, world, coordinates, and timer. | `maphide.admin.status` |
| `/bmpc config [page]` | Lists config values with pagination. | `maphide.admin.config` |
| `/bmpc config set <key> <value>` | Updates a config key and reloads. | `maphide.admin.set` |
| `/bmpc debug [page]` | Lists debug categories with pagination. | `maphide.admin.debug` |
| `/bmpc debug status` | Shows server, plugin, and MapHide setting diagnostics. | `maphide.admin.debug.status` |
| `/bmpc debug commands [page]` | Lists command routes and permission gates. | `maphide.admin.debug.commands` |
| `/bmpc debug permissions [page]` | Lists permission nodes, defaults, descriptions, and sender state. | `maphide.admin.debug.permissions` |
| `/bmpc debug placeholders [page]` | Lists PlaceholderAPI state and placeholders. | `maphide.admin.debug.placeholders` |
| `/bmpc reload` | Reloads config and translations. | `maphide.admin.reload` |

Player arguments support exact online player names. Selector arguments `@a`, `@p`, `@r`, and `@s` are available to admins.

## Permissions

| Permission | Default | Description |
| --- | --- | --- |
| `maphide.player` | `true` | Allows access to `/bmpc` player commands. |
| `maphide.player.help` | `true` | Allows `/bmpc help`. |
| `maphide.player.info` | `true` | Allows `/bmpc info` and `/bmpc version`. |
| `maphide.player.toggle` | `true` | Allows `/bmpc`, `/bmpc toggle`, and the configured alias. |
| `maphide.player.show` | `true` | Allows `/bmpc show`. |
| `maphide.player.hide` | `true` | Allows `/bmpc hide`. |
| `maphide.forcehide` | `false` | Forces the player hidden regardless of commands. |
| `maphide.forceshow` | `false` | Forces the player visible regardless of commands. |
| `maphide.admin` | `op` | Parent admin permission. |
| `maphide.admin.toggle` | `op` | Allows `/bmpc toggle <player>`. |
| `maphide.admin.show` | `op` | Allows `/bmpc show <player>`. |
| `maphide.admin.hide` | `op` | Allows `/bmpc hide <player>`. |
| `maphide.admin.status` | `op` | Allows `/bmpc status` and `/bmpc status <player>`. |
| `maphide.admin.config` | `op` | Allows `/bmpc config`. |
| `maphide.admin.set` | `op` | Allows `/bmpc config set <key> <value>`. |
| `maphide.admin.reload` | `op` | Allows `/bmpc reload`. |
| `maphide.admin.debug` | `op` | Parent debug permission for `/bmpc debug`. |
| `maphide.admin.debug.status` | `op` | Allows `/bmpc debug status`. |
| `maphide.admin.debug.commands` | `op` | Allows `/bmpc debug commands`. |
| `maphide.admin.debug.permissions` | `op` | Allows `/bmpc debug permissions`. |
| `maphide.admin.debug.placeholders` | `op` | Allows `/bmpc debug placeholders`. |

## Placeholders

PlaceholderAPI is optional. When installed, 1MB-MapHide registers:

| Placeholder | Description |
| --- | --- |
| `%maphide_visible%` | `true`, `false`, or `unknown`. |
| `%maphide_state%` | `visible`, `hidden`, or `unknown`. |
| `%maphide_forced%` | Whether the player has a force permission. |
| `%maphide_force_mode%` | `hide`, `show`, or `none`. |
| `%maphide_default_visibility%` | Current configured default: `visible` or `hidden`. |
| `%maphide_toggle_back_seconds%` | Configured auto-toggle seconds. |
| `%maphide_toggle_back_remaining%` | Seconds left on the player's active timer. |
| `%maphide_language%` | Active language code. |

## Building

Requirements:

- Installed Gradle 9.8.0 or newer
- Python 3.11 or newer on PATH
- Installed Oracle JDK 25 for Gradle and compilation, and Oracle JDK 27 for automated tests
- The exact Maven dependency `io.papermc.paper:paper-api:26.3.build.41-alpha`
- The Maven dependencies `de.bluecolored:bluemap-api:2.8.0` and `me.clip:placeholderapi:2.12.3`

A clean checkout can compile without a local Paper server. The local `servers/Paper-26.3` folder is only needed for `deployServers` and `verifyLocalPaperServer`.

Build release `030` and deploy it to the maintained Paper 26.3 test server:

```sh
./build.sh --no-daemon -PreleaseBuildNumber=030 clean build verifyLocalPaperServer
```

Jars are named like:

```text
1MB-BlueMap-MapHide-v2.0.2-030-j25-26.3.jar
```

The plugin version comes from `gradle.properties`. The three-digit build number is stored in `build-number.txt` and advances for a new unpinned jar build. Pin `releaseBuildNumber` when repeating a release build so retries reuse the intended number instead of incrementing it again. The Java and Paper parts of the jar name are target identifiers, not the semantic plugin version.

`build.sh` dynamically resolves installed Oracle JDK 25 and Oracle JDK 27 with macOS `java_home -F -v`, validates their major versions and Oracle vendor, and runs the installed Gradle on JDK 25. All plugin and test sources compile with JDK 25 and `--release 25`; all Java test tasks and their Java subprocesses use JDK 27. `verifyBuildJava` checks the actual toolchain paths. No JDKs are downloaded or silently substituted. Explicit installed-home overrides use `ONE_MB_BUILD_JAVA_HOME` and `ONE_MB_TEST_JAVA_HOME`; see the [build workflow](docs/installation.md#build). `verifyReleaseMetadata` checks source and documentation values. `verifyArtifactMetadata` checks the generated `plugin.yml`, build information, jar name, and every class for Java 25 bytecode without preview features. All run as part of `check`.

After a successful `build`, `deployServers` copies the jar into `servers/Paper-26.3/plugins/`. Before copying, it renames active older MapHide jars in that folder by appending `.disabled`.

Run `python3 scripts/smoke-test.py` after building to verify Paper startup, plugin loading, console commands, configuration reloads, placeholders, and clean shutdown on JDK 27. It uses an isolated copy of `servers/Paper-26.3` and saves logs under `servers/verification/`. See [runtime verification](docs/installation.md#runtime-verification) for coverage and [build 030 evidence](docs/verification/2026-09-25-paper-26.3.md) for results. Compilation uses Oracle JDK 25; Java tests and smoke servers use dynamically selected Oracle JDK 27. Set `ONE_MB_TEST_JAVA_HOME` or pass `--java-home` to select another installed Oracle 27 patch.

## Installation

1. Stop the Paper server.
2. Put `1MB-BlueMap-MapHide-v2.0.2-030-j25-26.3.jar` in `plugins/`.
3. Make sure BlueMap is also in `plugins/`.
4. Remove or disable old `BlueMapPlayerControl-*.jar` copies.
5. Start the server.
6. Configure `plugins/1MB-MapHide/config.yml`.
7. In game, run `/bmpc` or `/map hide` to toggle your marker.

## Credits

- Original addon: [BlueMapPlayerControl](https://github.com/TechnicJelle/BlueMapPlayerControl) by TechnicJelle.
- Map plugin: [BlueMap](https://github.com/BlueMap-Minecraft/BlueMap) by the BlueMap project.
- 1MB customization and `/map hide` workflow: [mrfloris](https://github.com/mrfloris).
- Development assistance: OpenAI.

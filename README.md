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

Version `2.0.0` modernizes the original BlueMapPlayerControl addon into 1MB-MapHide for Paper 1.21.11 and 26.1.2. It moves the project to Gradle and Java 25, keeps `/bmpc` as the main legacy-compatible command, adds configurable `/map hide` alias support, replaces legacy color codes with MiniMessage translations, introduces `plugins/1MB-MapHide/config.yml` and `Translations/Locale_EN.yml`, adds a comment-preserving config loader, adds the `maphide.*` permission model, supports forced visibility permissions, optional timed self-toggle behavior, default join visibility, paged help/info/debug commands, admin config/status commands, server and player diagnostics through `/bmpc status` and `/bmpc debug status`, PlaceholderAPI placeholders, fallback translation keys for older locale files, and automatic local test-server deployment with build-numbered jars.

## Supported Servers

This build is compiled with Java 25 and targets the local Paper 26.1.2 API. The current code path uses BlueMap API calls present in the local test setups:

| Server | BlueMap plugin |
| --- | --- |
| Paper 1.21.11 | `bluemap-5.16-paper.jar` |
| Paper 26.1.2 | `bluemap-5.20-paper.jar` |
| Paper 26.2 | `bluemap-5.20-paper.jar` |

Paper 26.1.2 remains the main target. Paper 26.2 is an experimental compatibility test target for the current 26.1.2-built jar.

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
| `maphide.player.info` | `true` | Allows `/bmpc info`. |
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

- Gradle 9 or newer
- Java 25
- The local `servers/Paper-26.1.2` folder with Paper's downloaded `libraries/` tree, `plugins/bluemap-5.20-paper.jar`, and optional `plugins/PlaceholderAPI-2.12.3-DEV-265.jar`

Build and deploy to the configured local test servers:

```sh
gradle --no-daemon build
```

Jars are named like:

```text
1MB-BlueMap-MapHide-v2.0.0-025-j25-26.1.2.jar
```

The plugin version is `2.0.0`. The three-digit build number is stored in `build-number.txt` and advances for every new jar build. The Java and Paper parts of the jar name are filename identifiers for your local target build, not the plugin version. After a successful `compileJava` or `build`, `deployServers` copies the jar into:

- `servers/Paper-1.21.11/plugins/`
- `servers/Paper-26.1.2/plugins/`
- `servers/Paper-26.2/plugins/`

Before copying the new jar, the deploy task renames active `1MB-BlueMap-MapHide-*.jar`, `1MB-MapHide-*.jar`, and `BlueMapPlayerControl-*.jar` files in those plugin folders by appending `.disabled`.

## Installation

1. Stop the Paper server.
2. Put `1MB-BlueMap-MapHide-v2.0.0-025-j25-26.1.2.jar` in `plugins/`.
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

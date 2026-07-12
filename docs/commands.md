# Commands

The main command is `/bmpc`. The player-friendly alias is `/map hide` by default and can be disabled or changed in `config.yml`.

Every `/bmpc` command first requires `maphide.player`. Subcommands then check their own permission node. The `maphide.admin` parent grants `maphide.player` plus the admin child permissions, but custom permission setups should keep both layers in mind.

## Player Commands

| Command | Description | Permission |
| --- | --- | --- |
| `/bmpc` | Legacy self toggle. | `maphide.player`, `maphide.player.toggle` |
| `/bmpc help [page]` | Shows paged command help. | `maphide.player`, `maphide.player.help` |
| `/bmpc info` | Shows plugin info, starting commands, version/build, and docs URL. | `maphide.player`, `maphide.player.info` |
| `/bmpc toggle` | Toggles your own BlueMap visibility. | `maphide.player`, `maphide.player.toggle` |
| `/bmpc show` | Shows your own BlueMap marker. | `maphide.player`, `maphide.player.show` |
| `/bmpc hide` | Hides your own BlueMap marker. | `maphide.player`, `maphide.player.hide` |
| `/map hide` | Configurable alias for `/bmpc toggle`. | `maphide.player`, `maphide.player.toggle` |

## Admin Commands

| Command | Description | Permission |
| --- | --- | --- |
| `/bmpc toggle <player>` | Toggles another online player. | `maphide.admin.toggle` |
| `/bmpc show <player>` | Shows another online player. | `maphide.admin.show` |
| `/bmpc hide <player>` | Hides another online player. | `maphide.admin.hide` |
| `/bmpc status` | Shows plugin, build, BlueMap, Paper, Java, and target diagnostics. | `maphide.admin.status` |
| `/bmpc status <player>` | Shows visibility, forced state, world, coordinates, and timer for a player. | `maphide.admin.status` |
| `/bmpc config [page]` | Lists config values with pagination. | `maphide.admin.config` |
| `/bmpc config set <key> <value>` | Updates a known config key, saves comments/defaults, and reloads the plugin config. | `maphide.admin.set` |
| `/bmpc debug [page]` | Lists debug categories. | `maphide.admin.debug` or any debug child node |
| `/bmpc debug status` | Shows server, plugin, and MapHide setting diagnostics. | `maphide.admin.debug.status` |
| `/bmpc debug commands [page]` | Lists command routes and permission gates. | `maphide.admin.debug.commands` |
| `/bmpc debug permissions [page]` | Lists permission nodes, defaults, descriptions, and sender state. | `maphide.admin.debug.permissions` |
| `/bmpc debug placeholders [page]` | Lists PlaceholderAPI state and placeholders. | `maphide.admin.debug.placeholders` |
| `/bmpc reload` | Reloads config, translations, build info, and forced-permission tasks. | `maphide.admin.reload` |

Player targets support exact online names. Admin target commands also accept Bukkit selectors such as `@a`, `@p`, `@r`, and `@s` where the server permits them.

## Console Behavior

Console can run admin information and management commands when permissions allow. Player-only commands such as `/map hide`, `/bmpc`, `/bmpc toggle`, `/bmpc show`, and `/bmpc hide` require an online player sender unless a target argument is supplied for an admin action.

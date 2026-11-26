# Permissions

All permission defaults are defined in `src/main/resources/plugin.yml` and mirrored by `/bmpc debug permissions`.

## Player Permissions

| Permission | Default | Description |
| --- | --- | --- |
| `maphide.player` | `true` | Allows access to `/bmpc` player commands. |
| `maphide.player.help` | `true` | Allows `/bmpc help`. |
| `maphide.player.info` | `true` | Allows `/bmpc info` and `/bmpc version`. |
| `maphide.player.toggle` | `true` | Allows `/bmpc`, `/bmpc toggle`, and the configured alias. |
| `maphide.player.show` | `true` | Allows `/bmpc show`. |
| `maphide.player.hide` | `true` | Allows `/bmpc hide`. |

## Forced Visibility Permissions

These permissions only enforce visibility when `forced-permissions.enabled` is `true`.

| Permission | Default | Description |
| --- | --- | --- |
| `maphide.forcehide` | `false` | Forces the player hidden from BlueMap. |
| `maphide.forceshow` | `false` | Forces the player visible on BlueMap. |

If a player has both force permissions, `forced-permissions.conflict-priority` decides which one wins. The default winner is `hide`.

## Admin Permissions

| Permission | Default | Description |
| --- | --- | --- |
| `maphide.admin` | `op` | Parent admin permission. Grants `maphide.player` and all admin children. |
| `maphide.admin.toggle` | `op` | Allows `/bmpc toggle <player>`. |
| `maphide.admin.show` | `op` | Allows `/bmpc show <player>`. |
| `maphide.admin.hide` | `op` | Allows `/bmpc hide <player>`. |
| `maphide.admin.status` | `op` | Allows `/bmpc status` and `/bmpc status <player>`. |
| `maphide.admin.config` | `op` | Allows `/bmpc config`. |
| `maphide.admin.set` | `op` | Allows `/bmpc config set <key> <value>`. |
| `maphide.admin.reload` | `op` | Allows `/bmpc reload`. |
| `maphide.admin.debug` | `op` | Allows `/bmpc debug` and all debug subcommands. |
| `maphide.admin.debug.status` | `op` | Allows `/bmpc debug status`. |
| `maphide.admin.debug.commands` | `op` | Allows `/bmpc debug commands`. |
| `maphide.admin.debug.permissions` | `op` | Allows `/bmpc debug permissions`. |
| `maphide.admin.debug.placeholders` | `op` | Allows `/bmpc debug placeholders`. |

## Permission Checks To Remember

- `/map hide` requires both `maphide.player` and `maphide.player.toggle`.
- Every `/bmpc` route requires `maphide.player`, including admin commands.
- A single debug child permission also allows the debug menu, which shows the sender's available categories.
- Forced permissions override both player commands and admin visibility changes while enabled.

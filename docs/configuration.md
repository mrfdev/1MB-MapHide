# Configuration

The plugin data folder is:

```text
plugins/1MB-MapHide/
```

Default files:

```text
plugins/1MB-MapHide/config.yml
plugins/1MB-MapHide/Translations/Locale_EN.yml
```

Configuration is loaded with Bukkit's `YamlConfiguration` comment support. Missing defaults are added safely, existing values are preserved, and known comments are refreshed when the plugin reloads.

## Settings

| Key | Default | Values | Reload behavior |
| --- | --- | --- | --- |
| `language` | `EN` | Translation suffix such as `EN` or `FR`. | Requires `/bmpc reload` or restart. |
| `bmpc-toggle-alias` | `/map hide` | Command with leading slash, or `""` to disable. | Requires `/bmpc reload` or restart. |
| `default-visibility` | `show` | `show` or `hide`. | Affects future joins after reload or restart. |
| `apply-default-visibility-on-first-join-only` | `true` | `true` or `false`. | Affects future joins after reload or restart. |
| `toggle-back-after-seconds` | `0` | `0` or positive whole seconds. Negative values are treated as `0`. | Applies to new toggles after reload or restart. |
| `forced-permissions.enabled` | `false` | `true` or `false`. | Requires `/bmpc reload` or restart. |
| `forced-permissions.hide-node` | `maphide.forcehide` | Permission node string. | Requires `/bmpc reload` or restart. |
| `forced-permissions.show-node` | `maphide.forceshow` | Permission node string. | Requires `/bmpc reload` or restart. |
| `forced-permissions.conflict-priority` | `hide` | `hide` or `show`. | Requires `/bmpc reload` or restart. |
| `forced-permissions.check-interval-seconds` | `0` | `0` or positive whole seconds. Negative values are treated as `0`. | Reload cancels and recreates the repeating task. |

## Language Files

The `language` value chooses a file from `Translations/Locale_<language>.yml`.

Example:

```yaml
language: FR
```

Then create:

```text
plugins/1MB-MapHide/Translations/Locale_FR.yml
```

If the requested file is missing, the plugin falls back to `Locale_EN.yml`.

## Alias Behavior

`bmpc-toggle-alias` maps a player command to the same logic as `/bmpc toggle`.

Default:

```yaml
bmpc-toggle-alias: "/map hide"
```

Set it to an empty string to disable the alias:

```yaml
bmpc-toggle-alias: ""
```

The alias requires both `maphide.player` and `maphide.player.toggle`.

## Command Driven Edits

Admins can update known config keys with:

```text
/bmpc config set <key> <value>
```

The command saves the file and reloads MapHide. Unknown keys are rejected. Values are parsed as booleans, integers, or strings.

Use `/bmpc config [page]` to list current values.

## Forced Permissions

Forced permissions are off by default. When enabled:

- Players with `maphide.forcehide` are kept hidden.
- Players with `maphide.forceshow` are kept visible.
- If both are present, `forced-permissions.conflict-priority` decides the result.
- Player commands and admin visibility commands cannot override the active forced state.

The repeating force check is disabled by default. Only set `forced-permissions.check-interval-seconds` above `0` if another plugin or workflow keeps undoing forced visibility.

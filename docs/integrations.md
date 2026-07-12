# Integrations

## BlueMap

BlueMap is required. MapHide uses the BlueMap API to read and update player marker visibility in the BlueMap web app.

If BlueMap is not ready yet, MapHide sends a "BlueMap is not ready yet" message and does not change visibility.

## PlaceholderAPI

PlaceholderAPI is optional. When present, MapHide registers the `maphide` expansion and exposes `%maphide_*%` placeholders.

Use `/bmpc debug placeholders` to confirm whether PlaceholderAPI is enabled and to list placeholder meanings.

## Permissions Plugins

A permissions plugin such as LuckPerms is recommended for production servers. MapHide uses Bukkit permissions for:

- Player command access.
- Admin command access.
- Forced visibility permissions.

The plugin does not require LuckPerms specifically.

## Command Menus And Aliases

Existing menus, command aliases, DeluxeMenus entries, or GUI buttons can continue to run `/bmpc` commands. The main command remains `/bmpc` for legacy compatibility.

The default `/map hide` alias is handled by MapHide itself and runs the same permission checks as `/bmpc toggle`.

## Compatibility Notes

The project is compiled for Java 25 and Paper 26.1.2. The current jar has been smoke-tested locally on:

| Server | BlueMap | Result |
| --- | --- | --- |
| Paper 1.21.11 | BlueMap 5.16 | Compatible in local testing. |
| Paper 26.1.2 | BlueMap 5.20 | Primary target. |
| Paper 26.2 | BlueMap 5.20 | Experimental compatibility passed local smoke testing. |

Paper 26.2 is treated as compatibility testing only until the project target is intentionally moved.

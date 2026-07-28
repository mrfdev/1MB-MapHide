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

The project is compiled for Java 25 and `paper-api:26.2.build.84-stable`. The current jar has been smoke-tested locally on:

| Server | BlueMap | Result |
| --- | --- | --- |
| Paper 26.2 build 84 stable | BlueMap 5.22 | Primary target. |

Paper 26.2 build 84 stable is the maintained Paper API and runtime target for this release. The generated plugin metadata declares `api-version: 26.2`, so older Paper versions require an older MapHide build. Java 25.0.4 is the build and primary runtime; Java 26.0.2 is also smoke-tested without changing the Java 25 bytecode target.

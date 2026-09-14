# Integrations

## BlueMap

BlueMap is required. MapHide uses the BlueMap API to read and update player marker visibility in the BlueMap web app.

Compilation uses the official `de.bluecolored:bluemap-api:2.8.0` artifact matching BlueMap 5.22.

If BlueMap is not ready yet, MapHide sends a "BlueMap is not ready yet" message and does not change visibility.

## PlaceholderAPI

PlaceholderAPI is optional. When present, MapHide registers the `maphide` expansion and exposes `%maphide_*%` placeholders.

Compilation uses the official `me.clip:placeholderapi:2.12.3` artifact; it is not bundled into the MapHide jar.

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

| Server | BlueMap | Java runtime |
| --- | --- | --- |
| Paper 26.2 build 84 stable | BlueMap 5.22 | 25.0.4.1 |
| Paper 26.2 build 84 stable | BlueMap 5.22 | 26.0.2.1 (live runtime version) |

Paper 26.2 build 84 stable is the maintained Paper API and runtime target for this release. The generated plugin metadata declares `api-version: 26.2`, so older Paper versions require an older MapHide build. JDK 25.0.4.1 is the build and test JDK. Both Java 25.0.4.1 and Java 26.0.2.1 are supported runtimes; live uses Java 26. Java 25 bytecode is retained without preview features.

See the [build 029 verification record](verification/2026-09-15-jdk-update.md) for exact runtime builds, checksums, results, and test scope.

### Historical verification

Build 028 was smoke-tested with Java 25.0.4 and Java 26.0.2. Those results describe the earlier release environment; the old installations have been removed.

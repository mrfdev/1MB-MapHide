# Integrations

## BlueMap

BlueMap is required. MapHide uses the BlueMap API to read and update player marker visibility in the BlueMap web app.

Compilation uses the official `de.bluecolored:bluemap-api:2.8.0` artifact; the existing visibility API is retained for the installed BlueMap runtime 5.27 / manifest version 5.26 (`bluemap-5.27-paper.jar`).

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

The project is compiled with dynamically selected installed Oracle JDK 25 for Java 25 and `paper-api:26.3.build.41-alpha`. The maintained test target is Paper 26.3 build 41 alpha on installed Oracle JDK 27. The generated metadata declares `api-version: 26.3`; earlier Paper versions require an older MapHide release. Java 25 bytecode is retained without preview features. Automated Java tests use installed Oracle JDK 27.

The new local fixture contains BlueMap runtime 5.27 / manifest version 5.26 (file `bluemap-5.27-paper.jar`), PlaceholderAPI 2.12.3, LuckPerms 5.5.85, CMI 9.8.10.1, CMILib 1.6.0.0, and Vault 1.7.3-CMI (file `Vault-1.7.4.jar`). CMI, CMILib, and Vault are disabled in the final local smoke fixture after a startup class-initialization deadlock involving CMILib and BlueMap; their configurations and data remain preserved. The passing maintained-server check covers MapHide, BlueMap, PlaceholderAPI, and LuckPerms. Only JARs were copied from the shared test server; existing project-local configurations and data were retained. MapHide does not dispatch LuckPerms commands or perform external player identity lookups.

See the [build 030 verification record](verification/2026-09-25-paper-26.3.md) for exact checksums, runtime results, third-party notices, and manual checks.

### Historical verification

Version 2.0.1 build 029 remains preserved at `v2.0.1-paper-26.2`. Its [Java 25/26 verification record](verification/2026-09-15-jdk-update.md) applies only to that baseline and its Paper 26.2 environment.

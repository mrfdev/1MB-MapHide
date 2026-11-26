# Integrations

## BlueMap

BlueMap is required. MapHide uses the BlueMap API to read and update player marker visibility in the BlueMap web app.

Compilation uses the official `de.bluecolored:bluemap-api:2.8.2` artifact; the existing visibility API is retained for the installed BlueMap 5.29 Paper release (`bluemap-5.29-paper.jar`).

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

MapHide works with Bukkit permission providers. The Paper 26.3 build 263 verification included LuckPerms 5.5.92.

## Command Menus And Aliases

Existing menus, command aliases, DeluxeMenus entries, or GUI buttons can continue to run `/bmpc` commands. The main command remains `/bmpc` for legacy compatibility.

The default `/map hide` alias is handled by MapHide itself and runs the same permission checks as `/bmpc toggle`.

## Compatibility Notes

The project is compiled with dynamically selected installed Oracle JDK 25 for Java 25 and `paper-api:26.3.build.263-stable`. Automated Java tests run with installed Oracle JDK 27. The maintained test target is Paper 26.3 build 263 stable. The generated metadata declares `api-version: 26.3`; earlier Paper versions require an older MapHide release. Java 25 bytecode is retained without preview features.

The isolated stable fixture uses BlueMap 5.29 (`bluemap-5.29-paper.jar`) and PlaceholderAPI 2.12.3, with fresh configuration and worlds. The smoke suite uses Oracle JDK 27, including operation without PlaceholderAPI and expected refusal to load without BlueMap. MapHide reads Bukkit permissions and targets BlueMap state by `Player#getUniqueId()`; it does not dispatch LuckPerms commands or perform external player identity lookups.

MapHide supports Paper. Evaluate MapHide placeholders on the server thread because the expansion reads live Bukkit and BlueMap state.

On this Paper build, BlueMap 5.29 and CMILib 1.6.1.4 deadlocked during startup before MapHide enabled, even with CMI absent. CMI 9.8.12.0 requires an entitled download for testing the latest CMI/CMILib pair. [Stable verification record](verification/2026-11-26-paper-26.3-stable-263.md)

### Historical verification

Version 2.0.1 build 029 remains preserved at `v2.0.1-paper-26.2`. Its [Java 25/26 verification record](verification/2026-09-15-jdk-update.md) applies only to that baseline and its Paper 26.2 environment.

The [2026-09-25 record](verification/2026-09-25-paper-26.3.md) describes the previous experimental 26.3 environment.

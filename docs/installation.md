# Installation And Building

## Requirements

- Paper server with BlueMap installed.
- Java 25 for this build.
- Paper 26.1.2 is the compile target.
- Paper 1.21.11, 26.1.2, and 26.2 have local compatibility test coverage for this code line.
- PlaceholderAPI is optional.

## Runtime Dependencies

Required:

- BlueMap

Optional:

- PlaceholderAPI, for `%maphide_*%` placeholders.
- A permissions plugin such as LuckPerms, for rank-specific access and forced visibility nodes.

## Install

1. Stop the server.
2. Put the built MapHide jar in `plugins/`.
3. Make sure BlueMap is also in `plugins/`.
4. Remove or disable old `BlueMapPlayerControl-*.jar`, `1MB-MapHide-*.jar`, or duplicate `1MB-BlueMap-MapHide-*.jar` files.
5. Start the server.
6. Review `plugins/1MB-MapHide/config.yml`.
7. Run `/bmpc info` and `/bmpc help`.
8. Test `/map hide` as a player.

## Update

1. Stop the server.
2. Disable or remove the old MapHide jar.
3. Install the new jar.
4. Start the server.
5. Run `/bmpc status`.
6. Review the startup log for MapHide, BlueMap, PlaceholderAPI, and command errors.

Existing `config.yml` values are preserved. Missing defaults and managed comments are added during reload/startup.

## Build

The normal local build command is:

```sh
gradle --no-daemon build
```

The jar name follows this pattern:

```text
1MB-BlueMap-MapHide-v2.0.0-<build>-j25-26.1.2.jar
```

`2.0.0` is the plugin version. The three-digit build number is stored in `build-number.txt` and advances for each new build. The `j25` and `26.1.2` parts identify the local Java and Paper target used for the build.

The Gradle deploy task copies the jar into local ignored test-server plugin folders and appends `.disabled` to older active MapHide jars.

## Readiness Check

Use:

```text
/bmpc status
/bmpc debug status
/bmpc debug commands
/bmpc debug permissions
/bmpc debug placeholders
```

The status output should show the MapHide version/build, BlueMap version, server version, Java runtime, and target build.

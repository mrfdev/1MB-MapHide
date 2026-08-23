# Installation And Building

## Requirements

- Paper server with BlueMap installed.
- Java 25 bytecode, built with JDK 25.0.4.
- Paper 26.2 build 84 stable is the maintained compile and release-test target.
- The exact compile coordinate is `io.papermc.paper:paper-api:26.2.build.84-stable`.
- BlueMapAPI `de.bluecolored:bluemap-api:2.8.0` and PlaceholderAPI `me.clip:placeholderapi:2.12.3` are resolved from their official Maven repositories.
- PlaceholderAPI is optional.

This release declares `api-version: 26.2` and is not intended for older Paper versions.

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
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-25.0.4.jdk/Contents/Home \
  gradle --no-daemon -PreleaseBuildNumber=028 clean build
```

The jar name follows this pattern:

```text
1MB-BlueMap-MapHide-v2.0.1-<build>-j25-26.2.jar
```

`2.0.1` is the plugin version. The three-digit build number is stored in `build-number.txt`. An unpinned artifact build advances it once; `-PreleaseBuildNumber=028` makes release retries reuse build `028`. The `j25` and `26.2` parts identify the Java bytecode and Paper target.

The Gradle deploy task copies the jar only into the maintained local `servers/Paper-26.2/plugins/` folder and appends `.disabled` to older active MapHide jars.

The local server folder is not required to compile or package the plugin. It is only used by `deployServers` and `verifyLocalPaperServer`.

`gradle check` runs release-documentation and generated-artifact drift checks. `gradle verifyLocalPaperServer` validates PaperScript's stable channel, Paper build, installed jar, and saved SHA-256.

## Readiness Check

Use:

```text
/bmpc status
/bmpc debug status
/bmpc debug commands
/bmpc debug permissions
/bmpc debug placeholders
```

The status output should show the MapHide version/build, exact compiled Paper API, BlueMap version, server version, Java runtime, and target build.

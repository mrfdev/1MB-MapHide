# Troubleshooting

## `/map hide` Says I Do Not Have Permission

The alias requires both:

```text
maphide.player
maphide.player.toggle
```

Check the player's rank permissions and confirm the alias is enabled with `/bmpc info`.

## `/map hide` Only Works For Players

That is expected. Console and command blocks cannot toggle their own marker. Admins can manage online players with target commands such as:

```text
/bmpc hide <player>
/bmpc show <player>
/bmpc toggle <player>
```

## A Player Cannot Change Their Marker

Check whether forced permissions are enabled:

```text
/bmpc debug status
/bmpc status <player>
```

If `forced-permissions.enabled` is `true`, `maphide.forcehide` or `maphide.forceshow` can override player and admin commands.

## BlueMap Is Not Ready

MapHide depends on BlueMap's API. Wait for BlueMap to finish enabling, then try again. If it keeps happening, check the server log for BlueMap startup errors.

## Placeholder Values Are Missing

Confirm PlaceholderAPI is installed and enabled, then run:

```text
/bmpc debug placeholders
```

MapHide only registers its expansion when PlaceholderAPI is present during plugin enable.

## Config Changes Did Not Apply

Run:

```text
/bmpc reload
```

Some behavior only affects future joins or future toggles. For example, changing `default-visibility` does not rewrite every existing player's current marker state.

## Duplicate Jar Problems

Only one active MapHide jar should be in the top-level `plugins/` folder. Disable or remove old files such as:

```text
BlueMapPlayerControl-*.jar
1MB-MapHide-*.jar
1MB-BlueMap-MapHide-*.jar
```

Files ending in `.disabled` are ignored by Paper.

## Useful Diagnostics

Use these commands when checking a server:

```text
/bmpc status
/bmpc debug status
/bmpc debug commands
/bmpc debug permissions
/bmpc debug placeholders
```

Also check `logs/latest.log` for plugin load failures, command exceptions, missing classes, unsupported Java versions, or BlueMap errors.

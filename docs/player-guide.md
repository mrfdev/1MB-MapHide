# 1MB-MapHide Player Guide

## Introduction

1MB-MapHide lets you control whether your live player marker appears on BlueMap. Use it when you want privacy while building, exploring, testing, or hanging out away from the crowd.

Your marker visibility affects the live BlueMap web map only. It does not make you vanished in game, invisible to other players, or hidden from staff tools.

## How Players Use It

Most players only need one command:

```text
/map hide
```

Running `/map hide` toggles your marker. If you are visible, it hides you. If you are hidden, it shows you again.

The legacy command is still available:

```text
/bmpc
/bmpc toggle
```

Use `/bmpc info` to see what MapHide is, which version is installed, useful starting commands, and the canonical docs link.

## Available Features

- Toggle your own BlueMap marker on or off.
- Explicitly show yourself with `/bmpc show`.
- Explicitly hide yourself with `/bmpc hide`.
- Use `/bmpc help` for command help.
- Use `/bmpc info` for plugin information and the docs URL.

## Quick Start

1. Join the server.
2. Run `/map hide`.
3. Read the message in chat to confirm whether you are now hidden or visible.
4. Run `/map hide` again whenever you want to switch back.

## Commands

| Command | What it does |
| --- | --- |
| `/map hide` | Toggles your BlueMap marker using the server's friendly alias. |
| `/bmpc` | Legacy self-toggle command. |
| `/bmpc toggle` | Toggles your BlueMap marker. |
| `/bmpc show` | Shows your BlueMap marker. |
| `/bmpc hide` | Hides your BlueMap marker. |
| `/bmpc help [page]` | Shows command help. |
| `/bmpc info` | Shows plugin information, starting commands, version/build, and the docs link. |

## Permissions Or Rank Requirements

The default player permissions are enabled for everyone:

| Permission | Default | What it allows |
| --- | --- | --- |
| `maphide.player` | everyone | Access to player `/bmpc` commands. |
| `maphide.player.toggle` | everyone | `/bmpc`, `/bmpc toggle`, and `/map hide`. |
| `maphide.player.show` | everyone | `/bmpc show`. |
| `maphide.player.hide` | everyone | `/bmpc hide`. |
| `maphide.player.help` | everyone | `/bmpc help`. |
| `maphide.player.info` | everyone | `/bmpc info`. |

Some ranks or staff-managed states can force your marker to stay visible or hidden. If that happens, the command tells you that your permissions are forcing your map state.

## Rewards, Costs, Limits, And Cooldowns

There are no rewards, currency costs, or item costs.

By default there is no cooldown and no automatic timer. The server can optionally configure an auto-toggle timer. If enabled, using `/map hide` before the timer ends resets that timer.

## Placeholders

Some menus, scoreboards, or other server features may use MapHide placeholders when PlaceholderAPI is installed. Player-facing values can include whether you are visible, hidden, forced, or waiting on a timer.

## Important Notes

- MapHide controls your BlueMap marker only.
- Staff may still be able to see you through normal moderation tools.
- Forced visibility permissions override player commands while enabled.
- If the map has just restarted, BlueMap may need a moment before marker changes work.

## Related Features

- BlueMap live map
- Server privacy and moderation tools
- Rank or permission menus that may run `/map hide` for you

## Technical Documentation

Technical documentation starts in the project README:

[README.md](../README.md)

# Placeholders

PlaceholderAPI is optional. MapHide registers the `maphide` expansion when PlaceholderAPI is enabled during MapHide startup. Evaluate MapHide placeholders on the server thread.

| Placeholder | Output |
| --- | --- |
| `%maphide_visible%` | `true`, `false`, or `unknown`. |
| `%maphide_state%` | `visible`, `hidden`, or `unknown`. |
| `%maphide_forced%` | `true` when the player has an active force permission, otherwise `false`. |
| `%maphide_force_mode%` | `hide`, `show`, or `none`. |
| `%maphide_default_visibility%` | `visible` or `hidden`, based on `default-visibility`. |
| `%maphide_toggle_back_seconds%` | The configured auto-toggle timer in seconds. |
| `%maphide_toggle_back_remaining%` | Seconds left on the player's active auto-toggle timer. |
| `%maphide_language%` | The active language code, such as `EN`. |

Without an online player, visibility and state return `unknown`, forced returns `false`, force mode returns `none`, and the remaining timer returns `0`. Configuration placeholders still return the current settings. While BlueMap's API is unavailable, an online player's visibility and state read as `false` and `hidden` until it becomes ready.

Use `/bmpc debug placeholders` to check whether PlaceholderAPI is enabled and list the MapHide placeholder names. Use `/papi info maphide` to confirm the expansion is registered and `/papi parse <player> %maphide_state%` to check the resolved value for an online player. [PlaceholderAPI commands](https://wiki.placeholderapi.com/users/commands/)

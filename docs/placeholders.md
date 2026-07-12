# Placeholders

PlaceholderAPI is optional. When PlaceholderAPI is installed before MapHide enables, the plugin registers the `maphide` expansion.

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

Offline or unavailable player values return safe fallbacks such as `unknown`, `false`, `none`, or `0`.

Use `/bmpc debug placeholders` in game or from console to confirm PlaceholderAPI state and list registered placeholder meanings.

# WebAdmin icon semantics

WebAdmin custom module icons are inline 24x24 SVG entries in
`WebAdminFrontendScripts.FLAT_ICON_KEYS` and `FLAT_ICON_GEOMETRY`.
They stay stroke-only, 2D, flat, transparent, and use `currentColor`.
UI backgrounds, hover states, glow, and bubbles are controlled by CSS.

Do not add emoji, text or letter placeholders, icon fonts, remote assets,
PNG/WebP icon files, image2 output, atlas references, or Minecraft-style
block redraws for WebAdmin custom module icons.

Icon color semantics:

- Signal: cyan/blue for channels, signal devices, signal joins, barriers, and aggregators.
- Condition: violet for condition groups, runtime gates, condition debugger, and replay.
- State: indigo for state variables, global/player state, and state write actions.
- Timer: blue for timer/scheduler/delay/countdown/repeat domains; timer-start may use green and timer-cancel may use red as operation semantics.
- Join: cyan for topology; amber is reserved for pending/join-status attention.
- Debug/Doctor: violet for Doctor/debug domains; green/yellow/red remain status severity colors.
- Action: amber for action execution, action bindings, and relay-style execution.
- Region: green for space/area/controller semantics.

Existing old keys remain valid for compatibility. New pages should prefer the
dedicated semantic key instead of reusing `history`, `action-binding`, or
`doctor-overview` as a placeholder.

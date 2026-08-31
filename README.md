# NewerChat

A client-side Forge mod for Minecraft 1.7.10 that reworks the chat input to behave
like newer versions: a live suggestion dropdown, command syntax highlighting and
usage tooltips.

1.7.10 has no Brigadier, so there is no client-side knowledge of command arguments.
The dropdown is filled from two sources: client commands / online players computed
locally, and the server's own `tab-complete` response requested while you type. The
result feels close to the modern chat without pretending the data model exists.

## Features

- Suggestion dropdown above the input line, filtered as you type. The matched
  prefix is highlighted.
- Arrow keys / Page Up / Page Down / mouse wheel to move through the list, Tab or
  click to accept.
- Command syntax highlighting: command name, numbers and coordinates, target
  selectors, online player names, quoted / JSON arguments each get their own colour.
- Server completions requested through `C14PacketTabComplete` with a short debounce;
  command names the server reports are remembered for highlighting.
- Clickable chat components keep working: links (with a confirm screen),
  `run_command`, `suggest_command`, and `show_text` hover tooltips.
- Own text field implementation (word jumps, selection, clipboard) so the coloured
  segments render independently of the MCP mappings.
- Colours, opacity, list size, debounce and every feature toggle live in
  `config/newerchat.cfg` and reload on change.

## Requirements

- JDK 8 (Temurin, Zulu, ...). 1.7.10 will not compile or run on anything newer.
- Everything else (Gradle 8.8, Forge, MCP mappings) is pulled by the wrapper and
  RetroFuturaGradle.

## Building

```
./gradlew build
```

The jar lands in `build/libs/NewerChat-<version>.jar` (the reobfuscated one, not
`-dev.jar`). Set `modrinthModsDir` in `gradle.properties` to have `build` copy it
into a launcher profile automatically; remove the line to skip that step.

For a dev client:

```
./gradlew runClient
```

## Configuration

`config/newerchat.cfg`, three sections:

- `general` — `enabled`, `syntaxHighlighting`, `queryServer`, `showTooltips`,
  `suggestPlayersInChat`, `maxSuggestions`, `debounceMs`.
- `appearance` — box background, selected row, input bar, base text, matched prefix.
- `syntax` — per-token colours (known / unknown command, number, selector, player,
  string).

Colours are ARGB hex, e.g. `E6101014`.

## Notes

- The screen must extend `GuiChat`: the engine only delivers the server's
  tab-complete response to a screen that is `instanceof GuiChat`.
- A few internal calls stay on SRG names (`func_146406_a`, `func_146236_a`,
  `func_146283_a`) because the current mapping set does not name them.
- Argument highlighting is heuristic, based on the shape of each token rather than
  the actual command.
- Mods that replace `GuiChat` with their own subclass are left alone (the swap
  checks for the exact class).

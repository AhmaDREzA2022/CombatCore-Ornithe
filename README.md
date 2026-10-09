# Combat Core (Ornithe / Fabric 1.8.9)

A lightweight, **client-side only** PvP helper mod for Minecraft 1.8.9, built with
[Ornithe](https://ornithemc.net) Loom + Ploceus (Feather "gen2"/Calamus mappings).

Four modules — **W-Tap**, **S-Tap**, **Jump Reset** and **Block Hit** — plus a small
module system, a ClickGUI and a JSON config. No reach, aim, velocity, ESP or any
other cheat: the mod only ever simulates input the player could produce themselves,
through vanilla key bindings.

## Modules

All modules are in the *Combat* category and are toggleable via the ClickGUI or an
optional per-module key binding.

### W-Tap

Releases the forward key for **one tick** shortly after landing a hit, then presses
it again. The one-tick release makes vanilla cancel sprinting (forward input drops
below the sprint threshold in `LocalClientPlayerEntity`'s per-tick update, which
sends a stop-sprinting packet); the re-press lets vanilla re-engage sprint a couple
of ticks later — a classic sprint reset, so the next hit is a fresh sprint hit again.

| Setting | Range | Default | Meaning |
| --- | --- | --- | --- |
| Delay | 0–10 ticks | 0 | wait after the hit; `0` = released on the next client tick |
| Chance | 0–100 % | 100 | probability of tapping on any given hit |

### S-Tap

Briefly presses **S** (backwards) after landing a hit — forward input drops to the
sprint threshold for the duration, cancelling sprint without releasing W, so momentum
is kept.

| Setting | Range | Default | Meaning |
| --- | --- | --- | --- |
| Delay | 0–10 ticks | 0 | wait after the hit; `0` = pressed on the next client tick |
| Duration | 1–10 ticks | 1 | how long S is held |

### Jump Reset

Jumps on the tick the player takes knockback. Being airborne during the knockback
tick changes how much of the horizontal velocity is applied, which noticeably
reduces the distance pushed.

Detection uses the local player's hurt timer (`LivingEntity.damagedTimer`, Feather's
name for MCP `hurtTime`), which vanilla sets to 10 whenever health actually drops —
a *rise* of the value while it is being watched is a fresh hit, so combo hits that
re-set it before it reached 0 are caught too. The jump key is only pressed while on
the ground (an airborne press would be a no-op).

| Setting | Range | Default | Meaning |
| --- | --- | --- | --- |
| Chance | 0–100 % | 100 | probability of jumping when hit |
| Min hurt time | 1–10 | 1 | only trigger when the fresh hurt timer is at least this (a vanilla full hit sets 10) |

### Block Hit

Right-clicks (blocks with the sword) a short moment after a hit, then releases —
the classic 1.8.9 blockhit cadence. Implemented as a **one-tick press of vanilla's
use key**: vanilla's own tick loop performs the right click, so every vanilla rule
is respected (reach, `useKeyCooldown`, "already using an item", never while a
screen is open). Only fires while a sword is the selected item, and releasing
restores the physical right-click state, so holding right click yourself is never
interrupted.

| Setting | Range | Default | Meaning |
| --- | --- | --- | --- |
| Delay | 0–10 ticks | 0 | wait after the hit; `0` = pressed on the next client tick |

## Key bindings

| Key | Action |
| --- | --- |
| `Right Shift` | open / close the ClickGUI (vanilla key binding; configurable `guiKey` in the config JSON and rebindable in Options → Controls) |
| per module (default: none) | toggle that module — set it in the ClickGUI (right click a module → *Key* row → press a key; ESC/Delete clears), also listed under Options → Controls → *Combat Core* |

All keys are vanilla `KeyBinding`s driven by the `Keyboard.next()` event queue,
so even presses shorter than one tick register reliably.

Module toggles only fire while no screen is open, so typing in chat never toggles
anything. A chat line confirms each key toggle (`[Combat Core] W-Tap: ON`).

## ClickGUI

Opened with **Right Shift**:

* **left click** a module — toggle it,
* **right click** a module — expand/collapse its settings,
* **drag** a slider — change a value,
* **left click** the *Key* row — press a key to bind, ESC/Delete clears,
* **ESC / Right Shift** — close (config saved on close).

## Config

`<game dir>/config/combatcore.json`, written with Minecraft's bundled Gson:

```json
{
  "guiKey": 54,
  "modules": {
    "W-Tap":      { "enabled": false, "key": 0, "settings": { "Delay": 0, "Chance": 100 } },
    "S-Tap":      { "enabled": false, "key": 0, "settings": { "Delay": 0, "Duration": 1 } },
    "Jump Reset": { "enabled": false, "key": 0, "settings": { "Chance": 100, "Min hurt time": 1 } },
    "Block Hit":  { "enabled": false, "key": 0, "settings": { "Delay": 0 } }
  }
}
```

Saves are dirty-flagged and debounced (at most one write per second while dragging),
written immediately when the ClickGUI closes, and flushed again on JVM shutdown.
Loading happens on the first client tick and tolerates malformed entries (defaults
are kept).

Key codes from the file are applied onto the live bindings on load, so a hand-edited
`guiKey`/`key` wins over `options.txt` on the next launch. Afterwards the bindings are
authoritative: rebinds made in the ClickGUI or Options → Controls are mirrored back
into the JSON automatically.

## Source layout

```
src/main/java/com/combatcore/
├── CombatCoreMod.java               client entry point, ClickGUI key, tick/attack fan-out
├── module/
│   ├── Module.java                  toggle lifecycle, settings, key binding, hooks
│   ├── ModuleManager.java           registry + per-tick / per-hit dispatch rules
│   ├── setting/
│   │   ├── Setting.java             named value base class
│   │   └── NumberSetting.java       clamped int setting (slider)
│   └── combat/
│       ├── WTap.java
│       ├── STap.java
│       ├── JumpReset.java
│       └── BlockHit.java
├── mixin/
│   ├── MinecraftMixin.java                      tick() HEAD → per-tick dispatch
│   ├── ClientPlayerInteractionManagerMixin.java attackEntity() TAIL → hit detection
│   └── GameOptionsMixin.java                    load() HEAD → register key bindings
├── config/ConfigManager.java        JSON load/save (debounced)
├── gui/ClickGuiScreen.java          Right-Shift ClickGUI
└── util/Keys.java                   key simulation helpers (KeyBinding.set / save-restore)
src/main/resources/
├── fabric.mod.json                  client-only entrypoint
└── combatcore.mixins.json           client mixin config
```

### Feather 1.8.9 names used

| Vanilla concept | Feather 1.8.9 |
| --- | --- |
| `Minecraft.runTick()` | `Minecraft.tick()` |
| `PlayerControllerMP.attackEntity(...)` | `ClientPlayerInteractionManager.attackEntity(...)` |
| `GameSettings.setKeyBindState(...)` | `KeyBinding.set(keyCode, pressed)` |
| `KeyBinding.onKeyPressed(...)` (pressed via `consumeClick()`) | `KeyBinding.click(keyCode)` / `KeyBinding.consumeClick()` |
| `EntityLivingBase.hurtTime` | `LivingEntity.damagedTimer` |
| `EntityClientPlayerMP` | `LocalClientPlayerEntity` |
| `GuiScreen.drawScreen / keyTyped` | `Screen.render / keyPressed` |
| `Gui.drawRect / fontRendererObj` | `GuiElement.fill / Screen.textRenderer` |

## Compatibility notes

* Client-side only (`"environment": "client"`); nothing is sent to servers that the
  player could not send by tapping keys themselves.
* Mixin handler names carry a `combatcore$` prefix and the package is unique, so
  they cannot collide with Polyfrost / OneClient or other mods' mixins.
* Five vanilla `KeyBinding` entries (GUI key + four module toggles) are registered
  in one `Combat Core` controls category — they show up in Options → Controls like
  any other binding, and their codes are stored in `options.txt` the vanilla way,
  so they cannot clash with other mods' controls.
* Only three mixin targets (`Minecraft.tick`, `ClientPlayerInteractionManager
  .attackEntity`, `GameOptions.load`), all injected without cancelling, so other
  mods' injections on the same methods keep working.

## Building

```sh
./gradlew build     # jar lands in build/libs/
```

* Gradle runs on **Java 25** (required by Loom/Ploceus 1.18 — see
  `org.gradle.java.home` in `gradle.properties`).
* Compilation uses the **JDK 21** toolchain declared in `build.gradle` and targets
  Java 8 bytecode (`options.release = 8`).
* For 1.8.9 only Feather mappings are used (`feather_build` in `gradle.properties`),
  since Raven / Sparrow / Nests stop at 1.8.2-pre4.

# CCTPickBlockFix

`CCTPickBlockFix` is an experimental Velocity-only compatibility fix for modern Minecraft clients joining `1.21.0 / 1.21.1` backends through `Velocity + ViaVersion / ViaBackwards`.

Its main target is the proxy-side `Pick Block / Pick Item` breakage seen when `1.21.2+` clients, especially `1.21.5`, connect to `1.21.0 / 1.21.1` backend servers.

## Status

- Platform: `Velocity`
- Java: `17`
- Build: `Gradle Kotlin DSL`
- Packet interception: `PacketEvents 2.12.0`
- Packaging: single shaded jar with relocated PacketEvents classes
- Scope: experimental fix, debug-first, safe fallback-first

## Goals

- Run only on the Velocity proxy
- Require no backend Paper/Purpur/Spigot plugin
- Detect modern pick packets from `1.21.2+` clients
- Recover common `Pick Block / Pick Item` behavior for `1.21.0 / 1.21.1` backends
- Provide detailed diagnostics for packet flow, activation gating, target resolution, and fallback behavior

## Non-Goals

- Perfect vanilla-equivalent behavior in every block/entity/NBT scenario
- Full server-world reconstruction on the proxy
- Backend modifications
- ViaVersion fork or protocol patching inside Via itself

## How It Works

The plugin intercepts relevant play packets on the Velocity proxy using PacketEvents.

High-level flow:

1. Detect modern incoming pick packets such as block/entity pick actions from newer clients.
2. Activate only when the client is newer than `1.21.1` and the backend protocol family matches `1.21.0 / 1.21.1`.
3. Cancel the original modern packet before it reaches the backend.
4. Resolve the target into an item using proxy-side caches:
   - current held slot
   - inventory snapshot
   - chunk/block cache
   - tracked entity cache
5. Apply a best-effort fix path:
   - select an existing matching hotbar item
   - swap an existing main-inventory item into the current hotbar slot
   - in creative mode, clone a new item into the current hotbar slot if no safe reusable item exists
6. Record a detailed trace for `/cctpick dump <player>`.

## Current Behavior

The current implementation is intentionally conservative.

- Native `1.21.1` or older clients are bypassed entirely.
- Fix logic targets only `1.21.2+` clients on `1.21.0 / 1.21.1` backends.
- Creative mode is the default supported mode.
- Survival mode is available as an experimental path and must be enabled in config.
- In creative mode, the plugin tries to avoid hijacking special utility/menu items that only look like normal blocks.
- If an inventory item looks non-vanilla because of custom metadata/components, the plugin prefers cloning a fresh item in creative mode instead of reusing that stack.

## Known Limitations

- This is still an experimental proxy-side workaround.
- The proxy does not have complete server-side world context, so some `Pick Block` and `Pick Entity` cases cannot be reproduced perfectly.
- Block entity data, exact NBT cloning, text-bearing blocks, and plugin-defined custom items are only best-effort.
- Some servers use menu items or utility items that visually mimic normal blocks. The plugin includes heuristics to avoid reusing them, but that logic cannot be perfect from the proxy alone.
- Survival behavior is intentionally more limited than creative behavior.

## Build

Requirements:

- Java 17
- internet access to:
  - `https://repo.codemc.io/repository/maven-releases/`
  - `https://repo.codemc.io/repository/maven-snapshots/`
  - `https://repo.papermc.io/repository/maven-public/`

Build:

```powershell
.\gradlew.bat build
```

Artifact:

```text
build/libs/CCTPickBlockFix-1.0.0.jar
```

## Install

1. Build the plugin.
2. Put `build/libs/CCTPickBlockFix-1.0.0.jar` into the Velocity `plugins` directory.
3. Keep your existing `ViaVersion / ViaBackwards` proxy setup.
4. Start or restart Velocity.
5. Edit the generated config if needed.

No backend companion plugin is required.

## Commands

- `/cctpick status`
- `/cctpick debug on`
- `/cctpick debug off`
- `/cctpick dump <player>`
- `/cctpick reload`

### `/cctpick status`

Shows:

- plugin version
- PacketEvents initialization status
- ViaVersion / ViaBackwards detection
- config switches relevant to activation
- online modern clients affected by the fix
- backend name, protocol info, and tracked gamemode

### `/cctpick dump <player>`

Shows the most recent pick attempt trace for that player, including:

- client version
- backend name and protocol
- packet type and packet id
- target block/entity details
- whether the modern packet was cancelled
- whether an inventory reuse path or creative clone path was used
- failure reasons and gating decisions

## Configuration

Default config file:

```text
src/main/resources/config.yml
```

Runtime options:

- `enabled`
- `debug`
- `creative_only`
- `experimental_survival`
- `only_when_backend_protocol_is_1_21_1`
- `log_packet_names`
- `log_packet_ids`
- `cancel_unknown_pick_packets`
- `emulate_when_direct_rewrite_fails`

Recommended default for production testing:

```yml
enabled: true
debug: false
creative_only: true
experimental_survival: false
only_when_backend_protocol_is_1_21_1: true
log_packet_names: true
log_packet_ids: false
cancel_unknown_pick_packets: false
emulate_when_direct_rewrite_fails: true
```

To experiment with survival behavior:

```yml
creative_only: false
experimental_survival: true
```

## Testing Matrix

| Client | Backend | Mode | Expected |
| --- | --- | --- | --- |
| `1.21.1` | `1.21.0 / 1.21.1` | Creative / Survival | Fully bypassed |
| `1.21.5` | `1.21.0 / 1.21.1` | Creative | Main target path |
| `1.21.5` | `1.21.0 / 1.21.1` | Survival | Experimental |
| `1.21.5` | other backend versions | Creative / Survival | Should not activate |

## How To Verify The Fix

### 1. Basic startup check

1. Start Velocity.
2. Run `/cctpick status`.
3. Confirm:
   - PacketEvents is initialized
   - your test player is shown as a client newer than `1.21.1`
   - backend protocol matches `1.21.0 / 1.21.1`

### 2. Creative mode checks

Test all of the following:

1. target item already in current hotbar slot
2. target item in another hotbar slot
3. target item only in main inventory
4. target item absent from inventory
5. target item present only as a special server/menu item

After each case:

```text
/cctpick dump <player>
```

Look for outcomes such as:

- `Existing hotbar item selected.`
- `Existing inventory item moved into the hotbar.`
- `Fallback emulate succeeded.`

### 3. Survival mode checks

If `experimental_survival: true`:

1. Put a matching item somewhere in inventory.
2. Pick the corresponding block.
3. Confirm the plugin moves or selects the existing item instead of creating a new one.

### 4. Regression check

Connect with a native `1.21.1` client and confirm the plugin does not interfere.

## Project Layout

```text
settings.gradle.kts
build.gradle.kts
gradle.properties
src/main/java/cn/cctstudio/velocity/pickblockfix/
src/main/resources/velocity-plugin.json
src/main/resources/config.yml
README.md
LICENSE
```

Main modules include:

- plugin bootstrap
- PacketEvents lifecycle manager
- packet listener and recognizer
- pick rewriter
- fallback executor
- player state cache
- world/inventory tracking
- command handling
- config and debug utilities

## Development Notes

- PacketEvents is shaded and relocated into the plugin jar.
- Velocity API is kept as `compileOnly`.
- The project is intended to build into a single deployable jar.
- Debug traces are first-class; when the fix cannot act safely, it should fail quietly and leave a useful trace.

## License

This repository is licensed under the MIT License. See [LICENSE](LICENSE).

Third-party dependencies keep their own licenses. The final shaded jar includes relocated PacketEvents classes, so if you redistribute builds publicly, review upstream dependency licenses and your redistribution obligations.

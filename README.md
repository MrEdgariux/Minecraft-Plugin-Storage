# Saugykla

Saugykla is a Paper plugin that organizes deposited items into automatically placed,
labelled barrels inside player-defined storage regions. A single plugin JAR targets
Minecraft/Paper 1.20.x and 1.21.x.

## Requirements

- A Paper server in the Minecraft 1.20.x or 1.21.x family
- The Java runtime required by that Paper release (Java 21 is recommended for
  currently maintained Paper 1.20.x and 1.21.x servers)

The plugin is compiled against Paper API 1.20.1 and emits Java 17 bytecode. It
does not use NMS, CraftBukkit internals, reflection, or version-specific server
packages. This allows the same JAR to load on both Java 17-era servers and newer
servers that run on Java 21.

## Compatibility

| Minecraft / Paper | Verification | Java runtime |
| --- | --- | --- |
| 1.20.1 | Runtime-tested on Paper build 196 | 21 tested; plugin bytecode also supports 17 |
| 1.20.2-1.20.5 | Expected compatible from the tested 1.20.1/1.20.6 endpoints; not runtime-tested | Use the version required by the server build |
| 1.20.6 | Runtime-tested on Paper build 151 | 21 |
| 1.21-1.21.1 | Runtime-tested on Paper 1.21.1 build 133 | 21 |
| 1.21.2-1.21.10 | Expected compatible from the tested 1.21.1/1.21.11 endpoints; not runtime-tested | 21 |
| 1.21.11 | Runtime-tested on Paper build 132 | 21 |

Use the latest Paper build available for the Minecraft patch release you choose.
Paper 1.20.6 and 1.21.11 are the preferred endpoints of their respective release
families. The table distinguishes API build verification from actual server
runtime testing; intermediate versions are supported based on the common 1.20.1
API baseline but are not claimed as individually runtime-verified.

Runtime smoke tests used Oracle JDK 21.0.12 on Windows. On all four tested Paper
versions, the plugin loaded and enabled without plugin exceptions, registered its
listener and command executor during startup, generated `config.yml`, initialized
the YAML repositories, and disabled cleanly. Paper 1.21.11 additionally verified
that `/plugins` lists Saugykla and that `help saugykla` resolves the command.
The plugin has no custom recipes to register. Player-driven region selection,
inventory interaction, barrel placement, highlighting, and migration of real
production data still require in-game testing and are not claimed as automated
runtime coverage.

## Build

From a fresh clone, run:

```text
./gradlew clean build
```

On Windows:

```text
gradlew.bat clean build
```

The plugin JAR is generated at `build/libs/Saugykla-1.0.0.jar`. Paper is a
`compileOnly` dependency and is not bundled into the plugin. The configured build
toolchain is JDK 21; Gradle intentionally emits Java 17-compatible class files. The
`build` task also compiles the source against representative Paper 1.20.6,
1.21.1, and 1.21.11 APIs.

## Installation

1. Copy `build/libs/Saugykla-1.0.0.jar` into the server's `plugins` directory.
2. Start the Paper server once to generate `plugins/Saugykla/config.yml`.
3. As an operator, select a storage region with `/s chunks start` and
   `/s chunks end` while looking at its corner blocks.
4. Add barrels and signs to the resource inventory with `/s r`.
5. Use `/s` near a storage region to deposit items.

Existing `resursai.yml`, `chunks.yml`, and `chests_v2.yml` files retain their
original keys and serialization formats. An existing legacy `chests.yml` file is
automatically migrated when `chests_v2.yml` is absent.

No compatibility migration is required when moving the same plugin data between
supported server versions. Always back up plugin and world data before changing
the Minecraft server version.

## Commands and permissions

- `/s` opens the deposit inventory.
- `/s r` opens the barrel/sign resource inventory.
- `/s hl [material]` toggles the particle path to a stored material.
- `/s s` checks whether the held material is stored.
- `/s chunks <start|end>` defines a storage region (`saugykla.chunks`, operator by default).
- `/s reset` resets barrel and region data (`saugykla.reset`, operator by default).

## Server verification

Before deploying to an established server, back up the `plugins/Saugykla`
directory. Verify region selection in both coordinate directions, deposits into
new and existing barrels, multi-barrel overflow, resource refunds after barrel
removal, highlighting, restart persistence, and legacy `chests.yml` migration.

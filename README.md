# Saugykla

Saugykla is a Paper plugin that organizes deposited items into automatically placed,
labelled barrels inside player-defined storage regions. It targets Minecraft/Paper 1.21.x.

## Requirements

- JDK 21
- A Paper 1.21.x server (the project compiles against Paper API 1.21.1)

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
`compileOnly` dependency and is not bundled into the plugin.

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

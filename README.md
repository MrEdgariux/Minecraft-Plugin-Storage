# Saugykla

Saugykla is a Paper plugin that organizes deposited items into automatically placed,
labelled barrels inside player-defined storage regions. A single plugin JAR targets
Minecraft/Paper 1.20.x and 1.21.x.

## Requirements

- A Paper server in the Minecraft 1.20.x or 1.21.x family
- Java 21 (required by Saugykla 1.4.1 and its bundled CoreMessages library)

The plugin is compiled against Paper API 1.20.1 and emits Java 21 bytecode. It
does not use NMS, CraftBukkit internals, reflection, or version-specific server
packages. Use Java 21 even with older Paper 1.20.x servers. The previous
`Saugykla-1.4.0.jar` remains the Java 17-bytecode artifact.

## Compatibility

| Minecraft / Paper | Verification | Java runtime |
| --- | --- | --- |
| 1.20.1 | Runtime-tested on Paper build 196 before the CoreMessages integration | 21 |
| 1.20.2-1.20.5 | Expected compatible from the tested 1.20.1/1.20.6 endpoints; not runtime-tested | 21 |
| 1.20.6 | Runtime-tested on Paper build 151 | 21 |
| 1.21-1.21.1 | Runtime-tested on Paper 1.21.1 build 133 | 21 |
| 1.21.2-1.21.10 | Expected compatible from the tested 1.21.1/1.21.11 endpoints; not runtime-tested | 21 |
| 1.21.11 | Runtime-tested on Paper build 132 | 21 |

Use the latest Paper build available for the Minecraft patch release you choose.
Paper 1.20.6 and 1.21.11 are the preferred endpoints of their respective release
families. The table distinguishes API build verification from actual server
runtime testing; intermediate versions are supported based on the common 1.20.1
API baseline but are not claimed as individually runtime-verified.

The runtime smoke tests above predate version 1.4.1 and used Oracle JDK 21.0.12
on Windows. On all four tested Paper
versions, the plugin loaded and enabled without plugin exceptions, registered its
listener and command executor during startup, generated `config.yml`, initialized
the YAML repositories, and disabled cleanly. Paper 1.21.11 additionally verified
that `/plugins` lists Saugykla and that `help saugykla` resolves the command.
The plugin has no custom recipes to register. Player-driven region selection,
inventory interaction, barrel placement, highlighting, and migration of real
production data still require in-game testing and are not claimed as automated
runtime coverage.

Version 1.4.1 compiles against the same four Paper APIs and tests the real
CoreMessages translation and chat APIs using the Paper 1.20.1 runtime classpath.
These tests cover language-file creation, fallback, legacy translation migration,
placeholder substitution, preserved edits, and player/console component delivery.
The shaded 1.4.1 JAR was also smoke-tested on Paper 1.20.1 build 196 with Java
21.0.12: startup, English language-file creation, and the console response to
`/s` succeeded. The 1.4.1 integration has not yet been runtime-tested on later
server versions or with in-game players.

## Build

From a fresh clone, run:

```text
./gradlew clean build
```

On Windows:

```text
gradlew.bat clean build
```

The server plugin JAR is generated at `build/libs/Saugykla-1.4.1.jar`. Install
this shaded JAR, not the `-plain.jar` development artifact. Paper and Adventure
are supplied by the server and are not bundled. CoreMessages 1.0.2 is downloaded
from JitPack, bundled, and relocated into a private package to avoid conflicts
with other plugins. The build toolchain and bytecode target are JDK 21. The
`build` task also compiles the source against representative Paper 1.20.6,
1.21.1, and 1.21.11 APIs.

To keep previous JARs in `build/libs`, use `./gradlew build` (Windows:
`gradlew.bat build`) without `clean`. The existing 1.4.0 JAR was preserved when
building 1.4.1.

## Installation

1. Copy `build/libs/Saugykla-1.4.1.jar` into the server's `plugins` directory.
   Keep only one Saugykla version installed on the server; retain old JARs elsewhere.
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

## Translations

All chat messages, including command errors, resource/storage failures, highlight
notifications, and debug output, use
[CoreMessages](https://github.com/MrEdgariux/CoreMessages). It is bundled into
Saugykla; no extra server plugin is required.

On first startup, `plugins/Saugykla/langs/en.yml` is created. Edit its values to
translate the plugin. Placeholders `%0%`, `%1%`, etc. insert values such as a
material name or region size. Legacy `&` colors and hex colors are supported.
Restart the server after editing translations.

For another language, copy `en.yml` to e.g. `lt.yml`, translate it, and set
`language: lt` in `config.yml`. Missing keys fall back to English; missing or
invalid selected languages also fall back to English with a console warning.
CoreMessages fills in new default keys without overwriting existing edits.

Matching legacy `config.yml` `translations` entries are copied into `langs/en.yml`
only when that file does not yet exist, converting `%s` to `%0%` for the resource
message. Existing config values are retained for reference. Obsolete entries
without a matching current message are not migrated. From then on, edit the
language files rather than the old config section. Inventory titles and server
diagnostic logs are separate from chat translations.

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

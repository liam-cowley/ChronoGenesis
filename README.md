# ChronoGenesis

Harvest the temporal essence of living things, grow biomechanical contraptions, and cultivate a
BioComputer strong enough to tear open the past — and eventually the end of time itself.

| | |
|---|---|
| Minecraft | **1.21.1** |
| Loader | **NeoForge 21.1.264** |
| Build plugin | ModDevGradle 2.0.148 (Gradle 9.2.1 via wrapper) |
| Java | **21** |
| Mappings | Mojang official + Parchment 2024.11.17 (real parameter names) |
| Mod ID | `chronogenesis`, package `com.chronogenesis` |

## First-time setup (Windows)

1. **Install a JDK 21** — e.g. Eclipse Temurin 21 (`winget install EclipseAdoptium.Temurin.21.JDK`).
   Check with `java -version` in a new terminal.
2. **Install IntelliJ IDEA Community** (recommended) and the **Minecraft Development** plugin
   (Settings → Plugins → Marketplace → "Minecraft Development").
3. **Open the project**: File → Open → select this folder (the one with `build.gradle`).
   Trust the project and let Gradle sync. The first sync downloads and decompiles Minecraft —
   expect 5–15 minutes.
4. In *Settings → Build → Build Tools → Gradle*, make sure **Gradle JVM** is set to your JDK 21.
5. Run the game: Gradle tool window → `Tasks → mod development → runClient`
   (or in a terminal: `gradlew runClient`).

In-game you should see a **ChronoGenesis** creative tab with Essence Vial, Temporal Residue and
Chronoflesh.

## Playing the current build

What exists so far (see `docs/design/early-game.md` for the full plan):

1. Pick up a **Clock**, craft the **Laboratory Journal** (book + clock + ink sac). It explains everything below.
2. Craft a **Chrono Syringe** and needles (iron = Density, gold = Speed, bone = Size, amethyst = Time).
   Fit a needle: hold it in the off hand and sneak-use the syringe. Use the syringe on a mob to draw essence,
   use it in the air to empty it into an **Essence Vial**.
3. Rotten flesh + bone meal + a full vial = a **Culture**. Build a frame of **Brass Armature**, use the culture
   on it, then feed the organism vials of the same essence. It grows into the frame.
4. Graft a **Tap**, **Leech Tendril** or **Enhancing Gland** onto the flesh. The gland fills you with essence:
   press **Z** to select a power and **G** to use it. Pools are shown top-left.
5. Time: amethyst needle + **Stasis Ampoule**. Over-harvest one area and a **Time Tear** opens
   (seal it with 4 drams of Density).

## Everyday commands

| Command | What it does |
|---|---|
| `gradlew runClient` | Launch a dev client with the mod (and JEI) loaded |
| `gradlew runServer` | Launch a dev dedicated server (catches client-only code leaking into common code) |
| `gradlew runData` | Regenerate models, blockstates, lang and loot tables into `src/generated/resources` |
| `gradlew runGameTestServer` | Run automated game tests |
| `gradlew build` | Build the release jar into `build/libs/` |
| `gradlew --refresh-dependencies` | Fix missing libraries in the IDE |

## Project layout

```
src/main/java/com/chronogenesis/
  ChronoGenesis.java        Mod entry point (registration only)
  ChronoGenesisClient.java  Client-only entry point
  Config.java               Common config
  registry/                 DeferredRegisters: blocks, items, tabs, data attachments, ...
  datagen/                  Data generators (run with `gradlew runData`)
src/main/resources/         Hand-made assets: textures, sounds, Patchouli book, structures
src/generated/resources/    Datagen output — never edit by hand, rerun runData instead
src/main/templates/         neoforge.mods.toml (filled from gradle.properties at build time)
```

## Optional library mods

- **GeckoLib** (pre-wired, commented out) — animated models for multiblock machines, prehistoric creatures and the Elder God.
- **Patchouli** (enabled) — the Laboratory Journal.

Before enabling, confirm the version in `gradle.properties` is the latest for 1.21.1.

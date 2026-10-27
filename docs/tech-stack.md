# ChronoGenesis — Tech Stack

- **Minecraft 1.21.1 + NeoForge 21.1.264**, ModDevGradle 2.0.148, Gradle 9.2.1 wrapper, Java 21,
  Mojang mappings + Parchment 2024.11.17.
- Mod id `chronogenesis`, base package `com.chronogenesis`, license MIT.
- Repo: https://github.com/liam-cowley/ChronoGenesis. `main` holds the merged early game (build-order
  steps 1-6); new work goes on feature branches and is merged into `main`.

## Why this version
- 1.21.1 is the long-lived "stable" modding version: the biggest ecosystem (Ars Nouveau, Botania,
  Occultism, GeckoLib, Patchouli, JEI, Curios all target it), and the modern NeoForge APIs (data
  components, data attachments, payload networking) are well documented.
- Newer versions (26.x) have a thinner library ecosystem; 1.20.1 Forge lacks the modern APIs.
- NeoForge over Fabric: built-in attachments, data maps and datagen suit a content-heavy mod.

## Libraries
- **Patchouli 1.21.1-93** (required): the Laboratory Journal.
- **JEI 19.21.2.313** (dev runtime only).
- **GeckoLib**: pre-wired in `build.gradle`, commented out; enable for animated organisms/creatures.
- **Curios**: not added yet; needed for the Umbilical Locket.

## Run configurations
| Run | What it does |
|---|---|
| `runClient` | Play the mod in a dev client |
| `runServer` | Dedicated server |
| `runData` | Regenerate models, blockstates, lang, block loot into `src/generated/resources` |
| `runGameTestServer` | Run the automated in-world tests (`gametest/ChronoGameTests`), then exit |

## CI (GitHub Actions)
- `build.yml`: compiles every push and pull request, then runs the game tests.
- `datagen.yml` (feature branches): runs datagen and commits the output back to the branch, boots a
  dedicated server as a smoke test, then runs the game tests. Problems appear as run annotations.

## Tools (`tools/`)
- `textures.py`: generates every texture (see `docs/assets/texture-catalogue.md`).
- `render_models.py`: isometric previews of the generated block models.
- `make_test_structures.py`: the GameTest arena structure (`floor7`).

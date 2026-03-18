# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

GD656Killicon is a Minecraft Forge mod (1.20.1, Forge 47.4.4, Java 17) that adds FPS-style kill icons, sounds, and HUD elements to Minecraft. It is a strictly client-server separated mod — all visual/audio features are client-side only, while kill detection and scoring logic run server-side. This repository is the "Valorant Edition" variant with Valorant-themed assets.

## Build & Run Commands

```bash
# Build the mod JAR
./gradlew build

# Run Minecraft client (development)
./gradlew runClient

# Run Minecraft server (development)
./gradlew runServer

# Run data generation
./gradlew runData
```

The built JAR is output to `build/libs/`. After `build`, `reobfJar` runs automatically. The `run/` directory is the working directory for dev launches.

## Architecture

### Server / Client Split

The mod is split into two strictly separated sides:

- **Server** (`server/`): Handles kill detection via Forge events, runs scoring logic, and sends packets to clients. No rendering code here.
- **Client** (`client/`): Receives packets, manages config/presets, and renders HUD elements. No game logic here.

### Server Side

- `ServerCore` — static registry of all server-side singletons: `BonusEngine`, `ComboTracker`, `CritTracker`, and all mod integration instances
- `server/event/ServerEventHandler` — listens to Forge events (player kills, damage, etc.) and feeds them into `ServerCore`
- `server/logic/core/BonusEngine` — batches and dispatches bonus score entries to players every 2 ticks
- `server/logic/integration/` — optional integrations (Tacz, SuperbWarfare, ImmersiveAircraft, Ywzj, Spotting, PingWheel). Each integration has an interface, a dummy no-op impl, and an event handler that activates only when the target mod is loaded
- `server/data/` — `PlayerData`, `PlayerDataManager`, `ServerData` for per-player kill/score/death stats stored server-side

### Client Side

- `client/render/HudElementManager` — central registry for all `IHudRenderer` instances. Renderers register by category+name and are all ticked on the `RenderGuiOverlayEvent.Post` for the chat panel overlay
- `client/render/impl/` — concrete HUD renderers (e.g. `ScrollingIconRenderer`, `CardRenderer`, `Battlefield1Renderer`, `BonusListRenderer`, `AceLogoRenderer`, `ComboIconRenderer`, etc.)
- `client/render/effect/` — reusable visual effects (`DigitalScrollEffect`, `IconRingEffect`, `TextScrambleEffect`)
- `client/config/ConfigManager` — facade over `ClientConfigManager` (global settings) and `ElementConfigManager` (per-preset element configs). Config is JSON-based and stored in `.minecraft/config/gd656killicon/`
- `client/config/PresetPackManager` — handles `.gdpack` preset file import/export
- `client/textures/` — `ModTextures` (built-in texture registration), `IconTextureAnimationManager` (spritesheet animation), `ExternalTextureManager` (user-replaced textures via drag-and-drop)
- `client/sounds/ExternalSoundManager` — manages user-replaced sounds (OGG/WAV drag-and-drop per preset)
- `client/stats/` — `ClientStatsManager` + `StatsPersistenceManager` for lifetime client-side stats (total kills, max kill distance, nemesis, etc.)
- `client/gui/` — config UI built with Cloth Config API. `MainConfigScreen` hosts tabs: `HomeTab`, `PresetConfigTab`, `GlobalConfigTab`, `ScoreboardTab`, `HelpTab`

### Network Layer

`NetworkHandler` registers all packets on a `SimpleChannel` at `gd656killicon:main`. Packets are server→client except `ScoreboardRequestPacket` (client→server):

| Packet | Direction | Purpose |
|---|---|---|
| `KillIconPacket` | S→C | Trigger kill icon display |
| `DamageSoundPacket` | S→C | Trigger hit/damage sound |
| `BonusScorePacket` | S→C | Display bonus score entry |
| `DeathPacket` | S→C | Notify client of local player death |
| `KillDistancePacket` | S→C | Send kill distance for stats |
| `ScoreboardRequestPacket` | C→S | Request current scoreboard data |
| `ScoreboardSyncPacket` | S→C | Send scoreboard data to client |

### Common

- `common/KillType` — enum of kill types (headshot, explosion, vehicle, etc.)
- `common/BonusType` — enum/constants for the 51+ bonus types (suppression, ace, backstab, etc.)

### Resources

- Built-in textures: `src/main/resources/assets/gd656killicon/textures/`
- Built-in sounds: `src/main/resources/assets/gd656killicon/sounds/`
- Resource packs (official presets): `src/main/resources/resourcepacks/` (also added as a source set)
- Generated resources: `src/generated/resources/`

### Optional Mod Integration Pattern

Each optional mod (e.g. Tacz) follows the same pattern:
1. `IXxxHandler` — interface
2. `DummyXxxHandler` — no-op implementation used when the mod is absent
3. `XxxEventHandler` — registers Forge events and delegates to the active handler
4. `XxxIntegration` — static `.get()` factory that returns the real or dummy handler based on mod presence

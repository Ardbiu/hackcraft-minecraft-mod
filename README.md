# HackCraft Mod

A Fabric Minecraft mod adding unique utility items for exploration and movement.

## Features

### 1. Glowstep Boots
Illuminates your path as you explore dark caves or the wilderness.
- **Effect**: Automatically places temporary, invisible light sources (Light Level 12) at your feet as you walk.
- **Duration**: Lights fade automatically after 10 seconds.
- **Details**: 
    - Works on servers.
    - Does not replace existing blocks (only Air/Replaceable).
    - No collision with light blocks.
- **Obtaining**: `/give @s modid:glowstep_boots`

### 2. Grapple Hook
A physics-based grappling hook for traversing terrain.
- **Usage**: Right-click to fire.
- **Range**: 30 blocks.
- **Mechanics**: 
    - Raycasts to the specific point you are looking at.
    - Smoothly pulls you towards the target.
    - Safely stops you before collision.
    - Prevents fall damage during use.
- **Cooldown**: 2 seconds.
- **Obtaining**: `/give @s modid:grapple_hook`

### 3. Recall Totem
A magical totem to mark and return to specific locations.
- **Save Location**: Right-click to save your current position (Dimension, Coordinates, Facing).
- **Teleport**: Right-click again to teleport back to the saved spot.
- **Clear**: Sneak + Right-click to clear the saved position.
- **Safety**: Automatically checks for safe footing and headroom before teleporting. Scans a 3x3 area if the exact spot is obstructed.
- **Persistence**: Saved locations persist across server restarts and logouts.
- **Cost**: 64 Durability (1 per teleport). 10-second cooldown.
- **Recipe**:
    - Top/Bottom: Amethyst Shard
    - Left/Right: Gold Ingot
    - Center: Ender Pearl

## Installation

1. Install [Fabric Loader](https://fabricmc.net/).
2. Drop this mod `.jar` into your `mods` folder.
3. Ensure [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api) is also installed.

## Development

This project is built using Gradle.

```bash
# Build the mod
./gradlew build
```

# HytaleDevLib - Hytale Development Library

[![Sponsor](https://img.shields.io/badge/Sponsor-❤-ea4aaa?style=for-the-badge&logo=github-sponsors)](https://github.com/sponsors/ShaneeexD)

A comprehensive utility library for Hytale plugin development, providing tested helpers and utilities that simplify common modding tasks. Built using decompiled Hytale source code for maximum compatibility and functionality.

This is a very early work in progress and there is a lot planned, feel free to make suggestions in Issues!

## Documentation

**[View Full Documentation on Wiki →](https://github.com/ShaneeexD/HytaleDevLib/wiki)**

## Features

HytaleDevLib provides five main helper classes that simplify Hytale plugin development:

### EventHelper
- Simplified event registration for item drops, pickups, and player joins
- Automatic transaction parsing for inventory events
- No manual EventRegistry boilerplate

### WorldHelper
- Thread-safe world operations
- Tick tracking and interval callbacks
- Player management and messaging
- Simplified logging with HytaleLogger
- **Time and day system** - Get/set game time, check day/night, moon phases
- **Day progress tracking** - Monitor sunlight levels and time of day

### EntityHelper
- Player lookup by name or UUID
- Entity teleportation and distance calculations
- Proximity searches (players within radius)
- Entity iteration and type filtering
- Player home/respawn position retrieval
- Readable NPC type names (e.g., "Cow", "Minnow", "Skeleton_Fighter")
- **NPC spawning** - Spawn any NPC by role name with proper ECS setup

### BlockHelper
- **Name-based block operations** - Minecraft-style block referencing
- Get and set blocks at any position with automatic client sync
- Fill or replace blocks in regions
- Find blocks by type within radius
- Count blocks in areas
- World editing utilities
- **3,951 block names** - Complete block ID mapping

### ComponentHelper
- Type-safe ECS component operations
- Display name management
- Item data extraction
- Graceful null handling

## Quick Start Examples

```java
// Spawn NPCs by name
Entity cow = EntityHelper.spawnNPC(world, "Cow", new Vector3d(100, 64, 100));
Entity deer = EntityHelper.spawnNPC(world, "Deer_Doe", 105, 64, 100);

// Set blocks by name (Minecraft-style)
BlockHelper.setBlockByName(world, x, y, z, "Rock_Stone");
BlockHelper.fillRegionByName(world, corner1, corner2, "Soil_Grass");

// Check time and spawn mobs at night
if (WorldHelper.isNighttime(world)) {
    EntityHelper.spawnNPC(world, "Skeleton_Fighter", x, y, z);
}

// Get time information
int hour = WorldHelper.getCurrentHour(world);  // 0-23
float dayProgress = WorldHelper.getDayProgress(world);  // 0.0-1.0
int moonPhase = WorldHelper.getMoonPhase(world);  // 0-7

// Set time of day
WorldHelper.setDayTime(world, 0.5);  // Set to noon

// Detect when players drop items
EventHelper.onItemDrop(this, (itemId, quantity) -> {
    WorldHelper.log(world, "Dropped: " + itemId + " x" + quantity);
});

// Run code every 5 seconds (100 ticks)
WorldHelper.onTickInterval(world, 100, currentTick -> {
    WorldHelper.log(world, "Periodic task executed!");
});
```

## Installation

Add as a dependency in your `build.gradle`:
```gradle
dependencies {
    implementation files("path/to/HytaleDevLib.jar")
}
```


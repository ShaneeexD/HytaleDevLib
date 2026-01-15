# HytaleDevLib - Hytale Development Library

[![Sponsor](https://img.shields.io/badge/Sponsor-❤-ea4aaa?style=for-the-badge&logo=github-sponsors)](https://github.com/sponsors/ShaneeexD)

A comprehensive utility library and example project for Hytale plugin development, providing tested helpers and utilities that simplify common modding tasks.

## Documentation

**[View Full Documentation on Wiki →](https://github.com/ShaneeexD/HytaleDevLib/wiki)**

- **[Library Documentation](https://github.com/ShaneeexD/HytaleDevLib/wiki/Library-Documentation)** - Complete API reference with usage examples

## Features

HytaleDevLib provides four main helper classes that simplify Hytale plugin development:

### EventHelper
- Simplified event registration for item drops, pickups, and player joins
- Automatic transaction parsing for inventory events
- No manual EventRegistry boilerplate

### WorldHelper
- Thread-safe world operations
- Tick tracking and interval callbacks
- Player management and messaging
- Simplified logging with HytaleLogger

### EntityHelper
- Player lookup by name or UUID
- Entity teleportation and distance calculations
- Proximity searches (players within radius)
- Entity iteration and type filtering
- **Player home/respawn position retrieval**
- Readable NPC type names (e.g., "Cow", "Minnow", "Skeleton_Fighter")

### BlockHelper
- Get and set blocks at any position
- Fill or replace blocks in regions
- Find blocks by type within radius
- Count blocks in areas
- World editing utilities

### ComponentHelper
- Type-safe ECS component operations
- Display name management
- Item data extraction
- Graceful null handling

## Quick Start Examples

```java
// Detect when players drop items
EventHelper.onItemDrop(this, (itemId, quantity) -> {
    WorldHelper.log(world, "Dropped: " + itemId + " x" + quantity);
});

// Teleport all cows to a location
List<Entity> allEntities = EntityHelper.getAllEntities(world);
for (Entity entity : allEntities) {
    if ("Cow".equals(EntityHelper.getEntityType(entity))) {
        EntityHelper.teleport(entity, new Vector3d(100, 64, 100));
    }
}

// Get player's home/respawn location
Vector3d homePos = EntityHelper.getPlayerHome(player);
if (homePos != null) {
    double distance = EntityHelper.getDistance(player, homePos);
    WorldHelper.log(world, "Distance from home: " + distance + " blocks");
}

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


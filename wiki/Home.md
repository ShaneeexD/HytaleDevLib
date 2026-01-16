# HytaleDevLib

A utility library that simplifies common Hytale modding tasks by providing tested helpers, utilities, and workarounds for the Hytale API.

## Documentation

- [Library Documentation](https://github.com/ShaneeexD/HytaleDevLib/blob/plugin/README_LIBRARY.md) - Complete HytaleDevLib API reference and usage guide

## Helper Classes

- [EventHelper](EventHelper) - Simple global event registration (chat, items, player join/disconnect)
- [EcsEventHelper](EcsEventHelper) - ECS-based events (block breaking, placing)
- [WorldHelper](WorldHelper) - World operations, tick tracking, time/day system
- [EntityHelper](EntityHelper) - Entity queries, teleportation, NPC spawning
- [BlockHelper](BlockHelper) - Block manipulation, region operations
- [ComponentHelper](ComponentHelper) - ECS component operations

## Features

This project provides a comprehensive utility library for Hytale modding with helpers for:

- Event handling - Item drops, pickups, player joins, chat, disconnects, block breaking/placing
- Entity management - Teleportation, proximity searches, player homes, entity iteration, NPC spawning
- World operations - Tick tracking, messaging, logging, time/day system
- Block operations - Name-based block setting, region filling, block finding
- ECS component manipulation - Type-safe component operations

## Quick Start

```java
// Example: Detect item drops
EventHelper.onItemDrop(plugin, (itemId, quantity) -> {
    WorldHelper.log(world, "Dropped: " + itemId + " x" + quantity);
});

// Example: Teleport all cows to a location
List<Entity> allEntities = EntityHelper.getAllEntities(world);
for (Entity entity : allEntities) {
    if ("Cow".equals(EntityHelper.getEntityType(entity))) {
        EntityHelper.teleport(entity, new Vector3d(100, 64, 100));
    }
}

// Example: Detect block breaking
this.getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, (event) -> {
    World world = event.getWorld();
    
    EcsEventHelper.onBlockBreak(world, (position, blockTypeId) -> {
        WorldHelper.log(world, "Block broken: " + blockTypeId + " at " + position);
    });
});
```

## Links

- [GitHub Repository](https://github.com/ShaneeexD/HytaleDevLib)
- [Hytale Modding Docs](https://hytalemodding.dev)
- [Hytale API Reference](https://hytalemodding.dev/en/docs/server/api)

# HytaleDevLib - Hytale Development Library

[![Sponsor](https://img.shields.io/badge/Sponsor-❤-ea4aaa?style=for-the-badge&logo=github-sponsors)](https://github.com/sponsors/ShaneeexD)

A comprehensive utility library for Hytale plugin development, providing tested helpers and utilities that simplify common modding tasks. Built using decompiled Hytale source code for maximum compatibility and functionality.

This is a very early work in progress and there is a lot planned, feel free to make suggestions in Issues!

## Documentation

**[View Full Documentation on Wiki →](https://github.com/ShaneeexD/HytaleDevLib/wiki)**

## Features

HytaleDevLib provides eleven main helper classes that simplify Hytale plugin development:

### EventHelper
- Simplified event registration for item drops, pickups, and crafting
- Player events: chat messages, join/disconnect tracking
- Automatic transaction parsing for inventory events
- No manual EventRegistry boilerplate

### EcsEventHelper
- Block events: break, place, and damage tracking with mining progress
- Zone discovery: detect map exploration with zone metadata
- Automatic ECS system registration for player events
- Smart filtering to remove false positives (e.g., "Empty" blocks)

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
- **Dynamic block ID resolution** - Uses game's native asset system for automatic compatibility
- Get and set blocks at any position with automatic client sync
- Fill or replace blocks in regions
- Find blocks by type within radius
- Count blocks in areas
- World editing utilities
- Works with modded blocks automatically

### InventoryHelper 
- Item management: give, remove, count items with quantity support
- Inventory checks: has item, is full, get active hotbar item
- Player operations: clear inventory, check capacity
- Safe API with null-safe operations and proper error handling

### BlockStateHelper
- **Block state management** - Work with chests, signs, and other stateful blocks
- Get/set/ensure block states at positions
- Check if blocks have state data
- Mark states for persistence after modifications
- Abstracts deprecated BlockState API for future compatibility

### ItemHelper
- **Item creation** - Create item stacks with specified quantities
- **Container operations** - Add items to specific slots or first available
- **Random slot placement** - Fill containers with items in random slots for natural loot
- **Container queries** - Count items, check space, get all items
- **Container modification** - Remove items, clear containers
- Item utilities: stackability checks, ID/quantity extraction

### PlayerHelper
- Messaging: send messages to players
- Permissions: check player permissions
- Game mode: get/check player game mode
- **Player data access** - Get player component and skin data
- Type checking: verify if entity is a player

### UIHelper
- **Custom page management** - Open/close custom UI pages
- **HUD control** - Show/hide specific HUD components
- **UI animations** - Fade in/out effects for smooth transitions
- Page manager and HUD manager access

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

// Detect crafting (NEW in v0.1.4)
EventHelper.onCraftRecipe(this, (itemId, quantity) -> {
    WorldHelper.log(world, "Crafted: " + quantity + "x " + itemId);
});

// Track zone discoveries (NEW in v0.1.4)
EcsEventHelper.onZoneDiscovery(world, (discoveryInfo) -> {
    WorldHelper.log(world, "Discovered: " + discoveryInfo.zoneName());
});

// Give items to players (NEW in v0.1.4)
InventoryHelper.giveItem(player, "Gem_Diamond", 5);

// Check player permissions (NEW in v0.1.4)
if (PlayerHelper.hasPermission(player, "admin.commands")) {
    PlayerHelper.sendMessage(player, "You have admin access!");
}

// Create a loot chest with random item placement
BlockHelper.setBlockByName(world, x, y, z, "Furniture_Desert_Chest_Small");
WorldHelper.waitTicks(world, 2, () -> {
    BlockState state = BlockStateHelper.ensureState(world, x, y, z);
    if (state instanceof ItemContainerState chestState) {
        ItemContainer container = chestState.getItemContainer();
        ItemHelper.fillContainerRandom(container,
            "Furniture_Crude_Torch", 2,
            "Ingredient_Bone_Fragment", 10,
            "Item_Diamond", 5
        );
        BlockStateHelper.markNeedsSave(chestState);
    }
});

// Fade out a custom UI page
UIHelper.fadeOutCustomPage(player, "myPage", 1000);

// Get player appearance data
PlayerSkin skin = PlayerHelper.getPlayerSkin(player);
if (skin != null) {
    WorldHelper.log(world, "Hair: " + skin.getHairStyle());
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


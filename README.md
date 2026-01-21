# HytaleDevLib - Hytale Development Library

[![Sponsor](https://img.shields.io/badge/Sponsor-❤-ea4aaa?style=for-the-badge&logo=github-sponsors)](https://github.com/sponsors/ShaneeexD)

A comprehensive utility library for Hytale plugin development, providing tested helpers and utilities that simplify common modding tasks. Built using decompiled Hytale source code for maximum compatibility and functionality.

This is a very early work in progress and there is a lot planned, feel free to make suggestions in Issues!

## Documentation

**[Website & Documentation](https://htdevlib.netlify.app)**

**[View Full Documentation on Wiki →](https://github.com/ShaneeexD/HytaleDevLib/wiki)**

**[Mod on CurseForge →](https://www.curseforge.com/hytale/mods/htdevlib)**

## Features

HytaleDevLib provides 19 main helper classes that simplify Hytale plugin development:

### EventHelper
- Simplified event registration for item drops, pickups, and crafting **with player entity access**
- Player events: chat messages, join/disconnect tracking
- Automatic transaction parsing for inventory events
- Access player stats directly in item event callbacks
- No manual EventRegistry boilerplate

### EcsEventHelper
- Block events: break, place, and damage tracking **with player entity access**
- Mining progress tracking and player-specific block interactions
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

### DeathHelper
- Track entity deaths
- Get what entity died (player or NPC)
- Get killer information (player, NPC or environment)
- Get damage source and how they died
- Get death position

### StatsHelper
- **Entity stats management** - Get/set health, stamina, mana, oxygen, and any custom stat
- **Stat modifiers** - Add additive or multiplicative buffs/debuffs to stats
- **Convenience methods** - Quick access to common stats (health, stamina, mana, oxygen)
- **Stat queries** - Get min/max values, percentages, check if stat exists
- **Min/Max operations** - Maximize, minimize, or reset stats to defaults
- **Auto-clamping** - Values automatically stay within min/max bounds
- **Modifier stacking** - Multiple modifiers with different keys stack together
  
### ZoneHelper
- **Zone discovery tracking** - Track which zones each player has discovered
- **Current zone queries** - Get a player's current zone or check if they're in a specific zone
- **Discovery management** - Mark zones as discovered, check discovery status
- **Player-in-zone searches** - Find all players currently in a specific zone
- **Zone statistics** - Get discovery counts, all discovered zones, and zone popularity

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

### LootHelper
- **Custom block drops** - Override default block drops with custom items
- **Loot tables** - Register custom loot for specific block types
- **Physical item entities** - Spawn items with proper physics and collision
- **Replacement or additive** - Replace default drops entirely or add bonus drops
- **Conditional drops** - Support for position-based, random, or custom logic
- **Velocity control** - Random or custom velocities for realistic drop patterns

### ContainerHelper
- **Automatic transaction parsing** - Get item ID, quantity, and action automatically
- **Interaction-based registration** - Auto-register containers when players interact with them
- **Existing container support** - Works with containers from previous worlds
- **Correct shift-click detection** - Properly detects ADDED vs REMOVED for shift-click transfers
- **ContainerTransaction API** - Clean API with `getAction()`, `getItemId()`, `getQuantity()`
- **Helper methods** - `isAdded()`, `isRemoved()`, `isMoved()`, `isSet()`
- **Event filtering** - Separate callbacks for add, remove, or all changes
- **Chest protection** - Build protection systems and item logging
- **64 container types** - Supports all chests (workbenches and furnaces etc are WIP)
- **Multi-container support** - Track multiple containers across worlds

### EquipmentHelper
- Track equipment changes for any `LivingEntity`
- Armor, utility/offhand, and tools container tracking
- Provides `EquipmentChange` with old/new items, slot info, and helpers (`isEquipping()`, `isUnequipping()`)

### ParticleHelper
- **Particle effect spawning** - Spawn 535+ particle systems at positions, blocks, or entities
- **2D/3D particles** - Positional particles or screen-space effects
- **Scale control** - Customize particle size (1.0 = normal, 2.0 = double)
- **Temporary vs looping** - Auto-cleanup for temporary effects, persistent for ambient
- **Player-specific particles** - Show particles to specific players only

### SoundHelper
- **Sound playback** - Play 1156+ sound events in 2D (UI) or 3D (positional)
- **2D sounds** - UI sounds, notifications, global events (no position)
- **3D sounds** - Positional audio with automatic distance attenuation
- **Volume and pitch control** - Customize playback (1.0 = normal)
- **Sound categories** - SFX, MUSIC, AMBIENT, VOICE, MASTER
- **Player-specific audio** - Play sounds to specific players only


### ComponentHelper
- Type-safe ECS component operations
- Display name management
- Item data extraction
- Graceful null handling

## Installation

Add as a dependency in your `build.gradle`:
```gradle
dependencies {
    implementation (files("path/to/HytaleDevLib.jar")
}
```


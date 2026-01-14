# HytaleDevLib - Developer Utility Library for Hytale Modding

A utility library that simplifies common Hytale modding tasks by providing tested helpers, utilities, and workarounds for the Hytale API.

## Helper Classes Overview

### 1. EventHelper
Simplified event registration for common game events. Handles the boilerplate of registering global event listeners and parsing transaction data.

**What it does:**
- Registers event listeners without manual EventRegistry calls
- Parses `LivingEntityInventoryChangeEvent` transactions to extract item details
- Provides callbacks for item drops, pickups, and player joins

**When to use:**
- Detecting when players drop or pick up items
- Tracking player join events
- Avoiding manual transaction string parsing

---

### 2. WorldHelper
Convenient utilities for world operations and thread-safe execution.

**What it does:**
- Executes tasks on the world's main thread
- Provides player management (get players, count)
- Broadcasts messages to all players
- Queries world state (paused, tick count)
- Simplifies logging with proper HytaleLogger API usage

**When to use:**
- Need thread-safe world operations
- Broadcasting server messages
- Logging debug/warning/error messages
- Checking world state or player counts

---

### 3. ComponentHelper
Type-safe ECS component operations using ComponentType for proper API usage.

**What it does:**
- Get/put/add/remove components with proper type safety
- Simplified display name management
- Extract item details (ID, quantity) from ItemComponent
- Handles null checks and exceptions gracefully

**When to use:**
- Adding/removing components from entities
- Setting display names on entities
- Reading item data from ground items
- Any ECS component manipulation

## Discovered API Information

### Working Events
- ✅ `AddPlayerToWorldEvent` - Player joins world
- ✅ `LivingEntityInventoryChangeEvent` - Inventory changes (drops, pickups)
- ✅ `BreakBlockEvent` - Block breaking (via global listener)
- ❌ `DropItemEvent` - Does NOT fire for player inventory drops
- ❌ `InteractivelyPickupItemEvent` - Needs testing

### Component Paths
```java
// Items
com.hypixel.hytale.server.core.modules.entity.item.ItemComponent
com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent

// Players
com.hypixel.hytale.server.core.entity.entities.Player
com.hypixel.hytale.server.core.universe.PlayerRef
```

### Known Issues
1. **ECS System Registration**: `PlayerRef.getComponentType()` returns null during plugin setup
2. **EntityEventSystem**: Cannot register with component queries
3. **Direct Entity Access**: No working pattern for querying entity store yet

## Usage Guide

### EventHelper Methods

#### `onItemDrop(plugin, callback)`
Detects when a player drops an item from their inventory.

```java
EventHelper.onItemDrop(this, (itemId, quantity) -> {
    getLogger().at(Level.INFO).log("Item dropped: " + itemId + " x" + quantity);
    // Example: Track dropped items, spawn custom effects, etc.
});
```

#### `onItemPickup(plugin, callback)`
Detects when a player picks up an item.

```java
EventHelper.onItemPickup(this, (itemId, quantity) -> {
    getLogger().at(Level.INFO).log("Player picked up: " + itemId + " x" + quantity);
    // Example: Award achievements, track collection progress
});
```

#### `onPlayerJoin(plugin, callback)`
Fires when a player joins the world.

```java
EventHelper.onPlayerJoin(this, player -> {
    getLogger().at(Level.INFO).log("Player joined: " + player.getName());
    // Example: Send welcome message, initialize player data
});
```

---

### WorldHelper Methods

#### `executeOnWorldThread(world, task)`
Executes a task on the world's main thread (thread-safe).

```java
WorldHelper.executeOnWorldThread(world, () -> {
    // Any world operations here are thread-safe
    // Example: Spawn entities, modify blocks, etc.
});
```

#### `broadcastMessage(world, message)`
Sends a message to all players in the world.

```java
WorldHelper.broadcastMessage(world, Message.raw("Server restart in 5 minutes!"));
```

#### `getPlayers(world)` / `getPlayerCount(world)`
Get all players or just the count.

```java
List<Entity> players = WorldHelper.getPlayers(world);
int count = WorldHelper.getPlayerCount(world);
```

#### `isPaused(world)` / `getCurrentTick(world)`
Query world state.

```java
if (!WorldHelper.isPaused(world)) {
    long tick = WorldHelper.getCurrentTick(world);
    // Do something based on tick count
}
```

#### `log(world, message)` / `logWarning(world, message)` / `logError(world, message)`
Simplified logging using HytaleLogger.

```java
WorldHelper.log(world, "Debug: Processing items");
WorldHelper.logWarning(world, "Warning: High entity count");
WorldHelper.logError(world, "Error: Failed to load data");
```

#### `onTick(world, callback)`
Register a callback that fires every single tick.

```java
WorldHelper.onTick(world, currentTick -> {
    // Runs every tick (20 times per second at 20 TPS)
    // Example: Update HUD, check conditions, etc.
});
```

#### `onTickInterval(world, interval, callback)`
Register a callback that fires every N ticks. More efficient than `onTick` for periodic tasks.

```java
// Run every second (20 ticks at 20 TPS)
WorldHelper.onTickInterval(world, 20, currentTick -> {
    WorldHelper.log(world, "One second passed, tick: " + currentTick);
});

// Run every 5 seconds (100 ticks)
WorldHelper.onTickInterval(world, 100, currentTick -> {
    // Example: Auto-save, cleanup, periodic checks
    int itemCount = 0; // Count items, etc.
});
```

#### `stopTickTracking(world)`
Stop all tick tracking for a world. Call this in your plugin's shutdown method.

```java
@Override
public void onDisable() {
    WorldHelper.stopTickTracking(world);
}
```

#### `waitTicks(world, ticks, callback)`
Wait for a specified number of ticks before executing a callback. Perfect for delayed actions.

```java
// Player joins -> wait 3 seconds (60 ticks) -> send welcome message
EventHelper.onPlayerJoinWorld(plugin, world -> {
    WorldHelper.waitTicks(world, 60, () -> {
        WorldHelper.broadcastMessage(world, Message.raw("Welcome to the server!"));
    });
});

// Wait 5 seconds (100 ticks) before spawning an entity
WorldHelper.waitTicks(world, 100, () -> {
    // Spawn entity, trigger event, etc.
    WorldHelper.log(world, "Delayed action executed!");
});

// Chain multiple delays
WorldHelper.waitTicks(world, 20, () -> {
    WorldHelper.log(world, "After 1 second");
    
    WorldHelper.waitTicks(world, 20, () -> {
        WorldHelper.log(world, "After 2 seconds total");
    });
});
```

**Note:** The tick tracker uses a background timer that polls `world.getTick()` every 50ms and executes callbacks on the world's main thread for thread safety. The `waitTicks` method creates a self-canceling timer for one-time execution.

---

### ComponentHelper Methods

#### Setup: Get Component Accessor
First, get the component accessor from the world:

```java
ComponentAccessor<EntityStore> accessor = world.getEntityStore().getStore().getAccessor();
```

#### `getComponent(accessor, entityRef, componentType)`
Get a component from an entity.

```java
ComponentType<EntityStore, ItemComponent> itemType = ItemComponent.getComponentType();
ItemComponent item = ComponentHelper.getComponent(accessor, entityRef, itemType);

if (item != null) {
    String itemId = item.getItemStack().getItemId();
}
```

#### `putComponent(accessor, entityRef, componentType, component)`
Add or update a component on an entity.

```java
ComponentType<EntityStore, DisplayNameComponent> nameType = DisplayNameComponent.getComponentType();
DisplayNameComponent nameComp = new DisplayNameComponent(Message.raw("Custom Label"));
ComponentHelper.putComponent(accessor, entityRef, nameType, nameComp);
```

#### `addComponent(accessor, entityRef, componentType, component)`
Add a new component (fails if already exists).

```java
ComponentHelper.addComponent(accessor, entityRef, componentType, component);
```

#### `removeComponent(accessor, entityRef, componentType)`
Remove a component from an entity.

```java
ComponentHelper.removeComponent(accessor, entityRef, nameType);
```

#### `hasComponent(accessor, entityRef, componentType)`
Check if an entity has a component.

```java
if (ComponentHelper.hasComponent(accessor, entityRef, itemType)) {
    // Entity is an item
}
```

#### `setDisplayName(accessor, entityRef, displayName)`
Simplified method to set display name (creates component automatically).

```java
ComponentHelper.setDisplayName(accessor, entityRef, "Diamond Sword +5");
```

#### `getItemId(accessor, entityRef)` / `getItemQuantity(accessor, entityRef)`
Convenience methods for item entities.

```java
String itemId = ComponentHelper.getItemId(accessor, entityRef);
int quantity = ComponentHelper.getItemQuantity(accessor, entityRef);

if (itemId != null) {
    getLogger().at(Level.INFO).log("Found item: " + itemId + " x" + quantity);
}
```

## Installation

Add as a dependency in your `build.gradle`:
```gradle
dependencies {
    implementation files("path/to/HytaleDevLib.jar")
}
```

## Contributing

Found a working pattern or API discovery? Contributions welcome!

## License

MIT License - Free to use in your Hytale mods

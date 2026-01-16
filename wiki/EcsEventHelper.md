# EcsEventHelper

EcsEventHelper provides simplified access to ECS-based events in Hytale. These events require Entity Component System (ECS) registration, which EcsEventHelper handles automatically for you.

## Overview

**What it does:**
- Creates and registers ECS systems automatically
- Provides simple callbacks for block breaking and placing
- Filters out false positives (e.g., "Empty" blocks during placement)
- Extracts block type and item information from events

**When to use:**
- Detecting when players break or place blocks
- Building protection systems or region management
- Tracking block modifications
- Creating custom building mechanics

## Important Note

⚠️ **ECS events must be registered after you have a World instance.** Register them in the `AddPlayerToWorldEvent` callback, not in your plugin's `setup()` method.

## Available Methods

### onBlockBreak(world, callback)

Detects when a player breaks a block.

**Callback Parameters:**
- `Vector3i position` - The exact position of the broken block
- `String blockTypeId` - The block type ID (e.g., "Soil_Dirt", "Rock_Stone")

**Features:**
- Automatically filters out "Empty" blocks (prevents false positives during block placement)
- Provides block type ID for identifying what was broken
- Fires for all block breaking actions

**Example:**
```java
EcsEventHelper.onBlockBreak(world, (position, blockTypeId) -> {
    getLogger().at(Level.INFO).log("Block broken: " + blockTypeId + " at " + position);
    
    // Example: Track mining statistics
    if (blockTypeId.contains("Ore_")) {
        // Player mined an ore block
        incrementMiningStats(blockTypeId);
    }
});
```

### onBlockPlace(world, callback)

Detects when a player places a block.

**Callback Parameters:**
- `Vector3i position` - The exact position where the block was placed
- `String itemId` - The item ID being placed from the player's hand

**Features:**
- Provides the item ID being placed
- Provides exact block position
- Fires for all block placements

**Example:**
```java
EcsEventHelper.onBlockPlace(world, (position, itemId) -> {
    getLogger().at(Level.INFO).log("Block placed: " + itemId + " at " + position);
    
    // Example: Track building statistics
    incrementBlocksPlaced(itemId);
});
```

## Complete Usage Example

```java
@Override
protected void setup() {
    getLogger().at(Level.INFO).log("Setting up plugin...");
    
    // Register simple global events first
    EventHelper.onPlayerChat(this, (username, message) -> {
        getLogger().at(Level.INFO).log("[Chat] " + username + ": " + message);
    });
    
    EventHelper.onItemDrop(this, (itemId, quantity) -> {
        getLogger().at(Level.INFO).log("[Drop] " + quantity + "x " + itemId);
    });
    
    // Register ECS events when world is available
    this.getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, (event) -> {
        World world = event.getWorld();
        getLogger().at(Level.INFO).log("World available, registering ECS events...");
        
        // Now we can register ECS events
        EcsEventHelper.onBlockBreak(world, (position, blockTypeId) -> {
            getLogger().at(Level.INFO).log("[Break] " + blockTypeId + " at " + position);
        });
        
        EcsEventHelper.onBlockPlace(world, (position, itemId) -> {
            getLogger().at(Level.INFO).log("[Place] " + itemId + " at " + position);
        });
    });
}
```

## Practical Examples

### Example 1: Protected Region System

```java
EcsEventHelper.onBlockBreak(world, (position, blockTypeId) -> {
    if (isInProtectedRegion(position)) {
        // Restore the block that was broken
        BlockHelper.setBlockByName(world, position, blockTypeId);
        WorldHelper.broadcastMessage(world, Message.raw("Cannot break blocks in protected area!"));
    }
});

private boolean isInProtectedRegion(Vector3i position) {
    // Check if position is in spawn protection (0,0 to 100,100)
    return position.getX() >= 0 && position.getX() <= 100 &&
           position.getZ() >= 0 && position.getZ() <= 100;
}
```

### Example 2: Build Height Limit

```java
EcsEventHelper.onBlockPlace(world, (position, itemId) -> {
    if (position.getY() > 100) {
        // Remove the placed block
        BlockHelper.setBlock(world, position, 0); // 0 = air
        WorldHelper.broadcastMessage(world, Message.raw("Cannot build above Y=100!"));
    }
});
```

### Example 3: Mining Statistics Tracker

```java
// Track what blocks players mine
private Map<String, Integer> miningStats = new HashMap<>();

EcsEventHelper.onBlockBreak(world, (position, blockTypeId) -> {
    // Increment counter for this block type
    miningStats.put(blockTypeId, miningStats.getOrDefault(blockTypeId, 0) + 1);
    
    // Special handling for ores
    if (blockTypeId.contains("Ore_Diamond")) {
        WorldHelper.broadcastMessage(world, Message.raw("A player found diamond ore!"));
    }
    
    // Log stats every 100 blocks
    int totalMined = miningStats.values().stream().mapToInt(Integer::intValue).sum();
    if (totalMined % 100 == 0) {
        getLogger().at(Level.INFO).log("Total blocks mined: " + totalMined);
        getLogger().at(Level.INFO).log("Most mined: " + getMostMinedBlock());
    }
});

private String getMostMinedBlock() {
    return miningStats.entrySet().stream()
        .max(Map.Entry.comparingByValue())
        .map(Map.Entry::getKey)
        .orElse("None");
}
```

### Example 4: Building Contest Tracker

```java
// Track blocks placed by players
private Map<String, Integer> buildingStats = new HashMap<>();

EcsEventHelper.onBlockPlace(world, (position, itemId) -> {
    // Get player who placed the block (you'd need to track this)
    String playerName = getCurrentPlayer(); // Implement this
    
    // Increment counter
    String key = playerName + ":" + itemId;
    buildingStats.put(key, buildingStats.getOrDefault(key, 0) + 1);
    
    // Check for milestones
    int totalPlaced = buildingStats.values().stream().mapToInt(Integer::intValue).sum();
    if (totalPlaced == 1000) {
        WorldHelper.broadcastMessage(world, Message.raw("1000 blocks placed in the building contest!"));
    }
});
```

### Example 5: Custom Block Drop System

```java
EcsEventHelper.onBlockBreak(world, (position, blockTypeId) -> {
    // Custom drops for specific blocks
    if (blockTypeId.equals("Rock_Stone")) {
        // 10% chance to drop extra cobblestone
        if (Math.random() < 0.1) {
            // Spawn extra item at the block position
            spawnItem(world, position, "Rock_Stone_Cobble", 1);
        }
    }
    
    if (blockTypeId.contains("Ore_")) {
        // Double ore drops on weekends
        if (isWeekend()) {
            String oreType = blockTypeId.replace("Ore_", "Ingot_");
            spawnItem(world, position, oreType, 2);
        }
    }
});
```

## Technical Details

### How It Works

When you call `EcsEventHelper.onBlockBreak()` or `onBlockPlace()`, the helper:

1. Creates a custom `EntityEventSystem` for the event type
2. Registers the system with `EntityStore.REGISTRY`
3. Sets up the query to target player entities using `PlayerRef.getComponentType()`
4. Configures dependencies using `RootDependency.first()`
5. Wraps your callback with error handling

This all happens automatically - you just provide the callback!

### Why "Empty" Blocks Are Filtered

When a player places a block, Hytale internally:
1. Fires a `BreakBlockEvent` for the "Empty" block at that position
2. Then fires a `PlaceBlockEvent` for the actual block

The `onBlockBreak()` method filters out "Empty" blocks to prevent false positives. If you need to detect when air blocks are explicitly broken (which is rare), you'll need to use the raw `BreakBlockEvent` directly.

### Performance Considerations

ECS events are efficient because they:
- Only fire for entities matching the query (players in this case)
- Run on the world's main thread (thread-safe)
- Are managed by Hytale's optimized ECS system

However, avoid expensive operations in your callbacks. If you need to do heavy processing, use `WorldHelper.executeOnWorldThread()` to defer it.

## Comparison with EventHelper

| Feature | EventHelper | EcsEventHelper |
|---------|-------------|----------------|
| Registration | Plugin setup | After World available |
| Event Types | Global events | ECS events |
| Examples | Chat, items, player join/leave | Block break/place |
| Complexity | Simple | Handles ECS complexity |
| When to use | Most events | Block-related events |

## See Also

- [EventHelper](EventHelper) - For simple global events
- [BlockHelper](BlockHelper) - For block manipulation
- [WorldHelper](WorldHelper) - For world operations

## Common Issues

### Issue: "ECS events not firing"

**Solution:** Make sure you're registering them in the `AddPlayerToWorldEvent` callback, not in `setup()`:

```java
// ❌ WRONG - Don't do this
@Override
protected void setup() {
    EcsEventHelper.onBlockBreak(world, ...); // world is null here!
}

// ✅ CORRECT - Do this
@Override
protected void setup() {
    this.getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, (event) -> {
        World world = event.getWorld();
        EcsEventHelper.onBlockBreak(world, ...); // Now world is available!
    });
}
```

### Issue: "Getting block break events when placing blocks"

**Solution:** This is already handled! The `onBlockBreak()` method filters out "Empty" blocks automatically. If you're still seeing issues, make sure you're using the latest version of HytaleDevLib.

### Issue: "Can't cancel events"

**Note:** ECS event callbacks in HytaleDevLib don't support cancellation directly. However, you can:
- Restore broken blocks using `BlockHelper.setBlockByName()`
- Remove placed blocks using `BlockHelper.setBlock(world, position, 0)`
- This achieves the same result as cancellation

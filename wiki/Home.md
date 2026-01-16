# HytaleDevLib Wiki

Welcome to the HytaleDevLib documentation! This library simplifies common Hytale modding tasks by providing tested helpers, utilities, and workarounds for the Hytale API.

## Quick Links

- [Getting Started](Getting-Started)
- [Installation](Installation)
- [API Reference](API-Reference)

## Helper Classes

### Event Helpers
- **[EventHelper](EventHelper)** - Simple global event registration (chat, items, player join/disconnect)
- **[EcsEventHelper](EcsEventHelper)** - ECS-based events (block breaking, placing) ⭐ NEW

### World & Entity Helpers
- **[WorldHelper](WorldHelper)** - World operations, tick tracking, time/day system
- **[EntityHelper](EntityHelper)** - Entity queries, teleportation, NPC spawning
- **[BlockHelper](BlockHelper)** - Block manipulation, region operations

### Component Helpers
- **[ComponentHelper](ComponentHelper)** - ECS component operations

## Event System Overview

HytaleDevLib provides two types of event helpers:

### EventHelper (Simple Global Events)
Use for events that can be registered globally:
- Player chat messages
- Item drops and pickups
- Player join/disconnect events

**Example:**
```java
EventHelper.onPlayerChat(this, (username, message) -> {
    getLogger().at(Level.INFO).log(username + " said: " + message);
});
```

### EcsEventHelper (ECS-Based Events)
Use for events that require ECS system registration:
- Block breaking
- Block placing

**Example:**
```java
this.getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, (event) -> {
    World world = event.getWorld();
    
    EcsEventHelper.onBlockBreak(world, (position, blockTypeId) -> {
        getLogger().at(Level.INFO).log("Block broken: " + blockTypeId);
    });
});
```

## Features

✅ **Simple Event Registration** - No boilerplate code  
✅ **Block Operations** - Name-based block setting (e.g., "Rock_Stone")  
✅ **Entity Management** - Find, teleport, spawn NPCs  
✅ **Tick Tracking** - Schedule delayed tasks and periodic callbacks  
✅ **Time & Day System** - Get/set game time, detect day/night  
✅ **Thread Safety** - Execute tasks on world's main thread  
✅ **ECS Support** - Simplified ECS event registration  

## Latest Updates

### Version 0.1.4
- ✨ Added **EcsEventHelper** for block breaking and placing events
- ✅ Fixed item drop quantity reporting (now shows actual quantity dropped)
- ✅ Added `onPlayerChat()` for chat message detection
- ✅ Added `onPlayerDisconnect()` for player disconnect tracking
- 🔧 Block break events now filter out "Empty" blocks automatically
- 📝 Comprehensive documentation and examples

## Getting Help

- Check the [API Reference](API-Reference) for detailed method documentation
- See [Examples](Examples) for common use cases
- Visit the [GitHub Issues](https://github.com/ShaneeexD/HytaleDevLib/issues) page for bug reports

## Contributing

Found a working pattern or API discovery? Contributions welcome! See the [Contributing Guide](Contributing) for details.

## License

MIT License - Free to use in your Hytale mods

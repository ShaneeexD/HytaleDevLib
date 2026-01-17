package org.hytaledevlib.lib;

import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.universe.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Helper utilities for working with World and entities.
 * Provides convenient methods for common world operations.
 * 
 * Note: This is a simplified helper that works with the current Hytale API.
 * For advanced entity queries, consider using ECS systems directly.
 */
public class WorldHelper {
    
    private static final ConcurrentHashMap<String, TickTracker> tickTrackers = new ConcurrentHashMap<>();
    
    /**
     * Internal class to track ticks and execute callbacks.
     */
    private static class TickTracker {
        private final World world;
        private final Timer timer;
        private long lastTick;
        private final List<TickCallback> callbacks;
        
        TickTracker(World world) {
            this.world = world;
            this.timer = new Timer("WorldHelper-TickTracker-" + world.getName(), true);
            this.lastTick = world.getTick();
            this.callbacks = new ArrayList<>();
        }
        
        void start() {
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    long currentTick = world.getTick();
                    if (currentTick != lastTick) {
                        long ticksPassed = currentTick - lastTick;
                        lastTick = currentTick;
                        
                        // Execute callbacks on world thread
                        world.execute(() -> {
                            for (TickCallback callback : new ArrayList<>(callbacks)) {
                                callback.ticksPassed += ticksPassed;
                                if (callback.ticksPassed >= callback.interval) {
                                    callback.ticksPassed = 0;
                                    try {
                                        callback.consumer.accept(currentTick);
                                    } catch (Exception e) {
                                        world.getLogger().at(Level.WARNING)
                                            .log("Error in tick callback: " + e.getMessage());
                                    }
                                }
                            }
                        });
                    }
                }
            }, 0, 50); // Check every 50ms (1 tick = 50ms at 20 TPS)
        }
        
        void addCallback(int interval, Consumer<Long> consumer) {
            callbacks.add(new TickCallback(interval, consumer));
        }
        
        void stop() {
            timer.cancel();
            callbacks.clear();
        }
    }
    
    /**
     * Internal class to store tick callback information.
     */
    private static class TickCallback {
        final int interval;
        final Consumer<Long> consumer;
        long ticksPassed;
        
        TickCallback(int interval, Consumer<Long> consumer) {
            this.interval = interval;
            this.consumer = consumer;
            this.ticksPassed = 0;
        }
    }
    
    /**
     * Execute a task on the world's main thread.
     * Useful for ensuring thread-safe operations.
     * 
     * @param world The world
     * @param task Task to execute
     */
    public static void executeOnWorldThread(World world, Runnable task) {
        world.execute(task);
    }
    
    /**
     * Get all players in the world.
     * 
     * @param world The world
     * @return List of players
     */
    public static List<Entity> getPlayers(World world) {
        return new ArrayList<>(world.getPlayers());
    }
    
    /**
     * Get the number of players in the world.
     * 
     * @param world The world
     * @return Number of players
     */
    public static int getPlayerCount(World world) {
        return world.getPlayers().size();
    }
    
    /**
     * Get a chunk at the specified chunk coordinates.
     * 
     * @param world The world
     * @param chunkX Chunk X coordinate
     * @param chunkZ Chunk Z coordinate
     * @return The WorldChunk, or null if not loaded
     */
    @javax.annotation.Nullable
    public static com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk getChunk(World world, int chunkX, int chunkZ) {
        try {
            // Convert chunk coordinates to chunk index
            long chunkIndex = com.hypixel.hytale.math.util.ChunkUtil.indexChunk(chunkX, chunkZ);
            return world.getChunk(chunkIndex);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Send a message to all players in the world.
     * 
     * @param world The world
     * @param message Message to send
     */
    public static void broadcastMessage(World world, com.hypixel.hytale.server.core.Message message) {
        world.sendMessage(message);
    }
    
    /**
     * Check if the world is currently paused.
     * 
     * @param world The world
     * @return true if paused
     */
    public static boolean isPaused(World world) {
        return world.isPaused();
    }
    
    /**
     * Get the current tick count for the world.
     * 
     * @param world The world
     * @return Current tick
     */
    public static long getCurrentTick(World world) {
        return world.getTick();
    }
    
    /**
     * Get the world's logger for debug output.
     * 
     * @param world The world
     * @return World logger
     */
    public static com.hypixel.hytale.logger.HytaleLogger getLogger(World world) {
        return world.getLogger();
    }
    
    /**
     * Log a message to the world's logger.
     * 
     * @param world The world
     * @param message Message to log
     */
    public static void log(World world, String message) {
        world.getLogger().at(Level.INFO).log(message);
    }
    
    /**
     * Log a warning to the world's logger.
     * 
     * @param world The world
     * @param message Warning message
     */
    public static void logWarning(World world, String message) {
        world.getLogger().at(Level.WARNING).log(message);
    }
    
    /**
     * Log an error to the world's logger.
     * 
     * @param world The world
     * @param message Error message
     */
    public static void logError(World world, String message) {
        world.getLogger().at(Level.SEVERE).log(message);
    }
    
    /**
     * Register a callback that fires every tick.
     * The callback receives the current tick number.
     * 
     * @param world The world to track
     * @param callback Callback to execute each tick
     */
    public static void onTick(World world, Consumer<Long> callback) {
        onTickInterval(world, 1, callback);
    }
    
    /**
     * Register a callback that fires every N ticks.
     * Useful for periodic tasks without creating full ECS systems.
     * 
     * Example: onTickInterval(world, 20, tick -> { ... }) fires once per second at 20 TPS
     * 
     * @param world The world to track
     * @param interval Number of ticks between callbacks
     * @param callback Callback to execute, receives current tick number
     */
    public static void onTickInterval(World world, int interval, Consumer<Long> callback) {
        String worldName = world.getName();
        TickTracker tracker = tickTrackers.computeIfAbsent(worldName, k -> {
            TickTracker newTracker = new TickTracker(world);
            newTracker.start();
            return newTracker;
        });
        tracker.addCallback(interval, callback);
    }
    
    /**
     * Stop tracking ticks for a world and clear all callbacks.
     * Call this when your plugin shuts down or the world unloads.
     * 
     * @param world The world to stop tracking
     */
    public static void stopTickTracking(World world) {
        String worldName = world.getName();
        TickTracker tracker = tickTrackers.remove(worldName);
        if (tracker != null) {
            tracker.stop();
        }
    }
    
    /**
     * Wait for a specified number of ticks before executing a callback.
     * Useful for delayed actions after events.
     * 
     * Example: Wait 60 ticks (3 seconds) after player joins before sending welcome message
     * 
     * @param world The world
     * @param ticks Number of ticks to wait
     * @param callback Callback to execute after the delay
     */
    public static void waitTicks(World world, int ticks, Runnable callback) {
        if (ticks <= 0) {
            // Execute immediately if no delay
            executeOnWorldThread(world, callback);
            return;
        }
        
        // Create a one-time delayed task using Timer
        java.util.Timer timer = new java.util.Timer("WorldHelper-WaitTicks", true);
        final long startTick = world.getTick();
        final long targetTick = startTick + ticks;
        
        timer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                long currentTick = world.getTick();
                if (currentTick >= targetTick) {
                    // Execute callback on world thread
                    world.execute(() -> {
                        try {
                            callback.run();
                        } catch (Exception e) {
                            world.getLogger().at(Level.WARNING)
                                .log("Error in waitTicks callback: " + e.getMessage());
                        }
                    });
                    
                    // Cancel this timer after execution
                    timer.cancel();
                }
            }
        }, 0, 50); // Check every 50ms
    }
    
    /**
     * Get the current game time as an Instant.
     * This represents the in-game date and time.
     * 
     * @param world The world
     * @return The current game time
     */
    public static java.time.Instant getGameTime(World world) {
        if (world == null) {
            return null;
        }
        
        try {
            com.hypixel.hytale.server.core.modules.time.WorldTimeResource timeResource = 
                world.getEntityStore().getStore().getResource(
                    com.hypixel.hytale.server.core.modules.time.WorldTimeResource.getResourceType()
                );
            return timeResource.getGameTime();
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Get the current game date and time as a LocalDateTime.
     * 
     * @param world The world
     * @return The current game date/time
     */
    public static java.time.LocalDateTime getGameDateTime(World world) {
        if (world == null) {
            return null;
        }
        
        try {
            com.hypixel.hytale.server.core.modules.time.WorldTimeResource timeResource = 
                world.getEntityStore().getStore().getResource(
                    com.hypixel.hytale.server.core.modules.time.WorldTimeResource.getResourceType()
                );
            return timeResource.getGameDateTime();
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Get the current hour of the day (0-23).
     * 
     * @param world The world
     * @return The current hour, or -1 if unavailable
     */
    public static int getCurrentHour(World world) {
        if (world == null) {
            return -1;
        }
        
        try {
            com.hypixel.hytale.server.core.modules.time.WorldTimeResource timeResource = 
                world.getEntityStore().getStore().getResource(
                    com.hypixel.hytale.server.core.modules.time.WorldTimeResource.getResourceType()
                );
            return timeResource.getCurrentHour();
        } catch (Exception e) {
            return -1;
        }
    }
    
    /**
     * Get the current day of the year (1-365).
     * 
     * @param world The world
     * @return The day of year, or -1 if unavailable
     */
    public static int getDayOfYear(World world) {
        java.time.LocalDateTime dateTime = getGameDateTime(world);
        return dateTime != null ? dateTime.getDayOfYear() : -1;
    }
    
    /**
     * Get the current year.
     * 
     * @param world The world
     * @return The year, or -1 if unavailable
     */
    public static int getYear(World world) {
        java.time.LocalDateTime dateTime = getGameDateTime(world);
        return dateTime != null ? dateTime.getYear() : -1;
    }
    
    /**
     * Get the day progress as a value between 0.0 and 1.0.
     * 0.0 = midnight, 0.5 = noon, 1.0 = next midnight
     * 
     * @param world The world
     * @return Day progress (0.0-1.0), or -1 if unavailable
     */
    public static float getDayProgress(World world) {
        if (world == null) {
            return -1;
        }
        
        try {
            com.hypixel.hytale.server.core.modules.time.WorldTimeResource timeResource = 
                world.getEntityStore().getStore().getResource(
                    com.hypixel.hytale.server.core.modules.time.WorldTimeResource.getResourceType()
                );
            return timeResource.getDayProgress();
        } catch (Exception e) {
            return -1;
        }
    }
    
    /**
     * Get the sunlight factor (0.0-1.0).
     * 0.0 = night, 1.0 = full daylight
     * 
     * @param world The world
     * @return Sunlight factor (0.0-1.0), or -1 if unavailable
     */
    public static double getSunlightFactor(World world) {
        if (world == null) {
            return -1;
        }
        
        try {
            com.hypixel.hytale.server.core.modules.time.WorldTimeResource timeResource = 
                world.getEntityStore().getStore().getResource(
                    com.hypixel.hytale.server.core.modules.time.WorldTimeResource.getResourceType()
                );
            return timeResource.getSunlightFactor();
        } catch (Exception e) {
            return -1;
        }
    }
    
    /**
     * Get the current moon phase (0-7 by default).
     * 
     * @param world The world
     * @return Moon phase index, or -1 if unavailable
     */
    public static int getMoonPhase(World world) {
        if (world == null) {
            return -1;
        }
        
        try {
            com.hypixel.hytale.server.core.modules.time.WorldTimeResource timeResource = 
                world.getEntityStore().getStore().getResource(
                    com.hypixel.hytale.server.core.modules.time.WorldTimeResource.getResourceType()
                );
            return timeResource.getMoonPhase();
        } catch (Exception e) {
            return -1;
        }
    }
    
    /**
     * Check if it's currently daytime.
     * 
     * @param world The world
     * @return true if daytime, false if night or unavailable
     */
    public static boolean isDaytime(World world) {
        double sunlight = getSunlightFactor(world);
        return sunlight > 0.5;
    }
    
    /**
     * Check if it's currently nighttime.
     * 
     * @param world The world
     * @return true if nighttime, false if day or unavailable
     */
    public static boolean isNighttime(World world) {
        double sunlight = getSunlightFactor(world);
        return sunlight >= 0 && sunlight <= 0.5;
    }
    
    /**
     * Set the game time to a specific instant.
     * 
     * @param world The world
     * @param time The time to set
     * @return true if successful
     */
    public static boolean setGameTime(World world, java.time.Instant time) {
        if (world == null || time == null) {
            return false;
        }
        
        try {
            com.hypixel.hytale.server.core.modules.time.WorldTimeResource timeResource = 
                world.getEntityStore().getStore().getResource(
                    com.hypixel.hytale.server.core.modules.time.WorldTimeResource.getResourceType()
                );
            timeResource.setGameTime(time, world, world.getEntityStore().getStore());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Set the time of day as a value between 0.0 and 1.0.
     * 0.0 = midnight, 0.5 = noon, 1.0 = next midnight
     * 
     * @param world The world
     * @param dayTime Day time value (0.0-1.0)
     * @return true if successful
     */
    public static boolean setDayTime(World world, double dayTime) {
        if (world == null || dayTime < 0.0 || dayTime > 1.0) {
            return false;
        }
        
        try {
            com.hypixel.hytale.server.core.modules.time.WorldTimeResource timeResource = 
                world.getEntityStore().getStore().getResource(
                    com.hypixel.hytale.server.core.modules.time.WorldTimeResource.getResourceType()
                );
            timeResource.setDayTime(dayTime, world, world.getEntityStore().getStore());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

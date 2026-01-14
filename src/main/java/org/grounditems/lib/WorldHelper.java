package org.grounditems.lib;

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
     * @return Player count
     */
    public static int getPlayerCount(World world) {
        return world.getPlayerCount();
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
}

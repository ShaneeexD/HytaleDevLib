package org.hytaledevlib.lib;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

/**
 * DataHelper - Utility for saving and loading JSON data with persistence.
 * 
 * Provides methods to:
 * - Save objects as JSON files
 * - Load objects from JSON files
 * - Async save/load operations
 * - Automatic backup creation
 * - Type-safe serialization/deserialization
 * 
 * Example usage:
 * <pre>{@code
 * // Save data
 * MyData data = new MyData();
 * DataHelper.saveJson("mydata.json", data);
 * 
 * // Load data
 * MyData loaded = DataHelper.loadJson("mydata.json", MyData.class);
 * 
 * // Async save
 * DataHelper.saveJsonAsync("mydata.json", data)
 *     .thenRun(() -> System.out.println("Save complete!"));
 * }</pre>
 */
public class DataHelper {
    
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .serializeNulls()
        .create();
    
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "DataHelper-IO");
        thread.setDaemon(true);
        return thread;
    });
    
    private static Path dataDirectory = Paths.get("plugins", "data");
    
    /**
     * Set the base directory for data storage.
     * Default is "plugins/data".
     * 
     * @param directory The directory path
     */
    public static void setDataDirectory(String directory) {
        dataDirectory = Paths.get(directory);
        ensureDirectoryExists(dataDirectory);
    }
    
    /**
     * Set the base directory for data storage.
     * 
     * @param directory The directory path
     */
    public static void setDataDirectory(Path directory) {
        dataDirectory = directory;
        ensureDirectoryExists(dataDirectory);
    }
    
    /**
     * Get the current data directory.
     * 
     * @return The data directory path
     */
    public static Path getDataDirectory() {
        return dataDirectory;
    }
    
    /**
     * Save an object as JSON to a file synchronously.
     * Creates a backup of the existing file if it exists.
     * 
     * @param filename The filename (relative to data directory)
     * @param data The object to save
     * @return true if save was successful
     */
    public static boolean saveJson(@Nonnull String filename, @Nonnull Object data) {
        return saveJson(filename, data, true);
    }
    
    /**
     * Save an object as JSON to a file synchronously.
     * 
     * @param filename The filename (relative to data directory)
     * @param data The object to save
     * @param createBackup Whether to create a backup of existing file
     * @return true if save was successful
     */
    public static boolean saveJson(@Nonnull String filename, @Nonnull Object data, boolean createBackup) {
        ensureDirectoryExists(dataDirectory);
        
        Path filePath = dataDirectory.resolve(filename);
        Path tempPath = dataDirectory.resolve(filename + ".tmp");
        
        try {
            // Ensure parent directories exist
            if (filePath.getParent() != null) {
                Files.createDirectories(filePath.getParent());
            }
            
            // Create backup if file exists and backup is requested
            if (createBackup && Files.exists(filePath)) {
                createBackup(filePath);
            }
            
            // Write to temp file first
            String json = GSON.toJson(data);
            try (Writer writer = new OutputStreamWriter(
                    new FileOutputStream(tempPath.toFile()), StandardCharsets.UTF_8)) {
                writer.write(json);
            }
            
            // Move temp file to actual file (atomic operation)
            Files.move(tempPath, filePath, StandardCopyOption.REPLACE_EXISTING);
            
            LOGGER.at(Level.FINE).log("Saved JSON data to: " + filename);
            return true;
            
        } catch (IOException e) {
            LOGGER.at(Level.WARNING).log("Failed to save JSON to " + filename + ": " + e.getMessage());
            
            // Clean up temp file if it exists
            try {
                Files.deleteIfExists(tempPath);
            } catch (IOException ignored) {}
            
            return false;
        }
    }
    
    /**
     * Save an object as JSON to a file asynchronously.
     * 
     * @param filename The filename (relative to data directory)
     * @param data The object to save
     * @return CompletableFuture that completes when save is done
     */
    public static CompletableFuture<Boolean> saveJsonAsync(@Nonnull String filename, @Nonnull Object data) {
        return CompletableFuture.supplyAsync(() -> saveJson(filename, data), EXECUTOR);
    }
    
    /**
     * Load an object from a JSON file synchronously.
     * 
     * @param filename The filename (relative to data directory)
     * @param clazz The class type to deserialize to
     * @param <T> The type of object to load
     * @return The loaded object, or null if file doesn't exist or load failed
     */
    @Nullable
    public static <T> T loadJson(@Nonnull String filename, @Nonnull Class<T> clazz) {
        Path filePath = dataDirectory.resolve(filename);
        
        if (!Files.exists(filePath)) {
            LOGGER.at(Level.FINE).log("JSON file does not exist: " + filename);
            return null;
        }
        
        try (Reader reader = new InputStreamReader(
                new FileInputStream(filePath.toFile()), StandardCharsets.UTF_8)) {
            
            T result = GSON.fromJson(reader, clazz);
            LOGGER.at(Level.FINE).log("Loaded JSON data from: " + filename);
            return result;
            
        } catch (IOException | JsonSyntaxException e) {
            LOGGER.at(Level.WARNING).log("Failed to load JSON from " + filename + ": " + e.getMessage());
            
            // Try to load from backup
            Path backupPath = dataDirectory.resolve(filename + ".backup");
            if (Files.exists(backupPath)) {
                LOGGER.at(Level.INFO).log("Attempting to load from backup: " + filename + ".backup");
                try (Reader reader = new InputStreamReader(
                        new FileInputStream(backupPath.toFile()), StandardCharsets.UTF_8)) {
                    return GSON.fromJson(reader, clazz);
                } catch (IOException | JsonSyntaxException ex) {
                    LOGGER.at(Level.WARNING).log("Failed to load from backup: " + ex.getMessage());
                }
            }
            
            return null;
        }
    }
    
    /**
     * Load an object from a JSON file asynchronously.
     * 
     * @param filename The filename (relative to data directory)
     * @param clazz The class type to deserialize to
     * @param <T> The type of object to load
     * @return CompletableFuture containing the loaded object
     */
    public static <T> CompletableFuture<T> loadJsonAsync(@Nonnull String filename, @Nonnull Class<T> clazz) {
        return CompletableFuture.supplyAsync(() -> loadJson(filename, clazz), EXECUTOR);
    }
    
    /**
     * Check if a JSON file exists.
     * 
     * @param filename The filename (relative to data directory)
     * @return true if the file exists
     */
    public static boolean exists(@Nonnull String filename) {
        return Files.exists(dataDirectory.resolve(filename));
    }
    
    /**
     * Delete a JSON file.
     * 
     * @param filename The filename (relative to data directory)
     * @return true if deletion was successful
     */
    public static boolean delete(@Nonnull String filename) {
        try {
            return Files.deleteIfExists(dataDirectory.resolve(filename));
        } catch (IOException e) {
            LOGGER.at(Level.WARNING).log("Failed to delete " + filename + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Create a backup of a file.
     * 
     * @param filePath The file to backup
     */
    private static void createBackup(Path filePath) {
        try {
            Path backupPath = Paths.get(filePath.toString() + ".backup");
            Files.copy(filePath, backupPath, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.at(Level.FINE).log("Created backup: " + backupPath.getFileName());
        } catch (IOException e) {
            LOGGER.at(Level.WARNING).log("Failed to create backup: " + e.getMessage());
        }
    }
    
    /**
     * Ensure a directory exists, creating it if necessary.
     * 
     * @param directory The directory path
     */
    private static void ensureDirectoryExists(Path directory) {
        try {
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
                LOGGER.at(Level.INFO).log("Created data directory: " + directory);
            }
        } catch (IOException e) {
            LOGGER.at(Level.WARNING).log("Failed to create directory " + directory + ": " + e.getMessage());
        }
    }
    
    /**
     * Get the Gson instance used for serialization.
     * Useful for custom serialization needs.
     * 
     * @return The Gson instance
     */
    public static Gson getGson() {
        return GSON;
    }
    
    /**
     * Shutdown the async executor.
     * Should be called on plugin shutdown.
     */
    public static void shutdown() {
        EXECUTOR.shutdown();
        LOGGER.at(Level.INFO).log("DataHelper executor shutdown");
    }
}

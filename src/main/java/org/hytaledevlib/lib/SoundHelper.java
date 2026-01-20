package org.hytaledevlib.lib;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

/**
 * SoundHelper provides simplified methods for playing sound effects in Hytale.
 * 
 * Features:
 * - Play 2D sounds (UI sounds, no position)
 * - Play 3D sounds (positional audio in the world)
 * - Control volume and pitch
 * - Play sounds to all nearby players or specific players
 * - Automatic player detection within sound range
 * 
 * Sound Categories:
 * - SFX - Sound effects (default)
 * - MUSIC - Background music
 * - AMBIENT - Ambient sounds
 * - VOICE - Voice/dialogue
 * - MASTER - Master volume
 * 
 * See SoundList.md for all available sound event IDs.
 */
public class SoundHelper {
    private static final Logger LOGGER = System.getLogger(SoundHelper.class.getName());
    
    /**
     * Play a 2D sound to all players (UI sound, no position).
     * 
     * @param world The world
     * @param soundEventId The sound event ID (e.g., "UI_Click", "UI_Error")
     */
    public static void playSound2D(World world, String soundEventId) {
        playSound2D(world, soundEventId, SoundCategory.SFX, 1.0f, 1.0f);
    }
    
    /**
     * Play a 2D sound to all players with custom volume and pitch.
     * 
     * @param world The world
     * @param soundEventId The sound event ID
     * @param volume Volume multiplier (1.0 = normal, 0.5 = half volume, 2.0 = double volume)
     * @param pitch Pitch multiplier (1.0 = normal, 0.5 = lower pitch, 2.0 = higher pitch)
     */
    public static void playSound2D(World world, String soundEventId, float volume, float pitch) {
        playSound2D(world, soundEventId, SoundCategory.SFX, volume, pitch);
    }
    
    /**
     * Play a 2D sound to all players with custom category, volume, and pitch.
     * 
     * @param world The world
     * @param soundEventId The sound event ID
     * @param category Sound category (SFX, MUSIC, AMBIENT, VOICE, MASTER)
     * @param volume Volume multiplier (1.0 = normal)
     * @param pitch Pitch multiplier (1.0 = normal)
     */
    public static void playSound2D(World world, String soundEventId, SoundCategory category, float volume, float pitch) {
        try {
            int soundIndex = getSoundIndex(soundEventId);
            if (soundIndex != 0) {
                Store<EntityStore> store = world.getEntityStore().getStore();
                SoundUtil.playSoundEvent2d(soundIndex, category, volume, pitch, store);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to play 2D sound '" + soundEventId + "': " + e.getMessage());
        }
    }
    
    /**
     * Play a 2D sound to a specific player.
     * 
     * @param world The world
     * @param soundEventId The sound event ID
     * @param player The player entity
     */
    public static void playSound2DToPlayer(World world, String soundEventId, Entity player) {
        playSound2DToPlayer(world, soundEventId, player, SoundCategory.SFX, 1.0f, 1.0f);
    }
    
    /**
     * Play a 2D sound to a specific player with custom volume and pitch.
     * 
     * @param world The world
     * @param soundEventId The sound event ID
     * @param player The player entity
     * @param volume Volume multiplier (1.0 = normal)
     * @param pitch Pitch multiplier (1.0 = normal)
     */
    public static void playSound2DToPlayer(World world, String soundEventId, Entity player, float volume, float pitch) {
        playSound2DToPlayer(world, soundEventId, player, SoundCategory.SFX, volume, pitch);
    }
    
    /**
     * Play a 2D sound to a specific player with custom category, volume, and pitch.
     * 
     * @param world The world
     * @param soundEventId The sound event ID
     * @param player The player entity
     * @param category Sound category
     * @param volume Volume multiplier (1.0 = normal)
     * @param pitch Pitch multiplier (1.0 = normal)
     */
    public static void playSound2DToPlayer(World world, String soundEventId, Entity player, SoundCategory category, float volume, float pitch) {
        try {
            int soundIndex = getSoundIndex(soundEventId);
            if (soundIndex != 0) {
                Store<EntityStore> store = world.getEntityStore().getStore();
                PlayerRef playerRef = ComponentHelper.getComponent(store, player.getReference(), PlayerRef.getComponentType());
                if (playerRef != null) {
                    SoundUtil.playSoundEvent2dToPlayer(playerRef, soundIndex, category, volume, pitch);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to play 2D sound to player '" + soundEventId + "': " + e.getMessage());
        }
    }
    
    /**
     * Play a 3D sound at a position, audible to all nearby players.
     * 
     * @param world The world
     * @param soundEventId The sound event ID (e.g., "Block_Break", "Explosion")
     * @param position The position to play the sound at
     */
    public static void playSound3D(World world, String soundEventId, Vector3d position) {
        playSound3D(world, soundEventId, position, SoundCategory.SFX, 1.0f, 1.0f);
    }
    
    /**
     * Play a 3D sound at a position with custom volume and pitch.
     * 
     * @param world The world
     * @param soundEventId The sound event ID
     * @param position The position to play the sound at
     * @param volume Volume multiplier (1.0 = normal)
     * @param pitch Pitch multiplier (1.0 = normal)
     */
    public static void playSound3D(World world, String soundEventId, Vector3d position, float volume, float pitch) {
        playSound3D(world, soundEventId, position, SoundCategory.SFX, volume, pitch);
    }
    
    /**
     * Play a 3D sound at a position with custom category, volume, and pitch.
     * 
     * @param world The world
     * @param soundEventId The sound event ID
     * @param position The position to play the sound at
     * @param category Sound category
     * @param volume Volume multiplier (1.0 = normal)
     * @param pitch Pitch multiplier (1.0 = normal)
     */
    public static void playSound3D(World world, String soundEventId, Vector3d position, SoundCategory category, float volume, float pitch) {
        try {
            int soundIndex = getSoundIndex(soundEventId);
            if (soundIndex != 0) {
                Store<EntityStore> store = world.getEntityStore().getStore();
                SoundUtil.playSoundEvent3d(soundIndex, category, position.getX(), position.getY(), position.getZ(), volume, pitch, store);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to play 3D sound '" + soundEventId + "': " + e.getMessage());
        }
    }
    
    /**
     * Play a 3D sound at a block position.
     * 
     * @param world The world
     * @param soundEventId The sound event ID
     * @param blockPosition The block position
     */
    public static void playSound3DAtBlock(World world, String soundEventId, Vector3i blockPosition) {
        Vector3d position = new Vector3d(
            blockPosition.getX() + 0.5,
            blockPosition.getY() + 0.5,
            blockPosition.getZ() + 0.5
        );
        playSound3D(world, soundEventId, position);
    }
    
    /**
     * Play a 3D sound at a block position with custom volume and pitch.
     * 
     * @param world The world
     * @param soundEventId The sound event ID
     * @param blockPosition The block position
     * @param volume Volume multiplier (1.0 = normal)
     * @param pitch Pitch multiplier (1.0 = normal)
     */
    public static void playSound3DAtBlock(World world, String soundEventId, Vector3i blockPosition, float volume, float pitch) {
        Vector3d position = new Vector3d(
            blockPosition.getX() + 0.5,
            blockPosition.getY() + 0.5,
            blockPosition.getZ() + 0.5
        );
        playSound3D(world, soundEventId, position, volume, pitch);
    }
    
    /**
     * Play a 3D sound at an entity's position.
     * 
     * @param world The world
     * @param soundEventId The sound event ID
     * @param entity The entity to play the sound at
     */
    public static void playSound3DAtEntity(World world, String soundEventId, Entity entity) {
        playSound3DAtEntity(world, soundEventId, entity, 1.0f, 1.0f);
    }
    
    /**
     * Play a 3D sound at an entity's position with custom volume and pitch.
     * 
     * @param world The world
     * @param soundEventId The sound event ID
     * @param entity The entity to play the sound at
     * @param volume Volume multiplier (1.0 = normal)
     * @param pitch Pitch multiplier (1.0 = normal)
     */
    public static void playSound3DAtEntity(World world, String soundEventId, Entity entity, float volume, float pitch) {
        try {
            Store<EntityStore> store = world.getEntityStore().getStore();
            TransformComponent transform = ComponentHelper.getComponent(store, entity.getReference(), TransformComponent.getComponentType());
            if (transform != null) {
                Vector3d position = transform.getPosition();
                playSound3D(world, soundEventId, position, volume, pitch);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to play 3D sound at entity '" + soundEventId + "': " + e.getMessage());
        }
    }
    
    /**
     * Play a 3D sound to a specific player at a position.
     * Only that player will hear the sound.
     * 
     * @param world The world
     * @param soundEventId The sound event ID
     * @param position The position to play the sound at
     * @param player The player who should hear the sound
     */
    public static void playSound3DToPlayer(World world, String soundEventId, Vector3d position, Entity player) {
        playSound3DToPlayer(world, soundEventId, position, player, SoundCategory.SFX, 1.0f, 1.0f);
    }
    
    /**
     * Play a 3D sound to a specific player at a position with custom volume and pitch.
     * 
     * @param world The world
     * @param soundEventId The sound event ID
     * @param position The position to play the sound at
     * @param player The player who should hear the sound
     * @param volume Volume multiplier (1.0 = normal)
     * @param pitch Pitch multiplier (1.0 = normal)
     */
    public static void playSound3DToPlayer(World world, String soundEventId, Vector3d position, Entity player, float volume, float pitch) {
        playSound3DToPlayer(world, soundEventId, position, player, SoundCategory.SFX, volume, pitch);
    }
    
    /**
     * Play a 3D sound to a specific player at a position with custom category, volume, and pitch.
     * 
     * @param world The world
     * @param soundEventId The sound event ID
     * @param position The position to play the sound at
     * @param player The player who should hear the sound
     * @param category Sound category
     * @param volume Volume multiplier (1.0 = normal)
     * @param pitch Pitch multiplier (1.0 = normal)
     */
    public static void playSound3DToPlayer(World world, String soundEventId, Vector3d position, Entity player, SoundCategory category, float volume, float pitch) {
        try {
            int soundIndex = getSoundIndex(soundEventId);
            if (soundIndex != 0) {
                Store<EntityStore> store = world.getEntityStore().getStore();
                SoundUtil.playSoundEvent3dToPlayer(
                    player.getReference(),
                    soundIndex,
                    category,
                    position.getX(),
                    position.getY(),
                    position.getZ(),
                    volume,
                    pitch,
                    store
                );
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to play 3D sound to player '" + soundEventId + "': " + e.getMessage());
        }
    }
    
    /**
     * Get the sound index from a sound event ID.
     * 
     * @param soundEventId The sound event ID
     * @return The sound index, or 0 if not found
     */
    private static int getSoundIndex(String soundEventId) {
        try {
            SoundEvent soundEvent = SoundEvent.getAssetMap().getAsset(soundEventId);
            if (soundEvent != null) {
                return SoundEvent.getAssetMap().getIndex(soundEventId);
            } else {
                LOGGER.log(Level.WARNING, "Sound event not found: " + soundEventId);
                return 0;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get sound index for '" + soundEventId + "': " + e.getMessage());
            return 0;
        }
    }
}

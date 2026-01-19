package org.hytaledevlib.lib;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Helper class for managing entity stats (health, stamina, mana, etc.).
 * Provides convenient methods for getting/setting stats and applying modifiers.
 */
public class StatsHelper {
    
    /**
     * Get the stat value for an entity.
     * 
     * @param entity Entity to get stat from
     * @param statName Stat name (e.g., "Health", "Stamina", "Mana")
     * @return Current stat value, or 0 if stat not found
     */
    public static float getStat(@Nonnull Entity entity, @Nonnull String statName) {
        EntityStatMap statMap = getStatMap(entity);
        if (statMap == null) {
            return 0.0f;
        }
        
        int statIndex = EntityStatType.getAssetMap().getIndex(statName);
        if (statIndex == Integer.MIN_VALUE) {
            return 0.0f;
        }
        
        EntityStatValue statValue = statMap.get(statIndex);
        return statValue != null ? statValue.get() : 0.0f;
    }
    
    /**
     * Get the stat value for an entity using a Ref.
     * 
     * @param ref Entity reference
     * @param store Entity store
     * @param statName Stat name (e.g., "Health", "Stamina", "Mana")
     * @return Current stat value, or 0 if stat not found
     */
    public static float getStat(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull String statName) {
        EntityStatMap statMap = store.getComponent(ref, EntityStatMap.getComponentType());
        if (statMap == null) {
            return 0.0f;
        }
        
        int statIndex = EntityStatType.getAssetMap().getIndex(statName);
        if (statIndex == Integer.MIN_VALUE) {
            return 0.0f;
        }
        
        EntityStatValue statValue = statMap.get(statIndex);
        return statValue != null ? statValue.get() : 0.0f;
    }
    
    /**
     * Set the stat value for an entity.
     * 
     * @param entity Entity to set stat on
     * @param statName Stat name (e.g., "Health", "Stamina", "Mana")
     * @param value New stat value (will be clamped to min/max)
     * @return Actual value set (after clamping), or 0 if stat not found
     */
    public static float setStat(@Nonnull Entity entity, @Nonnull String statName, float value) {
        EntityStatMap statMap = getStatMap(entity);
        if (statMap == null) {
            return 0.0f;
        }
        
        int statIndex = EntityStatType.getAssetMap().getIndex(statName);
        if (statIndex == Integer.MIN_VALUE) {
            return 0.0f;
        }
        
        return statMap.setStatValue(statIndex, value);
    }
    
    /**
     * Set the stat value for an entity using a Ref.
     * 
     * @param ref Entity reference
     * @param store Entity store
     * @param statName Stat name (e.g., "Health", "Stamina", "Mana")
     * @param value New stat value (will be clamped to min/max)
     * @return Actual value set (after clamping), or 0 if stat not found
     */
    public static float setStat(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull String statName, float value) {
        EntityStatMap statMap = store.getComponent(ref, EntityStatMap.getComponentType());
        if (statMap == null) {
            return 0.0f;
        }
        
        int statIndex = EntityStatType.getAssetMap().getIndex(statName);
        if (statIndex == Integer.MIN_VALUE) {
            return 0.0f;
        }
        
        return statMap.setStatValue(statIndex, value);
    }
    
    /**
     * Add to the stat value for an entity.
     * 
     * @param entity Entity to modify stat on
     * @param statName Stat name (e.g., "Health", "Stamina", "Mana")
     * @param amount Amount to add (can be negative to subtract)
     * @return New stat value (after clamping), or 0 if stat not found
     */
    public static float addStat(@Nonnull Entity entity, @Nonnull String statName, float amount) {
        EntityStatMap statMap = getStatMap(entity);
        if (statMap == null) {
            return 0.0f;
        }
        
        int statIndex = EntityStatType.getAssetMap().getIndex(statName);
        if (statIndex == Integer.MIN_VALUE) {
            return 0.0f;
        }
        
        return statMap.addStatValue(statIndex, amount);
    }
    
    /**
     * Add to the stat value for an entity using a Ref.
     * 
     * @param ref Entity reference
     * @param store Entity store
     * @param statName Stat name (e.g., "Health", "Stamina", "Mana")
     * @param amount Amount to add (can be negative to subtract)
     * @return New stat value (after clamping), or 0 if stat not found
     */
    public static float addStat(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull String statName, float amount) {
        EntityStatMap statMap = store.getComponent(ref, EntityStatMap.getComponentType());
        if (statMap == null) {
            return 0.0f;
        }
        
        int statIndex = EntityStatType.getAssetMap().getIndex(statName);
        if (statIndex == Integer.MIN_VALUE) {
            return 0.0f;
        }
        
        return statMap.addStatValue(statIndex, amount);
    }
    
    /**
     * Get the health of an entity.
     * Convenience method for getStat(entity, "Health").
     * 
     * @param entity Entity to get health from
     * @return Current health value
     */
    public static float getHealth(@Nonnull Entity entity) {
        return getStat(entity, "Health");
    }
    
    /**
     * Set the health of an entity.
     * Convenience method for setStat(entity, "Health", value).
     * 
     * @param entity Entity to set health on
     * @param value New health value
     * @return Actual health value set (after clamping)
     */
    public static float setHealth(@Nonnull Entity entity, float value) {
        return setStat(entity, "Health", value);
    }
    
    /**
     * Get the stamina of an entity.
     * Convenience method for getStat(entity, "Stamina").
     * 
     * @param entity Entity to get stamina from
     * @return Current stamina value
     */
    public static float getStamina(@Nonnull Entity entity) {
        return getStat(entity, "Stamina");
    }
    
    /**
     * Set the stamina of an entity.
     * Convenience method for setStat(entity, "Stamina", value).
     * 
     * @param entity Entity to set stamina on
     * @param value New stamina value
     * @return Actual stamina value set (after clamping)
     */
    public static float setStamina(@Nonnull Entity entity, float value) {
        return setStat(entity, "Stamina", value);
    }
    
    /**
     * Get the mana of an entity.
     * Convenience method for getStat(entity, "Mana").
     * 
     * @param entity Entity to get mana from
     * @return Current mana value
     */
    public static float getMana(@Nonnull Entity entity) {
        return getStat(entity, "Mana");
    }
    
    /**
     * Set the mana of an entity.
     * Convenience method for setStat(entity, "Mana", value).
     * 
     * @param entity Entity to set mana on
     * @param value New mana value
     * @return Actual mana value set (after clamping)
     */
    public static float setMana(@Nonnull Entity entity, float value) {
        return setStat(entity, "Mana", value);
    }
    
    /**
     * Get the oxygen of an entity.
     * Convenience method for getStat(entity, "Oxygen").
     * 
     * @param entity Entity to get oxygen from
     * @return Current oxygen value
     */
    public static float getOxygen(@Nonnull Entity entity) {
        return getStat(entity, "Oxygen");
    }
    
    /**
     * Set the oxygen of an entity.
     * Convenience method for setStat(entity, "Oxygen", value).
     * 
     * @param entity Entity to set oxygen on
     * @param value New oxygen value
     * @return Actual oxygen value set (after clamping)
     */
    public static float setOxygen(@Nonnull Entity entity, float value) {
        return setStat(entity, "Oxygen", value);
    }
    
    /**
     * Get the minimum value for a stat.
     * 
     * @param entity Entity to get stat min from
     * @param statName Stat name
     * @return Minimum stat value, or 0 if stat not found
     */
    public static float getStatMin(@Nonnull Entity entity, @Nonnull String statName) {
        EntityStatMap statMap = getStatMap(entity);
        if (statMap == null) {
            return 0.0f;
        }
        
        int statIndex = EntityStatType.getAssetMap().getIndex(statName);
        if (statIndex == Integer.MIN_VALUE) {
            return 0.0f;
        }
        
        EntityStatValue statValue = statMap.get(statIndex);
        return statValue != null ? statValue.getMin() : 0.0f;
    }
    
    /**
     * Get the maximum value for a stat.
     * 
     * @param entity Entity to get stat max from
     * @param statName Stat name
     * @return Maximum stat value, or 0 if stat not found
     */
    public static float getStatMax(@Nonnull Entity entity, @Nonnull String statName) {
        EntityStatMap statMap = getStatMap(entity);
        if (statMap == null) {
            return 0.0f;
        }
        
        int statIndex = EntityStatType.getAssetMap().getIndex(statName);
        if (statIndex == Integer.MIN_VALUE) {
            return 0.0f;
        }
        
        EntityStatValue statValue = statMap.get(statIndex);
        return statValue != null ? statValue.getMax() : 0.0f;
    }
    
    /**
     * Get the stat value as a percentage (0.0 to 1.0) between min and max.
     * 
     * @param entity Entity to get stat from
     * @param statName Stat name
     * @return Percentage (0.0 = min, 1.0 = max), or 0 if stat not found
     */
    public static float getStatPercentage(@Nonnull Entity entity, @Nonnull String statName) {
        EntityStatMap statMap = getStatMap(entity);
        if (statMap == null) {
            return 0.0f;
        }
        
        int statIndex = EntityStatType.getAssetMap().getIndex(statName);
        if (statIndex == Integer.MIN_VALUE) {
            return 0.0f;
        }
        
        EntityStatValue statValue = statMap.get(statIndex);
        return statValue != null ? statValue.asPercentage() : 0.0f;
    }
    
    /**
     * Set a stat to its maximum value.
     * 
     * @param entity Entity to maximize stat on
     * @param statName Stat name
     * @return Maximum stat value, or 0 if stat not found
     */
    public static float maximizeStat(@Nonnull Entity entity, @Nonnull String statName) {
        EntityStatMap statMap = getStatMap(entity);
        if (statMap == null) {
            return 0.0f;
        }
        
        int statIndex = EntityStatType.getAssetMap().getIndex(statName);
        if (statIndex == Integer.MIN_VALUE) {
            return 0.0f;
        }
        
        return statMap.maximizeStatValue(statIndex);
    }
    
    /**
     * Set a stat to its minimum value.
     * 
     * @param entity Entity to minimize stat on
     * @param statName Stat name
     * @return Minimum stat value, or 0 if stat not found
     */
    public static float minimizeStat(@Nonnull Entity entity, @Nonnull String statName) {
        EntityStatMap statMap = getStatMap(entity);
        if (statMap == null) {
            return 0.0f;
        }
        
        int statIndex = EntityStatType.getAssetMap().getIndex(statName);
        if (statIndex == Integer.MIN_VALUE) {
            return 0.0f;
        }
        
        return statMap.minimizeStatValue(statIndex);
    }
    
    /**
     * Reset a stat to its initial/default value.
     * 
     * @param entity Entity to reset stat on
     * @param statName Stat name
     * @return Reset stat value, or 0 if stat not found
     */
    public static float resetStat(@Nonnull Entity entity, @Nonnull String statName) {
        EntityStatMap statMap = getStatMap(entity);
        if (statMap == null) {
            return 0.0f;
        }
        
        int statIndex = EntityStatType.getAssetMap().getIndex(statName);
        if (statIndex == Integer.MIN_VALUE) {
            return 0.0f;
        }
        
        return statMap.resetStatValue(statIndex);
    }
    
    /**
     * Add a modifier to a stat.
     * Modifiers can add or multiply stat values.
     * 
     * @param entity Entity to add modifier to
     * @param statName Stat name
     * @param modifierKey Unique key for this modifier (e.g., "armor_bonus", "speed_potion")
     * @param target Whether to modify MIN or MAX value
     * @param calculationType ADDITIVE (adds amount) or MULTIPLICATIVE (multiplies by amount)
     * @param amount Amount to add or multiply by
     * @return Previous modifier with same key, or null if none
     */
    @Nullable
    public static Modifier addModifier(@Nonnull Entity entity, @Nonnull String statName, @Nonnull String modifierKey,
                                       @Nonnull Modifier.ModifierTarget target, 
                                       @Nonnull StaticModifier.CalculationType calculationType, float amount) {
        EntityStatMap statMap = getStatMap(entity);
        if (statMap == null) {
            return null;
        }
        
        int statIndex = EntityStatType.getAssetMap().getIndex(statName);
        if (statIndex == Integer.MIN_VALUE) {
            return null;
        }
        
        StaticModifier modifier = new StaticModifier(target, calculationType, amount);
        return statMap.putModifier(statIndex, modifierKey, modifier);
    }
    
    /**
     * Add an additive modifier to a stat's max value.
     * Example: addAdditiveModifier(player, "Health", "armor_bonus", 20) adds +20 to max health.
     * 
     * @param entity Entity to add modifier to
     * @param statName Stat name
     * @param modifierKey Unique key for this modifier
     * @param amount Amount to add
     * @return Previous modifier with same key, or null if none
     */
    @Nullable
    public static Modifier addAdditiveModifier(@Nonnull Entity entity, @Nonnull String statName, 
                                               @Nonnull String modifierKey, float amount) {
        return addModifier(entity, statName, modifierKey, Modifier.ModifierTarget.MAX, 
                          StaticModifier.CalculationType.ADDITIVE, amount);
    }
    
    /**
     * Add a multiplicative modifier to a stat's max value.
     * Example: addMultiplicativeModifier(player, "Health", "strength_potion", 1.5f) multiplies max health by 1.5.
     * 
     * @param entity Entity to add modifier to
     * @param statName Stat name
     * @param modifierKey Unique key for this modifier
     * @param multiplier Amount to multiply by
     * @return Previous modifier with same key, or null if none
     */
    @Nullable
    public static Modifier addMultiplicativeModifier(@Nonnull Entity entity, @Nonnull String statName, 
                                                     @Nonnull String modifierKey, float multiplier) {
        return addModifier(entity, statName, modifierKey, Modifier.ModifierTarget.MAX, 
                          StaticModifier.CalculationType.MULTIPLICATIVE, multiplier);
    }
    
    /**
     * Remove a modifier from a stat.
     * 
     * @param entity Entity to remove modifier from
     * @param statName Stat name
     * @param modifierKey Key of the modifier to remove
     * @return Removed modifier, or null if not found
     */
    @Nullable
    public static Modifier removeModifier(@Nonnull Entity entity, @Nonnull String statName, @Nonnull String modifierKey) {
        EntityStatMap statMap = getStatMap(entity);
        if (statMap == null) {
            return null;
        }
        
        int statIndex = EntityStatType.getAssetMap().getIndex(statName);
        if (statIndex == Integer.MIN_VALUE) {
            return null;
        }
        
        return statMap.removeModifier(statIndex, modifierKey);
    }
    
    /**
     * Get a modifier from a stat.
     * 
     * @param entity Entity to get modifier from
     * @param statName Stat name
     * @param modifierKey Key of the modifier to get
     * @return Modifier, or null if not found
     */
    @Nullable
    public static Modifier getModifier(@Nonnull Entity entity, @Nonnull String statName, @Nonnull String modifierKey) {
        EntityStatMap statMap = getStatMap(entity);
        if (statMap == null) {
            return null;
        }
        
        int statIndex = EntityStatType.getAssetMap().getIndex(statName);
        if (statIndex == Integer.MIN_VALUE) {
            return null;
        }
        
        return statMap.getModifier(statIndex, modifierKey);
    }
    
    /**
     * Check if an entity has a specific stat.
     * 
     * @param entity Entity to check
     * @param statName Stat name
     * @return true if entity has this stat
     */
    public static boolean hasStat(@Nonnull Entity entity, @Nonnull String statName) {
        EntityStatMap statMap = getStatMap(entity);
        if (statMap == null) {
            return false;
        }
        
        int statIndex = EntityStatType.getAssetMap().getIndex(statName);
        if (statIndex == Integer.MIN_VALUE) {
            return false;
        }
        
        return statMap.get(statIndex) != null;
    }
    
    /**
     * Get the EntityStatMap component for an entity.
     * 
     * @param entity Entity to get stat map from
     * @return EntityStatMap component, or null if not found
     */
    @Nullable
    private static EntityStatMap getStatMap(@Nonnull Entity entity) {
        if (entity.getWorld() == null) {
            return null;
        }
        
        Store<EntityStore> store = entity.getWorld().getEntityStore().getStore();
        return store.getComponent(entity.getReference(), EntityStatMap.getComponentType());
    }
}

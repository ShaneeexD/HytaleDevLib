package org.hytaledevlib.lib;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Helper utilities for working with ECS components.
 * Provides convenient methods using ComponentType for type-safe component access.
 * 
 * Example usage:
 * <pre>
 * ComponentAccessor&lt;EntityStore&gt; accessor = world.getEntityStore().getStore().getAccessor();
 * ComponentType&lt;EntityStore, ItemComponent&gt; itemType = ItemComponent.getComponentType();
 * 
 * if (ComponentHelper.hasComponent(accessor, entityRef, itemType)) {
 *     ItemComponent item = ComponentHelper.getComponent(accessor, entityRef, itemType);
 * }
 * </pre>
 */
public class ComponentHelper {
    
    /**
     * Get a component from an entity reference using ComponentType.
     * 
     * @param accessor Component accessor
     * @param entityRef Entity reference
     * @param componentType Component type
     * @return Component instance, or null if not present
     */
    public static <T extends Component<EntityStore>> T getComponent(
            ComponentAccessor<EntityStore> accessor,
            Ref<EntityStore> entityRef,
            ComponentType<EntityStore, T> componentType) {
        
        if (entityRef == null || componentType == null) {
            return null;
        }
        
        try {
            return accessor.getComponent(entityRef, componentType);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Add or update a component on an entity using ComponentType.
     * 
     * @param accessor Component accessor
     * @param entityRef Entity reference
     * @param componentType Component type
     * @param component Component instance
     * @return true if successful
     */
    public static <T extends Component<EntityStore>> boolean putComponent(
            ComponentAccessor<EntityStore> accessor,
            Ref<EntityStore> entityRef,
            ComponentType<EntityStore, T> componentType,
            T component) {
        
        if (entityRef == null || componentType == null || component == null) {
            return false;
        }
        
        try {
            accessor.putComponent(entityRef, componentType, component);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Add a component to an entity using ComponentType.
     * 
     * @param accessor Component accessor
     * @param entityRef Entity reference
     * @param componentType Component type
     * @param component Component instance
     * @return true if successful
     */
    public static <T extends Component<EntityStore>> boolean addComponent(
            ComponentAccessor<EntityStore> accessor,
            Ref<EntityStore> entityRef,
            ComponentType<EntityStore, T> componentType,
            T component) {
        
        if (entityRef == null || componentType == null || component == null) {
            return false;
        }
        
        try {
            accessor.addComponent(entityRef, componentType, component);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Remove a component from an entity using ComponentType.
     * 
     * @param accessor Component accessor
     * @param entityRef Entity reference
     * @param componentType Component type
     * @return true if successful
     */
    public static <T extends Component<EntityStore>> boolean removeComponent(
            ComponentAccessor<EntityStore> accessor,
            Ref<EntityStore> entityRef,
            ComponentType<EntityStore, T> componentType) {
        
        if (entityRef == null || componentType == null) {
            return false;
        }
        
        try {
            accessor.removeComponent(entityRef, componentType);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if an entity has a specific component.
     * This is a convenience method - check the archetype for better performance.
     * 
     * @param accessor Component accessor
     * @param entityRef Entity reference
     * @param componentType Component type
     * @return true if entity has the component
     */
    public static <T extends Component<EntityStore>> boolean hasComponent(
            ComponentAccessor<EntityStore> accessor,
            Ref<EntityStore> entityRef,
            ComponentType<EntityStore, T> componentType) {
        
        if (entityRef == null || componentType == null) {
            return false;
        }
        
        try {
            return getComponent(accessor, entityRef, componentType) != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Set a display name on an entity.
     * 
     * @param accessor Component accessor
     * @param entityRef Entity reference
     * @param displayName Display name text
     * @return true if successful
     */
    public static boolean setDisplayName(
            ComponentAccessor<EntityStore> accessor,
            Ref<EntityStore> entityRef,
            String displayName) {
        
        try {
            ComponentType<EntityStore, DisplayNameComponent> type = DisplayNameComponent.getComponentType();
            DisplayNameComponent component = new DisplayNameComponent(Message.raw(displayName));
            return putComponent(accessor, entityRef, type, component);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get the item ID from an item entity.
     * 
     * @param accessor Component accessor
     * @param entityRef Entity reference
     * @return Item ID, or null if not an item entity
     */
    public static String getItemId(
            ComponentAccessor<EntityStore> accessor,
            Ref<EntityStore> entityRef) {
        
        try {
            ComponentType<EntityStore, ItemComponent> type = ItemComponent.getComponentType();
            ItemComponent item = getComponent(accessor, entityRef, type);
            return item != null ? item.getItemStack().getItemId() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Get the item quantity from an item entity.
     * 
     * @param accessor Component accessor
     * @param entityRef Entity reference
     * @return Item quantity, or 0 if not an item entity
     */
    public static int getItemQuantity(
            ComponentAccessor<EntityStore> accessor,
            Ref<EntityStore> entityRef) {
        
        try {
            ComponentType<EntityStore, ItemComponent> type = ItemComponent.getComponentType();
            ItemComponent item = getComponent(accessor, entityRef, type);
            return item != null ? item.getItemStack().getQuantity() : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}

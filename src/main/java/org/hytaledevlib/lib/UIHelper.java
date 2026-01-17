package org.hytaledevlib.lib;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.HudComponent;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Helper class for UI operations in Hytale.
 * Provides utilities for managing custom pages, HUD components, and UI interactions.
 */
public class UIHelper {
    
    /**
     * Create a simple custom UI page with a builder pattern.
     * 
     * @param player The player entity
     * @param lifetime The page lifetime (UntilDismissed, UntilDeath, etc.)
     * @param builder Callback to build the UI using UICommandBuilder
     * @return true if the page was opened successfully
     */
    public static boolean openCustomPage(Entity player, CustomPageLifetime lifetime, 
                                        BiConsumer<UICommandBuilder, UIEventBuilder> builder) {
        try {
            Player playerComponent = PlayerHelper.getPlayerComponent(player);
            if (playerComponent == null) return false;
            
            PlayerRef playerRef = playerComponent.getPlayerRef();
            PageManager pageManager = playerComponent.getPageManager();
            
            CustomUIPage page = new CustomUIPage(playerRef, lifetime) {
                @Override
                public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder, 
                                @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {
                    builder.accept(commandBuilder, eventBuilder);
                }
            };
            
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref != null) {
                pageManager.openCustomPage(ref, ref.getStore(), page);
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Close the current custom page for a player.
     * 
     * @param player The player entity
     * @return true if successful
     */
    public static boolean closeCustomPage(Entity player) {
        try {
            Player playerComponent = PlayerHelper.getPlayerComponent(player);
            if (playerComponent == null) return false;
            
            PlayerRef playerRef = playerComponent.getPlayerRef();
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref != null) {
                playerComponent.getPageManager().setPage(ref, ref.getStore(), Page.None);
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Set the player's current page (e.g., Page.Inventory, Page.Map, Page.None).
     * 
     * @param player The player entity
     * @param page The page to set
     * @return true if successful
     */
    public static boolean setPage(Entity player, Page page) {
        try {
            Player playerComponent = PlayerHelper.getPlayerComponent(player);
            if (playerComponent == null) return false;
            
            PlayerRef playerRef = playerComponent.getPlayerRef();
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref != null) {
                playerComponent.getPageManager().setPage(ref, ref.getStore(), page);
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Show specific HUD components for a player.
     * 
     * @param player The player entity
     * @param components HUD components to show
     * @return true if successful
     */
    public static boolean showHudComponents(Entity player, HudComponent... components) {
        try {
            Player playerComponent = PlayerHelper.getPlayerComponent(player);
            if (playerComponent == null) return false;
            
            HudManager hudManager = playerComponent.getHudManager();
            PlayerRef playerRef = playerComponent.getPlayerRef();
            hudManager.showHudComponents(playerRef, components);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Hide specific HUD components for a player.
     * 
     * @param player The player entity
     * @param components HUD components to hide
     * @return true if successful
     */
    public static boolean hideHudComponents(Entity player, HudComponent... components) {
        try {
            Player playerComponent = PlayerHelper.getPlayerComponent(player);
            if (playerComponent == null) return false;
            
            HudManager hudManager = playerComponent.getHudManager();
            PlayerRef playerRef = playerComponent.getPlayerRef();
            hudManager.hideHudComponents(playerRef, components);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Set which HUD components are visible (replaces all current components).
     * 
     * @param player The player entity
     * @param components HUD components to show (all others will be hidden)
     * @return true if successful
     */
    public static boolean setVisibleHudComponents(Entity player, HudComponent... components) {
        try {
            Player playerComponent = PlayerHelper.getPlayerComponent(player);
            if (playerComponent == null) return false;
            
            HudManager hudManager = playerComponent.getHudManager();
            PlayerRef playerRef = playerComponent.getPlayerRef();
            hudManager.setVisibleHudComponents(playerRef, components);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get the currently visible HUD components for a player.
     * 
     * @param player The player entity
     * @return Set of visible HUD components, or null if failed
     */
    @Nullable
    public static Set<HudComponent> getVisibleHudComponents(Entity player) {
        try {
            Player playerComponent = PlayerHelper.getPlayerComponent(player);
            if (playerComponent == null) return null;
            
            return playerComponent.getHudManager().getVisibleHudComponents();
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Check if a player has a custom page open.
     * 
     * @param player The player entity
     * @return true if a custom page is open
     */
    public static boolean hasCustomPageOpen(Entity player) {
        try {
            Player playerComponent = PlayerHelper.getPlayerComponent(player);
            if (playerComponent == null) return false;
            
            return playerComponent.getPageManager().getCustomPage() != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Hide all HUD components (clean screen).
     * 
     * @param player The player entity
     * @return true if successful
     */
    public static boolean hideAllHud(Entity player) {
        return setVisibleHudComponents(player);
    }
    
    /**
     * Restore default HUD components.
     * 
     * @param player The player entity
     * @return true if successful
     */
    public static boolean showDefaultHud(Entity player) {
        return setVisibleHudComponents(player,
            HudComponent.UtilitySlotSelector,
            HudComponent.BlockVariantSelector,
            HudComponent.StatusIcons,
            HudComponent.Hotbar,
            HudComponent.Chat,
            HudComponent.Notifications,
            HudComponent.KillFeed,
            HudComponent.InputBindings,
            HudComponent.Reticle,
            HudComponent.Compass,
            HudComponent.Speedometer,
            HudComponent.ObjectivePanel,
            HudComponent.PortalPanel,
            HudComponent.EventTitle,
            HudComponent.Stamina,
            HudComponent.AmmoIndicator,
            HudComponent.Health,
            HudComponent.Mana,
            HudComponent.Oxygen,
            HudComponent.BuilderToolsLegend,
            HudComponent.Sleep
        );
    }
    
    // ==================== FADE UTILITIES ====================
    
    /**
     * Functional interface for fade completion callbacks.
     */
    @FunctionalInterface
    public interface FadeCallback {
        void onComplete();
    }
    
    /**
     * Fade an element's opacity over time using a custom page's command builder.
     * This requires the element to have an id/selector and the page to be open.
     * 
     * NOTE: For best results, use CSS transitions in your RML:
     * <pre>
     * .fade-element {
     *     opacity: 1;
     *     transition: opacity 0.3s ease;
     * }
     * </pre>
     * Then simply call set() once to change the opacity and let CSS handle the animation.
     * 
     * @param world The world for tick scheduling
     * @param player The player entity
     * @param selector CSS selector for the element (e.g., "#myElement" or ".myClass")
     * @param fromOpacity Starting opacity (0.0 to 1.0)
     * @param toOpacity Target opacity (0.0 to 1.0)
     * @param durationTicks Duration in game ticks (20 ticks = 1 second)
     * @param steps Number of opacity steps (more = smoother, but more packets)
     * @param onComplete Optional callback when fade completes
     */
    public static void fadeElement(com.hypixel.hytale.server.core.universe.world.World world,
                                   Entity player, String selector, 
                                   float fromOpacity, float toOpacity,
                                   int durationTicks, int steps,
                                   @Nullable FadeCallback onComplete) {
        if (steps <= 0) steps = 1;
        if (durationTicks <= 0) durationTicks = 1;
        
        final int ticksPerStep = Math.max(1, durationTicks / steps);
        final float opacityStep = (toOpacity - fromOpacity) / steps;
        final int totalSteps = steps;
        
        fadeStep(world, player, selector, fromOpacity, opacityStep, ticksPerStep, 0, totalSteps, onComplete);
    }
    
    private static void fadeStep(com.hypixel.hytale.server.core.universe.world.World world,
                                 Entity player, String selector,
                                 float currentOpacity, float opacityStep,
                                 int ticksPerStep, int currentStep, int totalSteps,
                                 @Nullable FadeCallback onComplete) {
        try {
            Player playerComponent = PlayerHelper.getPlayerComponent(player);
            if (playerComponent == null) return;
            
            // Send opacity update command via PageManager
            PageManager pageManager = playerComponent.getPageManager();
            CustomUIPage customPage = pageManager.getCustomPage();
            
            if (customPage != null) {
                // Build command to set opacity
                UICommandBuilder commandBuilder = new UICommandBuilder();
                commandBuilder.set(selector + "@style.opacity", currentOpacity);
                
                // Send update through PageManager
                pageManager.updateCustomPage(
                    new com.hypixel.hytale.protocol.packets.interface_.CustomPage(
                        customPage.getClass().getName(),
                        false,  // not initial
                        false,  // don't clear
                        customPage.getLifetime(),
                        commandBuilder.getCommands(),
                        UIEventBuilder.EMPTY_EVENT_BINDING_ARRAY
                    )
                );
            }
            
            // Schedule next step or complete
            if (currentStep < totalSteps - 1) {
                WorldHelper.waitTicks(world, ticksPerStep, () -> {
                    fadeStep(world, player, selector, currentOpacity + opacityStep, 
                            opacityStep, ticksPerStep, currentStep + 1, totalSteps, onComplete);
                });
            } else if (onComplete != null) {
                WorldHelper.waitTicks(world, ticksPerStep, onComplete::onComplete);
            }
        } catch (Exception e) {
            // Fade failed, call completion anyway
            if (onComplete != null) {
                onComplete.onComplete();
            }
        }
    }
    
    /**
     * Fade in an element (opacity 0 -> 1).
     * 
     * @param world The world for tick scheduling
     * @param player The player entity
     * @param selector CSS selector for the element
     * @param durationTicks Duration in game ticks (20 ticks = 1 second)
     * @param onComplete Optional callback when fade completes
     */
    public static void fadeIn(com.hypixel.hytale.server.core.universe.world.World world,
                              Entity player, String selector, int durationTicks,
                              @Nullable FadeCallback onComplete) {
        fadeElement(world, player, selector, 0.0f, 1.0f, durationTicks, 10, onComplete);
    }
    
    /**
     * Fade out an element (opacity 1 -> 0).
     * 
     * @param world The world for tick scheduling
     * @param player The player entity
     * @param selector CSS selector for the element
     * @param durationTicks Duration in game ticks (20 ticks = 1 second)
     * @param onComplete Optional callback when fade completes
     */
    public static void fadeOut(com.hypixel.hytale.server.core.universe.world.World world,
                               Entity player, String selector, int durationTicks,
                               @Nullable FadeCallback onComplete) {
        fadeElement(world, player, selector, 1.0f, 0.0f, durationTicks, 10, onComplete);
    }
}

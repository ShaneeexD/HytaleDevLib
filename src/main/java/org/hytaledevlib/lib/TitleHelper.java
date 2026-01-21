package org.hytaledevlib.lib;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.FormattedMessage;
import com.hypixel.hytale.protocol.packets.interface_.HideEventTitle;
import com.hypixel.hytale.protocol.packets.interface_.ShowEventTitle;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nullable;
import java.util.logging.Level;

/**
 * Helper class for displaying on-screen titles to players.
 * Uses Hytale's EventTitle system for large, centered text displays.
 * 
 * Perfect for:
 * - Quest notifications
 * - Boss encounter announcements
 * - Zone entry messages
 * - Achievement unlocks
 * - Important game events
 */
public class TitleHelper {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    
    /**
     * Show a major title to a player (large, prominent text).
     * 
     * @param player The player entity
     * @param primaryText Main title text (large)
     * @param secondaryText Subtitle text (smaller, optional)
     * @param durationSeconds How long to display in seconds (e.g., 3.0 = 3 seconds)
     * @return true if successful
     */
    public static boolean showMajorTitle(Entity player, String primaryText, 
                                        @Nullable String secondaryText, float durationSeconds) {
        return showTitle(player, true, primaryText, secondaryText, 
                        null, durationSeconds, 0.5f, 0.5f);
    }
    
    /**
     * Show a minor title to a player (smaller, less intrusive).
     * 
     * @param player The player entity
     * @param primaryText Main title text
     * @param secondaryText Subtitle text (optional)
     * @param durationSeconds How long to display in seconds (e.g., 2.0 = 2 seconds)
     * @return true if successful
     */
    public static boolean showMinorTitle(Entity player, String primaryText,
                                        @Nullable String secondaryText, float durationSeconds) {
        return showTitle(player, false, primaryText, secondaryText,
                        null, durationSeconds, 0.5f, 0.5f);
    }
    
    /**
     * Show a title with a custom icon.
     * 
     * @param player The player entity
     * @param isMajor true for major title (large), false for minor (smaller)
     * @param primaryText Main title text
     * @param secondaryText Subtitle text (optional)
     * @param iconPath Path to icon asset (optional)
     * @param durationSeconds How long to display in seconds
     * @return true if successful
     */
    public static boolean showTitleWithIcon(Entity player, boolean isMajor,
                                           String primaryText, @Nullable String secondaryText,
                                           String iconPath, float durationSeconds) {
        return showTitle(player, isMajor, primaryText, secondaryText, iconPath,
                        durationSeconds, 0.5f, 0.5f);
    }
    
    /**
     * Show a title with full control over timing.
     * 
     * @param player The player entity
     * @param isMajor true for major title (large), false for minor (smaller)
     * @param primaryText Main title text
     * @param secondaryText Subtitle text (optional)
     * @param iconPath Path to icon asset (optional)
     * @param durationSeconds How long to display in seconds
     * @param fadeInSeconds Fade in duration in seconds
     * @param fadeOutSeconds Fade out duration in seconds
     * @return true if successful
     */
    public static boolean showTitle(Entity player, boolean isMajor,
                                   String primaryText, @Nullable String secondaryText,
                                   @Nullable String iconPath,
                                   float durationSeconds, float fadeInSeconds, float fadeOutSeconds) {
        try {
            Player playerComponent = PlayerHelper.getPlayerComponent(player);
            if (playerComponent == null) {
                LOGGER.at(Level.WARNING).log("Cannot show title: entity is not a player");
                return false;
            }
            
            // Create FormattedMessage for primary and secondary text
            FormattedMessage primary = null;
            if (primaryText != null) {
                primary = new FormattedMessage();
                primary.rawText = primaryText;
            }
            
            FormattedMessage secondary = null;
            if (secondaryText != null) {
                secondary = new FormattedMessage();
                secondary.rawText = secondaryText;
            }
            
            // Create the show event title packet
            ShowEventTitle titlePacket = new ShowEventTitle(
                fadeInSeconds,
                fadeOutSeconds,
                durationSeconds,
                iconPath,
                isMajor,
                primary,
                secondary
            );
            
            // Send the packet to the player via PacketHandler
            PlayerRef playerRef = ComponentHelper.getComponent(
                playerComponent.getReference().getStore(),
                playerComponent.getReference(),
                PlayerRef.getComponentType()
            );
            
            if (playerRef != null) {
                playerRef.getPacketHandler().write(titlePacket);
            } else {
                LOGGER.at(Level.WARNING).log("Cannot show title: player ref is null");
                return false;
            }
            
            return true;
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("Failed to show title to player: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Show a quick notification title (1 second duration).
     * 
     * @param player The player entity
     * @param text The notification text
     * @return true if successful
     */
    public static boolean showNotification(Entity player, String text) {
        return showMinorTitle(player, text, null, 1.0f);
    }
    
    /**
     * Show a quest update title.
     * 
     * @param player The player entity
     * @param questName Quest name
     * @param objective Quest objective or update
     * @return true if successful
     */
    public static boolean showQuestUpdate(Entity player, String questName, String objective) {
        return showMajorTitle(player, questName, objective, 3.0f);
    }
    
    /**
     * Show a boss encounter title.
     * 
     * @param player The player entity
     * @param bossName Boss name
     * @param subtitle Optional subtitle (e.g., "Prepare for battle!")
     * @return true if successful
     */
    public static boolean showBossTitle(Entity player, String bossName, @Nullable String subtitle) {
        return showMajorTitle(player, bossName, subtitle, 4.0f);
    }
    
    /**
     * Show a zone entry title.
     * 
     * @param player The player entity
     * @param zoneName Zone name
     * @return true if successful
     */
    public static boolean showZoneEntry(Entity player, String zoneName) {
        return showMinorTitle(player, "Entering " + zoneName, null, 2.0f);
    }
    
    /**
     * Show an achievement unlock title.
     * 
     * @param player The player entity
     * @param achievementName Achievement name
     * @return true if successful
     */
    public static boolean showAchievement(Entity player, String achievementName) {
        return showMajorTitle(player, "Achievement Unlocked!", achievementName, 3.0f);
    }
    
    /**
     * Hide/clear the current title for a player with a fade out animation.
     * 
     * @param player The player entity
     * @param fadeOutSeconds Fade out duration in seconds
     * @return true if successful
     */
    public static boolean hideTitle(Entity player, float fadeOutSeconds) {
        try {
            Player playerComponent = PlayerHelper.getPlayerComponent(player);
            if (playerComponent == null) {
                LOGGER.at(Level.WARNING).log("Cannot hide title: entity is not a player");
                return false;
            }
            
            // Create the hide event title packet
            HideEventTitle hidePacket = new HideEventTitle(fadeOutSeconds);
            
            // Send the packet to the player via PacketHandler
            PlayerRef playerRef = ComponentHelper.getComponent(
                playerComponent.getReference().getStore(),
                playerComponent.getReference(),
                PlayerRef.getComponentType()
            );
            
            if (playerRef != null) {
                playerRef.getPacketHandler().write(hidePacket);
            } else {
                LOGGER.at(Level.WARNING).log("Cannot hide title: player ref is null");
                return false;
            }
            
            return true;
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("Failed to hide title for player: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Instantly clear the current title for a player (no fade).
     * 
     * @param player The player entity
     * @return true if successful
     */
    public static boolean clearTitle(Entity player) {
        return hideTitle(player, 0.0f);
    }
}

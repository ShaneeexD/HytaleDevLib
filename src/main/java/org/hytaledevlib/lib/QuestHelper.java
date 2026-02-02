package org.hytaledevlib.lib;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.World;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;

/**
 * QuestHelper - Manages quests, objectives, and player progress
 * 
 * Features:
 * - Quest creation with multiple objectives
 * - Progress tracking per player
 * - Quest completion rewards
 * - Quest chains and prerequisites
 * - Objective types: kill, collect, visit, custom
 */
public class QuestHelper {
    
    // Quest storage: questId -> Quest
    private static final Map<String, Quest> quests = new ConcurrentHashMap<>();
    
    // Player progress: playerUUID -> (questId -> QuestProgress)
    private static final Map<UUID, Map<String, QuestProgress>> playerProgress = new ConcurrentHashMap<>();
    
    /**
     * Quest definition
     */
    public static class Quest {
        private final String id;
        private final String name;
        private final String description;
        private final List<Objective> objectives;
        private final List<Reward> rewards;
        private final Set<String> prerequisites;
        private final boolean repeatable;
        
        public Quest(String id, String name, String description, List<Objective> objectives, 
                     List<Reward> rewards, Set<String> prerequisites, boolean repeatable) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.objectives = objectives;
            this.rewards = rewards;
            this.prerequisites = prerequisites != null ? prerequisites : new HashSet<>();
            this.repeatable = repeatable;
        }
        
        public String getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public List<Objective> getObjectives() { return objectives; }
        public List<Reward> getRewards() { return rewards; }
        public Set<String> getPrerequisites() { return prerequisites; }
        public boolean isRepeatable() { return repeatable; }
    }
    
    /**
     * Quest objective
     */
    public static class Objective {
        private final String id;
        private final ObjectiveType type;
        private final String target;
        private final int requiredAmount;
        private final String description;
        
        public Objective(String id, ObjectiveType type, String target, int requiredAmount, String description) {
            this.id = id;
            this.type = type;
            this.target = target;
            this.requiredAmount = requiredAmount;
            this.description = description;
        }
        
        public String getId() { return id; }
        public ObjectiveType getType() { return type; }
        public String getTarget() { return target; }
        public int getRequiredAmount() { return requiredAmount; }
        public String getDescription() { return description; }
    }
    
    /**
     * Objective types
     */
    public enum ObjectiveType {
        KILL,           // Kill X entities of type
        COLLECT,        // Collect X items
        VISIT,          // Visit a location/zone
        BREAK_BLOCK,    // Break X blocks of type
        PLACE_BLOCK,    // Place X blocks of type
        CUSTOM          // Custom objective with manual progress
    }
    
    /**
     * Quest reward
     */
    public static class Reward {
        private final RewardType type;
        private final String target;
        private final int amount;
        
        public Reward(RewardType type, String target, int amount) {
            this.type = type;
            this.target = target;
            this.amount = amount;
        }
        
        public RewardType getType() { return type; }
        public String getTarget() { return target; }
        public int getAmount() { return amount; }
    }
    
    /**
     * Reward types
     */
    public enum RewardType {
        ITEM,           // Give item
        CURRENCY,       // Give currency (requires EconomyHelper)
        EXPERIENCE,     // Give XP
        CUSTOM          // Custom reward handler
    }
    
    /**
     * Player quest progress
     */
    public static class QuestProgress {
        private final String questId;
        private final Map<String, Integer> objectiveProgress;
        private boolean completed;
        private long startTime;
        private long completionTime;
        
        public QuestProgress(String questId) {
            this.questId = questId;
            this.objectiveProgress = new ConcurrentHashMap<>();
            this.completed = false;
            this.startTime = System.currentTimeMillis();
            this.completionTime = 0;
        }
        
        public String getQuestId() { return questId; }
        public Map<String, Integer> getObjectiveProgress() { return objectiveProgress; }
        public boolean isCompleted() { return completed; }
        public void setCompleted(boolean completed) { 
            this.completed = completed;
            if (completed) {
                this.completionTime = System.currentTimeMillis();
            }
        }
        public long getStartTime() { return startTime; }
        public long getCompletionTime() { return completionTime; }
        
        public int getProgress(String objectiveId) {
            return objectiveProgress.getOrDefault(objectiveId, 0);
        }
        
        public void setProgress(String objectiveId, int progress) {
            objectiveProgress.put(objectiveId, progress);
        }
        
        public void incrementProgress(String objectiveId, int amount) {
            objectiveProgress.merge(objectiveId, amount, Integer::sum);
        }
    }
    
    /**
     * Quest builder for easy quest creation
     */
    public static class QuestBuilder {
        private String id;
        private String name;
        private String description;
        private final List<Objective> objectives = new ArrayList<>();
        private final List<Reward> rewards = new ArrayList<>();
        private final Set<String> prerequisites = new HashSet<>();
        private boolean repeatable = false;
        
        public QuestBuilder(String id) {
            this.id = id;
        }
        
        public QuestBuilder name(String name) {
            this.name = name;
            return this;
        }
        
        public QuestBuilder description(String description) {
            this.description = description;
            return this;
        }
        
        public QuestBuilder addObjective(String id, ObjectiveType type, String target, int amount, String description) {
            objectives.add(new Objective(id, type, target, amount, description));
            return this;
        }
        
        public QuestBuilder addKillObjective(String id, String entityType, int amount) {
            return addObjective(id, ObjectiveType.KILL, entityType, amount, "Kill " + amount + " " + entityType);
        }
        
        public QuestBuilder addCollectObjective(String id, String itemId, int amount) {
            return addObjective(id, ObjectiveType.COLLECT, itemId, amount, "Collect " + amount + " " + itemId);
        }
        
        public QuestBuilder addVisitObjective(String id, String zoneName) {
            return addObjective(id, ObjectiveType.VISIT, zoneName, 1, "Visit " + zoneName);
        }
        
        public QuestBuilder addReward(RewardType type, String target, int amount) {
            rewards.add(new Reward(type, target, amount));
            return this;
        }
        
        public QuestBuilder addItemReward(String itemId, int amount) {
            return addReward(RewardType.ITEM, itemId, amount);
        }
        
        public QuestBuilder addCurrencyReward(int amount) {
            return addReward(RewardType.CURRENCY, "coins", amount);
        }
        
        public QuestBuilder prerequisite(String questId) {
            prerequisites.add(questId);
            return this;
        }
        
        public QuestBuilder repeatable(boolean repeatable) {
            this.repeatable = repeatable;
            return this;
        }
        
        public Quest build() {
            return new Quest(id, name, description, objectives, rewards, prerequisites, repeatable);
        }
    }
    
    // ==================== Quest Registration ====================
    
    /**
     * Register a quest
     */
    public static void registerQuest(Quest quest) {
        quests.put(quest.getId(), quest);
    }
    
    /**
     * Get a quest by ID
     */
    @Nullable
    public static Quest getQuest(String questId) {
        return quests.get(questId);
    }
    
    /**
     * Get all registered quests
     */
    public static Collection<Quest> getAllQuests() {
        return quests.values();
    }
    
    // ==================== Player Quest Management ====================
    
    /**
     * Start a quest for a player
     */
    public static boolean startQuest(Player player, String questId) {
        Quest quest = quests.get(questId);
        if (quest == null) {
            return false;
        }
        
        UUID playerUUID = player.getUuid();
        
        // Check if already active
        if (hasActiveQuest(player, questId)) {
            return false;
        }
        
        // Check prerequisites
        for (String prereqId : quest.getPrerequisites()) {
            if (!hasCompletedQuest(player, prereqId)) {
                return false;
            }
        }
        
        // Check if already completed and not repeatable
        if (!quest.isRepeatable() && hasCompletedQuest(player, questId)) {
            return false;
        }
        
        // Start quest
        Map<String, QuestProgress> quests = playerProgress.computeIfAbsent(playerUUID, k -> new ConcurrentHashMap<>());
        quests.put(questId, new QuestProgress(questId));
        
        return true;
    }
    
    /**
     * Check if player has an active quest
     */
    public static boolean hasActiveQuest(Player player, String questId) {
        Map<String, QuestProgress> quests = playerProgress.get(player.getUuid());
        if (quests == null) {
            return false;
        }
        QuestProgress progress = quests.get(questId);
        return progress != null && !progress.isCompleted();
    }
    
    /**
     * Check if player has completed a quest
     */
    public static boolean hasCompletedQuest(Player player, String questId) {
        Map<String, QuestProgress> quests = playerProgress.get(player.getUuid());
        if (quests == null) {
            return false;
        }
        QuestProgress progress = quests.get(questId);
        return progress != null && progress.isCompleted();
    }
    
    /**
     * Get player's quest progress
     */
    @Nullable
    public static QuestProgress getQuestProgress(Player player, String questId) {
        Map<String, QuestProgress> quests = playerProgress.get(player.getUuid());
        return quests != null ? quests.get(questId) : null;
    }
    
    /**
     * Get all active quests for a player
     */
    public static List<QuestProgress> getActiveQuests(Player player) {
        Map<String, QuestProgress> quests = playerProgress.get(player.getUuid());
        if (quests == null) {
            return Collections.emptyList();
        }
        
        List<QuestProgress> active = new ArrayList<>();
        for (QuestProgress progress : quests.values()) {
            if (!progress.isCompleted()) {
                active.add(progress);
            }
        }
        return active;
    }
    
    // ==================== Objective Progress ====================
    
    /**
     * Update objective progress
     */
    public static void updateObjectiveProgress(Player player, String questId, String objectiveId, int amount) {
        QuestProgress progress = getQuestProgress(player, questId);
        if (progress == null || progress.isCompleted()) {
            return;
        }
        
        Quest quest = quests.get(questId);
        if (quest == null) {
            return;
        }
        
        // Find objective
        Objective objective = null;
        for (Objective obj : quest.getObjectives()) {
            if (obj.getId().equals(objectiveId)) {
                objective = obj;
                break;
            }
        }
        
        if (objective == null) {
            return;
        }
        
        // Update progress
        int currentProgress = progress.getProgress(objectiveId);
        int newProgress = Math.min(currentProgress + amount, objective.getRequiredAmount());
        progress.setProgress(objectiveId, newProgress);
        
        // Check if quest is complete
        checkQuestCompletion(player, questId);
    }
    
    /**
     * Set objective progress to a specific value
     */
    public static void setObjectiveProgress(Player player, String questId, String objectiveId, int progress) {
        QuestProgress questProgress = getQuestProgress(player, questId);
        if (questProgress == null || questProgress.isCompleted()) {
            return;
        }
        
        questProgress.setProgress(objectiveId, progress);
        checkQuestCompletion(player, questId);
    }
    
    /**
     * Check if all objectives are complete and complete the quest if so
     */
    private static void checkQuestCompletion(Player player, String questId) {
        Quest quest = quests.get(questId);
        QuestProgress progress = getQuestProgress(player, questId);
        
        if (quest == null || progress == null || progress.isCompleted()) {
            return;
        }
        
        // Check all objectives
        boolean allComplete = true;
        for (Objective objective : quest.getObjectives()) {
            int currentProgress = progress.getProgress(objective.getId());
            if (currentProgress < objective.getRequiredAmount()) {
                allComplete = false;
                break;
            }
        }
        
        if (allComplete) {
            completeQuest(player, questId);
        }
    }
    
    /**
     * Complete a quest and give rewards
     */
    public static void completeQuest(Player player, String questId) {
        Quest quest = quests.get(questId);
        QuestProgress progress = getQuestProgress(player, questId);
        
        if (quest == null || progress == null || progress.isCompleted()) {
            return;
        }
        
        progress.setCompleted(true);
        
        // Give rewards
        giveQuestRewards(player, quest);
        
        // Trigger completion callback if registered
        BiConsumer<Player, String> callback = completionCallbacks.get(questId);
        if (callback != null) {
            callback.accept(player, questId);
        }
    }
    
    /**
     * Give quest rewards to player
     */
    private static void giveQuestRewards(Player player, Quest quest) {
        for (Reward reward : quest.getRewards()) {
            switch (reward.getType()) {
                case ITEM:
                    // Give item reward
                    try {
                        InventoryHelper.giveItem(player, reward.getTarget(), reward.getAmount());
                    } catch (Exception e) {
                        System.err.println("[QuestHelper] Failed to give item reward: " + e.getMessage());
                    }
                    break;
                    
                case CURRENCY:
                    // Give currency (requires EconomyHelper)
                    try {
                        EconomyHelper.addBalance(player, reward.getAmount());
                    } catch (Exception e) {
                        System.err.println("[QuestHelper] Failed to give currency reward (EconomyHelper not available): " + e.getMessage());
                    }
                    break;
                    
                case EXPERIENCE:
                    // TODO: Implement XP rewards when XP system is available
                    break;
                    
                case CUSTOM:
                    // Custom rewards handled by callback
                    break;
            }
        }
    }
    
    // ==================== Quest Callbacks ====================
    
    private static final Map<String, BiConsumer<Player, String>> completionCallbacks = new ConcurrentHashMap<>();
    
    /**
     * Register a callback for when a quest is completed
     */
    public static void onQuestComplete(String questId, BiConsumer<Player, String> callback) {
        completionCallbacks.put(questId, callback);
    }
    
    // ==================== Utility Methods ====================
    
    /**
     * Abandon a quest
     */
    public static void abandonQuest(Player player, String questId) {
        Map<String, QuestProgress> quests = playerProgress.get(player.getUuid());
        if (quests != null) {
            quests.remove(questId);
        }
    }
    
    /**
     * Reset all quest progress for a player
     */
    public static void resetPlayerQuests(Player player) {
        playerProgress.remove(player.getUuid());
    }
    
    /**
     * Get quest completion percentage
     */
    public static double getQuestCompletionPercentage(Player player, String questId) {
        Quest quest = quests.get(questId);
        QuestProgress progress = getQuestProgress(player, questId);
        
        if (quest == null || progress == null) {
            return 0.0;
        }
        
        if (progress.isCompleted()) {
            return 100.0;
        }
        
        int totalRequired = 0;
        int totalProgress = 0;
        
        for (Objective objective : quest.getObjectives()) {
            totalRequired += objective.getRequiredAmount();
            totalProgress += Math.min(progress.getProgress(objective.getId()), objective.getRequiredAmount());
        }
        
        return totalRequired > 0 ? (totalProgress * 100.0) / totalRequired : 0.0;
    }
}

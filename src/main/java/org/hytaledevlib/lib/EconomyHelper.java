package org.hytaledevlib.lib;

import com.hypixel.hytale.server.core.entity.entities.Player;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;

/**
 * EconomyHelper - Virtual currency and transaction management
 * 
 * Features:
 * - Multiple currency support
 * - Player balance management
 * - Transaction logging
 * - Transaction callbacks
 * - Top balance queries
 * - Persistent storage ready
 */
public class EconomyHelper {
    
    // Player balances: playerUUID -> (currencyId -> balance)
    private static final Map<UUID, Map<String, Double>> balances = new ConcurrentHashMap<>();
    
    // Transaction history: playerUUID -> List<Transaction>
    private static final Map<UUID, List<Transaction>> transactionHistory = new ConcurrentHashMap<>();
    
    // Currency definitions: currencyId -> Currency
    private static final Map<String, Currency> currencies = new ConcurrentHashMap<>();
    
    // Default currency
    private static String defaultCurrency = "coins";
    
    // Transaction callbacks
    private static final List<BiConsumer<Player, Transaction>> transactionCallbacks = new ArrayList<>();
    
    /**
     * Currency definition
     */
    public static class Currency {
        private final String id;
        private final String name;
        private final String symbol;
        private final String pluralName;
        private final double startingBalance;
        private final boolean allowNegative;
        
        public Currency(String id, String name, String symbol, String pluralName, 
                       double startingBalance, boolean allowNegative) {
            this.id = id;
            this.name = name;
            this.symbol = symbol;
            this.pluralName = pluralName;
            this.startingBalance = startingBalance;
            this.allowNegative = allowNegative;
        }
        
        public String getId() { return id; }
        public String getName() { return name; }
        public String getSymbol() { return symbol; }
        public String getPluralName() { return pluralName; }
        public double getStartingBalance() { return startingBalance; }
        public boolean isAllowNegative() { return allowNegative; }
        
        public String format(double amount) {
            String amountStr = String.format("%.2f", amount);
            String currencyName = amount == 1.0 ? name : pluralName;
            return symbol + amountStr + " " + currencyName;
        }
    }
    
    /**
     * Transaction record
     */
    public static class Transaction {
        private final UUID playerUUID;
        private final String currencyId;
        private final double amount;
        private final TransactionType type;
        private final String reason;
        private final long timestamp;
        private final double balanceBefore;
        private final double balanceAfter;
        
        public Transaction(UUID playerUUID, String currencyId, double amount, 
                          TransactionType type, String reason, double balanceBefore, double balanceAfter) {
            this.playerUUID = playerUUID;
            this.currencyId = currencyId;
            this.amount = amount;
            this.type = type;
            this.reason = reason;
            this.timestamp = System.currentTimeMillis();
            this.balanceBefore = balanceBefore;
            this.balanceAfter = balanceAfter;
        }
        
        public UUID getPlayerUUID() { return playerUUID; }
        public String getCurrencyId() { return currencyId; }
        public double getAmount() { return amount; }
        public TransactionType getType() { return type; }
        public String getReason() { return reason; }
        public long getTimestamp() { return timestamp; }
        public double getBalanceBefore() { return balanceBefore; }
        public double getBalanceAfter() { return balanceAfter; }
    }
    
    /**
     * Transaction types
     */
    public enum TransactionType {
        DEPOSIT,        // Money added
        WITHDRAWAL,     // Money removed
        TRANSFER_SEND,  // Money sent to another player
        TRANSFER_RECEIVE, // Money received from another player
        PURCHASE,       // Money spent on purchase
        SALE,           // Money earned from sale
        REWARD,         // Money from quest/achievement
        ADMIN           // Admin-given money
    }
    
    // ==================== Currency Management ====================
    
    /**
     * Register a currency
     */
    public static void registerCurrency(Currency currency) {
        currencies.put(currency.getId(), currency);
    }
    
    /**
     * Register a simple currency
     */
    public static void registerCurrency(String id, String name, String symbol, double startingBalance) {
        registerCurrency(new Currency(id, name, symbol, name + "s", startingBalance, false));
    }
    
    /**
     * Get currency by ID
     */
    @Nullable
    public static Currency getCurrency(String currencyId) {
        return currencies.get(currencyId);
    }
    
    /**
     * Set the default currency
     */
    public static void setDefaultCurrency(String currencyId) {
        defaultCurrency = currencyId;
    }
    
    /**
     * Get the default currency
     */
    public static String getDefaultCurrency() {
        return defaultCurrency;
    }
    
    // ==================== Balance Management ====================
    
    /**
     * Get player balance (default currency)
     */
    public static double getBalance(Player player) {
        return getBalance(player, defaultCurrency);
    }
    
    /**
     * Get player balance for specific currency
     */
    public static double getBalance(Player player, String currencyId) {
        Map<String, Double> playerBalances = balances.get(player.getUuid());
        if (playerBalances == null) {
            Currency currency = currencies.get(currencyId);
            return currency != null ? currency.getStartingBalance() : 0.0;
        }
        
        Double balance = playerBalances.get(currencyId);
        if (balance == null) {
            Currency currency = currencies.get(currencyId);
            return currency != null ? currency.getStartingBalance() : 0.0;
        }
        
        return balance;
    }
    
    /**
     * Set player balance (default currency)
     */
    public static void setBalance(Player player, double amount) {
        setBalance(player, defaultCurrency, amount);
    }
    
    /**
     * Set player balance for specific currency
     */
    public static void setBalance(Player player, String currencyId, double amount) {
        Currency currency = currencies.get(currencyId);
        if (currency != null && !currency.isAllowNegative() && amount < 0) {
            amount = 0;
        }
        
        Map<String, Double> playerBalances = balances.computeIfAbsent(
            player.getUuid(), 
            k -> new ConcurrentHashMap<>()
        );
        
        double oldBalance = getBalance(player, currencyId);
        playerBalances.put(currencyId, amount);
        
        // Log transaction
        TransactionType type = amount > oldBalance ? TransactionType.DEPOSIT : TransactionType.WITHDRAWAL;
        logTransaction(player, currencyId, Math.abs(amount - oldBalance), type, "Balance set", oldBalance, amount);
    }
    
    /**
     * Add to player balance (default currency)
     */
    public static boolean addBalance(Player player, double amount) {
        return addBalance(player, defaultCurrency, amount, TransactionType.DEPOSIT, "Balance added");
    }
    
    /**
     * Add to player balance with reason
     */
    public static boolean addBalance(Player player, double amount, String reason) {
        return addBalance(player, defaultCurrency, amount, TransactionType.DEPOSIT, reason);
    }
    
    /**
     * Add to player balance for specific currency
     */
    public static boolean addBalance(Player player, String currencyId, double amount, TransactionType type, String reason) {
        if (amount < 0) {
            return false;
        }
        
        double currentBalance = getBalance(player, currencyId);
        double newBalance = currentBalance + amount;
        
        Map<String, Double> playerBalances = balances.computeIfAbsent(
            player.getUuid(), 
            k -> new ConcurrentHashMap<>()
        );
        
        playerBalances.put(currencyId, newBalance);
        
        // Log transaction
        logTransaction(player, currencyId, amount, type, reason, currentBalance, newBalance);
        
        return true;
    }
    
    /**
     * Remove from player balance (default currency)
     */
    public static boolean removeBalance(Player player, double amount) {
        return removeBalance(player, defaultCurrency, amount, TransactionType.WITHDRAWAL, "Balance removed");
    }
    
    /**
     * Remove from player balance with reason
     */
    public static boolean removeBalance(Player player, double amount, String reason) {
        return removeBalance(player, defaultCurrency, amount, TransactionType.WITHDRAWAL, reason);
    }
    
    /**
     * Remove from player balance for specific currency
     */
    public static boolean removeBalance(Player player, String currencyId, double amount, TransactionType type, String reason) {
        if (amount < 0) {
            return false;
        }
        
        double currentBalance = getBalance(player, currencyId);
        double newBalance = currentBalance - amount;
        
        Currency currency = currencies.get(currencyId);
        if (currency != null && !currency.isAllowNegative() && newBalance < 0) {
            return false; // Insufficient funds
        }
        
        Map<String, Double> playerBalances = balances.computeIfAbsent(
            player.getUuid(), 
            k -> new ConcurrentHashMap<>()
        );
        
        playerBalances.put(currencyId, newBalance);
        
        // Log transaction
        logTransaction(player, currencyId, amount, type, reason, currentBalance, newBalance);
        
        return true;
    }
    
    /**
     * Check if player has enough balance (default currency)
     */
    public static boolean hasBalance(Player player, double amount) {
        return hasBalance(player, defaultCurrency, amount);
    }
    
    /**
     * Check if player has enough balance for specific currency
     */
    public static boolean hasBalance(Player player, String currencyId, double amount) {
        return getBalance(player, currencyId) >= amount;
    }
    
    // ==================== Transactions ====================
    
    /**
     * Transfer money between players (default currency)
     */
    public static boolean transfer(Player sender, Player receiver, double amount) {
        return transfer(sender, receiver, defaultCurrency, amount, "Transfer");
    }
    
    /**
     * Transfer money between players with reason
     */
    public static boolean transfer(Player sender, Player receiver, double amount, String reason) {
        return transfer(sender, receiver, defaultCurrency, amount, reason);
    }
    
    /**
     * Transfer money between players for specific currency
     */
    public static boolean transfer(Player sender, Player receiver, String currencyId, double amount, String reason) {
        if (amount <= 0) {
            return false;
        }
        
        // Check if sender has enough
        if (!hasBalance(sender, currencyId, amount)) {
            return false;
        }
        
        // Remove from sender
        boolean removed = removeBalance(sender, currencyId, amount, TransactionType.TRANSFER_SEND, 
            "Transfer to " + EntityHelper.getName(receiver) + ": " + reason);
        
        if (!removed) {
            return false;
        }
        
        // Add to receiver
        addBalance(receiver, currencyId, amount, TransactionType.TRANSFER_RECEIVE, 
            "Transfer from " + EntityHelper.getName(sender) + ": " + reason);
        
        return true;
    }
    
    /**
     * Log a transaction
     */
    private static void logTransaction(Player player, String currencyId, double amount, 
                                      TransactionType type, String reason, 
                                      double balanceBefore, double balanceAfter) {
        Transaction transaction = new Transaction(
            player.getUuid(), 
            currencyId, 
            amount, 
            type, 
            reason, 
            balanceBefore, 
            balanceAfter
        );
        
        List<Transaction> history = transactionHistory.computeIfAbsent(
            player.getUuid(), 
            k -> new ArrayList<>()
        );
        
        history.add(transaction);
        
        // Trigger callbacks
        for (BiConsumer<Player, Transaction> callback : transactionCallbacks) {
            try {
                callback.accept(player, transaction);
            } catch (Exception e) {
                System.err.println("[EconomyHelper] Error in transaction callback: " + e.getMessage());
            }
        }
    }
    
    /**
     * Get transaction history for a player
     */
    public static List<Transaction> getTransactionHistory(Player player) {
        return transactionHistory.getOrDefault(player.getUuid(), Collections.emptyList());
    }
    
    /**
     * Get recent transactions for a player
     */
    public static List<Transaction> getRecentTransactions(Player player, int limit) {
        List<Transaction> history = getTransactionHistory(player);
        if (history.size() <= limit) {
            return new ArrayList<>(history);
        }
        return new ArrayList<>(history.subList(history.size() - limit, history.size()));
    }
    
    /**
     * Clear transaction history for a player
     */
    public static void clearTransactionHistory(Player player) {
        transactionHistory.remove(player.getUuid());
    }
    
    // ==================== Callbacks ====================
    
    /**
     * Register a callback for when a transaction occurs
     */
    public static void onTransaction(BiConsumer<Player, Transaction> callback) {
        transactionCallbacks.add(callback);
    }
    
    // ==================== Top Balances ====================
    
    /**
     * Get top balances (default currency)
     */
    public static List<Map.Entry<UUID, Double>> getTopBalances(int limit) {
        return getTopBalances(defaultCurrency, limit);
    }
    
    /**
     * Get top balances for specific currency
     */
    public static List<Map.Entry<UUID, Double>> getTopBalances(String currencyId, int limit) {
        List<Map.Entry<UUID, Double>> topBalances = new ArrayList<>();
        
        for (Map.Entry<UUID, Map<String, Double>> entry : balances.entrySet()) {
            Double balance = entry.getValue().get(currencyId);
            if (balance != null && balance > 0) {
                topBalances.add(new AbstractMap.SimpleEntry<>(entry.getKey(), balance));
            }
        }
        
        // Sort by balance descending
        topBalances.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        
        // Limit results
        if (topBalances.size() > limit) {
            topBalances = topBalances.subList(0, limit);
        }
        
        return topBalances;
    }
    
    // ==================== Utility Methods ====================
    
    /**
     * Format balance with currency symbol (default currency)
     */
    public static String formatBalance(double amount) {
        return formatBalance(defaultCurrency, amount);
    }
    
    /**
     * Format balance with currency symbol
     */
    public static String formatBalance(String currencyId, double amount) {
        Currency currency = currencies.get(currencyId);
        if (currency != null) {
            return currency.format(amount);
        }
        return String.format("%.2f %s", amount, currencyId);
    }
    
    /**
     * Get formatted balance for player (default currency)
     */
    public static String getFormattedBalance(Player player) {
        return formatBalance(getBalance(player));
    }
    
    /**
     * Get formatted balance for player with specific currency
     */
    public static String getFormattedBalance(Player player, String currencyId) {
        return formatBalance(currencyId, getBalance(player, currencyId));
    }
    
    /**
     * Reset all balances for a player
     */
    public static void resetPlayer(Player player) {
        balances.remove(player.getUuid());
        transactionHistory.remove(player.getUuid());
    }
    
    /**
     * Get total currency in circulation (default currency)
     */
    public static double getTotalInCirculation() {
        return getTotalInCirculation(defaultCurrency);
    }
    
    /**
     * Get total currency in circulation for specific currency
     */
    public static double getTotalInCirculation(String currencyId) {
        double total = 0.0;
        for (Map<String, Double> playerBalances : balances.values()) {
            Double balance = playerBalances.get(currencyId);
            if (balance != null) {
                total += balance;
            }
        }
        return total;
    }
    
    /**
     * Get number of players with positive balance (default currency)
     */
    public static int getPlayerCount() {
        return getPlayerCount(defaultCurrency);
    }
    
    /**
     * Get number of players with positive balance for specific currency
     */
    public static int getPlayerCount(String currencyId) {
        int count = 0;
        for (Map<String, Double> playerBalances : balances.values()) {
            Double balance = playerBalances.get(currencyId);
            if (balance != null && balance > 0) {
                count++;
            }
        }
        return count;
    }
    
    // ==================== Initialization ====================
    
    static {
        // Register default currency
        registerCurrency(new Currency(
            "coins",
            "Coin",
            "$",
            "Coins",
            0.0,
            false
        ));
    }
    
    // ==================== PERSISTENCE ====================
    
    /**
     * Data structure for saving economy data
     */
    private static class EconomySaveData {
        Map<String, Map<String, Double>> playerBalances = new HashMap<>();
        Map<String, List<Transaction>> playerTransactions = new HashMap<>();
    }
    
    /**
     * Save all economy data to disk.
     * 
     * @return true if save was successful
     */
    public static boolean saveEconomyData() {
        EconomySaveData saveData = new EconomySaveData();
        
        // Convert UUID keys to strings for JSON serialization
        for (Map.Entry<UUID, Map<String, Double>> entry : balances.entrySet()) {
            saveData.playerBalances.put(entry.getKey().toString(), entry.getValue());
        }
        
        for (Map.Entry<UUID, List<Transaction>> entry : transactionHistory.entrySet()) {
            saveData.playerTransactions.put(entry.getKey().toString(), entry.getValue());
        }
        
        return DataHelper.saveJson("economy/data.json", saveData);
    }
    
    /**
     * Save economy data asynchronously.
     */
    public static void saveEconomyDataAsync() {
        DataHelper.saveJsonAsync("economy/data.json", createSaveData())
            .thenAccept(success -> {
                if (success) {
                    // Success logged by DataHelper
                } else {
                    // Error logged by DataHelper
                }
            });
    }
    
    /**
     * Load all economy data from disk.
     * 
     * @return true if load was successful
     */
    public static boolean loadEconomyData() {
        EconomySaveData saveData = DataHelper.loadJson("economy/data.json", EconomySaveData.class);
        
        if (saveData == null) {
            return false;
        }
        
        // Clear existing data
        balances.clear();
        transactionHistory.clear();
        
        // Convert string keys back to UUIDs
        if (saveData.playerBalances != null) {
            for (Map.Entry<String, Map<String, Double>> entry : saveData.playerBalances.entrySet()) {
                try {
                    UUID playerUuid = UUID.fromString(entry.getKey());
                    balances.put(playerUuid, new ConcurrentHashMap<>(entry.getValue()));
                } catch (IllegalArgumentException e) {
                    // Invalid UUID, skip this entry
                }
            }
        }
        
        if (saveData.playerTransactions != null) {
            for (Map.Entry<String, List<Transaction>> entry : saveData.playerTransactions.entrySet()) {
                try {
                    UUID playerUuid = UUID.fromString(entry.getKey());
                    transactionHistory.put(playerUuid, new ArrayList<>(entry.getValue()));
                } catch (IllegalArgumentException e) {
                    // Invalid UUID, skip this entry
                }
            }
        }
        
        return true;
    }
    
    /**
     * Save economy data for a specific player.
     * 
     * @param player The player
     * @return true if save was successful
     */
    public static boolean savePlayerEconomyData(Player player) {
        Map<String, Object> playerData = new HashMap<>();
        
        Map<String, Double> playerBalances = balances.get(player.getUuid());
        if (playerBalances != null) {
            playerData.put("balances", playerBalances);
        }
        
        List<Transaction> transactions = transactionHistory.get(player.getUuid());
        if (transactions != null) {
            playerData.put("transactions", transactions);
        }
        
        if (playerData.isEmpty()) {
            return true; // Nothing to save
        }
        
        String filename = "economy/players/" + player.getUuid().toString() + ".json";
        return DataHelper.saveJson(filename, playerData);
    }
    
    /**
     * Load economy data for a specific player.
     * 
     * @param player The player
     * @return true if load was successful
     */
    @SuppressWarnings("unchecked")
    public static boolean loadPlayerEconomyData(Player player) {
        String filename = "economy/players/" + player.getUuid().toString() + ".json";
        
        Map<String, Object> playerData = (Map<String, Object>) 
            DataHelper.loadJson(filename, Map.class);
        
        if (playerData == null) {
            return false;
        }
        
        // Load balances
        if (playerData.containsKey("balances")) {
            Map<String, Double> playerBalances = (Map<String, Double>) playerData.get("balances");
            if (playerBalances != null) {
                balances.put(player.getUuid(), new ConcurrentHashMap<>(playerBalances));
            }
        }
        
        // Load transactions
        if (playerData.containsKey("transactions")) {
            List<Transaction> transactions = (List<Transaction>) playerData.get("transactions");
            if (transactions != null) {
                transactionHistory.put(player.getUuid(), new ArrayList<>(transactions));
            }
        }
        
        return true;
    }
    
    /**
     * Create save data object from current state.
     */
    private static EconomySaveData createSaveData() {
        EconomySaveData saveData = new EconomySaveData();
        
        for (Map.Entry<UUID, Map<String, Double>> entry : balances.entrySet()) {
            saveData.playerBalances.put(entry.getKey().toString(), entry.getValue());
        }
        
        for (Map.Entry<UUID, List<Transaction>> entry : transactionHistory.entrySet()) {
            saveData.playerTransactions.put(entry.getKey().toString(), entry.getValue());
        }
        
        return saveData;
    }
}

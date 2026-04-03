package com.prison.ranks;

import com.prison.database.DatabaseManager;
import com.prison.economy.EconomyAPI;
import com.prison.mines.MinesAPI;
import com.prison.permissions.PermissionEngine;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.Yaml;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import java.io.File;
import java.io.FileInputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * RankPlugin — entry point for the mine rank progression system.
 *
 * Loads rank costs and display data from config.yml, initializes
 * the RankManager singleton, and handles /rankup and /ranks commands.
 */
public class RankPlugin extends JavaPlugin implements Listener {

    private RankManager manager;
    private RanksGUI    gui;
    private final MiniMessage mm = MiniMessage.miniMessage();

    // Track which players have already been notified at their current rank to avoid spam
    // Cleared on rank-up so the notification fires again for the next rank
    private final Set<UUID> rankupReadyNotified = ConcurrentHashMap.newKeySet();

    @Override
    public void onEnable() {
        if (DatabaseManager.getInstance() == null) {
            getLogger().severe("core-database must be loaded before plugin-ranks!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        if (PermissionEngine.getInstance() == null) {
            getLogger().severe("core-permissions must be loaded before plugin-ranks!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        RankConfig config = loadRankConfig();

        manager = RankManager.initialize(config, getLogger());
        RanksAPI.initialize(manager, config);
        gui     = new RanksGUI(manager);

        getServer().getPluginManager().registerEvents(this, this);

        // Check every 5 seconds if any online player can now afford their next rank
        getServer().getScheduler().runTaskTimerAsynchronously(this, () ->
            getServer().getScheduler().runTask(this, this::checkRankupReady),
            100L, 100L
        );

        getLogger().info("Rank system enabled — " + RankConfig.RANK_ORDER.length + " mine ranks loaded.");
    }

    @Override
    public void onDisable() {
        RanksAPI.reset();
        getLogger().info("Rank system disabled.");
    }

    // ----------------------------------------------------------------
    // Rankup-ready notifier
    // ----------------------------------------------------------------

    /**
     * Runs on the main thread every 5 seconds.
     * Sends a one-shot action bar + chat ping when a player first becomes
     * able to afford their next rank. Resets when they actually rank up.
     */
    private void checkRankupReady() {
        EconomyAPI eco = EconomyAPI.getInstance();
        if (eco == null) return;

        for (Player player : getServer().getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            if (rankupReadyNotified.contains(uuid)) continue;

            boolean canRankUp = manager.canRankUp(uuid);
            if (!canRankUp) continue;

            // Has reached the threshold — notify once
            rankupReadyNotified.add(uuid);

            RankConfig config  = manager.getConfig();
            String currentRank = PermissionEngine.getInstance().getMineRank(uuid);
            String nextRank    = config.nextRank(currentRank);
            if (nextRank == null) continue; // already max rank

            RankConfig.RankData next = config.getRank(nextRank);
            String costStr = next != null ? RankManager.formatNumber(next.cost()) : "?";

            player.sendMessage(mm.deserialize(
                "\n<gold><bold>★ RANKUP READY!</bold></gold> " +
                "<green>You can afford " +
                (next != null ? next.display() : nextRank) +
                " <green>($" + costStr + "). " +
                "<yellow>Type <white>/rankup</white> or click in <white>/ranks</white>!</yellow>"));

            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.5f);
        }
    }

    // ----------------------------------------------------------------
    // Commands
    // ----------------------------------------------------------------

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command must be run by a player.");
            return true;
        }

        switch (command.getName().toLowerCase()) {
            case "rankup" -> handleRankup(player, args);
            case "ranks"  -> gui.open(player);
        }
        return true;
    }

    private void handleRankup(Player player, String[] args) {
        // /rankup autoteleport toggle
        if (args.length >= 2
         && args[0].equalsIgnoreCase("autoteleport")
         && args[1].equalsIgnoreCase("toggle")) {
            boolean newState = manager.toggleAutoteleport(player.getUniqueId());
            player.sendMessage(mm.deserialize("<yellow>Auto-teleport on rankup: <white>"
                + (newState ? "<green>ON" : "<red>OFF")));
            return;
        }

        // /rankup max — keep ranking up until out of money or at max rank
        if (args.length >= 1 && args[0].equalsIgnoreCase("max")) {
            handleRankupMax(player);
            return;
        }

        // /rankup
        RankManager.RankupResult result = manager.rankUp(player);
        RankConfig config = manager.getConfig();

        switch (result.type()) {
            case SUCCESS -> {
                String newRank = result.newRank();
                RankConfig.RankData data = config.getRank(newRank);
                String msg = config.getRankupMessage()
                    .replace("{rank}", newRank)
                    .replace("{display}", data != null ? data.display() : newRank);
                player.sendMessage(mm.deserialize(msg));

                // Title screen
                String displayName = data != null ? data.display() : newRank;
                player.showTitle(Title.title(
                    mm.deserialize("<gold><bold>⚡ RANK UP</bold></gold>"),
                    mm.deserialize("<gray>You have risen to <gold>" + displayName),
                    Title.Times.times(
                        Duration.ofMillis(500),
                        Duration.ofMillis(2500),
                        Duration.ofMillis(750)
                    )
                ));

                // Sound + particle burst
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                player.getWorld().spawnParticle(
                    org.bukkit.Particle.TOTEM_OF_UNDYING,
                    player.getLocation().add(0, 1, 0),
                    150, 0.6, 1.2, 0.6, 0.2
                );

                // Reset ready-notification so it fires again for the next rank
                rankupReadyNotified.remove(player.getUniqueId());

                // Milestone message for special ranks
                sendMilestoneMessage(player, newRank);

                // Broadcast for milestone ranks if configured
                String broadcast = config.getRankupBroadcast();
                if (broadcast != null && !broadcast.isBlank() && config.shouldBroadcast(newRank)) {
                    String bMsg = broadcast.replace("{player}", player.getName())
                                           .replace("{rank}", newRank)
                                           .replace("{display}", data != null ? data.display() : newRank);
                    Bukkit.broadcast(mm.deserialize(bMsg));
                }

                // Auto-teleport to new mine spawn
                if (manager.getAutoteleport(player.getUniqueId())) {
                    MinesAPI minesApi = MinesAPI.getInstance();
                    if (minesApi != null) {
                        org.bukkit.Location spawn = minesApi.getSpawnLocation(newRank);
                        if (spawn != null) {
                            player.teleport(spawn);
                        }
                    }
                }
            }
            case CANNOT_AFFORD -> {
                String msg = config.getCannotAffordMessage()
                    .replace("{cost}", RankManager.formatNumber(result.cost()))
                    .replace("{balance}", RankManager.formatNumber(result.balance()));
                player.sendMessage(mm.deserialize(msg));
            }
            case MAX_RANK -> player.sendMessage(mm.deserialize(config.getMaxRankMessage()));
            case ERROR    -> player.sendMessage(mm.deserialize("<red>An error occurred. Please try again."));
        }
    }

    private void handleRankupMax(Player player) {
        RankConfig config = manager.getConfig();
        int ranksGained = 0;
        String finalRank = null;

        while (true) {
            RankManager.RankupResult result = manager.rankUp(player);
            if (result.type() == RankManager.RankupResult.Type.SUCCESS) {
                ranksGained++;
                finalRank = result.newRank();
                rankupReadyNotified.remove(player.getUniqueId());

                // Broadcast for milestone ranks if configured
                String broadcast = config.getRankupBroadcast();
                if (broadcast != null && !broadcast.isBlank() && config.shouldBroadcast(finalRank)) {
                    RankConfig.RankData data = config.getRank(finalRank);
                    String bMsg = broadcast.replace("{player}", player.getName())
                                           .replace("{rank}", finalRank)
                                           .replace("{display}", data != null ? data.display() : finalRank);
                    Bukkit.broadcast(mm.deserialize(bMsg));
                }
            } else {
                // CANNOT_AFFORD or MAX_RANK — stop looping
                break;
            }
        }

        // Credit quest progress for all ranks gained in one shot.
        // QuestPlugin's onCommandPreprocess skips "/rankup max" so we are the only counter.
        if (ranksGained > 0) {
            try {
                com.prison.quests.QuestsAPI qapi = com.prison.quests.QuestsAPI.getInstance();
                if (qapi != null) qapi.addProgress(player.getUniqueId(), com.prison.quests.QuestType.RANKUPS, ranksGained);
            } catch (NoClassDefFoundError ignored) { /* PrisonQuests not loaded */ }
        }

        if (ranksGained == 0) {
            // Couldn't afford even one — show normal can't-afford message
            String currentRank = com.prison.permissions.PermissionEngine.getInstance().getMineRank(player.getUniqueId());
            String nextRank    = config.nextRank(currentRank);
            if (nextRank == null) {
                player.sendMessage(mm.deserialize(config.getMaxRankMessage()));
            } else {
                RankConfig.RankData nextData = config.getRank(nextRank);
                long cost = nextData != null ? nextData.cost() : 0;
                player.sendMessage(mm.deserialize(config.getCannotAffordMessage()
                    .replace("{cost}", RankManager.formatNumber(cost))
                    .replace("{balance}", "0")));
            }
            return;
        }

        // Show single summary for all ranks gained
        RankConfig.RankData finalData = config.getRank(finalRank);
        String displayName = finalData != null ? finalData.display() : finalRank;

        player.sendMessage(mm.deserialize(
            "<gold>★ <green>Ranked up <white>" + ranksGained + "x</white>! " +
            "<green>Now " + displayName + "<green>."));

        player.showTitle(Title.title(
            mm.deserialize("<gold><bold>RANK UP x" + ranksGained + "!</bold></gold>"),
            mm.deserialize("<yellow>You are now " + displayName),
            Title.Times.times(
                Duration.ofMillis(300),
                Duration.ofMillis(3000),
                Duration.ofMillis(700)
            )
        ));

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f + (ranksGained * 0.02f));
        player.getWorld().spawnParticle(
            org.bukkit.Particle.TOTEM_OF_UNDYING,
            player.getLocation().add(0, 1, 0),
            Math.min(300, 50 + ranksGained * 20), 0.8, 1.5, 0.8, 0.25
        );

        // Auto-teleport to new mine if enabled
        if (manager.getAutoteleport(player.getUniqueId())) {
            MinesAPI minesApi = MinesAPI.getInstance();
            if (minesApi != null) {
                org.bukkit.Location spawn = minesApi.getSpawnLocation(finalRank);
                if (spawn != null) player.teleport(spawn);
            }
        }

        // Milestone message for the highest rank reached in the max chain
        sendMilestoneMessage(player, finalRank);
    }

    // ----------------------------------------------------------------
    // Milestone messages — fire for landmark ranks E, J, N, Z
    // ----------------------------------------------------------------

    private void sendMilestoneMessage(Player player, String rank) {
        String msg = switch (rank.toUpperCase()) {
            case "E" -> "<gold>✦ Rank E reached. The Pharaoh's bazaars are open to you.";
            case "J" -> "<gold>✦ Rank J reached. The Khopesh's higher secrets are within reach.";
            case "N" -> "<gold>✦ Rank N reached. You may now speak with the gods... for a price.";
            case "Z" -> "<gold>⚡ YOU HAVE REACHED PHARAOH RANK. The highest mortal honor.";
            default  -> null;
        };

        if (msg == null) return;

        player.sendMessage(mm.deserialize(msg));

        if ("Z".equalsIgnoreCase(rank)) {
            // Extra dramatic title for Pharaoh rank
            player.showTitle(Title.title(
                mm.deserialize("<gold><bold>⚡ PHARAOH</bold></gold>"),
                mm.deserialize("<yellow>The highest mortal honor is yours."),
                Title.Times.times(
                    Duration.ofMillis(500),
                    Duration.ofMillis(4000),
                    Duration.ofMillis(1000)
                )
            ));
        }
    }

    // ----------------------------------------------------------------
    // GUI click — block all clicks; route affordable next-rank click to rankup
    // ----------------------------------------------------------------

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().title().equals(mm.deserialize("<gold><bold>Mine Rank Progression"))) return;

        event.setCancelled(true);
        int rawSlot = event.getRawSlot();

        // Close button
        if (rawSlot == 45) {
            player.closeInventory();
            return;
        }

        // Check if the clicked slot is the next rank's slot and the player can afford it
        RankConfig config = manager.getConfig();
        String currentRank  = PermissionEngine.getInstance().getMineRank(player.getUniqueId());
        int currentIndex    = config.rankIndex(currentRank);
        int nextIndex       = currentIndex + 1;

        if (nextIndex < RankConfig.RANK_ORDER.length && nextIndex < RanksGUI.RANK_SLOTS.length) {
            if (rawSlot == RanksGUI.RANK_SLOTS[nextIndex] && manager.canRankUp(player.getUniqueId())) {
                player.closeInventory();
                handleRankup(player, new String[0]);
            }
        }
    }

    // ----------------------------------------------------------------
    // Config loading — uses SnakeYAML directly (same pattern as core-permissions)
    // ----------------------------------------------------------------

    /**
     * Admin toolkit: update a rank's data, save to config, and hot-reload.
     * Changes apply immediately — no restart required.
     */
    public void adminUpdateRank(String rank, long cost, String display, String prefix) {
        String upper = rank.toUpperCase();
        getConfig().set("ranks." + upper + ".cost", cost);
        getConfig().set("ranks." + upper + ".display", display);
        getConfig().set("ranks." + upper + ".prefix", prefix);
        saveConfig();
        // Reload from the saved file and update the manager
        manager.reloadConfig(loadRankConfig());
        getLogger().info("[Ranks] Admin updated rank " + upper + ": cost=" + cost);
    }

    @SuppressWarnings("unchecked")
    RankConfig loadRankConfig() {
        File configFile = new File(getDataFolder(), "config.yml");
        try (FileInputStream fis = new FileInputStream(configFile)) {
            Map<String, Object> root = new Yaml().load(fis);

            boolean autoTp     = Boolean.TRUE.equals(root.get("autoteleport-default"));
            String rankupMsg   = (String) root.getOrDefault("rankup-message",   "<green>Ranked up to {display}!");
            String broadcast   = (String) root.getOrDefault("rankup-broadcast", "");
            String cantAfford  = (String) root.getOrDefault("cannot-afford-message", "<red>Not enough $.");
            String maxRank     = (String) root.getOrDefault("max-rank-message",  "<gold>You are rank Z!");

            Set<String> broadcastRanks = new HashSet<>();
            Object bRanksObj = root.get("rankup-broadcast-ranks");
            if (bRanksObj instanceof List<?> bRanksList) {
                for (Object item : bRanksList) {
                    if (item != null) broadcastRanks.add(item.toString().toUpperCase());
                }
            }

            Map<String, Object> ranksSection = (Map<String, Object>) root.get("ranks");
            Map<String, RankConfig.RankData> rankMap = new LinkedHashMap<>();

            for (String letter : RankConfig.RANK_ORDER) {
                Object entry = ranksSection.get(letter);
                if (!(entry instanceof Map)) continue;
                Map<String, Object> rankData = (Map<String, Object>) entry;

                long   cost    = ((Number) rankData.getOrDefault("cost",    0)).longValue();
                String display = (String)  rankData.getOrDefault("display", "Rank " + letter);
                String prefix  = (String)  rankData.getOrDefault("prefix",  "[" + letter + "]");

                rankMap.put(letter, new RankConfig.RankData(letter, cost, display, prefix));
            }

            getLogger().info("[Ranks] Loaded " + rankMap.size() + " rank definitions from config.");
            return new RankConfig(rankMap, autoTp, rankupMsg, broadcast, broadcastRanks, cantAfford, maxRank);

        } catch (Exception e) {
            getLogger().severe("[Ranks] Failed to load config: " + e.getMessage() + " — using defaults.");
            return buildDefaultConfig();
        }
    }

    private RankConfig buildDefaultConfig() {
        Map<String, RankConfig.RankData> rankMap = new LinkedHashMap<>();
        long[] costs = {
            0, 5000, 15000, 35000, 75000, 150000, 275000, 450000, 700000, 1000000,
            1500000, 2250000, 3500000, 5000000, 7500000, 11000000, 16000000, 23000000,
            33000000, 48000000, 70000000, 100000000, 150000000, 225000000, 350000000, 500000000
        };
        for (int i = 0; i < RankConfig.RANK_ORDER.length; i++) {
            String letter = RankConfig.RANK_ORDER[i];
            rankMap.put(letter, new RankConfig.RankData(letter, costs[i], "Rank " + letter, "[" + letter + "]"));
        }
        return new RankConfig(rankMap, true,
            "<green>You ranked up to <gold>{display}</gold>!",
            "", new HashSet<>(), "<red>You need <gold>${cost}</gold>. You have <gold>${balance}</gold>.",
            "<gold>You are rank Z — the highest mine rank!");
    }
}

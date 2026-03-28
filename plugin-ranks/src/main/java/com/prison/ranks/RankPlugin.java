package com.prison.ranks;

import com.prison.database.DatabaseManager;
import com.prison.mines.MinesAPI;
import com.prison.permissions.PermissionEngine;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.Yaml;

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
        gui     = new RanksGUI(manager);

        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Rank system enabled — " + RankConfig.RANK_ORDER.length + " mine ranks loaded.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Rank system disabled.");
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

                // Broadcast if configured
                String broadcast = config.getRankupBroadcast();
                if (broadcast != null && !broadcast.isBlank()) {
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

    // ----------------------------------------------------------------
    // GUI click — block all clicks in the ranks inventory (read-only)
    // ----------------------------------------------------------------

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().title().equals(mm.deserialize("<gold><bold>Mine Rank Progression"))) {
            event.setCancelled(true);
        }
    }

    // ----------------------------------------------------------------
    // Config loading — uses SnakeYAML directly (same pattern as core-permissions)
    // ----------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private RankConfig loadRankConfig() {
        File configFile = new File(getDataFolder(), "config.yml");
        try (FileInputStream fis = new FileInputStream(configFile)) {
            Map<String, Object> root = new Yaml().load(fis);

            boolean autoTp     = Boolean.TRUE.equals(root.get("autoteleport-default"));
            String rankupMsg   = (String) root.getOrDefault("rankup-message",   "<green>Ranked up to {display}!");
            String broadcast   = (String) root.getOrDefault("rankup-broadcast", "");
            String cantAfford  = (String) root.getOrDefault("cannot-afford-message", "<red>Not enough IGC.");
            String maxRank     = (String) root.getOrDefault("max-rank-message",  "<gold>You are rank Z!");

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
            return new RankConfig(rankMap, autoTp, rankupMsg, broadcast, cantAfford, maxRank);

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
            "", "<red>You need <gold>{cost} IGC</gold>. You have <gold>{balance} IGC</gold>.",
            "<gold>You are rank Z — the highest mine rank!");
    }
}

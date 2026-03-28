package com.prison.prestige;

import com.prison.database.DatabaseManager;
import com.prison.permissions.PermissionEngine;
import net.kyori.adventure.text.minimessage.MiniMessage;
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
import java.util.*;

public class PrestigePlugin extends JavaPlugin implements Listener {

    private PrestigeManager manager;
    private PrestigeGUI     gui;
    private final MiniMessage mm = MiniMessage.miniMessage();

    @Override
    public void onEnable() {
        if (DatabaseManager.getInstance() == null) {
            getLogger().severe("core-database must be loaded before plugin-prestige!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        if (PermissionEngine.getInstance() == null) {
            getLogger().severe("core-permissions must be loaded before plugin-prestige!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        PrestigeConfig config = loadPrestigeConfig();

        manager = PrestigeManager.initialize(config, getLogger());
        gui     = new PrestigeGUI(manager);

        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Prestige system enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Prestige system disabled.");
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
            case "prestige"     -> handlePrestige(player);
            case "prestigeinfo" -> handlePrestigeInfo(player, args);
        }
        return true;
    }

    private void handlePrestige(Player player) {
        if (!manager.canPrestige(player.getUniqueId())) {
            player.sendMessage(mm.deserialize("<red>You must be <white>Rank Z</white> to prestige."));
            return;
        }
        gui.openConfirm(player);
    }

    private void handlePrestigeInfo(Player player, String[] args) {
        int level = manager.getPrestigeLevel(player.getUniqueId());
        PrestigeConfig cfg = manager.getConfig();

        player.sendMessage(mm.deserialize("<dark_purple>═══ Prestige Info ═══"));
        player.sendMessage(mm.deserialize("<gray>Your prestige: <white>" + (level == 0 ? "None" : "P" + level)));

        double bonus = cfg.getTotalTokenBonus(level) * 100;
        player.sendMessage(mm.deserialize("<gray>Token earn bonus: <white>+" + String.format("%.1f", bonus) + "%"));
        player.sendMessage(mm.deserialize("<gray>Bonus per prestige: <white>+"
            + String.format("%.1f", cfg.getTokenMultiplierPerPrestige() * 100) + "% each"));

        if (level == 0) {
            player.sendMessage(mm.deserialize(""));
            player.sendMessage(mm.deserialize("<yellow>Reach <white>Rank Z</white> to unlock prestige!"));
        }
    }

    // ----------------------------------------------------------------
    // GUI click handler
    // ----------------------------------------------------------------

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().title().equals(mm.deserialize(PrestigeGUI.TITLE_STRING))) return;

        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == PrestigeGUI.SLOT_CONFIRM) {
            player.closeInventory();

            // Re-check eligibility (could have changed since GUI opened)
            if (!manager.canPrestige(player.getUniqueId())) {
                player.sendMessage(mm.deserialize("<red>You are no longer eligible to prestige."));
                return;
            }

            int newLevel = manager.executePrestige(player);
            if (newLevel > 0) {
                player.sendMessage(mm.deserialize("<dark_purple>You are now <light_purple><bold>Prestige " + newLevel + "</bold></light_purple><dark_purple>!"));
                player.sendMessage(mm.deserialize("<gray>Your mine rank has been reset to <white>A</white> and IGC wiped."));
                player.sendMessage(mm.deserialize("<gray>Token earn bonus: <white>+" + String.format("%.1f", manager.getConfig().getTotalTokenBonus(newLevel) * 100) + "%"));
            } else {
                player.sendMessage(mm.deserialize("<red>Prestige failed — please contact an admin."));
            }

        } else if (slot == PrestigeGUI.SLOT_CANCEL || slot == PrestigeGUI.SLOT_CANCEL2) {
            player.closeInventory();
            player.sendMessage(mm.deserialize("<gray>Prestige cancelled."));
        }
    }

    // ----------------------------------------------------------------
    // Config loading
    // ----------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private PrestigeConfig loadPrestigeConfig() {
        File configFile = new File(getDataFolder(), "config.yml");
        try (FileInputStream fis = new FileInputStream(configFile)) {
            Map<String, Object> root = new Yaml().load(fis);

            double multiplier = ((Number) root.getOrDefault("token-multiplier-per-prestige", 0.02)).doubleValue();
            String broadcast  = (String) root.getOrDefault("broadcast-message",
                "<gold><bold>{player}</bold> has prestiged to <bold>[P{prestige}]</bold>!");
            String prefix     = (String) root.getOrDefault("prefix-format",
                "<dark_purple>[<light_purple>P{level}</light_purple>]</dark_purple>");
            int maxPerms      = ((Number) root.getOrDefault("max-prestige-perms", 50)).intValue();

            Map<Integer, List<String>> rewards = new HashMap<>();
            Object rewardsObj = root.get("rewards");
            if (rewardsObj instanceof Map<?, ?> rewardsMap) {
                for (Map.Entry<?, ?> entry : rewardsMap.entrySet()) {
                    int tier = ((Number) entry.getKey()).intValue();
                    if (entry.getValue() instanceof Map<?, ?> tierData) {
                        Object cmds = ((Map<?, ?>) tierData).get("commands");
                        if (cmds instanceof List<?> cmdList) {
                            rewards.put(tier, cmdList.stream().map(Object::toString).toList());
                        }
                    }
                }
            }

            getLogger().info("[Prestige] Config loaded — " + rewards.size() + " tier rewards defined.");
            return new PrestigeConfig(multiplier, broadcast, prefix, rewards, maxPerms);

        } catch (Exception e) {
            getLogger().severe("[Prestige] Failed to load config: " + e.getMessage() + " — using defaults.");
            return new PrestigeConfig(0.02,
                "<gold><bold>{player}</bold> prestiged to P{prestige}!",
                "<dark_purple>[<light_purple>P{level}</light_purple>]</dark_purple>",
                Map.of(), 50);
        }
    }
}

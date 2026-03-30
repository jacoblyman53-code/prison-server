package com.prison.prestige;

import com.prison.database.DatabaseManager;
import com.prison.permissions.PermissionEngine;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.time.Duration;
import java.util.*;

public class PrestigePlugin extends JavaPlugin implements Listener {

    private PrestigeManager     manager;
    private PrestigeGUI         gui;
    private PrestigeShopManager shopManager;
    private PrestigeShopGUI     shopGui;
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

        int pointsPerPrestige = getConfig().getInt("prestige-shop-points-per-prestige", 10);
        shopManager = PrestigeShopManager.initialize(this, pointsPerPrestige);
        shopGui     = new PrestigeShopGUI(shopManager);

        // Register prestige sell/token bonus providers with EconomyAPI (soft-depend)
        try {
            com.prison.economy.EconomyAPI eco = com.prison.economy.EconomyAPI.getInstance();
            if (eco != null) {
                eco.setPrestigeSellBonusProvider(uuid -> shopManager.getSellBonus(uuid));
                eco.setPrestigeTokenBonusProvider(uuid -> shopManager.getTokenBonus(uuid));
                getLogger().info("[PrestigeShop] Registered sell + token bonus providers with EconomyAPI.");
            }
        } catch (NoClassDefFoundError ignored) {
            getLogger().warning("[PrestigeShop] PrisonEconomy not found — prestige bonuses disabled.");
        }

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
            case "prestige"      -> handlePrestige(player);
            case "prestigeinfo"  -> handlePrestigeInfo(player, args);
            case "prestigeshop"  -> shopGui.open(player);
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
    // Player join/quit — load/unload prestige shop data
    // ----------------------------------------------------------------

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        getServer().getScheduler().runTaskAsynchronously(this,
            () -> shopManager.loadPlayer(event.getPlayer().getUniqueId()));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        shopManager.unloadPlayer(event.getPlayer().getUniqueId());
    }

    // ----------------------------------------------------------------
    // GUI click handler
    // ----------------------------------------------------------------

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Prestige Shop GUI
        if (event.getView().title().equals(mm.deserialize(PrestigeShopGUI.TITLE_STRING))) {
            event.setCancelled(true);
            if (event.getClickedInventory() != event.getView().getTopInventory()) return;
            int slot = event.getRawSlot();
            if (shopGui.isCloseSlot(slot)) {
                player.closeInventory();
                return;
            }
            String upgradeId = shopGui.getUpgradeIdAtSlot(slot);
            if (upgradeId == null) return;

            PrestigeShopManager.PurchaseResult result = shopManager.purchase(player.getUniqueId(), upgradeId);
            PrestigeShopManager.UpgradeDef def = shopManager.getUpgrade(upgradeId);
            switch (result) {
                case OK -> {
                    player.sendMessage(mm.deserialize(
                        "<green>Purchased <light_purple>" + (def != null ? def.display() : upgradeId) +
                        "<green>! Points remaining: <white>" + shopManager.getPoints(player.getUniqueId())));
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
                    shopGui.open(player); // refresh GUI
                }
                case ALREADY_OWNED ->
                    player.sendMessage(mm.deserialize("<yellow>You already own that upgrade."));
                case INSUFFICIENT_POINTS ->
                    player.sendMessage(mm.deserialize(
                        "<red>Not enough prestige points. You have <white>" +
                        shopManager.getPoints(player.getUniqueId()) + " <red>pts, need <white>" +
                        (def != null ? def.cost() : "?") + "<red>."));
                case PREREQUISITE_NOT_MET ->
                    player.sendMessage(mm.deserialize("<red>You must purchase the previous tier first."));
                case NOT_FOUND ->
                    player.sendMessage(mm.deserialize("<red>Unknown upgrade."));
            }
            return;
        }

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

                // Title screen
                player.showTitle(Title.title(
                    mm.deserialize("<gradient:#FFD700:#FF6600><bold>PRESTIGE P" + newLevel + "</bold></gradient>"),
                    mm.deserialize("<gray>The journey continues..."),
                    Title.Times.times(
                        Duration.ofMillis(500),
                        Duration.ofMillis(3500),
                        Duration.ofMillis(1000)
                    )
                ));

                // Sound + dramatic particle burst (bigger than rankup)
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1.0f, 0.8f);
                player.getWorld().spawnParticle(
                    org.bukkit.Particle.TOTEM_OF_UNDYING,
                    player.getLocation().add(0, 1, 0),
                    300, 1.0, 2.0, 1.0, 0.3
                );
                player.getWorld().spawnParticle(
                    org.bukkit.Particle.END_ROD,
                    player.getLocation().add(0, 1, 0),
                    80, 1.2, 1.5, 1.2, 0.1
                );
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

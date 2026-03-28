package com.prison.economy;

import com.prison.database.DatabaseManager;
import com.prison.permissions.PermissionEngine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class EconomyPlugin extends JavaPlugin implements Listener {

    private EconomyAPI api;
    private final MiniMessage mm = MiniMessage.miniMessage();

    private String currencySymbol;
    private String tokenSymbol;
    private long minSellIntervalMs;
    private final EnumMap<Material, Long> configSellPrices = new EnumMap<>(Material.class);

    @Override
    public void onEnable() {
        if (DatabaseManager.getInstance() == null) {
            getLogger().severe("PrisonDatabase must be loaded before PrisonEconomy!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        if (PermissionEngine.getInstance() == null) {
            getLogger().severe("PrisonPermissions must be loaded before PrisonEconomy!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        loadEconomyConfig();

        SellPriceProvider defaultProvider = (material, player) ->
            configSellPrices.getOrDefault(material, 0L);

        api = new EconomyAPI(getLogger(), defaultProvider);

        getServer().getPluginManager().registerEvents(this, this);

        // Baltop refresh task
        int baltopSeconds = getConfigInt("baltop-refresh-seconds", 60);
        getServer().getScheduler().runTaskTimerAsynchronously(this,
            () -> api.refreshBaltop(), 20L, baltopSeconds * 20L);

        // Auto-save task
        int autoSaveSeconds = getConfigInt("auto-save-seconds", 300);
        getServer().getScheduler().runTaskTimerAsynchronously(this,
            () -> api.saveAllWallets(), autoSaveSeconds * 20L, autoSaveSeconds * 20L);

        getLogger().info("Economy system enabled.");
    }

    @Override
    public void onDisable() {
        // Flush all wallets to DB before shutdown
        if (api != null) {
            api.saveAllWallets();
        }
        getLogger().info("Economy system disabled — wallets saved.");
    }

    // ----------------------------------------------------------------
    // Events
    // ----------------------------------------------------------------

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Load wallet 1 tick after join to ensure the player row exists in DB
        getServer().getScheduler().runTaskLaterAsynchronously(this,
            () -> api.loadPlayer(event.getPlayer().getUniqueId()), 1L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        api.saveAndUnload(event.getPlayer().getUniqueId());
    }

    /**
     * Auto-sell: when a donor player with auto-sell enabled breaks a block,
     * intercept the drops, sell any sellable items, and remove them so they
     * don't land in the inventory.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockDrop(BlockDropItemEvent event) {
        Player player = event.getPlayer();
        if (!api.hasAutoSell(player.getUniqueId())) return;

        long totalSell = 0;
        var items = event.getItems();
        var iter = items.iterator();

        while (iter.hasNext()) {
            ItemStack stack = iter.next().getItemStack();
            long price = api.getSellPrice(stack.getType(), player);
            if (price > 0) {
                totalSell += price * stack.getAmount();
                iter.remove();
            }
        }

        if (totalSell > 0) {
            long earned = totalSell;
            api.addBalance(player.getUniqueId(), earned, TransactionType.MINE_SELL);
            player.sendActionBar(mm.deserialize(
                "<green>+" + currencySymbol + format(earned) + " <gray>(auto-sell)"));
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
            case "balance" -> showBalance(player);
            case "tokens"  -> showTokens(player);
            case "pay"     -> handlePay(player, args);
            case "baltop"  -> showBaltop(player);
            case "sell"    -> handleSell(player);
            case "sellall" -> handleSellAll(player);
            case "autosell"-> handleAutoSell(player);
            case "tokenlog"-> handleTokenLog(player);
            default -> { return false; }
        }
        return true;
    }

    // ----------------------------------------------------------------
    // Command Handlers
    // ----------------------------------------------------------------

    private void showBalance(Player player) {
        long bal = api.getBalance(player.getUniqueId());
        player.sendMessage(mm.deserialize(
            "<gold>Balance: <white>" + currencySymbol + formatFull(bal)));
    }

    private void showTokens(Player player) {
        long toks = api.getTokens(player.getUniqueId());
        player.sendMessage(mm.deserialize(
            "<aqua>Tokens: <white>" + tokenSymbol + formatFull(toks)));
    }

    private void handlePay(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(mm.deserialize("<red>Usage: /pay <player> <amount>"));
            return;
        }

        Player target = getServer().getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(mm.deserialize("<red>Player not found or not online."));
            return;
        }
        if (target.equals(player)) {
            player.sendMessage(mm.deserialize("<red>You cannot pay yourself."));
            return;
        }

        long amount;
        try {
            amount = Long.parseLong(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(mm.deserialize("<red>Invalid amount."));
            return;
        }
        if (amount <= 0) {
            player.sendMessage(mm.deserialize("<red>Amount must be greater than zero."));
            return;
        }

        long newBal = api.deductBalance(player.getUniqueId(), amount, TransactionType.PAY_SENT);
        if (newBal < 0) {
            long bal = api.getBalance(player.getUniqueId());
            player.sendMessage(mm.deserialize(
                "<red>Insufficient funds. You have " + currencySymbol + formatFull(bal) + "."));
            return;
        }

        api.addBalance(target.getUniqueId(), amount, TransactionType.PAY_RECEIVED);

        player.sendMessage(mm.deserialize(
            "<green>Paid <white>" + currencySymbol + formatFull(amount) +
            "<green> to <white>" + target.getName() + "<green>. Balance: <white>" +
            currencySymbol + formatFull(newBal)));
        target.sendMessage(mm.deserialize(
            "<green>You received <white>" + currencySymbol + formatFull(amount) +
            "<green> from <white>" + player.getName() + "<green>."));
    }

    private void showBaltop(Player player) {
        List<EconomyAPI.BaltopEntry> top = api.getBaltop();
        player.sendMessage(mm.deserialize("<gold><bold>═══ Richest Players ═══"));
        if (top.isEmpty()) {
            player.sendMessage(mm.deserialize("<gray>No data yet — leaderboard refreshes every minute."));
            return;
        }
        for (int i = 0; i < top.size(); i++) {
            EconomyAPI.BaltopEntry entry = top.get(i);
            String rank = (i == 0) ? "<gold>" : (i == 1) ? "<gray>" : (i == 2) ? "<#cd7f32>" : "<white>";
            player.sendMessage(mm.deserialize(
                rank + (i + 1) + ". <white>" + entry.name() +
                " <gray>— <white>" + currencySymbol + formatFull(entry.balance())));
        }
    }

    private void handleSell(Player player) {
        if (!api.canSell(player.getUniqueId(), minSellIntervalMs)) {
            player.sendMessage(mm.deserialize("<red>You are selling too fast. Slow down."));
            return;
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType() == Material.AIR) {
            player.sendMessage(mm.deserialize("<red>You are not holding anything to sell."));
            return;
        }

        long price = api.getSellPrice(held.getType(), player);
        if (price <= 0) {
            player.sendMessage(mm.deserialize("<red>That item has no sell value."));
            return;
        }

        long total = price * held.getAmount();
        player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        api.addBalance(player.getUniqueId(), total, TransactionType.MINE_SELL);
        api.recordSell(player.getUniqueId());

        player.sendMessage(mm.deserialize(
            "<green>Sold <white>" + held.getAmount() + "x " + formatMaterial(held.getType()) +
            "<green> for <white>" + currencySymbol + formatFull(total) + "<green>."));
    }

    private void handleSellAll(Player player) {
        if (!api.canSell(player.getUniqueId(), minSellIntervalMs)) {
            player.sendMessage(mm.deserialize("<red>You are selling too fast. Slow down."));
            return;
        }

        long totalEarned = 0;
        int itemsSold    = 0;

        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() == Material.AIR) continue;
            long price = api.getSellPrice(item.getType(), player);
            if (price <= 0) continue;
            totalEarned += price * item.getAmount();
            itemsSold   += item.getAmount();
            contents[i]  = null;
        }
        player.getInventory().setContents(contents);

        if (itemsSold == 0) {
            player.sendMessage(mm.deserialize("<gray>You have no sellable items."));
            return;
        }

        api.addBalance(player.getUniqueId(), totalEarned, TransactionType.MINE_SELL);
        api.recordSell(player.getUniqueId());

        player.sendMessage(mm.deserialize(
            "<green>Sold <white>" + itemsSold + " items<green> for <white>" +
            currencySymbol + formatFull(totalEarned) + "<green>."));
    }

    private void handleAutoSell(Player player) {
        // Donor perk — require donor permission
        if (!PermissionEngine.getInstance().hasPermission(player, "prison.donor.autosell")
                && !PermissionEngine.getInstance().hasPermission(player, "prison.admin.*")) {
            player.sendMessage(mm.deserialize(
                "<red>Auto-sell is a donor perk. Visit the store to unlock it!"));
            return;
        }

        boolean current = api.hasAutoSell(player.getUniqueId());
        api.setAutoSell(player.getUniqueId(), !current);

        if (!current) {
            player.sendMessage(mm.deserialize("<green>Auto-sell <bold>enabled</bold>. " +
                "<gray>Blocks will be sold automatically on break."));
        } else {
            player.sendMessage(mm.deserialize("<yellow>Auto-sell <bold>disabled</bold>."));
        }
    }

    private void handleTokenLog(Player player) {
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            try {
                List<EconomyAPI.TransactionRecord> records =
                    api.getRecentTransactions(player.getUniqueId(), "TOKEN", 10);

                getServer().getScheduler().runTask(this, () -> {
                    player.sendMessage(mm.deserialize("<aqua><bold>═══ Token Log ═══"));
                    if (records.isEmpty()) {
                        player.sendMessage(mm.deserialize("<gray>No token transactions found."));
                        return;
                    }
                    for (EconomyAPI.TransactionRecord r : records) {
                        String color = r.amount() >= 0 ? "<green>+" : "<red>";
                        String typeDisplay = r.type().replace("_", " ");
                        player.sendMessage(mm.deserialize(
                            color + tokenSymbol + formatFull(Math.abs(r.amount())) +
                            " <gray>(" + typeDisplay + ")" +
                            " <dark_gray>→ <gray>" + tokenSymbol + formatFull(r.balanceAfter())));
                    }
                });
            } catch (Exception e) {
                getLogger().warning("[Economy] tokenlog query failed: " + e.getMessage());
                player.sendMessage(mm.deserialize("<red>Could not load transaction log."));
            }
        });
    }

    // ----------------------------------------------------------------
    // Config Loading
    // ----------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private void loadEconomyConfig() {
        File configFile = new File(getDataFolder(), "config.yml");
        try (FileInputStream fis = new FileInputStream(configFile)) {
            Map<String, Object> root = new Yaml().load(fis);

            currencySymbol    = (String) root.getOrDefault("currency-symbol", "$");
            tokenSymbol       = (String) root.getOrDefault("token-symbol", "T");
            minSellIntervalMs = ((Number) root.getOrDefault("min-sell-interval-ms", 500)).longValue();

            Object pricesObj = root.get("sell-prices");
            if (pricesObj instanceof Map<?, ?> rawPrices) {
                for (Map.Entry<?, ?> entry : rawPrices.entrySet()) {
                    String name = entry.getKey().toString().toUpperCase();
                    long price  = ((Number) entry.getValue()).longValue();
                    try {
                        Material mat = Material.valueOf(name);
                        configSellPrices.put(mat, price);
                    } catch (IllegalArgumentException e) {
                        getLogger().warning("[Economy] Unknown material in sell-prices: " + name);
                    }
                }
            }

            getLogger().info("[Economy] Loaded " + configSellPrices.size() + " sell prices.");

        } catch (Exception e) {
            getLogger().severe("[Economy] Failed to load config: " + e.getMessage() + " — using defaults.");
        }
    }

    private int getConfigInt(String key, int def) {
        return getConfig().getInt(key, def);
    }

    // ----------------------------------------------------------------
    // Formatting
    // ----------------------------------------------------------------

    /** Full number with thousands separators: 1,234,567 */
    private String formatFull(long amount) {
        return String.format("%,d", amount);
    }

    /** Short format for action bar (1.2M, 50K, etc.) */
    private String format(long amount) {
        if (amount >= 1_000_000) return String.format("%.1fM", amount / 1_000_000.0);
        if (amount >= 1_000)    return String.format("%.1fK", amount / 1_000.0);
        return String.valueOf(amount);
    }

    /** "COAL_ORE" → "Coal Ore" */
    private String formatMaterial(Material m) {
        String[] parts = m.name().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0)));
            sb.append(part.substring(1).toLowerCase());
        }
        return sb.toString();
    }
}

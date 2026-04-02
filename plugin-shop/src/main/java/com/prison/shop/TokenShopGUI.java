package com.prison.shop;

import com.prison.economy.BoostManager;
import com.prison.economy.EconomyAPI;
import com.prison.economy.TransactionType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * TokenShopGUI — 27-slot GUI where players spend tokens on time-limited boosts.
 *
 * Layout:
 *   Row 0 (0-8):   border
 *   Row 1 (9-17):  [border x2] [Sell Boost] [border] [Token Storm] [border] [Mega Combo] [border x2]
 *   Row 2 (18-26): [border x4] [XP Flask] [border x2] [points info] [close]
 */
public class TokenShopGUI {

    public static final String TITLE_STRING = "<!italic>TOKEN SHOP";

    // Item slots
    private static final int SLOT_SELL_BOOST   = 11;
    private static final int SLOT_TOKEN_STORM  = 13;
    private static final int SLOT_MEGA_COMBO   = 15;
    private static final int SLOT_HASTE_FLASK  = 20;
    private static final int SLOT_XP_FLASK     = 22;
    private static final int SLOT_SPEED_FLASK  = 24;
    private static final int SLOT_CLOSE        = 26;

    private static final MiniMessage MM = MiniMessage.miniMessage();

    // Cleanup: players with GUI open
    private static final Set<UUID> openSessions = Collections.synchronizedSet(new HashSet<>());

    public static void open(Player player) {
        openSessions.add(player.getUniqueId());
        player.openInventory(buildInventory(player));
    }

    public static boolean isTitle(Component title) {
        return title.equals(MM.deserialize(TITLE_STRING));
    }

    public static void cleanup(UUID uuid) {
        openSessions.remove(uuid);
    }

    // ----------------------------------------------------------------
    // Click handler
    // ----------------------------------------------------------------

    public static void handleClick(Player player, int slot) {
        BoostManager bm  = BoostManager.getInstance();
        EconomyAPI   eco = EconomyAPI.getInstance();
        if (bm == null || eco == null) return;

        UUID uuid = player.getUniqueId();

        switch (slot) {
            case SLOT_SELL_BOOST -> purchase(player, uuid, eco, bm,
                "Sell Boost", 500, BoostManager.BoostType.SELL, 2.0, 5 * 60_000L, null);
            case SLOT_TOKEN_STORM -> purchase(player, uuid, eco, bm,
                "Token Storm", 750, BoostManager.BoostType.TOKEN, 2.0, 5 * 60_000L, null);
            case SLOT_MEGA_COMBO -> purchaseMega(player, uuid, eco, bm);
            case SLOT_HASTE_FLASK -> purchaseEffect(player, uuid, eco, "Haste Flask", 400,
                PotionEffectType.HASTE, 1, 3 * 60);
            case SLOT_XP_FLASK    -> purchaseXP(player, uuid, eco);
            case SLOT_SPEED_FLASK -> purchaseEffect(player, uuid, eco, "Speed Flask", 350,
                PotionEffectType.SPEED, 1, 3 * 60);
            case SLOT_CLOSE       -> player.closeInventory();
        }
    }

    // ----------------------------------------------------------------
    // Purchase logic
    // ----------------------------------------------------------------

    private static void purchase(Player player, UUID uuid, EconomyAPI eco, BoostManager bm,
                                  String name, long cost,
                                  BoostManager.BoostType type, double mult,
                                  long durationMs, BoostManager.BoostType secondType) {
        if (eco.getTokens(uuid) < cost) {
            player.sendMessage(MM.deserialize(
                "<red>Not enough tokens. Need <white>" + cost + " <red>tokens."));
            return;
        }
        eco.deductTokens(uuid, cost, TransactionType.IGC_SHOP_PURCHASE);
        bm.grantBoost(uuid, type, mult, durationMs);
        if (secondType != null) bm.grantBoost(uuid, secondType, mult, durationMs);

        long mins = durationMs / 60_000L;
        player.sendMessage(MM.deserialize(
            "<green>Activated <aqua>" + name + "<green>! <yellow>" +
            formatMult(mult) + " for " + mins + " minutes."));
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.5f);
        // Refresh GUI
        player.openInventory(buildInventory(player));
    }

    private static void purchaseMega(Player player, UUID uuid, EconomyAPI eco, BoostManager bm) {
        long cost = 1200L;
        if (eco.getTokens(uuid) < cost) {
            player.sendMessage(MM.deserialize(
                "<red>Not enough tokens. Need <white>" + cost + " <red>tokens."));
            return;
        }
        eco.deductTokens(uuid, cost, TransactionType.IGC_SHOP_PURCHASE);
        bm.grantBoost(uuid, BoostManager.BoostType.SELL,  2.0, 3 * 60_000L);
        bm.grantBoost(uuid, BoostManager.BoostType.TOKEN, 2.0, 3 * 60_000L);
        player.sendMessage(MM.deserialize(
            "<green>Activated <gold>Mega Combo<green>! <yellow>2x sell + 2x tokens for 3 minutes!"));
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.0f);
        player.openInventory(buildInventory(player));
    }

    private static void purchaseEffect(Player player, UUID uuid, EconomyAPI eco,
                                        String name, long cost,
                                        PotionEffectType effect, int amplifier, int durationSeconds) {
        if (eco.getTokens(uuid) < cost) {
            player.sendMessage(MM.deserialize(
                "<red>Not enough tokens. Need <white>" + cost + " <red>tokens."));
            return;
        }
        eco.deductTokens(uuid, cost, TransactionType.IGC_SHOP_PURCHASE);
        player.addPotionEffect(new PotionEffect(effect, durationSeconds * 20, amplifier, false, true));
        player.sendMessage(MM.deserialize(
            "<green>Activated <aqua>" + name + "<green>! <yellow>" + durationSeconds / 60 + " minutes."));
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.5f);
        player.openInventory(buildInventory(player));
    }

    private static void purchaseXP(Player player, UUID uuid, EconomyAPI eco) {
        long cost = 300L;
        if (eco.getTokens(uuid) < cost) {
            player.sendMessage(MM.deserialize(
                "<red>Not enough tokens. Need <white>" + cost + " <red>tokens."));
            return;
        }
        eco.deductTokens(uuid, cost, TransactionType.IGC_SHOP_PURCHASE);
        player.giveExpLevels(10);
        player.sendMessage(MM.deserialize("<green>You received <yellow>10 XP levels<green>!"));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        player.openInventory(buildInventory(player));
    }

    // ----------------------------------------------------------------
    // GUI builder
    // ----------------------------------------------------------------

    private static Inventory buildInventory(Player player) {
        UUID uuid = player.getUniqueId();
        BoostManager bm  = BoostManager.getInstance();
        EconomyAPI   eco = EconomyAPI.getInstance();

        Inventory inv = Bukkit.createInventory(null, 27, MM.deserialize(TITLE_STRING));

        long tokens = eco != null ? eco.getTokens(uuid) : 0L;

        inv.setItem(SLOT_SELL_BOOST,  makeSellBoostItem(tokens, bm, uuid));
        inv.setItem(SLOT_TOKEN_STORM, makeTokenStormItem(tokens, bm, uuid));
        inv.setItem(SLOT_MEGA_COMBO,  makeMegaComboItem(tokens, bm, uuid));
        inv.setItem(SLOT_HASTE_FLASK, makeHasteFlaskItem(tokens));
        inv.setItem(SLOT_XP_FLASK,    makeXPFlaskItem(tokens));
        inv.setItem(SLOT_SPEED_FLASK, makeSpeedFlaskItem(tokens));
        inv.setItem(4,                makeTokenInfoItem(tokens));
        inv.setItem(SLOT_CLOSE,       makeCloseItem());
        return inv;
    }

    // ----------------------------------------------------------------
    // Item builders
    // ----------------------------------------------------------------

    private static ItemStack makeSellBoostItem(long tokens, BoostManager bm, UUID uuid) {
        boolean active = bm != null && bm.hasBoost(uuid, BoostManager.BoostType.SELL);
        String remain  = (active && bm != null) ? bm.formatRemaining(uuid, BoostManager.BoostType.SELL) : "";
        ItemStack item = new ItemStack(Material.BLAZE_POWDER);
        ItemMeta meta  = item.getItemMeta();
        meta.displayName(MM.deserialize("<!italic><aqua>Sell Boost"));
        List<Component> lore = new ArrayList<>();
        lore.add(MM.deserialize("<!italic><gray><green>2x sell</green> multiplier for <green>5 minutes</green>."));
        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic><gold>$ <gold>Cost: <white>500 tokens"));
        lore.add(MM.deserialize("<!italic><aqua>\u2756 <gray>Your tokens: " + (tokens >= 500 ? "<white>" : "<red>") + tokens));
        if (active) {
            lore.add(MM.deserialize("<!italic><aqua>\u2756 <gray>Status: <green>\u2713 Active <gray>(" + remain + " remaining)"));
        } else {
            lore.add(MM.deserialize("<!italic><aqua>\u2756 <gray>Status: <red>\u2717 Inactive"));
        }
        lore.add(Component.empty());
        if (active) {
            lore.add(MM.deserialize("<!italic><green>\u2192 Already active!"));
        } else {
            lore.add(MM.deserialize("<!italic>" + (tokens >= 500 ? "<green>\u2192 Click to activate this boost!" : "<red>\u2717 Not enough tokens")));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack makeTokenStormItem(long tokens, BoostManager bm, UUID uuid) {
        boolean active = bm != null && bm.hasBoost(uuid, BoostManager.BoostType.TOKEN);
        String remain  = (active && bm != null) ? bm.formatRemaining(uuid, BoostManager.BoostType.TOKEN) : "";
        ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta  = item.getItemMeta();
        meta.displayName(MM.deserialize("<!italic><aqua>Token Storm"));
        List<Component> lore = new ArrayList<>();
        lore.add(MM.deserialize("<!italic><gray><green>2x token</green> earn rate for <green>5 minutes</green>."));
        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic><gold>$ <gold>Cost: <white>750 tokens"));
        lore.add(MM.deserialize("<!italic><aqua>\u2756 <gray>Your tokens: " + (tokens >= 750 ? "<white>" : "<red>") + tokens));
        if (active) {
            lore.add(MM.deserialize("<!italic><aqua>\u2756 <gray>Status: <green>\u2713 Active <gray>(" + remain + " remaining)"));
        } else {
            lore.add(MM.deserialize("<!italic><aqua>\u2756 <gray>Status: <red>\u2717 Inactive"));
        }
        lore.add(Component.empty());
        if (active) {
            lore.add(MM.deserialize("<!italic><green>\u2192 Already active!"));
        } else {
            lore.add(MM.deserialize("<!italic>" + (tokens >= 750 ? "<green>\u2192 Click to activate this boost!" : "<red>\u2717 Not enough tokens")));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack makeMegaComboItem(long tokens, BoostManager bm, UUID uuid) {
        boolean sellActive  = bm != null && bm.hasBoost(uuid, BoostManager.BoostType.SELL);
        boolean tokenActive = bm != null && bm.hasBoost(uuid, BoostManager.BoostType.TOKEN);
        boolean bothActive  = sellActive && tokenActive;
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta  = item.getItemMeta();
        meta.displayName(MM.deserialize("<!italic><aqua>Mega Combo"));
        List<Component> lore = new ArrayList<>();
        lore.add(MM.deserialize("<!italic><gray><green>2x sell</green> + <green>2x tokens</green> for <green>3 minutes</green>."));
        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic><gold>$ <gold>Cost: <white>1,200 tokens"));
        lore.add(MM.deserialize("<!italic><aqua>\u2756 <gray>Your tokens: " + (tokens >= 1200 ? "<white>" : "<red>") + tokens));
        if (bothActive) {
            lore.add(MM.deserialize("<!italic><aqua>\u2756 <gray>Status: <green>\u2713 Both Active"));
        } else {
            lore.add(MM.deserialize("<!italic><aqua>\u2756 <gray>Status: <red>\u2717 Inactive"));
        }
        lore.add(Component.empty());
        if (bothActive) {
            lore.add(MM.deserialize("<!italic><green>\u2192 Both boosts already active!"));
        } else {
            lore.add(MM.deserialize("<!italic>" + (tokens >= 1200 ? "<green>\u2192 Click to activate this boost!" : "<red>\u2717 Not enough tokens")));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack makeXPFlaskItem(long tokens) {
        ItemStack item = new ItemStack(Material.GLASS_BOTTLE);
        ItemMeta meta  = item.getItemMeta();
        meta.displayName(MM.deserialize("<!italic><aqua>XP Flask"));
        List<Component> lore = new ArrayList<>();
        lore.add(MM.deserialize("<!italic><gray>Instantly grants <green>10 XP levels</green>."));
        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic><gold>$ <gold>Cost: <white>300 tokens"));
        lore.add(MM.deserialize("<!italic><aqua>\u2756 <gray>Your tokens: " + (tokens >= 300 ? "<white>" : "<red>") + tokens));
        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic>" + (tokens >= 300 ? "<green>\u2192 Click to purchase this flask!" : "<red>\u2717 Not enough tokens")));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack makeHasteFlaskItem(long tokens) {
        ItemStack item = new ItemStack(Material.GOLDEN_PICKAXE);
        ItemMeta meta  = item.getItemMeta();
        meta.displayName(MM.deserialize("<!italic><aqua>Haste Flask"));
        List<Component> lore = new ArrayList<>();
        lore.add(MM.deserialize("<!italic><gray>Grants <green>Haste II</green> for <green>3 minutes</green>."));
        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic><gold>$ <gold>Cost: <white>400 tokens"));
        lore.add(MM.deserialize("<!italic><aqua>\u2756 <gray>Your tokens: " + (tokens >= 400 ? "<white>" : "<red>") + tokens));
        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic>" + (tokens >= 400 ? "<green>\u2192 Click to purchase this flask!" : "<red>\u2717 Not enough tokens")));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack makeSpeedFlaskItem(long tokens) {
        ItemStack item = new ItemStack(Material.SUGAR);
        ItemMeta meta  = item.getItemMeta();
        meta.displayName(MM.deserialize("<!italic><aqua>Speed Flask"));
        List<Component> lore = new ArrayList<>();
        lore.add(MM.deserialize("<!italic><gray>Grants <green>Speed II</green> for <green>3 minutes</green>."));
        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic><gold>$ <gold>Cost: <white>350 tokens"));
        lore.add(MM.deserialize("<!italic><aqua>\u2756 <gray>Your tokens: " + (tokens >= 350 ? "<white>" : "<red>") + tokens));
        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic>" + (tokens >= 350 ? "<green>\u2192 Click to purchase this flask!" : "<red>\u2717 Not enough tokens")));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack makeTokenInfoItem(long tokens) {
        ItemStack item = new ItemStack(Material.SUNFLOWER);
        ItemMeta meta  = item.getItemMeta();
        meta.displayName(MM.deserialize("<!italic><aqua>Your Tokens"));
        List<Component> lore = new ArrayList<>();
        lore.add(MM.deserialize("<!italic><gold>$ <white><bold>" + String.format("%,d", tokens) + "</bold> <gold>tokens"));
        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic><gray>Earn <green>tokens</green> by mining blocks."));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack makeCloseItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta  = item.getItemMeta();
        meta.displayName(MM.deserialize("<!italic><red>\u2717 Close"));
        List<Component> lore = new ArrayList<>();
        lore.add(MM.deserialize("<!italic><gray>Click to close this menu."));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static String formatMult(double m) {
        return String.format("%.1fx", m);
    }
}

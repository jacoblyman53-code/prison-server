package com.prison.coinflip;

import com.prison.menu.util.Gui;
import com.prison.menu.util.Fmt;
import com.prison.menu.util.Sounds;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CoinflipCreateGUI — 27-slot amount input GUI.
 *
 * The player chooses a preset amount or types a custom amount.
 * Confirm button (slot 22) submits the bet.
 */
public class CoinflipCreateGUI {

    public static final Component TITLE = MiniMessage.miniMessage().deserialize("<!italic>CREATE COINFLIP");
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final Map<UUID, Long> pendingAmounts = new ConcurrentHashMap<>();

    // Preset amount slots (row 1)
    private static final long[] PRESETS = {1_000L, 5_000L, 10_000L, 50_000L, 100_000L, 500_000L, 1_000_000L};
    private static final int[]  PRESET_SLOTS = {10, 11, 12, 13, 14, 15, 16};

    private static final int SLOT_CONFIRM = 22;
    private static final int SLOT_CANCEL  = 24;
    private static final int SLOT_BACK    = 18;

    public static void open(Player player) {
        pendingAmounts.put(player.getUniqueId(), 1_000L); // default
        player.openInventory(build(player));
    }

    public static void handleClick(Player player, int slot, CoinflipPlugin plugin) {
        UUID uuid = player.getUniqueId();

        if (slot == SLOT_BACK || slot == SLOT_CANCEL) {
            Sounds.nav(player);
            CoinflipBrowserGUI.open(player);
            return;
        }

        if (slot == SLOT_CONFIRM) {
            Long amount = pendingAmounts.get(uuid);
            if (amount == null || amount <= 0) {
                Sounds.deny(player);
                player.sendMessage(MM.deserialize("<red>Invalid bet amount."));
                return;
            }
            String error = CoinflipManager.getInstance().createTicket(player, amount);
            if (error != null) {
                Sounds.deny(player);
                player.sendMessage(MM.deserialize("<red>" + error));
            } else {
                Sounds.buy(player);
                player.closeInventory();
                pendingAmounts.remove(uuid);
            }
            return;
        }

        // Preset slot?
        for (int i = 0; i < PRESET_SLOTS.length; i++) {
            if (PRESET_SLOTS[i] == slot) {
                pendingAmounts.put(uuid, PRESETS[i]);
                Sounds.nav(player);
                player.openInventory(build(player));
                return;
            }
        }
    }

    // ----------------------------------------------------------------

    private static Inventory build(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        UUID uuid = player.getUniqueId();
        long selected = pendingAmounts.getOrDefault(uuid, 1_000L);
        long bal = 0;
        try {
            com.prison.economy.EconomyAPI api = com.prison.economy.EconomyAPI.getInstance();
            if (api != null) bal = api.getBalance(uuid);
        } catch (Exception ignored) {}

        // Close / Back button (slot 0 — re-uses SLOT_BACK handler via slot 18, close at 0 for spec)
        inv.setItem(0, Gui.make(Material.BARRIER,
            "<!italic><red>← Back to Coinflip",
            "<!italic><gray>Return to coinflip browser."));

        // Preset buttons
        for (int i = 0; i < PRESETS.length; i++) {
            long preset = PRESETS[i];
            boolean isSelected = preset == selected;
            boolean canAfford  = bal >= preset;
            Material mat = isSelected ? Material.LIME_CONCRETE : (canAfford ? Material.GOLD_INGOT : Material.RED_STAINED_GLASS_PANE);
            String name  = isSelected
                ? "<!italic><green>✓ <gold>$" + Fmt.compact(preset)
                : "<!italic><gold>$" + Fmt.compact(preset);
            String affordLine = canAfford
                ? "<!italic><green>✓ Can afford"
                : "<!italic><red>✗ Cannot afford";
            inv.setItem(PRESET_SLOTS[i], Gui.make(mat, name,
                "<!italic><aqua>✦ <gray>Bet Amount: <gold>$" + Fmt.number(preset),
                "<!italic><aqua>✦ <gray>Affordability: " + affordLine,
                "<!italic>",
                "<!italic><green>→ Click to place this bet!"));
        }

        // Confirm
        boolean canAffordSelected = bal >= selected;
        if (canAffordSelected) {
            inv.setItem(SLOT_CONFIRM, Gui.make(Material.GREEN_CONCRETE,
                "<!italic><green>✓ Confirm",
                "<!italic><gray>Click to confirm <green>creating<gray> this coinflip.",
                "<!italic><aqua>✦ <gray>Bet Amount: <gold>$" + Fmt.number(selected),
                "<!italic><aqua>✦ <gray>Potential Win: <green>$" + Fmt.number(selected * 2),
                "<!italic>",
                "<!italic><green>→ Click to confirm and lock funds."));
        } else {
            inv.setItem(SLOT_CONFIRM, Gui.make(Material.RED_CONCRETE,
                "<!italic><red>✗ Cannot Afford",
                "<!italic><aqua>✦ <gray>Required: <gold>$" + Fmt.number(selected),
                "<!italic><red>Select a smaller amount."));
        }

        inv.setItem(SLOT_CANCEL, Gui.make(Material.BARRIER,
            "<!italic><red>✗ Cancel",
            "<!italic><gray>Return to coinflip browser."));
        inv.setItem(SLOT_BACK, Gui.back());
        return inv;
    }
}

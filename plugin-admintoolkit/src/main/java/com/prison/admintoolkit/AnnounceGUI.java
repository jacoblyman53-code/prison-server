package com.prison.admintoolkit;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;

/**
 * AnnounceGUI — lets admins broadcast messages via chat, title, action bar, or boss bar.
 */
public class AnnounceGUI {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final Component TITLE = MM.deserialize("ANNOUNCEMENTS");

    // ----------------------------------------------------------------
    // Open
    // ----------------------------------------------------------------

    public static void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        // Content row (slots 9-17)
        inv.setItem(10, AdminPanel.makeItem(Material.PAPER,
            "<aqua>Chat Broadcast",
            "<gray>Broadcasts a <green>message<gray> to all players in chat.",
            "",
            "<green>→ <green>Click to <green><underlined>compose</underlined> a chat message!"));

        inv.setItem(12, AdminPanel.makeItem(Material.GOLDEN_CHESTPLATE,
            "<aqua>Title + Subtitle",
            "<gray>Shows a large <green>title<gray> on all players' screens.",
            "",
            "<green>→ <green>Click to <green><underlined>compose</underlined> a title!"));

        inv.setItem(14, AdminPanel.makeItem(Material.NAME_TAG,
            "<aqua>Action Bar",
            "<gray>Shows a <green>message<gray> above the hotbar.",
            "",
            "<green>→ <green>Click to <green><underlined>compose</underlined> an action bar message!"));

        inv.setItem(16, AdminPanel.makeItem(Material.BEACON,
            "<aqua>Boss Bar",
            "<gray>Shows a <green>boss bar<gray> at the top of all screens.",
            "",
            "<green>→ <green>Click to <green><underlined>compose</underlined> a boss bar message!"));

        player.openInventory(inv);
    }

    // ----------------------------------------------------------------
    // Title matching
    // ----------------------------------------------------------------

    public static boolean isTitle(Component title) {
        return TITLE.equals(title);
    }

    // ----------------------------------------------------------------
    // Click handling
    // ----------------------------------------------------------------

    public static void handleClick(Player player, int slot) {
        switch (slot) {
            case 10 -> {
                // Chat broadcast
                AnvilInputGUI.open(player, "Message...", text -> {
                    Bukkit.broadcast(MM.deserialize(text));
                    player.sendMessage(MM.deserialize("<green>Broadcast sent."));
                    Bukkit.getScheduler().runTask(AdminToolkitPlugin.getInstance(), (Runnable) player::closeInventory);
                });
            }
            case 12 -> {
                // Title + Subtitle
                AnvilInputGUI.open(player, "Title text (MiniMessage)...", text -> {
                    Title title = Title.title(
                        MM.deserialize(text),
                        Component.empty(),
                        Title.Times.times(
                            Duration.ofSeconds(1),
                            Duration.ofSeconds(3),
                            Duration.ofSeconds(1)
                        )
                    );
                    Bukkit.getScheduler().runTask(AdminToolkitPlugin.getInstance(), () -> {
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            p.showTitle(title);
                        }
                        player.sendMessage(MM.deserialize("<green>Title sent."));
                        player.closeInventory();
                    });
                });
            }
            case 14 -> {
                // Action bar
                AnvilInputGUI.open(player, "Action bar message...", text -> {
                    Bukkit.getScheduler().runTask(AdminToolkitPlugin.getInstance(), () -> {
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            p.sendActionBar(MM.deserialize(text));
                        }
                        player.sendMessage(MM.deserialize("<green>Action bar sent."));
                        player.closeInventory();
                    });
                });
            }
            case 16 -> {
                // Boss bar
                AnvilInputGUI.open(player, "Boss bar message...", text -> {
                    Bukkit.getScheduler().runTask(AdminToolkitPlugin.getInstance(), () -> {
                        BossBar bar = BossBar.bossBar(
                            MM.deserialize(text),
                            1.0f,
                            BossBar.Color.YELLOW,
                            BossBar.Overlay.PROGRESS
                        );
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            p.showBossBar(bar);
                        }
                        // Remove after 10 seconds (200 ticks)
                        Bukkit.getScheduler().runTaskLater(AdminToolkitPlugin.getInstance(), () -> {
                            for (Player p : Bukkit.getOnlinePlayers()) {
                                p.hideBossBar(bar);
                            }
                        }, 200L);
                        player.sendMessage(MM.deserialize("<green>Boss bar sent."));
                        player.closeInventory();
                    });
                });
            }
        }
    }
}

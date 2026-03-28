package com.prison.staff;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * ReportGUI — two inventory GUIs for the report system.
 *
 * 1) Confirm GUI (27-slot): player confirms before submitting a report.
 * 2) Queue GUI (54-slot): staff view/manage pending reports.
 */
public class ReportGUI {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MM/dd HH:mm");

    static final String CONFIRM_TITLE = "Report Confirmation";
    static final String QUEUE_TITLE   = "Report Queue";

    // Confirm GUI slots
    static final int CONFIRM_SLOT = 11;
    static final int CANCEL_SLOT  = 15;

    // Queue GUI — reports start at slot 9, end at slot 44 (3 rows of 9 minus borders)
    static final int PAGE_SIZE = 28; // 4 rows of 7 (with side borders)

    public static void openConfirm(Player player, String targetName, String reason) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text(CONFIRM_TITLE));

        ItemStack border = border();
        for (int i = 0; i < 27; i++) inv.setItem(i, border);

        // Info item
        ItemStack info = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta m = info.getItemMeta();
        m.displayName(MM.deserialize("<yellow>Reporting: <white>" + targetName));
        m.lore(List.of(
            MM.deserialize("<gray>Reason: <white>" + reason),
            MM.deserialize(""),
            MM.deserialize("<gray>Are you sure you want to submit this report?")
        ));
        info.setItemMeta(m);
        inv.setItem(13, info);

        // Confirm
        ItemStack confirm = new ItemStack(Material.LIME_WOOL);
        ItemMeta cm = confirm.getItemMeta();
        cm.displayName(MM.deserialize("<green><bold>CONFIRM — Submit Report"));
        confirm.setItemMeta(cm);
        inv.setItem(CONFIRM_SLOT, confirm);

        // Cancel
        ItemStack cancel = new ItemStack(Material.RED_WOOL);
        ItemMeta xm = cancel.getItemMeta();
        xm.displayName(MM.deserialize("<red><bold>CANCEL"));
        cancel.setItemMeta(xm);
        inv.setItem(CANCEL_SLOT, cancel);

        player.openInventory(inv);
    }

    public static void openQueue(Player player, List<ReportData> reports, int page) {
        int totalPages = Math.max(1, (int) Math.ceil(reports.size() / (double) PAGE_SIZE));
        page = Math.max(0, Math.min(page, totalPages - 1));

        Inventory inv = Bukkit.createInventory(null, 54, Component.text(QUEUE_TITLE));

        // Border
        ItemStack border = border();
        for (int i = 0; i < 9; i++)  inv.setItem(i, border);
        for (int i = 45; i < 54; i++) inv.setItem(i, border);
        for (int r = 1; r <= 4; r++) {
            inv.setItem(r * 9, border);
            inv.setItem(r * 9 + 8, border);
        }

        // Page info
        ItemStack pageInfo = new ItemStack(Material.PAPER);
        ItemMeta pm = pageInfo.getItemMeta();
        pm.displayName(MM.deserialize(
            "<gray>Page <white>" + (page + 1) + "<gray>/" + totalPages +
            "  <dark_gray>(" + reports.size() + " pending)"));
        pageInfo.setItemMeta(pm);
        inv.setItem(4, pageInfo);

        // Prev / Next
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta prevM = prev.getItemMeta();
            prevM.displayName(MM.deserialize("<yellow>← Previous Page"));
            prev.setItemMeta(prevM);
            inv.setItem(45, prev);
        }
        if (page < totalPages - 1) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nextM = next.getItemMeta();
            nextM.displayName(MM.deserialize("<yellow>Next Page →"));
            next.setItemMeta(nextM);
            inv.setItem(53, next);
        }

        // Report items in inner slots (1-7 for rows 1-4)
        int[] slots = new int[PAGE_SIZE];
        int si = 0;
        for (int row = 1; row <= 4; row++) {
            for (int col = 1; col <= 7; col++) {
                slots[si++] = row * 9 + col;
            }
        }

        int start = page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE && (start + i) < reports.size(); i++) {
            ReportData report = reports.get(start + i);
            inv.setItem(slots[i], makeReportItem(report));
        }

        player.openInventory(inv);
    }

    private static ItemStack makeReportItem(ReportData report) {
        ItemStack item = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MM.deserialize(
            "<red>" + report.reportedName() + " <dark_gray>#" + report.id()));

        List<Component> lore = new ArrayList<>();
        lore.add(MM.deserialize("<gray>Reporter: <white>" + report.reporterName()));
        lore.add(MM.deserialize("<gray>Reason: <yellow>" + report.reason()));
        lore.add(MM.deserialize("<gray>Time: <white>" + report.createdAt().format(FMT)));
        lore.add(MM.deserialize(""));
        lore.add(MM.deserialize("<aqua>Left-click <gray>— TP to reported player"));
        lore.add(MM.deserialize("<red>Right-click <gray>— Close report"));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack border() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta m = item.getItemMeta();
        m.displayName(Component.empty());
        item.setItemMeta(m);
        return item;
    }
}

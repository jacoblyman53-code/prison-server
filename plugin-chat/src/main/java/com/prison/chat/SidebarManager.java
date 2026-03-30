package com.prison.chat;

import com.prison.economy.BoostManager;
import com.prison.economy.EconomyAPI;
import com.prison.permissions.PermissionEngine;
import com.prison.ranks.RanksAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * SidebarManager — per-player sidebar scoreboard using the team-prefix trick.
 *
 * Each line is a Team whose name is "line_N". The team has exactly one entry
 * (an invisible dummy string like "§0§r") added to it. The team's prefix
 * Component is set to the actual display text. This avoids the 16-char limit
 * on score entry names while keeping the line stable (score never flickers).
 *
 * Line order: Bukkit sidebar shows highest score at top, so we assign
 * scores from (lineCount) down to 1 so that line index 0 = top.
 */
public class SidebarManager {

    // Invisible dummy entry strings — each must be unique within the scoreboard.
    // Legacy color codes render as zero-width in the sidebar but are distinct strings.
    private static final String[] DUMMY_ENTRIES = {
        "§0§r", "§1§r", "§2§r", "§3§r", "§4§r",
        "§5§r", "§6§r", "§7§r", "§8§r", "§9§r",
        "§a§r", "§b§r", "§c§r", "§d§r", "§e§r",
        "§f§r", "§0§0§r", "§1§1§r", "§2§2§r", "§3§3§r"
    };

    private final ChatPlugin plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public SidebarManager(ChatPlugin plugin) {
        this.plugin = plugin;
    }

    // ----------------------------------------------------------------
    // Public API
    // ----------------------------------------------------------------

    /**
     * Build or rebuild a fresh scoreboard for the given player and assign it.
     * Safe to call from the main thread only (Bukkit scoreboard API is not thread-safe).
     */
    public void buildBoard(Player player) {
        if (!player.isOnline()) return;

        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard board = manager.getNewScoreboard();

        // Register the sidebar objective
        Objective obj = board.registerNewObjective(
            "prison_sidebar",
            Criteria.DUMMY,
            mm.deserialize(plugin.getChatConfig().sidebarTitle())
        );
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Build the line content list (index 0 = top)
        List<Component> lines = buildLines(player);

        // Register teams and assign scores
        // Highest score = top. We assign (lines.size()) down to 1.
        for (int i = 0; i < lines.size(); i++) {
            if (i >= DUMMY_ENTRIES.length) break; // Safety: never exceed dummy pool

            String entry   = DUMMY_ENTRIES[i];
            String teamName = "line_" + i;
            int score      = lines.size() - i; // descending so index 0 is on top

            Team team = board.registerNewTeam(teamName);
            team.addEntry(entry);
            team.prefix(lines.get(i));
            team.suffix(Component.empty());

            obj.getScore(entry).setScore(score);
        }

        player.setScoreboard(board);
    }

    /**
     * Update the existing board in place for a player who already has one.
     * If the player doesn't have our objective, falls back to buildBoard().
     */
    public void updateBoard(Player player) {
        if (!player.isOnline()) return;

        Scoreboard board = player.getScoreboard();

        // If the player somehow lost our objective, rebuild from scratch
        if (board.getObjective("prison_sidebar") == null) {
            buildBoard(player);
            return;
        }

        List<Component> lines = buildLines(player);

        for (int i = 0; i < lines.size(); i++) {
            if (i >= DUMMY_ENTRIES.length) break;

            Team team = board.getTeam("line_" + i);
            if (team == null) {
                // Line count changed — safer to just rebuild
                buildBoard(player);
                return;
            }
            team.prefix(lines.get(i));
        }
    }

    /**
     * Remove the prison sidebar from a player (e.g. on quit or conflict).
     * Resets them to the server's main scoreboard.
     */
    public void removeBoard(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    // ----------------------------------------------------------------
    // Line Building
    // ----------------------------------------------------------------

    /**
     * Assembles the ordered list of line Components.
     * Index 0 is the topmost line displayed in the sidebar.
     */
    private List<Component> buildLines(Player player) {
        UUID uuid = player.getUniqueId();
        PermissionEngine perms = PermissionEngine.getInstance();

        // ---- Gather data ----
        String mineRank   = perms.getMineRank(uuid);
        int    prestige   = perms.getPrestige(uuid);
        int    online     = Bukkit.getOnlinePlayers().size();

        long balance = 0L;
        long tokens  = 0L;
        EconomyAPI eco = EconomyAPI.getInstance();
        if (eco != null) {
            balance = eco.getBalance(uuid);
            tokens  = eco.getTokens(uuid);
        }

        // ---- Build rank display ----
        String rankDisplay;
        if (mineRank == null || mineRank.isEmpty()) {
            rankDisplay = "<gray>None</gray>";
        } else {
            rankDisplay = "<white>" + mineRank.toUpperCase() + "</white>";
            if (prestige > 0) {
                rankDisplay = "<light_purple>P" + prestige + "</light_purple> " + rankDisplay;
            }
        }

        // ---- Divider (reused) ----
        String divider = plugin.getChatConfig().sidebarDivider();

        List<Component> lines = new ArrayList<>();

        // Line 0: divider (title is already in the objective display name)
        lines.add(mm.deserialize(divider));

        // Line 1: Rank label
        lines.add(mm.deserialize(
            "<gray>Rank: </gray>" + rankDisplay
        ));

        // Line 2: Rankup progress bar (or prestige prompt if at max)
        try {
            RanksAPI ranksApi = RanksAPI.getInstance();
            if (ranksApi != null && eco != null) {
                long nextCost = ranksApi.getNextRankCost(uuid);
                if (nextCost < 0) {
                    // Max rank Z — prompt to prestige
                    lines.add(mm.deserialize("<gold>✦ Ready to Prestige!</gold>"));
                } else {
                    // Build 8-segment bar
                    int filled = nextCost > 0 ? (int) Math.min(8, balance * 8L / nextCost) : 8;
                    String bar = "<green>" + "█".repeat(filled) + "</green>"
                               + "<dark_gray>" + "░".repeat(8 - filled) + "</dark_gray>";
                    int pct = nextCost > 0 ? (int) Math.min(100, balance * 100L / nextCost) : 100;
                    lines.add(mm.deserialize(
                        "<gray>Next: " + bar + " <white>" + pct + "%"));
                }
            }
        } catch (NoClassDefFoundError ignored) {
            // plugin-ranks not loaded at runtime — skip line
        }

        // Line 3 (was 2): blank
        lines.add(Component.empty());

        // Balance
        boolean ecoAvailable = (eco != null);
        if (ecoAvailable) {
            lines.add(mm.deserialize(
                "<gray>Balance: </gray><gold>" + formatAmount(balance) + " IGC</gold>"
            ));
            // Tokens
            lines.add(mm.deserialize(
                "<gray>Tokens: </gray><aqua>" + formatAmount(tokens) + "</aqua>"
            ));
        } else {
            lines.add(mm.deserialize("<gray>Balance: </gray><dark_gray>N/A</dark_gray>"));
            lines.add(mm.deserialize("<gray>Tokens: </gray><dark_gray>N/A</dark_gray>"));
        }

        // blank
        lines.add(Component.empty());

        // Mine reset countdown
        try {
            com.prison.mines.MinesAPI minesApi = com.prison.mines.MinesAPI.getInstance();
            if (minesApi != null && mineRank != null && !mineRank.isEmpty()) {
                com.prison.mines.MineData playerMine = minesApi.getMineForRank(mineRank);
                if (playerMine != null) {
                    long nextReset = minesApi.getNextResetMs(playerMine.id());
                    if (nextReset > 0) {
                        long remainSecs = Math.max(0L, (nextReset - System.currentTimeMillis()) / 1000L);
                        long mins = remainSecs / 60;
                        long secs = remainSecs % 60;
                        String timeStr = mins > 0
                            ? mins + "m " + String.format("%02d", secs) + "s"
                            : secs + "s";
                        lines.add(mm.deserialize(
                            "<gray>Mine Reset: <aqua>" + timeStr + "</aqua>"
                        ));
                    }
                }
            }
        } catch (NoClassDefFoundError ignored) {}

        // Active sell boost
        try {
            BoostManager boosts = BoostManager.getInstance();
            if (boosts != null && boosts.hasBoost(uuid, BoostManager.BoostType.SELL)) {
                double mult = boosts.getSellMultiplier(uuid);
                String remaining = boosts.formatRemaining(uuid, BoostManager.BoostType.SELL);
                String multStr = (mult == Math.floor(mult))
                    ? String.valueOf((int) mult)
                    : String.format("%.1f", mult);
                lines.add(mm.deserialize(
                    "<gray>Sell Boost: <green>" + multStr + "x <dark_green>(" + remaining + ")</dark_green>"
                ));
            }
        } catch (NoClassDefFoundError ignored) {}

        // Online players
        lines.add(mm.deserialize(
            "<gray>Players: </gray><yellow>" + online + "</yellow>"
        ));

        // bottom divider
        lines.add(mm.deserialize(divider));

        // server IP
        lines.add(mm.deserialize(
            "<dark_aqua>" + plugin.getChatConfig().sidebarServerIp() + "</dark_aqua>"
        ));

        return lines;
    }

    // ----------------------------------------------------------------
    // Formatting Utility
    // ----------------------------------------------------------------

    /**
     * Formats a long into a human-readable abbreviated form.
     *   999        → "999"
     *   1,500      → "1.5K"
     *   2,300,000  → "2.3M"
     *   1500000000 → "1.5B"
     */
    static String formatAmount(long amount) {
        if (amount < 0) return "-" + formatAmount(-amount);
        if (amount >= 1_000_000_000L) {
            return trimTrailingZero(String.format("%.1f", amount / 1_000_000_000.0)) + "B";
        }
        if (amount >= 1_000_000L) {
            return trimTrailingZero(String.format("%.1f", amount / 1_000_000.0)) + "M";
        }
        if (amount >= 1_000L) {
            return trimTrailingZero(String.format("%.1f", amount / 1_000.0)) + "K";
        }
        return String.valueOf(amount);
    }

    private static String trimTrailingZero(String s) {
        // "1.0K" → "1K", "1.5K" stays "1.5K"
        return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
    }
}

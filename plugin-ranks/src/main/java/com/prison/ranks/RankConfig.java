package com.prison.ranks;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * RankConfig — holds the cost, display name, and chat prefix for every mine rank.
 *
 * Loaded from config.yml on startup. Contains no Bukkit API references so it
 * can be used anywhere without a plugin context.
 *
 * Ranks are stored in A-Z order (LinkedHashMap preserves insertion order).
 */
public class RankConfig {

    public static final String[] RANK_ORDER = {
        "A","B","C","D","E","F","G","H","I","J","K","L","M",
        "N","O","P","Q","R","S","T","U","V","W","X","Y","Z"
    };

    public record RankData(String rank, long cost, String display, String prefix) {}

    private final Map<String, RankData> ranks;
    private final boolean autoteleportDefault;
    private final String rankupMessage;
    private final String rankupBroadcast;
    private final String cannotAffordMessage;
    private final String maxRankMessage;

    public RankConfig(Map<String, RankData> ranks,
                      boolean autoteleportDefault,
                      String rankupMessage,
                      String rankupBroadcast,
                      String cannotAffordMessage,
                      String maxRankMessage) {
        this.ranks               = Collections.unmodifiableMap(new LinkedHashMap<>(ranks));
        this.autoteleportDefault = autoteleportDefault;
        this.rankupMessage       = rankupMessage;
        this.rankupBroadcast     = rankupBroadcast;
        this.cannotAffordMessage = cannotAffordMessage;
        this.maxRankMessage      = maxRankMessage;
    }

    public RankData getRank(String letter) {
        return ranks.get(letter.toUpperCase());
    }

    public Map<String, RankData> getAllRanks() {
        return ranks;
    }

    /**
     * Get the letter that comes after the given rank.
     * Returns null if the given rank is Z (max).
     */
    public String nextRank(String currentRank) {
        String upper = currentRank.toUpperCase();
        for (int i = 0; i < RANK_ORDER.length - 1; i++) {
            if (RANK_ORDER[i].equals(upper)) return RANK_ORDER[i + 1];
        }
        return null; // Already at Z
    }

    /**
     * Returns true if the given rank is the maximum (Z).
     */
    public boolean isMaxRank(String rank) {
        return "Z".equalsIgnoreCase(rank);
    }

    /**
     * Returns the 0-based index of a rank (A=0, B=1, ... Z=25).
     */
    public int rankIndex(String rank) {
        String upper = rank.toUpperCase();
        for (int i = 0; i < RANK_ORDER.length; i++) {
            if (RANK_ORDER[i].equals(upper)) return i;
        }
        return 0;
    }

    public boolean isAutoteleportDefault() { return autoteleportDefault; }
    public String  getRankupMessage()       { return rankupMessage; }
    public String  getRankupBroadcast()     { return rankupBroadcast; }
    public String  getCannotAffordMessage() { return cannotAffordMessage; }
    public String  getMaxRankMessage()      { return maxRankMessage; }
}

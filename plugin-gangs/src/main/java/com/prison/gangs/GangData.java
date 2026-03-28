package com.prison.gangs;

import java.time.LocalDateTime;

/**
 * GangData — immutable snapshot of a gang's state.
 */
public record GangData(long id, String name, String tag, int level, long bankBalance, LocalDateTime createdAt) {

    /** Compute the gang level from a bank balance using the configured thresholds. */
    public static int computeLevel(long bankBalance, long[] thresholds) {
        int level = 1;
        for (int i = 0; i < thresholds.length; i++) {
            if (bankBalance >= thresholds[i]) level = i + 2;
            else break;
        }
        return level;
    }

    /** Return a copy of this record with an updated bank balance and recalculated level. */
    public GangData withBank(long newBalance, long[] thresholds) {
        return new GangData(id, name, tag, computeLevel(newBalance, thresholds), newBalance, createdAt);
    }

    /** Return a copy of this record with an updated level only. */
    public GangData withLevel(int newLevel) {
        return new GangData(id, name, tag, newLevel, bankBalance, createdAt);
    }
}

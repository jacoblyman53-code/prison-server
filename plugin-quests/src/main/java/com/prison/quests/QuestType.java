package com.prison.quests;

/**
 * QuestType — defines which player action increments quest progress.
 *
 * BLOCKS_MINED  — incremented by BlockBreakEvent (one per block)
 * SELL_COMMANDS — incremented when player uses /sell or /sellall
 * RANKUPS       — incremented when player uses /rankup
 * ONLINE_TIME   — incremented in minutes; tracked per login session
 * GANG_DEPOSITS — incremented when player successfully deposits IGC into the gang bank
 */
public enum QuestType {
    BLOCKS_MINED,
    SELL_COMMANDS,
    RANKUPS,
    ONLINE_TIME,
    GANG_DEPOSITS
}

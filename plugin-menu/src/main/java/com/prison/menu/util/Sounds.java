package com.prison.menu.util;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Sounds — standard sound feedback for GUI interactions.
 * Every click category has a consistent audio response.
 */
public final class Sounds {

    private Sounds() {}

    /** Light navigation click (menu open, page turn, tab switch). */
    public static void nav(Player p) {
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
    }

    /** Successful purchase / claim. */
    public static void buy(Player p) {
        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
    }

    /** Major upgrade or level-up. */
    public static void upgrade(Player p) {
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.0f);
    }

    /** Denied / cannot afford / locked. */
    public static void deny(Player p) {
        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1.0f);
    }

    /** Reward claimed / quest complete. */
    public static void reward(Player p) {
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.5f);
    }

    /** Sell success (manual sell all). */
    public static void sell(Player p) {
        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    /** High-value win (crate, coinflip). */
    public static void win(Player p) {
        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
    }

    /** Close / cancel menu. */
    public static void close(Player p) {
        p.playSound(p.getLocation(), Sound.BLOCK_CHEST_CLOSE, 0.4f, 1.2f);
    }
}

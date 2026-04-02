# NICE TO HAVE FEATURES

> Features that add polish but are not blocking v1 launch.
> Ordered by impact × ease. Top items should be considered for v1.5.

---

## Tier 1: High Impact, Low Effort (Do Soon)

### 1. `/guide` Command
**Description:** Opens a help GUI with Egyptian-themed tips for new players.
**Effort:** 1 day
**Impact:** Significantly reduces "what do I do?" support requests.
**Implementation:** New command in `plugin-menu` that opens a paginated help GUI.
Content comes from `GUI_DESIGN_SPEC.md` help menu standard.

### 2. Pending AH Pickup Notification
**Description:** When a player logs in and has items/coins from expired AH listings,
show a notification: `<gold>⚱ You have items waiting at the Grand Bazaar. /ah`
**Effort:** 2 hours
**Impact:** Reduces "I sold something but didn't get my Coins" support tickets.
**Implementation:** Player join listener in `plugin-auctionhouse`.

### 3. Sell Streak Visual Feedback
**Description:** Play a sound and show a brief title/action bar message when sell
streak hits a milestone (10×, 25×, 50×, 100×).
**Effort:** 2 hours
**Impact:** Makes the sell streak feel alive and rewarding.
**Implementation:** In sell handler, check streak tier and trigger effects on tier change.

### 4. Rank-Up Auto-Teleport to New Mine
**Description:** Option to automatically teleport player to their newly unlocked mine
on rank-up (currently a config option `autoteleport-default: true` exists).
**Effort:** Verify if already implemented. If not, 4 hours.
**Impact:** Smooth new player experience. They immediately see what they unlocked.

### 5. Mine Full Indicator
**Description:** If a mine is nearly full (>90% of reset volume occupied), show
an indicator in the Mine Browser GUI: `<gray>⚠ Mine filling up — reset soon`.
**Effort:** 4 hours
**Impact:** Players can choose to switch mines proactively.

---

## Tier 2: High Impact, Medium Effort

### 6. Player Statistics GUI
**Description:** A `/stats` or `/profile` GUI showing a player's statistics:
blocks mined, total Coins earned, sell streak record, Ascension count, etc.
**Effort:** 1–2 days
**Impact:** Keeps players engaged with personal progress tracking.
**Data:** Most stats are derivable from existing DB tables.

### 7. Dynasty War Automation
**Description:** Automate Dynasty Wars as a weekly event with scoring, leaderboard,
and reward delivery. Currently may require manual admin triggering.
**Effort:** 2–3 days
**Impact:** Endgame content that runs without admin involvement.
**Implementation:** Extend `plugin-events` with a Dynasty War event type.

### 8. Daily Login Reward
**Description:** Small reward for logging in each day (streak-based):
Day 1: 500 Coins, Day 2: 1,000, Day 3: 50 Relics, Day 7: Pharaoh's Reliquary Key.
**Effort:** 1–2 days
**Impact:** Strong daily retention driver. Almost universal in successful prison servers.
**Implementation:** New feature in `plugin-kits` or standalone plugin.

### 9. Milestone Announcements
**Description:** Server-wide broadcast when a player achieves specific milestones:
first to reach Pharaoh rank, first Ascension, first Dynasty, richest player threshold.
**Effort:** 1 day
**Impact:** Creates shared events and social awareness of other players' achievements.

### 10. Vote Rewards Automation
**Description:** Voting on Minecraft server lists (Minecraft-mp.com, Planet Minecraft)
rewards the player with a Canopic Chest key automatically.
**Effort:** 1–2 days (requires vote listener like NuVotifier)
**Impact:** Free advertising + player retention. Standard for prison servers.
**Dependency:** Requires NuVotifier or similar vote listener plugin.

---

## Tier 3: Medium Impact, Medium Effort

### 11. Mine Speedrun Leaderboard
**Description:** Track who mines the most blocks in a single reset cycle.
Top 3 get a bonus reward. Resets with the mine.
**Effort:** 2 days
**Impact:** Competitive endgame activity for power miners.

### 12. Prestige Shop Expansion
**Description:** Add more items to the prestige shop beyond Mine Profit and Relic Mastery.
Options: exclusive cosmetic, one-time Coin bonus, Khopesh slot expansion.
**Effort:** 1 day
**Impact:** More spending options for high-Ascension players.

### 13. Gang/Dynasty Chat
**Description:** A `/gc` (Dynasty chat) command for private Dynasty-only chat channel.
**Effort:** 4 hours
**Impact:** Social cohesion for Dynasty members.
**Note:** May already exist — verify in `plugin-gangs`.

### 14. Auction House Search
**Description:** `/ah search <item>` to find specific items in the Grand Bazaar.
**Effort:** 1–2 days
**Impact:** Usability improvement for players with large auction house inventories.

---

## Tier 4: Low Impact or High Effort (v2+)

### 15. Web Admin Dashboard
**Description:** Web panel for server admins to view economy stats, player profiles,
manage bans, view anticheat flags. Currently an empty directory.
**Effort:** 2–3 weeks
**Priority:** v2 (advertise as upcoming feature)

### 16. Trial of Horus PvP Arena
**Description:** Weekly PvP event in an Egyptian arena. Winner gets Relics.
**Effort:** 1–2 weeks (requires world build + plugin work)
**Priority:** v1.5 or v2

### 17. Boss Raid Events
**Description:** Server-wide raid boss event. All players fight a custom boss.
**Effort:** High (requires custom mob AI or external plugin)
**Priority:** v2

### 18. Player-Owned Shops
**Description:** Let endgame players rent a stall in the Merchant's Bazaar.
**Effort:** High
**Priority:** v2+

---

*See `CUT_FEATURES.md` for features explicitly excluded from v1.*
*See `FEATURE_COMPLETENESS_REVIEW.md` for the v1 build/stub/cut decisions.*

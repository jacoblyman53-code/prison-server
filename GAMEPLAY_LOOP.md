# GAMEPLAY LOOP DESIGN

> **Purpose:** Document the intended player experience from first join to end-game.
> This is the "experience promise" to buyers — what players will actually do on this server.
> Reference `PRODUCT_IDENTITY.md` for all naming conventions used here.

---

## Core Loop (The Heartbeat)

Every system on this server feeds into or out of this loop:

```
MINE BLOCKS → SELL BLOCKS → EARN COINS → RANK UP → UNLOCK BETTER MINE → MINE BLOCKS
                                  ↓
                           EARN RELICS → ENCHANT KHOPESH → MINE FASTER
                                  ↓
                           PRESTIGE (ASCEND) → RESET RANK → START LOOP AGAIN (FASTER)
```

The loop is intentionally repetitive. That's the genre. What makes it compelling:
1. **The mine improves.** Each rank unlocks a better tomb with rarer blocks and higher sell prices.
2. **The tool improves.** Relic-funded enchantments compound — a fully enchanted Khopesh mines exponentially faster.
3. **The player improves.** Ascension is a permanent power-up — each reset is shorter than the last.

---

## Hour-by-Hour: First Session

### Hour 1 — The Condemned Soul

Player joins for the first time.

**What happens:**
1. Spawn in the Egyptian-themed hub. Grand pyramid visible. Torches, hieroglyphs, ancient aesthetic.
2. Welcome title: `<gold>Welcome to The Pharaoh's Prison` / `<gray>Your sentence has begun.`
3. Starter Kit auto-delivered (basic pickaxe, some food, a scroll of instructions).
4. NPC or sign directs player to the Pit of Souls (free starter mine).
5. Player begins mining stone in the Pit of Souls.
6. First `/sell all` — earns a small amount of Coins. Chat message: `<gold>✦ You sold your haul for 124 Coins.`
7. Sell streak begins: `<gray>Sell streak: <gold>1x`. Piques curiosity.
8. Player opens `/ranks` (or main menu). Sees the full rank ladder from Slave → Pharaoh.
9. Sees Rank B costs 500 Coins. Already halfway there.
10. Ranks up to Serf. Title fires: `<gold>⚡ You have risen to <gold>[Serf]<gold>!`
11. Serf unlocks the Tomb of Aten (Rank A–B mine) — better than the Pit of Souls.

**Key emotion:** Immediate progress. Rank-up in the first 5 minutes.

**What the player wants after Hour 1:**
- To see how high the rank ladder goes.
- To understand what Relics are for.
- To see what a "good" mine looks like.

---

### Hours 2–4 — The Tomb Grinder

Player has settled into the loop.

**What happens:**
- Grinding through Ranks B–F (Serf → Scribe). Costs start small, ramp up gradually.
- Sell streak building. At 5x multiplier, chat hint fires: `<gray>Your sell streak is <gold>5x<gray>! Keep selling to grow it.`
- First Relic earned — from a quest or crate. Player opens the Khopesh menu.
- Sees 11 enchantments. Buys their first: **Blessing of Shu** (Speed) or **Anubis' Speed** (Efficiency).
- Immediately mines faster. The feedback loop clicks.
- Joins or creates a Dynasty (gang) if playing with friends.
- May browse the Grand Bazaar (auction house) to see what others are selling.

**Key emotion:** Systems opening up. The server feels deep.

**Bottleneck:**
- Rank G (Soldier) costs a meaningful jump. Player may slow down here.
- This is intentional — it's the first "wall." Players who push through feel rewarded.

---

### Day 1 (Hours 5–8) — The Initiate

**What happens:**
- Ranks F–L achieved (Scribe → Vizier). Mid-tier player.
- Sell streak may have reached 20–30x. Coins are flowing.
- Khopesh has 3–4 enchantments. Mining is noticeably faster than a new player.
- First Ascension becomes visible on the horizon (need Rank Z, still far away).
- Daily quests are being completed. Weekly quest progress underway.
- If lucky, opened a Canopic Chest from voting. May have bought a Tomb Chest from the shop.
- Cosmetic tag may have been unlocked. Player displays it in chat.

**Key emotion:** Investment. The player has built something. Switching servers feels like a loss.

---

## Weekly Progression

### Week 1 — Proving Worth
- Reach Rank P–R (Sorcerer → Djed range)
- Khopesh has 6–7 enchantments
- Sell streak regularly hitting 50–80x
- Active in Dynasty (gang), contributing to Dynasty Tomb
- Competing on leaderboards (top 10 richest)
- Has opened multiple crates
- First Pharaoh's Reliquary (rare crate) opened — may contain significant relic reward

### Week 2 — The Rise
- Ranks R–Z achievable for dedicated players
- First Ascension possible
- Khopesh approaching max enchants
- Dynasty may be competitive on leaderboards
- Has spent real money on Tebex (if monetization is working)
- 20–50x faster grinding than week 1 due to enchants + streak

### Month 1 — The Ascended
- 2–4 Ascensions for very active players
- Max-enchant Khopesh
- Permanent cosmetics collected
- Leaderboard competitor
- May be helping new players (increases retention for server owner)
- Endgame loop: Dynasty wars, auction house trading, leaderboard grinding, event participation

---

## Sell Streak Mechanic (Detailed)

The sell streak is the primary **retention hook** of the core loop.

**How it works:**
1. Each `/sell all` within a configurable time window increases the streak.
2. Streak multiplier: 1x → 2x → 5x → 10x → 25x → 50x → 100x (configurable steps)
3. If the player doesn't sell within the window, streak resets to 1x.
4. The streak multiplier applies to all sold blocks.

**Why this works:**
- Players won't log off mid-streak. "I just need one more sell."
- Forces active mining sessions rather than AFK.
- Creates urgency without punishment — it resets, not penalizes.

**Display:**
- Scoreboard shows current streak
- Chat message on each sell shows new streak level
- Sound effect on streak increase (milestone at 5x, 10x, 25x, 50x, 100x)

---

## Prestige / Ascension Loop

Ascension is for players who have "finished" the rank ladder. It's the end-game content.

**Ascension requirements:**
- Must be Rank Z (Pharaoh)
- Must have enough Relics to pay the Ascension cost (configurable)
- Opens the Ascension menu to confirm

**What Ascension does:**
- Resets rank back to A (Slave) — "you are reborn"
- Grants permanent Ascension bonuses (more Relics per sell, mine efficiency, etc.)
- Awards cosmetic Ascension title (displayed before rank prefix)
- Unlocks higher-tier mines that require Ascension rank to access (Tomb of Osiris, Tomb of Ra, etc.)

**Why players Ascend:**
1. Permanent stat bonuses make each run faster
2. Cosmetic titles are prestige signals in chat
3. High-Ascension mines have better block composition → more Coins → faster future ranks
4. Dynasty points for Ascension events

**Ascension Cadence:**
- First Ascension: several weeks for casual, ~1 week for dedicated
- Each subsequent Ascension: faster due to bonuses compounding
- Max (Living God): represents months of play for most players

---

## Dynasty (Gang) Loop

Dynasties add a social layer that increases retention and server stickiness.

**Daily Dynasty activity:**
- Contribute to the Dynasty Tomb (members mine in a shared mine, contributing to Dynasty XP)
- Dynasty missions (shared quests with bonus rewards)
- Dynasty leaderboard competition

**Weekly:**
- Dynasty Wars event (score-based competition between Dynasties)
- Top Dynasty wins a server-wide bonus (e.g., +25% sell value for 24 hours)

**Why this increases retention:**
- Players stay active to not let down their Dynasty
- Social bonds form — players recruit friends
- Competition with rival Dynasties creates narrative ("we beat House of Ra last week")

---

## Event Loop

Events break up the grind with limited-time activities.

**Regular Events (automated):**
| Event | Trigger | Duration | Effect |
|---|---|---|---|
| Ra's Blessing | Daily, random time | 30 min | 2x sell value |
| Thoth's Wisdom | 3x weekly | 1 hour | 2x Relic earn |
| Pharaoh's Offering | Weekly | 15 min | Drop party (random items fall at spawn) |
| Trial of Horus | Weekly | 45 min | PvP arena event, winner gets Relics |

**Admin-triggered Events:**
- Double everything events for holidays/milestones
- Special crate drops

**Why events matter:**
- Give players a reason to be online at specific times
- Create shared experiences that are talked about in chat
- Drive Tebex purchases (players want to maximize event rewards)

---

## Monetization Touch Points

Where in the gameplay loop real money is most naturally spent:

1. **After first crate opening** — Player sees what a Pharaoh's Reliquary contains. Wants more.
2. **At the rank wall (G, M, R, Z)** — Player hits a cost wall. A coin booster is tempting.
3. **After Ascension** — Player resets and sees how long the grind is. Donor mine access is appealing.
4. **After seeing a Pharaoh's Chosen player** — Cosmetic envy drives rank purchases.
5. **During Ra's Blessing event** — Player wants to maximize the event. Buys a booster.

---

## Retention Mechanisms Summary

| Mechanism | What It Does | Time Horizon |
|---|---|---|
| Sell streak | Keeps players mining in sessions | Session (1–3 hours) |
| Daily quests | Gives a daily login reason | Day |
| Weekly quests | Longer engagement target | Week |
| Dynasty missions | Social obligation to show up | Daily/Weekly |
| Rank progression | Long-term goal (Pharaoh) | Weeks |
| Ascension system | End-game goal | Months |
| Events | Scheduled appointment viewing | Weekly |
| Leaderboard competition | Ego-driven grinding | Ongoing |
| Cosmetic collection | Completionist hook | Ongoing |

---

*See `NEW_PLAYER_FLOW.md` for the exact first-join sequence.*
*See `PROGRESSION_MAP.md` for the complete rank/mine unlock chart.*

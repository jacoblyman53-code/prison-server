# ENDGAME DESIGN

> **Purpose:** Define what max-rank and max-prestige players do on the server daily.
> The endgame must be compelling — if it isn't, players quit at Pharaoh rank.

---

## The Endgame Problem

On most prison servers, reaching the max rank is the end of the game.
Players hit Rank Z, look around, and leave — because there's nothing left.

This server avoids that trap with three layers of endgame content:
1. **Vertical endgame:** Ascension (prestige) — always another level to reach
2. **Social endgame:** Dynasty competition — no ceiling on PvP-adjacent content
3. **Economic endgame:** Market dominance — the richest player has status

---

## Layer 1: Vertical Endgame (Ascension)

### Who this is for
Players who want a personal progression goal. Solo grinders. Completionists.

### What they do daily
- **Mine the Tomb of Anubis** — Pure Ancient Debris, maximum Coins/hour
- **Complete daily + weekly quests** — Relics for Khopesh upgrades
- **Target the next Ascension** — Saving Relics + Coins for the next ascension cost

### How long this lasts
10 Ascension levels, each requiring more than the last. At 1–2 months per playthrough
for dedicated players, this is 10–20 months of content.

### What Pharaoh-rank players feel
- Mining is fast (max enchants)
- Every `/sell` generates significant Coins (sell streak 100×, Ascension bonuses)
- The Tomb of Anubis feels exclusive and rewarding
- The next Ascension title is visible on the horizon

---

## Layer 2: Social Endgame (Dynasties)

### Who this is for
Players who want competition and community. Social players. PvP-adjacent players.

### What they do daily
- **Contribute to Dynasty Mine:** Mine in the Dynasty Tomb, contributing Dynasty XP
- **Dynasty Missions:** Shared quests with bonus rewards
- **Dynasty War events:** Weekly score-based competition

### Dynasty War Design
A weekly event where Dynasties compete for a server bonus:
- Score points by: contributing to Dynasty mine (1pt/100 blocks), completing Dynasty
  missions (10–50 pts), winning Dynasty event minigames (configurable)
- Top Dynasty wins: `+25% sell value for all members for 24 hours`
- Second place wins: `+10% sell value for all members for 12 hours`

Dynasty Wars create narrative — "we beat House of Ra last week" is a story players tell.

### Implementation Status
Dynasty system is IMPLEMENTED. Dynasty Wars event scheduling may need work.
Verify in `plugin-gangs` and `plugin-events` whether this is wired up.

---

## Layer 3: Economic Endgame (Market Dominance)

### Who this is for
Traders, min-maxers, players who enjoy economic games.

### What they do daily
- **Auction House arbitrage:** Buy low, sell high on the Grand Bazaar
- **Enchanted Khopesh trading:** Sell fully-enchanted pickaxes to new players for Coins
- **Crate key trading:** If keys are tradeable, broker them
- **Leaderboard competition:** Stay #1 richest player

### Leaderboard Prestige
The `/leaderboard` (Hall of Legends) shows richest players, top Dynasty, top Ascension.
Being on the leaderboard is a status symbol that drives continued play.

### Implementation Status
Leaderboard system IMPLEMENTED (`plugin-leaderboards`).
Auction House IMPLEMENTED (`plugin-auctionhouse`).
Item trading between players depends on whether AH allows non-standard items.

---

## Daily Loop for a Max-Rank Player

A Pharaoh-rank, Living God (max Ascension) player on a typical day:

**Morning (30 min):**
1. Complete 3 daily quests — Relic income
2. Sell streak to 100× — maximize Coin income for the session
3. Check Grand Bazaar for profitable trades

**Afternoon/Evening (1–2 hours):**
1. Mine Tomb of Anubis — maximum Coins/hour
2. Maintain sell streak — don't let it drop
3. Contribute to Dynasty Mine for Dynasty War points
4. Participate in any active Sacred Rites events

**Weekly:**
1. Dynasty War event — fight for the weekly bonus
2. Complete weekly quest
3. Open any accumulated crates

---

## Gaps in Endgame Content

| Gap | Impact | Recommended Fix |
|---|---|---|
| **No Black Market** | References exist in code/config but system is missing. Would provide a rotating secret shop for endgame items. | Build a minimal Black Market: 3–5 rotating items refreshed every hour, costs Relics. |
| **No endgame crafting** | No use for accumulated rare materials beyond selling | Optional future feature: "Divine Crafting" for cosmetics from collected items |
| **Events need more variety** | Only a few event types documented | Add: Treasure Hunt event, Tomb Raid event (co-op mining challenge) |
| **No PvP arena** | "Trial of Horus" referenced but no implementation found | Build or stub a PvP arena event — even a flat arena works for v1 |
| **Dynasty War not fully implemented** | No confirmation that automated Dynasty War scoring exists | Verify and complete in `plugin-gangs` + `plugin-events` (Phase 8) |

---

## Content That Extends the Endgame (Post-Launch)

These are features that would be added after v1 launch, promised to buyers as future updates:

1. **Seasonal events** — Monthly limited-time content tied to Egyptian holidays/myths
2. **Guild Wars 2.0** — More complex Dynasty competition with territory control
3. **Boss Raids** — Co-op events where players fight a raid boss for exclusive loot
4. **Legendary Khopesh tier** — A 12th enchant slot that requires 10 Ascensions to unlock
5. **Player-owned shops** — Let endgame players rent a shop stall in the Merchant's Bazaar

---

## What Makes This Endgame Better Than Typical Prison

Most prison servers have no endgame. Players hit max rank and quit.

This server has:
- **10 Ascension levels** — 10–20 months of vertical progression
- **Dynasty competition** — social engagement loop with weekly stakes
- **Economy depth** — meaningful trading via Auction House
- **Leaderboard prestige** — ego-driven retention

The combination means players stay for months, not days.
This is a key selling point for the product — document it prominently in sales materials.

---

*See `ASCENSION_SYSTEM.md` for detailed Ascension design.*
*See `GAMEPLAY_LOOP.md` for how the endgame connects to the full player journey.*

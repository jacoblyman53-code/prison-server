# ASCENSION SYSTEM (Prestige)

> **Purpose:** Complete design reference for the Ascension (prestige) system.
> Covers requirements, rewards, bonuses, and the value proposition for players.

---

## What Is Ascension?

Ascension is the prestige system. When a player reaches Pharaoh rank (Z), they may
petition the gods to be reborn — resetting their rank to Slave (A) but gaining
permanent divine bonuses and exclusive cosmetics.

**Core loop:** Rank up → Reach Pharaoh → Ascend → Rank up again (faster) → Ascend again.

Each Ascension cycle is shorter than the last due to compounding bonuses.

---

## Ascension Requirements

To ascend, a player must:
1. Be Rank Z (Pharaoh)
2. Pay the Ascension cost (see below — **TO BE CONFIGURED**)
3. Confirm the irreversible action in the Ascension GUI

### Ascension Cost (Recommended — not yet in config)

| Ascension # | Relic Cost | Coin Cost | Notes |
|---|---|---|---|
| 1 | 10,000 | 100,000,000 | First ascension — achievable in ~2 weeks |
| 2 | 15,000 | 120,000,000 | Slightly steeper |
| 3 | 20,000 | 140,000,000 | Mid-tier |
| 4 | 25,000 | 160,000,000 | — |
| 5 | 30,000 | 180,000,000 | — |
| 6 | 40,000 | 200,000,000 | — |
| 7 | 50,000 | 225,000,000 | — |
| 8 | 60,000 | 250,000,000 | — |
| 9 | 75,000 | 300,000,000 | Near-max |
| 10 | 100,000 | 500,000,000 | Final Ascension — very high bar |

> **These values are not in config yet.** Add to `plugin-prestige/config.yml` as:
> ```yaml
> ascension-costs:
>   1: {tokens: 10000, coins: 100000000}
>   2: {tokens: 15000, coins: 120000000}
>   ...
> ```
> The code in `plugin-prestige` needs to be extended to read and enforce this. (Phase 11)

---

## What Ascension Does

### Resets
- Mine rank: back to A (Slave)
- Coin balance: **NOT reset** (player keeps their Coins)
- Relic/Token balance: **NOT reset** (player keeps Relics)
- Khopesh enchants: **NOT reset** (permanent investment)
- Inventory: **NOT cleared**

### Grants
- Ascension number increments (+1)
- Ascension title prefix (chat, scoreboard)
- Permanent statistical bonus (see table below)
- Prestige Shop points (+10 per Ascension)
- Cosmetic unlock if milestone Ascension (1, 5, 10)

---

## Ascension Titles and Bonuses

| # | Title | Chat Prefix | Permanent Bonus | Milestone Cosmetic |
|---|---|---|---|---|
| 1 | First Awakening | `<light_purple>[Awakened I]</light_purple>` | +5% Relic earn from all sources | — |
| 2 | Second Awakening | `<light_purple>[Awakened II]</light_purple>` | +5% Relic earn, +5% sell speed | — |
| 3 | Third Awakening | `<light_purple>[Awakened III]</light_purple>` | +10% Relic earn, +5% sell speed | — |
| 4 | Soul of the Sands | `<gold>[Sand Soul]</gold>` | +2% sell multiplier added to base | — |
| 5 | Heart Weighed | `<gold>[Weighed]</gold>` | Mine access shortcut: Tomb of Osiris at Rank W–1 | Ra's Eye particle |
| 6 | Passed Judgment | `<gold>[Judged]</gold>` | +15% Relic earn total (cumulative) | — |
| 7 | Blessed of Osiris | `<gold>[Osiris-Blessed]</gold>` | Mine access shortcut: Tomb of Anubis at Rank Y–1 | Osiris Glow |
| 8 | Child of Ra | `<color:#FFD700>[Ra's Child]</color>` | +20% Relic earn total (cumulative) | Ra's Crown cosmetic tag |
| 9 | Eye of Horus | `<color:#FFD700>[Eye of Horus]</color>` | +5% all-currency multiplier | Horus Wings particle |
| 10 | Living God | `<color:#FFD700><bold>[Living God]</bold></color>` | Max title. No further resets. | Golden Pharaoh cosmetic set |

> **Implementation note:** Bonuses marked "% Relic earn" and "% sell multiplier" need
> to be implemented in the relevant plugin code. The prestige config currently only
> supports `+2% token-multiplier-per-prestige` (a flat multiplier, not tiered bonuses).
> Phase 11 should audit what's actually implemented and what needs code work.

---

## Ascension Chat Format

Full display when a player has both an Ascension title and a Mine Rank:

```
<light_purple>[Awakened III]</light_purple> <dark_gray>[<gold>Pharaoh</gold>]</dark_gray> PlayerName<gray>:</gray> message
```

When in a Dynasty:
```
<gold>[Dynasty: House of Ra]</gold> <light_purple>[Awakened III]</light_purple> <dark_gray>[<gold>Pharaoh</gold>]</dark_gray> PlayerName<gray>:</gray> message
```

---

## Value Proposition For Players

**Why would a player choose to Ascend?**

1. **The enchants remain.** A player with a fully enchanted Khopesh who Ascends is back
   at Slave rank, but can mine 10× faster than a new player. The next rank-up cycle
   takes a fraction of the time.

2. **Permanent statistical bonuses.** Each Ascension adds real power. +5% Relics sounds
   small, but after 10 Ascensions, the compounding effect is significant.

3. **Cosmetics are visible status.** The `[Living God]` prefix is a server-wide flex.
   Other players see it and ask "how do I get that?" — viral recruitment.

4. **Mine access shortcuts.** Ascensions 5 and 7 unlock top-tier mines one rank early.
   This is a real efficiency gain for repeat playthrough.

5. **Prestige Shop.** 10 points per Ascension compounds. At 10 Ascensions (100 points),
   a player can max both prestige shop upgrade trees.

---

## Broadcast and Sound

### On Ascension
**Title to ascending player:**
```
Title:    <light_purple>✦ ASCENSION
Subtitle: <gray>Your soul has been judged and found worthy. You are reborn.
```

**Server broadcast:**
```
<light_purple>✦ <player> has Ascended to <title>! The gods acknowledge their worth.
```

**Sound:** `minecraft:entity.ender_dragon.death` at low volume — dramatic and unique.

---

## Prestige Shop (Connected System)

10 Prestige Points per Ascension. Spend at `/prestige shop`.

| Upgrade | Tier 1 | Tier 2 | Tier 3 | Point Cost |
|---|---|---|---|---|
| Mine Profit | +5% sell | +10% sell | +20% sell | 5 / 15 / 30 |
| Relic Mastery | +10% Relics | +20% Relics | +40% Relics | 5 / 15 / 30 |

First 10 Ascensions = 100 points → enough to max both trees at the top tier.

---

## Implementation Status

| Feature | Status | Notes |
|---|---|---|
| Prestige level tracking | IMPLEMENTED | `plugin-prestige` |
| `+2% token multiplier per prestige` | IMPLEMENTED | Config key `token-multiplier-per-prestige` |
| Prestige broadcast | IMPLEMENTED | Config key `broadcast-message` |
| Prestige prefix format | IMPLEMENTED | Config key `prefix-format` |
| Tiered Ascension costs (Coins + Relics) | **NOT IMPLEMENTED** | Add to config + code (Phase 11) |
| Tiered Ascension bonuses (per title) | **PARTIALLY** | Only flat multiplier implemented |
| Milestone cosmetic unlocks | **NOT IMPLEMENTED** | Need cosmetics config update (Phase 8) |
| Mine access shortcuts at Ascension 5/7 | **NOT IMPLEMENTED** | Phase 8 work |

---

*See `ENDGAME_DESIGN.md` for how Ascension fits into the full end-game experience.*
*See `ECONOMY_BALANCE_TODO.md` for the Ascension cost config items that need adding.*

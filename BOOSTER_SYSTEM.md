# BOOSTER SYSTEM

> **Purpose:** Design document for the timed booster system.
> Defines how boosters work, how they stack, and what code changes are needed.

---

## Current State

The sell streak system provides a passive multiplier (up to 1.5×) for sustained play.
There is no separate "purchased booster" system yet. This document designs it.

---

## Booster Types

### Personal Boosters (apply to one player)

| Booster | Effect | Duration Options | Source |
|---|---|---|---|
| Ra's Blessing | +50% sell multiplier | 7 days, 30 days | Tebex |
| Thoth's Wisdom | +50% Relic earn | 7 days, 30 days | Tebex |
| Ra's Blessing (crate) | +25% sell multiplier | 24 hours | Crate reward |
| Thoth's Wisdom (crate) | +25% Relic earn | 24 hours | Crate reward |

### Server-Wide Boosters (apply to all players)

| Booster | Effect | Duration | Source |
|---|---|---|---|
| Server Ra's Blessing | 2× sell for all | 1–2 hours | Tebex purchase |
| Server Thoth's Wisdom | 2× Relic earn for all | 1–2 hours | Admin event |
| Weekend Blessing | 1.5× all currency | 48 hours | Admin seasonal |

---

## Stacking Rules

Boosters are additive with the sell streak multiplier:

```
Final sell multiplier = base × sell_streak × personal_booster × server_booster × donor_bonus
```

Example: Rank Z player, 100× streak, Ra's Blessing personal, during a Server Blessing:
- Base: 1.0
- Sell streak 100×: 1.5
- Personal Ra's Blessing: 1.5
- Server Blessing: 2.0
- Donor bonus (Chosen): 1.1
- **Final: 1.0 × 1.5 × 1.5 × 2.0 × 1.1 = 4.95×**

At Tomb of Anubis (600 avg/block), 12,000 blocks/hour: **35.6M Coins/hour**

This is intentionally high during stacked events — these are peak moments that create
excitement and drive Tebex purchases. It's not a sustainable rate.

---

## Display

### Scoreboard (when booster active)
```
<gold>✦ Ra's Blessing: <white>6d 23h
<gold>◆ Thoth's Wisdom: <white>6d 23h
```

### Chat notification on sell (when personal booster active)
```
<gold>✦ Sold: 1,240 Coins  <gray>(Ra's Blessing: +50% applied)
```

### Server-wide announcement on server booster activation
```
<gold>⚡ {player} has blessed the realm with Ra's favor!
<gold>⚡ The entire server earns 2× Coins for the next 2 hours!
```
Title to all players:
```
Title:    <gold>✦ RA'S BLESSING
Subtitle: <gray>2× Coins for 2 hours — the sun god smiles upon us!
```

---

## Implementation Plan

### Data Storage
A `boosters` table stores active personal boosters:
```sql
CREATE TABLE IF NOT EXISTS boosters (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_uuid VARCHAR(36) NOT NULL,
    booster_type VARCHAR(32) NOT NULL,   -- 'sell', 'relic', 'mine_speed'
    multiplier DECIMAL(5,2) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    granted_by VARCHAR(64) NOT NULL,    -- 'tebex', 'admin', 'crate'
    INDEX idx_booster_player (player_uuid),
    INDEX idx_booster_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### Server-Wide Booster Storage
In-memory only (does not survive restarts). Could use a config flag or Redis if multi-server.
Store in a static field on the plugin class with an expiry timestamp.

### Integration Points
1. **`plugin-economy` sell handler** — Before calculating sell proceeds, query active boosters
   for the player and multiply. Also check for active server-wide booster.
2. **`plugin-tebex` webhook handler** — On purchase of a booster package, insert a row
   into the `boosters` table with the appropriate expiry.
3. **Login check** — On player login, inform them of any active personal boosters.
4. **Scoreboard** — Display active booster timers.

### Admin Commands
```
/booster give <player> sell <multiplier> <duration>
/booster give <player> relic <multiplier> <duration>
/booster server sell <multiplier> <duration>
/booster list <player>
/booster remove <player> <id>
```

---

## Plugin Ownership

The booster system should live in `plugin-economy` or `plugin-events`.

**Recommendation: `plugin-economy`** — It's the plugin that handles all sell calculations.
The booster multiplier is applied in the sell path, so collocating the booster storage
and retrieval there keeps the logic together.

---

## Phase 8 Work Required

The booster system does not currently exist in code. To implement:

1. Add `boosters` table to `DatabaseManager` in `core-database`
2. Create `BoosterManager` class in `plugin-economy`
3. Wire booster multiplier into sell calculation
4. Wire Relic booster into all Relic-earning paths
5. Add display to scoreboard
6. Add admin commands
7. Wire Tebex delivery commands once `plugin-tebex` is complete

**Estimated complexity: MEDIUM** (2–4 days of development work)

---

*See `TEBEX_PRODUCT_MAP.md` for the exact Tebex package commands.*
*See `MONETIZATION_PLAN.md` for how boosters fit the overall revenue model.*

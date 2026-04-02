# DONOR RANKS

> **Purpose:** Complete perk list for each of the 4 donor ranks.
> This document defines exactly what each rank includes — for the Tebex store,
> the in-game donor perk GUI, and buyer setup documentation.

---

## Overview

Four donor ranks form a parallel progression track alongside the main mine rank ladder.
Donor ranks do not replace mine ranks — a Devotee who is mine rank Slave is still Slave.
The donor prefix is displayed alongside the mine rank prefix in chat.

### Chat Display
```
[Devotee] [Slave] PlayerName: message
```
Donor prefix appears first, then mine rank.

### Donor Rank Colors
| Rank | Egyptian Name | Color | MiniMessage |
|---|---|---|---|
| Donor | Devotee | Aqua | `<aqua>[Devotee]</aqua>` |
| DonorPlus | Acolyte | Blue | `<blue>[Acolyte]</blue>` |
| Elite | High Priest | Light Purple | `<light_purple>[High Priest]</light_purple>` |
| ElitePlus | Pharaoh's Chosen | Gold (bold) | `<gold><bold>[Chosen]</bold></gold>` |

---

## Devotee (Donor)

**Price:** $50 lifetime / $15/month
**Permission node:** `prison.donor.devotee`

### Mine Access
- **Sanctum of Amun** — mixed ore mine (Gold, Iron, Diamond)
- Session limit: **30 minutes per visit**
- After session expires: teleported out, 5-minute cooldown before re-entry

### Commands
- `/fly` — Creative flight **in donor mine only** (configurable permission zone)
- `/nick` — Set a nickname (cosmetic prefix, no color codes)

### Economy Perks
- Sell streak timeout: **90 seconds** (vs 60s default)
- `/sell` in donor mine has an additional **+10% sell bonus**

### Cosmetic Perks
- `[Devotee]` chat prefix in aqua
- Access to **3 exclusive cosmetic tags** (Devotee tag set)
- Unique join announcement: `<aqua>✦ [Devotee] PlayerName has entered the realm.`

### QoL Perks
- 2 `/sethome` slots (vs 1 default)
- Priority queue position when server is full
- No cooldown on `/warp`

---

## Acolyte (DonorPlus)

**Price:** $80 lifetime / $25/month
**Permission node:** `prison.donor.acolyte`
**Includes all Devotee perks, plus:**

### Mine Access
- All Devotee mines
- **Sanctum of Hathor** — high-value mine (Diamond, Emerald, Gold)
- Session limit: **45 minutes per visit**

### Commands
- `/fly` — in donor mines and personal plots (if plots system exists)
- `/nick` — with color codes (full MiniMessage color support)
- `/hat` — Wear any block as a hat (cosmetic only)

### Economy Perks
- Sell streak timeout: **120 seconds**
- Sell bonus in donor mines: **+15%**

### Cosmetic Perks
- `[Acolyte]` chat prefix in blue
- Access to **6 exclusive cosmetic tags** (Acolyte tag set)
- Unique particle effect: "Sand Storm" (particles trail while walking)
- Unique death message: `<blue>[Acolyte] PlayerName has returned to the sands.`

### QoL Perks
- 4 `/sethome` slots
- `/workbench` — Open a crafting table anywhere
- Priority queue position (higher than Devotee)

---

## High Priest (Elite)

**Price:** $120 lifetime / $45/month
**Permission node:** `prison.donor.highpriest`
**Includes all Acolyte perks, plus:**

### Mine Access
- All lower-tier donor mines
- **Hall of the Pharaoh** — premium mine (Emerald, Netherite, Diamond)
- Session limit: **60 minutes per visit**

### Commands
- `/fly` — in all donor mines and anywhere in the hub/lobby
- `/feed` — Restore hunger (1-minute cooldown)
- `/heal` — Restore health (5-minute cooldown)
- `/ptime` — Set personal time of day

### Economy Perks
- Sell streak timeout: **150 seconds**
- Sell bonus in donor mines: **+20%**
- **5% bonus on ALL sells** (applies everywhere, not just donor mines)

### Cosmetic Perks
- `[High Priest]` chat prefix in light purple
- Access to **10 exclusive cosmetic tags** (High Priest tag set)
- Custom join particle: Anubis-themed burst on entry
- Unique boss bar on login: `<light_purple>High Priest PlayerName has entered the Pharaoh's realm.`
- Pet: Tamed cat (Egyptian cat, follows player in hub)

### QoL Perks
- 8 `/sethome` slots
- `/enderchest` — Open ender chest anywhere
- Priority queue (highest standard position)
- Chat color: messages appear in white (vs gray for non-donors)

---

## Pharaoh's Chosen (ElitePlus)

**Price:** $175 lifetime / $70/month
**Permission node:** `prison.donor.chosen`
**Includes all High Priest perks, plus:**

### Mine Access
- All lower-tier donor mines
- **Chamber of the Gods** — maximum yield mine (best block mix)
- Session limit: **Unlimited**
- **+10% sell bonus on all mine income** (stacks with other bonuses)

### Commands
- `/fly` — everywhere on the server (except PvP arenas)
- `/god` — Invulnerability in hub and mines (not in PvP zones)
- `/speed` — Movement speed boost (configurable level)
- `/disguise` — Mob disguise cosmetic (if supported)

### Economy Perks
- Sell streak timeout: **180 seconds**
- **10% bonus on ALL sells** (best available)
- **5% bonus Relic earn from all sources**
- Doubled prestige shop points per Ascension (20 instead of 10)

### Cosmetic Perks
- `<gold><bold>[Chosen]</bold></gold>` chat prefix in bold gold
- Access to **all cosmetic tags** (including exclusive Chosen tags)
- **Golden Pharaoh cosmetic set:** Halo particle + golden trail + custom armor stand
- Server-wide announcement on first login of the day: `<gold>⚡ The Pharaoh's Chosen, PlayerName, has graced the realm.`
- Custom join sound: plays `minecraft:block.bell.use` to all online players on their join
- Chat messages in bold gold (optional toggle)

### QoL Perks
- Unlimited `/sethome` slots
- `/back` — Teleport to previous location after death
- `/ci` — Clear inventory (useful after mining sessions)
- Access to a Chosen-only Discord channel (buyer configures)
- Private support channel — direct contact with server admin team

---

## Donor Permission Node Hierarchy

Nodes are cumulative:

```
prison.donor.chosen
  ├── prison.donor.highpriest
  │     ├── prison.donor.acolyte
  │     │     └── prison.donor.devotee
  │     │           └── (all base perms)
```

In `plugin-donor`, the `isDonor(player)` check returns true for any donor rank.
The `getDonorRank(player)` method returns the specific rank for tier-gated features.

---

## How Donor Perks Are Enforced

| Perk Type | Enforced By |
|---|---|
| Donor mine session timer | `plugin-mines` (DonorSession tracking) |
| Donor mine access gate | Mine `permission-node` in mines config |
| Sell bonuses | `plugin-economy` (checks donor rank on sell) |
| Command permissions | `core-permissions` (plugin-donor registers nodes) |
| Chat prefix | `plugin-chat` (reads `DonorAPI.getDonorRankDisplay()`) |
| Cosmetic unlocks | `plugin-cosmetics` (checks donor rank for unlock gate) |
| Queue priority | Paper's queue mechanism (external plugin or custom) |

---

## Donor Rank Management Commands (Admin)

```
/donor grant <player> <devotee|acolyte|highpriest|chosen>
/donor revoke <player>
/donor check <player>
/donor list
```

> Verify these commands exist in `plugin-donor`. If not, they may need to be added (Phase 12).

---

*See `TEBEX_PRODUCT_MAP.md` for exact Tebex package setup.*
*See `MONETIZATION_PLAN.md` for pricing strategy and revenue projections.*

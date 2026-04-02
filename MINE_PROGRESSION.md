# MINE PROGRESSION

> **Purpose:** Complete reference for all mine tiers, block compositions, income rates,
> and access requirements. Sourced from `plugin-mines/config.yml` and
> `plugin-economy/config.yml` sell prices.

---

## Overview

Mines are organized into three categories:
1. **Standard Mines** — Open to any player with the required rank (A–Z)
2. **Donor Mines** — Require a paid donor rank (Devotee/Acolyte/High Priest/Chosen)
3. **Dynasty Mine** — Exclusive to Dynasty (Gang) members, shared mine

All mines auto-reset on a configurable timer (15 min default) or when 80% of blocks
are mined, whichever comes first.

---

## Standard Mines — Full Table

Block compositions from `plugin-mines/config.yml`. Sell prices from `plugin-economy/config.yml`.

### Pit of Souls (Rank A — Starter Mine)
| Block | % | Sell Price |
|---|---|---|
| Stone | 100% | 3/block |

**Average sell/block:** 3.0 Coins
**Purpose:** Get players started. Boring on purpose — motivates ranking up.

---

### Mine A — Tomb of Aten (Rank B: Serf)
| Block | % | Sell Price |
|---|---|---|
| Stone | 63% | 3 |
| Coal Ore | 37% | 8 |

**Average sell/block:** ~4.9 Coins
**Improvement over Pit:** +63% average value
**Deity:** Aten — the sun disk; symbol of raw energy and early power

---

### Mine B — Tomb of Thoth (Rank C: Laborer)
| Block | % | Sell Price |
|---|---|---|
| Stone | 56% | 3 |
| Coal Ore | 29% | 8 |
| Iron Ore | 15% | 15 |

**Average sell/block:** ~6.9 Coins
**Deity:** Thoth — god of knowledge and writing; the first step toward wisdom

---

### Mine C — Tomb of Thoth (upper) (Rank D: Craftsman)
| Block | % | Sell Price |
|---|---|---|
| Stone | 48% | 3 |
| Coal Ore | 24% | 8 |
| Iron Ore | 20% | 15 |
| Andesite | 8% | 6 |

**Average sell/block:** ~7.9 Coins
**Note:** Same deity as Mine B — Ranks C and D share the Thoth mine tier in lore.

---

### Mine E — Tomb of Sobek (Rank E: Merchant)
| Block | % | Sell Price |
|---|---|---|
| Stone | 50% | 3 |
| Iron Ore | 25% | 15 |
| Gold Ore | 25% | 35 |

**Average sell/block:** ~14.0 Coins
**Milestone:** Gold appears for the first time. Nearly 2× average value jump.
**Deity:** Sobek — the crocodile god of the Nile; power and cunning

---

### Mine G — Tomb of Hapi (Rank G: Soldier)
| Block | % | Sell Price |
|---|---|---|
| Stone | 40% | 3 |
| Gold Ore | 30% | 35 |
| Lapis Ore | 20% | 50 |
| Redstone Ore | 10% | 45 |

**Average sell/block:** ~24.1 Coins
**Deity:** Hapi — god of the Nile flood; abundance and prosperity

---

### Mine I — Tomb of Sekhmet (Rank I: Embalmer)
| Block | % | Sell Price |
|---|---|---|
| Stone | 30% | 3 |
| Gold Ore | 30% | 35 |
| Lapis Ore | 25% | 50 |
| Redstone Ore | 15% | 45 |

**Average sell/block:** ~29.5 Coins
**Deity:** Sekhmet — the lioness war goddess; ferocity and transformation

---

### Mine K — Tomb of Bastet (Rank K: Physician)
| Block | % | Sell Price |
|---|---|---|
| Stone | 20% | 3 |
| Gold Ore | 30% | 35 |
| Diamond Ore | 30% | 120 |
| Lapis Ore | 20% | 50 |

**Average sell/block:** ~57.6 Coins
**Milestone:** Diamond appears. Nearly 2× jump again.
**Deity:** Bastet — the cat goddess of home and fertility; grace and power combined

---

### Mine M — Tomb of Ptah (Rank M: Nomarch)
| Block | % | Sell Price |
|---|---|---|
| Stone | 15% | 3 |
| Gold Ore | 20% | 35 |
| Diamond Ore | 40% | 120 |
| Emerald Ore | 15% | 300 |
| Lapis Ore | 10% | 50 |

**Average sell/block:** ~103.5 Coins
**Milestone:** Emerald appears. Major income jump.
**Deity:** Ptah — the craftsman god and creator deity; mastery of creation

---

### Mine O — Tomb of Seth (Rank O: Sage)
| Block | % | Sell Price |
|---|---|---|
| Stone | 10% | 3 |
| Diamond Ore | 20% | 120 |
| Emerald Ore | 50% | 300 |
| Gold Ore | 20% | 35 |

**Average sell/block:** ~181.3 Coins
**Deity:** Seth — god of chaos and storms; dangerous power for the brave

---

### Mine Q — Tomb of Isis (Rank Q: Oracle)
| Block | % | Sell Price |
|---|---|---|
| Stone | 5% | 3 |
| Diamond Ore | 30% | 120 |
| Emerald Ore | 50% | 300 |
| Netherite Scrap | 15% | 600 |

**Average sell/block:** ~242.2 Coins
**Milestone:** Netherite Scrap first appears.
**Deity:** Isis — goddess of magic and healing; the most powerful goddess

---

### Mine S — Tomb of Horus (Rank S: Anubite)
| Block | % | Sell Price |
|---|---|---|
| Diamond Ore | 40% | 120 |
| Emerald Ore | 40% | 300 |
| Netherite Scrap | 20% | 600 |

**Average sell/block:** ~288.0 Coins
**Deity:** Horus — the sky falcon god; symbol of divine kingship

---

### Mine U — Tomb of Ra (Rank U: Champion)
| Block | % | Sell Price |
|---|---|---|
| Diamond Ore | 30% | 120 |
| Emerald Ore | 30% | 300 |
| Netherite Scrap | 40% | 600 |

**Average sell/block:** ~366.0 Coins
**Deity:** Ra — the supreme sun god; peak of divine power in mortal realm

---

### Mine W — Tomb of Osiris (Rank W: Immortal)
| Block | % | Sell Price |
|---|---|---|
| Emerald Ore | 20% | 300 |
| Netherite Scrap | 30% | 600 |
| Ancient Debris | 50% | 600 |

**Average sell/block:** ~480.0 Coins
**Note:** Ancient Debris and Netherite Scrap currently share the same sell price (600).
See `ECONOMY_BALANCE_TODO.md` item #8 for recommended adjustment.
**Deity:** Osiris — god of the afterlife and resurrection; death and rebirth

---

### Mine Y — Tomb of Anubis (Rank Y: Ascendant)
| Block | % | Sell Price |
|---|---|---|
| Ancient Debris | 100% | 600 |

**Average sell/block:** 600.0 Coins
**This is the crown jewel mine. Pure Ancient Debris. Maximum income.**
**At max enchants + 100× sell streak: ~28.8M+ Coins/hour**
**Deity:** Anubis — god of death and judgment; the final arbiter

---

## Income Summary Table

| Mine | Rank Req. | Avg/Block | Index (vs Pit of Souls) |
|---|---|---|---|
| Pit of Souls | A | 3.0 | 1× |
| Tomb of Aten | B | 4.9 | 1.6× |
| Tomb of Thoth (B) | C | 6.9 | 2.3× |
| Tomb of Thoth (C) | D | 7.9 | 2.6× |
| Tomb of Sobek | E | 14.0 | 4.7× |
| Tomb of Hapi | G | 24.1 | 8.0× |
| Tomb of Sekhmet | I | 29.5 | 9.8× |
| Tomb of Bastet | K | 57.6 | 19.2× |
| Tomb of Ptah | M | 103.5 | 34.5× |
| Tomb of Seth | O | 181.3 | 60.4× |
| Tomb of Isis | Q | 242.2 | 80.7× |
| Tomb of Horus | S | 288.0 | 96.0× |
| Tomb of Ra | U | 366.0 | 122.0× |
| Tomb of Osiris | W | 480.0 | 160.0× |
| Tomb of Anubis | Y | 600.0 | 200.0× |

The Tomb of Anubis generates 200× more Coins per block than the Pit of Souls.
This progression feels very satisfying to players as the grind accelerates.

---

## Donor Mines

| Mine Name | Donor Rank | Session Limit | Composition |
|---|---|---|---|
| Sanctum of Amun | Devotee | 30 min | Gold, Iron, Diamond mix (~57/block avg) |
| Sanctum of Hathor | Acolyte | 45 min | Diamond, Emerald, Gold (~180/block avg) |
| Hall of the Pharaoh | High Priest | 60 min | Emerald, Netherite, Diamond (~350/block avg) |
| Chamber of the Gods | Pharaoh's Chosen | Unlimited | Premium mix, +10% sell bonus (~500/block avg) |

> Donor mine compositions are not in config yet (mines are stubs with placeholder coords).
> Values above are targets. Verify and configure in Phase 8.

---

## Dynasty Mine

A shared mine accessible only to Dynasty (Gang) members. Block composition scales
with Dynasty level/upgrades (if this system is implemented).

**Default composition:** Similar to a mid-tier standard mine (Tomb of Bastet range).
The Dynasty Mine is a shared resource — all members benefit from upgrades.

---

## Mine Reset System

From config:
- `reset-batch-size: 1000` — blocks reset per tick (smooth, avoids TPS spikes)
- `reset-timer-mins: 15` — default auto-reset interval
- `reset-threshold: 0.80` — also resets when 80% of blocks are mined
- Broadcast to mine: `<gold>[Mine {mine}]</gold> <gray>Mine is resetting — stand clear!`

Players are teleported out automatically during reset if they're inside the mine.

---

## Config Locations

- Mine definitions: `plugin-mines/src/main/resources/config.yml`
- Mine admin commands: `/mine` (see `ADMIN_TOOLS.md`)
- Block sell prices: `plugin-economy/src/main/resources/config.yml`
- Permission nodes: `prison.mine.[a-z]` in `core-permissions`

---

## Phase 6 Action Items

- [ ] Update all mine `display:` values to Egyptian tomb names
- [ ] Configure actual coordinates for all 26 standard mines (world build required — Phase 14)
- [ ] Configure donor mine compositions and coordinates
- [ ] Verify `mine-type: DONOR` is properly set for donor mines
- [ ] Add Egyptian tomb entrance descriptions to mine GUI lore

---

*See `PROGRESSION_MAP.md` for the rank → mine unlock mapping table.*
*See `ECONOMY_DESIGN.md` for income rate analysis per mine tier.*

# PROGRESSION MAP

> **Purpose:** Complete reference chart of every rank, mine unlock, and prestige tier.
> Cross-reference with `PRODUCT_IDENTITY.md` for Egyptian names and
> `ECONOMY_DESIGN.md` for cost validation.

---

## Rank Ladder — Complete Table

| # | Letter | Egyptian Name | Rank-Up Cost | Cumulative Cost | Mine Unlocked | Kit Eligible |
|---|---|---|---|---|---|---|
| 1 | A | Slave | — (start) | 0 | Pit of Souls | starter |
| 2 | B | Serf | 5,000 | 5,000 | Tomb of Aten | — |
| 3 | C | Laborer | 15,000 | 20,000 | Tomb of Thoth | — |
| 4 | D | Craftsman | 35,000 | 55,000 | Tomb of Thoth | — |
| 5 | E | Merchant | 75,000 | 130,000 | Tomb of Sobek | rankE kit |
| 6 | F | Scribe | 150,000 | 280,000 | Tomb of Sobek | — |
| 7 | G | Soldier | 275,000 | 555,000 | Tomb of Hapi | — |
| 8 | H | Officer | 450,000 | 1,005,000 | Tomb of Hapi | — |
| 9 | I | Embalmer | 700,000 | 1,705,000 | Tomb of Sekhmet | — |
| 10 | J | Architect | 1,000,000 | 2,705,000 | Tomb of Sekhmet | rankJ kit |
| 11 | K | Physician | 1,500,000 | 4,205,000 | Tomb of Bastet | — |
| 12 | L | Vizier | 2,250,000 | 6,455,000 | Tomb of Bastet | — |
| 13 | M | Nomarch | 3,500,000 | 9,955,000 | Tomb of Ptah | — |
| 14 | N | High Priest | 5,000,000 | 14,955,000 | Tomb of Ptah | — |
| 15 | O | Sage | 7,500,000 | 22,455,000 | Tomb of Seth | rankO kit |
| 16 | P | Sorcerer | 11,000,000 | 33,455,000 | Tomb of Seth | — |
| 17 | Q | Oracle | 16,000,000 | 49,455,000 | Tomb of Isis | — |
| 18 | R | Djed | 23,000,000 | 72,455,000 | Tomb of Isis | — |
| 19 | S | Anubite | 33,000,000 | 105,455,000 | Tomb of Horus | — |
| 20 | T | Horus-Born | 48,000,000 | 153,455,000 | Tomb of Horus | rankT kit |
| 21 | U | Champion | 70,000,000 | 223,455,000 | Tomb of Ra | — |
| 22 | V | Demigod | 100,000,000 | 323,455,000 | Tomb of Ra | — |
| 23 | W | Immortal | 150,000,000 | 473,455,000 | Tomb of Osiris | — |
| 24 | X | Reborn | 225,000,000 | 698,455,000 | Tomb of Osiris | — |
| 25 | Y | Ascendant | 350,000,000 | 1,048,455,000 | Tomb of Anubis | — |
| 26 | Z | Pharaoh | 500,000,000 | 1,548,455,000 | Tomb of Anubis | rankZ kit |

**Total cost Slave → Pharaoh: ~1.548 billion Coins** (actual from `plugin-ranks/config.yml`)

> These are the live values from config. See `ECONOMY_DESIGN.md` for analysis of whether
> these numbers produce the intended 2–4 week progression timeline for dedicated players.

---

## Mine Progression — Block Composition

Each mine resets periodically (configurable, default 5 minutes or when X% mined).

| Mine Name | Rank Required | Reset Interval | Block Mix |
|---|---|---|---|
| Pit of Souls | A (Slave) | 3 min | 100% Stone |
| Tomb of Aten | B (Serf) | 4 min | 80% Stone, 20% Coal Ore |
| Tomb of Thoth | C (Laborer) | 5 min | 60% Stone, 30% Coal, 10% Iron |
| Tomb of Sobek | E (Merchant) | 5 min | 50% Stone, 25% Iron, 25% Gold |
| Tomb of Hapi | G (Soldier) | 5 min | 40% Stone, 30% Gold, 20% Lapis, 10% Redstone |
| Tomb of Sekhmet | I (Embalmer) | 6 min | 30% Stone, 30% Gold, 25% Lapis, 15% Redstone |
| Tomb of Bastet | K (Physician) | 6 min | 20% Stone, 30% Gold, 30% Diamond, 20% Lapis |
| Tomb of Ptah | M (Nomarch) | 7 min | 15% Stone, 20% Gold, 40% Diamond, 15% Emerald, 10% Lapis |
| Tomb of Seth | O (Sage) | 7 min | 10% Stone, 20% Diamond, 50% Emerald, 20% Gold |
| Tomb of Isis | Q (Oracle) | 8 min | 5% Stone, 30% Diamond, 50% Emerald, 15% Netherite Scrap |
| Tomb of Horus | S (Anubite) | 8 min | 40% Diamond, 40% Emerald, 20% Netherite Scrap |
| Tomb of Ra | U (Champion) | 9 min | 30% Diamond, 30% Emerald, 40% Netherite Scrap |
| Tomb of Osiris | W (Immortal) | 10 min | 20% Emerald, 30% Netherite Scrap, 50% Ancient Debris |
| Tomb of Anubis | Y (Ascendant) | 10 min | 100% Ancient Debris (Pure high-value) |

> Ancient Debris sells for the highest price. The Tomb of Anubis is the pinnacle.
> Actual sell prices per block are in `plugin-economy/src/main/resources/config.yml`.

---

## Donor Mine Access

Donor mines are separate from the rank ladder — accessible by donor rank, not mine rank.

| Mine Name | Required Donor Rank | Session Limit | Block Mix |
|---|---|---|---|
| Sanctum of Amun | Devotee (Donor) | 30 min/session | Mixed mid-tier ores |
| Sanctum of Hathor | Acolyte (DonorPlus) | 45 min/session | Diamond, Emerald, Gold |
| Hall of the Pharaoh | High Priest (Elite) | 60 min/session | Emerald, Netherite, Diamond |
| Chamber of the Gods | Pharaoh's Chosen (ElitePlus) | Unlimited | Premium mix with bonus sell rate |

> Donor mine session timer: 30 min default (configurable per mine in `plugin-mines/config.yml`).
> Admin bypass permission: `prison.admin.*` skips session checks.

---

## Ascension (Prestige) Ladder

Ascension requires: Pharaoh rank (Z) + Relic cost (configurable).

| Ascension # | Name | Chat Title | Permanent Bonus |
|---|---|---|---|
| 0 | (none) | — | Baseline |
| 1 | First Awakening | `[Awakened I]` | +5% Relic earn from sells |
| 2 | Second Awakening | `[Awakened II]` | +5% Relic earn, +10% sell speed |
| 3 | Third Awakening | `[Awakened III]` | +10% Relic earn, +10% sell speed |
| 4 | Soul of the Sands | `[Sand Soul]` | +2% sell multiplier bonus per rank tier |
| 5 | Heart Weighed | `[Weighed]` | Access to Tomb of Osiris without Immortal rank |
| 6 | Passed Judgment | `[Judged]` | +15% Relic earn from all sources |
| 7 | Blessed of Osiris | `[Osiris-Blessed]` | Access to Tomb of Anubis without Ascendant rank |
| 8 | Child of Ra | `[Ra's Child]` | +20% Relic earn, exclusive cosmetic: Ra's Crown tag |
| 9 | Eye of Horus | `[Eye of Horus]` | +5% all-currency multiplier (stacks with everything) |
| 10 | Living God | `[Living God]` | Max Ascension — exclusive cosmetic: Godly particle halo |

> **Note:** Ascension bonuses above (Relic earn %, sell speed, mine access) are the
> *design intent*. Verify which bonuses are implemented in code vs. which need Phase 8 work.
> The cosmetic rewards (tags, particles) are defined here and should be configured in
> `plugin-cosmetics` config.

---

## Khopesh Enchant Progression

Players invest Relics into Khopesh enchantments. Suggested progression order:

| Priority | Enchant (Egyptian Name) | Recommended Level | Relic Cost (total) |
|---|---|---|---|
| 1st | Blessing of Shu (Speed) | Max | Low |
| 2nd | Anubis' Speed (Efficiency) | Level 3–5 | Medium |
| 3rd | Hand of Ptah (Vein Miner) | Level 2–3 | Medium |
| 4th | Relic Sight (Token/Relic Finder) | Level 1–2 | Low |
| 5th | Forge of Ptah (Auto-Smelt) | Max | Medium |
| 6th | Eye of Ra (Laser) | Level 2+ | High |
| 7th | Luck of Thoth (Fortune) | Max | High |
| 8th | Wrath of Seth (Explosion) | Level 2+ | High |
| Later | All remaining enchants | Variable | High |

> This "suggested order" should appear in the Khopesh enchant GUI as a
> "Recommended Path" lore hint on each enchant. New players are often lost in this menu.

---

## Command Unlocks by Rank

Some commands/features unlock at specific ranks. Full list:

| Rank | Feature Unlocked |
|---|---|
| A (Slave) | `/sell`, `/quests`, `/kit starter`, `/ranks`, `/mines` |
| B (Serf) | Tomb of Aten access |
| E (Merchant) | `/ah` (Auction House), Kit rankE |
| G (Soldier) | Dynasty (Gang) creation |
| J (Architect) | Kit rankJ, higher Khopesh enchant tiers |
| N (High Priest) | `/prestige` command visible (can't use until Z) |
| O (Sage) | Kit rankO |
| T (Horus-Born) | Kit rankT |
| Z (Pharaoh) | `/prestige ascend` active, Kit rankZ, Pharaoh cosmetic tag |

---

## Key Progression Walls (Intentional)

These are points where the game naturally slows down. They are intentional design choices,
not bugs. Document them so buyers understand and don't accidentally "fix" them.

| Wall | Where | Why It Exists |
|---|---|---|
| First wall | Rank G (100K cost) | Separates casual from invested players |
| Second wall | Rank K (2M cost) | Requires Khopesh investment to grind efficiently |
| Third wall | Rank O (25M cost) | This is mid-game. Requires dedicated sessions. |
| Big wall | Rank S (100M cost) | End-game approaching. Dynasty activity helps here. |
| Final push | Ranks W–Z (180M–250M each) | The Pharaoh rank should feel like an achievement. |
| Ascension cost | After Rank Z | Relic cost should require 1–2 weeks of saving. |

---

*This document is a reference chart — it does not explain why the numbers are what they are.*
*For balance rationale, see `ECONOMY_DESIGN.md`.*
*For the player-facing experience at each stage, see `GAMEPLAY_LOOP.md`.*

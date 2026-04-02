# CURRENCY PURPOSES

> **Purpose:** Complete itemized reference for every place players earn and spend
> each currency. Use this to audit feedback loops and spot missing sinks or sources.

---

## Coins (IGC) — "The Gold of the Pharaoh"

### Sources (where Coins come from)

| Source | Rate | Notes |
|---|---|---|
| `/sell` or `/sell all` | Varies by block | Primary source. See sell prices in `plugin-economy/config.yml`. |
| Sell streak bonus | +5% to +50% | Multiplier on sell proceeds. |
| Auto Sell enchant (Khopesh) | Same as /sell | Sells inventory automatically on break. |
| Daily quests | ~1,000–50,000 | Varies by quest objective and rank. |
| Weekly quests | ~10,000–250,000 | Larger reward for longer-term goals. |
| Crate rewards | Variable | Some crates award Coins directly. |
| Events (Ra's Blessing) | 2× sell rate | Temporary multiplier — not new Coins, doubled sell proceeds. |
| Staff grant (admin command) | Admin-only | Emergency grants via `/eco give`. Logged. |

### Sinks (where Coins go)

| Sink | Amount | Notes |
|---|---|---|
| Rank-up (A → Z) | 5,000 – 500,000,000 per rank | Primary sink. Total: ~1.548B Slave → Pharaoh. |
| Dynasty upgrades | Varies | Contributes Coins to Dynasty fund. |
| Shop purchases | Varies | `/shop` — item purchases (system BETA). |
| Auction House listings | Listing fee (if configured) | True sink if fee exists. Net-zero otherwise. |
| Coinflip bets | Varies | Redistributes to winner. Net-zero (no house cut currently). |
| Prestige/Ascension cost | TBD | **Gap:** Ascension currently requires Relics only, not Coins. |

---

## Relics (Tokens) — "Ancient Power Crystals"

### Sources (where Relics come from)

| Source | Rate | Notes |
|---|---|---|
| Daily quests | ~10–100 | Smaller Relic awards mixed in with Coin rewards. |
| Weekly quests | ~50–500 | Significant Relic source for consistent players. |
| Canopic Chest (common crate) | ~10–50 | Low range — incentivizes better crates. |
| Tomb Chest (uncommon crate) | ~50–200 | Mid-range source. |
| Pharaoh's Reliquary (rare crate) | ~200–1,000 | High-value Relic source — drives Tebex. |
| Sell streak milestones | Bonus at 10×/25×/50×/100× | Small bonuses — see config for exact values. |
| Tokenator enchant | Per-block drop chance | Creates a Relic flywheel for invested players. |
| Prestige rewards | Per `plugin-prestige/config.yml` | Commands that can grant Relics. |
| Staff grant (admin command) | Admin-only | Via `/eco token give` or similar. |

### Sinks (where Relics go)

| Sink | Amount | Notes |
|---|---|---|
| Khopesh enchants | 250 – 200,000 per purchase | Primary sink. Total ~653,500 to max all enchants. |
| Prestige shop upgrades | 5/15/30 prestige points | Prestige points ≠ Relics — see prestige config. |
| Ascension cost | TBD | If Relics are used as the Ascension payment. |

---

## Prestige Points — Separate Micro-Currency

From `plugin-prestige/config.yml`:
- 10 points granted per Prestige/Ascension
- Spent in the Prestige Shop on upgrades (Mine Profit, Token Mastery tiers: 5/15/30 pts)
- This is a separate mini-currency from Relics
- Players cannot convert between Prestige Points and Relics

### Prestige Shop Upgrades (design intent, verify in code)
| Upgrade | Cost (points) | Tier 1 | Tier 2 | Tier 3 |
|---|---|---|---|---|
| Mine Profit | 5 / 15 / 30 | +5% sell | +10% sell | +20% sell |
| Token/Relic Mastery | 5 / 15 / 30 | +10% Relics | +20% Relics | +40% Relics |

---

## Economy Map (Visual)

```
PLAYERS MINE
     │
     ▼
BLOCKS BROKEN
     │
     ├──► /sell ──► COINS earned (× sell streak multiplier)
     │                     │
     │                     ├──► Rank-up cost ──► COINS destroyed ✓
     │                     ├──► Shop purchase ──► COINS to server ✓
     │                     ├──► Dynasty fund ──► COINS pooled
     │                     └──► Coinflip/AH ──► COINS redistributed ↔
     │
     └──► Tokenator enchant ──► RELICS earned (small per block)

DAILY QUESTS ──────────────────► COINS + RELICS earned
WEEKLY QUESTS ─────────────────► COINS + RELICS earned
CRATE OPENING ─────────────────► RELICS + items earned
SELL STREAK MILESTONES ─────────► RELICS earned (bonus)

RELICS EARNED
     │
     └──► Khopesh enchant purchase ──► RELICS destroyed ✓
     └──► Ascension cost (TBD) ──────► RELICS destroyed ✓

PRESTIGE/ASCEND
     │
     └──► +10 Prestige Points ──► spend in Prestige Shop ──► permanent bonuses
```

---

## Identified Gaps

| Gap | Impact | Recommendation |
|---|---|---|
| No Coin sink at max rank | After Pharaoh rank, Coins accumulate with no outlet | Add Coin component to Ascension cost |
| No house cut on Coinflip | Coins not destroyed — only redistributed | Consider 2–5% house cut that destroys Coins |
| No AH listing fee as sink | AH is net-zero for Coins | Consider 1–2% transaction fee as a mild sink |
| Prestige cost not in config | Prestige cost (Relics needed) not found in prestige/config.yml | Needs to be added before launch |
| Quest reward amounts not visible | Quest config not audited | Audit `plugin-quests/config.yml` to verify reward ranges |

---

*See `ECONOMY_DESIGN.md` for flow analysis and inflation risk assessment.*
*See `ECONOMY_BALANCE_TODO.md` for specific numbers to adjust before launch.*

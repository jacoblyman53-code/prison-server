# ECONOMY DESIGN

> **Purpose:** Analyze the economy numbers, validate the feedback loops, and document
> the design philosophy behind all currency systems.
> All figures sourced from live config files — not estimates.

---

## Philosophy

### Dual Currency Design
This server uses two currencies intentionally:

**Coins (IGC)** — the "wet" currency. Flows in fast, flows out fast.
- High volume, earned constantly, spent on rank-up walls
- Players always feel flush with Coins because they earn them with every `/sell`
- The big rank-up costs drain them just as fast — creates a satisfying treadmill

**Relics (Tokens)** — the "dry" currency. Scarce, meaningful, long-term.
- Low volume, earned from quests/crates/sell streaks
- Spent on permanent enchants — every Relic spent has lasting impact
- Scarcity makes crate purchases tempting (Relics from crates = faster enchants)

The two currencies serve different psychological roles: Coins give moment-to-moment
feedback, Relics give long-term investment satisfaction.

---

## Coin Flow Analysis

### Coin Sources (per hour estimates)

Sell prices and mine composition from config. Assumes average mining speed of
~3,000 blocks/hour for a new player, scaling to ~12,000+/hour with max enchants.

| Mine | Rank | Avg Block Value | Blocks/hr (new) | Coins/hr (new) | Coins/hr (maxed) |
|---|---|---|---|---|---|
| Pit of Souls | A | ~3 | 3,000 | ~9,000 | ~36,000 |
| Tomb of Aten | B | ~5 | 3,200 | ~16,000 | ~64,000 |
| Tomb of Thoth | C–D | ~9 | 3,500 | ~31,500 | ~126,000 |
| Tomb of Sobek | E–F | ~22 | 4,000 | ~88,000 | ~352,000 |
| Tomb of Hapi | G–H | ~27 | 4,500 | ~121,500 | ~486,000 |
| Tomb of Sekhmet | I–J | ~35 | 5,000 | ~175,000 | ~700,000 |
| Tomb of Bastet | K–L | ~70 | 5,500 | ~385,000 | ~1,540,000 |
| Tomb of Ptah | M–N | ~130 | 6,000 | ~780,000 | ~3,120,000 |
| Tomb of Seth | O–P | ~200 | 6,500 | ~1,300,000 | ~5,200,000 |
| Tomb of Isis | Q–R | ~270 | 7,000 | ~1,890,000 | ~7,560,000 |
| Tomb of Horus | S–T | ~350 | 8,000 | ~2,800,000 | ~11,200,000 |
| Tomb of Ra | U–V | ~420 | 9,000 | ~3,780,000 | ~15,120,000 |
| Tomb of Osiris | W–X | ~500 | 10,000 | ~5,000,000 | ~20,000,000 |
| Tomb of Anubis | Y–Z | ~600 | 12,000 | ~7,200,000 | ~28,800,000 |

> Block values estimated from config sell prices weighted by mine composition.
> Mining speed estimates: "new" = no enchants, "maxed" = full Khopesh.
> **These are estimates. Real measurements should be done in-game.**

### Coin Sinks
- **Rank-up costs:** Primary sink. 1.548B total Slave → Pharaoh.
- **Coinflip:** Redistributes coins, doesn't destroy them. Net-zero sink.
- **Auction House:** Redistributes coins, doesn't destroy them. Net-zero sink.
- **Dynasty upgrades:** If applicable — coins leave the player.
- **Shop purchases:** IGC spent on items.

**Problem:** The only true coin sink is rank-up. At high ranks with max enchants,
a player in the Tomb of Anubis earns ~28M coins/hour. Rank Y→Z costs 500M.
That's ~17 hours of grinding at max efficiency. This is acceptable for the final rank
but should be validated — see `ECONOMY_BALANCE_TODO.md`.

---

## Relic (Token) Flow Analysis

### Relic Sources (rough rates, not in config — needs measurement)
- **Quests:** 50–500 Relics per quest (daily and weekly)
- **Crates:** Variable — Canopic Chest: ~10–50, Tomb Chest: ~50–200, Pharaoh's Reliquary: ~200–1,000
- **Sell streak milestones:** Bonus Relics at 10x, 25x, 50x, 100x streak
- **Prestige reward:** 10 prestige shop points per prestige (spend on Relic bonuses)
- **Tokenator enchant:** Custom enchant that drops Relics from mined blocks

### Relic Sinks (total cost to max all enchants)

From `plugin-pickaxe/config.yml`:

| Enchant | Max Level | Total Cost (Relics) |
|---|---|---|
| Efficiency | 5 | 20,000 |
| Fortune | 3 | 17,000 |
| Silk Touch | 1 | 5,000 |
| Auto Sell | 3 | 12,000 |
| Speed Mine | 3 | 10,250 |
| Explosive | 5 | 265,500 |
| Laser | 5 | 49,750 |
| Tokenator | 5 | 41,000 |
| Jackpot | 3 | 23,000 |
| Nuke | 3 | 100,000 |
| Tunnel Drill | 3 | 30,500 |
| Lightning | 5 | 79,500 |
| **TOTAL** | **all max** | **~653,500 Relics** |

### Relic Economy Assessment
- 653,500 Relics to fully enchant the Khopesh
- This is a substantial long-term goal — appropriate for the prestige system
- The Tokenator enchant creates a Relic flywheel: invest Relics → earn more Relics
- The first 50,000 Relics (efficiency, fortune, silk touch, auto sell, speed) provide
  the biggest mining efficiency gains — early investment pays off fast
- The last 200,000+ Relics (explosive, nuke at high levels) are luxury purchases

---

## Sell Streak Analysis

Sell streak multipliers from config:
```
5 consecutive sells:  1.05× (5% bonus)
10 consecutive sells: 1.10× (10% bonus)
25 consecutive sells: 1.20× (20% bonus)
50 consecutive sells: 1.35× (35% bonus)
100 consecutive sells: 1.50× (50% bonus)
```
Timeout window: 60 seconds between sells to maintain streak.

### Assessment
- At 100× streak, player earns 50% more Coins per sell
- This is meaningful but not economy-breaking (it's 1.5x, not 10x)
- The 60-second timeout is generous — players can mine comfortably between sells
- Streak creates session stickiness: players won't log off mid-streak
- **Potential issue:** At max rank + max enchants + 100× streak:
  - ~28.8M coins/hr base × 1.5 = ~43M coins/hr
  - Rank Y→Z takes ~12 hours at this rate (was 17 without streak)
  - This is fine — the streak is a reward for engagement, not a bypass

---

## Coinflip Impact

- Coinflip redistributes coins between two players, net zero
- The coinflip fee (if any — check config) would be a true sink
- Without a fee, coinflip creates no inflation and no deflation
- Risk: a lucky player could accumulate wealth very fast
- Mitigation: coinflip min/max bet limits (verify in `plugin-coinflip/config.yml`)

---

## Auction House Impact

- Auction house redistributes coins, doesn't create or destroy them
- Net-zero for the economy
- Creates a "marketplace" for players to trade enchanted pickaxes, crates, etc.
- The listing fee (if any) would be a true coin sink

---

## Progression Timeline Estimate

Based on mining rates and rank costs:

| Rank | Target Hours (new player, no enchants) | Notes |
|---|---|---|
| A → E | ~2 hours | Early rush, small costs |
| E → J | ~5 hours | First meaningful wall at G |
| J → O | ~15 hours | Mid-game. Enchants critical here. |
| O → T | ~30 hours | End-game approaching. Long sessions. |
| T → Z | ~50 hours | Final push. Dedicated players only. |
| **Total A → Z** | **~100 hours** | Estimate for new player without enchants |

With max Khopesh enchants (achievable mid-game, ~2–3 weeks of Relic grinding):
- A → Z: ~30–40 hours (3.5× speed multiplier from enchants is conservative estimate)

**Assessment:** The economy is tuned for 1–3 months of active play to reach Pharaoh rank.
This is appropriate for a premium server setup. Players who spend real money on Relic crates
or donor mine access can accelerate significantly.

---

## Inflation Risk Assessment

### Low Risk: Short-term inflation
- New players start at 0. Coin entry is controlled by mine access.
- The rank-up wall absorbs coins efficiently at each tier.

### Medium Risk: Late-game inflation
- Max-rank players with max enchants generate Coins faster than they can spend them
- Once at Pharaoh rank, the primary Coin sink (rank-up) disappears
- Mitigation: Ascension (prestige) resets rank and creates a new sink
- Mitigation: Dynasty upgrades (if they cost Coins)
- **Gap:** If Ascension costs Relics but not Coins, max-rank Pharaoh players will
  accumulate Coins indefinitely. This drives coinflip and auction house activity
  but could cause price inflation on tradeable items.
- **Recommendation:** Add a Coin component to the Ascension cost, or add a Coin sink
  in the late-game (cosmetics, exclusive items, named items).

### Low Risk: Relic inflation
- 653,500 Relics to max enchants is a substantial ceiling
- Tokenator creates a flywheel but at diminishing returns
- No evidence of runaway Relic production in the current design

---

## Key Config Values to Watch

| Config | File | Current Value | Risk |
|---|---|---|---|
| sell-streak-timeout-seconds | economy/config.yml | 60s | Fine |
| Tomb of Anubis sell value (Ancient Debris) | economy/config.yml | 600/block | Monitor |
| Rank Z cost | ranks/config.yml | 500,000,000 | Fine |
| Prestige token-multiplier-per-prestige | prestige/config.yml | +2%/level | Fine |
| Explosive L5 cost | pickaxe/config.yml | 200,000 Relics | Fine — high cost is intentional |

---

*See `CURRENCY_PURPOSES.md` for a complete itemized list of all sources and sinks.*
*See `ECONOMY_BALANCE_TODO.md` for specific numbers that need review before launch.*

# ECONOMY BALANCE TODO

> **Purpose:** Specific numbers in config that need review or testing before launch.
> Each item has a current value, a concern, and a recommended action.
> Items are ordered by priority — fix the top items first.

---

## Priority 1 — Must Fix Before Launch

### 1. Prestige / Ascension Cost Not Configured
**File:** `plugin-prestige/config.yml`
**Current:** No Relic or Coin cost for prestige defined in config
**Concern:** Players can ascend for free — the system has no cost gate.
**Action:** Add a `prestige-cost-tokens: [amount]` config key. Suggested starting value:
- Ascension 1: 10,000 Relics
- Ascension 2: 20,000 Relics
- Ascension 3–5: 30,000 Relics each
- Ascension 6–10: 50,000 Relics each
**Rationale:** 10,000 Relics for first Ascension is achievable in 1–2 weeks of quests + crates.
Makes Ascension feel earned without being gated behind months of grinding.

---

### 2. No Coin Sink at Max Rank
**Concern:** After reaching Pharaoh (Rank Z), the primary Coin sink (rank-up) disappears.
Max-rank players accumulate Coins with nowhere to spend them.
**Action:** Add Coins as a component of the Ascension cost (alongside Relics).
Suggested: `prestige-cost-coins: [amount]` in prestige config.
- Ascension 1: 100,000,000 Coins (100M — one-fifth of rank Z cost)
- Scale up 20% per Ascension
**Rationale:** This gives rich end-game players a meaningful Coin drain and connects the
two progression systems.

---

### 3. Rank-Up Messages Use Generic Names
**File:** `plugin-ranks/config.yml`
**Current:** `rankup-message: "<green>You ranked up to <gold><bold>{display}</bold></gold>! Keep mining."`
**Current display values:** "Rank A", "Rank B", etc.
**Action:** Either:
  a) Update all 26 `display` values to Egyptian names (Slave, Serf, etc.) in config
  b) Or do it in Phase 6 as part of the GUI overhaul
**Phase dependency:** This should be done in Phase 6 alongside all GUI text changes.

---

### 4. `max-rank-message` References "Prestige Coming Soon"
**File:** `plugin-ranks/config.yml`
**Current:** `max-rank-message: "<gold>You are rank Z — the highest mine rank! Prestige coming soon."`
**Concern:** Prestige IS implemented. This message is stale and confusing.
**Action:** Update to: `"<gold>You have reached [Pharaoh] rank — the pinnacle of mortal power. You may now Ascend. <gray>/prestige"`

---

## Priority 2 — Fix Before Soft Launch

### 5. Rank B Cost Seems Too High for a First Wall
**File:** `plugin-ranks/config.yml`
**Current:** Rank B costs 5,000 Coins
**Concern:** At the Pit of Souls (Stone only, 3 Coins/block), a new player earns ~9,000 Coins/hr.
Rank B takes ~33 minutes of grinding. This is fine for a first wall, but the starter
experience should feel faster — first rank-up in under 10 minutes.
**Recommendation:** Consider reducing Rank B cost to 1,000–2,000 Coins, or improve the
starter mine's sell rate slightly.
**Note:** Rank A mine (Tomb of Aten) is only unlocked at Rank B — so the player grinds
stone until they can afford Rank B. First 30 minutes is pure stone. Consider a small
Coal seam in the Pit of Souls to make it more interesting.

---

### 6. Sell Streak Timeout May Be Too Tight
**File:** `plugin-economy/config.yml`
**Current:** `sell-streak-timeout-seconds: 60`
**Concern:** 60 seconds between `/sell` commands to maintain streak. Players need to
mine AND sell within 60 seconds of their last sell. With mining time factored in,
this may be stressful for keyboard players.
**Recommendation:** Test with real players. Consider 90–120 seconds for a more
comfortable streak maintenance experience. The goal is engagement, not stress.

---

### 7. Coinflip Has No House Cut
**File:** `plugin-coinflip/config.yml` (verify location)
**Current:** Unknown — no fee visible in audit
**Concern:** Without a house cut, Coinflip is purely coin redistribution. No sink.
**Recommendation:** Add a 2% house cut that is destroyed (not given to anyone). This
creates a mild late-game Coin sink and is a standard gambling-game mechanic.
**Note:** Only implement if the Coinflip config supports a fee parameter. Otherwise
log as a Phase 11 code task.

---

### 8. Ancient Debris vs Netherite Scrap Price Inconsistency
**File:** `plugin-economy/config.yml`
**Current:** `ANCIENT_DEBRIS: 600` and `NETHERITE_SCRAP: 600`
**Concern:** Ancient Debris and Netherite Scrap sell for the same price (600 each).
In vanilla, Ancient Debris is rarer and used to craft Netherite Scrap (4 debris → 4 scrap).
If these sell for the same amount, there's no reason to ever craft scrap.
**Recommendation:** Either increase Ancient Debris price (to 900–1200) or decrease
Netherite Scrap price (to 400–450). Since mines likely use `NETHERITE_SCRAP` in their
composition rather than `ANCIENT_DEBRIS`, check what the Tomb of Anubis actually drops.

---

### 9. Netherite Ingot Sell Price
**Current:** `NETHERITE_INGOT: 2400`
**Concern:** In vanilla, 4 Netherite Scrap + 4 Gold Ingots = 1 Netherite Ingot.
4 × 600 (scrap) + 4 × 38 (gold) = 2,552 in raw materials.
Selling as an ingot gives 2,400 — **less than crafting and selling separately**.
This is unusual and may confuse players who auto-smelt using the Forge of Ptah enchant.
**Recommendation:** Increase Netherite Ingot to 3,000–4,000, or document that
players should NOT smelt/combine Netherite before selling.

---

## Priority 3 — Nice to Have

### 10. Auction House Listing Fee
No listing fee is visible in the audit. Adding a 1–2% fee would create a mild
Coin sink that compounds over many transactions. Optional for v1.

### 11. Verify Quest Reward Ranges
Quest rewards were not audited. Run a pass over `plugin-quests/config.yml` to ensure
rewards are proportional to rank — early-game quests should give early-game amounts.

### 12. Gang/Dynasty Upgrade Costs
If Dynasty upgrades cost Coins, verify the amounts create a meaningful sink for
Dynasty members at all rank tiers. If they don't cost Coins yet, consider adding it.

### 13. Crate Relic Drops Not Configured
The exact Relic amounts in crate loot tables were not verified. Ensure the amounts
support the Relic progression timeline (full Khopesh enchant in 1–2 months of play).

---

## Configuration Files That Need Updates Before Launch

| File | Change Needed |
|---|---|
| `plugin-prestige/config.yml` | Add Relic + Coin cost for Ascension |
| `plugin-ranks/config.yml` | Update `display` values to Egyptian names; fix `max-rank-message` |
| `plugin-economy/config.yml` | Review Ancient Debris vs Netherite Scrap pricing |
| `plugin-coinflip/config.yml` | Add house cut fee if supported |
| Mine configs | Verify Tomb of Anubis drops Ancient Debris, not Netherite Scrap |

---

*Once these items are resolved, update `ECONOMY_DESIGN.md` with revised numbers.*
*Regression-test on a local server before deploying to SparkHost.*

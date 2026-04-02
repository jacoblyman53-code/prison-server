# TECHNICAL DEBT LOG

> **Purpose:** Known technical debt items tracked here with severity, effort, and status.
> Reference when prioritizing engineering work. Update as items are resolved.

---

## Severity Definitions
- **BLOCKING** — Must fix before launch
- **HIGH** — Should fix before v1; degrades quality or functionality
- **MEDIUM** — Fix before v1.5; acceptable workaround exists
- **LOW** — v2 candidate; minimal user impact

---

## Active Items

### TD-001: Prestige Cost Not Configured
**Severity:** HIGH — BLOCKING
**Effort:** Small (config + code)
**Description:** `plugin-prestige/config.yml` has no Relic or Coin cost for Ascension.
Players can ascend for free, removing the entire progression gate.
**Action:**
1. Add `ascension-costs` map to prestige config
2. Add cost check in `PrestigePlugin.onCommand()` before allowing ascension
**Status:** OPEN
**Phase:** 11

---

### TD-002: Shop System (BETA)
**Severity:** HIGH — BLOCKING
**Effort:** Medium (1–2 days)
**Description:** `plugin-shop` is marked BETA. Admin price editing may not work.
Shop categories may not have enough items. Prices may not be balanced.
**Action:**
1. Audit `plugin-shop` source code
2. Verify admin can edit prices at runtime
3. Populate shop with 5+ categories, 10–20 items each
4. Balance prices against mine income rates
**Status:** OPEN
**Phase:** 11

---

### TD-003: Tebex Webhook Handler Missing
**Severity:** HIGH — BLOCKING (for paid features)
**Effort:** Medium (2–3 days)
**Description:** `plugin-tebex` has scaffolding but no webhook POST handler.
Donor ranks, crate keys, and boosters cannot be auto-delivered after purchase.
**Action:**
1. Implement HTTP POST endpoint listener
2. Validate webhook secret
3. Parse package ID from Tebex payload
4. Execute configured commands or insert into queue
5. Handle offline player delivery (queue in DB, execute on login)
**Status:** OPEN
**Phase:** 11

---

### TD-004: New Player Onboarding Missing
**Severity:** HIGH — BLOCKING (for retention)
**Effort:** Small (4–8 hours)
**Description:** No welcome sequence, no auto-kit delivery, no objective pointer.
New players are spawned into the world with no guidance.
**Action:**
1. First-join listener → title sequence + staggered chat messages
2. Auto-deliver `starter` kit on first join (not requiring `/kit starter`)
3. Action bar objective pointer to first mine warp
4. Return-visit greeting with rank + balance display
**Status:** OPEN
**Phase:** 11 (or 8)

---

### TD-005: Generic "Rank A/B/C..." Names in Config
**Severity:** MEDIUM
**Effort:** Small (30 min)
**Description:** All 26 ranks have `display: "Rank A"` etc. in config.
The Egyptian names from `PRODUCT_IDENTITY.md` need to be applied.
**Action:** Update all 26 `display:` values in `plugin-ranks/config.yml`
**Status:** OPEN
**Phase:** 6

---

### TD-006: Rank-Up Broadcast is Empty
**Severity:** MEDIUM
**Effort:** Trivial (5 min)
**Description:** `rankup-broadcast: ""` — no server-wide announcement on rank-up.
Rank-ups are a social signal that drives competition and FOMO.
**Action:** Set broadcast to fire for milestone ranks (G, J, O, T, Z at minimum).
**Status:** OPEN
**Phase:** 6

---

### TD-007: `max-rank-message` References "Prestige Coming Soon"
**Severity:** MEDIUM
**Effort:** Trivial (5 min)
**Description:** Prestige IS implemented. The message is misleading.
**Action:** Update to reference Ascension (`/prestige ascend`)
**Status:** OPEN
**Phase:** 6

---

### TD-008: Ancient Debris / Netherite Scrap Same Price
**Severity:** MEDIUM
**Effort:** Trivial (config change)
**Description:** Both sell for 600/block. Ancient Debris should be worth more (rarer).
**Action:** See `ECONOMY_BALANCE_TODO.md` item #8
**Status:** OPEN
**Phase:** 10 (config pass)

---

### TD-009: Netherite Ingot Sell Value Below Craft Value
**Severity:** MEDIUM
**Effort:** Trivial (config change)
**Description:** Selling 4 Netherite Scrap (4×600=2400) + 4 Gold (4×38=152) = 2552.
But Netherite Ingot sells for 2400 — less than its components.
Auto-Smelt enchant users lose money.
**Action:** Increase Netherite Ingot sell price to 3000–4000
**Status:** OPEN
**Phase:** 10 (config pass)

---

### TD-010: No Unit Tests
**Severity:** LOW
**Effort:** Large (ongoing)
**Description:** Zero automated tests. All testing is manual/in-game.
Any refactor or new feature risks undetected regressions.
**Action:** Not practical to add full test coverage retroactively.
Priority targets for unit tests:
- Economy calculation functions (sell amounts, streak multipliers)
- Rank-up validation (cost checks)
- Permission node parsing
**Status:** OPEN — v2 backlog
**Phase:** N/A (ongoing)

---

### TD-011: `core-regions` Lacks Documentation
**Severity:** LOW
**Effort:** Small (documentation)
**Description:** How mine regions link to the mine system is not documented.
Buyers may struggle to understand the relationship.
**Action:** Add javadoc to `RegionsAPI` and a README section in `core-regions`
**Status:** OPEN
**Phase:** 11

---

### TD-012: Config Files Lack Comments
**Severity:** LOW
**Effort:** Medium (across 26 configs)
**Description:** Many config.yml files have no or minimal comments explaining keys.
`CONFIG_GUIDE.md` covers this but in-file comments are a better buyer experience.
**Action:** Add `# Description` comments above every config key
**Status:** OPEN (partial — some configs have good comments already)
**Phase:** 10

---

### TD-013: Black Market GUI is a Dead End
**Severity:** MEDIUM
**Effort:** Small (stub) or Large (full implementation)
**Description:** `BlackMarketMenuGUI.java` opens but has no items.
Players who find it are confused.
**Decision:** See `FEATURE_COMPLETENESS_REVIEW.md` — stub cleanly for v1.
**Action:** Update GUI to show "Coming Soon" with Egyptian flavor text
**Status:** OPEN
**Phase:** 8

---

## Resolved Items

*(None yet — this will fill as items are addressed)*

---

## Debt Register Summary

| ID | Description | Severity | Status |
|---|---|---|---|
| TD-001 | Prestige cost not configured | HIGH | OPEN |
| TD-002 | Shop system BETA | HIGH | OPEN |
| TD-003 | Tebex webhook missing | HIGH | OPEN |
| TD-004 | No onboarding | HIGH | OPEN |
| TD-005 | Generic rank names | MEDIUM | OPEN |
| TD-006 | Rank-up broadcast empty | MEDIUM | OPEN |
| TD-007 | Stale max-rank-message | MEDIUM | OPEN |
| TD-008 | Ancient Debris pricing | MEDIUM | OPEN |
| TD-009 | Netherite Ingot pricing | MEDIUM | OPEN |
| TD-010 | No unit tests | LOW | OPEN (v2) |
| TD-011 | core-regions undocumented | LOW | OPEN |
| TD-012 | Config comments missing | LOW | OPEN (partial) |
| TD-013 | Black Market dead end | MEDIUM | OPEN |

---

*Update this log when items are resolved.*
*Mark resolved items with date and PR/commit reference.*

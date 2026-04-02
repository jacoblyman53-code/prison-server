# EXECUTION PLAN — Prison Server Productization

> **Goal:** Transform the codebase into a premium, sellable Minecraft Prison server setup
> worth $5,000 on the open market.
>
> **Timeline:** 15 phases, sequential where dependent, parallelizable where independent.
> **Baseline:** See `PRODUCT_AUDIT.md` for gap analysis that drives this plan.

---

## How to Read This Plan

Each phase has:
- **Goal** — what you're accomplishing
- **Deliverables** — exact files created or changed
- **Depends on** — which phases must be done first
- **Priority** — CRITICAL / HIGH / MEDIUM / LOW

Phases 1–5 are **foundation** (design decisions). They must be done in order.
Phases 6–12 are **implementation** (code + docs). Can be parallelized.
Phases 13–15 are **packaging** (sales + release). Done last.

---

## Phase 1 — Repository Audit & Baseline

**Status: COMPLETE**
**Goal:** Honest assessment of what exists, what's missing, and what needs to be built.

### Deliverables
- [x] `PRODUCT_AUDIT.md` — Full system audit with scores and gap list
- [x] `EXECUTION_PLAN.md` — This file

---

## Phase 2 — Product Identity

**Status: PENDING**
**Priority: CRITICAL**
**Depends on:** Phase 1
**Goal:** Define the Egyptian mythology theme that differentiates this product from every other prison server on the market.

### Why This Phase Is Critical
Generic "Prison" theming is a commodity. Egyptian mythology is uncommon, visually striking, and provides a complete naming system for every game element. It justifies premium pricing by making the product feel handcrafted rather than assembled.

### Deliverables
- [ ] `PRODUCT_IDENTITY.md`
  - Theme statement and elevator pitch
  - Full name mapping: ranks, mines, currencies, crates, prestige system
  - Rank names (Servant → Pharaoh, 26 names A→Z)
  - Mine names (Tomb of [deity] for each tier)
  - Currency rename: IGC → **Coins** (gold coins, Egyptian aesthetic); Tokens → **Relics**
  - Crate rename: Ancient Chest, Pharaoh's Chest, Sacred Relic
  - Prestige rename: **Ascension** (ascending to godhood)
  - Color palette: Gold (#FFD700), Sand (#E8C87A), Obsidian (#1a1a2e), Deep Blue (#2d3561)
  - Server name recommendation: **"The Pharaoh's Prison"** or **"Kingdom of Kha"**
  - Logo / wordmark direction for the sales page

### Implementation Note
Phase 2 is a *design* phase only. Actual renaming in code happens in Phase 6 (GUI overhaul). This phase produces the specification that Phase 6 implements.

---

## Phase 3 — Gameplay Loop Design

**Status: PENDING**
**Priority: CRITICAL**
**Depends on:** Phase 2
**Goal:** Document the intended player progression from first join to end-game. This becomes the product's "experience promise" to buyers.

### Deliverables
- [ ] `GAMEPLAY_LOOP.md`
  - Hour-by-hour progression: what a player does in hour 1, day 1, week 1
  - Economy entry points: how players earn their first coins
  - Bottleneck points: where the game slows down and why (rank-up cost walls)
  - Retention mechanics: sell streaks, daily quests, gang rivalry, prestige

- [ ] `NEW_PLAYER_FLOW.md`
  - Exact sequence from first join: spawn → tutorial → first mine → first sell → first rank-up
  - What messages, titles, and sounds fire at each milestone
  - What the starter kit contains and why
  - Onboarding gaps that need to be built (see Phase 8)

- [ ] `PROGRESSION_MAP.md`
  - Visual (text-based) map of all 26 ranks and what each unlocks
  - Mine access gates (which rank unlocks which mine)
  - Prestige gate (minimum rank before first prestige)
  - Token income curve across the full progression

---

## Phase 4 — Economy Redesign

**Status: PENDING**
**Priority: HIGH**
**Depends on:** Phase 3
**Goal:** Validate and document the economy numbers. Identify any broken feedback loops.

### Deliverables
- [ ] `ECONOMY_DESIGN.md`
  - Philosophy: why dual-currency (coins + relics)
  - Coin flow: all sources in, all sinks out, steady-state analysis
  - Relic flow: all sources in, all sinks out
  - Sell streak analysis: at what streak does the economy become inflationary
  - Coinflip impact: expected coin churn per day at various player counts
  - Auction house impact: does it create money or just redistribute it

- [ ] `CURRENCY_PURPOSES.md`
  - Every place a player earns Coins, with approximate rate at each rank
  - Every place a player spends Coins
  - Every place a player earns Relics
  - Every place a player spends Relics
  - Which currency is "harder" and why that matters for crate purchases

- [ ] `ECONOMY_BALANCE_TODO.md`
  - Specific numbers in config that should be reviewed before launch
  - Known untested assumptions (e.g., sell streak x100 at high rank — is this too fast?)
  - Recommended adjustments for small (10 player), medium (50 player), large (100+ player) servers

---

## Phase 5 — Ranks, Mines, and Prestige Design

**Status: PENDING**
**Priority: HIGH**
**Depends on:** Phase 2, Phase 3
**Goal:** Document the complete progression system in detail suitable for both buyers and players.

### Deliverables
- [ ] `RANK_LADDER.md`
  - All 26 ranks with Egyptian names
  - Cost to rank up from each tier
  - Cumulative cost to reach each tier
  - What each rank unlocks (mine access, commands, cosmetics, kit eligibility)

- [ ] `MINE_PROGRESSION.md`
  - All mine tiers with Egyptian tomb names
  - Block compositions at each tier (what percentage stone, coal, iron, gold, diamond, etc.)
  - Approximate coins/hour at each mine tier (used for economy validation)
  - Which mines are standard vs donor-exclusive vs gang-exclusive

- [ ] `ASCENSION_SYSTEM.md` (Prestige)
  - How many times a player can ascend
  - What ascension resets (rank, not inventory)
  - Ascension rewards: relic bonuses, cosmetic unlocks, mine access expansion
  - Why a player would choose to ascend (the value proposition)

- [ ] `ENDGAME_DESIGN.md`
  - What does a max-prestige player do on a daily basis
  - Gang wars as an endgame activity
  - Auction house as a passive income endgame
  - Events as endgame content
  - What content a buyer should add to extend the endgame

---

## Phase 6 — GUI and UX Overhaul

**Status: PENDING**
**Priority: HIGH**
**Depends on:** Phase 2, Phase 5
**Goal:** Apply the Egyptian theme to every GUI. Ensure every menu is polished to spec.

### Deliverables
- [ ] `GUI_MASTER_SPEC.md`
  - Applies `GUI_DESIGN_SPEC.md` to the Egyptian theme
  - Color scheme for MiniMessage tags: `<gold>`, `<color:#E8C87A>`, etc.
  - Standard item names in Egyptian style (e.g., "Pharaoh's Ranks" not "Rank Menu")
  - Icon choices for each category in the main menu

- Per-GUI specifications and code updates for all 28 GUIs:
  - [ ] Main Menu — Egyptian decorative border, gold/sand theme
  - [ ] Ranks Menu — Rank names use Egyptian tier names
  - [ ] Mines Menu — Mine names use tomb names
  - [ ] Prestige/Ascension Menu — "Ascend to the next life" framing
  - [ ] Crates Menu — Chest names updated
  - [ ] Shop Menu — "Merchant's Bazaar" or "Market of Karnak"
  - [ ] Coinflip Menu — "The Sphinx's Gamble" framing
  - [ ] Auction House — "Grand Bazaar" framing
  - [ ] Gangs Menu — "Dynasties" or "Factions of the Nile"
  - [ ] All remaining 19 GUIs — theme pass

### Implementation Note
This is the largest code-change phase. Budget for this being 2–3x the effort of other phases.
The existing GUI framework is solid — this is a content/text pass, not a structural rewrite.

---

## Phase 7 — Monetization Integration

**Status: PENDING**
**Priority: HIGH**
**Depends on:** Phase 5
**Goal:** Complete the Tebex integration and document every purchasable product.

### Deliverables
- [ ] `MONETIZATION_PLAN.md`
  - Revenue model: cosmetic-only vs pay-to-win considerations
  - EULA compliance notes (Mojang ToS for server monetization)
  - Pricing strategy for each product tier
  - Expected revenue at various player counts

- [ ] `TEBEX_PRODUCT_MAP.md`
  - Every item available for purchase with exact Tebex command grants
  - Package names, prices, and descriptions ready to copy into Tebex dashboard
  - Crate key packages (single key, 5-pack, 10-pack)
  - Donor rank packages (all 4 tiers)
  - Booster packages (sell streak, coin booster)
  - Cosmetics packages (tag packs, particle packs)

- [ ] `DONOR_RANKS.md`
  - Exact perk list for each of the 4 donor ranks
  - Permission nodes granted per rank
  - Mine access granted per rank (which donor mines)
  - Session length per donor mine per rank
  - Cosmetic perks per rank
  - GUI display for each rank's perk page

- [ ] `BOOSTER_SYSTEM.md`
  - Design doc for a timed booster system (2x coins, 2x relics, etc.)
  - How boosters interact with sell streaks
  - Whether this needs code changes or can use existing hooks

### Code Changes Required
- Complete `plugin-tebex` webhook handler
- Implement command execution queue for async Tebex grant delivery
- Ensure donor rank assignment is instant from Tebex callback

---

## Phase 8 — Content Completeness

**Status: PENDING**
**Priority: HIGH**
**Depends on:** Phase 3
**Goal:** Identify every feature referenced in the code or configs that isn't fully built, and decide: build it, stub it cleanly, or cut it.

### Deliverables
- [ ] `FEATURE_COMPLETENESS_REVIEW.md`
  - Complete list of every system and its status (built / partial / stub / cut)
  - Decision rationale for each partial/stub system
  - Black Market: build it or remove all references
  - Web dashboard: scope a v1 or remove entirely

- [ ] `NICE_TO_HAVE_FEATURES.md`
  - Features that would add polish but are not blocking release
  - Each with: description, estimated complexity, priority score
  - Examples: player statistics GUI, mine speedrun leaderboard, gang war scheduling

- [ ] `CUT_FEATURES.md`
  - Features explicitly cut for v1 to avoid scope creep
  - Documented so buyers know they're not bugs
  - Each with: "why cut" and "how to re-enable if desired"

### Priority Decisions to Make
1. **Black Market** — A full Black Market system would add significant value. Minimum viable version: random rotating shop refreshing every hour. Decision needed before Phase 8 can complete.
2. **Web Dashboard** — Would cost significant effort. Could be positioned as a "coming soon" bonus. Decision needed.
3. **Starter Kit / Onboarding** — Low effort, high impact. Should be built before launch.

---

## Phase 9 — Deployment Structure

**Status: PENDING**
**Priority: HIGH**
**Depends on:** Phase 1
**Goal:** Make the product installable by someone who didn't write it.

### Deliverables
- [ ] `DEPLOYMENT_GUIDE.md`
  - Requirements: Java 21, Paper 1.21.x, MySQL 8.0+
  - Step-by-step server setup from blank VPS
  - MySQL database creation and user setup
  - `config.yml` minimum required changes for a new install
  - First-boot checklist (what to verify is working)
  - Common errors and their solutions

- [ ] `INSTALLATION_GUIDE.md` (buyer-facing, simpler)
  - How to drop the JARs into a plugins folder
  - How to configure the database connection
  - How to run the Gradle build yourself vs using prebuilt JARs
  - How to connect the Tebex webhook

- [ ] `PROJECT_STRUCTURE.md`
  - Map of every module and what it does
  - Dependency graph (which plugins depend on which)
  - Where to find: economy config, mine config, rank config, permission nodes
  - How to add a new plugin to the monorepo

---

## Phase 10 — Configurability Documentation

**Status: PENDING**
**Priority: MEDIUM**
**Depends on:** Phase 9
**Goal:** Every config.yml should be self-documenting. Buyers should be able to tune the server without reading the source code.

### Deliverables
- [ ] `CONFIG_GUIDE.md`
  - Every config.yml file, every key, what it does
  - Which keys are safe to change at runtime (hot-reload) vs require restart
  - Which keys are dangerous to change (economy multipliers — warn about inflation)
  - Recommended values for: small server, medium server, large server

- [ ] `EXTENSION_GUIDE.md`
  - How to add a new rank to the ladder
  - How to add a new mine
  - How to add a new crate tier
  - How to add a new custom enchant
  - How to add a new quest type
  - How to add a new donor rank tier

- [ ] `ADMIN_EDITING_GUIDE.md`
  - All `/mine` admin commands
  - All `/rank admin` commands
  - All admin toolkit operations
  - How to use the staff tools plugin
  - How to grant/revoke permissions manually

---

## Phase 11 — Code Health

**Status: PENDING**
**Priority: MEDIUM**
**Depends on:** Phase 8 (need to know what's being kept)
**Goal:** Clean up the technical debt identified in the audit. Not a rewrite — targeted fixes only.

### Deliverables
- [ ] `TECHNICAL_DEBT_LOG.md`
  - Full list of known technical debt items
  - Each rated: severity, effort, blocking/non-blocking
  - Items resolved during this phase marked done
  - Items deferred to v2 documented with rationale

- [ ] `CODE_HEALTH_IMPROVEMENTS.md`
  - Summary of changes made in this phase
  - Before/after for any significant refactors
  - Notes for future maintainers

### Target Items (from audit)
1. `plugin-shop` — complete admin price editing or clearly scope what's in v1
2. `plugin-tebex` — complete webhook handler (also Phase 7)
3. Config files — add inline comments to all config.yml files
4. `core-regions` — document how mine regions link to the mine system
5. Validate all `softdepend` entries are correct across all plugin.yml files

---

## Phase 12 — Admin Tooling

**Status: PENDING**
**Priority: MEDIUM**
**Depends on:** Phase 10
**Goal:** Ensure server admins (buyers running the server) have everything they need to operate the server day-to-day.

### Deliverables
- [ ] `ADMIN_TOOLS.md`
  - Complete list of admin commands across all plugins
  - Each command: syntax, permission, what it does, example usage
  - Which commands are safe for moderators vs owner-only

- [ ] `STAFF_OPERATIONS.md`
  - Daily operations: how to handle player reports, how to handle economy disputes
  - How to use the anticheat review queue
  - How to use the economy toolkit (give/take/set)
  - How to issue refunds via coinflip log review
  - How to manage gang disputes

- [ ] `PERMISSIONS_MATRIX.md`
  - Every permission node in the system (~200+)
  - Organized by plugin
  - Which rank/group gets each permission by default
  - Which permissions are dangerous to give to non-staff

---

## Phase 13 — Sales Documentation

**Status: PENDING**
**Priority: HIGH**
**Depends on:** All previous phases
**Goal:** Create the sales materials that justify the price and convert a browser into a buyer.

### Deliverables
- [ ] `README.md` (complete rewrite)
  - Hero section: server name, tagline, screenshot/banner
  - 5-sentence pitch: what this is, who it's for, why it's worth it
  - Feature highlights with icons
  - Quick-start link
  - Screenshots section (placeholder for actual screenshots)
  - Purchase / contact link

- [ ] `FEATURES.md`
  - Detailed feature list organized by system
  - Each feature: name, description, what makes it special
  - Comparison table vs typical prison setups (optional)

- [ ] `WHY_THIS_PRODUCT.md`
  - The honest case for why this is worth $5,000
  - What it would cost to build this from scratch (time × developer rate)
  - What ongoing updates/support are included (if any)
  - Who the ideal buyer is

- [ ] `DIFFERENTIATORS.md`
  - Specific features not found in competing products
  - Technical advantages (custom permissions, zero Vault dependency, etc.)
  - Content advantages (Egyptian theme, 28-GUI system, etc.)

- [ ] `QUICK_START.md`
  - 10-step guide from purchase to running server
  - Assumes a buyer with basic server hosting knowledge
  - Links to DEPLOYMENT_GUIDE.md and CONFIG_GUIDE.md for detail

- [ ] `FAQ.md`
  - Answers to questions buyers will ask before purchasing
  - "Does it work on X host?" "Do I need MySQL?" "Can I change the theme?" etc.

---

## Phase 14 — World Integration

**Status: PENDING**
**Priority: HIGH**
**Depends on:** Phase 5
**Goal:** Give buyers a path to get a playable world set up. The code means nothing without a world.

### Deliverables
- [ ] `WORLD_INTEGRATION_GUIDE.md`
  - How mine regions are defined in `core-regions`
  - How to link a WorldEdit selection to a mine
  - How to set spawn, mine warps, and the main hub location
  - How to configure the donor mine region gate
  - Required plugin setup: WorldEdit, WorldGuard (if used), or equivalent

- [ ] `MINIMUM_VIABLE_WORLD.md`
  - The absolute minimum world setup to have a playable server
  - 26 mine regions (can all be identical for testing)
  - A spawn point
  - Basic warp points
  - How long this takes with WorldEdit

- [ ] `PREMIUM_WORLD_PLAN.md`
  - What a high-quality world build looks like
  - Egyptian architecture recommendations: pyramid spawn, tomb mine entrances
  - Estimated build time or cost to commission a world builder
  - References to marketplaces where Egyptian Minecraft builds can be purchased

---

## Phase 15 — Final Review and Release

**Status: PENDING**
**Priority: CRITICAL**
**Depends on:** All previous phases
**Goal:** Final pass to ensure the product is coherent, complete, and ready to sell.

### Deliverables
- [ ] `FINAL_PRODUCT_REVIEW.md`
  - Re-run the audit from Phase 1 against the completed product
  - Updated scorecard (target: 9/10 overall)
  - Any remaining gaps with explicit "v2" designation
  - Confirmation that all Phase 1 gaps are resolved or consciously deferred

- [ ] `RELEASE_CHECKLIST.md`
  - Pre-release checklist: builds clean, all tests pass (manual), config defaults are sane
  - GitHub release tag instructions
  - Sales page publication steps
  - First-day support preparation (what questions to expect)

---

## Execution Order

```
Phase 1 ──► Phase 2 ──► Phase 3 ──► Phase 4
                  │           │
                  └───────────┴──► Phase 5
                                       │
                   ┌───────────────────┤
                   ▼                   ▼
              Phase 6              Phase 7
              Phase 8              Phase 9
              Phase 11             Phase 10
              Phase 12
                   │
                   ▼
              Phase 13
              Phase 14
                   │
                   ▼
              Phase 15 ──► RELEASE
```

---

## Resource Estimate

| Phase | Complexity | Notes |
|---|---|---|
| 1 | Done | Audit complete |
| 2 | Low | Design/writing only |
| 3 | Low | Design/writing only |
| 4 | Medium | Requires reading all economy configs |
| 5 | Low | Design/writing only |
| 6 | **High** | 28 GUI code changes |
| 7 | Medium | Tebex code + docs |
| 8 | Medium | Some code, mostly decisions |
| 9 | Low | Writing only |
| 10 | Medium | Requires reading all 26 config files |
| 11 | Medium | Targeted code fixes |
| 12 | Low | Writing only |
| 13 | Medium | Marketing writing |
| 14 | Low | Writing + world guide |
| 15 | Low | Review + checklist |

---

*This plan is a living document. Update it as phases complete or priorities shift.*
*Last updated: April 2026.*

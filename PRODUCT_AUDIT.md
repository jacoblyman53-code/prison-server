# PRODUCT AUDIT — Prison Server

> **Purpose:** Honest, complete assessment of the codebase as it stands before productization.
> Written April 2026. Used as the baseline for all 15 phases of the execution plan.

---

## 1. Project Overview

A fully custom Minecraft 1.21 Prison server built as a **26-module Gradle monorepo**.
Every system is hand-written — no LuckPerms, no Vault, no third-party economy plugins.
The stack is Java 21 + Paper 1.21 + Adventure API + MySQL (HikariCP).

**Modules:**

| Module | Type | Status |
|---|---|---|
| core-database | Shared library + plugin | PRODUCTION |
| core-permissions | Shared library + plugin | PRODUCTION |
| core-regions | Shared library + plugin | PRODUCTION |
| plugin-admintoolkit | Admin utility plugin | PRODUCTION |
| plugin-anticheat | Anti-cheat plugin | PRODUCTION |
| plugin-auctionhouse | Auction house plugin | PRODUCTION |
| plugin-chat | Chat formatting plugin | PRODUCTION |
| plugin-coinflip | Coinflip gambling plugin | PRODUCTION |
| plugin-cosmetics | Cosmetics / tags plugin | PRODUCTION |
| plugin-crates | Crate reward plugin | PRODUCTION |
| plugin-donor | Donor rank management | PRODUCTION |
| plugin-economy | Currency + transactions | PRODUCTION |
| plugin-events | Scheduled server events | PRODUCTION |
| plugin-gangs | Gang system plugin | PRODUCTION |
| plugin-kits | Kit distribution plugin | PRODUCTION |
| plugin-leaderboards | Holographic leaderboards | PRODUCTION |
| plugin-menu | Main hub menu + all GUIs | PRODUCTION |
| plugin-mines | Mine system + resets | PRODUCTION |
| plugin-pickaxe | Custom enchant pickaxe | PRODUCTION |
| plugin-prestige | Prestige system | PRODUCTION |
| plugin-quests | Daily/weekly quests | PRODUCTION |
| plugin-ranks | Rank-up system | PRODUCTION |
| plugin-shop | Shop system | **BETA** |
| plugin-staff | Staff tools | PRODUCTION |
| plugin-tebex | Tebex donation integration | **BETA** |
| plugin-warps | Warp management | PRODUCTION |
| web-dashboard | Web admin panel | **NOT STARTED** |

---

## 2. What Works (Strengths)

### Architecture
- Clean multi-module monorepo with a shared database core. All plugins share one HikariCP connection pool, preventing connection sprawl.
- Custom permission system with three parallel trees: **mine rank** (A–Z), **donor rank** (donor/donorplus/elite/eliteplus), **staff rank** (helper/mod/admin/owner). No external dependency risk.
- Async write queue (`queueWrite()`) with 500ms flush interval keeps the main thread clean for high-frequency operations (block breaks, economy changes).
- `CREATE TABLE IF NOT EXISTS` on every table — safe for fresh installs and restarts.
- `executeAndGetId()` for INSERT + auto-key retrieval — no race conditions on ID lookups.

### Economy
- Dual currency: **IGC** (mine income, spent on ranks/upgrades) and **Tokens** (prestige/enchant currency).
- 26-rank ladder (A→Z) with tuned cumulative costs scaling to ~500M IGC total.
- Sell streak multiplier system (x5 → x100) rewarding sustained grinding.
- Full `TransactionType` enum tagging every currency movement — audit-ready.
- Coinflip with proper `COINFLIP_BET / COINFLIP_WIN / COINFLIP_REFUND` transaction types and a `coinflip_logs` table.

### GUI System
- 28 interconnected GUIs all routed through `MainMenuGUI`.
- Compliant with `GUI_DESIGN_SPEC.md`: no filler, content top-left, controls bottom row, slot 0 = back/close, pagination at slots 45/53.
- MiniMessage throughout — no legacy `§` codes in any GUI class.
- Crystal-clear tooltips with explicit confirm steps on all destructive admin actions.

### Game Systems
- 11 custom pickaxe enchants with token costs, levels, and descriptions.
- Prestige system with token rewards and mine unlock gates.
- Daily + weekly quests with configurable objectives.
- Gang system with shared mines, contribution tracking, and leaderboards.
- Holographic leaderboards for richest players, top gang, top prestige.
- Crate system with Ancient/Regular/Vote tiers and weighted loot tables.
- Auction house with bidding, expiry, and mail pickup.
- Warp management with permission-gated destinations.
- Anti-cheat with flag system and admin review queue.
- BungeeCord plugin messaging channel registered and ready for multi-server expansion.

### Quality
- Zero legacy ChatColor — full Adventure API compliance.
- Staff action logging in `staff_actions` table for all admin economy operations.
- All DB schema is indexed appropriately (UUID columns, timestamp columns).
- Donor mine sessions with configurable timer (default 30 min) and auto-teleport on expiry.

---

## 3. What Is Incomplete (Gaps)

### Hard Gaps (missing features that affect gameplay)

| Gap | Detail | Severity |
|---|---|---|
| **Black Market** | Referenced in config and GUI navigation but the system does not exist. Players who find the warp or menu entry will hit a dead end. | HIGH |
| **plugin-shop** | Shop GUI exists but is marked BETA. Admin editing tools not found. Prices likely hardcoded or incomplete. | HIGH |
| **plugin-tebex** | Tebex integration scaffolding exists but donation flow is not complete. Donor perks cannot be granted automatically. | HIGH |
| **web-dashboard** | Directory exists, no code. Admin web panel is entirely absent. | MEDIUM |
| **Rank milestone kits** | Added 5 kits (E, J, O, T, Z) but no auto-delivery on rank-up. Players must manually claim from `/kit`. | MEDIUM |
| **No new-player onboarding** | No starter kit, no welcome message flow beyond plugin defaults, no tutorial. | MEDIUM |
| **No world files** | The product ships code only. Buyers need a prison world, spawn, mine regions. No world templates included. | HIGH |

### Soft Gaps (missing polish / sellability items)

| Gap | Detail |
|---|---|
| **No README.md** | The repository root has no README. A buyer landing on the repo has no orientation. |
| **No installation guide** | No documented setup steps from fresh VPS to running server. |
| **No config guide** | 26 config.yml files with no unified documentation of what each key does. |
| **No permissions matrix** | 200+ permission nodes with no reference sheet. |
| **No gameplay balance doc** | Economy numbers exist in config but no rationale for the scaling decisions. |
| **Naming is generic** | Everything is "Prison" themed — plugin names, class names, currency name. No unique identity that justifies premium pricing. |
| **No sales page copy** | No FEATURES.md, no pitch text, no "why buy this" document. |
| **No world integration guide** | No direction for buyers on mine region setup, spawn build requirements, warp points. |

---

## 4. Economy Assessment

### Currency: IGC (In-Game Currency)
- Primary grind currency. Earned by selling blocks at `/sell`.
- Spent on: rank-ups (A→Z), pickaxe enchant token conversion, gang upgrades.
- Sell values are in `plugin-economy/src/main/resources/config.yml`.

### Currency: Tokens
- Prestige/enchant currency. Earned by: prestige rewards, quests, crates, sell streaks.
- Spent on: custom pickaxe enchants (11 types), prestige costs.
- Token economy is deliberately scarce to drive crate purchases.

### Rank Costs (A→Z, cumulative ~500M IGC)
Progression is intentionally long to maximize playtime before prestige.
Scale has been tuned but **not externally validated** — no player data.

### Sell Streak Multiplier
- x5 → x100 scaling over consecutive sell sessions.
- Well-implemented but **not documented** for buyers or players in-game.

### Coinflip
- IGC only. No token coinflip.
- Min/max bet not clearly surfaced in GUI.
- `coinflip_logs` table records every game for dispute resolution.

---

## 5. GUI Assessment

### Score: 9/10
All GUIs are spec-compliant. The design system is coherent and consistent. The only items preventing a 10/10:
- Some lore strings use generic Prison language rather than the intended Egyptian theme.
- Sell streak display could be more prominent (currently a small stat in a sub-menu).
- Black Market GUI entry point exists but leads nowhere.

### GUI Inventory
28 GUIs confirmed in the `plugin-menu` module:
- Main Menu, Ranks, Prestige, Mines, Shop, Auction House, Crates, Coinflip, Gangs, Quests, Leaderboards, Cosmetics/Tags, Kits, Warps, Events, Pickaxe Upgrades, Pickaxe Enchants, Token Shop, Donor Perks, Staff Tools, Economy Tools, Player Management, Reports, Chat Filter, Anticheat Review, Settings, Help, Black Market (stub)

---

## 6. Code Health

### Strengths
- Consistent use of `queueWrite()` for async DB — no synchronous writes on the main thread.
- No magic strings for permission nodes — constants are centralized.
- No legacy color codes in any plugin.
- Minimal external dependencies: HikariCP, Paper API, and that's it.

### Technical Debt
- `plugin-shop`: Partially implemented. Unclear if buy/sell prices are editable at runtime.
- `plugin-tebex`: Scaffolding only. Needs Tebex webhook handler and command grant queue.
- `web-dashboard`: Completely empty. Not a debt so much as a future feature.
- Some config.yml files have no comments explaining what the values control.
- No unit tests anywhere. All testing is manual/in-game.
- `core-regions` has no documentation on how mine regions are defined or linked to the mine system.

---

## 7. Deployment Assessment

### Current State
- Automated deploy script (`deploy.ps1`) builds all 28 JARs, uploads via WinSCP SFTP to SparkHost, and restarts via Pterodactyl API.
- GitHub repo: `jacoblyman53-code/prison-server`.
- SparkHost SFTP chroot: remote path must be `""` (empty string), not `/home/container`.

### For Productization
- The deploy script is dev-specific (hardcoded SparkHost credentials, personal paths).
- A buyer needs: a clean `INSTALLATION_GUIDE.md`, a gradle build command, and a folder structure doc.
- No Docker setup. No CI/CD pipeline. Fine for the target audience (server owners) but worth noting.

---

## 8. Monetization Readiness

### Tebex Integration: 30% complete
- `plugin-tebex` exists with scaffolding.
- No webhook endpoint, no command execution queue, no package → permission mapping.
- **Blocking:** Donor ranks cannot be granted automatically without this.

### Donor Ranks: Defined
- 4 tiers: donor, donorplus, elite, eliteplus.
- Permissions exist. Donor mine sessions work.
- GUI for donor perks exists.
- **Gap:** No documented list of what each tier gets. No Tebex product descriptions.

### Crate Economy: Ready
- 3 crate tiers: Ancient, Regular, Vote.
- Loot tables are configurable.
- No crate key shop connected to Tebex yet.

---

## 9. Competitive Positioning

### What Makes This Product Stand Out
1. **Zero external plugin dependencies.** Most prison servers require LuckPerms, Vault, EssentialsX. This server needs none of them — reducing compatibility surface area and update breakage risk.
2. **Custom permission engine.** Three parallel permission trees with database-backed storage.
3. **Full GUI system.** 28 GUIs with a consistent design spec — something most sold setups lack.
4. **Production-grade database.** HikariCP pool, async write queue, indexed schema, full transaction logging.
5. **Code is readable and extensible.** Every system has clear entry points and shared APIs.

### What Holds It Back From $5,000 Pricing
1. **No theme.** "Prison server" is a commodity. An Egyptian mythology theme with unique naming would differentiate it.
2. **No world.** Buyers expect a spawn + mine layout. Code-only sales require the buyer to build everything.
3. **Incomplete systems.** Black Market, Tebex, and the web dashboard are gaps a premium buyer will notice.
4. **No documentation.** A $5,000 product needs a complete manual, not just source code.
5. **No sales copy.** Nothing on the repo explains why this is worth the price.

---

## 10. Summary Scorecard

| Category | Score | Notes |
|---|---|---|
| Core Systems | 9/10 | Solid. Minor gaps in shop and tebex. |
| Economy Design | 8/10 | Well-structured. Needs balance validation. |
| GUI/UX | 9/10 | Spec-compliant. Needs theme pass. |
| Code Health | 8/10 | Clean. No tests. Some incomplete modules. |
| Documentation | 2/10 | Almost nothing. Biggest gap. |
| Monetization | 4/10 | Tebex incomplete. Donor perks undocumented. |
| Sales Readiness | 1/10 | No pitch, no README, no world. |
| **Overall** | **6/10** | Strong foundation. Needs 15-phase treatment. |

---

*This audit is the baseline. Every improvement in the execution plan references back to the gaps identified here.*

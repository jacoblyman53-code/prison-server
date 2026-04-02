# DIFFERENTIATORS

> Specific features not found in competing products.
> Use this to answer "why should I buy this over X?"

---

## Technical Differentiators

### No Vault Dependency
**What others do:** Every other prison setup uses Vault as the economy bridge.
Vault requires: Vault plugin + a Vault-compatible economy plugin (EssentialsX, etc.)
**What this does:** Custom economy with direct HikariCP access. No Vault. No bridge.
**Why it matters:** Vault is a legacy abstraction. It adds complexity and a dependency
that can break. This server handles economy with a clean, typed `EconomyAPI`.

### No LuckPerms Dependency
**What others do:** LuckPerms is nearly universal in Minecraft server setups.
**What this does:** Custom permission engine with three parallel trees, all in MySQL.
**Why it matters:** LuckPerms is a third-party tool you must keep updated and trust.
This server controls its own permission storage — no migration, no compatibility risk.

### Async Write Queue
**What others do:** Write to DB synchronously on the main thread, or use SQLite (single-threaded).
**What this does:** All high-frequency writes (balance changes, block break counts) go into
an async queue flushed every 500ms. The main thread is never blocked waiting for MySQL.
**Why it matters:** At 100 players all mining simultaneously, this is the difference between
20 TPS and 18 TPS. Production-grade servers can't afford synchronous DB writes.

### Full Transaction Log
**What others do:** Economy just stores a balance. No history.
**What this does:** Every Coin and Relic movement is logged with a `TransactionType` enum
(SELL_ALL, RANK_UP, COINFLIP_BET, COINFLIP_WIN, etc.) and timestamp.
**Why it matters:** Economy disputes are resolved in minutes. Anticheat investigations
have a paper trail. No more "I don't know what happened to their Coins."

### `executeAndGetId()` for Inserts
**What others do:** Run INSERT, then run a separate SELECT to get the generated ID.
**What this does:** `executeAndGetId()` wraps the INSERT and reads the `GENERATED_KEYS`
in one database round-trip, with proper error handling.
**Why it matters:** Eliminates a class of race conditions where another INSERT happens
between the INSERT and the SELECT.

---

## Gameplay Differentiators

### Sell Streak Multiplier (Not AFK-Farmable)
**What others do:** Passive income multipliers or flat sell bonuses.
**What this does:** Sell streak requires `/sell all` within a 60-second window to maintain.
Going AFK doesn't maintain the streak — you must actively sell.
**Why it matters:** Keeps players engaged. Can't just AFK farm and come back to find
100× multiplier maintained. Rewards session play.

### Three-Parallel-Prefix Chat System
**What others do:** One chat prefix (usually just rank).
**What this does:** Three prefixes display simultaneously — Ascension title, Mine rank, Donor rank.
A max player looks like: `[Living God] [Pharaoh] [Chosen] PlayerName`
**Why it matters:** Each prefix is a status symbol. Players grind for all three independently.
More progression vectors = more retention.

### Donor Mine Session Timers
**What others do:** Donor mines are either unlimited or not separated from regular mines.
**What this does:** Each donor mine has a configurable session timer (default 30 min).
After the timer, the player is automatically teleported out with a cooldown before re-entry.
**Why it matters:** Session timers prevent a single donor from monopolizing the mine 24/7.
Keeps donor mines feeling exclusive and worth paying for.

### Configurable Mine Type System
**What others do:** All mines are the same type.
**What this does:** Each mine has a `mine-type` (STANDARD / DONOR / PRESTIGE) with
different access rules enforced at the plugin level.
**Why it matters:** Operators can create prestige-gated mines (require Ascension level 3)
without code changes — just config.

---

## GUI Differentiators

### Design Spec Compliance
**What others do:** Each GUI is built independently by different developers with no shared standard.
**What this does:** All 28 GUIs follow a single spec (`GUI_DESIGN_SPEC.md`):
- No filler items
- Content from top-left
- Controls in bottom row only
- Slot 0 = always back/close
- Every item has a lore with a call to action

**Why it matters:** Players learn the navigation once and it works everywhere.
No "where is the back button in THIS menu?"

### MiniMessage Everywhere
**What others do:** Mix of legacy `§` codes and modern Adventure API. Inconsistent.
**What this does:** 100% MiniMessage in all GUI strings, chat, and titles.
**Why it matters:** MiniMessage supports gradients, hover events, click events — features
impossible with legacy codes. Ready for the future of Paper's text API.

---

## Theme Differentiators

### Egyptian Mythology — Not Just "Prison"
**What others do:** Rank A-Z with no names. "Sell blocks. Buy rank." Generic.
**What this does:** 26 ranks named after Egyptian hierarchy (Slave → Pharaoh).
14 mines named after Egyptian gods (Tomb of Aten → Tomb of Anubis).
Currency named Coins and Relics. Khopesh pickaxe. Ascension prestige.
**Why it matters:** A theme creates an emotional investment. Players don't just "rank up" —
they "become a Pharaoh." That's the difference between a 3-day player and a 3-month player.

### Thematic Lore Voice
**What others do:** "Click to rank up to Rank B."
**What this does:** `<color:#E8C87A>The gods watch. Prove yourself worthy.` / `<green>▸ Click to ascend`
**Why it matters:** Every tooltip reinforces the world. The server feels alive.

---

## Documentation Differentiators

### 30+ Documentation Files
**What others do:** A 2-page "README" or nothing.
**What this does:** 30+ Markdown files covering every aspect of the server:
- Economy balance analysis with actual numbers
- Player progression design (hour-by-hour)
- Admin procedures for real situations (disputes, bans, anticheat)
- Tebex product definitions ready to copy into the dashboard
- Extension guides for common customizations

**Why it matters:** Buyers aren't left guessing. The documentation is the product.

---

*See `WHY_THIS_PRODUCT.md` for the broader value proposition.*
*See `FAQ.md` for answers to common buyer questions.*

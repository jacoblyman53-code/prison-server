# FEATURES

> Complete feature list for The Pharaoh's Prison.
> Organized by system. Use this as the definitive "what's in the box" reference.

---

## Economy System

**plugin-economy + plugin-ranks**

- [x] Dual currency: **Coins** (grind) and **Relics** (enchant)
- [x] 26-rank ladder from Slave (A) to Pharaoh (Z)
- [x] Total cost ~1.548 billion Coins from start to max rank
- [x] Configurable sell prices per Bukkit material type (100+ materials)
- [x] Sell streak multiplier: 1× → 1.5× (5 tiers, configurable timeout)
- [x] Full transaction logging with `TransactionType` enum tags
- [x] Async balance writes with 500ms flush (HikariCP)
- [x] `/baltop` and `/baltop tokens` leaderboards
- [x] Sell rate limiting (anti-macro, configurable interval)

---

## Mine System

**plugin-mines + core-regions**

- [x] 14 standard mine tiers (Pit of Souls → Tomb of Anubis)
- [x] 4 donor-exclusive mines (Sanctum of Amun → Chamber of the Gods)
- [x] Configurable block composition per mine (exact percentages)
- [x] Auto-reset on timer (default 15 min) or threshold (default 80% mined)
- [x] Smooth batch reset: configurable blocks-per-tick (no TPS spikes)
- [x] Donor session timer with configurable limit and auto-teleport on expiry
- [x] Mine type system: STANDARD / DONOR / PRESTIGE
- [x] Admin commands: `/mine setcorner1/2`, `/mine setspawn`, `/mine enable/fill/reset`
- [x] Per-mine sell price overrides (override economy defaults per mine)

---

## Custom Khopesh (Pickaxe)

**plugin-pickaxe**

- [x] 11 custom enchantments with Egyptian mythology names
- [x] 3 vanilla enchants purchasable via Relics (Efficiency, Fortune, Silk Touch)
- [x] Per-enchant max level and per-level Relic costs (all configurable)
- [x] Full Relic investment to max all enchants: ~654,000 Relics
- [x] Enchant menu GUI with current level, upgrade cost, and lore description
- [x] Personalized Khopesh — item displays player's name
- [x] Tokenator enchant — earns Relics from mined blocks (creates Relic flywheel)

**Enchant list:**
Anubis' Speed, Luck of Thoth, Wrath of Seth, Eye of Ra, Wrath of Apophis, Hand of Ptah,
Forge of Ptah, Relic Sight, Blessing of Shu, Leap of Horus, Heartbeat of Sekhmet

---

## Ascension (Prestige)

**plugin-prestige**

- [x] 10 Ascension tiers (First Awakening → Living God)
- [x] Rank resets on Ascension; Coins, Relics, and enchants preserved
- [x] +2% Relic earn bonus per Ascension level (compounding)
- [x] 10 Prestige Shop points per Ascension
- [x] Prestige Shop: Mine Profit and Relic Mastery upgrade trees
- [x] Configurable per-tier console command rewards
- [x] Server-wide broadcast on Ascension with custom MiniMessage format
- [x] Unique title prefix per Ascension tier in chat and scoreboard

---

## GUI System

**plugin-menu**

- [x] 28 interconnected GUIs — all accessible from the main menu
- [x] Design-spec compliant: no filler, content top-left, controls bottom row
- [x] Slot 0 = back/close on all menus
- [x] MiniMessage throughout — no legacy `§` codes
- [x] Egyptian theme: gold/sand palette, tomb names, divine voice
- [x] Rotating gameplay tips in main menu
- [x] Confirmation dialogs for destructive actions (prestige, rank-up)

**GUI list:**
Main Menu, Divine Ranks, Sacred Tombs, Khopesh, Divine Enchantments, Sacred Quests,
Merchant's Bazaar (Shop), Grand Bazaar (Auction House), Sphinx's Gamble (Coinflip),
Pharaoh's Relics (Crates), Dynasty Home, Hall of Legends (Leaderboards), Divine Adornments,
Divine Blessings (Boosts), Divine Kits, Sacred Warps, Settings, Shadow Bazaar (stub),
Ascension, Ascension Shop, Sell Center, Shop Category Picker, Shop Category Page, Shop Quantity

---

## Crates System

**plugin-crates**

- [x] 3 crate tiers: Canopic Chest, Tomb Chest, Pharaoh's Reliquary
- [x] Weighted loot tables with configurable rewards
- [x] Reward types: items, console commands (for Relics, cosmetics, etc.)
- [x] Key system: keys are inventory items, consumed on crate open
- [x] Admin command to give keys to players
- [x] Crate block placement in world

---

## Kits System

**plugin-kits**

- [x] Configurable kit definitions with items, names, lore, enchantments
- [x] Cooldown support (per-kit, in seconds)
- [x] One-time kit flag (can never claim again)
- [x] Rank requirement gate (e.g., must be Rank E to claim rankE kit)
- [x] Permission requirement gate
- [x] 5 rank milestone kits: rankE, rankJ, rankO, rankT, rankZ
- [x] Admin override: `/kit give <player> <kit>` bypasses all checks

---

## Quests System

**plugin-quests**

- [x] Daily quests (reset every 24 hours)
- [x] Weekly quests (reset every 7 days)
- [x] Multiple objective types: mine-blocks, sell-amount, rank-up, etc.
- [x] Configurable Coin and Relic rewards per quest
- [x] Quest progress GUI with timer display
- [x] Auto-claim on completion

---

## Auction House

**plugin-auctionhouse**

- [x] Player-to-player item listings
- [x] Configurable listing duration
- [x] Item pickup on listing expiry (mail system)
- [x] Admin view of all listings
- [x] Sort/filter GUI options
- [x] Coin transactions between buyer and seller

---

## Coinflip

**plugin-coinflip**

- [x] Player creates a coinflip with a Coin wager
- [x] Any player can accept an open coinflip
- [x] 50/50 randomized outcome
- [x] Transaction logging: `COINFLIP_BET`, `COINFLIP_WIN`, `COINFLIP_REFUND`
- [x] Persistent `coinflip_logs` table for dispute resolution
- [x] Cancel/refund with `COINFLIP_REFUND` transaction type
- [x] Permission gate: `prison.coinflip.use` (default: true)

---

## Dynasty (Gang) System

**plugin-gangs**

- [x] Dynasty creation, joining, and leaving
- [x] Dynasty-exclusive mine (shared resource)
- [x] Member contribution tracking
- [x] Dynasty leaderboard (Hall of Legends)
- [x] Dynasty chat prefix in messages
- [x] Dynasty upgrades (upgradeable capabilities)
- [x] Dynasty treasury (shared Coin pool)

---

## Cosmetics / Tags

**plugin-cosmetics**

- [x] Chat tag system with custom display tags
- [x] Particle effect cosmetics
- [x] Tag equipped/unequipped status in GUI
- [x] Per-cosmetic unlock requirements (permission or purchase)
- [x] Live chat preview in cosmetics GUI

---

## Leaderboards

**plugin-leaderboards**

- [x] Richest players leaderboard
- [x] Top Dynasty leaderboard
- [x] Top Ascension level leaderboard
- [x] Configurable refresh interval
- [x] Hall of Legends GUI with category selection

---

## Chat System

**plugin-chat**

- [x] Mine rank prefix in chat
- [x] Donor rank prefix (shown alongside mine rank)
- [x] Ascension prefix (shown before mine rank)
- [x] Staff rank override display
- [x] Full MiniMessage format — no legacy color codes
- [x] Configurable per-rank prefix strings

---

## Anti-Cheat

**plugin-anticheat**

- [x] Automated flag system for suspicious behavior
- [x] Staff review queue GUI
- [x] Per-player flag history
- [x] Admin clear flags command
- [x] Indexed DB storage for flag queries

---

## Permission Engine

**core-permissions**

- [x] Custom permission engine — zero LuckPerms dependency
- [x] Three parallel permission trees: mine rank, donor rank, staff rank
- [x] Database-backed storage (MySQL)
- [x] Per-player permission grant/revoke/check
- [x] Cumulative mine rank permissions (Rank J = A through J)
- [x] Wildcard support (`prison.admin.*`)

---

## Database & Infrastructure

**core-database + core-regions**

- [x] HikariCP connection pool (configurable size, timeout)
- [x] Async write queue with 500ms flush interval
- [x] `CREATE TABLE IF NOT EXISTS` on every table (safe first-run + restart)
- [x] Full transaction log for all economy events
- [x] BungeeCord plugin messaging channel registered
- [x] `MessagingAPI` helpers: `connectToServer`, `requestServerName`, `forward`
- [x] Region system for mine bounding boxes
- [x] `executeAndGetId()` for INSERT + auto-key retrieval

---

## Staff & Admin Tools

**plugin-staff + plugin-admintoolkit**

- [x] Vanish, spectate, freeze, mute, ban, kick
- [x] Admin economy toolkit GUI (give/take/set with confirmations)
- [x] All staff actions logged to `staff_actions` table
- [x] Anticheat review integration
- [x] Configurable staff permission tiers (helper/mod/admin/owner)

---

*See `PRODUCT_AUDIT.md` for an honest assessment of what's complete vs. in progress.*

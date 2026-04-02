# PROJECT STRUCTURE

> **Purpose:** Map of every module, its dependencies, and where to find key configuration.
> Reference this when you need to know where something lives.

---

## Module Dependency Graph

```
core-database  ─────────────────────────── (no game dependencies)
      │
      └──► core-permissions ────────────── (depends on core-database)
      │
      └──► core-regions ────────────────── (depends on core-database)
      │
      └──► plugin-economy ─────────────── (depends on core-database)
      │
      └──► All other plugins ──────────── (depend on core-database)
                │
                └──► plugin-ranks ──────── (depends on core-database, core-permissions, plugin-economy)
                └──► plugin-mines ──────── (depends on core-database, core-permissions, core-regions, plugin-economy)
                                           (soft depends: plugin-donor)
                └──► plugin-prestige ───── (depends on core-database, plugin-economy, plugin-ranks)
                └──► plugin-shop ──────── (depends on core-database, plugin-economy)
                └──► plugin-menu ──────── (depends on most plugins via soft-dependency)
                └──► plugin-tebex ──────── (depends on core-database, plugin-donor)
```

Key rule: `core-database` is the foundation. It must start first.
Paper enforces load order via `depend:` in each plugin's `plugin.yml`.

---

## Module Reference

### `core-database`
**Purpose:** Shared database connection pool + plugin messaging
**Main class:** `com.prison.database.DatabasePlugin`
**Key classes:**
- `DatabaseManager` — HikariCP pool, `query()`, `execute()`, `queueWrite()`, `executeAndGetId()`
- `MessagingAPI` — BungeeCord plugin messaging helpers (`connectToServer`, `requestServerName`, `forward`)
**Config:** `plugins/PrisonDatabase/config.yml` — DB host, port, credentials, pool size

---

### `core-permissions`
**Purpose:** Custom permission engine — no LuckPerms dependency
**Main class:** `com.prison.permissions.PermissionsPlugin`
**Key API:**
- `PermissionsAPI.hasPermission(player, node)` — Check a permission
- `PermissionsAPI.grant(player, node)` — Grant a permission
- `PermissionsAPI.revoke(player, node)` — Remove a permission
**DB tables:** `player_permissions`
**No config** — all data is in the database

---

### `core-regions`
**Purpose:** Region definitions for mines, warps, and protected zones
**Main class:** `com.prison.regions.RegionsPlugin`
**Key API:**
- `RegionsAPI.getRegion(id)` — Get a named region
- `RegionsAPI.isInRegion(location, id)` — Check if location is in a region
**DB tables:** `regions`
**No config** — regions are defined in-game via admin commands

---

### `plugin-economy`
**Purpose:** Coin and Relic balance management, sell logic, transaction history
**Main class:** `com.prison.economy.EconomyPlugin`
**Key API:**
- `EconomyAPI.getBalance(uuid)` — Get Coin balance
- `EconomyAPI.getTokenBalance(uuid)` — Get Relic balance
- `EconomyAPI.addBalance(uuid, amount, type)` — Add Coins with transaction type
- `EconomyAPI.removeBalance(uuid, amount, type)` — Remove Coins
- `EconomyAPI.getSellPrice(material)` — Get sell price for a block type
**DB tables:** `player_wallets`, `transaction_log`
**Config:** `plugin-economy/src/main/resources/config.yml` — sell prices, streak settings

---

### `plugin-ranks`
**Purpose:** 26-rank (A→Z) progression system
**Main class:** `com.prison.ranks.RanksPlugin`
**Key API:**
- `RanksAPI.getRank(uuid)` — Get player's current rank letter
- `RanksAPI.rankUp(uuid)` — Attempt rank-up (checks balance)
**DB tables:** `player_ranks`
**Config:** `plugin-ranks/src/main/resources/config.yml` — rank costs, display names, prefixes

---

### `plugin-mines`
**Purpose:** Mine definitions, block filling, reset system, donor sessions
**Main class:** `com.prison.mines.MinesPlugin`
**Key classes:**
- `MineManager` — Load/save mines, fill, reset
- `MineData` — Record type: id, display, composition, corners, type, permissions
**DB tables:** None (mines stored in config.yml, region coords in regions table)
**Config:** `plugin-mines/src/main/resources/config.yml` — all mine definitions

---

### `plugin-permissions` (core-permissions) → permission nodes
**All known permission nodes:**
- `prison.admin.*` — Full admin access
- `prison.mine.[a-z]` — Mine access by rank (granted automatically on rank-up)
- `prison.donor.devotee` / `acolyte` / `highpriest` / `chosen` — Donor tiers
- `prison.staff.helper` / `mod` / `admin` / `owner` — Staff tiers
- `prison.coinflip.use` — Coinflip access (default: true)
- `prison.prestige.use` — Prestige access (granted at rank Z)

---

### `plugin-prestige`
**Purpose:** Prestige/Ascension system
**Config:** `plugin-prestige/src/main/resources/config.yml`
- `token-multiplier-per-prestige` — Relic bonus per ascension level
- `prestige-shop-points-per-prestige` — Points per ascension
- `rewards` — Per-tier console command rewards

---

### `plugin-menu`
**Purpose:** All 21 GUI classes + menu navigation
**Main class:** `com.prison.menu.MenuPlugin`
**GUI classes:** See `GUI_MASTER_SPEC.md` for full list
**Key utilities:**
- `Fmt.java` — Text formatting helpers
- `Gui.java` — Inventory building (create inventory, set items)
- `Sounds.java` — Sound effect constants
- `TopBand.java` — GUI top band/header component

---

### `plugin-pickaxe`
**Purpose:** Custom Khopesh enchant system
**Config:** `plugin-pickaxe/src/main/resources/config.yml`
- All enchant definitions with max levels and token costs
- 11 custom enchants + 3 vanilla enchants via token purchase

---

### `plugin-crates`
**Purpose:** Crate key system and loot tables
**Config:** `plugin-crates/src/main/resources/config.yml`
- 3 crate types: canopic, tomb, pharaoh (Egyptian naming in Phase 6)
- Loot tables with weighted rewards

---

### `plugin-kits`
**Purpose:** Kit management — admin, rank-milestone, and donor kits
**Config:** `plugin-kits/src/main/resources/config.yml`
- Starter kit, rank kits (E, J, O, T, Z), donor welcome kits

---

### `plugin-quests`
**Purpose:** Daily and weekly quest system
**Config:** `plugin-quests/src/main/resources/config.yml`
- Quest definitions, objectives, rewards

---

### `plugin-auctionhouse`
**Purpose:** Player-to-player auction listings with bidding and expiry
**Config:** `plugin-auctionhouse/src/main/resources/config.yml`
**DB tables:** `auction_listings`, `auction_bids`

---

### `plugin-coinflip`
**Purpose:** Coin gambling between two players
**Config:** `plugin-coinflip/src/main/resources/config.yml`
**DB tables:** `coinflip_tickets`, `coinflip_logs`

---

### `plugin-chat`
**Purpose:** Chat formatting — rank prefix, donor prefix, staff prefix
**Reads from:** `RanksAPI`, `DonorAPI`, `PermissionsAPI` to build chat format

---

### `plugin-cosmetics`
**Purpose:** Cosmetic tags and visual effects
**Config:** `plugin-cosmetics/src/main/resources/config.yml`
**DB tables:** `player_cosmetics`

---

### `plugin-gangs`
**Purpose:** Dynasty (gang) system with shared mines and leaderboards
**DB tables:** `gangs`, `gang_members`, `gang_contributions`

---

### `plugin-leaderboards`
**Purpose:** Holographic leaderboards — richest players, top Dynasty, top Ascension
**Reads from:** Economy, Ranks, Prestige, and Gangs databases

---

### `plugin-donor`
**Purpose:** Donor rank management and DonorAPI
**Key API:**
- `DonorAPI.isDonor(uuid)` — Check if player has any donor rank
- `DonorAPI.getDonorRank(uuid)` — Get specific donor rank name
**DB tables:** `donor_ranks`

---

### `plugin-shop`
**Purpose:** Item shop with buy/sell functionality (BETA)
**Config:** `plugin-shop/src/main/resources/config.yml`
**Status:** BETA — prices and categories need validation before launch

---

### `plugin-tebex`
**Purpose:** Tebex donation webhook handler (BETA)
**Config:** `plugin-tebex/src/main/resources/config.yml` — webhook secret, port
**Status:** BETA — webhook handler not fully implemented

---

### `plugin-events`
**Purpose:** Scheduled and manual server events (Ra's Blessing, etc.)
**Config:** `plugin-events/src/main/resources/config.yml`

---

### `plugin-anticheat`
**Purpose:** Flag suspicious behavior for admin review
**DB tables:** `anticheat_flags`
**Admin command:** `/anticheat review`

---

### `plugin-staff`
**Purpose:** Staff tools — vanish, fly, spectate, teleport, report review
**Permission required:** `prison.staff.helper` or higher

---

### `plugin-admintoolkit`
**Purpose:** Admin economy tools (give/take/set Coins and Relics), player management
**GUI:** `EcoToolsGUI.java` — 6 operations with logging
**All operations logged to `staff_actions` table**

---

### `plugin-warps`
**Purpose:** Warp point management
**Config:** `plugin-warps/src/main/resources/config.yml` — warp name → location

---

## Finding Configuration

| I want to change... | File |
|---|---|
| Sell prices | `plugin-economy/src/main/resources/config.yml` |
| Rank costs | `plugin-ranks/src/main/resources/config.yml` |
| Mine block composition | `plugin-mines/src/main/resources/config.yml` |
| Pickaxe enchant costs | `plugin-pickaxe/src/main/resources/config.yml` |
| Crate loot tables | `plugin-crates/src/main/resources/config.yml` |
| Kit contents | `plugin-kits/src/main/resources/config.yml` |
| Prestige costs/rewards | `plugin-prestige/src/main/resources/config.yml` |
| Database connection | `plugins/PrisonDatabase/config.yml` (runtime) |

---

## Adding a New Module

1. Create `plugin-newfeature/` directory
2. Add `plugin-newfeature/build.gradle` with `compileOnly project(':core-database')` etc.
3. Add `include 'plugin-newfeature'` to `settings.gradle`
4. Create `plugin.yml` with correct `depend:` entries
5. Write your `JavaPlugin` main class
6. Build with `./gradlew :plugin-newfeature:shadowJar`

The `DatabaseManager.getInstance()` is available to any plugin after `core-database` loads.

---

*See `CONFIG_GUIDE.md` for documentation of every config key.*
*See `DEPLOYMENT_GUIDE.md` for server setup steps.*

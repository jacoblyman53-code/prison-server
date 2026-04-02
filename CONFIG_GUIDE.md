# CONFIG GUIDE

> **Purpose:** Every configuration key documented across all 26 plugins.
> Use this to tune the server without reading source code.
> "Hot-reload" = can be changed with `/reload confirm` or plugin reload. "Restart" = requires server restart.

---

## core-database — `plugins/PrisonDatabase/config.yml`
*(Generated at runtime, not in the source tree)*

| Key | Default | Type | Reload? | Description |
|---|---|---|---|---|
| `database.host` | `localhost` | String | Restart | MySQL server hostname or IP |
| `database.port` | `3306` | Int | Restart | MySQL port |
| `database.name` | `prison_db` | String | Restart | Database name |
| `database.username` | `root` | String | Restart | MySQL username |
| `database.password` | `""` | String | Restart | MySQL password |
| `database.pool-size` | `10` | Int | Restart | HikariCP connection pool size. Increase for 100+ players. |
| `database.connection-timeout` | `30000` | Int (ms) | Restart | Max wait for a connection before error |

---

## plugin-economy — `plugin-economy/src/main/resources/config.yml`

| Key | Default | Type | Reload? | Description |
|---|---|---|---|---|
| `currency-symbol` | `$` | String | Hot | Display symbol for Coins in legacy contexts |
| `token-symbol` | `T` | String | Hot | Display symbol for Relics |
| `min-sell-interval-ms` | `500` | Int | Hot | Minimum ms between `/sell` commands (anti-macro) |
| `sell-streak-timeout-seconds` | `60` | Int | Hot | Seconds after a sell before streak resets |
| `baltop-refresh-seconds` | `60` | Int | Hot | How often the top balance leaderboard refreshes |
| `auto-save-seconds` | `300` | Int | Restart | Wallet auto-save interval (safety net) |
| `sell-prices.*` | (see config) | Map | Hot | Sell price per material. Key = Bukkit material name. |

**Changing sell prices:** Edit the `sell-prices:` block. Changes take effect on hot-reload.
**Risk:** Large sell price increases can cause economy inflation. Test on a staging server first.

---

## plugin-ranks — `plugin-ranks/src/main/resources/config.yml`

| Key | Default | Type | Reload? | Description |
|---|---|---|---|---|
| `autoteleport-default` | `true` | Boolean | Hot | New players auto-teleport to their mine on rank-up |
| `rankup-message` | (see config) | String | Hot | Chat message on successful rank-up. `{rank}` `{display}` |
| `rankup-broadcast` | `""` | String | Hot | Broadcast on rank-up (empty = no broadcast) |
| `cannot-afford-message` | (see config) | String | Hot | Message when player can't afford rank-up |
| `max-rank-message` | (see config) | String | Hot | Message when player is already Pharaoh (max rank) |
| `ranks.A.cost` | `0` | Long | Restart | Cost to rank up FROM rank A (should always be 0) |
| `ranks.B.cost` | `5000` | Long | Restart | Cost to rank up to rank B |
| `ranks.Z.cost` | `500000000` | Long | Restart | Cost to rank up to Pharaoh |
| `ranks.*.display` | `"Rank X"` | String | Hot | Display name in GUIs and messages |
| `ranks.*.prefix` | (see config) | String | Hot | MiniMessage prefix used in chat |

**Changing rank costs:** Edit `ranks.X.cost`. Requires restart to take effect in the rank system.
**Warning:** Reducing rank costs mid-server can break economy balance if rich players already have coins saved.

---

## plugin-mines — `plugin-mines/src/main/resources/config.yml`

| Key | Default | Type | Reload? | Description |
|---|---|---|---|---|
| `default-donor-session-mins` | `30` | Int | Hot | Session length for DONOR mines in minutes. 0 = unlimited. |
| `reset-batch-size` | `1000` | Int | Restart | Blocks reset per tick during mine fill. Higher = faster but more TPS impact. |
| `reset-warning-message` | (see config) | String | Hot | Broadcast to mine on reset start |
| `reset-done-message` | (see config) | String | Hot | Broadcast to mine on reset completion |
| `mines.*.enabled` | `false` | Boolean | Hot | Whether this mine is active |
| `mines.*.display` | (see config) | String | Hot | MiniMessage display name shown in GUI |
| `mines.*.world` | `"world"` | String | Restart | World name for this mine |
| `mines.*.corner1` | `[0,64,0]` | List | Restart | Mine bounding box corner 1 [x,y,z] |
| `mines.*.corner2` | `[0,64,0]` | List | Restart | Mine bounding box corner 2 [x,y,z] |
| `mines.*.spawn-x/y/z` | `0.5/66/0.5` | Double | Restart | Teleport point for mine entry/reset |
| `mines.*.composition.*` | (see config) | Map | Hot | Block type → integer weight. Must sum to 100. |
| `mines.*.sell-prices` | `{}` | Map | Hot | Override sell prices for this mine specifically. Empty = use economy defaults. |
| `mines.*.reset-timer-mins` | `15` | Int | Hot | Auto-reset interval in minutes. 0 = disable timer. |
| `mines.*.reset-threshold` | `0.80` | Double | Hot | Reset when this fraction of blocks is mined (0.0–1.0). 0 = disable. |
| `mines.*.permission-node` | `prison.mine.a` | String | Restart | Permission required to mine here |
| `mines.*.mine-type` | `STANDARD` | Enum | Restart | STANDARD, DONOR, or PRESTIGE |
| `mines.*.prestige-required` | `0` | Int | Restart | Minimum Ascension level for PRESTIGE type mines |
| `mines.*.donor-session-mins` | (default) | Int | Hot | Override session length for this specific DONOR mine |

**Adding a new mine:** Copy an existing mine block, give it a new ID, set `enabled: false`,
configure corners/spawn via in-game commands, then set `enabled: true`.

---

## plugin-prestige — `plugin-prestige/src/main/resources/config.yml`

| Key | Default | Type | Reload? | Description |
|---|---|---|---|---|
| `token-multiplier-per-prestige` | `0.02` | Double | Hot | Additive Relic earn bonus per Ascension level (+2% per) |
| `broadcast-message` | (see config) | String | Hot | Server-wide broadcast on Ascension. `{player}` `{prestige}` |
| `prefix-format` | `<dark_purple>[<light_purple>P{level}</light_purple>]</dark_purple>` | String | Hot | Ascension level prefix. `{level}` = number |
| `prestige-shop-points-per-prestige` | `10` | Int | Hot | Points awarded per Ascension for the Prestige Shop |
| `max-prestige-perms` | `50` | Int | Restart | Max Ascension level that gets its own permission node |
| `rewards.*` | (see config) | Map | Hot | Per-tier commands run on reaching that Ascension level |

---

## plugin-pickaxe — `plugin-pickaxe/src/main/resources/config.yml`

| Key | Default | Type | Description |
|---|---|---|---|
| `custom-enchants.*.display` | (name) | String | Display name in GUI |
| `custom-enchants.*.max-level` | (varies) | Int | Maximum purchasable level |
| `custom-enchants.*.icon` | (material) | String | Bukkit material for the GUI icon |
| `custom-enchants.*.token-costs.*` | (varies) | Long | Cost in Relics for each level |
| `vanilla-enchants.*` | same structure | — | Efficiency, Fortune, Silk Touch with Relic costs |

**Changing enchant costs:** Edit `token-costs`. Hot-reload supported.
**Warning:** Reducing costs mid-server means players who bought at the higher price were overcharged. Consider refund policy.

---

## plugin-kits — `plugin-kits/src/main/resources/config.yml`

| Key | Description |
|---|---|
| `kits.*.display` | Display name in GUI |
| `kits.*.cooldown` | Cooldown in seconds (0 = once only for one-time kits) |
| `kits.*.one-time` | If true, can only be claimed once ever |
| `kits.*.required-rank` | Required mine rank letter (e.g., `E`) |
| `kits.*.required-permission` | Alternative permission node gate |
| `kits.*.items` | List of items with material, amount, name, lore, enchantments |

---

## plugin-crates — `plugin-crates/src/main/resources/config.yml`

| Key | Description |
|---|---|
| `crates.*.display` | Crate display name |
| `crates.*.key-item` | Material and name of the key item |
| `crates.*.rewards` | Weighted reward list — each entry has `weight`, `commands` or `items` |

**Adding a reward:** Add an entry to `rewards` with a `weight` (higher = more common)
and either `items` (give items) or `commands` (run console commands on win).

---

## plugin-coinflip — `plugin-coinflip/src/main/resources/config.yml`

| Key | Description |
|---|---|
| `min-bet` | Minimum Coins for a coinflip |
| `max-bet` | Maximum Coins for a coinflip (0 = no limit) |
| `house-cut-percent` | % of pot taken as fee (0 = no fee) — see `ECONOMY_BALANCE_TODO.md` |

---

## plugin-quests — `plugin-quests/src/main/resources/config.yml`

| Key | Description |
|---|---|
| `daily-quests` | List of daily quest definitions |
| `weekly-quests` | List of weekly quest definitions |
| `quests.*.display` | Quest display name |
| `quests.*.objective` | Objective type (mine-blocks, sell-amount, rank-up, etc.) |
| `quests.*.target` | Target number for the objective |
| `quests.*.rewards.coins` | Coin reward |
| `quests.*.rewards.tokens` | Relic reward |

---

## Safe to Change at Any Time (Hot-Reload)

These keys can be changed while the server is running and reloaded with `/reload confirm`
(or the plugin's own reload command) without risk:
- Sell prices
- Rank display names and prefixes
- Message strings
- Sell streak timeout
- Mine display names and reset timers
- Quest rewards

## Dangerous to Change Mid-Server

These keys affect economy balance. Change only during maintenance windows:
- Rank costs (reduces/increases player grind remaining)
- Mine block composition (changes income rates immediately)
- Sell streak multiplier tiers
- Enchant costs (affects all future purchases)
- Prestige multiplier per level

---

*See `EXTENSION_GUIDE.md` for how to add new items to configs (mines, ranks, etc.)*
*See `ECONOMY_BALANCE_TODO.md` for specific values recommended for adjustment.*

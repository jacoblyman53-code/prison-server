# EXTENSION GUIDE

> **Purpose:** Step-by-step instructions for common extensions buyers will want to make.
> No source code changes required for any of these — config only.

---

## Adding a New Rank

Ranks are defined in `plugin-ranks/src/main/resources/config.yml`.

### Step 1 — Add the rank entry
```yaml
ranks:
  # ... existing ranks ...
  AA:
    cost: 750000000
    display: "Grand Pharaoh"
    prefix: "<dark_purple>[<gold><bold>Grand Pharaoh</bold></gold>]</dark_purple>"
```

### Step 2 — Add a permission node
The permission node `prison.mine.aa` (lowercase) needs to be registered.
Grant it to yourself: `/permissions grant <player> prison.mine.aa`

For it to be automatically granted on rank-up, the rank system uses the letter/key
from the config as the permission suffix. A key of `AA` → `prison.mine.aa`.

### Step 3 — Optionally add a mine
See "Adding a New Mine" below — create a mine that requires `prison.mine.aa`.

### Step 4 — Restart the server
Rank cost data is loaded at startup.

---

## Adding a New Mine

Mines are defined in `plugin-mines/src/main/resources/config.yml`.

### Step 1 — Add the mine entry
```yaml
mines:
  # ... existing mines ...
  donor_sun:
    enabled: false
    display: "<gold>Sanctum of the Sun</gold>"
    world: "world"
    corner1: [0, 64, 0]
    corner2: [0, 64, 0]
    spawn-x: 0.5
    spawn-y: 66.0
    spawn-z: 0.5
    spawn-yaw: 0.0
    spawn-pitch: 0.0
    composition:
      DIAMOND_ORE: 50
      EMERALD_ORE: 50
    sell-prices: {}
    reset-timer-mins: 10
    reset-threshold: 0.80
    permission-node: "prison.donor.devotee"
    mine-type: "DONOR"
    prestige-required: 0
    donor-session-mins: 30
```

### Step 2 — Set the mine corners in-game
```
/mine setcorner1 donor_sun
/mine setcorner2 donor_sun
/mine setspawn donor_sun
```

### Step 3 — Enable and fill
```
/mine enable donor_sun
/mine fill donor_sun
```

### Step 4 — Test
Teleport to the mine: `/mine tp donor_sun`

---

## Adding a New Crate Tier

Crates are defined in `plugin-crates/src/main/resources/config.yml`.

### Step 1 — Add crate definition
```yaml
crates:
  # ... existing crates ...
  divine:
    display: "<light_purple>Chamber of the Gods Crate</light_purple>"
    key-item:
      material: AMETHYST_SHARD
      name: "<light_purple><!italic>Chamber Key"
    rewards:
      - weight: 50
        coins: 500000
      - weight: 30
        tokens: 2000
      - weight: 15
        items:
          - material: DIAMOND
            amount: 64
      - weight: 5
        commands:
          - "prisoncosmetic unlock {player} divine_halo"
```

### Step 2 — Place a crate in the world
Use the admin command (verify syntax with `/crates help`):
```
/crates place divine
```
Right-click the crate block to activate it.

### Step 3 — Give a test key
```
prisoncratekey give YourName divine 1
```

---

## Adding a New Custom Enchant

Custom enchants are defined in `plugin-pickaxe/src/main/resources/config.yml`.

**Note:** Adding a completely new enchant type (one with new behavior) requires
source code changes. Adding a new level to an existing enchant or adding a new
"vanilla" enchant (one handled by Minecraft's enchant system) is config-only.

### To add a new purchasable vanilla enchant tier
```yaml
vanilla-enchants:
  mending:
    display: "Mending"
    max-level: 1
    icon: EXPERIENCE_BOTTLE
    token-costs:
      1: 25000
```

### To increase max level of an existing custom enchant
```yaml
custom-enchants:
  explosive:
    max-level: 7    # was 5
    token-costs:
      6: 500000
      7: 1500000
```

---

## Adding a New Daily Quest Type

Quests are defined in `plugin-quests/src/main/resources/config.yml`.

```yaml
daily-quests:
  # ... existing quests ...
  mine_emeralds:
    display: "Mine 100 Emerald Ore"
    description: "Extract 100 Emerald Ore blocks from any Sacred Tomb"
    objective:
      type: "mine-blocks"
      block: "EMERALD_ORE"
      target: 100
    rewards:
      coins: 50000
      tokens: 150
```

### Supported Objective Types
| Type | Target | Description |
|---|---|---|
| `mine-blocks` | block count | Mine X blocks of a specific type |
| `sell-amount` | coin amount | Sell until you've earned X Coins total |
| `rank-up` | rank count | Rank up X times |
| `open-crates` | crate count | Open X crates |
| `kill-players` | kill count | Kill X players (PvP events) |
| `complete-quests` | quest count | Meta-quest: complete X other quests |

> Verify which objective types are actually implemented in `plugin-quests` source code.
> If a type isn't recognized, the quest will either not progress or produce an error.

---

## Adding a New Donor Rank Tier

If you want to add a 5th donor rank (e.g., "Demi-God"):

### Step 1 — Add permission node
Grant yourself the permission to test:
```
/permissions grant <player> prison.donor.demigod
```

### Step 2 — Define perks in plugin-donor config
```yaml
donor-ranks:
  demigod:
    display: "<color:#B983FF>Demi-God"
    prefix: "<color:#B983FF>[Demi-God]"
    mines:
      - demigod_mine
    sell-bonus: 0.15
    session-mins: 90
```
*(Exact config format depends on plugin-donor implementation — verify keys)*

### Step 3 — Add Tebex package
Follow `TEBEX_PRODUCT_MAP.md` pattern to create the Tebex package.

---

## Adding a New Warp

Warps are managed via commands:
```
/warp create <name>    (while standing at the desired location)
/warp delete <name>
/warp list
```

To require a permission for a warp:
```
/warp setpermission <name> <permission.node>
```

---

## Changing Server Theme (Advanced)

If you want to change the Egyptian theme to something else:

1. Update `PRODUCT_IDENTITY.md` with your new theme (for documentation)
2. Edit all 26 rank `display:` names in `plugin-ranks/config.yml`
3. Edit mine `display:` names in `plugin-mines/config.yml`
4. Update GUI strings in `plugin-menu` source code (requires recompile)
5. Update `plugin-chat` prefix formats
6. Update `plugin-prestige` prefix-format

The cleanest approach: update the config files (hot-reload) first, then tackle the
GUI source code in a follow-up build.

---

*See `CONFIG_GUIDE.md` for documentation of every config key.*
*See `ADMIN_EDITING_GUIDE.md` for in-game admin commands.*

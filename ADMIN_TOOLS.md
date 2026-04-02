# ADMIN TOOLS

> **Purpose:** Complete list of all admin commands across all plugins, with syntax,
> required permissions, and usage examples.
> Organized by plugin for quick reference.

---

## core-permissions

| Command | Permission | Description |
|---|---|---|
| `/permissions grant <player> <node>` | `prison.admin.*` | Grant a permission node |
| `/permissions revoke <player> <node>` | `prison.admin.*` | Remove a permission node |
| `/permissions list <player>` | `prison.admin.*` | List all permissions for a player |
| `/permissions check <player> <node>` | `prison.admin.*` | Check if player has a specific node |

---

## plugin-economy

| Command | Permission | Description |
|---|---|---|
| `/eco give <player> igc <amount>` | `prison.admin.eco` | Give Coins to a player |
| `/eco take <player> igc <amount>` | `prison.admin.eco` | Remove Coins from a player |
| `/eco set <player> igc <amount>` | `prison.admin.eco` | Set a player's Coin balance |
| `/eco give <player> tokens <amount>` | `prison.admin.eco` | Give Relics to a player |
| `/eco take <player> tokens <amount>` | `prison.admin.eco` | Remove Relics from a player |
| `/eco set <player> tokens <amount>` | `prison.admin.eco` | Set a player's Relic balance |
| `/eco balance <player>` | `prison.admin.eco` | View a player's balances |
| `/baltop` | (any) | View top Coin holders |
| `/baltop tokens` | (any) | View top Relic holders |

All eco operations are logged to `staff_actions` table.

---

## plugin-ranks

| Command | Permission | Description |
|---|---|---|
| `/rank set <player> <letter>` | `prison.admin.ranks` | Force-set a player's rank |
| `/rank check <player>` | `prison.admin.ranks` | Check a player's current rank |
| `/rank list` | `prison.admin.ranks` | List all ranks with costs |
| `/rankup` | (any) | Player ranks themselves up (no admin perm) |

---

## plugin-mines

| Command | Permission | Description |
|---|---|---|
| `/mine setcorner1 <id>` | `prison.admin.mines` | Set corner 1 of mine bounding box |
| `/mine setcorner2 <id>` | `prison.admin.mines` | Set corner 2 of mine bounding box |
| `/mine setspawn <id>` | `prison.admin.mines` | Set mine teleport point |
| `/mine enable <id>` | `prison.admin.mines` | Activate a mine |
| `/mine disable <id>` | `prison.admin.mines` | Deactivate a mine |
| `/mine fill <id>` | `prison.admin.mines` | Fill mine with its block composition |
| `/mine reset <id>` | `prison.admin.mines` | Fill + broadcast reset to players inside |
| `/mine info <id>` | `prison.admin.mines` | View mine configuration |
| `/mine list` | `prison.admin.mines` | List all mines and their status |
| `/mine tp <id>` | `prison.admin.mines` | Teleport to mine spawn |
| `/mine` | `prison.admin.mines` | Shows mine help |

---

## plugin-prestige

| Command | Permission | Description |
|---|---|---|
| `/prestige check <player>` | `prison.admin.prestige` | View a player's Ascension level |
| `/prestige set <player> <level>` | `prison.admin.prestige` | Force-set Ascension level |
| `/prestige shop` | `prison.prestige.use` | Open Prestige Shop (player command) |
| `/prestige ascend` | `prison.prestige.use` | Attempt Ascension (player command) |

---

## plugin-donor

| Command | Permission | Description |
|---|---|---|
| `/donor grant <player> <rank>` | `prison.admin.donor` | Grant a donor rank |
| `/donor revoke <player>` | `prison.admin.donor` | Remove donor rank |
| `/donor check <player>` | `prison.admin.donor` | View a player's donor rank |
| `/donor list` | `prison.admin.donor` | List all players with donor ranks |

---

## plugin-kits

| Command | Permission | Description |
|---|---|---|
| `/kit give <player> <kit>` | `prison.admin.kits` | Give a kit, bypassing cooldown/requirements |
| `/kit list` | `prison.admin.kits` | List all kits |
| `/kit info <kit>` | `prison.admin.kits` | View kit contents |
| `/kit <name>` | (kit-specific) | Claim a kit (player command) |

---

## plugin-warps

| Command | Permission | Description |
|---|---|---|
| `/warp create <name>` | `prison.admin.warps` | Create warp at current location |
| `/warp delete <name>` | `prison.admin.warps` | Delete a warp |
| `/warp setpermission <name> <node>` | `prison.admin.warps` | Gate a warp behind a permission |
| `/warp list` | (any) | List available warps |
| `/warp <name>` | (any, or gated) | Warp to location |

---

## plugin-gangs

| Command | Permission | Description |
|---|---|---|
| `/gang disband <name>` | `prison.admin.gangs` | Force disband a Dynasty |
| `/gang kick <dynasty> <player>` | `prison.admin.gangs` | Remove player from Dynasty |
| `/gang info <name>` | `prison.admin.gangs` | View Dynasty details |
| `/gang list` | `prison.admin.gangs` | List all Dynasties |

---

## plugin-anticheat

| Command | Permission | Description |
|---|---|---|
| `/anticheat review` | `prison.staff.mod` | Open anticheat flag review GUI |
| `/anticheat clear <player>` | `prison.admin.anticheat` | Clear all flags for a player |
| `/anticheat flags <player>` | `prison.staff.mod` | View flags for a specific player |

---

## plugin-staff

| Command | Permission | Description |
|---|---|---|
| `/vanish` or `/v` | `prison.staff.helper` | Toggle vanish |
| `/spectate <player>` | `prison.staff.helper` | Spectate a player |
| `/freeze <player>` | `prison.staff.mod` | Freeze a player in place |
| `/unfreeze <player>` | `prison.staff.mod` | Unfreeze |
| `/mute <player> <duration>` | `prison.staff.mod` | Mute player for duration |
| `/unmute <player>` | `prison.staff.mod` | Remove mute |
| `/ban <player> [reason]` | `prison.staff.admin` | Ban a player |
| `/unban <player>` | `prison.staff.admin` | Unban |
| `/kick <player> [reason]` | `prison.staff.helper` | Kick from server |

---

## plugin-events

| Command | Permission | Description |
|---|---|---|
| `/event start <name>` | `prison.admin.events` | Start a server event |
| `/event stop <name>` | `prison.admin.events` | Stop an active event |
| `/event list` | `prison.admin.events` | List available events |

---

## plugin-crates

| Command | Permission | Description |
|---|---|---|
| `prisoncratekey give <player> <type> <amount>` | Console/admin | Give crate keys |
| `/crates place <type>` | `prison.admin.crates` | Place a crate block in the world |
| `/crates info <type>` | `prison.admin.crates` | View crate loot table |

---

## plugin-admintoolkit

Accessed via GUI: `/admintool` or from main menu (staff slot)

| Operation | Description |
|---|---|
| Give IGC | Give Coins with confirmation |
| Take IGC | Remove Coins with confirmation |
| Set IGC | Set Coin balance with confirmation |
| Give Tokens | Give Relics with confirmation |
| Take Tokens | Remove Relics with confirmation |
| Set Tokens | Set Relic balance with confirmation |

All operations logged. Take operations fail gracefully if balance insufficient.

---

## Leaderboards

| Command | Permission | Description |
|---|---|---|
| `/leaderboard` or `/lb` | (any) | Open leaderboard selector GUI |
| `/leaderboard refresh` | `prison.admin.*` | Force refresh all leaderboard caches |

---

*See `STAFF_OPERATIONS.md` for how to use these commands in real situations.*
*See `PERMISSIONS_MATRIX.md` for the full permission node list.*

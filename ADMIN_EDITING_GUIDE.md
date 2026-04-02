# ADMIN EDITING GUIDE

> **Purpose:** All in-game admin commands and how to use them.
> For server owners and senior staff managing The Pharaoh's Prison day-to-day.
> Requires permission: `prison.admin.*` for all commands below unless noted.

---

## Economy Commands

### Give Coins to a Player
```
/eco give <player> igc <amount>
/eco give <player> tokens <amount>
```
Example: `/eco give PlayerName igc 100000`

### Take Coins from a Player
```
/eco take <player> igc <amount>
/eco take <player> tokens <amount>
```
Note: Cannot take more than a player's balance. Silently caps at current balance.

### Set a Player's Balance
```
/eco set <player> igc <amount>
/eco set <player> tokens <amount>
```
Warning: Setting to 0 is effectively a wipe. Confirm with the player first.

### View a Player's Balance
```
/eco balance <player>
```

### View Top Balances
```
/baltop
/baltop tokens
```
Refreshes every 60 seconds. See `plugin-economy/config.yml` for interval setting.

All economy operations by staff are logged to `staff_actions` table with admin's UUID and timestamp.

---

## Mine Management Commands

### Set Mine Corners
```
/mine setcorner1 <id>    (stand at corner, run command)
/mine setcorner2 <id>    (stand at opposite corner)
/mine setspawn <id>      (stand at mine entrance)
```

### Enable/Disable a Mine
```
/mine enable <id>
/mine disable <id>
```

### Fill/Reset a Mine
```
/mine fill <id>          (fill with composition, no reset notification)
/mine reset <id>         (fill + broadcast reset message to players inside)
```

### View Mine Info
```
/mine info <id>
/mine list
```

### Teleport to Mine
```
/mine tp <id>
```

### Update Mine Composition (in-game)
Mine composition is in config. To change it:
1. Edit `plugin-mines/config.yml`
2. `/mine reload` (if supported) or restart
3. `/mine fill <id>` to apply the new composition

---

## Rank Commands

### Check a Player's Rank
```
/rank check <player>
```

### Force-Set a Player's Rank
```
/rank set <player> <letter>
```
Example: `/rank set PlayerName Z`
Warning: This bypasses cost checks. The player does not pay for the rank.

### View Rank Costs
```
/rank list
```

---

## Permission Commands

### Grant a Permission
```
/permissions grant <player> <node>
```
Example: `/permissions grant PlayerName prison.admin.*`

### Revoke a Permission
```
/permissions revoke <player> <node>
```

### List a Player's Permissions
```
/permissions list <player>
```

### Check a Specific Permission
```
/permissions check <player> <node>
```

---

## Donor Commands

### Grant a Donor Rank
```
/donor grant <player> <devotee|acolyte|highpriest|chosen>
```

### Revoke Donor Rank
```
/donor revoke <player>
```

### Check Donor Status
```
/donor check <player>
```

---

## Kit Commands

### Give a Kit to a Player (Admin Override)
```
/kit give <player> <kit-name>
```
Bypasses cooldown and requirement checks.

### List All Kits
```
/kit list
```

---

## Prestige/Ascension Commands

### Check a Player's Prestige Level
```
/prestige check <player>
```

### Force-Set Prestige Level
```
/prestige set <player> <level>
```
Use with caution — does not award retroactive bonuses.

---

## Crate Commands

### Give Crate Keys
```
prisoncratekey give <player> <canopic|tomb|pharaoh> <amount>
```
Run from console or via `/execute as console`:
```
/execute run prisoncratekey give PlayerName pharaoh 3
```

---

## Staff Tools

### Vanish
```
/vanish
/v
```
Permission: `prison.staff.helper`

### Spectate a Player
```
/spectate <player>
/spec <player>
```

### Teleport to Player
```
/tp <player>
/tphere <player>
```

### View Anticheat Flags
```
/anticheat review
/ac review
```
Permission: `prison.staff.mod`

### Freeze a Player (investigate)
```
/freeze <player>
```

### Mute/Unmute
```
/mute <player> <duration> [reason]
/unmute <player>
```

### Ban/Unban
```
/ban <player> [reason]
/unban <player>
```

### Kick
```
/kick <player> [reason]
```

---

## Leaderboard Commands

### Force Refresh Leaderboards
```
/leaderboard refresh
```

---

## Warp Management

```
/warp create <name>
/warp delete <name>
/warp setpermission <name> <node>
/warp list
/warp info <name>
```

---

## Gang / Dynasty Management

### View Dynasty Info
```
/gang info <dynasty-name>
```

### Disband a Dynasty (admin)
```
/gang disband <dynasty-name>
```

### Remove a Member
```
/gang kick <dynasty-name> <player>
```

---

## Event Management

### Start a Server Event
```
/event start <event-name>
/event stop <event-name>
/event list
```

---

## Server Utility

### Broadcast a Message
```
/broadcast <message>
```
Supports MiniMessage formatting.

### Force Save All Player Data
```
/prison save
```

---

## Permission Level Reference

| Permission | Who Should Have It | Access |
|---|---|---|
| `prison.admin.*` | Server owner only | Everything |
| `prison.staff.admin` | Senior admins | Most admin commands, no eco set |
| `prison.staff.mod` | Moderators | Anticheat review, mute, ban, freeze |
| `prison.staff.helper` | Helpers | Vanish, spectate, basic teleport |
| `prison.mine.z` | Regular players (max rank) | All mine access |
| `prison.donor.chosen` | Top donor | All donor perks |

---

*See `STAFF_OPERATIONS.md` for daily operational procedures.*
*See `PERMISSIONS_MATRIX.md` for the full permission node list.*

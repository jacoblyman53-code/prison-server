# PERMISSIONS MATRIX

> **Purpose:** Every permission node in the system organized by plugin.
> Shows who gets each permission by default.
> Use this when configuring a new staff role or donor rank.

---

## Legend
- **✓** = Has permission by default
- **A** = Admin/Owner only
- **D** = Donor rank specific
- **S** = Staff rank specific
- **–** = Not assigned by default

---

## Core Permissions

### `prison.admin.*`
**Description:** Wildcard — grants ALL admin permissions
**Default holders:** Owner
**Includes:** All nodes listed below prefixed with `prison.admin.`

### Mine Rank Permissions (auto-granted on rank-up)

| Permission | Description | Granted at |
|---|---|---|
| `prison.mine.a` | Access Pit of Souls + Tomb of Aten | Rank A (start) |
| `prison.mine.b` | Access Tomb of Aten | Rank B |
| `prison.mine.c` | Access Tomb of Thoth | Rank C |
| `prison.mine.d` | — | Rank D |
| `prison.mine.e` | Access Tomb of Sobek | Rank E |
| `prison.mine.f` | — | Rank F |
| `prison.mine.g` | Access Tomb of Hapi + Dynasty creation | Rank G |
| `prison.mine.h` | — | Rank H |
| `prison.mine.i` | Access Tomb of Sekhmet | Rank I |
| `prison.mine.j` | Access Tomb of Sekhmet (upper) | Rank J |
| `prison.mine.k` | Access Tomb of Bastet | Rank K |
| `prison.mine.l` | — | Rank L |
| `prison.mine.m` | Access Tomb of Ptah | Rank M |
| `prison.mine.n` | Access Tomb of Ptah + /prestige visible | Rank N |
| `prison.mine.o` | Access Tomb of Seth | Rank O |
| `prison.mine.p` | — | Rank P |
| `prison.mine.q` | Access Tomb of Isis | Rank Q |
| `prison.mine.r` | — | Rank R |
| `prison.mine.s` | Access Tomb of Horus | Rank S |
| `prison.mine.t` | — | Rank T |
| `prison.mine.u` | Access Tomb of Ra | Rank U |
| `prison.mine.v` | — | Rank V |
| `prison.mine.w` | Access Tomb of Osiris | Rank W |
| `prison.mine.x` | — | Rank X |
| `prison.mine.y` | Access Tomb of Anubis | Rank Y |
| `prison.mine.z` | + /prestige ascend active | Rank Z |

Mine permissions are **cumulative** — a player with `prison.mine.j` also has A through I.

---

## Donor Rank Permissions

| Permission | Rank | Mine Access |
|---|---|---|
| `prison.donor.devotee` | Devotee | Sanctum of Amun |
| `prison.donor.acolyte` | Acolyte | + Sanctum of Hathor |
| `prison.donor.highpriest` | High Priest | + Hall of the Pharaoh |
| `prison.donor.chosen` | Pharaoh's Chosen | + Chamber of the Gods |

Donor permissions are also cumulative — `highpriest` includes `acolyte` and `devotee`.

---

## Staff Rank Permissions

### `prison.staff.helper`
| Permission | Description |
|---|---|
| `prison.staff.helper` | Base helper permission |
| `prison.staff.vanish` | Toggle vanish |
| `prison.staff.spectate` | Spectate players |
| `prison.staff.kick` | Kick players |
| `prison.staff.fly` | Fly on server |
| `prison.staff.tp` | Teleport to/from players |

### `prison.staff.mod` (includes all helper perms)
| Permission | Description |
|---|---|
| `prison.staff.mod` | Base moderator permission |
| `prison.staff.mute` | Mute players |
| `prison.staff.ban` | Ban/unban players |
| `prison.staff.freeze` | Freeze players |
| `prison.staff.anticheat` | View and act on anticheat flags |
| `prison.staff.chatfilter` | Manage chat filter |

### `prison.staff.admin` (includes all mod perms)
| Permission | Description |
|---|---|
| `prison.staff.admin` | Base admin permission |
| `prison.admin.eco` | Economy give/take/set commands |
| `prison.admin.ranks` | Force-set player ranks |
| `prison.admin.kits` | Give kits bypassing requirements |
| `prison.admin.mines` | Mine management commands |
| `prison.admin.warps` | Warp management |
| `prison.admin.gangs` | Force gang/dynasty actions |
| `prison.admin.crates` | Give crate keys, place crates |
| `prison.admin.events` | Start/stop server events |
| `prison.admin.donor` | Grant/revoke donor ranks |
| `prison.admin.prestige` | Set prestige levels |
| `prison.admin.anticheat` | Clear anticheat flags |

### `prison.staff.owner` / `prison.admin.*`
All of the above plus:
| Permission | Description |
|---|---|
| `prison.admin.permissions` | Manage the permission system itself |
| `prison.admin.reload` | Reload plugin configurations |
| `prison.admin.debug` | Enable debug logging |

---

## Player Permissions (Default True — All Players)

These are granted to all players regardless of rank:

| Permission | Description |
|---|---|
| `prison.coinflip.use` | Use the coinflip system |
| `prison.sell.use` | Use /sell and /sellall |
| `prison.ranks.view` | View the ranks GUI |
| `prison.quests.view` | View quests GUI |
| `prison.ah.use` | Use the auction house |
| `prison.warps.use` | Use /warp to travel |
| `prison.chat.use` | Send chat messages |
| `prison.leaderboard.view` | View leaderboards |
| `prison.cosmetics.view` | Open cosmetics menu |
| `prison.gang.create` | Create a Dynasty (after Rank G) |
| `prison.gang.join` | Join a Dynasty |

---

## Prestige Permissions (Auto-granted on Ascension)

| Permission | Granted at |
|---|---|
| `prison.prestige.1` | Ascension 1 |
| `prison.prestige.2` | Ascension 2 |
| ... | ... |
| `prison.prestige.50` | Ascension 50 (max tracked) |

These can be used to gate content behind Ascension level.

---

## Recommended Role Setups

### New Server — Minimal Staff Team
```
Owner: prison.admin.*
Admin: prison.staff.admin
Mod: prison.staff.mod
Helper: prison.staff.helper
```

### Expanded Staff Team
```
Owner: prison.admin.*
Senior Admin: prison.staff.admin + prison.admin.eco
Junior Admin: prison.staff.admin (no eco)
Senior Mod: prison.staff.mod + prison.staff.anticheat
Mod: prison.staff.mod
Helper: prison.staff.helper
Builder: prison.staff.helper + prison.admin.mines
```

---

## Granting Permissions

### In-Game
```
/permissions grant <player> prison.staff.mod
/permissions grant <player> prison.admin.*
```

### For a Whole Group (if group system is extended)
Currently permissions are per-player. To set up a group system, extend `core-permissions`
with a group inheritance model — see `EXTENSION_GUIDE.md`.

---

*See `ADMIN_EDITING_GUIDE.md` for the commands that use these permissions.*
*See `STAFF_OPERATIONS.md` for how to assign permissions to new staff members.*

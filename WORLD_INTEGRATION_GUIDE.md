# WORLD INTEGRATION GUIDE

> **Purpose:** How to set up a playable world — from defining mine regions
> to setting spawn points and connecting all moving parts.
> This is required reading before opening your server to players.

---

## Overview

The Pharaoh's Prison code is designed to work with any world, but the world must:
1. Have defined mine regions (bounding box corners + spawn point per mine)
2. Have a spawn point set
3. Have warp points configured for each mine
4. Have the main menu compass item available to players

The product ships with placeholder coordinates (`[0,64,0]`) for all mines.
You must replace these with real coordinates from your world.

---

## Understanding Mine Regions

Each mine in `plugin-mines/config.yml` has:
- `corner1` and `corner2` — two opposite corners of the mine's bounding box (3D cuboid)
- `spawn-x/y/z/yaw/pitch` — where players teleport when the mine resets
- `enabled: true/false` — must be `true` for the mine to work

The mine system fills the bounding box with blocks according to `composition`.
The spawn point should be **outside** the bounding box, at the mine entrance.

---

## Mine Setup Procedure

For each mine, you need to:

### Method A: In-Game Commands (Recommended)

1. **Stand inside the mine region at one corner**
   ```
   /mine setcorner1 A
   ```

2. **Stand at the opposite corner**
   ```
   /mine setcorner2 A
   ```

3. **Stand at the mine entrance (outside the mining area)**
   ```
   /mine setspawn A
   ```

4. **Enable the mine**
   ```
   /mine enable A
   ```

5. **Fill the mine with its block composition**
   ```
   /mine fill A
   ```

6. **Verify** — `/mine tp A` should teleport you to the spawn point.
   The mine should be filled with the correct blocks.

Repeat for every mine (A through Z, plus donor mines and free mine).

### Method B: Manual Config Edit

Edit `plugin-mines/config.yml` directly:
```yaml
mines:
  A:
    enabled: true
    corner1: [100, 60, 200]
    corner2: [149, 79, 249]
    spawn-x: 125.5
    spawn-y: 82.0
    spawn-z: 224.5
    spawn-yaw: 180.0
    spawn-pitch: 0.0
```
Then run `/mine fill A` to fill with blocks.

---

## Minimum Viable World Setup

To have a playable server, you need at minimum:

### 1. A Spawn Point
```
/setworldspawn    (run while standing at spawn)
```
Players spawn here on first join. Should be a safe, open area.

### 2. A Pit of Souls (Free Starter Mine)
A simple stone pit, any size. Recommend 20×20×5 minimum.
Configure as mine `free` or `pit` in mines config.
```
/mine setcorner1 pit
/mine setcorner2 pit
/mine setspawn pit
/mine enable pit
/mine fill pit
```

### 3. At Least Mine A through E
Enough mines for a new player to progress through the first few ranks.
You can add the remaining mines later.

### 4. A Warp to Each Mine
```
/warp create pit     (standing at mine entrance)
/warp create a       (standing at mine A entrance)
/warp create b
... etc.
```

### 5. A Main Hub Area
Players should have somewhere to stand at spawn that isn't a mine.
Ideally: a central hub area with signs or NPCs pointing to the mines and `/menu`.

---

## Recommended Mine Sizes

| Mine Tier | Suggested Size | Notes |
|---|---|---|
| Pit of Souls | 20×20×5 | Stone only — doesn't need to be big |
| Tomb of Aten (A–B) | 25×25×8 | |
| Tomb of Thoth (C–D) | 30×30×10 | |
| Tomb of Sobek (E–F) | 35×35×12 | |
| Tomb of Hapi (G–H) | 40×40×12 | |
| Tomb of Sekhmet (I–J) | 45×45×15 | |
| Tomb of Bastet (K–L) | 50×50×15 | Diamond appears |
| Tomb of Ptah (M–N) | 50×50×15 | |
| Tomb of Seth (O–P) | 55×55×18 | |
| Tomb of Isis (Q–R) | 55×55×18 | |
| Tomb of Horus (S–T) | 60×60×20 | Netherite appears |
| Tomb of Ra (U–V) | 60×60×20 | |
| Tomb of Osiris (W–X) | 65×65×20 | Ancient Debris |
| Tomb of Anubis (Y–Z) | 70×70×20 | Largest mine — max yield |
| Donor mines | 40×40×15 each | Separate area from standard mines |

**Rule of thumb:** Larger mines = less frequent resets = more simultaneous players.
Configure `reset-timer-mins` and `reset-threshold` to match your expected player counts.

---

## World Architecture Suggestions

### Layout Option A: Hub-and-Spoke (Simple)
- Central hub at spawn
- Mines radiate outward from hub
- Short `/warp` teleports to each mine
- Good for most setups

### Layout Option B: Egyptian City (Premium)
- Grand pyramid spawn structure
- Mines accessible through tomb entrances in the city
- Underground mine shafts with atmospheric lighting
- Dynasty mine in a separate "Valley of the Kings" area
- Donor mines in exclusive "Inner Temple" area

### Layout Option C: Underground (Thematic)
- Players spawn on the surface near a pyramid entrance
- Walk down into the catacombs
- Each mine is a chamber deeper in the catacombs
- Higher-tier mines are literally deeper underground

Option B and C create a significantly more immersive experience and justify premium Tebex prices.
Option A is sufficient for a functional v1 that can be upgraded later.

---

## Warp Setup for All Mines

After mines are configured, create warps:
```bash
# At the Pit of Souls entrance
/warp create pit

# At each standard mine entrance
/warp create aten
/warp create thoth
/warp create sobek
/warp create hapi
/warp create sekhmet
/warp create bastet
/warp create ptah
/warp create seth
/warp create isis
/warp create horus
/warp create ra
/warp create osiris
/warp create anubis

# At donor mine entrances (gate behind permission)
/warp create sanctum_amun
/warp setpermission sanctum_amun prison.donor.devotee
/warp create sanctum_hathor
/warp setpermission sanctum_hathor prison.donor.acolyte
# ... etc.
```

---

## Testing the World Setup

Once mines are configured:

1. `/mine list` — verify all mines show `enabled: true`
2. `/mine tp A` → `/mine tp Z` — verify each teleport works
3. Mine a few blocks in Mine A — verify blocks appear in inventory
4. `/sell all` — verify Coins received
5. Test mine reset: `/mine reset A` — verify blocks refill
6. Test as a Rank A player (no extra permissions) — verify they can only access Mine A

---

*See `MINIMUM_VIABLE_WORLD.md` for the bare minimum setup to be playable.*
*See `PREMIUM_WORLD_PLAN.md` for how to build a world that justifies premium pricing.*

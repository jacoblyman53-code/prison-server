# MINIMUM VIABLE WORLD

> The absolute minimum world setup to have a playable server.
> Everything here is achievable in 2–4 hours with WorldEdit.
> This gets you from code to playable — polish can come later.

---

## Goal

A server where a player can:
1. Join and spawn in a safe area
2. Get a starter kit
3. Find the first mine
4. Mine, sell, and rank up
5. Progress through at least the first 5–10 ranks

Nothing fancy. Functional. Can be replaced with a proper world later.

---

## What You Actually Need

| Item | Required? | Notes |
|---|---|---|
| Spawn point | ✓ YES | `/setworldspawn` at a flat area |
| Mine A (Tomb of Aten) | ✓ YES | First real mine after Rank B |
| Pit of Souls | ✓ YES | Starter mine for Rank A |
| Mines B through E | ✓ YES | Cover first ~5 ranks |
| Mines F through Z | Later | Can add progressively |
| Donor mines | Later | Not needed for launch testing |
| Dynasty mine | Later | Not needed for basic launch |
| Hub build | NO | Flat platform works for MVP |
| Pyramid spawn | NO | Nice to have, not required |

---

## Step-by-Step MVW Build (2–4 hours)

### 1. Create a Flat World
In server.properties:
```properties
level-type=flat
generate-structures=false
```
Or use a pre-existing world. A superflat world is fastest for testing.

### 2. Set the Spawn Point
Find a flat area. Stand on it. Run:
```
/setworldspawn
```

Place a sign: "Welcome to The Pharaoh's Prison — /menu to get started"

### 3. Create the Pit of Souls
Dig or WorldEdit a 20×20×5 hole:
```
//pos1          (stand at one corner)
//pos2          (stand at opposite corner)
//set stone     (fill with stone)
```

Set it up:
```
/mine setcorner1 pit
/mine setcorner2 pit
/mine setspawn pit
/mine enable pit
/mine fill pit
/warp create pit
```

### 4. Create Mine A (Tomb of Aten)
Dig a 25×25×8 hole in a different location.
Set it up:
```
/mine setcorner1 A
/mine setcorner2 A
/mine setspawn A
/mine enable A
/mine fill A
/warp create aten
```
Add a sign at the entrance: "Tomb of Aten — Rank B required"

### 5. Repeat for Mines B through E
Each mine: dig → setcorner1/2 → setspawn → enable → fill → create warp

Mines C and D can share the same physical mine or be separate.

### 6. Add Signs or NPCs for Navigation
At spawn, add signs:
```
[Sign 1] Pit of Souls: /warp pit
[Sign 2] Main Menu: /menu
[Sign 3] View Ranks: /ranks
```

### 7. Test It
```
1. Log in as a new player
2. Verify you spawn at the spawn point
3. Verify starter kit is auto-delivered
4. /warp pit → mine → /sell all → get Coins
5. /ranks → rank up to Serf
6. /warp aten → mine B mine
7. Continue ranking up
```

If all that works: you have a minimum viable world.

---

## Time Estimate

| Task | Time |
|---|---|
| Set up spawn | 15 min |
| Dig and configure Pit of Souls | 20 min |
| Dig and configure Mines A–E | 60–90 min (15–18 min each) |
| Signs and navigation | 20 min |
| Testing | 30 min |
| **Total** | **~2.5–3 hours** |

---

## What This Is Missing

This minimum setup is enough to test the server but will feel empty to real players.
Before launch, you should add:
- An atmospheric spawn hub
- Atmospheric mine entrances (not just holes in the ground)
- A /shop area
- An /ah area (Auction House)
- A Dynasty mine location
- Proper lighting and decoration

See `PREMIUM_WORLD_PLAN.md` for what a polished world looks like.

---

*See `WORLD_INTEGRATION_GUIDE.md` for full mine configuration reference.*
*See `PREMIUM_WORLD_PLAN.md` for a world design that justifies premium pricing.*

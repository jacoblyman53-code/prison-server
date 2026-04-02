# NEW PLAYER FLOW

> **Purpose:** Document the exact first-join sequence — every message, title, sound,
> and action that fires from first join to first rank-up.
> This is the server's first impression. It must be perfect.

---

## Current State vs. Target State

### Current State (as of audit)
- No welcome message beyond Paper defaults
- No starter kit auto-delivery
- No tutorial NPC or signs
- Player spawns and must self-discover the `/mine` and `/sell` commands
- No onboarding is the #1 gap for new player retention

### Target State (this document)
- Warm, thematic welcome sequence
- Auto-delivered starter kit
- Clear first objective
- First rank-up within 3 minutes for any player who follows the flow

**Note:** The onboarding code changes are tracked in Phase 8 (Content Completeness).
This document specifies the design so Phase 8 has a clear target.

---

## First-Join Sequence

### Step 1: Initial Spawn (0 seconds)
Player joins for the first time.

**Location:** Spawn hub — Egyptian pyramid, hieroglyph walls, torches.
**Spawn point:** Bottom of pyramid steps, facing the grand entrance.

**Title sequence:**
```
Title:    <gold>THE PHARAOH'S PRISON
Subtitle: <gray>Your sentence has begun. Prove your worth.
Fade-in:  20 ticks | Stay: 60 ticks | Fade-out: 20 ticks
```

**Sound:** `minecraft:block.bell.use` at pitch 0.8 — grand, resonant.

**Chat messages (staggered, 1 second apart):**
```
[1s] <color:#E8C87A>⚱ Welcome to The Pharaoh's Prison, <player>.
[2s] <gray>You are a Slave — the lowest of the condemned.
[3s] <gray>Mine the tombs. Sell your haul. Earn the Pharaoh's favor.
[4s] <color:#E8C87A>⚱ Type <gold>/guide</gold> at any time for help.
```

---

### Step 2: Starter Kit (3 seconds after join)
Auto-delivered via the kit system. No command required.

**Kit name:** `starter` (internal), displayed as "Condemned Soul's Rations"

**Contents:**
| Item | Amount | Notes |
|---|---|---|
| Stone Pickaxe | 1 | Named: `<gray><!italic>Worn Chisel` — thematic, but functional |
| Bread | 16 | Basic food |
| Torch | 8 | For dark mines |
| Paper "Scroll" | 1 | Named: `<gold><!italic>Scroll of the Condemned` — click to open guide (or just lore) |

**Scroll lore:**
```
<color:#E8C87A>● Mine blocks in a Sacred Tomb
<color:#E8C87A>● Use /sell to sell your haul
<color:#E8C87A>● Use /ranks to see your path
<color:#E8C87A>● Earn Coins → Rank Up → Better Tombs
<gray>───────────────────
<gray>The gods are watching.
```

**Chat message:**
```
<gold>⚱ The Pharaoh grants you a Condemned Soul's Kit.
<gray>Check your inventory.
```

**Sound:** `minecraft:entity.experience_orb.pickup` — reward sound.

---

### Step 3: Objective Pointer (8 seconds after join)
After the welcome settles, give the player their first objective.

**Action bar message** (persists for 30 seconds):
```
<gold>▸ Find the Pit of Souls and begin mining   <gray>[/warp pit]
```

**Compass in action bar pulses** (if action bar is cycled): alternate between
the objective and the player's current Coins balance.

**Chat message:**
```
<gray>Your first tomb awaits: <gold>/warp pit
<gray>Mine blocks, then <gold>/sell all</gold> to earn Coins.
```

---

### Step 4: First Mine (player warps to Pit of Souls)
Player arrives at the Pit of Souls — a basic stone mine shaped like an Egyptian burial pit.

**On warp arrival, action bar:**
```
<gray>Pit of Souls  <gold>|  <gray>Mine here to begin your sentence
```

No message spam. Let the player just mine.

**First block break sound:** Normal. No special effect (don't over-engineer).

---

### Step 5: First Sell
Player types `/sell all` or `/sell`.

**If this is their first sell ever:**

Chat output:
```
<gold>✦ You sold your haul to the Merchant.
<gold>✦ Earned: <white>124 Coins    <gray>Sell streak: <gold>1×
<gray>Keep selling to grow your streak multiplier!
```

**If player's balance crosses 250 Coins (halfway to first rank):**

Action bar:
```
<gold>▸ Almost enough to rank up! Check <gold>/ranks
```

---

### Step 6: First Rank-Up (Slave → Serf)
Player opens `/ranks` or the main menu Ranks section and clicks rank up.

**Rank-up confirmation screen fires.**

On confirm:

**Title:**
```
Title:    <gold>⚡ RANK UP
Subtitle: <gray>You have risen to <gold>Serf
Fade-in:  10 | Stay: 50 | Fade-out: 15
```

**Sound:** `minecraft:ui.toast.challenge_complete` — distinct and rewarding.

**Chat:**
```
<gold>⚡ <player> has risen to <gold>[Serf]<gold>!
<gray>The Tomb of Aten is now accessible. <gold>/warp aten
```

**Broadcast to server** (configurable on/off):
```
<gold>⚡ <player> ranked up to <gold>[Serf]<gold>!
```

---

### Step 7: Tomb of Aten Unlock
The Tomb of Aten is the first real mine (better than the Pit of Souls).

**Action bar (after rank up, 5 seconds):**
```
<color:#E8C87A>▸ New tomb unlocked: <gold>Tomb of Aten  <gray>[/warp aten]
```

The player now has a reason to go somewhere new. The loop continues.

---

## Return Visit Flow (Second Session)

When a player who has played before rejoins:

**No welcome title** (too intrusive for returning players).

**Chat message:**
```
<color:#E8C87A>⚱ Welcome back, <player>. <gray>Rank: <gold>[Serf]  <gray>Coins: <gold>✦ 847
```

If they have completed daily quests since last session was reset:
```
<gold>✦ New sacred quests are available. <gray>/quests
```

If they have pending auction house items:
```
<color:#E8C87A>⚱ You have items waiting at the Grand Bazaar. <gray>/ah
```

---

## Milestone Messages

These fire automatically when a player reaches specific thresholds.

| Trigger | Message |
|---|---|
| Rank E (Merchant) | `<gold>✦ You are now a Merchant. The Pharaoh's bazaars are open to you.` |
| Rank J (Architect) | `<gold>✦ You are now an Architect. The Khopesh's higher secrets are within reach.` |
| Rank N (High Priest) | `<gold>✦ You are now a High Priest. You may speak with the gods... for a price.` |
| Rank Z (Pharaoh) | `<gold>⚡ YOU HAVE REACHED PHARAOH RANK. The highest mortal honor.` + title |
| Rank Z | Broadcast: `<gold>⚡ <player> has achieved [Pharaoh] rank — the mightiest mortal soul!` |
| First Ascension | `<light_purple>✦ Your soul has been judged and found worthy. You are Reborn.` |
| Sell streak 10x | `<gold>✦ Sell streak: 10×! The gods multiply your earnings.` |
| Sell streak 100x | `<gold>⚡ SELL STREAK: 100×! You have the Pharaoh's full favor.` |
| First crate opened | `<color:#E8C87A>⚱ Your offering to the gods has been accepted...` then reveal |
| Dynasty created | `<gold>✦ Dynasty <name> has been founded. May your bloodline be eternal.` |

---

## Missing Code (Phase 8 Build List)

The following onboarding elements do not exist yet and must be built:

| Feature | Where | Complexity |
|---|---|---|
| Auto-deliver starter kit on first join | `plugin-kits` or `core-database` join event | Low |
| First-join title + staggered chat messages | New listener in `plugin-menu` or standalone | Low |
| Return visit greeting with balance display | Player join listener | Low |
| Action bar objective pointer after join | Join listener with scheduler | Low |
| Milestone messages (rank thresholds) | `plugin-ranks` rank-up handler | Medium |
| Pending AH notification on join | `plugin-auctionhouse` join listener | Low |
| `/guide` command | New command in `plugin-menu` | Medium |

All of these are low-to-medium complexity. They have a disproportionate impact
on new player retention and should be prioritized in Phase 8.

---

*See `GAMEPLAY_LOOP.md` for the broader progression experience.*
*See `PROGRESSION_MAP.md` for the complete rank unlock chart.*

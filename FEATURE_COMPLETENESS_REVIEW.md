# FEATURE COMPLETENESS REVIEW

> **Purpose:** Every feature in the codebase rated by completion status.
> Drives decisions on what to build, stub cleanly, or cut before v1 launch.

---

## System Status Table

| System | Plugin | Status | Blocking Launch? |
|---|---|---|---|
| Database / connection pool | core-database | ✓ COMPLETE | — |
| Permission engine | core-permissions | ✓ COMPLETE | — |
| Region management | core-regions | ✓ COMPLETE | — |
| Economy (Coins + Relics) | plugin-economy | ✓ COMPLETE | — |
| Rank-up system | plugin-ranks | ✓ COMPLETE | — |
| Mine system + resets | plugin-mines | ✓ COMPLETE | — |
| Custom Khopesh enchants | plugin-pickaxe | ✓ COMPLETE | — |
| Prestige/Ascension | plugin-prestige | ✓ COMPLETE (costs missing) | Partial |
| Crates system | plugin-crates | ✓ COMPLETE | — |
| Kits system | plugin-kits | ✓ COMPLETE | — |
| Quests (daily + weekly) | plugin-quests | ✓ COMPLETE | — |
| Auction house | plugin-auctionhouse | ✓ COMPLETE | — |
| Coinflip | plugin-coinflip | ✓ COMPLETE | — |
| Chat formatting | plugin-chat | ✓ COMPLETE | — |
| Cosmetics / tags | plugin-cosmetics | ✓ COMPLETE | — |
| Gangs / Dynasties | plugin-gangs | ✓ COMPLETE | — |
| Leaderboards | plugin-leaderboards | ✓ COMPLETE | — |
| Warps | plugin-warps | ✓ COMPLETE | — |
| Anti-cheat | plugin-anticheat | ✓ COMPLETE | — |
| Donor rank management | plugin-donor | ✓ COMPLETE | — |
| Events (RA's Blessing etc.) | plugin-events | ✓ COMPLETE (partial) | Partial |
| Staff tools | plugin-staff | ✓ COMPLETE | — |
| Admin toolkit | plugin-admintoolkit | ✓ COMPLETE | — |
| Menu / GUI system | plugin-menu | ✓ COMPLETE | — |
| **Shop system** | plugin-shop | ⚠️ BETA | YES |
| **Tebex integration** | plugin-tebex | ⚠️ BETA | YES |
| **Black Market** | plugin-menu | ✗ STUB | NO (cut for v1) |
| **Booster system** | plugin-economy | ✗ MISSING | NO (nice to have) |
| **New player onboarding** | (none) | ✗ MISSING | YES |
| **Web dashboard** | web-dashboard | ✗ EMPTY | NO |

---

## Decision Table: Build, Stub, or Cut

### Build Before Launch (Blocking)

#### 1. Plugin-Shop (BETA → PRODUCTION)
**Why blocking:** A prison server without a functioning shop is incomplete.
Players need a way to buy items with their earned Coins.
**What's needed:**
- Admin price editing (can set prices without recompiling)
- At least 5–10 shop categories with items
- Clear buy/sell interface
- Integration with economy plugin

**Minimum viable shop:** One category per resource type (Mining Supplies, Food,
Potions, Blocks, Misc). 10–20 items each. Prices in config.

#### 2. Tebex Integration (BETA → PRODUCTION)
**Why blocking:** Donor rank purchases cannot be delivered without it.
**What's needed:**
- Webhook handler (POST endpoint — requires a port or reverse proxy on SparkHost)
- Command execution queue (DB-backed, survives restarts)
- Offline player delivery (execute commands on next login)
- Webhook secret validation

#### 3. New Player Onboarding (MISSING → COMPLETE)
**Why blocking:** Without onboarding, new players are lost. This is the biggest
retention gap identified in the audit.
**What's needed:**
- First-join detection (already handled by Paper)
- Welcome title sequence
- Starter kit auto-delivery on first join (not manual `/kit starter`)
- Chat messages with first objectives
- Action bar pointer to first mine

---

### Stub Cleanly (Not Blocking but Must Not Confuse Players)

#### Black Market — DECISION: STUB CLEANLY FOR V1, PLAN V2

The Black Market (`BlackMarketMenuGUI.java`) currently opens but has no content.
**For v1:** Display a "Coming Soon" GUI with Egyptian mystery flavor:
```
<gold><!italic>The Shadow Bazaar stirs...
<gray>────────────────────────
<color:#E8C87A>● Ancient merchants whisper of wares beyond imagining
<gray>This market opens in a future age.
<gray>────────────────────────
<gray>Check back in a future update.
```
Remove the warp to the Black Market if it's a dead end. Only accessible via main menu.

#### Web Dashboard — DECISION: CUT ENTIRELY FROM V1, ADVERTISE AS V2

The web-dashboard directory is empty. Building a web admin panel is substantial work.
**For v1:** Remove all references to a web dashboard. Add to FAQ: "A web admin panel
is planned for v2." Buyers will accept this — it's not expected on a first purchase.

---

### Nice to Have (Build If Time Permits)

#### Booster System
Documented in `BOOSTER_SYSTEM.md`. Not blocking, adds monetization depth.
**Priority:** Medium. Build after shop and Tebex are complete.

#### Dynasty War Automation
Dynasty Wars are referenced but may not be fully automated.
**Priority:** Medium. Manual admin-triggered events work for v1.

#### Trial of Horus PvP Arena
Referenced in events design. Not currently implemented.
**Priority:** Low. Can be "Coming Soon" for v1 without hurting launch.

#### `/guide` Command
New player tutorial command. Very low effort, high impact.
**Priority:** HIGH. Should be part of the onboarding work.

---

## Missing Features That Must Be Noted in Documentation

These are gaps that buyers need to know about before purchase:

1. **World files not included.** The product is code + config. Buyers must build
   or purchase a world. Document clearly in `INSTALLATION_GUIDE.md` and `FAQ.md`.

2. **Mine coordinates are not set.** All mines in config have `corner1: [0,64,0]`
   placeholder coordinates. Buyers must run `/mine setcorner1` etc. for each mine.
   Document in `WORLD_INTEGRATION_GUIDE.md`.

3. **Shop prices may need tuning.** The shop is BETA — prices may not be balanced
   against the mine income rates. Buyers should test before opening.

4. **Tebex webhook requires a port.** SparkHost and similar hosts may restrict
   inbound HTTP connections. Buyers need guidance on setting up the webhook endpoint.

---

*See `NICE_TO_HAVE_FEATURES.md` for the v2 feature backlog.*
*See `CUT_FEATURES.md` for features explicitly not in v1.*

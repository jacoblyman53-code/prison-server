# CUT FEATURES — V1

> Features explicitly excluded from the v1 product release.
> Documented here so buyers know these are intentional decisions, not bugs or oversights.
> Each item includes why it was cut and how to re-enable or add it.

---

## Black Market (Shadow Bazaar) — Full Implementation

**Cut from:** Full dynamic marketplace with rotating secret stock
**What ships instead:** A "Coming Soon" stub GUI with Egyptian flavor text
**Why cut:** Full Black Market requires a rotating item pool, pricing engine, stock
tracking, and significant balancing. Risk of releasing unbalanced content.
**Status in v1:** GUI exists but shows "coming soon" — no dead ends for players.
**How to enable for v2:**
- Design the item pool (5–8 items, rotated hourly by a scheduler)
- Implement `BlackMarketManager` class with `scheduledRefresh()`
- Store rotation state in DB (`black_market_stock` table)
- Wire into `BlackMarketMenuGUI.java`

---

## Web Admin Dashboard

**Cut from:** v1 entirely
**Why cut:** Building a web panel (React/Vue + REST API) is 2–3 weeks of work.
It would delay launch significantly and is not expected by buyers at this price point.
**Status in v1:** `web-dashboard/` directory exists but is empty.
**Advertise as:** "Web admin panel — planned for v2"
**How to enable:**
- The `core-database` module already has a `DatabaseManager` with full query access
- A Spring Boot or Micronaut REST API backend can connect to the same MySQL database
- The web frontend can then call the REST API

---

## Dynasty Wars (Automated)

**Cut from:** Fully automated weekly event with scoring
**What ships instead:** Dynasty competition exists but may require admin to trigger events manually
**Why cut:** Automating the Dynasty War requires careful scheduling, score tallying,
and reward distribution. Risk of edge cases (dynasty disbands mid-war, etc.)
**Status in v1:** Manual admin can trigger `plugin-events` events; Dynasty rankings exist.
**How to enable:**
- Extend `plugin-events` with a `DynastyWarEvent` type
- Add a scheduled `CronJob` (weekly Friday 8pm) that starts the war
- Score based on Dynasty mine contribution, missions completed
- Auto-award top Dynasty at war end via command execution

---

## Trial of Horus (PvP Arena)

**Cut from:** Automated weekly PvP event
**Why cut:** Requires a physical PvP arena build (world work) + PvP enable/disable logic
in specific regions. World work is out of scope for the code product.
**Status in v1:** Not implemented. Can be added by buyer once world is built.
**How to enable:**
- Build an Egyptian arena in the world
- Define a region with PvP enabled
- Create a `TrialOfHorusEvent` in `plugin-events`
- Award winner with Relics via console command

---

## Vote Rewards

**Cut from:** Automated vote-to-reward system
**Why cut:** Requires NuVotifier (external plugin) dependency and vote listener setup.
Adding third-party plugin dependencies increases setup complexity for buyers.
**Status in v1:** Not implemented. The Canopic Chest crate type exists and can be used as a vote reward.
**How to enable:**
- Install NuVotifier or VotifierPlus on the server
- Add a vote listener that calls `prisoncratekey give {player} canopic 1` on vote
- Add vote sites to `DEPLOYMENT_GUIDE.md`

---

## Daily Login Rewards

**Cut from:** Streak-based daily login reward system
**Why cut:** Not implemented yet. Would require a new manager tracking last-login dates.
**Status in v1:** Not implemented.
**How to enable:**
- Add `last_login` column to player records in DB
- On join, check if 24h has passed; grant daily reward kit
- Kit contents scale with consecutive login streak

---

## Booster System (Full)

**Cut from:** Full booster system with personal + server boosters
**What ships instead:** Sell streak multiplier (built) + donor sell bonuses (built)
**Why cut:** The booster system requires additional DB table, Tebex integration,
scoreboard display, and careful stacking logic. High complexity for v1.
**Status in v1:** Design documented in `BOOSTER_SYSTEM.md`. Not coded.
**How to enable:**
- Follow `BOOSTER_SYSTEM.md` implementation plan
- Add `boosters` table to `DatabaseManager`
- Build `BoosterManager` in `plugin-economy`

---

## Player-Owned Shops

**Cut from:** v1 and v2 consideration
**Why cut:** Requires significant economic design work to avoid undermining the
Merchant's Bazaar (auction house). High complexity, unclear player demand.
**Status:** Not started, no architecture.

---

## Mine Speedrun Leaderboard

**Cut from:** v1
**Why cut:** Nice to have but not core to the product. Adds complexity to mine reset tracking.
**How to enable:**
- Track blocks broken per reset cycle per player (in-memory, reset on mine reset)
- After reset, persist top 3 to a `mine_speedrun_records` table
- Display in mine GUI or as a separate leaderboard

---

## Notes for Buyers

All cut features are documented here because:
1. You might find references to them in code comments or configs — they are intentional stubs
2. If you want to add them, the architecture is designed to accommodate them
3. The `plugin-events` system is designed to be extended with new event types
4. The `core-database` makes adding new tables straightforward via `CREATE TABLE IF NOT EXISTS`

This product is designed to be extended. Everything here is a future opportunity, not a failure.

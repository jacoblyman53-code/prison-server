# RELEASE CHECKLIST

> Run through this checklist before publishing the product for sale.
> Every item must be checked or consciously deferred with a reason.

---

## Code Quality Checks

- [ ] `./gradlew shadowJar` completes with `BUILD SUCCESSFUL` (zero warnings)
- [ ] All 26 JARs are present in their respective `build/libs/` directories
- [ ] Server starts cleanly: all 26 plugins load without errors in console
- [ ] No `[ERROR]` lines in startup console log
- [ ] Database tables are created on first run (verify with `SHOW TABLES;`)
- [ ] No hardcoded developer credentials in any config or source file

---

## Core Gameplay Tests

Run these as a fresh player (no admin permissions):

- [ ] Join server → starter kit auto-delivered
- [ ] Welcome title fires on first join
- [ ] `/warp pit` works → Pit of Souls is accessible
- [ ] Mining stone in Pit of Souls adds to inventory
- [ ] `/sell all` → Coins received and displayed in chat
- [ ] Sell streak counter increments
- [ ] `/ranks` opens the rank GUI
- [ ] Rank A → B rank-up works (costs 5,000 Coins)
- [ ] After rank-up: Tomb of Aten is accessible
- [ ] Mine A blocks fill with correct composition
- [ ] Mine reset fires after threshold (mine 80%+ of blocks)
- [ ] `/menu` opens the main menu GUI
- [ ] All main menu buttons open their respective GUIs
- [ ] `/quests` shows daily quests
- [ ] `/ah` opens auction house
- [ ] Khopesh enchant GUI opens with available enchants
- [ ] Prestige system requires Rank Z (cannot prestige at Rank A)
- [ ] Coinflip creates and resolves correctly
- [ ] Dynasty can be created after Rank G

---

## Economy Checks

- [ ] Sell prices match `plugin-economy/config.yml` (test with 1 stone → 3 Coins)
- [ ] Rank costs match `plugin-ranks/config.yml` (Rank B = 5,000)
- [ ] Sell streak multiplier applies correctly at 5× (1.05× multiplier on next sell)
- [ ] Token (Relic) earn works from quests
- [ ] Khopesh enchant purchase deducts Relics correctly
- [ ] Economy balance TODO items reviewed: see `ECONOMY_BALANCE_TODO.md`
- [ ] Ancient Debris and Netherite Scrap prices reviewed (TD-008)
- [ ] Prestige cost configured (TD-001) — **BLOCKING if not done**

---

## Permission Checks

- [ ] New player has mine rank `prison.mine.a` after join
- [ ] Rank-up grants the correct mine permission node
- [ ] `/permissions grant player prison.admin.*` works
- [ ] Admin can access all admin commands
- [ ] Regular player cannot use admin commands
- [ ] Donor rank permissions are enforced on donor mines

---

## GUI Checks

- [ ] All 28 GUIs open without errors
- [ ] Slot 0 is always "back" or "close" on every GUI
- [ ] No GUI has filler items (glass panes filling empty slots)
- [ ] Every interactive item has a lore with a CTA
- [ ] Confirmation dialog appears for prestige/ascension
- [ ] Mine browser shows mine names and requirements correctly

---

## Staff Tools Checks

- [ ] `/eco give PlayerName igc 1000` works
- [ ] Economy operation logged to `staff_actions` table
- [ ] `/vanish` works for staff
- [ ] `/anticheat review` opens the flag review GUI
- [ ] `/mine fill A` refills Mine A correctly as admin

---

## Documentation Checks

- [ ] `README.md` is accurate and up to date
- [ ] `INSTALLATION_GUIDE.md` steps work on a fresh server
- [ ] `CONFIG_GUIDE.md` all keys match actual config files
- [ ] `FEATURES.md` accurately lists what's implemented
- [ ] `FAQ.md` answers are accurate for the current state
- [ ] `PRODUCT_AUDIT.md` scores reflect current state
- [ ] `TECHNICAL_DEBT_LOG.md` has no BLOCKING items remaining (or they're documented as known)

---

## Configuration Checks

- [ ] All rank `display:` values are Egyptian names (not "Rank A", "Rank B", etc.)
- [ ] `max-rank-message` in ranks config references Ascension correctly
- [ ] Mine `display:` values are Egyptian tomb names
- [ ] `rankup-broadcast` is enabled for key ranks (Z at minimum)
- [ ] Default `prestige-cost` is set to a reasonable value

---

## World Checks

- [ ] Spawn point is set (`/setworldspawn` done)
- [ ] All configured mines have non-placeholder coordinates
- [ ] All configured mines are `enabled: true`
- [ ] All mine warps exist (`/warp A`, `/warp pit`, etc.)
- [ ] Mine A through Z can all be reached via warp
- [ ] Players cannot access mines above their rank (permission check)
- [ ] Donor mines are inaccessible to non-donors

---

## Git / Repository Checks

- [ ] All source code is committed
- [ ] All documentation files are committed
- [ ] No sensitive files committed (no real MySQL passwords, no personal API keys)
- [ ] `.gitignore` covers `build/`, `*.class`, `local.properties`
- [ ] Repository is set to private (or ready to be made private before sale)
- [ ] Latest commit builds cleanly from a fresh clone

---

## Pre-Sale Checklist

- [ ] `README.md` has correct GitHub repository URL
- [ ] Screenshots or demo video ready (highly recommended for sales page)
- [ ] Price set and justified against `WHY_THIS_PRODUCT.md`
- [ ] Sales page on BuiltByBit, MC-Market, or Tebex marketplace
- [ ] Support channel established (Discord, GitHub Issues, or email)
- [ ] License terms documented

---

## Known Issues at Launch (Acceptable)

These items are known and documented but do not block launch:

| Item | Workaround / Notes |
|---|---|
| No auto-onboarding | Players receive starter kit manually with `/kit starter` |
| Shop is BETA | Functional but limited. Document in FAQ. |
| Tebex webhook needs completion | Donor ranks can be granted manually via `/donor grant` |
| Black Market is stub | Shows "Coming Soon" GUI — not a dead end |
| Web dashboard empty | Removed from all user-facing docs |
| World not included | Documented clearly in FAQ and README |

---

*If all checked items pass and all BLOCKING issues are resolved: product is release-ready.*
*Archive this checklist with the release date and version tag for future reference.*

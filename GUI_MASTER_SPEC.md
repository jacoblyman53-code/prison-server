# GUI MASTER SPEC — Egyptian Theme

> **Purpose:** Apply the Egyptian mythology theme from `PRODUCT_IDENTITY.md` to every GUI
> in the server. This is the implementation target for Phase 6 code changes.
>
> **Authority:** This document extends `GUI_DESIGN_SPEC.md`. The layout rules, navigation
> conventions, and lore structure in `GUI_DESIGN_SPEC.md` remain in force.
> This document adds the Egyptian naming, colors, and voice on top.
>
> **Scope:** 21 GUIs in `plugin-menu` plus auction house, coinflip, pickaxe, and other
> plugin-specific GUIs. Total: ~28 GUIs requiring a theme pass.

---

## MiniMessage Color Tokens (Egyptian Palette)

Use these exact tags throughout all GUI strings. Do not use legacy `§` codes.

```
<gold>         → Primary gold accent       #FFAA00
<color:#E8C87A> → Sand/parchment           #E8C87A
<gray>         → Body text / secondary     #AAAAAA
<white>        → Headers / item names      #FFFFFF
<red>          → Danger / negative         #FF5555
<green>        → Success / positive        #55FF55
<light_purple> → Ascension / divine        #FF55FF
<blue>         → Info / highlight          #5555FF
<!italic>      → ALWAYS on item names      (required by Paper GUI standard)
```

Standard item name prefix: `<!italic>` — removes default italic from GUI items.

---

## Title Convention

From `GUI_DESIGN_SPEC.md`: titles are ALL CAPS, no leading/trailing whitespace.

| GUI File | Old Title | New Egyptian Title |
|---|---|---|
| `MainMenuGUI.java` | `Main Menu` | `PHARAOH'S PRISON` |
| `RankProgressionGUI.java` | `RANKS` or similar | `DIVINE RANKS` |
| `MineBrowserGUI.java` | `MINES` | `SACRED TOMBS` |
| `PrestigeShopMenuGUI.java` | `PRESTIGE SHOP` | `ASCENSION SHOP` |
| `PrestigeConfirmGUI.java` | `PRESTIGE` | `ASCEND?` |
| `ShopCategoryPickerGUI.java` | `SHOP` | `MERCHANT'S BAZAAR` |
| `ShopCategoryPageGUI.java` | `SHOP — [category]` | `BAZAAR — [category]` |
| `ShopQuantityGUI.java` | `BUY` | `PURCHASE` |
| `CratesHubGUI.java` | `CRATES` | `PHARAOH'S RELICS` |
| `GangHomeGUI.java` | `GANG` | `DYNASTY` |
| `KitsMenuGUI.java` | `KITS` | `DIVINE KITS` |
| `QuestsMenuGUI.java` | `QUESTS` | `SACRED QUESTS` |
| `PickaxeHomeGUI.java` | `PICKAXE` | `KHOPESH` |
| `PickaxeEnchantsGUI.java` | `ENCHANTMENTS` | `DIVINE ENCHANTMENTS` |
| `SellCenterGUI.java` | `SELL` | `MERCHANT SELL` |
| `LeaderboardSelectorGUI.java` | `LEADERBOARDS` | `HALL OF LEGENDS` |
| `CosmeticsMenuGUI.java` | `COSMETICS` | `DIVINE ADORNMENTS` |
| `BoostsDetailGUI.java` | `BOOSTS` | `DIVINE BLESSINGS` |
| `WarpsMenuGUI.java` | `WARPS` | `SACRED WARPS` |
| `SettingsGUI.java` | `SETTINGS` | `SETTINGS` (keep neutral) |
| `BlackMarketMenuGUI.java` | `BLACK MARKET` | `SHADOW BAZAAR` |
| Auction House | `AUCTION HOUSE` | `GRAND BAZAAR` |
| Coinflip | `COINFLIP` | `SPHINX'S GAMBLE` |
| Donor Perks | `DONOR PERKS` | `DIVINE BLESSINGS` |

---

## Main Menu (`MainMenuGUI.java`)

### Slot Layout (6 rows, 54 slots)
```
[ ][ ][ ][ ][T][T][G][ ][X]   row 1 (top nav)
[ ][A][B][C][D][E][ ][ ][ ]   row 2 (core features)
[ ][F][G][H][I][J][ ][ ][ ]   row 3 (features cont.)
[ ][K][L][M][N][O][ ][ ][ ]   row 4 (features cont.)
[ ][ ][ ][ ][ ][ ][ ][ ][ ]   row 5 (empty or tips)
[ ][ ][ ][ ][ ][ ][ ][ ][ ]   row 6 (bottom)
```

> Note: Actual slot assignments should be verified from `MainMenuGUI.java`. The above
> is schematic. The existing layout is working — this is a content/text pass only.

### Feature Button Names (Egyptian)
Map the existing slot assignments to Egyptian names:

| Slot | Feature | Egyptian Name | Icon Material |
|---|---|---|---|
| 6 | Gang Home | Dynasty Hall | GOLD_BLOCK |
| 8 | Close | Leave the Palace | BARRIER |
| 10 | Mine Browser | Sacred Tombs | STONE_PICKAXE |
| 11 | Pickaxe Home | Your Khopesh | GOLDEN_PICKAXE |
| 12 | Pickaxe Enchants | Divine Enchantments | ENCHANTING_TABLE |
| 13 | Rank Progression | Divine Ranks | TOTEM_OF_UNDYING |
| 14 | Prestige | Ascension | NETHER_STAR |
| 19 | Shop | Merchant's Bazaar | EMERALD |
| 20 | Sell Center | Sell Your Haul | GOLD_INGOT |
| 21 | Crates Hub | Pharaoh's Relics | CHEST |
| (AH) | Auction House | Grand Bazaar | PLAYER_HEAD |
| (Quests) | Quests | Sacred Quests | WRITABLE_BOOK |
| (Cosmetics) | Cosmetics | Divine Adornments | LIME_DYE |
| (Leaderboard) | Leaderboards | Hall of Legends | DIAMOND |
| (Warps) | Warps | Sacred Warps | COMPASS |
| (Events) | Events | Sacred Rites | CLOCK |
| (Boosts) | Boosts | Divine Blessings | BEACON |
| (Black Market) | Shadow Bazaar | COAL_BLOCK |
| (Settings) | Settings | Settings | COMPARATOR |

### Rotating Tips (MainMenuGUI)
Update the 8 rotating tips to Egyptian voice:

```java
// Replace existing TIPS array with:
private static final String[] TIPS = {
    "<gray>Mine in the <gold>Sacred Tombs</gold> and sell your haul to earn <gold>✦ Coins</gold>.",
    "<gray>Enchant your <gold>Khopesh</gold> with <gold>◆ Relics</gold> to mine faster and earn more.",
    "<gray>Found a <gold>Dynasty</gold> with allies and compete for the weekly Dynasty War bonus.",
    "<gray>Ascend when you reach <gold>[Pharaoh]</gold> rank to unlock permanent divine power.",
    "<gray>Visit the <gold>Shadow Bazaar</gold> for rare rotating goods — it refreshes hourly.",
    "<gray>Complete <gold>Sacred Quests</gold> daily and weekly for <gold>◆ Relics</gold> and <gold>✦ Coins</gold>.",
    "<gray>Try your luck at the <gold>Sphinx's Gamble</gold> — double your gold or lose it all.",
    "<gray>Open <gold>Pharaoh's Relics</gold> for rare enchantments, cosmetics, and divine rewards.",
};
```

---

## Rank Progression GUI (`RankProgressionGUI.java`)

### Title: `DIVINE RANKS`

### Rank Item Lore Template
```
<gold><!italic>[Pharaoh] — Rank Z
<gray>────────────────────────
<color:#E8C87A>● The living god — peak of mortal rank
<gray>────────────────────────
<gold>✦ Cost: <white>500,000,000 Coins
<gray>Cumulative: <white>1,548,455,000 Coins
<gray>────────────────────────
<gray>Unlocks: <gold>Tomb of Anubis
<gray>Kit: <gold>Pharaoh's Armory
<gray>────────────────────────
<green>▸ Click to rank up to Pharaoh
```

### Current Rank Indicator
```
<color:#E8C87A>✦ Your Rank: <gold>[Vizier] — Rank L
<gray>────────────────────────
<gold>✦ Balance: <white>4,205,000 Coins
<green>▸ You can afford this rank!
```

---

## Mine Browser GUI (`MineBrowserGUI.java`)

### Title: `SACRED TOMBS`

### Mine Item Lore Template
```
<gold><!italic>Tomb of Anubis
<gray>────────────────────────
<color:#E8C87A>● Sacred to the god of judgment
<gray>────────────────────────
<gold>Composition:
<gray>+ 100% Ancient Debris
<gray>────────────────────────
<gold>✦ Avg value: <white>600 Coins/block
<gray>Reset interval: <gold>15 minutes
<gray>────────────────────────
<yellow>Requires: <gold>[Ascendant] — Rank Y
<gray>────────────────────────
<green>▸ Click to teleport to this tomb
```

### Locked Mine Item
```
<gray><!italic>Tomb of Anubis — LOCKED
<gray>────────────────────────
<red>✗ Requires rank <gold>[Ascendant]</gold>
<gray>You are: <gold>[Vizier] — Rank L
<gray>────────────────────────
<red>▸ Rank up to unlock this tomb
```

---

## Prestige / Ascension Confirm GUI (`PrestigeConfirmGUI.java`)

### Title: `ASCEND?`

### Confirm Item (slot 11 — green glass pane or custom item)
```
<green><!italic>✦ Ascend to the Next Life
<gray>────────────────────────
<color:#E8C87A>You will be reborn as a Slave.
<color:#E8C87A>Your Coins, Relics, and Khopesh
<color:#E8C87A>enchantments are preserved.
<gray>────────────────────────
<gold>Cost: <white>10,000 ◆ Relics
<gold>Cost: <white>100,000,000 ✦ Coins
<gray>────────────────────────
<gold>Reward: <white>[Awakened I] title
<gold>Reward: <white>+5% Relic earn permanently
<gold>Reward: <white>+10 Ascension Points
<gray>────────────────────────
<green>▸ CONFIRM — Ascend now
```

### Cancel Item (slot 15 — red glass pane)
```
<red><!italic>✗ Return to the Mortal Realm
<gray>────────────────────────
<gray>Do not Ascend yet.
<gray>────────────────────────
<red>▸ Cancel
```

---

## Khopesh Home GUI (`PickaxeHomeGUI.java`)

### Title: `KHOPESH`

### Khopesh Display Item Lore
```
<gold><!italic>Khopesh of [PlayerName]
<gray>────────────────────────
<color:#E8C87A>Ancient Khopesh — Blessed by the Forge God
<gray>────────────────────────
<gold>Enchantments:
<gray>+ Anubis' Speed V
<gray>+ Luck of Thoth III
<gray>+ Hand of Ptah II
<gray>────────────────────────
<gold>◆ Relics invested: <white>34,250
<gray>────────────────────────
<green>▸ Click to view Divine Enchantments
```

---

## Pickaxe Enchants GUI (`PickaxeEnchantsGUI.java`)

### Title: `DIVINE ENCHANTMENTS`

### Enchant Item Lore Template (unlocked)
```
<gold><!italic>Anubis' Speed — Level III
<gray>────────────────────────
<color:#E8C87A>● Swift as the god of death himself
<gray>────────────────────────
<gold>Effect: <white>+60% mining speed
<gray>Current level: <gold>III / V
<gray>────────────────────────
<gold>◆ Cost to upgrade: <white>2,000 Relics
<gray>You have: <gold>◆ 4,500 Relics
<gray>────────────────────────
<green>▸ Click to upgrade to Level IV
```

### Enchant Item Lore Template (maxed)
```
<gold><!italic>Anubis' Speed — MAX
<gray>────────────────────────
<color:#E8C87A>● Swift as the god of death himself
<gray>────────────────────────
<gold>Effect: <white>+100% mining speed (max)
<gray>────────────────────────
<gold>✓ Fully unlocked — the gods are pleased
```

---

## Dynasty Home GUI (`GangHomeGUI.java`)

### Title: `DYNASTY — [Dynasty Name]`

### Dynasty Info Item
```
<gold><!italic>House of [DynastyName]
<gray>────────────────────────
<color:#E8C87A>● Members: 8 / 20
<color:#E8C87A>● Dynasty Rank: #3 on the Hall of Legends
<gray>────────────────────────
<gold>✦ Dynasty Treasury: <white>4,200,000 Coins
<gold>◆ Dynasty Relics: <white>1,250
<gray>────────────────────────
<gray>Your contribution this week: <gold>2,400 blocks
<gray>────────────────────────
<green>▸ Click to view Dynasty details
```

---

## Sacred Quests GUI (`QuestsMenuGUI.java`)

### Title: `SACRED QUESTS`

### Active Quest Item
```
<gold><!italic>Daily: Mine 500 blocks in any Tomb
<gray>────────────────────────
<color:#E8C87A>● The gods demand tribute from the depths
<gray>────────────────────────
<gold>Progress: <white>312 / 500 blocks
<gray>────────────────────────
<gold>Reward: <white>◆ 50 Relics + ✦ 10,000 Coins
<gray>Time remaining: <gold>14h 22m
<gray>────────────────────────
<green>▸ In progress...
```

---

## Shadow Bazaar GUI (`BlackMarketMenuGUI.java`)

### Title: `SHADOW BAZAAR`

### Rotating Item Lore
```
<gray><!italic>Blessed Khopesh Shard
<gray>────────────────────────
<color:#E8C87A>● A fragment of divine power, sold in secret
<gray>────────────────────────
<gold>✦ Price: <white>50,000 Coins
<gray>────────────────────────
<gray>Stock: <gold>3 remaining
<gray>Refreshes in: <gold>23 minutes
<gray>────────────────────────
<green>▸ Click to purchase
```

> Note: Black Market is a stub. Build instructions in Phase 8.

---

## Crates Hub GUI (`CratesHubGUI.java`)

### Title: `PHARAOH'S RELICS`

### Crate Button Lore
```
<gold><!italic>Pharaoh's Reliquary
<gray>────────────────────────
<color:#E8C87A>● Sealed with the Pharaoh's own mark
<gray>────────────────────────
<gray>Contains:
<gray>+ 200–1,000 ◆ Relics
<gray>+ Rare Khopesh enchant scrolls
<gray>+ Exclusive cosmetic tags
<gray>+ ✦ Coin pouches
<gray>────────────────────────
<gold>Keys owned: <white>3
<gray>────────────────────────
<green>▸ Click to open (uses 1 key)
```

---

## Hall of Legends GUI (`LeaderboardSelectorGUI.java`)

### Title: `HALL OF LEGENDS`

### Leaderboard Category Item
```
<gold><!italic>Wealthiest Souls
<gray>────────────────────────
<color:#E8C87A>● Those with the most gold in the Pharaoh's realm
<gray>────────────────────────
<gold>#1 <white>PlayerName — <gold>✦ 892,400,000
<gold>#2 <white>SomePlayer — <gold>✦ 441,200,000
<gold>#3 <white>AnotherPlayer — <gold>✦ 213,000,000
<gray>────────────────────────
<green>▸ Click to view full leaderboard
```

---

## Implementation Checklist (Phase 6)

### plugin-menu changes needed:
- [ ] `MainMenuGUI.java` — Title, button names, TIPS array
- [ ] `RankProgressionGUI.java` — Title, all rank display names and lore
- [ ] `MineBrowserGUI.java` — Title, all mine names and lore
- [ ] `PrestigeConfirmGUI.java` — Title, confirm/cancel lore
- [ ] `PrestigeShopMenuGUI.java` — Title, upgrade item lore
- [ ] `PickaxeHomeGUI.java` — Title, Khopesh display lore
- [ ] `PickaxeEnchantsGUI.java` — Title, all enchant names and lore
- [ ] `GangHomeGUI.java` — Title, Dynasty wording throughout
- [ ] `QuestsMenuGUI.java` — Title, quest item lore
- [ ] `BlackMarketMenuGUI.java` — Title, all lore
- [ ] `CratesHubGUI.java` — Title, crate names and lore
- [ ] `LeaderboardSelectorGUI.java` — Title, category names
- [ ] `CosmeticsMenuGUI.java` — Title, cosmetic item lore
- [ ] `BoostsDetailGUI.java` — Title, boost item lore
- [ ] `WarpsMenuGUI.java` — Title, warp item lore
- [ ] `KitsMenuGUI.java` — Title, kit item lore
- [ ] `SellCenterGUI.java` — Title, sell item lore
- [ ] `ShopCategoryPickerGUI.java` — Title
- [ ] `ShopCategoryPageGUI.java` — Title, item lore
- [ ] `ShopQuantityGUI.java` — Title

### Other plugin GUI changes needed:
- [ ] `plugin-auctionhouse` — Auction house GUI title and lore
- [ ] `plugin-coinflip` — Coinflip GUI title and lore
- [ ] `plugin-ranks` — Config: display names, prefixes, messages
- [ ] `plugin-mines` — Config: mine display names
- [ ] `plugin-kits` — Config: kit display names
- [ ] `plugin-prestige` — Config: prestige messages, prefix format

---

## Implementation Order

The lowest-risk, highest-impact order:
1. Config files first (ranks, mines, prestige) — no recompile needed to test
2. MainMenuGUI.java — highest-visibility change
3. RankProgressionGUI.java — players see this constantly
4. MineBrowserGUI.java — players use this constantly
5. PickaxeEnchantsGUI.java — major investment portal
6. All remaining GUIs

---

*This spec is the implementation guide for Phase 6 code changes.*
*Layout rules remain as per `GUI_DESIGN_SPEC.md`. This doc covers names, text, and colors only.*

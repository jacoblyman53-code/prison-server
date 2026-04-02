# Prison Server — GUI & Item Lore Design Specification

This document is the authoritative design standard for every inventory GUI and item lore string
in this server. All GUI work must conform to these rules without exception. Do not deviate
from this spec unless the user explicitly overrides a rule for a specific menu.

---

## 1. Layout Rules

### Slot Usage
- **No decorative filler.** Empty slots stay empty. Never use glass panes, stained glass,
  or any item purely as padding or border decoration.
- **Content fills from top-left.** Items are placed row by row starting at slot 0. No
  artificial centering or padding around content.
- **Controls belong in the bottom row only.** Row 6 (slots 45–53) is reserved for navigation,
  pagination, filters, and action buttons. Never mix controls into the content area.

### Inventory Size
| Use case | Rows | Notes |
|---|---|---|
| Content-heavy (shop category, AH, upgrades) | 6 | Bottom row = controls |
| Medium menus (help, tags, stats) | 3–5 | Bottom row = controls if needed |
| Simple dialogs (confirm, single action) | 3 | Centered layout acceptable |

### Reserved Slots
- **Slot 0 (top-left):** Always the close/back button. Never put content here.
- **Slot 53 (bottom-right):** Next Page button when pagination is present.
- **Slot 45 (bottom-left):** Previous Page button when pagination is present.

---

## 2. Title Formatting

| Menu type | Format | Example |
|---|---|---|
| Utility / generic | ALL CAPS | `HELP`, `SETTINGS`, `MINES` |
| Named feature or category | Title Case | `Auction House`, `Redstone`, `Farming` |
| Collection with count | Title Case + `(n)` | `Your Tags (7)`, `Upgrades (3)` |

Titles must never include color codes in the inventory title string — use the natural white/yellow
default that Minecraft renders. Style lives in the items, not the title bar.

---

## 3. Navigation & Controls

### Close / Back Button
- **Item:** Barrier block (red X appearance)
- **Name:** `§c✗ Close` or `§c← Back to [Parent Name]`
- **Slot:** Always slot 0 (top-left)
- **Lore:** `§7Click to close this menu.` or `§7Return to [Parent Menu].`

### Pagination
- **Next Page item:** Lime dye or green arrow
  - Name: `§a→ Next Page`
  - Lore: `§7Page §f{current} §7→ §f{next}`
  - Slot: 53 (bottom-right)
- **Previous Page item:** Gray dye or arrow
  - Name: `§7← Previous Page`
  - Lore: `§7Page §f{current} §7← §f{prev}`
  - Slot: 45 (bottom-left)

### Other Control Buttons
Place in the bottom row (slots 45–53) only. Leave empty slots between controls — do not fill
the bottom row with filler items.

---

## 4. Color Palette

Use legacy `§` codes or Adventure API components consistently. Never mix both in one lore block.

| Role | Color code | Adventure color | Usage |
|---|---|---|---|
| Section header / name | `§b` (aqua) | `AQUA` | ✦ bullet lines, item name on tooltip |
| Primary body text | `§7` (gray) | `GRAY` | Descriptions, metadata |
| Highlighted keyword | `§a` (green) | `GREEN` | Key words within descriptions |
| Proper nouns / names | `§e` (yellow) | `YELLOW` | Source names, rank names, locations |
| Currency / cost | `§6` (gold) | `GOLD` | Token costs, prices, $ lines |
| CTA line | `§a` (green) | `GREEN` | → Click to... lines |
| Positive / owned | `§a` (green) | `GREEN` | ✓ owned, active, enabled |
| Negative / inactive | `§c` (red) | `RED` | ✗ missing, None, disabled |
| Sub-bullets | `§3` (dark aqua) | `DARK_AQUA` | ◆ sub-list items |
| Benefit lines | `§a` (green) | `GREEN` | + [Category] lines |
| Item theme title | varies | varies | Match item's natural theme |

---

## 5. Bullet & Symbol System

Every line of item lore uses a specific symbol with a specific meaning. Never swap symbols between roles.

| Symbol | Color | Role |
|---|---|---|
| `●` | `§a` green | Help menu system entry title |
| `✦` | `§b` aqua | Primary section header / main metadata |
| `◆` | `§3` dark aqua | Sub-item within a ✦ section |
| `→` | `§a` green | Call-to-action line |
| `+` | `§a` green | Benefit / reward line |
| `$` | `§6` gold | Currency cost or stat |
| `✓` | `§a` green | Positive status (owned, active, enabled) |
| `✗` | `§c` red | Negative status (not owned, disabled) |

---

## 6. Item Lore / Tooltip Structure

### Standard Item Detail Card
Use this structure for any interactive item (upgrades, shop items, collectibles, tools).

```
§b[Item or Feature Name]              ← aqua, matches item theme
§7✦ §7Description that explains what  ← gray body, §agreen§7 on key word(s)
§7  this item or upgrade does.
§a+ §a[Reward Category]               ← green benefit, category in brackets
                                       ← blank line (empty lore entry "")
§b✦ §7Related Section: §f(count)      ← section header with count
§3  ◆ §7Sub-item one
§3  ◆ §7Sub-item two
                                       ← blank line
§6$ §6Cost/Stat Label: §fvalue         ← gold for currency stat
                                       ← blank line
§a→ §aClick to §nverb§a this thing!   ← green CTA, §n underline on the verb
§6$ §6Status Field: §cNone            ← red for inactive/empty state
```

### Rules for Lore Writing
- Always put a blank line (`""`) between sections — never run sections together.
- Every tooltip must end with a `→ Click to...` CTA unless the item is purely decorative/display.
- Key descriptive words (the most important noun or verb in the description) get `§a` green highlight.
- Proper nouns (crate names, rank names, mine names, plugin names) get `§e` yellow.
- Level or tier is shown as a **number suffix** on the item name: `Auto Sell 5`, not `Auto Sell Lvl 5`.
- Counts in section headers use parentheses: `Upgrades: (2)`, not `Upgrades: 2`.
- `None` is always `§cNone` (red). Never use gray or white for an inactive state.
- `Active` / `Owned` / `Enabled` is always `§a` (green).

### Chat Preview Lines
When an item affects how a player appears in chat (tags, prefixes, name colors), include:

```
                                       ← blank line
§b✦ §7Chat Preview:
§7[Tag] [Rank] Username: Hey!          ← render exactly as it appears in chat
```

---

## 7. Help Menu Standard

- **Inventory size:** 3–5 rows depending on number of systems
- **Title:** `HELP` (all caps)
- **Item type:** PAPER for every entry — no exceptions
- **Item name:** `§a● §fSYSTEM NAME §7(/command)` — green bullet, white name, gray command
- **Lore line 1:** `§7Description in gray. Conversational, one sentence.`
- **Lore line 2:** `§a→ §aClick to open §n[system name]§a.`
- Do not include stat/cost sections in help items — description + CTA only.

Example:
```
Name:  §a● §fTAGS §7(/tags)
Lore:  §7Click here to view your flashy new tags or equip tags you own!
       §a→ §aClick to open §ntags§a.
```

---

## 8. Shop Menus

### Category Page
- **Size:** 6 rows
- **Title:** Category name in Title Case (`Redstone`, `Farming`, `Minerals`)
- **Slot 0:** Back button (`§c← View Shops`)
- **Content rows (1–5):** Shop items packed from top-left, no filler
- **Bottom row:** Navigation only

### Shop Home Page
- **Size:** 3–4 rows
- **Title:** `Shop`
- **Items:** One item per category, representing the category visually
- **Item lore:** Follow standard detail card — name, description, `→ Click to browse [category].`

### Shop Item Lore
```
§6[Item Name]
§b✦ §7Buy: §f{buy_price} §6tokens
§b✦ §7Sell: §f{sell_price} §6tokens
                        ← blank line
§a→ §aLeft-click §nto buy§a.
§a→ §aRight-click §nto sell§a.
```

---

## 9. Auction House

- **Size:** 6 rows
- **Title:** `Auction House`
- **Content rows (1–5):** Listings packed from top-left
- **Slot 53:** Next Page (`§a→ Next Page`, lore: `§7Page §f{n} §7→ §f{n+1}`)
- **Slot 45:** Previous Page

### Listing Item Lore
Preserve original item lore, then append an AH section below a separator:
```
[original item lore]
§8§m--------------------           ← dark gray strikethrough divider
§b✦ §7Seller: §f{seller}
§b✦ §7Price: §6{price} tokens
§b✦ §7Listed: §f{time ago}
                        ← blank line
§a→ §aClick to §nbuy§a this listing!
```

---

## 10. Upgrade Menus (Pickaxe, Tools, etc.)

- **Size:** 6 rows
- **Title:** `Upgrades` or `[Tool] Upgrades`
- **Slot 0:** Red X close button
- **Content:** Upgrade items packed from top-left
- **Upgrade item lore:** Full detail card (see Section 6)
  - Name includes current level as suffix: `Auto Sell 5`
  - Includes `$ Tokens Spent:` stat
  - Includes `$ Auto-Upgrades: §cNone` or `§aEnabled` status
  - `→ Click to Manage` CTA

---

## 11. Confirmation Dialogs

Used before any destructive or expensive action (prestige, purchase, reset).

- **Size:** 3 rows
- **Title:** `Confirm [Action]`
- **Layout:**
  - Slot 11: Green confirm item (Lime Wool or Lime Dye)
    - Name: `§a✓ Confirm`
    - Lore: `§7Click to confirm §a[action description]§7.`
  - Slot 13: Display item showing what is being confirmed
    - Name: Item or action name
    - Lore: Full detail card of what's being confirmed
  - Slot 15: Red cancel item (Red Wool or Barrier)
    - Name: `§c✗ Cancel`
    - Lore: `§7Click to cancel and return.`
- No other items in the inventory.

---

## 12. Tags / Cosmetics Menu

- **Size:** 4–6 rows
- **Title:** `Your Tags (n)` with dynamic count
- **Item type:** Player heads or representative display items
- **Lore structure:** Full detail card including Chat Preview section
- **Top row:** Navigation bar (close, category filters if needed)
- **Status:** Show `§a✓ Equipped` or `§c✗ Not Owned` as the last metadata line before CTA

---

## 13. General Rules (Apply Everywhere)

1. **Every interactive item must have a `→ Click to...` CTA.** No silent buttons.
2. **Every menu must have a close/back button at slot 0.**
3. **Never put raw unformatted text in lore.** Every lore line uses the color system above.
4. **Blank lines between sections are mandatory.** One empty string `""` per section break.
5. **Item names must never be plain white default text** unless it is intentionally unformatted
   (e.g., a player-named item). All system-generated item names use the color palette.
6. **Consistency over creativity.** If a pattern exists in this spec, use it exactly.
   Do not invent new bullet symbols, new line formats, or new color roles.
7. **Lore must be concise.** Max ~4 lines of body text per section. If more detail is needed,
   split into sub-items with ◆ bullets rather than writing long paragraphs.

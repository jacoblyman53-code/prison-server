# PRODUCT IDENTITY — Egyptian Mythology Theme

> **Purpose:** The authoritative naming and theme reference for this product.
> Every rank name, mine name, currency name, crate name, and UI string should
> match this document. This is the spec that Phase 6 (GUI overhaul) implements.

---

## Theme Statement

**"The Pharaoh's Prison"** — An ancient Egyptian underworld where condemned souls
mine the eternal tombs, earn the Pharaoh's favor, and ascend through the ranks of
the divine hierarchy to claim godhood.

Players are not prisoners serving time. They are **souls proving their worth** to
the gods. The grind is a spiritual journey. Rank-up is not a promotion — it is
*elevation*. Prestige is not a reset — it is *Ascension*.

This framing justifies the grind loop thematically and makes every system feel
intentional rather than mechanical.

---

## Server Name

**Primary:** `The Pharaoh's Prison`
**Tagline:** *"Earn your place among the gods."*
**Short form** (for scoreboards, prefixes): `PHARAOH`

### Color Identity
| Element | Color | MiniMessage Tag | Hex |
|---|---|---|---|
| Primary accent | Gold | `<gold>` | `#FFAA00` |
| Secondary accent | Sand/Parchment | `<color:#E8C87A>` | `#E8C87A` |
| Background/dark | Obsidian | `<color:#1a1a2e>` | `#1a1a2e` |
| Highlight | Royal Blue | `<blue>` / `<color:#5555FF>` | `#5555FF` |
| Danger/negative | Blood Red | `<red>` | `#FF5555` |
| Success/positive | Emerald | `<green>` | `#55FF55` |
| Neutral text | Light Gray | `<gray>` | `#AAAAAA` |
| Headers | White | `<white>` | `#FFFFFF` |

---

## Currency Names

### Primary Currency (was: IGC)
**Name: Coins**
**Symbol:** `✦` (gold coin symbol in lore) or `C` in compact display
**In lore:** `<gold>✦ Coins`
**Description:** "Gold Coins of the Pharaoh — the lifeblood of the tombs."
- Earned by: mining and selling blocks at the Merchant
- Spent on: rank ascension, gang upgrades

### Secondary Currency (was: Tokens)
**Name: Relics**
**Symbol:** `◆` or `R` in compact display
**In lore:** `<color:#E8C87A>◆ Relics`
**Description:** "Ancient Relics of divine power — used to enchant your Khopesh."
- Earned by: ascension rewards, sacred quests, crate drops, sell streaks
- Spent on: Khopesh enchantments (custom pickaxe upgrades)

### Display Convention
- Full amounts: `<gold>✦ 1,250,000 Coins`
- Compact (scoreboard/chat): `<gold>1.25M C`
- Relics full: `<color:#E8C87A>◆ 840 Relics`
- Relics compact: `<color:#E8C87A>840 R`

---

## Rank Names (A → Z, 26 Ranks)

The ranks follow the soul's journey from condemned prisoner to living god.
Each name is drawn from Egyptian mythology, hierarchy, and divine titles.

| Letter | Rank Name | Title Prefix | Theme |
|---|---|---|---|
| A | Slave | `[Slave]` | The lowest — condemned to the deepest tomb |
| B | Serf | `[Serf]` | Bound to labor, no freedom |
| C | Laborer | `[Laborer]` | One who works, not yet recognized |
| D | Craftsman | `[Craftsman]` | Skilled, but still property |
| E | Merchant | `[Merchant]` | First taste of commerce |
| F | Scribe | `[Scribe]` | Knowledge begins here |
| G | Soldier | `[Soldier]` | Earns the right to fight |
| H | Officer | `[Officer]` | Commands in the Pharaoh's name |
| I | Embalmer | `[Embalmer]` | Touches the sacred dead |
| J | Architect | `[Architect]` | Builder of monuments |
| K | Physician | `[Physician]` | Healer, trusted by royalty |
| L | Vizier | `[Vizier]` | The Pharaoh's right hand |
| M | Nomarch | `[Nomarch]` | Governor of a nome (province) |
| N | High Priest | `[High Priest]` | Speaks to the gods |
| O | Sage | `[Sage]` | Ancient wisdom keeper |
| P | Sorcerer | `[Sorcerer]` | Commands forbidden power |
| Q | Oracle | `[Oracle]` | Sees beyond mortal sight |
| R | Djed | `[Djed]` | Pillar of stability, Osiris symbol |
| S | Anubite | `[Anubite]` | Servant of Anubis, weigher of souls |
| T | Horus-Born | `[Horus-Born]` | Touched by the falcon god |
| U | Champion | `[Champion]` | Proven in the divine arena |
| V | Demigod | `[Demigod]` | Half mortal, half divine |
| W | Immortal | `[Immortal]` | Has passed through death |
| X | Reborn | `[Reborn]` | Osiris's resurrection complete |
| Y | Ascendant | `[Ascendant]` | Rising to the heavens |
| Z | Pharaoh | `[Pharaoh]` | The living god — peak of mortal rank |

### Rank Display Colors
- Ranks A–E: `<gray>` (humble origins)
- Ranks F–J: `<white>` (recognized)
- Ranks K–O: `<yellow>` (elevated)
- Ranks P–T: `<gold>` (powerful)
- Ranks U–Y: `<light_purple>` (divine)
- Rank Z: `<color:#FFD700>` bold (Pharaoh — unique gold)

---

## Mine Names

Mines are the **Tombs of the Gods** — each sacred to a different Egyptian deity.
Higher-tier mines are more dangerous (in lore) and more valuable (in blocks).

| Tier | Mine Name | Deity | Block Tier |
|---|---|---|---|
| 1 (A–B) | Tomb of Aten | Sun disk god | Stone, Coal |
| 2 (C–D) | Tomb of Thoth | God of knowledge | Stone, Coal, Iron |
| 3 (E–F) | Tomb of Sobek | Crocodile god | Iron, Coal, Gold |
| 4 (G–H) | Tomb of Hapi | Nile flood god | Gold, Iron, Lapis |
| 5 (I–J) | Tomb of Sekhmet | Lioness war goddess | Gold, Lapis, Redstone |
| 6 (K–L) | Tomb of Bastet | Cat goddess | Gold, Diamond, Lapis |
| 7 (M–N) | Tomb of Ptah | Craftsman god | Diamond, Gold, Emerald |
| 8 (O–P) | Tomb of Seth | God of chaos | Diamond, Emerald |
| 9 (Q–R) | Tomb of Isis | Magic goddess | Diamond, Emerald, Netherite scrap |
| 10 (S–T) | Tomb of Horus | Sky god | Emerald, Netherite |
| 11 (U–V) | Tomb of Ra | Supreme sun god | Netherite, Emerald |
| 12 (W–X) | Tomb of Osiris | God of the dead | Netherite, Ancient Debris |
| 13 (Y–Z) | Tomb of Anubis | Judgment god | Pure Netherite |
| Free Mine | Pit of Souls | (none) | Stone only — starter mine |

### Donor Mines
| Name | Required Donor Rank | Description |
|---|---|---|
| Sanctum of Amun | Donor | Sacred and blessed — mixed ores |
| Sanctum of Hathor | DonorPlus | Love goddess mine — rare ores |
| Hall of the Pharaoh | Elite | Pharaoh's private mine — premium ores |
| Chamber of the Gods | ElitePlus | The divine vault — max yield |

### Mine Display
- Mine name in menu: `<gold>Tomb of Horus`
- Mine subtitle: `<gray>Sacred to the Sky God`
- Required rank: `<yellow>Requires: Horus-Born <gray>(Rank T)`

---

## Prestige System: Ascension

**Name: Ascension**
**Lore:** "A soul who has reached Pharaoh rank may petition the gods for
Ascension — to die, be judged by Anubis, and be reborn more powerful.
Each Ascension is a new life, with greater divine favor."

| Prestige # | Ascension Name | Lore Title |
|---|---|---|
| 1 | First Awakening | `[Awakened I]` |
| 2 | Second Awakening | `[Awakened II]` |
| 3 | Third Awakening | `[Awakened III]` |
| 4 | Soul of the Sands | `[Sand Soul]` |
| 5 | Heart Weighed | `[Weighed]` |
| 6 | Passed Judgment | `[Judged]` |
| 7 | Blessed of Osiris | `[Osiris-Blessed]` |
| 8 | Child of Ra | `[Ra's Child]` |
| 9 | Eye of Horus | `[Eye of Horus]` |
| 10 | Living God | `[Living God]` ← max |

**Ascension display:** `<light_purple>[Awakened II] <gold>[Pharaoh]` PlayerName

---

## Crate Names

| Old Name | New Name | Rarity | Color |
|---|---|---|---|
| Vote Crate | Canopic Chest | Common | `<gray>` |
| Regular Crate | Tomb Chest | Uncommon | `<yellow>` |
| Ancient Crate | Pharaoh's Reliquary | Rare | `<gold>` |
| (future) | Chamber of the Gods Crate | Legendary | `<light_purple>` |

**Opening animation text:** "The ancient seals break... the Pharaoh's gift is revealed."

---

## Donor Rank Names

| Old Name | New Name | Color | Prefix |
|---|---|---|---|
| Donor | Devotee | `<aqua>` | `[Devotee]` |
| DonorPlus | Acolyte | `<blue>` | `[Acolyte]` |
| Elite | High Priest | `<light_purple>` | `[High Priest]` |
| ElitePlus | Pharaoh's Chosen | `<gold>` | `[Chosen]` |

---

## Custom Pickaxe: The Khopesh

The custom pickaxe is the **Khopesh** — the curved Egyptian sword repurposed as
a divine mining tool.

- Item name: `<gold><!italic>Khopesh of [PlayerName]` (personalized)
- Lore header: `<color:#E8C87A>Ancient Khopesh — Blessed by the Forge God`
- Enchant menu title: `KHOPESH ENCHANTMENTS`

### Enchant Name Mapping
| Old Name | Egyptian Name | Thematic Reason |
|---|---|---|
| Efficiency (custom) | Anubis' Speed | Swift as the god of death |
| Fortune | Luck of Thoth | God of knowledge/fate |
| Explosion | Wrath of Seth | Chaos god's destructive power |
| Laser | Eye of Ra | Ra's all-seeing light beam |
| Nuke | Wrath of Apophis | The serpent who devours all |
| Vein Miner | Hand of Ptah | Master craftsman's reach |
| Auto-Smelt | Forge of Ptah | Ptah's sacred furnace |
| Token Finder | Relic Sight | Sees hidden relics in stone |
| Speed | Blessing of Shu | God of wind and movement |
| Jump | Leap of Horus | Falcon god's aerial gift |
| Haste | Heartbeat of Sekhmet | War goddess's battle fury |

---

## Gang System: Dynasties

Gangs are renamed **Dynasties** — rival ruling houses competing for dominance.

- Gang/Dynasty creation: "Found a Dynasty"
- Gang mine: "Dynasty Tomb" (exclusive to members)
- Gang leaderboard: "Dynasty Rankings"
- Gang chat prefix: `<gold>[Dynasty: {name}]`

---

## Events System: Sacred Rites

Server events are themed as **Sacred Rites** — divine contests overseen by the gods.

- Double sell event: "Ra's Blessing" (the sun god's favor doubles your harvest)
- Double XP event: "Thoth's Wisdom" (knowledge multiplied)
- Drop party: "Pharaoh's Offering" (the Pharaoh scatters gifts)
- PvP event: "Trial of Horus" (combat in the Pharaoh's arena)

---

## Chat Format

```
[Ascension I] [Pharaoh] PlayerName: message
```

Colors:
- Ascension prefix: `<light_purple>`
- Rank prefix: `<gold>`
- Player name: `<white>` (or donor color if applicable)
- Separator colon: `<gray>`
- Message: `<white>`

Donor override (donor rank shows instead of mine rank):
```
[Chosen] PlayerName: message
```

---

## GUI Title Convention (Egyptian Theme)

All GUI titles follow the pattern: `EGYPTIAN NAME` in ALL CAPS (per GUI_DESIGN_SPEC).

| GUI | Old Title | New Title |
|---|---|---|
| Main Menu | PRISON | PHARAOH'S PRISON |
| Ranks | RANKS | DIVINE RANKS |
| Mines | MINES | SACRED TOMBS |
| Prestige | PRESTIGE | ASCENSION |
| Shop | SHOP | MERCHANT'S BAZAAR |
| Auction House | AUCTION HOUSE | GRAND BAZAAR |
| Crates | CRATES | PHARAOH'S RELICS |
| Coinflip | COINFLIP | SPHINX'S GAMBLE |
| Gangs | GANGS | DYNASTIES |
| Quests | QUESTS | SACRED QUESTS |
| Pickaxe | PICKAXE | KHOPESH |
| Enchants | ENCHANTMENTS | DIVINE ENCHANTMENTS |
| Donor Perks | DONOR PERKS | DIVINE BLESSINGS |
| Leaderboards | LEADERBOARDS | HALL OF LEGENDS |
| Cosmetics | COSMETICS | DIVINE ADORNMENTS |
| Black Market | BLACK MARKET | SHADOW BAZAAR |

---

## Lore Voice and Tone

Every lore line should feel like an ancient inscription — formal, grand, slightly ominous.

**Do:**
- "The gods smile upon your ascension."
- "Only the worthy may enter the Tomb of Anubis."
- "Your Relics pulse with ancient power."
- "Pharaoh demands tribute — rank up or be forgotten."

**Don't:**
- "Click to rank up!" (too casual)
- "You need 1,000,000 coins." (too mechanical, no theme)
- "Buy crates for better loot!" (too transactional)

**Lore template for rank-up confirmation:**
```
<gold>✦ Ascend to <rank_name>
<gray>Cost: <gold>✦ <amount> Coins
<color:#E8C87A>The gods watch. Prove yourself worthy.
<green>▸ Click to ascend
```

---

*This document is the source of truth for all naming and theme decisions.*
*When Phase 6 (GUI overhaul) is executed, every string in every GUI must match this spec.*

# The Pharaoh's Prison

> *"Earn your place among the gods."*

A premium, fully custom Minecraft 1.21 Prison server setup built from scratch — no LuckPerms, no Vault, no EssentialsX. 26 plugins, 28 GUIs, a complete economy system, and an Egyptian mythology theme that makes every system feel handcrafted and alive.

---

## What This Is

**The Pharaoh's Prison** is a complete, production-ready Prison server codebase built as a 26-module Gradle monorepo. Everything is custom: the permission engine, the economy, the mine system, the rank ladder, the GUI framework, and the database layer.

Players are condemned souls in an ancient Egyptian underworld — mining the sacred tombs of the gods, earning the Pharaoh's favor, and ascending through divine ranks from humble Slave to the living god Pharaoh.

---

## Feature Highlights

### Economy
- **Dual currency:** Coins (grind currency) and Relics (enchant currency)
- **26-rank ladder** (Slave → Pharaoh) with ~1.5 billion Coin total progression cost
- **Sell streak multiplier:** 1× → 1.5× sustained play reward (not AFK-farmable)
- **Full transaction logging** — every Coin movement is auditable

### Mine System
- **14 mine tiers** named after Egyptian gods (Tomb of Aten → Tomb of Anubis)
- **Donor mines** (4 tiers: Sanctum of Amun → Chamber of the Gods)
- **Configurable composition** — exact block percentages per mine
- **Auto-reset** on timer or threshold; smooth batch reset (no TPS spikes)

### Custom Khopesh (Pickaxe)
- **11 custom enchants** + 3 vanilla enchants purchasable with Relics
- Explosive, Laser, Nuke, Vein Miner, Auto-Smelt, Lightning, and more
- Full 654,000 Relic progression to max all enchants
- Each enchant named after an Egyptian deity

### Ascension (Prestige)
- **10 Ascension tiers** — from First Awakening to Living God
- Rank resets but Coins, Relics, and Khopesh enchants are **preserved**
- Permanent stat bonuses compound with each Ascension
- Exclusive cosmetic titles and particles at milestone Ascensions

### GUI System
- **28 interconnected GUIs** — all routed through the main menu
- Compliant with a rigorous design spec: no filler, consistent layout, MiniMessage throughout
- Egyptian theme: every menu has a tomb name, gold/sand color palette, and thematic voice

### Social Systems
- **Dynasty (Gang) system** — shared mines, Dynasty War events, leaderboards
- **Auction House** (Grand Bazaar) — player-to-player trading
- **Coinflip** (Sphinx's Gamble) — risk your Coins against another player
- **Daily + Weekly Quests** (Sacred Quests)
- **Leaderboards** (Hall of Legends) — Richest, Top Dynasty, Top Ascension

### Infrastructure
- **Zero external plugin dependencies** — no LuckPerms, Vault, or EssentialsX required
- **Custom permission engine** — three parallel trees (mine rank, donor rank, staff rank)
- **HikariCP connection pool** — production-grade DB with async write queue
- **BungeeCord messaging** — ready for multi-server expansion

---

## Tech Stack

| Component | Technology |
|---|---|
| Server | Paper 1.21.x |
| Language | Java 21 |
| Build | Gradle 8 (multi-module monorepo) |
| Database | MySQL 8.0+ / HikariCP |
| Text | Adventure API + MiniMessage |
| Plugins | 26 custom plugins, zero external dependencies |

---

## Requirements

- **Java 21** (required — Paper 1.21 does not run on earlier versions)
- **Paper 1.21.x** (latest build recommended)
- **MySQL 8.0+** (any host's managed DB works fine)
- **4–8 GB RAM** for 20–100 players

---

## Quick Start

```bash
# Clone the repository
git clone https://github.com/jacoblyman53-code/prison-server

# Build all 26 plugins
cd "prison-server/Minecraft server"
./gradlew shadowJar

# Copy built JARs to your server's plugins/ folder
# Configure MySQL in plugins/PrisonDatabase/config.yml
# Start your server
```

Full setup: see [INSTALLATION_GUIDE.md](INSTALLATION_GUIDE.md)

---

## Documentation

| Document | Description |
|---|---|
| [INSTALLATION_GUIDE.md](INSTALLATION_GUIDE.md) | 10-step quick start |
| [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) | Full server setup guide |
| [CONFIG_GUIDE.md](CONFIG_GUIDE.md) | Every config key documented |
| [EXTENSION_GUIDE.md](EXTENSION_GUIDE.md) | How to add mines, ranks, enchants, etc. |
| [ADMIN_TOOLS.md](ADMIN_TOOLS.md) | All admin commands |
| [PERMISSIONS_MATRIX.md](PERMISSIONS_MATRIX.md) | Full permission node reference |
| [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md) | Module dependency map |
| [GAMEPLAY_LOOP.md](GAMEPLAY_LOOP.md) | Player progression design |
| [ECONOMY_DESIGN.md](ECONOMY_DESIGN.md) | Economy analysis and balance |
| [PRODUCT_IDENTITY.md](PRODUCT_IDENTITY.md) | Egyptian theme naming spec |

---

## What Buyers Get

- ✓ Full source code for all 26 plugins
- ✓ Gradle build system — build in one command
- ✓ Complete documentation suite (this repo)
- ✓ MySQL schema with all tables auto-created on first run
- ✓ Config files with reasonable defaults
- ✓ Automated deploy script for Windows (WinSCP + Pterodactyl)

**Not included:**
- World files (see [WORLD_INTEGRATION_GUIDE.md](WORLD_INTEGRATION_GUIDE.md) for setup guidance)
- Hosting (use any Paper-compatible host)
- Tebex store setup (see [TEBEX_PRODUCT_MAP.md](TEBEX_PRODUCT_MAP.md))

---

## License

This codebase is sold for use on a single server. Resale or redistribution of the source code is not permitted.

---

*Built with Java 21, Paper 1.21, Adventure API, HikariCP, and zero compromises.*

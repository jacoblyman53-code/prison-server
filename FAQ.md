# FAQ

> Answers to questions buyers ask before and after purchase.

---

## Before Purchase

### Is this compatible with Paper 1.21?
Yes. The server is built specifically for Paper 1.21. It uses Paper's API directly.
It will NOT run on Spigot, CraftBukkit, or Fabric. Paper only.

### Do I need LuckPerms or Vault?
No. This server has a custom permission engine and a custom economy system.
LuckPerms and Vault are not required and will not be loaded even if present.

### What plugins do I need to install separately?
None. All 26 plugins are custom-built and included in this product.
You only need: Paper 1.21.x, Java 21, and MySQL 8.0+.

### Does it come with a world?
No. The product is code and configuration. You need to build or purchase a world.
See `WORLD_INTEGRATION_GUIDE.md` for minimum world requirements and setup instructions.
For a polished Egyptian-themed world, look for Egyptian Minecraft builds on
Minecraftmaps.com, Planet Minecraft, or commission a builder.

### What server host does this work with?
Any host that supports Paper 1.21 and MySQL. Tested on SparkHost (Sparked Host).
Works with BisectHosting, Pebblehost, MCProHosting, and any VPS (DigitalOcean, etc.).

### Do I need a VPS or will shared hosting work?
Shared hosting works fine. You need:
- The ability to run Paper (most prison-focused hosts support this)
- MySQL database access (most hosts provide this in the control panel)
- At least 4GB RAM

### How difficult is setup?
Intermediate. You need to:
- Build the plugins (one Gradle command)
- Configure the MySQL connection
- Set up mine regions in-game with admin commands

If you've run a Minecraft server before and know how to edit config files, you can set this up.
If this is your first server, read `DEPLOYMENT_GUIDE.md` carefully — it covers every step.

### Can I see the source code before buying?
The GitHub repository is private until purchase. The documentation (this repo) is public.

### Does this work with BungeeCord / Velocity?
The BungeeCord plugin messaging channel is registered and the `MessagingAPI` class is
ready for multi-server use. Individual servers can be configured to run behind a proxy.
This has NOT been tested in a full BungeeCord network setup.

---

## After Purchase / Setup

### How do I add my own ranks?
Edit `plugin-ranks/src/main/resources/config.yml` and add a new entry.
See `EXTENSION_GUIDE.md` → "Adding a New Rank" for step-by-step instructions.

### How do I change rank names to my own theme?
Edit the `display:` and `prefix:` values in `plugin-ranks/config.yml`.
For a full theme change, also update mine names in `plugin-mines/config.yml` and
GUI strings in `plugin-menu` source code (requires recompile).

### Can I add more mines?
Yes. See `EXTENSION_GUIDE.md` → "Adding a New Mine".
You can add unlimited mines by adding entries to `plugin-mines/config.yml`.

### How do I set up Tebex for donations?
See `TEBEX_PRODUCT_MAP.md` for exact package definitions and commands.
See `MONETIZATION_PLAN.md` for pricing strategy.
Note: `plugin-tebex` webhook handler is partially implemented (BETA) — see `TECHNICAL_DEBT_LOG.md` TD-003.

### The mines aren't filling with blocks — why?
Mine coordinates haven't been set. All mines default to `[0,64,0]` placeholders.
Run: `/mine setcorner1 A`, `/mine setcorner2 A`, `/mine setspawn A`, `/mine enable A`, `/mine fill A`
Repeat for each mine. See `WORLD_INTEGRATION_GUIDE.md` for full mine setup.

### How do I give a player a donor rank?
```
/donor grant PlayerName devotee
```
Replace `devotee` with `acolyte`, `highpriest`, or `chosen` for higher tiers.

### Players can't see the main menu — how do I open it?
The menu opens via the `/menu` command, or configure it to open when right-clicking
a specific item (check `plugin-menu/config.yml` for the trigger item setting).

### Can I change the sell prices?
Yes. Edit `plugin-economy/src/main/resources/config.yml` → `sell-prices:` block.
Changes take effect after `/reload confirm` or server restart.

### How do I add a custom enchant?
Adding a new enchant type requires source code changes to `plugin-pickaxe`.
Adding a new level to an existing enchant or adding a purchasable vanilla enchant
(like Mending) is config-only. See `EXTENSION_GUIDE.md`.

### The prestige/ascension seems free — how do I add a cost?
This is a known gap (TD-001 in `TECHNICAL_DEBT_LOG.md`).
Add `ascension-costs` to `plugin-prestige/config.yml` following the format in `ASCENSION_SYSTEM.md`.
The plugin code also needs to be updated to read and enforce this cost.

### Can I use this with multiple servers (network)?
The BungeeCord channel is registered. Each server needs its own MySQL database or
share a database with proper schema isolation. Multi-server support is architectural
but has not been tested in a network setup.

### Is there a web admin panel?
Not in v1. The `web-dashboard/` directory exists but is empty.
A web admin panel is planned for v2.

### Can I sell this to others?
No. The license allows use on a single server. Resale or redistribution is not permitted.

---

## Economy Questions

### How long does it take to reach max rank?
For a dedicated player with no enchants: approximately 80–100 hours.
With a fully-enchanted Khopesh: approximately 30–40 hours.
This is tuned for 1–3 months of casual play or 2–4 weeks of dedicated grinding.

### The economy feels too fast/slow — how do I tune it?
See `ECONOMY_BALANCE_TODO.md` for specific recommendations.
The main levers are: sell prices, rank costs, and mine composition.
All are in config files. See `CONFIG_GUIDE.md` for the exact keys.

### Can players trade items?
Yes, via the Auction House (Grand Bazaar). Players can list items for a Coin price.
Direct player-to-player trades are not implemented (no `/trade` command).

---

*Still have a question? Open an issue on the GitHub repository or contact the seller directly.*

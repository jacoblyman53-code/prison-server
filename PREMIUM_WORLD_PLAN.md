# PREMIUM WORLD PLAN

> What a high-quality world looks like for The Pharaoh's Prison.
> A premium world dramatically increases player retention and Tebex conversion.
> This document guides a world builder or helps you commission one.

---

## The Goal

A premium world should:
1. Make the Egyptian mythology theme visceral and immersive
2. Give players a "wow" moment when they first join
3. Make higher-tier mines feel exciting, not just bigger holes
4. Create areas that feel exclusive (donor mines, Dynasty mine, prestige areas)
5. Be functional — players can navigate without getting lost

---

## Spawn: The Eternal City

The spawn hub is an Egyptian city built above the mines — the surface world
where souls arrive before descending into the tombs.

### Key Elements

**Grand Pyramid Entry:**
- A large step pyramid visible from spawn
- Players spawn at the base of the pyramid steps
- The pyramid entrance leads down into the mine network
- Gold blocks and sandstone construction

**City Square:**
- Stalls and merchants around a central oasis (water fountain, date palms)
- Signs for commands: `/menu`, `/ranks`, `/quests`
- NPC merchants (if Citizens/NPCs are configured)
- The "Grand Bazaar" building for the Auction House

**Pharaoh's Palace:**
- Imposing structure at the back of the square
- Contains the cosmetics room, leaderboards board
- Visual representation of the server's prestige

### Materials
- Sandstone, Smooth Sandstone, Cut Sandstone
- Gold Blocks (accent)
- Dark Prismarine (water features)
- Terracotta (warm color variation)
- Torches, Lanterns, Sea Lanterns (lighting)

---

## Mine Network: The Tombs

Mines should look like tomb entrances, not just holes.

### Surface Entrances
Each mine tier has a distinctive entrance:
- **Early mines (A–E):** Simple stone archways with hieroglyph-like patterns
- **Mid mines (F–M):** More elaborate sandstone entrances with statues
- **Late mines (N–T):** Dark prismarine frames, glowing accents, imposing scale
- **End-game mines (U–Z):** Obsidian and gilded accents, particles if possible

### Mine Chamber Design
Inside each mine, the walls should feel like a real tomb:
- Stone block frame around the mining area
- Torches or lanterns along the walls
- An overhead observation area (staff/spectator walkway)
- Exit path clearly marked back to spawn

### Mine Layout
Two options:

**Option A: Linear Descent**
Mines are arranged underground in a spiral or linear path.
Players walk deeper to reach higher-tier mines.
Visual metaphor: descending into the underworld.

**Option B: Radial**
Mines radiate outward from the central hub.
Higher-tier mines are further from spawn but still connected.
Warps supplement the walking distance.

---

## Donor Mines: The Inner Sanctums

Donor mines should feel visually distinct and exclusive.

### Visual Language
- Gold and lapis accents (vs sandstone for standard mines)
- Higher ceiling (more spacious feeling)
- Better lighting
- Egyptian deity statues framing the entrance
- A barrier/gate that only opens for donor-rank players (symbolic)

### Sanctum of Amun (Devotee)
- Warm gold and sandstone palette
- Amun-Ra sun disk motif above entrance
- Spacious chamber with natural lighting from above

### Chamber of the Gods (Chosen)
- Purple and gold palette
- Elaborate hieroglyphs on all walls
- Multiple light sources: lanterns, sea lanterns, torches
- Clearly the most impressive donor mine — visual payoff for the price

---

## Dynasty Mine: The Valley of Conflict

The Dynasty mine should feel competitive and shared.

### Design
- Located in a separate valley or canyon
- Multiple mine chambers accessible from a shared courtyard
- A Dynasty leaderboard display in the courtyard
- Aggressive styling: dark stone, weapon motifs

---

## World Build Services

If you don't have time to build the world yourself, you can commission a world builder:

**Where to find Minecraft world builders:**
- **BuiltByBit (builtbybit.com)** — World build marketplace
- **MC-Market** — Alternative marketplace
- **Planet Minecraft** — Community, some paid builds
- **SpigotMC** — Some world builders advertise here
- **Fiverr** — Individual builders

**What to tell them:**
> "Egyptian mythology-themed Prison server world. Needs:
> - Egyptian city spawn hub with pyramid
> - 26 standard mine chambers with distinct entrances (themed to Egyptian gods)
> - 4 premium donor mine chambers (more elaborate)
> - Dynasty mine area
> - All mines are approximately [SIZE] blocks
> Style reference: [Egyptian Minecraft build you like]"

**Cost estimate:** $300–$1,500 depending on builder and detail level.
A premium world at $800–$1,200 will pay for itself through improved Tebex conversion.

---

## Free Resources

**Free Egyptian Minecraft builds to start from:**
- Planet Minecraft: search "Egyptian" — many free builds available
- Minecraft Maps: egyptian-themed maps often include temples, pyramids

**WorldEdit tips for efficiency:**
- `//copy` and `//paste` to repeat similar mine chambers
- `//rotate` to vary entrance orientations
- `//stack` to create repeating columns

---

## Technical Requirements

All builds must work with the mine system:

- Mine bounding box areas must be **flat-bottomed** (no sloped floor)
- The bounding box should be **entirely air** when `/mine fill` runs — don't put blocks inside the mine area that you want to stay
- Spawn points should be **outside** the bounding box, at the entrance
- Donor mine spawn must be **inside the permission-gated area** (so players who lose session time are teleported to the mine's own entrance, not spawn)

---

## Checklist: World Ready for Launch

- [ ] Spawn hub built and set with `/setworldspawn`
- [ ] All 26 standard mines built and configured
- [ ] All 4 donor mines built and configured
- [ ] Dynasty mine built and configured
- [ ] Pit of Souls starter mine built and configured
- [ ] All mine warps created
- [ ] Shop area exists (even if shop is BETA)
- [ ] Auction House area exists
- [ ] Navigation signs/NPCs at spawn
- [ ] Basic lighting throughout (no dark areas that spawn mobs in mines)
- [ ] Spawn set for all important areas
- [ ] Tested: new player can navigate from spawn to first mine without getting lost

---

*See `WORLD_INTEGRATION_GUIDE.md` for the technical mine setup procedure.*
*See `MINIMUM_VIABLE_WORLD.md` for a 3-hour minimal world to test the code first.*

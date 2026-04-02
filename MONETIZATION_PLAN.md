# MONETIZATION PLAN

> **Purpose:** Revenue model for a server running The Pharaoh's Prison.
> This is guidance for the buyer who runs the server, not for the product sale itself.
> The goal: a setup where a server can sustain $500–$2,000/month in Tebex revenue.

---

## Revenue Model: Cosmetic-Heavy, Pay-for-Convenience

This server uses a **cosmetic-primary, convenience-secondary** model.

**Why:** This model:
1. Complies with Mojang's EULA (no selling gameplay advantages that affect PvP)
2. Has the highest long-term sustainability — players feel safe spending
3. Avoids the reputation damage of pure "pay to win"
4. Still generates strong revenue because cosmetics + convenience are what players want

### What Can Be Sold (EULA Safe)
- Cosmetic items: tags, particle effects, nick colors, pets, hats
- Rank prefixes in chat (cosmetic rank, not affecting gameplay)
- Donor mine access (a separate track, not the main mine ladder)
- Boosters (temporary multipliers — legal in Mojang EULA since 2014 FAQ update)
- Crate keys
- Starting resources on first join only (one-time "starter pack")
- Priority queue access when server is full

### What Cannot Be Sold (EULA Violations)
- Permanent gameplay advantages over non-payers in main progression (rank skipping)
- The ability to /fly in survival contexts that affect gameplay
- Invulnerability, overpowered weapons, anything that creates unfair PvP advantages

> **Note:** Donor mines are a grey area. They are a separate track, not an advantage
> in the main economy. They're defensible as "premium content" rather than "pay to win."
> Most established prison servers sell donor mines without EULA issues.

---

## Pricing Strategy

### Pricing Psychology for Minecraft Server Stores

The market rates for prison server Tebex products (2024–2025):

| Product Type | Market Range | Recommended Price |
|---|---|---|
| Chat cosmetic tag | $3–$10 | $5–$8 |
| Tag pack (5 tags) | $10–$25 | $15 |
| Donor rank tier 1 | $10–$25 | $15/mo or $50 lifetime |
| Donor rank tier 2 | $20–$50 | $25/mo or $80 lifetime |
| Donor rank tier 3 | $40–$80 | $45/mo or $120 lifetime |
| Donor rank tier 4 | $60–$150 | $70/mo or $175 lifetime |
| Crate key (single) | $2–$5 | $3 |
| Crate key (5-pack) | $8–$20 | $12 (20% discount) |
| Crate key (10-pack) | $15–$35 | $20 (33% discount) |
| 7-day booster (2×) | $5–$15 | $8 |
| 30-day booster (2×) | $15–$40 | $20 |
| Starter pack | $5–$15 | $10 |

### Revenue Estimate at Various Player Counts

| Active Players | Conversion Rate | Avg Spend | Monthly Revenue |
|---|---|---|---|
| 20 | 10% = 2 paying | $20/mo avg | $40/mo |
| 50 | 10% = 5 paying | $25/mo avg | $125/mo |
| 100 | 12% = 12 paying | $30/mo avg | $360/mo |
| 200 | 12% = 24 paying | $35/mo avg | $840/mo |
| 500 | 15% = 75 paying | $40/mo avg | $3,000/mo |

A successful mid-size prison server (100–200 players) should generate $500–$1,000/month.
This requires consistent new player acquisition (voting, advertising, word of mouth).

---

## Tebex Integration Requirements

### Current Status: 30% complete
The `plugin-tebex` module exists with scaffolding but the webhook handler is not implemented.

### What Needs to Be Built (Phase 8)
1. **Webhook endpoint** — Tebex sends a POST request when a purchase is made
2. **Command execution queue** — Store commands in DB, execute when player is online
3. **Package → command mapping** — Each Tebex package ID maps to server commands
4. **Online/offline delivery** — If player is offline, queue the commands for next login

### Webhook Flow
```
Player buys on Tebex → Tebex sends webhook to server IP:port
→ plugin-tebex receives POST → validates secret → reads package ID
→ looks up command list → if player online, execute now
→ if player offline, insert into tebex_queue table
→ on player login, check tebex_queue → execute pending commands
```

---

## Recommended Tebex Store Structure

### Category: Donor Ranks
Four-tier ladder matching the game's donor rank system.

| Package | Type | Price | Commands on Purchase |
|---|---|---|---|
| Devotee | One-time | $50 | `lp user {player} parent set devotee` + welcome message |
| Devotee Monthly | Subscription | $15/mo | Same as above |
| Acolyte | One-time | $80 | `lp user {player} parent set acolyte` |
| Acolyte Monthly | Subscription | $25/mo | Same |
| High Priest | One-time | $120 | `lp user {player} parent set highpriest` |
| High Priest Monthly | Subscription | $45/mo | Same |
| Pharaoh's Chosen | One-time | $175 | `lp user {player} parent set chosen` |
| Pharaoh's Chosen Monthly | Subscription | $70/mo | Same |

> Note: Permission commands above assume permissions are granted via `plugin-donor`
> admin commands, not LuckPerms. Adjust to match actual implementation.

### Category: Pharaoh's Relics (Crate Keys)
| Package | Price | Command |
|---|---|---|
| 1 Canopic Key | $3 | `give {player} canopic_key 1` or kit command |
| 5 Canopic Keys | $12 | x5 |
| 1 Tomb Key | $5 | `give {player} tomb_key 1` |
| 5 Tomb Keys | $22 | x5 |
| 1 Pharaoh's Reliquary Key | $10 | `give {player} pharaoh_key 1` |
| 3 Pharaoh's Reliquary Keys | $25 | x3 |

### Category: Divine Blessings (Boosters)
| Package | Price | Duration | Effect |
|---|---|---|---|
| Ra's Blessing (personal, 7 days) | $8 | 7 days | 1.5× sell multiplier |
| Ra's Blessing (personal, 30 days) | $25 | 30 days | 1.5× sell multiplier |
| Thoth's Wisdom (personal, 7 days) | $8 | 7 days | 1.5× Relic earn |
| Server Boost (2h) | $15 | 2 hours | 2× sell for entire server |

### Category: Divine Adornments (Cosmetics)
| Package | Price | Contents |
|---|---|---|
| Egyptian Tag Pack | $12 | 5 themed chat tags |
| Chosen One Tag | $8 | Single premium tag |
| Anubis Particle Set | $10 | Anubis-themed particles |
| Golden Pharaoh Cosmetic Set | $20 | Full cosmetic bundle |
| Starter Pack of the Condemned | $10 | First-join boost (one-time) |

---

## Monetization Psychology Touch Points

Key moments where a player is most likely to spend real money:

1. **First crate opening** — Player sees what's inside a Pharaoh's Reliquary. Wants more keys.
   - *Trigger in-game:* After first crate open, action bar: `<gold>▸ Get more Reliquary keys in our store!`

2. **At rank walls** (G, J, O, T, Z) — Player hits a cost wall. A sell booster is tempting.
   - *Trigger:* When player can't afford rank-up, show a subtle hint in the rank GUI.

3. **After seeing a cosmetic tag in chat** — "How do I get that?"
   - *Ensure tags are visible and distinctive.*

4. **During a Ra's Blessing event** — Player wants to maximize the event window.
   - *Trigger:* Event announcement mentions personal boosters stack with the event.

5. **After first Ascension** — Player resets and sees the grind ahead. Donor mine access is appealing.
   - *Trigger:* Post-Ascension GUI shows donor mine comparison.

---

## EULA Compliance Notes

Mojang's EULA (as of 2024) allows:
- Cosmetic changes: YES
- Chat tags and prefixes: YES
- Access to servers within your network: YES
- "Ranks" that affect gameplay (survival, non-PvP): GRAY AREA — generally allowed if not PvP-affecting
- Temporary 2× sell multipliers: ALLOWED (confirmed in Mojang's 2014 clarification)
- Crate keys: ALLOWED (random cosmetic-primary loot)

**Consult current Mojang EULA before launch.** Rules change periodically.
Document at: https://www.minecraft.net/en-us/eula and https://www.minecraft.net/en-us/terms

---

*See `TEBEX_PRODUCT_MAP.md` for exact package definitions ready to enter into Tebex.*
*See `DONOR_RANKS.md` for exact perk lists per donor rank.*

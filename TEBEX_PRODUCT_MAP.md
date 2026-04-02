# TEBEX PRODUCT MAP

> **Purpose:** Exact product definitions ready to enter into the Tebex dashboard.
> Each entry includes the package name, description, price, and exact server commands.
> `{player}` is replaced by Tebex with the buyer's Minecraft username.

---

## Setup Requirements

Before creating packages, the Tebex webhook must be configured:
1. Install Tebex plugin on server (or implement webhook in `plugin-tebex`)
2. Set webhook secret in server config
3. Set Tebex store URL in Tebex dashboard
4. Test with a $0 package before going live

See `INSTALLATION_GUIDE.md` for Tebex setup steps.

---

## Category: Donor Ranks

### Devotee (Lifetime)
**Price:** $50.00
**Type:** One-time
**Description:**
> Become a Devotee of the Pharaoh. Gain access to the Sanctum of Amun private mine,
> exclusive chat tag, fly in donor mines, and more. Your title of [Devotee] is permanent.

**Commands on purchase:**
```
prisondonor grant {player} devotee
tellraw {player} ["",{"text":"✦ Welcome, ","color":"aqua"},{"text":"Devotee","bold":true,"color":"aqua"},{"text":"! Your divine rank has been granted.","color":"gray"}]
prisonkit give {player} donor_welcome
```

---

### Devotee (Monthly Subscription)
**Price:** $15.00/month
**Type:** Subscription
**Description:** Same as Lifetime but renews monthly.
**Commands on purchase:** Same as Devotee (Lifetime)
**Commands on expiry:**
```
prisondonor revoke {player} devotee
tellraw {player} ["",{"text":"Your Devotee subscription has expired. Renew at our store!","color":"gray"}]
```

---

### Acolyte (Lifetime)
**Price:** $80.00
**Type:** One-time
**Description:**
> Ascend to Acolyte of the Sacred Order. Unlocks the Sanctum of Hathor, colored nicknames,
> Sand Storm particle trail, expanded home slots, and all Devotee perks.

**Commands on purchase:**
```
prisondonor grant {player} acolyte
tellraw {player} ["",{"text":"✦ ","color":"blue"},{"text":"Acolyte","bold":true,"color":"blue"},{"text":" rank granted! The Order welcomes you.","color":"gray"}]
prisonkit give {player} donor_acolyte_welcome
```

---

### Acolyte (Monthly Subscription)
**Price:** $25.00/month
**Type:** Subscription

---

### High Priest (Lifetime)
**Price:** $120.00
**Type:** One-time
**Description:**
> Rise to High Priest — servant of the gods. Unlocks the Hall of the Pharaoh,
> personal /fly in the hub, /feed and /heal commands, and a 5% sell bonus everywhere.
> Includes all Devotee and Acolyte perks.

**Commands on purchase:**
```
prisondonor grant {player} highpriest
tellraw {player} ["",{"text":"✦ ","color":"light_purple"},{"text":"High Priest","bold":true,"color":"light_purple"},{"text":" rank granted! The gods acknowledge you.","color":"gray"}]
prisonkit give {player} donor_highpriest_welcome
```

---

### High Priest (Monthly Subscription)
**Price:** $45.00/month
**Type:** Subscription

---

### Pharaoh's Chosen (Lifetime)
**Price:** $175.00
**Type:** One-time
**Description:**
> The highest honor — Pharaoh's Chosen. Unlimited access to the Chamber of the Gods,
> /fly everywhere, /god mode in safe zones, 10% sell bonus, the Golden Pharaoh cosmetic set,
> and a daily server-wide announcement of your arrival. Includes all lower donor perks.

**Commands on purchase:**
```
prisondonor grant {player} chosen
broadcast §6§l⚡ The Pharaoh's Chosen, §e{player}§6§l, has joined the divine order!
prisonkit give {player} donor_chosen_welcome
prisoncosmetic unlock {player} golden_pharaoh_set
```

---

### Pharaoh's Chosen (Monthly Subscription)
**Price:** $70.00/month
**Type:** Subscription

---

## Category: Pharaoh's Relics (Crate Keys)

### Canopic Key × 1
**Price:** $3.00
**Description:** A key to open one Canopic Chest. Contains common rewards, Coins, and a chance at Relics.
**Commands:**
```
prisoncratekey give {player} canopic 1
```

### Canopic Key × 5
**Price:** $12.00 (20% savings)
**Commands:**
```
prisoncratekey give {player} canopic 5
```

### Tomb Key × 1
**Price:** $5.00
**Description:** A key to open one Tomb Chest. Contains uncommon rewards, Relics, and rare cosmetics.
**Commands:**
```
prisoncratekey give {player} tomb 1
```

### Tomb Key × 5
**Price:** $22.00 (12% savings)
**Commands:**
```
prisoncratekey give {player} tomb 5
```

### Pharaoh's Reliquary Key × 1
**Price:** $10.00
**Description:** The rarest key. Opens a Pharaoh's Reliquary — contains 200–1,000 Relics, exclusive cosmetics, and powerful enchant scrolls.
**Commands:**
```
prisoncratekey give {player} pharaoh 1
```

### Pharaoh's Reliquary Key × 3
**Price:** $25.00 (17% savings)
**Commands:**
```
prisoncratekey give {player} pharaoh 3
```

---

## Category: Divine Blessings (Boosters)

### Ra's Blessing — Personal (7 Days)
**Price:** $8.00
**Description:** Double your Coins for 7 days. The sun god's personal blessing on all your sells. Stacks with server-wide events.
**Commands:**
```
prisonboost give {player} sell 1.5 7d
```

### Ra's Blessing — Personal (30 Days)
**Price:** $25.00
**Commands:**
```
prisonboost give {player} sell 1.5 30d
```

### Thoth's Wisdom — Personal (7 Days)
**Price:** $8.00
**Description:** Earn 50% more Relics from all sources for 7 days. Accelerate your Khopesh enchanting journey.
**Commands:**
```
prisonboost give {player} relic 1.5 7d
```

### Server Blessing of Ra (2 Hours)
**Price:** $15.00
**Description:** Grants the entire server 2× sell multiplier for 2 hours. Announce to all players when activated — become the hero of the realm.
**Commands:**
```
prisonboost server sell 2.0 2h
broadcast §6§l✦ §e{player}§6 has blessed the realm with Ra's favor! §72× sell for 2 hours!
```

---

## Category: Divine Adornments (Cosmetics)

### Egyptian Tag Bundle
**Price:** $12.00
**Description:** 5 exclusive Egyptian mythology chat tags. Show your devotion to the Pharaoh.
**Commands:**
```
prisoncosmetic unlock {player} tag_ankh
prisoncosmetic unlock {player} tag_eye_of_ra
prisoncosmetic unlock {player} tag_scarab
prisoncosmetic unlock {player} tag_djed
prisoncosmetic unlock {player} tag_lotus
```

### The Anubis Particle Set
**Price:** $10.00
**Description:** Exclusive Anubis-themed particle trail and ambient effects. Walk with the god of death.
**Commands:**
```
prisoncosmetic unlock {player} particle_anubis_trail
prisoncosmetic unlock {player} particle_anubis_ambient
```

### Starter Pack of the Condemned
**Price:** $10.00
**Type:** One-time (can only be purchased once per account)
**Description:** A one-time boost for new players. Receive a bag of Coins and Relics to jumpstart your journey. Only available once.
**Commands:**
```
prisoneco give {player} igc 100000
prisoneco give {player} tokens 500
prisonkit give {player} starter_donor
```

---

## Command Reference

These are the expected Tebex commands based on current plugin structure.
**Verify each command works in-game before going live.**

| Command | Plugin | Status |
|---|---|---|
| `prisondonor grant <player> <rank>` | plugin-donor | Verify exists |
| `prisondonor revoke <player> <rank>` | plugin-donor | Verify exists |
| `prisoncratekey give <player> <type> <amount>` | plugin-crates | Verify exists |
| `prisonboost give <player> <type> <multiplier> <duration>` | plugin-events | **Likely needs implementation** |
| `prisonboost server <type> <multiplier> <duration>` | plugin-events | **Likely needs implementation** |
| `prisoncosmetic unlock <player> <id>` | plugin-cosmetics | Verify exists |
| `prisonkit give <player> <id>` | plugin-kits | Verify exists |
| `prisoneco give <player> <igc|tokens> <amount>` | plugin-economy | Verify exists |

---

## Tebex Setup Checklist

- [ ] Create Tebex store at tebex.io
- [ ] Install/configure `plugin-tebex` webhook handler
- [ ] Enter webhook secret in both Tebex dashboard and server config
- [ ] Create all categories above
- [ ] Create all packages above with exact commands
- [ ] Test each package with a $0 test product
- [ ] Set up subscription renewal/expiry logic
- [ ] Add store link to server MOTD and in-game messages
- [ ] Add `/store` command that opens the Tebex link

---

*See `DONOR_RANKS.md` for full perk lists that these packages grant.*
*See `MONETIZATION_PLAN.md` for pricing rationale and revenue projections.*

# STAFF OPERATIONS

> **Purpose:** Day-to-day operational procedures for server staff.
> Covers: player reports, economy disputes, anticheat review, and common situations.

---

## Report Handling Procedure

### When a Player Reports Another Player

1. **Acknowledge the reporter:** `<gold>Thank you for the report. We're investigating.`

2. **Gather information:**
   - Get the reported player's username
   - Ask for approximate time and what happened
   - Ask for any screenshots or recordings

3. **Investigate:**
   - Use `/spectate <reported_player>` to watch them without being detected
   - Check `/anticheat flags <reported_player>` for automated flags
   - Check chat logs if applicable

4. **Take action:**
   - No evidence: close the report, inform reporter
   - Clear cheating: `/ban <player> Cheating — [description]`
   - Suspicious but unclear: `/freeze <player>` → investigate further
   - Chat offense: `/mute <player> 1h Inappropriate language`

5. **Document the outcome** in your staff chat or Discord.

---

## Economy Dispute Handling

### "I didn't receive my purchase from Tebex"

1. Ask for order ID from Tebex receipt email
2. Check `tebex_queue` table in MySQL:
   ```sql
   SELECT * FROM tebex_queue WHERE player_uuid = 'UUID_HERE';
   ```
3. If queued: player was offline when delivery was attempted — will deliver on next login
4. If not found: check Tebex dashboard for the order status
5. If Tebex shows "Executed" but player didn't receive: manually grant via admin command

### "I lost Coins due to a bug"

1. Check `transaction_log` table:
   ```sql
   SELECT * FROM transaction_log
   WHERE player_uuid = 'UUID_HERE'
   ORDER BY created_at DESC
   LIMIT 20;
   ```
2. Identify the suspicious transaction (type, amount, timestamp)
3. If a bug is confirmed: use `/eco give <player> igc <amount>` to restore
4. Log the refund in `staff_actions` (happens automatically via admin toolkit)

### "I sold items but didn't get paid"

1. Check sell logs (if `transaction_log` tracks sells with SELL_ALL type)
2. Check if player's balance was recently set to 0 by accident
3. Check `staff_actions` for any admin intervention on this player
4. If genuine bug: refund estimated amount and file a bug report

---

## Anticheat Review Procedure

Access the anticheat review queue: `/anticheat review`

### Flag Types and Actions

| Flag Type | Threshold for Action | Action |
|---|---|---|
| Mining too fast | 3+ flags in 1 hour | Spectate + check enchants |
| Selling impossible items | Any | Investigate immediately — possible dupe |
| Balance spike (unusual) | >10× normal rate | Check transaction log |
| Multiple flags within 10 min | 5+ | Temporary freeze + investigate |

### Investigating a Flagged Player
1. Open `/anticheat review` → select the player
2. Note flag type, count, timestamps
3. Cross-reference with `/eco balance <player>` — unusually high? Investigate.
4. Use `/spectate <player>` — watch for speed, range, or impossible actions
5. Clear flags if player is legitimate: `/anticheat clear <player>`
6. Ban if cheating confirmed: `/ban <player> Hacking — [flag details]`

---

## Coinflip Disputes

All coinflip games are logged in `coinflip_logs`:
```sql
SELECT * FROM coinflip_logs
WHERE creator_uuid = 'UUID' OR acceptor_uuid = 'UUID'
ORDER BY resolved_at DESC
LIMIT 10;
```

The log shows: creator, acceptor, winner, amount, and timestamp.
If a player claims they won but didn't receive Coins:
1. Check the log — who is recorded as `winner_uuid`?
2. Check `transaction_log` for a `COINFLIP_WIN` entry
3. If the DB shows they won but their balance doesn't reflect it: this is a bug
4. Refund via `/eco give <player> igc <amount>`

---

## Ban and Punishment Guidelines

### Chat Offenses
| Offense | First | Repeat |
|---|---|---|
| Mild inappropriate language | Verbal warning | 1h mute |
| Hate speech / slurs | Immediate 24h mute | Permanent mute / ban |
| Spamming | 1h mute | 24h mute |
| Advertising other servers | Immediate 7d ban | Permanent ban |

### Gameplay Offenses
| Offense | First | Repeat |
|---|---|---|
| Suspected hacking — minor | 24h ban | 7d ban |
| Confirmed hacking | Permanent ban | — |
| Exploiting duplication bugs | 7d ban + economy rollback | Permanent ban |
| AFK macro / autoclicker | 7d ban | Permanent ban |
| Coinflip/economy scam | 7d ban + coin rollback | Permanent ban |

---

## Daily Staff Checklist

**Each day:**
- [ ] Check anticheat review queue (`/anticheat review`)
- [ ] Check open player reports (your reporting channel)
- [ ] Verify no unusual balance spikes in top players (`/baltop`)
- [ ] Check if any events need to be started manually

**Weekly:**
- [ ] Review `staff_actions` log for any irregular admin economy operations
- [ ] Dynasty War results — announce winner
- [ ] Check `tebex_queue` for any stuck deliveries
- [ ] Back up MySQL database (or verify host auto-backup is working)

---

## Staff Rank Permissions Summary

| Rank | Vanish | Spectate | Mute | Ban | Eco Commands | Anticheat | Full Admin |
|---|---|---|---|---|---|---|---|
| Helper | ✓ | ✓ | ✗ | ✗ | ✗ | View only | ✗ |
| Mod | ✓ | ✓ | ✓ | ✓ | ✗ | Full | ✗ |
| Admin | ✓ | ✓ | ✓ | ✓ | ✓ | Full | Partial |
| Owner | ✓ | ✓ | ✓ | ✓ | ✓ | Full | ✓ |

---

*See `ADMIN_TOOLS.md` for complete command syntax reference.*
*See `PERMISSIONS_MATRIX.md` for exact permission nodes per role.*

#!/usr/bin/env python3
from weasyprint import HTML

html = """<!DOCTYPE html>
<html><head><style>
body{font-family:Helvetica,Arial,sans-serif;margin:40px 50px;color:#1a1a1a;font-size:11px;line-height:1.5}
h1{font-size:28px;margin-bottom:2px}
h2{font-size:18px;border-bottom:2px solid #222;padding-bottom:4px;margin-top:30px}
h3{font-size:13px;margin-top:16px;margin-bottom:4px}
ul{margin:4px 0 8px 18px;padding:0}
li{margin-bottom:2px}
.subtitle{font-size:14px;color:#555;margin-bottom:30px}
.tag{display:inline-block;padding:1px 7px;border-radius:3px;font-size:10px;font-weight:bold;color:#fff;margin-left:6px}
.complete{background:#2d8a4e}
.partial{background:#c77c00}
.needs-work{background:#c0392b}
table{width:100%;border-collapse:collapse;font-size:10px;margin:8px 0}
th,td{border:1px solid #ccc;padding:4px 6px;text-align:left}
th{background:#f0f0f0;font-weight:bold}
.page-break{page-break-before:always}
.title-page{text-align:center;padding-top:180px}
.title-page h1{font-size:36px;border:none}
</style></head><body>

<div class="title-page">
<h1>Custom Prison Server</h1>
<p class="subtitle">Full System &amp; Feature Breakdown</p>
<p style="color:#888">19 Systems &bull; 98 Source Files &bull; March 2026</p>
</div>

<div class="page-break"></div>

<h2>1. High-Level Overview</h2>
<ul>
<li><b>Type:</b> Prison Server (Mine &rarr; Rank &rarr; Prestige loop)</li>
<li><b>Gameplay Loop:</b> Mine blocks &rarr; sell for IGC &rarr; rank up (A&rarr;Z) &rarr; prestige &rarr; earn token bonuses &rarr; upgrade pickaxe &rarr; repeat</li>
<li><b>Progression:</b> 26 mine ranks, unlimited prestige levels, 6 custom pickaxe enchants, gang leveling</li>
<li><b>Monetization:</b> Tebex store &rarr; Donor ranks (4 tiers), crate keys, listing limits</li>
<li><b>Unique Selling Points:</b> Fully custom codebase (no third-party plugins), GUI-based admin toolkit, animated crate openings, player auction house, gang bank &amp; leveling system</li>
</ul>

<h2>2. System Breakdown</h2>

<!-- CORE SYSTEMS -->
<h3>Core: Database <span class="tag complete">COMPLETE</span></h3>
<ul>
<li>HikariCP connection pool (10-20 connections), batch write flushing</li>
<li>22 tables covering all server data (players, transactions, punishments, etc.)</li>
<li>Every other system depends on this</li>
</ul>

<h3>Core: Permissions <span class="tag complete">COMPLETE</span></h3>
<ul>
<li>Three rank systems: Mine (A-Z), Donor (4 tiers), Staff (6 tiers)</li>
<li>In-memory permission cache &mdash; instant checks, async rebuilds</li>
<li>Hierarchical staff inheritance (Owner &rarr; Helper)</li>
</ul>

<h3>Core: Regions <span class="tag complete">COMPLETE</span></h3>
<ul>
<li>Protected zones with flags: PvP, build, entry, mob spawning</li>
<li>Chunk-based spatial indexing for instant lookups</li>
<li>Admin wand tool (golden axe), entry/exit messages</li>
<li>Commands: /rg create, delete, flag, list, info</li>
</ul>

<!-- PROGRESSION SYSTEMS -->
<h3>Economy (Two Currencies) <span class="tag complete">COMPLETE</span></h3>
<ul>
<li><b>IGC ($):</b> Earned by mining/selling blocks. Used for rankups, shops, auctions</li>
<li><b>Tokens (T):</b> Premium currency for pickaxe enchants. Earned via mining + prestige bonuses</li>
<li>Commands: /bal, /tokens, /pay, /baltop, /sell, /sellall, /autosell, /tokenlog</li>
<li>Thread-safe AtomicLong wallets, full transaction logging</li>
</ul>

<h3>Ranks (A&rarr;Z Progression) <span class="tag complete">COMPLETE</span></h3>
<ul>
<li>26 mine ranks with exponential IGC costs (5K &rarr; 500M)</li>
<li>Each rank unlocks a new mine with better ores</li>
<li>GUI showing completed/current/locked ranks color-coded</li>
<li>Auto-teleport to new mine on rankup, server broadcasts</li>
<li>Commands: /rankup, /ranks</li>
</ul>

<h3>Prestige (Endgame Loop) <span class="tag complete">COMPLETE</span></h3>
<ul>
<li>At rank Z: reset to A, wipe IGC, gain permanent +2% token bonus per level</li>
<li>Keeps: tokens, donor rank, pickaxe enchants</li>
<li>Confirmation GUI (shows gains vs losses), tier milestone rewards</li>
<li>Commands: /prestige, /prestigeinfo</li>
</ul>

<!-- MINING SYSTEMS -->
<h3>Mines <span class="tag complete">COMPLETE</span></h3>
<ul>
<li>26 mines (A-Z) with weighted random block compositions</li>
<li>Auto-reset on timer or % mined threshold</li>
<li>Async block building to avoid lag, batched main-thread application</li>
<li>Per-mine sell prices, player eject on reset</li>
<li>Commands: /mine tp, list, info, create, delete, setcomposition, setprice</li>
</ul>

<h3>Pickaxe (Custom Enchants) <span class="tag complete">COMPLETE</span></h3>
<ul>
<li>Special prison pickaxe with 6 custom + 3 vanilla enchants</li>
<li><b>Custom:</b> Explosive (AoE), Laser (line), Sellall (auto), Tokenator (multiplier), Speed (haste), Jackpot (inventory fill)</li>
<li>Upgrade GUI opened by sneak+right-click, token costs scale with prestige</li>
<li>Persistent DB storage, Fortune/Silk Touch conflict resolution</li>
</ul>

<!-- ECONOMY SYSTEMS -->
<h3>Shop (IGC Item Store) <span class="tag complete">COMPLETE</span></h3>
<ul>
<li>Category-based item shop, fully managed in-game by admins</li>
<li>Paginated browsing, confirm purchase dialogs, stock tracking</li>
<li>Items added by holding them + entering price via anvil GUI</li>
<li>Commands: /shop, /shopadmin</li>
</ul>

<h3>Auction House <span class="tag complete">COMPLETE</span></h3>
<ul>
<li>Player-to-player item marketplace with listing fees (5%)</li>
<li>48-hour expiry, material filtering, paginated GUI</li>
<li>Donor rank increases listing limits (3 &rarr; 16)</li>
<li>Offline seller payment &amp; buyer item delivery</li>
<li>Commands: /ah, /ah sell, /ah mylistings, /ah search</li>
</ul>

<h3>Crates (Loot Boxes) <span class="tag complete">COMPLETE</span></h3>
<ul>
<li>3 tiers: Common, Rare, Legendary with weighted reward pools</li>
<li>Animated spinning GUI reveal, broadcast on rare wins</li>
<li>Rewards: IGC, tokens, crate keys, custom items</li>
<li>PDC-tagged keys prevent duplication, offline key delivery</li>
<li>Commands: /crate give, setblock, simulate, list</li>
</ul>

<h3>Kits <span class="tag complete">COMPLETE</span></h3>
<ul>
<li>3 types: Free, Rank-locked, Donor-exclusive</li>
<li>Cooldown-based (hourly/daily) or one-time claim</li>
<li>Donor kits reset on server restart with min-cooldown enforcement</li>
<li>Commands: /kit, /kit list, /kit preview</li>
</ul>

<!-- SOCIAL SYSTEMS -->
<h3>Gangs <span class="tag complete">COMPLETE</span></h3>
<ul>
<li>Create crews with shared bank, roles (Leader/Officer/Member)</li>
<li>Gang levels based on bank balance (9 thresholds up to 12M IGC)</li>
<li>Donor ranks increase member cap (10 &rarr; 20)</li>
<li>Gang chat, leaderboard, invite system with 120s expiry</li>
<li>Commands: /gang create, invite, deposit, withdraw, promote, top, chat</li>
</ul>

<h3>Chat &amp; Tablist <span class="tag complete">COMPLETE</span></h3>
<ul>
<li>Formatted chat with rank prefixes (Prestige + Donor + Mine + Staff)</li>
<li>Custom tablist with header/footer showing online count</li>
<li>Per-rank prefix overrides, MiniMessage formatting</li>
</ul>

<!-- MONETIZATION -->
<h3>Donor Ranks <span class="tag complete">COMPLETE</span></h3>
<ul>
<li>4 tiers: Donor, Donor+, Elite, Elite+ with token multipliers (1.25x&rarr;2.0x)</li>
<li>Perks: auto-sell, donor mines, better kits, more auction slots, bigger gangs</li>
<li>Command: /donorrank</li>
</ul>

<h3>Tebex Integration <span class="tag complete">COMPLETE</span></h3>
<ul>
<li>Processes store purchases: donor ranks and crate keys</li>
<li>Duplicate detection, offline delivery queue, admin audit trail</li>
<li>Commands: /tebexdeliver (console), /tebex history, /tebex pending</li>
</ul>

<!-- ADMIN/STAFF -->
<h3>Admin Toolkit <span class="tag complete">COMPLETE</span></h3>
<ul>
<li>Master GUI panel with 8 sub-tools accessible via admin compass</li>
<li>Mine editor (composition), rank editor (costs/display), economy tools</li>
<li>Player management (ban/mute/kick/freeze/teleport/inventory)</li>
<li>Announcements (chat/title/actionbar/bossbar), server tools (whitelist/maintenance/PvP/restart)</li>
</ul>

<h3>Staff Tools <span class="tag complete">COMPLETE</span></h3>
<ul>
<li>Punishments: /ban, /tempban, /ipban, /mute, /tempmute, /kick, /unban, /unmute</li>
<li>Tools: /freeze, /invsee, /stafftp, /sc (staff chat), /staff (list)</li>
<li>Report system: /report (player), /reports (staff GUI with pagination)</li>
<li>Staff mode: vanish, god mode, flight on join</li>
</ul>

<h3>Anticheat <span class="tag complete">COMPLETE</span></h3>
<ul>
<li>3 detectors: sell rate spam, block break rate, token earn rate</li>
<li>Alerts staff only (no auto-bans), accounts for pickaxe AoE enchants</li>
<li>Commands: /acflags, /acclear</li>
</ul>

<h3>Warps &amp; Navigation <span class="tag complete">COMPLETE</span></h3>
<ul>
<li>Admin-defined warp points with optional permission gates</li>
<li>Mines GUI: 26 mines color-coded by accessibility, click to teleport</li>
<li>Commands: /warp, /warps, /spawn, /mines</li>
</ul>

<div class="page-break"></div>

<h2>3. Feature Matrix</h2>
<table>
<tr><th>Feature</th><th>System</th><th>Done?</th><th>Notes</th></tr>
<tr><td>Block mining</td><td>Mines</td><td>Yes</td><td>26 mines, auto-reset</td></tr>
<tr><td>Block selling</td><td>Economy</td><td>Yes</td><td>/sell, /sellall, auto-sell</td></tr>
<tr><td>Rank progression</td><td>Ranks</td><td>Yes</td><td>A-Z, exponential costs</td></tr>
<tr><td>Prestige reset</td><td>Prestige</td><td>Yes</td><td>Token bonus per level</td></tr>
<tr><td>Custom enchants</td><td>Pickaxe</td><td>Yes</td><td>6 custom + 3 vanilla</td></tr>
<tr><td>Enchant upgrade GUI</td><td>Pickaxe</td><td>Yes</td><td>Token-based upgrades</td></tr>
<tr><td>Two currencies</td><td>Economy</td><td>Yes</td><td>IGC + Tokens</td></tr>
<tr><td>Player trading</td><td>Economy</td><td>Yes</td><td>/pay command</td></tr>
<tr><td>Leaderboard</td><td>Economy</td><td>Yes</td><td>/baltop top 10</td></tr>
<tr><td>Item shop</td><td>Shop</td><td>Yes</td><td>Category-based, in-game admin</td></tr>
<tr><td>Auction house</td><td>AuctionHouse</td><td>Yes</td><td>Fees, expiry, filtering</td></tr>
<tr><td>Loot crates</td><td>Crates</td><td>Yes</td><td>3 tiers, animated opening</td></tr>
<tr><td>Kits</td><td>Kits</td><td>Yes</td><td>Free/Rank/Donor types</td></tr>
<tr><td>Gangs/crews</td><td>Gangs</td><td>Yes</td><td>Bank, levels, roles</td></tr>
<tr><td>Gang chat</td><td>Gangs</td><td>Yes</td><td>/gc, staff spy</td></tr>
<tr><td>Chat formatting</td><td>Chat</td><td>Yes</td><td>Rank prefixes, tablist</td></tr>
<tr><td>Donor ranks</td><td>Donor</td><td>Yes</td><td>4 tiers, token multipliers</td></tr>
<tr><td>Store integration</td><td>Tebex</td><td>Yes</td><td>Auto-delivery, offline queue</td></tr>
<tr><td>Admin GUI panel</td><td>AdminToolkit</td><td>Yes</td><td>8 sub-tools</td></tr>
<tr><td>Mine editor GUI</td><td>AdminToolkit</td><td>Yes</td><td>Composition editing</td></tr>
<tr><td>Rank editor GUI</td><td>AdminToolkit</td><td>Yes</td><td>Cost/display editing</td></tr>
<tr><td>Player management</td><td>AdminToolkit</td><td>Yes</td><td>Ban/mute/kick/freeze GUI</td></tr>
<tr><td>Punishments</td><td>Staff</td><td>Yes</td><td>Ban/mute/kick/ipban</td></tr>
<tr><td>Player reports</td><td>Staff</td><td>Yes</td><td>GUI queue, resolution</td></tr>
<tr><td>Staff mode</td><td>Staff</td><td>Yes</td><td>Vanish, god, flight</td></tr>
<tr><td>Anti-cheat</td><td>Anticheat</td><td>Yes</td><td>3 detectors, staff alerts</td></tr>
<tr><td>Region protection</td><td>Regions</td><td>Yes</td><td>Flags, spatial indexing</td></tr>
<tr><td>Warps</td><td>Warps</td><td>Yes</td><td>Permission-gated</td></tr>
<tr><td>Mine teleport GUI</td><td>Warps</td><td>Yes</td><td>Color-coded by access</td></tr>
<tr><td>Crate editor GUI</td><td>AdminToolkit</td><td>No</td><td>"Coming soon" placeholder</td></tr>
<tr><td>Kit GUI</td><td>Kits</td><td>No</td><td>Text-only, no GUI</td></tr>
<tr><td>Scoreboard/sidebar</td><td>None</td><td>No</td><td>Not implemented</td></tr>
<tr><td>Quests/missions</td><td>None</td><td>No</td><td>Not implemented</td></tr>
<tr><td>Pets/companions</td><td>None</td><td>No</td><td>Not implemented</td></tr>
<tr><td>Cosmetics</td><td>None</td><td>No</td><td>Not implemented</td></tr>
<tr><td>Daily rewards</td><td>None</td><td>No</td><td>Not implemented</td></tr>
<tr><td>Events/bosses</td><td>None</td><td>No</td><td>Not implemented</td></tr>
<tr><td>PvP arena</td><td>None</td><td>No</td><td>Not implemented</td></tr>
<tr><td>Backpacks</td><td>None</td><td>No</td><td>Not implemented</td></tr>
</table>

<div class="page-break"></div>

<h2>4. Gameplay Flow</h2>
<h3>New Player</h3>
<ul>
<li>Joins at spawn, receives starter kit (stone pickaxe, bread, torches)</li>
<li>Warps to Mine A via /mines GUI or /mine tp a</li>
<li>Mines stone/coal, sells with /sell or /sellall</li>
</ul>
<h3>Early Game (Ranks A-H)</h3>
<ul>
<li>Grind IGC to /rankup through early mines</li>
<li>Start earning tokens from mining, begin pickaxe upgrades (Efficiency, Tokenator)</li>
<li>Claim daily kits, join or create a gang</li>
</ul>
<h3>Mid Game (Ranks I-R)</h3>
<ul>
<li>Unlock Explosive/Laser enchants for faster mining</li>
<li>Contribute to gang bank, push gang level</li>
<li>Use auction house to trade valuable items</li>
<li>Open crate keys from store purchases or rewards</li>
</ul>
<h3>Late Game (Ranks S-Z)</h3>
<ul>
<li>Max pickaxe enchants, earn tokens rapidly</li>
<li>Reach rank Z, choose to prestige for +2% permanent token bonus</li>
<li>Repeat A-Z cycle faster each time with better pickaxe</li>
<li>Compete on /baltop and /gang top leaderboards</li>
</ul>

<h2>5. Economy &amp; Progression</h2>
<h3>Currency Flow</h3>
<ul>
<li><b>IGC in:</b> Block selling (primary), auction sales, crate rewards, kits</li>
<li><b>IGC out:</b> Rankups (primary sink), shop purchases, auction fees, gang deposits</li>
<li><b>Tokens in:</b> Mining (base rate * donor * prestige * tokenator), crate rewards</li>
<li><b>Tokens out:</b> Pickaxe enchant upgrades (only sink)</li>
</ul>
<h3>Scaling</h3>
<ul>
<li>Rank costs exponential (5K &rarr; 500M) &mdash; strong IGC sink</li>
<li>Enchant costs scale with prestige level (+10% per prestige)</li>
<li>Higher mines yield better ores worth more IGC</li>
</ul>
<h3>Inflation Risks</h3>
<ul>
<li><b>Moderate:</b> No cap on IGC generation &mdash; max-enchant players at high ranks produce rapidly</li>
<li><b>Low:</b> Tokens are well-balanced (single sink in enchants, scaling costs)</li>
<li><b>Risk:</b> Auto-sell + Explosive + max Fortune at rank Z prints money fast. Prestige wipe helps but doesn't eliminate</li>
</ul>

<div class="page-break"></div>

<h2>6. Missing Features &amp; Gaps</h2>
<h3>Major Missing Systems</h3>
<ul>
<li><b>Scoreboard/Sidebar:</b> No persistent HUD showing rank, balance, tokens, prestige. Expected on every prison server</li>
<li><b>Quest/Mission System:</b> No daily/weekly objectives to guide players. Huge retention opportunity</li>
<li><b>Events System:</b> No scheduled events (2x tokens, mine rush, king of the hill). Key for engagement</li>
<li><b>PvP Arena:</b> No structured PvP despite having PvP flag support in regions</li>
<li><b>Backpacks/Extra Storage:</b> No inventory expansion for miners</li>
</ul>
<h3>Missing Quality-of-Life</h3>
<ul>
<li><b>Daily Login Rewards:</b> No incentive for daily returns</li>
<li><b>Cosmetics:</b> No trails, chat colors, name tags, particle effects</li>
<li><b>Pets/Companions:</b> No mining pets or passive bonuses</li>
<li><b>Achievement System:</b> No milestones or badges</li>
<li><b>Kit GUI:</b> Kits are text-only, should have clickable GUI</li>
<li><b>Crate Editor GUI:</b> Admin toolkit has placeholder, not implemented</li>
</ul>
<h3>Weak Areas</h3>
<ul>
<li><b>Player Retention:</b> No quests, dailies, or events to bring players back</li>
<li><b>Social Features:</b> Gang system is solid but no gang wars, territories, or competitions</li>
<li><b>Monetization Depth:</b> Only donor ranks and crate keys &mdash; could sell cosmetics, pets, backpacks</li>
<li><b>Anti-Cheat:</b> Detection only, no auto-punishment or escalation</li>
<li><b>Appeal System:</b> Bans link to Discord, no in-game appeal flow</li>
</ul>

<h2>7. Priority Recommendations</h2>
<h3>Top 10 Highest-Impact Improvements</h3>
<table>
<tr><th>#</th><th>Item</th><th>Effort</th><th>Impact</th></tr>
<tr><td>1</td><td>Add scoreboard sidebar (rank, balance, tokens, prestige)</td><td>Quick win</td><td>Huge &mdash; expected feature</td></tr>
<tr><td>2</td><td>Build quest/mission system (daily/weekly tasks)</td><td>Major build</td><td>Huge &mdash; retention driver</td></tr>
<tr><td>3</td><td>Add daily login rewards</td><td>Quick win</td><td>High &mdash; brings players back</td></tr>
<tr><td>4</td><td>Create kit selection GUI</td><td>Quick win</td><td>Medium &mdash; better UX</td></tr>
<tr><td>5</td><td>Build events system (2x tokens, mine rush)</td><td>Major build</td><td>High &mdash; engagement spikes</td></tr>
<tr><td>6</td><td>Add cosmetics system (trails, chat colors)</td><td>Medium</td><td>High &mdash; monetization + fun</td></tr>
<tr><td>7</td><td>Implement backpacks for miners</td><td>Medium</td><td>High &mdash; QoL + monetizable</td></tr>
<tr><td>8</td><td>Add gang wars/competitions</td><td>Major build</td><td>High &mdash; social engagement</td></tr>
<tr><td>9</td><td>Finish crate editor GUI in admin toolkit</td><td>Quick win</td><td>Medium &mdash; admin QoL</td></tr>
<tr><td>10</td><td>Add anti-cheat auto-escalation (warn &rarr; mute &rarr; tempban)</td><td>Quick win</td><td>Medium &mdash; reduces staff load</td></tr>
</table>

</body></html>"""

HTML(string=html).write_pdf("/home/user/prison-server/Prison_Server_Breakdown.pdf")
print("Done: Prison_Server_Breakdown.pdf")

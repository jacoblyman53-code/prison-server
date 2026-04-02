# DEPLOYMENT GUIDE

> **Audience:** Server administrators setting up The Pharaoh's Prison for the first time.
> **Prerequisites:** Basic familiarity with Minecraft server hosting.
> **Time required:** 30–60 minutes for a basic setup.

---

## Requirements

| Requirement | Minimum | Recommended |
|---|---|---|
| Java | 21 | 21 (LTS) |
| Paper version | 1.21.x | Latest 1.21.x |
| RAM | 4GB | 6–8GB |
| MySQL | 8.0+ | 8.0+ |
| Storage | 10GB | 20GB+ |

> This server requires **Java 21**. Paper 1.21 will not run on Java 17 or earlier.
> MySQL is required — SQLite is not supported. A hosted MySQL database (e.g., from your
> host's control panel or a free PlanetScale instance) works fine.

---

## Step 1: Prepare the Server

### 1a. Download Paper 1.21
Download the latest Paper 1.21.x JAR from https://papermc.io/downloads/paper

### 1b. Accept the EULA
Run the server once to generate `eula.txt`, then set `eula=true`.

### 1c. Configure `server.properties`
Minimum changes:
```properties
online-mode=true
max-players=100
server-port=25565
spawn-protection=0
```

### 1d. Configure `config/paper-global.yml`
For BungeeCord/proxy use:
```yaml
proxies:
  bungee-cord:
    online-mode: false
```
Leave as default if running as a standalone server.

---

## Step 2: Set Up MySQL

### 2a. Create Database and User
Connect to your MySQL server and run:
```sql
CREATE DATABASE pharaoh_prison CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'pharaoh'@'%' IDENTIFIED BY 'your_secure_password_here';
GRANT ALL PRIVILEGES ON pharaoh_prison.* TO 'pharaoh'@'%';
FLUSH PRIVILEGES;
```

### 2b. Note Your Connection Details
You'll need:
- Host (e.g., `localhost` or your DB server IP)
- Port (default: `3306`)
- Database name: `pharaoh_prison`
- Username: `pharaoh`
- Password: your chosen password

---

## Step 3: Build the Plugins

### 3a. Clone the Repository
```bash
git clone https://github.com/jacoblyman53-code/prison-server.git
cd prison-server/Minecraft\ server
```

### 3b. Build All JARs
```bash
./gradlew shadowJar
```

Output JARs will be in each module's `build/libs/` directory.

### 3c. Using the Deploy Script (Windows)
If you have PowerShell:
```powershell
./deploy.ps1 -All
```
This builds all modules and copies JARs to the output folder.

---

## Step 4: Install Plugins

Copy all built JARs to your server's `plugins/` folder:
```
plugins/
  PrisonDatabase.jar       ← MUST load first (core-database)
  PrisonPermissions.jar    ← MUST load second (core-permissions)
  PrisonRegions.jar        ← core-regions
  PrisonAdminToolkit.jar
  PrisonAnticheat.jar
  PrisonAuctionHouse.jar
  PrisonChat.jar
  PrisonCoinflip.jar
  PrisonCosmetics.jar
  PrisonCrates.jar
  PrisonDonor.jar
  PrisonEconomy.jar
  PrisonEvents.jar
  PrisonGangs.jar
  PrisonKits.jar
  PrisonLeaderboards.jar
  PrisonMenu.jar
  PrisonMines.jar
  PrisonPickaxe.jar
  PrisonPrestige.jar
  PrisonQuests.jar
  PrisonRanks.jar
  PrisonShop.jar
  PrisonStaff.jar
  PrisonTebex.jar
  PrisonWarps.jar
```

> **Load order matters.** Paper loads plugins alphabetically by default, but
> `plugin.yml` `depend:` entries enforce the correct order. If a plugin fails to load,
> check the console for missing dependency errors.

---

## Step 5: Configure the Database Connection

The database config is in `plugins/PrisonDatabase/config.yml` (generated on first run).

Edit it to match your MySQL details:
```yaml
database:
  host: localhost
  port: 3306
  name: pharaoh_prison
  username: pharaoh
  password: your_secure_password_here
  pool-size: 10
  connection-timeout: 30000
```

---

## Step 6: First Boot

### 6a. Start the Server
```bash
java -Xmx4G -Xms1G -jar paper-1.21.x-xxx.jar --nogui
```

### 6b. Watch the Console
Expected startup sequence:
```
[PrisonDatabase] Database module enabled successfully.
[PrisonDatabase] BungeeCord plugin messaging channels registered.
[PrisonPermissions] Permission engine initialized.
[PrisonRegions] Region manager initialized.
... (all other plugins)
[PrisonMines] Mines module enabled. X mines loaded.
[PrisonRanks] Rank system initialized. 26 ranks loaded.
```

If you see `Failed to initialize database` — check your MySQL credentials and ensure
MySQL is running and the `pharaoh_prison` database exists.

### 6c. Verify Tables Were Created
Connect to MySQL and check:
```sql
USE pharaoh_prison;
SHOW TABLES;
```
You should see 20+ tables including `player_wallets`, `player_ranks`, `mine_data`, etc.

---

## Step 7: Configure Mine Regions

All mines default to placeholder coordinates `[0, 64, 0]`. You must set up real regions.

For each mine (A through Z + free + donor mines):
```
/mine setcorner1 A     (stand at one corner, run command)
/mine setcorner2 A     (stand at opposite corner, run command)
/mine setspawn A       (stand at mine entrance, run command)
/mine enable A         (activate the mine)
/mine fill A           (fill the mine with its block composition)
```

See `WORLD_INTEGRATION_GUIDE.md` for detailed mine setup instructions.

---

## Step 8: Grant Yourself Admin Permissions

As the server owner, give yourself the admin permission node:
```
/permissions grant <your_username> prison.admin.*
```

Or if you have console access, run this as a console command without `/`.

---

## Step 9: Basic Configuration Checks

Before opening to players, verify these configs:

### plugin-ranks/config.yml
- Review `ranks:` section — costs look correct for your server size
- Set `rankup-broadcast:` if you want rank-up announcements

### plugin-economy/config.yml
- Review `sell-prices:` — these are the live values
- Adjust `sell-streak-timeout-seconds:` if desired

### plugin-mines/config.yml
- Set `default-donor-session-mins:` (default 30)
- Verify mine permissions match your regions

### plugin-kits/config.yml
- Ensure `starter` kit is configured (auto-delivered to new players)

---

## Step 10: Test the Core Loop

Before opening to players:
1. Log in as a regular player (use an alt or `/gamemode adventure`)
2. Receive the starter kit automatically
3. Warp to the Pit of Souls: `/warp pit`
4. Mine a few blocks
5. Run `/sell all` — verify Coins received
6. Run `/ranks` — verify the GUI opens
7. Rank up to Serf — verify the rank-up fires
8. Check that the Tomb of Aten is now accessible

If all of these work, the core loop is functional.

---

## Common Errors

| Error | Likely Cause | Fix |
|---|---|---|
| `Failed to initialize database` | MySQL not running or wrong credentials | Check MySQL service, verify config.yml credentials |
| `Plugin X failed to load — missing dependency Y` | Plugin Y JAR missing from plugins/ | Verify all JARs are present |
| `Cannot find region for mine A` | Mine corners not set | Run `/mine setcorner1 A` etc. |
| `package com.prison.X does not exist` | Build error — recompile | Run `./gradlew clean shadowJar` |
| Players spawn underground | Spawn point not set | Set spawn with `setworldspawn` command |

---

## SparkHost Specific Notes

If using SparkHost (Sparked Host):
- SFTP remote path: set to `""` (empty string), NOT `/home/container`
- Pterodactyl restart: use the API or web panel restart button
- MySQL: use the internal database feature or a separate managed DB
- Port for Tebex webhook: check if SparkHost allows custom port bindings

---

*See `INSTALLATION_GUIDE.md` for the buyer-friendly simplified version.*
*See `WORLD_INTEGRATION_GUIDE.md` for mine and world setup details.*

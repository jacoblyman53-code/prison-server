# INSTALLATION GUIDE

> **Quick-start for buyers.** Full technical detail is in `DEPLOYMENT_GUIDE.md`.
> This guide gets you from purchase to running server in 10 steps.

---

## What You Need

- **Java 21** — [Download from adoptium.net](https://adoptium.net)
- **Paper 1.21.x** — Download at papermc.io
- **MySQL 8.0+** — From your host's control panel or a free external DB
- A Minecraft server host (SparkHost, BisectHosting, Pebblehost, or any VPS)

---

## 10-Step Quick Start

### Step 1 — Get the Files
```
git clone https://github.com/jacoblyman53-code/prison-server
cd "prison-server/Minecraft server"
```
Or download as a ZIP from the GitHub repository.

### Step 2 — Build the Plugins
**On Windows:**
```powershell
./gradlew shadowJar
```
**On Mac/Linux:**
```bash
chmod +x gradlew && ./gradlew shadowJar
```

All 26 plugin JARs will be output to each module's `build/libs/` folder.

### Step 3 — Set Up Your Server Folder
```
my-server/
  paper-1.21.x.jar
  plugins/
    (paste all built JARs here)
  eula.txt (set eula=true)
```

### Step 4 — Create Your MySQL Database
In your MySQL client:
```sql
CREATE DATABASE pharaoh_prison;
CREATE USER 'pharaoh'@'%' IDENTIFIED BY 'YourPassword123!';
GRANT ALL PRIVILEGES ON pharaoh_prison.* TO 'pharaoh'@'%';
```

### Step 5 — Start the Server Once (Generate Configs)
```bash
java -Xmx4G -jar paper-1.21.x.jar --nogui
```
Let it generate configs and crash on DB connection. That's fine.

### Step 6 — Edit the Database Config
Open `plugins/PrisonDatabase/config.yml`:
```yaml
database:
  host: localhost         # Your MySQL host
  port: 3306
  name: pharaoh_prison
  username: pharaoh
  password: YourPassword123!
```

### Step 7 — Start the Server Again
This time it should start fully. Check the console for:
```
[PrisonDatabase] Database module enabled successfully.
```
If you see this, the connection works. All tables are created automatically.

### Step 8 — Grant Yourself Admin
In the server console (not in-game):
```
permissions grant YourUsername prison.admin.*
```

### Step 9 — Set Up Your World and Mines
- Build or import an Egyptian-themed spawn and mine layout
- Use `/mine setcorner1 A`, `/mine setcorner2 A`, `/mine setspawn A`, `/mine enable A`
  for each of the 26 standard mines
- See `WORLD_INTEGRATION_GUIDE.md` for the full mine setup walkthrough

### Step 10 — Test It
Log in with a regular Minecraft account and:
1. Verify you get a starter kit
2. Mine and `/sell all` — verify Coins received
3. Open `/ranks` and rank up
4. Check the main menu opens (`/menu` or by right-clicking the compass if configured)

You're live. Welcome to The Pharaoh's Prison.

---

## Connecting Tebex (Optional)

To accept real-money donations:
1. Create a store at tebex.io
2. In your Tebex dashboard → Game Servers → Add Server
3. Get your secret key
4. Edit `plugins/PrisonTebex/config.yml`:
   ```yaml
   secret-key: "your-tebex-secret-key"
   webhook-port: 4567
   ```
5. Restart the server
6. Create your packages using the commands in `TEBEX_PRODUCT_MAP.md`

Note: Your server host must allow inbound connections on the webhook port.
Check with your host if you have issues with the webhook not arriving.

---

## If Something Goes Wrong

**Plugin fails to load:**
```
[ERROR] Could not load plugin X — missing dependency Y
```
Fix: Make sure all 26 JAR files are in the `plugins/` folder.

**Database connection fails:**
```
[SEVERE] Failed to initialize database
```
Fix: Check MySQL credentials in `plugins/PrisonDatabase/config.yml`.

**Players spawn underground:**
Set the world spawn point: `/setworldspawn` at the correct location.

**Mine shows placeholder blocks:**
Mine corners haven't been set. Run the `/mine setcorner1/2` commands.

For detailed troubleshooting, see `DEPLOYMENT_GUIDE.md`.

---

*Full technical documentation: `DEPLOYMENT_GUIDE.md`*
*Mine and world setup: `WORLD_INTEGRATION_GUIDE.md`*
*All config keys explained: `CONFIG_GUIDE.md`*

# QUICK START

> From purchase to running server in under an hour.
> Assumes basic familiarity with Minecraft server hosting.
> For detailed steps, see `INSTALLATION_GUIDE.md`.

---

## Prerequisites Checklist

Before you begin, confirm you have:
- [ ] Java 21 installed (`java -version` should show `21.x.x`)
- [ ] Paper 1.21.x JAR downloaded from papermc.io
- [ ] MySQL 8.0+ accessible (your host's panel or external service)
- [ ] Git installed (or the ZIP downloaded from GitHub)

---

## 10 Steps

### 1. Get the code
```bash
git clone https://github.com/jacoblyman53-code/prison-server
```

### 2. Build all plugins
```bash
cd "prison-server/Minecraft server"
./gradlew shadowJar
```
Wait for `BUILD SUCCESSFUL`. All 26 JARs are built.

### 3. Set up your server folder
```
my-server/
  paper-1.21.x.jar
  plugins/
    (copy all JARs from each module's build/libs/ folder)
  eula.txt  (set eula=true)
```

### 4. Create MySQL database
```sql
CREATE DATABASE pharaoh_prison;
CREATE USER 'pharaoh'@'%' IDENTIFIED BY 'CHANGE_ME_123';
GRANT ALL PRIVILEGES ON pharaoh_prison.* TO 'pharaoh'@'%';
```

### 5. First boot (generates config files)
```bash
java -Xmx4G -jar paper-1.21.x.jar --nogui
```
It may fail on DB connect — that's OK. Stop the server.

### 6. Configure the database
Edit `plugins/PrisonDatabase/config.yml`:
```yaml
database:
  host: localhost
  port: 3306
  name: pharaoh_prison
  username: pharaoh
  password: CHANGE_ME_123
```

### 7. Start the server again
This time it should start fully. Look for:
```
[PrisonDatabase] Database module enabled successfully.
```

### 8. Grant yourself admin
In the server console:
```
permissions grant YourMinecraftName prison.admin.*
```

### 9. Set up mines
For each of the 26 mines, you need to define their location in your world:
```
/mine setcorner1 A
/mine setcorner2 A
/mine setspawn A
/mine enable A
/mine fill A
```
Repeat for mines B through Z (and donor mines).
See `WORLD_INTEGRATION_GUIDE.md` for a complete mine setup walkthrough.

### 10. Test the core loop
Log in with a player account:
- Receive starter kit automatically
- Go to `/warp pit` (Pit of Souls starter mine)
- Mine some blocks
- `/sell all` — verify Coins received
- `/ranks` — verify GUI opens
- Click to rank up to Serf

If all that works, your server is running.

---

## Next Steps

After basic setup is working:

1. **Build your world** — See `WORLD_INTEGRATION_GUIDE.md`
2. **Tune the economy** — See `ECONOMY_BALANCE_TODO.md` for recommended adjustments
3. **Apply the Egyptian theme** — Apply `PRODUCT_IDENTITY.md` naming to configs
4. **Set up Tebex** — See `TEBEX_PRODUCT_MAP.md`
5. **Configure staff** — See `PERMISSIONS_MATRIX.md` for staff role setup
6. **Review all configs** — See `CONFIG_GUIDE.md` for every available setting

---

## Need Help?

- Detailed setup: `DEPLOYMENT_GUIDE.md`
- Config reference: `CONFIG_GUIDE.md`
- Something broken: `TECHNICAL_DEBT_LOG.md` (known issues)
- Common errors: `DEPLOYMENT_GUIDE.md` → "Common Errors" section

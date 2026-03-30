package com.prison.pickaxe;

import com.prison.economy.EconomyAPI;
import com.prison.economy.TransactionType;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import java.util.HashSet;

public class PickaxePlugin extends JavaPlugin implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private PickaxeManager manager;
    private PickaxeConfig  pcfg;
    private UpgradeGUI     upgradeGUI;

    // Per-player GUI session state
    // key = player UUID, value = current state
    private final ConcurrentHashMap<UUID, GUISession> sessions = new ConcurrentHashMap<>();
    private final Random random = new Random();

    private static class GUISession {
        UpgradeGUI.Tab tab = UpgradeGUI.Tab.CUSTOM;
        // When a confirm dialog is open, these hold the pending purchase:
        String pendingEnchantId = null;
        int    pendingFromLevel = 0;
        long   pendingCost     = 0;
        boolean confirmOpen    = false;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        pcfg = new PickaxeConfig(getConfig());

        manager    = PickaxeManager.initialize(this, pcfg, getLogger());
        new PickaxeAPI();

        upgradeGUI = new UpgradeGUI(pcfg);
        getServer().getPluginManager().registerEvents(this, this);

        // DB table
        ensureTable();

        getLogger().info("[Pickaxe] Plugin enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("[Pickaxe] Plugin disabled.");
    }

    // ----------------------------------------------------------------
    // DB Table
    // ----------------------------------------------------------------

    private void ensureTable() {
        try {
            com.prison.database.DatabaseManager.getInstance().execute(
                "CREATE TABLE IF NOT EXISTS pickaxes (" +
                "  uuid       VARCHAR(36) NOT NULL PRIMARY KEY," +
                "  owner_uuid VARCHAR(36) NOT NULL," +
                "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );
            com.prison.database.DatabaseManager.getInstance().execute(
                "CREATE TABLE IF NOT EXISTS pickaxe_enchants (" +
                "  pickaxe_uuid VARCHAR(36) NOT NULL," +
                "  enchant_id   VARCHAR(32) NOT NULL," +
                "  level        INT NOT NULL DEFAULT 0," +
                "  PRIMARY KEY (pickaxe_uuid, enchant_id)," +
                "  FOREIGN KEY (pickaxe_uuid) REFERENCES pickaxes(uuid) ON DELETE CASCADE" +
                ")"
            );
        } catch (Exception e) {
            getLogger().severe("[Pickaxe] Failed to create tables: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------------
    // Commands
    // ----------------------------------------------------------------

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("In-game only.");
            return true;
        }

        switch (cmd.getName().toLowerCase()) {
            case "enchant" -> {
                ItemStack held = getHeldPickaxe(player);
                if (held == null) {
                    player.sendMessage(MM.deserialize("<red>Hold your prison pickaxe to open the enchant menu."));
                    return true;
                }
                GUISession session = sessions.computeIfAbsent(player.getUniqueId(), k -> new GUISession());
                session.confirmOpen = false;
                session.pendingEnchantId = null;
                upgradeGUI.open(player, session.tab, held, getPrestige(player));
            }
            case "pickaxe" -> {
                if (args.length == 0) {
                    player.sendMessage(MM.deserialize("<red>Usage: /pickaxe [give|reload]"));
                    return true;
                }
                switch (args[0].toLowerCase()) {
                    case "give" -> {
                        if (!player.hasPermission("prison.admin.*")) {
                            player.sendMessage(MM.deserialize("<red>No permission."));
                            return true;
                        }
                        Player target = args.length >= 2 ? Bukkit.getPlayer(args[1]) : player;
                        if (target == null) {
                            player.sendMessage(MM.deserialize("<red>Player not found."));
                            return true;
                        }
                        manager.issuePickaxe(target);
                        player.sendMessage(MM.deserialize("<green>Pickaxe given to <white>" + target.getName()));
                    }
                    case "reload" -> {
                        if (!player.hasPermission("prison.admin.*")) {
                            player.sendMessage(MM.deserialize("<red>No permission."));
                            return true;
                        }
                        reloadConfig();
                        pcfg = new PickaxeConfig(getConfig());
                        upgradeGUI = new UpgradeGUI(pcfg);
                        player.sendMessage(MM.deserialize("<green>Pickaxe config reloaded."));
                    }
                    default -> player.sendMessage(MM.deserialize("<red>Unknown subcommand."));
                }
            }
        }
        return true;
    }

    // ----------------------------------------------------------------
    // Block break — LOW priority: block non-server pickaxes in mines
    // ----------------------------------------------------------------

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockBreakLow(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        // Only check in mine areas
        if (!isInMine(event.getBlock())) return;

        if (!manager.isServerPickaxe(item)) {
            event.setCancelled(true);
            player.sendMessage(MM.deserialize("<red>You need a prison pickaxe to mine here."));
        }
    }

    // ----------------------------------------------------------------
    // Block break — MONITOR priority: apply enchant effects + earn tokens
    // ----------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreakMonitor(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!manager.isServerPickaxe(item)) return;
        if (!isInMine(event.getBlock())) return;

        Block broken = event.getBlock();
        Material material = broken.getType();

        // --- Token earn ---
        awardTokens(player, item, material);

        // --- Jackpot ---
        double jackpotChance = manager.getJackpotChance(item);
        if (jackpotChance > 0.0 && Math.random() * 100 < jackpotChance) {
            triggerJackpot(player, broken);
        }

        // --- Explosive ---
        int explosive = manager.getEnchantLevel(item, "explosive");
        if (explosive > 0) {
            triggerExplosive(player, item, broken, explosive);
        }

        // --- Laser ---
        int laser = manager.getEnchantLevel(item, "laser");
        if (laser > 0) {
            triggerLaser(player, item, broken, laser);
        }

        // --- Nuke ---
        int nuke = manager.getEnchantLevel(item, "nuke");
        if (nuke > 0) {
            double nukeChance = pcfg.getNukeChances().getOrDefault(nuke, 0.05);
            if (random.nextDouble() < nukeChance) {
                triggerNuke(player, item, broken, nuke);
            }
        }

        // --- Tunnel ---
        int tunnel = manager.getEnchantLevel(item, "tunnel");
        if (tunnel > 0) {
            triggerTunnel(player, item, broken, tunnel);
        }

        // --- Lightning ---
        int lightning = manager.getEnchantLevel(item, "lightning");
        if (lightning > 0) {
            double lightningChance = pcfg.getLightningChances().getOrDefault(lightning, 0.15);
            if (random.nextDouble() < lightningChance) {
                triggerLightning(player, item, broken, lightning);
            }
        }

        // --- Sellall threshold check ---
        int sellallThreshold = manager.getSellallThreshold(item);
        if (sellallThreshold > 0) {
            checkSellallThreshold(player, sellallThreshold);
        }
    }

    // ----------------------------------------------------------------
    // PlayerInteractEvent — sneak + right-click opens upgrade GUI
    // ----------------------------------------------------------------

    @EventHandler
    public void onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR
            && event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (!player.isSneaking()) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!manager.isServerPickaxe(item)) return;

        event.setCancelled(true);

        GUISession session = sessions.computeIfAbsent(player.getUniqueId(), k -> new GUISession());
        session.confirmOpen = false;
        session.pendingEnchantId = null;

        int prestige = getPrestige(player);
        upgradeGUI.open(player, session.tab, item, prestige);
    }

    // ----------------------------------------------------------------
    // InventoryClickEvent — route clicks in upgrade / confirm GUI
    // ----------------------------------------------------------------

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        var title = event.getView().title();

        boolean isUpgradeGUI = title.equals(MM.deserialize(pcfg.getUpgradeTitle()));
        boolean isConfirmGUI = title.equals(MM.deserialize(pcfg.getConfirmTitle()));

        if (!isUpgradeGUI && !isConfirmGUI) return;
        event.setCancelled(true);

        int slot = event.getRawSlot();
        GUISession session = sessions.get(player.getUniqueId());
        if (session == null) return;

        if (isUpgradeGUI) {
            handleUpgradeClick(player, slot, session);
        } else {
            handleConfirmClick(player, slot, session);
        }
    }

    private void handleUpgradeClick(Player player, int slot, GUISession session) {
        // Tab switch
        if (upgradeGUI.isTabSlot(slot)) {
            session.tab = upgradeGUI.getTabAtSlot(slot);
            ItemStack item = getHeldPickaxe(player);
            if (item == null) { player.closeInventory(); return; }
            upgradeGUI.open(player, session.tab, item, getPrestige(player));
            return;
        }

        // Enchant slot clicked
        String enchantId = upgradeGUI.getEnchantAtSlot(slot, session.tab);
        if (enchantId == null) return;

        ItemStack item = getHeldPickaxe(player);
        if (item == null) { player.closeInventory(); return; }

        EnchantDef def = pcfg.getEnchant(enchantId);
        if (def == null) return;

        int currentLevel = manager.getEnchantLevel(item, enchantId);
        if (currentLevel >= def.maxLevel()) {
            player.sendMessage(MM.deserialize("<red>That enchant is already maxed out."));
            return;
        }

        // Check vanilla conflict (Fortune ↔ Silk Touch)
        if (enchantId.equals("fortune") && manager.getEnchantLevel(item, "silk_touch") > 0) {
            player.sendMessage(MM.deserialize("<red>Fortune conflicts with Silk Touch."));
            return;
        }
        if (enchantId.equals("silk_touch") && manager.getEnchantLevel(item, "fortune") > 0) {
            player.sendMessage(MM.deserialize("<red>Silk Touch conflicts with Fortune."));
            return;
        }

        long cost = pcfg.scaledCost(def, currentLevel, getPrestige(player));
        long tokens = EconomyAPI.getInstance().getTokens(player.getUniqueId());

        // Open confirm dialog
        session.pendingEnchantId = enchantId;
        session.pendingFromLevel = currentLevel;
        session.pendingCost      = cost;
        session.confirmOpen      = true;

        upgradeGUI.openConfirm(player, enchantId, currentLevel, cost, tokens);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
    }

    private void handleConfirmClick(Player player, int slot, GUISession session) {
        if (upgradeGUI.isCancelSlot(slot)) {
            session.confirmOpen = false;
            // Return to upgrade GUI
            ItemStack item = getHeldPickaxe(player);
            if (item == null) { player.closeInventory(); return; }
            upgradeGUI.open(player, session.tab, item, getPrestige(player));
            return;
        }

        if (!upgradeGUI.isConfirmSlot(slot)) return;
        if (session.pendingEnchantId == null) return;

        // Attempt purchase
        long result = EconomyAPI.getInstance().deductTokens(
            player.getUniqueId(), session.pendingCost, TransactionType.ENCHANT_PURCHASE);

        if (result < 0) {
            player.sendMessage(MM.deserialize("<red>Not enough tokens!"));
            session.confirmOpen = false;
            player.closeInventory();
            return;
        }

        // Apply enchant
        ItemStack item = getHeldPickaxe(player);
        if (item == null) {
            // Refund
            EconomyAPI.getInstance().addTokens(player.getUniqueId(), session.pendingCost, TransactionType.ENCHANT_PURCHASE);
            player.sendMessage(MM.deserialize("<red>Could not find your pickaxe. Purchase refunded."));
            session.confirmOpen = false;
            player.closeInventory();
            return;
        }

        int newLevel = session.pendingFromLevel + 1;
        manager.setEnchantLevel(player, item, session.pendingEnchantId, newLevel);

        EnchantDef def = pcfg.getEnchant(session.pendingEnchantId);
        player.sendMessage(MM.deserialize(
            "<green>Upgraded <aqua>" + (def != null ? def.display() : session.pendingEnchantId)
            + " <green>to level <white>" + newLevel
            + "<green>! Cost: <gold>" + session.pendingCost + " tokens"
        ));
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.6f, 1.4f);

        // Apply Speed Mine haste immediately if that was the enchant
        if (session.pendingEnchantId.equals("speed")) {
            applyHaste(player, item);
        }

        session.pendingEnchantId = null;
        session.confirmOpen = false;

        // Refresh GUI
        upgradeGUI.open(player, session.tab, item, getPrestige(player));
    }

    // ----------------------------------------------------------------
    // InventoryCloseEvent — clean up session
    // ----------------------------------------------------------------

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        GUISession session = sessions.get(player.getUniqueId());
        if (session != null && !session.confirmOpen) {
            sessions.remove(player.getUniqueId());
        }
    }

    // ----------------------------------------------------------------
    // PlayerMoveEvent — close GUI if player moves
    // ----------------------------------------------------------------

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!event.hasChangedPosition()) return;
        Player player = event.getPlayer();
        if (sessions.containsKey(player.getUniqueId())) {
            player.closeInventory();
            sessions.remove(player.getUniqueId());
        }
    }

    // ----------------------------------------------------------------
    // PlayerToggleSneakEvent — close GUI if player stops sneaking
    // ----------------------------------------------------------------

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        if (event.isSneaking()) return; // just started sneaking
        Player player = event.getPlayer();
        if (sessions.containsKey(player.getUniqueId())) {
            player.closeInventory();
            sessions.remove(player.getUniqueId());
        }
    }

    // ----------------------------------------------------------------
    // PlayerJoinEvent — sync pickaxe from DB; auto-issue on first join
    // ----------------------------------------------------------------

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Check if the player already has a server pickaxe in their inventory
        boolean hasPickaxe = false;
        for (ItemStack it : player.getInventory().getContents()) {
            if (manager.isServerPickaxe(it)) { hasPickaxe = true; break; }
        }

        if (hasPickaxe) {
            manager.syncFromDatabase(player);
        } else {
            // If they have no pickaxe, check whether they've ever been issued one
            manager.hasPickaxeRecord(player.getUniqueId()).thenAccept(hasRecord -> {
                if (!hasRecord) {
                    // First join — issue a prison pickaxe
                    manager.issuePickaxe(player).thenRun(() ->
                        getServer().getScheduler().runTask(this, () ->
                            player.sendMessage(MM.deserialize(
                                "<green>You've received your <gold>Prison Pickaxe</gold>! " +
                                "Sneak + right-click or use <white>/enchant</white> to upgrade it."))
                        )
                    );
                }
                // If hasRecord but no pickaxe in inventory: they lost it — admin can re-issue with /pickaxe give
            });
        }
    }

    // ----------------------------------------------------------------
    // PlayerItemHeldEvent — apply/remove Speed Mine haste
    // ----------------------------------------------------------------

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        // Schedule 1 tick later so the slot switch has happened
        getServer().getScheduler().runTaskLater(this, () -> {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (manager.isServerPickaxe(item)) {
                applyHaste(player, item);
            } else {
                // Remove haste if it was from Speed Mine (check if still has a pickaxe)
                removePickaxeHaste(player);
            }
        }, 1L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        sessions.remove(event.getPlayer().getUniqueId());
    }

    // ----------------------------------------------------------------
    // Enchant effect implementations
    // ----------------------------------------------------------------

    private void awardTokens(Player player, ItemStack item, Material material) {
        long base = pcfg.getBaseTokensPerBlock();
        if (base <= 0) return;

        // Donor multiplier
        double donorMult = getDonorMultiplier(player);

        // Prestige multiplier: prestige 0 = 1.0x, prestige N = 1.0 + N * 0.05
        double prestigeMult = 1.0 + (getPrestige(player) * 0.05);

        // Tokenator multiplier
        double tokenatorMult = manager.getTokenatorMultiplier(item);

        // Fortune multiplier (vanilla: increases drops)
        int fortune = manager.getEnchantLevel(item, "fortune");
        double fortuneMult = 1.0 + (fortune * 0.25); // rough bonus: +25% per level

        long tokens = Math.max(1L, Math.round(base * donorMult * prestigeMult * tokenatorMult * fortuneMult));
        EconomyAPI.getInstance().addTokens(player.getUniqueId(), tokens, TransactionType.TOKEN_EARN);
    }

    private void triggerExplosive(Player player, ItemStack item, Block center, int level) {
        int radius = level; // radius 1=3x3, 2=5x5, etc.
        World world = center.getWorld();
        String worldName = world.getName();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    Block b = center.getRelative(dx, dy, dz);
                    if (b.getType().isAir()) continue;
                    if (!isInMine(b)) continue;
                    if (!player.hasPermission("prison.mine.*") && !isBlockBreakable(player, b)) continue;

                    // Award tokens for this block too
                    awardTokens(player, item, b.getType());

                    b.setType(Material.AIR, false);
                }
            }
        }
    }

    private void triggerLaser(Player player, ItemStack item, Block start, int level) {
        int length = level * 2 + 1; // level 1=3, 2=5, etc.
        BlockFace face = getPlayerFacingFace(player);

        Block current = start.getRelative(face); // start from the next block
        for (int i = 0; i < length - 1; i++) {
            if (current.getType().isAir()) break;
            if (!isInMine(current)) break;

            awardTokens(player, item, current.getType());
            current.setType(Material.AIR, false);
            current = current.getRelative(face);
        }
    }

    private void triggerNuke(Player player, ItemStack item, Block center, int level) {
        int[] radii = {3, 4, 5};
        int radius = radii[Math.min(level - 1, 2)];
        World world = center.getWorld();
        int r2 = radius * radius;
        int count = 0;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz > r2) continue;
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    Block b = center.getRelative(dx, dy, dz);
                    if (b.getType().isAir() || !isInMine(b)) continue;
                    awardTokens(player, item, b.getType());
                    b.setType(Material.AIR, false);
                    count++;
                }
            }
        }

        if (count > 0) {
            world.spawnParticle(Particle.LARGE_SMOKE,
                center.getLocation().add(0.5, 0.5, 0.5),
                40, radius * 0.4, radius * 0.4, radius * 0.4, 0.05);
            world.playSound(center.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 0.9f);
            player.sendActionBar(MM.deserialize(
                "<red>☄ <bold>NUKE!</bold> <dark_gray>[+" + count + " blocks]"));
        }
    }

    private void triggerTunnel(Player player, ItemStack item, Block start, int level) {
        int depth = level * 2 + 1; // 3 / 5 / 7 blocks deep
        BlockFace face = getPlayerFacingFace(player);
        Block current = start.getRelative(face);
        for (int d = 0; d < depth - 1; d++) {
            for (int dy = 0; dy <= 1; dy++) { // 2 blocks tall (feet + head height)
                Block b = current.getRelative(0, dy, 0);
                if (!b.getType().isAir() && isInMine(b)) {
                    awardTokens(player, item, b.getType());
                    b.setType(Material.AIR, false);
                }
            }
            current = current.getRelative(face);
        }
    }

    private void triggerLightning(Player player, ItemStack item, Block center, int level) {
        int chainCount = pcfg.getLightningChains().getOrDefault(level, 5);
        World world = center.getWorld();
        int[][] dirs = {{-1,0,0},{1,0,0},{0,-1,0},{0,1,0},{0,0,-1},{0,0,1}};

        Deque<Block> queue = new ArrayDeque<>();
        Set<Block> visited = new HashSet<>();
        queue.add(center);
        visited.add(center);
        int broken = 0;

        while (!queue.isEmpty() && broken < chainCount) {
            Block curr = queue.poll();
            List<Block> candidates = new ArrayList<>();
            for (int[] d : dirs) {
                Block adj = curr.getRelative(d[0], d[1], d[2]);
                if (!visited.contains(adj) && !adj.getType().isAir() && isInMine(adj)) {
                    candidates.add(adj);
                }
            }
            if (candidates.isEmpty()) break;
            Block next = candidates.get(random.nextInt(candidates.size()));
            visited.add(next);
            awardTokens(player, item, next.getType());
            next.setType(Material.AIR, false);
            broken++;
            queue.add(next);
        }

        if (broken > 0) {
            world.spawnParticle(Particle.END_ROD,
                center.getLocation().add(0.5, 0.5, 0.5),
                25, 1.0, 1.0, 1.0, 0.08);
            world.playSound(center.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.3f, 1.8f);
            player.sendActionBar(MM.deserialize(
                "<aqua>⚡ <bold>Lightning!</bold> <dark_gray>[+" + broken + " chains]"));
        }
    }

    private void triggerJackpot(Player player, Block near) {
        // Find which mine we're in and fill inventory with random mine blocks
        if (!isInMine(near)) return;

        try {
            com.prison.mines.MinesAPI minesApi = com.prison.mines.MinesAPI.getInstance();
            if (minesApi == null) return;

            Location loc = near.getLocation();
            com.prison.mines.MineData mine = minesApi.getMineAt(loc);
            if (mine == null) return;

            Map<org.bukkit.Material, Double> comp = mine.composition();
            if (comp.isEmpty()) return;

            // Pick a random material weighted by composition
            List<org.bukkit.Material> mats = new ArrayList<>(comp.keySet());

            // Fill empty inventory slots with mine blocks
            int filled = 0;
            for (int i = 0; i < player.getInventory().getSize(); i++) {
                ItemStack existing = player.getInventory().getItem(i);
                if (existing == null || existing.getType().isAir()) {
                    org.bukkit.Material mat = mats.get(new Random().nextInt(mats.size()));
                    player.getInventory().setItem(i, new ItemStack(mat, 64));
                    filled++;
                }
            }

            player.sendActionBar(MM.deserialize(
                "<gold>★ <bold>JACKPOT!</bold> <yellow>+" + filled + " slots filled!"));
            player.sendMessage(MM.deserialize("<gold><bold>JACKPOT! </bold><yellow>Your inventory has been filled with mine blocks!"));
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 0.8f);
            near.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING,
                near.getLocation().add(0.5, 1.0, 0.5), 60, 1.0, 1.5, 1.0, 0.15);
        } catch (NoClassDefFoundError ignored) {
            // PrisonMines not loaded
        }
    }

    private void checkSellallThreshold(Player player, int thresholdPercent) {
        int total = player.getInventory().getSize();
        int filled = 0;
        for (ItemStack it : player.getInventory().getContents()) {
            if (it != null && !it.getType().isAir()) filled++;
        }
        int percentFull = (int) ((filled / (double) total) * 100);
        if (percentFull >= thresholdPercent) {
            // Trigger auto-sell same as if they typed /sellall
            // We trigger via EconomyPlugin's auto-sell flag check — just fire the same logic
            // For simplicity, delegate to EconomyAPI's auto-sell path:
            // EconomyPlugin handles BlockDropItemEvent — here we just set their sell time to 0
            // so the next block drop auto-sells. Actually, we should sell now.
            sellInventory(player);
        }
    }

    private void sellInventory(Player player) {
        long totalEarned = 0;
        for (ItemStack it : player.getInventory().getContents()) {
            if (it == null || it.getType().isAir()) continue;
            long price = EconomyAPI.getInstance().getSellPrice(it.getType(), player);
            if (price <= 0) continue;
            totalEarned += price * it.getAmount();
            player.getInventory().remove(it);
        }
        if (totalEarned > 0) {
            EconomyAPI.getInstance().addBalance(player.getUniqueId(), totalEarned, TransactionType.MINE_SELL);
            player.sendMessage(MM.deserialize(
                "<green>Auto Sell enchant: sold for <gold>$" + totalEarned));
        }
    }

    // ----------------------------------------------------------------
    // Speed Mine haste
    // ----------------------------------------------------------------

    private void applyHaste(Player player, ItemStack item) {
        int level = manager.getEnchantLevel(item, "speed");
        if (level <= 0) {
            removePickaxeHaste(player);
            return;
        }
        // Duration: effectively permanent while holding (re-applied on hold events)
        // Use a long duration so it doesn't flicker
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.HASTE,
            Integer.MAX_VALUE,
            level - 1,  // amplifier 0 = Haste I, 1 = Haste II, etc.
            true, false, false  // ambient, no particles for clean look, no icon
        ));
    }

    private void removePickaxeHaste(Player player) {
        // Only remove if we gave it (check it was ambient)
        PotionEffect effect = player.getPotionEffect(PotionEffectType.HASTE);
        if (effect != null && effect.isAmbient()) {
            player.removePotionEffect(PotionEffectType.HASTE);
        }
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private boolean isInMine(Block block) {
        try {
            com.prison.mines.MinesAPI api = com.prison.mines.MinesAPI.getInstance();
            if (api == null) return false;
            return api.getMineAt(block.getLocation()) != null;
        } catch (NoClassDefFoundError e) {
            return false;
        }
    }

    private ItemStack getHeldPickaxe(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        if (manager.isServerPickaxe(main)) return main;
        // Check off-hand
        ItemStack off = player.getInventory().getItemInOffHand();
        if (manager.isServerPickaxe(off)) return off;
        return null;
    }

    private int getPrestige(Player player) {
        try {
            com.prison.prestige.PrestigeManager pm = com.prison.prestige.PrestigeManager.getInstance();
            return pm != null ? pm.getPrestigeLevel(player.getUniqueId()) : 0;
        } catch (NoClassDefFoundError e) {
            return 0;
        }
    }

    private double getDonorMultiplier(Player player) {
        try {
            com.prison.donor.DonorAPI api = com.prison.donor.DonorAPI.getInstance();
            return api != null ? api.getTokenMultiplier(player.getUniqueId()) : 1.0;
        } catch (NoClassDefFoundError e) {
            return 1.0;
        }
    }

    private BlockFace getPlayerFacingFace(Player player) {
        float yaw = player.getLocation().getYaw();
        yaw = ((yaw % 360) + 360) % 360;
        if (yaw < 45 || yaw >= 315) return BlockFace.SOUTH;
        if (yaw < 135) return BlockFace.WEST;
        if (yaw < 225) return BlockFace.NORTH;
        return BlockFace.EAST;
    }

    private boolean isBlockBreakable(Player player, Block block) {
        // Mines check is already done — this is a safety guard
        return true;
    }
}

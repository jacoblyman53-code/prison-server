package com.prison.gangs;

import com.prison.permissions.PermissionEngine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GangCommand — handles all /gang subcommands and /gc.
 */
public class GangCommand implements CommandExecutor {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final GangPlugin plugin;
    private final GangManager manager;

    /** UUID → timestamp of when /gang disband was first typed. 30s window to confirm. */
    private final Map<UUID, Long> pendingDisband = new ConcurrentHashMap<>();

    public GangCommand(GangPlugin plugin, GangManager manager) {
        this.plugin  = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command must be run by a player.");
            return true;
        }

        // /gc <message...>
        if (command.getName().equalsIgnoreCase("gc")) {
            if (args.length == 0) {
                player.sendMessage(MM.deserialize("<red>Usage: /gc <message>"));
                return true;
            }
            handleGangChat(player, String.join(" ", args));
            return true;
        }

        // /gang
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create"   -> handleCreate(player, args);
            case "disband"  -> handleDisband(player);
            case "invite"   -> handleInvite(player, args);
            case "accept"   -> handleAccept(player);
            case "deny"     -> handleDeny(player);
            case "leave"    -> handleLeave(player);
            case "kick"     -> handleKick(player, args);
            case "promote"  -> handlePromote(player, args);
            case "demote"   -> handleDemote(player, args);
            case "transfer" -> handleTransfer(player, args);
            case "deposit"  -> handleDeposit(player, args);
            case "withdraw" -> handleWithdraw(player, args);
            case "info"     -> handleInfo(player, args);
            case "top"      -> handleTop(player);
            case "chat"     -> {
                if (args.length < 2) { player.sendMessage(MM.deserialize("<red>Usage: /gang chat <message>")); return true; }
                handleGangChat(player, String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)));
            }
            default -> sendHelp(player);
        }
        return true;
    }

    // ----------------------------------------------------------------
    // Subcommand handlers
    // ----------------------------------------------------------------

    private void handleCreate(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(MM.deserialize("<red>Usage: /gang create <name> <tag>"));
            return;
        }

        if (!PermissionEngine.getInstance().hasPermission(player, "prison.gang.create")) {
            player.sendMessage(MM.deserialize("<red>You don't have permission to create a gang."));
            return;
        }

        String name = args[1];
        String tag  = args[2].toUpperCase();

        manager.createGang(player.getUniqueId(), name, tag).thenAccept(result -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                switch (result) {
                    case SUCCESS ->
                        player.sendMessage(MM.deserialize("<green>Gang <white>" + name + " <gray>[" + tag + "]</gray><green> created!"));
                    case NAME_TAKEN ->
                        player.sendMessage(MM.deserialize("<red>A gang with that name already exists."));
                    case TAG_TAKEN ->
                        player.sendMessage(MM.deserialize("<red>A gang with that tag already exists."));
                    case ALREADY_IN_GANG ->
                        player.sendMessage(MM.deserialize("<red>You are already in a gang. Leave first."));
                    case INVALID_NAME ->
                        player.sendMessage(MM.deserialize("<red>Invalid name. Use 3-20 alphanumeric characters only."));
                    case INVALID_TAG ->
                        player.sendMessage(MM.deserialize("<red>Invalid tag. Use 2-6 alphanumeric characters only."));
                    default ->
                        player.sendMessage(MM.deserialize("<red>An error occurred. Please try again."));
                }
            });
        });
    }

    private void handleDisband(Player player) {
        GangData gang = manager.getGangOf(player.getUniqueId());
        if (gang == null) { player.sendMessage(MM.deserialize("<red>You are not in a gang.")); return; }

        // Require a second /gang disband within 30 seconds to confirm
        UUID uuid = player.getUniqueId();
        Long firstTime = pendingDisband.get(uuid);
        long now = System.currentTimeMillis();

        if (firstTime == null || now - firstTime > 30_000L) {
            pendingDisband.put(uuid, now);
            player.sendMessage(MM.deserialize(
                "<red>⚠ Are you sure you want to disband <white>" + gang.name() + "</white><red>?\n" +
                "<gray>This will kick all members and delete the gang permanently.\n" +
                "<yellow>Type <white>/gang disband</white> again within 30s to confirm."));
            return;
        }

        // Confirmed — clear pending and proceed
        pendingDisband.remove(uuid);

        manager.disbandGang(player.getUniqueId()).thenAccept(result -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                switch (result) {
                    case SUCCESS -> {
                        String msg = "<red>Gang <white>" + gang.name() + "</white><red> has been disbanded.";
                        broadcastToOnlineMembers(gang.id(), msg);
                        // broadcastToOnlineMembers already handles offline, but player may have left cache — send direct
                        if (!player.isOnline()) return;
                        player.sendMessage(MM.deserialize(msg));
                    }
                    case NOT_LEADER ->
                        player.sendMessage(MM.deserialize("<red>Only the gang leader can disband the gang."));
                    default ->
                        player.sendMessage(MM.deserialize("<red>An error occurred. Please try again."));
                }
            });
        });
    }

    private void handleInvite(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(MM.deserialize("<red>Usage: /gang invite <player>")); return; }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(MM.deserialize("<red>Player not found or not online."));
            return;
        }
        if (target.equals(player)) {
            player.sendMessage(MM.deserialize("<red>You cannot invite yourself."));
            return;
        }

        String donorRank = null;
        com.prison.donor.DonorAPI donorApi = com.prison.donor.DonorAPI.getInstance();
        if (donorApi != null) donorRank = donorApi.getDonorRank(target.getUniqueId());

        GangManager.InviteResult result = manager.invitePlayer(player.getUniqueId(), target.getUniqueId(), donorRank);
        GangData gang = manager.getGangOf(player.getUniqueId());

        switch (result) {
            case SUCCESS -> {
                player.sendMessage(MM.deserialize("<green>Invited <white>" + target.getName() + "</white><green> to your gang."));
                target.sendMessage(MM.deserialize("<aqua><white>" + player.getName() + "</white><aqua> has invited you to join gang <white>"
                    + (gang != null ? gang.name() : "?") + "</white><aqua>. Type <white>/gang accept</white> or <white>/gang deny</white>."));

                // Schedule invite expiry
                int expireSeconds = plugin.getConfig().getInt("invite-expire-seconds", 120);
                UUID targetUuid = target.getUniqueId();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (manager.getPendingInvite(targetUuid) != null) {
                        manager.denyInvite(targetUuid);
                        Player t = Bukkit.getPlayer(targetUuid);
                        if (t != null) t.sendMessage(MM.deserialize("<gray>Your gang invite has expired."));
                    }
                }, expireSeconds * 20L);
            }
            case NOT_IN_GANG      -> player.sendMessage(MM.deserialize("<red>You are not in a gang."));
            case NO_PERMISSION    -> player.sendMessage(MM.deserialize("<red>Only officers and leaders can invite."));
            case TARGET_IN_GANG   -> player.sendMessage(MM.deserialize("<red>" + target.getName() + " is already in a gang."));
            case ALREADY_INVITED  -> player.sendMessage(MM.deserialize("<red>" + target.getName() + " already has a pending invite."));
            case GANG_FULL        -> player.sendMessage(MM.deserialize("<red>Your gang is full."));
        }
    }

    private void handleAccept(Player player) {
        GangData invitingGang = manager.getPendingInvite(player.getUniqueId());
        if (invitingGang == null) {
            player.sendMessage(MM.deserialize("<red>You have no pending gang invite."));
            return;
        }

        manager.acceptInvite(player.getUniqueId()).thenAccept(result -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                switch (result) {
                    case SUCCESS -> {
                        player.sendMessage(MM.deserialize("<green>You joined gang <white>" + invitingGang.name() + "</white><green>!"));
                        broadcastToOnlineMembers(invitingGang.id(), "<aqua><white>" + player.getName() + "</white><aqua> joined the gang!");
                    }
                    case NO_INVITE ->
                        player.sendMessage(MM.deserialize("<red>That gang no longer exists."));
                    case GANG_FULL ->
                        player.sendMessage(MM.deserialize("<red>The gang is now full."));
                    default ->
                        player.sendMessage(MM.deserialize("<red>An error occurred. Please try again."));
                }
            });
        });
    }

    private void handleDeny(Player player) {
        boolean had = manager.denyInvite(player.getUniqueId());
        if (had) {
            player.sendMessage(MM.deserialize("<gray>Gang invite denied."));
        } else {
            player.sendMessage(MM.deserialize("<red>You have no pending gang invite."));
        }
    }

    private void handleLeave(Player player) {
        GangData gang = manager.getGangOf(player.getUniqueId());
        if (gang == null) { player.sendMessage(MM.deserialize("<red>You are not in a gang.")); return; }

        manager.leaveGang(player.getUniqueId()).thenAccept(result -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                switch (result) {
                    case SUCCESS -> {
                        player.sendMessage(MM.deserialize("<gray>You left gang <white>" + gang.name() + "</white><gray>."));
                        broadcastToOnlineMembers(gang.id(), "<gray><white>" + player.getName() + "</white> left the gang.");
                    }
                    case LEADER_MUST_TRANSFER ->
                        player.sendMessage(MM.deserialize("<red>You are the leader. Transfer leadership first with <white>/gang transfer <player></white><red>, or disband with <white>/gang disband</white><red>."));
                    default ->
                        player.sendMessage(MM.deserialize("<red>An error occurred. Please try again."));
                }
            });
        });
    }

    private void handleKick(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(MM.deserialize("<red>Usage: /gang kick <player>")); return; }

        Player target = Bukkit.getPlayer(args[1]);
        UUID targetUuid;
        String targetName;
        if (target != null) {
            targetUuid = target.getUniqueId();
            targetName = target.getName();
        } else {
            // Allow kicking offline members
            OfflinePlayer offline = Bukkit.getOfflinePlayer(args[1]);
            targetUuid = offline.getUniqueId();
            targetName = offline.getName() != null ? offline.getName() : args[1];
        }

        manager.kickMember(player.getUniqueId(), targetUuid).thenAccept(result -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                switch (result) {
                    case SUCCESS -> {
                        player.sendMessage(MM.deserialize("<green>Kicked <white>" + targetName + "</white> from the gang."));
                        if (target != null && target.isOnline()) {
                            target.sendMessage(MM.deserialize("<red>You were kicked from the gang."));
                        }
                    }
                    case NOT_IN_GANG         -> player.sendMessage(MM.deserialize("<red>You are not in a gang."));
                    case NO_PERMISSION       -> player.sendMessage(MM.deserialize("<red>Only officers and leaders can kick members."));
                    case TARGET_NOT_IN_GANG  -> player.sendMessage(MM.deserialize("<red>That player is not in your gang."));
                    case CANNOT_KICK_HIGHER  -> player.sendMessage(MM.deserialize("<red>Officers cannot kick other officers or the leader."));
                    default                  -> player.sendMessage(MM.deserialize("<red>An error occurred. Please try again."));
                }
            });
        });
    }

    private void handlePromote(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(MM.deserialize("<red>Usage: /gang promote <player>")); return; }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { player.sendMessage(MM.deserialize("<red>That player is not online.")); return; }

        manager.promoteToOfficer(player.getUniqueId(), target.getUniqueId()).thenAccept(result -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                switch (result) {
                    case SUCCESS -> {
                        player.sendMessage(MM.deserialize("<green>Promoted <white>" + target.getName() + "</white> to Officer."));
                        target.sendMessage(MM.deserialize("<gold>You were promoted to <yellow>Officer</yellow> in your gang!"));
                    }
                    case NOT_IN_GANG        -> player.sendMessage(MM.deserialize("<red>You are not in a gang."));
                    case NO_PERMISSION      -> player.sendMessage(MM.deserialize("<red>Only the leader can promote members."));
                    case TARGET_NOT_IN_GANG -> player.sendMessage(MM.deserialize("<red>That player is not in your gang."));
                    case ALREADY_THAT_ROLE  -> player.sendMessage(MM.deserialize("<red>That player is already an Officer or higher."));
                    default                 -> player.sendMessage(MM.deserialize("<red>An error occurred."));
                }
            });
        });
    }

    private void handleDemote(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(MM.deserialize("<red>Usage: /gang demote <player>")); return; }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { player.sendMessage(MM.deserialize("<red>That player is not online.")); return; }

        manager.demoteToMember(player.getUniqueId(), target.getUniqueId()).thenAccept(result -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                switch (result) {
                    case SUCCESS -> {
                        player.sendMessage(MM.deserialize("<green>Demoted <white>" + target.getName() + "</white> to Member."));
                        target.sendMessage(MM.deserialize("<yellow>You were demoted to <gray>Member</gray> in your gang."));
                    }
                    case NOT_IN_GANG        -> player.sendMessage(MM.deserialize("<red>You are not in a gang."));
                    case NO_PERMISSION      -> player.sendMessage(MM.deserialize("<red>Only the leader can demote officers."));
                    case TARGET_NOT_IN_GANG -> player.sendMessage(MM.deserialize("<red>That player is not in your gang."));
                    case ALREADY_THAT_ROLE  -> player.sendMessage(MM.deserialize("<red>That player is already a Member."));
                    default                 -> player.sendMessage(MM.deserialize("<red>An error occurred."));
                }
            });
        });
    }

    private void handleTransfer(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(MM.deserialize("<red>Usage: /gang transfer <player>")); return; }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { player.sendMessage(MM.deserialize("<red>That player is not online.")); return; }

        manager.transferLeadership(player.getUniqueId(), target.getUniqueId()).thenAccept(result -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                switch (result) {
                    case SUCCESS -> {
                        player.sendMessage(MM.deserialize("<green>Leadership transferred to <white>" + target.getName() + "</white>."));
                        target.sendMessage(MM.deserialize("<gold>You are now the <bold>Leader</bold> of your gang!"));
                    }
                    case NOT_IN_GANG        -> player.sendMessage(MM.deserialize("<red>You are not in a gang."));
                    case NOT_LEADER         -> player.sendMessage(MM.deserialize("<red>Only the leader can transfer leadership."));
                    case TARGET_NOT_IN_GANG -> player.sendMessage(MM.deserialize("<red>That player is not in your gang."));
                    default                 -> player.sendMessage(MM.deserialize("<red>An error occurred."));
                }
            });
        });
    }

    private void handleDeposit(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(MM.deserialize("<red>Usage: /gang deposit <amount>")); return; }

        long amount;
        try {
            amount = Long.parseLong(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(MM.deserialize("<red>Invalid amount."));
            return;
        }
        if (amount <= 0) { player.sendMessage(MM.deserialize("<red>Amount must be positive.")); return; }

        GangData gangBefore = manager.getGangOf(player.getUniqueId());
        int levelBefore = gangBefore != null ? gangBefore.level() : 1;

        manager.deposit(player.getUniqueId(), amount).thenAccept(result -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                switch (result) {
                    case SUCCESS -> {
                        GangData gang = manager.getGangOf(player.getUniqueId());
                        String bankStr = gang == null ? "?" : String.format("%,d", gang.bankBalance());
                        player.sendMessage(MM.deserialize("<green>Deposited <white>" + String.format("%,d", amount)
                            + "</white> IGC to the gang bank. Bank: <white>" + bankStr + "</white> IGC."));

                        // Quest progress
                        try {
                            com.prison.quests.QuestsAPI qapi = com.prison.quests.QuestsAPI.getInstance();
                            if (qapi != null) qapi.addProgress(player.getUniqueId(), com.prison.quests.QuestType.GANG_DEPOSITS, 1L);
                        } catch (NoClassDefFoundError ignored) { /* PrisonQuests not loaded */ }

                        // Gang level-up notification
                        if (gang != null && gang.level() > levelBefore) {
                            String msg = "\n<gold>⚔ <yellow>Gang <white>" + gang.name()
                                + "</white> levelled up to <gold>Level " + gang.level() + "</gold>!";
                            for (UUID memberId : manager.getMembers(gang.id())) {
                                Player member = Bukkit.getPlayer(memberId);
                                if (member != null) {
                                    member.sendMessage(MM.deserialize(msg));
                                    member.playSound(member.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 0.8f);
                                }
                            }
                        }
                    }
                    case NOT_IN_GANG        -> player.sendMessage(MM.deserialize("<red>You are not in a gang."));
                    case INSUFFICIENT_FUNDS -> player.sendMessage(MM.deserialize("<red>You don't have enough IGC."));
                    default                 -> player.sendMessage(MM.deserialize("<red>An error occurred."));
                }
            });
        });
    }

    private void handleWithdraw(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(MM.deserialize("<red>Usage: /gang withdraw <amount>")); return; }

        long amount;
        try {
            amount = Long.parseLong(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(MM.deserialize("<red>Invalid amount."));
            return;
        }
        if (amount <= 0) { player.sendMessage(MM.deserialize("<red>Amount must be positive.")); return; }

        manager.withdraw(player.getUniqueId(), amount).thenAccept(result -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                switch (result) {
                    case SUCCESS -> {
                        GangData gang = manager.getGangOf(player.getUniqueId());
                        String bankStr = gang == null ? "?" : String.format("%,d", gang.bankBalance());
                        player.sendMessage(MM.deserialize("<green>Withdrew <white>" + String.format("%,d", amount)
                            + "</white> IGC from the gang bank. Bank: <white>" + bankStr + "</white> IGC."));
                    }
                    case NOT_IN_GANG        -> player.sendMessage(MM.deserialize("<red>You are not in a gang."));
                    case NO_PERMISSION      -> player.sendMessage(MM.deserialize("<red>Only officers and leaders can withdraw."));
                    case INSUFFICIENT_FUNDS -> player.sendMessage(MM.deserialize("<red>The gang bank doesn't have enough IGC."));
                    default                 -> player.sendMessage(MM.deserialize("<red>An error occurred."));
                }
            });
        });
    }

    private void handleInfo(Player player, String[] args) {
        // /gang info [gangname]
        GangData gang;
        if (args.length >= 2) {
            gang = manager.getGangByName(args[1]);
            if (gang == null) {
                player.sendMessage(MM.deserialize("<red>Gang not found: " + args[1]));
                return;
            }
        } else {
            gang = manager.getGangOf(player.getUniqueId());
            if (gang == null) {
                player.sendMessage(MM.deserialize("<red>You are not in a gang."));
                return;
            }
        }

        final GangData finalGang = gang;
        Set<UUID> memberUuids = manager.getMembers(gang.id());
        int memberCount = memberUuids.size();

        // Build member list
        StringBuilder memberList = new StringBuilder();
        for (UUID uuid : memberUuids) {
            GangRole role = manager.getMemberRole(uuid);
            String rolePfx = role == null ? "" : role.display() + " ";
            Player online = Bukkit.getPlayer(uuid);
            String nameStr = online != null
                ? "<green>" + online.getName() + "</green>"
                : "<gray>" + (Bukkit.getOfflinePlayer(uuid).getName() != null
                    ? Bukkit.getOfflinePlayer(uuid).getName() : uuid.toString().substring(0, 8)) + "</gray>";
            memberList.append("\n  ").append(rolePfx).append(nameStr);
        }

        player.sendMessage(MM.deserialize(
            "<dark_aqua>--- Gang Info: <white>" + finalGang.name() + " <gray>[" + finalGang.tag() + "]</gray> ---\n"
            + "<aqua>Level: <white>" + finalGang.level() + "\n"
            + "<aqua>Bank: <gold>" + String.format("%,d", finalGang.bankBalance()) + " IGC\n"
            + "<aqua>Members: <white>" + memberCount + "\n"
            + "<aqua>Roster:" + memberList
        ));
    }

    private void handleTop(Player player) {
        java.util.List<GangData> top = manager.getLeaderboard();
        if (top.isEmpty()) {
            player.sendMessage(MM.deserialize("<gray>No gangs have been created yet."));
            return;
        }

        StringBuilder sb = new StringBuilder("<gold><bold>--- Gang Leaderboard ---</bold></gold>");
        for (int i = 0; i < top.size(); i++) {
            GangData g = top.get(i);
            sb.append("\n<yellow>").append(i + 1).append(". </yellow>")
              .append("<white>").append(g.name()).append("</white>")
              .append(" <gray>[").append(g.tag()).append("]</gray>")
              .append(" <aqua>Lv").append(g.level()).append("</aqua>")
              .append(" <gold>").append(String.format("%,d", g.bankBalance())).append(" IGC</gold>")
              .append(" <gray>(").append(manager.getMemberCount(g.id())).append(" members)</gray>");
        }
        player.sendMessage(MM.deserialize(sb.toString()));
    }

    // ----------------------------------------------------------------
    // Gang chat
    // ----------------------------------------------------------------

    private void handleGangChat(Player player, String message) {
        GangData gang = manager.getGangOf(player.getUniqueId());
        if (gang == null) {
            player.sendMessage(MM.deserialize("<red>You are not in a gang."));
            return;
        }

        String safeMessage = MM.escapeTags(message);
        Component chatLine = MM.deserialize(
            "<dark_green>[Gang] <gray>[" + gang.tag() + "]</gray> <green>" + player.getName() + ": <white>" + safeMessage
        );

        // Send to all online gang members
        Set<UUID> members = manager.getMembers(gang.id());
        for (UUID uuid : members) {
            Player member = Bukkit.getPlayer(uuid);
            if (member != null) member.sendMessage(chatLine);
        }

        // Staff with admin permission also see gang chat
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!members.contains(online.getUniqueId())
                && PermissionEngine.getInstance().hasPermission(online, "prison.admin.*")) {
                online.sendMessage(MM.deserialize("<dark_gray>[GangSpy] ").append(chatLine));
            }
        }
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    /** Broadcast a MiniMessage string to all online members of a gang (excluding senders). */
    private void broadcastToOnlineMembers(long gangId, String miniMessage) {
        Component msg = MM.deserialize(miniMessage);
        for (UUID uuid : manager.getMembers(gangId)) {
            Player member = Bukkit.getPlayer(uuid);
            if (member != null) member.sendMessage(msg);
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(MM.deserialize(
            "<gold><bold>--- Gang Commands ---</bold></gold>\n"
            + "<yellow>/gang create <name> <tag></yellow> <gray>- Create a gang\n"
            + "<yellow>/gang invite <player></yellow> <gray>- Invite a player\n"
            + "<yellow>/gang accept | deny</yellow> <gray>- Accept/deny an invite\n"
            + "<yellow>/gang leave</yellow> <gray>- Leave your gang\n"
            + "<yellow>/gang kick <player></yellow> <gray>- Kick a member\n"
            + "<yellow>/gang promote | demote <player></yellow> <gray>- Change officer role\n"
            + "<yellow>/gang transfer <player></yellow> <gray>- Transfer leadership\n"
            + "<yellow>/gang deposit | withdraw <amount></yellow> <gray>- Gang bank\n"
            + "<yellow>/gang info [name]</yellow> <gray>- View gang info\n"
            + "<yellow>/gang top</yellow> <gray>- Gang leaderboard\n"
            + "<yellow>/gc <message></yellow> <gray>- Gang chat"
        ));
    }
}

package com.circuitdrop.kingdomclaim.command;

import com.circuitdrop.kingdomclaim.KingdomClaimPlugin;
import com.circuitdrop.kingdomclaim.gui.MainMenuGui;
import com.circuitdrop.kingdomclaim.manager.KingdomManager;
import com.circuitdrop.kingdomclaim.manager.PlayerDataManager;
import com.circuitdrop.kingdomclaim.manager.WarManager;
import com.circuitdrop.kingdomclaim.model.ClaimChunk;
import com.circuitdrop.kingdomclaim.model.Kingdom;
import com.circuitdrop.kingdomclaim.model.Rank;
import com.circuitdrop.kingdomclaim.model.RelationType;
import com.circuitdrop.kingdomclaim.util.ChunkVisualizer;
import com.circuitdrop.kingdomclaim.util.MapRenderer;
import com.circuitdrop.kingdomclaim.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Single entry point for every "/kingdom" subcommand. Kept as one dispatcher
 * class (rather than per-command classes) since most subcommands are a few
 * lines of validation around a KingdomManager call.
 */
public class KingdomCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of(
            "create", "disband", "invite", "accept", "deny", "invites", "leave", "kick",
            "promote", "demote", "transfer", "claim", "unclaim", "map", "visualize", "gui",
            "relation", "declarewar", "chat", "sethome", "home", "info", "list", "admin", "help");

    private final KingdomClaimPlugin plugin;
    private final KingdomManager kingdoms;
    private final PlayerDataManager playerData;
    private final WarManager warManager;

    public KingdomCommand(KingdomClaimPlugin plugin, KingdomManager kingdoms, PlayerDataManager playerData, WarManager warManager) {
        this.plugin = plugin;
        this.kingdoms = kingdoms;
        this.playerData = playerData;
        this.warManager = warManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        String sub = args[0].toLowerCase();
        String[] rest = args.length > 1 ? List.of(args).subList(1, args.length).toArray(new String[0]) : new String[0];

        switch (sub) {
            case "create" -> requirePlayer(sender, p -> create(p, rest));
            case "disband" -> requirePlayer(sender, this::disband);
            case "invite" -> requirePlayer(sender, p -> invite(p, rest));
            case "accept" -> requirePlayer(sender, p -> respondInvite(p, rest, true));
            case "deny" -> requirePlayer(sender, p -> respondInvite(p, rest, false));
            case "invites" -> requirePlayer(sender, this::listInvites);
            case "leave" -> requirePlayer(sender, this::leave);
            case "kick" -> requirePlayer(sender, p -> kick(p, rest));
            case "promote" -> requirePlayer(sender, p -> changeRank(p, rest, true));
            case "demote" -> requirePlayer(sender, p -> changeRank(p, rest, false));
            case "transfer" -> requirePlayer(sender, p -> transfer(p, rest));
            case "claim" -> requirePlayer(sender, this::claim);
            case "unclaim" -> requirePlayer(sender, this::unclaim);
            case "map" -> requirePlayer(sender, this::map);
            case "visualize" -> requirePlayer(sender, this::visualize);
            case "gui" -> requirePlayer(sender, this::openGui);
            case "relation" -> requirePlayer(sender, p -> relation(p, rest));
            case "declarewar" -> requirePlayer(sender, p -> declareWar(p, rest));
            case "chat" -> requirePlayer(sender, this::toggleChat);
            case "sethome" -> requirePlayer(sender, this::setHome);
            case "home" -> requirePlayer(sender, this::home);
            case "info" -> info(sender, rest);
            case "list" -> list(sender);
            case "admin" -> requirePlayer(sender, p -> admin(p, rest));
            default -> sendHelp(sender);
        }
        return true;
    }

    // ---- membership ----

    private void create(Player player, String[] args) {
        if (args.length < 1) {
            Messages.error(player, "Usage: /kingdom create <name>");
            return;
        }
        if (kingdoms.kingdomOf(player.getUniqueId()).isPresent()) {
            Messages.error(player, "You are already in a kingdom.");
            return;
        }
        String name = args[0];
        if (name.length() < 3 || name.length() > 16) {
            Messages.error(player, "Kingdom names must be 3-16 characters.");
            return;
        }
        if (kingdoms.isNameTaken(name)) {
            Messages.error(player, "That kingdom name is already taken.");
            return;
        }
        kingdoms.createKingdom(name, player.getUniqueId());
        Messages.success(player, "Founded the kingdom of " + name + "!");
    }

    private void disband(Player player) {
        Kingdom kingdom = requireOwnKingdom(player);
        if (kingdom == null) return;
        if (kingdom.rankOf(player.getUniqueId()) != Rank.KING) {
            Messages.error(player, "Only the king can disband the kingdom.");
            return;
        }
        kingdoms.disbandKingdom(kingdom);
        Messages.success(player, "The kingdom of " + kingdom.name() + " has been disbanded.");
    }

    private void invite(Player player, String[] args) {
        Kingdom kingdom = requireOwnKingdom(player);
        if (kingdom == null) return;
        if (!kingdom.rankOf(player.getUniqueId()).atLeast(Rank.OFFICER)) {
            Messages.error(player, "Only officers and the king can invite players.");
            return;
        }
        if (args.length < 1) {
            Messages.error(player, "Usage: /kingdom invite <player>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            Messages.error(player, "Player not found or offline.");
            return;
        }
        if (kingdoms.kingdomOf(target.getUniqueId()).isPresent()) {
            Messages.error(player, target.getName() + " is already in a kingdom.");
            return;
        }
        kingdoms.invite(kingdom, target.getUniqueId());
        Messages.success(player, "Invited " + target.getName() + " to " + kingdom.name() + ".");
        Messages.info(target, player.getName() + " invited you to join " + kingdom.name()
                + ". Use /kingdom accept " + kingdom.name() + " to join.");
    }

    private void respondInvite(Player player, String[] args, boolean accept) {
        if (args.length < 1) {
            Messages.error(player, "Usage: /kingdom " + (accept ? "accept" : "deny") + " <kingdom>");
            return;
        }
        Optional<Kingdom> kingdomOpt = kingdoms.byName(args[0]);
        if (kingdomOpt.isEmpty() || !kingdoms.hasInvite(player.getUniqueId(), kingdomOpt.get())) {
            Messages.error(player, "You don't have a pending invite from that kingdom.");
            return;
        }
        Kingdom kingdom = kingdomOpt.get();
        if (!accept) {
            kingdoms.removeInvite(player.getUniqueId(), kingdom);
            Messages.info(player, "Declined the invite from " + kingdom.name() + ".");
            return;
        }
        if (kingdoms.kingdomOf(player.getUniqueId()).isPresent()) {
            Messages.error(player, "You are already in a kingdom.");
            return;
        }
        kingdoms.addMember(kingdom, player.getUniqueId(), Rank.MEMBER);
        Messages.success(player, "You joined " + kingdom.name() + "!");
    }

    private void listInvites(Player player) {
        var ids = kingdoms.invitesFor(player.getUniqueId());
        if (ids.isEmpty()) {
            Messages.info(player, "You have no pending invites.");
            return;
        }
        String names = ids.stream()
                .map(kingdoms::byId)
                .flatMap(Optional::stream)
                .map(Kingdom::name)
                .collect(Collectors.joining(", "));
        Messages.info(player, "Pending invites: " + names);
    }

    private void leave(Player player) {
        Kingdom kingdom = requireOwnKingdom(player);
        if (kingdom == null) return;
        if (kingdom.rankOf(player.getUniqueId()) == Rank.KING && kingdom.memberCount() > 1) {
            Messages.error(player, "Transfer kingship with /kingdom transfer <player> before leaving.");
            return;
        }
        kingdoms.removeMember(kingdom, player.getUniqueId());
        if (kingdom.memberCount() == 0) {
            kingdoms.disbandKingdom(kingdom);
        }
        Messages.success(player, "You left " + kingdom.name() + ".");
    }

    private void kick(Player player, String[] args) {
        Kingdom kingdom = requireOwnKingdom(player);
        if (kingdom == null) return;
        if (args.length < 1) {
            Messages.error(player, "Usage: /kingdom kick <player>");
            return;
        }
        Optional<UUID> targetOpt = findMemberByName(kingdom, args[0]);
        if (targetOpt.isEmpty()) {
            Messages.error(player, args[0] + " is not a member of your kingdom.");
            return;
        }
        UUID target = targetOpt.get();
        Rank actorRank = kingdom.rankOf(player.getUniqueId());
        Rank targetRank = kingdom.rankOf(target);
        if (!actorRank.atLeast(Rank.OFFICER) || targetRank.atLeast(actorRank)) {
            Messages.error(player, "You cannot kick that member.");
            return;
        }
        kingdoms.removeMember(kingdom, target);
        Messages.success(player, "Kicked " + args[0] + " from the kingdom.");
    }

    private void changeRank(Player player, String[] args, boolean promote) {
        Kingdom kingdom = requireOwnKingdom(player);
        if (kingdom == null) return;
        if (args.length < 1) {
            Messages.error(player, "Usage: /kingdom " + (promote ? "promote" : "demote") + " <player>");
            return;
        }
        Rank actorRank = kingdom.rankOf(player.getUniqueId());
        if (!actorRank.atLeast(Rank.OFFICER)) {
            Messages.error(player, "Only officers and the king can change ranks.");
            return;
        }
        Optional<UUID> targetOpt = findMemberByName(kingdom, args[0]);
        Rank targetRank = targetOpt.map(kingdom::rankOf).orElse(null);
        if (targetRank == null || targetRank.atLeast(actorRank)) {
            Messages.error(player, "You cannot change that member's rank.");
            return;
        }
        UUID target = targetOpt.get();
        if (promote) {
            if (targetRank != Rank.MEMBER) {
                Messages.error(player, args[0] + " is already an officer. Use /kingdom transfer to make them king.");
                return;
            }
            kingdoms.addMember(kingdom, target, Rank.OFFICER);
            Messages.success(player, "Promoted " + args[0] + " to Officer.");
        } else {
            if (targetRank != Rank.OFFICER) {
                Messages.error(player, args[0] + " is not an officer.");
                return;
            }
            kingdoms.addMember(kingdom, target, Rank.MEMBER);
            Messages.success(player, "Demoted " + args[0] + " to Member.");
        }
    }

    private void transfer(Player player, String[] args) {
        Kingdom kingdom = requireOwnKingdom(player);
        if (kingdom == null) return;
        if (kingdom.rankOf(player.getUniqueId()) != Rank.KING) {
            Messages.error(player, "Only the king can transfer kingship.");
            return;
        }
        if (args.length < 1) {
            Messages.error(player, "Usage: /kingdom transfer <player>");
            return;
        }
        Optional<UUID> targetOpt = findMemberByName(kingdom, args[0]);
        if (targetOpt.isEmpty()) {
            Messages.error(player, args[0] + " is not a member of your kingdom.");
            return;
        }
        kingdoms.addMember(kingdom, targetOpt.get(), Rank.KING);
        kingdoms.addMember(kingdom, player.getUniqueId(), Rank.OFFICER);
        Messages.success(player, "You handed the crown of " + kingdom.name() + " to " + args[0] + ".");
    }

    // ---- claims ----

    private void claim(Player player) {
        Kingdom kingdom = requireOwnKingdom(player);
        if (kingdom == null) return;
        if (!kingdom.rankOf(player.getUniqueId()).atLeast(Rank.OFFICER)) {
            Messages.error(player, "Only officers and the king can claim land.");
            return;
        }
        ClaimChunk chunk = ClaimChunk.of(player.getLocation());
        Optional<Kingdom> existing = kingdoms.kingdomAt(chunk);
        if (existing.isPresent()) {
            Messages.error(player, "This chunk is already claimed by " + existing.get().name() + ".");
            return;
        }
        int maxClaims = plugin.getConfig().getInt("max-claims-per-kingdom", 200);
        if (kingdom.claims().size() >= maxClaims) {
            Messages.error(player, "Your kingdom has reached its claim limit (" + maxClaims + ").");
            return;
        }
        kingdoms.claimChunk(kingdom, chunk);
        Messages.success(player, "Claimed chunk (" + chunk.x() + ", " + chunk.z() + ") for " + kingdom.name() + ".");
    }

    private void unclaim(Player player) {
        Kingdom kingdom = requireOwnKingdom(player);
        if (kingdom == null) return;
        if (!kingdom.rankOf(player.getUniqueId()).atLeast(Rank.OFFICER)) {
            Messages.error(player, "Only officers and the king can unclaim land.");
            return;
        }
        ClaimChunk chunk = ClaimChunk.of(player.getLocation());
        if (!kingdom.claims().contains(chunk)) {
            Messages.error(player, "Your kingdom doesn't own this chunk.");
            return;
        }
        kingdoms.unclaimChunk(kingdom, chunk);
        Messages.success(player, "Unclaimed chunk (" + chunk.x() + ", " + chunk.z() + ").");
    }

    private void map(Player player) {
        player.sendMessage(MapRenderer.render(player, kingdoms));
    }

    private void visualize(Player player) {
        Kingdom kingdom = requireOwnKingdom(player);
        if (kingdom == null) return;
        if (kingdom.claims().isEmpty()) {
            Messages.error(player, "Your kingdom has no claims to show.");
            return;
        }
        ChunkVisualizer.visualize(plugin, player, kingdom.claims(), Color.LIME, 15);
        Messages.success(player, "Showing your kingdom's claim borders for 15 seconds.");
    }

    private void openGui(Player player) {
        Kingdom kingdom = requireOwnKingdom(player);
        if (kingdom == null) return;
        MainMenuGui.open(player, kingdom, plugin);
    }

    // ---- diplomacy ----

    private void relation(Player player, String[] args) {
        Kingdom kingdom = requireOwnKingdom(player);
        if (kingdom == null) return;
        if (!kingdom.rankOf(player.getUniqueId()).atLeast(Rank.OFFICER)) {
            Messages.error(player, "Only officers and the king can change relations.");
            return;
        }
        if (args.length < 2) {
            Messages.error(player, "Usage: /kingdom relation <ally|neutral|enemy> <kingdom>");
            return;
        }
        if (args[0].equalsIgnoreCase("war")) {
            Messages.error(player, "War can't be set directly — use /kingdom declarewar <kingdom>.");
            return;
        }
        RelationType type;
        try {
            type = RelationType.valueOf(args[0].toUpperCase());
        } catch (IllegalArgumentException e) {
            Messages.error(player, "Unknown relation type. Use ally, neutral or enemy.");
            return;
        }
        Optional<Kingdom> targetOpt = kingdoms.byName(args[1]);
        if (targetOpt.isEmpty() || targetOpt.get().id().equals(kingdom.id())) {
            Messages.error(player, "Unknown kingdom: " + args[1]);
            return;
        }
        kingdoms.setRelation(kingdom, targetOpt.get(), type);
        Messages.success(player, kingdom.name() + " now considers " + targetOpt.get().name() + ": " + type.name());
    }

    private void declareWar(Player player, String[] args) {
        if (args.length < 1) {
            Messages.error(player, "Usage: /kingdom declarewar <kingdom>");
            return;
        }
        Optional<Kingdom> targetOpt = kingdoms.byName(args[0]);
        if (targetOpt.isEmpty()) {
            Messages.error(player, "Unknown kingdom: " + args[0]);
            return;
        }
        warManager.declareWar(player, targetOpt.get());
    }

    // ---- misc ----

    private void toggleChat(Player player) {
        if (kingdoms.kingdomOf(player.getUniqueId()).isEmpty()) {
            Messages.error(player, "You are not in a kingdom.");
            return;
        }
        playerData.toggleKingdomChat(player.getUniqueId());
        boolean enabled = playerData.isKingdomChatEnabled(player.getUniqueId());
        Messages.success(player, "Kingdom chat " + (enabled ? "enabled" : "disabled") + ".");
    }

    private void setHome(Player player) {
        Kingdom kingdom = requireOwnKingdom(player);
        if (kingdom == null) return;
        if (!kingdom.rankOf(player.getUniqueId()).atLeast(Rank.OFFICER)) {
            Messages.error(player, "Only officers and the king can set the kingdom home.");
            return;
        }
        ClaimChunk here = ClaimChunk.of(player.getLocation());
        if (!kingdom.claims().contains(here)) {
            Messages.error(player, "You must be standing in your kingdom's claimed land.");
            return;
        }
        Location loc = player.getLocation();
        kingdom.setHome(loc.getWorld().getName() + ";" + loc.getX() + ";" + loc.getY() + ";" + loc.getZ()
                + ";" + loc.getYaw() + ";" + loc.getPitch());
        Messages.success(player, "Kingdom home set.");
    }

    private void home(Player player) {
        Kingdom kingdom = requireOwnKingdom(player);
        if (kingdom == null) return;
        if (kingdom.home() == null) {
            Messages.error(player, "Your kingdom has not set a home yet.");
            return;
        }
        String[] parts = kingdom.home().split(";");
        var world = Bukkit.getWorld(parts[0]);
        if (world == null) {
            Messages.error(player, "The kingdom home's world is not loaded.");
            return;
        }
        Location loc = new Location(world, Double.parseDouble(parts[1]), Double.parseDouble(parts[2]),
                Double.parseDouble(parts[3]), Float.parseFloat(parts[4]), Float.parseFloat(parts[5]));
        player.teleportAsync(loc);
        Messages.success(player, "Teleported to your kingdom's home.");
    }

    private void info(CommandSender sender, String[] args) {
        Optional<Kingdom> kingdomOpt;
        if (args.length >= 1) {
            kingdomOpt = kingdoms.byName(args[0]);
        } else if (sender instanceof Player player) {
            kingdomOpt = kingdoms.kingdomOf(player.getUniqueId());
        } else {
            Messages.error(sender, "Usage: /kingdom info <name>");
            return;
        }
        if (kingdomOpt.isEmpty()) {
            Messages.error(sender, "Kingdom not found.");
            return;
        }
        Kingdom kingdom = kingdomOpt.get();
        String kingName = Optional.ofNullable(kingdom.king())
                .map(id -> Bukkit.getOfflinePlayer(id).getName())
                .orElse("none");
        Messages.info(sender, kingdom.name() + " — King: " + kingName
                + " | Members: " + kingdom.memberCount() + " | Claims: " + kingdom.claims().size());
    }

    private void list(CommandSender sender) {
        List<Kingdom> sorted = kingdoms.all().stream()
                .sorted(Comparator.comparingInt(Kingdom::memberCount).reversed())
                .toList();
        if (sorted.isEmpty()) {
            Messages.info(sender, "There are no kingdoms yet.");
            return;
        }
        String names = sorted.stream()
                .map(k -> k.name() + " (" + k.memberCount() + ")")
                .collect(Collectors.joining(", "));
        Messages.info(sender, "Kingdoms: " + names);
    }

    private void admin(Player player, String[] args) {
        if (!player.hasPermission("kingdomclaim.admin")) {
            Messages.error(player, "You don't have permission to do that.");
            return;
        }
        if (args.length < 1 || !args[0].equalsIgnoreCase("bypass")) {
            Messages.error(player, "Usage: /kingdom admin bypass");
            return;
        }
        boolean nowEnabled = playerData.toggleBypass(player.getUniqueId());
        Messages.success(player, "Protection bypass " + (nowEnabled ? "enabled" : "disabled") + ".");
    }

    // ---- helpers ----

    private Optional<UUID> findMemberByName(Kingdom kingdom, String name) {
        return kingdom.members().keySet().stream()
                .filter(id -> name.equalsIgnoreCase(Bukkit.getOfflinePlayer(id).getName()))
                .findFirst();
    }

    private Kingdom requireOwnKingdom(Player player) {
        Optional<Kingdom> kingdom = kingdoms.kingdomOf(player.getUniqueId());
        if (kingdom.isEmpty()) {
            Messages.error(player, "You are not in a kingdom.");
            return null;
        }
        return kingdom.get();
    }

    private void requirePlayer(CommandSender sender, java.util.function.Consumer<Player> action) {
        if (!(sender instanceof Player player)) {
            Messages.error(sender, "This command can only be used by players.");
            return;
        }
        action.accept(player);
    }

    private void sendHelp(CommandSender sender) {
        Messages.info(sender, "KingdomClaim commands: /kingdom <create|disband|invite|accept|deny|invites|leave|kick|"
                + "promote|demote|transfer|claim|unclaim|map|visualize|gui|relation|declarewar|chat|sethome|home|info|list>");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return SUBCOMMANDS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            return switch (sub) {
                case "invite", "kick", "promote", "demote", "transfer" -> onlinePlayerNames(args[1]);
                case "accept", "deny", "info", "declarewar" -> kingdomNames(args[1]);
                case "relation" -> Stream.of("ally", "neutral", "enemy")
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                default -> List.of();
            };
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("relation")) {
            return kingdomNames(args[2]);
        }
        return List.of();
    }

    private List<String> onlinePlayerNames(String prefix) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(n -> n.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }

    private List<String> kingdomNames(String prefix) {
        return kingdoms.all().stream()
                .map(Kingdom::name)
                .filter(n -> n.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }
}

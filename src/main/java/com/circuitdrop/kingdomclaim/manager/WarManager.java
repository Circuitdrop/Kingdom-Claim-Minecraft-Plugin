package com.circuitdrop.kingdomclaim.manager;

import com.circuitdrop.kingdomclaim.model.Kingdom;
import com.circuitdrop.kingdomclaim.model.Rank;
import com.circuitdrop.kingdomclaim.model.RelationType;
import com.circuitdrop.kingdomclaim.util.Diplomacy;
import com.circuitdrop.kingdomclaim.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Gates how a kingdom enters WAR: only a king may declare it, only against
 * a kingdom whose king is currently online, and it doesn't take effect until
 * a 30-minute notice period has elapsed. Nothing war-related (PvP, TNT
 * raiding) is active until {@link #startWar} actually writes the WAR
 * relation — a pending declaration grants no permissions on its own, and
 * the declaring king can call it off any time before then with
 * {@link #cancelDeclaration}. Either side's king can end an active war
 * unilaterally via {@link #surrender}, which takes effect immediately with
 * no notice period — only the defender pays a claim-loss penalty for it,
 * though; the kingdom that declared the war can always call it off for free.
 */
public class WarManager {

    private static final long DECLARATION_NOTICE_TICKS = 20L * 60L * 30L; // 30 minutes

    private final KingdomManager kingdoms;
    private final Plugin plugin;
    private final Map<String, PendingDeclaration> pendingDeclarations = new HashMap<>();

    private record PendingDeclaration(UUID attackerId, BukkitTask task) {
    }

    public WarManager(KingdomManager kingdoms, Plugin plugin) {
        this.kingdoms = kingdoms;
        this.plugin = plugin;
    }

    public boolean isActiveWar(Kingdom a, Kingdom b) {
        return Diplomacy.effectiveRelation(a, b) == RelationType.WAR;
    }

    public void declareWar(Player declarer, Kingdom target) {
        Optional<Kingdom> attackerOpt = kingdoms.kingdomOf(declarer.getUniqueId());
        if (attackerOpt.isEmpty()) {
            Messages.error(declarer, "You are not in a kingdom.");
            return;
        }
        Kingdom attacker = attackerOpt.get();
        if (attacker.id().equals(target.id())) {
            Messages.error(declarer, "You cannot declare war on your own kingdom.");
            return;
        }
        if (attacker.rankOf(declarer.getUniqueId()) != Rank.KING) {
            Messages.error(declarer, "Only the king can declare war.");
            return;
        }
        if (isActiveWar(attacker, target)) {
            Messages.error(declarer, "You are already at war with " + target.name() + ".");
            return;
        }
        String key = pairKey(attacker.id(), target.id());
        if (pendingDeclarations.containsKey(key)) {
            Messages.error(declarer, "A war declaration against " + target.name() + " is already pending.");
            return;
        }
        UUID defenderKingId = target.king();
        Player defenderKing = defenderKingId == null ? null : Bukkit.getPlayer(defenderKingId);
        if (defenderKing == null || !defenderKing.isOnline()) {
            Messages.error(declarer, target.name() + "'s king must be online for you to declare war on them.");
            return;
        }

        broadcastToKingdom(attacker, attacker.name() + " has declared war on " + target.name()
                + "! Fighting begins in 30 minutes.");
        broadcastToKingdom(target, attacker.name() + " has declared war on your kingdom! Defend your claims — fighting begins in 30 minutes.");

        UUID attackerId = attacker.id();
        UUID targetId = target.id();
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> startWar(attackerId, targetId, key), DECLARATION_NOTICE_TICKS);
        pendingDeclarations.put(key, new PendingDeclaration(attackerId, task));
    }

    /** Only the king of the kingdom that declared the war may call it off, and only before it starts. */
    public void cancelDeclaration(Player player, Kingdom target) {
        Optional<Kingdom> ownOpt = kingdoms.kingdomOf(player.getUniqueId());
        if (ownOpt.isEmpty()) {
            Messages.error(player, "You are not in a kingdom.");
            return;
        }
        Kingdom own = ownOpt.get();
        if (own.rankOf(player.getUniqueId()) != Rank.KING) {
            Messages.error(player, "Only the king can cancel a war declaration.");
            return;
        }
        String key = pairKey(own.id(), target.id());
        PendingDeclaration pending = pendingDeclarations.get(key);
        if (pending == null) {
            Messages.error(player, "There is no pending war declaration between your kingdoms.");
            return;
        }
        if (!pending.attackerId().equals(own.id())) {
            Messages.error(player, "Only the kingdom that declared the war can cancel it.");
            return;
        }
        pendingDeclarations.remove(key);
        pending.task().cancel();
        String message = own.name() + " has called off its war declaration against " + target.name() + ".";
        broadcastToKingdom(own, message);
        broadcastToKingdom(target, message);
    }

    /**
     * Either side of a war can end it unilaterally, resetting both directions
     * to NEUTRAL. Only the defender pays a price for it: if the kingdom
     * calling surrender is the one that originally declared this war (see
     * {@link #declareWar}), it just calls off the fight and keeps its land.
     * Otherwise it's a real capitulation, and it strips claimLossPercent% of
     * the surrendering kingdom's claims (from its territory's edges inward) -
     * see {@link KingdomManager#applySurrenderPenalty}. A war that was already
     * active before attacker-tracking existed has no recorded declarer, so it
     * falls back to the old rule of whoever surrenders paying the penalty.
     */
    public void surrender(Player player, Kingdom target, int claimLossPercent) {
        Optional<Kingdom> ownOpt = kingdoms.kingdomOf(player.getUniqueId());
        if (ownOpt.isEmpty()) {
            Messages.error(player, "You are not in a kingdom.");
            return;
        }
        Kingdom own = ownOpt.get();
        if (own.id().equals(target.id())) {
            Messages.error(player, "You cannot surrender to your own kingdom.");
            return;
        }
        if (own.rankOf(player.getUniqueId()) != Rank.KING) {
            Messages.error(player, "Only the king can surrender.");
            return;
        }
        if (!isActiveWar(own, target)) {
            Messages.error(player, "You are not at war with " + target.name() + ".");
            return;
        }
        boolean ownDeclaredThisWar = own.warsDeclared().contains(target.id());
        kingdoms.setRelation(own, target, RelationType.NEUTRAL);
        kingdoms.setRelation(target, own, RelationType.NEUTRAL);
        own.warsDeclared().remove(target.id());
        target.warsDeclared().remove(own.id());

        String message;
        if (ownDeclaredThisWar) {
            message = own.name() + " has called off its war against " + target.name() + ". No claims change hands.";
        } else {
            int lost = kingdoms.applySurrenderPenalty(own, claimLossPercent);
            message = own.name() + " has surrendered to " + target.name() + ". The war is over.";
            if (lost > 0) {
                message += " " + own.name() + " ceded " + lost + " border chunk(s) and its claim limit is"
                        + " reduced until an admin lifts the penalty.";
            }
        }
        broadcastToKingdom(own, message);
        broadcastToKingdom(target, message);
    }

    private void startWar(UUID attackerId, UUID defenderId, String key) {
        pendingDeclarations.remove(key);
        Optional<Kingdom> attackerOpt = kingdoms.byId(attackerId);
        Optional<Kingdom> defenderOpt = kingdoms.byId(defenderId);
        if (attackerOpt.isEmpty() || defenderOpt.isEmpty()) {
            return; // one side disbanded during the notice period
        }
        Kingdom attacker = attackerOpt.get();
        Kingdom defender = defenderOpt.get();
        kingdoms.setRelation(attacker, defender, RelationType.WAR);
        kingdoms.setRelation(defender, attacker, RelationType.WAR);
        attacker.warsDeclared().add(defender.id());
        broadcastToKingdom(attacker, "War with " + defender.name() + " has begun!");
        broadcastToKingdom(defender, "War with " + attacker.name() + " has begun! They may now breach your claims with TNT.");
    }

    private void broadcastToKingdom(Kingdom kingdom, String message) {
        for (UUID member : kingdom.members().keySet()) {
            Player player = Bukkit.getPlayer(member);
            if (player != null) {
                Messages.info(player, message);
            }
        }
    }

    private String pairKey(UUID a, UUID b) {
        return a.compareTo(b) < 0 ? a + ":" + b : b + ":" + a;
    }
}

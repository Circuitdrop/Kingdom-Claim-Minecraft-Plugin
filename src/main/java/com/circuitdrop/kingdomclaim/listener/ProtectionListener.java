package com.circuitdrop.kingdomclaim.listener;

import com.circuitdrop.kingdomclaim.manager.KingdomManager;
import com.circuitdrop.kingdomclaim.manager.PlayerDataManager;
import com.circuitdrop.kingdomclaim.model.ClaimChunk;
import com.circuitdrop.kingdomclaim.model.Kingdom;
import com.circuitdrop.kingdomclaim.model.Rank;
import com.circuitdrop.kingdomclaim.model.RelationType;
import com.circuitdrop.kingdomclaim.util.Diplomacy;
import com.circuitdrop.kingdomclaim.util.Messages;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.data.Openable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Optional;
import java.util.UUID;

/**
 * Enforces claim protection: only kingdom members (officer+) may break/place
 * blocks or use containers/doors inside their own claim, and PvP inside a claim
 * is only allowed when the attacker's kingdom is at ENEMY/WAR with the defender's
 * (or the claim owner's) kingdom.
 */
public class ProtectionListener implements Listener {

    private final KingdomManager kingdoms;
    private final PlayerDataManager playerData;

    public ProtectionListener(KingdomManager kingdoms, PlayerDataManager playerData) {
        this.kingdoms = kingdoms;
        this.playerData = playerData;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!canBuild(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            Messages.error(event.getPlayer(), "This land belongs to another kingdom.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!canBuild(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            Messages.error(event.getPlayer(), "This land belongs to another kingdom.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (!canBuild(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            Messages.error(event.getPlayer(), "This land belongs to another kingdom.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        boolean protectedBlock = block.getState() instanceof Container || block.getBlockData() instanceof Openable;
        if (!protectedBlock) {
            return;
        }
        if (!canBuild(event.getPlayer(), block.getLocation())) {
            event.setCancelled(true);
            Messages.error(event.getPlayer(), "This land belongs to another kingdom.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Player attacker = resolveAttacker(event);
        if (attacker == null || attacker.equals(victim)) {
            return;
        }
        ClaimChunk chunk = ClaimChunk.of(victim.getLocation());
        Optional<Kingdom> owner = kingdoms.kingdomAt(chunk);
        if (owner.isEmpty()) {
            return; // wilderness: PvP always allowed
        }
        if (playerData.isBypassing(attacker.getUniqueId())) {
            return;
        }
        Optional<Kingdom> attackerKingdom = kingdoms.kingdomOf(attacker.getUniqueId());
        if (attackerKingdom.isPresent() && attackerKingdom.get().id().equals(owner.get().id())) {
            event.setCancelled(true);
            Messages.error(attacker, "You cannot attack fellow kingdom members on your own land.");
            return;
        }
        RelationType relation = attackerKingdom.map(k -> Diplomacy.effectiveRelation(k, owner.get())).orElse(RelationType.NEUTRAL);
        if (!relation.pvpAllowed()) {
            event.setCancelled(true);
            Messages.error(attacker, "PvP is disabled here — your kingdom is not at war with " + owner.get().name() + ".");
        }
    }

    private Player resolveAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            return player;
        }
        if (event.getDamager() instanceof org.bukkit.entity.Projectile projectile
                && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }
        return null;
    }

    /**
     * True if the player may break/place/use blocks at the given location: either the
     * chunk is unclaimed, they are an officer+ member of the owning kingdom, they have
     * bypass toggled on, or the claim is currently raidable (owner's kingdom at WAR with theirs).
     */
    private boolean canBuild(Player player, org.bukkit.Location location) {
        if (playerData.isBypassing(player.getUniqueId())) {
            return true;
        }
        ClaimChunk chunk = ClaimChunk.of(location);
        Optional<Kingdom> ownerOpt = kingdoms.kingdomAt(chunk);
        if (ownerOpt.isEmpty()) {
            return true;
        }
        Kingdom owner = ownerOpt.get();
        UUID playerId = player.getUniqueId();
        if (owner.isMember(playerId) && owner.rankOf(playerId).atLeast(Rank.OFFICER)) {
            return true;
        }
        Optional<Kingdom> playerKingdom = kingdoms.kingdomOf(playerId);
        return playerKingdom.isPresent() && Diplomacy.effectiveRelation(owner, playerKingdom.get()).claimsRaidable();
    }
}

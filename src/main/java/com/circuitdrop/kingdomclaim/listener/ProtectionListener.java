package com.circuitdrop.kingdomclaim.listener;

import com.circuitdrop.kingdomclaim.manager.KingdomManager;
import com.circuitdrop.kingdomclaim.manager.PlayerDataManager;
import com.circuitdrop.kingdomclaim.manager.WarManager;
import com.circuitdrop.kingdomclaim.model.ClaimChunk;
import com.circuitdrop.kingdomclaim.model.Kingdom;
import com.circuitdrop.kingdomclaim.model.RelationType;
import com.circuitdrop.kingdomclaim.util.Diplomacy;
import com.circuitdrop.kingdomclaim.util.Messages;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Enforces claim protection.
 * <p>
 * At peace, only members of the owning kingdom may break/place blocks,
 * use buckets, or right-click any block in a claim — there is no
 * officer-only carve-out for everyday building.
 * <p>
 * During an active WAR (see {@link WarManager}), the attacking kingdom's
 * members gain exactly one privilege inside the defender's claims: placing
 * and lighting TNT. They can still not break blocks directly, interact with
 * anything, or place any other block — the only way in is blowing a way in.
 * TNT placed/lit under that privilege is tagged so only its own blast (and
 * only against that specific defender's claims) is allowed to remove blocks;
 * every other explosion inside a claim (creepers, unrelated TNT, blast
 * spill onto an unrelated claim) is neutralized.
 */
public class ProtectionListener implements Listener {

    private static final long RAID_AUTHORIZATION_WINDOW_MS = 15_000L;

    private final KingdomManager kingdoms;
    private final PlayerDataManager playerData;
    private final WarManager warManager;
    private final Map<Block, RaidAuthorization> raidAuthorizations = new HashMap<>();

    public ProtectionListener(KingdomManager kingdoms, PlayerDataManager playerData, WarManager warManager) {
        this.kingdoms = kingdoms;
        this.playerData = playerData;
        this.warManager = warManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
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
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        if (playerData.isBypassing(player.getUniqueId())) {
            return;
        }
        Optional<Kingdom> ownerOpt = kingdoms.kingdomAt(ClaimChunk.of(block.getLocation()));
        if (ownerOpt.isEmpty()) {
            return;
        }
        Kingdom owner = ownerOpt.get();
        if (owner.isMember(player.getUniqueId())) {
            return;
        }
        if (block.getType() == Material.TNT && atWarWith(player, owner)) {
            return; // sole exception: attackers may place TNT during an active war
        }
        event.setCancelled(true);
        Messages.error(player, "This land belongs to another kingdom.");
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
        Player player = event.getPlayer();
        if (playerData.isBypassing(player.getUniqueId())) {
            return;
        }
        Optional<Kingdom> ownerOpt = kingdoms.kingdomAt(ClaimChunk.of(block.getLocation()));
        if (ownerOpt.isEmpty()) {
            return;
        }
        Kingdom owner = ownerOpt.get();
        if (owner.isMember(player.getUniqueId())) {
            return;
        }
        if (block.getType() == Material.TNT && atWarWith(player, owner)) {
            return; // let the interaction through so BlockIgniteEvent can fire and be authorized below
        }
        event.setCancelled(true);
        Messages.error(player, "This land belongs to another kingdom.");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent event) {
        if (event.getBlock().getType() != Material.TNT) {
            return;
        }
        Player player = event.getPlayer();
        if (player == null) {
            // Non-player ignition (redstone, fire spread, chain explosion): only ever
            // allowed on a block we've already authorized as part of an ongoing raid.
            Optional<Kingdom> ownerOpt = kingdoms.kingdomAt(ClaimChunk.of(event.getBlock().getLocation()));
            if (ownerOpt.isPresent() && !isAuthorized(event.getBlock())) {
                event.setCancelled(true);
            }
            return;
        }
        if (playerData.isBypassing(player.getUniqueId())) {
            authorizeRaid(event.getBlock(), null);
            return;
        }
        Optional<Kingdom> ownerOpt = kingdoms.kingdomAt(ClaimChunk.of(event.getBlock().getLocation()));
        if (ownerOpt.isEmpty()) {
            return;
        }
        Kingdom owner = ownerOpt.get();
        if (owner.isMember(player.getUniqueId())) {
            return;
        }
        if (atWarWith(player, owner)) {
            authorizeRaid(event.getBlock(), owner.id());
            return;
        }
        event.setCancelled(true);
        Messages.error(player, "You can only light TNT here during an active war.");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        RaidAuthorization auth = event.getEntityType() == EntityType.PRIMED_TNT
                ? consumeAuthorization(event.getEntity().getLocation().getBlock())
                : null;
        event.blockList().removeIf(block -> {
            Optional<Kingdom> ownerOpt = kingdoms.kingdomAt(ClaimChunk.of(block.getLocation()));
            if (ownerOpt.isEmpty()) {
                return false; // unclaimed: never protected
            }
            if (auth == null) {
                return true; // no valid raid authorization: protect everything claimed
            }
            if (auth.defenderKingdomId() == null) {
                return false; // admin-bypass-lit TNT: unrestricted
            }
            return !ownerOpt.get().id().equals(auth.defenderKingdomId()); // only the declared target is breachable
        });
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

    private boolean atWarWith(Player player, Kingdom owner) {
        Optional<Kingdom> attackerKingdom = kingdoms.kingdomOf(player.getUniqueId());
        return attackerKingdom.isPresent() && warManager.isActiveWar(attackerKingdom.get(), owner);
    }

    /** True if the player may break/place blocks or use buckets at the given location. */
    private boolean canBuild(Player player, Location location) {
        if (playerData.isBypassing(player.getUniqueId())) {
            return true;
        }
        Optional<Kingdom> ownerOpt = kingdoms.kingdomAt(ClaimChunk.of(location));
        return ownerOpt.isEmpty() || ownerOpt.get().isMember(player.getUniqueId());
    }

    private void authorizeRaid(Block block, UUID defenderKingdomId) {
        raidAuthorizations.put(block, new RaidAuthorization(defenderKingdomId, System.currentTimeMillis() + RAID_AUTHORIZATION_WINDOW_MS));
    }

    private boolean isAuthorized(Block block) {
        RaidAuthorization auth = raidAuthorizations.get(block);
        return auth != null && auth.expiresAt() >= System.currentTimeMillis();
    }

    private RaidAuthorization consumeAuthorization(Block block) {
        RaidAuthorization auth = raidAuthorizations.remove(block);
        if (auth == null || auth.expiresAt() < System.currentTimeMillis()) {
            return null;
        }
        return auth;
    }

    /** defenderKingdomId is null for an admin-bypass ignition, meaning "unrestricted". */
    private record RaidAuthorization(UUID defenderKingdomId, long expiresAt) {
    }
}

package com.circuitdrop.kingdomclaim.manager;

import com.circuitdrop.kingdomclaim.model.ClaimChunk;
import com.circuitdrop.kingdomclaim.model.Kingdom;
import com.circuitdrop.kingdomclaim.model.Rank;
import com.circuitdrop.kingdomclaim.model.RelationType;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * In-memory source of truth for every kingdom, its claims and its membership.
 * All lookups are O(1) via secondary indexes kept in sync with the primary
 * {@link #kingdomsById} map. Persistence is handled separately by DataManager,
 * which reads/writes this manager's state wholesale.
 */
public class KingdomManager {

    private final Map<UUID, Kingdom> kingdomsById = new HashMap<>();
    private final Map<String, UUID> idByLowercaseName = new HashMap<>();
    private final Map<ClaimChunk, UUID> kingdomByClaim = new HashMap<>();
    private final Map<UUID, UUID> kingdomByPlayer = new HashMap<>();
    private final Map<UUID, Set<UUID>> pendingInvites = new HashMap<>(); // player -> kingdom ids that invited them
    private final TeamManager teamManager;

    public KingdomManager(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    public Kingdom createKingdom(String name, UUID founder) {
        UUID id = UUID.randomUUID();
        Kingdom kingdom = new Kingdom(id, name, founder);
        register(kingdom);
        kingdomByPlayer.put(founder, id);
        return kingdom;
    }

    /** Used by DataManager when rehydrating kingdoms from disk. */
    public void register(Kingdom kingdom) {
        kingdomsById.put(kingdom.id(), kingdom);
        idByLowercaseName.put(kingdom.name().toLowerCase(), kingdom.id());
        for (UUID member : kingdom.members().keySet()) {
            kingdomByPlayer.put(member, kingdom.id());
        }
        for (ClaimChunk claim : kingdom.claims()) {
            kingdomByClaim.put(claim, kingdom.id());
        }
        teamManager.sync(kingdom);
    }

    public void disbandKingdom(Kingdom kingdom) {
        kingdomsById.remove(kingdom.id());
        idByLowercaseName.remove(kingdom.name().toLowerCase());
        kingdom.members().keySet().forEach(kingdomByPlayer::remove);
        kingdom.claims().forEach(kingdomByClaim::remove);
        for (Kingdom other : kingdomsById.values()) {
            other.relations().remove(kingdom.id());
        }
        teamManager.remove(kingdom);
    }

    public void updateColor(Kingdom kingdom, NamedTextColor color) {
        kingdom.setColor(color);
        teamManager.sync(kingdom);
    }

    public void renameKingdom(Kingdom kingdom, String newName) {
        idByLowercaseName.remove(kingdom.name().toLowerCase());
        kingdom.setName(newName);
        idByLowercaseName.put(newName.toLowerCase(), kingdom.id());
    }

    public Optional<Kingdom> byName(String name) {
        return Optional.ofNullable(idByLowercaseName.get(name.toLowerCase())).map(kingdomsById::get);
    }

    public Optional<Kingdom> byId(UUID id) {
        return Optional.ofNullable(kingdomsById.get(id));
    }

    public Optional<Kingdom> kingdomOf(UUID player) {
        return Optional.ofNullable(kingdomByPlayer.get(player)).map(kingdomsById::get);
    }

    public Optional<Kingdom> kingdomAt(ClaimChunk claim) {
        return Optional.ofNullable(kingdomByClaim.get(claim)).map(kingdomsById::get);
    }

    public boolean isNameTaken(String name) {
        return idByLowercaseName.containsKey(name.toLowerCase());
    }

    public Collection<Kingdom> all() {
        return kingdomsById.values();
    }

    public void addMember(Kingdom kingdom, UUID player, Rank rank) {
        kingdom.members().put(player, rank);
        kingdomByPlayer.put(player, kingdom.id());
        clearInvites(player);
        teamManager.sync(kingdom);
    }

    public void removeMember(Kingdom kingdom, UUID player) {
        kingdom.members().remove(player);
        kingdomByPlayer.remove(player);
        teamManager.sync(kingdom);
    }

    public boolean claimChunk(Kingdom kingdom, ClaimChunk chunk) {
        if (kingdomByClaim.containsKey(chunk)) {
            return false;
        }
        kingdom.claims().add(chunk);
        kingdomByClaim.put(chunk, kingdom.id());
        return true;
    }

    public boolean unclaimChunk(Kingdom kingdom, ClaimChunk chunk) {
        if (!kingdom.claims().remove(chunk)) {
            return false;
        }
        kingdomByClaim.remove(chunk);
        return true;
    }

    public void setRelation(Kingdom from, Kingdom to, RelationType type) {
        from.relations().put(to.id(), type);
    }

    public void invite(Kingdom kingdom, UUID player) {
        pendingInvites.computeIfAbsent(player, k -> new HashSet<>()).add(kingdom.id());
    }

    public boolean hasInvite(UUID player, Kingdom kingdom) {
        return pendingInvites.getOrDefault(player, Set.of()).contains(kingdom.id());
    }

    public Set<UUID> invitesFor(UUID player) {
        return pendingInvites.getOrDefault(player, Set.of());
    }

    public void clearInvites(UUID player) {
        pendingInvites.remove(player);
    }

    public void removeInvite(UUID player, Kingdom kingdom) {
        Set<UUID> set = pendingInvites.get(player);
        if (set != null) {
            set.remove(kingdom.id());
            if (set.isEmpty()) {
                pendingInvites.remove(player);
            }
        }
    }
}

package com.circuitdrop.kingdomclaim.model;

import net.kyori.adventure.text.format.NamedTextColor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A player-run kingdom: its membership roster, claimed chunks, and diplomatic
 * relations towards other kingdoms. Instances are mutated in place by the managers
 * and persisted wholesale by {@code DataManager}.
 */
public class Kingdom {

    private final UUID id;
    private String name;
    private final Map<UUID, Rank> members = new LinkedHashMap<>();
    private final Set<ClaimChunk> claims = new HashSet<>();
    private final Map<UUID, RelationType> relations = new HashMap<>();
    private String home; // serialized ClaimChunk-style world;x;y;z;yaw;pitch, nullable
    private NamedTextColor color; // nullable; defaults to white when unset
    // Per-claim "x;y;z;yaw;pitch" of wherever the claimer was standing when they claimed
    // it, so the claims GUI can teleport back there instead of guessing a highest-block Y.
    // A claim with no entry here (loaded from a save made before this existed) falls back
    // to that highest-block guess.
    private final Map<ClaimChunk, String> claimSpawns = new HashMap<>();
    // Chunks subtracted from max-claims-per-kingdom as a war-surrender penalty,
    // on top of whatever chunks were actually stripped. Only an admin can clear it
    // (/kingdom admin resetpenalty), which is the point - it isn't self-service.
    private int claimCapPenalty;

    public Kingdom(UUID id, String name, UUID founder) {
        this.id = id;
        this.name = name;
        this.members.put(founder, Rank.KING);
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<UUID, Rank> members() {
        return members;
    }

    public boolean isMember(UUID player) {
        return members.containsKey(player);
    }

    public Rank rankOf(UUID player) {
        return members.get(player);
    }

    public UUID king() {
        return members.entrySet().stream()
                .filter(e -> e.getValue() == Rank.KING)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    public Set<ClaimChunk> claims() {
        return claims;
    }

    public Map<ClaimChunk, String> claimSpawns() {
        return claimSpawns;
    }

    public Map<UUID, RelationType> relations() {
        return relations;
    }

    public RelationType relationTo(UUID otherKingdomId) {
        return relations.getOrDefault(otherKingdomId, RelationType.NEUTRAL);
    }

    public String home() {
        return home;
    }

    public void setHome(String home) {
        this.home = home;
    }

    public int memberCount() {
        return members.size();
    }

    public NamedTextColor color() {
        return color;
    }

    public void setColor(NamedTextColor color) {
        this.color = color;
    }

    public int claimCapPenalty() {
        return claimCapPenalty;
    }

    public void setClaimCapPenalty(int claimCapPenalty) {
        this.claimCapPenalty = claimCapPenalty;
    }
}

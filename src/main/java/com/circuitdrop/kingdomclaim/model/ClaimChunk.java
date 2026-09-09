package com.circuitdrop.kingdomclaim.model;

import org.bukkit.Chunk;
import org.bukkit.Location;

/**
 * Identifies a single claimed chunk by world name and chunk coordinates.
 */
public record ClaimChunk(String world, int x, int z) {

    public static ClaimChunk of(Chunk chunk) {
        return new ClaimChunk(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
    }

    public static ClaimChunk of(Location location) {
        return new ClaimChunk(location.getWorld().getName(), location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    public String serialize() {
        return world + ";" + x + ";" + z;
    }

    public static ClaimChunk deserialize(String raw) {
        String[] parts = raw.split(";");
        return new ClaimChunk(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }

    /** Center of the chunk at a safe-ish reference Y, used for GUI teleport targets. */
    public int centerBlockX() {
        return (x << 4) + 8;
    }

    public int centerBlockZ() {
        return (z << 4) + 8;
    }
}

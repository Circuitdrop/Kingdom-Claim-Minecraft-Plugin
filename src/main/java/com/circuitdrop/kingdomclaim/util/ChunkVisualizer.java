package com.circuitdrop.kingdomclaim.util;

import com.circuitdrop.kingdomclaim.model.ClaimChunk;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;

/**
 * Draws claim chunk borders as particles visible only to the requesting player
 * (via the per-player {@code Player#spawnParticle} overload, so this never spams
 * other players nearby). Used by "/kingdom visualize" and the claim GUI.
 */
public final class ChunkVisualizer {

    private ChunkVisualizer() {
    }

    public static BukkitTask visualize(Plugin plugin, Player player, Collection<ClaimChunk> chunks,
                                        Color color, int durationSeconds) {
        Particle.DustOptions dust = new Particle.DustOptions(color, 1.2f);
        BukkitRunnable task = new BukkitRunnable() {
            int ticksRemaining = durationSeconds * 20;

            @Override
            public void run() {
                if (!player.isOnline() || ticksRemaining <= 0) {
                    cancel();
                    return;
                }
                for (ClaimChunk chunk : chunks) {
                    if (chunk.world().equals(player.getWorld().getName())) {
                        drawBorder(player, chunk, dust);
                    }
                }
                ticksRemaining -= 10;
            }
        };
        return task.runTaskTimer(plugin, 0L, 10L);
    }

    private static void drawBorder(Player player, ClaimChunk chunk, Particle.DustOptions dust) {
        double minX = chunk.x() << 4;
        double minZ = chunk.z() << 4;
        double maxX = minX + 16;
        double maxZ = minZ + 16;
        double y = player.getLocation().getY() + 1;

        double step = 1.0;
        for (double x = minX; x <= maxX; x += step) {
            spawn(player, x, y, minZ, dust);
            spawn(player, x, y, maxZ, dust);
        }
        for (double z = minZ; z <= maxZ; z += step) {
            spawn(player, minX, y, z, dust);
            spawn(player, maxX, y, z, dust);
        }
        for (double dy = -3; dy <= 3; dy += 1.0) {
            spawn(player, minX, y + dy, minZ, dust);
            spawn(player, minX, y + dy, maxZ, dust);
            spawn(player, maxX, y + dy, minZ, dust);
            spawn(player, maxX, y + dy, maxZ, dust);
        }
    }

    private static void spawn(Player player, double x, double y, double z, Particle.DustOptions dust) {
        player.spawnParticle(Particle.DUST, x, y, z, 1, 0, 0, 0, 0, dust);
    }
}

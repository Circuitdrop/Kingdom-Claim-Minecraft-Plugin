package com.circuitdrop.kingdomclaim.manager;

import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Transient, non-persisted per-player state: kingdom-chat toggle and the
 * currently running claim-visualization task (so a second /k visualize cancels the first).
 */
public class PlayerDataManager {

    private final Set<UUID> kingdomChatEnabled = new HashSet<>();
    private final Set<UUID> protectionBypass = new HashSet<>();
    private final Map<UUID, BukkitTask> visualizerTasks = new HashMap<>();

    public boolean isKingdomChatEnabled(UUID player) {
        return kingdomChatEnabled.contains(player);
    }

    public void toggleKingdomChat(UUID player) {
        if (!kingdomChatEnabled.remove(player)) {
            kingdomChatEnabled.add(player);
        }
    }

    public boolean isBypassing(UUID player) {
        return protectionBypass.contains(player);
    }

    public boolean toggleBypass(UUID player) {
        if (!protectionBypass.remove(player)) {
            protectionBypass.add(player);
            return true;
        }
        return false;
    }

    public void setVisualizerTask(UUID player, BukkitTask task) {
        cancelVisualizer(player);
        visualizerTasks.put(player, task);
    }

    public void cancelVisualizer(UUID player) {
        BukkitTask existing = visualizerTasks.remove(player);
        if (existing != null) {
            existing.cancel();
        }
    }

    public void clear(UUID player) {
        kingdomChatEnabled.remove(player);
        protectionBypass.remove(player);
        cancelVisualizer(player);
    }
}

package com.circuitdrop.kingdomclaim.listener;

import com.circuitdrop.kingdomclaim.manager.KingdomManager;
import com.circuitdrop.kingdomclaim.manager.PlayerDataManager;
import com.circuitdrop.kingdomclaim.util.Messages;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerLifecycleListener implements Listener {

    private final KingdomManager kingdoms;
    private final PlayerDataManager playerData;

    public PlayerLifecycleListener(KingdomManager kingdoms, PlayerDataManager playerData) {
        this.kingdoms = kingdoms;
        this.playerData = playerData;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        int invites = kingdoms.invitesFor(event.getPlayer().getUniqueId()).size();
        if (invites > 0) {
            Messages.info(event.getPlayer(), "You have " + invites + " pending kingdom invite(s). Use /kingdom invites to view them.");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        playerData.clear(event.getPlayer().getUniqueId());
    }
}

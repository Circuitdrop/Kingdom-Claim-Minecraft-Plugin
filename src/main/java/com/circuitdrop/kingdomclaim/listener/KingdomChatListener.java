package com.circuitdrop.kingdomclaim.listener;

import com.circuitdrop.kingdomclaim.manager.KingdomManager;
import com.circuitdrop.kingdomclaim.manager.PlayerDataManager;
import com.circuitdrop.kingdomclaim.model.Kingdom;
import com.circuitdrop.kingdomclaim.util.Messages;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Optional;

/**
 * Redirects chat to kingdom-only when a player has kingdom chat toggled on
 * (via "/kingdom chat"). Messages never leave the sender's kingdom.
 */
public class KingdomChatListener implements Listener {

    private final KingdomManager kingdoms;
    private final PlayerDataManager playerData;

    public KingdomChatListener(KingdomManager kingdoms, PlayerDataManager playerData) {
        this.kingdoms = kingdoms;
        this.playerData = playerData;
    }

    @EventHandler(ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player sender = event.getPlayer();
        if (!playerData.isKingdomChatEnabled(sender.getUniqueId())) {
            return;
        }
        Optional<Kingdom> kingdomOpt = kingdoms.kingdomOf(sender.getUniqueId());
        if (kingdomOpt.isEmpty()) {
            playerData.toggleKingdomChat(sender.getUniqueId());
            Messages.error(sender, "You are no longer in a kingdom — kingdom chat turned off.");
            return;
        }
        event.setCancelled(true);
        Kingdom kingdom = kingdomOpt.get();
        Component formatted = Component.text("[K] ", NamedTextColor.GOLD)
                .append(Component.text(sender.getName() + ": ", NamedTextColor.YELLOW))
                .append(event.message().color(NamedTextColor.WHITE));

        for (Player online : Bukkit.getOnlinePlayers()) {
            kingdoms.kingdomOf(online.getUniqueId())
                    .filter(k -> k.id().equals(kingdom.id()))
                    .ifPresent(k -> online.sendMessage(formatted));
        }
    }
}

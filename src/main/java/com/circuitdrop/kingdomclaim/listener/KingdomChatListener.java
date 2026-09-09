package com.circuitdrop.kingdomclaim.listener;

import com.circuitdrop.kingdomclaim.manager.KingdomManager;
import com.circuitdrop.kingdomclaim.manager.PlayerDataManager;
import com.circuitdrop.kingdomclaim.manager.TeamManager;
import com.circuitdrop.kingdomclaim.model.Kingdom;
import com.circuitdrop.kingdomclaim.model.Rank;
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
 * Two responsibilities: redirect chat to kingdom-only when a player has
 * kingdom chat toggled on (via "/kingdom chat"), and — regardless of that
 * toggle — render every kingdom member's name in their kingdom's chosen
 * color, with a crown prefix for the king, everywhere chat shows a name.
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
        Optional<Kingdom> kingdomOpt = kingdoms.kingdomOf(sender.getUniqueId());

        if (playerData.isKingdomChatEnabled(sender.getUniqueId())) {
            if (kingdomOpt.isEmpty()) {
                playerData.toggleKingdomChat(sender.getUniqueId());
                Messages.error(sender, "You are no longer in a kingdom — kingdom chat turned off.");
                return;
            }
            event.setCancelled(true);
            Kingdom kingdom = kingdomOpt.get();
            Component formatted = Component.text("[K] ", NamedTextColor.GOLD)
                    .append(nameComponent(sender, kingdom))
                    .append(Component.text(": ", NamedTextColor.GRAY))
                    .append(event.message().color(NamedTextColor.WHITE));

            for (Player online : Bukkit.getOnlinePlayers()) {
                kingdoms.kingdomOf(online.getUniqueId())
                        .filter(k -> k.id().equals(kingdom.id()))
                        .ifPresent(k -> online.sendMessage(formatted));
            }
            return;
        }

        Component name = kingdomOpt.map(k -> nameComponent(sender, k))
                .orElse(Component.text(sender.getName(), NamedTextColor.WHITE));
        event.renderer((source, sourceDisplayName, message, viewer) ->
                Component.text("<").append(name).append(Component.text("> ")).append(message));
    }

    private Component nameComponent(Player player, Kingdom kingdom) {
        NamedTextColor color = kingdom.color() != null ? kingdom.color() : NamedTextColor.WHITE;
        Component name = Component.text(player.getName(), color);
        return kingdom.rankOf(player.getUniqueId()) == Rank.KING ? TeamManager.CROWN.append(name) : name;
    }
}

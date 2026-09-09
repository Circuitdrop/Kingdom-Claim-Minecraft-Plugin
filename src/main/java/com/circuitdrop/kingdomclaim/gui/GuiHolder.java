package com.circuitdrop.kingdomclaim.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.InventoryHolder;

/**
 * Marker interface implemented by every KingdomClaim menu's inventory holder.
 * {@code GuiListener} dispatches all clicks in a GUI inventory to
 * {@link #onClick}, cancelling the event beforehand so nothing can be taken.
 */
public interface GuiHolder extends InventoryHolder {

    void onClick(Player player, int slot, ClickType clickType);
}

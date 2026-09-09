package com.circuitdrop.kingdomclaim.gui;

import com.circuitdrop.kingdomclaim.KingdomClaimPlugin;
import com.circuitdrop.kingdomclaim.model.Kingdom;
import com.circuitdrop.kingdomclaim.model.Rank;
import com.circuitdrop.kingdomclaim.util.ItemBuilder;
import com.circuitdrop.kingdomclaim.util.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Member roster with in-place rank management: left-click promotes,
 * right-click demotes, shift-click kicks — all gated by the acting player's
 * rank outranking the target's (kingship transfer stays a deliberate command,
 * not a click, to avoid accidental handoffs).
 */
public class MembersGui implements GuiHolder {

    private final KingdomClaimPlugin plugin;
    private final Kingdom kingdom;
    private final Inventory inventory;
    private final List<UUID> orderedMembers;

    private MembersGui(KingdomClaimPlugin plugin, Kingdom kingdom) {
        this.plugin = plugin;
        this.kingdom = kingdom;
        this.orderedMembers = new ArrayList<>(kingdom.members().keySet());
        this.inventory = Bukkit.createInventory(this, 54,
                Component.text(kingdom.name() + " — Members", NamedTextColor.AQUA, TextDecoration.BOLD));
        populate();
    }

    public static void open(Player player, Kingdom kingdom, KingdomClaimPlugin plugin) {
        player.openInventory(new MembersGui(plugin, kingdom).inventory);
    }

    private void populate() {
        for (int i = 0; i < orderedMembers.size() && i < 45; i++) {
            UUID member = orderedMembers.get(i);
            Rank rank = kingdom.rankOf(member);
            String name = Bukkit.getOfflinePlayer(member).getName();
            ItemBuilder builder = ItemBuilder.playerHead(member)
                    .name(Component.text((name == null ? "Unknown" : name), NamedTextColor.WHITE))
                    .lore(Component.text("Rank: " + rank.displayName(), NamedTextColor.GRAY));
            if (rank != Rank.KING) {
                builder.lore(Component.text("Left-click: promote", NamedTextColor.GREEN))
                        .lore(Component.text("Right-click: demote", NamedTextColor.YELLOW))
                        .lore(Component.text("Shift-click: kick", NamedTextColor.RED));
            }
            inventory.setItem(i, builder.build());
        }
        inventory.setItem(49, new ItemBuilder(Material.BARRIER).name(Component.text("Back", NamedTextColor.YELLOW)).build());

        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(Component.empty()).build();
        for (int slot = 45; slot < 54; slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler);
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void onClick(Player player, int slot, ClickType clickType) {
        if (slot == 49) {
            MainMenuGui.open(player, kingdom, plugin);
            return;
        }
        if (slot >= orderedMembers.size() || slot >= 45) {
            return;
        }
        UUID target = orderedMembers.get(slot);
        Rank actorRank = kingdom.rankOf(player.getUniqueId());
        Rank targetRank = kingdom.rankOf(target);
        if (actorRank == null || targetRank == null || !actorRank.atLeast(Rank.OFFICER)) {
            Messages.error(player, "Only officers and the king can manage members.");
            return;
        }
        if (target.equals(player.getUniqueId()) || targetRank.atLeast(actorRank)) {
            Messages.error(player, "You cannot manage a member of equal or higher rank.");
            return;
        }

        if (clickType.isShiftClick()) {
            plugin.getKingdomManager().removeMember(kingdom, target);
            Messages.success(player, "Kicked " + nameOf(target) + " from the kingdom.");
        } else if (clickType.isRightClick()) {
            if (targetRank == Rank.OFFICER) {
                plugin.getKingdomManager().addMember(kingdom, target, Rank.MEMBER);
                Messages.success(player, "Demoted " + nameOf(target) + " to Member.");
            }
        } else {
            if (targetRank == Rank.MEMBER) {
                plugin.getKingdomManager().addMember(kingdom, target, Rank.OFFICER);
                Messages.success(player, "Promoted " + nameOf(target) + " to Officer.");
            }
        }
        open(player, kingdom, plugin);
    }

    private String nameOf(UUID uuid) {
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return name == null ? uuid.toString() : name;
    }
}

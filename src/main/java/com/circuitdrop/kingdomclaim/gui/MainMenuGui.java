package com.circuitdrop.kingdomclaim.gui;

import com.circuitdrop.kingdomclaim.KingdomClaimPlugin;
import com.circuitdrop.kingdomclaim.model.Kingdom;
import com.circuitdrop.kingdomclaim.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Landing screen opened by "/kingdom gui": member roster, claim list and
 * relations, each one click away.
 */
public class MainMenuGui implements GuiHolder {

    private static final int SIZE = 27;

    private final KingdomClaimPlugin plugin;
    private final Kingdom kingdom;
    private final Inventory inventory;

    private MainMenuGui(KingdomClaimPlugin plugin, Kingdom kingdom) {
        this.plugin = plugin;
        this.kingdom = kingdom;
        this.inventory = Bukkit.createInventory(this, SIZE, Component.text(kingdom.name(), NamedTextColor.GOLD, TextDecoration.BOLD));
        populate();
    }

    public static void open(Player player, Kingdom kingdom, KingdomClaimPlugin plugin) {
        player.openInventory(new MainMenuGui(plugin, kingdom).inventory);
    }

    private void populate() {
        inventory.setItem(11, new ItemBuilder(Material.PLAYER_HEAD)
                .name(Component.text("Members", NamedTextColor.AQUA, TextDecoration.BOLD))
                .lore(Component.text(kingdom.memberCount() + " member(s) — click to manage", NamedTextColor.GRAY))
                .build());

        inventory.setItem(13, new ItemBuilder(Material.MAP)
                .name(Component.text("Claims", NamedTextColor.GREEN, TextDecoration.BOLD))
                .lore(Component.text(kingdom.claims().size() + " claimed chunk(s) — click to manage", NamedTextColor.GRAY))
                .build());

        inventory.setItem(15, new ItemBuilder(Material.IRON_SWORD)
                .name(Component.text("Relations", NamedTextColor.RED, TextDecoration.BOLD))
                .lore(Component.text("View and change diplomacy", NamedTextColor.GRAY))
                .build());

        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(Component.empty()).build();
        for (int slot = 0; slot < SIZE; slot++) {
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
        switch (slot) {
            case 11 -> MembersGui.open(player, kingdom, plugin);
            case 13 -> ClaimListGui.open(player, kingdom, plugin);
            case 15 -> RelationGui.open(player, kingdom, plugin);
            default -> {
            }
        }
    }
}

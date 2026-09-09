package com.circuitdrop.kingdomclaim.gui;

import com.circuitdrop.kingdomclaim.KingdomClaimPlugin;
import com.circuitdrop.kingdomclaim.model.Kingdom;
import com.circuitdrop.kingdomclaim.model.Rank;
import com.circuitdrop.kingdomclaim.model.RelationType;
import com.circuitdrop.kingdomclaim.util.Diplomacy;
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

/**
 * Diplomacy screen: shows every other kingdom colored by the effective
 * relation (see {@link Diplomacy}) and lets officers/king change their own
 * kingdom's declared stance towards it.
 */
public class RelationGui implements GuiHolder {

    private final KingdomClaimPlugin plugin;
    private final Kingdom kingdom;
    private final Inventory inventory;
    private final List<Kingdom> others;

    private RelationGui(KingdomClaimPlugin plugin, Kingdom kingdom) {
        this.plugin = plugin;
        this.kingdom = kingdom;
        this.others = new ArrayList<>();
        for (Kingdom other : plugin.getKingdomManager().all()) {
            if (!other.id().equals(kingdom.id())) {
                others.add(other);
            }
        }
        this.inventory = Bukkit.createInventory(this, 54,
                Component.text(kingdom.name() + " — Relations", NamedTextColor.RED, TextDecoration.BOLD));
        populate();
    }

    public static void open(Player player, Kingdom kingdom, KingdomClaimPlugin plugin) {
        player.openInventory(new RelationGui(plugin, kingdom).inventory);
    }

    private void populate() {
        for (int i = 0; i < others.size() && i < 45; i++) {
            Kingdom other = others.get(i);
            RelationType effective = Diplomacy.effectiveRelation(kingdom, other);
            inventory.setItem(i, new ItemBuilder(materialFor(effective))
                    .name(Component.text(other.name(), NamedTextColor.WHITE, TextDecoration.BOLD))
                    .lore(Component.text("Relation: ", NamedTextColor.GRAY).append(Component.text(effective.name(), effective.color())))
                    .lore(Component.text("Left-click: propose Ally", NamedTextColor.GREEN))
                    .lore(Component.text("Right-click: set Neutral", NamedTextColor.YELLOW))
                    .lore(Component.text("Shift-left: declare Enemy", NamedTextColor.GOLD))
                    .lore(Component.text("Shift-right: declare War", NamedTextColor.DARK_RED))
                    .build());
        }
        inventory.setItem(49, new ItemBuilder(Material.BARRIER).name(Component.text("Back", NamedTextColor.YELLOW)).build());

        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(Component.empty()).build();
        for (int slot = 45; slot < 54; slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler);
            }
        }
    }

    private Material materialFor(RelationType type) {
        return switch (type) {
            case ALLY -> Material.LIME_WOOL;
            case NEUTRAL -> Material.YELLOW_WOOL;
            case ENEMY -> Material.ORANGE_WOOL;
            case WAR -> Material.RED_WOOL;
        };
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
        if (slot >= others.size() || slot >= 45) {
            return;
        }
        Rank actorRank = kingdom.rankOf(player.getUniqueId());
        if (actorRank == null || !actorRank.atLeast(Rank.OFFICER)) {
            Messages.error(player, "Only officers and the king can change relations.");
            return;
        }
        Kingdom other = others.get(slot);
        RelationType newRelation = switch (clickType) {
            case LEFT -> RelationType.ALLY;
            case RIGHT -> RelationType.NEUTRAL;
            case SHIFT_LEFT -> RelationType.ENEMY;
            case SHIFT_RIGHT -> RelationType.WAR;
            default -> null;
        };
        if (newRelation == null) {
            return;
        }
        plugin.getKingdomManager().setRelation(kingdom, other, newRelation);
        Messages.success(player, kingdom.name() + " now considers " + other.name() + ": " + newRelation.name());
        open(player, kingdom, plugin);
    }
}

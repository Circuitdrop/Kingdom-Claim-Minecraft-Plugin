package com.circuitdrop.kingdomclaim.gui;

import com.circuitdrop.kingdomclaim.KingdomClaimPlugin;
import com.circuitdrop.kingdomclaim.model.ClaimChunk;
import com.circuitdrop.kingdomclaim.model.Kingdom;
import com.circuitdrop.kingdomclaim.model.Rank;
import com.circuitdrop.kingdomclaim.util.ChunkVisualizer;
import com.circuitdrop.kingdomclaim.util.ItemBuilder;
import com.circuitdrop.kingdomclaim.util.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Paginated list of a kingdom's claimed chunks: left-click teleports to the
 * chunk center, shift-click (officer+) unclaims it, and the bottom bar can
 * visualize every claim in-world at once via {@link ChunkVisualizer}.
 */
public class ClaimListGui implements GuiHolder {

    private static final int PAGE_SIZE = 45;

    private final KingdomClaimPlugin plugin;
    private final Kingdom kingdom;
    private final int page;
    private final List<ClaimChunk> claims;
    private final Inventory inventory;

    private ClaimListGui(KingdomClaimPlugin plugin, Kingdom kingdom, int page) {
        this.plugin = plugin;
        this.kingdom = kingdom;
        this.page = page;
        this.claims = new ArrayList<>(kingdom.claims());
        this.inventory = Bukkit.createInventory(this, 54,
                Component.text(kingdom.name() + " — Claims (page " + (page + 1) + ")", NamedTextColor.GREEN, TextDecoration.BOLD));
        populate();
    }

    public static void open(Player player, Kingdom kingdom, KingdomClaimPlugin plugin) {
        openPage(player, kingdom, plugin, 0);
    }

    public static void openPage(Player player, Kingdom kingdom, KingdomClaimPlugin plugin, int page) {
        player.openInventory(new ClaimListGui(plugin, kingdom, page).inventory);
    }

    private void populate() {
        int from = page * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, claims.size());
        for (int i = from; i < to; i++) {
            ClaimChunk claim = claims.get(i);
            inventory.setItem(i - from, new ItemBuilder(Material.GRASS_BLOCK)
                    .name(Component.text(claim.world() + " (" + claim.x() + ", " + claim.z() + ")", NamedTextColor.WHITE))
                    .lore(Component.text("Left-click: teleport", NamedTextColor.GRAY))
                    .lore(Component.text("Shift-click: unclaim (officer+)", NamedTextColor.GRAY))
                    .build());
        }

        if (page > 0) {
            inventory.setItem(45, navItem(Material.ARROW, "Previous page"));
        }
        if (to < claims.size()) {
            inventory.setItem(53, navItem(Material.ARROW, "Next page"));
        }
        inventory.setItem(49, navItem(Material.BARRIER, "Back"));
        inventory.setItem(48, navItem(Material.BLAZE_POWDER, "Visualize all claims"));

        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(Component.empty()).build();
        for (int slot = 45; slot < 54; slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler);
            }
        }
    }

    private ItemStack navItem(Material material, String name) {
        return new ItemBuilder(material).name(Component.text(name, NamedTextColor.YELLOW)).build();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void onClick(Player player, int slot, ClickType clickType) {
        if (slot == 45 && page > 0) {
            openPage(player, kingdom, plugin, page - 1);
            return;
        }
        if (slot == 53) {
            openPage(player, kingdom, plugin, page + 1);
            return;
        }
        if (slot == 49) {
            MainMenuGui.open(player, kingdom, plugin);
            return;
        }
        if (slot == 48) {
            ChunkVisualizer.visualize(plugin, player, kingdom.claims(), Color.LIME, 15);
            Messages.success(player, "Visualizing " + kingdom.claims().size() + " claim(s) for 15 seconds.");
            return;
        }
        int index = page * PAGE_SIZE + slot;
        if (slot >= 45 || index >= claims.size()) {
            return;
        }
        ClaimChunk claim = claims.get(index);
        if (clickType.isShiftClick()) {
            handleUnclaim(player, claim);
        } else {
            handleTeleport(player, claim);
        }
    }

    private void handleUnclaim(Player player, ClaimChunk claim) {
        Rank rank = kingdom.rankOf(player.getUniqueId());
        if (rank == null || !rank.atLeast(Rank.OFFICER)) {
            Messages.error(player, "Only officers and the king can unclaim land.");
            return;
        }
        plugin.getKingdomManager().unclaimChunk(kingdom, claim);
        Messages.success(player, "Unclaimed chunk (" + claim.x() + ", " + claim.z() + ").");
        openPage(player, kingdom, plugin, page);
    }

    private void handleTeleport(Player player, ClaimChunk claim) {
        World world = Bukkit.getWorld(claim.world());
        if (world == null) {
            Messages.error(player, "That world is not currently loaded.");
            return;
        }
        int x = claim.centerBlockX();
        int z = claim.centerBlockZ();
        int y = world.getHighestBlockYAt(x, z) + 1;
        player.closeInventory();
        player.teleportAsync(new Location(world, x + 0.5, y, z + 0.5));
        Messages.success(player, "Teleported to claim (" + claim.x() + ", " + claim.z() + ").");
    }
}

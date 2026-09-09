package com.circuitdrop.kingdomclaim.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Small fluent helper for building menu icons without repeating the same
 * ItemMeta boilerplate in every GUI class.
 */
public final class ItemBuilder {

    private final ItemStack stack;
    private final List<Component> lore = new ArrayList<>();
    private Component name;

    public ItemBuilder(Material material) {
        this.stack = new ItemStack(material);
    }

    public static ItemBuilder playerHead(UUID owner) {
        ItemBuilder builder = new ItemBuilder(Material.PLAYER_HEAD);
        ItemMeta meta = builder.stack.getItemMeta();
        if (meta instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(org.bukkit.Bukkit.getOfflinePlayer(owner));
            builder.stack.setItemMeta(skullMeta);
        }
        return builder;
    }

    public ItemBuilder name(Component name) {
        this.name = name.decoration(TextDecoration.ITALIC, false);
        return this;
    }

    public ItemBuilder lore(Component line) {
        this.lore.add(line.decoration(TextDecoration.ITALIC, false));
        return this;
    }

    public ItemStack build() {
        ItemMeta meta = stack.getItemMeta();
        if (name != null) {
            meta.displayName(name);
        }
        if (!lore.isEmpty()) {
            meta.lore(lore);
        }
        stack.setItemMeta(meta);
        return stack;
    }
}

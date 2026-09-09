package com.circuitdrop.kingdomclaim.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;

/**
 * Central place for the plugin's chat prefix and small message-building helpers,
 * so every command/listener sends consistently styled Adventure components.
 */
public final class Messages {

    private Messages() {
    }

    public static void info(CommandSender sender, String message) {
        sender.sendMessage(prefixed(Component.text(message, NamedTextColor.GRAY)));
    }

    public static void success(CommandSender sender, String message) {
        sender.sendMessage(prefixed(Component.text(message, NamedTextColor.GREEN)));
    }

    public static void error(CommandSender sender, String message) {
        sender.sendMessage(prefixed(Component.text(message, NamedTextColor.RED)));
    }

    public static Component prefixed(Component body) {
        return Component.text("[", NamedTextColor.DARK_GRAY)
                .append(Component.text("Kingdom", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text("] ", NamedTextColor.DARK_GRAY))
                .append(body);
    }
}

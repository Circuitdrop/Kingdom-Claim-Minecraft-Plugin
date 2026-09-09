package com.circuitdrop.kingdomclaim.manager;

import com.circuitdrop.kingdomclaim.model.ClaimChunk;
import com.circuitdrop.kingdomclaim.model.Kingdom;
import com.circuitdrop.kingdomclaim.model.Rank;
import com.circuitdrop.kingdomclaim.model.RelationType;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Flat-file (YAML) persistence for all kingdoms. Loaded once on enable and
 * saved wholesale on disable / periodic autosave; there is no incremental
 * write-through, so callers that mutate state should not assume durability
 * until the next save.
 */
public class DataManager {

    private final Plugin plugin;
    private final File file;

    public DataManager(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "kingdoms.yml");
    }

    public void load(KingdomManager manager) {
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("kingdoms");
        if (root == null) {
            return;
        }
        for (String idKey : root.getKeys(false)) {
            try {
                ConfigurationSection section = root.getConfigurationSection(idKey);
                if (section == null) {
                    continue;
                }
                UUID id = UUID.fromString(idKey);
                String name = section.getString("name", "Unnamed");
                Kingdom kingdom = new Kingdom(id, name, id); // membership below overwrites this placeholder entry
                readMembers(kingdom, section);
                readClaims(kingdom, section);
                if (section.contains("home")) {
                    kingdom.setHome(section.getString("home"));
                }
                if (section.contains("color")) {
                    kingdom.setColor(NamedTextColor.NAMES.value(section.getString("color")));
                }
                manager.register(kingdom);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load kingdom entry '" + idKey + "'", e);
            }
        }
        // Relations reference other kingdom ids, so resolve them in a second pass
        // once every kingdom object exists.
        for (String idKey : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(idKey);
            if (section == null) {
                continue;
            }
            UUID id = UUID.fromString(idKey);
            manager.byId(id).ifPresent(kingdom -> readRelations(kingdom, section));
        }
    }

    private void readMembers(Kingdom kingdom, ConfigurationSection section) {
        kingdom.members().clear();
        ConfigurationSection members = section.getConfigurationSection("members");
        if (members == null) {
            return;
        }
        for (String memberKey : members.getKeys(false)) {
            UUID memberId = UUID.fromString(memberKey);
            Rank rank = Rank.valueOf(members.getString(memberKey, "MEMBER"));
            kingdom.members().put(memberId, rank);
        }
    }

    private void readClaims(Kingdom kingdom, ConfigurationSection section) {
        for (String raw : section.getStringList("claims")) {
            kingdom.claims().add(ClaimChunk.deserialize(raw));
        }
    }

    private void readRelations(Kingdom kingdom, ConfigurationSection section) {
        ConfigurationSection relations = section.getConfigurationSection("relations");
        if (relations == null) {
            return;
        }
        for (String otherKey : relations.getKeys(false)) {
            UUID otherId = UUID.fromString(otherKey);
            RelationType type = RelationType.valueOf(relations.getString(otherKey, "NEUTRAL"));
            kingdom.relations().put(otherId, type);
        }
    }

    public void save(KingdomManager manager) {
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection root = yaml.createSection("kingdoms");
        for (Kingdom kingdom : manager.all()) {
            ConfigurationSection section = root.createSection(kingdom.id().toString());
            section.set("name", kingdom.name());
            if (kingdom.home() != null) {
                section.set("home", kingdom.home());
            }
            if (kingdom.color() != null) {
                section.set("color", NamedTextColor.NAMES.key(kingdom.color()));
            }
            ConfigurationSection members = section.createSection("members");
            kingdom.members().forEach((uuid, rank) -> members.set(uuid.toString(), rank.name()));
            section.set("claims", kingdom.claims().stream().map(ClaimChunk::serialize).toList());
            ConfigurationSection relations = section.createSection("relations");
            kingdom.relations().forEach((uuid, type) -> relations.set(uuid.toString(), type.name()));
        }
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save kingdoms.yml", e);
        }
    }
}

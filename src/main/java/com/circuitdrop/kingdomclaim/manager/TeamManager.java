package com.circuitdrop.kingdomclaim.manager;

import com.circuitdrop.kingdomclaim.model.Kingdom;
import com.circuitdrop.kingdomclaim.model.Rank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

/**
 * Mirrors each kingdom's membership onto two scoreboard teams on the main
 * scoreboard — one for the king, one for everyone else — so every member's
 * nametag and tab-list entry render in the kingdom's chosen color, with the
 * king additionally getting a crown prefix. Two teams per kingdom exist
 * because a scoreboard entry can only belong to one team at a time, and the
 * crown must apply to the king alone rather than the whole kingdom.
 * <p>
 * Entries are kept in sync with kingdom membership (not online status), so a
 * returning player renders correctly the instant they log in.
 */
public class TeamManager {

    /** White chess king glyph — renders fine in vanilla's default font, no resource pack needed. */
    public static final Component CROWN = Component.text("♔ ");

    private final Scoreboard scoreboard;

    public TeamManager() {
        this.scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
    }

    public void sync(Kingdom kingdom) {
        NamedTextColor color = kingdom.color() != null ? kingdom.color() : NamedTextColor.WHITE;
        Team kingTeam = teamFor(kingTeamName(kingdom));
        Team memberTeam = teamFor(memberTeamName(kingdom));
        kingTeam.color(color);
        memberTeam.color(color);
        kingTeam.prefix(CROWN);

        clearEntries(kingTeam);
        clearEntries(memberTeam);
        for (Map.Entry<UUID, Rank> entry : kingdom.members().entrySet()) {
            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            if (name == null) {
                continue;
            }
            if (entry.getValue() == Rank.KING) {
                kingTeam.addEntry(name);
            } else {
                memberTeam.addEntry(name);
            }
        }
    }

    public void remove(Kingdom kingdom) {
        unregisterIfExists(kingTeamName(kingdom));
        unregisterIfExists(memberTeamName(kingdom));
    }

    private Team teamFor(String name) {
        Team team = scoreboard.getTeam(name);
        return team != null ? team : scoreboard.registerNewTeam(name);
    }

    private void clearEntries(Team team) {
        for (String entry : new ArrayList<>(team.getEntries())) {
            team.removeEntry(entry);
        }
    }

    private void unregisterIfExists(String name) {
        Team team = scoreboard.getTeam(name);
        if (team != null) {
            team.unregister();
        }
    }

    private String kingTeamName(Kingdom kingdom) {
        return "kK_" + shortId(kingdom);
    }

    private String memberTeamName(Kingdom kingdom) {
        return "kM_" + shortId(kingdom);
    }

    private String shortId(Kingdom kingdom) {
        return kingdom.id().toString().replace("-", "").substring(0, 12);
    }
}

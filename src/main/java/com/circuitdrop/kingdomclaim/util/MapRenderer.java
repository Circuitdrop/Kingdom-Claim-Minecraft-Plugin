package com.circuitdrop.kingdomclaim.util;

import com.circuitdrop.kingdomclaim.manager.KingdomManager;
import com.circuitdrop.kingdomclaim.model.ClaimChunk;
import com.circuitdrop.kingdomclaim.model.Kingdom;
import com.circuitdrop.kingdomclaim.model.RelationType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

/**
 * Renders a small ASCII grid of chunk ownership around the player, used by
 * "/kingdom map". Complements {@link ChunkVisualizer}, which shows borders
 * in-world rather than as a chat readout.
 */
public final class MapRenderer {

    private static final int RADIUS = 5;

    private MapRenderer() {
    }

    public static Component render(Player viewer, KingdomManager kingdoms) {
        Kingdom viewerKingdom = kingdoms.kingdomOf(viewer.getUniqueId()).orElse(null);
        ClaimChunk here = ClaimChunk.of(viewer.getLocation());

        Component result = Component.text("Claim map (you are in the center)", NamedTextColor.GOLD, TextDecoration.BOLD)
                .appendNewline();

        for (int dz = -RADIUS; dz <= RADIUS; dz++) {
            Component row = Component.empty();
            for (int dx = -RADIUS; dx <= RADIUS; dx++) {
                if (dx == 0 && dz == 0) {
                    row = row.append(Component.text("X", NamedTextColor.WHITE, TextDecoration.BOLD));
                    continue;
                }
                ClaimChunk chunk = new ClaimChunk(here.world(), here.x() + dx, here.z() + dz);
                row = row.append(symbolFor(chunk, viewerKingdom, kingdoms));
            }
            result = result.append(row).appendNewline();
        }

        result = result.append(Component.text("■ ", NamedTextColor.GREEN))
                .append(Component.text("your kingdom  ", NamedTextColor.GRAY))
                .append(Component.text("■ ", NamedTextColor.YELLOW))
                .append(Component.text("other/neutral  ", NamedTextColor.GRAY))
                .append(Component.text("■ ", NamedTextColor.RED))
                .append(Component.text("enemy/war  ", NamedTextColor.GRAY))
                .append(Component.text("· ", NamedTextColor.DARK_GRAY))
                .append(Component.text("unclaimed", NamedTextColor.GRAY));

        return result;
    }

    private static Component symbolFor(ClaimChunk chunk, Kingdom viewerKingdom, KingdomManager kingdoms) {
        return kingdoms.kingdomAt(chunk)
                .map(owner -> {
                    if (viewerKingdom != null && owner.id().equals(viewerKingdom.id())) {
                        return Component.text("■", NamedTextColor.GREEN);
                    }
                    if (viewerKingdom != null) {
                        RelationType relation = Diplomacy.effectiveRelation(viewerKingdom, owner);
                        if (relation == RelationType.ENEMY || relation == RelationType.WAR) {
                            return Component.text("■", NamedTextColor.RED);
                        }
                        if (relation == RelationType.ALLY) {
                            return Component.text("■", NamedTextColor.GREEN);
                        }
                    }
                    return Component.text("■", NamedTextColor.YELLOW);
                })
                .orElse(Component.text("·", NamedTextColor.DARK_GRAY));
    }
}

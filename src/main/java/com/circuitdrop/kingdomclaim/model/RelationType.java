package com.circuitdrop.kingdomclaim.model;

import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Diplomatic state one kingdom holds towards another. Relations are one-directional
 * proposals that become mutual once both sides declare the same (or a compatible) state;
 * see {@code RelationManager} for the reconciliation rules.
 */
public enum RelationType {
    ALLY(NamedTextColor.GREEN, false, false),
    NEUTRAL(NamedTextColor.YELLOW, false, false),
    ENEMY(NamedTextColor.RED, true, false),
    WAR(NamedTextColor.DARK_RED, true, true);

    private final NamedTextColor color;
    private final boolean pvpAllowed;
    private final boolean claimsRaidable;

    RelationType(NamedTextColor color, boolean pvpAllowed, boolean claimsRaidable) {
        this.color = color;
        this.pvpAllowed = pvpAllowed;
        this.claimsRaidable = claimsRaidable;
    }

    public NamedTextColor color() {
        return color;
    }

    /** Whether members of the two kingdoms may fight each other, including inside claims. */
    public boolean pvpAllowed() {
        return pvpAllowed;
    }

    /** Whether the enemy kingdom may break/place blocks inside claimed land (only during WAR). */
    public boolean claimsRaidable() {
        return claimsRaidable;
    }
}

package com.circuitdrop.kingdomclaim.model;

import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Diplomatic state one kingdom holds towards another. ALLY/NEUTRAL/ENEMY are
 * one-directional declarations reconciled by {@code Diplomacy} (ALLY needs both
 * sides to agree; ENEMY takes effect unilaterally). WAR is never set directly —
 * only {@code WarManager} writes it, after its declare-war gating and notice delay.
 * <p>
 * ENEMY is purely a diplomatic signal: it grants no combat or raiding rights
 * of its own. Wilderness PvP is already unrestricted for everyone regardless
 * of relation, so ENEMY has no claim-interior effect either — only WAR does.
 */
public enum RelationType {
    ALLY(NamedTextColor.GREEN, false, false),
    NEUTRAL(NamedTextColor.YELLOW, false, false),
    ENEMY(NamedTextColor.RED, false, false),
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

    /** Whether members of the two kingdoms may fight each other inside a claim (only true for WAR). */
    public boolean pvpAllowed() {
        return pvpAllowed;
    }

    /** Whether the enemy kingdom may break/place blocks inside claimed land (only during WAR). */
    public boolean claimsRaidable() {
        return claimsRaidable;
    }
}

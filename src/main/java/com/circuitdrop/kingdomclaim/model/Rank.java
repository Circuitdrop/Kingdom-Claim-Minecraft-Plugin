package com.circuitdrop.kingdomclaim.model;

/**
 * Membership rank inside a {@link Kingdom}, ordered from lowest to highest authority.
 */
public enum Rank {
    MEMBER(0, "Member"),
    OFFICER(1, "Officer"),
    KING(2, "King");

    private final int level;
    private final String displayName;

    Rank(int level, String displayName) {
        this.level = level;
        this.displayName = displayName;
    }

    public boolean atLeast(Rank other) {
        return this.level >= other.level;
    }

    public String displayName() {
        return displayName;
    }
}

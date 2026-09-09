package com.circuitdrop.kingdomclaim.util;

import com.circuitdrop.kingdomclaim.model.Kingdom;
import com.circuitdrop.kingdomclaim.model.RelationType;

/**
 * Reconciles the two kingdoms' one-directional relation declarations into a
 * single effective relation:
 * <ul>
 *   <li>ALLY only takes effect when both sides declare it towards each other.</li>
 *   <li>ENEMY/WAR take effect as soon as either side declares them, using the
 *       more hostile of the two declarations — so a kingdom that has been
 *       declared at war can fight back / raid even before it declares anything itself.</li>
 * </ul>
 */
public final class Diplomacy {

    private Diplomacy() {
    }

    public static RelationType effectiveRelation(Kingdom a, Kingdom b) {
        if (a.id().equals(b.id())) {
            return RelationType.ALLY;
        }
        RelationType aToB = a.relationTo(b.id());
        RelationType bToA = b.relationTo(a.id());
        if (aToB == RelationType.ALLY && bToA == RelationType.ALLY) {
            return RelationType.ALLY;
        }
        return hostilityRank(aToB) >= hostilityRank(bToA) ? demoteAlly(aToB) : demoteAlly(bToA);
    }

    private static int hostilityRank(RelationType type) {
        return switch (type) {
            case WAR -> 2;
            case ENEMY -> 1;
            default -> 0; // NEUTRAL, or an ALLY offer that wasn't reciprocated
        };
    }

    private static RelationType demoteAlly(RelationType type) {
        return type == RelationType.ALLY ? RelationType.NEUTRAL : type;
    }
}

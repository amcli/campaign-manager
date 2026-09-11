package com.dnd.campaignmanager.campaign;

import com.dnd.campaignmanager.gamesystem.FieldType;
import lombok.Getter;

import java.util.Set;

@Getter
public enum ConstraintType {
    MAX_VALUE(Set.of(FieldType.NUMBER), true, false),
    MIN_VALUE(Set.of(FieldType.NUMBER), true, false),
    MAX_TOTAL(Set.of(FieldType.NUMBER), true, false),
    MAX_ENTRIES(Set.of(FieldType.LONG_TEXT), true, false),
    MAX_REPEATS(Set.of(FieldType.LONG_TEXT), true, false),
    FORBIDDEN_TEXT(Set.of(FieldType.TEXT, FieldType.LONG_TEXT), false, true);

    private final Set<FieldType> appliesTo;
    private final boolean needsLimit;
    private final boolean needsText;

    ConstraintType(Set<FieldType> appliesTo, boolean needsLimit, boolean needsText) {
        this.appliesTo = appliesTo;
        this.needsLimit = needsLimit;
        this.needsText = needsText;
    }
}

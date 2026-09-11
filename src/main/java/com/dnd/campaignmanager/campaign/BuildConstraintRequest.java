package com.dnd.campaignmanager.campaign;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BuildConstraintRequest(
        @NotNull ConstraintType type,
        @Size(max = 60) String section,
        @Size(max = 64) String fieldKey,
        Integer limit,
        @Size(max = 120) String text
) {
}

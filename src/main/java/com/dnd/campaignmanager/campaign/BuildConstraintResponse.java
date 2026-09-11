package com.dnd.campaignmanager.campaign;

import com.dnd.campaignmanager.gamesystem.SheetTemplate;

public record BuildConstraintResponse(
        Long id,
        ConstraintType type,
        String section,
        String fieldKey,
        Integer limit,
        String text,
        String description
) {
    public static BuildConstraintResponse from(BuildConstraint constraint, SheetTemplate template) {
        return new BuildConstraintResponse(
                constraint.getId(),
                constraint.getType(),
                constraint.getSection(),
                constraint.getFieldKey(),
                constraint.getLimit(),
                constraint.getText(),
                constraint.describe(template));
    }
}

package com.dnd.campaignmanager.campaign;

import com.dnd.campaignmanager.user.UserSummary;

import java.util.List;

public record CampaignCharacterEntry(
        Long id,
        String name,
        UserSummary owner,
        List<String> buildViolations
) {
}

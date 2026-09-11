package com.dnd.campaignmanager.character;

import jakarta.validation.constraints.NotNull;

public record JoinCampaignRequest(@NotNull Long campaignId) {
}

package com.dnd.campaignmanager.campaign;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CampaignUpdateRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 5000) String description,
        @NotNull CampaignStatus status,
        @Min(1) Integer maxPlayers
) {
}

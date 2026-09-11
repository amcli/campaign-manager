package com.dnd.campaignmanager.campaign;

import com.dnd.campaignmanager.gamesystem.GameSystem;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CampaignRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 5000) String description,
        @NotNull GameSystem gameSystem,
        CampaignStatus status,
        @Min(1) Integer maxPlayers
) {
    public CampaignStatus statusOrDefault() {
        return status == null ? CampaignStatus.PLANNING : status;
    }
}

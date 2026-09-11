package com.dnd.campaignmanager.campaign;

import com.dnd.campaignmanager.gamesystem.GameSystem;
import com.dnd.campaignmanager.user.UserSummary;

import java.time.Instant;

public record CampaignSummary(
        Long id,
        String name,
        GameSystem gameSystem,
        String gameSystemName,
        String gameMasterTitle,
        CampaignStatus status,
        UserSummary dungeonMaster,
        int playerCount,
        Integer maxPlayers,
        Instant updatedAt
) {
    public static CampaignSummary from(Campaign campaign) {
        return new CampaignSummary(
                campaign.getId(),
                campaign.getName(),
                campaign.getGameSystem(),
                campaign.getGameSystem().getDisplayName(),
                campaign.getGameSystem().getGameMasterTitle(),
                campaign.getStatus(),
                UserSummary.from(campaign.getDungeonMaster()),
                campaign.getPlayers().size(),
                campaign.getMaxPlayers(),
                campaign.getUpdatedAt());
    }
}

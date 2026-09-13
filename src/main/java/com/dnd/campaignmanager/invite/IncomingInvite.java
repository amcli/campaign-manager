package com.dnd.campaignmanager.invite;

import com.dnd.campaignmanager.campaign.Campaign;
import com.dnd.campaignmanager.character.PlayerCharacter;
import com.dnd.campaignmanager.gamesystem.GameSystem;
import com.dnd.campaignmanager.user.UserSummary;

import java.time.Instant;

public record IncomingInvite(
        Long id,
        Long campaignId,
        String campaignName,
        GameSystem gameSystem,
        String gameSystemName,
        String gameMasterTitle,
        UserSummary dungeonMaster,
        Long characterId,
        String characterName,
        Instant createdAt
) {
    public static IncomingInvite from(CampaignInvite invite) {
        Campaign campaign = invite.getCampaign();
        PlayerCharacter character = invite.getCharacter();
        return new IncomingInvite(
                invite.getId(),
                campaign.getId(),
                campaign.getName(),
                campaign.getGameSystem(),
                campaign.getGameSystem().getDisplayName(),
                campaign.getGameSystem().getGameMasterTitle(),
                UserSummary.from(campaign.getDungeonMaster()),
                character.getId(),
                character.getName(),
                invite.getCreatedAt());
    }
}

package com.dnd.campaignmanager.invite;

import com.dnd.campaignmanager.character.PlayerCharacter;
import com.dnd.campaignmanager.user.UserSummary;

import java.time.Instant;

public record PendingInvite(
        Long id,
        Long characterId,
        String characterName,
        UserSummary player,
        Instant createdAt
) {
    public static PendingInvite from(CampaignInvite invite) {
        PlayerCharacter character = invite.getCharacter();
        return new PendingInvite(
                invite.getId(),
                character.getId(),
                character.getName(),
                UserSummary.from(character.getOwner()),
                invite.getCreatedAt());
    }
}

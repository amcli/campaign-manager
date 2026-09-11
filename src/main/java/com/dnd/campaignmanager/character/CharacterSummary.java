package com.dnd.campaignmanager.character;

import com.dnd.campaignmanager.gamesystem.GameSystem;
import com.dnd.campaignmanager.user.UserSummary;

import java.time.Instant;

public record CharacterSummary(
        Long id,
        String name,
        GameSystem gameSystem,
        String gameSystemName,
        CampaignRef campaign,
        UserSummary owner,
        Instant updatedAt
) {
    public static CharacterSummary from(PlayerCharacter character) {
        return new CharacterSummary(
                character.getId(),
                character.getName(),
                character.getGameSystem(),
                character.getGameSystem().getDisplayName(),
                CampaignRef.from(character.getCampaign()),
                UserSummary.from(character.getOwner()),
                character.getUpdatedAt());
    }
}

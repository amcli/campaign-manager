package com.dnd.campaignmanager.character;

import com.dnd.campaignmanager.gamesystem.GameSystem;
import com.dnd.campaignmanager.user.UserSummary;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record CharacterDetail(
        Long id,
        String name,
        GameSystem gameSystem,
        String gameSystemName,
        CampaignRef campaign,
        UserSummary owner,
        Map<String, String> sheet,
        List<String> buildViolations,
        Instant createdAt,
        Instant updatedAt
) {
    public static CharacterDetail from(PlayerCharacter character) {
        List<String> violations = character.isInCampaign()
                ? character.getCampaign().buildViolations(character.getSheet())
                : List.of();
        return new CharacterDetail(
                character.getId(),
                character.getName(),
                character.getGameSystem(),
                character.getGameSystem().getDisplayName(),
                CampaignRef.from(character.getCampaign()),
                UserSummary.from(character.getOwner()),
                Map.copyOf(character.getSheet()),
                violations,
                character.getCreatedAt(),
                character.getUpdatedAt());
    }
}

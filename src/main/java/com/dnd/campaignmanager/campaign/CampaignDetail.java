package com.dnd.campaignmanager.campaign;

import com.dnd.campaignmanager.gamesystem.GameSystem;
import com.dnd.campaignmanager.invite.PendingInvite;
import com.dnd.campaignmanager.user.UserSummary;

import java.time.Instant;
import java.util.List;

public record CampaignDetail(
        Long id,
        String name,
        String description,
        GameSystem gameSystem,
        String gameSystemName,
        String gameMasterTitle,
        CampaignStatus status,
        Integer maxPlayers,
        UserSummary dungeonMaster,
        boolean viewerIsDungeonMaster,
        List<UserSummary> players,
        List<CampaignCharacterEntry> characters,
        List<NoteResponse> notes,
        List<BuildConstraintResponse> buildConstraints,
        List<PendingInvite> pendingInvites,
        Instant createdAt,
        Instant updatedAt
) {
}

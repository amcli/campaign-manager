package com.dnd.campaignmanager.campaign;

import java.time.Instant;

public record NoteResponse(
        Long id,
        String title,
        String content,
        NoteCategory category,
        boolean sharedWithPlayers,
        Instant createdAt,
        Instant updatedAt
) {
    public static NoteResponse from(CampaignNote note) {
        return new NoteResponse(
                note.getId(),
                note.getTitle(),
                note.getContent(),
                note.getCategory(),
                note.isSharedWithPlayers(),
                note.getCreatedAt(),
                note.getUpdatedAt());
    }
}

package com.dnd.campaignmanager.campaign;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NoteRequest(
        @NotBlank @Size(max = 120) String title,
        @Size(max = 10000) String content,
        @NotNull NoteCategory category,
        boolean sharedWithPlayers
) {
}

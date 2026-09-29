package com.dnd.campaignmanager.invite;

import jakarta.validation.constraints.NotNull;

public record InviteRequest(@NotNull Long characterId) {
}

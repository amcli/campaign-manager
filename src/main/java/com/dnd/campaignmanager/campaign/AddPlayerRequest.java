package com.dnd.campaignmanager.campaign;

import jakarta.validation.constraints.NotBlank;

public record AddPlayerRequest(@NotBlank String username) {
}

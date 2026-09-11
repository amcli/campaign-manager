package com.dnd.campaignmanager.character;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record CharacterUpdateRequest(
        @NotBlank @Size(max = 120) String name,
        Map<String, String> sheet
) {
}

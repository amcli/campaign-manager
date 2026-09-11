package com.dnd.campaignmanager.character;

import com.dnd.campaignmanager.gamesystem.GameSystem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record CharacterRequest(
        @NotBlank @Size(max = 120) String name,
        @NotNull GameSystem gameSystem,
        Map<String, String> sheet
) {
}

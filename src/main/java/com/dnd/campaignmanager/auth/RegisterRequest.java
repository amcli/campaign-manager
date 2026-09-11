package com.dnd.campaignmanager.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 50)
        @Pattern(regexp = "[A-Za-z0-9_.-]+", message = "Use only letters, numbers, underscore, dot or dash")
        String username,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 100) String password
) {
}

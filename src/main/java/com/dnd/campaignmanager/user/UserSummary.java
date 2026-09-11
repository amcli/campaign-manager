package com.dnd.campaignmanager.user;

public record UserSummary(Long id, String username) {

    public static UserSummary from(User user) {
        return new UserSummary(user.getId(), user.getUsername());
    }
}

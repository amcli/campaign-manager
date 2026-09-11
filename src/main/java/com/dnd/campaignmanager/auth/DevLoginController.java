package com.dnd.campaignmanager.auth;

import com.dnd.campaignmanager.user.AppUserPrincipal;
import com.dnd.campaignmanager.user.User;
import com.dnd.campaignmanager.user.UserService;
import com.dnd.campaignmanager.user.UserSummary;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Skips the password check entirely and signs the caller in as a fixed local account.
 * Only wired up when app.dev-login.enabled=true, which is the case for the default
 * (embedded H2) profile and off for the mysql and test profiles.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.dev-login.enabled", havingValue = "true")
@Slf4j
public class DevLoginController {

    private final UserService userService;
    private final SessionLogin sessionLogin;

    @PostConstruct
    void warnOnStartup() {
        log.warn("Dev login bypass is ENABLED. Do not run this configuration anywhere reachable by others.");
    }

    @PostMapping("/dev-login")
    public UserSummary devLogin(HttpServletRequest request, HttpServletResponse response) {
        User devUser = userService.getOrCreateDevUser();
        AppUserPrincipal principal = sessionLogin.withoutPassword(devUser, request, response);
        return new UserSummary(principal.id(), principal.username());
    }
}

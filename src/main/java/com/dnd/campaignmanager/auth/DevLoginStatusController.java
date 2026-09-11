package com.dnd.campaignmanager.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class DevLoginStatusController {

    @Value("${app.dev-login.enabled:false}")
    private boolean enabled;

    @GetMapping("/dev-login-status")
    public DevLoginStatus status() {
        return new DevLoginStatus(enabled);
    }
}

package com.dnd.campaignmanager.auth;

import com.dnd.campaignmanager.user.AppUserPrincipal;
import com.dnd.campaignmanager.user.User;
import com.dnd.campaignmanager.user.UserService;
import com.dnd.campaignmanager.user.UserSummary;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final SessionLogin sessionLogin;

    @PostMapping("/register")
    public ResponseEntity<UserSummary> register(@Valid @RequestBody RegisterRequest request,
                                                HttpServletRequest httpRequest,
                                                HttpServletResponse httpResponse) {
        User user = userService.register(request.username(), request.email(), request.password());
        sessionLogin.withPassword(request.username(), request.password(), httpRequest, httpResponse);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserSummary.from(user));
    }

    @PostMapping("/login")
    public UserSummary login(@Valid @RequestBody LoginRequest request,
                             HttpServletRequest httpRequest,
                             HttpServletResponse httpResponse) {
        AppUserPrincipal principal =
                sessionLogin.withPassword(request.username(), request.password(), httpRequest, httpResponse);
        return new UserSummary(principal.id(), principal.username());
    }

    @GetMapping("/me")
    public UserSummary me(@AuthenticationPrincipal AppUserPrincipal principal) {
        return new UserSummary(principal.id(), principal.username());
    }
}

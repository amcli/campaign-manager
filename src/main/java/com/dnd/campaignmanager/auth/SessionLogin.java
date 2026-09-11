package com.dnd.campaignmanager.auth;

import com.dnd.campaignmanager.user.AppUserPrincipal;
import com.dnd.campaignmanager.user.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class SessionLogin {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    AppUserPrincipal withPassword(String username, String password,
                                  HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(username, password));
        return start(authentication, request, response);
    }

    AppUserPrincipal withoutPassword(User user, HttpServletRequest request, HttpServletResponse response) {
        AppUserPrincipal principal = AppUserPrincipal.from(user);
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        return start(authentication, request, response);
    }

    private AppUserPrincipal start(Authentication authentication,
                                   HttpServletRequest request, HttpServletResponse response) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        return (AppUserPrincipal) authentication.getPrincipal();
    }
}

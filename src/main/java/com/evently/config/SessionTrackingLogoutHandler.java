package com.evently.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

import com.evently.service.SessionService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Removes the session row from the {@code sessions} table when a user logs out.
 *
 * OWNER: Alei
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SessionTrackingLogoutHandler implements LogoutHandler {

    private final SessionService sessionService;

    @Override
    public void logout(HttpServletRequest request,
                       HttpServletResponse response,
                       Authentication authentication) {

        HttpSession session = request.getSession(false);
        if (session == null) return;

        try {
            sessionService.deleteSession(session.getId());
        } catch (Exception e) {
            // Never block logout due to session-tracking failure.
            log.error("Failed to delete session record on logout: {}", e.getMessage());
        }
    }
}

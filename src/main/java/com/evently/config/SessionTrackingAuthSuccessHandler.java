package com.evently.config;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.evently.service.SessionService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Extends Spring Security's default success handler to record a row in the
 * {@code sessions} table after every successful login.
 *
 * <p>Inherits the redirect-to-{@code /events} behaviour from
 * {@link SimpleUrlAuthenticationSuccessHandler}.
 *
 * OWNER: Alei
 */
@Component
@Slf4j
public class SessionTrackingAuthSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final SessionService sessionService;

    public SessionTrackingAuthSuccessHandler(SessionService sessionService) {
        super("/events");          // default success URL
        setAlwaysUseDefaultTargetUrl(true);
        this.sessionService = sessionService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        // Ensure session exists (creates one if not yet initialised).
        String sessionToken = request.getSession(true).getId();
        String ipAddress    = request.getRemoteAddr();
        String userAgent    = request.getHeader("User-Agent");
        String email        = authentication.getName();

        try {
            sessionService.createSession(email, sessionToken, ipAddress, userAgent);
        } catch (Exception e) {
            // Never block login due to session-tracking failure.
            log.error("Failed to record session for user {}: {}", email, e.getMessage());
        }

        super.onAuthenticationSuccess(request, response, authentication);
    }
}

package com.evently.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.evently.model.Session;
import com.evently.repository.SessionRepository;
import com.evently.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Manages login-session records in the {@code sessions} table.
 *
 * <p>Called by the Spring Security success handler (login) and logout handler.
 *
 * OWNER: Alei
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SessionService {

    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;

    /**
     * Records a new session row when a user successfully authenticates.
     *
     * @param userEmail    the authenticated user's email
     * @param sessionToken the servlet session ID ({@code HttpSession#getId()})
     * @param ipAddress    remote address of the client (may be {@code null})
     * @param userAgent    User-Agent header value (may be {@code null})
     */
    public void createSession(String userEmail, String sessionToken,
                              String ipAddress, String userAgent) {
        userRepository.findByEmail(userEmail).ifPresentOrElse(user -> {
            // If a stale row exists for this token (e.g., devtools restart), replace it.
            sessionRepository.findBySessionToken(sessionToken).ifPresent(sessionRepository::delete);

            Session session = Session.builder()
                    .user(user)
                    .sessionToken(sessionToken)
                    .ipAddress(ipAddress)
                    .userAgent(trimUserAgent(userAgent))
                    .lastActivity(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .build();
            sessionRepository.save(session);
            log.info("Session created: user={}, token={}...", userEmail, abbrev(sessionToken));
        }, () -> log.warn("createSession: no user found for email={}", userEmail));
    }

    /**
     * Removes the session row identified by the given token.
     * Called on logout or session invalidation.
     */
    public void deleteSession(String sessionToken) {
        sessionRepository.findBySessionToken(sessionToken).ifPresent(s -> {
            sessionRepository.delete(s);
            log.info("Session deleted: user={}, token={}...",
                    s.getUser().getEmail(), abbrev(sessionToken));
        });
    }

    /**
     * Returns all sessions for the given user, newest activity first.
     * Used by admin views.
     */
    @Transactional(readOnly = true)
    public List<Session> getSessionsForUser(Long userId) {
        return sessionRepository.findByUserIdOrderByLastActivityDesc(userId);
    }

    // -----------------------------------------------------------------------

    private String trimUserAgent(String ua) {
        if (ua == null) return null;
        return ua.length() > 500 ? ua.substring(0, 500) : ua;
    }

    private String abbrev(String token) {
        if (token == null || token.length() <= 8) return token;
        return token.substring(0, 8);
    }
}

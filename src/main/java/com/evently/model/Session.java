package com.evently.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity for the {@code sessions} table.
 *
 * <p>Records every authenticated login session for audit and session-management purposes.
 * A row is created when a user successfully logs in and deleted when they log out or
 * their session expires.
 *
 * OWNER: Alei (security config integration)
 */
@Entity
@Table(name = "sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The authenticated user who owns this session. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * The Servlet container session ID ({@code HttpSession#getId()}).
     * Used to look up and expire sessions.
     */
    @Column(name = "session_token", nullable = false, unique = true, length = 255)
    private String sessionToken;

    /** Remote IP address of the client (supports IPv6, max 45 chars). */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /** User-Agent header sent by the browser. */
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    /** Updated to now() on each authenticated request (or on login). */
    @Column(name = "last_activity", nullable = false)
    private LocalDateTime lastActivity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (lastActivity == null) lastActivity = now;
    }
}

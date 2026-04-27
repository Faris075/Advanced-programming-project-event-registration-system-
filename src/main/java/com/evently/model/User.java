package com.evently.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * JPA entity for the `users` table.
 *
 * Stores registered user accounts.  The is_admin flag is the role gate for the
 * admin dashboard.  Security question/answer are stored separately from the
 * main password and both are BCrypt-hashed — never plain text.
 *
 * OWNER: Faris (entity definition)
 * SECURITY CONFIG: Alei (UserDetailsService loads this entity)
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /** Login identifier – must be unique across the system. */
    @Column(nullable = false, unique = true)
    private String email;

    /** BCrypt hash – never store or log plain text. */
    @Column(nullable = false)
    private String password;

    /** true = full admin access to /admin/** endpoints. */
    @Column(name = "is_admin", nullable = false)
    private boolean isAdmin = false;

    /** Free-text question chosen by the user during setup. */
    @Column(name = "security_question", length = 500)
    private String securityQuestion;

    /** BCrypt hash of the security answer – compared with Hash.check(). */
    @Column(name = "security_answer")
    private String securityAnswer;

    /** User's preferred display currency code, e.g. "USD", "EUR", "EGP". */
    @Column(name = "currency_preference", nullable = false, length = 10)
    private String currencyPreference = "USD";

    /** Spring Security remember-me token. */
    @Column(name = "remember_token", length = 100)
    private String rememberToken;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

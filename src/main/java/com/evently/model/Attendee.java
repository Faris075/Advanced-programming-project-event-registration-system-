package com.evently.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * JPA entity for the `attendees` table.
 *
 * An Attendee is a contact record (name + email) that can be reused across
 * multiple event registrations.  Intentionally separate from the User table so:
 *   1. Admins can register guests who don't have a user account.
 *   2. A single person is deduplicated by email in RegistrationService
 *      using findByEmail + save-or-return pattern.
 *
 * OWNER: Faris (entity + repository)
 * USAGE: Mohamed Ahmed (RegistrationService creates/reuses attendees)
 */
@Entity
@Table(name = "attendees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attendee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /** Deduplication key — one Attendee row per email address. */
    @Column(nullable = false, unique = true)
    private String email;

    /** Optional contact phone number. */
    @Column(length = 30)
    private String phone;

    /** Optional organisation / company name. */
    private String company;

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

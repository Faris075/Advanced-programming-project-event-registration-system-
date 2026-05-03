package com.evently.model;

import com.evently.model.converter.PaymentStatusConverter;
import com.evently.model.converter.RegistrationStatusConverter;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * JPA entity for the `registrations` table.
 *
 * Join table linking an Attendee to an Event.  Tracks the full lifecycle of a
 * booking (status, payment, waitlist position).
 *
 * Key invariants enforced by RegistrationService:
 *   - (event_id, attendee_id) is unique — no duplicate bookings.
 *   - waitlistPosition is NULL for CONFIRMED registrations, an integer ≥ 1 for WAITLISTED.
 *   - PAID payment_status is immutable (admin cannot revert it to PENDING).
 *   - Cancellation of a CONFIRMED booking must trigger waitlist promotion in the same transaction.
 *
 * OWNER: Faris (entity + repository)
 * CORE LOGIC: Mohamed Ahmed (RegistrationService)
 */
@Entity
@Table(
    name = "registrations",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_event_attendee",
        columnNames = {"event_id", "attendee_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The event this booking belongs to. EAGER is fine here (always needed). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    /** The contact record of the person who booked. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attendee_id", nullable = false)
    private Attendee attendee;

    /** When the booking was originally made (set to now at insert). */
    @Column(name = "registration_date", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime registrationDate;

    /** Booking lifecycle — see RegistrationStatus enum. */
    @Convert(converter = RegistrationStatusConverter.class)
    @Column(nullable = false)
    private RegistrationStatus status = RegistrationStatus.CONFIRMED;

    /** Payment lifecycle — see PaymentStatus enum. */
    @Convert(converter = PaymentStatusConverter.class)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    /**
     * Sequential queue position for WAITLISTED registrations.
     * NULL for CONFIRMED bookings.  Reordered (decremented) when a lower-position
     * entry is promoted or cancelled.
     */
    @Column(name = "waitlist_position")
    private Integer waitlistPosition;

    /**
     * True when an admin used the force-add feature to bypass capacity limits.
     * Capped at 5 overrides per event.
     */
    @Column(name = "is_admin_override", nullable = false)
    private boolean isAdminOverride = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        registrationDate = LocalDateTime.now();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

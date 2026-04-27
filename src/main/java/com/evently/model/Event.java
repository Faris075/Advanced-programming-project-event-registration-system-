package com.evently.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA entity for the `events` table.
 *
 * Central aggregate of the system — registrations, attendees, and all capacity
 * logic reference this entity.
 *
 * Lifecycle:  DRAFT → PUBLISHED → (COMPLETED | CANCELLED)
 *   - Admins control transitions manually via AdminEventController.
 *   - PUBLISHED → COMPLETED is also triggered automatically by EventScheduler
 *     when dateTime is in the past.
 *
 * OWNER: Faris (entity) | Mohamed Morsy (CRUD controller + scheduler)
 */
@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    /** Full description displayed on the detail page. Stored as TEXT. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /** Event start date and time; used for "upcoming" filtering and auto-completion. */
    @Column(name = "date_time", nullable = false)
    private LocalDateTime dateTime;

    @Column(nullable = false)
    private String location;

    /** Maximum number of confirmed registrations allowed. */
    @Column(nullable = false)
    private int capacity;

    /** NULL means the event is free. Uses DECIMAL(10,2) at DB level. */
    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status = EventStatus.DRAFT;

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

    /** Convenience: true when the event has no price (free to attend). */
    @Transient
    public boolean isFree() {
        return price == null || price.compareTo(BigDecimal.ZERO) == 0;
    }
}

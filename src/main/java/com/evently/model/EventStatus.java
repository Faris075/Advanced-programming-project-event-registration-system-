package com.evently.model;

/**
 * Event lifecycle status.
 *
 * Transitions:
 *   DRAFT      → PUBLISHED  (admin publishes)
 *   PUBLISHED  → CANCELLED  (admin cancels)
 *   PUBLISHED  → COMPLETED  (auto via scheduler when dateTime is in the past)
 */
public enum EventStatus {
    DRAFT,
    PUBLISHED,
    CANCELLED,
    COMPLETED
}

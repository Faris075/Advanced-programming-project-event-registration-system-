package com.evently.model;

/**
 * Booking lifecycle for a single registration record.
 *
 *   CONFIRMED  – seat reserved; attendee has a guaranteed spot.
 *   WAITLISTED – event is full; attendee queued for automatic promotion on cancellation.
 *   CANCELLED  – attendee withdrew or admin cancelled.
 */
public enum RegistrationStatus {
    CONFIRMED,
    WAITLISTED,
    CANCELLED
}

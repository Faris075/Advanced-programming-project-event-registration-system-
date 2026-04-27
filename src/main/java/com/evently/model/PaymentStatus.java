package com.evently.model;

/**
 * Payment lifecycle for a registration.
 *
 *   PENDING  – payment not yet received (default; also used for free events).
 *   PAID     – mock payment processed successfully.
 *   REFUNDED – payment returned on cancellation of a paid registration.
 *
 * Business rule: once PAID, status cannot be reverted by admin to PENDING.
 */
public enum PaymentStatus {
    PENDING,
    PAID,
    REFUNDED
}

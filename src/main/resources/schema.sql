-- ============================================================
-- Evently Database Schema
-- Run once against a fresh MySQL 8 server:
--   mysql -u root -p < src/main/resources/schema.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS evently_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE evently_db;

-- ----------------------------------------------------------------
-- TABLE: users
-- Stores registered accounts. is_admin flag controls admin access.
-- is_super_admin: only one super admin exists; can promote/demote admins.
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL, -- BCrypt hash; never plain text
    is_admin TINYINT(1) NOT NULL DEFAULT 0,
    is_super_admin TINYINT(1) NOT NULL DEFAULT 0,
    security_question VARCHAR(500) NULL,
    security_answer VARCHAR(255) NULL, -- BCrypt hash
    currency_preference VARCHAR(10) NOT NULL DEFAULT 'USD',
    remember_token VARCHAR(100) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------
-- TABLE: events
-- Central aggregate. Lifecycle: draft → published → completed/cancelled.
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS events (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    date_time DATETIME NOT NULL, -- used for auto-completion check
    location VARCHAR(255) NOT NULL,
    capacity INT NOT NULL,
    price DECIMAL(10, 2) NULL, -- NULL = free event
    status ENUM(
        'draft',
        'published',
        'cancelled',
        'completed'
    ) NOT NULL DEFAULT 'draft',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_events_status (status),
    INDEX idx_events_date_time (date_time)
);

-- ----------------------------------------------------------------
-- TABLE: attendees
-- Contact records. Separate from users so admins can register guests.
-- Deduplicated by email via findByEmail + updateOrCreate pattern.
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS attendees (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(30) NULL,
    company VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_attendees_email (email)
);

-- ----------------------------------------------------------------
-- TABLE: registrations
-- Join table linking attendees to events.
-- Tracks booking lifecycle, payment status, and waitlist position.
-- ----------------------------------------------------------------


CREATE TABLE IF NOT EXISTS registrations (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    event_id            BIGINT UNSIGNED NOT NULL,
    attendee_id         BIGINT UNSIGNED NOT NULL,
    registration_date   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status              ENUM('confirmed','waitlisted','cancelled') NOT NULL DEFAULT 'confirmed',
    payment_status      ENUM('pending','paid','refunded')          NOT NULL DEFAULT 'pending',
    waitlist_position   INT             NULL,     -- NULL for confirmed; integer queue position for waitlisted
    is_admin_override   TINYINT(1)      NOT NULL DEFAULT 0,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_reg_event     FOREIGN KEY (event_id)    REFERENCES events(id)    ON DELETE CASCADE,
    CONSTRAINT fk_reg_attendee  FOREIGN KEY (attendee_id) REFERENCES attendees(id) ON DELETE CASCADE,

-- DB-level duplicate prevention (second layer after service-level check)


UNIQUE KEY uq_event_attendee (event_id, attendee_id),

    INDEX idx_registrations_event_id    (event_id),
    INDEX idx_registrations_attendee_id (attendee_id),
    INDEX idx_registrations_status      (status)
);

-- ----------------------------------------------------------------
-- TABLE: sessions
-- Tracks active user login sessions for audit and session management.
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sessions (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    session_token VARCHAR(255) NOT NULL UNIQUE,
    ip_address VARCHAR(45) NULL,
    user_agent TEXT NULL,
    last_activity TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_sessions_user_id (user_id),
    INDEX idx_sessions_token (session_token)
);
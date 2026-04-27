# Evently — Event Registration System

A full-stack Java web application for event discovery, attendee registration, waitlist management, and admin controls — built with Spring Boot 3, Spring MVC, Spring Security, and MySQL.

---

## Table of Contents

1. [Features](#features)
2. [Tech Stack](#tech-stack)
3. [Team](#team)
4. [Prerequisites](#prerequisites)
5. [Database Setup](#database-setup)
6. [Project Setup & Configuration](#project-setup--configuration)
7. [Running the Application](#running-the-application)
8. [Admin Credentials](#admin-credentials)
9. [Application Flow](#application-flow)
10. [Project Structure](#project-structure)
11. [Architecture & Design Decisions](#architecture--design-decisions)
12. [Security](#security)
13. [Running Tests](#running-tests)
14. [Environment Variables Reference](#environment-variables-reference)

---

## Features

### Public / Attendee
- Browse paginated list of upcoming published events with remaining spot count and price
- View full event detail page with capacity progress bar
- Multi-step registration: details form → mock payment → confirmation page
- Automatic waitlisting when an event is full, with sequential position tracking
- "Fully Booked" badge replaces the Register button when capacity is reached
- "Already Registered" badge for returning users
- Cancel your own registration; waitlisted attendees are automatically promoted
- User registration with email + password + security question setup
- Password recovery via security question (no email dependency)
- Optional email-based password reset
- Preferred display currency selector (USD, EUR, GBP, SAR, AED, EGP, etc.)
- Personal registration history dashboard

### Admin
- Full event CRUD (create, edit, update, delete) with status control
- Event statuses: draft → published → completed / cancelled
- Scheduled daily job auto-marks past published events as completed
- Registration listing per event with status/email filters
- Manual registration status change (with payment immutability: PAID cannot be reverted)
- Force-add a registration for any attendee (bypasses capacity; max 5 overrides per event)
- CSV export of registrations (respects active filters)
- User management: view all users, promote to admin, delete accounts
- Admin dashboard with summary statistics

### Security & Reliability
- Pessimistic row-level locking (`@Lock(PESSIMISTIC_WRITE)`) inside `@Transactional` blocks prevents double-booking race conditions
- DB-level unique constraint on `(event_id, attendee_id)` as second layer of duplicate prevention
- Async email dispatch (Spring `@Async`) does not block the HTTP thread
- Session-gated multi-step registration flow prevents step-skipping
- Bcrypt hashing for passwords and security question answers
- CSRF protection on all forms (Spring Security default)
- Role-based access: admin routes guarded by both Spring Security and `AdminInterceptor`

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.x |
| Web | Spring MVC (`DispatcherServlet`) |
| Security | Spring Security 6 |
| ORM | Spring Data JPA + Hibernate |
| View Engine | Thymeleaf 3 + Thymeleaf Extras Spring Security |
| Database | MySQL 8.0 |
| Build | Apache Maven |
| Email | Spring Boot Mail (JavaMailSender) |
| Testing | JUnit 5, Mockito, MockMvc, H2 (in-memory) |
| CI | GitHub Actions |

---

## Team

| Developer | Branch | Responsibility |
|-----------|--------|----------------|
| **Faris** | `feature/faris-setup` | Project setup, database schema, JPA entities, repositories, data seeder, global config |
| **Alei** | `feature/alei-auth` | Authentication (login/register/logout), security question recovery, profile management, admin user management views |
| **Mohamed Morsy** | `feature/morsy-admin-events` | Admin event CRUD, status management, scheduled auto-completion, admin dashboard, admin layout |
| **Mohamed Ehab** | `feature/ehab-public-events` | Public events listing, event detail page, home page, public layout, CSS design tokens |
| **Mohamed Ahmed** | `feature/ahmed-registration` | Registration flow (form → payment → confirmation), waitlist logic, cancellation, email notifications, user dashboard |
| **Islam** | `feature/islam-admin-mgmt` | Admin registration management, CSV export, force-add, feature tests, GitHub Actions CI |

---

## Prerequisites

- **Java 17+** — `java -version`
- **Maven 3.8+** — `mvn -version`
- **MySQL 8.0+** — running locally or via Docker (`docker run -p 3306:3306 -e MYSQL_ROOT_PASSWORD=secret mysql:8`)
- **Git**
- (Optional) A Mailtrap.io account for email testing in development

---

## Database Setup

### 1. Create the database and tables

Connect to MySQL and run the schema file:

```bash
mysql -u root -p < src/main/resources/schema.sql
```

Or run the statements manually:

```sql
CREATE DATABASE IF NOT EXISTS evently_db
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE evently_db;

CREATE TABLE users (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name                VARCHAR(255)    NOT NULL,
    email               VARCHAR(255)    NOT NULL UNIQUE,
    password            VARCHAR(255)    NOT NULL,
    is_admin            TINYINT(1)      NOT NULL DEFAULT 0,
    security_question   VARCHAR(500)    NULL,
    security_answer     VARCHAR(255)    NULL,
    currency_preference VARCHAR(10)     NOT NULL DEFAULT 'USD',
    remember_token      VARCHAR(100)    NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE events (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(255)   NOT NULL,
    description TEXT           NOT NULL,
    date_time   DATETIME       NOT NULL,
    location    VARCHAR(255)   NOT NULL,
    capacity    INT            NOT NULL,
    price       DECIMAL(10,2)  NULL,
    status      ENUM('draft','published','cancelled','completed') NOT NULL DEFAULT 'draft',
    created_at  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_events_status    (status),
    INDEX idx_events_date_time (date_time)
);

CREATE TABLE attendees (
    id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL UNIQUE,
    phone      VARCHAR(30)  NULL,
    company    VARCHAR(255) NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_attendees_email (email)
);

CREATE TABLE registrations (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    event_id            BIGINT UNSIGNED NOT NULL,
    attendee_id         BIGINT UNSIGNED NOT NULL,
    registration_date   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status              ENUM('confirmed','waitlisted','cancelled') NOT NULL DEFAULT 'confirmed',
    payment_status      ENUM('pending','paid','refunded')          NOT NULL DEFAULT 'pending',
    waitlist_position   INT             NULL,
    is_admin_override   TINYINT(1)      NOT NULL DEFAULT 0,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (event_id)    REFERENCES events(id)    ON DELETE CASCADE,
    FOREIGN KEY (attendee_id) REFERENCES attendees(id) ON DELETE CASCADE,
    UNIQUE KEY uq_registration (event_id, attendee_id),
    INDEX idx_registrations_event_id    (event_id),
    INDEX idx_registrations_attendee_id (attendee_id),
    INDEX idx_registrations_status      (status)
);
```

The application's `DataSeeder` (runs on startup) will automatically insert the default admin user and 5 sample events if the database is empty.

---

## Project Setup & Configuration

### 1. Clone the repository

```bash
git clone https://github.com/<your-org>/evently.git
cd evently
```

### 2. Create your local configuration file

Create `src/main/resources/application-local.properties` (this file is gitignored and never committed):

```properties
# Database
DB_USERNAME=root
DB_PASSWORD=your_mysql_password

# Mail (use Mailtrap for local dev — https://mailtrap.io)
MAIL_HOST=smtp.mailtrap.io
MAIL_PORT=2525
MAIL_USERNAME=your_mailtrap_username
MAIL_PASSWORD=your_mailtrap_password

# Remember-me secret
APP_SECRET=local-dev-secret-change-me
```

### 3. Install dependencies

```bash
mvn clean install -DskipTests
```

---

## Running the Application

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

The application starts at **http://localhost:8080**.

To run with a specific port:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local -Dserver.port=9090
```

---

## Admin Credentials

The `DataSeeder` inserts a default admin account on the first startup if no admin exists.

| Field | Value |
|-------|-------|
| Email | `admin@evently.com` |
| Password | `Admin@1234` |

> Change this password immediately after first login in a production environment.

---

## Application Flow

### User Registration & Event Booking

```
/                       → Landing page
/auth/register          → Create account + set security question
/auth/login             → Login (email + password + remember me)
/events                 → Browse paginated published events
/events/{id}            → Event detail with capacity and Register button
/register/events/{id}   → Step 1: Registration form (name, email, phone)
/register/events/{id}/payment  → Step 2: Mock payment form
/register/confirmation/{id}    → Step 3: Confirmation (confirmed or waitlisted)
/dashboard              → My registrations history + cancel option
```

### Admin Flow

```
/admin                           → Dashboard with summary stats
/admin/events                    → Event listing (all statuses)
/admin/events/new                → Create event form
/admin/events/{id}/edit          → Edit event form
/admin/events/{id}/registrations → All registrations for an event
/admin/users                     → All user accounts
```

---

## Project Structure

```
src/
├── main/
│   ├── java/com/evently/
│   │   ├── EvidentlyApplication.java       ← main class (@EnableScheduling, @EnableAsync)
│   │   ├── config/
│   │   │   ├── SecurityConfig.java         ← Spring Security rules + UserDetailsService
│   │   │   ├── WebMvcConfig.java           ← Admin interceptor registration
│   │   │   └── PasswordEncoderConfig.java  ← BCryptPasswordEncoder bean
│   │   ├── controller/
│   │   │   ├── HomeController.java
│   │   │   ├── PublicEventController.java
│   │   │   ├── RegistrationController.java
│   │   │   ├── DashboardController.java
│   │   │   ├── admin/
│   │   │   │   ├── AdminDashboardController.java
│   │   │   │   ├── AdminEventController.java
│   │   │   │   ├── AdminRegistrationController.java
│   │   │   │   └── AdminUserController.java
│   │   │   └── auth/
│   │   │       ├── AuthController.java
│   │   │       ├── SecurityQuestionController.java
│   │   │       └── ProfileController.java
│   │   ├── model/
│   │   │   ├── User.java
│   │   │   ├── Event.java
│   │   │   ├── Attendee.java
│   │   │   ├── Registration.java
│   │   │   ├── EventStatus.java            ← enum
│   │   │   ├── RegistrationStatus.java     ← enum
│   │   │   └── PaymentStatus.java          ← enum
│   │   ├── repository/
│   │   │   ├── UserRepository.java
│   │   │   ├── EventRepository.java
│   │   │   ├── AttendeeRepository.java
│   │   │   └── RegistrationRepository.java
│   │   ├── service/
│   │   │   ├── EventService.java
│   │   │   ├── RegistrationService.java    ← core waitlist logic
│   │   │   ├── AdminRegistrationService.java
│   │   │   └── AdminUserService.java
│   │   ├── dto/
│   │   │   ├── RegistrationDto.java
│   │   │   ├── RegistrationFormDto.java
│   │   │   ├── PaymentDto.java
│   │   │   ├── EventDto.java
│   │   │   ├── ProfileDto.java
│   │   │   └── PasswordChangeDto.java
│   │   ├── exception/
│   │   │   ├── EventNotFoundException.java
│   │   │   ├── DuplicateRegistrationException.java
│   │   │   └── GlobalControllerAdvice.java ← @ControllerAdvice
│   │   ├── scheduler/
│   │   │   └── EventScheduler.java         ← daily cron job
│   │   ├── email/
│   │   │   └── EmailService.java
│   │   ├── interceptor/
│   │   │   └── AdminInterceptor.java
│   │   └── DataSeeder.java                 ← CommandLineRunner for seed data
│   └── resources/
│       ├── application.properties
│       ├── schema.sql
│       ├── templates/
│       │   ├── fragments/
│       │   │   ├── public-layout.html      ← nav, footer, flash messages
│       │   │   └── admin-layout.html       ← sidebar, top bar
│       │   ├── index.html
│       │   ├── events/
│       │   │   ├── index.html
│       │   │   └── show.html
│       │   ├── registration/
│       │   │   ├── form.html
│       │   │   ├── payment.html
│       │   │   └── confirmation.html
│       │   ├── dashboard/
│       │   │   └── index.html
│       │   ├── auth/
│       │   │   ├── login.html
│       │   │   ├── register.html
│       │   │   ├── security-setup.html
│       │   │   └── forgot-password.html
│       │   ├── profile/
│       │   │   └── index.html
│       │   ├── admin/
│       │   │   ├── dashboard.html
│       │   │   ├── events/
│       │   │   │   ├── index.html
│       │   │   │   ├── form.html
│       │   │   │   └── show.html
│       │   │   ├── registrations/
│       │   │   │   └── index.html
│       │   │   └── users/
│       │   │       └── index.html
│       │   ├── email/
│       │   │   ├── registration-confirmation.html
│       │   │   └── waitlist-promotion.html
│       │   └── error/
│       │       ├── 403.html
│       │       ├── 404.html
│       │       └── 500.html
│       └── static/
│           ├── css/
│           │   └── app.css
│           └── js/
│               └── app.js
└── test/
    ├── java/com/evently/
    │   ├── service/
    │   │   └── RegistrationServiceTest.java
    │   └── controller/
    │       ├── RegistrationFlowIT.java
    │       └── AdminEventControllerTest.java
    └── resources/
        └── application-test.properties
```

---

## Architecture & Design Decisions

### MVC with Service Layer
Controllers handle HTTP (parsing requests, redirecting, building model), Services contain all business logic and are the only layer that is `@Transactional`, Repositories handle data access. Views (Thymeleaf) are pure presentation.

### Why Separate `attendees` Table from `users`?
An `Attendee` is a contact record identified by email. This allows admins to register guests who don't have a user account. An authenticated user fills in their own details (or they reuse a previously created attendee row found by email).

### Waitlist Concurrency Safety
The waitlist promotion reads the first waitlisted record with `@Lock(PESSIMISTIC_WRITE)` inside a `@Transactional` block. This ensures that if two cancellations happen simultaneously, only one attendee is promoted per cancellation, eliminating the race condition that would otherwise allow the same waitlisted row to be promoted twice.

### Mock Payment
The payment step collects card-like form data for UX realism but **does not transmit any data to a payment processor and does not persist card numbers**. The `PaymentDto` is discarded after the registration is committed to the database.

### Async Emails
Confirmation and promotion emails are dispatched via Spring's `@Async` on a separate thread pool. This means the HTTP response is returned to the user immediately, even if the mail server is slow or temporarily unavailable. A failed email is logged but never causes the registration to roll back.

### Scheduled Job
`EventScheduler` runs at midnight every day (`cron = "0 0 0 * * *"`) and calls `EventService.markPastEventsCompleted()`. This method is idempotent — it only touches `PUBLISHED` events whose `dateTime` is in the past. Running it multiple times does not corrupt already-completed records.

---

## Security

| Concern | Implementation |
|---------|---------------|
| Password hashing | `BCryptPasswordEncoder` (strength 12) |
| Security answer hashing | BCrypt via same encoder |
| CSRF | Spring Security default — all POST forms protected automatically by Thymeleaf `th:action` |
| Admin access | `SecurityConfig` role-based + `AdminInterceptor` double check on `/admin/**` |
| Session fixation | Spring Security default protection enabled |
| Input validation | Bean Validation (`@Valid`) on all DTOs before any write |
| SQL injection | JPA/JPQL with named parameters only — no raw string concatenation |
| Sensitive config | All credentials in environment variables, never in source code |
| Error messages | Anti-enumeration: forgot-password always returns the same message regardless of whether the email exists |
| Card data | Mock only — never persisted, never logged |

---

## Running Tests

Tests use an in-memory H2 database configured via `application-test.properties`. No MySQL connection is required to run tests.

```bash
# Run all tests
mvn test -Dspring.profiles.active=test

# Run a specific test class
mvn test -Dtest=RegistrationServiceTest -Dspring.profiles.active=test

# Run only integration tests
mvn test -Dtest=*IT -Dspring.profiles.active=test
```

CI runs these automatically on every push and pull request via GitHub Actions (`.github/workflows/ci.yml`).

---

## Environment Variables Reference

| Variable | Description | Example |
|----------|-------------|---------|
| `DB_USERNAME` | MySQL database username | `root` |
| `DB_PASSWORD` | MySQL database password | `secret` |
| `MAIL_HOST` | SMTP server host | `smtp.mailtrap.io` |
| `MAIL_PORT` | SMTP server port | `2525` |
| `MAIL_USERNAME` | SMTP username | *(from Mailtrap inbox)* |
| `MAIL_PASSWORD` | SMTP password | *(from Mailtrap inbox)* |
| `APP_SECRET` | Remember-me token signing key | `change-me-in-prod` |

Set these either in `application-local.properties` (local dev, gitignored) or as system environment variables (production/CI).

---

## Recommended Dev Tools

- **IntelliJ IDEA** (Community or Ultimate) — excellent Spring Boot and Thymeleaf support
- **TablePlus** or **DBeaver** — GUI MySQL client
- **Mailtrap.io** — free email sandbox for testing outbound emails
- **Postman** — useful for testing API endpoints manually before views are built

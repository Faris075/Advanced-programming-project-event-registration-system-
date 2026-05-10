# Evently — Event Registration System
## Developer Instructions & Task Distribution

> **Stack:** Java 25 (bytecode 21) · Spring Boot 3.x · Spring MVC · Spring Security · Spring Data JPA · Hibernate · MariaDB 10.4 · Thymeleaf · Maven
> **Team:** Faris · Alei · Mohamed Morsy · Mohamed Ehab · Mohamed Ahmed · Islam

---

## Table of Contents
1. [Project Overview](#project-overview)
2. [Architecture Reference](#architecture-reference)
3. [Database Schema](#database-schema)
4. [Git Branching Strategy](#git-branching-strategy)
5. [Developer Task Assignments](#developer-task-assignments)
   - [Developer 1 — Faris: Project Setup & Database Layer](#developer-1--faris-project-setup--database-layer)
   - [Developer 2 — Alei: Authentication & User Management](#developer-2--alei-authentication--user-management)
   - [Developer 3 — Mohamed Morsy: Admin Event Management](#developer-3--mohamed-morsy-admin-event-management)
   - [Developer 4 — Mohamed Ehab: Public Events & Event Detail](#developer-4--mohamed-ehab-public-events--event-detail)
   - [Developer 5 — Mohamed Ahmed: Registration & Waitlist Logic](#developer-5--mohamed-ahmed-registration--waitlist-logic)
   - [Developer 6 — Islam: Admin Registration/User Management, Tests & Export](#developer-6--islam-admin-registrationuser-management-tests--export)
6. [Shared Conventions](#shared-conventions)
7. [Integration Checklist](#integration-checklist)

---

## Project Overview

**Evently** is a full-stack web application where users browse events, register for them, and are automatically placed on a waitlist when an event is at capacity. Admins manage events, registrations, and user accounts through a separate dashboard.

**Core feature areas:**
- Authentication (register, login, remember-me, password reset, security question)
- Public events listing + event detail pages
- Multi-step registration flow with mock payment + confirmation
- Capacity enforcement + automatic waitlist promotion
- Email notifications (confirmation, waitlist promotion, event cancellation)
- Admin dashboard: event CRUD, registration management, user management
- CSV export, filtering, role-based access control
- Scheduled job: auto-mark past events as `completed`

---

## Architecture Reference

```
com.evently
├── config/           ← Spring Security config, Web MVC config, Scheduler config
├── controller/       ← Spring MVC @Controllers (one per feature area)
│   ├── admin/        ← Admin-only controllers
│   └── auth/         ← Login, Register, Password Reset controllers
├── model/            ← JPA @Entity classes
├── repository/       ← Spring Data JPA @Repository interfaces
├── service/          ← Business logic services
├── dto/              ← Data Transfer Objects (form-backing beans)
├── exception/        ← Custom exceptions + global exception handler
├── scheduler/        ← @Scheduled tasks
├── email/            ← Mail service + Thymeleaf email templates
└── util/             ← Shared utilities
src/main/resources/
├── templates/        ← Thymeleaf HTML templates
│   ├── admin/
│   ├── auth/
│   ├── events/
│   ├── registration/
│   └── fragments/    ← Reusable layout fragments (header, footer, nav)
├── static/
│   ├── css/          ← Global stylesheet with CSS custom properties
│   └── js/
└── application.properties
```

**Request lifecycle:**
```
HTTP Request → DispatcherServlet → @Controller → @Service → @Repository → Database
                     ↓
              Thymeleaf View ← Model attributes
```

---

## Database Schema

All developers must use this exact schema. Only **Faris** creates it; everyone else references it.

```sql
-- ============================================================
-- DATABASE: evently_db
-- ============================================================
CREATE DATABASE IF NOT EXISTS evently_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE evently_db;

-- ============================================================
-- TABLE: users
-- Stores registered user accounts. is_admin flag controls
-- access to the admin dashboard.
-- ============================================================
CREATE TABLE users (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name                VARCHAR(255)    NOT NULL,
    email               VARCHAR(255)    NOT NULL UNIQUE,
    password            VARCHAR(255)    NOT NULL,               -- bcrypt hash
    is_admin            TINYINT(1)      NOT NULL DEFAULT 0,
    security_question   VARCHAR(500)    NULL,
    security_answer     VARCHAR(255)    NULL,                   -- bcrypt hash
    currency_preference VARCHAR(10)     NOT NULL DEFAULT 'USD',
    remember_token      VARCHAR(100)    NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ============================================================
-- TABLE: events
-- Central aggregate. All registrations reference this table.
-- Lifecycle: draft → published → completed/cancelled
-- ============================================================
CREATE TABLE events (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(255)   NOT NULL,
    description TEXT           NOT NULL,
    date_time   DATETIME       NOT NULL,                        -- event start; used for auto-completion
    location    VARCHAR(255)   NOT NULL,
    capacity    INT            NOT NULL,
    price       DECIMAL(10,2)  NULL,                           -- NULL = free event
    status      ENUM('draft','published','cancelled','completed') NOT NULL DEFAULT 'draft',
    created_at  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_events_status    (status),
    INDEX idx_events_date_time (date_time)
);

-- ============================================================
-- TABLE: attendees
-- Contact records, intentionally separate from users so that
-- admins can register guests. Deduped by email.
-- ============================================================
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

-- ============================================================
-- TABLE: registrations
-- Join table linking attendees to events. Tracks booking
-- lifecycle, payment, and waitlist position.
-- ============================================================
CREATE TABLE registrations (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    event_id            BIGINT UNSIGNED NOT NULL,
    attendee_id         BIGINT UNSIGNED NOT NULL,
    registration_date   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status              ENUM('confirmed','waitlisted','cancelled') NOT NULL DEFAULT 'confirmed',
    payment_status      ENUM('pending','paid','refunded')          NOT NULL DEFAULT 'pending',
    waitlist_position   INT             NULL,                       -- NULL for confirmed registrations
    is_admin_override   TINYINT(1)      NOT NULL DEFAULT 0,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (event_id)   REFERENCES events(id)    ON DELETE CASCADE,
    FOREIGN KEY (attendee_id) REFERENCES attendees(id) ON DELETE CASCADE,
    UNIQUE KEY uq_registration (event_id, attendee_id),            -- prevents duplicate bookings at DB level
    INDEX idx_registrations_event_id   (event_id),
    INDEX idx_registrations_attendee_id (attendee_id),
    INDEX idx_registrations_status     (status)
);

-- ============================================================
-- DEFAULT ADMIN SEED
-- Password: Admin@1234 (bcrypt-hashed at insert time via app seeder)
-- ============================================================
-- Run the DataSeeder class; it inserts this record only if no
-- admin user exists yet.
```

---

## Git Branching Strategy

```
main
 └── develop            ← integration branch; all features merge here first
      ├── feature/faris-setup
      ├── feature/alei-auth
      ├── feature/morsy-admin-events
      ├── feature/ehab-public-events
      ├── feature/ahmed-registration
      └── feature/islam-admin-mgmt
```

**Rules:**
1. **Never push directly to `main` or `develop`**. Always open a Pull Request.
2. Create your feature branch from `develop`: `git checkout -b feature/<your-name>-<area> develop`
3. Commit often with meaningful messages: `feat: add waitlist promotion logic`
4. Before opening your PR, pull the latest `develop` and resolve any merge conflicts locally.
5. At least one other developer must review your PR before merging.

---

## Developer Task Assignments

---

### Developer 1 — Faris: Project Setup & Database Layer

**Estimated files: ~20** | **Branch:** `feature/faris-setup`

#### Phase 0 — Repository & Project Bootstrap

- [ ] Create a private GitHub repository named `evently`
- [ ] Initialize a Maven Spring Boot 3.x project (https://start.spring.io) with these dependencies:
  - Spring Web
  - Spring Security
  - Spring Data JPA
  - Thymeleaf
  - Thymeleaf Extras Spring Security 6
  - MariaDB Driver (`org.mariadb.jdbc:mariadb-java-client`)
  - Spring Boot DevTools
  - Lombok
  - Spring Boot Mail
  - Validation
  - Spring Boot Actuator (optional)
- [ ] Set up the base `pom.xml` and push the initial skeleton
- [ ] Create `.gitignore` (IntelliJ IDEA / VS Code / Maven standard)
- [ ] Add `application.properties` template (no real credentials — use placeholders)
- [ ] Create `application-local.properties` and add it to `.gitignore`
- [ ] Invite all 5 other developers as collaborators

#### Phase 1 — Database Setup

- [ ] Create the MariaDB database `evently_db` using the exact schema defined in [Database Schema](#database-schema)
- [ ] Write the schema as a plain `schema.sql` file in `src/main/resources/` so any developer running the app fresh can execute it
- [ ] Configure `application.properties` with the JDBC connection:
  ```properties
  spring.datasource.url=jdbc:mariadb://localhost:3306/evently_db?useSSL=false&serverTimezone=UTC
  spring.datasource.username=${DB_USERNAME}
  spring.datasource.password=${DB_PASSWORD}
  spring.datasource.driver-class-name=org.mariadb.jdbc.Driver
  spring.jpa.hibernate.ddl-auto=validate
  spring.jpa.show-sql=true
  spring.jpa.properties.hibernate.format_sql=true
  ```
  > No explicit `hibernate.dialect` — auto-detected from the MariaDB driver.

#### Phase 2 — JPA Entities

Create all four entity classes in `com.evently.model`:

- [ ] **`User.java`** — fields: `id`, `name`, `email`, `password`, `isAdmin` (boolean), `securityQuestion`, `securityAnswer`, `currencyPreference`, `rememberToken`, `createdAt`, `updatedAt`. Annotate with `@Entity @Table(name="users")`. Add `@OneToMany` to registrations if needed.
- [ ] **`Event.java`** — fields: `id`, `title`, `description`, `dateTime` (LocalDateTime), `location`, `capacity`, `price` (BigDecimal), `status` (EventStatus enum), `createdAt`, `updatedAt`. Add a transient helper method `getRemainingSpots()` that returns `capacity - confirmedCount` (confirmedCount injected from query).
- [ ] **`Attendee.java`** — fields: `id`, `name`, `email`, `phone`, `company`, `createdAt`, `updatedAt`.
- [ ] **`Registration.java`** — fields: `id`, `event` (`@ManyToOne`), `attendee` (`@ManyToOne`), `registrationDate`, `status` (RegistrationStatus enum), `paymentStatus` (PaymentStatus enum), `waitlistPosition` (Integer, nullable), `isAdminOverride`, `createdAt`, `updatedAt`.

- [ ] Create the three enum types in `com.evently.model`:
  - `EventStatus { DRAFT, PUBLISHED, CANCELLED, COMPLETED }`
  - `RegistrationStatus { CONFIRMED, WAITLISTED, CANCELLED }`
  - `PaymentStatus { PENDING, PAID, REFUNDED }`

#### Phase 3 — Spring Data JPA Repositories

Create repository interfaces in `com.evently.repository`:

- [ ] `UserRepository extends JpaRepository<User, Long>` — add:
  - `Optional<User> findByEmail(String email)`
  - `boolean existsByEmail(String email)`
- [ ] `EventRepository extends JpaRepository<Event, Long>` — add:
  - `Page<Event> findByStatus(EventStatus status, Pageable pageable)`
  - `List<Event> findByStatusAndDateTimeBefore(EventStatus status, LocalDateTime dateTime)`
  - `@Query("SELECT COUNT(r) FROM Registration r WHERE r.event.id = :eventId AND r.status = 'CONFIRMED'") long countConfirmedByEventId(@Param("eventId") Long eventId)`
- [ ] `AttendeeRepository extends JpaRepository<Attendee, Long>` — add:
  - `Optional<Attendee> findByEmail(String email)`
- [ ] `RegistrationRepository extends JpaRepository<Registration, Long>` — add:
  - `Optional<Registration> findByEventIdAndAttendeeId(Long eventId, Long attendeeId)`
  - `List<Registration> findByAttendeeIdOrderByCreatedAtDesc(Long attendeeId)`
  - `List<Registration> findByEventId(Long eventId)`
  - `@Query("SELECT r FROM Registration r WHERE r.event.id = :eventId AND r.status = 'WAITLISTED' ORDER BY r.waitlistPosition ASC") List<Registration> findWaitlistedByEventIdOrdered(@Param("eventId") Long eventId)`
  - `@Lock(LockModeType.PESSIMISTIC_WRITE) @Query("SELECT r FROM Registration r WHERE r.id = :id") Optional<Registration> findByIdWithLock(@Param("id") Long id)`

#### Phase 4 — Data Seeder

- [ ] Create `DataSeeder.java` implementing `CommandLineRunner` in `com.evently`:
  - If no admin user exists, insert one: email `admin@evently.com`, password `Admin@1234` (BCrypt-encoded), `isAdmin = true`
  - Insert 5 sample published events with future dates
  - Ensure it is idempotent (check before inserting)

#### Phase 5 — Global Configuration Beans

- [ ] Create `WebMvcConfig.java` — configure static resources, view resolvers (Thymeleaf is auto-configured but you can add custom resource handlers)
- [ ] Create `PasswordEncoderConfig.java` — expose a `BCryptPasswordEncoder` `@Bean`
- [ ] Create a base `GlobalControllerAdvice.java` with `@ControllerAdvice` — add `@ExceptionHandler` for `EntityNotFoundException` (404) and generic `Exception` (500) returning appropriate error pages
- [ ] Create `templates/error/404.html` and `templates/error/500.html`

---

### Developer 2 — Alei: Authentication & User Management

**Estimated files: ~18** | **Branch:** `feature/alei-auth`
**Depends on:** Faris's entities and repositories being merged first (or mock them locally)

#### Phase 1 — Spring Security Configuration

- [ ] Create `SecurityConfig.java` in `com.evently.config`:
  - Implement `UserDetailsService` that loads `User` by email
  - Configure `HttpSecurity`: permit `/`, `/events/**`, `/auth/**`, `/css/**`, `/js/**`; require auth for `/dashboard/**`, `/register/**`; restrict `/admin/**` to ROLE_ADMIN
  - Set login page: `GET /auth/login`, `POST /auth/login`, default success URL `/events`
  - Set logout URL: `POST /auth/logout`, success URL `/`
  - Configure remember-me with a secret key and `UserDetailsService`
  - Use `BCryptPasswordEncoder` bean from Faris's config

#### Phase 2 — Registration

- [ ] Create `RegistrationDto.java` in `com.evently.dto` — fields: `name`, `email`, `password`, `passwordConfirm`. Add Bean Validation annotations (`@NotBlank`, `@Email`, `@Size`).
- [ ] Create `AuthController.java` in `com.evently.controller.auth`:
  - `GET /auth/register` → show registration form
  - `POST /auth/register` → validate DTO, check duplicate email, hash password with BCrypt, save User, redirect to `/auth/security-setup`
- [ ] Create `templates/auth/register.html` — form with name, email, password, confirm-password fields; inline Thymeleaf validation error display

#### Phase 3 — Login / Logout

- [ ] Add to `AuthController.java`:
  - `GET /auth/login` → show login form (Spring Security handles the POST automatically)
- [ ] Create `templates/auth/login.html` — form with email, password, remember-me checkbox; error message display

#### Phase 4 — Security Question Setup & Recovery

- [ ] Create `SecurityQuestionController.java`:
  - `GET /auth/security-setup` → show question setup form (redirect if already set)
  - `POST /auth/security-setup` → save the question (plain text) and answer (BCrypt-hashed)
  - `GET /auth/forgot-password` → step 1: enter email
  - `POST /auth/forgot-password` → look up user, store email in session, redirect to step 2
  - `GET /auth/forgot-password/question` → step 2: show the user's security question
  - `POST /auth/forgot-password/question` → verify BCrypt answer, on success put userId in session, redirect to step 3
  - `GET /auth/forgot-password/reset` → step 3: new password form
  - `POST /auth/forgot-password/reset` → validate, hash, save new password; clear session attributes; redirect to login with success message
- [ ] Create templates: `auth/security-setup.html`, `auth/forgot-password.html`, `auth/forgot-password-question.html`, `auth/forgot-password-reset.html`
- [ ] **Security note:** All three steps must validate that the session contains the expected token/id from the previous step. A user who navigates directly to step 3 without completing steps 1–2 must be redirected back to step 1.

#### Phase 5 — Password Reset via Email (optional / bonus)

- [ ] If included: generate a UUID token, store in a `password_reset_tokens` table with expiry, send email with reset link, validate token on arrival, clear token after use.

#### Phase 6 — Profile Management ✅ COMPLETE

- [x] `ProfileController.java` at `com.evently.controller.auth` — handles GET /profile, POST /profile/update, POST /profile/password, POST /profile/security-question, POST /profile/currency, POST /profile/delete
- [x] `templates/auth/profile.html` — Personal Info, Change Password, Security Question, Currency Preference, Danger Zone (delete account, hidden for admins)
- [x] Controller uses `@RequestParam` directly — no separate DTO files needed (controller-level validation in place)

#### Phase 7 — Admin: User Management Views

- [ ] Create `AdminUserController.java` in `com.evently.controller.admin`:
  - `GET /admin/users` → paginated list of all users
  - `POST /admin/users/{id}/promote` → set `isAdmin = true`
  - `POST /admin/users/{id}/delete` → delete user (cannot delete self)
- [ ] Create `templates/admin/users/index.html` — table with name, email, role badge, promote/delete actions

---

### Developer 3 — Mohamed Morsy: Admin Event Management

**Estimated files: ~14** | **Branch:** `feature/morsy-admin-events`
**Depends on:** Faris's entities and repositories

#### Phase 1 — Admin Security Middleware

- [ ] Create `AdminInterceptor.java` implementing `HandlerInterceptor`:
  - In `preHandle`, check if authenticated user has `isAdmin = true`. If not, send HTTP 403 (or redirect to `/403`).
- [ ] Register it in `WebMvcConfig.java` for all paths matching `/admin/**`
- [ ] Create `templates/error/403.html`

#### Phase 2 — Admin Event CRUD

- [ ] Create `EventDto.java` in `com.evently.dto` — fields: `title`, `description`, `dateTime` (LocalDateTime), `location`, `capacity`, `price`, `status`. Add `@FutureOrPresent` on `dateTime` for creation, `@NotBlank`, `@NotNull`, `@Min(1)` as appropriate.
- [ ] Create `EventService.java` in `com.evently.service`:
  - `Page<Event> getAllEvents(Pageable pageable)` — all events for admin
  - `Optional<Event> getById(Long id)`
  - `Event create(EventDto dto)` — save with status DRAFT by default
  - `Event update(Long id, EventDto dto)` — load, update fields, save
  - `void delete(Long id)` — delete (cascade handles registrations)
  - `Event changeStatus(Long id, EventStatus newStatus)` — validate allowed transitions
  - `void markPastEventsCompleted()` — used by scheduler
- [ ] Create `AdminEventController.java` in `com.evently.controller.admin`:
  - `GET /admin/events` → paginated event list with search
  - `GET /admin/events/new` → show create form
  - `POST /admin/events` → validate EventDto, save, redirect with success flash
  - `GET /admin/events/{id}/edit` → show pre-filled edit form
  - `POST /admin/events/{id}` → validate, update, redirect
  - `POST /admin/events/{id}/delete` → delete, redirect
  - `POST /admin/events/{id}/status` → change status (enum value from request param)

#### Phase 3 — Admin Event Views (Thymeleaf)

- [ ] Create `templates/admin/events/index.html`:
  - Table with columns: Title, Date/Time, Location, Status (badge), Capacity, Actions
  - Pagination links
  - "New Event" button
- [ ] Create `templates/admin/events/form.html` — shared create/edit form:
  - All EventDto fields
  - Date-time picker (HTML `<input type="datetime-local">`)
  - Status dropdown (only shown on edit; disabled for completed/cancelled)
  - Inline Thymeleaf validation errors on each field
- [ ] Create `templates/admin/events/show.html` — event detail for admin with registration count and status badge

#### Phase 4 — Scheduled Task (Auto-Complete Past Events)

- [ ] Create `EventScheduler.java` in `com.evently.scheduler`:
  ```java
  @Scheduled(cron = "0 0 0 * * *") // runs daily at midnight
  public void markCompletedEvents() {
      eventService.markPastEventsCompleted();
  }
  ```
- [ ] Enable scheduling in the main application class with `@EnableScheduling`
- [ ] `markPastEventsCompleted()` in `EventService`:
  - Find all events with status `PUBLISHED` and `dateTime` before `LocalDateTime.now()`
  - For each, set status to `COMPLETED` and save
  - Log the count of updated events (use SLF4J)

#### Phase 5 — Admin Dashboard Home

- [ ] Create `AdminDashboardController.java`:
  - `GET /admin` or `GET /admin/dashboard` → show summary stats: total events, total registrations, upcoming events, total users
- [ ] Create `templates/admin/dashboard.html` — stat cards + quick navigation links

#### Phase 6 — Admin Layout Fragment

- [ ] Create `templates/fragments/admin-layout.html` — shared layout for all admin pages:
  - Sidebar navigation: Dashboard, Events, Registrations, Users
  - Top bar with logged-in username and logout button
  - Main content area

---

### Developer 4 — Mohamed Ehab: Public Events & Event Detail

**Estimated files: ~12** | **Branch:** `feature/ehab-public-events`
**Depends on:** Faris's entities and repositories; Faris's EventService (or create your own service methods)

#### Phase 1 — Public Event Service

Add these methods to `EventService.java` (coordinate with Morsy via the shared Java file):

- [ ] `Page<Event> getPublishedEvents(Pageable pageable)` — only `status = PUBLISHED`
- [ ] `Optional<Event> getPublishedEventById(Long id)`
- [ ] `long getConfirmedCount(Long eventId)` — query registrations table
- [ ] `boolean isFullyBooked(Long eventId)` — confirmedCount >= capacity
- [ ] `boolean isAlreadyRegistered(Long eventId, Long attendeeId)` — check registrations

**If EventService.java is not merged yet**, extend it in your branch and resolve conflicts when merging.

#### Phase 2 — Home / Landing Page

- [ ] Create `HomeController.java` in `com.evently.controller`:
  - `GET /` → redirect to `/events`
- [ ] Create `templates/index.html` — simple landing page with headline, CTA buttons ("Browse Events", "Login", "Register")

#### Phase 3 — Public Events Listing

- [ ] Create `PublicEventController.java` in `com.evently.controller`:
  - `GET /events` → fetch published events, paginated (default: 10 per page, sorted by dateTime ASC)
  - Pass to model: `events` (Page object), `currentPage`, `totalPages`
- [ ] Create `templates/events/index.html` — event listing page:
  - Each card: event title, date/time (formatted), location, price (or "Free"), remaining spots
  - **"Fully Booked" badge** (amber) when `remainingSpots <= 0` instead of "Register" button
  - **"Completed" badge** (grey) for completed events
  - **"Already Registered" badge** (green) if authenticated user has already registered
  - Pagination component at the bottom
  - "No upcoming events" empty state

#### Phase 4 — Event Detail Page

- [ ] Add to `PublicEventController.java`:
  - `GET /events/{id}` → load event; if not found or not published → 404; pass confirmedCount, remainingSpots, isFullyBooked, isAlreadyRegistered to model
- [ ] Create `templates/events/show.html` — event detail:
  - Full event info: title, description, date/time, location, price, capacity, remaining spots
  - Progress bar for capacity usage
  - **Register button** → `/register/events/{id}` (only shown if authenticated, not fully booked, not already registered)
  - **"Fully Booked"** message with waitlist info when at capacity (show "Join Waitlist" button)
  - **"Already Registered"** badge + link to registration history
  - Login prompt for unauthenticated users
  - Back to events link

#### Phase 5 — Shared Public Layout Fragment

- [ ] Create `templates/fragments/public-layout.html` — shared layout for public pages:
  - Navigation bar: logo, "Events" link, "Login"/"Register" for guests, username + "Dashboard" + "Logout" for authenticated users
  - Footer
  - Flash message area (success/error/info)

#### Phase 6 — Responsive CSS Base

- [ ] Create `src/main/resources/static/css/app.css`:
  - Define CSS custom properties (design tokens) in `:root`:
    ```css
    :root {
      --color-primary: #4f46e5;
      --color-success: #16a34a;
      --color-warning: #d97706;
      --color-danger:  #dc2626;
      --color-grey:    #6b7280;
      --radius-md:     0.5rem;
      --shadow-sm:     0 1px 3px rgba(0,0,0,.1);
    }
    @media (prefers-color-scheme: dark) { :root { ... } }
    ```
  - Base styles for cards, badges, buttons, tables, forms
  - Responsive grid for event cards

---

### Developer 5 — Mohamed Ahmed: Registration & Waitlist Logic

**Estimated files: ~16** | **Branch:** `feature/ahmed-registration`
**Depends on:** Faris's repositories; Ehab's public event pages (for links)

This is the most critical and complex slice of the system. Read the non-functional requirements section on transactions carefully.

#### Phase 1 — Registration Service (Core Logic)

- [ ] Create `RegistrationService.java` in `com.evently.service`. This service is the heart of the application:
  - `Registration register(Long eventId, RegistrationFormDto dto)`
    1. `@Transactional` — wrap the entire operation
    2. Load `Event` by id; throw `EventNotFoundException` if not found or not PUBLISHED
    3. Prevent past-event registration (check `event.dateTime.isBefore(LocalDateTime.now())`)
    4. Find or create `Attendee` by email (use `findByEmail` or create new)
    5. Check for existing registration → throw `DuplicateRegistrationException` if found
    6. Count confirmed registrations with a lock:
       - `long confirmed = registrationRepository.countConfirmedByEventId(eventId)`
    7. If `confirmed < event.capacity` → create `Registration` with `CONFIRMED` + `PENDING`
    8. Else → create with `WAITLISTED`; assign `waitlistPosition = currentWaitlistSize + 1`
    9. Save and return the registration
    10. Dispatch confirmation email asynchronously (see Phase 4)

  - `void cancelRegistration(Long registrationId, Long userId)`
    1. `@Transactional`
    2. Load registration, verify ownership (attendee email matches logged-in user)
    3. Set status to CANCELLED; if `paymentStatus == PAID`, set to REFUNDED
    4. **Waitlist promotion:** find the first `WAITLISTED` registration for this event (`waitlistPosition = 1`) using a pessimistic lock:
       - `registrationRepository.findByIdWithLock(firstWaitlistedId)`
       - Set its status to `CONFIRMED`, clear `waitlistPosition`
       - Reorder all remaining waitlisted registrations (decrement positions)
    5. Save all changes
    6. Send promotion email to promoted attendee (async)

  - `List<Registration> getRegistrationHistoryForUser(Long userId)` — look up attendee by user email, return all their registrations with event data

- [ ] Create `EventNotFoundException.java`, `DuplicateRegistrationException.java` in `com.evently.exception`

#### Phase 2 — Registration DTOs

- [ ] Create `RegistrationFormDto.java` — fields: `name`, `email`, `phone`. Bean Validation: `@NotBlank`, `@Email`, `@Pattern` for phone.
- [ ] Create `PaymentDto.java` — fields: `cardNumber` (length 16, validated as mock), `expiryMonth`, `expiryYear`, `cvv`. **This is a mock — do NOT store raw card data anywhere. Process the mock and discard.**

#### Phase 3 — Registration Controller & Flow

- [ ] Create `RegistrationController.java` in `com.evently.controller`:
  - `GET /register/events/{eventId}` → show registration form; require auth; 404 if event not found/not published
  - `POST /register/events/{eventId}` → validate `RegistrationFormDto`; save to session as "pendingRegistration"; redirect to payment page
  - `GET /register/events/{eventId}/payment` → show mock payment form; require session to contain pending registration (prevent skipping)
  - `POST /register/events/{eventId}/payment` → validate `PaymentDto`; call `registrationService.register(...)`; on success redirect to confirmation page; on `DuplicateRegistrationException` show error; on `EventFullException` show waitlist page
  - `GET /register/confirmation/{registrationId}` → show success page with registration details
  - `POST /register/{registrationId}/cancel` → call `registrationService.cancelRegistration(...)`; redirect to dashboard with flash

- [ ] **Session guard:** if a user navigates directly to `/register/events/{id}/payment` without a pending registration in session, redirect them to `/register/events/{id}` with an error message.

#### Phase 4 — Email Notifications

- [ ] Configure mail in `application.properties`:
  ```properties
  spring.mail.host=${MAIL_HOST:smtp.mailtrap.io}
  spring.mail.port=${MAIL_PORT:2525}
  spring.mail.username=${MAIL_USERNAME}
  spring.mail.password=${MAIL_PASSWORD}
  spring.mail.properties.mail.smtp.auth=true
  spring.mail.properties.mail.smtp.starttls.enable=true
  ```
- [ ] Create `EmailService.java` in `com.evently.email`:
  - `@Async void sendRegistrationConfirmation(Registration registration)` — use Spring's `JavaMailSender` to send an email from a Thymeleaf template
  - `@Async void sendWaitlistPromotion(Registration registration)` — notify attendee they are now confirmed
  - Enable `@Async` in `SecurityConfig.java` or a separate config with `@EnableAsync`
- [ ] Create `templates/email/registration-confirmation.html` — Thymeleaf email template
- [ ] Create `templates/email/waitlist-promotion.html`

#### Phase 5 — Registration Views

- [ ] Create `templates/registration/form.html` — registration form (name, email, phone); event summary on the side
- [ ] Create `templates/registration/payment.html` — mock credit card form; event + amount summary; disclaimer "This is a demo — no real payment is processed"
- [ ] Create `templates/registration/confirmation.html` — success/waitlist confirmation page:
  - Status badge (Confirmed/Waitlisted)
  - Registration details (event name, date, attendee name, position if waitlisted)
  - "View My Registrations" link
- [ ] Create `templates/registration/duplicate.html` or use flash messages for duplicate errors

#### Phase 6 — User Registration Dashboard

- [ ] Create `DashboardController.java` in `com.evently.controller`:
  - `GET /dashboard` → show user's registration history
- [ ] Create `templates/dashboard/index.html` — table of past/current registrations:
  - Columns: Event Name, Date, Status badge, Payment Status badge, Action (Cancel button for non-cancelled, non-completed events)

---

### Developer 6 — Islam: Admin Registration/User Management, Tests & Export

**Estimated files: ~18** | **Branch:** `feature/islam-admin-mgmt`
**Depends on:** All other branches being near-complete (or mocked)

#### Phase 1 — Admin Registration Management

- [ ] Create `AdminRegistrationController.java` in `com.evently.controller.admin`:
  - `GET /admin/events/{eventId}/registrations` → list all registrations for an event with filters (status, search by attendee email)
  - `POST /admin/events/{eventId}/registrations/{id}/status` → change registration status (with validation: PAID cannot be reverted, promote waitlisted on confirm)
  - `POST /admin/events/{eventId}/registrations/add` → force-add a registration for any attendee (bypass capacity check; set `isAdminOverride = true`; max 5 overrides per event)
  - `GET /admin/events/{eventId}/registrations/export` → export to CSV (see Phase 3)
- [ ] Create `AdminRegistrationService.java`:
  - `List<Registration> getRegistrationsForEvent(Long eventId, RegistrationStatus statusFilter, String emailFilter)`
  - `Registration forceAddRegistration(Long eventId, RegistrationFormDto dto)` — skip capacity check, set override flag
  - `Registration changeStatus(Long id, RegistrationStatus newStatus)` — apply business rules; wrap in transaction
  - `byte[] exportToCsv(Long eventId, RegistrationStatus statusFilter)` — generate CSV bytes in memory

#### Phase 2 — Admin Registration Views

- [ ] Create `templates/admin/registrations/index.html`:
  - Filter bar: status dropdown + email search input
  - Table: Attendee Name, Email, Phone, Registered Date, Status badge, Payment badge, Waitlist Position, Actions
  - "Force-Add" button that opens a modal with the registration form
  - "Export CSV" button
- [ ] Create `templates/admin/registrations/add-modal.html` (Thymeleaf fragment) — modal overlay with name, email, phone inputs

#### Phase 3 — CSV Export

- [ ] In `AdminRegistrationService.java`, implement `exportToCsv(Long eventId, RegistrationStatus filter)`:
  ```java
  // Build CSV manually (no external library needed):
  // Header: ID,Attendee Name,Email,Phone,Registered Date,Status,Payment Status,Waitlist Position
  // Rows: one per registration matching the filter
  ```
- [ ] In `AdminRegistrationController.java`, the export endpoint:
  ```java
  @GetMapping("/export")
  public ResponseEntity<byte[]> export(...) {
      byte[] csv = adminRegistrationService.exportToCsv(eventId, statusFilter);
      return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"registrations.csv\"")
          .contentType(MediaType.parseMediaType("text/csv"))
          .body(csv);
  }
  ```

#### Phase 4 — Feature Tests

- [ ] Configure `src/test/resources/application-test.properties`:
  ```properties
  spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL
  spring.datasource.driver-class-name=org.h2.Driver
  spring.jpa.hibernate.ddl-auto=create-drop
  spring.mail.host=localhost
  spring.mail.port=25
  ```
- [ ] Add H2 dependency to `pom.xml` with `<scope>test</scope>`
- [ ] Create `RegistrationServiceTest.java` (unit test, mock repositories):
  - Test: successful confirmed registration
  - Test: waitlist registration when event is full
  - Test: duplicate registration throws `DuplicateRegistrationException`
  - Test: cancellation promotes earliest waitlisted attendee
  - Test: cannot register for past event
- [ ] Create `RegistrationFlowIT.java` (integration test with `@SpringBootTest`):
  - Test full HTTP flow: GET registration form → POST form data → GET payment page → POST payment → GET confirmation
  - Use `MockMvc`
- [ ] Create `AdminEventControllerTest.java`:
  - Test: non-admin cannot access `/admin/**` (expect 403)
  - Test: admin can create an event
  - Test: admin can delete an event

#### Phase 5 — GitHub Actions CI

- [ ] Create `.github/workflows/ci.yml`:
  ```yaml
  name: CI
  on: [push, pull_request]
  jobs:
    test:
      runs-on: ubuntu-latest
      steps:
        - uses: actions/checkout@v4
        - uses: actions/setup-java@v4
          with: { java-version: '17', distribution: 'temurin' }
        - run: mvn test -Dspring.profiles.active=test
  ```

#### Phase 6 — Remaining Admin User Management

*(Alei covers the controller; Islam covers the service layer integration and any remaining tests)*

- [ ] Create `AdminUserService.java`:
  - `Page<User> getAllUsers(Pageable pageable)`
  - `User promoteToAdmin(Long userId)` — set `isAdmin = true`
  - `void deleteUser(Long userId)` — check user is not deleting themselves

---

## Shared Conventions

### Code Style
- Use **Lombok** `@Getter`, `@Setter`, `@Builder`, `@AllArgsConstructor`, `@NoArgsConstructor` on entity/DTO classes to reduce boilerplate.
- Every `@Controller` should be thin — delegate all business logic to a `@Service`.
- Every `@Service` method that writes data should be `@Transactional`.
- Log important actions with `private static final Logger log = LoggerFactory.getLogger(ClassName.class)`.

### Security Rules (Non-Negotiable)
- **NEVER** store plain-text passwords or security question answers. Always use `BCryptPasswordEncoder`.
- **NEVER** store raw card data (mock payment only).
- All admin routes protected by `AdminInterceptor` (Morsy) **AND** Spring Security role check.
- CSRF protection is enabled by default in Spring Security — do not disable it. All forms must include Thymeleaf's `th:action` (which auto-includes CSRF token).
- Validate all user input with Bean Validation (`@Valid`) before any database write.
- Use parameterized queries (JPA/JPQL) — never concatenate strings into queries.

### Thymeleaf Template Conventions
- All public pages extend `fragments/public-layout.html` using `th:replace` or `th:decorate`.
- All admin pages extend `fragments/admin-layout.html`.
- Flash messages use session attributes: `successMessage`, `errorMessage`. Add them to layouts.
- Dates format: `${#temporals.format(event.dateTime, 'dd MMM yyyy, HH:mm')}`.
- Prices: `${#numbers.formatDecimal(event.price, 1, 2)}` or show "Free" if null.
- Status badges: use CSS classes `.badge .badge-confirmed`, `.badge-waitlisted`, etc.

### Exception Handling
- Throw meaningful custom exceptions from `@Service` methods.
- Catch them in `GlobalControllerAdvice.java` (Faris) and return the appropriate error page or redirect with a flash message.
- Never let raw `Exception` or `RuntimeException` propagate to the user.

### Environment Variables
| Variable | Description | Default (dev) |
|----------|-------------|---------------|
| `DB_USERNAME` | MariaDB username | `root` |
| `DB_PASSWORD` | MariaDB password | *(blank for XAMPP default)* |
| `MAIL_HOST` | SMTP host | `smtp.mailtrap.io` |
| `MAIL_PORT` | SMTP port | `2525` |
| `MAIL_USERNAME` | SMTP username | *(Mailtrap inbox)* |
| `MAIL_PASSWORD` | SMTP password | *(Mailtrap inbox)* |
| `APP_SECRET` | Remember-me key | `evently-dev-secret` |

Set these in `application-local.properties` (gitignored) or as OS environment variables.

---

## Integration Checklist

Use this checklist before merging any branch into `develop`:

- [ ] All my pages extend the correct layout fragment
- [ ] All forms use `th:action` (CSRF token included automatically)
- [ ] All passwords/answers use BCrypt  
- [ ] All service writes are `@Transactional`
- [ ] No raw SQL string concatenation — only JPQL/named parameters
- [ ] Flash messages show correctly for my actions
- [ ] My endpoints return proper HTTP status codes on error
- [ ] I have checked the error pages load correctly (404, 403, 500)
- [ ] No credentials are hardcoded in any source file
- [ ] My feature branch is up to date with `develop` (no conflicts)
- [ ] All my tests pass: `mvn test -Dspring.profiles.active=test`

---

## Final Product Reference

The final application must match these screens at a minimum:

| Screen | URL | Who Builds It |
|--------|-----|---------------|
| Home / Landing | `/` | Ehab |
| Events Listing | `/events` | Ehab |
| Event Detail | `/events/{id}` | Ehab |
| Register (Step 1) | `/register/events/{id}` | Ahmed |
| Payment (Step 2) | `/register/events/{id}/payment` | Ahmed |
| Confirmation (Step 3) | `/register/confirmation/{id}` | Ahmed |
| Login | `/auth/login` | Alei |
| Register Account | `/auth/register` | Alei |
| Forgot Password | `/auth/forgot-password` | Alei |
| Profile | `/profile` | Alei |
| User Dashboard | `/dashboard` | Ahmed |
| Admin Dashboard | `/admin` | Morsy |
| Admin Events | `/admin/events` | Morsy |
| Admin Event Form | `/admin/events/new` | Morsy |
| Admin Registrations | `/admin/events/{id}/registrations` | Islam |
| Admin Users | `/admin/users` | Alei |
| 403 | `/403` | Faris |
| 404 | (auto) | Faris |

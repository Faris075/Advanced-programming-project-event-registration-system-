# Faris — Task Sheet
## Project: Evently Event Registration System

**Your branch:** `feature/faris-setup`
**Merge target:** `develop`
**You are:** the foundation. Everyone else depends on your work being done first.

---

## Your Role
You set up the entire project skeleton, database, and data layer. No one can start coding their feature until your entities and repositories are available. Push your branch as soon as possible and let the team know when it is merged into `develop`.

---

## Prerequisites
- Java 21+ installed (`java -version`) — project runs on Java 25 JVM, compiled to Java 21 bytecode
- Maven 3.8+ installed (`mvn -version`)
- MariaDB 10.4 running locally (bundled with XAMPP — start via XAMPP Control Panel)
- IntelliJ IDEA (recommended) or VS Code with Java Extension Pack
- Git configured with your GitHub account

---

## Step-by-Step Tasks

### Step 1 — Bootstrap the Maven project
- [x] Go to [https://start.spring.io](https://start.spring.io) and generate a project with:
  - [x] **Group:** `com.evently`
  - [x] **Artifact:** `evently`
  - [x] **Java:** 21
  - [x] **Packaging:** Jar
  - [x] **Dependencies:** Spring Web, Spring Security, Spring Data JPA, Thymeleaf, Thymeleaf Extras Spring Security 6, MySQL Driver, Spring Boot DevTools, Lombok, Spring Boot Starter Mail, Validation
- [x] Download, unzip, and push to the `feature/faris-setup` branch
- [x] Invite the other 5 developers as GitHub collaborators

### Step 2 — Configuration files
- [x] Edit `src/main/resources/application.properties`:
  ```properties
  spring.datasource.url=jdbc:mariadb://localhost:3306/evently_db?useSSL=false&serverTimezone=UTC
  spring.datasource.username=${DB_USERNAME:root}
  spring.datasource.password=${DB_PASSWORD:}
  spring.datasource.driver-class-name=org.mariadb.jdbc.Driver
  spring.jpa.hibernate.ddl-auto=validate
  spring.jpa.show-sql=true
  spring.jpa.open-in-view=false
  spring.thymeleaf.cache=false
  evently.security.remember-me-key=${APP_SECRET:evently-dev-secret}
  ```
  > **Note:** No explicit `hibernate.dialect` is needed — Hibernate auto-detects MariaDB from the driver.
- [x] Create `src/main/resources/application-local.properties` (add this filename to `.gitignore`):
  ```properties
  DB_USERNAME=root
  DB_PASSWORD=your_password_here
  ```

### Step 3 — Create the MariaDB database
Run these SQL commands in HeidiSQL/DBeaver or the MariaDB CLI:
```sql
CREATE DATABASE IF NOT EXISTS evently_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE evently_db;

CREATE TABLE IF NOT EXISTS users (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name                VARCHAR(255) NOT NULL,
    email               VARCHAR(255) NOT NULL UNIQUE,
    password            VARCHAR(255) NOT NULL,
    is_admin            TINYINT(1)   NOT NULL DEFAULT 0,
    security_question   VARCHAR(500) NULL,
    security_answer     VARCHAR(255) NULL,
    currency_preference VARCHAR(10)  NOT NULL DEFAULT 'USD',
    remember_token      VARCHAR(100) NULL,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS events (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(255)  NOT NULL,
    description TEXT          NOT NULL,
    date_time   DATETIME      NOT NULL,
    location    VARCHAR(255)  NOT NULL,
    capacity    INT           NOT NULL,
    price       DECIMAL(10,2) NULL,
    status      ENUM('draft','published','cancelled','completed') NOT NULL DEFAULT 'draft',
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_events_status (status),
    INDEX idx_events_date_time (date_time)
);

CREATE TABLE IF NOT EXISTS attendees (
    id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL UNIQUE,
    phone      VARCHAR(30)  NULL,
    company    VARCHAR(255) NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_attendees_email (email)
);

CREATE TABLE IF NOT EXISTS registrations (
    id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    event_id          BIGINT UNSIGNED NOT NULL,
    attendee_id       BIGINT UNSIGNED NOT NULL,
    registration_date TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status            ENUM('confirmed','waitlisted','cancelled') NOT NULL DEFAULT 'confirmed',
    payment_status    ENUM('pending','paid','refunded')          NOT NULL DEFAULT 'pending',
    waitlist_position INT             NULL,
    is_admin_override TINYINT(1)      NOT NULL DEFAULT 0,
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_reg_event    FOREIGN KEY (event_id)    REFERENCES events(id)    ON DELETE CASCADE,
    CONSTRAINT fk_reg_attendee FOREIGN KEY (attendee_id) REFERENCES attendees(id) ON DELETE CASCADE,
    UNIQUE KEY uq_event_attendee (event_id, attendee_id),
    INDEX idx_reg_event_id    (event_id),
    INDEX idx_reg_attendee_id (attendee_id),
    INDEX idx_reg_status      (status)
);
```

- [x] Run SQL commands in HeidiSQL/DBeaver or the MariaDB CLI (all 4 tables created)

- [x] Also save this as `src/main/resources/schema.sql` so any team member can run it.

### Step 4 — Create the three enum types
- [x] Create these three files in `src/main/java/com/evently/model/`:

  - [x] **`EventStatus.java`**
  ```java
  package com.evently.model;
  public enum EventStatus { DRAFT, PUBLISHED, CANCELLED, COMPLETED }
  ```

  - [x] **`RegistrationStatus.java`**
  ```java
  package com.evently.model;
  public enum RegistrationStatus { CONFIRMED, WAITLISTED, CANCELLED }
  ```

  - [x] **`PaymentStatus.java`**
  ```java
  package com.evently.model;
  public enum PaymentStatus { PENDING, PAID, REFUNDED }
  ```

### Step 5 — Create the four JPA entities
- [x] All four files go in `src/main/java/com/evently/model/`. Use `@Entity`, `@Table`, `@Id`, `@GeneratedValue`, `@Column`, `@Enumerated(EnumType.STRING)`, `@ManyToOne`, `@JoinColumn`. Use Lombok `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder` on every entity.

- [x] Required entities: **`User.java`**, **`Event.java`**, **`Attendee.java`**, **`Registration.java`**
(Full field lists are in `DEVELOPER_INSTRUCTIONS.md` → Phase 2.)

- [x] **Critical rule:** Use `@Column(name = "is_admin")` on the `isAdmin` boolean. Hibernate must know the DB column name.

### Step 6 — Create the four repository interfaces
- [x] All four files go in `src/main/java/com/evently/repository/`. Each extends `JpaRepository<Entity, Long>`.

- [x] Key custom methods added (others will call these):
  - [x] `UserRepository`: `findByEmail(String email)`, `existsByEmail(String email)`
  - [x] `EventRepository`: `findByStatusOrderByDateTimeAsc(...)`, `findByStatusAndDateTimeBefore(...)`, `countConfirmedRegistrations(@Param("eventId") Long eventId)` — **use fully-qualified JPQL enum reference:** `com.evently.model.RegistrationStatus.CONFIRMED`, never string literals
  - [x] `AttendeeRepository`: `findByEmail(String email)`
  - [x] `RegistrationRepository`: `findByEventIdAndAttendeeId(...)`, `findWaitlistedByEventIdOrdered(...)` (use `com.evently.model.RegistrationStatus.WAITLISTED` in the JPQL), `findByIdWithLock(@Param("id") Long id)` with `@Lock(LockModeType.PESSIMISTIC_WRITE)`

### Step 7 — Create configuration beans
- [x] `src/main/java/com/evently/config/PasswordEncoderConfig.java` — `@Bean BCryptPasswordEncoder(12)`
- [x] `src/main/java/com/evently/config/WebMvcConfig.java` — implements `WebMvcConfigurer`, registers `AdminInterceptor` for `/admin/**`, registers static resource handlers
- [x] `src/main/java/com/evently/exception/GlobalControllerAdvice.java` — `@ControllerAdvice` with `@ExceptionHandler` for `EventNotFoundException` (→ `error/404`) and `Exception` (→ `error/500`)

### Step 8 — Create the DataSeeder
- [x] `src/main/java/com/evently/DataSeeder.java` — implements `CommandLineRunner`
  - [x] If `admin@evently.com` doesn't exist: insert admin user with BCrypt-encoded password `Admin@1234`, `isAdmin = true`
  - [x] If no events exist: insert 5 sample published events with `dateTime` = now + 7/14/30/45/60 days
  - [x] Must be idempotent (check before inserting, never insert twice)

### Step 9 — Create the main application class
- [x] `src/main/java/com/evently/EventlyApplication.java`
  - [x] Annotate with `@SpringBootApplication`, `@EnableScheduling`, `@EnableAsync`

### Step 10 — Create error page stubs
- [x] `src/main/resources/templates/error/404.html`
- [x] `src/main/resources/templates/error/500.html`
- [x] `src/main/resources/templates/error/403.html`

---

## Files You Own (create all of these)

| File | Package | Status |
|------|---------|--------|
| `EventlyApplication.java` | `com.evently` | ✅ Done |
| `DataSeeder.java` | `com.evently` | ✅ Done |
| `User.java` | `com.evently.model` | ✅ Done |
| `Event.java` | `com.evently.model` | ✅ Done |
| `Attendee.java` | `com.evently.model` | ✅ Done |
| `Registration.java` | `com.evently.model` | ✅ Done |
| `EventStatus.java` | `com.evently.model` | ✅ Done |
| `RegistrationStatus.java` | `com.evently.model` | ✅ Done |
| `PaymentStatus.java` | `com.evently.model` | ✅ Done |
| `UserRepository.java` | `com.evently.repository` | ✅ Done |
| `EventRepository.java` | `com.evently.repository` | ✅ Done |
| `AttendeeRepository.java` | `com.evently.repository` | ✅ Done |
| `RegistrationRepository.java` | `com.evently.repository` | ✅ Done |
| `PasswordEncoderConfig.java` | `com.evently.config` | ✅ Done |
| `WebMvcConfig.java` | `com.evently.config` | ✅ Done |
| `GlobalControllerAdvice.java` | `com.evently.exception` | ✅ Done |
| `EventNotFoundException.java` | `com.evently.exception` | ✅ Done |
| `DuplicateRegistrationException.java` | `com.evently.exception` | ✅ Done |
| `schema.sql` | `src/main/resources/` | ✅ Done |
| `application.properties` | `src/main/resources/` | ✅ Done |
| `error/404.html`, `403.html`, `500.html` | `src/main/resources/templates/error/` | ✅ Done |

---

## Done? Notify the team
When your branch is merged into `develop`, send a message to the group so Alei, Morsy, Ehab, Ahmed, and Islam can pull `develop` and start their branches.

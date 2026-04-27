# Islam — Task Sheet
## Project: Evently Event Registration System

**Your branch:** `feature/islam-admin-mgmt`
**Merge target:** `develop`
**Depends on:** All other branches merged. You go last because your work covers admin registration management (requires Ahmed's RegistrationService), admin user views (requires Alei's), tests (require everything), and CI (requires the full project to build).

---

## Your Role
You own the admin registration management panel (view, filter, force-add, CSV export), the admin-side registration service layer, all JUnit 5 unit + integration tests, the H2 test configuration, and the GitHub Actions CI workflow. You are also responsible for making sure the full project compiles and all tests pass before the final merge.

---

## Prerequisites
- Pull `develop` after all 5 other branches are merged
- Verify application starts successfully: `mvn spring-boot:run`
- Verify `http://localhost:8080/events` shows events
- Verify `http://localhost:8080/admin` is protected (redirects to login if unauthenticated)

---

## Step-by-Step Tasks

### Step 1 — AdminRegistrationService
Create `src/main/java/com/evently/service/AdminRegistrationService.java`.

This service extends what Ahmed built. It adds admin-only capabilities.

```java
package com.evently.service;

import com.evently.model.*;
import com.evently.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminRegistrationService {

    private final RegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final AttendeeRepository attendeeRepository;
    private final EmailService emailService;

    /** Get all registrations for an event (any status). */
    public List<Registration> getByEvent(Long eventId) {
        return registrationRepository.findByEventId(eventId);
    }

    /** Get registrations filtered by status. */
    public List<Registration> getByEventAndStatus(Long eventId, RegistrationStatus status) {
        return registrationRepository.findByEventIdAndStatus(eventId, status);
    }

    /**
     * Admin force-add: bypass capacity limit and add attendee as CONFIRMED.
     * Sets isAdminOverride = true on the registration.
     */
    @Transactional
    public Registration forceAdd(Long eventId, String attendeeEmail) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("Event not found"));
        Attendee attendee = attendeeRepository.findByEmail(attendeeEmail)
            .orElseThrow(() -> new IllegalArgumentException("Attendee not found: " + attendeeEmail));

        if (registrationRepository.findByEventIdAndAttendeeId(eventId, attendee.getId()).isPresent()) {
            throw new IllegalStateException("Attendee is already registered for this event");
        }

        Registration registration = Registration.builder()
            .event(event)
            .attendee(attendee)
            .status(RegistrationStatus.CONFIRMED)
            .paymentStatus(PaymentStatus.PAID)
            .isAdminOverride(true)
            .build();

        Registration saved = registrationRepository.save(registration);
        emailService.sendConfirmationEmail(saved);
        return saved;
    }

    /**
     * Cancel a registration as admin; promotes waitlist if applicable.
     * Delegates to RegistrationService to reuse the waitlist-promotion logic.
     */
    @Transactional
    public void cancelRegistration(Long registrationId, RegistrationService registrationService) {
        registrationService.cancelRegistration(registrationId);
    }

    /**
     * Generate a CSV string of all registrations for an event.
     * Format: id,attendee_name,attendee_email,status,payment_status,registered_at
     */
    public String exportCsv(Long eventId) {
        List<Registration> regs = registrationRepository.findByEventId(eventId);
        StringBuilder sb = new StringBuilder("id,name,email,status,payment_status,registered_at\n");
        for (Registration r : regs) {
            sb.append(r.getId()).append(",")
              .append(r.getAttendee().getName()).append(",")
              .append(r.getAttendee().getEmail()).append(",")
              .append(r.getStatus()).append(",")
              .append(r.getPaymentStatus()).append(",")
              .append(r.getRegistrationDate()).append("\n");
        }
        return sb.toString();
    }
}
```

### Step 2 — AdminRegistrationController
Create `src/main/java/com/evently/controller/admin/AdminRegistrationController.java`:

| Method | URL | Action |
|--------|-----|--------|
| GET | `/admin/registrations` | List all registrations across all events, paged. Optional `?eventId=` and `?status=` filter params. |
| GET | `/admin/registrations/event/{eventId}` | All registrations for a specific event. Accepts `?status=CONFIRMED` etc. |
| POST | `/admin/registrations/event/{eventId}/force-add` | Force-add by attendee email. `@RequestParam String attendeeEmail`. |
| POST | `/admin/registrations/{id}/cancel` | Cancel one registration. Triggers waitlist promotion. |
| GET | `/admin/registrations/event/{eventId}/export.csv` | Stream CSV download. Set `Content-Type: text/csv` and `Content-Disposition: attachment; filename="registrations.csv"`. |

CSV export example:
```java
@GetMapping("/admin/registrations/event/{eventId}/export.csv")
public ResponseEntity<String> exportCsv(@PathVariable Long eventId) {
    String csv = adminRegistrationService.exportCsv(eventId);
    return ResponseEntity.ok()
        .header("Content-Disposition", "attachment; filename=\"registrations-event-" + eventId + ".csv\"")
        .contentType(org.springframework.http.MediaType.parseMediaType("text/csv"))
        .body(csv);
}
```

### Step 3 — Admin registration templates
Create in `src/main/resources/templates/admin/registrations/`:

| Template | Purpose |
|----------|---------|
| `index.html` | Filterable table of registrations: event name, attendee name/email, status badge, payment badge, "Cancel" button |
| `event.html` | Registrations for one specific event. Status filter tabs. "Export CSV" link. "Force Add" form (email input). |

Both templates must include the CSRF token in all form submissions.

### Step 4 — H2 test configuration
Create `src/test/resources/application.properties`:

```properties
spring.datasource.url=jdbc:h2:mem:evently_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect
spring.mail.host=localhost
spring.mail.port=3025
evently.security.remember-me-key=test-secret
```

This uses an in-memory H2 DB that resets between test runs. `create-drop` creates the schema from the entity classes on startup and drops on shutdown — no `schema.sql` needed for tests.

### Step 5 — Unit tests for RegistrationService
Create `src/test/java/com/evently/service/RegistrationServiceTest.java`:

Write at minimum these **5 unit tests** (use `@ExtendWith(MockitoExtension.class)` and mock all repositories):

1. **`register_confirmsWhenCapacityAvailable`** — Mock `countConfirmedRegistrations` returning `2`, event capacity `100`. Assert saved registration has `status = CONFIRMED` and `waitlistPosition = null`.

2. **`register_waitlistsWhenAtCapacity`** — Mock `countConfirmedRegistrations` returning event capacity. Assert saved registration has `status = WAITLISTED` and `waitlistPosition = 1`.

3. **`register_throwsDuplicateRegistrationException`** — Mock `findByEventIdAndAttendeeId` returning a non-empty Optional. Assert `DuplicateRegistrationException` is thrown.

4. **`register_throwsWhenEventNotPublished`** — Mock event with `status = CANCELLED`. Assert `IllegalStateException` is thrown.

5. **`promoteNextFromWaitlist_promotesFirstWaitlistedAttendee`** — Set up one WAITLISTED registration. Assert it becomes CONFIRMED and `waitlistPosition` is null after calling `promoteNextFromWaitlist`.

Template:
```java
@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock EventRepository eventRepository;
    @Mock AttendeeRepository attendeeRepository;
    @Mock RegistrationRepository registrationRepository;
    @Mock EmailService emailService;

    @InjectMocks RegistrationService registrationService;

    @Test
    void register_confirmsWhenCapacityAvailable() {
        // Arrange
        Event event = Event.builder().id(1L).capacity(100)
            .status(EventStatus.PUBLISHED).build();
        Attendee attendee = Attendee.builder().id(1L)
            .name("Test").email("test@test.com").build();
        RegistrationFormDto form = new RegistrationFormDto();
        form.setName("Test"); form.setEmail("test@test.com");

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(attendeeRepository.findByEmail("test@test.com")).thenReturn(Optional.of(attendee));
        when(registrationRepository.findByEventIdAndAttendeeId(1L, 1L)).thenReturn(Optional.empty());
        when(registrationRepository.countConfirmedRegistrations(1L)).thenReturn(2L);
        when(registrationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Registration result = registrationService.register(1L, form);

        // Assert
        assertEquals(RegistrationStatus.CONFIRMED, result.getStatus());
        assertNull(result.getWaitlistPosition());
    }
    // ... other tests
}
```

### Step 6 — Integration tests with MockMvc
Create `src/test/java/com/evently/controller/PublicEventControllerTest.java`:

Use `@WebMvcTest(PublicEventController.class)` with `@MockBean` for repositories.

Write at minimum these **3 integration tests**:

1. **`getEvents_returns200WithEventList`** — Mock `findByStatusOrderByDateTimeAsc` returning a list of 2 events. `GET /events` returns HTTP 200 and the model contains `events`.

2. **`getEventDetail_returns200ForPublishedEvent`** — Mock finding 1 published event. `GET /events/1` returns HTTP 200.

3. **`getEventDetail_redirectsForUnpublishedEvent`** — Mock finding 1 DRAFT event. `GET /events/1` returns a redirect to `/events`.

Template:
```java
@WebMvcTest(PublicEventController.class)
@Import(SecurityConfig.class)
class PublicEventControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean EventRepository eventRepository;
    @MockBean RegistrationRepository registrationRepository;
    @MockBean UserRepository userRepository;
    @MockBean PasswordEncoderConfig passwordEncoderConfig;

    @Test
    void getEvents_returns200WithEventList() throws Exception {
        when(eventRepository.findByStatusOrderByDateTimeAsc(EventStatus.PUBLISHED))
            .thenReturn(List.of(
                Event.builder().id(1L).title("Test Event")
                    .dateTime(LocalDateTime.now().plusDays(7))
                    .status(EventStatus.PUBLISHED).build()
            ));

        mockMvc.perform(get("/events"))
            .andExpect(status().isOk())
            .andExpect(model().attributeExists("events"));
    }
}
```

### Step 7 — GitHub Actions CI workflow
Create `.github/workflows/ci.yml` at the repository root:

```yaml
name: Java CI

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven

      - name: Build and test
        run: mvn --batch-mode --update-snapshots verify
        env:
          DB_USERNAME: sa
          DB_PASSWORD: ''
          APP_SECRET: ci-test-secret
          MAIL_USERNAME: ''
          MAIL_PASSWORD: ''
```

This workflow runs on every push to `main` or `develop` and on every pull request. It uses H2 (no MySQL needed in CI) because the test `application.properties` configures H2.

---

## Files You Own

| File | Package / Path |
|------|---------------|
| `AdminRegistrationService.java` | `com.evently.service` |
| `AdminRegistrationController.java` | `com.evently.controller.admin` |
| `admin/registrations/index.html` | `templates/admin/registrations/` |
| `admin/registrations/event.html` | `templates/admin/registrations/` |
| `application.properties` (test) | `src/test/resources/` |
| `RegistrationServiceTest.java` | `src/test/java/com/evently/service/` |
| `PublicEventControllerTest.java` | `src/test/java/com/evently/controller/` |
| `.github/workflows/ci.yml` | `.github/workflows/` |

---

## Running All Tests
```bash
mvn test
```

To run only your tests:
```bash
mvn test -Dtest=RegistrationServiceTest,PublicEventControllerTest
```

---

## Common Mistakes to Avoid
- In `@WebMvcTest`, Spring Security is active by default. Import `SecurityConfig` with `@Import` or use `@WithMockUser` on tests that access protected routes.
- H2's `MODE=MySQL` handles most syntax differences, but some MySQL-specific SQL (like `TINYINT(1)`) may not work in H2. Rely on JPA/Hibernate for schema creation in tests (`ddl-auto=create-drop`) rather than running `schema.sql`.
- The CSV export sets `Content-Type: text/csv`. Never set it to `application/json` — the browser must prompt a download.
- Always `@MockBean EmailService` in any test that exercises code that calls `EmailService` — otherwise Spring will try to create a real `JavaMailSender`.

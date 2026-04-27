# Mohamed Morsy — Task Sheet
## Project: Evently Event Registration System

**Your branch:** `feature/morsy-admin-events`
**Merge target:** `develop`
**Depends on:** Faris's branch (entities + repositories). Alei's `SecurityConfig` must also be merged so admin routes are protected before you test.

---

## Your Role
You own the admin event management panel: full CRUD for events (create, edit, publish, cancel, delete), the admin dashboard with statistics, the event auto-completion scheduler, and the main admin layout template that all other admin pages extend.

---

## Prerequisites
- Pull `develop` (Faris + Alei merged)
- Database created and schema applied
- Application starts successfully (`mvn spring-boot:run`) and you can log in as `admin@evently.com` / `Admin@1234`

---

## Step-by-Step Tasks

### Step 1 — AdminInterceptor
Create `src/main/java/com/evently/interceptor/AdminInterceptor.java`.

This interceptor is a last-resort safeguard on top of `SecurityConfig`. It double-checks that any user hitting `/admin/**` has the `ROLE_ADMIN` authority.

```java
package com.evently.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetails ud) {
            boolean isAdmin = ud.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            if (isAdmin) return true;
        }
        response.sendRedirect(request.getContextPath() + "/auth/login?forbidden");
        return false;
    }
}
```

The `WebMvcConfig` (created by Faris) already registers this interceptor for `/admin/**`. You just need to create the file.

### Step 2 — EventDto
Create `src/main/java/com/evently/dto/EventDto.java`:

```java
package com.evently.dto;

import com.evently.model.EventStatus;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
public class EventDto {
    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Date and time is required")
    @Future(message = "Event must be in the future")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime dateTime;

    @NotBlank(message = "Location is required")
    private String location;

    @Min(value = 1, message = "Capacity must be at least 1")
    private int capacity;

    @DecimalMin(value = "0.0", message = "Price cannot be negative")
    private BigDecimal price;   // null = free event

    private EventStatus status = EventStatus.DRAFT;
}
```

### Step 3 — AdminDashboardController
Create `src/main/java/com/evently/controller/admin/AdminDashboardController.java`:

- **GET `/admin`** (or `/admin/dashboard`) → gather stats and return `"admin/dashboard"`:
  - Total events count (all statuses)
  - Upcoming published events count
  - Total registrations count
  - Total confirmed registrations count
  - Total waitlisted registrations count
  - Total users count
  - List of the 5 most recent events

Use `EventRepository` and `RegistrationRepository` to pull these numbers. Add them all to the model.

```java
model.addAttribute("totalEvents", eventRepository.count());
model.addAttribute("publishedEvents", eventRepository.countByStatus(EventStatus.PUBLISHED));
// etc.
```

### Step 4 — AdminEventController (full CRUD)
Create `src/main/java/com/evently/controller/admin/AdminEventController.java`:

| Method | URL | Action |
|--------|-----|--------|
| GET | `/admin/events` | Paginated list of all events (all statuses). Accepts `?page=0&size=10`. |
| GET | `/admin/events/create` | Show create-event form with empty `EventDto` |
| POST | `/admin/events/create` | Validate form, create `Event` from DTO, save with status `DRAFT`, redirect to `/admin/events` |
| GET | `/admin/events/{id}/edit` | Load event, populate `EventDto`, show edit form |
| POST | `/admin/events/{id}/edit` | Validate form, update entity fields (do not allow changing status here), save, redirect |
| POST | `/admin/events/{id}/publish` | Set `status = PUBLISHED`, save, redirect back with flash message |
| POST | `/admin/events/{id}/cancel` | Set `status = CANCELLED`, save. Also set all CONFIRMED/WAITLISTED registrations for this event to CANCELLED. Redirect back. |
| POST | `/admin/events/{id}/delete` | Only allow deletion if status is `DRAFT` or `CANCELLED`. Otherwise reject with error message. Delete, redirect to `/admin/events`. |

**Event → DTO conversion helper (add as private method):**
```java
private Event fromDto(EventDto dto) {
    return Event.builder()
        .title(dto.getTitle())
        .description(dto.getDescription())
        .dateTime(dto.getDateTime())
        .location(dto.getLocation())
        .capacity(dto.getCapacity())
        .price(dto.getPrice())
        .status(dto.getStatus())
        .build();
}
```

### Step 5 — EventScheduler
Create `src/main/java/com/evently/scheduler/EventScheduler.java`:

```java
package com.evently.scheduler;

import com.evently.model.EventStatus;
import com.evently.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventScheduler {

    private final EventRepository eventRepository;

    @Scheduled(cron = "0 0 0 * * *")   // midnight every day
    @Transactional
    public void markCompletedEvents() {
        var pastEvents = eventRepository
            .findByStatusAndDateTimeBefore(EventStatus.PUBLISHED, LocalDateTime.now());
        pastEvents.forEach(event -> event.setStatus(EventStatus.COMPLETED));
        eventRepository.saveAll(pastEvents);
        log.info("Marked {} events as COMPLETED", pastEvents.size());
    }
}
```

### Step 6 — Admin layout template
Create `src/main/resources/templates/admin/layout.html`. This is a Thymeleaf layout fragment that all other admin pages extend:

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title th:text="${pageTitle} + ' — Evently Admin'">Evently Admin</title>
    <link rel="stylesheet" th:href="@{/css/admin.css}" />
</head>
<body>
<nav>
    <a th:href="@{/admin}">Dashboard</a>
    <a th:href="@{/admin/events}">Events</a>
    <a th:href="@{/admin/registrations}">Registrations</a>
    <a th:href="@{/admin/users}">Users</a>
    <form th:action="@{/auth/logout}" method="post" style="display:inline">
        <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}" />
        <button type="submit">Logout</button>
    </form>
</nav>
<main th:fragment="content">
    <!-- child pages replace this fragment -->
</main>
</body>
</html>
```

### Step 7 — Admin event templates
Create these in `src/main/resources/templates/admin/events/`:

| Template | Purpose |
|----------|---------|
| `index.html` | Paginated table: title, date, capacity, status, action buttons (Edit, Publish, Cancel, Delete) |
| `create.html` | Form that POSTs to `/admin/events/create` |
| `edit.html` | Pre-filled form that POSTs to `/admin/events/{id}/edit` |

For every form that is NOT `method="get"` you must include the CSRF token:
```html
<input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}" />
```

### Step 8 — Admin dashboard template
Create `src/main/resources/templates/admin/dashboard.html`:
- 6 stat cards (Total Events, Published, Total Registrations, Confirmed, Waitlisted, Total Users)
- A table showing the 5 most recent events

---

## Files You Own

| File | Package / Path |
|------|---------------|
| `AdminInterceptor.java` | `com.evently.interceptor` |
| `EventDto.java` | `com.evently.dto` |
| `AdminDashboardController.java` | `com.evently.controller.admin` |
| `AdminEventController.java` | `com.evently.controller.admin` |
| `EventScheduler.java` | `com.evently.scheduler` |
| `admin/layout.html` | `templates/admin/` |
| `admin/dashboard.html` | `templates/admin/` |
| `admin/events/index.html` | `templates/admin/events/` |
| `admin/events/create.html` | `templates/admin/events/` |
| `admin/events/edit.html` | `templates/admin/events/` |

---

## Common Mistakes to Avoid
- Do NOT create `@EnableWebSecurity` or touch `SecurityConfig` — that is Alei's file.
- Do NOT touch `WebMvcConfig` — Faris owns it. If you need to add the interceptor registration, tell Faris.
- When querying enums in JPQL, **always use the fully qualified Java enum constant**, never a string literal. For example: `AND r.status = com.evently.model.RegistrationStatus.CONFIRMED`.
- Always redirect after a POST (Post/Redirect/Get pattern) to prevent duplicate form submissions.
- The publish/cancel/delete actions must use `POST` (not `GET`) to be CSRF-protected.
